package com.chatbi.service;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
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

        String response = aiModelService.generateText(finalPrompt, defaultProvider);
        return Response.from(AiMessage.from(response));
    }

    /**
     * 生成文本（便捷方法）
     */
    public String generate(String prompt) {
        return aiModelService.generateText(prompt, defaultProvider);
    }

    /**
     * 生成文本（指定 provider）
     */
    public String generate(String prompt, String provider) {
        return aiModelService.generateText(prompt, provider);
    }
}
