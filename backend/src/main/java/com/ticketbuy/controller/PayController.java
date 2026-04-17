package com.ticketbuy.controller;

import com.ticketbuy.exception.Result;
import com.ticketbuy.service.PayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;

@RestController
@RequestMapping("/pay")
public class PayController {
    private static final Logger log = LoggerFactory.getLogger(PayController.class);

    private final PayService payService;

    public PayController(PayService payService) {
        this.payService = payService;
    }

    /**
     * 创建支付
     */
    @GetMapping("/create/{orderNo}")
    public void createPayment(
            @PathVariable String orderNo,
            HttpServletResponse response) throws IOException {
        try {
            String form = payService.createPayment(orderNo);
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write(form);
            response.getWriter().flush();
        } catch (Exception e) {
            log.error("Create payment error", e);
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write("<html><body><h2>支付创建失败</h2><p>" + e.getMessage() + "</p><a href='/orders.html'>返回订单列表</a></body></html>");
        }
    }

    /**
     * 支付宝回调
     */
    @PostMapping("/callback")
    public String handleCallback(HttpServletRequest request) {
        return payService.handleCallback(request);
    }

    /**
     * 模拟支付成功（仅用于测试）
     */
    @PostMapping("/mock-success/{orderNo}")
    public Result<Void> mockPaymentSuccess(@PathVariable String orderNo) {
        try {
            payService.mockPaymentSuccess(orderNo);
            return Result.success();
        } catch (Exception e) {
            log.error("Mock payment error", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 查询支付状态
     */
    @GetMapping("/query/{orderNo}")
    public Result<String> queryPayment(@PathVariable String orderNo) {
        try {
            String result = payService.queryPayment(orderNo);
            return Result.success(result);
        } catch (Exception e) {
            log.error("Query payment error", e);
            return Result.error("查询支付状态失败");
        }
    }
}
