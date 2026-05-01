package com.chatbi.service;

import com.chatbi.entity.PromptVersion;
import com.chatbi.service.rag.EmbeddingService;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * SQL 生成服务
 *
 * 改造说明（Month 2 Week 1）：
 * - Prompt 模板从数据库动态加载（PromptVersionService），支持运行时切换
 * - 新增 userId 参数支持 A/B 灰度测试（userId % 100 < grayScalePercent）
 * - 无数据库配置时自动 fallback 到静态模板，保证兼容性
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SqlGenerationService {

    private final ChatbiLanguageModel chatbiLanguageModel;
    private final EmbeddingService embeddingService;
    private final PromptVersionService promptVersionService;

    /**
     * 默认静态 Prompt 模板（fallback）
     */
    private static final String DEFAULT_TEMPLATE = """
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

    private static final String DEFAULT_FEW_SHOT_EXAMPLES = """
        问题：查询本月销售额总和
        SQL：SELECT SUM(`amount`) AS total_amount FROM `sales` WHERE CAST(`created_at` AS DATE) BETWEEN DATE '2026-03-01' AND DATE '2026-03-31' LIMIT 100

        问题：按地区统计销售额，显示前10名
        SQL：SELECT `region`, SUM(`amount`) AS total_amount FROM `sales` GROUP BY `region` ORDER BY total_amount DESC LIMIT 10
        """;

    /**
     * 生成 SQL（自动根据 userId 灰度选择 Prompt 版本）
     */
    public String generateSql(String question, List<AiQueryService.TableSchema> schemas, Long userId) {
        return generateSql(question, schemas, userId, null);
    }

    /**
     * 生成 SQL（指定 provider + userId 灰度）
     */
    public String generateSql(String question, List<AiQueryService.TableSchema> schemas, Long userId, String provider) {
        String template = resolveTemplate(userId);
        String tableSchemaText = buildTableSchemaText(schemas);
        String businessTerms = buildBusinessTerms(question);
        String historicalQueries = buildHistoricalQueries(question);

        PromptTemplate promptTemplate = PromptTemplate.from(template);
        Map<String, Object> variables = new HashMap<>();
        variables.put("tableSchema", tableSchemaText);
        variables.put("businessTerms", businessTerms);
        variables.put("historicalQueries", historicalQueries);
        variables.put("question", question);

        Prompt prompt = promptTemplate.apply(variables);
        String promptText = prompt.text();

        log.info("SQL 生成 Prompt - 长度: {}, 表数量: {}, userId: {}, provider: {}",
                promptText.length(), schemas.size(), userId, provider);

        String response = provider != null && !provider.isBlank()
                ? chatbiLanguageModel.generate(promptText, provider)
                : chatbiLanguageModel.generate(promptText);
        return extractSql(response);
    }

    /**
     * 解析当前用户应使用的 Prompt 模板
     *
     * 策略：
     * 1. 如果 userId 不为空，尝试按灰度规则匹配
     * 2. 否则使用最新的 active Prompt
     * 3. 数据库无配置时 fallback 到 DEFAULT_TEMPLATE
     */
    private String resolveTemplate(Long userId) {
        Optional<PromptVersion> versionOpt;
        if (userId != null) {
            versionOpt = promptVersionService.resolveForUser(userId);
        } else {
            versionOpt = promptVersionService.getLatestActive();
        }

        if (versionOpt.isPresent()) {
            PromptVersion version = versionOpt.get();
            log.debug("使用数据库 Prompt 版本 - tag: {}, status: {}, gray: {}%",
                    version.getVersionTag(), version.getStatus(), version.getGrayScalePercent());
            return version.getTemplate();
        }

        log.debug("无数据库 Prompt 配置，使用默认静态模板");
        return DEFAULT_TEMPLATE;
    }

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

    private String buildBusinessTerms(String question) {
        try {
            List<EmbeddingService.SearchResult> results = embeddingService.search(question, 3);
            if (results.isEmpty()) {
                return "暂无业务术语映射";
            }
            return results.stream()
                    .map(r -> "- " + r.text() + " (相关度: " + String.format("%.2f", r.similarity()) + ")")
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            log.warn("业务术语向量检索失败，fallback 到默认值", e);
            return "暂无业务术语映射";
        }
    }

    private String buildHistoricalQueries(String question) {
        try {
            List<EmbeddingService.SearchResult> results = embeddingService.search(question + " SQL示例", 3);
            if (results.isEmpty()) {
                return DEFAULT_FEW_SHOT_EXAMPLES;
            }
            return results.stream()
                    .map(r -> "- 问题: " + r.text())
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            log.warn("历史查询向量检索失败，fallback 到默认示例", e);
            return DEFAULT_FEW_SHOT_EXAMPLES;
        }
    }

    private String extractSql(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        content = content.replaceAll("(?s)```sql\\s*", "");
        content = content.replaceAll("(?s)```\\s*", "");
        content = content.trim();
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
}
