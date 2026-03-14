package com.chatbi.service;

import com.chatbi.config.AiConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 智能异常检测服务
 * 自动发现数据中的异常模式
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnomalyDetectionService {

    private final AiConfig aiConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 异常检测结果
     */
    public static class AnomalyResult {
        private String type; // spike(突增), drop(突降), outlier(离群点), trend_change(趋势变化)
        private String description;
        private String severity; // high, medium, low
        private Map<String, Object> details;
        private List<String> suggestions;

        public AnomalyResult(String type, String description, String severity) {
            this.type = type;
            this.description = description;
            this.severity = severity;
            this.details = new HashMap<>();
            this.suggestions = new ArrayList<>();
        }

        // Getters and Setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        public Map<String, Object> getDetails() { return details; }
        public void setDetails(Map<String, Object> details) { this.details = details; }
        public List<String> getSuggestions() { return suggestions; }
        public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }
    }

    /**
     * 检测数据异常
     */
    public List<AnomalyResult> detectAnomalies(List<Map<String, Object>> data, String context) {
        List<AnomalyResult> anomalies = new ArrayList<>();

        if (data == null || data.size() < 3) {
            return anomalies;
        }

        try {
            // 1. 统计异常检测（离群点）
            anomalies.addAll(detectOutliers(data));

            // 2. 趋势异常检测（突增突降）
            anomalies.addAll(detectTrendAnomalies(data));

            // 3. 周期性异常检测
            anomalies.addAll(detectPeriodicAnomalies(data));

            // 4. 使用AI增强异常分析
            if (aiConfig.isEnabled() && !anomalies.isEmpty()) {
                enhanceWithAI(anomalies, data, context);
            }

            log.info("异常检测完成 - 发现 {} 个异常", anomalies.size());
            return anomalies;

        } catch (Exception e) {
            log.error("异常检测失败", e);
            return anomalies;
        }
    }

    /**
     * 检测离群点
     */
    private List<AnomalyResult> detectOutliers(List<Map<String, Object>> data) {
        List<AnomalyResult> anomalies = new ArrayList<>();
        Map<String, Object> firstRow = data.get(0);

        // 找到数值列
        for (Map.Entry<String, Object> entry : firstRow.entrySet()) {
            String columnName = entry.getKey();
            Object value = entry.getValue();

            if (isNumeric(value)) {
                List<Double> values = data.stream()
                    .map(row -> row.get(columnName))
                    .filter(this::isNumeric)
                    .map(this::toDouble)
                    .collect(Collectors.toList());

                if (values.size() >= 5) {
                    // 使用3σ原则检测离群点
                    double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                    double variance = values.stream()
                        .mapToDouble(v -> Math.pow(v - mean, 2))
                        .average()
                        .orElse(0);
                    double stdDev = Math.sqrt(variance);

                    for (int i = 0; i < values.size(); i++) {
                        double val = values.get(i);
                        double zScore = Math.abs((val - mean) / stdDev);

                        if (zScore > 2.5) {
                            AnomalyResult anomaly = new AnomalyResult(
                                "outlier",
                                String.format("%s 存在异常值：%.2f（偏离平均值 %.1f 个标准差）",
                                    columnName, val, zScore),
                                zScore > 3 ? "high" : "medium"
                            );
                            anomaly.getDetails().put("column", columnName);
                            anomaly.getDetails().put("value", val);
                            anomaly.getDetails().put("mean", mean);
                            anomaly.getDetails().put("stdDev", stdDev);
                            anomaly.getDetails().put("zScore", zScore);
                            anomaly.getDetails().put("index", i);

                            anomaly.getSuggestions().add("检查数据来源是否准确");
                            anomaly.getSuggestions().add("分析异常值产生的原因");
                            anomaly.getSuggestions().add("考虑是否需要数据清洗");

                            anomalies.add(anomaly);
                        }
                    }
                }
            }
        }

        return anomalies.stream().limit(5).collect(Collectors.toList());
    }

    /**
     * 检测趋势异常（突增突降）
     */
    private List<AnomalyResult> detectTrendAnomalies(List<Map<String, Object>> data) {
        List<AnomalyResult> anomalies = new ArrayList<>();
        Map<String, Object> firstRow = data.get(0);

        for (Map.Entry<String, Object> entry : firstRow.entrySet()) {
            String columnName = entry.getKey();
            Object value = entry.getValue();

            if (isNumeric(value)) {
                List<Double> values = data.stream()
                    .map(row -> row.get(columnName))
                    .filter(this::isNumeric)
                    .map(this::toDouble)
                    .collect(Collectors.toList());

                if (values.size() >= 3) {
                    // 检测突增突降
                    for (int i = 1; i < values.size(); i++) {
                        double prev = values.get(i - 1);
                        double curr = values.get(i);

                        if (prev != 0) {
                            double changeRate = ((curr - prev) / prev) * 100;

                            // 突增
                            if (changeRate > 50) {
                                AnomalyResult anomaly = new AnomalyResult(
                                    "spike",
                                    String.format("%s 出现突增：从 %.2f 增长到 %.2f（增长 %.1f%%）",
                                        columnName, prev, curr, changeRate),
                                    changeRate > 100 ? "high" : "medium"
                                );
                                anomaly.getDetails().put("column", columnName);
                                anomaly.getDetails().put("previousValue", prev);
                                anomaly.getDetails().put("currentValue", curr);
                                anomaly.getDetails().put("changeRate", changeRate);
                                anomaly.getDetails().put("index", i);

                                anomaly.getSuggestions().add("分析增长原因，是否有营销活动");
                                anomaly.getSuggestions().add("确认数据是否准确");
                                anomaly.getSuggestions().add("评估是否可持续");

                                anomalies.add(anomaly);
                            }
                            // 突降
                            else if (changeRate < -50) {
                                AnomalyResult anomaly = new AnomalyResult(
                                    "drop",
                                    String.format("%s 出现突降：从 %.2f 下降到 %.2f（下降 %.1f%%）",
                                        columnName, prev, curr, Math.abs(changeRate)),
                                    Math.abs(changeRate) > 100 ? "high" : "medium"
                                );
                                anomaly.getDetails().put("column", columnName);
                                anomaly.getDetails().put("previousValue", prev);
                                anomaly.getDetails().put("currentValue", curr);
                                anomaly.getDetails().put("changeRate", changeRate);
                                anomaly.getDetails().put("index", i);

                                anomaly.getSuggestions().add("紧急排查下降原因");
                                anomaly.getSuggestions().add("检查是否有系统故障");
                                anomaly.getSuggestions().add("制定应对措施");

                                anomalies.add(anomaly);
                            }
                        }
                    }
                }
            }
        }

        return anomalies.stream().limit(3).collect(Collectors.toList());
    }

    /**
     * 检测周期性异常
     */
    private List<AnomalyResult> detectPeriodicAnomalies(List<Map<String, Object>> data) {
        List<AnomalyResult> anomalies = new ArrayList<>();

        // 简化实现：检测是否有明显的周期性模式被打破
        if (data.size() >= 7) {
            Map<String, Object> firstRow = data.get(0);

            for (Map.Entry<String, Object> entry : firstRow.entrySet()) {
                String columnName = entry.getKey();
                Object value = entry.getValue();

                if (isNumeric(value)) {
                    List<Double> values = data.stream()
                        .map(row -> row.get(columnName))
                        .filter(this::isNumeric)
                        .map(this::toDouble)
                        .collect(Collectors.toList());

                    // 检测周期性（简单实现：检查是否有规律的波动）
                    if (values.size() >= 7) {
                        double avgChange = 0;
                        int changeCount = 0;

                        for (int i = 1; i < values.size(); i++) {
                            double change = values.get(i) - values.get(i - 1);
                            avgChange += Math.abs(change);
                            changeCount++;
                        }

                        avgChange /= changeCount;

                        // 检查最后一个变化是否异常
                        if (values.size() >= 2) {
                            double lastChange = Math.abs(values.get(values.size() - 1) - values.get(values.size() - 2));
                            if (lastChange > avgChange * 2) {
                                AnomalyResult anomaly = new AnomalyResult(
                                    "trend_change",
                                    String.format("%s 的变化模式出现异常，最近变化幅度超过平均水平", columnName),
                                    "medium"
                                );
                                anomaly.getDetails().put("column", columnName);
                                anomaly.getDetails().put("avgChange", avgChange);
                                anomaly.getDetails().put("lastChange", lastChange);

                                anomaly.getSuggestions().add("分析模式变化的原因");
                                anomaly.getSuggestions().add("评估是否需要调整策略");

                                anomalies.add(anomaly);
                            }
                        }
                    }
                }
            }
        }

        return anomalies.stream().limit(2).collect(Collectors.toList());
    }

    /**
     * 使用AI增强异常分析
     */
    private void enhanceWithAI(List<AnomalyResult> anomalies, List<Map<String, Object>> data, String context) {
        try {
            if (anomalies.isEmpty()) {
                return;
            }

            // 构建AI提示词
            StringBuilder prompt = new StringBuilder();
            prompt.append("作为数据分析专家，请分析以下检测到的数据异常，并给出专业建议：\n\n");
            prompt.append("业务场景：").append(context).append("\n\n");
            prompt.append("检测到的异常：\n");

            for (int i = 0; i < Math.min(3, anomalies.size()); i++) {
                AnomalyResult anomaly = anomalies.get(i);
                prompt.append(i + 1).append(". ").append(anomaly.getDescription()).append("\n");
            }

            prompt.append("\n请针对每个异常：\n");
            prompt.append("1. 分析可能的原因\n");
            prompt.append("2. 评估影响程度\n");
            prompt.append("3. 给出具体的应对建议\n");
            prompt.append("\n请用简洁的语言回答，每个异常的分析不超过100字。");

            // 调用AI
            String aiAnalysis = callAI(prompt.toString());

            if (aiAnalysis != null && !aiAnalysis.isEmpty()) {
                // 将AI分析结果添加到第一个异常的建议中
                if (!anomalies.isEmpty()) {
                    anomalies.get(0).getDetails().put("aiAnalysis", aiAnalysis);
                }
            }

        } catch (Exception e) {
            log.error("AI增强分析失败", e);
        }
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
            requestBody.put("max_tokens", 500);

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
                log.error("AI调用失败 - 状态码: {}", response.code());
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

    /**
     * 判断是否为数值
     */
    private boolean isNumeric(Object value) {
        return value instanceof Number;
    }

    /**
     * 转换为Double
     */
    private Double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }
}
