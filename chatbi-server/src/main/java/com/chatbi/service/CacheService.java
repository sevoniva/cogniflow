package com.chatbi.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * 缓存服务 - 多级缓存实现（Caffeine L1 + Redis L2）
 *
 * 改造说明：
 * - 原实现使用无限增长的 ConcurrentHashMap，无容量限制，存在内存泄漏风险
 * - 新实现使用 Caffeine 本地缓存（W-TinyLFU 驱逐策略）+ Redis 分布式缓存
 * - 保留原有 API 兼容性，调用方无需修改
 * - 推荐逐步迁移到 @Cacheable / @CacheEvict 注解驱动
 */
@Slf4j
@Service
public class CacheService {

    private Cache<String, Object> localCache;

    private RedisTemplate<String, Object> redisTemplate;

    @Value("${app.redis.enabled:false}")
    private boolean redisEnabled = false;

    @Value("${app.cache.local.max-size:1000}")
    private long localMaxSize;

    @Value("${app.cache.local.expire-minutes:10}")
    private long localExpireMinutes;

    @Autowired
    public CacheService() {
    }

    CacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Autowired(required = false)
    void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void init() {
        this.localCache = Caffeine.newBuilder()
                .maximumSize(localMaxSize)
                .expireAfterWrite(localExpireMinutes, TimeUnit.MINUTES)
                .recordStats()
                .removalListener((key, value, cause) ->
                        log.debug("Caffeine 缓存驱逐 - key: {}, cause: {}", key, cause))
                .build();
        log.info("CacheService 初始化完成 - localMaxSize: {}, localExpireMinutes: {}, redisEnabled: {}",
                localMaxSize, localExpireMinutes, redisEnabled);
    }

    /**
     * 设置缓存（本地 + Redis）
     */
    public void set(String key, Object value) {
        localCache.put(key, value);
        if (redisEnabled && redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(key, value);
            } catch (Exception e) {
                log.warn("Redis 缓存写入失败 - key: {}", key, e);
            }
        }
    }

    /**
     * 设置缓存（带过期时间）
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        localCache.put(key, value);
        if (redisEnabled && redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(key, value, timeout, unit);
            } catch (Exception e) {
                log.warn("Redis 缓存写入失败 - key: {}", key, e);
            }
        }
    }

    /**
     * 获取缓存（先本地，再 Redis）
     */
    public Object get(String key) {
        // 1. 先查本地缓存
        Object localValue = localCache.getIfPresent(key);
        if (localValue != null) {
            return localValue;
        }

        // 2. 本地未命中，查 Redis
        if (redisEnabled && redisTemplate != null) {
            try {
                Object redisValue = redisTemplate.opsForValue().get(key);
                if (redisValue != null) {
                    // 回填本地缓存
                    localCache.put(key, redisValue);
                    return redisValue;
                }
            } catch (Exception e) {
                log.warn("Redis 缓存读取失败 - key: {}", key, e);
            }
        }

        return null;
    }

    /**
     * 获取缓存并转换为指定类型
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object value = get(key);
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * 删除缓存（本地 + Redis）
     */
    public void delete(String key) {
        localCache.invalidate(key);
        if (redisEnabled && redisTemplate != null) {
            try {
                redisTemplate.delete(key);
            } catch (Exception e) {
                log.warn("Redis 缓存删除失败 - key: {}", key, e);
            }
        }
    }

    /**
     * 判断缓存是否存在
     */
    public Boolean hasKey(String key) {
        Object localValue = localCache.getIfPresent(key);
        if (localValue != null) {
            return true;
        }
        if (redisEnabled && redisTemplate != null) {
            try {
                return redisTemplate.hasKey(key);
            } catch (Exception e) {
                log.warn("Redis 缓存查询失败 - key: {}", key, e);
            }
        }
        return false;
    }

    /**
     * 设置过期时间
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        // Caffeine 不支持单独设置过期时间，需要重新 put
        Object value = localCache.getIfPresent(key);
        if (value != null) {
            localCache.put(key, value);
        }

        if (redisEnabled && redisTemplate != null) {
            try {
                return redisTemplate.expire(key, timeout, unit);
            } catch (Exception e) {
                log.warn("Redis 设置过期时间失败 - key: {}", key, e);
            }
        }
        return value != null;
    }

    /**
     * 获取过期时间（秒）
     */
    public Long getExpire(String key) {
        if (redisEnabled && redisTemplate != null) {
            try {
                return redisTemplate.getExpire(key);
            } catch (Exception e) {
                log.warn("Redis 获取过期时间失败 - key: {}", key, e);
            }
        }
        // Caffeine 不支持精确获取剩余过期时间
        Object value = localCache.getIfPresent(key);
        return value != null ? -1L : -2L;
    }

    /**
     * 自增
     */
    public Long increment(String key) {
        return increment(key, 1);
    }

    /**
     * 自增（指定步长）
     */
    public Long increment(String key, long delta) {
        if (redisEnabled && redisTemplate != null) {
            try {
                Long result = redisTemplate.opsForValue().increment(key, delta);
                if (result != null) {
                    localCache.put(key, result);
                }
                return result;
            } catch (Exception e) {
                log.warn("Redis 自增失败 - key: {}", key, e);
            }
        }
        // 本地缓存自增（降级）
        Object current = localCache.getIfPresent(key);
        long value = (current instanceof Number ? ((Number) current).longValue() : 0L) + delta;
        localCache.put(key, value);
        return value;
    }

    /**
     * 自减
     */
    public Long decrement(String key) {
        return decrement(key, 1);
    }

    /**
     * 自减（指定步长）
     */
    public Long decrement(String key, long delta) {
        return increment(key, -delta);
    }

    /**
     * 获取本地缓存统计信息
     */
    public com.github.benmanes.caffeine.cache.stats.CacheStats getLocalStats() {
        return localCache.stats();
    }

    /**
     * 清空本地缓存
     */
    public void clearLocal() {
        localCache.invalidateAll();
        log.info("本地缓存已清空");
    }
}
