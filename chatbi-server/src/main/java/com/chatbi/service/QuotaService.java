package com.chatbi.service;

import com.chatbi.entity.UsageRecord;
import com.chatbi.repository.UsageRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 配额服务
 *
 * 管理用户 API 调用和查询的配额限制。
 * 按自然日重置配额。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuotaService {

    private final UsageRecordMapper usageRecordMapper;

    @Value("${app.quota.ai-calls-per-day:1000}")
    private int aiCallsPerDay;

    @Value("${app.quota.queries-per-day:500}")
    private int queriesPerDay;

    @Value("${app.quota.enabled:true}")
    private boolean quotaEnabled;

    /**
     * 检查用户是否有 AI 调用配额
     */
    public boolean hasAiQuota(Long userId, int requested) {
        if (!quotaEnabled) {
            return true;
        }
        int used = usageRecordMapper.sumCostByUserAndType(userId, "ai_call", todayStart());
        boolean allowed = used + requested <= aiCallsPerDay;
        if (!allowed) {
            log.warn("AI 调用配额超限 - userId: {}, used: {}, requested: {}, limit: {}", userId, used, requested, aiCallsPerDay);
        }
        return allowed;
    }

    /**
     * 检查用户是否有查询配额
     */
    public boolean hasQueryQuota(Long userId, int requested) {
        if (!quotaEnabled) {
            return true;
        }
        int used = usageRecordMapper.sumCostByUserAndType(userId, "query", todayStart());
        boolean allowed = used + requested <= queriesPerDay;
        if (!allowed) {
            log.warn("查询配额超限 - userId: {}, used: {}, requested: {}, limit: {}", userId, used, requested, queriesPerDay);
        }
        return allowed;
    }

    /**
     * 记录用量
     */
    public void record(Long userId, String resourceType, String action, int cost, String referenceId, String metadata) {
        UsageRecord record = UsageRecord.builder()
                .userId(userId)
                .resourceType(resourceType)
                .action(action)
                .cost(cost)
                .referenceId(referenceId)
                .metadata(metadata)
                .build();
        usageRecordMapper.insert(record);
        log.debug("用量记录 - userId: {}, type: {}, action: {}, cost: {}", userId, resourceType, action, cost);
    }

    private LocalDateTime todayStart() {
        return LocalDateTime.now().toLocalDate().atStartOfDay();
    }
}
