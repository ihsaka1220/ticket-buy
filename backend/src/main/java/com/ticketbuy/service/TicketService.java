package com.ticketbuy.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ticketbuy.config.KafkaConfig;
import com.ticketbuy.dto.BuyTicketRequest;
import com.ticketbuy.dto.EventDetailResponse;
import com.ticketbuy.dto.TicketResponse;
import com.ticketbuy.entity.*;
import com.ticketbuy.exception.BusinessException;
import com.ticketbuy.kafka.OrderMessage;
import com.ticketbuy.kafka.TicketProducer;
import com.ticketbuy.repository.*;
import com.ticketbuy.cache.TwoLevelCacheTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TicketService {
    private static final Logger log = LoggerFactory.getLogger(TicketService.class);

    private final EventMapper eventMapper;
    private final EventCategoryMapper categoryMapper;
    private final TicketTypeMapper ticketTypeMapper;
    private final OrderMapper orderMapper;
    private final TicketInventoryService inventoryService;
    private final TicketProducer ticketProducer;
    private final StringRedisTemplate stringRedisTemplate;
    private final TwoLevelCacheTemplate twoLevelCacheTemplate;

    @Value("${ticket.order-timeout-minutes:15}")
    private int orderTimeoutMinutes;

    public TicketService(EventMapper eventMapper, EventCategoryMapper categoryMapper,
                         TicketTypeMapper ticketTypeMapper, OrderMapper orderMapper,
                         TicketInventoryService inventoryService, TicketProducer ticketProducer,
                         StringRedisTemplate stringRedisTemplate, TwoLevelCacheTemplate twoLevelCacheTemplate) {
        this.eventMapper = eventMapper;
        this.categoryMapper = categoryMapper;
        this.ticketTypeMapper = ticketTypeMapper;
        this.orderMapper = orderMapper;
        this.inventoryService = inventoryService;
        this.ticketProducer = ticketProducer;
        this.stringRedisTemplate = stringRedisTemplate;
        this.twoLevelCacheTemplate = twoLevelCacheTemplate;
    }

    public List<Event> getEventList(Long categoryId) {
        String cacheKey = "eventList:" + (categoryId != null ? categoryId : "all");

        return twoLevelCacheTemplate.get(cacheKey, List.class, 300, () -> {
            LambdaQueryWrapper<Event> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Event::getStatus, 2);
            if (categoryId != null) {
                queryWrapper.eq(Event::getCategoryId, categoryId);
            }
            queryWrapper.orderByAsc(Event::getEventDate);
            return eventMapper.selectList(queryWrapper);
        });
    }

    public EventDetailResponse getEventDetail(Long eventId) {
        String cacheKey = "eventDetail:" + eventId;

        return twoLevelCacheTemplate.get(cacheKey, EventDetailResponse.class, 300, () -> {
            Event event = eventMapper.selectById(eventId);
            if (event == null) {
                return null;
            }

            EventCategory category = categoryMapper.selectById(event.getCategoryId());
            String categoryName = category != null ? category.getName() : "未知";

            List<TicketType> ticketTypes = ticketTypeMapper.selectList(
                    new LambdaQueryWrapper<TicketType>()
                            .eq(TicketType::getEventId, eventId)
                            .eq(TicketType::getStatus, 1)
            );

            return EventDetailResponse.from(event, categoryName, ticketTypes);
        });
    }

    @Transactional
    public TicketResponse buyTicket(Long userId, BuyTicketRequest request) {
        Event event = eventMapper.selectById(request.getEventId());
        if (event == null || event.getStatus() != 2) {
            throw new BusinessException("演出不存在或未开售");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(event.getSaleStartTime()) || now.isAfter(event.getSaleEndTime())) {
            throw new BusinessException("不在售票时间内");
        }

        TicketType ticketType = ticketTypeMapper.selectById(request.getTicketTypeId());
        if (ticketType == null || ticketType.getStatus() != 1) {
            throw new BusinessException("票档不存在或已停售");
        }

        if (!ticketType.getEventId().equals(request.getEventId())) {
            throw new BusinessException("票档与演出不匹配");
        }

        inventoryService.checkPurchaseQualification(userId, request.getTicketTypeId(), request.getQuantity());

        String orderNo = generateOrderNo();

        inventoryService.deductStock(userId, request.getTicketTypeId(), request.getQuantity(), orderNo);

        try {
            BigDecimal totalAmount = ticketType.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
            Order order = new Order();
            order.setOrderNo(orderNo);
            order.setUserId(userId);
            order.setEventId(request.getEventId());
            order.setTicketTypeId(request.getTicketTypeId());
            order.setQuantity(request.getQuantity());
            order.setTotalAmount(totalAmount);
            order.setStatus(1);
            order.setExpireTime(LocalDateTime.now().plusMinutes(orderTimeoutMinutes));

            orderMapper.insert(order);

            OrderMessage orderMessage = OrderMessage.builder()
                    .orderNo(orderNo)
                    .userId(userId)
                    .eventId(request.getEventId())
                    .ticketTypeId(request.getTicketTypeId())
                    .quantity(request.getQuantity())
                    .totalAmount(totalAmount)
                    .type(1)
                    .build();
            ticketProducer.sendOrderMessage(orderMessage);

            return TicketResponse.builder()
                    .orderNo(orderNo)
                    .eventTitle(event.getTitle())
                    .ticketTypeName(ticketType.getName())
                    .quantity(request.getQuantity())
                    .totalAmount(totalAmount.toString())
                    .expireTimeSeconds((long) orderTimeoutMinutes * 60)
                    .status("PENDING_PAYMENT")
                    .build();

        } catch (Exception e) {
            inventoryService.rollbackStock(String.valueOf(request.getTicketTypeId()), request.getQuantity(), orderNo);
            throw new BusinessException("购票失败，请重试");
        }
    }

    public Order getOrderByNo(String orderNo) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo)
        );
        // 被动关单：检查订单是否超时
        checkAndCloseExpiredOrder(order);
        return order;
    }

    public List<Order> getUserOrders(Long userId) {
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getUserId, userId)
                        .orderByDesc(Order::getCreatedAt)
        );
        // 被动关单：检查所有待支付订单是否超时
        for (Order order : orders) {
            checkAndCloseExpiredOrder(order);
        }
        return orders;
    }

    /**
     * 被动关单：检查并关闭超时订单
     */
    private void checkAndCloseExpiredOrder(Order order) {
        if (order == null || order.getStatus() != 1) {
            return;
        }
        if (order.getExpireTime() != null && LocalDateTime.now().isAfter(order.getExpireTime())) {
            try {
                // 使用乐观锁更新订单状态
                LambdaUpdateWrapper<Order> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(Order::getId, order.getId())
                        .eq(Order::getStatus, 1)  // 乐观锁：只更新待支付状态的订单
                        .set(Order::getStatus, 3)
                        .set(Order::getRemark, "系统自动关闭-超时未支付");
                int updated = orderMapper.update(null, updateWrapper);

                if (updated > 0) {
                    // 释放库存
                    inventoryService.rollbackStock(String.valueOf(order.getTicketTypeId()), order.getQuantity(), order.getOrderNo());
                    order.setStatus(3);
                    log.info("Order auto closed due to timeout: orderNo={}", order.getOrderNo());
                }
            } catch (Exception e) {
                log.error("Failed to close expired order: orderNo={}", order.getOrderNo(), e);
            }
        }
    }

    @Transactional
    public void cancelOrder(Long userId, String orderNo) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getOrderNo, orderNo)
                        .eq(Order::getUserId, userId)
        );

        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        if (order.getStatus() != 1) {
            throw new BusinessException("订单状态不允许取消");
        }

        LambdaUpdateWrapper<Order> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Order::getId, order.getId())
                .set(Order::getStatus, 3);
        orderMapper.update(null, updateWrapper);

        OrderMessage cancelMessage = OrderMessage.builder()
                .orderNo(orderNo)
                .userId(userId)
                .ticketTypeId(order.getTicketTypeId())
                .quantity(order.getQuantity())
                .type(2)
                .build();
        ticketProducer.sendOrderMessage(cancelMessage);
    }

    private String generateOrderNo() {
        return "ORD" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
