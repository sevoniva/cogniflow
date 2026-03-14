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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PolicyVersionService 测试")
class PolicyVersionServiceTest {

    @Mock
    private AuditLogMapper auditLogMapper;

    @Test
    @DisplayName("记录策略版本应自动递增版本号")
    void testRecordPolicyVersion() {
        PolicyVersionService service = new PolicyVersionService(auditLogMapper, new ObjectMapper());
        AuditLog previous = AuditLog.builder()
            .id(1L)
            .action("POLICY_VERSION_DATA_PERMISSION")
            .resourceType("policy-version")
            .requestBody("scope=DATA_PERMISSION,version=2,operation=UPDATE")
            .createdAt(LocalDateTime.now())
            .build();
        when(auditLogMapper.selectOne(any())).thenReturn(previous);

        service.recordPolicyVersion("DATA_PERMISSION", "UPDATE", Map.of("id", 7L), "admin");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper, atLeastOnce()).insert(captor.capture());
        AuditLog inserted = captor.getValue();
        assertNotNull(inserted);
        assertEquals("policy-version", inserted.getResourceType());
        assertEquals("POLICY_VERSION_DATA_PERMISSION", inserted.getAction());
        assertEquals("scope=DATA_PERMISSION,version=3,operation=UPDATE", inserted.getRequestBody());
    }

    @Test
    @DisplayName("策略版本列表接口应返回结构化版本信息")
    void testListPolicyVersions() {
        PolicyVersionService service = new PolicyVersionService(auditLogMapper, new ObjectMapper());
        AuditLog row = AuditLog.builder()
            .id(2L)
            .action("POLICY_VERSION_ALERT_RULE")
            .resourceType("policy-version")
            .requestBody("scope=ALERT_RULE,version=5,operation=DELETE")
            .responseBody("{\"id\":10}")
            .username("admin")
            .createdAt(LocalDateTime.now())
            .build();
        when(auditLogMapper.selectList(any())).thenReturn(List.of(row));

        List<Map<String, Object>> list = service.listPolicyVersions("ALERT_RULE", 20);

        assertEquals(1, list.size());
        assertEquals("ALERT_RULE", list.get(0).get("scope"));
        assertEquals(5, list.get(0).get("version"));
        assertEquals("DELETE", list.get(0).get("operation"));
    }
}
