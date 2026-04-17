package com.ticketbuy.service;

import com.ticketbuy.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;

@Service
public class TicketInventoryService {
    private static final Logger log = LoggerFactory.getLogger(TicketInventoryService.class);

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${ticket.max-buy-per-user:3}")
    private int maxBuyPerUser;

    @Value("${ticket.order-timeout-minutes:15}")
    private int orderTimeoutMinutes;

    private DefaultRedisScript<List> checkPurchaseScript;
    private DefaultRedisScript<List> deductStockScript;
    private DefaultRedisScript<List> rollbackStockScript;

    public TicketInventoryService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @PostConstruct
    public void init() {
        checkPurchaseScript = new DefaultRedisScript<>();
        checkPurchaseScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/check_purchase.lua")));
        checkPurchaseScript.setResultType(List.class);

        deductStockScript = new DefaultRedisScript<>();
        deductStockScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/deduct_stock.lua")));
        deductStockScript.setResultType(List.class);

        rollbackStockScript = new DefaultRedisScript<>();
        rollbackStockScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/rollback_stock.lua")));
        rollbackStockScript.setResultType(List.class);
    }

    public void checkPurchaseQualification(Long userId, Long ticketTypeId, Integer quantity) {
        String stockKey = "stock:" + ticketTypeId;
        String userBuyKey = "user:buy:" + userId + ":" + ticketTypeId;
        String userTotalKey = "user:total:" + userId;

        List<Long> result = stringRedisTemplate.execute(
                checkPurchaseScript,
                List.of(stockKey, userBuyKey, userTotalKey),
                String.valueOf(quantity),
                String.valueOf(maxBuyPerUser),
                String.valueOf(maxBuyPerUser * 10)
        );

        if (result == null || result.isEmpty()) {
            throw new BusinessException("购票资格校验失败");
        }

        Long code = result.get(0);
        if (code == null || code <= 0) {
            String message = result.size() > 1 ? String.valueOf(result.get(1)) : "购票资格校验失败";
            throw new BusinessException(code != null ? code.intValue() : -1, message);
        }
    }

    public void deductStock(Long userId, Long ticketTypeId, Integer quantity, String orderNo) {
        String stockKey = "stock:" + ticketTypeId;
        String userBuyKey = "user:buy:" + userId + ":" + ticketTypeId;
        String userTotalKey = "user:total:" + userId;
        long expireTime = System.currentTimeMillis() / 1000 + orderTimeoutMinutes * 60;

        List<Long> result = stringRedisTemplate.execute(
                deductStockScript,
                List.of(stockKey, userBuyKey, userTotalKey),
                String.valueOf(quantity),
                String.valueOf(maxBuyPerUser),
                String.valueOf(maxBuyPerUser * 10),
                String.valueOf(expireTime),
                orderNo
        );

        if (result == null || result.isEmpty()) {
            throw new BusinessException("库存扣减失败");
        }

        Long code = result.get(0);
        if (code == null || code <= 0) {
            String message = result.size() > 1 ? String.valueOf(result.get(1)) : "库存扣减失败";
            throw new BusinessException(code != null ? code.intValue() : -1, message);
        }

        log.info("Stock deducted successfully: userId={}, ticketTypeId={}, quantity={}, orderNo={}",
                userId, ticketTypeId, quantity, orderNo);
    }

    public void rollbackStock(String ticketTypeId, Integer quantity, String orderNo) {
        String stockKey = "stock:" + ticketTypeId;
        String userBuyKey = "user:buy:*:" + ticketTypeId;
        String userTotalKey = "user:total:*";

        List<Long> result = stringRedisTemplate.execute(
                rollbackStockScript,
                List.of(stockKey, userBuyKey, userTotalKey),
                String.valueOf(quantity),
                orderNo
        );

        log.info("Stock rolled back: ticketTypeId={}, quantity={}, orderNo={}", ticketTypeId, quantity, orderNo);
    }

    public void initStock(Long ticketTypeId, Integer stock) {
        String stockKey = "stock:" + ticketTypeId;
        stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(stock));
        log.info("Stock initialized: ticketTypeId={}, stock={}", ticketTypeId, stock);
    }

    public Integer getCurrentStock(Long ticketTypeId) {
        String stockKey = "stock:" + ticketTypeId;
        String stock = stringRedisTemplate.opsForValue().get(stockKey);
        return stock != null ? Integer.parseInt(stock) : null;
    }
}
