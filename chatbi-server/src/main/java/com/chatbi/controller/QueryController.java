package com.chatbi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatbi.config.AiConfig;
import com.chatbi.dto.ApiResponse;
import com.chatbi.entity.Metric;
import com.chatbi.entity.QueryHistory;
import com.chatbi.entity.QueryResult;
import com.chatbi.entity.Synonym;
import com.chatbi.repository.DataSourceMapper;
import com.chatbi.repository.MetricMapper;
import com.chatbi.repository.SynonymMapper;
import com.chatbi.support.MetricSemanticMatcher;
import com.chatbi.service.AiModelService;
import com.chatbi.service.AiQueryService;
import com.chatbi.service.BusinessInsightService;
import com.chatbi.service.AccessAlertService;
import com.chatbi.service.QueryGovernanceService;
import com.chatbi.service.QueryExecutionService;
import com.chatbi.service.QueryHistoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 用户侧查询接口 - 对应前端 IChatbiService 查询部分
 */
@Slf4j
@Tag(name = "用户查询", description = "用户侧查询接口 - 对应前端 IChatbiService 查询部分")
@RestController
@RequestMapping("/api/query")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class QueryController {

    private static final Map<String, List<String>> METRIC_KEYWORDS = createMetricKeywords();
    private static final int AMBIGUITY_SCORE_DELTA_THRESHOLD = 20;
    private static final int AMBIGUITY_SECOND_SCORE_THRESHOLD = 100;
    private static final double AMBIGUITY_SIMILARITY_THRESHOLD = 0.88;
    private static final double AMBIGUITY_SIMILARITY_DELTA_THRESHOLD = 0.03;
    private static final double FUZZY_MATCH_THRESHOLD = 0.87;
    private static final double TYPO_FUZZY_MATCH_THRESHOLD = 0.66;
    private static final double TYPO_FUZZY_GAP_THRESHOLD = 0.10;

    private final MetricMapper metricMapper;
    private final SynonymMapper synonymMapper;
    private final DataSourceMapper dataSourceMapper;
    private final AiModelService aiModelService;
    private final AiQueryService aiQueryService;
    private final QueryExecutionService queryExecutionService;
    private final QueryHistoryService queryHistoryService;
    private final BusinessInsightService businessInsightService;
    private final AccessAlertService accessAlertService;
    private final AiConfig aiConfig;
    private final ObjectMapper objectMapper;

    /**
     * 执行查询
     * 返回与前端 QueryResult 类型匹配的结构
     */
    @Operation(summary = "执行查询")
    @PostMapping
    public ApiResponse<QueryResult> executeQuery(@RequestBody Map<String, String> request, HttpServletRequest servletRequest) {
        String text = extractQueryText(request);
        Long userId = resolveUserId(request, servletRequest);
        long startTime = System.currentTimeMillis();

        if (text == null || text.trim().isEmpty()) {
            return ApiResponse.error("查询内容不能为空");
        }

        try {
            // 1. 获取启用的指标
            LambdaQueryWrapper<Metric> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Metric::getStatus, "active"); // status = "active" 表示启用
            List<Metric> activeMetrics = metricMapper.selectList(wrapper);
            List<Synonym> synonyms = synonymMapper.selectList(null);
            Map<String, Object> aiStatus = buildAiStatus();

            if (activeMetrics.isEmpty()) {
                QueryResult recovered = buildGuidedOverviewResult(
                    text,
                    "当前无可用指标，已自动返回经营总览。请先在管理后台配置指标后继续查询。",
                    List.of(),
                    false,
                    "guided-discovery",
                    false,
                    aiStatus
                );
                attachDiagnosis(recovered, "NO_ACTIVE_METRICS",
                    "当前未配置可用业务指标，已切换到经营总览。",
                    List.of("进入管理后台新增指标", "配置指标同义词", "返回后重试查询"), true);
                saveSuccessHistory(text, userId, 1L, recovered.getData(), System.currentTimeMillis() - startTime);
                return ApiResponse.ok(recovered);
            }

            com.chatbi.entity.DataSource dataSource = dataSourceMapper.selectById(1L);
            MetricMatchAnalysis metricMatch = analyzeMetricMatch(text, activeMetrics, synonyms);
            List<String> candidateMetrics = metricMatch.candidateMetrics();
            Metric matched = metricMatch.matchedMetric();

            if (metricMatch.ambiguous()) {
                List<String> disambiguationSuggestions = buildDisambiguationSuggestions(text, candidateMetrics);
                QueryResult disambiguationResult = enrichResult(
                    buildOverviewResult(text, "识别到多个可能指标，先返回经营总览并等待用户澄清"),
                    text,
                    "经营总览",
                    false
                );
                disambiguationResult.setSource("guided-disambiguation");
                disambiguationResult.setSuggestions(disambiguationSuggestions);
                disambiguationResult.setCandidateMetrics(candidateMetrics);
                disambiguationResult.setDisambiguation(true);
                disambiguationResult.setAiStatus(aiStatus);
                attachDiagnosis(disambiguationResult, "AMBIGUOUS_METRIC",
                    "检测到多个高相似指标，需先澄清具体分析口径。",
                    disambiguationSuggestions, true);
                if (candidateMetrics.isEmpty()) {
                    disambiguationResult.setSummary("识别到多个可能指标，请补充更明确的指标名称后重试。");
                } else {
                    String examples = disambiguationSuggestions.stream().limit(2).reduce((left, right) -> left + "、" + right).orElse("");
                    disambiguationResult.setSummary(trimSummary("识别到多个可能指标：" + String.join("、", candidateMetrics)
                        + "。请先明确一个指标名称后再查询。"
                        + (examples.isBlank() ? "" : "可直接这样问：" + examples + "。")));
                }
                saveSuccessHistory(text, userId, dataSource != null ? dataSource.getId() : 1L, disambiguationResult.getData(), System.currentTimeMillis() - startTime);
                return ApiResponse.ok(disambiguationResult);
            }

            if (matched != null) {
                QueryResult result = enrichResult(buildMetricResult(text, matched), text, matched.getName(), aiConfig.isRuntimeEnabled());
                result.setCandidateMetrics(List.of(matched.getName()));
                result.setDisambiguation(false);
                result.setAiStatus(aiStatus);
                attachDiagnosis(result, "QUERY_EXECUTED", "已识别业务指标并返回查询结果。", result.getSuggestions(), false);
                saveSuccessHistory(text, userId, dataSource != null ? dataSource.getId() : 1L, result.getData(), System.currentTimeMillis() - startTime);
                return ApiResponse.ok(result);
            }

            if (isGreetingIntent(text) || !aiConfig.isRuntimeEnabled() || isOverviewIntent(text)) {
                List<String> guidedSuggestions = buildGuidedSuggestions(text, candidateMetrics);
                QueryResult result = buildGuidedOverviewResult(
                    text,
                    "当前未识别到明确指标，先返回经营总览，建议使用 AI 对话继续追问",
                    guidedSuggestions,
                    !candidateMetrics.isEmpty(),
                    "guided-discovery",
                    aiConfig.isRuntimeEnabled() && !isGreetingIntent(text),
                    aiStatus
                );
                result.setCandidateMetrics(candidateMetrics);
                attachDiagnosis(result, "METRIC_NOT_RECOGNIZED",
                    "未识别到可直接查询的业务指标，已返回经营总览。",
                    result.getSuggestions(), true);
                saveSuccessHistory(text, userId, dataSource != null ? dataSource.getId() : 1L, result.getData(), System.currentTimeMillis() - startTime);
                return ApiResponse.ok(result);
            }

            // 2. 获取数据源（使用第一个可用的数据源）
            if (dataSource == null) {
                QueryResult recovered = buildGuidedOverviewResult(
                    text,
                    "当前未配置可用数据源，已返回经营总览。请在管理后台完成数据源配置。",
                    buildGuidedSuggestions(text, candidateMetrics),
                    false,
                    "guided-recovery",
                    false,
                    aiStatus
                );
                recovered.setCandidateMetrics(candidateMetrics);
                attachDiagnosis(recovered, "DATASOURCE_UNAVAILABLE",
                    "查询数据源不可用，已自动降级为经营总览。",
                    List.of("检查数据源连接", "确认数据源账号权限", "完成配置后重试"), true);
                saveSuccessHistory(text, userId, 1L, recovered.getData(), System.currentTimeMillis() - startTime);
                return ApiResponse.ok(recovered);
            }

            // 3. 提取表结构
            List<AiQueryService.TableSchema> schemas = queryExecutionService.extractTableSchemas(dataSource);
            if (schemas.isEmpty()) {
                QueryResult recovered = buildGuidedOverviewResult(
                    text,
                    "当前数据源未发现可查询业务表，已返回经营总览。请在管理后台检查数据源元数据。",
                    buildGuidedSuggestions(text, candidateMetrics),
                    false,
                    "guided-recovery",
                    false,
                    aiStatus
                );
                recovered.setCandidateMetrics(candidateMetrics);
                attachDiagnosis(recovered, "NO_BUSINESS_TABLE",
                    "数据源未发现可用业务表，已自动降级为经营总览。",
                    List.of("同步数据源元数据", "确认业务表是否存在", "检查表权限后重试"), true);
                saveSuccessHistory(text, userId, dataSource.getId(), recovered.getData(), System.currentTimeMillis() - startTime);
                return ApiResponse.ok(recovered);
            }

            // 4. 使用AI生成SQL
            String sql;
            try {
                sql = aiQueryService.generateSqlWithLLM(text, schemas);
                log.info("AI生成SQL成功 - 查询: {}, SQL: {}", text, sql);
            } catch (Exception e) {
                log.warn("AI生成SQL失败，使用规则引擎降级 - {}", e.getMessage());
                QueryResult result = buildGuidedOverviewResult(
                    text,
                    "外部大模型暂不可用，已切换为经营总览",
                    buildGuidedSuggestions(text, candidateMetrics),
                    false,
                    "guided-recovery",
                    false,
                    aiStatus
                );
                result.setCandidateMetrics(candidateMetrics);
                attachDiagnosis(result, "LLM_SQL_FAILED",
                    "外部模型 SQL 生成失败，已切换语义兜底结果。",
                    result.getSuggestions(), true);
                saveSuccessHistory(text, userId, dataSource.getId(), result.getData(), System.currentTimeMillis() - startTime);
                return ApiResponse.ok(result);
            }

            // 5. 验证SQL安全性
            QueryGovernanceService.ValidationResult validation = aiQueryService.validateSqlDetail(sql);
            if (!validation.valid()) {
                log.warn("SQL治理校验未通过，降级返回经营总览 - query: {}, reason: {}", text, validation.message());
                recordAccessAlertSafely(
                    userId,
                    "user-" + userId,
                    text,
                    sql,
                    validation.message(),
                    "query"
                );
                QueryResult recovered = buildGuidedOverviewResult(
                    text,
                    "查询语句未通过治理校验，已自动切换为经营总览。你可以明确业务指标后重试。",
                    buildGuidedSuggestions(text, candidateMetrics),
                    false,
                    "guided-recovery",
                    false,
                    aiStatus
                );
                recovered.setCandidateMetrics(candidateMetrics);
                attachDiagnosis(recovered, "SQL_GOVERNANCE_BLOCKED",
                    "生成 SQL 未通过安全治理校验，系统已阻断并返回经营总览。",
                    List.of("补充明确业务指标", "补充时间范围或维度", "在 AI 对话中继续追问"), true);
                saveSuccessHistory(text, userId, dataSource.getId(), recovered.getData(), System.currentTimeMillis() - startTime);
                return ApiResponse.ok(recovered);
            }

            // 6. 执行SQL
            List<Map<String, Object>> data = queryExecutionService.executeQuery(dataSource, sql, userId);

            // 7. 保存查询历史
            long executionTime = System.currentTimeMillis() - startTime;
            saveSuccessHistory(text, userId, dataSource.getId(), data, executionTime);

            // 8. 构建结果
            QueryResult result = QueryResult.builder()
                    .query(text)
                    .sql(sql)
                    .metric(extractMetricName(text, activeMetrics, synonyms))
                    .timeRange(extractTimeRange(text))
                    .dimension("数据维度")
                    .total(data.size())
                    .data(data)
                    .source("llm")
                    .build();
            enrichResult(result, text, result.getMetric(), true);
            result.setCandidateMetrics(candidateMetrics);
            result.setDisambiguation(false);
            result.setAiStatus(aiStatus);
            attachDiagnosis(result, "QUERY_EXECUTED", "查询执行完成，已返回结构化结果。", result.getSuggestions(), false);

            log.info("查询执行成功 - 耗时: {}ms, 结果数: {}", executionTime, data.size());
            return ApiResponse.ok(result);

        } catch (Exception e) {
            log.error("查询执行失败 - 查询: {}", text, e);

            // 保存失败的查询历史
            try {
                long executionTime = System.currentTimeMillis() - startTime;
                saveFailedHistory(text, userId, executionTime, e.getMessage());
            } catch (Exception ex) {
                log.error("保存失败查询历史失败", ex);
            }

            try {
                LambdaQueryWrapper<Metric> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(Metric::getStatus, "active");
                List<Metric> activeMetrics = metricMapper.selectList(wrapper);
                List<Synonym> synonyms = synonymMapper.selectList(null);
                List<String> candidateMetrics = recommendMetricNames(text, activeMetrics, synonyms);
                List<String> guidedSuggestions = buildGuidedSuggestions(text, candidateMetrics);

                QueryResult recovered = buildOverviewResult(text, "查询链路异常，已自动降级为经营总览");
                recovered.setSource("guided-recovery");
                recovered = enrichResult(recovered, text, "经营总览", false);
                recovered.setSuggestions(guidedSuggestions);
                recovered.setCandidateMetrics(candidateMetrics);
                recovered.setDisambiguation(false);
                recovered.setAiStatus(buildAiStatus());
                attachDiagnosis(recovered, "QUERY_CHAIN_EXCEPTION",
                    "查询链路出现异常，已切换经营总览保护模式。",
                    guidedSuggestions, true);
                if (!candidateMetrics.isEmpty()) {
                    recovered.setSummary(trimSummary(recovered.getSummary() + " 可直接查询指标：" + String.join("、", candidateMetrics) + "。"));
                }
                return ApiResponse.ok(recovered);
            } catch (Exception recoveryEx) {
                log.error("查询降级恢复失败 - 查询: {}", text, recoveryEx);
                return ApiResponse.ok(buildEmergencyRecoveryResult(text, e, recoveryEx));
            }
        }
    }

    /**
     * 执行可视化查询构建器生成的 SQL
     */
    @Operation(summary = "执行可视化查询构建器生成的 SQL")
    @PostMapping("/execute")
    public ApiResponse<Map<String, Object>> executeSql(@RequestBody Map<String, Object> request, HttpServletRequest servletRequest) {
        Object sqlValue = request != null ? request.get("sql") : null;
        String sql = sqlValue == null ? null : String.valueOf(sqlValue).trim();
        if (sql == null || sql.isEmpty()) {
            return ApiResponse.error("SQL 不能为空");
        }

        QueryGovernanceService.ValidationResult validation = aiQueryService.validateSqlDetail(sql);
        if (!validation.valid()) {
            Long userId = resolveUserId(request, servletRequest);
            recordAccessAlertSafely(
                userId,
                "user-" + userId,
                "可视化查询器执行 SQL",
                sql,
                validation.message(),
                "query-builder"
            );
            return ApiResponse.error(validation.message());
        }

        Long userId = resolveUserId(request, servletRequest);
        Long datasourceId = 1L;
        if (request != null && request.get("datasourceId") != null) {
            datasourceId = Long.parseLong(String.valueOf(request.get("datasourceId")));
        }

        com.chatbi.entity.DataSource dataSource = dataSourceMapper.selectById(datasourceId);
        if (dataSource == null) {
            return ApiResponse.error("数据源不存在");
        }

        try {
            List<Map<String, Object>> records = queryExecutionService.executeQuery(dataSource, sql, userId);
            Map<String, Object> payload = new HashMap<>();
            payload.put("records", records);
            payload.put("total", records.size());
            payload.put("sql", sql);
            return ApiResponse.ok(payload);
        } catch (Exception e) {
            log.error("执行构建器 SQL 失败 - datasourceId: {}, sql: {}", datasourceId, sql, e);
            return ApiResponse.error("SQL 执行失败：" + e.getMessage());
        }
    }

    private String extractQueryText(Map<String, String> request) {
        if (request == null || request.isEmpty()) {
            return null;
        }
        for (String key : List.of("text", "query", "queryText", "message", "content")) {
            String value = request.get(key);
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    private void recordAccessAlertSafely(Long userId, String username, String queryText, String sql, String reason, String scene) {
        try {
            accessAlertService.recordBlockedAccess(userId, username, queryText, sql, reason, scene);
        } catch (Exception ex) {
            log.warn("记录越权告警失败 - userId: {}, reason: {}", userId, ex.getMessage());
        }
    }

    /**
     * 从查询文本中提取指标名称
     */
    private String extractMetricName(String query, List<Metric> metrics, List<Synonym> synonyms) {
        MetricMatchAnalysis analysis = analyzeMetricMatch(query, metrics, synonyms);
        Metric matched = analysis.matchedMetric();
        return matched != null ? matched.getName() : "未知指标";
    }

    /**
     * 获取热门查询（基于启用的指标）
     */
    @Operation(summary = "获取热门查询（基于启用的指标）")
    @GetMapping("/hot")
    public ApiResponse<List<Map<String, Object>>> getHotQueries() {
        LambdaQueryWrapper<Metric> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Metric::getStatus, "active");
        List<Metric> activeMetrics = metricMapper.selectList(wrapper);
        List<QueryHistory> histories = queryHistoryService.page(1L, 1L, 500L).getRecords();
        List<Map<String, Object>> hotQueries = new ArrayList<>();
        String[] timePrefixes = {"本月", "本季度", "今年", "上月"};

        for (int index = 0; index < Math.min(5, activeMetrics.size()); index++) {
            Metric metric = activeMetrics.get(index);
            long hitCount = histories.stream()
                    .filter(history -> {
                        String content = history.getQueryContent();
                        return content != null && content.contains(metric.getName());
                    })
                    .count();
            Map<String, Object> item = new HashMap<>();
            item.put("id", metric.getId());
            item.put("text", timePrefixes[index % timePrefixes.length] + metric.getName());
            item.put("count", hitCount + "次");
            hotQueries.add(item);
        }

        return ApiResponse.ok(hotQueries);
    }

    /**
     * 获取示例查询
     */
    @Operation(summary = "获取示例查询")
    @GetMapping("/examples")
    public ApiResponse<List<String>> getExamples() {
        return ApiResponse.ok(List.of(
            "先给我一个经营总览",
            "本月销售额是多少？",
            "库存周转天数按仓库对比",
            "本季度客户投诉量按区域分布",
            "上月审批平均时长是多少？"
        ));
    }

    /**
     * 匹配指标 - 优先使用同义词配置
     */
    private Metric matchMetric(String query, List<Metric> metrics, List<Synonym> synonyms) {
        // 1. 直接匹配指标名称
        for (Metric m : metrics) {
            if (MetricSemanticMatcher.containsTerm(query, m.getName())) {
                return m;
            }
        }

        // 2. 使用同义词匹配 - 查询词匹配同义词，返回对应标准词的指标
        for (Synonym synonym : synonyms) {
            if (synonym.getAliases() == null) {
                continue;
            }
            // 检查查询是否包含任何一个同义词
            for (String alias : synonym.getAliases()) {
                if (MetricSemanticMatcher.containsTerm(query, alias)) {
                    Metric mapped = findMetricByName(metrics, synonym.getStandardWord());
                    if (mapped != null) {
                        return mapped;
                    }
                }
            }
        }

        // 3. 硬编码关键词匹配（兜底）
        for (Map.Entry<String, List<String>> entry : METRIC_KEYWORDS.entrySet()) {
            boolean matchedKeyword = entry.getValue().stream().anyMatch(keyword -> MetricSemanticMatcher.containsTerm(query, keyword));
            if (matchedKeyword) {
                Metric mapped = findMetricByName(metrics, entry.getKey());
                if (mapped != null) {
                    return mapped;
                }
            }
        }

        return resolveByFuzzySimilarity(query, metrics, synonyms);
    }

    private MetricMatchAnalysis analyzeMetricMatch(String query, List<Metric> metrics, List<Synonym> synonyms) {
        if (metrics == null || metrics.isEmpty()) {
            return new MetricMatchAnalysis(null, List.of(), false);
        }
        List<Metric> uniqueMetrics = deduplicateMetrics(metrics);
        Map<String, MetricScore> scoreMap = buildMetricScores(query, metrics, synonyms);
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
        Metric matched = ambiguous ? null : matchMetric(query, metrics, synonyms);
        return new MetricMatchAnalysis(matched, candidateMetrics, ambiguous);
    }

    private Map<String, MetricScore> buildMetricScores(String query, List<Metric> metrics, List<Synonym> synonyms) {
        Map<String, MetricScore> scores = new LinkedHashMap<>();
        for (Metric metric : metrics) {
            MetricScore score = new MetricScore();
            if (MetricSemanticMatcher.containsTerm(query, metric.getName())) {
                score.score += 100;
                score.directHits += 1;
            }
            for (String keyword : METRIC_KEYWORDS.getOrDefault(metric.getName(), List.of())) {
                if (MetricSemanticMatcher.containsTerm(query, keyword)) {
                    score.score += 36;
                }
            }
            double fuzzy = MetricSemanticMatcher.similarity(query, metric.getName());
            score.similarity = Math.max(score.similarity, fuzzy);
            if (fuzzy >= 0.82) {
                score.score += (int) Math.round(fuzzy * 20);
            } else if (fuzzy >= TYPO_FUZZY_MATCH_THRESHOLD && hasStrongMetricIntent(query, metric)) {
                score.score += (int) Math.round(fuzzy * 12);
            }
            if (metric.getDefinition() != null) {
                for (String token : List.of("销售", "毛利", "利润", "回款", "库存", "履约", "交付", "投诉", "工时", "研发", "费用", "审批", "时长")) {
                    if (MetricSemanticMatcher.containsTerm(query, token) && metric.getDefinition().contains(token)) {
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
                if (MetricSemanticMatcher.containsTerm(query, alias)) {
                    score.score += 80;
                    score.directHits += 1;
                } else {
                    double fuzzy = MetricSemanticMatcher.similarity(query, alias);
                    if (fuzzy >= 0.87) {
                        score.score += (int) Math.round(fuzzy * 40);
                    } else if (fuzzy >= TYPO_FUZZY_MATCH_THRESHOLD && hasStrongMetricIntent(query, findMetricByName(metrics, synonym.getStandardWord()))) {
                        score.score += (int) Math.round(fuzzy * 20);
                    }
                }
                score.similarity = Math.max(score.similarity, MetricSemanticMatcher.similarity(query, alias));
            }
        }

        return scores;
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

    private Metric resolveByFuzzySimilarity(String query, List<Metric> metrics, List<Synonym> synonyms) {
        Metric bestMetric = null;
        double bestScore = 0;
        double secondBestScore = 0;

        for (Metric metric : metrics) {
            double score = MetricSemanticMatcher.similarity(query, metric.getName());
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
                double score = MetricSemanticMatcher.similarity(query, alias);
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
            && hasStrongMetricIntent(query, bestMetric)) {
            return bestMetric;
        }
        return null;
    }

    private boolean hasStrongMetricIntent(String query, Metric metric) {
        if (metric == null) {
            return false;
        }
        List<String> actionHints = List.of(
            "多少", "趋势", "对比", "比较", "分析", "变化", "占比", "同比", "环比",
            "本月", "上月", "本周", "今年", "去年", "按", "排名",
            "看", "看看", "看下", "看一下", "帮我看", "瞅瞅", "瞅下",
            "情况", "如何", "怎么样", "咋样", "咋回事", "行不行", "盘下"
        );
        boolean actionHit = actionHints.stream().anyMatch(item -> MetricSemanticMatcher.containsTerm(query, item));
        boolean metricHint = METRIC_KEYWORDS.getOrDefault(metric.getName(), List.of(metric.getName()))
            .stream()
            .anyMatch(item -> MetricSemanticMatcher.containsTerm(query, item));
        return actionHit || metricHint;
    }

    private Metric findMetricByName(List<Metric> metrics, String metricName) {
        return metrics.stream()
            .filter(metric -> metric.getName().equals(metricName))
            .findFirst()
            .orElse(null);
    }

    private boolean isGreetingIntent(String query) {
        return containsAny(query, List.of("你好", "您好", "hi", "hello", "在吗"));
    }

    private static Map<String, List<String>> createMetricKeywords() {
        Map<String, List<String>> keywordMap = new LinkedHashMap<>();
        keywordMap.put("销售额", List.of("销售额", "销售", "营收", "收入", "营业额", "销售收入", "业绩", "revenue", "sales"));
        keywordMap.put("毛利率", List.of("毛利率", "毛利", "利润率", "利润", "盈利", "grossmargin", "margin", "profitmargin"));
        keywordMap.put("回款额", List.of("回款额", "回款", "到账", "收款", "现金回笼", "cashcollection", "collection"));
        keywordMap.put("库存周转天数", List.of("库存周转天数", "库存周转", "库存", "周转天数", "库存效率", "inventoryturnover", "inventory"));
        keywordMap.put("订单履约率", List.of("订单履约率", "履约率", "履约", "交付履约", "按时履约", "fulfillmentrate", "fulfillment"));
        keywordMap.put("项目交付及时率", List.of("项目交付及时率", "交付及时率", "交付率", "项目交付", "交付效率", "ontimedelivery", "deliveryrate"));
        keywordMap.put("客户投诉量", List.of("客户投诉量", "投诉", "客诉", "投诉量", "客户体验", "complaint", "complaints"));
        keywordMap.put("研发工时利用率", List.of("研发工时利用率", "工时", "工时利用率", "研发", "研发效率", "研发产能", "rdutilization", "utilization"));
        keywordMap.put("部门费用支出", List.of("部门费用支出", "费用", "支出", "成本", "开销", "花费", "expense", "cost"));
        keywordMap.put("审批平均时长", List.of("审批平均时长", "审批", "审批效率", "审批时长", "审批时效", "流程效率", "approvaltime", "workflow"));
        return keywordMap;
    }

    private List<String> recommendMetricNames(String query, List<Metric> metrics, List<Synonym> synonyms) {
        if (metrics == null || metrics.isEmpty()) {
            return List.of();
        }

        List<Metric> uniqueMetrics = deduplicateMetrics(metrics);
        Map<String, MetricScore> scores = buildMetricScores(query, metrics, synonyms);
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

    private List<Metric> deduplicateMetrics(List<Metric> metrics) {
        Map<String, Metric> unique = new LinkedHashMap<>();
        for (Metric metric : metrics) {
            if (metric == null || metric.getName() == null || metric.getName().isBlank()) {
                continue;
            }
            unique.putIfAbsent(metric.getName(), metric);
        }
        return new ArrayList<>(unique.values());
    }

    private List<String> buildDisambiguationSuggestions(String query, List<String> candidateMetrics) {
        if (candidateMetrics == null || candidateMetrics.isEmpty()) {
            return buildGuidedSuggestions(query, List.of());
        }
        return candidateMetrics.stream()
            .map(metric -> buildMetricSuggestion(metric, query))
            .distinct()
            .limit(4)
            .toList();
    }

    private List<String> buildGuidedSuggestions(String query, List<String> candidateMetrics) {
        if (isGreetingIntent(query) || isOverviewIntent(query)) {
            return List.of("先给我一个经营总览", "本月销售额是多少？", "库存周转天数按仓库对比");
        }
        if (candidateMetrics == null || candidateMetrics.isEmpty()) {
            return inferFallbackMetrics(query).stream()
                .map(metric -> buildMetricSuggestion(metric, query))
                .distinct()
                .limit(4)
                .toList();
        }

        return candidateMetrics.stream()
            .map(metric -> buildMetricSuggestion(metric, query))
            .distinct()
            .limit(4)
            .toList();
    }

    private String buildMetricSuggestion(String metric, String query) {
        String timePrefix = inferTimePrefix(query, metric);
        if (containsAny(query, List.of("趋势", "变化", "走势"))) {
            return timePrefix + metric + "趋势如何？";
        }
        if (containsAny(query, List.of("对比", "比较", "排名", "按"))) {
            return timePrefix + metric + "按区域对比";
        }
        if (containsAny(query, List.of("占比", "构成", "结构"))) {
            return timePrefix + metric + "占比如何？";
        }
        return switch (metric) {
            case "销售额" -> timePrefix + "销售额是多少？";
            case "毛利率" -> timePrefix + "毛利率趋势如何？";
            case "回款额" -> timePrefix + "回款额是多少？";
            case "库存周转天数" -> "库存周转天数按仓库对比";
            case "订单履约率" -> timePrefix + "订单履约率如何？";
            case "项目交付及时率" -> "上季度项目交付及时率";
            case "客户投诉量" -> "本季度客户投诉量按区域分布";
            case "研发工时利用率" -> "研发工时利用率按团队对比";
            case "部门费用支出" -> timePrefix + "部门费用支出按部门对比";
            case "审批平均时长" -> "上月审批平均时长是多少？";
            default -> timePrefix + metric;
        };
    }

    private String inferTimePrefix(String query, String metric) {
        if (containsAny(query, List.of("今日", "今天"))) {
            return "今日";
        }
        if (containsAny(query, List.of("昨日", "昨天"))) {
            return "昨日";
        }
        if (containsAny(query, List.of("本周", "这周", "周内"))) {
            return "本周";
        }
        if (containsAny(query, List.of("上周"))) {
            return "上周";
        }
        if (containsAny(query, List.of("本季度", "本季", "本q"))) {
            return "本季度";
        }
        if (containsAny(query, List.of("上季度", "上季"))) {
            return "上季度";
        }
        if (containsAny(query, List.of("本年", "今年", "年度"))) {
            return "今年";
        }
        if (containsAny(query, List.of("去年"))) {
            return "去年";
        }
        if (containsAny(query, List.of("上月"))) {
            return "上月";
        }
        if (containsAny(query, List.of("本月", "当月"))) {
            return "本月";
        }
        if ("项目交付及时率".equals(metric) || "客户投诉量".equals(metric)) {
            return "本季度";
        }
        return "本月";
    }

    private List<String> inferFallbackMetrics(String query) {
        if (containsAny(query, List.of("研发", "交付", "上线", "迭代", "项目"))) {
            return List.of("研发工时利用率", "项目交付及时率", "审批平均时长");
        }
        if (containsAny(query, List.of("客户", "客诉", "投诉", "体验", "留存"))) {
            return List.of("客户投诉量", "订单履约率", "回款额");
        }
        if (containsAny(query, List.of("成本", "费用", "支出", "预算"))) {
            return List.of("部门费用支出", "毛利率", "库存周转天数");
        }
        return List.of("销售额", "毛利率", "库存周转天数");
    }

    private String detectGuidanceScenario(String query, List<String> candidateMetrics) {
        if (containsAny(query, List.of("研发", "交付", "上线", "迭代", "项目"))) {
            return "研发效能场景";
        }
        if (containsAny(query, List.of("客户", "客诉", "投诉", "体验", "留存"))) {
            return "客户经营场景";
        }
        if (containsAny(query, List.of("成本", "费用", "支出", "预算"))) {
            return "成本管控场景";
        }
        List<String> metrics = candidateMetrics == null ? List.of() : candidateMetrics;
        if (metrics.stream().anyMatch(metric -> metric != null && metric.contains("研发"))) {
            return "研发效能场景";
        }
        if (metrics.stream().anyMatch(metric -> metric != null && (metric.contains("客户") || metric.contains("投诉") || metric.contains("履约")))) {
            return "客户经营场景";
        }
        if (metrics.stream().anyMatch(metric -> metric != null && (metric.contains("费用") || metric.contains("毛利") || metric.contains("库存")))) {
            return "成本管控场景";
        }
        return "综合经营场景";
    }

    private boolean isOverviewIntent(String query) {
        return containsAny(query, List.of(
                "经营总览",
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
                "帮我看看",
                "总结一下",
                "总结下",
                "情况怎么样"
            ));
    }

    private boolean containsAny(String query, List<String> keywords) {
        return keywords.stream().anyMatch(item -> MetricSemanticMatcher.containsTerm(query, item));
    }

    private QueryResult buildMetricResult(String text, Metric matched) {
        BusinessInsightService.QueryPlan plan = businessInsightService.queryMetric(matched.getName(), text);
        return QueryResult.builder()
            .query(text)
            .sql(plan.getSql())
            .metric(matched.getName())
            .timeRange(extractTimeRange(text))
            .dimension(plan.getDimension())
            .total(plan.getData().size())
            .data(plan.getData())
            .source("business-insight")
            .build();
    }

    private QueryResult buildOverviewResult(String text, String reason) {
        List<Map<String, Object>> overview = safeOverviewRows();
        return QueryResult.builder()
            .query(text)
            .sql("-- " + reason)
            .metric("经营总览")
            .timeRange("当前概览")
            .dimension("指标")
            .total(overview.size())
            .data(overview)
            .source("overview")
            .build();
    }

    private List<Map<String, Object>> safeOverviewRows() {
        try {
            List<Map<String, Object>> rows = businessInsightService.getOverviewRows();
            if (rows != null && !rows.isEmpty()) {
                return rows;
            }
        } catch (Exception ex) {
            log.warn("获取经营总览失败，使用本地兜底数据 - {}", ex.getMessage());
        }
        return List.of(
            createOverviewRow("累计销售额", 568000, "元"),
            createOverviewRow("活跃客户数", 212, "家"),
            createOverviewRow("订单履约率", 96.2, "%"),
            createOverviewRow("库存周转天数", 32, "天")
        );
    }

    private Map<String, Object> createOverviewRow(String metric, Object value, String unit) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("指标", metric);
        row.put("数值", value);
        row.put("单位", unit);
        return row;
    }

    private QueryResult buildGuidedOverviewResult(
        String text,
        String reason,
        List<String> suggestions,
        boolean disambiguation,
        String source,
        boolean allowAiSummary,
        Map<String, Object> aiStatus
    ) {
        QueryResult result = enrichResult(
            buildOverviewResult(text, reason),
            text,
            "经营总览",
            allowAiSummary
        );
        result.setSource(source);
        result.setSuggestions(suggestions == null || suggestions.isEmpty()
            ? List.of("先给我一个经营总览", "本月销售额是多少？", "库存周转天数按仓库对比")
            : suggestions);
        result.setDisambiguation(disambiguation);
        result.setAiStatus(aiStatus);
        result.setSummary(trimSummary(reason + "。 " + (result.getSummary() == null ? "" : result.getSummary())));
        return result;
    }

    private void attachDiagnosis(
        QueryResult result,
        String code,
        String reason,
        List<String> actions,
        boolean recovered
    ) {
        if (result == null) {
            return;
        }
        Map<String, Object> diagnosis = new LinkedHashMap<>();
        diagnosis.put("code", code);
        diagnosis.put("reason", reason);
        diagnosis.put("recovered", recovered);
        diagnosis.put("actions", actions == null ? List.of() : actions.stream().filter(item -> item != null && !item.isBlank()).distinct().limit(4).toList());
        diagnosis.put("guidanceScenario", detectGuidanceScenario(result.getQuery(), result.getCandidateMetrics()));
        List<String> metricPreview = result.getCandidateMetrics() == null ? List.of() : result.getCandidateMetrics().stream()
            .filter(item -> item != null && !item.isBlank())
            .distinct()
            .limit(3)
            .toList();
        diagnosis.put("candidateMetricsPreview", metricPreview);
        diagnosis.put("candidateMetricCount", result.getCandidateMetrics() == null ? 0 : result.getCandidateMetrics().size());
        diagnosis.put("intentTags", buildIntentTags(result.getQuery()));
        result.setDiagnosis(diagnosis);
    }

    private List<String> buildIntentTags(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        List<String> tags = new ArrayList<>();
        if (containsAny(query, List.of("看", "看下", "看一下", "看看", "帮我看", "帮我看看", "瞅瞅", "瞅下"))) {
            tags.add("口语查看");
        }
        if (containsAny(query, List.of("咋样", "咋回事", "行不行"))) {
            tags.add("口语追问");
        }
        if (containsAny(query, List.of("趋势", "走势", "变化"))) {
            tags.add("趋势分析");
        }
        if (containsAny(query, List.of("对比", "比较", "差异"))) {
            tags.add("对比分析");
        }
        if (containsAny(query, List.of("占比", "构成", "结构"))) {
            tags.add("占比结构");
        }
        if (containsAny(query, List.of("本月", "上月", "本周", "上周", "本季度", "上季度", "今年", "去年", "今日", "昨天"))) {
            tags.add("时间范围");
        }
        if (containsAny(query, List.of("排名", "Top", "top", "最高", "最低"))) {
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

    private QueryResult enrichResult(QueryResult result, String queryText, String metricName, boolean allowAiSummary) {
        List<String> suggestions = buildSuggestions(metricName);
        result.setSuggestions(suggestions);

        String summary = buildLocalSummary(result, metricName);
        boolean aiSummaryApplied = false;

        if (allowAiSummary && aiConfig.isRuntimeEnabled()) {
            try {
                summary = trimSummary(aiModelService.generateText(buildSummaryPrompt(queryText, result, metricName, suggestions)));
                aiSummaryApplied = summary != null && !summary.isBlank();
            } catch (Exception ex) {
                log.warn("查询结果 AI 解读失败，降级为本地摘要 - query: {}, error: {}", queryText, ex.getMessage());
            }
        }

        result.setSummary(summary);
        if (aiSummaryApplied && result.getSource() != null && !result.getSource().startsWith("llm")) {
            result.setSource(result.getSource() + "-ai");
        }
        return result;
    }

    private QueryResult buildEmergencyRecoveryResult(String text, Exception rootError, Exception recoverError) {
        QueryResult recovered = QueryResult.builder()
            .query(text)
            .sql("-- 查询链路二次恢复：返回本地兜底经营概览")
            .metric("经营总览")
            .timeRange("当前概览")
            .dimension("指标")
            .total(0)
            .data(safeOverviewRows())
            .source("guided-recovery")
            .build();

        recovered = enrichResult(recovered, text, "经营总览", false);
        recovered.setSuggestions(List.of("先给我一个经营总览", "本月销售额是多少？", "库存周转天数按仓库对比"));
        recovered.setCandidateMetrics(List.of());
        recovered.setDisambiguation(false);
        attachDiagnosis(recovered, "EMERGENCY_RECOVERY",
            "查询链路出现连续异常，已进入保护模式并返回兜底经营总览。",
            recovered.getSuggestions(), true);

        Map<String, Object> aiStatus = new LinkedHashMap<>(buildAiStatus());
        aiStatus.put("hardRecovered", true);
        aiStatus.put("recoverReason", rootError.getMessage());
        aiStatus.put("recoverFallbackReason", recoverError.getMessage());
        recovered.setAiStatus(aiStatus);
        recovered.setSummary(trimSummary("查询链路出现异常，系统已自动切换保护模式并返回经营总览。你可以继续追问具体指标。"));
        recovered.setTotal(recovered.getData() == null ? 0 : recovered.getData().size());
        return recovered;
    }

    private String buildSummaryPrompt(String queryText, QueryResult result, String metricName, List<String> suggestions) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是企业级 BI 分析助手。请基于真实查询结果输出简洁专业的中文摘要。");
        prompt.append("要求：1）先给一句结论；2）再给2到3条关键发现；3）不要编造数据；4）不要输出 SQL；5）控制在180字以内；6）最后补一句“可继续追问：”并从提供的建议里挑1到2个。");
        prompt.append("\n\n用户问题：").append(queryText);
        prompt.append("\n指标：").append(metricName == null || metricName.isBlank() ? result.getMetric() : metricName);
        prompt.append("\n时间范围：").append(result.getTimeRange());
        prompt.append("\n维度：").append(result.getDimension());
        prompt.append("\n结果来源：").append(result.getSource());
        prompt.append("\n查询结果：\n");

        List<Map<String, Object>> rows = result.getData() == null ? List.of() : result.getData();
        rows.stream().limit(8).forEach(row -> {
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
        if (rows.size() > 8) {
            prompt.append("- 其余结果省略，共 ").append(rows.size()).append(" 条\n");
        }

        prompt.append("建议追问：").append(String.join("；", suggestions));
        return prompt.toString();
    }

    private String buildLocalSummary(QueryResult result, String metricName) {
        List<Map<String, Object>> rows = result.getData() == null ? List.of() : result.getData();
        if (rows.isEmpty()) {
            return "未查询到符合条件的数据，建议调整时间范围、指标名称或筛选维度后重试。";
        }

        if ("经营总览".equals(metricName) || "经营总览".equals(result.getMetric())) {
            String highlights = rows.stream()
                .limit(3)
                .map(this::formatOverviewHighlight)
                .reduce((left, right) -> left + "，" + right)
                .orElse("已返回最新经营指标");
            return "已返回最新经营总览，" + highlights + "。如需定位原因，可继续追问区域、部门或趋势变化。";
        }

        String valueField = findPrimaryValueField(rows);
        String dimensionField = rows.get(0).keySet().stream().findFirst().orElse("维度");
        Map<String, Object> bestRow = findPeakRow(rows, valueField);

        if (bestRow != null && valueField != null && rows.size() > 1) {
            return "已返回“" + result.getMetric() + "”查询结果，共 " + rows.size() + " 条；当前最突出的是“"
                + bestRow.get(dimensionField) + "”，" + valueField + "为" + bestRow.get(valueField)
                + "。可继续追问趋势变化、区域拆解或异常原因。";
        }

        if (valueField != null) {
            Object value = rows.get(0).get(valueField);
            return "已返回“" + result.getMetric() + "”查询结果，当前核心数值为 " + value
                + "，时间范围为" + result.getTimeRange() + "。可继续追问同比、环比或维度拆解。";
        }

        return "已返回“" + result.getMetric() + "”查询结果，共 " + rows.size() + " 条，建议继续追问趋势、对比或异常点。";
    }

    private String findPrimaryValueField(List<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            return null;
        }
        Map<String, Object> firstRow = rows.get(0);
        return firstRow.entrySet().stream()
            .filter(entry -> entry.getValue() instanceof Number)
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse(null);
    }

    private Map<String, Object> findPeakRow(List<Map<String, Object>> rows, String valueField) {
        if (valueField == null) {
            return null;
        }
        return rows.stream()
            .filter(row -> row.get(valueField) instanceof Number)
            .max(Comparator.comparing(row -> ((Number) row.get(valueField)).doubleValue()))
            .orElse(null);
    }

    private List<String> buildSuggestions(String metricName) {
        String target = metricName == null || metricName.isBlank() ? "经营总览" : metricName;
        return switch (target) {
            case "销售额" -> List.of("按区域拆解本月销售额", "本月销售额趋势如何？", "销售额和毛利率一起分析");
            case "毛利率" -> List.of("毛利率按区域对比", "毛利率趋势如何？", "哪个产品类别毛利率最高？");
            case "回款额" -> List.of("各部门回款额对比", "回款额趋势如何？", "回款额与销售额是否同步");
            case "库存周转天数" -> List.of("库存周转天数按仓库对比", "哪个品类周转最慢？", "库存周转趋势如何？");
            case "订单履约率" -> List.of("订单履约率按区域对比", "近三个月履约率趋势", "履约率最低的是哪个区域");
            case "项目交付及时率" -> List.of("各团队项目交付及时率", "项目交付及时率趋势", "交付延迟最多的区域");
            case "客户投诉量" -> List.of("客户投诉量按区域分布", "投诉量趋势如何？", "哪个渠道投诉最多");
            case "研发工时利用率" -> List.of("研发工时利用率按团队对比", "研发工时利用率趋势", "哪个成员利用率最高");
            case "部门费用支出" -> List.of("部门费用支出按部门对比", "费用支出趋势如何？", "费用异常最高的部门");
            case "审批平均时长" -> List.of("审批平均时长按部门拆解", "审批平均时长趋势", "哪个流程审批最慢");
            default -> List.of("先给我一个经营总览", "本月销售额是多少？", "库存周转天数按仓库对比");
        };
    }

    private String trimSummary(String summary) {
        if (summary == null) {
            return null;
        }
        String normalized = summary.replace("```", "").trim();
        return normalized.length() > 240 ? normalized.substring(0, 240) : normalized;
    }

    private String formatOverviewHighlight(Map<String, Object> row) {
        String indicator = String.valueOf(row.get("指标"));
        Object rawValue = row.get("数值");
        String unit = row.get("单位") == null ? "" : String.valueOf(row.get("单位"));
        String value = rawValue == null ? "-" : String.valueOf(rawValue);
        if (!unit.isBlank() && value.endsWith(unit)) {
            return indicator + "为" + value;
        }
        return indicator + "为" + value + unit;
    }

    private Long resolveUserId(Map<?, ?> request, HttpServletRequest servletRequest) {
        if (request != null && request.get("userId") != null) {
            return Long.parseLong(String.valueOf(request.get("userId")));
        }
        Object requestUserId = servletRequest != null ? servletRequest.getAttribute("userId") : null;
        if (requestUserId != null) {
            return Long.parseLong(String.valueOf(requestUserId));
        }
        return null;
    }

    private String resolveUsername(Long userId) {
        if (userId == null) {
            return "anonymous";
        }
        if (userId == 1L) {
            return "admin";
        }
        if (userId == 2L) {
            return "analyst";
        }
        return "user-" + userId;
    }

    private void saveSuccessHistory(String text, Long userId, Long dataSourceId, List<Map<String, Object>> data, long executionTime) {
        try {
            QueryHistory history = QueryHistory.builder()
                    .userId(userId == null ? 0L : userId)
                    .username(resolveUsername(userId))
                    .queryName(text.length() > 50 ? text.substring(0, 50) : text)
                    .queryType("NATURAL_LANGUAGE")
                    .queryContent(text)
                    .datasourceId(dataSourceId)
                    .resultData(objectMapper.writeValueAsString(data.size() > 10 ? data.subList(0, 10) : data))
                    .duration(executionTime)
                    .status("SUCCESS")
                    .isFavorite(false)
                    .build();
            queryHistoryService.save(history);
            log.info("查询历史已保存 - ID: {}", history.getId());
        } catch (Exception e) {
            log.error("保存查询历史失败", e);
        }
    }

    private void saveFailedHistory(String text, Long userId, long executionTime, String errorMessage) {
        QueryHistory history = QueryHistory.builder()
                .userId(userId == null ? 0L : userId)
                .username(resolveUsername(userId))
                .queryName(text.length() > 50 ? text.substring(0, 50) : text)
                .queryType("NATURAL_LANGUAGE")
                .queryContent(text)
                .duration(executionTime)
                .status("FAILED")
                .errorMsg(errorMessage)
                .isFavorite(false)
                .build();
        queryHistoryService.save(history);
    }

    /**
     * 从查询文本中提取时间范围
     */
    private String extractTimeRange(String query) {
        if (query.contains("本月")) {
            return java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy年MM月"));
        } else if (query.contains("本季度")) {
            int month = java.time.LocalDate.now().getMonthValue();
            int quarter = (month - 1) / 3 + 1;
            return java.time.LocalDate.now().getYear() + "年Q" + quarter;
        } else if (query.contains("今年")) {
            return java.time.LocalDate.now().getYear() + "年";
        } else if (query.contains("上月")) {
            return java.time.LocalDate.now().minusMonths(1).format(java.time.format.DateTimeFormatter.ofPattern("yyyy年MM月"));
        } else if (query.contains("上季度")) {
            int month = java.time.LocalDate.now().minusMonths(3).getMonthValue();
            int quarter = (month - 1) / 3 + 1;
            return java.time.LocalDate.now().minusMonths(3).getYear() + "年Q" + quarter;
        } else {
            return java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy年MM月"));
        }
    }

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

    private static class MetricScore {
        private int score;
        private double similarity;
        private int directHits;
    }
}
