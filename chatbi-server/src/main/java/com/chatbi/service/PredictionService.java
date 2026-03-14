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
 * 预测分析服务
 * 基于历史数据进行趋势预测
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PredictionService {

    private final AiConfig aiConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 预测结果
     */
    public static class PredictionResult {
        private String column;
        private List<Double> historicalValues;
        private List<Double> predictedValues;
        private String method; // linear, exponential, ai
        private double confidence; // 0-1
        private String trend; // increasing, decreasing, stable
        private Map<String, Object> metadata;

        public PredictionResult() {
            this.metadata = new HashMap<>();
        }

        // Getters and Setters
        public String getColumn() { return column; }
        public void setColumn(String column) { this.column = column; }
        public List<Double> getHistoricalValues() { return historicalValues; }
        public void setHistoricalValues(List<Double> historicalValues) { this.historicalValues = historicalValues; }
        public List<Double> getPredictedValues() { return predictedValues; }
        public void setPredictedValues(List<Double> predictedValues) { this.predictedValues = predictedValues; }
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        public String getTrend() { return trend; }
        public void setTrend(String trend) { this.trend = trend; }
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }

    /**
     * 预测未来值
     */
    public PredictionResult predict(List<Map<String, Object>> historicalData, String targetColumn, int periods) {
        PredictionResult result = new PredictionResult();
        result.setColumn(targetColumn);

        if (historicalData == null || historicalData.size() < 3) {
            log.warn("历史数据不足，无法进行预测");
            return result;
        }

        try {
            // 提取目标列的历史值
            List<Double> values = historicalData.stream()
                .map(row -> row.get(targetColumn))
                .filter(this::isNumeric)
                .map(this::toDouble)
                .collect(Collectors.toList());

            if (values.size() < 3) {
                log.warn("有效数据点不足，无法进行预测");
                return result;
            }

            result.setHistoricalValues(values);

            // 1. 尝试线性回归预测
            List<Double> linearPrediction = linearRegression(values, periods);

            // 2. 尝试指数平滑预测
            List<Double> exponentialPrediction = exponentialSmoothing(values, periods);

            // 3. 选择最佳预测方法
            double linearError = calculateError(values, linearPrediction);
            double exponentialError = calculateError(values, exponentialPrediction);

            if (linearError < exponentialError) {
                result.setPredictedValues(linearPrediction);
                result.setMethod("linear");
                result.setConfidence(calculateConfidence(linearError, values));
            } else {
                result.setPredictedValues(exponentialPrediction);
                result.setMethod("exponential");
                result.setConfidence(calculateConfidence(exponentialError, values));
            }

            // 4. 判断趋势
            result.setTrend(detectTrend(values));

            // 5. 使用AI增强预测分析
            if (aiConfig != null && aiConfig.isEnabled()) {
                enhanceWithAI(result, historicalData, targetColumn);
            }

            log.info("预测完成 - 列: {}, 方法: {}, 置信度: {}", targetColumn, result.getMethod(), result.getConfidence());
            return result;

        } catch (Exception e) {
            log.error("预测失败", e);
            return result;
        }
    }

    /**
     * 线性回归预测
     */
    private List<Double> linearRegression(List<Double> values, int periods) {
        int n = values.size();

        // 计算线性回归参数
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;

        for (int i = 0; i < n; i++) {
            sumX += i;
            sumY += values.get(i);
            sumXY += i * values.get(i);
            sumX2 += i * i;
        }

        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        double intercept = (sumY - slope * sumX) / n;

        // 预测未来值
        List<Double> predictions = new ArrayList<>();
        for (int i = 0; i < periods; i++) {
            double predicted = slope * (n + i) + intercept;
            predictions.add(Math.max(0, predicted)); // 确保非负
        }

        return predictions;
    }

    /**
     * 指数平滑预测
     */
    private List<Double> exponentialSmoothing(List<Double> values, int periods) {
        double alpha = 0.3; // 平滑系数

        // 初始化
        double smoothed = values.get(0);

        // 计算平滑值
        for (int i = 1; i < values.size(); i++) {
            smoothed = alpha * values.get(i) + (1 - alpha) * smoothed;
        }

        // 计算趋势
        double trend = 0;
        if (values.size() >= 2) {
            trend = values.get(values.size() - 1) - values.get(values.size() - 2);
        }

        // 预测未来值
        List<Double> predictions = new ArrayList<>();
        double lastValue = smoothed;

        for (int i = 0; i < periods; i++) {
            double predicted = lastValue + trend;
            predictions.add(Math.max(0, predicted));
            lastValue = predicted;
        }

        return predictions;
    }

    /**
     * 计算预测误差
     */
    private double calculateError(List<Double> actual, List<Double> predicted) {
        if (actual.size() < 2 || predicted.isEmpty()) {
            return Double.MAX_VALUE;
        }

        // 使用最后几个实际值与预测值比较
        int compareSize = Math.min(3, actual.size());
        double error = 0;

        for (int i = 0; i < compareSize; i++) {
            int actualIndex = actual.size() - compareSize + i;
            if (i < predicted.size()) {
                error += Math.abs(actual.get(actualIndex) - predicted.get(i));
            }
        }

        return error / compareSize;
    }

    /**
     * 计算置信度
     */
    private double calculateConfidence(double error, List<Double> values) {
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(1);

        if (mean == 0) {
            return 0.5;
        }

        // 相对误差
        double relativeError = error / mean;

        // 转换为置信度 (0-1)
        double confidence = 1 - Math.min(1, relativeError);

        return Math.max(0, Math.min(1, confidence));
    }

    /**
     * 检测趋势
     */
    private String detectTrend(List<Double> values) {
        if (values.size() < 2) {
            return "stable";
        }

        int increasing = 0;
        int decreasing = 0;

        for (int i = 1; i < values.size(); i++) {
            if (values.get(i) > values.get(i - 1)) {
                increasing++;
            } else if (values.get(i) < values.get(i - 1)) {
                decreasing++;
            }
        }

        double increasingRatio = (double) increasing / (values.size() - 1);
        double decreasingRatio = (double) decreasing / (values.size() - 1);

        if (increasingRatio > 0.6) {
            return "increasing";
        } else if (decreasingRatio > 0.6) {
            return "decreasing";
        } else {
            return "stable";
        }
    }

    /**
     * 使用AI增强预测分析
     */
    private void enhanceWithAI(PredictionResult result, List<Map<String, Object>> data, String column) {
        try {
            StringBuilder prompt = new StringBuilder();
            prompt.append("作为数据分析专家，请分析以下预测结果：\n\n");
            prompt.append("指标：").append(column).append("\n");
            prompt.append("历史数据：").append(formatValues(result.getHistoricalValues())).append("\n");
            prompt.append("预测值：").append(formatValues(result.getPredictedValues())).append("\n");
            prompt.append("趋势：").append(result.getTrend()).append("\n");
            prompt.append("置信度：").append(String.format("%.1f%%", result.getConfidence() * 100)).append("\n\n");

            prompt.append("请提供：\n");
            prompt.append("1. 预测结果的业务解读\n");
            prompt.append("2. 可能影响预测准确性的因素\n");
            prompt.append("3. 基于预测的行动建议\n");
            prompt.append("\n请用简洁的语言回答，总字数不超过150字。");

            String aiAnalysis = callAI(prompt.toString());

            if (aiAnalysis != null && !aiAnalysis.isEmpty()) {
                result.getMetadata().put("aiAnalysis", aiAnalysis);
            }

        } catch (Exception e) {
            log.error("AI增强分析失败", e);
        }
    }

    /**
     * 格式化数值列表
     */
    private String formatValues(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return "[]";
        }

        return values.stream()
            .limit(10)
            .map(v -> String.format("%.2f", v))
            .collect(Collectors.joining(", ", "[", values.size() > 10 ? ", ...]" : "]"));
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
            requestBody.put("max_tokens", 400);

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
