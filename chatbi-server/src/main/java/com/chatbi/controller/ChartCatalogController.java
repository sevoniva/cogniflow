package com.chatbi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.chatbi.common.Result;
import com.chatbi.service.ChartDataValidationService;
import com.chatbi.service.EnterpriseChartCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 企业级图表目录接口
 */
@Tag(name = "企业级图表目录", description = "企业级图表目录接口")
@RestController
@RequestMapping("/api/chart-catalog")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ChartCatalogController {

    private final EnterpriseChartCatalogService enterpriseChartCatalogService;
    private final ChartDataValidationService chartDataValidationService;

    @Operation(summary = "获取图表目录汇总")
    @GetMapping("/summary")
    public Result<Map<String, Object>> getSummary() {
        return Result.ok(enterpriseChartCatalogService.getSummary());
    }

    @Operation(summary = "获取图表类型")
    @GetMapping("/types")
    public Result<List<Map<String, Object>>> getTypes(@RequestParam(defaultValue = "0") Integer limit) {
        if (limit == null || limit <= 0) {
            return Result.ok(enterpriseChartCatalogService.getCatalog());
        }
        return Result.ok(enterpriseChartCatalogService.getFeaturedTypes(limit));
    }

    @Operation(summary = "获取图表数据验证结果")
    @GetMapping("/validation")
    public Result<Map<String, Object>> getValidation(
        @RequestParam(defaultValue = "0") Integer limit,
        @RequestParam(defaultValue = "false") Boolean onlyFailed
    ) {
        int safeLimit = limit == null ? 0 : Math.max(limit, 0);
        boolean failedOnly = onlyFailed != null && onlyFailed;
        return Result.ok(chartDataValidationService.getValidationSummary(safeLimit, failedOnly));
    }
}
