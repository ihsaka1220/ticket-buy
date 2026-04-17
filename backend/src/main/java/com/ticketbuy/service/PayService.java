package com.ticketbuy.service;

import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ticketbuy.config.AlipayConfig;
import com.ticketbuy.entity.Order;
import com.ticketbuy.kafka.PaymentMessage;
import com.ticketbuy.kafka.TicketProducer;
import com.ticketbuy.repository.OrderMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class PayService {
    private static final Logger log = LoggerFactory.getLogger(PayService.class);

    private final AlipayClient alipayClient;
    private final AlipayConfig alipayConfig;
    private final OrderMapper orderMapper;
    private final TicketProducer ticketProducer;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${alipay.mock-mode:true}")
    private boolean mockMode;

    public PayService(AlipayClient alipayClient, AlipayConfig alipayConfig,
                      OrderMapper orderMapper, TicketProducer ticketProducer,
                      StringRedisTemplate stringRedisTemplate) {
        this.alipayClient = alipayClient;
        this.alipayConfig = alipayConfig;
        this.orderMapper = orderMapper;
        this.ticketProducer = ticketProducer;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public String createPayment(String orderNo) throws Exception {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo)
        );

        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        if (order.getStatus() != 1) {
            throw new RuntimeException("订单状态异常");
        }

        String form;

        if (mockMode) {
            // 模拟支付模式 - 生成模拟支付页面
            form = generateMockPaymentPage(order);
            log.info("Mock payment mode enabled, generated mock payment page for order: {}", orderNo);
        } else {
            // 真实支付宝支付
            AlipayTradePagePayRequest payRequest = new AlipayTradePagePayRequest();
            payRequest.setReturnUrl(alipayConfig.getReturnUrl());
            payRequest.setNotifyUrl(alipayConfig.getNotifyUrl());

            StringBuilder bizContent = new StringBuilder();
            bizContent.append("{");
            bizContent.append("\"out_trade_no\":\"").append(orderNo).append("\",");
            bizContent.append("\"total_amount\":\"").append(order.getTotalAmount().setScale(2, BigDecimal.ROUND_HALF_UP)).append("\",");
            bizContent.append("\"subject\":\"门票购买-").append(orderNo).append("\",");
            bizContent.append("\"product_code\":\"FAST_INSTANT_TRADE_PAY\"");
            bizContent.append("}");

            payRequest.setBizContent(bizContent.toString());
            form = alipayClient.pageExecute(payRequest).getBody();
        }

        String payCodeKey = "pay:code:" + orderNo;
        stringRedisTemplate.opsForValue().set(payCodeKey, form, 15, TimeUnit.MINUTES);

        return form;
    }

    /**
     * 生成模拟支付页面
     */
    private String generateMockPaymentPage(Order order) {
        return "<!DOCTYPE html>\n" +
            "<html lang=\"zh-CN\">\n" +
            "<head>\n" +
            "    <meta charset=\"UTF-8\">\n" +
            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
            "    <title>模拟支付</title>\n" +
            "    <style>\n" +
            "        body { font-family: Arial, sans-serif; background: #f5f5f5; display: flex; justify-content: center; align-items: center; min-height: 100vh; margin: 0; }\n" +
            "        .container { background: white; padding: 40px; border-radius: 10px; box-shadow: 0 2px 20px rgba(0,0,0,0.1); text-align: center; max-width: 400px; }\n" +
            "        h1 { color: #1677ff; margin-bottom: 20px; }\n" +
            "        .order-info { text-align: left; margin: 20px 0; padding: 15px; background: #f5f5f5; border-radius: 5px; }\n" +
            "        .order-info p { margin: 8px 0; }\n" +
            "        .amount { font-size: 32px; color: #ff4d4f; font-weight: bold; margin: 20px 0; }\n" +
            "        .btn { display: inline-block; padding: 15px 50px; background: #1677ff; color: white; border: none; border-radius: 5px; font-size: 16px; cursor: pointer; margin: 10px; text-decoration: none; }\n" +
            "        .btn:hover { background: #4096ff; }\n" +
            "        .btn-success { background: #52c41a; }\n" +
            "        .btn-success:hover { background: #73d13d; }\n" +
            "        .mock-badge { display: inline-block; background: #faad14; color: white; padding: 3px 10px; border-radius: 3px; font-size: 12px; margin-bottom: 10px; }\n" +
            "    </style>\n" +
            "</head>\n" +
            "<body>\n" +
            "    <div class=\"container\">\n" +
            "        <span class=\"mock-badge\">模拟支付</span>\n" +
            "        <h1>💳 支付确认</h1>\n" +
            "        <div class=\"order-info\">\n" +
            "            <p><strong>订单号：</strong>" + order.getOrderNo() + "</p>\n" +
            "            <p><strong>商品：</strong>门票购买</p>\n" +
            "            <p><strong>数量：</strong>" + order.getQuantity() + " 张</p>\n" +
            "        </div>\n" +
            "        <div class=\"amount\">¥ " + order.getTotalAmount() + "</div>\n" +
            "        <div>\n" +
            "            <button class=\"btn btn-success\" onclick=\"paySuccess()\">确认支付</button>\n" +
            "            <a href=\"/orders.html\" class=\"btn\">取消</a>\n" +
            "        </div>\n" +
            "    </div>\n" +
            "    <script>\n" +
            "        function paySuccess() {\n" +
            "            fetch('/api/pay/mock-success/" + order.getOrderNo() + "', { method: 'POST' })\n" +
            "                .then(res => res.json())\n" +
            "                .then(data => {\n" +
            "                    if (data.code === 200) {\n" +
            "                        alert('支付成功！');\n" +
            "                        window.location.href = '/orders.html';\n" +
            "                    } else {\n" +
            "                        alert('支付失败：' + data.message);\n" +
            "                    }\n" +
            "                })\n" +
            "                .catch(err => alert('网络错误'));\n" +
            "        }\n" +
            "    </script>\n" +
            "</body>\n" +
            "</html>";
    }

    /**
     * 模拟支付成功
     */
    public void mockPaymentSuccess(String orderNo) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo)
        );

        if (order == null || order.getStatus() != 1) {
            throw new RuntimeException("订单不存在或已支付");
        }

        // 发送支付成功消息
        PaymentMessage message = PaymentMessage.builder()
                .orderNo(orderNo)
                .tradeNo("MOCK" + System.currentTimeMillis())
                .amount(order.getTotalAmount())
                .payStatus("SUCCESS")
                .userId(order.getUserId())
                .build();
        ticketProducer.sendPaymentMessage(message);

        log.info("Mock payment success for order: {}", orderNo);
    }

    public String handleCallback(HttpServletRequest request) {
        try {
            Map<String, String> params = extractParams(request);

            // 模拟模式下直接返回成功
            if (mockMode) {
                log.info("Mock mode: callback received");
                return "success";
            }

            boolean signVerified = AlipaySignature.rsaCheckV1(
                    params,
                    alipayConfig.getPublicKey(),
                    alipayConfig.getCharset(),
                    alipayConfig.getSignType()
            );

            if (!signVerified) {
                log.error("Alipay signature verification failed");
                return "failure";
            }

            String tradeStatus = params.get("trade_status");
            String outTradeNo = params.get("out_trade_no");
            String tradeNo = params.get("trade_no");

            log.info("Payment callback: orderNo={}, tradeNo={}, status={}", outTradeNo, tradeNo, tradeStatus);

            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                Order order = orderMapper.selectOne(
                        new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, outTradeNo)
                );

                if (order != null && order.getStatus() == 1) {
                    PaymentMessage message = PaymentMessage.builder()
                            .orderNo(outTradeNo)
                            .tradeNo(tradeNo)
                            .amount(order.getTotalAmount())
                            .payStatus("SUCCESS")
                            .userId(order.getUserId())
                            .build();
                    ticketProducer.sendPaymentMessage(message);
                }
            }

            return "success";
        } catch (Exception e) {
            log.error("Payment callback error", e);
            return "failure";
        }
    }

    public String queryPayment(String orderNo) throws Exception {
        if (mockMode) {
            Order order = orderMapper.selectOne(
                    new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo)
            );
            if (order == null) {
                return "{\"code\":\"FAIL\",\"msg\":\"订单不存在\"}";
            }
            String status = order.getStatus() == 2 ? "TRADE_SUCCESS" : "WAIT_BUYER_PAY";
            return "{\"code\":\"SUCCESS\",\"trade_status\":\"" + status + "\"}";
        }

        // 真实查询逻辑
        return "{\"code\":\"SUCCESS\",\"trade_status\":\"WAIT_BUYER_PAY\"}";
    }

    private Map<String, String> extractParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (String name : requestParams.keySet()) {
            String[] values = requestParams.get(name);
            StringBuilder valueStr = new StringBuilder();
            for (int i = 0; i < values.length; i++) {
                valueStr.append((i == values.length - 1) ? values[i] : values[i] + ",");
            }
            params.put(name, valueStr.toString());
        }
        return params;
    }
}
