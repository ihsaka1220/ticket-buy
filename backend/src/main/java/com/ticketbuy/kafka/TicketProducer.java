package com.ticketbuy.kafka;

import com.ticketbuy.config.KafkaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import java.util.concurrent.CompletableFuture;

@Component
public class TicketProducer {
    private static final Logger log = LoggerFactory.getLogger(TicketProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;

    public TicketProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendOrderMessage(OrderMessage message) {
        String json = toJson(message);
        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(
                KafkaConfig.TOPIC_TICKET_ORDER,
                message.getOrderNo(),
                json
        );
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Order message sent successfully: {}", json);
            } else {
                log.error("Failed to send order message: {}", json, ex);
            }
        });
    }

    public void sendPaymentMessage(PaymentMessage message) {
        String json = toJson(message);
        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(
                KafkaConfig.TOPIC_PAYMENT,
                message.getOrderNo(),
                json
        );
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Payment message sent successfully: {}", json);
            } else {
                log.error("Failed to send payment message: {}", json, ex);
            }
        });
    }

    private String toJson(Object obj) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("Failed to serialize message", e);
            return "{}";
        }
    }
}
