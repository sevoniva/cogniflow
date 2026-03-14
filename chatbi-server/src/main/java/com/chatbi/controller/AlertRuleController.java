package com.chatbi.controller;

import com.chatbi.common.Result;
import com.chatbi.entity.AlertRule;
import com.chatbi.service.AlertRuleService;
import com.chatbi.service.PolicyVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 告警规则控制器
 */
@RestController
@RequestMapping("/api/alert-rules")
@RequiredArgsConstructor
public class AlertRuleController {

    private final AlertRuleService alertRuleService;
    private final PolicyVersionService policyVersionService;

    /**
     * 获取所有告警规则
     */
    @GetMapping
    public Result<List<AlertRule>> list() {
        return Result.ok(alertRuleService.list());
    }

    /**
     * 根据 ID 查询
     */
    @GetMapping("/{id}")
    public Result<AlertRule> getById(@PathVariable Long id) {
        return Result.ok(alertRuleService.getById(id));
    }

    /**
     * 创建告警规则
     */
    @PostMapping
    public Result<AlertRule> create(@RequestBody AlertRule alertRule) {
        AlertRule created = alertRuleService.create(alertRule);
        policyVersionService.recordPolicyVersion("ALERT_RULE", "CREATE", created, "admin");
        return Result.ok(created);
    }

    /**
     * 更新告警规则
     */
    @PutMapping("/{id}")
    public Result<AlertRule> update(
            @PathVariable Long id,
            @RequestBody AlertRule alertRule) {
        AlertRule updated = alertRuleService.update(id, alertRule);
        policyVersionService.recordPolicyVersion("ALERT_RULE", "UPDATE", updated, "admin");
        return Result.ok(updated);
    }

    /**
     * 删除告警规则
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        AlertRule existing = alertRuleService.getById(id);
        alertRuleService.delete(id);
        policyVersionService.recordPolicyVersion("ALERT_RULE", "DELETE", existing, "admin");
        return Result.ok();
    }

    /**
     * 启用/禁用告警规则
     */
    @PatchMapping("/{id}/status")
    public Result<Void> toggleStatus(
            @PathVariable Long id,
            @RequestParam Integer status) {
        alertRuleService.toggleStatus(id, status);
        AlertRule latest = alertRuleService.getById(id);
        policyVersionService.recordPolicyVersion("ALERT_RULE", "STATUS", latest, "admin");
        return Result.ok();
    }
}
