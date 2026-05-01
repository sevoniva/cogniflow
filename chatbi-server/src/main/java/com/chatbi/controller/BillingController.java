package com.chatbi.controller;

import com.chatbi.common.Result;
import com.chatbi.service.BillingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 计费统计接口
 */
@Tag(name = "计费统计", description = "用户用量与配额统计")
@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;

    @Operation(summary = "当前用户今日用量")
    @GetMapping("/today")
    public Result<Map<String, Object>> todayUsage(@PathVariable(required = false) Long userId) {
        return Result.ok(billingService.todayUsage(userId));
    }

    @Operation(summary = "当前用户本月用量")
    @GetMapping("/month")
    public Result<Map<String, Object>> monthUsage(@PathVariable(required = false) Long userId) {
        return Result.ok(billingService.monthUsage(userId));
    }

    @Operation(summary = "系统总体用量（Admin）")
    @GetMapping("/system-overview")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> systemOverview() {
        return Result.ok(billingService.systemOverview());
    }
}
