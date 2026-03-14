package com.chatbi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chatbi.entity.AuditLog;
import com.chatbi.repository.AuditLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审计日志服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogMapper auditLogMapper;

    /**
     * 分页查询审计日志
     */
    public Page<AuditLog> page(Long userId, String action, String resourceType,
                                LocalDateTime startTime, LocalDateTime endTime,
                                int current, int size) {
        return page(userId, action, resourceType, null, null, startTime, endTime, current, size);
    }

    /**
     * 分页查询审计日志（增强筛选）。
     */
    public Page<AuditLog> page(Long userId, String action, String resourceType,
                               String result, String keyword, LocalDateTime startTime, LocalDateTime endTime,
                               int current, int size) {
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();

        if (userId != null) {
            wrapper.eq(AuditLog::getUserId, userId);
        }
        if (action != null && !action.isEmpty()) {
            wrapper.eq(AuditLog::getAction, action);
        }
        if (resourceType != null && !resourceType.isEmpty()) {
            wrapper.eq(AuditLog::getResourceType, resourceType);
        }
        if (result != null && !result.isEmpty()) {
            wrapper.eq(AuditLog::getResult, result);
        }
        if (keyword != null && !keyword.isBlank()) {
            String safeKeyword = keyword.trim();
            wrapper.and(item -> item.like(AuditLog::getUsername, safeKeyword)
                .or()
                .like(AuditLog::getAction, safeKeyword)
                .or()
                .like(AuditLog::getResourceType, safeKeyword)
                .or()
                .like(AuditLog::getErrorMessage, safeKeyword)
                .or()
                .like(AuditLog::getRequestBody, safeKeyword)
                .or()
                .like(AuditLog::getRequestUri, safeKeyword));
        }
        if (startTime != null) {
            wrapper.ge(AuditLog::getCreatedAt, startTime);
        }
        if (endTime != null) {
            wrapper.le(AuditLog::getCreatedAt, endTime);
        }

        wrapper.orderByDesc(AuditLog::getCreatedAt);

        return auditLogMapper.selectPage(new Page<>(current, size), wrapper);
    }

    /**
     * 统计最近审计记录中的资源类型分布。
     */
    public List<String> topResourceTypes(int limit) {
        int safeLimit = Math.min(Math.max(limit, 10), 500);
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(AuditLog::getResourceType)
            .isNotNull(AuditLog::getResourceType)
            .groupBy(AuditLog::getResourceType)
            .orderByDesc(AuditLog::getCreatedAt)
            .last("LIMIT " + safeLimit);
        return auditLogMapper.selectList(wrapper).stream()
            .map(AuditLog::getResourceType)
            .filter(item -> item != null && !item.isBlank())
            .toList();
    }

    /**
     * 根据 ID 查询审计日志
     */
    public AuditLog getById(Long id) {
        return auditLogMapper.selectById(id);
    }

    /**
     * 保存审计日志
     */
    @Transactional(rollbackFor = Exception.class)
    public void save(AuditLog auditLog) {
        auditLogMapper.insert(auditLog);
    }

    /**
     * 删除审计日志
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        auditLogMapper.deleteById(id);
    }

    /**
     * 清理过期日志
     */
    @Transactional(rollbackFor = Exception.class)
    public int cleanExpiredLogs(LocalDateTime expireTime, int limit) {
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(AuditLog::getCreatedAt, expireTime)
               .last("LIMIT " + limit);

        List<AuditLog> logs = auditLogMapper.selectList(wrapper);
        if (logs.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (AuditLog log : logs) {
            count += auditLogMapper.deleteById(log.getId());
        }

        log.info("清理过期审计日志 {} 条", count);
        return count;
    }

    /**
     * 统计审计日志数量
     */
    public Long count(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();

        if (userId != null) {
            wrapper.eq(AuditLog::getUserId, userId);
        }
        if (startTime != null) {
            wrapper.ge(AuditLog::getCreatedAt, startTime);
        }
        if (endTime != null) {
            wrapper.le(AuditLog::getCreatedAt, endTime);
        }

        return auditLogMapper.selectCount(wrapper);
    }
}
