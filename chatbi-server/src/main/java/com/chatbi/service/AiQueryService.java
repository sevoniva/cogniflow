package com.chatbi.service;

import com.chatbi.config.AiConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 智能查询服务 - 基于 LLM 的 Text-to-SQL
 *
 * 改造说明：
 * - LLM SQL 生成委托给 SqlGenerationService（使用 LangChain4j PromptTemplate）
 * - 保留规则引擎作为离线兜底方案
 * - 保留原有接口兼容性
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiQueryService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AiConfig aiConfig;
    private final SqlGenerationService sqlGenerationService;
    private final QueryGovernanceService queryGovernanceService;

    /**
     * 表结构信息
     */
    public static class TableSchema {
        private String tableName;
        private List<Column> columns;
        private String description;

        public static class Column {
            private String name;
            private String type;
            private String description;
            private boolean primaryKey;

            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
            public String getType() { return type; }
            public void setType(String type) { this.type = type; }
            public String getDescription() { return description; }
            public void setDescription(String description) { this.description = description; }
            public boolean isPrimaryKey() { return primaryKey; }
            public void setPrimaryKey(boolean primaryKey) { this.primaryKey = primaryKey; }
        }

        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
        public List<Column> getColumns() { return columns; }
        public void setColumns(List<Column> columns) { this.columns = columns; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    /**
     * 使用 LLM 生成 SQL
     */
    public String generateSqlWithLLM(String naturalLanguage, List<TableSchema> schemas) {
        try {
            // 检查AI是否启用
            if (!aiConfig.isEnabled()) {
                throw new RuntimeException("AI功能未启用");
            }

            // 获取当前配置的提供商
            AiConfig.ProviderConfig provider = aiConfig.getCurrentProvider();
            log.info("使用AI提供商: {}, 模型: {}", provider.getName(), provider.getModel());

            // 使用 LangChain4j PromptTemplate 生成 SQL
            return sqlGenerationService.generateSql(naturalLanguage, schemas, provider.getName());

        } catch (Exception e) {
            log.error("LLM 生成 SQL 失败：{}", e.getMessage());
            // 降级到规则引擎
            return generateSqlWithRules(naturalLanguage, schemas);
        }
    }

    /**
     * 规则引擎降级方案
     */
    public String generateSqlWithRules(String question, List<TableSchema> schemas) {
        if (schemas == null || schemas.isEmpty()) {
            throw new RuntimeException("没有可用的表结构");
        }

        TableSchema schema = schemas.get(0);
        StringBuilder sql = new StringBuilder("SELECT ");
        LocalDate today = LocalDate.now();

        String lowerQuestion = question.toLowerCase();

        // 检测聚合函数
        boolean hasAggregation = false;
        String aggFunction = null;
        String aggColumn = null;

        if (lowerQuestion.contains("总和") || lowerQuestion.contains("合计") || lowerQuestion.contains("求和")) {
            aggFunction = "SUM";
            hasAggregation = true;
        } else if (lowerQuestion.contains("平均") || lowerQuestion.contains("平均值")) {
            aggFunction = "AVG";
            hasAggregation = true;
        } else if (lowerQuestion.contains("数量") || lowerQuestion.contains("个数") || lowerQuestion.contains("count")) {
            aggFunction = "COUNT";
            hasAggregation = true;
        } else if (lowerQuestion.contains("最大") || lowerQuestion.contains("最高")) {
            aggFunction = "MAX";
            hasAggregation = true;
        } else if (lowerQuestion.contains("最小") || lowerQuestion.contains("最低")) {
            aggFunction = "MIN";
            hasAggregation = true;
        }

        // 查找聚合字段
        if (hasAggregation) {
            for (TableSchema.Column col : schema.getColumns()) {
                if (lowerQuestion.contains(col.getName().toLowerCase()) &&
                    (col.getType().contains("INT") || col.getType().contains("DECIMAL") ||
                     col.getType().contains("DOUBLE") || col.getType().contains("FLOAT") ||
                     col.getType().contains("BIGINT") || col.getType().contains("NUMERIC"))) {
                    aggColumn = col.getName();
                    break;
                }
            }
            if (aggColumn == null) {
                aggColumn = findNumericColumn(schema);
            }
            if (aggColumn == null) {
                aggColumn = "*";
            }
            sql.append(aggFunction).append("(`").append(aggColumn).append("`) AS result");
        } else {
            sql.append("*");
        }

        sql.append(" FROM `").append(schema.getTableName()).append("`");

        // WHERE 条件
        List<String> conditions = new ArrayList<>();

        // 时间条件
        String timeColumn = findTimeColumn(schema);
        if (timeColumn != null) {
            addTimeConditions(lowerQuestion, today, conditions, timeColumn);
        }

        // 检测分组
        String groupByColumn = findGroupByColumn(lowerQuestion, schema);
        if (groupByColumn != null && !hasAggregation) {
            sql = new StringBuilder("SELECT `" + groupByColumn + "`, COUNT(*) AS count FROM `" + schema.getTableName() + "`");
            hasAggregation = true;
        }

        // 添加 WHERE 条件
        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }

        // 添加 GROUP BY
        if (groupByColumn != null) {
            sql.append(" GROUP BY `").append(groupByColumn).append("`");
        }

        // 检测排序
        if (lowerQuestion.contains("降序") || lowerQuestion.contains("从高到低") || lowerQuestion.contains("最高")) {
            sql.append(" ORDER BY result DESC");
        } else if (lowerQuestion.contains("升序") || lowerQuestion.contains("从低到高") || lowerQuestion.contains("最低")) {
            sql.append(" ORDER BY result ASC");
        }

        // 检测 LIMIT
        java.util.regex.Pattern limitPattern = java.util.regex.Pattern.compile("(前|top)(\\d+)");
        java.util.regex.Matcher limitMatcher = limitPattern.matcher(lowerQuestion);
        if (limitMatcher.find()) {
            sql.append(" LIMIT ").append(limitMatcher.group(2));
        } else {
            sql.append(" LIMIT 100");
        }

        log.info("规则引擎生成SQL：{}", sql);
        return sql.toString();
    }

    private void addTimeConditions(String question, LocalDate today, List<String> conditions, String timeColumn) {
        if (question.contains("今天") || question.contains("今日")) {
            conditions.add(buildDateCondition(timeColumn, today, today));
        } else if (question.contains("昨天")) {
            LocalDate yesterday = today.minusDays(1);
            conditions.add(buildDateCondition(timeColumn, yesterday, yesterday));
        } else if (question.contains("本周") || question.contains("这周")) {
            LocalDate weekStart = today.with(DayOfWeek.MONDAY);
            conditions.add(buildDateCondition(timeColumn, weekStart, weekStart.plusDays(6)));
        } else if (question.contains("上周")) {
            LocalDate currentWeekStart = today.with(DayOfWeek.MONDAY);
            LocalDate lastWeekStart = currentWeekStart.minusWeeks(1);
            conditions.add(buildDateCondition(timeColumn, lastWeekStart, lastWeekStart.plusDays(6)));
        } else if (question.contains("本月") || question.contains("这个月")) {
            LocalDate monthStart = today.withDayOfMonth(1);
            conditions.add(buildDateCondition(timeColumn, monthStart, monthStart.plusMonths(1).minusDays(1)));
        } else if (question.contains("上月") || question.contains("上个月")) {
            LocalDate monthStart = today.withDayOfMonth(1).minusMonths(1);
            conditions.add(buildDateCondition(timeColumn, monthStart, monthStart.plusMonths(1).minusDays(1)));
        } else if (question.contains("本年") || question.contains("今年")) {
            LocalDate yearStart = today.withDayOfYear(1);
            conditions.add(buildDateCondition(timeColumn, yearStart, yearStart.plusYears(1).minusDays(1)));
        } else if (question.contains("去年")) {
            LocalDate yearStart = today.withDayOfYear(1).minusYears(1);
            conditions.add(buildDateCondition(timeColumn, yearStart, yearStart.plusYears(1).minusDays(1)));
        } else if (question.contains("最近")) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("最近(\\d+)天");
            java.util.regex.Matcher matcher = pattern.matcher(question);
            if (matcher.find()) {
                int days = Integer.parseInt(matcher.group(1));
                conditions.add(buildDateCondition(timeColumn, today.minusDays(Math.max(days - 1, 0)), today));
            }
        }
    }

    private String buildDateCondition(String timeColumn, LocalDate startDate, LocalDate endDate) {
        return "CAST(`" + timeColumn + "` AS DATE) BETWEEN DATE '" + startDate + "' AND DATE '" + endDate + "'";
    }

    /**
     * 查找时间字段（改造：不再硬编码 created_at）
     */
    private String findTimeColumn(TableSchema schema) {
        if (schema.getColumns() == null) return null;
        // 优先查找名为 created_at / updated_at / create_time / update_time 的字段
        for (TableSchema.Column col : schema.getColumns()) {
            String name = col.getName().toLowerCase();
            if (name.equals("created_at") || name.equals("create_time") ||
                name.equals("updated_at") || name.equals("update_time") ||
                name.equals("date") || name.equals("time") || name.endsWith("_date") || name.endsWith("_time")) {
                if (col.getType().toUpperCase().contains("DATE") || col.getType().toUpperCase().contains("TIME") ||
                    col.getType().toUpperCase().contains("TIMESTAMP")) {
                    return col.getName();
                }
            }
        }
        return null;
    }

    /**
     * 查找数值字段
     */
    private String findNumericColumn(TableSchema schema) {
        if (schema.getColumns() == null) return null;
        for (TableSchema.Column col : schema.getColumns()) {
            String type = col.getType().toUpperCase();
            if (type.contains("INT") || type.contains("DECIMAL") || type.contains("DOUBLE") ||
                type.contains("FLOAT") || type.contains("BIGINT") || type.contains("NUMERIC")) {
                return col.getName();
            }
        }
        return null;
    }

    /**
     * 查找分组字段
     */
    private String findGroupByColumn(String question, TableSchema schema) {
        if (schema.getColumns() == null) return null;
        for (TableSchema.Column col : schema.getColumns()) {
            String colName = col.getName().toLowerCase();
            if (question.contains("按" + colName) || question.contains("分组" + colName) ||
                question.contains(colName + "分组")) {
                return col.getName();
            }
        }
        return null;
    }

    /**
     * 解析 SQL 为结构化查询对象
     */
    public Map<String, Object> parseSql(String sql) {
        Map<String, Object> result = new HashMap<>();
        result.put("sql", sql);
        result.put("success", true);

        String upperSql = sql.trim().toUpperCase();
        if (upperSql.startsWith("SELECT")) {
            result.put("queryType", "SELECT");
        } else if (upperSql.startsWith("INSERT")) {
            result.put("queryType", "INSERT");
        } else if (upperSql.startsWith("UPDATE")) {
            result.put("queryType", "UPDATE");
        } else if (upperSql.startsWith("DELETE")) {
            result.put("queryType", "DELETE");
        }

        return result;
    }

    /**
     * 验证 SQL 安全性
     */
    public QueryGovernanceService.ValidationResult validateSqlDetail(String sql) {
        return queryGovernanceService.validate(sql);
    }

    public boolean validateSql(String sql) {
        return validateSqlDetail(sql).valid();
    }


}
