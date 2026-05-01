package com.chatbi.config;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 分布式链路追踪配置
 *
 * 启用 @Observed 注解支持，自动为带注解的方法创建 span。
 */
@Slf4j
@Configuration
public class TracingConfig {

    @Bean
    public ObservedAspect observedAspect(ObservationRegistry observationRegistry) {
        log.info("ObservedAspect 已注册 - @Observed 注解生效");
        return new ObservedAspect(observationRegistry);
    }
}
