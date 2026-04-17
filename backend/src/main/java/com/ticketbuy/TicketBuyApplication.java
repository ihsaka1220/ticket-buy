package com.ticketbuy;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling
@MapperScan("com.ticketbuy.repository")
public class TicketBuyApplication {
    public static void main(String[] args) {
        SpringApplication.run(TicketBuyApplication.class, args);
    }
}
