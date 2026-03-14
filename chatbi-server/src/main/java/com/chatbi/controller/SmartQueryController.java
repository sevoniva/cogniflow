package com.chatbi.controller;

import com.chatbi.common.Result;
import com.chatbi.engine.QueryStatement;
import com.chatbi.engine.TextToSqlConverter;
import com.chatbi.entity.Metric;
import com.chatbi.entity.Synonym;
import com.chatbi.service.MetricService;
import com.chatbi.service.SynonymService;
import com.chatbi.service.QueryExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 智能查询控制器 - Text-to-SQL
 */
@Slf4j
@Tag(name = "智能查询", description = "自然语言查询接口，支持 Text-to-SQL 转换")
@RestController
@RequestMapping("/api/smart-query")
@RequiredArgsConstructor
public class SmartQueryController {

    private final TextToSqlConverter textToSqlConverter;
    private final MetricService metricService;
    private final SynonymService synonymService;
    private final QueryExecutionService queryExecutionService;

    /**
     * 执行自然语言查询
     */
    @Operation(summary = "执行自然语言查询", description = "将自然语言转换为 SQL 并执行查询")
    @PostMapping("/execute")
    public Result<QueryResponse> execute(
            @Parameter(description = "自然语言查询语句") @RequestBody QueryRequest request
    ) {
        String query = request.getQuery();
        if (query == null || query.trim().isEmpty()) {
            return Result.error("查询内容不能为空");
        }

        try {
            // 获取指标和同义词
            List<Metric> metrics = metricService.listActiveMetrics();
            List<Synonym> synonyms = synonymService.list();

            // 转换为 SQL
            String sql = textToSqlConverter.convert(query, metrics, synonyms);

            log.info("自然语言 '{}' 转换为 SQL: {}", query, sql);

            // 执行 SQL 查询
            List<Map<String, Object>> queryResult = queryExecutionService.execute(sql);

            QueryResponse response = QueryResponse.builder()
                    .originalQuery(query)
                    .generatedSql(sql)
                    .queryStatement(parseToStatement(sql))
                    .data(queryResult)
                    .success(true)
                    .message("查询执行成功，共 " + queryResult.size() + " 条结果")
                    .build();

            return Result.ok(response);

        } catch (Exception e) {
            log.error("查询执行失败：{}", e.getMessage(), e);
            return Result.error("查询执行失败：" + e.getMessage());
        }
    }

    /**
     * 解析 SQL 为结构化语句
     */
    private QueryStatement parseToStatement(String sql) {
        // 简化实现，实际应该解析 SQL
        QueryStatement statement = new QueryStatement();
        statement.setTable("unknown");
        return statement;
    }

    /**
     * 查询请求
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    public static class QueryRequest {
        @Parameter(description = "自然语言查询语句", required = true)
        private String query;
    }

    /**
     * 查询响应
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class QueryResponse {
        private String originalQuery;
        private String generatedSql;
        private QueryStatement queryStatement;
        private boolean success;
        private String message;
        private List<Map<String, Object>> data;
    }
}
