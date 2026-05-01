package com.chatbi.service;

import com.chatbi.repository.UsageRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 计费统计服务
 *
 * 提供用户用量统计、账单概览。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BillingService {

    private final UsageRecordMapper usageRecordMapper;

    /**
     * 获取用户今日用量统计
     */
    public Map<String, Object> todayUsage(Long userId) {
        LocalDateTime since = LocalDateTime.now().toLocalDate().atStartOfDay();
        Map<String, Object> stats = new HashMap<>();
        stats.put("aiCalls", usageRecordMapper.sumCostByUserAndType(userId, "ai_call", since));
        stats.put("queries", usageRecordMapper.sumCostByUserAndType(userId, "query", since));
        stats.put("exports", usageRecordMapper.sumCostByUserAndType(userId, "export", since));
        return stats;
    }

    /**
     * 获取用户本月用量统计
     */
    public Map<String, Object> monthUsage(Long userId) {
        LocalDateTime since = LocalDateTime.now().withDayOfMonth(1).toLocalDate().atStartOfDay();
        Map<String, Object> stats = new HashMap<>();
        stats.put("aiCalls", usageRecordMapper.sumCostByUserAndType(userId, "ai_call", since));
        stats.put("queries", usageRecordMapper.sumCostByUserAndType(userId, "query", since));
        stats.put("exports", usageRecordMapper.sumCostByUserAndType(userId, "export", since));
        return stats;
    }

    /**
     * 获取系统总体用量（admin 用）
     */
    public Map<String, Object> systemOverview() {
        LocalDateTime since = LocalDateTime.now().toLocalDate().atStartOfDay();
        Map<String, Object> stats = new HashMap<>();
        // 这里可以用更复杂的 SQL 聚合，先用简单实现
        stats.put("date", since.toLocalDate().toString());
        return stats;
    }
}
