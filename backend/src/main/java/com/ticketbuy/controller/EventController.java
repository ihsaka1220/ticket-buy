package com.ticketbuy.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ticketbuy.annotation.RateLimit;
import com.ticketbuy.dto.BuyTicketRequest;
import com.ticketbuy.dto.EventDetailResponse;
import com.ticketbuy.dto.TicketResponse;
import com.ticketbuy.entity.Event;
import com.ticketbuy.entity.EventCategory;
import com.ticketbuy.exception.BusinessException;
import com.ticketbuy.exception.Result;
import com.ticketbuy.repository.EventCategoryMapper;
import com.ticketbuy.service.TicketService;
import com.ticketbuy.service.UserService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/events")
public class EventController {

    private final TicketService ticketService;
    private final EventCategoryMapper categoryMapper;
    private final UserService userService;

    public EventController(TicketService ticketService, EventCategoryMapper categoryMapper, UserService userService) {
        this.ticketService = ticketService;
        this.categoryMapper = categoryMapper;
        this.userService = userService;
    }

    @GetMapping
    @RateLimit(type = RateLimit.LimitType.IP, max = 200)
    public Result<List<Event>> getEventList(
            @RequestParam(required = false) Long categoryId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        List<Event> events = ticketService.getEventList(categoryId);
        return Result.success(events);
    }

    @GetMapping("/{id}")
    @RateLimit(type = RateLimit.LimitType.IP, max = 200)
    public Result<EventDetailResponse> getEventDetail(@PathVariable Long id) {
        EventDetailResponse detail = ticketService.getEventDetail(id);
        if (detail == null) {
            throw new BusinessException("演出不存在");
        }
        return Result.success(detail);
    }

    @GetMapping("/categories")
    public Result<List<EventCategory>> getCategories() {
        List<EventCategory> categories = categoryMapper.selectList(
                new LambdaQueryWrapper<EventCategory>().eq(EventCategory::getStatus, 1)
        );
        return Result.success(categories);
    }

    @PostMapping("/buy")
    @RateLimit(type = RateLimit.LimitType.USER, window = 10, max = 5)
    public Result<TicketResponse> buyTicket(
            @Validated @RequestBody BuyTicketRequest request,
            @RequestHeader("Authorization") String token) {
        Long userId = getUserId(token);
        TicketResponse response = ticketService.buyTicket(userId, request);
        return Result.success(response);
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
