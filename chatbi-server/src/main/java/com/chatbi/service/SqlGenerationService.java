package com.chatbi.service;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SQL 生成服务
 *
 * 使用 LangChain4j 的 PromptTemplate 管理 SQL 生成 Prompt，
 * 支持模板化、变量注入、RAG 增强（预留向量检索接口）。
 *
 * 改造说明：
 * - 替换原 AiQueryService 中硬编码的 StringBuilder Prompt
 * - Prompt 模板可配置、可扩展
 * - 支持 Few-shot 示例动态加载（预留）
 * - 支持业务术语映射注入（预留 RAG）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SqlGenerationService {

    private final ChatbiLanguageModel chatbiLanguageModel;

    /**
     * SQL 生成 Prompt 模板
     */
    private static final String SQL_GENERATION_TEMPLATE = """
        你是一个专业的SQL专家。请根据用户的自然语言问题和数据库表结构，生成准确的SQL查询语句。

        ## 数据库表结构

        {{tableSchema}}

        ## 业务术语映射

        {{businessTerms}}

        ## 历史成功查询示例

        {{historicalQueries}}

        ## 用户问题

        {{question}}

        ## 生成要求

        1. **只输出SQL语句**，不要包含任何解释或注释
        2. 使用反引号包裹表名和字段名（如 `table_name`、`column_name`）
        3. 使用UTF-8字符集
        4. 默认查询前100条结果（除非用户明确指定数量）
        5. 对于时间查询，优先使用 `created_at` 字段
        6. 对于聚合查询，使用适当的 GROUP BY 子句
        7. 确保SQL语法正确，可以直接在 H2 / ANSI SQL 环境执行
        8. 对于复杂查询，可以使用子查询、JOIN等高级特性
        9. 注意SQL注入防护，不要直接拼接用户输入

        现在请为上述用户问题生成SQL：
        """;

    /**
     * 默认的 Few-shot 示例
     */
    private static final String DEFAULT_FEW_SHOT_EXAMPLES = """
        问题：查询本月销售额总和
        SQL：SELECT SUM(`amount`) AS total_amount FROM `sales` WHERE CAST(`created_at` AS DATE) BETWEEN DATE '2026-03-01' AND DATE '2026-03-31' LIMIT 100

        问题：按地区统计销售额，显示前10名
        SQL：SELECT `region`, SUM(`amount`) AS total_amount FROM `sales` GROUP BY `region` ORDER BY total_amount DESC LIMIT 10
        """;

    /**
     * 生成 SQL
     *
     * @param question 用户自然语言问题
     * @param schemas  表结构信息
     * @return 生成的 SQL
     */
    public String generateSql(String question, List<AiQueryService.TableSchema> schemas) {
        String tableSchemaText = buildTableSchemaText(schemas);
        String businessTerms = buildBusinessTerms();      // 预留：RAG 注入业务术语
        String historicalQueries = buildHistoricalQueries(); // 预留：RAG 注入历史查询

        PromptTemplate promptTemplate = PromptTemplate.from(SQL_GENERATION_TEMPLATE);
        Map<String, Object> variables = new HashMap<>();
        variables.put("tableSchema", tableSchemaText);
        variables.put("businessTerms", businessTerms);
        variables.put("historicalQueries", historicalQueries);
        variables.put("question", question);

        Prompt prompt = promptTemplate.apply(variables);
        String promptText = prompt.text();

        log.info("SQL 生成 Prompt - 长度: {}, 表数量: {}", promptText.length(), schemas.size());

        String response = chatbiLanguageModel.generate(promptText);
        return extractSql(response);
    }

    /**
     * 生成 SQL（使用指定 provider）
     */
    public String generateSql(String question, List<AiQueryService.TableSchema> schemas, String provider) {
        String tableSchemaText = buildTableSchemaText(schemas);
        String businessTerms = buildBusinessTerms();
        String historicalQueries = buildHistoricalQueries();

        PromptTemplate promptTemplate = PromptTemplate.from(SQL_GENERATION_TEMPLATE);
        Map<String, Object> variables = new HashMap<>();
        variables.put("tableSchema", tableSchemaText);
        variables.put("businessTerms", businessTerms);
        variables.put("historicalQueries", historicalQueries);
        variables.put("question", question);

        Prompt prompt = promptTemplate.apply(variables);
        String promptText = prompt.text();

        log.info("SQL 生成 Prompt - 长度: {}, 表数量: {}, provider: {}", promptText.length(), schemas.size(), provider);

        String response = chatbiLanguageModel.generate(promptText, provider);
        return extractSql(response);
    }

    /**
     * 构建表结构文本
     */
    private String buildTableSchemaText(List<AiQueryService.TableSchema> schemas) {
        if (schemas == null || schemas.isEmpty()) {
            return "无表结构信息";
        }

        StringBuilder sb = new StringBuilder();
        for (AiQueryService.TableSchema schema : schemas) {
            sb.append("### 表名：`").append(schema.getTableName()).append("`");
            if (schema.getDescription() != null && !schema.getDescription().isBlank()) {
                sb.append(" - ").append(schema.getDescription());
            }
            sb.append("\n\n字段列表：\n");

            if (schema.getColumns() != null) {
                for (AiQueryService.TableSchema.Column col : schema.getColumns()) {
                    sb.append("- `").append(col.getName()).append("` (").append(col.getType()).append(")");
                    if (col.isPrimaryKey()) {
                        sb.append(" **[主键]**");
                    }
                    if (col.getDescription() != null && !col.getDescription().isBlank()) {
                        sb.append(" - ").append(col.getDescription());
                    }
                    sb.append("\n");
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * 构建业务术语映射（预留 RAG 接口）
     *
     * TODO: 接入向量检索，动态注入业务术语映射
     * 例如："GMV" -> "order_amount", "华东区" -> "region='east'"
     */
    private String buildBusinessTerms() {
        // 预留：从 EmbeddingStore 检索相关术语
        return "暂无业务术语映射";
    }

    /**
     * 构建历史成功查询示例（预留 RAG 接口）
     *
     * TODO: 接入向量检索，动态加载相似问题的成功 SQL 示例
     */
    private String buildHistoricalQueries() {
        // 预留：从 EmbeddingStore 检索相似查询的历史 SQL
        return DEFAULT_FEW_SHOT_EXAMPLES;
    }

    /**
     * 从响应中提取 SQL
     */
    private String extractSql(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }

        // 去除 markdown 代码块标记
        content = content.replaceAll("(?s)```sql\\s*", "");
        content = content.replaceAll("(?s)```\\s*", "");
        content = content.trim();

        // 如果包含多个语句，只取第一个
        int semicolonIndex = content.indexOf(';');
        if (semicolonIndex > 0) {
            content = content.substring(0, semicolonIndex + 1);
        }

        return normalizeSqlDialect(content);
    }

    /**
     * SQL 方言归一化
     */
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
}
