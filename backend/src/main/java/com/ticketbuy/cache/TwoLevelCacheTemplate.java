package com.ticketbuy.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.benmanes.caffeine.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

@Component
public class TwoLevelCacheTemplate {
    private static final Logger log = LoggerFactory.getLogger(TwoLevelCacheTemplate.class);
    private static final String REDIS_KEY_PREFIX = "cache:l2:";
    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.registerModule(new JavaTimeModule());
    }

    @Autowired
    private Cache<Object, Object> caffeineCache;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public <T> T get(String key, Class<T> clazz, long logicalExpireSeconds, java.util.function.Supplier<T> loader) {
        Object l1Value = caffeineCache.getIfPresent(key);
        if (l1Value != null) {
            if (l1Value instanceof CacheData) {
                CacheData<?> cacheData = (CacheData<?>) l1Value;
                if (!cacheData.isExpired()) {
                    log.debug("L1 cache hit: {}", key);
                    return (T) cacheData.getData();
                }
            } else if (!"NULL".equals(l1Value)) {
                log.debug("L1 cache hit: {}", key);
                return (T) l1Value;
            }
        }

        String redisKey = REDIS_KEY_PREFIX + key;
        String redisValue = stringRedisTemplate.opsForValue().get(redisKey);
        if (redisValue != null) {
            log.debug("L2 cache hit: {}", key);
            try {
                CacheData<T> cacheData = mapper.readValue(redisValue, mapper.getTypeFactory().constructParametricType(CacheData.class, clazz));
                if (cacheData != null && !cacheData.isExpired()) {
                    caffeineCache.put(key, cacheData);
                    return cacheData.getData();
                }
            } catch (Exception e) {
                log.warn("Failed to parse cache data: {}", e.getMessage());
            }
        }

        if (redisValue != null && redisValue.contains("\"data\":null")) {
            log.debug("Cache penetration, null value: {}", key);
            return null;
        }

        T data = loader.get();

        if (data == null) {
            stringRedisTemplate.opsForValue().set(redisKey, "{\"data\":null,\"expireTime\":0}", 120, TimeUnit.SECONDS);
            log.debug("Cache penetration stored null: {}", key);
            return null;
        }

        CacheData<T> newCacheData = new CacheData<>(data, logicalExpireSeconds);
        caffeineCache.put(key, newCacheData);
        try {
            stringRedisTemplate.opsForValue().set(redisKey, mapper.writeValueAsString(newCacheData), logicalExpireSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Failed to serialize cache data: {}", e.getMessage());
        }
        log.debug("Cache miss, loaded and cached: {}", key);

        return data;
    }

    public void evict(String key) {
        caffeineCache.invalidate(key);
        stringRedisTemplate.delete(REDIS_KEY_PREFIX + key);
        log.debug("Cache evicted: {}", key);
    }

    public void put(String key, Object value, long ttlSeconds) {
        CacheData<Object> cacheData = new CacheData<>(value, ttlSeconds);
        caffeineCache.put(key, cacheData);
        try {
            stringRedisTemplate.opsForValue().set(REDIS_KEY_PREFIX + key, mapper.writeValueAsString(cacheData), ttlSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Failed to serialize cache data: {}", e.getMessage());
        }
    }

    public static class CacheData<T> {
        private T data;
        private long expireTime;

        public CacheData() {}

        public CacheData(T data, long ttlSeconds) {
            this.data = data;
            this.expireTime = System.currentTimeMillis() + ttlSeconds * 1000;
        }

        public T getData() { return data; }
        public void setData(T data) { this.data = data; }
        public long getExpireTime() { return expireTime; }
        public void setExpireTime(long expireTime) { this.expireTime = expireTime; }

        public boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }
    }
}
