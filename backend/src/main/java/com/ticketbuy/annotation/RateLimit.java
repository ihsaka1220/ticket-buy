package com.ticketbuy.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    /**
     * 限流类型: GLOBAL - 全局限流, IP - IP限流, USER - 用户限流
     */
    LimitType type() default LimitType.GLOBAL;

    /**
     * 限流窗口时间（秒）
     */
    int window() default 60;

    /**
     * 限流最大值
     */
    int max() default 100;

    enum LimitType {
        GLOBAL,
        IP,
        USER
    }
}
