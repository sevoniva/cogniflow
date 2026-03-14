package com.chatbi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 查询缓存服务
 * 优先使用 Redis；无 Redis 时自动降级为进程内缓存。
 */
@Slf4j
@Service
public class QueryCacheService {

    private static final String CACHE_PREFIX = "chatbi:query:cache:";
    private static final long DEFAULT_CACHE_TTL = 30;

    private final ObjectMapper objectMapper;
    private final Map<String, LocalCacheEntry> localCache = new ConcurrentHashMap<>();

    private StringRedisTemplate redisTemplate;
    @Value("${app.redis.enabled:false}")
    private boolean redisEnabled = false;

    @Autowired
    public QueryCacheService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    QueryCacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
    }

    @Autowired(required = false)
    void setRedisTemplate(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public List<Map<String, Object>> getCachedResult(String sql, Long dataSourceId) {
        return getCachedResult(sql, dataSourceId, "global");
    }

    public List<Map<String, Object>> getCachedResult(String sql, Long dataSourceId, String scopeKey) {
        String cacheKey = generateCacheKey(sql, dataSourceId, scopeKey);

        List<Map<String, Object>> redisResult = getFromRedis(cacheKey);
        if (redisResult != null) {
            return redisResult;
        }

        return getFromLocalCache(cacheKey);
    }

    public void cacheResult(String sql, Long dataSourceId, List<Map<String, Object>> result) {
        cacheResult(sql, dataSourceId, "global", result, DEFAULT_CACHE_TTL);
    }

    public void cacheResult(String sql, Long dataSourceId, List<Map<String, Object>> result, long ttlMinutes) {
        cacheResult(sql, dataSourceId, "global", result, ttlMinutes);
    }

    public void cacheResult(String sql, Long dataSourceId, String scopeKey, List<Map<String, Object>> result, long ttlMinutes) {
        String cacheKey = generateCacheKey(sql, dataSourceId, scopeKey);
        cacheToLocal(cacheKey, result, ttlMinutes);

        if (!redisEnabled || redisTemplate == null) {
            return;
        }

        try {
            String resultJson = objectMapper.writeValueAsString(result);
            redisTemplate.opsForValue().set(cacheKey, resultJson, ttlMinutes, TimeUnit.MINUTES);
            log.info("缓存查询结果 - key: {}, ttl: {}分钟, 结果数: {}", cacheKey, ttlMinutes, result.size());
        } catch (Exception e) {
            log.warn("Redis 缓存写入失败，已降级为本地缓存 - key: {}", cacheKey, e);
        }
    }

    public void invalidateCache(String sql, Long dataSourceId) {
        invalidateCache(sql, dataSourceId, "global");
    }

    public void invalidateCache(String sql, Long dataSourceId, String scopeKey) {
        String cacheKey = generateCacheKey(sql, dataSourceId, scopeKey);
        localCache.remove(cacheKey);

        if (!redisEnabled || redisTemplate == null) {
            return;
        }

        try {
            redisTemplate.delete(cacheKey);
        } catch (Exception e) {
            log.warn("Redis 缓存删除失败 - key: {}", cacheKey, e);
        }
    }

    public void invalidateDataSourceCache(Long dataSourceId) {
        String prefix = CACHE_PREFIX + dataSourceId + ":";
        localCache.keySet().removeIf(key -> key.startsWith(prefix));

        if (!redisEnabled || redisTemplate == null) {
            return;
        }

        try {
            var keys = redisTemplate.keys(prefix + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("Redis 数据源缓存清理失败 - dataSourceId: {}", dataSourceId, e);
        }
    }

    public void invalidateAllCache() {
        localCache.clear();

        if (!redisEnabled || redisTemplate == null) {
            return;
        }

        try {
            var keys = redisTemplate.keys(CACHE_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("Redis 全量缓存清理失败", e);
        }
    }

    public Map<String, Object> getCacheStats() {
        cleanupExpiredLocalCache();
        long localCount = localCache.size();

        if (!redisEnabled || redisTemplate == null) {
            return Map.of(
                "backend", "local",
                "totalCachedQueries", localCount,
                "cachePrefix", CACHE_PREFIX,
                "defaultTtlMinutes", DEFAULT_CACHE_TTL
            );
        }

        try {
            var keys = redisTemplate.keys(CACHE_PREFIX + "*");
            long redisCount = keys == null ? 0 : keys.size();
            return Map.of(
                "backend", "redis+local",
                "redisCachedQueries", redisCount,
                "localCachedQueries", localCount,
                "cachePrefix", CACHE_PREFIX,
                "defaultTtlMinutes", DEFAULT_CACHE_TTL
            );
        } catch (Exception e) {
            log.warn("Redis 缓存统计失败，返回本地缓存统计", e);
            return Map.of(
                "backend", "local-fallback",
                "totalCachedQueries", localCount,
                "cachePrefix", CACHE_PREFIX,
                "defaultTtlMinutes", DEFAULT_CACHE_TTL
            );
        }
    }

    private List<Map<String, Object>> getFromRedis(String cacheKey) {
        if (!redisEnabled || redisTemplate == null) {
            return null;
        }

        try {
            String cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (cachedJson == null) {
                return null;
            }

            log.info("命中 Redis 查询缓存 - key: {}", cacheKey);
            return objectMapper.readValue(
                cachedJson,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class)
            );
        } catch (Exception e) {
            log.warn("读取 Redis 查询缓存失败，尝试本地缓存 - key: {}", cacheKey, e);
            return null;
        }
    }

    private List<Map<String, Object>> getFromLocalCache(String cacheKey) {
        cleanupExpiredLocalCache();

        LocalCacheEntry entry = localCache.get(cacheKey);
        if (entry == null) {
            return null;
        }

        log.debug("命中本地查询缓存 - key: {}", cacheKey);
        return entry.result();
    }

    private void cacheToLocal(String cacheKey, List<Map<String, Object>> result, long ttlMinutes) {
        long expiresAt = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(ttlMinutes);
        localCache.put(cacheKey, new LocalCacheEntry(result, expiresAt));
    }

    private void cleanupExpiredLocalCache() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, LocalCacheEntry>> iterator = localCache.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, LocalCacheEntry> entry = iterator.next();
            if (entry.getValue().expiresAt() <= now) {
                iterator.remove();
            }
        }
    }

    private String generateCacheKey(String sql, Long dataSourceId, String scopeKey) {
        String sqlHash = hashSql(sql);
        String safeScopeKey = scopeKey == null || scopeKey.isBlank() ? "global" : scopeKey;
        return CACHE_PREFIX + dataSourceId + ":" + safeScopeKey + ":" + sqlHash;
    }

    private String hashSql(String sql) {
        try {
            String normalizedSql = sql.trim().replaceAll("\\s+", " ").toLowerCase();
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(normalizedSql.getBytes());

            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("SQL 哈希失败", e);
            return String.valueOf(sql.hashCode());
        }
    }

    public boolean shouldCache(String sql) {
        String lowerSql = sql.toLowerCase();

        if (lowerSql.contains("now()") || lowerSql.contains("current_timestamp")) {
            return false;
        }
        if (lowerSql.contains("rand()") || lowerSql.contains("random()")) {
            return false;
        }
        if (lowerSql.contains("insert") || lowerSql.contains("update") ||
            lowerSql.contains("delete") || lowerSql.contains("create") ||
            lowerSql.contains("drop") || lowerSql.contains("alter")) {
            return false;
        }

        return true;
    }

    public long decideCacheTtl(String sql) {
        String lowerSql = sql.toLowerCase();

        if (lowerSql.contains("group by") || lowerSql.contains("count(") ||
            lowerSql.contains("sum(") || lowerSql.contains("avg(")) {
            return 60;
        }

        if (lowerSql.contains("where") &&
            (lowerSql.contains("< now()") || lowerSql.contains("< current_date"))) {
            return 120;
        }

        return DEFAULT_CACHE_TTL;
    }

    private record LocalCacheEntry(List<Map<String, Object>> result, long expiresAt) {
    }
}
