package com.ticketbuy.aspect;

import com.ticketbuy.annotation.RateLimit;
import com.ticketbuy.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.UUID;

@Aspect
@Component
public class RateLimitAspect {
    private static final Logger log = LoggerFactory.getLogger(RateLimitAspect.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final HttpServletRequest httpServletRequest;

    @Value("${ticket.rate-limit.global-window:60}")
    private int defaultGlobalWindow;

    @Value("${ticket.rate-limit.global-max:10000}")
    private int defaultGlobalMax;

    @Value("${ticket.rate-limit.ip-window:60}")
    private int defaultIpWindow;

    @Value("${ticket.rate-limit.ip-max:100}")
    private int defaultIpMax;

    @Value("${ticket.rate-limit.user-window:60}")
    private int defaultUserWindow;

    @Value("${ticket.rate-limit.user-max:20}")
    private int defaultUserMax;

    private DefaultRedisScript<Long> rateLimitScript;

    public RateLimitAspect(StringRedisTemplate stringRedisTemplate, HttpServletRequest httpServletRequest) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.httpServletRequest = httpServletRequest;
    }

    @PostConstruct
    public void init() {
        rateLimitScript = new DefaultRedisScript<>();
        rateLimitScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/rate_limit.lua")));
        rateLimitScript.setResultType(Long.class);
    }

    @Around("@annotation(com.ticketbuy.annotation.RateLimit)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);

        String keyPrefix;
        int window;
        int max;

        switch (rateLimit.type()) {
            case IP:
                keyPrefix = "rate:ip:";
                window = rateLimit.window() > 0 ? rateLimit.window() : defaultIpWindow;
                max = rateLimit.max() > 0 ? rateLimit.max() : defaultIpMax;
                break;
            case USER:
                keyPrefix = "rate:user:";
                window = rateLimit.window() > 0 ? rateLimit.window() : defaultUserWindow;
                max = rateLimit.max() > 0 ? rateLimit.max() : defaultUserMax;
                break;
            default:
                keyPrefix = "rate:global:";
                window = rateLimit.window() > 0 ? rateLimit.window() : defaultGlobalWindow;
                max = rateLimit.max() > 0 ? rateLimit.max() : defaultGlobalMax;
        }

        String key = keyPrefix + getLimitKey(rateLimit.type());
        String requestId = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();

        Long result = stringRedisTemplate.execute(
                rateLimitScript,
                Collections.singletonList(key),
                String.valueOf(window),
                String.valueOf(max),
                String.valueOf(now),
                requestId
        );

        if (result == null || result == 0) {
            log.warn("Rate limit exceeded: {}", key);
            throw new BusinessException(429, "请求过于频繁，请稍后重试");
        }

        log.debug("Rate limit check passed, remaining: {}, key: {}", result, key);
        return point.proceed();
    }

    private String getLimitKey(RateLimit.LimitType type) {
        switch (type) {
            case IP:
                String ip = getClientIp();
                return ip != null ? ip : "unknown";
            case USER:
                String userId = httpServletRequest.getHeader("X-User-Id");
                return StringUtils.isNotBlank(userId) ? userId : "anonymous";
            default:
                return "global";
        }
    }

    private String getClientIp() {
        String ip = httpServletRequest.getHeader("X-Forwarded-For");
        if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = httpServletRequest.getHeader("Proxy-Client-IP");
        }
        if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = httpServletRequest.getHeader("WL-Proxy-Client-IP");
        }
        if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = httpServletRequest.getHeader("X-Real-IP");
        }
        if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = httpServletRequest.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
