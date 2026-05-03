package com.chatbi.controller;

import com.chatbi.config.AiConfig;
import com.chatbi.entity.Metric;
import com.chatbi.repository.DataSourceMapper;
import com.chatbi.repository.MetricMapper;
import com.chatbi.entity.Synonym;
import com.chatbi.repository.SynonymMapper;
import com.chatbi.service.AiQueryService;
import com.chatbi.service.AiModelService;
import com.chatbi.service.AccessAlertService;
import com.chatbi.service.BusinessInsightService;
import com.chatbi.service.ConversationService;
import com.chatbi.service.EnterpriseChartCatalogService;
import com.chatbi.service.MetricMatchingService;
import com.chatbi.service.QueryExecutionService;
import com.chatbi.service.QueryResultAnalysisService;
import com.chatbi.service.SmartRecommendationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConversationController 测试")
class ConversationControllerTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private record SemanticBenchmarkCase(
        String caseId,
        String category,
        String contextMessage,
        String followupMessage,
        boolean expectedConflict,
        String expectedSecondaryMetric
    ) {
    }

    @Mock
    private AiQueryService aiQueryService;
    @Mock
    private QueryExecutionService queryExecutionService;
    @Mock
    private AiModelService aiModelService;
    @Mock
    private DataSourceMapper dataSourceMapper;
    @Mock
    private MetricMapper metricMapper;
    @Mock
    private SynonymMapper synonymMapper;
    @Mock
    private SmartRecommendationService recommendationService;
    @Mock
    private QueryResultAnalysisService analysisService;
    @Mock
    private BusinessInsightService businessInsightService;
    @Mock
    private EnterpriseChartCatalogService enterpriseChartCatalogService;
    @Mock
    private AccessAlertService accessAlertService;
    @Mock
    private MetricMatchingService metricMatchingService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ConversationService conversationService = new ConversationService(new ObjectMapper());
        AiConfig aiConfig = new AiConfig();
        aiConfig.setEnabled(false);
        aiConfig.setDefaultProvider("kimi");
        aiConfig.setProviders(Map.of("kimi", new AiConfig.ProviderConfig()));

        ConversationController controller = new ConversationController(
            conversationService,
            metricMatchingService,
            aiQueryService,
            aiModelService,
            queryExecutionService,
            dataSourceMapper,
            metricMapper,
            synonymMapper,
            recommendationService,
            analysisService,
            businessInsightService,
            enterpriseChartCatalogService,
            accessAlertService,
            aiConfig
        );

        Metric sales = new Metric();
        sales.setId(1L);
        sales.setCode("SALES_REVENUE");
        sales.setName("销售额");
        sales.setDefinition("按时间周期统计销售收入");
        sales.setStatus("active");

        Metric grossMargin = new Metric();
        grossMargin.setId(2L);
        grossMargin.setCode("GROSS_MARGIN");
        grossMargin.setName("毛利率");
        grossMargin.setDefinition("收入减成本后的毛利占比");
        grossMargin.setStatus("active");

        Metric cashCollection = new Metric();
        cashCollection.setId(3L);
        cashCollection.setCode("CASH_COLLECTION");
        cashCollection.setName("回款额");
        cashCollection.setDefinition("销售订单实际回款金额");
        cashCollection.setStatus("active");

        lenient().when(metricMapper.selectList(any())).thenReturn(List.of(sales, grossMargin, cashCollection));
        lenient().when(synonymMapper.selectList(any())).thenReturn(List.of());
        lenient().when(metricMatchingService.getActiveMetrics()).thenReturn(List.of(sales, grossMargin, cashCollection));
        lenient().when(metricMatchingService.getAllSynonyms()).thenReturn(List.of());
        lenient().when(metricMatchingService.isGreetingIntent(anyString())).thenReturn(false);
        lenient().when(metricMatchingService.isOverviewIntent(anyString())).thenReturn(false);
        lenient().when(metricMatchingService.buildGuidedSuggestions(anyString(), any())).thenReturn(List.of("先给我一个数据概览"));
        lenient().when(metricMatchingService.buildMetricExamples(anyString())).thenReturn(List.of("本月核心指标是多少？"));
        lenient().when(metricMatchingService.inferFallbackMetrics(anyString())).thenReturn(List.of("销售额", "毛利率", "回款额"));
        lenient().when(enterpriseChartCatalogService.getSummary()).thenReturn(Map.of("total", 119));
        lenient().when(enterpriseChartCatalogService.getFamilies()).thenReturn(List.of("bar", "line", "pie"));
        lenient().when(enterpriseChartCatalogService.getVariants()).thenReturn(List.of("classic", "enterprise"));
        lenient().when(enterpriseChartCatalogService.getCatalog()).thenReturn(List.of(
            Map.of("type", "bar.enterprise", "family", "bar", "variant", "enterprise", "displayName", "bar-enterprise"),
            Map.of("type", "line.enterprise", "family", "line", "variant", "enterprise", "displayName", "line-enterprise")
        ));
        lenient().when(enterpriseChartCatalogService.getFeaturedTypes(anyInt())).thenReturn(List.of(
            Map.of("type", "bar.enterprise", "family", "bar"),
            Map.of("type", "line.enterprise", "family", "line")
        ));
        lenient().when(businessInsightService.getOverviewRows()).thenReturn(List.of(
            Map.of("指标", "累计销售额", "数值", 568000, "单位", "元"),
            Map.of("指标", "活跃客户数", "数值", 212, "单位", "家")
        ));

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("泛化问题降级为数据概览而非报错")
    void testSendMessage_FallbackToGuidedDiscovery() throws Exception {
        mockMvc.perform(post("/api/conversation/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"帮我看一下这个平台怎么样\",\"userId\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.metric").value("数据概览"))
            .andExpect(jsonPath("$.data.source").value("guided-discovery"))
            .andExpect(jsonPath("$.data.diagnosis.code").value("METRIC_NOT_RECOGNIZED"))
            .andExpect(jsonPath("$.data.suggestions[0]").value("先给我一个数据概览"))
            .andExpect(jsonPath("$.data.candidateMetrics").isArray());
    }

    @Test
    @DisplayName("研发类泛问法返回研发场景建议")
    void testSendMessage_RndIntentShouldReturnScenarioSuggestions() throws Exception {
        mockMvc.perform(post("/api/conversation/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"最近研发效能怎么样\",\"userId\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.metric").value("数据概览"))
            .andExpect(jsonPath("$.data.source").value("guided-discovery"))
            .andExpect(jsonPath("$.data.diagnosis.guidanceScenario").value("综合分析场景"))
            .andExpect(jsonPath("$.data.suggestions", hasItems(containsString("销售额"))));
    }

    @Test
    @DisplayName("能力接口返回运行状态和指标列表")
    void testGetCapabilities() throws Exception {
        mockMvc.perform(get("/api/conversation/capabilities"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.ai.mode").value("semantic"))
            .andExpect(jsonPath("$.data.metricCount").value(3))
            .andExpect(jsonPath("$.data.chartTypeCount").value(119))
            .andExpect(jsonPath("$.data.chartCatalog[0].type").value("bar.enterprise"))
            .andExpect(jsonPath("$.data.quickStartMetrics[0]").value("销售额"))
            .andExpect(jsonPath("$.data.fallbackPrompts[0]").value("先给我一个数据概览"))
            .andExpect(jsonPath("$.data.metrics[0].name").value("销售额"));
    }

    @Test
    @DisplayName("带空格的指标问法也能命中业务指标")
    void testSendMessage_MatchMetricWithSpacingNoise() throws Exception {
        when(businessInsightService.queryMetric(any(Metric.class), anyString())).thenReturn(
            new BusinessInsightService.QueryPlan(
                "SELECT '销售额' AS metric_name, 12345 AS metric_value",
                "指标",
                List.of(Map.of("metric_name", "销售额", "metric_value", 12345))
            )
        );

        mockMvc.perform(post("/api/conversation/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"请帮我看 销 售-额 趋势\",\"userId\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.metric").value("销售额"));
    }

    @Test
    @DisplayName("轻微错别字指标问法也能命中业务指标")
    void testSendMessage_MatchMetricWithMinorTypo() throws Exception {
        when(businessInsightService.queryMetric(any(Metric.class), anyString())).thenReturn(
            new BusinessInsightService.QueryPlan(
                "SELECT '销售额' AS metric_name, 12345 AS metric_value",
                "指标",
                List.of(Map.of("metric_name", "销售额", "metric_value", 12345))
            )
        );

        mockMvc.perform(post("/api/conversation/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"本月销受额趋势如何\",\"userId\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.metric").value("销售额"));
    }

    @Test
    @DisplayName("口语化错字问法也能命中业务指标")
    void testSendMessage_MatchMetricWithColloquialTypoIntent() throws Exception {
        when(businessInsightService.queryMetric(any(Metric.class), anyString())).thenReturn(
            new BusinessInsightService.QueryPlan(
                "SELECT '销售额' AS metric_name, 12345 AS metric_value",
                "指标",
                List.of(Map.of("metric_name", "销售额", "metric_value", 12345))
            )
        );

        mockMvc.perform(post("/api/conversation/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"帮我看下销受额情况\",\"userId\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.metric").value("销售额"))
            .andExpect(jsonPath("$.data.diagnosis.intentTags", hasItem("口语查看")))
            .andExpect(jsonPath("$.data.diagnosis.candidateMetricsPreview", hasItem("销售额")));
    }

    @Test
    @DisplayName("多组口语问法保持稳定识别并返回诊断依据")
    void testSendMessage_ColloquialRegressionSet() throws Exception {
        when(businessInsightService.queryMetric(any(Metric.class), anyString())).thenReturn(
            new BusinessInsightService.QueryPlan(
                "SELECT '销售额' AS metric_name, 12345 AS metric_value",
                "指标",
                List.of(Map.of("metric_name", "销售额", "metric_value", 12345))
            )
        );

        Synonym salesSynonym = new Synonym();
        salesSynonym.setStandardWord("销售额");
        salesSynonym.setAliases(List.of("revenue", "营收"));
        when(metricMatchingService.getAllSynonyms()).thenReturn(List.of(salesSynonym));

        List<String> prompts = List.of(
            "帮我看下本月销受额",
            "销售额这周咋样",
            "给我盘下本月营收行不行",
            "帮我看下销售额情况",
            "看看本月revenue走势"
        );

        ObjectMapper mapper = new ObjectMapper();
        for (String prompt : prompts) {
            String content = mockMvc.perform(post("/api/conversation/message")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"message\":\"" + prompt + "\",\"userId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.metric").value("销售额"))
                .andExpect(jsonPath("$.data.diagnosis.candidateMetricsPreview", hasItem("销售额")))
                .andReturn()
                .getResponse()
                .getContentAsString();

            JsonNode diagnosis = mapper.readTree(content).path("data").path("diagnosis");
            JsonNode intentTags = diagnosis.path("intentTags");
            assertTrue(intentTags.isArray() && intentTags.size() > 0,
                "口语问法应携带至少一个诊断标签: " + prompt);
        }
    }

    @Test
    @DisplayName("业务查询异常时自动降级为数据概览，不返回失败")
    void testSendMessage_RecoveryOnQueryFailure() throws Exception {
        when(businessInsightService.queryMetric(any(Metric.class), anyString())).thenThrow(new IllegalStateException("模拟业务查询异常"));

        mockMvc.perform(post("/api/conversation/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"本月销售额\",\"userId\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.metric").value("数据概览"))
            .andExpect(jsonPath("$.data.source").value("guided-discovery"))
            .andExpect(jsonPath("$.data.diagnosis.code").value("CONVERSATION_EXCEPTION"))
            .andExpect(jsonPath("$.data.recoveredFromError").value(true));
    }

    @Test
    @DisplayName("降级查询概览异常时仍返回可继续结果")
    void testSendMessage_RecoveryShouldUseLocalOverviewFallback() throws Exception {
        when(businessInsightService.queryMetric(any(Metric.class), anyString())).thenThrow(new IllegalStateException("模拟业务查询异常"));
        when(businessInsightService.getOverviewRows()).thenThrow(new IllegalStateException("模拟概览查询异常"));

        mockMvc.perform(post("/api/conversation/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"本月销售额\",\"userId\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.metric").value("数据概览"))
            .andExpect(jsonPath("$.data.source").value("guided-discovery"))
            .andExpect(jsonPath("$.data.diagnosis.code").value("CONVERSATION_EXCEPTION"))
            .andExpect(jsonPath("$.data.recoveredFromError").value(true))
            .andExpect(jsonPath("$.data.dataCount").value(0));
    }

    @Test
    @DisplayName("歧义指标问法返回澄清引导而不是误命中")
    void testSendMessage_AmbiguousMetricShouldDisambiguate() throws Exception {
        Metric sales = new Metric();
        sales.setId(1L);
        sales.setCode("SALES_REVENUE");
        sales.setName("销售额");
        sales.setDefinition("按时间周期统计销售收入");
        sales.setStatus("active");

        Metric salesTarget = new Metric();
        salesTarget.setId(3L);
        salesTarget.setCode("SALES_TARGET");
        salesTarget.setName("销售目标额");
        salesTarget.setDefinition("销售目标金额");
        salesTarget.setStatus("active");

        Metric grossMargin = new Metric();
        grossMargin.setId(2L);
        grossMargin.setCode("GROSS_MARGIN");
        grossMargin.setName("毛利率");
        grossMargin.setDefinition("收入减成本后的毛利占比");
        grossMargin.setStatus("active");

        when(metricMatchingService.getActiveMetrics()).thenReturn(List.of(sales, salesTarget, grossMargin));

        mockMvc.perform(post("/api/conversation/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"请比较销售额和销售目标额\",\"userId\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.metric").value("数据概览"))
            .andExpect(jsonPath("$.data.source").value("guided-disambiguation"))
            .andExpect(jsonPath("$.data.diagnosis.code").value("AMBIGUOUS_METRIC"))
            .andExpect(jsonPath("$.data.disambiguation").value(true))
            .andExpect(jsonPath("$.data.suggestions[0]", containsString("对比")))
            .andExpect(jsonPath("$.data.candidateMetrics", hasItems("销售额", "销售目标额")));
    }

    @Test
    @DisplayName("多轮对话指代词应复用上文指标继续分析")
    void testSendMessage_ContextReferenceShouldReuseLastMetric() throws Exception {
        when(businessInsightService.queryMetric(any(Metric.class), anyString())).thenReturn(
            new BusinessInsightService.QueryPlan(
                "SELECT '销售额' AS metric_name, 12345 AS metric_value",
                "指标",
                List.of(Map.of("metric_name", "销售额", "metric_value", 12345))
            )
        );

        String createResponse = mockMvc.perform(post("/api/conversation/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":1}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String conversationId = new ObjectMapper().readTree(createResponse).path("data").path("conversationId").asText();

        mockMvc.perform(post("/api/conversation/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"conversationId\":\"" + conversationId + "\",\"message\":\"本月销售额\",\"userId\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.metric").value("销售额"));

        mockMvc.perform(post("/api/conversation/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"conversationId\":\"" + conversationId + "\",\"message\":\"这个指标同比呢\",\"userId\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.metric").value("销售额"))
            .andExpect(jsonPath("$.data.diagnosis.code").value("CONTEXT_METRIC_REUSED"))
            .andExpect(jsonPath("$.data.diagnosis.intentTags", hasItem("上下文指代")));
    }

    @Test
    @DisplayName("多轮对话指代追问应沿用上文时间范围")
    void testSendMessage_ContextReferenceShouldReuseLastTimeRange() throws Exception {
        when(businessInsightService.queryMetric(any(Metric.class), anyString())).thenReturn(
            new BusinessInsightService.QueryPlan(
                "SELECT '销售额' AS metric_name, 12345 AS metric_value",
                "指标",
                List.of(Map.of("metric_name", "销售额", "metric_value", 12345))
            )
        );

        String createResponse = mockMvc.perform(post("/api/conversation/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":1}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String conversationId = new ObjectMapper().readTree(createResponse).path("data").path("conversationId").asText();

        mockMvc.perform(post("/api/conversation/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"conversationId\":\"" + conversationId + "\",\"message\":\"上周销售额\",\"userId\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.metric").value("销售额"));

        mockMvc.perform(post("/api/conversation/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"conversationId\":\"" + conversationId + "\",\"message\":\"这个指标同比呢\",\"userId\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.metric").value("销售额"))
            .andExpect(jsonPath("$.data.diagnosis.code").value("CONTEXT_METRIC_REUSED"))
            .andExpect(jsonPath("$.data.diagnosis.reason", containsString("上周")));

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(businessInsightService, times(2)).queryMetric(any(Metric.class), queryCaptor.capture());
        List<String> capturedQueries = queryCaptor.getAllValues();
        assertTrue(capturedQueries.get(1).contains("上周"), "追问应继承上轮时间范围");
    }

    @Test
    @DisplayName("多轮复合指代对比应复用上文时间并拼接当前时间")
    void testSendMessage_ContextReferenceCompareShouldUseContextAndExplicitTime() throws Exception {
        when(businessInsightService.queryMetric(any(Metric.class), anyString())).thenReturn(
            new BusinessInsightService.QueryPlan(
                "SELECT '销售额' AS metric_name, 12345 AS metric_value",
                "指标",
                List.of(Map.of("metric_name", "销售额", "metric_value", 12345))
            )
        );

        String createResponse = mockMvc.perform(post("/api/conversation/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":1}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String conversationId = new ObjectMapper().readTree(createResponse).path("data").path("conversationId").asText();

        mockMvc.perform(post("/api/conversation/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"conversationId\":\"" + conversationId + "\",\"message\":\"本周销售额\",\"userId\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.metric").value("销售额"));

        mockMvc.perform(post("/api/conversation/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"conversationId\":\"" + conversationId + "\",\"message\":\"这个和上周那个对比\",\"userId\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.metric").value("销售额"))
            .andExpect(jsonPath("$.data.diagnosis.code").value("CONTEXT_METRIC_REUSED"))
            .andExpect(jsonPath("$.data.diagnosis.reason", containsString("本周 vs 上周")));

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(businessInsightService, times(2)).queryMetric(any(Metric.class), queryCaptor.capture());
        List<String> capturedQueries = queryCaptor.getAllValues();
        assertTrue(capturedQueries.get(1).contains("本周"), "对比追问应包含上文时间范围");
        assertTrue(capturedQueries.get(1).contains("上周"), "对比追问应包含当前显式时间范围");
        assertTrue(capturedQueries.get(1).contains("对比"), "对比追问应保留对比语义");
    }

    @Test
    @DisplayName("多轮指代应支持上文指标与新指标的复合对比")
    void testSendMessage_ContextReferenceShouldCompareWithSecondaryMetric() throws Exception {
        when(businessInsightService.queryMetric(any(Metric.class), anyString())).thenReturn(
            new BusinessInsightService.QueryPlan(
                "SELECT '销售额' AS metric_name, 12345 AS metric_value",
                "指标",
                List.of(Map.of("metric_name", "销售额", "metric_value", 12345))
            )
        );

        String createResponse = mockMvc.perform(post("/api/conversation/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":1}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String conversationId = new ObjectMapper().readTree(createResponse).path("data").path("conversationId").asText();

        mockMvc.perform(post("/api/conversation/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"conversationId\":\"" + conversationId + "\",\"message\":\"本周销售额\",\"userId\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.metric").value("销售额"));

        mockMvc.perform(post("/api/conversation/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"conversationId\":\"" + conversationId + "\",\"message\":\"这个和毛利率上周对比\",\"userId\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.metric").value("销售额"))
            .andExpect(jsonPath("$.data.diagnosis.code").value("CONTEXT_METRIC_REUSED"))
            .andExpect(jsonPath("$.data.diagnosis.reason", containsString("毛利率")))
            .andExpect(jsonPath("$.data.diagnosis.reason", containsString("本周 vs 上周")))
            .andExpect(jsonPath("$.data.diagnosis.slotConflict").value(false))
            .andExpect(jsonPath("$.data.diagnosis.slotEvidence.secondaryMetric.value").value("毛利率"))
            .andExpect(jsonPath("$.data.diagnosis.slotEvidence.secondaryMetric.source").value("semantic-candidates"));

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(businessInsightService, times(2)).queryMetric(any(Metric.class), queryCaptor.capture());
        List<String> capturedQueries = queryCaptor.getAllValues();
        assertTrue(capturedQueries.get(1).contains("销售额"), "对比追问应保留上文指标");
        assertTrue(capturedQueries.get(1).contains("毛利率"), "对比追问应包含新指标");
        assertTrue(capturedQueries.get(1).contains("本周"), "对比追问应包含上文时间范围");
        assertTrue(capturedQueries.get(1).contains("上周"), "对比追问应包含当前显式时间范围");
    }

    @Test
    @DisplayName("多轮指代出现多个候选对比指标时应返回槽位冲突诊断")
    void testSendMessage_ContextReferenceShouldExposeSecondaryMetricConflict() throws Exception {
        when(businessInsightService.queryMetric(any(Metric.class), anyString())).thenReturn(
            new BusinessInsightService.QueryPlan(
                "SELECT '销售额' AS metric_name, 12345 AS metric_value",
                "指标",
                List.of(Map.of("metric_name", "销售额", "metric_value", 12345))
            )
        );

        String createResponse = mockMvc.perform(post("/api/conversation/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":1}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String conversationId = new ObjectMapper().readTree(createResponse).path("data").path("conversationId").asText();

        mockMvc.perform(post("/api/conversation/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"conversationId\":\"" + conversationId + "\",\"message\":\"本周销售额\",\"userId\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.metric").value("销售额"));

        mockMvc.perform(post("/api/conversation/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"conversationId\":\"" + conversationId + "\",\"message\":\"这个和毛利率或回款额上周对比\",\"userId\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.metric").value("销售额"))
            .andExpect(jsonPath("$.data.diagnosis.code").value("CONTEXT_METRIC_REUSED"))
            .andExpect(jsonPath("$.data.diagnosis.slotConflict").value(true))
            .andExpect(jsonPath("$.data.diagnosis.slotEvidence.secondaryMetric.conflict").value(true))
            .andExpect(jsonPath("$.data.diagnosis.slotEvidence.secondaryMetric.candidates", hasItems("毛利率", "回款额")))
            .andExpect(jsonPath("$.data.diagnosis.slotEvidence.secondaryMetric.rankedCandidates[0].metric", notNullValue()))
            .andExpect(jsonPath("$.data.diagnosis.slotEvidence.secondaryMetric.rankedCandidates[0].reason", notNullValue()))
            .andExpect(jsonPath("$.data.diagnosis.actions[0]", containsString("请明确对比指标")));
    }

    @Test
    @DisplayName("隐式比较问法命中单一指标时应自动执行对比")
    void testSendMessage_ContextReferenceImplicitCompareShouldExecute() throws Exception {
        when(businessInsightService.queryMetric(any(Metric.class), anyString())).thenReturn(
            new BusinessInsightService.QueryPlan(
                "SELECT '销售额' AS metric_name, 12345 AS metric_value",
                "指标",
                List.of(Map.of("metric_name", "销售额", "metric_value", 12345))
            )
        );

        String createResponse = mockMvc.perform(post("/api/conversation/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":1}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String conversationId = new ObjectMapper().readTree(createResponse).path("data").path("conversationId").asText();

        mockMvc.perform(post("/api/conversation/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"conversationId\":\"" + conversationId + "\",\"message\":\"本周销售额\",\"userId\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.metric").value("销售额"));

        mockMvc.perform(post("/api/conversation/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"conversationId\":\"" + conversationId + "\",\"message\":\"这个和毛利率上周怎么样？\",\"userId\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.diagnosis.code").value("CONTEXT_METRIC_REUSED"))
            .andExpect(jsonPath("$.data.diagnosis.slotConflict").value(false))
            .andExpect(jsonPath("$.data.diagnosis.slotEvidence.secondaryMetric.value").value("毛利率"));

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(businessInsightService, times(2)).queryMetric(any(Metric.class), queryCaptor.capture());
        List<String> capturedQueries = queryCaptor.getAllValues();
        assertTrue(capturedQueries.get(1).contains("销售额"), "隐式比较应保留主指标");
        assertTrue(capturedQueries.get(1).contains("毛利率"), "隐式比较应命中二级指标");
    }

    @Test
    @DisplayName("无疑问词并列指标场景应触发隐式对比")
    void testSendMessage_ContextReferenceImplicitCompareWithoutQuestionShouldExecute() throws Exception {
        when(businessInsightService.queryMetric(any(Metric.class), anyString())).thenReturn(
            new BusinessInsightService.QueryPlan(
                "SELECT '销售额' AS metric_name, 12345 AS metric_value",
                "指标",
                List.of(Map.of("metric_name", "销售额", "metric_value", 12345))
            )
        );

        String createResponse = mockMvc.perform(post("/api/conversation/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":1}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String conversationId = new ObjectMapper().readTree(createResponse).path("data").path("conversationId").asText();

        mockMvc.perform(post("/api/conversation/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"conversationId\":\"" + conversationId + "\",\"message\":\"本周销售额\",\"userId\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.metric").value("销售额"));

        mockMvc.perform(post("/api/conversation/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"conversationId\":\"" + conversationId + "\",\"message\":\"这个和毛利率上周一起看\",\"userId\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.diagnosis.code").value("CONTEXT_METRIC_REUSED"))
            .andExpect(jsonPath("$.data.diagnosis.slotConflict").value(false))
            .andExpect(jsonPath("$.data.diagnosis.slotEvidence.secondaryMetric.value").value("毛利率"));

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(businessInsightService, times(2)).queryMetric(any(Metric.class), queryCaptor.capture());
        List<String> capturedQueries = queryCaptor.getAllValues();
        assertTrue(capturedQueries.get(1).contains("销售额"), "并列隐式比较应保留主指标");
        assertTrue(capturedQueries.get(1).contains("毛利率"), "并列隐式比较应命中二级指标");
    }

    @Test
    @DisplayName("并列双时间表达应优先选择与上下文不同的时间槽位")
    void testSendMessage_ContextReferenceDualTimeShouldPreferDifferentExplicitTime() throws Exception {
        when(businessInsightService.queryMetric(any(Metric.class), anyString())).thenReturn(
            new BusinessInsightService.QueryPlan(
                "SELECT '销售额' AS metric_name, 12345 AS metric_value",
                "指标",
                List.of(Map.of("metric_name", "销售额", "metric_value", 12345))
            )
        );

        String createResponse = mockMvc.perform(post("/api/conversation/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":1}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String conversationId = new ObjectMapper().readTree(createResponse).path("data").path("conversationId").asText();

        mockMvc.perform(post("/api/conversation/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"conversationId\":\"" + conversationId + "\",\"message\":\"本周销售额\",\"userId\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.metric").value("销售额"));

        mockMvc.perform(post("/api/conversation/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"conversationId\":\"" + conversationId + "\",\"message\":\"这个和毛利率本周上周对比\",\"userId\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.diagnosis.code").value("CONTEXT_METRIC_REUSED"))
            .andExpect(jsonPath("$.data.diagnosis.slotConflict").value(false));

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(businessInsightService, times(2)).queryMetric(any(Metric.class), queryCaptor.capture());
        List<String> capturedQueries = queryCaptor.getAllValues();
        assertTrue(capturedQueries.get(1).contains("销售额 本周 和 毛利率 上周"), "双时间并列表达应优先采用与上下文不同的显式时间");
        assertTrue(capturedQueries.get(1).contains("销售额"), "双时间并列表达应保留主指标");
        assertTrue(capturedQueries.get(1).contains("毛利率"), "双时间并列表达应包含对比指标");
    }

    @Test
    @DisplayName("多时间多指标并列应按二级指标就近绑定时间槽位")
    void testSendMessage_ContextReferenceShouldBindSecondaryMetricNearestTime() throws Exception {
        when(businessInsightService.queryMetric(any(Metric.class), anyString())).thenReturn(
            new BusinessInsightService.QueryPlan(
                "SELECT '销售额' AS metric_name, 12345 AS metric_value",
                "指标",
                List.of(Map.of("metric_name", "销售额", "metric_value", 12345))
            )
        );

        String createResponse = mockMvc.perform(post("/api/conversation/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":1}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String conversationId = new ObjectMapper().readTree(createResponse).path("data").path("conversationId").asText();

        mockMvc.perform(post("/api/conversation/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"conversationId\":\"" + conversationId + "\",\"message\":\"本月销售额\",\"userId\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.metric").value("销售额"));

        mockMvc.perform(post("/api/conversation/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"conversationId\":\"" + conversationId + "\",\"message\":\"这个上周和毛利率本周对比\",\"userId\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.diagnosis.code").value("CONTEXT_METRIC_REUSED"))
            .andExpect(jsonPath("$.data.diagnosis.slotConflict").value(false))
            .andExpect(jsonPath("$.data.diagnosis.slotEvidence.secondaryMetric.timeReference").value("本周"));

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(businessInsightService, times(2)).queryMetric(any(Metric.class), queryCaptor.capture());
        List<String> capturedQueries = queryCaptor.getAllValues();
        assertTrue(capturedQueries.get(1).contains("销售额 本月 和 毛利率 本周"), "二级指标应绑定就近时间词");
    }

    @Test
    @DisplayName("多指标多时间并列时应按连接词后首指标优先级执行")
    void testSendMessage_ContextReferenceShouldPrioritizeSecondaryMetricAfterConnector() throws Exception {
        when(businessInsightService.queryMetric(any(Metric.class), anyString())).thenReturn(
            new BusinessInsightService.QueryPlan(
                "SELECT '销售额' AS metric_name, 12345 AS metric_value",
                "指标",
                List.of(Map.of("metric_name", "销售额", "metric_value", 12345))
            )
        );

        String createResponse = mockMvc.perform(post("/api/conversation/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":1}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String conversationId = new ObjectMapper().readTree(createResponse).path("data").path("conversationId").asText();

        mockMvc.perform(post("/api/conversation/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"conversationId\":\"" + conversationId + "\",\"message\":\"本月销售额\",\"userId\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.metric").value("销售额"));

        mockMvc.perform(post("/api/conversation/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"conversationId\":\"" + conversationId + "\",\"message\":\"这个和毛利率本周以及回款额上月对比\",\"userId\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.diagnosis.code").value("CONTEXT_METRIC_REUSED"))
            .andExpect(jsonPath("$.data.diagnosis.slotConflict").value(false))
            .andExpect(jsonPath("$.data.diagnosis.slotEvidence.secondaryMetric.value").value("毛利率"))
            .andExpect(jsonPath("$.data.diagnosis.slotEvidence.secondaryMetric.source").value("semantic-candidates-priority"))
            .andExpect(jsonPath("$.data.diagnosis.slotEvidence.secondaryMetric.timeReference").value("本周"));

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(businessInsightService, times(2)).queryMetric(any(Metric.class), queryCaptor.capture());
        List<String> capturedQueries = queryCaptor.getAllValues();
        assertTrue(capturedQueries.get(1).startsWith("销售额 本月 和 毛利率 本周 对比"), "多候选并列应优先使用连接词后首个指标及其时间槽位");
    }

    @Test
    @DisplayName("含分别语义的多指标多时间并列应按顺序优先绑定首指标")
    void testSendMessage_ContextReferenceShouldPrioritizeSecondaryMetricForSeparateSequence() throws Exception {
        when(businessInsightService.queryMetric(any(Metric.class), anyString())).thenReturn(
            new BusinessInsightService.QueryPlan(
                "SELECT '销售额' AS metric_name, 12345 AS metric_value",
                "指标",
                List.of(Map.of("metric_name", "销售额", "metric_value", 12345))
            )
        );

        String createResponse = mockMvc.perform(post("/api/conversation/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":1}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String conversationId = new ObjectMapper().readTree(createResponse).path("data").path("conversationId").asText();

        mockMvc.perform(post("/api/conversation/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"conversationId\":\"" + conversationId + "\",\"message\":\"本月销售额\",\"userId\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.metric").value("销售额"));

        mockMvc.perform(post("/api/conversation/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"conversationId\":\"" + conversationId + "\",\"message\":\"这个和毛利率本周回款额上月分别对比\",\"userId\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.diagnosis.code").value("CONTEXT_METRIC_REUSED"))
            .andExpect(jsonPath("$.data.diagnosis.slotConflict").value(false))
            .andExpect(jsonPath("$.data.diagnosis.slotEvidence.secondaryMetric.value").value("毛利率"))
            .andExpect(jsonPath("$.data.diagnosis.slotEvidence.secondaryMetric.source").value("semantic-candidates-priority"))
            .andExpect(jsonPath("$.data.diagnosis.slotEvidence.secondaryMetric.timeReference").value("本周"));

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(businessInsightService, times(2)).queryMetric(any(Metric.class), queryCaptor.capture());
        List<String> capturedQueries = queryCaptor.getAllValues();
        assertTrue(capturedQueries.get(1).startsWith("销售额 本月 和 毛利率 本周 对比"), "分别语义下应优先绑定首个指标的时间槽位");
    }

    @Test
    @DisplayName("无时间槽位支撑的多指标同时比较仍应返回冲突澄清")
    void testSendMessage_ContextReferenceShouldKeepConflictWithoutTimeSequenceEvidence() throws Exception {
        when(businessInsightService.queryMetric(any(Metric.class), anyString())).thenReturn(
            new BusinessInsightService.QueryPlan(
                "SELECT '销售额' AS metric_name, 12345 AS metric_value",
                "指标",
                List.of(Map.of("metric_name", "销售额", "metric_value", 12345))
            )
        );

        String createResponse = mockMvc.perform(post("/api/conversation/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":1}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String conversationId = new ObjectMapper().readTree(createResponse).path("data").path("conversationId").asText();

        mockMvc.perform(post("/api/conversation/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"conversationId\":\"" + conversationId + "\",\"message\":\"本月销售额\",\"userId\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.metric").value("销售额"));

        mockMvc.perform(post("/api/conversation/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"conversationId\":\"" + conversationId + "\",\"message\":\"这个和毛利率回款额同时对比\",\"userId\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.diagnosis.code").value("CONTEXT_METRIC_REUSED"))
            .andExpect(jsonPath("$.data.diagnosis.slotConflict").value(true))
            .andExpect(jsonPath("$.data.diagnosis.slotEvidence.secondaryMetric.conflict").value(true))
            .andExpect(jsonPath("$.data.diagnosis.slotEvidence.secondaryMetric.candidates", hasItems("毛利率", "回款额")));
    }

    @Test
    @DisplayName("多连接符长句且首片段不唯一时应按连接词后顺序优先级选择指标")
    void testSendMessage_ContextReferenceShouldPrioritizeSecondaryMetricForMixedConnectors() throws Exception {
        when(businessInsightService.queryMetric(any(Metric.class), anyString())).thenReturn(
            new BusinessInsightService.QueryPlan(
                "SELECT '销售额' AS metric_name, 12345 AS metric_value",
                "指标",
                List.of(Map.of("metric_name", "销售额", "metric_value", 12345))
            )
        );

        String createResponse = mockMvc.perform(post("/api/conversation/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":1}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String conversationId = new ObjectMapper().readTree(createResponse).path("data").path("conversationId").asText();

        mockMvc.perform(post("/api/conversation/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"conversationId\":\"" + conversationId + "\",\"message\":\"本月销售额\",\"userId\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.metric").value("销售额"));

        mockMvc.perform(post("/api/conversation/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"conversationId\":\"" + conversationId + "\",\"message\":\"这个和毛利率回款额本周还有回款额上月对比\",\"userId\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.diagnosis.code").value("CONTEXT_METRIC_REUSED"))
            .andExpect(jsonPath("$.data.diagnosis.slotConflict").value(false))
            .andExpect(jsonPath("$.data.diagnosis.slotEvidence.secondaryMetric.value").value("毛利率"))
            .andExpect(jsonPath("$.data.diagnosis.slotEvidence.secondaryMetric.source").value("semantic-candidates-priority"))
            .andExpect(jsonPath("$.data.diagnosis.slotEvidence.secondaryMetric.rankedCandidates[0].metric").value("毛利率"))
            .andExpect(jsonPath("$.data.diagnosis.slotEvidence.secondaryMetric.timeReference").value("本周"));

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(businessInsightService, times(2)).queryMetric(any(Metric.class), queryCaptor.capture());
        List<String> capturedQueries = queryCaptor.getAllValues();
        assertTrue(capturedQueries.get(1).startsWith("销售额 本月 和 毛利率 本周 对比"), "多连接符长句应优先绑定连接词后首个指标");
    }

    @Test
    @DisplayName("隐式比较低置信场景应强制澄清")
    void testSendMessage_ContextReferenceImplicitCompareShouldClarifyWhenLowConfidence() throws Exception {
        when(businessInsightService.queryMetric(any(Metric.class), anyString())).thenReturn(
            new BusinessInsightService.QueryPlan(
                "SELECT '销售额' AS metric_name, 12345 AS metric_value",
                "指标",
                List.of(Map.of("metric_name", "销售额", "metric_value", 12345))
            )
        );

        String createResponse = mockMvc.perform(post("/api/conversation/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":1}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String conversationId = new ObjectMapper().readTree(createResponse).path("data").path("conversationId").asText();

        Synonym profitSynonym = new Synonym();
        profitSynonym.setStandardWord("毛利率");
        profitSynonym.setAliases(List.of("利润"));
        when(metricMatchingService.getAllSynonyms()).thenReturn(List.of(profitSynonym));

        mockMvc.perform(post("/api/conversation/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"conversationId\":\"" + conversationId + "\",\"message\":\"本周销售额\",\"userId\":1}"))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/conversation/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"conversationId\":\"" + conversationId + "\",\"message\":\"这个跟利润上周怎么样？\",\"userId\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.diagnosis.slotConflict").value(true))
            .andExpect(jsonPath("$.data.diagnosis.slotEvidence.secondaryMetric.value").value(nullValue()))
            .andExpect(jsonPath("$.data.diagnosis.slotEvidence.secondaryMetric.candidates", hasItem("毛利率")))
            .andExpect(jsonPath("$.data.diagnosis.actions[0]", containsString("请明确对比指标")));
    }

    @Test
    @DisplayName("复杂多轮语义基线准确率应达到90%以上")
    void testSendMessage_MultiTurnSemanticBenchmarkShouldReachNinetyPercent() throws Exception {
        when(businessInsightService.queryMetric(any(Metric.class), anyString())).thenReturn(
            new BusinessInsightService.QueryPlan(
                "SELECT '销售额' AS metric_name, 12345 AS metric_value",
                "指标",
                List.of(Map.of("metric_name", "销售额", "metric_value", 12345))
            )
        );

        List<SemanticBenchmarkCase> cases = List.of(
            new SemanticBenchmarkCase("C01", "timeReferenceResolution", "上周销售额", "这个指标同比呢", false, null),
            new SemanticBenchmarkCase("C02", "timeReferenceResolution", "本周销售额", "这个和上周那个对比", false, null),
            new SemanticBenchmarkCase("C03", "secondaryMetricBinding", "本周销售额", "这个和毛利率上周怎么样？", false, "毛利率"),
            new SemanticBenchmarkCase("C04", "secondaryMetricBinding", "本周销售额", "这个和毛利率上周一起看", false, "毛利率"),
            new SemanticBenchmarkCase("C05", "secondaryMetricBinding", "本月销售额", "这个上周和毛利率本周对比", false, "毛利率"),
            new SemanticBenchmarkCase("C06", "secondaryMetricBinding", "本月销售额", "这个和毛利率本周以及回款额上月对比", false, "毛利率"),
            new SemanticBenchmarkCase("C07", "secondaryMetricBinding", "本月销售额", "这个和毛利率本周回款额上月分别对比", false, "毛利率"),
            new SemanticBenchmarkCase("C08", "secondaryMetricBinding", "本月销售额", "这个和毛利率回款额本周还有回款额上月对比", false, "毛利率"),
            new SemanticBenchmarkCase("C09", "disambiguationConflict", "本周销售额", "这个和毛利率或回款额上周对比", true, null),
            new SemanticBenchmarkCase("C10", "disambiguationConflict", "本周销售额", "这个跟利润上周怎么样？", true, null),
            new SemanticBenchmarkCase("C11", "secondaryMetricBinding", "本周销售额", "把这个和毛利率本周放一起看", false, "毛利率"),
            new SemanticBenchmarkCase("C12", "secondaryMetricBinding", "本周销售额", "对比一下这个和毛利率上周", false, "毛利率"),
            new SemanticBenchmarkCase("C13", "disambiguationConflict", "本月销售额", "这个和毛利率上周再和回款额本周看下", true, null),
            new SemanticBenchmarkCase("C14", "disambiguationConflict", "本月销售额", "这个和毛利率本周、回款额上周比较", true, null),
            new SemanticBenchmarkCase("C15", "disambiguationConflict", "本月销售额", "这个和毛利率上周以及回款额上周比较", true, null),
            new SemanticBenchmarkCase("C16", "disambiguationConflict", "本周销售额", "这个和毛利率或者回款额上周对比", true, null),
            new SemanticBenchmarkCase("C17", "secondaryMetricBinding", "本周销售额", "这个和利润或毛利率上周对比", false, "毛利率"),
            new SemanticBenchmarkCase("C18", "secondaryMetricBinding", "本周销售额", "这个和毛利率上周再看一下", false, "毛利率"),
            new SemanticBenchmarkCase("C19", "secondaryMetricBinding", "本月销售额", "这个和毛利率本周一起对比看看", false, "毛利率"),
            new SemanticBenchmarkCase("C20", "disambiguationConflict", "本月销售额", "这个和毛利率上周、回款额本周一起看", true, null)
        );

        int passed = 0;
        List<Map<String, Object>> failedCases = new ArrayList<>();
        for (SemanticBenchmarkCase benchmarkCase : cases) {
            String conversationId = createConversation();
            sendMessage(conversationId, benchmarkCase.contextMessage());
            JsonNode response = sendMessage(conversationId, benchmarkCase.followupMessage());

            JsonNode data = response.path("data");
            JsonNode diagnosis = data.path("diagnosis");
            boolean actualConflict = diagnosis.path("slotConflict").asBoolean();
            String actualMetric = data.path("metric").asText();
            String secondaryMetric = diagnosis.path("slotEvidence").path("secondaryMetric").path("value").asText(null);

            boolean pass = "销售额".equals(actualMetric) && actualConflict == benchmarkCase.expectedConflict();
            if (benchmarkCase.expectedSecondaryMetric() != null) {
                pass = pass && benchmarkCase.expectedSecondaryMetric().equals(secondaryMetric);
            }
            if (pass) {
                passed++;
            } else {
                Map<String, Object> failure = new LinkedHashMap<>();
                failure.put("caseId", benchmarkCase.caseId());
                failure.put("category", benchmarkCase.category());
                failure.put("contextMessage", benchmarkCase.contextMessage());
                failure.put("followupMessage", benchmarkCase.followupMessage());
                failure.put("expectedConflict", benchmarkCase.expectedConflict());
                failure.put("actualConflict", actualConflict);
                failure.put("expectedSecondaryMetric", benchmarkCase.expectedSecondaryMetric());
                failure.put("actualSecondaryMetric", secondaryMetric);
                failure.put("actualMetric", actualMetric);
                failedCases.add(failure);
            }
        }

        double accuracy = cases.isEmpty() ? 0.0 : (passed * 100.0) / cases.size();
        writeSemanticBenchmarkReport(cases.size(), passed, accuracy, failedCases);
        assertTrue(accuracy >= 90.0,
            String.format("复杂多轮语义基线未达标: %.1f%% (passed=%d,total=%d,failed=%s)",
                accuracy, passed, cases.size(), failedCases));
    }

    private void writeSemanticBenchmarkReport(int total, int passed, double accuracy, List<Map<String, Object>> failedCases) throws Exception {
        Path reportPath = Path.of("target", "semantic-benchmark-report.json");
        Files.createDirectories(reportPath.getParent());
        Map<String, Object> report = Map.of(
            "generatedAt", Instant.now().toString(),
            "suite", "conversation-multi-turn-semantic-benchmark",
            "threshold", 90.0,
            "accuracy", Math.round(accuracy * 100.0) / 100.0,
            "passed", passed,
            "total", total,
            "failedCases", failedCases
        );
        Files.writeString(reportPath, OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(report), StandardCharsets.UTF_8);
    }

    private String createConversation() throws Exception {
        String createResponse = mockMvc.perform(post("/api/conversation/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":1}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
        return OBJECT_MAPPER.readTree(createResponse).path("data").path("conversationId").asText();
    }

    private JsonNode sendMessage(String conversationId, String message) throws Exception {
        String content = mockMvc.perform(post("/api/conversation/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"conversationId\":\"" + conversationId + "\",\"message\":\"" + message + "\",\"userId\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
        return OBJECT_MAPPER.readTree(content);
    }
}
