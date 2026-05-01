package com.chatbi.service.rag;

import com.chatbi.entity.Metric;
import com.chatbi.repository.MetricMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 向量数据同步服务
 *
 * 将业务元数据（指标、表结构）同步到向量存储，
 * 为 NL2SQL RAG 提供语义检索的语料库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingSyncService {

    private final EmbeddingService embeddingService;
    private final MetricMapper metricMapper;

    /**
     * 全量同步所有业务元数据到向量存储
     */
    public void syncAll() {
        log.info("开始全量同步业务元数据到向量存储...");
        embeddingService.clear();

        syncMetrics();
        // TODO: 表结构同步需要连接数据源获取 schema，暂时跳过

        log.info("全量同步完成，当前文档数: {}", embeddingService.size());
    }

    /**
     * 同步指标定义
     */
    public void syncMetrics() {
        List<Metric> metrics = metricMapper.selectList(null);
        if (metrics == null || metrics.isEmpty()) {
            log.warn("无可同步的指标数据");
            return;
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
}
