package com.chatbi.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 多级缓存配置：Caffeine（本地L1）+ Redis（远程L2）
 *
 * 改造说明：
 * - 替换原 CacheService 中无限增长的 ConcurrentHashMap
 * - Caffeine 提供 W-TinyLFU 驱逐策略、容量限制、过期策略
 * - Redis 提供分布式缓存能力
 * - 支持 @Cacheable / @CacheEvict / @CachePut 注解驱动
 */
@Slf4j
@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${app.cache.local.max-size:1000}")
    private long localMaxSize;

    @Value("${app.cache.local.expire-minutes:10}")
    private long localExpireMinutes;

    @Value("${app.cache.redis.expire-minutes:30}")
    private long redisExpireMinutes;

    /**
     * Caffeine 本地缓存管理器（L1）
     */
    @Bean("caffeineCacheManager")
    @Primary
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(localMaxSize)
                .expireAfterWrite(Duration.ofMinutes(localExpireMinutes))
                .recordStats()  // 开启统计，便于监控
                .removalListener((key, value, cause) ->
                    log.debug("Caffeine 缓存驱逐 - key: {}, cause: {}", key, cause))
        );
        return manager;
    }

    /**
     * Redis 缓存管理器（L2）
     */
    @Bean("redisCacheManager")
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(redisExpireMinutes))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(jsonSerializer))
                .disableCachingNullValues();

        // 为不同缓存名称配置不同的过期时间
        Map<String, RedisCacheConfiguration> configMap = new HashMap<>();
        configMap.put("metrics", defaultConfig.entryTtl(Duration.ofMinutes(60)));
        configMap.put("queryResult", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        configMap.put("user", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        configMap.put("chartCatalog", defaultConfig.entryTtl(Duration.ofMinutes(120)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(configMap)
                .transactionAware()
                .build();
    }

    /**
     * 缓存名称常量
     */
    public static final class CacheNames {
        public static final String METRICS = "metrics";
        public static final String QUERY_RESULT = "queryResult";
        public static final String USER = "user";
        public static final String CHART_CATALOG = "chartCatalog";
        public static final String CONVERSATION = "conversation";
        public static final String DATA_SOURCE = "dataSource";
        public static final String PERMISSION = "permission";
        public static final String AI_PROVIDER = "aiProvider";

        private CacheNames() {}
    }
}
