package com.chatbi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatbi.dto.ApiResponse;
import com.chatbi.entity.Metric;
import com.chatbi.repository.MetricMapper;
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

    private final MetricMapper metricMapper;

    /**
     * 获取所有指标
     */
    @Operation(summary = "获取所有指标")
    @GetMapping
    public ApiResponse<List<Metric>> getMetrics() {
        return ApiResponse.ok(metricMapper.selectList(null));
    }

    /**
     * 获取启用的指标（用户侧调用）
     */
    @Operation(summary = "获取启用的指标（用户侧调用）")
    @GetMapping("/active")
    public ApiResponse<List<Metric>> getActiveMetrics() {
        LambdaQueryWrapper<Metric> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Metric::getStatus, List.of("active", "1"));
        return ApiResponse.ok(metricMapper.selectList(wrapper));
    }

    /**
     * 根据 ID 获取指标
     */
    @Operation(summary = "根据 ID 获取指标")
    @GetMapping("/{id}")
    public ApiResponse<Metric> getMetricById(@PathVariable Long id) {
        Metric metric = metricMapper.selectById(id);
        if (metric != null) {
            return ApiResponse.ok(metric);
        }
        return ApiResponse.error("指标不存在");
    }

    /**
     * 新增指标
     */
    @Operation(summary = "新增指标")
    @PostMapping
    public ApiResponse<Metric> addMetric(@RequestBody Metric request) {
        // 检查编码是否已存在
        LambdaQueryWrapper<Metric> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Metric::getCode, request.getCode());
        if (metricMapper.selectCount(wrapper) > 0) {
            return ApiResponse.error("指标编码已存在");
        }

        metricMapper.insert(request);
        return ApiResponse.ok(request);
    }

    /**
     * 更新指标
     */
    @Operation(summary = "更新指标")
    @PutMapping("/{id}")
    public ApiResponse<Metric> updateMetric(@PathVariable Long id, @RequestBody Metric request) {
        Metric metric = metricMapper.selectById(id);
        if (metric == null) {
            return ApiResponse.error("指标不存在");
        }

        if (request.getName() != null) {
            metric.setName(request.getName());
        }
        if (request.getDefinition() != null) {
            metric.setDefinition(request.getDefinition());
        }
        if (request.getStatus() != null) {
            metric.setStatus(request.getStatus());
        }

        metricMapper.updateById(metric);
        return ApiResponse.ok(metric);
    }

    /**
     * 删除指标
     */
    @Operation(summary = "删除指标")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteMetric(@PathVariable Long id) {
        metricMapper.deleteById(id);
        return ApiResponse.ok();
    }

    /**
     * 切换指标状态
     */
    @Operation(summary = "切换指标状态")
    @PatchMapping("/{id}/toggle")
    public ApiResponse<Metric> toggleMetricStatus(@PathVariable Long id) {
        Metric metric = metricMapper.selectById(id);
        if (metric == null) {
            return ApiResponse.error("指标不存在");
        }

        String current = String.valueOf(metric.getStatus());
        boolean active = "active".equalsIgnoreCase(current) || "1".equals(current);
        metric.setStatus(active ? "inactive" : "active");
        metricMapper.updateById(metric);
        return ApiResponse.ok(metric);
    }
}
