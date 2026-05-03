package com.chatbi.controller;

import com.chatbi.common.Result;
import com.chatbi.repository.DataSourceMapper;
import com.chatbi.service.AiQueryService;
import com.chatbi.service.QueryExecutionService;
import com.chatbi.service.QueryGovernanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * AI 智能查询控制器
 */
@Slf4j
@Tag(name = "AI 智能查询", description = "基于 LLM 的自然语言查询接口")
@RestController
@RequestMapping("/api/ai-query")
@RequiredArgsConstructor
public class AiQueryController {

    private static final List<String> PRIORITY_TABLES = List.of(
        "sales_order",
        "sales_summary",
        "customer",
        "inventory",
        "service_ticket",
        "agile_project",
        "financial_record",
        "approval_record"
    );

    private final AiQueryService aiQueryService;
    private final QueryExecutionService queryExecutionService;
    private final DataSourceMapper dataSourceMapper;

    /**
     * AI 智能查询
     */
    @Operation(summary = "AI 智能查询", description = "使用自然语言进行数据查询，自动转换为 SQL 执行")
    @PostMapping("/query")
    public Result<QueryResponse> query(
            @Parameter(description = "查询请求") @RequestBody QueryRequest request) {

        String question = request.getQuestion();
        if (question == null || question.trim().isEmpty()) {
            return Result.error("问题不能为空");
        }

        try {
            // 构建表结构（实际应该从数据库获取）
            List<AiQueryService.TableSchema> schemas = buildSchemas(request.getDatasourceId());

            // 使用 LLM 生成 SQL
            String sql = aiQueryService.generateSqlWithLLM(question, schemas);

            log.info("AI 生成的 SQL: {}", sql);

            // 验证 SQL 安全性
            QueryGovernanceService.ValidationResult validation = aiQueryService.validateSqlDetail(sql);
            if (!validation.valid()) {
                log.warn("AI 查询 SQL 校验未通过，降级返回经营总览 - reason: {}", validation.message());
                return Result.ok(buildFallbackResponse(question, "SQL 校验未通过，已返回经营总览并提供推荐问法", validation.message(),
                    request.getDatasourceId(), request.getUserId()));
            }

            // 执行查询
            com.chatbi.entity.DataSource dataSource = resolveDatasource(request.getDatasourceId());
            if (dataSource == null) {
                return Result.ok(buildFallbackResponse(question, "未配置可用数据源，已返回经营总览并提供推荐问法", "未配置可用数据源",
                    request.getDatasourceId(), request.getUserId()));
            }
            List<Map<String, Object>> data = queryExecutionService.executeQuery(dataSource, sql, request.getUserId());

            QueryResponse response = QueryResponse.builder()
                    .question(question)
                    .generatedSql(sql)
                    .data(data)
                    .success(true)
                    .message("查询成功，共 " + data.size() + " 条结果")
                    .build();

            return Result.ok(response);

        } catch (Exception e) {
            log.error("AI 查询失败：{}", e.getMessage(), e);
            return Result.ok(buildFallbackResponse(question, "AI 查询链路异常，已自动切换经营总览并提供推荐问法", e.getMessage(),
                request.getDatasourceId(), request.getUserId()));
        }
    }

    /**
     * SQL 预览
     */
    @Operation(summary = "SQL 预览", description = "根据自然语言生成 SQL 但不执行")
    @PostMapping("/preview")
    public Result<SqlPreviewResponse> previewSql(
            @Parameter(description = "查询请求") @RequestBody QueryRequest request) {

        String question = request.getQuestion();
        if (question == null || question.trim().isEmpty()) {
            return Result.error("问题不能为空");
        }

        try {
            List<AiQueryService.TableSchema> schemas = buildSchemas(request.getDatasourceId());
            String sql = aiQueryService.generateSqlWithLLM(question, schemas);

            SqlPreviewResponse response = SqlPreviewResponse.builder()
                    .question(question)
                    .generatedSql(sql)
                    .success(true)
                    .build();

            return Result.ok(response);

        } catch (Exception e) {
            log.error("SQL 预览失败：{}", e.getMessage(), e);
            return Result.error("SQL 预览失败：" + e.getMessage());
        }
    }

    /**
     * SQL 安全性校验
     */
    @Operation(summary = "SQL 校验", description = "验证 SQL 语句的安全性")
    @PostMapping("/validate")
    public Result<ValidateResponse> validateSql(
            @Parameter(description = "SQL 语句") @RequestBody Map<String, String> request) {

        String sql = request.get("sql");
        QueryGovernanceService.ValidationResult validation = aiQueryService.validateSqlDetail(sql);

        ValidateResponse response = ValidateResponse.builder()
                .valid(validation.valid())
                .message(validation.message())
                .build();

        return Result.ok(response);
    }

    /**
     * 构建表结构信息
     */
    private List<AiQueryService.TableSchema> buildSchemas(Long datasourceId) {
        com.chatbi.entity.DataSource dataSource = resolveDatasource(datasourceId);
        if (dataSource == null) {
            throw new IllegalStateException("未配置可用数据源");
        }

        List<AiQueryService.TableSchema> schemas = queryExecutionService.extractTableSchemas(dataSource);
        if (schemas.isEmpty()) {
            throw new IllegalStateException("数据源中没有可用的业务表");
        }

        return schemas.stream()
            .filter(schema -> schema.getColumns() != null && !schema.getColumns().isEmpty())
            .sorted(Comparator
                .comparingInt((AiQueryService.TableSchema schema) -> {
                    int index = PRIORITY_TABLES.indexOf(schema.getTableName());
                    return index >= 0 ? index : Integer.MAX_VALUE;
                })
                .thenComparing(AiQueryService.TableSchema::getTableName))
            .limit(12)
            .toList();
    }

    private com.chatbi.entity.DataSource resolveDatasource(Long datasourceId) {
        if (datasourceId != null) {
            return dataSourceMapper.selectById(datasourceId);
        }

        com.chatbi.entity.DataSource defaultDataSource = dataSourceMapper.selectById(1L);
        if (defaultDataSource != null) {
            return defaultDataSource;
        }

        return dataSourceMapper.selectList(null).stream().findFirst().orElse(null);
    }

    private QueryResponse buildFallbackResponse(String question, String message, String recoverReason, Long datasourceId, Long userId) {
        return QueryResponse.builder()
            .question(question)
            .generatedSql("-- AI 查询降级：未生成有效 SQL")
            .data(List.of())
            .success(true)
            .source("guided-recovery")
            .recovered(true)
            .recoverReason(recoverReason)
            .suggestions(List.of("先给我一个数据概览"))
            .message(message)
            .build();
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class QueryResponse {
        private String question;
        private String generatedSql;
        private List<Map<String, Object>> data;
        private boolean success;
        private String message;
        private String source;
        private Boolean recovered;
        private String recoverReason;
        private List<String> suggestions;
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SqlPreviewResponse {
        private String question;
        private String generatedSql;
        private boolean success;
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ValidateResponse {
        private boolean valid;
        private String message;
    }

    @Data
    public static class QueryRequest {
        private String question;
        private Long datasourceId;
        private Long userId;
    }
}
