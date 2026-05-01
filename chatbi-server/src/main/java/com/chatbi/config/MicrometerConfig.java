package com.chatbi.config;

import com.chatbi.service.QueryExecutionService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Gauge;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Micrometer 自定义指标注册配置
 *
 * 注册业务级 Gauge 指标，暴露给 Prometheus / Actuator。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class MicrometerConfig {

    private final MeterRegistry meterRegistry;
    private final QueryExecutionService queryExecutionService;

    @PostConstruct
    public void registerGauges() {
        Gauge.builder("datasource.pool.cache.size",
                        () -> queryExecutionService.getDataSourceCacheSize())
                .description("数据源连接池缓存条目数")
                .register(meterRegistry);

        Gauge.builder("datasource.pool.cache.hit.rate",
                        () -> queryExecutionService.getPoolStats().hitRate())
                .description("数据源连接池缓存命中率")
                .register(meterRegistry);

        Gauge.builder("datasource.pool.cache.miss.count",
                        () -> queryExecutionService.getPoolStats().missCount())
                .description("数据源连接池缓存未命中次数")
                .register(meterRegistry);

        log.info("Micrometer 自定义 Gauge 指标注册完成");
    }
}
