package com.ticketbuy.config;

import com.ticketbuy.service.TicketInventoryService;
import com.ticketbuy.entity.TicketType;
import com.ticketbuy.repository.TicketTypeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class DataInitRunner implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataInitRunner.class);

    private final TicketTypeMapper ticketTypeMapper;
    private final TicketInventoryService inventoryService;

    public DataInitRunner(TicketTypeMapper ticketTypeMapper, TicketInventoryService inventoryService) {
        this.ticketTypeMapper = ticketTypeMapper;
        this.inventoryService = inventoryService;
    }

    @Override
    public void run(String... args) {
        log.info("Initializing Redis stock data...");
        List<TicketType> ticketTypes = ticketTypeMapper.selectList(null);
        for (TicketType ticketType : ticketTypes) {
            inventoryService.initStock(ticketType.getId(), ticketType.getAvailableStock());
        }
        log.info("Redis stock data initialized, total: {}", ticketTypes.size());
    }
}
