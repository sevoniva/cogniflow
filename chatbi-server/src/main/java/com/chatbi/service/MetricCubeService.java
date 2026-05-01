package com.chatbi.service;

import com.chatbi.entity.Metric;
import com.chatbi.repository.MetricMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MetricCube 查询服务（Headless BI）
 *
 * 将用户的自然语言问题映射到预定义的指标立方体，
 * 自动生成统一口径的 SQL，避免直接查原始表。
 *
 * 改造说明（Month 3 Week 1）：
 * - 指标层抽象：cubeSql 模板 + 维度/度量定义
 * - 时间表达自动解析：本月/上月/最近7天/本季度等
 * - 维度自动匹配：按地区/按产品/按月份等
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetricCubeService {

    private final MetricMapper metricMapper;
    private final ObjectMapper objectMapper;

    private static final Pattern TIME_PATTERN = Pattern.compile(
            "(本月|上月|本季度|上季度|今年|去年|最近\\s*(\\d+)\\s*天|最近\\s*(\\d+)\\s*周|最近\\s*(\\d+)\\s*个月|最近\\s*(\\d+)\\s*年)"
    );

    /**
     * 尝试将用户问题解析为 MetricCube 查询
     *
     * @param question 用户自然语言
     * @return 若命中指标，返回生成的 SQL；否则返回 null（让 LLM 处理）
     */
    public String tryResolve(String question) {
        if (question == null || question.isBlank()) {
            return null;
        }

        // 1. 识别问题中的指标名称
        List<Metric> metrics = metricMapper.selectList(null);
        if (metrics == null || metrics.isEmpty()) {
            return null;
        }

        Metric matchedMetric = null;
        String normalizedQuestion = question.toLowerCase();
        for (Metric metric : metrics) {
            if (!"active".equals(metric.getStatus())) {
                continue;
            }
            String metricName = metric.getName();
            if (metricName != null && normalizedQuestion.contains(metricName.toLowerCase())) {
                matchedMetric = metric;
                break;
            }
        }

        if (matchedMetric == null) {
            return null;
        }

        // 2. 若指标配置了 cubeSql，使用模板引擎生成 SQL
        if (matchedMetric.getCubeSql() != null && !matchedMetric.getCubeSql().isBlank()) {
            return buildFromCubeSql(matchedMetric, question);
        }

        // 3. Fallback：使用 measures + dimensions 自动生成简单 SQL
        return buildFromMeasures(matchedMetric, question);
    }

    private String buildFromCubeSql(Metric metric, String question) {
        String sql = metric.getCubeSql();

        // 解析时间维度
        TimeFilter timeFilter = parseTimeFilter(question);
        sql = sql.replace("{{timeFilter}}", timeFilter.sql());
        sql = sql.replace("{{startDate}}", timeFilter.startDate());
        sql = sql.replace("{{endDate}}", timeFilter.endDate());

        // 解析分组维度
        String dimensionField = parseDimensionField(question, metric);
        String quotedDimension = dimensionField.isBlank() ? "" : "`" + dimensionField + "`";
        sql = sql.replace("{{dimension}}", quotedDimension);
        sql = sql.replace("{{dimensionAlias}}", quotedDimension.isBlank() ? "" : ", " + quotedDimension);

        // 解析聚合函数
        String aggregation = metric.getAggregation() != null ? metric.getAggregation() : "SUM";
        sql = sql.replace("{{aggregation}}", aggregation);
        sql = sql.replace("{{measureField}}", metric.getColumnName() != null ? metric.getColumnName() : "");

        log.info("MetricCube SQL 生成 - metric: {}, sql: {}", metric.getName(), sql);
        return sql;
    }

    private String buildFromMeasures(Metric metric, String question) {
        String aggregation = metric.getAggregation() != null ? metric.getAggregation() : "SUM";
        String measureField = metric.getColumnName() != null ? metric.getColumnName() : "";
        String tableName = metric.getTableName() != null ? metric.getTableName() : "";

        TimeFilter timeFilter = parseTimeFilter(question);
        String dimensionField = parseDimensionField(question, metric);

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        if (!dimensionField.isBlank()) {
            sql.append("`").append(dimensionField).append("`, ");
        }
        sql.append(aggregation).append("(`").append(measureField).append("`) AS `").append(metric.getName()).append("_result`");
        sql.append(" FROM `").append(tableName).append("`");
        if (!timeFilter.sql().isBlank()) {
            sql.append(" WHERE ").append(timeFilter.sql());
        }
        if (!dimensionField.isBlank()) {
            sql.append(" GROUP BY `").append(dimensionField).append("`");
        }
        sql.append(" LIMIT 100");

        log.info("MetricCube 自动生成 SQL - metric: {}, sql: {}", metric.getName(), sql);
        return sql.toString();
    }

    /**
     * 解析问题中的时间表达
     */
    TimeFilter parseTimeFilter(String question) {
        Matcher matcher = TIME_PATTERN.matcher(question);
        if (!matcher.find()) {
            return new TimeFilter("", "", "");
        }

        String timeExpr = matcher.group(1);
        return switch (timeExpr) {
            case "本月" -> new TimeFilter(
                    "CAST(`created_at` AS DATE) >= DATE_TRUNC('MONTH', CURRENT_DATE) AND CAST(`created_at` AS DATE) < DATEADD('MONTH', 1, DATE_TRUNC('MONTH', CURRENT_DATE))",
                    "DATE_TRUNC('MONTH', CURRENT_DATE)",
                    "DATEADD('MONTH', 1, DATE_TRUNC('MONTH', CURRENT_DATE))"
            );
            case "上月" -> new TimeFilter(
                    "CAST(`created_at` AS DATE) >= DATEADD('MONTH', -1, DATE_TRUNC('MONTH', CURRENT_DATE)) AND CAST(`created_at` AS DATE) < DATE_TRUNC('MONTH', CURRENT_DATE)",
                    "DATEADD('MONTH', -1, DATE_TRUNC('MONTH', CURRENT_DATE))",
                    "DATE_TRUNC('MONTH', CURRENT_DATE)"
            );
            case "本季度" -> new TimeFilter(
                    "CAST(`created_at` AS DATE) >= DATE_TRUNC('QUARTER', CURRENT_DATE) AND CAST(`created_at` AS DATE) < DATEADD('QUARTER', 1, DATE_TRUNC('QUARTER', CURRENT_DATE))",
                    "DATE_TRUNC('QUARTER', CURRENT_DATE)",
                    "DATEADD('QUARTER', 1, DATE_TRUNC('QUARTER', CURRENT_DATE))"
            );
            case "今年" -> new TimeFilter(
                    "YEAR(`created_at`) = YEAR(CURRENT_DATE)",
                    "DATE_TRUNC('YEAR', CURRENT_DATE)",
                    "DATEADD('YEAR', 1, DATE_TRUNC('YEAR', CURRENT_DATE))"
            );
            default -> {
                // 处理 "最近 N 天/周/月/年"
                if (timeExpr.contains("天")) {
                    int days = extractNumber(timeExpr);
                    yield new TimeFilter(
                            "CAST(`created_at` AS DATE) >= DATEADD('DAY', -" + days + ", CURRENT_DATE)",
                            "DATEADD('DAY', -" + days + ", CURRENT_DATE)",
                            "CURRENT_DATE"
                    );
                }
                if (timeExpr.contains("周")) {
                    int weeks = extractNumber(timeExpr);
                    yield new TimeFilter(
                            "CAST(`created_at` AS DATE) >= DATEADD('WEEK', -" + weeks + ", CURRENT_DATE)",
                            "DATEADD('WEEK', -" + weeks + ", CURRENT_DATE)",
                            "CURRENT_DATE"
                    );
                }
                if (timeExpr.contains("个月")) {
                    int months = extractNumber(timeExpr);
                    yield new TimeFilter(
                            "CAST(`created_at` AS DATE) >= DATEADD('MONTH', -" + months + ", CURRENT_DATE)",
                            "DATEADD('MONTH', -" + months + ", CURRENT_DATE)",
                            "CURRENT_DATE"
                    );
                }
                yield new TimeFilter("", "", "");
            }
        };
    }

    private String parseDimensionField(String question, Metric metric) {
        if (metric.getDimensions() == null || metric.getDimensions().isBlank()) {
            return "";
        }
        try {
            List<Map<String, String>> dims = objectMapper.readValue(metric.getDimensions(), new TypeReference<>() {});
            String normalized = question.toLowerCase();
            for (Map<String, String> dim : dims) {
                String dimName = dim.get("name");
                if (dimName != null && normalized.contains(dimName.toLowerCase())) {
                    return dim.getOrDefault("field", "");
                }
            }
        } catch (Exception e) {
            log.warn("维度解析失败 - metric: {}", metric.getName(), e);
        }
        return "";
    }

    private int extractNumber(String text) {
        Matcher m = Pattern.compile("\\d+").matcher(text);
        return m.find() ? Integer.parseInt(m.group()) : 7;
    }

    public record TimeFilter(String sql, String startDate, String endDate) {}
}
