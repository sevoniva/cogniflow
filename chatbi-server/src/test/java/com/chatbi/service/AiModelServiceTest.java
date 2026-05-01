package com.chatbi.service;

import com.chatbi.config.AiConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiModelService 主备切换测试")
class AiModelServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private AiObservabilityService aiObservabilityService;

    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void setUp() {
        circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
    }

    @Test
    @DisplayName("主提供商限流时应自动切换到备提供商")
    void shouldSwitchToBackupProviderOnRateLimit() {
        AiConfig aiConfig = buildConfig(
            "kimi",
            provider("Kimi", "https://primary.test", "k-primary", true, "moonshot-v1-8k"),
            provider("OpenAI", "https://backup.test", "k-backup", true, "gpt-4o-mini")
        );
        AiModelService service = new AiModelService(aiConfig, restTemplate, aiObservabilityService, circuitBreakerRegistry);

        when(restTemplate.postForEntity(eq("https://primary.test/chat/completions"), any(HttpEntity.class), eq(Map.class)))
            .thenThrow(new RuntimeException("429 rate limit"));
        when(restTemplate.postForEntity(eq("https://backup.test/chat/completions"), any(HttpEntity.class), eq(Map.class)))
            .thenReturn(ResponseEntity.ok(openAiResponse("backup-ok")));

        String answer = service.generateText("请总结本周销售趋势", "kimi");

        assertEquals("backup-ok", answer);
        verify(aiObservabilityService).recordFailure(
            eq("kimi"),
            eq("generateText"),
            anyLong(),
            eq(1),
            anyInt(),
            eq("RATE_LIMIT"),
            contains("429")
        );
        verify(aiObservabilityService).recordSuccess(
            eq("openai"),
            eq("generateText"),
            anyLong(),
            eq(1),
            anyInt()
        );
        verify(aiObservabilityService).recordProviderSwitch(
            eq("kimi"),
            eq("openai"),
            eq("RATE_LIMIT"),
            contains("429")
        );
    }

    @Test
    @DisplayName("鉴权失败应立即失败且不切换备提供商")
    void shouldNotSwitchWhenAuthFailure() {
        AiConfig aiConfig = buildConfig(
            "openai",
            provider("Kimi", "https://backup-kimi.test", "k-kimi", true, "moonshot-v1-8k"),
            provider("OpenAI", "https://primary-openai.test", "k-openai", true, "gpt-4o-mini")
        );
        AiModelService service = new AiModelService(aiConfig, restTemplate, aiObservabilityService, circuitBreakerRegistry);

        when(restTemplate.postForEntity(eq("https://primary-openai.test/chat/completions"), any(HttpEntity.class), eq(Map.class)))
            .thenThrow(new RuntimeException("401 unauthorized"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.generateText("测试鉴权失败", "openai"));

        assertTrue(ex.getMessage().contains("401"));
        verify(restTemplate, never()).postForEntity(eq("https://backup-kimi.test/chat/completions"), any(HttpEntity.class), eq(Map.class));
        verify(aiObservabilityService).recordFailure(
            eq("openai"),
            eq("generateText"),
            anyLong(),
            eq(1),
            anyInt(),
            eq("AUTH"),
            contains("401")
        );
        verify(aiObservabilityService, never()).recordProviderSwitch(
            eq("openai"),
            eq("kimi"),
            eq("AUTH"),
            contains("401")
        );
    }

    @Test
    @DisplayName("主提供商配置异常时应切换到可用备提供商")
    void shouldSwitchWhenPrimaryConfigInvalid() {
        AiConfig aiConfig = buildConfig(
            "kimi",
            provider("Kimi", "https://primary.test", "", true, "moonshot-v1-8k"),
            provider("OpenAI", "https://backup.test", "k-backup", true, "gpt-4o-mini")
        );
        AiModelService service = new AiModelService(aiConfig, restTemplate, aiObservabilityService, circuitBreakerRegistry);

        when(restTemplate.postForEntity(eq("https://backup.test/chat/completions"), any(HttpEntity.class), eq(Map.class)))
            .thenReturn(ResponseEntity.ok(openAiResponse("backup-from-config")));

        String answer = service.generateText("请输出OK", "kimi");

        assertEquals("backup-from-config", answer);
        verify(aiObservabilityService).recordFailure(
            eq("kimi"),
            eq("generateText"),
            anyLong(),
            eq(1),
            anyInt(),
            eq("CONFIG"),
            contains("API Key")
        );
        verify(aiObservabilityService).recordSuccess(
            eq("openai"),
            eq("generateText"),
            anyLong(),
            eq(1),
            anyInt()
        );
        verify(aiObservabilityService).recordProviderSwitch(
            eq("kimi"),
            eq("openai"),
            eq("CONFIG"),
            contains("API Key")
        );
    }

    private AiConfig buildConfig(String defaultProvider, AiConfig.ProviderConfig kimi, AiConfig.ProviderConfig openai) {
        AiConfig config = new AiConfig();
        config.setEnabled(true);
        config.setDefaultProvider(defaultProvider);
        config.setMaxRetries(1);
        Map<String, AiConfig.ProviderConfig> providers = new LinkedHashMap<>();
        providers.put("kimi", kimi);
        providers.put("openai", openai);
        config.setProviders(providers);
        return config;
    }

    private AiConfig.ProviderConfig provider(String name, String apiUrl, String apiKey, boolean enabled, String model) {
        AiConfig.ProviderConfig config = new AiConfig.ProviderConfig();
        config.setName(name);
        config.setApiUrl(apiUrl);
        config.setApiKey(apiKey);
        config.setEnabled(enabled);
        config.setModel(model);
        return config;
    }

    private Map<String, Object> openAiResponse(String content) {
        return Map.of(
            "choices", List.of(
                Map.of(
                    "message", Map.of("content", content)
                )
            )
        );
    }
}
