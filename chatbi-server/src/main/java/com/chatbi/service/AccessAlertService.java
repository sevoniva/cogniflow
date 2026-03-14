package com.chatbi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatbi.common.constant.SysConstant;
import com.chatbi.entity.AuditLog;
import com.chatbi.repository.AuditLogMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * 越权访问告警服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccessAlertService {

    private static final String RESOURCE_TYPE = "access-alert";
    private static final String ACTION_BLOCKED = "ACCESS_POLICY_BLOCKED";

    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    public void recordBlockedAccess(
        Long userId,
        String username,
        String queryText,
        String sql,
        String reason,
        String scene
    ) {
        String normalizedReason = normalizeReason(reason);
        String severity = inferSeverity(normalizedReason);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", "OPEN");
        payload.put("severity", severity);
        payload.put("scene", normalizeScene(scene));
        payload.put("queryText", safe(queryText));
        payload.put("sql", safe(sql));
        payload.put("reason", normalizedReason);
        payload.put("ackBy", "");
        payload.put("ackAt", 0L);

        long now = System.currentTimeMillis();
        AuditLog logEntry = AuditLog.builder()
            .traceId("acl" + now)
            .userId(userId == null ? 0L : userId)
            .username(username == null || username.isBlank() ? "unknown" : username)
            .action(ACTION_BLOCKED)
            .resourceType(RESOURCE_TYPE)
            .requestMethod("SYSTEM")
            .requestUri("/api/audit/access-alerts")
            .requestBody(toJson(payload))
            .responseStatus(403)
            .responseBody("")
            .executeTimeMs(0)
            .result(SysConstant.RESULT_FAILED)
            .errorMessage(normalizedReason)
            .createdAt(LocalDateTime.now())
            .build();
        auditLogMapper.insert(logEntry);
    }

    public List<Map<String, Object>> listAlerts(String status, String severity, String keyword, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuditLog::getResourceType, RESOURCE_TYPE)
            .eq(AuditLog::getAction, ACTION_BLOCKED)
            .orderByDesc(AuditLog::getCreatedAt);
        if (keyword != null && !keyword.isBlank()) {
            String safeKeyword = keyword.trim();
            wrapper.and(item -> item.like(AuditLog::getUsername, safeKeyword)
                .or().like(AuditLog::getErrorMessage, safeKeyword)
                .or().like(AuditLog::getRequestBody, safeKeyword));
        }
        wrapper.last("LIMIT " + safeLimit);

        String normalizedStatus = normalizeUpper(status);
        String normalizedSeverity = normalizeUpper(severity);
        return auditLogMapper.selectList(wrapper).stream()
            .map(this::toAlertRow)
            .filter(item -> normalizedStatus == null || normalizedStatus.equals(item.get("status")))
            .filter(item -> normalizedSeverity == null || normalizedSeverity.equals(item.get("severity")))
            .toList();
    }

    public List<String> listReasons(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(AuditLog::getErrorMessage)
            .eq(AuditLog::getResourceType, RESOURCE_TYPE)
            .eq(AuditLog::getAction, ACTION_BLOCKED)
            .isNotNull(AuditLog::getErrorMessage)
            .groupBy(AuditLog::getErrorMessage)
            .orderByDesc(AuditLog::getCreatedAt)
            .last("LIMIT " + safeLimit);
        return auditLogMapper.selectList(wrapper).stream()
            .map(AuditLog::getErrorMessage)
            .filter(item -> item != null && !item.isBlank())
            .toList();
    }

    public Map<String, Object> acknowledge(Long id, String operator) {
        AuditLog logEntry = auditLogMapper.selectById(id);
        if (logEntry == null || !Objects.equals(RESOURCE_TYPE, logEntry.getResourceType())) {
            throw new IllegalArgumentException("越权告警记录不存在");
        }
        Map<String, Object> payload = parsePayload(logEntry.getRequestBody());
        payload.put("status", "ACKED");
        payload.put("ackBy", operator == null || operator.isBlank() ? "admin" : operator);
        payload.put("ackAt", System.currentTimeMillis());
        logEntry.setRequestBody(toJson(payload));
        auditLogMapper.updateById(logEntry);
        return toAlertRow(logEntry);
    }

    private Map<String, Object> toAlertRow(AuditLog logEntry) {
        Map<String, Object> payload = parsePayload(logEntry.getRequestBody());
        long timestamp = logEntry.getCreatedAt() == null
            ? 0L
            : logEntry.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", logEntry.getId());
        row.put("timestamp", timestamp);
        row.put("userId", logEntry.getUserId());
        row.put("username", logEntry.getUsername());
        row.put("status", payload.getOrDefault("status", "OPEN"));
        row.put("severity", payload.getOrDefault("severity", "HIGH"));
        row.put("scene", payload.getOrDefault("scene", "unknown"));
        row.put("queryText", payload.getOrDefault("queryText", ""));
        row.put("sql", payload.getOrDefault("sql", ""));
        row.put("reason", payload.getOrDefault("reason", logEntry.getErrorMessage()));
        row.put("ackBy", payload.getOrDefault("ackBy", ""));
        row.put("ackAt", payload.getOrDefault("ackAt", 0L));
        return row;
    }

    private Map<String, Object> parsePayload(String raw) {
        if (raw == null || raw.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<>() {});
        } catch (Exception ex) {
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("status", "OPEN");
            fallback.put("severity", "HIGH");
            fallback.put("reason", raw);
            return fallback;
        }
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            log.debug("序列化越权告警失败: {}", ex.getMessage());
            return "{}";
        }
    }

    private String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "未知治理拦截";
        }
        return reason.trim();
    }

    private String normalizeScene(String scene) {
        if (scene == null || scene.isBlank()) {
            return "unknown";
        }
        return scene.trim().toLowerCase(Locale.ROOT);
    }

    private String inferSeverity(String reason) {
        String normalized = reason.toLowerCase(Locale.ROOT);
        if (normalized.contains("系统治理表") || normalized.contains("禁止查询")) {
            return "CRITICAL";
        }
        if (normalized.contains("权限") || normalized.contains("治理")) {
            return "HIGH";
        }
        return "MEDIUM";
    }

    private String normalizeUpper(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }
        return value.length() > 2000 ? value.substring(0, 2000) : value;
    }
}
