package com.ticketbuy.kafka;

import com.ticketbuy.config.KafkaConfig;
import com.ticketbuy.entity.Invoice;
import com.ticketbuy.entity.Order;
import com.ticketbuy.repository.InvoiceMapper;
import com.ticketbuy.repository.OrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.LocalDateTime;

@Component
public class PaymentConsumer {
    private static final Logger log = LoggerFactory.getLogger(PaymentConsumer.class);

    private final OrderMapper orderMapper;
    private final InvoiceMapper invoiceMapper;

    public PaymentConsumer(OrderMapper orderMapper, InvoiceMapper invoiceMapper) {
        this.orderMapper = orderMapper;
        this.invoiceMapper = invoiceMapper;
    }

    @KafkaListener(topics = KafkaConfig.TOPIC_PAYMENT, groupId = "ticket-buy-group")
    @Transactional
    public void handlePaymentMessage(String message) {
        try {
            PaymentMessage paymentMessage = parseMessage(message);
            log.info("Received payment message: {}", paymentMessage);

            if ("SUCCESS".equals(paymentMessage.getPayStatus())) {
                handlePaymentSuccess(paymentMessage);
            } else {
                handlePaymentFailed(paymentMessage);
            }
        } catch (Exception e) {
            log.error("Failed to process payment message: {}", message, e);
        }
    }

    private void handlePaymentSuccess(PaymentMessage message) {
        LambdaUpdateWrapper<Order> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Order::getOrderNo, message.getOrderNo())
                .set(Order::getStatus, 2)
                .set(Order::getPayTradeNo, message.getTradeNo())
                .set(Order::getPayTime, LocalDateTime.now());
        orderMapper.update(null, updateWrapper);

        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getOrderNo, message.getOrderNo())
        );
        if (order != null) {
            Invoice invoice = new Invoice();
            invoice.setOrderId(order.getId());
            invoice.setUserId(order.getUserId());
            invoice.setTitleType(1);
            invoice.setTitle("个人");
            invoice.setAmount(order.getTotalAmount());
            invoice.setStatus(1);
            invoiceMapper.insert(invoice);
        }

        log.info("Payment processed successfully: {}", message.getOrderNo());
    }

    private void handlePaymentFailed(PaymentMessage message) {
        log.warn("Payment failed: {}", message.getOrderNo());
    }

    private PaymentMessage parseMessage(String json) throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        return mapper.readValue(json, PaymentMessage.class);
    }
}
