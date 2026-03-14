package com.chatbi.service;

import com.chatbi.config.AiConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 智能查询服务 - 基于 LLM 的 Text-to-SQL
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiQueryService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AiConfig aiConfig;
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
        String prompt = buildPrompt(naturalLanguage, schemas);

        try {
            // 检查AI是否启用
            if (!aiConfig.isEnabled()) {
                throw new RuntimeException("AI功能未启用");
            }

            // 获取当前配置的提供商
            AiConfig.ProviderConfig provider = aiConfig.getCurrentProvider();
            log.info("使用AI提供商: {}, 模型: {}", provider.getName(), provider.getModel());

            return callLLM(prompt, provider);

        } catch (Exception e) {
            log.error("LLM 生成 SQL 失败：{}", e.getMessage());
            // 降级到规则引擎
            return generateSqlWithRules(naturalLanguage, schemas);
        }
    }

    /**
     * 构建 Prompt
     */
    private String buildPrompt(String question, List<TableSchema> schemas) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个专业的SQL专家。请根据用户的自然语言问题和数据库表结构，生成准确的SQL查询语句。\n\n");

        sb.append("## 数据库表结构\n\n");
        for (TableSchema schema : schemas) {
            sb.append("### 表名：`").append(schema.getTableName()).append("`");
            if (schema.getDescription() != null) {
                sb.append(" - ").append(schema.getDescription());
            }
            sb.append("\n\n字段列表：\n");

            for (TableSchema.Column col : schema.getColumns()) {
                sb.append("- `").append(col.getName()).append("` (").append(col.getType()).append(")");
                if (col.isPrimaryKey()) {
                    sb.append(" **[主键]**");
                }
                if (col.getDescription() != null) {
                    sb.append(" - ").append(col.getDescription());
                }
                sb.append("\n");
            }
            sb.append("\n");
        }

        sb.append("## 用户问题\n\n");
        sb.append(question).append("\n\n");

        sb.append("## 生成要求\n\n");
        sb.append("1. **只输出SQL语句**，不要包含任何解释或注释\n");
        sb.append("2. 使用反引号包裹表名和字段名（如 `table_name`、`column_name`）\n");
        sb.append("3. 使用UTF-8字符集\n");
        sb.append("4. 默��查询前100条结果（除非用户明确指定数量）\n");
        sb.append("5. 对于时间查询，优先使用 `created_at` 字段\n");
        sb.append("6. 对于聚合查询，使用适当的 GROUP BY 子句\n");
        sb.append("7. 确保SQL语法正确，可以直接在 H2 / ANSI SQL 环境执行\n");
        sb.append("8. 对于复杂查询，可以使用子查询、JOIN等高级特性\n");
        sb.append("9. 注意SQL注入防护，不要直接拼接用户输入\n\n");

        sb.append("## 示例\n\n");
        sb.append("问题：查询本月销售额总和\n");
        sb.append("SQL：SELECT SUM(`amount`) AS total_amount FROM `sales` WHERE CAST(`created_at` AS DATE) BETWEEN DATE '2026-03-01' AND DATE '2026-03-31' LIMIT 100\n\n");

        sb.append("问题：按地区统计销售额，显示前10名\n");
        sb.append("SQL：SELECT `region`, SUM(`amount`) AS total_amount FROM `sales` GROUP BY `region` ORDER BY total_amount DESC LIMIT 10\n\n");

        sb.append("现在请为上述用户问题生成SQL：\n");

        return sb.toString();
    }

    /**
     * 调用 LLM API
     */
    private String callLLM(String prompt, AiConfig.ProviderConfig provider) {
        try {
            // 构建请求
            Map<String, Object> requestBody = new HashMap<>();
            List<Map<String, String>> messages = new ArrayList<>();

            // 添加系统消息
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", "你是一个SQL专家，需要根据用户的问题和数据库表结构生成正确的SQL查询语句。只输出SQL，不要解释。");
            messages.add(systemMessage);

            // 添加用户消息
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.add(userMessage);

            requestBody.put("model", provider.getModel());
            requestBody.put("messages", messages);
            requestBody.put("temperature", provider.getTemperature());
            requestBody.put("max_tokens", provider.getMaxTokens());

            // 使用 OkHttpClient 发送请求
            okhttp3.MediaType JSON = okhttp3.MediaType.parse("application/json; charset=utf-8");
            okhttp3.RequestBody body = okhttp3.RequestBody.create(JSON, objectMapper.writeValueAsString(requestBody));

            okhttp3.OkHttpClient client = new okhttp3.OkHttpClient.Builder()
                    .connectTimeout(aiConfig.getTimeout(), java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(aiConfig.getTimeout(), java.util.concurrent.TimeUnit.SECONDS)
                    .build();

            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(provider.getApiUrl() + "/chat/completions")
                    .addHeader("Authorization", "Bearer " + provider.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            log.info("调用LLM API: {}", provider.getApiUrl());
            okhttp3.Response response = client.newCall(request).execute();

            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "无响应内容";
                log.error("LLM API调用失败 - 状态码: {}, 响应: {}", response.code(), errorBody);
                throw new RuntimeException("LLM API调用失败: " + response.code());
            }

            String responseBody = response.body().string();
            log.debug("LLM响应: {}", responseBody);

            // 解析响应
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            JsonNode choices = jsonNode.get("choices");
            if (choices != null && choices.size() > 0) {
                JsonNode messageNode = choices.get(0).get("message");
                if (messageNode != null) {
                    String content = messageNode.get("content").asText();
                    String sql = extractSql(content);
                    log.info("LLM生成SQL成功: {}", sql);
                    return sql;
                }
            }

            throw new RuntimeException("LLM 响应格式异常");

        } catch (Exception e) {
            log.error("调用 LLM 失败：{}", e.getMessage(), e);
            throw new RuntimeException("调用 LLM 失败：" + e.getMessage(), e);
        }
    }

    /**
     * 从响应中提取 SQL
     */
    private String extractSql(String content) {
        // 去除 markdown 代码块标记
        content = content.replaceAll("```sql\\s*", "");
        content = content.replaceAll("```\\s*", "");
        content = content.trim();

        // 如果包含多个语句，只取第一个
        int semicolonIndex = content.indexOf(';');
        if (semicolonIndex > 0) {
            content = content.substring(0, semicolonIndex + 1);
        }

        return normalizeSqlDialect(content);
    }

    private String normalizeSqlDialect(String sql) {
        String normalized = sql;
        normalized = normalized.replaceAll("(?i)CURRENT_DATE\\s*-\\s*INTERVAL\\s*'([0-9]+)\\s+YEAR(S)?'", "DATEADD('YEAR', -$1, CURRENT_DATE)");
        normalized = normalized.replaceAll("(?i)CURRENT_DATE\\s*-\\s*INTERVAL\\s*'([0-9]+)\\s+MONTH(S)?'", "DATEADD('MONTH', -$1, CURRENT_DATE)");
        normalized = normalized.replaceAll("(?i)CURRENT_DATE\\s*-\\s*INTERVAL\\s*'([0-9]+)\\s+DAY(S)?'", "DATEADD('DAY', -$1, CURRENT_DATE)");
        normalized = normalized.replaceAll("(?i)DATE_SUB\\s*\\(\\s*CURRENT_DATE\\s*,\\s*INTERVAL\\s*([0-9]+)\\s+YEAR(S)?\\s*\\)", "DATEADD('YEAR', -$1, CURRENT_DATE)");
        normalized = normalized.replaceAll("(?i)DATE_SUB\\s*\\(\\s*CURRENT_DATE\\s*,\\s*INTERVAL\\s*([0-9]+)\\s+MONTH(S)?\\s*\\)", "DATEADD('MONTH', -$1, CURRENT_DATE)");
        normalized = normalized.replaceAll("(?i)DATE_SUB\\s*\\(\\s*CURRENT_DATE\\s*,\\s*INTERVAL\\s*([0-9]+)\\s+DAY(S)?\\s*\\)", "DATEADD('DAY', -$1, CURRENT_DATE)");
        return normalized;
    }

    /**
     * 规则引擎降级方案
     */
    private String generateSqlWithRules(String question, List<TableSchema> schemas) {
        if (schemas.isEmpty()) {
            throw new RuntimeException("没有可用的表结构");
        }

        TableSchema schema = schemas.get(0);
        StringBuilder sql = new StringBuilder("SELECT ");
        LocalDate today = LocalDate.now();

        question = question.toLowerCase();

        // 检测聚合函数
        boolean hasAggregation = false;
        String aggFunction = null;
        String aggColumn = null;

        if (question.contains("总和") || question.contains("合计") || question.contains("求和")) {
            aggFunction = "SUM";
            hasAggregation = true;
        } else if (question.contains("平均") || question.contains("平均值")) {
            aggFunction = "AVG";
            hasAggregation = true;
        } else if (question.contains("数量") || question.contains("个数") || question.contains("count")) {
            aggFunction = "COUNT";
            hasAggregation = true;
        } else if (question.contains("最大") || question.contains("最高")) {
            aggFunction = "MAX";
            hasAggregation = true;
        } else if (question.contains("最小") || question.contains("最低")) {
            aggFunction = "MIN";
            hasAggregation = true;
        }

        // 查找聚合字段
        if (hasAggregation) {
            for (TableSchema.Column col : schema.getColumns()) {
                if (question.contains(col.getName().toLowerCase()) &&
                    (col.getType().contains("INT") || col.getType().contains("DECIMAL") ||
                     col.getType().contains("DOUBLE") || col.getType().contains("FLOAT"))) {
                    aggColumn = col.getName();
                    break;
                }
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
        if (question.contains("今天") || question.contains("今日")) {
            conditions.add(buildDateCondition(today, today));
        } else if (question.contains("昨天")) {
            LocalDate yesterday = today.minusDays(1);
            conditions.add(buildDateCondition(yesterday, yesterday));
        } else if (question.contains("本周") || question.contains("这周")) {
            LocalDate weekStart = today.with(DayOfWeek.MONDAY);
            conditions.add(buildDateCondition(weekStart, weekStart.plusDays(6)));
        } else if (question.contains("上周")) {
            LocalDate currentWeekStart = today.with(DayOfWeek.MONDAY);
            LocalDate lastWeekStart = currentWeekStart.minusWeeks(1);
            conditions.add(buildDateCondition(lastWeekStart, lastWeekStart.plusDays(6)));
        } else if (question.contains("本月") || question.contains("这个月")) {
            LocalDate monthStart = today.withDayOfMonth(1);
            conditions.add(buildDateCondition(monthStart, monthStart.plusMonths(1).minusDays(1)));
        } else if (question.contains("上月") || question.contains("上个月")) {
            LocalDate monthStart = today.withDayOfMonth(1).minusMonths(1);
            conditions.add(buildDateCondition(monthStart, monthStart.plusMonths(1).minusDays(1)));
        } else if (question.contains("本年") || question.contains("今年")) {
            LocalDate yearStart = today.withDayOfYear(1);
            conditions.add(buildDateCondition(yearStart, yearStart.plusYears(1).minusDays(1)));
        } else if (question.contains("去年")) {
            LocalDate yearStart = today.withDayOfYear(1).minusYears(1);
            conditions.add(buildDateCondition(yearStart, yearStart.plusYears(1).minusDays(1)));
        } else if (question.contains("最近")) {
            // 提取天数
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("最近(\\d+)天");
            java.util.regex.Matcher matcher = pattern.matcher(question);
            if (matcher.find()) {
                int days = Integer.parseInt(matcher.group(1));
                conditions.add(buildDateCondition(today.minusDays(Math.max(days - 1, 0)), today));
            }
        }

        // 检测分组
        String groupByColumn = null;
        for (TableSchema.Column col : schema.getColumns()) {
            if (question.contains("按" + col.getName()) || question.contains("分组" + col.getName()) ||
                question.contains(col.getName() + "分组")) {
                groupByColumn = col.getName();
                if (!hasAggregation) {
                    sql = new StringBuilder("SELECT `" + groupByColumn + "`, COUNT(*) AS count FROM `" + schema.getTableName() + "`");
                    hasAggregation = true;
                }
                break;
            }
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
        if (question.contains("降序") || question.contains("从高到低") || question.contains("最高")) {
            sql.append(" ORDER BY result DESC");
        } else if (question.contains("升序") || question.contains("从低到高") || question.contains("最低")) {
            sql.append(" ORDER BY result ASC");
        }

        // 检测 LIMIT
        java.util.regex.Pattern limitPattern = java.util.regex.Pattern.compile("(前|top)(\\d+)");
        java.util.regex.Matcher limitMatcher = limitPattern.matcher(question);
        if (limitMatcher.find()) {
            sql.append(" LIMIT ").append(limitMatcher.group(2));
        } else {
            sql.append(" LIMIT 100");
        }

        log.info("规则引擎生成SQL：{}", sql);
        return sql.toString();
    }

    private String buildDateCondition(LocalDate startDate, LocalDate endDate) {
        return "CAST(`created_at` AS DATE) BETWEEN DATE '" + startDate + "' AND DATE '" + endDate + "'";
    }

    /**
     * 解析 SQL 为结构化查询对象
     */
    public Map<String, Object> parseSql(String sql) {
        Map<String, Object> result = new HashMap<>();
        result.put("sql", sql);
        result.put("success", true);

        // 简单解析 SQL 类型
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
