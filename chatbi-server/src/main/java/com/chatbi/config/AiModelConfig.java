package com.chatbi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * AI模型配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "chatbi.ai")
public class AiModelConfig {

    /**
     * 默认使用的模型提供商
     */
    private String defaultProvider = "openai";

    /**
     * 是否启用AI功能
     */
    private boolean enabled = true;

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

    /**
     * 模型提供商配置
     */
    @Data
    public static class ProviderConfig {
        /**
         * 提供商名称
         */
        private String name;

        /**
         * API地址
         */
        private String apiUrl;

        /**
         * API密钥
         */
        private String apiKey;

        /**
         * 模型名称
         */
        private String model;

        /**
         * 温度参数
         */
        private Double temperature = 0.7;

        /**
         * 最大Token数
         */
        private Integer maxTokens = 2000;

        /**
         * 是否启用
         */
        private boolean enabled = true;

        /**
         * 额外参数
         */
        private Map<String, Object> extraParams = new HashMap<>();
    }
}
