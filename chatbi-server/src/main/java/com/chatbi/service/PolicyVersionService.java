package com.chatbi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatbi.common.constant.SysConstant;
import com.chatbi.entity.AuditLog;
import com.chatbi.repository.AuditLogMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 策略版本快照服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyVersionService {

    private static final String RESOURCE_TYPE = "policy-version";
    private static final String ACTION_PREFIX = "POLICY_VERSION_";

    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    public void recordPolicyVersion(String scope, String operation, Object snapshot, String operator) {
        String safeScope = normalizeScope(scope);
        String safeOperation = operation == null || operation.isBlank() ? "UPDATE" : operation.trim().toUpperCase();
        int version = nextVersion(safeScope);
        long now = System.currentTimeMillis();

        AuditLog logEntry = AuditLog.builder()
            .traceId("plv" + now)
            .userId(0L)
            .username((operator == null || operator.isBlank()) ? "system" : operator)
            .action(ACTION_PREFIX + safeScope)
            .resourceType(RESOURCE_TYPE)
            .requestMethod("SYSTEM")
            .requestUri("/api/audit/policy-versions")
            .requestBody("scope=" + safeScope + ",version=" + version + ",operation=" + safeOperation)
            .responseStatus(200)
            .responseBody(toJson(snapshot))
            .executeTimeMs(0)
            .result(SysConstant.RESULT_SUCCESS)
            .errorMessage("policy-version-snapshot")
            .createdAt(LocalDateTime.now())
            .build();
        auditLogMapper.insert(logEntry);
    }

    public List<Map<String, Object>> listPolicyVersions(String scope, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuditLog::getResourceType, RESOURCE_TYPE)
            .orderByDesc(AuditLog::getCreatedAt);
        if (scope != null && !scope.isBlank()) {
            wrapper.eq(AuditLog::getAction, ACTION_PREFIX + normalizeScope(scope));
        }
        wrapper.last("LIMIT " + safeLimit);
        return auditLogMapper.selectList(wrapper).stream().map(this::toVersionRow).toList();
    }

    public List<String> listScopes(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(AuditLog::getAction)
            .eq(AuditLog::getResourceType, RESOURCE_TYPE)
            .isNotNull(AuditLog::getAction)
            .groupBy(AuditLog::getAction)
            .orderByDesc(AuditLog::getCreatedAt)
            .last("LIMIT " + safeLimit);
        return auditLogMapper.selectList(wrapper).stream()
            .map(AuditLog::getAction)
            .filter(item -> item != null && item.startsWith(ACTION_PREFIX))
            .map(item -> item.substring(ACTION_PREFIX.length()))
            .toList();
    }

    private int nextVersion(String scope) {
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuditLog::getResourceType, RESOURCE_TYPE)
            .eq(AuditLog::getAction, ACTION_PREFIX + scope)
            .orderByDesc(AuditLog::getCreatedAt)
            .last("LIMIT 1");
        AuditLog last = auditLogMapper.selectOne(wrapper);
        if (last == null || last.getRequestBody() == null || last.getRequestBody().isBlank()) {
            return 1;
        }
        String rawVersion = parsePairs(last.getRequestBody()).get("version");
        try {
            return Integer.parseInt(rawVersion) + 1;
        } catch (Exception ex) {
            return 1;
        }
    }

    private Map<String, Object> toVersionRow(AuditLog logEntry) {
        Map<String, String> pairs = parsePairs(logEntry.getRequestBody());
        Map<String, Object> row = new LinkedHashMap<>();
        long timestamp = logEntry.getCreatedAt() == null
            ? 0L
            : logEntry.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        row.put("id", logEntry.getId());
        row.put("timestamp", timestamp);
        row.put("scope", pairs.getOrDefault("scope", parseScopeByAction(logEntry.getAction())));
        row.put("version", parseInt(pairs.get("version")));
        row.put("operation", pairs.getOrDefault("operation", "UPDATE"));
        row.put("operator", logEntry.getUsername());
        row.put("snapshot", parseJson(logEntry.getResponseBody()));
        return row;
    }

    private String parseScopeByAction(String action) {
        if (action == null || !action.startsWith(ACTION_PREFIX)) {
            return "UNKNOWN";
        }
        return action.substring(ACTION_PREFIX.length());
    }

    private int parseInt(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (Exception ex) {
            return 0;
        }
    }

    private Map<String, String> parsePairs(String raw) {
        Map<String, String> values = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) {
            return values;
        }
        String[] parts = raw.split(",");
        for (String part : parts) {
            String[] pair = part.split("=", 2);
            if (pair.length == 2) {
                values.put(pair[0].trim(), pair[1].trim());
            }
        }
        return values;
    }

    private Object parseJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(raw, Object.class);
        } catch (Exception ex) {
            return raw;
        }
    }

    private String toJson(Object value) {
        if (value == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            log.debug("策略版本快照序列化失败: {}", ex.getMessage());
            return String.valueOf(value);
        }
    }

    private String normalizeScope(String scope) {
        if (scope == null || scope.isBlank()) {
            return "UNKNOWN";
        }
        return scope.trim().toUpperCase().replace('-', '_');
    }
}
