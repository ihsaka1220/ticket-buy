package com.ticketbuy.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String TOPIC_TICKET_ORDER = "ticket-order-topic";
    public static final String TOPIC_PAYMENT = "payment-topic";
    public static final String TOPIC_INVOICE = "invoice-topic";

    @Bean
    public NewTopic orderTopic() {
        return TopicBuilder.name(TOPIC_TICKET_ORDER)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentTopic() {
        return TopicBuilder.name(TOPIC_PAYMENT)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic invoiceTopic() {
        return TopicBuilder.name(TOPIC_INVOICE)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
