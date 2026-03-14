package com.chatbi.service;

import com.chatbi.config.AiConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * AI数据助手服务
 * 提供智能问答和数据咨询
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAssistantService {

    private final AiConfig aiConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AnomalyDetectionService anomalyDetectionService;
    private final DataExplorationService dataExplorationService;
    private final PredictionService predictionService;

    /**
     * 助手响应
     */
    public static class AssistantResponse {
        private String answer;
        private String type; // explanation, suggestion, analysis, guidance
        private List<String> followUpQuestions;
        private Map<String, Object> data;

        public AssistantResponse() {
            this.followUpQuestions = new ArrayList<>();
            this.data = new HashMap<>();
        }

        // Getters and Setters
        public String getAnswer() { return answer; }
        public void setAnswer(String answer) { this.answer = answer; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public List<String> getFollowUpQuestions() { return followUpQuestions; }
        public void setFollowUpQuestions(List<String> followUpQuestions) { this.followUpQuestions = followUpQuestions; }
        public Map<String, Object> getData() { return data; }
        public void setData(Map<String, Object> data) { this.data = data; }
    }

    /**
     * 智能问答
     */
    public AssistantResponse ask(String question, List<Map<String, Object>> contextData, String businessContext) {
        AssistantResponse response = new AssistantResponse();

        try {
            log.info("AI助手收到问题: {}", question);

            // 1. 判断问题类型
            String questionType = classifyQuestion(question);
            response.setType(questionType);

            // 2. 根据问题类型处理
            switch (questionType) {
                case "analysis":
                    handleAnalysisQuestion(question, contextData, businessContext, response);
                    break;
                case "anomaly":
                    handleAnomalyQuestion(question, contextData, businessContext, response);
                    break;
                case "prediction":
                    handlePredictionQuestion(question, contextData, businessContext, response);
                    break;
                case "insight":
                    handleInsightQuestion(question, contextData, businessContext, response);
                    break;
                case "guidance":
                    handleGuidanceQuestion(question, contextData, businessContext, response);
                    break;
                default:
                    handleGeneralQuestion(question, contextData, businessContext, response);
            }

            // 3. 生成追问建议
            response.setFollowUpQuestions(generateFollowUpQuestions(question, questionType));

            log.info("AI助手回答完成 - 类型: {}", questionType);
            return response;

        } catch (Exception e) {
            log.error("AI助手处理失败", e);
            response.setAnswer("抱歉，我暂时无法回答这个问题。请尝试换一种方式提问。");
            response.setType("error");
            return response;
        }
    }

    /**
     * 分类问题类型
     */
    private String classifyQuestion(String question) {
        String lowerQuestion = question.toLowerCase();

        if (lowerQuestion.contains("为什么") || lowerQuestion.contains("原因") ||
            lowerQuestion.contains("分析") || lowerQuestion.contains("如何")) {
            return "analysis";
        } else if (lowerQuestion.contains("异常") || lowerQuestion.contains("问题") ||
                   lowerQuestion.contains("不正常") || lowerQuestion.contains("波动")) {
            return "anomaly";
        } else if (lowerQuestion.contains("预测") || lowerQuestion.contains("未来") ||
                   lowerQuestion.contains("趋势") || lowerQuestion.contains("会")) {
            return "prediction";
        } else if (lowerQuestion.contains("洞察") || lowerQuestion.contains("发现") ||
                   lowerQuestion.contains("特点") || lowerQuestion.contains("规律")) {
            return "insight";
        } else if (lowerQuestion.contains("怎么做") || lowerQuestion.contains("建议") ||
                   lowerQuestion.contains("应该") || lowerQuestion.contains("如何改进")) {
            return "guidance";
        } else {
            return "general";
        }
    }

    /**
     * 处理分析类问题
     */
    private void handleAnalysisQuestion(String question, List<Map<String, Object>> data,
                                        String context, AssistantResponse response) {
        if (data == null || data.isEmpty()) {
            response.setAnswer("当前没有可分析的数据。请先执行查询获取数据。");
            return;
        }

        // 使用AI进行深度分析
        String aiAnswer = analyzeWithAI(question, data, context);
        response.setAnswer(aiAnswer != null ? aiAnswer : "数据分析中，请稍候...");
    }

    /**
     * 处理异常检测问题
     */
    private void handleAnomalyQuestion(String question, List<Map<String, Object>> data,
                                       String context, AssistantResponse response) {
        if (data == null || data.isEmpty()) {
            response.setAnswer("当前没有可检测的数据。请先执行查询获取数据。");
            return;
        }

        List<AnomalyDetectionService.AnomalyResult> anomalies =
            anomalyDetectionService.detectAnomalies(data, context);

        if (anomalies.isEmpty()) {
            response.setAnswer("经过检测，��前数据没有发现明显异常。数据表现正常。");
        } else {
            StringBuilder answer = new StringBuilder();
            answer.append("检测到 ").append(anomalies.size()).append(" 个异常：\n\n");

            for (int i = 0; i < Math.min(3, anomalies.size()); i++) {
                AnomalyDetectionService.AnomalyResult anomaly = anomalies.get(i);
                answer.append(i + 1).append(". ").append(anomaly.getDescription()).append("\n");

                if (!anomaly.getSuggestions().isEmpty()) {
                    answer.append("   建议：").append(anomaly.getSuggestions().get(0)).append("\n");
                }
                answer.append("\n");
            }

            response.setAnswer(answer.toString());
            response.getData().put("anomalies", anomalies);
        }
    }

    /**
     * 处理预测类问题
     */
    private void handlePredictionQuestion(String question, List<Map<String, Object>> data,
                                          String context, AssistantResponse response) {
        if (data == null || data.size() < 3) {
            response.setAnswer("历史数据不足，无法进行准确预测。建议至少需要3个以上的历史数据点。");
            return;
        }

        // 找到数值列
        String numericColumn = findNumericColumn(data);
        if (numericColumn == null) {
            response.setAnswer("数据中没有可预测的数值字段。");
            return;
        }

        PredictionService.PredictionResult prediction =
            predictionService.predict(data, numericColumn, 3);

        if (prediction.getPredictedValues() == null || prediction.getPredictedValues().isEmpty()) {
            response.setAnswer("预测失败，请检查数据质量。");
            return;
        }

        StringBuilder answer = new StringBuilder();
        answer.append("基于历史数据，对 ").append(numericColumn).append(" 的预测如下：\n\n");
        answer.append("预测方法：").append(getMethodText(prediction.getMethod())).append("\n");
        answer.append("置信度：").append(String.format("%.1f%%", prediction.getConfidence() * 100)).append("\n");
        answer.append("趋势：").append(getTrendText(prediction.getTrend())).append("\n\n");

        answer.append("未来3期预测值：\n");
        for (int i = 0; i < prediction.getPredictedValues().size(); i++) {
            answer.append("第 ").append(i + 1).append(" 期：")
                  .append(String.format("%.2f", prediction.getPredictedValues().get(i)))
                  .append("\n");
        }

        // 添加AI分析
        if (prediction.getMetadata().containsKey("aiAnalysis")) {
            answer.append("\n").append(prediction.getMetadata().get("aiAnalysis"));
        }

        response.setAnswer(answer.toString());
        response.getData().put("prediction", prediction);
    }

    /**
     * 处理洞察类问题
     */
    private void handleInsightQuestion(String question, List<Map<String, Object>> data,
                                       String context, AssistantResponse response) {
        if (data == null || data.isEmpty()) {
            response.setAnswer("当前没有可分析的数据。请先执行查询获取数据。");
            return;
        }

        List<DataExplorationService.DataInsight> insights =
            dataExplorationService.exploreData(data, context);

        if (insights.isEmpty()) {
            response.setAnswer("当前数据中没有发现特别的洞察。数据表现较为平稳。");
        } else {
            StringBuilder answer = new StringBuilder();
            answer.append("发现 ").append(insights.size()).append(" 个重要洞察：\n\n");

            for (int i = 0; i < Math.min(3, insights.size()); i++) {
                DataExplorationService.DataInsight insight = insights.get(i);
                answer.append(i + 1).append(". ").append(insight.getTitle()).append("\n");
                answer.append("   ").append(insight.getDescription()).append("\n");

                if (!insight.getRecommendations().isEmpty()) {
                    answer.append("   建议：").append(insight.getRecommendations().get(0)).append("\n");
                }
                answer.append("\n");
            }

            response.setAnswer(answer.toString());
            response.getData().put("insights", insights);
        }
    }

    /**
     * 处理指导类问题
     */
    private void handleGuidanceQuestion(String question, List<Map<String, Object>> data,
                                        String context, AssistantResponse response) {
        // 使用AI提供指导建议
        String guidance = provideGuidanceWithAI(question, data, context);
        response.setAnswer(guidance != null ? guidance : "正在为您分析，请稍候...");
    }

    /**
     * 处理通用问题
     */
    private void handleGeneralQuestion(String question, List<Map<String, Object>> data,
                                       String context, AssistantResponse response) {
        // 使用AI回答通用问题
        String answer = answerWithAI(question, data, context);
        response.setAnswer(answer != null ? answer : "我正在学习中，暂时无法回答这个问题。");
    }

    /**
     * 使用AI分析
     */
    private String analyzeWithAI(String question, List<Map<String, Object>> data, String context) {
        try {
            StringBuilder prompt = new StringBuilder();
            prompt.append("作为数据分析专家，请回答以下问题：\n\n");
            prompt.append("业务场景：").append(context).append("\n");
            prompt.append("问题：").append(question).append("\n\n");
            prompt.append("数据概况：\n");
            prompt.append("- 数据量：").append(data.size()).append(" 条\n");

            if (!data.isEmpty()) {
                Map<String, Object> firstRow = data.get(0);
                prompt.append("- 字段数：").append(firstRow.size()).append(" 个\n");
            }

            prompt.append("\n请提供专业的分析和建议，字数控制在150字以内。");

            return callAI(prompt.toString());

        } catch (Exception e) {
            log.error("AI分析失败", e);
            return null;
        }
    }

    /**
     * 使用AI提供指导
     */
    private String provideGuidanceWithAI(String question, List<Map<String, Object>> data, String context) {
        try {
            StringBuilder prompt = new StringBuilder();
            prompt.append("作为业务顾问，请针对以下问题提供可执行的建议：\n\n");
            prompt.append("业务场景：").append(context).append("\n");
            prompt.append("问题：").append(question).append("\n\n");
            prompt.append("请提供3-5条具体的行动建议，每条建议简洁明了。");

            return callAI(prompt.toString());

        } catch (Exception e) {
            log.error("AI指导失败", e);
            return null;
        }
    }

    /**
     * 使用AI回答
     */
    private String answerWithAI(String question, List<Map<String, Object>> data, String context) {
        try {
            StringBuilder prompt = new StringBuilder();
            prompt.append("作为智能数据助手，请回答用户的问题：\n\n");
            prompt.append("业务场景：").append(context).append("\n");
            prompt.append("问题：").append(question).append("\n\n");
            prompt.append("请用简洁、友好的语言回答，字数控制在100字以内。");

            return callAI(prompt.toString());

        } catch (Exception e) {
            log.error("AI回答失败", e);
            return null;
        }
    }

    /**
     * 生成追问建议
     */
    private List<String> generateFollowUpQuestions(String question, String questionType) {
        List<String> questions = new ArrayList<>();

        switch (questionType) {
            case "analysis":
                questions.add("有什么改进建议吗？");
                questions.add("这个趋势会持续吗？");
                questions.add("有哪些风险需要注意？");
                break;
            case "anomaly":
                questions.add("如何预防类似异常？");
                questions.add("异常的影响有多大？");
                questions.add("需要采取什么措施？");
                break;
            case "prediction":
                questions.add("预测的准确度如何？");
                questions.add("影响预测的因素有哪些？");
                questions.add("如何提高预测准确性？");
                break;
            case "insight":
                questions.add("如何利用这些洞察？");
                questions.add("还有其他���现吗？");
                questions.add("这些洞察的价值是什么？");
                break;
            case "guidance":
                questions.add("具体应该怎么做？");
                questions.add("预期效果如何？");
                questions.add("有什么风险吗？");
                break;
            default:
                questions.add("能详细解释一下吗？");
                questions.add("有什么建议吗？");
                questions.add("还有其他相关信息吗？");
        }

        return questions.stream().limit(3).collect(java.util.stream.Collectors.toList());
    }

    /**
     * 调用AI
     */
    private String callAI(String prompt) {
        try {
            if (!aiConfig.isEnabled()) {
                return null;
            }

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
