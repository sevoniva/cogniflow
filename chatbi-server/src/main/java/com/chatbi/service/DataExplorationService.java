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
 * 智能数据探索服务
 * 自动发现数据洞察和模式
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataExplorationService {

    private final AiConfig aiConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 数据洞察结果
     */
    public static class DataInsight {
        private String type; // correlation(相关性), distribution(分布), pattern(模式), comparison(对比)
        private String title;
        private String description;
        private String importance; // high, medium, low
        private Map<String, Object> data;
        private List<String> recommendations;

        public DataInsight(String type, String title, String description, String importance) {
            this.type = type;
            this.title = title;
            this.description = description;
            this.importance = importance;
            this.data = new HashMap<>();
            this.recommendations = new ArrayList<>();
        }

        // Getters and Setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getImportance() { return importance; }
        public void setImportance(String importance) { this.importance = importance; }
        public Map<String, Object> getData() { return data; }
        public void setData(Map<String, Object> data) { this.data = data; }
        public List<String> getRecommendations() { return recommendations; }
        public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
    }

    /**
     * 探索数据洞察
     */
    public List<DataInsight> exploreData(List<Map<String, Object>> data, String context) {
        List<DataInsight> insights = new ArrayList<>();

        if (data == null || data.size() < 2) {
            return insights;
        }

        try {
            // 1. 分布分析
            insights.addAll(analyzeDistribution(data));

            // 2. 相关性分析
            insights.addAll(analyzeCorrelation(data));

            // 3. 对比分析
            insights.addAll(analyzeComparison(data));

            // 4. 模式识别
            insights.addAll(recognizePatterns(data));

            // 5. 使用AI增强洞察
            if (aiConfig.isEnabled() && !insights.isEmpty()) {
                enhanceWithAI(insights, data, context);
            }

            log.info("数据探索完成 - 发现 {} 个洞察", insights.size());
            return insights;

        } catch (Exception e) {
            log.error("数据探索失败", e);
            return insights;
        }
    }

    /**
     * 分布分析
     */
    private List<DataInsight> analyzeDistribution(List<Map<String, Object>> data) {
        List<DataInsight> insights = new ArrayList<>();
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

                if (values.size() >= 5) {
                    // 计算分布特征
                    double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                    double max = values.stream().mapToDouble(Double::doubleValue).max().orElse(0);
                    double min = values.stream().mapToDouble(Double::doubleValue).min().orElse(0);

                    // 计算集中度
                    long aboveMean = values.stream().filter(v -> v > mean).count();
                    double concentration = (double) aboveMean / values.size();

                    if (concentration > 0.8 || concentration < 0.2) {
                        DataInsight insight = new DataInsight(
                            "distribution",
                            columnName + " 分布不均",
                            String.format("%s 的数据分布不均衡，%.0f%% 的数据%s平均值",
                                columnName, concentration * 100, concentration > 0.5 ? "高于" : "低于"),
                            "medium"
                        );
                        insight.getData().put("column", columnName);
                        insight.getData().put("mean", mean);
                        insight.getData().put("max", max);
                        insight.getData().put("min", min);
                        insight.getData().put("concentration", concentration);

                        insight.getRecommendations().add("关注数据分布的原因");
                        insight.getRecommendations().add("考虑是否需要分层分析");

                        insights.add(insight);
                    }
                }
            }
        }

        return insights.stream().limit(2).collect(Collectors.toList());
    }

    /**
     * 相关性分析
     */
    private List<DataInsight> analyzeCorrelation(List<Map<String, Object>> data) {
        List<DataInsight> insights = new ArrayList<>();
        Map<String, Object> firstRow = data.get(0);

        // 找出所有数值列
        List<String> numericColumns = firstRow.entrySet().stream()
            .filter(e -> isNumeric(e.getValue()))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        // 计算两两相关性
        for (int i = 0; i < numericColumns.size(); i++) {
            for (int j = i + 1; j < numericColumns.size(); j++) {
                String col1 = numericColumns.get(i);
                String col2 = numericColumns.get(j);

                List<Double> values1 = data.stream()
                    .map(row -> row.get(col1))
                    .filter(this::isNumeric)
                    .map(this::toDouble)
                    .collect(Collectors.toList());

                List<Double> values2 = data.stream()
                    .map(row -> row.get(col2))
                    .filter(this::isNumeric)
                    .map(this::toDouble)
                    .collect(Collectors.toList());

                if (values1.size() >= 5 && values1.size() == values2.size()) {
                    double correlation = calculateCorrelation(values1, values2);

                    if (Math.abs(correlation) > 0.7) {
                        DataInsight insight = new DataInsight(
                            "correlation",
                            col1 + " 与 " + col2 + " 相关性分析",
                            String.format("%s 与 %s 存在%s相关性（相关系数：%.2f）",
                                col1, col2, correlation > 0 ? "正" : "负", Math.abs(correlation)),
                            Math.abs(correlation) > 0.85 ? "high" : "medium"
                        );
                        insight.getData().put("column1", col1);
                        insight.getData().put("column2", col2);
                        insight.getData().put("correlation", correlation);

                        if (correlation > 0) {
                            insight.getRecommendations().add("可以通过提升 " + col1 + " 来提升 " + col2);
                        } else {
                            insight.getRecommendations().add("需要平衡 " + col1 + " 和 " + col2 + " 的关系");
                        }

                        insights.add(insight);
                    }
                }
            }
        }

        return insights.stream().limit(2).collect(Collectors.toList());
    }

    /**
     * 对比分析
     */
    private List<DataInsight> analyzeComparison(List<Map<String, Object>> data) {
        List<DataInsight> insights = new ArrayList<>();

        if (data.size() < 2) {
            return insights;
        }

        Map<String, Object> firstRow = data.get(0);

        // 找到分类列和数值列
        String categoryColumn = null;
        String valueColumn = null;

        for (Map.Entry<String, Object> entry : firstRow.entrySet()) {
            if (!isNumeric(entry.getValue()) && categoryColumn == null) {
                categoryColumn = entry.getKey();
            } else if (isNumeric(entry.getValue()) && valueColumn == null) {
                valueColumn = entry.getKey();
            }
        }

        if (categoryColumn != null && valueColumn != null) {
            String catCol = categoryColumn;
            String valCol = valueColumn;

            // 按分类统计
            Map<Object, Double> categoryValues = data.stream()
                .collect(Collectors.groupingBy(
                    row -> row.get(catCol),
                    Collectors.averagingDouble(row -> toDouble(row.get(valCol)))
                ));

            if (categoryValues.size() >= 2) {
                // 找出最大和最小
                Map.Entry<Object, Double> max = categoryValues.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .orElse(null);
                Map.Entry<Object, Double> min = categoryValues.entrySet().stream()
                    .min(Map.Entry.comparingByValue())
                    .orElse(null);

                if (max != null && min != null && max.getValue() > 0) {
                    double diff = ((max.getValue() - min.getValue()) / min.getValue()) * 100;

                    if (diff > 50) {
                        DataInsight insight = new DataInsight(
                            "comparison",
                            catCol + " 差异显著",
                            String.format("%s 中，%s 的 %s（%.2f）比 %s（%.2f）高出 %.1f%%",
                                catCol, max.getKey(), valCol, max.getValue(),
                                min.getKey(), min.getValue(), diff),
                            diff > 100 ? "high" : "medium"
                        );
                        insight.getData().put("categoryColumn", catCol);
                        insight.getData().put("valueColumn", valCol);
                        insight.getData().put("maxCategory", max.getKey());
                        insight.getData().put("maxValue", max.getValue());
                        insight.getData().put("minCategory", min.getKey());
                        insight.getData().put("minValue", min.getValue());
                        insight.getData().put("difference", diff);

                        insight.getRecommendations().add("学习 " + max.getKey() + " 的成功经验");
                        insight.getRecommendations().add("改进 " + min.getKey() + " 的表现");

                        insights.add(insight);
                    }
                }
            }
        }

        return insights;
    }

    /**
     * 模式识别
     */
    private List<DataInsight> recognizePatterns(List<Map<String, Object>> data) {
        List<DataInsight> insights = new ArrayList<>();

        if (data.size() < 5) {
            return insights;
        }

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

                if (values.size() >= 5) {
                    // 检测增长模式
                    int increasingCount = 0;
                    int decreasingCount = 0;

                    for (int i = 1; i < values.size(); i++) {
                        if (values.get(i) > values.get(i - 1)) {
                            increasingCount++;
                        } else if (values.get(i) < values.get(i - 1)) {
                            decreasingCount++;
                        }
                    }

                    double increasingRatio = (double) increasingCount / (values.size() - 1);
                    double decreasingRatio = (double) decreasingCount / (values.size() - 1);

                    if (increasingRatio > 0.7) {
                        DataInsight insight = new DataInsight(
                            "pattern",
                            columnName + " 持续增长",
                            String.format("%s 呈现持续增长趋势，%.0f%% 的时间段在增长",
                                columnName, increasingRatio * 100),
                            "high"
                        );
                        insight.getData().put("column", columnName);
                        insight.getData().put("pattern", "increasing");
                        insight.getData().put("ratio", increasingRatio);

                        insight.getRecommendations().add("保持当前策略，继续增长");
                        insight.getRecommendations().add("预测未来趋势，提前规划");

                        insights.add(insight);
                    } else if (decreasingRatio > 0.7) {
                        DataInsight insight = new DataInsight(
                            "pattern",
                            columnName + " 持续下降",
                            String.format("%s 呈现持续下降趋势，%.0f%% 的时间段在下降",
                                columnName, decreasingRatio * 100),
                            "high"
                        );
                        insight.getData().put("column", columnName);
                        insight.getData().put("pattern", "decreasing");
                        insight.getData().put("ratio", decreasingRatio);

                        insight.getRecommendations().add("紧急分析下降原因");
                        insight.getRecommendations().add("制定止跌回升计划");

                        insights.add(insight);
                    }
                }
            }
        }

        return insights.stream().limit(2).collect(Collectors.toList());
    }

    /**
     * 计算相关系数
     */
    private double calculateCorrelation(List<Double> x, List<Double> y) {
        if (x.size() != y.size() || x.isEmpty()) {
            return 0;
        }

        double meanX = x.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double meanY = y.stream().mapToDouble(Double::doubleValue).average().orElse(0);

        double numerator = 0;
        double denomX = 0;
        double denomY = 0;

        for (int i = 0; i < x.size(); i++) {
            double diffX = x.get(i) - meanX;
            double diffY = y.get(i) - meanY;
            numerator += diffX * diffY;
            denomX += diffX * diffX;
            denomY += diffY * diffY;
        }

        if (denomX == 0 || denomY == 0) {
            return 0;
        }

        return numerator / Math.sqrt(denomX * denomY);
    }

    /**
     * 使用AI增强洞察
     */
    private void enhanceWithAI(List<DataInsight> insights, List<Map<String, Object>> data, String context) {
        try {
            if (insights.isEmpty()) {
                return;
            }

            StringBuilder prompt = new StringBuilder();
            prompt.append("作为数据分析专家，请分析以下数据洞察，并给出深入的业务建议：\n\n");
            prompt.append("业务场景：").append(context).append("\n\n");
            prompt.append("发现的洞察：\n");

            for (int i = 0; i < Math.min(3, insights.size()); i++) {
                DataInsight insight = insights.get(i);
                prompt.append(i + 1).append(". ").append(insight.getTitle()).append("\n");
                prompt.append("   ").append(insight.getDescription()).append("\n");
            }

            prompt.append("\n请针对这些洞察：\n");
            prompt.append("1. 分析背后的业务含义\n");
            prompt.append("2. 给出可执行的行动建议\n");
            prompt.append("3. 预测可能的发展趋势\n");
            prompt.append("\n请用简洁的语言回答，总字数不超过200字。");

            String aiAnalysis = callAI(prompt.toString());

            if (aiAnalysis != null && !aiAnalysis.isEmpty()) {
                if (!insights.isEmpty()) {
                    insights.get(0).getData().put("aiAnalysis", aiAnalysis);
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

    private boolean isNumeric(Object value) {
        return value instanceof Number;
    }

    private Double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }
}
