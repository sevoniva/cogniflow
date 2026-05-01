package com.chatbi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.chatbi.common.Result;
import com.chatbi.entity.SysUser;
import com.chatbi.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理控制器
 */
@Tag(name = "用户管理", description = "用户管理控制器")
@RestController
@RequestMapping("/api/system/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 根据 ID 查询用户
     */
    @Operation(summary = "根据 ID 查询用户")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:query')")
    public Result<SysUser> get(@PathVariable Long id) {
        SysUser user = userService.getById(id);
        return Result.ok(user);
    }

    /**
     * 创建用户
     */
    @Operation(summary = "创建用户")
    @PostMapping
    @PreAuthorize("hasAuthority('system:user:add')")
    public Result<SysUser> create(@RequestBody SysUser user) {
        SysUser created = userService.create(user);
        return Result.ok("创建成功", created);
    }

    /**
     * 更新用户
     */
    @Operation(summary = "更新用户")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:update')")
    public Result<SysUser> update(@PathVariable Long id, @RequestBody SysUser user) {
        SysUser updated = userService.update(id, user);
        return Result.ok("更新成功", updated);
    }

    /**
     * 删除用户
     */
    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.ok("删除成功", null);
    }

    /**
     * 重置密码
     */
    @Operation(summary = "重置密码")
    @PostMapping("/{id}/password/reset")
    @PreAuthorize("hasAuthority('system:user:update')")
    public Result<Void> resetPassword(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> body
    ) {
        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.isBlank()) {
            return Result.error("新密码不能为空");
        }
        userService.resetPassword(id, newPassword);
        return Result.ok("密码重置成功", null);
    }

    /**
     * 修改密码
     */
    @Operation(summary = "修改密码")
    @PostMapping("/password/change")
    public Result<Void> changePassword(
            @RequestBody java.util.Map<String, String> body
    ) {
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        if (oldPassword == null || newPassword == null) {
            return Result.error("密码不能为空");
        }
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error("未找到当前用户信息");
        }
        userService.changePassword(userId, oldPassword, newPassword);
        return Result.ok("密码修改成功", null);
    }

    /**
     * 获取当前登录用户 ID
     */
    private Long getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                String username = userDetails.getUsername();
                // 从用户名解析用户 ID（格式：userId:username）
                if (username.contains(":")) {
                    String[] parts = username.split(":");
                    return Long.parseLong(parts[0]);
                }
            }
        } catch (Exception e) {
            // 忽略异常
        }
        return null;
    }

    /**
     * 更新用户状态
     */
    @Operation(summary = "更新用户状态")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('system:user:update')")
    public Result<Void> updateStatus(
            @PathVariable Long id,
            @RequestParam Integer status
    ) {
        userService.updateStatus(id, status);
        return Result.ok("状态更新成功", null);
    }
}
