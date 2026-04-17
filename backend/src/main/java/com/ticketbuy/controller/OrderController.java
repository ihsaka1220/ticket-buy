package com.ticketbuy.controller;

import com.ticketbuy.entity.Order;
import com.ticketbuy.exception.BusinessException;
import com.ticketbuy.exception.Result;
import com.ticketbuy.service.TicketService;
import com.ticketbuy.service.UserService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final TicketService ticketService;
    private final UserService userService;

    public OrderController(TicketService ticketService, UserService userService) {
        this.ticketService = ticketService;
        this.userService = userService;
    }

    @GetMapping
    public Result<List<Order>> getUserOrders(@RequestHeader("Authorization") String token) {
        Long userId = getUserId(token);
        List<Order> orders = ticketService.getUserOrders(userId);
        return Result.success(orders);
    }

    @GetMapping("/{orderNo}")
    public Result<Order> getOrderDetail(@PathVariable String orderNo) {
        Order order = ticketService.getOrderByNo(orderNo);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        return Result.success(order);
    }

    @PostMapping("/{orderNo}/cancel")
    public Result<Void> cancelOrder(
            @PathVariable String orderNo,
            @RequestHeader("Authorization") String token) {
        Long userId = getUserId(token);
        ticketService.cancelOrder(userId, orderNo);
        return Result.success();
    }

    private Long getUserId(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Long userId = userService.getUserIdByToken(token);
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        return userId;
    }
}
