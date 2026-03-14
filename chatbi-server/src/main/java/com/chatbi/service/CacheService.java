package com.chatbi.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Redis 缓存服务
 */
@Service
public class CacheService {

    private final Map<String, LocalValue> localCache = new ConcurrentHashMap<>();

    private RedisTemplate<String, Object> redisTemplate;
    @Value("${app.redis.enabled:false}")
    private boolean redisEnabled = false;

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

    /**
     * 设置缓存
     */
    public void set(String key, Object value) {
        localCache.put(key, new LocalValue(value, null));
        if (redisEnabled && redisTemplate != null) {
            redisTemplate.opsForValue().set(key, value);
        }
    }

    /**
     * 设置缓存（带过期时间）
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        localCache.put(key, new LocalValue(value, System.currentTimeMillis() + unit.toMillis(timeout)));
        if (redisEnabled && redisTemplate != null) {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
        }
    }

    /**
     * 获取缓存
     */
    public Object get(String key) {
        if (redisEnabled && redisTemplate != null) {
            try {
                return redisTemplate.opsForValue().get(key);
            } catch (Exception ignored) {
            }
        }
        return getLocalValue(key);
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
     * 删除缓存
     */
    public void delete(String key) {
        localCache.remove(key);
        if (redisEnabled && redisTemplate != null) {
            redisTemplate.delete(key);
        }
    }

    /**
     * 判断缓存是否存在
     */
    public Boolean hasKey(String key) {
        if (redisEnabled && redisTemplate != null) {
            return redisTemplate.hasKey(key);
        }
        return getLocalValue(key) != null;
    }

    /**
     * 设置过期时间
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        LocalValue localValue = localCache.get(key);
        if (localValue != null) {
            localCache.put(key, new LocalValue(localValue.value(), System.currentTimeMillis() + unit.toMillis(timeout)));
        }
        if (redisEnabled && redisTemplate != null) {
            return redisTemplate.expire(key, timeout, unit);
        }
        return localValue != null;
    }

    /**
     * 获取过期时间（秒）
     */
    public Long getExpire(String key) {
        if (redisEnabled && redisTemplate != null) {
            return redisTemplate.getExpire(key);
        }
        LocalValue localValue = localCache.get(key);
        if (localValue == null || localValue.expiresAt() == null) {
            return -1L;
        }
        return Math.max(0L, (localValue.expiresAt() - System.currentTimeMillis()) / 1000);
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
            return redisTemplate.opsForValue().increment(key, delta);
        }
        long value = ((Number) getLocalValueOrDefault(key, 0L)).longValue() + delta;
        localCache.put(key, new LocalValue(value, null));
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
        if (redisEnabled && redisTemplate != null) {
            return redisTemplate.opsForValue().decrement(key, delta);
        }
        long value = ((Number) getLocalValueOrDefault(key, 0L)).longValue() - delta;
        localCache.put(key, new LocalValue(value, null));
        return value;
    }

    private Object getLocalValue(String key) {
        LocalValue localValue = localCache.get(key);
        if (localValue == null) {
            return null;
        }
        if (localValue.expiresAt() != null && localValue.expiresAt() < System.currentTimeMillis()) {
            localCache.remove(key);
            return null;
        }
        return localValue.value();
    }

    private Object getLocalValueOrDefault(String key, Object defaultValue) {
        Object value = getLocalValue(key);
        return value == null ? defaultValue : value;
    }

    private record LocalValue(Object value, Long expiresAt) {
    }
}
