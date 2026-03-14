package com.chatbi.controller;

import com.chatbi.common.Result;
import com.chatbi.entity.Subscription;
import com.chatbi.service.SubscriptionService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 订阅管理控制器
 */
@Tag(name = "订阅管理", description = "提供订阅的创建、查询、删除等功能")
@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    /**
     * 分页查询订阅列表
     */
    @Operation(summary = "分页查询订阅列表", description = "返回订阅列表")
    @GetMapping
    @PreAuthorize("hasAuthority('subscription:query')")
    public Result<Page<Subscription>> list(
            @Parameter(description = "订阅人 ID") @RequestParam(required = false) Long subscriberId,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<Subscription> page = subscriptionService.page(subscriberId, current, size);
        return Result.ok(page);
    }

    /**
     * 根据 ID 查询订阅
     */
    @Operation(summary = "根据 ID 查询订阅", description = "返回指定 ID 的订阅详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('subscription:query')")
    public Result<Subscription> get(@PathVariable Long id) {
        Subscription subscription = subscriptionService.getById(id);
        return Result.ok(subscription);
    }

    /**
     * 创建订阅
     */
    @Operation(summary = "创建订阅", description = "创建新的订阅")
    @PostMapping
    @PreAuthorize("hasAuthority('subscription:add')")
    public Result<Subscription> create(@RequestBody Subscription subscription) {
        Subscription created = subscriptionService.create(subscription);
        return Result.ok("创建成功", created);
    }

    /**
     * 更新订阅
     */
    @Operation(summary = "更新订阅", description = "更新订阅配置")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('subscription:update')")
    public Result<Subscription> update(@PathVariable Long id, @RequestBody Subscription subscription) {
        Subscription updated = subscriptionService.update(id, subscription);
        return Result.ok("更新成功", updated);
    }

    /**
     * 删除订阅
     */
    @Operation(summary = "删除订阅", description = "删除指定 ID 的订阅")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('subscription:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        subscriptionService.delete(id);
        return Result.ok("删除成功", null);
    }

    /**
     * 获取我的订阅
     */
    @Operation(summary = "获取我的订阅", description = "返回当前登录用户的订阅列表")
    @GetMapping("/my")
    public Result<Page<Subscription>> getMySubscriptions(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<Subscription> page = subscriptionService.page(userId, current, size);
        return Result.ok(page);
    }
}
