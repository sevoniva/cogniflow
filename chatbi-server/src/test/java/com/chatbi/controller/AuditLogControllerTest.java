package com.chatbi.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chatbi.entity.AuditLog;
import com.chatbi.service.AccessAlertService;
import com.chatbi.service.AuditLogService;
import com.chatbi.service.PolicyVersionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditLogController 测试")
class AuditLogControllerTest {

    @Mock
    private AuditLogService auditLogService;
    @Mock
    private PolicyVersionService policyVersionService;
    @Mock
    private AccessAlertService accessAlertService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AuditLogController controller = new AuditLogController(auditLogService, policyVersionService, accessAlertService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("审计日志分页接口支持增强筛选参数")
    void testGetAuditLogsWithFilters() throws Exception {
        AuditLog row = AuditLog.builder()
            .id(1L)
            .username("admin")
            .action("AI_OBSERVABILITY_THRESHOLD_UPDATE")
            .resourceType("ai-observability-threshold")
            .result("SUCCESS")
            .build();
        Page<AuditLog> page = new Page<>(1, 20);
        page.setRecords(List.of(row));
        page.setTotal(1);
        when(auditLogService.page(eq(1L), eq("AI_OBSERVABILITY_THRESHOLD_UPDATE"), eq("ai-observability-threshold"),
            eq("SUCCESS"), eq("admin"), eq(null), eq(null), eq(1), eq(20))).thenReturn(page);

        mockMvc.perform(get("/api/audit/logs")
                .param("userId", "1")
                .param("action", "AI_OBSERVABILITY_THRESHOLD_UPDATE")
                .param("resourceType", "ai-observability-threshold")
                .param("result", "SUCCESS")
                .param("keyword", "admin")
                .param("current", "1")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.records[0].resourceType").value("ai-observability-threshold"));
    }

    @Test
    @DisplayName("审计资源类型选项接口返回列表")
    void testGetResourceTypeOptions() throws Exception {
        when(auditLogService.topResourceTypes(eq(50))).thenReturn(List.of(
            "ai-observability",
            "ai-observability-threshold",
            "ai-provider-switch"
        ));

        mockMvc.perform(get("/api/audit/logs/options").param("limit", "50"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0]").value("ai-observability"))
            .andExpect(jsonPath("$.data[1]").value("ai-observability-threshold"));
    }

    @Test
    @DisplayName("策略版本历史接口应返回版本列表")
    void testGetPolicyVersions() throws Exception {
        when(policyVersionService.listPolicyVersions(eq("DATA_PERMISSION"), eq(20))).thenReturn(List.of(
            java.util.Map.of("scope", "DATA_PERMISSION", "version", 3, "operation", "UPDATE")
        ));

        mockMvc.perform(get("/api/audit/policy-versions")
                .param("scope", "DATA_PERMISSION")
                .param("limit", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].scope").value("DATA_PERMISSION"))
            .andExpect(jsonPath("$.data[0].version").value(3));
    }

    @Test
    @DisplayName("策略版本范围选项接口应返回列表")
    void testGetPolicyVersionScopes() throws Exception {
        when(policyVersionService.listScopes(eq(50))).thenReturn(List.of(
            "DATA_PERMISSION",
            "DATA_MASKING",
            "ALERT_RULE"
        ));

        mockMvc.perform(get("/api/audit/policy-versions/options").param("limit", "50"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0]").value("DATA_PERMISSION"))
            .andExpect(jsonPath("$.data[1]").value("DATA_MASKING"));
    }

    @Test
    @DisplayName("越权告警列表接口应返回告警记录")
    void testGetAccessAlerts() throws Exception {
        when(accessAlertService.listAlerts(eq("OPEN"), eq("CRITICAL"), eq("系统治理表"), eq(20))).thenReturn(List.of(
            java.util.Map.of("id", 11, "status", "OPEN", "severity", "CRITICAL", "reason", "禁止查询系统治理表：sys_user")
        ));

        mockMvc.perform(get("/api/audit/access-alerts")
                .param("status", "OPEN")
                .param("severity", "CRITICAL")
                .param("keyword", "系统治理表")
                .param("limit", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].status").value("OPEN"))
            .andExpect(jsonPath("$.data[0].severity").value("CRITICAL"));
    }

    @Test
    @DisplayName("越权告警选项接口应返回状态和级别")
    void testGetAccessAlertOptions() throws Exception {
        when(accessAlertService.listReasons(eq(50))).thenReturn(List.of(
            "禁止查询系统治理表：sys_user",
            "SQL 治理失败，请调整查询后重试"
        ));

        mockMvc.perform(get("/api/audit/access-alerts/options").param("limit", "50"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.statuses[0]").value("OPEN"))
            .andExpect(jsonPath("$.data.severities[0]").value("CRITICAL"))
            .andExpect(jsonPath("$.data.reasons[0]").value("禁止查询系统治理表：sys_user"));
    }

    @Test
    @DisplayName("越权告警确认接口应返回确认后状态")
    void testAcknowledgeAccessAlert() throws Exception {
        when(accessAlertService.acknowledge(eq(11L), eq("auditor"))).thenReturn(
            java.util.Map.of("id", 11, "status", "ACKED", "ackBy", "auditor")
        );

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/audit/access-alerts/11/ack")
                .param("operator", "auditor"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("ACKED"))
            .andExpect(jsonPath("$.data.ackBy").value("auditor"));
    }
}
