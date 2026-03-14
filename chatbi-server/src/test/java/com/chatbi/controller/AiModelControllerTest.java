package com.chatbi.controller;

import com.chatbi.config.AiConfig;
import com.chatbi.service.AiModelService;
import com.chatbi.service.AiObservabilityService;
import com.chatbi.service.AiProviderSettingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiModelController 测试")
class AiModelControllerTest {

    @Mock
    private AiModelService aiModelService;
    @Mock
    private AiProviderSettingService aiProviderSettingService;
    @Mock
    private AiObservabilityService aiObservabilityService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AiConfig aiConfig = new AiConfig();
        aiConfig.setEnabled(false);
        aiConfig.setDefaultProvider("kimi");

        AiConfig.ProviderConfig kimi = new AiConfig.ProviderConfig();
        kimi.setName("Kimi");
        kimi.setModel("moonshot-v1-32k");
        kimi.setApiUrl("https://api.moonshot.cn/v1");
        kimi.setEnabled(false);
        aiConfig.setProviders(Map.of("kimi", kimi));

        AiModelController controller = new AiModelController(aiModelService, aiObservabilityService, aiConfig, aiProviderSettingService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("状态接口返回真实运行状态")
    void testGetStatus() throws Exception {
        mockMvc.perform(get("/api/ai-model/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.mode").value("semantic"))
            .andExpect(jsonPath("$.data.runtimeEnabled").value(false))
            .andExpect(jsonPath("$.data.providerName").value("Kimi"));
    }

    @Test
    @DisplayName("连接测试透传服务结果")
    void testConnection() throws Exception {
        when(aiModelService.testConnection("kimi")).thenReturn(false);
        mockMvc.perform(post("/api/ai-model/test/kimi"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").value(false));
    }

    @Test
    @DisplayName("允许更新运行开关")
    void testUpdateRuntime() throws Exception {
        mockMvc.perform(put("/api/ai-model/runtime")
                .contentType("application/json")
                .content("{\"enabled\":true}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("可观测性接口返回聚合结果")
    void testGetObservability() throws Exception {
        when(aiObservabilityService.snapshot()).thenReturn(Map.of(
            "totalCalls", 12,
            "successCalls", 10,
            "failedCalls", 2
        ));
        mockMvc.perform(get("/api/ai-model/observability"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.totalCalls").value(12))
            .andExpect(jsonPath("$.data.failedCalls").value(2));
    }

    @Test
    @DisplayName("可观测性告警接口返回健康状态")
    void testGetObservabilityAlerts() throws Exception {
        when(aiObservabilityService.alertSnapshot()).thenReturn(Map.of(
            "healthStatus", "degraded",
            "healthScore", 85
        ));
        mockMvc.perform(get("/api/ai-model/observability/alerts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.healthStatus").value("degraded"))
            .andExpect(jsonPath("$.data.healthScore").value(85));
    }

    @Test
    @DisplayName("可观测性告警历史接口返回记录")
    void testGetObservabilityAlertHistory() throws Exception {
        when(aiObservabilityService.alertHistory(eq(20), eq(null), eq(null))).thenReturn(java.util.List.of(
            Map.of("status", "alert", "action", "AI_OBSERVABILITY_ALERT")
        ));
        mockMvc.perform(get("/api/ai-model/observability/alerts/history"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].status").value("alert"))
            .andExpect(jsonPath("$.data[0].action").value("AI_OBSERVABILITY_ALERT"));
    }

    @Test
    @DisplayName("可观测性告警历史支持按状态和关键字筛选")
    void testGetObservabilityAlertHistoryWithFilters() throws Exception {
        when(aiObservabilityService.alertHistory(eq(30), eq("alert"), eq("网络"))).thenReturn(java.util.List.of(
            Map.of("status", "alert", "action", "AI_OBSERVABILITY_ALERT", "summary", "网络异常")
        ));
        mockMvc.perform(get("/api/ai-model/observability/alerts/history")
                .param("limit", "30")
                .param("status", "alert")
                .param("keyword", "网络"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].summary").value("网络异常"));
    }

    @Test
    @DisplayName("主备切换历史接口支持筛选参数")
    void testGetObservabilitySwitchHistory() throws Exception {
        when(aiObservabilityService.switchHistory(eq(10), eq("kimi"), eq("openai"), eq("RATE_LIMIT"))).thenReturn(java.util.List.of(
            Map.of(
                "fromProvider", "kimi",
                "toProvider", "openai",
                "category", "RATE_LIMIT"
            )
        ));

        mockMvc.perform(get("/api/ai-model/observability/switches")
                .param("limit", "10")
                .param("fromProvider", "kimi")
                .param("toProvider", "openai")
                .param("category", "RATE_LIMIT"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].fromProvider").value("kimi"))
            .andExpect(jsonPath("$.data[0].toProvider").value("openai"))
            .andExpect(jsonPath("$.data[0].category").value("RATE_LIMIT"));
    }

    @Test
    @DisplayName("可观测阈值配置接口返回当前阈值")
    void testGetObservabilityThresholds() throws Exception {
        when(aiObservabilityService.thresholdSettings()).thenReturn(Map.of(
            "windowMinutes", 12,
            "failureRateWarning", 0.25
        ));
        mockMvc.perform(get("/api/ai-model/observability/thresholds"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.windowMinutes").value(12))
            .andExpect(jsonPath("$.data.failureRateWarning").value(0.25));
    }

    @Test
    @DisplayName("可观测阈值更新接口应返回更新后阈值")
    void testUpdateObservabilityThresholds() throws Exception {
        when(aiObservabilityService.updateThresholdSettings(org.mockito.ArgumentMatchers.any(), eq("admin"))).thenReturn(Map.of(
            "windowMinutes", 15,
            "failureRateWarning", 0.3
        ));

        mockMvc.perform(put("/api/ai-model/observability/thresholds")
                .contentType("application/json")
                .content("{\"windowMinutes\":15,\"failureRateWarning\":0.3}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.windowMinutes").value(15))
            .andExpect(jsonPath("$.data.failureRateWarning").value(0.3));
    }

    @Test
    @DisplayName("可观测阈值历史接口返回记录")
    void testGetObservabilityThresholdHistory() throws Exception {
        when(aiObservabilityService.thresholdHistory(eq(20))).thenReturn(java.util.List.of(
            Map.of("operator", "admin", "action", "AI_OBSERVABILITY_THRESHOLD_UPDATE")
        ));
        mockMvc.perform(get("/api/ai-model/observability/thresholds/history"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].operator").value("admin"))
            .andExpect(jsonPath("$.data[0].action").value("AI_OBSERVABILITY_THRESHOLD_UPDATE"));
    }
}
