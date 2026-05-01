package com.chatbi.service;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Chatbi 语言模型适配器
 *
 * 将现有的多 Provider LLM 调用能力包装为 LangChain4j 的 ChatLanguageModel 接口。
 * 这样可以在保留现有 provider 切换、重试、监控能力的同时，
 * 获得 LangChain4j 的 PromptTemplate、ChatMemory、RAG 等高级能力。
 */
@Slf4j
@RequiredArgsConstructor
public class ChatbiLanguageModel implements ChatLanguageModel {

    private final AiModelService aiModelService;
    private final String defaultProvider;
    private final MeterRegistry meterRegistry;

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages) {
        // 将 LangChain4j 的 ChatMessage 列表转换为单一 prompt 字符串
        StringBuilder promptBuilder = new StringBuilder();
        String systemPrompt = null;

        for (ChatMessage message : messages) {
            if (message instanceof SystemMessage) {
                systemPrompt = ((SystemMessage) message).text();
            } else if (message instanceof UserMessage) {
                if (promptBuilder.length() > 0) {
                    promptBuilder.append("\n\n");
                }
                promptBuilder.append(((UserMessage) message).singleText());
            } else if (message instanceof AiMessage) {
                if (promptBuilder.length() > 0) {
                    promptBuilder.append("\n\n");
                }
                promptBuilder.append("Assistant: ").append(((AiMessage) message).text());
            }
        }

        // 如果有系统消息，放在最前面
        String finalPrompt;
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            finalPrompt = systemPrompt + "\n\n" + promptBuilder;
        } else {
            finalPrompt = promptBuilder.toString();
        }

        log.debug("LangChain4j generate - prompt length: {}, provider: {}", finalPrompt.length(), defaultProvider);

        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            String response = aiModelService.generateText(finalPrompt, defaultProvider);
            sample.stop(Timer.builder("ai.model.duration")
                    .tag("provider", defaultProvider)
                    .tag("status", "success")
                    .register(meterRegistry));
            meterRegistry.counter("ai.model.calls",
                    "provider", defaultProvider, "status", "success").increment();
            return Response.from(AiMessage.from(response));
        } catch (Exception e) {
            sample.stop(Timer.builder("ai.model.duration")
                    .tag("provider", defaultProvider)
                    .tag("status", "error")
                    .tag("error", e.getClass().getSimpleName())
                    .register(meterRegistry));
            meterRegistry.counter("ai.model.calls",
                    "provider", defaultProvider, "status", "error").increment();
            throw e;
        }
    }

    /**
     * 生成文本（便捷方法）
     *
     * 带熔断和重试保护：连续失败自动开启熔断，fallback 到规则引擎。
     */
    @CircuitBreaker(name = "aiGeneration", fallbackMethod = "generateFallback")
    @Retry(name = "aiGeneration")
    public String generate(String prompt) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            String result = aiModelService.generateText(prompt, defaultProvider);
            sample.stop(Timer.builder("ai.model.duration")
                    .tag("provider", defaultProvider)
                    .tag("status", "success")
                    .register(meterRegistry));
            meterRegistry.counter("ai.model.calls",
                    "provider", defaultProvider, "status", "success").increment();
            return result;
        } catch (Exception e) {
            sample.stop(Timer.builder("ai.model.duration")
                    .tag("provider", defaultProvider)
                    .tag("status", "error")
                    .tag("error", e.getClass().getSimpleName())
                    .register(meterRegistry));
            meterRegistry.counter("ai.model.calls",
                    "provider", defaultProvider, "status", "error").increment();
            throw e;
        }
    }

    /**
     * 生成文本（指定 provider）
     */
    @CircuitBreaker(name = "aiGeneration", fallbackMethod = "generateFallback")
    @Retry(name = "aiGeneration")
    public String generate(String prompt, String provider) {
        String effectiveProvider = provider != null ? provider : defaultProvider;
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            String result = aiModelService.generateText(prompt, provider);
            sample.stop(Timer.builder("ai.model.duration")
                    .tag("provider", effectiveProvider)
                    .tag("status", "success")
                    .register(meterRegistry));
            meterRegistry.counter("ai.model.calls",
                    "provider", effectiveProvider, "status", "success").increment();
            return result;
        } catch (Exception e) {
            sample.stop(Timer.builder("ai.model.duration")
                    .tag("provider", effectiveProvider)
                    .tag("status", "error")
                    .tag("error", e.getClass().getSimpleName())
                    .register(meterRegistry));
            meterRegistry.counter("ai.model.calls",
                    "provider", effectiveProvider, "status", "error").increment();
            throw e;
        }
    }

    /**
     * 熔断/降级 fallback：返回语义引擎提示，引导用户缩小问题范围
     */
    private String generateFallback(String prompt, Exception ex) {
        log.warn("AI 调用熔断/降级 - provider: {}, 原因: {}", defaultProvider, ex.getMessage());
        return "系统当前繁忙，已切换至语义引擎模式。\n"
            + "请尝试用更具体的业务术语描述您的问题，例如：\n"
            + "- 本月华东区销售额总和\n"
            + "- 按产品类别统计订单数量\n"
            + "- 最近7天新增客户数";
    }

    private String generateFallback(String prompt, String provider, Exception ex) {
        log.warn("AI 调用熔断/降级 - provider: {}, 原因: {}", provider, ex.getMessage());
        return generateFallback(prompt, ex);
    }
}
