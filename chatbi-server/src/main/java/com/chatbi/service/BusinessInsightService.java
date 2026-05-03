package com.chatbi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatbi.entity.Metric;
import com.chatbi.repository.MetricMapper;
import com.chatbi.support.SqlDialectHelper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 数据洞察服务（通用化改造后）
 *
 * 不再硬编码任何业务指标 SQL，所有查询均基于用户在管理后台配置的指标定义动态生成。
 * 指标配置通过 Metric 实体的 cubeSql / measures / dimensions / aggregation 等字段驱动。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessInsightService {

    private final JdbcTemplate jdbcTemplate;
    private final SqlDialectHelper sqlDialectHelper;
    private final MetricCubeService metricCubeService;
    private final MetricMapper metricMapper;

    /**
     * 基于指标配置执行查询
     *
     * @param metric    指标配置
     * @param queryText 用户原始查询文本（用于解析时间维度、分组维度）
     * @return 查询计划（包含生成的 SQL 和查询结果）
     */
    public QueryPlan queryMetric(Metric metric, String queryText) {
        if (metric == null) {
            throw new IllegalArgumentException("指标不能为空");
        }

        String sql;
        if (metric.getCubeSql() != null && !metric.getCubeSql().isBlank()) {
            sql = metricCubeService.buildFromCubeSql(metric, queryText != null ? queryText : "");
        } else {
            sql = metricCubeService.buildFromMeasures(metric, queryText != null ? queryText : "");
        }

        log.info("指标查询执行 - metric: {}, sql: {}", metric.getName(), sql);
        List<Map<String, Object>> data = jdbcTemplate.queryForList(sql);
        return new QueryPlan(sql, "指标", data);
    }

    /**
     * 获取概览数据 - 基于用户配置的 active 指标动态聚合
     *
     * 对每个 active 指标生成无维度、无时间过滤的简单聚合 SQL，取聚合值作为概览。
     * 如果指标未配置数据口径（tableName + columnName + aggregation），则跳过。
     * 不返回任何 mock 数据，全部基于真实数据库查询。
     */
    public List<Map<String, Object>> getOverviewRows() {
        List<Metric> activeMetrics = metricMapper.selectList(
                new LambdaQueryWrapper<Metric>().eq(Metric::getStatus, "active")
        );

        if (activeMetrics == null || activeMetrics.isEmpty()) {
            log.info("当前无 active 指标，概览返回空列表");
            return List.of();
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Metric metric : activeMetrics.stream().limit(6).toList()) {
            try {
                Map<String, Object> row = queryOverviewForMetric(metric);
                if (row != null) {
                    rows.add(row);
                }
            } catch (Exception e) {
                log.warn("概览指标查询失败 - metric: {}, reason: {}", metric.getName(), e.getMessage());
            }
        }
        return rows;
    }

    /**
     * 查询单个指标的概览值
     */
    private Map<String, Object> queryOverviewForMetric(Metric metric) {
        String tableName = metric.getTableName();
        String columnName = metric.getColumnName();
        String aggregation = metric.getAggregation();

        // 如果没有基础配置，尝试用 cubeSql 生成概览
        if ((tableName == null || tableName.isBlank() || columnName == null || columnName.isBlank())
                && (metric.getCubeSql() == null || metric.getCubeSql().isBlank())) {
            log.debug("指标未配置数据口径，跳过概览 - metric: {}", metric.getName());
            return null;
        }

        String sql;
        if (metric.getCubeSql() != null && !metric.getCubeSql().isBlank()) {
            // 使用 cubeSql 模板，去掉维度变量，生成纯聚合
            sql = metric.getCubeSql()
                    .replace("{{timeFilter}}", "1 = 1")
                    .replace("{{startDate}}", "")
                    .replace("{{endDate}}", "")
                    .replace("{{dimension}}", "")
                    .replace("{{dimensionAlias}}", "")
                    .replace("{{aggregation}}", aggregation != null ? aggregation : "SUM")
                    .replace("{{measureField}}", columnName != null ? columnName : "");
            // 清理多余逗号和空格
            sql = sql.replaceAll("\\s+,", "").replaceAll(",\\s+", ", ").trim();
        } else {
            String agg = aggregation != null && !aggregation.isBlank() ? aggregation : "SUM";
            sql = String.format("SELECT %s(`%s`) AS `value` FROM `%s` LIMIT 1",
                    agg, columnName, tableName);
        }

        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);
        if (result.isEmpty()) {
            return null;
        }

        Object value = result.get(0).values().stream().findFirst().orElse(null);
        return overviewRow(metric.getName(), normalizeOverviewValue(value), "");
    }

    /**
     * 获取图表数据（通用化）
     *
     * 不再通过 chartType 硬编码映射，而是根据指标名称直接查询。
     */
    public List<Map<String, Object>> getChartData(String metricName, String dimension) {
        if (metricName == null || metricName.isBlank()) {
            return List.of();
        }

        LambdaQueryWrapper<Metric> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Metric::getName, metricName).eq(Metric::getStatus, "active");
        Metric metric = metricMapper.selectOne(wrapper);

        if (metric == null) {
            log.warn("未找到指标配置 - metricName: {}", metricName);
            return List.of();
        }

        QueryPlan plan = queryMetric(metric, "按" + (dimension != null ? dimension : ""));
        return plan.getData();
    }

    private Map<String, Object> overviewRow(String metric, Object value, String unit) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("指标", metric);
        row.put("数值", value);
        row.put("单位", unit);
        return row;
    }

    private Number normalizeOverviewValue(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            if (number instanceof Double || number instanceof Float || value.toString().contains(".")) {
                return BigDecimal.valueOf(number.doubleValue()).setScale(1, RoundingMode.HALF_UP).doubleValue();
            }
            return number.longValue();
        }
        if (value instanceof String text) {
            String normalized = text.replace("%", "").replace(",", "").trim();
            if (normalized.isBlank()) {
                return 0;
            }
            return normalized.contains(".")
                    ? BigDecimal.valueOf(Double.parseDouble(normalized)).setScale(1, RoundingMode.HALF_UP).doubleValue()
                    : Long.parseLong(normalized);
        }
        return 0;
    }

    @Data
    @AllArgsConstructor
    public static class QueryPlan {
        private String sql;
        private String dimension;
        private List<Map<String, Object>> data;
    }
}
