package com.chatbi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.chatbi.common.Result;
import com.chatbi.entity.Metric;
import com.chatbi.service.MetricService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 指标管理接口 - 对应前端 IAdminService 指标部分
 */
@Tag(name = "指标管理", description = "指标管理接口 - 对应前端 IAdminService 指标部分")
@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
public class MetricController {

    private final MetricService metricService;

    @Operation(summary = "获取所有指标")
    @GetMapping
    public Result<List<Metric>> getMetrics() {
        return Result.ok(metricService.list());
    }

    @Operation(summary = "获取启用的指标（用户侧调用）")
    @GetMapping("/active")
    public Result<List<Metric>> getActiveMetrics() {
        return Result.ok(metricService.listActiveMetrics());
    }

    @Operation(summary = "根据 ID 获取指标")
    @GetMapping("/{id}")
    public Result<Metric> getMetricById(@PathVariable Long id) {
        Metric metric = metricService.getById(id);
        if (metric != null) {
            return Result.ok(metric);
        }
        return Result.error("指标不存在");
    }

    @Operation(summary = "新增指标")
    @PostMapping
    public Result<Metric> addMetric(@RequestBody Metric request) {
        try {
            return Result.ok(metricService.create(request));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "更新指标")
    @PutMapping("/{id}")
    public Result<Metric> updateMetric(@PathVariable Long id, @RequestBody Metric request) {
        try {
            return Result.ok(metricService.update(id, request));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "删除指标")
    @DeleteMapping("/{id}")
    public Result<Void> deleteMetric(@PathVariable Long id) {
        metricService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "切换指标状态")
    @PatchMapping("/{id}/toggle")
    public Result<Metric> toggleMetricStatus(@PathVariable Long id) {
        try {
            return Result.ok(metricService.toggleStatus(id));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}
