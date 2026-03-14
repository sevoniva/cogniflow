package com.chatbi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatbi.common.constant.SysConstant;
import com.chatbi.config.AiObservabilityThresholdProperties;
import com.chatbi.entity.AuditLog;
import com.chatbi.repository.AuditLogMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 调用可观测性服务（进程内轻量实现）。
 * 用于快速定位“是否真实调用外部模型、失败分布、延迟表现”。
 */
@Slf4j
@Service
public class AiObservabilityService {

    private static final int DEFAULT_RECENT_LIMIT = 50;
    private static final int DEFAULT_SWITCH_EVENT_LIMIT = 30;
    private static final int DEFAULT_HEALTH_WINDOW_MINUTES = 10;
    private static final int DEFAULT_MIN_SAMPLE_SIZE_FOR_ALERT = 5;
    private static final double DEFAULT_FAILURE_RATE_WARNING = 0.20;
    private static final double DEFAULT_FAILURE_RATE_CRITICAL = 0.40;
    private static final long DEFAULT_LATENCY_WARNING_MS = 3000;
    private static final long DEFAULT_LATENCY_CRITICAL_MS = 6000;
    private static final int DEFAULT_CONSECUTIVE_FAILURE_WARNING = 3;
    private static final int DEFAULT_CONSECUTIVE_FAILURE_CRITICAL = 6;
    private static final int DEFAULT_CATEGORY_SPIKE_WARNING = 2;
    private static final int DEFAULT_CATEGORY_SPIKE_CRITICAL = 4;
    private static final String OBSERVABILITY_RESOURCE_TYPE = "ai-observability";
    private static final String OBSERVABILITY_ALERT_ACTION = "AI_OBSERVABILITY_ALERT";
    private static final String OBSERVABILITY_RECOVERY_ACTION = "AI_OBSERVABILITY_RECOVERY";
    private static final String PROVIDER_SWITCH_RESOURCE_TYPE = "ai-provider-switch";
    private static final String PROVIDER_SWITCH_ACTION = "AI_PROVIDER_SWITCH";
    private static final String THRESHOLD_RESOURCE_TYPE = "ai-observability-threshold";
    private static final String THRESHOLD_UPDATE_ACTION = "AI_OBSERVABILITY_THRESHOLD_UPDATE";
    private static final long OBS_EVENT_COOLDOWN_MILLIS = 60_000L;
    private static final Pattern SCORE_PATTERN = Pattern.compile("score\\s*=\\s*(\\d+)");

    private final AtomicLong totalCalls = new AtomicLong();
    private final AtomicLong successCalls = new AtomicLong();
    private final AtomicLong failedCalls = new AtomicLong();
    private final AtomicLong totalLatencyMs = new AtomicLong();
    private final AtomicLong providerSwitchCount = new AtomicLong();
    private final AuditLogMapper auditLogMapper;

    private final Map<String, AtomicLong> failureCategories = new ConcurrentHashMap<>();
    private final Map<String, ProviderStats> providerStats = new ConcurrentHashMap<>();
    private final Deque<CallRecord> recentCalls = new ArrayDeque<>();
    private final Deque<ProviderSwitchRecord> recentProviderSwitches = new ArrayDeque<>();
    private volatile String lastPersistedHealthStatus = "no-traffic";
    private volatile String lastPersistedFingerprint = "";
    private volatile long lastPersistedAt = 0L;
    private volatile boolean thresholdSettingsLoaded = false;
    private AiObservabilityNotificationService notificationService;
    private AiObservabilityThresholdProperties thresholdProperties;

    public record ThresholdMutation(
        Integer windowMinutes,
        Integer minSampleSizeForAlert,
        Double failureRateWarning,
        Double failureRateCritical,
        Long latencyWarningMs,
        Long latencyCriticalMs,
        Integer consecutiveFailureWarning,
        Integer consecutiveFailureCritical,
        Integer categorySpikeWarning,
        Integer categorySpikeCritical,
        Integer recentCallLimit,
        Integer switchEventLimit
    ) {}

    public AiObservabilityService() {
        this(null, null);
    }

    @Autowired
    public AiObservabilityService(AuditLogMapper auditLogMapper) {
        this(auditLogMapper, null);
    }

    public AiObservabilityService(AuditLogMapper auditLogMapper, AiObservabilityThresholdProperties thresholdProperties) {
        this.auditLogMapper = auditLogMapper;
        this.thresholdProperties = thresholdProperties;
    }

    @Autowired(required = false)
    void setNotificationService(AiObservabilityNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Autowired(required = false)
    void setThresholdProperties(AiObservabilityThresholdProperties thresholdProperties) {
        this.thresholdProperties = thresholdProperties;
    }

    public void recordSuccess(String provider, String operation, long durationMs, int attempts, int promptChars) {
        totalCalls.incrementAndGet();
        successCalls.incrementAndGet();
        totalLatencyMs.addAndGet(Math.max(durationMs, 0));
        providerStats(provider).recordSuccess(durationMs);
        appendRecent(new CallRecord(
            System.currentTimeMillis(),
            provider,
            operation,
            true,
            durationMs,
            attempts,
            promptChars,
            "SUCCESS",
            null
        ));
        persistHealthEventIfNeeded();
    }

    public void recordFailure(String provider, String operation, long durationMs, int attempts, int promptChars, String category, String message) {
        totalCalls.incrementAndGet();
        failedCalls.incrementAndGet();
        totalLatencyMs.addAndGet(Math.max(durationMs, 0));
        providerStats(provider).recordFailure(durationMs);
        failureCategories.computeIfAbsent(category, key -> new AtomicLong()).incrementAndGet();
        appendRecent(new CallRecord(
            System.currentTimeMillis(),
            provider,
            operation,
            false,
            durationMs,
            attempts,
            promptChars,
            category,
            message
        ));
        persistHealthEventIfNeeded();
    }

    public Map<String, Object> snapshot() {
        ensureThresholdSettingsInitialized();
        long total = totalCalls.get();
        long success = successCalls.get();
        long failed = failedCalls.get();
        double successRate = total == 0 ? 1.0 : (double) success / total;
        long avgLatency = total == 0 ? 0 : totalLatencyMs.get() / total;
        HealthSnapshot health = buildHealthSnapshot();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalCalls", total);
        result.put("successCalls", success);
        result.put("failedCalls", failed);
        result.put("successRate", round(successRate * 100));
        result.put("avgLatencyMs", avgLatency);
        result.put("failureCategories", failureCategories.entrySet().stream()
            .sorted((left, right) -> Long.compare(right.getValue().get(), left.getValue().get()))
            .collect(LinkedHashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue().get()), Map::putAll));
        result.put("providers", providerStats.entrySet().stream()
            .sorted(Comparator.comparing(Map.Entry::getKey))
            .map(entry -> entry.getValue().toMap(entry.getKey()))
            .toList());
        result.put("windowFailureCategories", windowFailureCategories());
        result.put("topFailureCategory", resolveTopFailureCategory());
        result.put("recentCalls", recentCallMaps());
        result.put("providerSwitchCount", providerSwitchCount.get());
        result.put("recentProviderSwitches", recentProviderSwitchMaps());
        result.put("healthStatus", health.status);
        result.put("healthScore", health.score);
        result.put("windowMinutes", healthWindowMinutes());
        result.put("thresholds", thresholdSnapshot());
        result.put("alerts", health.alerts.stream().map(AlertItem::toMap).toList());
        return result;
    }

    public Map<String, Object> alertSnapshot() {
        ensureThresholdSettingsInitialized();
        HealthSnapshot health = buildHealthSnapshot();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("healthStatus", health.status);
        result.put("healthScore", health.score);
        result.put("windowMinutes", healthWindowMinutes());
        result.put("windowFailureCategories", windowFailureCategories());
        result.put("topFailureCategory", resolveTopFailureCategory());
        result.put("providerSwitchCount", providerSwitchCount.get());
        result.put("windowProviderSwitchCount", windowProviderSwitchCount());
        result.put("thresholds", thresholdSnapshot());
        result.put("alerts", health.alerts.stream().map(AlertItem::toMap).toList());
        return result;
    }

    public void recordProviderSwitch(String fromProvider, String toProvider, String category, String reason) {
        ensureThresholdSettingsInitialized();
        long now = System.currentTimeMillis();
        providerSwitchCount.incrementAndGet();
        appendProviderSwitch(new ProviderSwitchRecord(
            now,
            blankToUnknown(fromProvider),
            blankToUnknown(toProvider),
            blankToUnknown(category),
            reason == null ? "" : reason
        ));
        persistProviderSwitchIfNeeded(now, fromProvider, toProvider, category, reason);
    }

    public List<Map<String, Object>> alertHistory(int limit) {
        return alertHistory(limit, null, null);
    }

    public List<Map<String, Object>> alertHistory(int limit, String status, String keyword) {
        if (auditLogMapper == null) {
            return List.of();
        }
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        String normalizedStatus = normalizeStatus(status);
        String normalizedKeyword = keyword == null ? "" : keyword.trim();

        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuditLog::getResourceType, OBSERVABILITY_RESOURCE_TYPE)
            .orderByDesc(AuditLog::getCreatedAt);
        if ("alert".equals(normalizedStatus)) {
            wrapper.eq(AuditLog::getAction, OBSERVABILITY_ALERT_ACTION);
        } else if ("recovery".equals(normalizedStatus)) {
            wrapper.eq(AuditLog::getAction, OBSERVABILITY_RECOVERY_ACTION);
        } else {
            wrapper.in(AuditLog::getAction, OBSERVABILITY_ALERT_ACTION, OBSERVABILITY_RECOVERY_ACTION);
        }
        if (!normalizedKeyword.isEmpty()) {
            wrapper.and(item -> item.like(AuditLog::getErrorMessage, normalizedKeyword)
                .or()
                .like(AuditLog::getRequestBody, normalizedKeyword));
        }
        wrapper.last("LIMIT " + safeLimit);
        List<AuditLog> logs = auditLogMapper.selectList(wrapper);
        return logs.stream().map(this::toHistoryRow).toList();
    }

    public List<Map<String, Object>> switchHistory(int limit, String fromProvider, String toProvider, String category) {
        ensureThresholdSettingsInitialized();
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        if (auditLogMapper == null) {
            return recentProviderSwitchMaps().stream().limit(safeLimit).toList();
        }
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuditLog::getResourceType, PROVIDER_SWITCH_RESOURCE_TYPE)
            .eq(AuditLog::getAction, PROVIDER_SWITCH_ACTION)
            .orderByDesc(AuditLog::getCreatedAt);
        if (fromProvider != null && !fromProvider.isBlank()) {
            wrapper.like(AuditLog::getRequestBody, "from=" + fromProvider.trim());
        }
        if (toProvider != null && !toProvider.isBlank()) {
            wrapper.like(AuditLog::getRequestBody, "to=" + toProvider.trim());
        }
        if (category != null && !category.isBlank()) {
            wrapper.like(AuditLog::getRequestBody, "category=" + category.trim().toUpperCase());
        }
        wrapper.last("LIMIT " + safeLimit);
        return auditLogMapper.selectList(wrapper).stream().map(this::toSwitchHistoryRow).toList();
    }

    public synchronized Map<String, Object> thresholdSettings() {
        ensureThresholdSettingsInitialized();
        return thresholdSnapshot();
    }

    public synchronized Map<String, Object> updateThresholdSettings(ThresholdMutation mutation, String operator) {
        ensureThresholdSettingsInitialized();
        if (mutation == null) {
            throw new IllegalArgumentException("阈值参数不能为空");
        }
        AiObservabilityThresholdProperties target = ensureThresholdProperties();
        applyThresholdMutation(target, mutation);
        persistThresholdUpdate(target, operator);
        return thresholdSnapshot();
    }

    public List<Map<String, Object>> thresholdHistory(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        if (auditLogMapper == null) {
            return List.of();
        }
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuditLog::getResourceType, THRESHOLD_RESOURCE_TYPE)
            .eq(AuditLog::getAction, THRESHOLD_UPDATE_ACTION)
            .orderByDesc(AuditLog::getCreatedAt)
            .last("LIMIT " + safeLimit);
        return auditLogMapper.selectList(wrapper).stream().map(this::toThresholdHistoryRow).toList();
    }

    private ProviderStats providerStats(String provider) {
        return providerStats.computeIfAbsent(provider == null || provider.isBlank() ? "unknown" : provider, key -> new ProviderStats());
    }

    private synchronized void appendRecent(CallRecord record) {
        recentCalls.addFirst(record);
        while (recentCalls.size() > recentCallLimit()) {
            recentCalls.removeLast();
        }
    }

    private synchronized List<Map<String, Object>> recentCallMaps() {
        List<Map<String, Object>> rows = new ArrayList<>(recentCalls.size());
        for (CallRecord record : recentCalls) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("timestamp", record.timestamp);
            row.put("provider", record.provider);
            row.put("operation", record.operation);
            row.put("success", record.success);
            row.put("durationMs", record.durationMs);
            row.put("attempts", record.attempts);
            row.put("promptChars", record.promptChars);
            row.put("category", record.category);
            row.put("message", record.message);
            rows.add(row);
        }
        return rows;
    }

    private synchronized void appendProviderSwitch(ProviderSwitchRecord record) {
        recentProviderSwitches.addFirst(record);
        while (recentProviderSwitches.size() > switchEventLimit()) {
            recentProviderSwitches.removeLast();
        }
    }

    private synchronized List<Map<String, Object>> recentProviderSwitchMaps() {
        List<Map<String, Object>> rows = new ArrayList<>(recentProviderSwitches.size());
        for (ProviderSwitchRecord record : recentProviderSwitches) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("timestamp", record.timestamp);
            row.put("fromProvider", record.fromProvider);
            row.put("toProvider", record.toProvider);
            row.put("category", record.category);
            row.put("reason", record.reason);
            rows.add(row);
        }
        return rows;
    }

    private synchronized long windowProviderSwitchCount() {
        long now = System.currentTimeMillis();
        return recentProviderSwitches.stream()
            .filter(record -> now - record.timestamp <= healthWindowMillis())
            .count();
    }

    private synchronized List<CallRecord> recentCallSnapshot() {
        return new ArrayList<>(recentCalls);
    }

    private void persistHealthEventIfNeeded() {
        if (auditLogMapper == null) {
            return;
        }
        try {
            HealthSnapshot snapshot = buildHealthSnapshot();
            long now = System.currentTimeMillis();
            String fingerprint = buildAlertFingerprint(snapshot);
            String status = snapshot.status;
            boolean isAlertStatus = "critical".equals(status) || "degraded".equals(status);
            boolean wasAlertStatus = "critical".equals(lastPersistedHealthStatus) || "degraded".equals(lastPersistedHealthStatus);
            boolean changed = !fingerprint.equals(lastPersistedFingerprint) || !status.equals(lastPersistedHealthStatus);
            boolean cooldownReached = now - lastPersistedAt >= OBS_EVENT_COOLDOWN_MILLIS;

            if (isAlertStatus && (changed || cooldownReached)) {
                persistAuditEvent(snapshot, OBSERVABILITY_ALERT_ACTION, now);
                notifyEvent(snapshot, OBSERVABILITY_ALERT_ACTION);
                lastPersistedHealthStatus = status;
                lastPersistedFingerprint = fingerprint;
                lastPersistedAt = now;
                return;
            }

            if (!isAlertStatus && wasAlertStatus && (changed || cooldownReached)) {
                persistAuditEvent(snapshot, OBSERVABILITY_RECOVERY_ACTION, now);
                notifyEvent(snapshot, OBSERVABILITY_RECOVERY_ACTION);
                lastPersistedHealthStatus = status;
                lastPersistedFingerprint = fingerprint;
                lastPersistedAt = now;
            }
        } catch (Exception ex) {
            log.debug("记录 AI 可观测性事件失败: {}", ex.getMessage());
        }
    }

    private String buildAlertFingerprint(HealthSnapshot snapshot) {
        String alertCodes = snapshot.alerts.stream()
            .map(alert -> alert.code + ":" + alert.level)
            .sorted()
            .reduce((left, right) -> left + "|" + right)
            .orElse("");
        return snapshot.status + "#" + snapshot.score + "#" + alertCodes;
    }

    private void persistAuditEvent(HealthSnapshot snapshot, String action, long timestamp) {
        String alertMessage = snapshot.alerts.stream()
            .map(alert -> "[" + alert.level + "] " + alert.title)
            .limit(4)
            .reduce((left, right) -> left + "；" + right)
            .orElse("无告警");

        AuditLog logEntry = AuditLog.builder()
            .traceId("aiobs" + timestamp)
            .userId(0L)
            .username("system")
            .action(action)
            .resourceType(OBSERVABILITY_RESOURCE_TYPE)
            .requestMethod("SYSTEM")
            .requestUri("/api/ai-model/observability/alerts")
            .requestBody("status=" + snapshot.status + ", score=" + snapshot.score)
            .responseStatus(200)
            .responseBody(alertMessage)
            .executeTimeMs(0)
            .result(SysConstant.RESULT_SUCCESS)
            .errorMessage(alertMessage)
            .createdAt(LocalDateTime.now())
            .build();
        auditLogMapper.insert(logEntry);
    }

    private void notifyEvent(HealthSnapshot snapshot, String action) {
        if (notificationService == null) {
            return;
        }
        try {
            String summary = snapshot.alerts.stream()
                .map(alert -> "[" + alert.level + "] " + alert.title)
                .limit(4)
                .reduce((left, right) -> left + "；" + right)
                .orElse("无告警");
            notificationService.notifyEvent(action, snapshot.status, snapshot.score,
                snapshot.alerts.stream().map(AlertItem::toMap).toList(), summary);
        } catch (Exception ex) {
            log.debug("发送 AI 可观测性通知失败: {}", ex.getMessage());
        }
    }

    private void persistProviderSwitchIfNeeded(long timestamp, String fromProvider, String toProvider, String category, String reason) {
        if (auditLogMapper == null) {
            return;
        }
        AuditLog logEntry = AuditLog.builder()
            .traceId("aisw" + timestamp)
            .userId(0L)
            .username("system")
            .action(PROVIDER_SWITCH_ACTION)
            .resourceType(PROVIDER_SWITCH_RESOURCE_TYPE)
            .requestMethod("SYSTEM")
            .requestUri("/api/ai-model/observability/switches")
            .requestBody("from=" + blankToUnknown(fromProvider)
                + ",to=" + blankToUnknown(toProvider)
                + ",category=" + blankToUnknown(category))
            .responseStatus(200)
            .responseBody(reason == null ? "" : reason)
            .executeTimeMs(0)
            .result(SysConstant.RESULT_SUCCESS)
            .errorMessage(reason == null ? "" : reason)
            .createdAt(LocalDateTime.now())
            .build();
        auditLogMapper.insert(logEntry);
    }

    private void persistThresholdUpdate(AiObservabilityThresholdProperties target, String operator) {
        if (auditLogMapper == null) {
            return;
        }
        long now = System.currentTimeMillis();
        String thresholdPayload = "windowMinutes=" + target.getWindowMinutes()
            + ",minSampleSizeForAlert=" + target.getMinSampleSizeForAlert()
            + ",failureRateWarning=" + target.getFailureRateWarning()
            + ",failureRateCritical=" + target.getFailureRateCritical()
            + ",latencyWarningMs=" + target.getLatencyWarningMs()
            + ",latencyCriticalMs=" + target.getLatencyCriticalMs()
            + ",consecutiveFailureWarning=" + target.getConsecutiveFailureWarning()
            + ",consecutiveFailureCritical=" + target.getConsecutiveFailureCritical()
            + ",categorySpikeWarning=" + target.getCategorySpikeWarning()
            + ",categorySpikeCritical=" + target.getCategorySpikeCritical()
            + ",recentCallLimit=" + target.getRecentCallLimit()
            + ",switchEventLimit=" + target.getSwitchEventLimit();
        AuditLog logEntry = AuditLog.builder()
            .traceId("aith" + now)
            .userId(0L)
            .username((operator == null || operator.isBlank()) ? "system" : operator)
            .action(THRESHOLD_UPDATE_ACTION)
            .resourceType(THRESHOLD_RESOURCE_TYPE)
            .requestMethod("SYSTEM")
            .requestUri("/api/ai-model/observability/thresholds")
            .requestBody(thresholdPayload)
            .responseStatus(200)
            .responseBody("ok")
            .executeTimeMs(0)
            .result(SysConstant.RESULT_SUCCESS)
            .errorMessage("threshold-updated")
            .createdAt(LocalDateTime.now())
            .build();
        auditLogMapper.insert(logEntry);
    }

    private Map<String, Object> toHistoryRow(AuditLog logEntry) {
        Map<String, Object> row = new LinkedHashMap<>();
        long timestamp = logEntry.getCreatedAt() == null
            ? 0L
            : logEntry.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        String status = OBSERVABILITY_RECOVERY_ACTION.equals(logEntry.getAction()) ? "recovery" : "alert";
        row.put("id", logEntry.getId());
        row.put("timestamp", timestamp);
        row.put("action", logEntry.getAction());
        row.put("resourceType", logEntry.getResourceType());
        row.put("status", status);
        row.put("summary", logEntry.getErrorMessage() == null ? "" : logEntry.getErrorMessage());
        row.put("score", parseScore(logEntry.getRequestBody()));
        row.put("rawScore", logEntry.getRequestBody());
        row.put("level", resolveLevel(logEntry.getErrorMessage(), status));
        row.put("result", logEntry.getResult());
        return row;
    }

    private Map<String, Object> toSwitchHistoryRow(AuditLog logEntry) {
        Map<String, Object> row = new LinkedHashMap<>();
        long timestamp = logEntry.getCreatedAt() == null
            ? 0L
            : logEntry.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        Map<String, String> parsed = parseSwitchRequest(logEntry.getRequestBody());
        row.put("id", logEntry.getId());
        row.put("timestamp", timestamp);
        row.put("action", logEntry.getAction());
        row.put("fromProvider", parsed.getOrDefault("from", "unknown"));
        row.put("toProvider", parsed.getOrDefault("to", "unknown"));
        row.put("category", parsed.getOrDefault("category", "UNKNOWN"));
        row.put("reason", logEntry.getErrorMessage() == null ? "" : logEntry.getErrorMessage());
        return row;
    }

    private Map<String, Object> toThresholdHistoryRow(AuditLog logEntry) {
        Map<String, Object> row = new LinkedHashMap<>();
        long timestamp = logEntry.getCreatedAt() == null
            ? 0L
            : logEntry.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        row.put("id", logEntry.getId());
        row.put("timestamp", timestamp);
        row.put("action", logEntry.getAction());
        row.put("operator", logEntry.getUsername());
        row.put("thresholds", parseSwitchRequest(logEntry.getRequestBody()));
        return row;
    }

    private Map<String, String> parseSwitchRequest(String requestBody) {
        Map<String, String> values = new LinkedHashMap<>();
        if (requestBody == null || requestBody.isBlank()) {
            return values;
        }
        String[] pairs = requestBody.split(",");
        for (String pair : pairs) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2) {
                values.put(parts[0].trim(), parts[1].trim());
            }
        }
        return values;
    }

    private String blankToUnknown(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim();
    }

    private String normalizeStatus(String status) {
        if (status == null) {
            return "all";
        }
        String normalized = status.trim().toLowerCase();
        if ("alert".equals(normalized) || "recovery".equals(normalized)) {
            return normalized;
        }
        return "all";
    }

    private void ensureThresholdSettingsInitialized() {
        if (thresholdSettingsLoaded) {
            return;
        }
        synchronized (this) {
            if (thresholdSettingsLoaded) {
                return;
            }
            loadPersistedThresholdSettings();
            thresholdSettingsLoaded = true;
        }
    }

    private void loadPersistedThresholdSettings() {
        if (auditLogMapper == null) {
            return;
        }
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuditLog::getResourceType, THRESHOLD_RESOURCE_TYPE)
            .eq(AuditLog::getAction, THRESHOLD_UPDATE_ACTION)
            .orderByDesc(AuditLog::getCreatedAt)
            .last("LIMIT 1");
        AuditLog row = auditLogMapper.selectOne(wrapper);
        if (row == null || row.getRequestBody() == null || row.getRequestBody().isBlank()) {
            return;
        }
        Map<String, String> values = parseSwitchRequest(row.getRequestBody());
        AiObservabilityThresholdProperties target = ensureThresholdProperties();
        applyPersistedThreshold(target, values, "windowMinutes");
        applyPersistedThreshold(target, values, "minSampleSizeForAlert");
        applyPersistedThreshold(target, values, "failureRateWarning");
        applyPersistedThreshold(target, values, "failureRateCritical");
        applyPersistedThreshold(target, values, "latencyWarningMs");
        applyPersistedThreshold(target, values, "latencyCriticalMs");
        applyPersistedThreshold(target, values, "consecutiveFailureWarning");
        applyPersistedThreshold(target, values, "consecutiveFailureCritical");
        applyPersistedThreshold(target, values, "categorySpikeWarning");
        applyPersistedThreshold(target, values, "categorySpikeCritical");
        applyPersistedThreshold(target, values, "recentCallLimit");
        applyPersistedThreshold(target, values, "switchEventLimit");
    }

    private void applyPersistedThreshold(AiObservabilityThresholdProperties target, Map<String, String> values, String key) {
        String value = values.get(key);
        if (value == null || value.isBlank()) {
            return;
        }
        try {
            switch (key) {
                case "windowMinutes" -> target.setWindowMinutes(Integer.parseInt(value));
                case "minSampleSizeForAlert" -> target.setMinSampleSizeForAlert(Integer.parseInt(value));
                case "failureRateWarning" -> target.setFailureRateWarning(Double.parseDouble(value));
                case "failureRateCritical" -> target.setFailureRateCritical(Double.parseDouble(value));
                case "latencyWarningMs" -> target.setLatencyWarningMs(Long.parseLong(value));
                case "latencyCriticalMs" -> target.setLatencyCriticalMs(Long.parseLong(value));
                case "consecutiveFailureWarning" -> target.setConsecutiveFailureWarning(Integer.parseInt(value));
                case "consecutiveFailureCritical" -> target.setConsecutiveFailureCritical(Integer.parseInt(value));
                case "categorySpikeWarning" -> target.setCategorySpikeWarning(Integer.parseInt(value));
                case "categorySpikeCritical" -> target.setCategorySpikeCritical(Integer.parseInt(value));
                case "recentCallLimit" -> target.setRecentCallLimit(Integer.parseInt(value));
                case "switchEventLimit" -> target.setSwitchEventLimit(Integer.parseInt(value));
                default -> { }
            }
        } catch (NumberFormatException ex) {
            log.debug("忽略无效阈值配置 key={}, value={}", key, value);
        }
    }

    private AiObservabilityThresholdProperties ensureThresholdProperties() {
        if (thresholdProperties == null) {
            thresholdProperties = new AiObservabilityThresholdProperties();
        }
        return thresholdProperties;
    }

    private void applyThresholdMutation(AiObservabilityThresholdProperties target, ThresholdMutation mutation) {
        if (mutation.windowMinutes() != null) {
            target.setWindowMinutes(mutation.windowMinutes());
        }
        if (mutation.minSampleSizeForAlert() != null) {
            target.setMinSampleSizeForAlert(mutation.minSampleSizeForAlert());
        }
        if (mutation.failureRateWarning() != null) {
            target.setFailureRateWarning(mutation.failureRateWarning());
        }
        if (mutation.failureRateCritical() != null) {
            target.setFailureRateCritical(mutation.failureRateCritical());
        }
        if (mutation.latencyWarningMs() != null) {
            target.setLatencyWarningMs(mutation.latencyWarningMs());
        }
        if (mutation.latencyCriticalMs() != null) {
            target.setLatencyCriticalMs(mutation.latencyCriticalMs());
        }
        if (mutation.consecutiveFailureWarning() != null) {
            target.setConsecutiveFailureWarning(mutation.consecutiveFailureWarning());
        }
        if (mutation.consecutiveFailureCritical() != null) {
            target.setConsecutiveFailureCritical(mutation.consecutiveFailureCritical());
        }
        if (mutation.categorySpikeWarning() != null) {
            target.setCategorySpikeWarning(mutation.categorySpikeWarning());
        }
        if (mutation.categorySpikeCritical() != null) {
            target.setCategorySpikeCritical(mutation.categorySpikeCritical());
        }
        if (mutation.recentCallLimit() != null) {
            target.setRecentCallLimit(mutation.recentCallLimit());
        }
        if (mutation.switchEventLimit() != null) {
            target.setSwitchEventLimit(mutation.switchEventLimit());
        }
    }

    private Integer parseScore(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        Matcher matcher = SCORE_PATTERN.matcher(raw);
        if (!matcher.find()) {
            return null;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String resolveLevel(String summary, String status) {
        if ("recovery".equals(status)) {
            return "info";
        }
        if (summary == null || summary.isBlank()) {
            return "warning";
        }
        String lower = summary.toLowerCase();
        if (lower.contains("critical") || lower.contains("严重")) {
            return "critical";
        }
        return "warning";
    }

    private HealthSnapshot buildHealthSnapshot() {
        ensureThresholdSettingsInitialized();
        long now = System.currentTimeMillis();
        List<CallRecord> records = recentCallSnapshot();
        List<CallRecord> windowCalls = records.stream()
            .filter(record -> now - record.timestamp <= healthWindowMillis())
            .toList();

        long totalWindowCalls = windowCalls.size();
        if (totalWindowCalls == 0) {
            if (totalCalls.get() == 0) {
                return new HealthSnapshot("no-traffic", 100, List.of());
            }
            return new HealthSnapshot("healthy", 100, List.of());
        }

        long windowFailedCalls = windowCalls.stream().filter(record -> !record.success).count();
        long windowLatencyAvg = (long) windowCalls.stream().mapToLong(record -> Math.max(record.durationMs, 0)).average().orElse(0);
        double failureRate = (double) windowFailedCalls / totalWindowCalls;

        List<AlertItem> alerts = new ArrayList<>();
        if (totalWindowCalls >= minSampleSizeForAlert()) {
            if (failureRate >= failureRateCriticalThreshold()) {
                alerts.add(new AlertItem(
                    "failure-rate",
                    "critical",
                    "失败率过高",
                    "最近 " + healthWindowMinutes() + " 分钟失败率达到 " + round(failureRate * 100) + "%，请检查 API Key、配额或网络稳定性。",
                    now
                ));
            } else if (failureRate >= failureRateWarningThreshold()) {
                alerts.add(new AlertItem(
                    "failure-rate",
                    "warning",
                    "失败率偏高",
                    "最近 " + healthWindowMinutes() + " 分钟失败率为 " + round(failureRate * 100) + "%，建议排查失败分类并观察趋势。",
                    now
                ));
            }
        }

        if (windowLatencyAvg >= latencyCriticalMsThreshold()) {
            alerts.add(new AlertItem(
                "latency",
                "critical",
                "调用延迟严重",
                "最近 " + healthWindowMinutes() + " 分钟平均耗时 " + windowLatencyAvg + "ms，明显高于预期阈值。",
                now
            ));
        } else if (windowLatencyAvg >= latencyWarningMsThreshold()) {
            alerts.add(new AlertItem(
                "latency",
                "warning",
                "调用延迟偏高",
                "最近 " + healthWindowMinutes() + " 分钟平均耗时 " + windowLatencyAvg + "ms，建议关注上游模型负载。",
                now
            ));
        }

        int consecutiveFailures = countConsecutiveFailures(records);
        if (consecutiveFailures >= consecutiveFailureCriticalThreshold()) {
            alerts.add(new AlertItem(
                "consecutive-failures",
                "critical",
                "连续失败告警",
                "最近连续失败 " + consecutiveFailures + " 次，建议立即切换默认提供商或降级模式。",
                now
            ));
        } else if (consecutiveFailures >= consecutiveFailureWarningThreshold()) {
            alerts.add(new AlertItem(
                "consecutive-failures",
                "warning",
                "连续失败预警",
                "最近连续失败 " + consecutiveFailures + " 次，请优先检查鉴权和网络链路。",
                now
            ));
        }

        long authFailures = windowCalls.stream()
            .filter(record -> !record.success)
            .filter(record -> "AUTH".equalsIgnoreCase(record.category) || "CONFIG".equalsIgnoreCase(record.category))
            .count();
        if (authFailures > 0) {
            alerts.add(new AlertItem(
                "auth-config",
                "warning",
                "鉴权或配置异常",
                "最近窗口内出现 " + authFailures + " 次 AUTH/CONFIG 失败，请检查提供商启用状态和 API Key。",
                now
            ));
        }
        long timeoutFailures = countFailureByCategory(windowCalls, "TIMEOUT");
        if (timeoutFailures >= categorySpikeCriticalThreshold()) {
            alerts.add(new AlertItem(
                "timeout-spike",
                "critical",
                "超时失败激增",
                "最近窗口内 TIMEOUT 失败达到 " + timeoutFailures + " 次，建议检查网络链路和模型响应超时配置。",
                now
            ));
        } else if (timeoutFailures >= categorySpikeWarningThreshold()) {
            alerts.add(new AlertItem(
                "timeout-spike",
                "warning",
                "超时失败升高",
                "最近窗口内 TIMEOUT 失败达到 " + timeoutFailures + " 次，建议观察上游负载并评估重试策略。",
                now
            ));
        }

        long rateLimitFailures = countFailureByCategory(windowCalls, "RATE_LIMIT");
        if (rateLimitFailures >= categorySpikeCriticalThreshold()) {
            alerts.add(new AlertItem(
                "rate-limit-spike",
                "critical",
                "限流失败激增",
                "最近窗口内 RATE_LIMIT 失败达到 " + rateLimitFailures + " 次，建议切换备份提供商并提升配额。",
                now
            ));
        } else if (rateLimitFailures >= categorySpikeWarningThreshold()) {
            alerts.add(new AlertItem(
                "rate-limit-spike",
                "warning",
                "限流失败升高",
                "最近窗口内 RATE_LIMIT 失败达到 " + rateLimitFailures + " 次，建议降低并发并启用主备切换。",
                now
            ));
        }

        if (authFailures >= categorySpikeWarningThreshold()) {
            alerts.add(new AlertItem(
                "auth-failures",
                authFailures >= categorySpikeCriticalThreshold() ? "critical" : "warning",
                authFailures >= categorySpikeCriticalThreshold() ? "鉴权失败激增" : "鉴权失败升高",
                "最近窗口内 AUTH/CONFIG 失败达到 " + authFailures + " 次，请核对 API Key、权限和提供商启用状态。",
                now
            ));
        }

        int score = 100;
        for (AlertItem alert : alerts) {
            score -= "critical".equals(alert.level()) ? 35 : 15;
        }
        score = Math.max(score, 0);

        String status = "healthy";
        if (alerts.stream().anyMatch(alert -> "critical".equals(alert.level()))) {
            status = "critical";
        } else if (!alerts.isEmpty()) {
            status = "degraded";
        }

        return new HealthSnapshot(status, score, alerts);
    }

    private Map<String, Long> windowFailureCategories() {
        ensureThresholdSettingsInitialized();
        long now = System.currentTimeMillis();
        return recentCallSnapshot().stream()
            .filter(record -> now - record.timestamp <= healthWindowMillis())
            .filter(record -> !record.success)
            .collect(LinkedHashMap::new, (map, record) -> {
                String category = (record.category == null || record.category.isBlank()) ? "UNKNOWN" : record.category;
                map.put(category, map.getOrDefault(category, 0L) + 1);
            }, Map::putAll);
    }

    private Map<String, Object> thresholdSnapshot() {
        ensureThresholdSettingsInitialized();
        Map<String, Object> threshold = new LinkedHashMap<>();
        threshold.put("windowMinutes", healthWindowMinutes());
        threshold.put("minSampleSizeForAlert", minSampleSizeForAlert());
        threshold.put("failureRateWarning", failureRateWarningThreshold());
        threshold.put("failureRateCritical", failureRateCriticalThreshold());
        threshold.put("latencyWarningMs", latencyWarningMsThreshold());
        threshold.put("latencyCriticalMs", latencyCriticalMsThreshold());
        threshold.put("consecutiveFailureWarning", consecutiveFailureWarningThreshold());
        threshold.put("consecutiveFailureCritical", consecutiveFailureCriticalThreshold());
        threshold.put("categorySpikeWarning", categorySpikeWarningThreshold());
        threshold.put("categorySpikeCritical", categorySpikeCriticalThreshold());
        threshold.put("recentCallLimit", recentCallLimit());
        threshold.put("switchEventLimit", switchEventLimit());
        return threshold;
    }

    private int healthWindowMinutes() {
        if (thresholdProperties == null) {
            return DEFAULT_HEALTH_WINDOW_MINUTES;
        }
        return Math.max(thresholdProperties.getWindowMinutes(), 1);
    }

    private long healthWindowMillis() {
        return healthWindowMinutes() * 60_000L;
    }

    private int minSampleSizeForAlert() {
        if (thresholdProperties == null) {
            return DEFAULT_MIN_SAMPLE_SIZE_FOR_ALERT;
        }
        return Math.max(thresholdProperties.getMinSampleSizeForAlert(), 1);
    }

    private double failureRateWarningThreshold() {
        double configured = thresholdProperties == null ? DEFAULT_FAILURE_RATE_WARNING : thresholdProperties.getFailureRateWarning();
        return clamp(configured, 0.0, 1.0);
    }

    private double failureRateCriticalThreshold() {
        double configured = thresholdProperties == null ? DEFAULT_FAILURE_RATE_CRITICAL : thresholdProperties.getFailureRateCritical();
        return clamp(configured, failureRateWarningThreshold(), 1.0);
    }

    private long latencyWarningMsThreshold() {
        long configured = thresholdProperties == null ? DEFAULT_LATENCY_WARNING_MS : thresholdProperties.getLatencyWarningMs();
        return Math.max(configured, 1L);
    }

    private long latencyCriticalMsThreshold() {
        long configured = thresholdProperties == null ? DEFAULT_LATENCY_CRITICAL_MS : thresholdProperties.getLatencyCriticalMs();
        return Math.max(configured, latencyWarningMsThreshold());
    }

    private int consecutiveFailureWarningThreshold() {
        int configured = thresholdProperties == null ? DEFAULT_CONSECUTIVE_FAILURE_WARNING : thresholdProperties.getConsecutiveFailureWarning();
        return Math.max(configured, 1);
    }

    private int consecutiveFailureCriticalThreshold() {
        int configured = thresholdProperties == null ? DEFAULT_CONSECUTIVE_FAILURE_CRITICAL : thresholdProperties.getConsecutiveFailureCritical();
        return Math.max(configured, consecutiveFailureWarningThreshold());
    }

    private int categorySpikeWarningThreshold() {
        int configured = thresholdProperties == null ? DEFAULT_CATEGORY_SPIKE_WARNING : thresholdProperties.getCategorySpikeWarning();
        return Math.max(configured, 1);
    }

    private int categorySpikeCriticalThreshold() {
        int configured = thresholdProperties == null ? DEFAULT_CATEGORY_SPIKE_CRITICAL : thresholdProperties.getCategorySpikeCritical();
        return Math.max(configured, categorySpikeWarningThreshold());
    }

    private int recentCallLimit() {
        int configured = thresholdProperties == null ? DEFAULT_RECENT_LIMIT : thresholdProperties.getRecentCallLimit();
        return Math.max(configured, 1);
    }

    private int switchEventLimit() {
        int configured = thresholdProperties == null ? DEFAULT_SWITCH_EVENT_LIMIT : thresholdProperties.getSwitchEventLimit();
        return Math.max(configured, 1);
    }

    private double clamp(double value, double min, double max) {
        if (Double.isNaN(value)) {
            return min;
        }
        return Math.min(Math.max(value, min), max);
    }

    private String resolveTopFailureCategory() {
        return windowFailureCategories().entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("NONE");
    }

    private long countFailureByCategory(List<CallRecord> windowCalls, String category) {
        return windowCalls.stream()
            .filter(record -> !record.success)
            .filter(record -> category.equalsIgnoreCase(record.category))
            .count();
    }

    private int countConsecutiveFailures(List<CallRecord> records) {
        int count = 0;
        for (CallRecord record : records) {
            if (record.success) {
                break;
            }
            count++;
        }
        return count;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    @Getter
    private static class ProviderStats {
        private final AtomicLong calls = new AtomicLong();
        private final AtomicLong success = new AtomicLong();
        private final AtomicLong failed = new AtomicLong();
        private final AtomicLong latencyMs = new AtomicLong();

        void recordSuccess(long durationMs) {
            calls.incrementAndGet();
            success.incrementAndGet();
            latencyMs.addAndGet(Math.max(durationMs, 0));
        }

        void recordFailure(long durationMs) {
            calls.incrementAndGet();
            failed.incrementAndGet();
            latencyMs.addAndGet(Math.max(durationMs, 0));
        }

        Map<String, Object> toMap(String provider) {
            long total = calls.get();
            long successCount = success.get();
            long failedCount = failed.get();
            long avg = total == 0 ? 0 : latencyMs.get() / total;
            double successRate = total == 0 ? 1.0 : (double) successCount / total;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("provider", provider);
            row.put("totalCalls", total);
            row.put("successCalls", successCount);
            row.put("failedCalls", failedCount);
            row.put("successRate", Math.round(successRate * 10000.0) / 100.0);
            row.put("avgLatencyMs", avg);
            return row;
        }
    }

    private record CallRecord(
        long timestamp,
        String provider,
        String operation,
        boolean success,
        long durationMs,
        int attempts,
        int promptChars,
        String category,
        String message
    ) {}

    private record ProviderSwitchRecord(
        long timestamp,
        String fromProvider,
        String toProvider,
        String category,
        String reason
    ) {}

    private record AlertItem(
        String code,
        String level,
        String title,
        String message,
        long timestamp
    ) {
        Map<String, Object> toMap() {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("code", code);
            row.put("level", level);
            row.put("title", title);
            row.put("message", message);
            row.put("timestamp", timestamp);
            return row;
        }
    }

    private record HealthSnapshot(
        String status,
        int score,
        List<AlertItem> alerts
    ) {}
}
