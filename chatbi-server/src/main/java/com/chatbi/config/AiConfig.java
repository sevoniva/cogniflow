package com.chatbi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * AI配置类
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "chatbi.ai")
public class AiConfig {

    /**
     * 是否启用AI功能
     */
    private boolean enabled = false;

    /**
     * 默认使用的模型提供商
     */
    private String defaultProvider = "kimi";

    /**
     * 请求超时时间（秒）
     */
    private int timeout = 30;

    /**
     * 最大重试次数
     */
    private int maxRetries = 3;

    /**
     * 模型提供商配置
     */
    private Map<String, ProviderConfig> providers = new HashMap<>();

    @Data
    public static class ProviderConfig {
        private String name;
        private String apiUrl;
        private String apiKey;
        private String model;
        private double temperature = 0.7;
        private int maxTokens = 2000;
        private boolean enabled = false;
        private Map<String, Object> extraParams = new HashMap<>();
    }

    /**
     * 获取指定提供商配置
     */
    public ProviderConfig getProvider(String provider) {
        if (providers == null || provider == null) {
            return null;
        }
        return providers.get(provider);
    }

    /**
     * 当前默认提供商是否已达到可调用状态
     */
    public boolean isRuntimeEnabled() {
        if (!enabled) {
            return false;
        }

        ProviderConfig config = getProvider(defaultProvider);
        return config != null
            && config.isEnabled()
            && StringUtils.hasText(config.getApiUrl())
            && StringUtils.hasText(config.getApiKey());
    }

    public String getRuntimeMode() {
        return isRuntimeEnabled() ? "llm" : "semantic";
    }

    public String getRuntimeReason() {
        if (!enabled) {
            return "未启用外部大模型，系统当前使用业务语义引擎";
        }

        ProviderConfig config = getProvider(defaultProvider);
        if (config == null) {
            return "未找到默认 AI 提供商配置";
        }
        if (!config.isEnabled()) {
            return "默认 AI 提供商未启用";
        }
        if (!StringUtils.hasText(config.getApiKey())) {
            return "默认 AI 提供商缺少 API Key";
        }

        return "外部大模型已就绪";
    }

    /**
     * 获取当前启用的提供商配置
     */
    public ProviderConfig getCurrentProvider() {
        if (!isRuntimeEnabled()) {
            throw new RuntimeException("AI运行未就绪: " + getRuntimeReason());
        }

        ProviderConfig config = getProvider(defaultProvider);
        return config;
    }
}
