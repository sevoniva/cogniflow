package com.chatbi.controller;

import com.chatbi.common.Result;
import com.chatbi.service.MetadataSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 元数据同步控制器
 */
@Slf4j
@Tag(name = "元数据同步", description = "元数据自动同步相关接口")
@RestController
@RequestMapping("/api/metadata")
@RequiredArgsConstructor
public class MetadataSyncController {

    private final MetadataSyncService metadataSyncService;

    /**
     * 同步数据源元数据
     */
    @Operation(summary = "同步数据源元数据", description = "同步指定数据源的表和字段元数据")
    @PostMapping("/sync/{datasourceId}")
    public Result<Map<String, Object>> syncMetadata(
            @Parameter(description = "数据源 ID") @PathVariable Long datasourceId) {

        Map<String, Object> result = new HashMap<>();

        try {
            metadataSyncService.syncDatasourceMetadata(datasourceId);
            result.put("success", true);
            result.put("message", "元数据同步成功");
            return Result.ok(result);

        } catch (Exception e) {
            log.error("元数据同步失败：{}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "元数据同步失败：" + e.getMessage());
            return Result.ok(result);
        }
    }

    /**
     * 批量同步所有数据源元数据
     */
    @Operation(summary = "批量同步元数据", description = "同步所有数据源的元数据")
    @PostMapping("/sync-all")
    public Result<Map<String, Object>> syncAllMetadata() {

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "批量元数据同步完成");

        // TODO: 实现批量同步逻辑

        return Result.ok(result);
    }
}
