package com.chatbi.service;

import com.chatbi.entity.AuditLog;
import com.chatbi.repository.AuditLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccessAlertService 测试")
class AccessAlertServiceTest {

    @Mock
    private AuditLogMapper auditLogMapper;

    @Test
    @DisplayName("记录越权告警应落审计日志")
    void testRecordBlockedAccess() {
        AccessAlertService service = new AccessAlertService(auditLogMapper, new ObjectMapper());
        service.recordBlockedAccess(7L, "alice", "查 sys_user", "SELECT * FROM sys_user", "禁止查询系统治理表：sys_user", "query");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper).insert(captor.capture());
        AuditLog row = captor.getValue();
        assertEquals("access-alert", row.getResourceType());
        assertEquals("ACCESS_POLICY_BLOCKED", row.getAction());
        assertEquals("FAILED", row.getResult());
        assertTrue(row.getRequestBody().contains("\"status\":\"OPEN\""));
        assertTrue(row.getRequestBody().contains("\"severity\":\"CRITICAL\""));
    }

    @Test
    @DisplayName("查询越权告警应返回结构化字段")
    void testListAlerts() {
        AccessAlertService service = new AccessAlertService(auditLogMapper, new ObjectMapper());
        AuditLog row = AuditLog.builder()
            .id(11L)
            .resourceType("access-alert")
            .action("ACCESS_POLICY_BLOCKED")
            .userId(3L)
            .username("bob")
            .requestBody("{\"status\":\"OPEN\",\"severity\":\"HIGH\",\"scene\":\"query\",\"queryText\":\"查销售额\",\"sql\":\"SELECT * FROM sales\",\"reason\":\"SQL 治理失败\"}")
            .errorMessage("SQL 治理失败")
            .createdAt(LocalDateTime.now())
            .build();
        when(auditLogMapper.selectList(any())).thenReturn(List.of(row));

        List<Map<String, Object>> data = service.listAlerts("OPEN", "HIGH", "", 20);
        assertEquals(1, data.size());
        assertEquals("OPEN", data.get(0).get("status"));
        assertEquals("HIGH", data.get(0).get("severity"));
        assertEquals("bob", data.get(0).get("username"));
    }

    @Test
    @DisplayName("确认告警后状态应更新为 ACKED")
    void testAcknowledge() {
        AccessAlertService service = new AccessAlertService(auditLogMapper, new ObjectMapper());
        AuditLog row = AuditLog.builder()
            .id(12L)
            .resourceType("access-alert")
            .action("ACCESS_POLICY_BLOCKED")
            .requestBody("{\"status\":\"OPEN\",\"severity\":\"HIGH\"}")
            .createdAt(LocalDateTime.now())
            .build();
        when(auditLogMapper.selectById(12L)).thenReturn(row);

        Map<String, Object> acked = service.acknowledge(12L, "auditor");
        verify(auditLogMapper).updateById(any(AuditLog.class));
        assertEquals("ACKED", acked.get("status"));
        assertEquals("auditor", acked.get("ackBy"));
    }
}
