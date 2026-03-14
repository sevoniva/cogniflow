package com.chatbi.controller;

import com.chatbi.common.Result;
import com.chatbi.entity.DataPermissionRule;
import com.chatbi.service.DataPermissionService;
import com.chatbi.service.PolicyVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据权限控制器 - 行级权限
 */
@RestController
@RequestMapping("/api/data-permissions")
@RequiredArgsConstructor
public class DataPermissionController {

    private final DataPermissionService dataPermissionService;
    private final PolicyVersionService policyVersionService;

    /**
     * 获取所有规则
     */
    @GetMapping
    public Result<List<DataPermissionRule>> list() {
        return Result.ok(dataPermissionService.list());
    }

    /**
     * 根据 ID 查询
     */
    @GetMapping("/{id}")
    public Result<DataPermissionRule> getById(@PathVariable Long id) {
        return Result.ok(dataPermissionService.getById(id));
    }

    /**
     * 创建规则
     */
    @PostMapping
    public Result<DataPermissionRule> create(@RequestBody DataPermissionRule rule) {
        DataPermissionRule created = dataPermissionService.create(rule);
        policyVersionService.recordPolicyVersion("DATA_PERMISSION", "CREATE", created, "admin");
        return Result.ok(created);
    }

    /**
     * 更新规则
     */
    @PutMapping("/{id}")
    public Result<DataPermissionRule> update(
            @PathVariable Long id,
            @RequestBody DataPermissionRule rule) {
        DataPermissionRule updated = dataPermissionService.update(id, rule);
        policyVersionService.recordPolicyVersion("DATA_PERMISSION", "UPDATE", updated, "admin");
        return Result.ok(updated);
    }

    /**
     * 删除规则
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        DataPermissionRule existing = dataPermissionService.getById(id);
        dataPermissionService.delete(id);
        policyVersionService.recordPolicyVersion("DATA_PERMISSION", "DELETE", existing, "admin");
        return Result.ok();
    }

    /**
     * 获取用户在某表上的权限规则
     */
    @GetMapping("/user/{userId}/table/{tableName}")
    public Result<List<DataPermissionRule>> getUserRules(
            @PathVariable Long userId,
            @PathVariable String tableName) {
        return Result.ok(dataPermissionService.getUserRules(userId, tableName));
    }
}
