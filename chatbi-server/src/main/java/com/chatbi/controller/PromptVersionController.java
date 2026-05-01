package com.chatbi.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chatbi.common.Result;
import com.chatbi.entity.PromptVersion;
import com.chatbi.service.PromptVersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Prompt 版本管理接口
 */
@Tag(name = "Prompt 版本管理", description = "SQL 生成 Prompt 的版本控制与 A/B 测试")
@RestController
@RequestMapping("/api/admin/prompt-versions")
@RequiredArgsConstructor
public class PromptVersionController {

    private final PromptVersionService promptVersionService;

    @Operation(summary = "创建 Prompt 版本")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PromptVersion> create(@RequestBody PromptVersion version) {
        return Result.ok(promptVersionService.create(version));
    }

    @Operation(summary = "更新 Prompt 版本")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PromptVersion> update(@PathVariable Long id, @RequestBody PromptVersion version) {
        return Result.ok(promptVersionService.update(id, version));
    }

    @Operation(summary = "删除 Prompt 版本")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        promptVersionService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "根据 ID 查询")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PromptVersion> getById(@PathVariable Long id) {
        return Result.ok(promptVersionService.getById(id));
    }

    @Operation(summary = "分页查询")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Page<PromptVersion>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        return Result.ok(promptVersionService.page(current, size, status));
    }

    @Operation(summary = "查询所有非废弃版本")
    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<PromptVersion>> list() {
        return Result.ok(promptVersionService.listAllActiveOrDraft());
    }

    @Operation(summary = "激活指定版本")
    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PromptVersion> activate(@PathVariable Long id) {
        return Result.ok(promptVersionService.activate(id));
    }

    @Operation(summary = "复制版本")
    @PostMapping("/{id}/duplicate")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PromptVersion> duplicate(@PathVariable Long id) {
        return Result.ok(promptVersionService.duplicate(id));
    }
}
