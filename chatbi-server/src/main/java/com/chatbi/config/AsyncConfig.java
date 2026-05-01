package com.chatbi.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务配置
 *
 * 为审计日志、通知推送等提供统一的异步线程池。
 * 替代手动的 new Thread()，实现可监控、可配置的异步执行。
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    public static final String AUDIT_LOG_EXECUTOR = "auditLogExecutor";

    @Bean(name = AUDIT_LOG_EXECUTOR)
    public Executor auditLogExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("audit-log-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        log.info("审计日志异步线程池初始化完成 - core: 2, max: 10, queue: 500");
        return executor;
    }
}
