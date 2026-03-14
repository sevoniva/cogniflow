package com.chatbi.controller;

import com.chatbi.common.Result;
import com.chatbi.service.EmbeddedAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 嵌入式分析控制器
 */
@Slf4j
@Tag(name = "嵌入式分析", description = "仪表板嵌入相关接口")
@RestController
@RequestMapping("/api/embedded")
@RequiredArgsConstructor
public class EmbeddedAnalyticsController {

    private final EmbeddedAnalyticsService embeddedAnalyticsService;

    /**
     * 创建嵌入配置
     */
    @Operation(summary = "创建嵌入配置", description = "生成仪表板嵌入代码")
    @PostMapping("/create")
    public Result<Map<String, Object>> createEmbed(
            @Parameter(description = "嵌入配置") @RequestBody EmbeddedAnalyticsService.EmbedConfig request) {
        try {
            var share = embeddedAnalyticsService.createEmbed(request);

            Map<String, Object> response = Map.of(
                    "shareId", share.getId(),
                    "embedToken", share.getShareToken(),
                    "embedUrl", "/embed/" + share.getShareToken(),
                    "iframeCode", "<iframe src=\"/embed/" + share.getShareToken() + "\" width=\"100%\" height=\"600\" frameborder=\"0\"></iframe>",
                    "expireTime", share.getExpireTime()
            );

            return Result.ok(response);
        } catch (Exception e) {
            log.error("创建嵌入配置失败：{}", e.getMessage(), e);
            return Result.error("创建失败：" + e.getMessage());
        }
    }

    /**
     * 获取嵌入配置
     */
    @Operation(summary = "获取嵌入配置", description = "根据 Token 获取嵌入配置")
    @GetMapping("/{embedToken}")
    public Result<Map<String, Object>> getEmbed(
            @Parameter(description = "嵌入 Token") @PathVariable String embedToken,
            HttpServletRequest request) {
        try {
            var data = embeddedAnalyticsService.getEmbedDashboardData(
                embedToken,
                request.getHeader("Origin"),
                request.getRemoteAddr(),
                request.getHeader("User-Agent")
            );
            return Result.ok(data);
        } catch (Exception e) {
            log.error("获取嵌入配置失败：{}", e.getMessage(), e);
            return Result.error("获取失败：" + e.getMessage());
        }
    }

    /**
     * 验证嵌入访问
     */
    @Operation(summary = "验证嵌入访问", description = "验证嵌入访问的合法性")
    @PostMapping("/validate")
    public Result<Map<String, Object>> validateEmbed(
            @Parameter(description = "嵌入 Token") @RequestParam String token,
            HttpServletRequest request) {
        try {
            String origin = request.getHeader("Origin");
            boolean valid = embeddedAnalyticsService.validateEmbed(token, origin);

            Map<String, Object> response = Map.of(
                    "valid", valid,
                    "token", token
            );

            if (valid) {
                return Result.ok(response);
            } else {
                return Result.error("嵌入访问验证失败");
            }
        } catch (Exception e) {
            log.error("验证嵌入访问失败：{}", e.getMessage(), e);
            return Result.error("验证失败：" + e.getMessage());
        }
    }

    /**
     * 更新嵌入配置
     */
    @Operation(summary = "更新嵌入配置", description = "更新嵌入配置信息")
    @PutMapping("/{shareId}")
    public Result<Map<String, Object>> updateEmbed(
            @Parameter(description = "分享 ID") @PathVariable Long shareId,
            @Parameter(description = "嵌入配置") @RequestBody EmbeddedAnalyticsService.EmbedConfig request) {
        try {
            var share = embeddedAnalyticsService.updateEmbed(shareId, request);

            Map<String, Object> response = Map.of(
                    "shareId", share.getId(),
                    "title", share.getTitle(),
                    "expireTime", share.getExpireTime()
            );

            return Result.ok(response);
        } catch (Exception e) {
            log.error("更新嵌入配置失败：{}", e.getMessage(), e);
            return Result.error("更新失败：" + e.getMessage());
        }
    }

    /**
     * 删除嵌入配置
     */
    @Operation(summary = "删除嵌入配置", description = "禁用嵌入配置")
    @DeleteMapping("/{shareId}")
    public Result<Void> deleteEmbed(@PathVariable Long shareId) {
        try {
            embeddedAnalyticsService.deleteEmbed(shareId);
            return Result.ok();
        } catch (Exception e) {
            log.error("删除嵌入配置失败：{}", e.getMessage(), e);
            return Result.error("删除失败：" + e.getMessage());
        }
    }

    /**
     * 获取嵌入统计信息
     */
    @Operation(summary = "获取嵌入统计", description = "获取嵌入仪表板的访问统计")
    @GetMapping("/{embedToken}/stats")
    public Result<EmbeddedAnalyticsService.EmbedStats> getStats(
            @Parameter(description = "嵌入 Token") @PathVariable String embedToken) {
        try {
            return Result.ok(embeddedAnalyticsService.getEmbedStats(embedToken));
        } catch (Exception e) {
            log.error("获取统计信息失败：{}", e.getMessage(), e);
            return Result.error("获取失败：" + e.getMessage());
        }
    }
}
