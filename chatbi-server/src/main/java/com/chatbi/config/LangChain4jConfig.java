package com.chatbi.config;

import com.chatbi.service.AiModelService;
import com.chatbi.service.ChatbiLanguageModel;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LangChain4j 配置
 *
 * 将现有的多 Provider LLM 能力包装为 LangChain4j 的 ChatLanguageModel 接口，
 * 以便使用 PromptTemplate、ChatMemory、RAG 等高级能力。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class LangChain4jConfig {

    private final AiModelService aiModelService;
    private final AiConfig aiConfig;
    private final MeterRegistry meterRegistry;

    @Bean
    public ChatbiLanguageModel chatbiLanguageModel() {
        String defaultProvider = aiConfig.getDefaultProvider();
        log.info("LangChain4j ChatbiLanguageModel 初始化完成 - defaultProvider: {}", defaultProvider);
        return new ChatbiLanguageModel(aiModelService, defaultProvider, meterRegistry);
    }
}
