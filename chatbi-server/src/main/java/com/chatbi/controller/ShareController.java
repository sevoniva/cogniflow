package com.chatbi.controller;

import com.chatbi.common.Result;
import com.chatbi.entity.Share;
import com.chatbi.service.ShareService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 分享管理控制器
 */
@Tag(name = "分享管理", description = "提供分享链接的创建、查询、删除等功能")
@RestController
@RequestMapping("/api/shares")
@RequiredArgsConstructor
public class ShareController {

    private final ShareService shareService;

    /**
     * 分页查询分享列表
     */
    @Operation(summary = "分页查询分享列表", description = "返回当前用户创建的分享列表")
    @GetMapping
    @PreAuthorize("hasAuthority('share:query')")
    public Result<Page<Share>> list(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<Share> page = shareService.page(userId, current, size);
        return Result.ok(page);
    }

    /**
     * 根据 ID 查询分享
     */
    @Operation(summary = "根据 ID 查询分享", description = "返回指定 ID 的分享详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('share:query')")
    public Result<Share> get(@PathVariable Long id) {
        Share share = shareService.getById(id);
        return Result.ok(share);
    }

    /**
     * 根据 Token 查询分享（公开接口）
     */
    @Operation(summary = "根据 Token 查询分享", description = "通过分享 Token 获取分享信息，用于公开访问")
    @GetMapping("/token/{token}")
    public Result<Share> getByToken(@PathVariable String token) {
        Share share = shareService.getByToken(token);

        if (share == null) {
            return Result.error("分享链接不存在");
        }

        if (!shareService.isValid(share)) {
            return Result.error("分享链接已过期或无效");
        }

        // 增加访问次数
        shareService.incrementVisits(share.getId());

        return Result.ok(share);
    }

    /**
     * 创建分享
     */
    @Operation(summary = "创建分享", description = "创建新的分享链接")
    @PostMapping
    @PreAuthorize("hasAuthority('share:add')")
    public Result<Share> create(@RequestBody Share share) {
        Share created = shareService.create(share);
        return Result.ok("创建成功", created);
    }

    /**
     * 更新分享
     */
    @Operation(summary = "更新分享", description = "更新分享配置")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('share:update')")
    public Result<Share> update(@PathVariable Long id, @RequestBody Share share) {
        Share updated = shareService.update(id, share);
        return Result.ok("更新成功", updated);
    }

    /**
     * 删除分享
     */
    @Operation(summary = "删除分享", description = "删除指定 ID 的分享")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('share:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        shareService.delete(id);
        return Result.ok("删除成功", null);
    }

    /**
     * 验证分享
     */
    @Operation(summary = "验证分享", description = "验证分享链接是否有效")
    @PostMapping("/{id}/validate")
    public Result<Map<String, Object>> validate(@PathVariable Long id) {
        Share share = shareService.getById(id);
        boolean valid = shareService.isValid(share);

        Map<String, Object> result = Map.of(
                "valid", valid,
                "message", valid ? "分享链接有效" : "分享链接已过期或无效"
        );

        return Result.ok(result);
    }
}
