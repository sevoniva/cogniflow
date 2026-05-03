package com.chatbi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.chatbi.common.Result;
import com.chatbi.entity.Dashboard;
import com.chatbi.service.BusinessInsightService;
import com.chatbi.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 仪表板控制器
 */
@Tag(name = "仪表板", description = "仪表板控制器")
@RestController
@RequestMapping("/api/dashboards")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final BusinessInsightService businessInsightService;

    /**
     * 获取仪表板列表
     */
    @Operation(summary = "获取仪表板列表")
    @GetMapping
    public Result<List<Dashboard>> list(
            @RequestParam(required = false) Long createdBy,
            @RequestParam(required = false) Boolean isPublic) {
        return Result.ok(dashboardService.list(createdBy, isPublic));
    }

    /**
     * 根据 ID 查询
     */
    @Operation(summary = "根据 ID 查询")
    @GetMapping("/{id}")
    public Result<Dashboard> getById(@PathVariable Long id) {
        return Result.ok(dashboardService.getById(id));
    }

    /**
     * 创建仪表板
     */
    @Operation(summary = "创建仪表板")
    @PostMapping
    public Result<Dashboard> create(@RequestBody Dashboard dashboard) {
        return Result.ok(dashboardService.create(dashboard));
    }

    /**
     * 更新仪表板
     */
    @Operation(summary = "更新仪表板")
    @PutMapping("/{id}")
    public Result<Dashboard> update(
            @PathVariable Long id,
            @RequestBody Dashboard dashboard) {
        return Result.ok(dashboardService.update(id, dashboard));
    }

    /**
     * 删除仪表板
     */
    @Operation(summary = "删除仪表板")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        dashboardService.delete(id);
        return Result.ok();
    }

    /**
     * 发布/取消��布
     */
    @Operation(summary = "发布/取消发布")
    @PatchMapping("/{id}/publish")
    public Result<Void> togglePublish(
            @PathVariable Long id,
            @RequestParam(required = false) Integer status) {
        dashboardService.togglePublish(id, status == null ? 1 : status);
        return Result.ok();
    }

    /**
     * 更新布局配置
     */
    @Operation(summary = "更新布局配置")
    @PutMapping("/{id}/layout")
    public Result<Dashboard> updateLayout(
            @PathVariable Long id,
            @RequestBody DashboardLayoutRequest request) {
        return Result.ok(dashboardService.updateLayout(id, request.getLayoutConfig(), request.getChartsConfig()));
    }

    /**
     * 获取仪表板统计数据
     */
    @Operation(summary = "获取仪表板统计数据")
    @GetMapping("/{id}/stats")
    public Result<Map<String, Object>> getDashboardStats(@PathVariable Long id) {
        return Result.ok(Map.of("overview", businessInsightService.getOverviewRows()));
    }

    /**
     * 获取仪表板图表数据
     */
    @Operation(summary = "获取仪表板图表数据")
    @GetMapping("/{id}/chart-data")
    public Result<Map<String, Object>> getChartData(
            @PathVariable Long id,
            @RequestParam String chartType,
            @RequestParam(required = false, defaultValue = "部门") String dimension) {
        return Result.ok(Map.of("data", businessInsightService.getChartData(chartType, dimension)));
    }

    public static class DashboardLayoutRequest {
        private String layoutConfig;
        private String chartsConfig;

        public String getLayoutConfig() {
            return layoutConfig;
        }

        public void setLayoutConfig(String layoutConfig) {
            this.layoutConfig = layoutConfig;
        }

        public String getChartsConfig() {
            return chartsConfig;
        }

        public void setChartsConfig(String chartsConfig) {
            this.chartsConfig = chartsConfig;
        }
    }
}
