package com.chatbi.service.rag;

import com.chatbi.entity.Metric;
import com.chatbi.repository.MetricMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 向量数据同步服务
 *
 * 改造说明（Month 2 Week 3）：
 * - 支持全量同步 + 增量同步（基于 updatedAt）
 * - 同步状态追踪（lastSyncTime, syncCount, documentCount）
 * - XXL-JOB 定时调用
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingSyncService {

    private final EmbeddingService embeddingService;
    private final MetricMapper metricMapper;

    private final AtomicReference<LocalDateTime> lastSyncTime = new AtomicReference<>();
    private final AtomicInteger totalSyncCount = new AtomicInteger(0);

    /**
     * 全量同步所有业务元数据到向量存储
     */
    public SyncResult syncAll() {
        log.info("开始全量同步业务元数据到向量存储...");
        long start = System.currentTimeMillis();
        embeddingService.clear();

        int metricsCount = syncMetrics(null);

        LocalDateTime now = LocalDateTime.now();
        lastSyncTime.set(now);
        totalSyncCount.incrementAndGet();

        log.info("全量同步完成 - metrics: {}, 总文档数: {}, 耗时: {}ms",
                metricsCount, embeddingService.size(), System.currentTimeMillis() - start);
        return new SyncResult(true, metricsCount, 0, embeddingService.size(), now, "全量同步成功");
    }

    /**
     * 增量同步：只同步自上次同步以来变更的数据
     */
    public SyncResult syncIncremental() {
        LocalDateTime since = lastSyncTime.get();
        if (since == null) {
            log.info("首次同步，执行全量同步");
            return syncAll();
        }

        log.info("开始增量同步 - since: {}", since);
        long start = System.currentTimeMillis();

        int metricsCount = syncMetrics(since);
        int deletedCount = cleanupDeletedMetrics();

        LocalDateTime now = LocalDateTime.now();
        lastSyncTime.set(now);
        totalSyncCount.incrementAndGet();

        log.info("增量同步完成 - metrics: {}, deleted: {}, 总文档数: {}, 耗时: {}ms",
                metricsCount, deletedCount, embeddingService.size(), System.currentTimeMillis() - start);
        return new SyncResult(true, metricsCount, deletedCount, embeddingService.size(), now, "增量同步成功");
    }

    /**
     * 同步指标定义
     *
     * @param since 若为 null 则同步全部，否则只同步 updatedAt >= since 的指标
     */
    public int syncMetrics(LocalDateTime since) {
        LambdaQueryWrapper<Metric> wrapper = new LambdaQueryWrapper<>();
        if (since != null) {
            wrapper.ge(Metric::getUpdatedAt, since);
        }
        List<Metric> metrics = metricMapper.selectList(wrapper);
        if (metrics == null || metrics.isEmpty()) {
            log.warn("无可同步的指标数据");
            return 0;
        }

        Map<String, String> documents = new HashMap<>();
        for (Metric metric : metrics) {
            String key = "metric:" + metric.getId();
            String text = String.format("指标名称: %s, 编码: %s, 定义: %s, 数据类型: %s, 数据表: %s, 字段: %s",
                    metric.getName(),
                    metric.getCode() != null ? metric.getCode() : "",
                    metric.getDefinition() != null ? metric.getDefinition() : "",
                    metric.getDataType() != null ? metric.getDataType() : "",
                    metric.getTableName() != null ? metric.getTableName() : "",
                    metric.getColumnName() != null ? metric.getColumnName() : "");
            documents.put(key, text);
        }

        embeddingService.storeAll(documents);
        log.info("指标同步完成 - 数量: {}", documents.size());
        return documents.size();
    }

    /**
     * 清理已被删除的指标（数据库中不存在但向量库中存在的 metric key）
     */
    private int cleanupDeletedMetrics() {
        Set<String> keys = embeddingService.keys();
        List<Long> existingIds = metricMapper.selectList(null).stream()
                .map(Metric::getId)
                .toList();

        int deleted = 0;
        for (String key : keys) {
            if (key.startsWith("metric:")) {
                Long id = Long.parseLong(key.substring("metric:".length()));
                if (!existingIds.contains(id)) {
                    embeddingService.remove(key);
                    deleted++;
                }
            }
        }
        if (deleted > 0) {
            log.info("清理已删除指标向量 - 数量: {}", deleted);
        }
        return deleted;
    }

    /**
     * 同步单条历史查询记录（成功查询）
     */
    public void syncHistoricalQuery(String question, String sql, String tableName) {
        String key = "query:" + System.currentTimeMillis();
        String text = String.format("问题: %s, SQL: %s, 表: %s", question, sql, tableName);
        embeddingService.store(key, text);
        log.debug("历史查询同步成功 - key: {}", key);
    }

    /**
     * 获取同步状态
     */
    public SyncStatus getStatus() {
        return new SyncStatus(
                lastSyncTime.get(),
                totalSyncCount.get(),
                embeddingService.size()
        );
    }

    public record SyncResult(boolean success, int syncedCount, int deletedCount,
                             int totalDocuments, LocalDateTime syncTime, String message) {}

    public record SyncStatus(LocalDateTime lastSyncTime, int totalSyncCount, int documentCount) {}
}
