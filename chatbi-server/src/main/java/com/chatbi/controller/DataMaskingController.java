package com.chatbi.controller;

import com.chatbi.common.Result;
import com.chatbi.entity.DataMaskingRule;
import com.chatbi.service.DataMaskingService;
import com.chatbi.service.PolicyVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据脱敏控制器
 */
@RestController
@RequestMapping("/api/data-masking")
@RequiredArgsConstructor
public class DataMaskingController {

    private final DataMaskingService dataMaskingService;
    private final PolicyVersionService policyVersionService;

    /**
     * 获取所有规则
     */
    @GetMapping
    public Result<List<DataMaskingRule>> list() {
        return Result.ok(dataMaskingService.list());
    }

    /**
     * 根据 ID 查询
     */
    @GetMapping("/{id}")
    public Result<DataMaskingRule> getById(@PathVariable Long id) {
        return Result.ok(dataMaskingService.getById(id));
    }

    /**
     * 创建规则
     */
    @PostMapping
    public Result<DataMaskingRule> create(@RequestBody DataMaskingRule rule) {
        DataMaskingRule created = dataMaskingService.create(rule);
        policyVersionService.recordPolicyVersion("DATA_MASKING", "CREATE", created, "admin");
        return Result.ok(created);
    }

    /**
     * 更新规则
     */
    @PutMapping("/{id}")
    public Result<DataMaskingRule> update(
            @PathVariable Long id,
            @RequestBody DataMaskingRule rule) {
        DataMaskingRule updated = dataMaskingService.update(id, rule);
        policyVersionService.recordPolicyVersion("DATA_MASKING", "UPDATE", updated, "admin");
        return Result.ok(updated);
    }

    /**
     * 删除规则
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        DataMaskingRule existing = dataMaskingService.getById(id);
        dataMaskingService.delete(id);
        policyVersionService.recordPolicyVersion("DATA_MASKING", "DELETE", existing, "admin");
        return Result.ok();
    }

    /**
     * 脱敏数据值
     */
    @GetMapping("/mask")
    public Result<String> maskValue(
            @RequestParam Long userId,
            @RequestParam String tableName,
            @RequestParam String fieldName,
            @RequestParam String value) {
        return Result.ok(dataMaskingService.maskValue(userId, tableName, fieldName, value));
    }
}
