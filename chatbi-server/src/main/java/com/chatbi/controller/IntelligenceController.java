package com.chatbi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.chatbi.common.Result;
import com.chatbi.repository.DataSourceMapper;
import com.chatbi.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 智能分析控制器
 * 提供AI增强的数据分析功能
 */
@Slf4j
@Tag(name = "智能分析", description = "智能分析控制器")
@RestController
@RequestMapping("/api/intelligence")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class IntelligenceController {

    private final AnomalyDetectionService anomalyDetectionService;
    private final DataExplorationService dataExplorationService;
    private final PredictionService predictionService;
    private final ReportGenerationService reportGenerationService;
    private final AiAssistantService aiAssistantService;
    private final QueryExecutionService queryExecutionService;
    private final AiQueryService aiQueryService;
    private final DataSourceMapper dataSourceMapper;

    /**
     * 异常检测
     */
    @Operation(summary = "异常检测")
    @PostMapping("/anomaly-detection")
    public Result<Map<String, Object>> detectAnomalies(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> data = (List<Map<String, Object>>) request.get("data");
            String context = (String) request.getOrDefault("context", "数据分析");
            Long userId = request.get("userId") != null ? Long.parseLong(request.get("userId").toString()) : null;

            if (data == null || data.isEmpty()) {
                return Result.error("数据不能为空");
            }

            List<AnomalyDetectionService.AnomalyResult> anomalies =
                anomalyDetectionService.detectAnomalies(data, context);

            Map<String, Object> response = new HashMap<>();
            response.put("anomalies", anomalies);
            response.put("count", anomalies.size());
            response.put("hasAnomalies", !anomalies.isEmpty());

            return Result.ok(response);

        } catch (Exception e) {
            log.error("异常检测失败", e);
            return Result.error("异常检测失败：" + e.getMessage());
        }
    }

    /**
     * 数据探索
     */
    @Operation(summary = "数据探索")
    @PostMapping("/data-exploration")
    public Result<Map<String, Object>> exploreData(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> data = (List<Map<String, Object>>) request.get("data");
            String context = (String) request.getOrDefault("context", "数据分析");

            if (data == null || data.isEmpty()) {
                return Result.error("数据不能为空");
            }

            List<DataExplorationService.DataInsight> insights =
                dataExplorationService.exploreData(data, context);

            Map<String, Object> response = new HashMap<>();
            response.put("insights", insights);
            response.put("count", insights.size());
            response.put("hasInsights", !insights.isEmpty());

            return Result.ok(response);

        } catch (Exception e) {
            log.error("数据探索失败", e);
            return Result.error("数据探索失败：" + e.getMessage());
        }
    }

    /**
     * 趋势预测
     */
    @Operation(summary = "趋势预测")
    @PostMapping("/prediction")
    public Result<PredictionService.PredictionResult> predict(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> data = (List<Map<String, Object>>) request.get("data");
            String column = (String) request.get("column");
            Integer periods = (Integer) request.getOrDefault("periods", 3);

            if (data == null || data.isEmpty()) {
                return Result.error("数据不能为空");
            }

            if (column == null || column.isEmpty()) {
                return Result.error("预测列不能为空");
            }

            PredictionService.PredictionResult prediction =
                predictionService.predict(data, column, periods);

            return Result.ok(prediction);

        } catch (Exception e) {
            log.error("趋势预测失败", e);
            return Result.error("趋势预测失败：" + e.getMessage());
        }
    }

    /**
     * 生成智能报表
     */
    @Operation(summary = "生成智能报表")
    @PostMapping("/generate-report")
    public Result<ReportGenerationService.Report> generateReport(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> data = (List<Map<String, Object>>) request.get("data");
            String topic = (String) request.getOrDefault("topic", "数据分析");
            String context = (String) request.getOrDefault("context", "业务数据");

            if (data == null || data.isEmpty()) {
                return Result.error("数据不能为空");
            }

            ReportGenerationService.Report report =
                reportGenerationService.generateReport(topic, data, context);

            return Result.ok(report);

        } catch (Exception e) {
            log.error("报表生成失败", e);
            return Result.error("报表生成失败：" + e.getMessage());
        }
    }

    /**
     * AI助手问答
     */
    @Operation(summary = "AI助手问答")
    @PostMapping("/assistant/ask")
    public Result<AiAssistantService.AssistantResponse> askAssistant(@RequestBody Map<String, Object> request) {
        try {
            String question = (String) request.get("question");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> contextData = (List<Map<String, Object>>) request.get("contextData");
            String businessContext = (String) request.getOrDefault("businessContext", "数据分析");

            if (question == null || question.trim().isEmpty()) {
                return Result.error("问题不能为空");
            }

            AiAssistantService.AssistantResponse response =
                aiAssistantService.ask(question, contextData, businessContext);

            return Result.ok(response);

        } catch (Exception e) {
            log.error("AI助手问答失败", e);
            return Result.error("AI助手问答失败：" + e.getMessage());
        }
    }

    /**
     * 综合智能分析（一次性执行所有分析）
     */
    @Operation(summary = "综合智能分析（一次性执行所有分析）")
    @PostMapping("/comprehensive-analysis")
    public Result<Map<String, Object>> comprehensiveAnalysis(@RequestBody Map<String, Object> request) {
        try {
            Long dataSourceId = request.get("dataSourceId") != null ?
                Long.parseLong(request.get("dataSourceId").toString()) : 1L;
            String sql = (String) request.get("sql");
            String context = (String) request.getOrDefault("context", "数据分析");
            Long userId = request.get("userId") != null ? Long.parseLong(request.get("userId").toString()) : null;

            if (sql == null || sql.trim().isEmpty()) {
                return Result.error("SQL不能为空");
            }

            // 1. 执行查询
            QueryGovernanceService.ValidationResult validation = aiQueryService.validateSqlDetail(sql);
            if (!validation.valid()) {
                return Result.error(validation.message());
            }

            com.chatbi.entity.DataSource dataSource = dataSourceMapper.selectById(dataSourceId);
            if (dataSource == null) {
                return Result.error("数据源不存在");
            }

            List<Map<String, Object>> data = queryExecutionService.executeQuery(dataSource, sql, userId);

            if (data.isEmpty()) {
                return Result.error("查询结果为空");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("data", data);
            response.put("dataCount", data.size());

            // 2. 异常检测
            List<AnomalyDetectionService.AnomalyResult> anomalies =
                anomalyDetectionService.detectAnomalies(data, context);
            response.put("anomalies", anomalies);

            // 3. 数据洞察
            List<DataExplorationService.DataInsight> insights =
                dataExplorationService.exploreData(data, context);
            response.put("insights", insights);

            // 4. 趋势预测（如果数据足够）
            if (data.size() >= 5) {
                String numericColumn = findNumericColumn(data);
                if (numericColumn != null) {
                    PredictionService.PredictionResult prediction =
                        predictionService.predict(data, numericColumn, 3);
                    response.put("prediction", prediction);
                }
            }

            // 5. 生成摘要
            StringBuilder summary = new StringBuilder();
            summary.append("分��了 ").append(data.size()).append(" 条数据。");

            if (!anomalies.isEmpty()) {
                summary.append("发现 ").append(anomalies.size()).append(" 个异常。");
            }

            if (!insights.isEmpty()) {
                summary.append("发现 ").append(insights.size()).append(" 个洞察。");
            }

            response.put("summary", summary.toString());

            return Result.ok(response);

        } catch (Exception e) {
            log.error("综合分析失败", e);
            return Result.error("综合分析失败：" + e.getMessage());
        }
    }

    /**
     * 获取智能建议
     */
    @Operation(summary = "获取智能建议")
    @PostMapping("/suggestions")
    public Result<List<String>> getSuggestions(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> data = (List<Map<String, Object>>) request.get("data");
            String context = (String) request.getOrDefault("context", "数据分析");

            List<String> suggestions = new ArrayList<>();

            if (data != null && !data.isEmpty()) {
                // 基于数据生成建议
                if (data.size() < 10) {
                    suggestions.add("数据量较少，建议扩大查询范围");
                }

                // 检查是否有异常
                List<AnomalyDetectionService.AnomalyResult> anomalies =
                    anomalyDetectionService.detectAnomalies(data, context);
                if (!anomalies.isEmpty()) {
                    suggestions.add("发现数据异常，建议进行异常分析");
                }

                // 检查是否可以预测
                if (data.size() >= 5) {
                    suggestions.add("数据量充足，可以进行趋势预测");
                }

                // 通用建议
                suggestions.add("可以生成完整的分析报告");
                suggestions.add("可以向AI助手提问获取更多洞察");
            } else {
                suggestions.add("请先执行查询获取数据");
            }

            return Result.ok(suggestions);

        } catch (Exception e) {
            log.error("获取建议失败", e);
            return Result.error("获取建议失败：" + e.getMessage());
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 查找数值列
     */
    private String findNumericColumn(List<Map<String, Object>> data) {
        if (data.isEmpty()) {
            return null;
        }

        Map<String, Object> firstRow = data.get(0);
        for (Map.Entry<String, Object> entry : firstRow.entrySet()) {
            if (entry.getValue() instanceof Number) {
                return entry.getKey();
            }
        }

        return null;
    }
}
