package com.chatbi.interceptor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 限流服务单元测试
 */
class RateLimitServiceTest {

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        rateLimitService = new RateLimitService();
    }

    @Test
    void tryConsumeLogin_shouldAllowWithinLimit() {
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimitService.tryConsumeLogin("127.0.0.1", "admin"),
                "第 " + (i + 1) + " 次登录请求应被允许");
        }
    }

    @Test
    void tryConsumeLogin_shouldBlockOverLimit() {
        for (int i = 0; i < 5; i++) {
            rateLimitService.tryConsumeLogin("127.0.0.1", "admin");
        }
        assertFalse(rateLimitService.tryConsumeLogin("127.0.0.1", "admin"),
            "第 6 次登录请求应被限流");
    }

    @Test
    void tryConsumeQuery_shouldAllowWithinLimit() {
        for (int i = 0; i < 100; i++) {
            assertTrue(rateLimitService.tryConsumeQuery("user_1"),
                "第 " + (i + 1) + " 次查询请求应被允许");
        }
    }

    @Test
    void tryConsumeQuery_shouldBlockOverLimit() {
        for (int i = 0; i < 100; i++) {
            rateLimitService.tryConsumeQuery("user_2");
        }
        assertFalse(rateLimitService.tryConsumeQuery("user_2"),
            "第 101 次查询请求应被限流");
    }

    @Test
    void tryConsumeLogin_shouldIsolateDifferentKeys() {
        for (int i = 0; i < 5; i++) {
            rateLimitService.tryConsumeLogin("127.0.0.1", "admin");
        }
        // 不同 IP + 用户名应独立计数
        assertTrue(rateLimitService.tryConsumeLogin("192.168.1.1", "admin"));
        assertTrue(rateLimitService.tryConsumeLogin("127.0.0.1", "other"));
    }
}
