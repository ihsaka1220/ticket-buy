package com.ticketbuy.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ticketbuy.entity.Order;
import com.ticketbuy.kafka.OrderMessage;
import com.ticketbuy.kafka.TicketProducer;
import com.ticketbuy.repository.OrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class OrderTimeoutTask {
    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutTask.class);

    private final OrderMapper orderMapper;
    private final TicketProducer ticketProducer;

    public OrderTimeoutTask(OrderMapper orderMapper, TicketProducer ticketProducer) {
        this.orderMapper = orderMapper;
        this.ticketProducer = ticketProducer;
    }

    @Scheduled(fixedRate = 60000)
    public void checkTimeoutOrders() {
        log.info("Checking timeout orders...");

        LocalDateTime now = LocalDateTime.now();

        List<Order> timeoutOrders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getStatus, 1)
                        .lt(Order::getExpireTime, now)
                        .orderByAsc(Order::getCreatedAt)
        );

        log.info("Found {} timeout orders", timeoutOrders.size());

        for (Order order : timeoutOrders) {
            try {
                closeOrder(order);
            } catch (Exception e) {
                log.error("Failed to close order: {}", order.getOrderNo(), e);
            }
        }
    }

    private void closeOrder(Order order) {
        log.info("Closing order: {}", order.getOrderNo());

        LambdaUpdateWrapper<Order> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Order::getId, order.getId())
                .eq(Order::getStatus, 1)
                .set(Order::getStatus, 5);

        int updated = orderMapper.update(null, updateWrapper);
        if (updated == 0) {
            log.warn("Order status already changed: {}", order.getOrderNo());
            return;
        }

        OrderMessage cancelMessage = OrderMessage.builder()
                .orderNo(order.getOrderNo())
                .userId(order.getUserId())
                .ticketTypeId(order.getTicketTypeId())
                .quantity(order.getQuantity())
                .type(2)
                .build();
        ticketProducer.sendOrderMessage(cancelMessage);

        log.info("Order closed successfully: {}", order.getOrderNo());
    }
}
