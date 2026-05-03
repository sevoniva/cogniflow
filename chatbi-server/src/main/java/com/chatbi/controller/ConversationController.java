package com.chatbi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatbi.common.Result;
import com.chatbi.config.AiConfig;
import com.chatbi.entity.Metric;
import com.chatbi.entity.Synonym;
import com.chatbi.repository.DataSourceMapper;
import com.chatbi.repository.MetricMapper;
import com.chatbi.repository.SynonymMapper;
import com.chatbi.support.MetricSemanticMatcher;
import com.chatbi.service.MetricMatchingService;
import com.chatbi.service.AiQueryService;
import com.chatbi.service.AiModelService;
import com.chatbi.service.AccessAlertService;
import com.chatbi.service.BusinessInsightService;
import com.chatbi.service.QueryGovernanceService;
import com.chatbi.service.ConversationService;
import com.chatbi.service.EnterpriseChartCatalogService;
import com.chatbi.service.QueryExecutionService;
import com.chatbi.service.QueryResultAnalysisService;
import com.chatbi.service.SmartRecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 多轮对话控制器
 * 实现AI对话式查询
 */
@Slf4j
@Tag(name = "多轮对话", description = "多轮对话控制器")
@RestController
@RequestMapping("/api/conversation")
@RequiredArgsConstructor
public class ConversationController {

    private static final Map<String, List<String>> METRIC_KEYWORDS = createMetricKeywords();
    private static final int AMBIGUITY_SCORE_DELTA_THRESHOLD = 20;
    private static final int AMBIGUITY_SECOND_SCORE_THRESHOLD = 100;
    private static final double AMBIGUITY_SIMILARITY_THRESHOLD = 0.88;
    private static final double AMBIGUITY_SIMILARITY_DELTA_THRESHOLD = 0.03;
    private static final double FUZZY_MATCH_THRESHOLD = 0.87;
    private static final double TYPO_FUZZY_MATCH_THRESHOLD = 0.66;
    private static final double TYPO_FUZZY_GAP_THRESHOLD = 0.10;
    private static final double SECONDARY_METRIC_CONFIDENCE_THRESHOLD = 0.78;
    private static final Set<String> BLOCKED_ANALYTIC_TABLES = Set.of(
        "sys_user",
        "sys_role",
        "sys_permission",
        "sys_user_role",
        "sys_role_permission",
        "audit_logs",
        "query_history",
        "query_favorites",
        "query_result",
        "data_sources",
        "metrics",
        "synonyms",
        "ai_provider_settings",
        "ai_runtime_settings"
    );
    private static final List<String> CONTEXT_REFERENCE_TERMS = List.of(
        "这个指标",
        "该指标",
        "这个",
        "那个",
        "它",
        "上一个指标",
        "上一条",
        "刚才那个",
        "前面那个",
        "延续这个"
    );
    private static final List<String> EXPLICIT_TIME_REFERENCE_TERMS = List.of(
        "近30天",
        "最近30天",
        "近7天",
        "最近7天",
        "本季度",
        "上季度",
        "本月",
        "上月",
        "本周",
        "这周",
        "上周",
        "今日",
        "今天",
        "昨日",
        "昨天",
        "本年",
        "今年",
        "去年"
    );

    private final ConversationService conversationService;
    private final MetricMatchingService metricMatchingService;
    private final AiQueryService aiQueryService;
    private final AiModelService aiModelService;
    private final QueryExecutionService queryExecutionService;
    private final DataSourceMapper dataSourceMapper;
    private final MetricMapper metricMapper;
    private final SynonymMapper synonymMapper;
    private final SmartRecommendationService recommendationService;
    private final QueryResultAnalysisService analysisService;
    private final BusinessInsightService businessInsightService;
    private final EnterpriseChartCatalogService enterpriseChartCatalogService;
    private final AccessAlertService accessAlertService;
    private final AiConfig aiConfig;

    /**
     * 创建新对话
     */
    @Operation(summary = "创建新对话")
    @PostMapping("/create")
    public Result<Map<String, Object>> createConversation(@RequestBody Map<String, Object> request) {
        Long userId = request.get("userId") != null ?
            Long.parseLong(request.get("userId").toString()) : 1L;

        ConversationService.Conversation conversation = conversationService.createConversation(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("conversationId", conversation.getConversationId());
        response.put("createdAt", conversation.getCreatedAt());

        return Result.ok(response);
    }

    /**
     * 发送消息（多轮对话核心接口）
     */
    @Operation(summary = "发送消息（多轮对话核心接口）")
    @PostMapping("/message")
    public Result<Map<String, Object>> sendMessage(@RequestBody Map<String, Object> request) {
        String conversationId = (String) request.get("conversationId");
        String message = (String) request.get("message");
        Long userId = request.get("userId") != null ?
            Long.parseLong(request.get("userId").toString()) : 1L;

        if (message == null || message.trim().isEmpty()) {
            return Result.error("消息不能为空");
        }

        try {
            // 1. 如果没有conversationId，创建新对话
            if (conversationId == null || conversationId.isEmpty()) {
                ConversationService.Conversation conversation = conversationService.createConversation(userId);
                conversationId = conversation.getConversationId();
            }

            // 2. 添加用户消息
            conversationService.addUserMessage(conversationId, message);

            // 3. 判断是否是追问
            boolean isFollowUp = conversationService.isFollowUpQuestion(conversationId, message);

            // 4. 优先走业务指标匹配，确保对话查询稳定命中真实业务数据
            QueryExecution execution = resolveQuery(conversationId, message, isFollowUp, userId);
            enrichDiagnosisWithScenario(execution.diagnosis(), message, execution.candidateMetrics());

            // 5. 生成智能解读
            List<String> suggestions = execution.suggestions().isEmpty()
                ? generateSuggestions(message, execution.data(), conversationId)
                : execution.suggestions();
            String chartType = execution.chartType() != null ? execution.chartType() : recommendChartType(execution.data());
            Map<String, Object> aiStatus = buildAiStatus();
            InterpretationResult interpretationResult = execution.reply() != null
                ? new InterpretationResult(execution.reply(), execution.source())
                : buildInterpretation(message, execution.data(), isFollowUp, execution.metricName(), execution.source());
            String interpretation = interpretationResult.reply();
            String responseSource = interpretationResult.source();

            // 6. 添加助手消息
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("dataCount", execution.data().size());
            metadata.put("isFollowUp", isFollowUp);
            metadata.put("metricName", execution.metricName());
            metadata.put("querySource", responseSource);
            metadata.put("chartType", chartType);
            metadata.put("data", execution.data());
            metadata.put("suggestions", suggestions);
            metadata.put("candidateMetrics", execution.candidateMetrics());
            metadata.put("disambiguation", execution.disambiguation());
            metadata.put("aiStatus", aiStatus);
            metadata.put("diagnosis", execution.diagnosis());
            conversationService.addAssistantMessage(conversationId, interpretation, execution.sql(), metadata);

            // 7. 更新对话上下文
            conversationService.updateContext(conversationId, "lastQuery", message);
            conversationService.updateContext(conversationId, "lastSql", execution.sql());
            conversationService.updateContext(conversationId, "lastDataCount", execution.data().size());
            if (!"guided-discovery".equals(execution.source()) && !"overview".equals(execution.source())) {
                conversationService.updateContext(conversationId, "lastMetric", execution.metricName());
            }

            // 8. 记录查询（用于推荐算法）
            com.chatbi.entity.DataSource dataSource = getDefaultDataSource();
            recommendationService.recordQuery(userId, dataSource != null ? dataSource.getId() : 1L, message);

            // 10. 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("conversationId", conversationId);
            response.put("message", interpretation);
            response.put("sql", execution.sql());
            response.put("data", execution.data());
            response.put("dataCount", execution.data().size());
            response.put("isFollowUp", isFollowUp);
            response.put("metric", execution.metricName());
            response.put("source", responseSource);
            response.put("suggestions", suggestions);
            response.put("candidateMetrics", execution.candidateMetrics());
            response.put("disambiguation", execution.disambiguation());
            response.put("chartType", chartType);
            response.put("aiStatus", aiStatus);
            response.put("diagnosis", execution.diagnosis());

            return Result.ok(response);

        } catch (Exception e) {
            log.error("处理消息失败 - conversationId: {}, message: {}", conversationId, message, e);
            try {
                if (conversationId == null || conversationId.isBlank()) {
                    ConversationService.Conversation conversation = conversationService.createConversation(userId);
                    conversationId = conversation.getConversationId();
                }

                List<Map<String, Object>> overviewRows = safeOverviewRows();
                List<String> fallbackSuggestions = getDefaultSuggestions();
                String fallbackReply = "系统处理该问题时发生异常，已自动切换为数据概览，避免中断分析。你可以继续问："
                    + String.join("、", fallbackSuggestions);

                Map<String, Object> metadata = new LinkedHashMap<>();
                metadata.put("dataCount", overviewRows.size());
                metadata.put("isFollowUp", false);
                metadata.put("metricName", "数据概览");
                metadata.put("querySource", "guided-discovery");
                metadata.put("chartType", "bar");
                metadata.put("data", overviewRows);
                metadata.put("suggestions", fallbackSuggestions);
                metadata.put("candidateMetrics", List.of());
                metadata.put("disambiguation", false);
                metadata.put("aiStatus", buildAiStatus());
                metadata.put("recoveredFromError", true);
                metadata.put("diagnosis", createDiagnosis(
                    "CONVERSATION_EXCEPTION",
                    "对话处理链路出现异常，已自动切换数据概览保护模式。",
                    fallbackSuggestions,
                    true
                ));
                enrichDiagnosisWithScenario(metadata.get("diagnosis"), message, List.of());
                conversationService.addAssistantMessage(
                    conversationId,
                    fallbackReply,
                    "-- 对话链路异常，已降级为数据概览",
                    metadata
                );

                conversationService.updateContext(conversationId, "lastQuery", message);
                conversationService.updateContext(conversationId, "lastSql", "-- 对话链路异常，已降级为数据概览");
                conversationService.updateContext(conversationId, "lastDataCount", overviewRows.size());

                Map<String, Object> recovered = new LinkedHashMap<>();
                recovered.put("conversationId", conversationId);
                recovered.put("message", fallbackReply);
                recovered.put("sql", "-- 对话链路异常，已降级为数据概览");
                recovered.put("data", overviewRows);
                recovered.put("dataCount", overviewRows.size());
                recovered.put("isFollowUp", false);
                recovered.put("metric", "数据概览");
                recovered.put("source", "guided-discovery");
                recovered.put("suggestions", fallbackSuggestions);
                recovered.put("candidateMetrics", List.of());
                recovered.put("disambiguation", false);
                recovered.put("chartType", "bar");
                recovered.put("aiStatus", buildAiStatus());
                recovered.put("recoveredFromError", true);
                recovered.put("recoverReason", e.getMessage());
                recovered.put("diagnosis", createDiagnosis(
                    "CONVERSATION_EXCEPTION",
                    "对话处理链路出现异常，已自动切换数据概览保护模式。",
                    fallbackSuggestions,
                    true
                ));
                enrichDiagnosisWithScenario(recovered.get("diagnosis"), message, List.of());
                return Result.ok(recovered);
            } catch (Exception recoverError) {
                log.error("对话降级恢复失败 - conversationId: {}, message: {}", conversationId, message, recoverError);
                return Result.ok(buildHardFailSafeResponse(conversationId, userId, message, e, recoverError));
            }
        }
    }

    /**
     * 发送消息（流式输出）
     *
     * 通过 SSE 返回处理进度和最终结果，前端可实现打字机效果。
     */
    @Operation(summary = "发送消息（流式输出）")
    @PostMapping(value = "/message/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMessage(@RequestBody Map<String, Object> request) {
        SseEmitter emitter = new SseEmitter(120_000L);

        new Thread(() -> {
            try {
                String conversationId = (String) request.get("conversationId");
                String message = (String) request.get("message");
                Long userId = request.get("userId") != null ?
                    Long.parseLong(request.get("userId").toString()) : 1L;

                if (message == null || message.trim().isEmpty()) {
                    emitter.send(SseEmitter.event().name("error").data("{\"error\":\"消息不能为空\"}"));
                    emitter.complete();
                    return;
                }

                // 1. 创建对话
                emitter.send(SseEmitter.event().name("status").data("{\"step\":\"CREATING_CONVERSATION\"}"));
                if (conversationId == null || conversationId.isEmpty()) {
                    ConversationService.Conversation conversation = conversationService.createConversation(userId);
                    conversationId = conversation.getConversationId();
                }

                // 2. 添加用户消息
                emitter.send(SseEmitter.event().name("status").data("{\"step\":\"ANALYZING_INTENT\"}"));
                conversationService.addUserMessage(conversationId, message);

                // 3. 判断追问
                boolean isFollowUp = conversationService.isFollowUpQuestion(conversationId, message);

                // 4. 解析查询
                emitter.send(SseEmitter.event().name("status").data("{\"step\":\"GENERATING_SQL\"}"));
                QueryExecution execution = resolveQuery(conversationId, message, isFollowUp, userId);
                enrichDiagnosisWithScenario(execution.diagnosis(), message, execution.candidateMetrics());

                // 5. 生成智能解读
                emitter.send(SseEmitter.event().name("status").data("{\"step\":\"GENERATING_INTERPRETATION\"}"));
                List<String> suggestions = execution.suggestions().isEmpty()
                    ? generateSuggestions(message, execution.data(), conversationId)
                    : execution.suggestions();
                String chartType = execution.chartType() != null ? execution.chartType() : recommendChartType(execution.data());
                Map<String, Object> aiStatus = buildAiStatus();
                InterpretationResult interpretationResult = execution.reply() != null
                    ? new InterpretationResult(execution.reply(), execution.source())
                    : buildInterpretation(message, execution.data(), isFollowUp, execution.metricName(), execution.source());
                String interpretation = interpretationResult.reply();
                String responseSource = interpretationResult.source();

                // 6. 添加助手消息
                emitter.send(SseEmitter.event().name("status").data("{\"step\":\"FINALIZING\"}"));
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("dataCount", execution.data().size());
                metadata.put("isFollowUp", isFollowUp);
                metadata.put("metricName", execution.metricName());
                metadata.put("querySource", responseSource);
                metadata.put("chartType", chartType);
                metadata.put("data", execution.data());
                metadata.put("suggestions", suggestions);
                metadata.put("candidateMetrics", execution.candidateMetrics());
                metadata.put("disambiguation", execution.disambiguation());
                metadata.put("aiStatus", aiStatus);
                metadata.put("diagnosis", execution.diagnosis());
                conversationService.addAssistantMessage(conversationId, interpretation, execution.sql(), metadata);

                // 7. 更新对话上下文
                conversationService.updateContext(conversationId, "lastQuery", message);
                conversationService.updateContext(conversationId, "lastSql", execution.sql());
                conversationService.updateContext(conversationId, "lastDataCount", execution.data().size());
                if (!"guided-discovery".equals(execution.source()) && !"overview".equals(execution.source())) {
                    conversationService.updateContext(conversationId, "lastMetric", execution.metricName());
                }

                // 8. 记录查询
                com.chatbi.entity.DataSource dataSource = getDefaultDataSource();
                recommendationService.recordQuery(userId, dataSource != null ? dataSource.getId() : 1L, message);

                // 9. 发送最终结果
                Map<String, Object> response = new HashMap<>();
                response.put("conversationId", conversationId);
                response.put("message", interpretation);
                response.put("sql", execution.sql());
                response.put("data", execution.data());
                response.put("dataCount", execution.data().size());
                response.put("isFollowUp", isFollowUp);
                response.put("metric", execution.metricName());
                response.put("source", responseSource);
                response.put("suggestions", suggestions);
                response.put("candidateMetrics", execution.candidateMetrics());
                response.put("disambiguation", execution.disambiguation());
                response.put("chartType", chartType);
                response.put("aiStatus", aiStatus);
                response.put("diagnosis", execution.diagnosis());

                emitter.send(SseEmitter.event().name("result").data(Result.ok(response)));
                emitter.complete();

            } catch (Exception e) {
                log.error("流式处理消息失败", e);
                try {
                    emitter.send(SseEmitter.event().name("error").data(Result.error(e.getMessage())));
                } catch (Exception ex) {
                    log.error("发送流式错误事件失败", ex);
                }
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }

    /**
     * 获取对话历史
     */
    @Operation(summary = "获取对话历史")
    @GetMapping("/{conversationId}/history")
    public Result<List<ConversationService.Message>> getHistory(@PathVariable String conversationId) {
        ConversationService.Conversation conversation = conversationService.getConversation(conversationId);
        if (conversation == null) {
            return Result.error("对话不存在");
        }

        return Result.ok(conversation.getMessages());
    }

    /**
     * 获取对话列表
     */
    @Operation(summary = "获取对话列表")
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> getConversationList(@RequestParam(defaultValue = "1") Long userId) {
        List<Map<String, Object>> conversations = conversationService.getUserConversations(userId).stream()
            .map(conversation -> {
                Map<String, Object> summary = new HashMap<>();
                summary.put("id", conversation.getConversationId());
                summary.put("title", buildConversationTitle(conversation));
                summary.put("time", conversation.getUpdatedAt());
                summary.put("messageCount", conversation.getMessages().size());
                return summary;
            })
            .toList();

        return Result.ok(conversations);
    }

    /**
     * 删除对话
     */
    @Operation(summary = "删除对话")
    @DeleteMapping("/{conversationId}")
    public Result<Void> deleteConversation(@PathVariable String conversationId) {
        conversationService.deleteConversation(conversationId);
        return Result.ok();
    }

    /**
     * 获取查询建议
     */
    @Operation(summary = "获取查询建议")
    @GetMapping("/suggestions")
    public Result<List<String>> getSuggestions(@RequestParam(required = false) String conversationId) {
        List<String> suggestions = new ArrayList<>();

        if (conversationId != null && !conversationId.isEmpty()) {
            ConversationService.Conversation conversation = conversationService.getConversation(conversationId);
            if (conversation != null && !conversation.getMessages().isEmpty()) {
                // 基于对话历史生成建议
                suggestions = generateContextualSuggestions(conversation);
            }
        }

        // 如果没有对话历史，返回通用建议
        if (suggestions.isEmpty()) {
            suggestions = getDefaultSuggestions();
        }

        return Result.ok(suggestions);
    }

    /**
     * 获取对话能力说明与运行状态
     */
    @Operation(summary = "获取对话能力说明与运行状态")
    @GetMapping("/capabilities")
    public Result<Map<String, Object>> getCapabilities() {
        List<Metric> activeMetrics = metricMatchingService.getActiveMetrics();
        List<Synonym> synonyms = metricMatchingService.getAllSynonyms();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("ai", buildAiStatus());
        payload.put("metricCount", activeMetrics.size());
        payload.put("synonymCount", synonyms.size());
        payload.put("chartTypeCount", enterpriseChartCatalogService.getSummary().get("total"));
        payload.put("chartFamilies", enterpriseChartCatalogService.getFamilies());
        payload.put("chartVariants", enterpriseChartCatalogService.getVariants());
        payload.put("chartCatalog", enterpriseChartCatalogService.getCatalog());
        payload.put("featuredChartTypes", enterpriseChartCatalogService.getFeaturedTypes(24));
        payload.put("quickStartMetrics", activeMetrics.stream().map(Metric::getName).limit(12).toList());
        payload.put("fallbackPrompts", getDefaultSuggestions());
        payload.put("metrics", activeMetrics.stream().map(metric -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", metric.getName());
            item.put("definition", metric.getDefinition());
            item.put("examples", buildMetricExamples(metric.getName()));
            item.put("aliases", synonyms.stream()
                .filter(synonym -> metric.getName().equals(synonym.getStandardWord()))
                .findFirst()
                .map(Synonym::getAliases)
                .orElse(List.of()));
            return item;
        }).toList());
        payload.put("starterQuestions", getDefaultSuggestions());
        payload.put("overview", businessInsightService.getOverviewRows());
        payload.put("usageTips", List.of(
            "先说指标，再补充时间范围，例如：本月销售额、上月审批平均时长",
            "可以继续追问，例如：那华东呢？增长趋势如何？",
            "如果未启用外部大模型，系统会优先使用真实业务指标和同义词语义引擎"
        ));
        return Result.ok(payload);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 获取默认数据源
     */
    private com.chatbi.entity.DataSource getDefaultDataSource() {
        return dataSourceMapper.selectById(1L);
    }

    /**
     * 生成智能解读
     */
    private InterpretationResult buildInterpretation(String query, List<Map<String, Object>> data, boolean isFollowUp, String metricName, String source) {
        if (aiConfig.isRuntimeEnabled() && "business-insight".equals(source) && metricName != null && !metricName.isBlank()) {
            try {
                String aiReply = aiModelService.generateText(buildMetricInsightPrompt(query, metricName, data, isFollowUp));
                return new InterpretationResult(aiReply, "business-insight-ai");
            } catch (Exception ex) {
                log.warn("业务指标 AI 解读失败，降级为本地解读 - metric: {}, error: {}", metricName, ex.getMessage());
            }
        }

        return new InterpretationResult(generateInterpretation(query, data, isFollowUp, metricName), source);
    }

    private String generateInterpretation(String query, List<Map<String, Object>> data, boolean isFollowUp, String metricName) {
        if (data.isEmpty()) {
            return "未查询到相关数据";
        }

        StringBuilder interpretation = new StringBuilder();

        if (isFollowUp) {
            interpretation.append("根据您的追问，");
        }

        if (metricName != null && !metricName.isBlank()) {
            interpretation.append("已为您分析“").append(metricName).append("”，");
        }

        // 基础统计
        interpretation.append("查询到 ").append(data.size()).append(" 条数据");

        // 如果是聚合查询，提取关键数值
        if (data.size() == 1 && data.get(0).size() == 1) {
            Object value = data.get(0).values().iterator().next();
            interpretation.append("，结果为：").append(formatNumber(value));
        } else if (data.size() <= 10) {
            // 小数据集，可以详细描述
            interpretation.append("。");
        } else {
            // ��数据集，只描述概况
            interpretation.append("，以下是前 10 条记录。");
        }

        return interpretation.toString();
    }

    private String buildMetricInsightPrompt(String query, String metricName, List<Map<String, Object>> data, boolean isFollowUp) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是数据分析助手。请基于真实查询结果给出简洁专业的中文解读。");
        prompt.append("要求：1）先给一句结论；2）再给2到3条关键发现；3）不要编造不存在的数据；4）控制在180字以内；5）不要输出SQL。");
        if (isFollowUp) {
            prompt.append("当前问题是追问，需要承接上下文。");
        }
        prompt.append("\n\n用户问题：").append(query);
        prompt.append("\n指标名称：").append(metricName);
        prompt.append("\n查询结果：\n");

        data.stream().limit(8).forEach(row -> {
            prompt.append("- ");
            boolean first = true;
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                if (!first) {
                    prompt.append("，");
                }
                prompt.append(entry.getKey()).append("=").append(entry.getValue());
                first = false;
            }
            prompt.append("\n");
        });

        if (data.size() > 8) {
            prompt.append("- 其余结果省略，共 ").append(data.size()).append(" 条\n");
        }

        return prompt.toString();
    }

    /**
     * 生成推荐问题
     */
    private List<String> generateSuggestions(String query, List<Map<String, Object>> data, String conversationId) {
        List<String> recommendations = recommendationService.getNextStepRecommendations(query, data);
        if (recommendations == null || recommendations.isEmpty()) {
            return getDefaultSuggestions();
        }
        return recommendations.stream().filter(Objects::nonNull).distinct().limit(5).toList();
    }

    /**
     * 推荐图表类型
     */
    private String recommendChartType(List<Map<String, Object>> data) {
        if (data.isEmpty()) {
            return "table";
        }

        Map<String, Object> firstRow = data.get(0);
        int columnCount = firstRow.size();

        // 单值结果
        if (data.size() == 1 && columnCount == 1) {
            return "number";
        }

        // 时间序列数据
        if (firstRow.keySet().stream().anyMatch(k ->
            k.toString().contains("date") || k.toString().contains("time") ||
            k.toString().contains("month") || k.toString().contains("year"))) {
            return "line";
        }

        // 分类数据
        if (columnCount == 2) {
            // 如果第二列是数值，推荐柱状图或饼图
            Object secondValue = firstRow.values().toArray()[1];
            if (secondValue instanceof Number) {
                return data.size() <= 6 ? "pie" : "bar";
            }
        }

        // 多维数据
        if (columnCount > 2) {
            return "table";
        }

        return "bar"; // 默认柱状图
    }

    /**
     * 生成基于上下文的建议
     */
    private List<String> generateContextualSuggestions(ConversationService.Conversation conversation) {
        List<String> suggestions = new ArrayList<>();
        List<ConversationService.Message> messages = conversation.getMessages();

        if (messages.isEmpty()) {
            return getDefaultSuggestions();
        }

        // 获取最后一条用户消息
        ConversationService.Message lastMessage = null;
        for (int i = messages.size() - 1; i >= 0; i--) {
            if ("user".equals(messages.get(i).getRole())) {
                lastMessage = messages.get(i);
                break;
            }
        }

        if (lastMessage != null) {
            String content = lastMessage.getContent();
            suggestions = generateSuggestions(content, new ArrayList<>(), conversation.getConversationId());
        }

        return suggestions;
    }

    /**
     * 获取默认建议
     */
    private List<String> getDefaultSuggestions() {
        return List.of("先给我一个数据概览");
    }

    private List<Map<String, Object>> safeOverviewRows() {
        try {
            List<Map<String, Object>> rows = businessInsightService.getOverviewRows();
            if (rows != null && !rows.isEmpty()) {
                return rows;
            }
        } catch (Exception ex) {
            log.warn("获取数据概览失败 - {}", ex.getMessage());
        }
        return List.of();
    }

    private Map<String, Object> createOverviewRow(String metric, Object value, String unit) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("指标", metric);
        row.put("数值", value);
        row.put("单位", unit);
        return row;
    }

    private Map<String, Object> buildHardFailSafeResponse(
        String conversationId,
        Long userId,
        String message,
        Exception rootError,
        Exception recoverError
    ) {
        String finalConversationId = conversationId;
        if (finalConversationId == null || finalConversationId.isBlank()) {
            finalConversationId = "conv-fallback-" + Math.abs(ThreadLocalRandom.current().nextInt());
        }

        List<Map<String, Object>> overviewRows = safeOverviewRows();
        List<String> fallbackSuggestions = getDefaultSuggestions();
        Map<String, Object> aiStatus = buildAiStatus();

        // 无论会话持久化是否成功，都保证返回结构化可继续数据，避免前端出现阻断性报错。
        try {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("dataCount", overviewRows.size());
            metadata.put("isFollowUp", false);
            metadata.put("metricName", "数据概览");
            metadata.put("querySource", "guided-recovery");
            metadata.put("chartType", "bar");
            metadata.put("data", overviewRows);
            metadata.put("suggestions", fallbackSuggestions);
            metadata.put("candidateMetrics", List.of());
            metadata.put("disambiguation", false);
            metadata.put("aiStatus", aiStatus);
            metadata.put("recoveredFromError", true);
            metadata.put("hardRecovered", true);
            metadata.put("diagnosis", createDiagnosis(
                "CONVERSATION_HARD_RECOVERY",
                "对话链路连续异常，已进入保护模式并返回兜底数据概览。",
                fallbackSuggestions,
                true
            ));
            enrichDiagnosisWithScenario(metadata.get("diagnosis"), message, List.of());
            conversationService.addAssistantMessage(
                finalConversationId,
                "系统已进入保护模式，已返回数据概览并保留可继续追问建议。",
                "-- 对话链路二次恢复：返回本地兜底数据概览",
                metadata
            );
            conversationService.updateContext(finalConversationId, "lastQuery", message);
            conversationService.updateContext(finalConversationId, "lastSql", "-- 对话链路二次恢复：返回本地兜底数据概览");
            conversationService.updateContext(finalConversationId, "lastDataCount", overviewRows.size());
        } catch (Exception persistError) {
            log.warn("硬恢复持久化失败，继续返回可用响应 - conversationId: {}, error: {}", finalConversationId, persistError.getMessage());
        }

        Map<String, Object> recovered = new LinkedHashMap<>();
        recovered.put("conversationId", finalConversationId);
        recovered.put("message", "系统已进入保护模式，已返回数据概览并保留可继续追问建议。你可以继续问：" + String.join("、", fallbackSuggestions));
        recovered.put("sql", "-- 对话链路二次恢复：返回本地兜底数据概览");
        recovered.put("data", overviewRows);
        recovered.put("dataCount", overviewRows.size());
        recovered.put("isFollowUp", false);
        recovered.put("metric", "数据概览");
        recovered.put("source", "guided-recovery");
        recovered.put("suggestions", fallbackSuggestions);
        recovered.put("candidateMetrics", List.of());
        recovered.put("disambiguation", false);
        recovered.put("chartType", "bar");
        recovered.put("aiStatus", aiStatus);
        recovered.put("recoveredFromError", true);
        recovered.put("hardRecovered", true);
        recovered.put("recoverReason", rootError.getMessage());
        recovered.put("recoverFallbackReason", recoverError.getMessage());
        recovered.put("userId", userId);
        recovered.put("diagnosis", createDiagnosis(
            "CONVERSATION_HARD_RECOVERY",
            "对话链路连续异常，已进入保护模式并返回兜底数据概览。",
            fallbackSuggestions,
            true
        ));
        enrichDiagnosisWithScenario(recovered.get("diagnosis"), message, List.of());
        return recovered;
    }

    /**
     * 格式化数字
     */
    private String formatNumber(Object value) {
        if (value instanceof Number) {
            double num = ((Number) value).doubleValue();
            if (num >= 100000000) {
                return String.format("%.2f亿", num / 100000000);
            } else if (num >= 10000) {
                return String.format("%.2f万", num / 10000);
            } else {
                return String.format("%.2f", num);
            }
        }
        return value.toString();
    }

    private QueryExecution resolveQuery(String conversationId, String message, boolean isFollowUp, Long userId) {
        List<Metric> activeMetrics = metricMatchingService.getActiveMetrics();
        List<Synonym> synonyms = metricMatchingService.getAllSynonyms();
        boolean overviewIntent = isOverviewIntent(message);

        if (isGreetingIntent(message)) {
            return buildGuidedDiscovery(message, activeMetrics, synonyms, "已为你准备可直接使用的企业分析问法");
        }

        MetricMatchAnalysis matchAnalysis = analyzeMetricMatch(message, activeMetrics, synonyms);
        if (matchAnalysis.ambiguous() && !isFollowUp) {
            return buildMetricDisambiguation(message, matchAnalysis.candidateMetrics(), activeMetrics, synonyms);
        }

        Metric contextMetric = resolveMetricFromConversationContext(conversationId, activeMetrics);
        boolean contextReferenceIntent = isFollowUp && isContextReferenceIntent(message);
        boolean contextMetricReused = false;

        Metric metric = null;
        if (contextReferenceIntent && contextMetric != null) {
            metric = contextMetric;
            contextMetricReused = true;
        }

        if (metric == null) {
            metric = resolveMetric(conversationId, message, isFollowUp, activeMetrics, synonyms);
            if (metric != null && contextMetric != null && metric.getName().equals(contextMetric.getName()) && contextReferenceIntent) {
                contextMetricReused = true;
            }
        }

        if (metric != null) {
            String contextTimeReference = contextMetricReused ? resolveTimeReferenceFromConversationContext(conversationId) : null;
            List<String> explicitTimeCandidates = extractExplicitTimeReferences(message);
            String explicitTimeReference = selectExplicitTimeReference(explicitTimeCandidates, contextTimeReference);
            String secondaryMetricTimeReference = null;
            String reusedTimeReference = null;
            boolean contextTimeComparison = false;
            SecondaryMetricResolution secondaryMetricResolution = resolveSecondaryMetricForContextComparison(
                message,
                contextMetricReused ? metric : null,
                activeMetrics,
                synonyms,
                matchAnalysis,
                contextTimeReference
            );
            Metric secondaryMetric = secondaryMetricResolution.selectedMetric();
            boolean contextMetricComparison = shouldBuildContextMetricComparisonQuery(contextMetricReused, secondaryMetricResolution);
            if (contextMetricReused && !containsExplicitTimeReference(message)) {
                reusedTimeReference = contextTimeReference;
            }

            String baseQuery = message.contains(metric.getName()) ? message : metric.getName() + " " + message;
            String effectiveQuery = reusedTimeReference == null ? baseQuery : reusedTimeReference + " " + baseQuery;
            if (shouldBuildContextTimeComparisonQuery(contextMetricReused, message, contextTimeReference, explicitTimeReference)) {
                effectiveQuery = metric.getName() + " " + contextTimeReference + " 和 " + explicitTimeReference + " 对比 " + message;
                contextTimeComparison = true;
            }
            if (contextMetricComparison) {
                secondaryMetricTimeReference = resolveSecondaryMetricTimeReference(
                    message,
                    secondaryMetric,
                    synonyms,
                    contextTimeReference,
                    explicitTimeCandidates
                );
                if (contextTimeReference != null
                    && secondaryMetricTimeReference != null
                    && !contextTimeReference.equals(secondaryMetricTimeReference)) {
                    effectiveQuery = metric.getName()
                        + " "
                        + contextTimeReference
                        + " 和 "
                        + secondaryMetric.getName()
                        + " "
                        + secondaryMetricTimeReference
                        + " 对比 "
                        + message;
                    contextTimeComparison = true;
                    explicitTimeReference = secondaryMetricTimeReference;
                } else {
                    String timeSegment = "";
                    if (contextTimeComparison) {
                        timeSegment = contextTimeReference + " 和 " + explicitTimeReference + " ";
                    } else if (reusedTimeReference != null && !reusedTimeReference.isBlank()) {
                        timeSegment = reusedTimeReference + " ";
                    }
                    effectiveQuery = metric.getName() + " 和 " + secondaryMetric.getName() + " " + timeSegment + "对比 " + message;
                }
            }
            BusinessInsightService.QueryPlan plan = businessInsightService.queryMetric(metric, effectiveQuery);
            Map<String, Object> diagnosis = contextMetricReused
                ? createDiagnosis(
                    "CONTEXT_METRIC_REUSED",
                    buildContextReuseReason(
                        reusedTimeReference,
                        contextTimeComparison,
                        contextTimeReference,
                        explicitTimeReference,
                        contextMetricComparison,
                        secondaryMetric == null ? null : secondaryMetric.getName(),
                        secondaryMetricResolution.conflict()
                    ),
                    buildMetricExamples(metric.getName()),
                    false
                )
                : createDiagnosis("QUERY_EXECUTED", "已命中业务指标并返回结果。", List.of(), false);
            if (contextMetricReused) {
                enrichContextSlotEvidence(
                    diagnosis,
                    metric,
                    secondaryMetricResolution,
                    contextTimeReference,
                    explicitTimeReference,
                    contextTimeComparison,
                    secondaryMetricTimeReference
                );
            }
            return new QueryExecution(
                metric.getName(),
                plan.getSql(),
                plan.getData(),
                "business-insight",
                null,
                null,
                List.of(),
                List.of(metric.getName()),
                false,
                diagnosis
            );
        }

        if (aiConfig.isRuntimeEnabled() && overviewIntent) {
            return buildAiOverviewReply(message);
        }

        if (!aiConfig.isRuntimeEnabled()) {
            return buildGuidedDiscovery(message, activeMetrics, synonyms, "当前未启用外部大模型，已切换为数据概览和指标引导");
        }

        String contextPrompt = conversationService.buildContextPrompt(conversationId);
        String enhancedMessage = contextPrompt + "\n## 当前问题\n\n" + message;
        com.chatbi.entity.DataSource dataSource = getDefaultDataSource();

        if (dataSource == null) {
            throw new IllegalStateException("未配置数据源");
        }

        List<AiQueryService.TableSchema> schemas = filterBusinessSchemas(queryExecutionService.extractTableSchemas(dataSource));
        if (schemas.isEmpty()) {
            return buildGuidedDiscovery(message, activeMetrics, synonyms, "未发现可用于业务分析的数据表，已先返回数据概览");
        }

        try {
            String sql = aiQueryService.generateSqlWithLLM(enhancedMessage, schemas);
            QueryGovernanceService.ValidationResult validation = aiQueryService.validateSqlDetail(sql);
            if (!validation.valid()) {
                recordAccessAlertSafely(
                    userId,
                    "user-" + userId,
                    message,
                    sql,
                    validation.message(),
                    "conversation"
                );
                throw new IllegalStateException(validation.message());
            }

            return new QueryExecution(
                "自由分析",
                sql,
                queryExecutionService.executeQuery(dataSource, sql, userId),
                "llm",
                null,
                null,
                List.of(),
                List.of(),
                false,
                createDiagnosis("QUERY_EXECUTED", "外部模型查询执行完成。", List.of(), false)
            );
        } catch (Exception ex) {
            log.warn("对话外部大模型调用失败，降级为语义引导 - {}", ex.getMessage());
            if (overviewIntent) {
                return buildGuidedDiscovery(message, activeMetrics, synonyms, "该问题暂未命中可执行业务查询，已返回数据概览并附可执行问法");
            }
            return buildGuidedDiscovery(message, activeMetrics, synonyms, "外部大模型调用失败，已切换为数据概览和指标引导");
        }
    }

    private List<AiQueryService.TableSchema> filterBusinessSchemas(List<AiQueryService.TableSchema> schemas) {
        if (schemas == null || schemas.isEmpty()) {
            return List.of();
        }
        return schemas.stream()
            .filter(this::isBusinessSchema)
            .limit(40)
            .toList();
    }

    private boolean isBusinessSchema(AiQueryService.TableSchema schema) {
        if (schema == null || schema.getTableName() == null || schema.getTableName().isBlank()) {
            return false;
        }
        String tableName = schema.getTableName().toLowerCase(Locale.ROOT);
        if (tableName.startsWith("sys_") || tableName.startsWith("qrtz_") || tableName.startsWith("flyway_")) {
            return false;
        }
        if (tableName.startsWith("act_") || tableName.startsWith("undo_")) {
            return false;
        }
        return !BLOCKED_ANALYTIC_TABLES.contains(tableName);
    }

    private Metric resolveMetric(String conversationId, String message, boolean isFollowUp, List<Metric> activeMetrics, List<Synonym> synonyms) {
        if (activeMetrics.isEmpty()) {
            return null;
        }

        for (Metric metric : activeMetrics) {
            if (MetricSemanticMatcher.containsTerm(message, metric.getName())) {
                return metric;
            }
        }

        for (Synonym synonym : synonyms) {
            if (synonym.getAliases() == null) {
                continue;
            }
            for (String alias : synonym.getAliases()) {
                if (MetricSemanticMatcher.containsTerm(message, alias)) {
                    Metric mapped = findMetricByName(activeMetrics, synonym.getStandardWord());
                    if (mapped != null) {
                        return mapped;
                    }
                }
            }
        }

        if (isFollowUp) {
            ConversationService.Conversation conversation = conversationService.getConversation(conversationId);
            if (conversation != null) {
                Object lastMetric = conversation.getContext().get("lastMetric");
                if (lastMetric != null) {
                    Metric mapped = findMetricByName(activeMetrics, String.valueOf(lastMetric));
                    if (mapped != null) {
                        return mapped;
                    }
                }
            }
        }

        return resolveByFuzzySimilarity(message, activeMetrics, synonyms);
    }

    private Metric resolveMetricFromConversationContext(String conversationId, List<Metric> activeMetrics) {
        ConversationService.Conversation conversation = conversationService.getConversation(conversationId);
        if (conversation == null) {
            return null;
        }
        Object lastMetric = conversation.getContext().get("lastMetric");
        if (lastMetric == null) {
            return null;
        }
        return findMetricByName(activeMetrics, String.valueOf(lastMetric));
    }

    private String resolveTimeReferenceFromConversationContext(String conversationId) {
        ConversationService.Conversation conversation = conversationService.getConversation(conversationId);
        if (conversation == null) {
            return null;
        }
        Object lastQuery = conversation.getContext().get("lastQuery");
        if (lastQuery == null) {
            return null;
        }
        return extractExplicitTimeReference(String.valueOf(lastQuery));
    }

    private boolean containsExplicitTimeReference(String message) {
        return !extractExplicitTimeReferences(message).isEmpty();
    }

    private String extractExplicitTimeReference(String message) {
        List<String> timeCandidates = extractExplicitTimeReferences(message);
        return timeCandidates.isEmpty() ? null : timeCandidates.get(0);
    }

    private List<String> extractExplicitTimeReferences(String message) {
        List<TimeReferenceHit> hits = extractExplicitTimeReferenceHits(message);
        if (hits.isEmpty()) {
            return List.of();
        }
        return hits.stream().map(TimeReferenceHit::value).toList();
    }

    private List<TimeReferenceHit> extractExplicitTimeReferenceHits(String message) {
        if (message == null || message.isBlank()) {
            return List.of();
        }
        LinkedHashMap<String, Integer> earliestPositions = new LinkedHashMap<>();
        for (String term : EXPLICIT_TIME_REFERENCE_TERMS) {
            String normalized = normalizeTimeReferenceTerm(term);
            if (!MetricSemanticMatcher.containsTerm(message, term)) {
                continue;
            }
            int index = message.indexOf(term);
            if (index < 0) {
                index = Integer.MAX_VALUE / 2;
            }
            Integer existing = earliestPositions.get(normalized);
            if (existing == null || index < existing) {
                earliestPositions.put(normalized, index);
            }
        }
        if (earliestPositions.isEmpty()) {
            return List.of();
        }
        return earliestPositions.entrySet().stream()
            .map(entry -> new TimeReferenceHit(entry.getKey(), entry.getValue()))
            .sorted(Comparator.comparingInt(TimeReferenceHit::position))
            .toList();
    }

    private String selectExplicitTimeReference(List<String> explicitTimeCandidates, String contextTimeReference) {
        if (explicitTimeCandidates == null || explicitTimeCandidates.isEmpty()) {
            return null;
        }
        if (contextTimeReference == null || contextTimeReference.isBlank()) {
            return explicitTimeCandidates.get(0);
        }
        for (String candidate : explicitTimeCandidates) {
            if (!contextTimeReference.equals(candidate)) {
                return candidate;
            }
        }
        return explicitTimeCandidates.get(0);
    }

    private String normalizeTimeReferenceTerm(String term) {
        if ("今天".equals(term)) {
            return "今日";
        }
        if ("昨天".equals(term)) {
            return "昨日";
        }
        if ("这周".equals(term)) {
            return "本周";
        }
        if ("最近7天".equals(term)) {
            return "近7天";
        }
        if ("最近30天".equals(term)) {
            return "近30天";
        }
        return term;
    }

    private String resolveSecondaryMetricTimeReference(
        String message,
        Metric secondaryMetric,
        List<Synonym> synonyms,
        String contextTimeReference,
        List<String> explicitTimeCandidates
    ) {
        if (secondaryMetric == null) {
            return selectExplicitTimeReference(explicitTimeCandidates, contextTimeReference);
        }
        List<TimeReferenceHit> hits = extractExplicitTimeReferenceHits(message);
        if (hits.isEmpty()) {
            return null;
        }
        int secondaryMetricPos = locateSecondaryMetricMention(message, secondaryMetric, synonyms);
        if (secondaryMetricPos < 0) {
            return selectExplicitTimeReference(explicitTimeCandidates, contextTimeReference);
        }
        TimeReferenceHit nearest = hits.stream()
            .min(
                Comparator
                    .comparingInt((TimeReferenceHit hit) -> Math.abs(hit.position() - secondaryMetricPos))
                    .thenComparingInt(hit -> hit.position() >= secondaryMetricPos ? 0 : 1)
                    .thenComparingInt(TimeReferenceHit::position)
            )
            .orElse(hits.get(0));
        if (contextTimeReference != null && contextTimeReference.equals(nearest.value())) {
            return hits.stream()
                .map(TimeReferenceHit::value)
                .filter(value -> !contextTimeReference.equals(value))
                .findFirst()
                .orElse(nearest.value());
        }
        return nearest.value();
    }

    private int locateSecondaryMetricMention(String message, Metric secondaryMetric, List<Synonym> synonyms) {
        if (message == null || secondaryMetric == null) {
            return -1;
        }
        int namePos = message.indexOf(secondaryMetric.getName());
        if (namePos >= 0) {
            return namePos;
        }
        if (synonyms == null || synonyms.isEmpty()) {
            return -1;
        }
        for (Synonym synonym : synonyms) {
            if (!secondaryMetric.getName().equals(synonym.getStandardWord()) || synonym.getAliases() == null) {
                continue;
            }
            for (String alias : synonym.getAliases()) {
                int aliasPos = message.indexOf(alias);
                if (aliasPos >= 0) {
                    return aliasPos;
                }
            }
        }
        return -1;
    }

    private String buildContextReuseReason(String reusedTimeReference) {
        return buildContextReuseReason(reusedTimeReference, false, null, null, false, null, false);
    }

    private String buildContextReuseReason(
        String reusedTimeReference,
        boolean contextTimeComparison,
        String contextTimeReference,
        String explicitTimeReference,
        boolean contextMetricComparison,
        String comparisonMetricName,
        boolean secondaryMetricConflict
    ) {
        if (secondaryMetricConflict) {
            return "已根据上文指标继续分析，但检测到多个候选对比指标，请先明确需要对比的指标后继续。";
        }
        if (contextMetricComparison && comparisonMetricName != null && !comparisonMetricName.isBlank()) {
            if (contextTimeComparison
                && contextTimeReference != null
                && !contextTimeReference.isBlank()
                && explicitTimeReference != null
                && !explicitTimeReference.isBlank()) {
                return "已根据上文指标继续分析，并按指标“"
                    + comparisonMetricName
                    + "”与上文指标在时间范围“"
                    + contextTimeReference
                    + " vs "
                    + explicitTimeReference
                    + "”执行对比。"
                    + "如需切换指标请直接说出新的指标名称。";
            }
            return "已根据上文指标继续分析，并按指标“" + comparisonMetricName + "”执行对比。"
                + "如需切换指标请直接说出新的指标名称。";
        }
        if (contextTimeComparison
            && contextTimeReference != null
            && !contextTimeReference.isBlank()
            && explicitTimeReference != null
            && !explicitTimeReference.isBlank()) {
            return "已根据上文指标继续分析，并按时间范围“" + contextTimeReference + " vs " + explicitTimeReference + "”执行对比。"
                + "如需切换指标请直接说出新的指标名称。";
        }
        if (reusedTimeReference == null || reusedTimeReference.isBlank()) {
            return "已根据上文指标继续分析，如需切换指标请直接说出新的指标名称。";
        }
        return "已根据上文指标继续分析，并沿用时间范围“" + reusedTimeReference + "”。如需切换指标请直接说出新的指标名称。";
    }

    private SecondaryMetricResolution resolveSecondaryMetricForContextComparison(
        String message,
        Metric contextMetric,
        List<Metric> activeMetrics,
        List<Synonym> synonyms,
        MetricMatchAnalysis matchAnalysis,
        String contextTimeReference
    ) {
        if (contextMetric == null || message == null || message.isBlank()) {
            return new SecondaryMetricResolution(null, List.of(), false, "none", 0D, null, List.of());
        }
        boolean explicitComparisonIntent = containsAny(message, List.of("对比", "比较", "差异", "vs", "VS"));
        List<Metric> metricCandidates = new ArrayList<>();
        String source = "none";
        if (matchAnalysis != null && matchAnalysis.candidateMetrics() != null) {
            for (String candidate : matchAnalysis.candidateMetrics()) {
                if (!contextMetric.getName().equals(candidate)) {
                    Metric mapped = findMetricByName(activeMetrics, candidate);
                    if (mapped != null) {
                        metricCandidates.add(mapped);
                    }
                }
            }
            if (!metricCandidates.isEmpty()) {
                source = "semantic-candidates";
            }
        }
        if (metricCandidates.isEmpty()) {
            for (Metric item : activeMetrics) {
                if (!contextMetric.getName().equals(item.getName())
                    && MetricSemanticMatcher.containsTerm(message, item.getName())) {
                    metricCandidates.add(item);
                }
            }
            if (!metricCandidates.isEmpty()) {
                source = "direct-metric";
            }
        }
        if (metricCandidates.isEmpty()) {
            for (Synonym synonym : synonyms) {
                if (synonym.getAliases() == null || synonym.getStandardWord() == null) {
                    continue;
                }
                Metric mapped = findMetricByName(activeMetrics, synonym.getStandardWord());
                if (mapped == null || contextMetric.getName().equals(mapped.getName())) {
                    continue;
                }
                for (String alias : synonym.getAliases()) {
                    if (MetricSemanticMatcher.containsTerm(message, alias)) {
                        metricCandidates.add(mapped);
                        break;
                    }
                }
            }
            if (!metricCandidates.isEmpty()) {
                source = "synonym";
            }
        }
        List<Metric> uniqueCandidates = metricCandidates.stream()
            .collect(LinkedHashMap<String, Metric>::new, (acc, item) -> acc.putIfAbsent(item.getName(), item), LinkedHashMap::putAll)
            .values()
            .stream()
            .toList();
        List<Map<String, Object>> rankedCandidates = buildSecondaryMetricRankings(
            message,
            uniqueCandidates,
            synonyms,
            contextTimeReference
        );
        boolean implicitComparisonIntent = !explicitComparisonIntent
            && isImplicitComparisonIntent(message, !uniqueCandidates.isEmpty());
        if (!explicitComparisonIntent && !implicitComparisonIntent) {
            return new SecondaryMetricResolution(null, List.of(), false, "none", 0D, null, List.of());
        }
        if (uniqueCandidates.isEmpty()) {
            return new SecondaryMetricResolution(null, List.of(), false, "none", 0D, null, List.of());
        }
        if (uniqueCandidates.size() == 1) {
            Metric selectedMetric = uniqueCandidates.get(0);
            boolean explicitMetricNameHit = MetricSemanticMatcher.containsTerm(message, selectedMetric.getName());
            double confidence = explicitComparisonIntent
                ? 0.92D
                : (explicitMetricNameHit ? 0.82D : 0.74D);
            if (confidence < SECONDARY_METRIC_CONFIDENCE_THRESHOLD) {
                return new SecondaryMetricResolution(
                    null,
                    List.of(selectedMetric.getName()),
                    true,
                    source,
                    confidence,
                    "识别到候选对比指标，但当前语义置信度不足，请明确说明是否需要对比。",
                    rankedCandidates
                );
            }
            return new SecondaryMetricResolution(
                selectedMetric,
                List.of(selectedMetric.getName()),
                false,
                source,
                confidence,
                null,
                rankedCandidates
            );
        }
        Metric prioritizedMetric = resolvePrioritizedSecondaryMetric(
            message,
            uniqueCandidates,
            synonyms,
            contextTimeReference
        );
        if (prioritizedMetric != null) {
            return new SecondaryMetricResolution(
                prioritizedMetric,
                uniqueCandidates.stream().map(Metric::getName).limit(4).toList(),
                false,
                source + "-priority",
                0.86D,
                "检测到多指标多时间并列表达，已按连接词后首个指标优先级执行。",
                rankedCandidates
            );
        }
        String candidatesText = uniqueCandidates.stream().map(Metric::getName).limit(3).collect(Collectors.joining("、"));
        return new SecondaryMetricResolution(
            null,
            uniqueCandidates.stream().map(Metric::getName).limit(4).toList(),
            true,
            source,
            0.35D,
            "检测到多个候选对比指标：" + candidatesText,
            rankedCandidates
        );
    }

    private List<Map<String, Object>> buildSecondaryMetricRankings(
        String message,
        List<Metric> candidates,
        List<Synonym> synonyms,
        String contextTimeReference
    ) {
        if (message == null || message.isBlank() || candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        int connectorIndex = locatePrimaryComparisonConnector(message);
        List<TimeReferenceHit> timeHits = extractExplicitTimeReferenceHits(message);
        int firstTimeAfterConnector = timeHits.stream()
            .filter(hit -> connectorIndex >= 0 && hit.position() > connectorIndex)
            .filter(hit -> contextTimeReference == null || !contextTimeReference.equals(hit.value()))
            .mapToInt(TimeReferenceHit::position)
            .findFirst()
            .orElse(Integer.MAX_VALUE);

        List<Map<String, Object>> rankings = new ArrayList<>();
        for (Metric candidate : candidates) {
            int position = locateSecondaryMetricMention(message, candidate, synonyms);
            int score = 0;
            List<String> reasons = new ArrayList<>();
            if (position >= 0) {
                score += 20;
                reasons.add("命中指标词");
            }
            if (connectorIndex >= 0 && position > connectorIndex) {
                score += 20;
                reasons.add("位于比较连接词后");
            }
            if (firstTimeAfterConnector < Integer.MAX_VALUE && position >= 0 && position < firstTimeAfterConnector) {
                score += 15;
                reasons.add("位于首时间槽位之前");
            }
            if (containsAny(message, List.of("以及", "还有")) && position >= 0) {
                score += 10;
                reasons.add("命中扩展连接词场景");
            }
            if (!timeHits.isEmpty() && position >= 0) {
                int nearestDistance = timeHits.stream()
                    .mapToInt(hit -> Math.abs(hit.position() - position))
                    .min()
                    .orElse(Integer.MAX_VALUE);
                if (nearestDistance <= 8) {
                    score += 15;
                    reasons.add("与时间词距离近");
                } else if (nearestDistance <= 16) {
                    score += 8;
                    reasons.add("与时间词距离中等");
                }
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("metric", candidate.getName());
            item.put("score", score);
            item.put("position", position);
            item.put("reason", reasons.isEmpty() ? "候选基础命中" : String.join(" + ", reasons));
            rankings.add(item);
        }

        rankings.sort((left, right) -> {
            int scoreCompare = Integer.compare((int) right.get("score"), (int) left.get("score"));
            if (scoreCompare != 0) {
                return scoreCompare;
            }
            int lp = (int) left.get("position");
            int rp = (int) right.get("position");
            if (lp < 0 && rp < 0) {
                return 0;
            }
            if (lp < 0) {
                return 1;
            }
            if (rp < 0) {
                return -1;
            }
            return Integer.compare(lp, rp);
        });
        return rankings.stream().limit(4).toList();
    }

    private Metric resolvePrioritizedSecondaryMetric(
        String message,
        List<Metric> candidates,
        List<Synonym> synonyms,
        String contextTimeReference
    ) {
        if (message == null || message.isBlank() || candidates == null || candidates.size() < 2) {
            return null;
        }
        List<TimeReferenceHit> timeHits = extractExplicitTimeReferenceHits(message);
        if (timeHits.size() < 2) {
            return null;
        }
        int connectorIndex = locatePrimaryComparisonConnector(message);
        if (connectorIndex < 0) {
            return null;
        }
        int segmentStart = connectorIndex + 1;
        int segmentEnd = locateSecondarySegmentEnd(message, segmentStart);
        String primarySegment = message.substring(segmentStart, segmentEnd);
        List<Metric> segmentCandidates = candidates.stream()
            .filter(candidate -> metricAppearsInSegment(primarySegment, candidate, synonyms))
            .toList();
        if (segmentCandidates.size() != 1) {
            if (containsAny(message, List.of("分别", "同时", "一并"))) {
                Metric orderedMetric = resolvePrioritizedMetricBySequence(
                    message,
                    candidates,
                    synonyms,
                    contextTimeReference,
                    connectorIndex,
                    timeHits
                );
                if (orderedMetric != null) {
                    return orderedMetric;
                }
            }
            Metric weightedMetric = resolvePrioritizedMetricByConnectorWeights(
                message,
                candidates,
                synonyms,
                contextTimeReference,
                connectorIndex,
                timeHits
            );
            if (weightedMetric != null) {
                return weightedMetric;
            }
            return null;
        }
        Metric prioritized = segmentCandidates.get(0);
        int metricPos = locateSecondaryMetricMention(message, prioritized, synonyms);
        if (metricPos < 0) {
            return null;
        }
        int nearestDistance = timeHits.stream()
            .mapToInt(hit -> Math.abs(hit.position() - metricPos))
            .min()
            .orElse(Integer.MAX_VALUE);
        if (nearestDistance > 10) {
            return null;
        }
        String secondaryTime = resolveSecondaryMetricTimeReference(
            message,
            prioritized,
            synonyms,
            contextTimeReference,
            timeHits.stream().map(TimeReferenceHit::value).toList()
        );
        return secondaryTime == null ? null : prioritized;
    }

    private Metric resolvePrioritizedMetricByConnectorWeights(
        String message,
        List<Metric> candidates,
        List<Synonym> synonyms,
        String contextTimeReference,
        int connectorIndex,
        List<TimeReferenceHit> timeHits
    ) {
        if (message == null
            || candidates == null
            || candidates.size() < 2
            || timeHits == null
            || timeHits.size() < 2
            || !containsAny(message, List.of("以及", "还有"))) {
            return null;
        }
        List<Metric> rankedByPosition = candidates.stream()
            .sorted(Comparator.comparingInt(metric -> {
                int pos = locateSecondaryMetricMention(message, metric, synonyms);
                return pos < 0 ? Integer.MAX_VALUE : pos;
            }))
            .filter(metric -> locateSecondaryMetricMention(message, metric, synonyms) > connectorIndex)
            .toList();
        if (rankedByPosition.isEmpty()) {
            return null;
        }
        Metric prioritized = rankedByPosition.get(0);
        String secondaryTime = resolveSecondaryMetricTimeReference(
            message,
            prioritized,
            synonyms,
            contextTimeReference,
            timeHits.stream().map(TimeReferenceHit::value).toList()
        );
        return secondaryTime == null ? null : prioritized;
    }

    private Metric resolvePrioritizedMetricBySequence(
        String message,
        List<Metric> candidates,
        List<Synonym> synonyms,
        String contextTimeReference,
        int connectorIndex,
        List<TimeReferenceHit> timeHits
    ) {
        if (message == null || candidates == null || candidates.isEmpty() || timeHits == null || timeHits.isEmpty()) {
            return null;
        }
        TimeReferenceHit firstHit = timeHits.stream()
            .filter(hit -> hit.position() > connectorIndex)
            .filter(hit -> contextTimeReference == null || !contextTimeReference.equals(hit.value()))
            .findFirst()
            .orElseGet(() -> timeHits.stream().filter(hit -> hit.position() > connectorIndex).findFirst().orElse(null));
        if (firstHit == null) {
            return null;
        }
        int segmentEnd = Math.min(message.length(), firstHit.position() + firstHit.value().length());
        if (segmentEnd <= connectorIndex + 1) {
            return null;
        }
        String sequenceSegment = message.substring(connectorIndex + 1, segmentEnd);
        List<Metric> sequenceCandidates = candidates.stream()
            .filter(candidate -> metricAppearsInSegment(sequenceSegment, candidate, synonyms))
            .toList();
        if (sequenceCandidates.size() != 1) {
            return null;
        }
        Metric prioritized = sequenceCandidates.get(0);
        String secondaryTime = resolveSecondaryMetricTimeReference(
            message,
            prioritized,
            synonyms,
            contextTimeReference,
            timeHits.stream().map(TimeReferenceHit::value).toList()
        );
        return secondaryTime == null ? null : prioritized;
    }

    private int locatePrimaryComparisonConnector(String message) {
        int position = Integer.MAX_VALUE;
        for (String connector : List.of("和", "与", "跟")) {
            int index = message.indexOf(connector);
            if (index >= 0 && index < position) {
                position = index;
            }
        }
        return position == Integer.MAX_VALUE ? -1 : position;
    }

    private int locateSecondarySegmentEnd(String message, int start) {
        int end = message.length();
        for (String marker : List.of("或", "以及", "并且", "还有", "同时", "，", ",", "。", "；", ";")) {
            int markerIndex = message.indexOf(marker, start);
            if (markerIndex >= 0 && markerIndex < end) {
                end = markerIndex;
            }
        }
        return end;
    }

    private boolean metricAppearsInSegment(String segment, Metric metric, List<Synonym> synonyms) {
        if (segment == null || segment.isBlank() || metric == null) {
            return false;
        }
        if (MetricSemanticMatcher.containsTerm(segment, metric.getName())) {
            return true;
        }
        if (synonyms == null || synonyms.isEmpty()) {
            return false;
        }
        for (Synonym synonym : synonyms) {
            if (!metric.getName().equals(synonym.getStandardWord()) || synonym.getAliases() == null) {
                continue;
            }
            for (String alias : synonym.getAliases()) {
                if (MetricSemanticMatcher.containsTerm(segment, alias)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean shouldBuildContextTimeComparisonQuery(
        boolean contextMetricReused,
        String message,
        String contextTimeReference,
        String explicitTimeReference
    ) {
        return contextMetricReused
            && contextTimeReference != null
            && explicitTimeReference != null
            && !contextTimeReference.equals(explicitTimeReference)
            && containsAny(message, List.of("对比", "比较", "差异"));
    }

    private boolean shouldBuildContextMetricComparisonQuery(
        boolean contextMetricReused,
        SecondaryMetricResolution resolution
    ) {
        return contextMetricReused
            && resolution != null
            && resolution.selectedMetric() != null
            && !resolution.conflict()
            && resolution.confidence() >= SECONDARY_METRIC_CONFIDENCE_THRESHOLD;
    }

    private boolean isImplicitComparisonIntent(String message, boolean hasSecondaryMetricCandidate) {
        if (message == null || message.isBlank()) {
            return false;
        }
        boolean connectorHit = containsAny(message, List.of("和", "与", "跟"));
        boolean questionHint = containsAny(message, List.of("如何", "怎么样", "咋样", "吗", "哪个", "谁更"))
            || message.contains("？")
            || message.contains("?");
        boolean coordinationHint = containsAny(message, List.of("一起", "同时", "分别", "并列", "同看", "一并", "放一起"));
        boolean compactTimePairHint = containsAny(message, List.of("本周上周", "本月上月", "今天昨天", "今年去年", "上周本周", "上月本月", "去年今年"));
        return connectorHit && (questionHint || coordinationHint || compactTimePairHint || hasSecondaryMetricCandidate);
    }

    private void enrichContextSlotEvidence(
        Map<String, Object> diagnosis,
        Metric primaryMetric,
        SecondaryMetricResolution secondaryMetricResolution,
        String contextTimeReference,
        String explicitTimeReference,
        boolean contextTimeComparison,
        String secondaryMetricTimeReference
    ) {
        if (diagnosis == null || primaryMetric == null) {
            return;
        }
        Map<String, Object> slotEvidence = new LinkedHashMap<>();
        slotEvidence.put("primaryMetric", Map.of(
            "value", primaryMetric.getName(),
            "source", "conversation-context",
            "confidence", 1.0
        ));
        Map<String, Object> secondarySlot = new LinkedHashMap<>();
        secondarySlot.put("value", secondaryMetricResolution.selectedMetric() == null ? null : secondaryMetricResolution.selectedMetric().getName());
        secondarySlot.put("source", secondaryMetricResolution.source());
        secondarySlot.put("confidence", secondaryMetricResolution.confidence());
        secondarySlot.put("conflict", secondaryMetricResolution.conflict());
        secondarySlot.put("candidates", secondaryMetricResolution.candidates());
        secondarySlot.put("reason", secondaryMetricResolution.reason());
        secondarySlot.put("rankedCandidates", secondaryMetricResolution.rankedCandidates());
        secondarySlot.put("timeReference", secondaryMetricTimeReference);
        slotEvidence.put("secondaryMetric", secondarySlot);
        Map<String, Object> contextTimeSlot = new LinkedHashMap<>();
        contextTimeSlot.put("value", contextTimeReference);
        contextTimeSlot.put("used", contextTimeReference != null);
        slotEvidence.put("timeContext", contextTimeSlot);
        Map<String, Object> explicitTimeSlot = new LinkedHashMap<>();
        explicitTimeSlot.put("value", explicitTimeReference);
        explicitTimeSlot.put("used", explicitTimeReference != null);
        slotEvidence.put("timeExplicit", explicitTimeSlot);
        slotEvidence.put("timeComparison", contextTimeComparison);
        diagnosis.put("slotEvidence", slotEvidence);
        diagnosis.put("slotConflict", secondaryMetricResolution.conflict());
        if (secondaryMetricResolution.conflict()) {
            @SuppressWarnings("unchecked")
            List<String> actions = new ArrayList<>((List<String>) diagnosis.getOrDefault("actions", List.of()));
            actions.add(0, "请明确对比指标（候选：" + String.join("、", secondaryMetricResolution.candidates()) + "）");
            diagnosis.put("actions", actions.stream().filter(item -> item != null && !item.isBlank()).distinct().limit(4).toList());
        }
    }

    private boolean isContextReferenceIntent(String message) {
        return containsAny(message, CONTEXT_REFERENCE_TERMS);
    }

    private void recordAccessAlertSafely(Long userId, String username, String queryText, String sql, String reason, String scene) {
        try {
            accessAlertService.recordBlockedAccess(userId, username, queryText, sql, reason, scene);
        } catch (Exception ex) {
            log.warn("记录越权告警失败 - userId: {}, reason: {}", userId, ex.getMessage());
        }
    }

    private List<Metric> getActiveMetrics() {
        LambdaQueryWrapper<Metric> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Metric::getStatus, "active");
        return metricMapper.selectList(wrapper);
    }

    private QueryExecution buildGuidedDiscovery(String message, List<Metric> activeMetrics, List<Synonym> synonyms, String reason) {
        List<String> candidateMetrics = recommendMetricNames(message, activeMetrics, synonyms);
        List<String> suggestedQuestions = buildGuidedSuggestions(message, candidateMetrics);
        Map<String, Object> diagnosis = createDiagnosis(
            "METRIC_NOT_RECOGNIZED",
            reason,
            suggestedQuestions,
            true
        );
        enrichDiagnosisWithScenario(diagnosis, message, candidateMetrics);

        StringBuilder reply = new StringBuilder();
        if (isGreetingIntent(message)) {
            reply.append("我是 ChatBI。");
        } else if (isOverviewIntent(message)) {
            reply.append("已先返回数据概览。");
        } else {
            reply.append(reason).append("。");
        }
        reply.append("你可以直接这样问：")
            .append(String.join("、", suggestedQuestions));
        if (!aiConfig.isRuntimeEnabled()) {
            reply.append("。当前外部大模型未启用，如需真实调用 Kimi，请到管理后台的 AI 设置页补充配置。");
        }

        return new QueryExecution(
            "数据概览",
            "-- 未识别到明确业务指标，返回数据概览",
            businessInsightService.getOverviewRows(),
            "guided-discovery",
            reply.toString(),
            "bar",
            suggestedQuestions,
            candidateMetrics,
            false,
            diagnosis
        );
    }

    private QueryExecution buildMetricDisambiguation(String message, List<String> candidateMetrics, List<Metric> activeMetrics, List<Synonym> synonyms) {
        List<String> targetMetrics = (candidateMetrics == null || candidateMetrics.isEmpty())
            ? recommendMetricNames(message, activeMetrics, synonyms)
            : candidateMetrics;
        List<String> suggestions = buildGuidedSuggestions(message, targetMetrics);
        Map<String, Object> diagnosis = createDiagnosis(
            "AMBIGUOUS_METRIC",
            "检测到多个高相似指标，需要先澄清分析口径。",
            suggestions,
            true
        );
        enrichDiagnosisWithScenario(diagnosis, message, targetMetrics);
        String metricHints = targetMetrics.isEmpty() ? "请补充明确的业务指标名称" : String.join("、", targetMetrics);
        String reply = "识别到多个可能指标：" + metricHints + "。请先明确一个指标后我再继续分析。"
            + "你可以直接这样问：" + String.join("、", suggestions);

        return new QueryExecution(
            "数据概览",
            "-- 语义歧义澄清：检测到多个高相似指标",
            businessInsightService.getOverviewRows(),
            "guided-disambiguation",
            reply,
            "bar",
            suggestions,
            targetMetrics,
            true,
            diagnosis
        );
    }

    private QueryExecution buildAiOverviewReply(String message) {
        List<Map<String, Object>> overviewRows = businessInsightService.getOverviewRows();
        String reply;
        try {
            reply = aiModelService.generateText(buildOverviewPrompt(message, overviewRows));
        } catch (Exception ex) {
            log.warn("数据概览 AI 解读失败，降级为数据概览引导 - {}", ex.getMessage());
            return buildGuidedDiscovery(message, metricMatchingService.getActiveMetrics(), metricMatchingService.getAllSynonyms(), "外部大模型生成数据摘要失败，已切换为数据概览");
        }

        return new QueryExecution(
            "数据概览",
            "-- 基于数据概览生成 AI 解读",
            overviewRows,
            "llm",
            reply,
            "bar",
            getDefaultSuggestions(),
            List.of("数据概览"),
            false,
            createDiagnosis(
                "OVERVIEW_AI_REPLY",
                "已基于数据概览生成 AI 解读。",
                getDefaultSuggestions(),
                false
            )
        );
    }

    private String buildOverviewPrompt(String message, List<Map<String, Object>> overviewRows) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是数据分析助手。请基于以下真实数据指标，回答用户问题。");
        prompt.append("要求：1）只输出中文分析，不要输出SQL；2）先给出整体判断；3）再列出2到4条风险或机会；4）给出2条可继续追问的建议；5）控制在220字以内。\n\n");
        prompt.append("用户问题：").append(message).append("\n\n");
        prompt.append("数据指标：\n");
        for (Map<String, Object> row : overviewRows) {
            prompt.append("- ").append(row.get("指标")).append("：").append(row.get("数值")).append(row.get("单位")).append("\n");
        }
        return prompt.toString();
    }

    private List<String> recommendMetricNames(String message, List<Metric> activeMetrics, List<Synonym> synonyms) {
        List<Metric> uniqueMetrics = deduplicateMetrics(activeMetrics);
        Map<String, MetricScore> scores = buildMetricScores(message, activeMetrics, synonyms);
        return uniqueMetrics.stream()
            .map(metric -> Map.entry(metric.getName(), scores.get(metric.getName())))
            .filter(item -> item.getValue() != null && item.getValue().score > 0)
            .sorted((left, right) -> {
                int scoreCompare = Integer.compare(right.getValue().score, left.getValue().score);
                if (scoreCompare != 0) {
                    return scoreCompare;
                }
                return Double.compare(right.getValue().similarity, left.getValue().similarity);
            })
            .map(Map.Entry::getKey)
            .limit(4)
            .toList();
    }

    private MetricMatchAnalysis analyzeMetricMatch(String message, List<Metric> activeMetrics, List<Synonym> synonyms) {
        if (activeMetrics == null || activeMetrics.isEmpty()) {
            return new MetricMatchAnalysis(null, List.of(), false);
        }

        List<Metric> uniqueMetrics = deduplicateMetrics(activeMetrics);
        Map<String, MetricScore> scoreMap = buildMetricScores(message, activeMetrics, synonyms);
        List<MetricRanking> ranking = uniqueMetrics.stream()
            .map(metric -> {
                MetricScore score = scoreMap.get(metric.getName());
                if (score == null) {
                    return new MetricRanking(metric, 0, 0, 0);
                }
                return new MetricRanking(metric, score.score, score.similarity, score.directHits);
            })
            .sorted(Comparator
                .comparingInt(MetricRanking::score).reversed()
                .thenComparing(Comparator.comparingDouble(MetricRanking::similarity).reversed())
                .thenComparing(item -> item.metric().getName()))
            .toList();

        List<String> candidateMetrics = ranking.stream()
            .filter(item -> item.score() > 0 || item.similarity() >= AMBIGUITY_SIMILARITY_THRESHOLD)
            .map(item -> item.metric().getName())
            .limit(4)
            .toList();

        boolean ambiguous = isAmbiguousMetricMatch(ranking);
        Metric matched = ambiguous ? null : resolveByFuzzySimilarity(message, activeMetrics, synonyms);
        return new MetricMatchAnalysis(matched, candidateMetrics, ambiguous);
    }

    private Map<String, MetricScore> buildMetricScores(String message, List<Metric> activeMetrics, List<Synonym> synonyms) {
        Map<String, MetricScore> scores = new LinkedHashMap<>();
        for (Metric metric : activeMetrics) {
            MetricScore score = new MetricScore();
            if (MetricSemanticMatcher.containsTerm(message, metric.getName())) {
                score.score += 100;
                score.directHits += 1;
            }
            double fuzzySimilarity = MetricSemanticMatcher.similarity(message, metric.getName());
            score.similarity = Math.max(score.similarity, fuzzySimilarity);
            if (fuzzySimilarity >= 0.82) {
                score.score += (int) Math.round(fuzzySimilarity * 20);
            } else if (fuzzySimilarity >= TYPO_FUZZY_MATCH_THRESHOLD && hasStrongMetricIntent(message, metric)) {
                score.score += (int) Math.round(fuzzySimilarity * 12);
            }
            if (metric.getDefinition() != null) {
                String[] queryTokens = message.split("[^\\u4e00-\\u9fa5a-zA-Z0-9]+");
                for (String token : queryTokens) {
                    if (token.length() >= 2 && metric.getDefinition().contains(token)) {
                        score.score += 20;
                    }
                }
            }
            scores.put(metric.getName(), score);
        }

        for (Synonym synonym : synonyms) {
            MetricScore score = scores.get(synonym.getStandardWord());
            if (score == null || synonym.getAliases() == null || synonym.getAliases().isEmpty()) {
                continue;
            }
            for (String alias : synonym.getAliases()) {
                if (MetricSemanticMatcher.containsTerm(message, alias)) {
                    score.score += 80;
                    score.directHits += 1;
                } else {
                    double fuzzySimilarity = MetricSemanticMatcher.similarity(message, alias);
                    if (fuzzySimilarity >= 0.87) {
                        score.score += (int) Math.round(fuzzySimilarity * 40);
                    } else if (fuzzySimilarity >= TYPO_FUZZY_MATCH_THRESHOLD && hasStrongMetricIntent(message, findMetricByName(activeMetrics, synonym.getStandardWord()))) {
                        score.score += (int) Math.round(fuzzySimilarity * 20);
                    }
                }
                score.similarity = Math.max(score.similarity, MetricSemanticMatcher.similarity(message, alias));
            }
        }
        return scores;
    }

    private List<Metric> deduplicateMetrics(List<Metric> activeMetrics) {
        Map<String, Metric> unique = new LinkedHashMap<>();
        for (Metric metric : activeMetrics) {
            if (metric == null || metric.getName() == null || metric.getName().isBlank()) {
                continue;
            }
            unique.putIfAbsent(metric.getName(), metric);
        }
        return new ArrayList<>(unique.values());
    }

    private boolean isAmbiguousMetricMatch(List<MetricRanking> ranking) {
        List<MetricRanking> effectiveCandidates = ranking.stream()
            .filter(item -> item.score() > 0 || item.similarity() >= AMBIGUITY_SIMILARITY_THRESHOLD)
            .limit(3)
            .toList();
        if (effectiveCandidates.size() < 2) {
            return false;
        }

        MetricRanking top = effectiveCandidates.get(0);
        MetricRanking second = effectiveCandidates.get(1);
        if (top.directHits() > 0 && second.directHits() > 0) {
            return true;
        }
        if (second.score() >= AMBIGUITY_SECOND_SCORE_THRESHOLD
            && (top.score() - second.score()) <= AMBIGUITY_SCORE_DELTA_THRESHOLD) {
            return true;
        }
        return top.similarity() >= 0.9
            && second.similarity() >= AMBIGUITY_SIMILARITY_THRESHOLD
            && (top.similarity() - second.similarity()) <= AMBIGUITY_SIMILARITY_DELTA_THRESHOLD;
    }

    private Metric resolveByFuzzySimilarity(String message, List<Metric> metrics, List<Synonym> synonyms) {
        Metric bestMetric = null;
        double bestScore = 0;
        double secondBestScore = 0;

        for (Metric metric : metrics) {
            double score = MetricSemanticMatcher.similarity(message, metric.getName());
            if (score > bestScore) {
                secondBestScore = bestScore;
                bestScore = score;
                bestMetric = metric;
            } else if (score > secondBestScore) {
                secondBestScore = score;
            }
        }

        for (Synonym synonym : synonyms) {
            if (synonym.getAliases() == null || synonym.getAliases().isEmpty()) {
                continue;
            }
            Metric mapped = findMetricByName(metrics, synonym.getStandardWord());
            if (mapped == null) {
                continue;
            }
            for (String alias : synonym.getAliases()) {
                double score = MetricSemanticMatcher.similarity(message, alias);
                if (score > bestScore) {
                    secondBestScore = bestScore;
                    bestScore = score;
                    bestMetric = mapped;
                } else if (mapped != bestMetric && score > secondBestScore) {
                    secondBestScore = score;
                }
            }
        }

        if (bestScore >= FUZZY_MATCH_THRESHOLD) {
            return bestMetric;
        }
        if (bestScore >= TYPO_FUZZY_MATCH_THRESHOLD
            && (bestScore - secondBestScore) >= TYPO_FUZZY_GAP_THRESHOLD
            && hasStrongMetricIntent(message, bestMetric)) {
            return bestMetric;
        }
        return null;
    }

    private boolean hasStrongMetricIntent(String message, Metric metric) {
        if (metric == null) {
            return false;
        }
        List<String> actionHints = List.of(
            "多少", "趋势", "对比", "比较", "分析", "变化", "占比", "同比", "环比",
            "本月", "上月", "本周", "今年", "去年", "按", "排名",
            "看", "看看", "看下", "看一下", "帮我看", "瞅瞅", "瞅下",
            "情况", "如何", "怎么样", "咋样", "咋回事", "行不行", "盘下"
        );
        boolean actionHit = actionHints.stream().anyMatch(hint -> MetricSemanticMatcher.containsTerm(message, hint));
        boolean metricNameHit = metric.getName() != null && MetricSemanticMatcher.containsTerm(message, metric.getName());
        return actionHit || metricNameHit;
    }

    private Metric findMetricByName(List<Metric> metrics, String name) {
        return metrics.stream()
            .filter(metric -> metric.getName().equals(name))
            .findFirst()
            .orElse(null);
    }

    private List<String> buildGuidedSuggestions(String message, List<String> candidateMetrics) {
        if (isGreetingIntent(message) || isOverviewIntent(message)) {
            return getDefaultSuggestions();
        }
        if (candidateMetrics.isEmpty()) {
            return inferFallbackMetrics(message).stream()
                .map(metric -> buildMetricSuggestion(metric, message))
                .distinct()
                .limit(4)
                .toList();
        }

        return candidateMetrics.stream()
            .map(metric -> buildMetricSuggestion(metric, message))
            .distinct()
            .limit(4)
            .toList();
    }

    private String buildMetricSuggestion(String metric, String message) {
        String timePrefix = inferTimePrefix(message, metric);
        if (containsAny(message, List.of("趋势", "变化", "走势"))) {
            return timePrefix + metric + "趋势如何？";
        }
        if (containsAny(message, List.of("对比", "比较", "排名", "按"))) {
            return timePrefix + metric + "按区域对比";
        }
        if (containsAny(message, List.of("占比", "构成", "结构"))) {
            return timePrefix + metric + "占比如何？";
        }
        return timePrefix + metric + "是多少？";
    }

    private String inferTimePrefix(String message, String metric) {
        if (containsAny(message, List.of("今日", "今天"))) {
            return "今日";
        }
        if (containsAny(message, List.of("昨日", "昨天"))) {
            return "昨日";
        }
        if (containsAny(message, List.of("本周", "这周", "周内"))) {
            return "本周";
        }
        if (containsAny(message, List.of("上周"))) {
            return "上周";
        }
        if (containsAny(message, List.of("本季度", "本季", "本q"))) {
            return "本季度";
        }
        if (containsAny(message, List.of("上季度", "上季"))) {
            return "上季度";
        }
        if (containsAny(message, List.of("本年", "今年", "年度"))) {
            return "今年";
        }
        if (containsAny(message, List.of("去年"))) {
            return "去年";
        }
        if (containsAny(message, List.of("上月"))) {
            return "上月";
        }
        if (containsAny(message, List.of("本月", "当月"))) {
            return "本月";
        }
        return "本月";
    }

    private List<String> inferFallbackMetrics(String message) {
        List<Metric> activeMetrics = metricMatchingService.getActiveMetrics();
        if (activeMetrics == null || activeMetrics.isEmpty()) {
            return List.of();
        }
        return activeMetrics.stream()
            .map(Metric::getName)
            .filter(name -> name != null && !name.isBlank())
            .distinct()
            .limit(3)
            .toList();
    }

    private String detectGuidanceScenario(String message, List<String> candidateMetrics) {
        return "综合分析场景";
    }

    private static Map<String, List<String>> createMetricKeywords() {
        return new LinkedHashMap<>();
    }

    private boolean isGreetingIntent(String message) {
        return containsAny(message, List.of("你好", "您好", "hi", "hello", "在吗"));
    }

    private boolean isOverviewIntent(String message) {
        return containsAny(message, List.of(
            "数据概览",
            "经营情况",
            "业务情况",
            "整体情况",
            "总体情况",
            "整体分析",
            "现状",
            "概况",
            "总览",
            "最近怎么样",
            "有没有风险",
            "风险点",
            "分析一下",
            "帮我分析",
            "帮我看下",
            "帮我看一下",
            "看一下",
            "帮我看看",
            "总结一下",
            "总结下",
            "情况怎么样",
            "平台怎么样",
            "系统怎么样",
            "产品怎么样",
            "是否可用",
            "好用吗",
            "体验怎么样",
            "评估一下"
        ));
    }

    private boolean containsAny(String message, List<String> keywords) {
        return keywords.stream().anyMatch(keyword -> MetricSemanticMatcher.containsTerm(message, keyword));
    }

    private Map<String, Object> createDiagnosis(String code, String reason, List<String> actions, boolean recovered) {
        Map<String, Object> diagnosis = new LinkedHashMap<>();
        diagnosis.put("code", code);
        diagnosis.put("reason", reason);
        diagnosis.put("recovered", recovered);
        diagnosis.put("actions", actions == null ? List.of() : actions.stream().filter(item -> item != null && !item.isBlank()).distinct().limit(4).toList());
        return diagnosis;
    }

    private void enrichDiagnosisWithScenario(Object diagnosis, String message, List<String> candidateMetrics) {
        if (!(diagnosis instanceof Map<?, ?> map)) {
            return;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> diagnosisMap = (Map<String, Object>) map;
        diagnosisMap.put("guidanceScenario", detectGuidanceScenario(message, candidateMetrics));
        List<String> metricPreview = candidateMetrics == null ? List.of() : candidateMetrics.stream()
            .filter(item -> item != null && !item.isBlank())
            .distinct()
            .limit(3)
            .toList();
        diagnosisMap.put("candidateMetricsPreview", metricPreview);
        diagnosisMap.put("candidateMetricCount", candidateMetrics == null ? 0 : candidateMetrics.size());
        diagnosisMap.put("intentTags", buildIntentTags(message));
    }

    private List<String> buildIntentTags(String message) {
        if (message == null || message.isBlank()) {
            return List.of();
        }
        List<String> tags = new ArrayList<>();
        if (containsAny(message, List.of("看", "看下", "看一下", "看看", "帮我看", "帮我看看", "瞅瞅", "瞅下"))) {
            tags.add("口语查看");
        }
        if (containsAny(message, CONTEXT_REFERENCE_TERMS)) {
            tags.add("上下文指代");
        }
        if (containsAny(message, List.of("咋样", "咋回事", "行不行"))) {
            tags.add("口语追问");
        }
        if (containsAny(message, List.of("趋势", "走势", "变化"))) {
            tags.add("趋势分析");
        }
        if (containsAny(message, List.of("对比", "比较", "差异"))) {
            tags.add("对比分析");
        }
        if (containsAny(message, List.of("占比", "构成", "结构"))) {
            tags.add("占比结构");
        }
        if (containsAny(message, List.of("本月", "上月", "本周", "上周", "本季度", "上季度", "今年", "去年", "今日", "昨天"))) {
            tags.add("时间范围");
        }
        if (containsAny(message, List.of("排名", "Top", "top", "最高", "最低"))) {
            tags.add("排序诉求");
        }
        return tags;
    }

    private Map<String, Object> buildAiStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        AiConfig.ProviderConfig provider = aiConfig.getProvider(aiConfig.getDefaultProvider());
        status.put("mode", aiConfig.getRuntimeMode());
        status.put("enabled", aiConfig.isEnabled());
        status.put("runtimeEnabled", aiConfig.isRuntimeEnabled());
        status.put("reason", aiConfig.getRuntimeReason());
        status.put("defaultProvider", aiConfig.getDefaultProvider());
        status.put("providerName", provider != null ? provider.getName() : aiConfig.getDefaultProvider());
        status.put("model", provider != null ? provider.getModel() : null);
        status.put("providerEnabled", provider != null && provider.isEnabled());
        status.put("apiKeyConfigured", provider != null && provider.getApiKey() != null && !provider.getApiKey().isBlank());
        return status;
    }

    private List<String> buildMetricExamples(String metricName) {
        return switch (metricName) {
            case "销售额" -> List.of("本月销售额是多少？", "华东销售额趋势如何？");
            case "毛利率" -> List.of("本季度毛利率变化", "哪个区域毛利率最高？");
            case "回款额" -> List.of("本月回款额是多少？", "各部门回款额对比");
            case "库存周转天数" -> List.of("库存周转天数按仓库对比", "哪个产品类别库存周转最慢？");
            case "订单履约率" -> List.of("本月订单履约率", "各区域履约率对比");
            case "项目交付及时率" -> List.of("上季度项目交付及时率", "各团队项目交付及时率");
            case "客户投诉量" -> List.of("本季度客户投诉量", "客户投诉量按区域分布");
            case "研发工时利用率" -> List.of("研发工时利用率趋势", "各成员工时利用率");
            case "部门费用支出" -> List.of("本月部门费用支出", "费用支出按部门对比");
            case "审批平均时长" -> List.of("上月审批平均时长", "审批平均时长按部门对比");
            default -> List.of("本月" + metricName, metricName + "趋势");
        };
    }

    private record QueryExecution(
        String metricName,
        String sql,
        List<Map<String, Object>> data,
        String source,
        String reply,
        String chartType,
        List<String> suggestions,
        List<String> candidateMetrics,
        boolean disambiguation,
        Map<String, Object> diagnosis
    ) {}

    private record InterpretationResult(
        String reply,
        String source
    ) {}

    private record MetricMatchAnalysis(
        Metric matchedMetric,
        List<String> candidateMetrics,
        boolean ambiguous
    ) {}

    private record MetricRanking(
        Metric metric,
        int score,
        double similarity,
        int directHits
    ) {}

    private record SecondaryMetricResolution(
        Metric selectedMetric,
        List<String> candidates,
        boolean conflict,
        String source,
        double confidence,
        String reason,
        List<Map<String, Object>> rankedCandidates
    ) {}

    private record TimeReferenceHit(String value, int position) {}

    private static class MetricScore {
        private int score;
        private double similarity;
        private int directHits;
    }

    private String buildConversationTitle(ConversationService.Conversation conversation) {
        return conversation.getMessages().stream()
            .filter(message -> "user".equals(message.getRole()) && message.getContent() != null && !message.getContent().isBlank())
            .map(ConversationService.Message::getContent)
            .findFirst()
            .map(title -> title.length() > 18 ? title.substring(0, 18) + "..." : title)
            .orElse("新对话");
    }
}
