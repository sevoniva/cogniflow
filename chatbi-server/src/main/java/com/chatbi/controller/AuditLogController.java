package com.chatbi.controller;

import com.chatbi.common.Result;
import com.chatbi.entity.AuditLog;
import com.chatbi.service.AccessAlertService;
import com.chatbi.service.AuditLogService;
import com.chatbi.service.PolicyVersionService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 审计日志控制器
 */
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;
    private final PolicyVersionService policyVersionService;
    private final AccessAlertService accessAlertService;

    /**
     * 分页查询审计日志
     */
    @GetMapping("/logs")
    @PreAuthorize("hasAuthority('admin')")
    public Result<Page<AuditLog>> getAuditLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<AuditLog> page = auditLogService.page(userId, action, resourceType, result, keyword,
                startTime, endTime, current, size);
        return Result.ok(page);
    }

    @GetMapping("/logs/options")
    @PreAuthorize("hasAuthority('admin')")
    public Result<List<String>> getResourceTypeOptions(@RequestParam(defaultValue = "100") int limit) {
        return Result.ok(auditLogService.topResourceTypes(limit));
    }

    @GetMapping("/policy-versions")
    @PreAuthorize("hasAuthority('admin')")
    public Result<List<Map<String, Object>>> getPolicyVersions(
            @RequestParam(required = false) String scope,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return Result.ok(policyVersionService.listPolicyVersions(scope, limit));
    }

    @GetMapping("/policy-versions/options")
    @PreAuthorize("hasAuthority('admin')")
    public Result<List<String>> getPolicyVersionScopes(@RequestParam(defaultValue = "100") int limit) {
        return Result.ok(policyVersionService.listScopes(limit));
    }

    @GetMapping("/access-alerts")
    @PreAuthorize("hasAuthority('admin')")
    public Result<List<Map<String, Object>>> getAccessAlerts(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return Result.ok(accessAlertService.listAlerts(status, severity, keyword, limit));
    }

    @GetMapping("/access-alerts/options")
    @PreAuthorize("hasAuthority('admin')")
    public Result<Map<String, Object>> getAccessAlertOptions(@RequestParam(defaultValue = "100") int limit) {
        return Result.ok(Map.of(
            "statuses", List.of("OPEN", "ACKED"),
            "severities", List.of("CRITICAL", "HIGH", "MEDIUM"),
            "reasons", accessAlertService.listReasons(limit)
        ));
    }

    @PutMapping("/access-alerts/{id}/ack")
    @PreAuthorize("hasAuthority('admin')")
    public Result<Map<String, Object>> acknowledgeAccessAlert(
            @PathVariable Long id,
            @RequestParam(defaultValue = "admin") String operator
    ) {
        return Result.ok(accessAlertService.acknowledge(id, operator));
    }

    /**
     * 根据 ID 查询审计日志详情
     */
    @GetMapping("/logs/{id}")
    @PreAuthorize("hasAuthority('admin')")
    public Result<AuditLog> getAuditLog(@PathVariable Long id) {
        AuditLog log = auditLogService.getById(id);
        return Result.ok(log);
    }

    /**
     * 统计审计日志
     */
    @GetMapping("/logs/count")
    @PreAuthorize("hasAuthority('admin')")
    public Result<Long> count(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime
    ) {
        Long count = auditLogService.count(userId, startTime, endTime);
        return Result.ok(count);
    }
}
