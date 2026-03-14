package com.chatbi.controller;

import com.chatbi.common.Result;
import com.chatbi.entity.SysPermission;
import com.chatbi.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 权限管理控制器
 */
@RestController
@RequestMapping("/api/system/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    /**
     * 查询权限树
     */
    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('system:permission:query')")
    public Result<List<SysPermission>> tree() {
        List<SysPermission> tree = permissionService.tree();
        return Result.ok(tree);
    }

    /**
     * 根据 ID 查询权限
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:permission:query')")
    public Result<SysPermission> get(@PathVariable Long id) {
        SysPermission permission = permissionService.getById(id);
        return Result.ok(permission);
    }

    /**
     * 创建权限
     */
    @PostMapping
    @PreAuthorize("hasAuthority('system:permission:add')")
    public Result<SysPermission> create(@RequestBody SysPermission permission) {
        SysPermission created = permissionService.create(permission);
        return Result.ok("创建成功", created);
    }

    /**
     * 更新权限
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:permission:update')")
    public Result<SysPermission> update(
            @PathVariable Long id,
            @RequestBody SysPermission permission
    ) {
        SysPermission updated = permissionService.update(id, permission);
        return Result.ok("更新成功", updated);
    }

    /**
     * 删除权限
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:permission:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        permissionService.delete(id);
        return Result.ok("删除成功", null);
    }
}
