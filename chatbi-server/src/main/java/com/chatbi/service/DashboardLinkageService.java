package com.chatbi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.*;

/**
 * 仪表板联动过滤服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardLinkageService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final QueryExecutionService queryExecutionService;
    private final DataSource dataSource;

    /**
     * 联动过滤配置
     */
    public static class LinkageConfig {
        private Long sourceChartId;
        private Long targetChartId;
        private String sourceField;
        private String targetField;
        private String filterType; // EQUALS, IN, CONTAINS

        public Long getSourceChartId() { return sourceChartId; }
        public void setSourceChartId(Long sourceChartId) { this.sourceChartId = sourceChartId; }
        public Long getTargetChartId() { return targetChartId; }
        public void setTargetChartId(Long targetChartId) { this.targetChartId = targetChartId; }
        public String getSourceField() { return sourceField; }
        public void setSourceField(String sourceField) { this.sourceField = sourceField; }
        public String getTargetField() { return targetField; }
        public void setTargetField(String targetField) { this.targetField = targetField; }
        public String getFilterType() { return filterType; }
        public void setFilterType(String filterType) { this.filterType = filterType; }
    }

    /**
     * 应用联动过滤
     */
    public String applyLinkageFilter(String originalSql, String selectedValue, LinkageConfig config) {
        if (originalSql == null || originalSql.trim().isEmpty()) {
            return originalSql;
        }

        // 解析原始 SQL
        String sql = originalSql.trim();
        String upperSql = sql.toUpperCase();

        // 判断是否有 WHERE 子句
        boolean hasWhere = upperSql.contains(" WHERE ");

        // 构建过滤条件
        String filterCondition = buildFilterCondition(config, selectedValue);

        if (hasWhere) {
            // 在 WHERE 子句后添加条件
            int whereIndex = upperSql.indexOf(" WHERE ");
            String beforeWhere = sql.substring(0, whereIndex + 7);
            String afterWhere = sql.substring(whereIndex + 7);

            // 检查是否已有 ORDER BY、GROUP BY 等
            return insertCondition(beforeWhere, afterWhere, filterCondition);
        } else {
            // 添加 WHERE 子句
            return insertWhereClause(sql, filterCondition);
        }
    }

    /**
     * 构建过滤条件
     */
    private String buildFilterCondition(LinkageConfig config, String selectedValue) {
        String field = config.getTargetField();
        String filterType = config.getFilterType() != null ? config.getFilterType() : "EQUALS";

        return switch (filterType) {
            case "EQUALS" -> field + " = '" + escapeSql(selectedValue) + "'";
            case "IN" -> field + " IN ('" + escapeSql(selectedValue) + "')";
            case "CONTAINS" -> field + " LIKE '%" + escapeSql(selectedValue) + "%'";
            case "NOT_EQUALS" -> field + " != '" + escapeSql(selectedValue) + "'";
            case "GREATER_THAN" -> field + " > '" + escapeSql(selectedValue) + "'";
            case "LESS_THAN" -> field + " < '" + escapeSql(selectedValue) + "'";
            default -> field + " = '" + escapeSql(selectedValue) + "'";
        };
    }

    /**
     * 在 WHERE 子句中插入条件
     */
    private String insertCondition(String beforeWhere, String afterWhere, String condition) {
        // 检查是否有 ORDER BY
        String upperAfter = afterWhere.toUpperCase();
        int orderByIndex = upperAfter.indexOf(" ORDER BY ");
        int groupByIndex = upperAfter.indexOf(" GROUP BY ");
        int limitIndex = upperAfter.indexOf(" LIMIT ");

        int insertPosition = afterWhere.length();
        String suffix = "";

        // 找到最后一个子句的位置
        if (orderByIndex > 0) {
            insertPosition = orderByIndex;
            suffix = afterWhere.substring(orderByIndex);
        } else if (groupByIndex > 0) {
            insertPosition = groupByIndex;
            suffix = afterWhere.substring(groupByIndex);
        } else if (limitIndex > 0) {
            insertPosition = limitIndex;
            suffix = afterWhere.substring(limitIndex);
        }

        return beforeWhere + afterWhere.substring(0, insertPosition).trim() +
               " AND " + condition + " " + suffix;
    }

    /**
     * 插入 WHERE 子句
     */
    private String insertWhereClause(String sql, String condition) {
        String upperSql = sql.toUpperCase();

        // 检查是否有 ORDER BY、GROUP BY 等
        int orderByIndex = upperSql.indexOf(" ORDER BY ");
        int groupByIndex = upperSql.indexOf(" GROUP BY ");
        int limitIndex = upperSql.indexOf(" LIMIT ");

        if (orderByIndex > 0) {
            return sql.substring(0, orderByIndex) + " WHERE " + condition +
                   sql.substring(orderByIndex);
        } else if (groupByIndex > 0) {
            return sql.substring(0, groupByIndex) + " WHERE " + condition +
                   sql.substring(groupByIndex);
        } else if (limitIndex > 0) {
            return sql.substring(0, limitIndex) + " WHERE " + condition +
                   sql.substring(limitIndex);
        } else {
            return sql + " WHERE " + condition;
        }
    }

    /**
     * SQL 转义
     */
    private String escapeSql(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("'", "''");
    }

    /**
     * 获取联动后的数据
     */
    public List<Map<String, Object>> getLinkedData(Long chartId, String selectedValue,
                                                    Map<Long, LinkageConfig> linkageConfigs) {
        // 获取图表的原始查询
        String originalSql = getChartQuery(chartId);

        if (originalSql == null) {
            return Collections.emptyList();
        }

        // 应用所有联动过滤
        String filteredSql = originalSql;
        for (LinkageConfig config : linkageConfigs.values()) {
            if (config.getTargetChartId().equals(chartId)) {
                filteredSql = applyLinkageFilter(filteredSql, selectedValue, config);
            }
        }

        log.info("联动过滤后 SQL: {}", filteredSql);

        // 执行查询
        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            return jdbcTemplate.queryForList(filteredSql);
        } catch (Exception e) {
            log.error("联动查询失败：{}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取图表的查询语句（从配置中解析）
     */
    private String getChartQuery(Long chartId) {
        // TODO: 实际应该从仪表板配置中获取
        // 这里返回示例 SQL
        return "SELECT * FROM orders WHERE 1=1";
    }

    /**
     * 下钻分析
     */
    public Map<String, Object> drillDown(String originalSql, String dimension, String value) {
        // 添加下钻条件
        String drillSql = applyLinkageFilter(originalSql, value, createConfig(dimension));

        // 执行查询
        List<Map<String, Object>> data = executeQuery(drillSql);

        Map<String, Object> result = new HashMap<>();
        result.put("sql", drillSql);
        result.put("data", data);
        result.put("dimension", dimension);
        result.put("value", value);

        return result;
    }

    /**
     * 上卷分析
     */
    public Map<String, Object> rollUp(String originalSql, String dimension) {
        // 移除下钻条件，上卷到更高层级
        String rollupSql = removeDrillCondition(originalSql, dimension);

        // 执行查询
        List<Map<String, Object>> data = executeQuery(rollupSql);

        Map<String, Object> result = new HashMap<>();
        result.put("sql", rollupSql);
        result.put("data", data);
        result.put("dimension", dimension);

        return result;
    }

    /**
     * 执行查询
     */
    private List<Map<String, Object>> executeQuery(String sql) {
        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            return jdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            log.error("查询失败：{}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 创建联动配置
     */
    private LinkageConfig createConfig(String dimension) {
        LinkageConfig config = new LinkageConfig();
        config.setTargetField(dimension);
        config.setFilterType("EQUALS");
        return config;
    }

    /**
     * 移除下钻条件
     */
    private String removeDrillCondition(String sql, String dimension) {
        // 移除指定维度的过滤条件
        // 简化实现，实际应该解析 SQL 并移除对应条件
        return sql.replaceAll("AND\\s+" + dimension + "\\s*=\\s*'[^']*'", "");
    }
}
