package com.chatbi.controller;

import com.chatbi.common.Result;
import com.chatbi.config.AiConfig;
import com.chatbi.service.AiModelService;
import com.chatbi.service.AiObservabilityService;
import com.chatbi.service.AiProviderSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI模型管理控制器
 */
@Tag(name = "AI模型管理", description = "AI模型配置和管理接口")
@RestController
@RequestMapping("/api/ai-model")
@RequiredArgsConstructor
public class AiModelController {

    private final AiModelService aiModelService;
    private final AiObservabilityService aiObservabilityService;
    private final AiConfig aiConfig;
    private final AiProviderSettingService aiProviderSettingService;

    public record RuntimeUpdateRequest(Boolean enabled) {}

    public record ProviderUpdateRequest(
        String name,
        String apiUrl,
        String model,
        Boolean enabled,
        String apiKey,
        Boolean clearApiKey,
        Double temperature,
        Integer maxTokens
    ) {}

    public record ObservabilityThresholdUpdateRequest(
        Integer windowMinutes,
        Integer minSampleSizeForAlert,
        Double failureRateWarning,
        Double failureRateCritical,
        Long latencyWarningMs,
        Long latencyCriticalMs,
        Integer consecutiveFailureWarning,
        Integer consecutiveFailureCritical,
        Integer categorySpikeWarning,
        Integer categorySpikeCritical,
        Integer recentCallLimit,
        Integer switchEventLimit
    ) {}

    /**
     * 获取AI模型配置列表
     */
    @Operation(summary = "获取AI模型配置列表")
    @GetMapping("/providers")
    public Result<Map<String, Object>> getProviders() {
        aiProviderSettingService.initializeRuntimeSettings();
        Map<String, Object> result = buildRuntimeStatus();

        Map<String, Map<String, Object>> providers = new LinkedHashMap<>();
        aiConfig.getProviders().forEach((key, config) -> {
            Map<String, Object> providerInfo = new LinkedHashMap<>();
            providerInfo.put("name", config.getName());
            providerInfo.put("model", config.getModel());
            providerInfo.put("enabled", config.isEnabled());
            providerInfo.put("apiUrl", config.getApiUrl());
            providerInfo.put("apiKeyConfigured", config.getApiKey() != null && !config.getApiKey().isBlank());
            providerInfo.put("selected", key.equals(aiConfig.getDefaultProvider()));
            providerInfo.put("temperature", config.getTemperature());
            providerInfo.put("maxTokens", config.getMaxTokens());
            providers.put(key, providerInfo);
        });
        result.put("providers", providers);

        return Result.ok(result);
    }

    @Operation(summary = "获取AI运行状态")
    @GetMapping("/status")
    public Result<Map<String, Object>> getStatus() {
        aiProviderSettingService.initializeRuntimeSettings();
        return Result.ok(buildRuntimeStatus());
    }

    @Operation(summary = "获取AI调用可观测性摘要")
    @GetMapping("/observability")
    public Result<Map<String, Object>> getObservability() {
        aiProviderSettingService.initializeRuntimeSettings();
        return Result.ok(aiObservabilityService.snapshot());
    }

    @Operation(summary = "获取AI调用健康告警快照")
    @GetMapping("/observability/alerts")
    public Result<Map<String, Object>> getObservabilityAlerts() {
        aiProviderSettingService.initializeRuntimeSettings();
        return Result.ok(aiObservabilityService.alertSnapshot());
    }

    @Operation(summary = "获取AI调用告警历史")
    @GetMapping("/observability/alerts/history")
    public Result<List<Map<String, Object>>> getObservabilityAlertHistory(
        @RequestParam(defaultValue = "20") int limit,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String keyword
    ) {
        aiProviderSettingService.initializeRuntimeSettings();
        return Result.ok(aiObservabilityService.alertHistory(limit, status, keyword));
    }

    @Operation(summary = "获取AI主备切换历史")
    @GetMapping("/observability/switches")
    public Result<List<Map<String, Object>>> getObservabilitySwitchHistory(
        @RequestParam(defaultValue = "20") int limit,
        @RequestParam(required = false) String fromProvider,
        @RequestParam(required = false) String toProvider,
        @RequestParam(required = false) String category
    ) {
        aiProviderSettingService.initializeRuntimeSettings();
        return Result.ok(aiObservabilityService.switchHistory(limit, fromProvider, toProvider, category));
    }

    @Operation(summary = "获取AI可观测阈值配置")
    @GetMapping("/observability/thresholds")
    public Result<Map<String, Object>> getObservabilityThresholds() {
        aiProviderSettingService.initializeRuntimeSettings();
        return Result.ok(aiObservabilityService.thresholdSettings());
    }

    @Operation(summary = "更新AI可观测阈值配置")
    @PutMapping("/observability/thresholds")
    public Result<Map<String, Object>> updateObservabilityThresholds(
        @RequestBody(required = false) ObservabilityThresholdUpdateRequest request
    ) {
        aiProviderSettingService.initializeRuntimeSettings();
        return Result.ok(aiObservabilityService.updateThresholdSettings(
            new AiObservabilityService.ThresholdMutation(
                request != null ? request.windowMinutes() : null,
                request != null ? request.minSampleSizeForAlert() : null,
                request != null ? request.failureRateWarning() : null,
                request != null ? request.failureRateCritical() : null,
                request != null ? request.latencyWarningMs() : null,
                request != null ? request.latencyCriticalMs() : null,
                request != null ? request.consecutiveFailureWarning() : null,
                request != null ? request.consecutiveFailureCritical() : null,
                request != null ? request.categorySpikeWarning() : null,
                request != null ? request.categorySpikeCritical() : null,
                request != null ? request.recentCallLimit() : null,
                request != null ? request.switchEventLimit() : null
            ),
            "admin"
        ));
    }

    @Operation(summary = "获取AI可观测阈值变更历史")
    @GetMapping("/observability/thresholds/history")
    public Result<List<Map<String, Object>>> getObservabilityThresholdHistory(
        @RequestParam(defaultValue = "20") int limit
    ) {
        aiProviderSettingService.initializeRuntimeSettings();
        return Result.ok(aiObservabilityService.thresholdHistory(limit));
    }

    /**
     * 测试AI模型连接
     */
    @Operation(summary = "测试AI模型连接")
    @PostMapping("/test/{provider}")
    public Result<Boolean> testConnection(@PathVariable String provider) {
        boolean success = aiModelService.testConnection(provider);
        return Result.ok(success);
    }

    /**
     * 切换默认AI模型
     */
    @Operation(summary = "切换默认AI模型")
    @PutMapping("/default-provider")
    public Result<Void> switchProvider(@RequestParam String provider) {
        aiProviderSettingService.switchDefaultProvider(provider);
        return Result.ok();
    }

    @Operation(summary = "更新AI运行开关")
    @PutMapping("/runtime")
    public Result<Void> updateRuntime(@RequestBody(required = false) RuntimeUpdateRequest request) {
        aiProviderSettingService.updateRuntime(request != null ? request.enabled() : null);
        return Result.ok();
    }

    @Operation(summary = "更新AI模型配置")
    @PutMapping("/providers/{provider}")
    public Result<Void> updateProvider(@PathVariable String provider, @RequestBody(required = false) ProviderUpdateRequest request) {
        aiProviderSettingService.updateProvider(provider, new AiProviderSettingService.ProviderMutation(
            request != null ? request.name() : null,
            request != null ? request.apiUrl() : null,
            request != null ? request.model() : null,
            request != null ? request.enabled() : null,
            request != null ? request.apiKey() : null,
            request != null ? request.clearApiKey() : null,
            request != null ? request.temperature() : null,
            request != null ? request.maxTokens() : null
        ));
        return Result.ok();
    }

    private Map<String, Object> buildRuntimeStatus() {
        Map<String, Object> result = new LinkedHashMap<>();
        AiConfig.ProviderConfig provider = aiConfig.getProvider(aiConfig.getDefaultProvider());

        result.put("defaultProvider", aiConfig.getDefaultProvider());
        result.put("enabled", aiConfig.isEnabled());
        result.put("runtimeEnabled", aiConfig.isRuntimeEnabled());
        result.put("mode", aiConfig.getRuntimeMode());
        result.put("reason", aiConfig.getRuntimeReason());
        result.put("providerName", provider != null ? provider.getName() : aiConfig.getDefaultProvider());
        result.put("model", provider != null ? provider.getModel() : null);
        result.put("apiKeyConfigured", provider != null && provider.getApiKey() != null && !provider.getApiKey().isBlank());
        result.put("providerEnabled", provider != null && provider.isEnabled());
        return result;
    }
}
