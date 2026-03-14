package com.chatbi.controller;

import com.chatbi.common.Result;
import com.chatbi.entity.SysRole;
import com.chatbi.service.RoleService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理控制器
 */
@RestController
@RequestMapping("/api/system/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    /**
     * 分页查询角色列表
     */
    @GetMapping
    @PreAuthorize("hasAuthority('system:role:query')")
    public Result<Page<SysRole>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<SysRole> page = roleService.page(keyword, status, current, size);
        return Result.ok(page);
    }

    /**
     * 查询所有启用状态的角色
     */
    @GetMapping("/active")
    public Result<List<SysRole>> listActive() {
        List<SysRole> roles = roleService.listActive();
        return Result.ok(roles);
    }

    /**
     * 根据 ID 查询角色
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:query')")
    public Result<SysRole> get(@PathVariable Long id) {
        SysRole role = roleService.getById(id);
        return Result.ok(role);
    }

    /**
     * 创建角色
     */
    @PostMapping
    @PreAuthorize("hasAuthority('system:role:add')")
    public Result<SysRole> create(@RequestBody SysRole role) {
        SysRole created = roleService.create(role);
        return Result.ok("创建成功", created);
    }

    /**
     * 更新角色
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:update')")
    public Result<SysRole> update(@PathVariable Long id, @RequestBody SysRole role) {
        SysRole updated = roleService.update(id, role);
        return Result.ok("更新成功", updated);
    }

    /**
     * 删除角色
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.ok("删除成功", null);
    }

    /**
     * 分配权限给角色
     */
    @PostMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('system:role:update')")
    public Result<Void> assignPermissions(
            @PathVariable Long id,
            @RequestBody List<Long> permissionIds
    ) {
        roleService.assignPermissions(id, permissionIds);
        return Result.ok("分配成功", null);
    }

    /**
     * 查询角色的权限 ID 列表
     */
    @GetMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('system:role:query')")
    public Result<List<Long>> getPermissions(@PathVariable Long id) {
        List<Long> permissionIds = roleService.getPermissionIds(id);
        return Result.ok(permissionIds);
    }
}
