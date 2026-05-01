package com.chatbi.interceptor;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 限流服务
 *
 * 基于 Caffeine 滑动窗口计数器实现本地限流，
 * 后续可替换为 Redis 实现分布式限流。
 */
@Slf4j
@Component
public class RateLimitService {

    // 登录限流：key = "login:" + ip + ":" + username
    private final Cache<String, AtomicInteger> loginBuckets = Caffeine.newBuilder()
        .expireAfterWrite(60, TimeUnit.SECONDS)
        .build();

    // 查询限流：key = "query:" + userId
    private final Cache<String, AtomicInteger> queryBuckets = Caffeine.newBuilder()
        .expireAfterWrite(60, TimeUnit.SECONDS)
        .build();

    private static final int LOGIN_MAX_REQUESTS = 5;
    private static final int QUERY_MAX_REQUESTS = 100;

    /**
     * 尝试消费登录请求令牌
     */
    public boolean tryConsumeLogin(String ip, String username) {
        String key = "login:" + ip + ":" + (username != null ? username : "anonymous");
        AtomicInteger counter = loginBuckets.get(key, k -> new AtomicInteger(0));
        int count = counter.incrementAndGet();
        if (count > LOGIN_MAX_REQUESTS) {
            log.warn("登录限流触发 - key: {}, count: {}", key, count);
            return false;
        }
        return true;
    }

    /**
     * 尝试消费查询请求令牌
     */
    public boolean tryConsumeQuery(String userId) {
        String key = "query:" + (userId != null ? userId : "anonymous");
        AtomicInteger counter = queryBuckets.get(key, k -> new AtomicInteger(0));
        int count = counter.incrementAndGet();
        if (count > QUERY_MAX_REQUESTS) {
            log.warn("查询限流触发 - key: {}, count: {}", key, count);
            return false;
        }
        return true;
    }
}
