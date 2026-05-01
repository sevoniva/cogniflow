package com.chatbi.config;

import com.chatbi.service.ChatbiLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 向量嵌入模型配置
 *
 * 使用 OpenAI text-embedding-ada-002 生成查询/文档的向量表示，
 * 为 NL2SQL RAG 提供语义检索能力。
 */
@Slf4j
@Configuration
public class EmbeddingConfig {

    @Bean
    public EmbeddingModel embeddingModel(AiConfig aiConfig) {
        AiConfig.ProviderConfig provider = aiConfig.getProvider("openai");
        String apiKey = provider != null ? provider.getApiKey() : null;
        String baseUrl = provider != null ? provider.getApiUrl() : null;

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("OpenAI API Key 未配置，EmbeddingModel 将使用默认配置");
            apiKey = "demo";
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.openai.com/v1";
        }

        log.info("初始化 EmbeddingModel - baseUrl: {}", baseUrl);

        return OpenAiEmbeddingModel.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .modelName("text-embedding-ada-002")
            .build();
    }
}
