package com.ticketbuy.kafka;

import com.ticketbuy.config.KafkaConfig;
import com.ticketbuy.entity.Order;
import com.ticketbuy.entity.TicketType;
import com.ticketbuy.repository.OrderMapper;
import com.ticketbuy.repository.TicketTypeMapper;
import com.ticketbuy.service.TicketInventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.List;

@Component
public class OrderConsumer {
    private static final Logger log = LoggerFactory.getLogger(OrderConsumer.class);

    private final OrderMapper orderMapper;
    private final TicketTypeMapper ticketTypeMapper;
    private final TicketInventoryService ticketInventoryService;
    private final StringRedisTemplate stringRedisTemplate;
    private final TicketProducer ticketProducer;
    private final InvoiceService invoiceService;

    public OrderConsumer(OrderMapper orderMapper, TicketTypeMapper ticketTypeMapper,
                         TicketInventoryService ticketInventoryService,
                         StringRedisTemplate stringRedisTemplate,
                         TicketProducer ticketProducer, InvoiceService invoiceService) {
        this.orderMapper = orderMapper;
        this.ticketTypeMapper = ticketTypeMapper;
        this.ticketInventoryService = ticketInventoryService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.ticketProducer = ticketProducer;
        this.invoiceService = invoiceService;
    }

    @KafkaListener(topics = KafkaConfig.TOPIC_TICKET_ORDER, groupId = "ticket-buy-group")
    public void handleOrderMessage(String message) {
        try {
            OrderMessage orderMessage = parseMessage(message);
            log.info("Received order message: {}", orderMessage);

            if (orderMessage.getType() == 1) {
                handleCreateOrder(orderMessage);
            } else if (orderMessage.getType() == 2) {
                handleCancelOrder(orderMessage);
            }
        } catch (Exception e) {
            log.error("Failed to process order message: {}", message, e);
        }
    }

    @Transactional
    public void handleCreateOrder(OrderMessage message) {
        try {
            int updateCount = updateTicketStock(message.getTicketTypeId(), message.getQuantity());
            if (updateCount == 0) {
                log.warn("Failed to update ticket stock, orderNo: {}", message.getOrderNo());
                return;
            }

            String payUrl = generatePayUrl(message);

            LambdaUpdateWrapper<Order> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Order::getOrderNo, message.getOrderNo())
                    .set(Order::getPayUrl, payUrl);
            orderMapper.update(null, updateWrapper);

            String payCodeKey = "pay:code:" + message.getOrderNo();
            stringRedisTemplate.opsForValue().set(payCodeKey, payUrl, 15, java.util.concurrent.TimeUnit.MINUTES);

            log.info("Order created successfully: {}", message.getOrderNo());
        } catch (Exception e) {
            log.error("Failed to create order: {}", message.getOrderNo(), e);
        }
    }

    @Transactional
    public void handleCancelOrder(OrderMessage message) {
        try {
            TicketType ticketType = ticketTypeMapper.selectById(message.getTicketTypeId());
            if (ticketType != null) {
                ticketType.setAvailableStock(ticketType.getAvailableStock() + message.getQuantity());
                ticketTypeMapper.updateById(ticketType);
            }

            ticketInventoryService.rollbackStock(
                    String.valueOf(message.getTicketTypeId()),
                    message.getQuantity(),
                    message.getOrderNo()
            );

            LambdaUpdateWrapper<Order> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Order::getOrderNo, message.getOrderNo())
                    .set(Order::getStatus, 5);
            orderMapper.update(null, updateWrapper);

            stringRedisTemplate.delete("pay:code:" + message.getOrderNo());

            log.info("Order cancelled: {}", message.getOrderNo());
        } catch (Exception e) {
            log.error("Failed to cancel order: {}", message.getOrderNo(), e);
        }
    }

    private int updateTicketStock(Long ticketTypeId, Integer quantity) {
        return ticketTypeMapper.update(null,
                new LambdaUpdateWrapper<TicketType>()
                        .eq(TicketType::getId, ticketTypeId)
                        .ge(TicketType::getAvailableStock, quantity)
                        .setSql("available_stock = available_stock - " + quantity)
        );
    }

    private String generatePayUrl(OrderMessage message) {
        return "https://openapi.alipay.com/gateway.do?out_trade_no=" + message.getOrderNo()
                + "&total_amount=" + message.getTotalAmount()
                + "&subject=" + message.getEventId();
    }

    private OrderMessage parseMessage(String json) throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        return mapper.readValue(json, OrderMessage.class);
    }
}
