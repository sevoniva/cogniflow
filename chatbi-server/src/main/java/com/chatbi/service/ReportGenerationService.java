package com.chatbi.service;

import com.chatbi.config.AiConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 智能报表生成服务
 * 自动生成数据分析报告
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportGenerationService {

    private final AiConfig aiConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AnomalyDetectionService anomalyDetectionService;
    private final DataExplorationService dataExplorationService;
    private final PredictionService predictionService;
    private final QueryResultAnalysisService analysisService;

    /**
     * 报表结果
     */
    public static class Report {
        private String reportId;
        private String title;
        private String summary;
        private LocalDateTime generatedAt;
        private List<ReportSection> sections;
        private Map<String, Object> metadata;

        public Report() {
            this.reportId = UUID.randomUUID().toString();
            this.generatedAt = LocalDateTime.now();
            this.sections = new ArrayList<>();
            this.metadata = new HashMap<>();
        }

        // Getters and Setters
        public String getReportId() { return reportId; }
        public void setReportId(String reportId) { this.reportId = reportId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
        public List<ReportSection> getSections() { return sections; }
        public void setSections(List<ReportSection> sections) { this.sections = sections; }
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }

    /**
     * 报表章节
     */
    public static class ReportSection {
        private String title;
        private String type; // overview, analysis, anomaly, insight, prediction, recommendation
        private String content;
        private List<Map<String, Object>> data;
        private Map<String, Object> visualization;

        public ReportSection(String title, String type) {
            this.title = title;
            this.type = type;
            this.data = new ArrayList<>();
            this.visualization = new HashMap<>();
        }

        // Getters and Setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public List<Map<String, Object>> getData() { return data; }
        public void setData(List<Map<String, Object>> data) { this.data = data; }
        public Map<String, Object> getVisualization() { return visualization; }
        public void setVisualization(Map<String, Object> visualization) { this.visualization = visualization; }
    }

    /**
     * 生成智能报表
     */
    public Report generateReport(String topic, List<Map<String, Object>> data, String context) {
        Report report = new Report();
        report.setTitle(topic + " - 数据分析报告");

        try {
            log.info("开始生成报表 - 主题: {}", topic);

            // 1. 数据概览
            ReportSection overview = generateOverview(data);
            report.getSections().add(overview);

            // 2. 数据分析
            ReportSection analysis = generateAnalysis(data);
            report.getSections().add(analysis);

            // 3. 异常检测
            List<AnomalyDetectionService.AnomalyResult> anomalies =
                anomalyDetectionService.detectAnomalies(data, context);
            if (!anomalies.isEmpty()) {
                ReportSection anomalySection = generateAnomalySection(anomalies);
                report.getSections().add(anomalySection);
            }

            // 4. 数据洞察
            List<DataExplorationService.DataInsight> insights =
                dataExplorationService.exploreData(data, context);
            if (!insights.isEmpty()) {
                ReportSection insightSection = generateInsightSection(insights);
                report.getSections().add(insightSection);
            }

            // 5. 趋势预测
            String numericColumn = findNumericColumn(data);
            if (numericColumn != null && data.size() >= 5) {
                PredictionService.PredictionResult prediction =
                    predictionService.predict(data, numericColumn, 3);
                if (prediction.getPredictedValues() != null && !prediction.getPredictedValues().isEmpty()) {
                    ReportSection predictionSection = generatePredictionSection(prediction);
                    report.getSections().add(predictionSection);
                }
            }

            // 6. 行动建议
            ReportSection recommendation = generateRecommendation(data, anomalies, insights);
            report.getSections().add(recommendation);

            // 7. 使用AI生成报表摘要
            if (aiConfig.isEnabled()) {
                String summary = generateSummaryWithAI(report, context);
                report.setSummary(summary);
            } else {
                report.setSummary("本报告分析了 " + data.size() + " 条数据，包含数据概览、深度分析、异常检测、洞察发现和趋势预测等内容。");
            }

            report.getMetadata().put("dataCount", data.size());
            report.getMetadata().put("sectionCount", report.getSections().size());

            log.info("报表生成完成 - ID: {}, 章节数: {}", report.getReportId(), report.getSections().size());
            return report;

        } catch (Exception e) {
            log.error("报表生成失败", e);
            report.setSummary("报表生成过程中出现错误");
            return report;
        }
    }

    /**
     * 生成数据概览
     */
    private ReportSection generateOverview(List<Map<String, Object>> data) {
        ReportSection section = new ReportSection("数据概览", "overview");

        StringBuilder content = new StringBuilder();
        content.append("本次分析共包含 ").append(data.size()).append(" 条数据记录。\n\n");

        if (!data.isEmpty()) {
            Map<String, Object> firstRow = data.get(0);
            content.append("数据维度：").append(firstRow.size()).append(" 个字段\n");

            // 统计数值字段
            long numericCount = firstRow.values().stream()
                .filter(v -> v instanceof Number)
                .count();
            content.append("数值字段：").append(numericCount).append(" 个\n");
            content.append("分类字段：").append(firstRow.size() - numericCount).append(" 个\n");
        }

        section.setContent(content.toString());
        section.setData(data.stream().limit(5).collect(Collectors.toList()));

        return section;
    }

    /**
     * 生成数据分析
     */
    private ReportSection generateAnalysis(List<Map<String, Object>> data) {
        ReportSection section = new ReportSection("数据分析", "analysis");

        String analysis = analysisService.generateInterpretation("数据分析", data, false);
        section.setContent(analysis);

        return section;
    }

    /**
     * 生成异常检测章节
     */
    private ReportSection generateAnomalySection(List<AnomalyDetectionService.AnomalyResult> anomalies) {
        ReportSection section = new ReportSection("异常检测", "anomaly");

        StringBuilder content = new StringBuilder();
        content.append("检测到 ").append(anomalies.size()).append(" 个数据异常：\n\n");

        for (int i = 0; i < anomalies.size(); i++) {
            AnomalyDetectionService.AnomalyResult anomaly = anomalies.get(i);
            content.append(i + 1).append(". ").append(anomaly.getDescription()).append("\n");
            content.append("   严重程度：").append(getSeverityText(anomaly.getSeverity())).append("\n");

            if (!anomaly.getSuggestions().isEmpty()) {
                content.append("   建议：").append(anomaly.getSuggestions().get(0)).append("\n");
            }
            content.append("\n");
        }

        section.setContent(content.toString());

        return section;
    }

    /**
     * 生成洞察章节
     */
    private ReportSection generateInsightSection(List<DataExplorationService.DataInsight> insights) {
        ReportSection section = new ReportSection("数据洞察", "insight");

        StringBuilder content = new StringBuilder();
        content.append("发现 ").append(insights.size()).append(" 个重要洞察：\n\n");

        for (int i = 0; i < insights.size(); i++) {
            DataExplorationService.DataInsight insight = insights.get(i);
            content.append(i + 1).append(". ").append(insight.getTitle()).append("\n");
            content.append("   ").append(insight.getDescription()).append("\n");

            if (!insight.getRecommendations().isEmpty()) {
                content.append("   建议：").append(insight.getRecommendations().get(0)).append("\n");
            }
            content.append("\n");
        }

        section.setContent(content.toString());

        return section;
    }

    /**
     * 生成预测章节
     */
    private ReportSection generatePredictionSection(PredictionService.PredictionResult prediction) {
        ReportSection section = new ReportSection("趋势预测", "prediction");

        StringBuilder content = new StringBuilder();
        content.append("基于历史数据对 ").append(prediction.getColumn()).append(" 进行预测：\n\n");
        content.append("预测方法：").append(getMethodText(prediction.getMethod())).append("\n");
        content.append("置信度：").append(String.format("%.1f%%", prediction.getConfidence() * 100)).append("\n");
        content.append("趋势：").append(getTrendText(prediction.getTrend())).append("\n\n");

        content.append("预测值：");
        for (int i = 0; i < prediction.getPredictedValues().size(); i++) {
            content.append(String.format("%.2f", prediction.getPredictedValues().get(i)));
            if (i < prediction.getPredictedValues().size() - 1) {
                content.append(", ");
            }
        }
        content.append("\n");

        section.setContent(content.toString());

        // 添加可视化配置
        section.getVisualization().put("type", "line");
        section.getVisualization().put("historical", prediction.getHistoricalValues());
        section.getVisualization().put("predicted", prediction.getPredictedValues());

        return section;
    }

    /**
     * 生成建议章节
     */
    private ReportSection generateRecommendation(List<Map<String, Object>> data,
                                                   List<AnomalyDetectionService.AnomalyResult> anomalies,
                                                   List<DataExplorationService.DataInsight> insights) {
        ReportSection section = new ReportSection("行动建议", "recommendation");

        StringBuilder content = new StringBuilder();
        content.append("基于以上分析，我们建议：\n\n");

        int recommendationCount = 1;

        // 基于异常的建议
        if (!anomalies.isEmpty()) {
            content.append(recommendationCount++).append(". 优先处理检测到的数据异常，特别是高严重程度的异常\n");
        }

        // 基于洞察的建议
        if (!insights.isEmpty()) {
            content.append(recommendationCount++).append(". 关注发现的数据洞察，制定相应的业务策略\n");
        }

        // 通用建议
        content.append(recommendationCount++).append(". 持续监控关键指标，建立预警机制\n");
        content.append(recommendationCount++).append(". 定期进行数据分析，及时发现问题和机会\n");
        content.append(recommendationCount).append(". 基于数据驱动决策，优化业务流程\n");

        section.setContent(content.toString());

        return section;
    }

    /**
     * 使用AI生成报表摘要
     */
    private String generateSummaryWithAI(Report report, String context) {
        try {
            StringBuilder prompt = new StringBuilder();
            prompt.append("请为以下数据分析报告生成一个简洁的摘要（100字以内）：\n\n");
            prompt.append("报告主题：").append(report.getTitle()).append("\n");
            prompt.append("业务场景：").append(context).append("\n\n");

            prompt.append("报告内容：\n");
            for (ReportSection section : report.getSections()) {
                prompt.append("- ").append(section.getTitle()).append("\n");
            }

            prompt.append("\n请用一段话概括报告的核心发现和价值。");

            String summary = callAI(prompt.toString());
            return summary != null ? summary : "本报告对数据进行了全面分析，包含多个维度的洞察和建议。";

        } catch (Exception e) {
            log.error("AI生成摘要失败", e);
            return "本报告对数据进行了全面分析，包含多个维度的洞察和建议。";
        }
    }

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

    /**
     * 调用AI
     */
    private String callAI(String prompt) {
        try {
            AiConfig.ProviderConfig provider = aiConfig.getCurrentProvider();

            Map<String, Object> requestBody = new HashMap<>();
            List<Map<String, String>> messages = new ArrayList<>();

            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.add(userMessage);

            requestBody.put("model", provider.getModel());
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 300);

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

            okhttp3.Response response = client.newCall(request).execute();

            if (!response.isSuccessful()) {
                return null;
            }

            String responseBody = response.body().string();
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            JsonNode choices = jsonNode.get("choices");

            if (choices != null && choices.size() > 0) {
                JsonNode messageNode = choices.get(0).get("message");
                if (messageNode != null) {
                    return messageNode.get("content").asText();
                }
            }

            return null;

        } catch (Exception e) {
            log.error("调用AI失败", e);
            return null;
        }
    }

    private String getSeverityText(String severity) {
        switch (severity) {
            case "high": return "高";
            case "medium": return "中";
            case "low": return "低";
            default: return "未知";
        }
    }

    private String getMethodText(String method) {
        switch (method) {
            case "linear": return "线性回归";
            case "exponential": return "指数平滑";
            case "ai": return "AI预测";
            default: return "未知";
        }
    }

    private String getTrendText(String trend) {
        switch (trend) {
            case "increasing": return "上升";
            case "decreasing": return "下降";
            case "stable": return "稳定";
            default: return "未知";
        }
    }
}
