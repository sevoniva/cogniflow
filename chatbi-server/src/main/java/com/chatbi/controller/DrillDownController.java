package com.chatbi.controller;

import com.chatbi.common.Result;
import com.chatbi.service.DrillDownService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 下钻/上卷分析控制器
 */
@Slf4j
@Tag(name = "下钻分析", description = "下钻/上卷分析相关接口")
@RestController
@RequestMapping("/api/drill")
@RequiredArgsConstructor
public class DrillDownController {

    private final DrillDownService drillDownService;

    /**
     * 执行下钻分析
     */
    @Operation(summary = "下钻分析", description = "执行下钻操作，查看更细粒度的数据")
    @PostMapping("/down")
    public Result<DrillDownService.DrillDownResponse> drillDown(
            @Parameter(description = "下钻请求") @RequestBody DrillDownService.DrillDownRequest request) {
        try {
            DrillDownService.DrillDownResponse response = drillDownService.drillDown(request);
            return Result.ok(response);
        } catch (Exception e) {
            log.error("下钻分析失败：{}", e.getMessage(), e);
            return Result.error("下钻分析失败：" + e.getMessage());
        }
    }

    /**
     * 执行上卷分析
     */
    @Operation(summary = "上卷分析", description = "执行上卷操作，查看更高层级的汇总数据")
    @PostMapping("/up")
    public Result<DrillDownService.DrillDownResponse> rollUp(
            @Parameter(description = "上卷请求") @RequestBody DrillDownService.DrillDownRequest request) {
        try {
            DrillDownService.DrillDownResponse response = drillDownService.rollUp(request);
            return Result.ok(response);
        } catch (Exception e) {
            log.error("上卷分析失败：{}", e.getMessage(), e);
            return Result.error("上卷分析失败：" + e.getMessage());
        }
    }

    /**
     * 获取层级配置列表
     */
    @Operation(summary = "获取层级配置", description = "获取所有可用的层级配置")
    @GetMapping("/hierarchies")
    public Result<List<DrillDownService.HierarchyConfig>> getHierarchies() {
        return Result.ok(drillDownService.getHierarchies());
    }

    /**
     * 获取可用维度
     */
    @Operation(summary = "获取可用维度", description = "获取支持下钻/上卷的维度列表")
    @GetMapping("/dimensions")
    public Result<List<Map<String, String>>> getDimensions() {
        List<Map<String, String>> dimensions = List.of(
                Map.of("name", "year", "label", "年", "hierarchy", "time"),
                Map.of("name", "quarter", "label", "季度", "hierarchy", "time"),
                Map.of("name", "month", "label", "月", "hierarchy", "time"),
                Map.of("name", "day", "label", "日", "hierarchy", "time"),
                Map.of("name", "country", "label", "国家", "hierarchy", "geo"),
                Map.of("name", "province", "label", "省份", "hierarchy", "geo"),
                Map.of("name", "city", "label", "城市", "hierarchy", "geo"),
                Map.of("name", "category", "label", "品类", "hierarchy", "product"),
                Map.of("name", "subcategory", "label", "子品类", "hierarchy", "product"),
                Map.of("name", "brand", "label", "品牌", "hierarchy", "product")
        );
        return Result.ok(dimensions);
    }
}
