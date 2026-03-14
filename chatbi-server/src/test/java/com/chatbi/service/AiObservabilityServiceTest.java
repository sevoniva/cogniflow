package com.chatbi.service;

import com.chatbi.config.AiObservabilityThresholdProperties;
import com.chatbi.entity.AuditLog;
import com.chatbi.repository.AuditLogMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiObservabilityService 测试")
class AiObservabilityServiceTest {

    @Mock
    private AuditLogMapper auditLogMapper;
    @Mock
    private AiObservabilityNotificationService notificationService;

    @Test
    @DisplayName("无调用时健康状态应为 no-traffic")
    void testNoTrafficHealth() {
        AiObservabilityService service = new AiObservabilityService();

        Map<String, Object> snapshot = service.snapshot();

        assertEquals("no-traffic", snapshot.get("healthStatus"));
        assertEquals(100, snapshot.get("healthScore"));
        List<?> alerts = (List<?>) snapshot.get("alerts");
        assertTrue(alerts.isEmpty());
    }

    @Test
    @DisplayName("高失败率应触发 critical 告警")
    void testCriticalFailureRateAlert() {
        AiObservabilityService service = new AiObservabilityService();
        service.recordFailure("kimi", "generateText", 820, 1, 120, "RATE_LIMIT", "429");
        service.recordFailure("kimi", "generateText", 900, 1, 130, "RATE_LIMIT", "429");
        service.recordFailure("kimi", "generateText", 980, 1, 118, "NETWORK", "connection reset");
        service.recordFailure("kimi", "generateText", 760, 1, 119, "TIMEOUT", "timeout");
        service.recordFailure("kimi", "generateText", 700, 1, 121, "UNKNOWN", "unknown");
        service.recordSuccess("kimi", "generateText", 600, 1, 110);

        Map<String, Object> snapshot = service.snapshot();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> alerts = (List<Map<String, Object>>) snapshot.get("alerts");

        assertEquals("critical", snapshot.get("healthStatus"));
        assertTrue(alerts.stream().anyMatch(item -> "failure-rate".equals(item.get("code"))));
    }

    @Test
    @DisplayName("连续失败与鉴权失败应生成对应告警")
    void testConsecutiveAndAuthAlert() {
        AiObservabilityService service = new AiObservabilityService();
        service.recordFailure("kimi", "generateText", 1000, 1, 80, "AUTH", "401 unauthorized");
        service.recordFailure("kimi", "generateText", 980, 1, 90, "CONFIG", "api key missing");
        service.recordFailure("kimi", "generateText", 1020, 1, 86, "AUTH", "403 forbidden");

        Map<String, Object> alertSnapshot = service.alertSnapshot();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> alerts = (List<Map<String, Object>>) alertSnapshot.get("alerts");

        assertTrue(alerts.stream().anyMatch(item -> "consecutive-failures".equals(item.get("code"))));
        assertTrue(alerts.stream().anyMatch(item -> "auth-config".equals(item.get("code"))));
        assertTrue(alerts.stream().anyMatch(item -> "auth-failures".equals(item.get("code"))));
    }

    @Test
    @DisplayName("窗口失败分类与分类峰值告警应输出")
    void testWindowFailureCategoriesAndSpikeAlerts() {
        AiObservabilityService service = new AiObservabilityService();
        service.recordFailure("kimi", "generateText", 880, 1, 120, "TIMEOUT", "timeout");
        service.recordFailure("kimi", "generateText", 920, 1, 125, "TIMEOUT", "timed out");
        service.recordFailure("kimi", "generateText", 860, 1, 118, "RATE_LIMIT", "429");
        service.recordFailure("kimi", "generateText", 840, 1, 121, "RATE_LIMIT", "rate limited");
        service.recordFailure("kimi", "generateText", 800, 1, 117, "AUTH", "401");
        service.recordSuccess("kimi", "generateText", 620, 1, 110);

        Map<String, Object> snapshot = service.snapshot();
        @SuppressWarnings("unchecked")
        Map<String, Object> windowFailureCategories = (Map<String, Object>) snapshot.get("windowFailureCategories");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> alerts = (List<Map<String, Object>>) snapshot.get("alerts");

        assertEquals(2L, windowFailureCategories.get("TIMEOUT"));
        assertEquals(2L, windowFailureCategories.get("RATE_LIMIT"));
        assertTrue(List.of("TIMEOUT", "RATE_LIMIT").contains(snapshot.get("topFailureCategory")));
        assertTrue(alerts.stream().anyMatch(item -> "timeout-spike".equals(item.get("code"))));
        assertTrue(alerts.stream().anyMatch(item -> "rate-limit-spike".equals(item.get("code"))));
    }

    @Test
    @DisplayName("可观测阈值配置应生效")
    void testCustomThresholdsApplied() {
        AiObservabilityThresholdProperties props = new AiObservabilityThresholdProperties();
        props.setWindowMinutes(15);
        props.setMinSampleSizeForAlert(1);
        props.setLatencyWarningMs(100);
        props.setLatencyCriticalMs(200);
        AiObservabilityService service = new AiObservabilityService(null, props);
        service.recordSuccess("kimi", "generateText", 150, 1, 40);

        Map<String, Object> snapshot = service.snapshot();
        @SuppressWarnings("unchecked")
        Map<String, Object> thresholdMap = (Map<String, Object>) snapshot.get("thresholds");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> alerts = (List<Map<String, Object>>) snapshot.get("alerts");

        assertEquals(15, snapshot.get("windowMinutes"));
        assertEquals(15, thresholdMap.get("windowMinutes"));
        assertEquals(1, thresholdMap.get("minSampleSizeForAlert"));
        assertEquals(100L, thresholdMap.get("latencyWarningMs"));
        assertTrue(alerts.stream().anyMatch(item -> "latency".equals(item.get("code"))));
    }

    @Test
    @DisplayName("主备切换事件应写入可观测快照")
    void testProviderSwitchSnapshot() {
        AiObservabilityService service = new AiObservabilityService();
        service.recordProviderSwitch("kimi", "openai", "RATE_LIMIT", "429");

        Map<String, Object> snapshot = service.snapshot();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> switches = (List<Map<String, Object>>) snapshot.get("recentProviderSwitches");

        assertEquals(1L, snapshot.get("providerSwitchCount"));
        assertEquals(1, switches.size());
        assertEquals("kimi", switches.get(0).get("fromProvider"));
        assertEquals("openai", switches.get(0).get("toProvider"));
        assertEquals("RATE_LIMIT", switches.get(0).get("category"));
    }

    @Test
    @DisplayName("告警状态应写入审计日志")
    void testPersistAlertToAuditLog() {
        AiObservabilityService service = new AiObservabilityService(auditLogMapper);
        service.setNotificationService(notificationService);
        service.recordFailure("kimi", "generateText", 820, 1, 120, "RATE_LIMIT", "429");
        service.recordFailure("kimi", "generateText", 900, 1, 130, "RATE_LIMIT", "429");
        service.recordFailure("kimi", "generateText", 980, 1, 118, "NETWORK", "connection reset");
        service.recordFailure("kimi", "generateText", 760, 1, 119, "TIMEOUT", "timeout");
        service.recordFailure("kimi", "generateText", 700, 1, 121, "UNKNOWN", "unknown");

        verify(auditLogMapper, atLeastOnce()).insert(any(AuditLog.class));
        verify(notificationService, atLeastOnce()).notifyEvent(
            org.mockito.ArgumentMatchers.eq("AI_OBSERVABILITY_ALERT"),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyInt(),
            org.mockito.ArgumentMatchers.anyList(),
            org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    @DisplayName("告警历史接口应返回审计日志记录")
    void testAlertHistory() {
        AiObservabilityService service = new AiObservabilityService(auditLogMapper);
        AuditLog row = AuditLog.builder()
            .id(1L)
            .action("AI_OBSERVABILITY_ALERT")
            .resourceType("ai-observability")
            .errorMessage("[critical] 失败率过高")
            .result("SUCCESS")
            .createdAt(LocalDateTime.now())
            .build();
        when(auditLogMapper.selectList(any())).thenReturn(List.of(row));

        List<Map<String, Object>> history = service.alertHistory(20);

        assertEquals(1, history.size());
        assertEquals("alert", history.get(0).get("status"));
        assertEquals("AI_OBSERVABILITY_ALERT", history.get(0).get("action"));
    }

    @Test
    @DisplayName("告警历史筛选与分数字段映射应正确")
    void testAlertHistoryWithFilterAndScore() {
        AiObservabilityService service = new AiObservabilityService(auditLogMapper);
        AuditLog row = AuditLog.builder()
            .id(2L)
            .action("AI_OBSERVABILITY_RECOVERY")
            .resourceType("ai-observability")
            .requestBody("status=healthy, score=92")
            .errorMessage("恢复正常")
            .result("SUCCESS")
            .createdAt(LocalDateTime.now())
            .build();
        when(auditLogMapper.selectList(any())).thenReturn(List.of(row));

        List<Map<String, Object>> history = service.alertHistory(30, "recovery", "恢复");

        assertEquals(1, history.size());
        assertEquals("recovery", history.get(0).get("status"));
        assertEquals("info", history.get(0).get("level"));
        assertEquals(92, history.get(0).get("score"));
    }

    @Test
    @DisplayName("主备切换历史接口应返回审计日志记录")
    void testSwitchHistory() {
        AiObservabilityService service = new AiObservabilityService(auditLogMapper);
        AuditLog row = AuditLog.builder()
            .id(3L)
            .action("AI_PROVIDER_SWITCH")
            .resourceType("ai-provider-switch")
            .requestBody("from=kimi,to=openai,category=RATE_LIMIT")
            .errorMessage("429 rate limit")
            .createdAt(LocalDateTime.now())
            .build();
        when(auditLogMapper.selectList(any())).thenReturn(List.of(row));

        List<Map<String, Object>> history = service.switchHistory(10, "kimi", "openai", "RATE_LIMIT");

        assertEquals(1, history.size());
        assertEquals("kimi", history.get(0).get("fromProvider"));
        assertEquals("openai", history.get(0).get("toProvider"));
        assertEquals("RATE_LIMIT", history.get(0).get("category"));
    }

    @Test
    @DisplayName("阈值更新应持久化并返回最新阈值")
    void testUpdateThresholdSettings() {
        AiObservabilityService service = new AiObservabilityService(auditLogMapper);
        AiObservabilityService.ThresholdMutation mutation = new AiObservabilityService.ThresholdMutation(
            20, 3, 0.3, 0.5, 2500L, 5000L, 2, 5, 2, 4, 40, 25
        );

        Map<String, Object> thresholds = service.updateThresholdSettings(mutation, "admin");

        assertEquals(20, thresholds.get("windowMinutes"));
        assertEquals(0.3, thresholds.get("failureRateWarning"));
        verify(auditLogMapper, atLeastOnce()).insert(any(AuditLog.class));
    }

    @Test
    @DisplayName("阈值历史接口应返回审计记录")
    void testThresholdHistory() {
        AiObservabilityService service = new AiObservabilityService(auditLogMapper);
        AuditLog row = AuditLog.builder()
            .id(9L)
            .action("AI_OBSERVABILITY_THRESHOLD_UPDATE")
            .resourceType("ai-observability-threshold")
            .username("admin")
            .requestBody("windowMinutes=15,failureRateWarning=0.3")
            .createdAt(LocalDateTime.now())
            .build();
        when(auditLogMapper.selectList(any())).thenReturn(List.of(row));

        List<Map<String, Object>> history = service.thresholdHistory(20);

        assertEquals(1, history.size());
        assertEquals("admin", history.get(0).get("operator"));
        @SuppressWarnings("unchecked")
        Map<String, String> thresholdMap = (Map<String, String>) history.get(0).get("thresholds");
        assertEquals("15", thresholdMap.get("windowMinutes"));
    }
}
