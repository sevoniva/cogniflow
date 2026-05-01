package com.chatbi.service;

import com.chatbi.config.AiConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AI模型服务 - 支持多种国内大模型
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiModelService {

    private final AiConfig aiConfig;
    private final RestTemplate restTemplate;
    private final AiObservabilityService aiObservabilityService;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    /**
     * 调用AI模型生成SQL
     */
    public String generateSql(String prompt) {
        return generateText(prompt, aiConfig.getDefaultProvider());
    }

    public String generateSql(String prompt, String provider) {
        return generateText(prompt, provider);
    }

    public String generateText(String prompt) {
        return generateText(prompt, aiConfig.getDefaultProvider());
    }

    public String generateText(String prompt, String provider) {
        String requestedProvider = (provider == null || provider.isBlank()) ? aiConfig.getDefaultProvider() : provider;
        int promptChars = prompt == null ? 0 : prompt.length();
        List<String> providerSequence = buildProviderSequence(requestedProvider);
        RuntimeException lastException = null;

        for (int index = 0; index < providerSequence.size(); index++) {
            String providerName = providerSequence.get(index);
            boolean hasNextProvider = index < providerSequence.size() - 1;
            long providerStart = System.currentTimeMillis();

            // CircuitBreaker 状态检查（Month 2 Week 2）
            CircuitBreaker cb = getCircuitBreaker(providerName);
            if (cb.getState() == CircuitBreaker.State.OPEN) {
                log.warn("Provider CircuitBreaker OPEN，跳过 - provider: {}", providerName);
                if (!hasNextProvider) {
                    throw new RuntimeException("所有AI模型提供商熔断器均已开启，请稍后重试");
                }
                String nextProvider = providerSequence.get(index + 1);
                aiObservabilityService.recordProviderSwitch(providerName, nextProvider, "CIRCUIT_OPEN", "CircuitBreaker OPEN");
                continue;
            }

            AiConfig.ProviderConfig config;
            try {
                config = resolveProvider(providerName);
            } catch (RuntimeException ex) {
                long durationMs = System.currentTimeMillis() - providerStart;
                String category = classifyFailure(ex);
                aiObservabilityService.recordFailure(providerName, "generateText", durationMs, 1, promptChars, category, ex.getMessage());
                lastException = ex;
                if (!hasNextProvider || !shouldSwitchProvider(category)) {
                    throw ex;
                }
                String nextProvider = providerSequence.get(index + 1);
                log.warn("AI主提供商调用失败，切换备提供商 - from: {}, to: {}, category: {}, reason: {}",
                    providerName, nextProvider, category, ex.getMessage());
                aiObservabilityService.recordProviderSwitch(providerName, nextProvider, category, ex.getMessage());
                continue;
            }

            try {
                return invokeWithRetry(prompt, providerName, config, promptChars, cb);
            } catch (RuntimeException ex) {
                lastException = ex;
                String category = classifyFailure(ex);
                if (!hasNextProvider || !shouldSwitchProvider(category)) {
                    throw ex;
                }
                String nextProvider = providerSequence.get(index + 1);
                log.warn("AI主提供商调用失败，切换备提供商 - from: {}, to: {}, category: {}, reason: {}",
                    providerName, nextProvider, category, ex.getMessage());
                aiObservabilityService.recordProviderSwitch(providerName, nextProvider, category, ex.getMessage());
            }
        }

        throw lastException != null ? lastException : new RuntimeException("AI模型调用失败");
    }

    /**
     * 获取或创建 Provider 对应的 CircuitBreaker
     */
    private CircuitBreaker getCircuitBreaker(String providerName) {
        String cbName = providerName.toLowerCase();
        try {
            return circuitBreakerRegistry.circuitBreaker(cbName);
        } catch (Exception e) {
            // 如果配置中未定义该 provider 的 circuit breaker，fallback 到动态创建
            log.debug("Provider CircuitBreaker 未在配置中定义，动态创建 - name: {}", cbName);
            return circuitBreakerRegistry.circuitBreaker(cbName + "-dynamic");
        }
    }

    private String invokeWithRetry(String prompt, String providerName, AiConfig.ProviderConfig config, int promptChars, CircuitBreaker circuitBreaker) {
        RuntimeException lastException = null;
        int maxAttempts = Math.max(aiConfig.getMaxRetries(), 1);
        long start = System.currentTimeMillis();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                // 使用 CircuitBreaker 包裹 provider 调用
                String response = circuitBreaker.executeSupplier(() -> {
                    return switch (providerName.toLowerCase()) {
                        case "openai" -> callOpenAI(prompt, config);
                        case "kimi" -> callKimi(prompt, config);
                        case "bailian" -> callBailian(prompt, config);
                        case "minimax" -> callMiniMax(prompt, config);
                        case "qwen" -> callQwen(prompt, config);
                        case "qianwen" -> callQwen(prompt, config);
                        case "generic" -> callGenericApi(prompt, config);
                        default -> throw new RuntimeException("不支持的AI模型提供商: " + providerName);
                    };
                });
                long durationMs = System.currentTimeMillis() - start;
                aiObservabilityService.recordSuccess(providerName, "generateText", durationMs, attempt, promptChars);
                log.info("AI调用成功 - provider: {}, attempts: {}, durationMs: {}, promptChars: {}",
                    providerName, attempt, durationMs, promptChars);
                return response;
            } catch (io.github.resilience4j.circuitbreaker.CallNotPermittedException ex) {
                // CircuitBreaker OPEN，直接抛出让上层切换 provider
                throw new RuntimeException("Provider CircuitBreaker OPEN: " + providerName, ex);
            } catch (RuntimeException ex) {
                lastException = ex;
                if (!shouldRetry(ex) || attempt == maxAttempts) {
                    long durationMs = System.currentTimeMillis() - start;
                    String category = classifyFailure(ex);
                    aiObservabilityService.recordFailure(providerName, "generateText", durationMs, attempt, promptChars, category, ex.getMessage());
                    log.warn("AI调用失败 - provider: {}, attempts: {}, durationMs: {}, category: {}, error: {}",
                        providerName, attempt, durationMs, category, ex.getMessage());
                    throw ex;
                }

                long backoffMillis = Math.min(800L * attempt, 2400L);
                log.warn("AI模型调用失败，准备重试 - provider: {}, attempt: {}/{}, error: {}",
                    providerName, attempt, maxAttempts, ex.getMessage());
                try {
                    Thread.sleep(backoffMillis);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw ex;
                }
            }
        }

        RuntimeException finalError = lastException != null ? lastException : new RuntimeException("AI模型调用失败");
        long durationMs = System.currentTimeMillis() - start;
        String category = classifyFailure(finalError);
        aiObservabilityService.recordFailure(providerName, "generateText", durationMs, maxAttempts, promptChars, category, finalError.getMessage());
        throw finalError;
    }

    private List<String> buildProviderSequence(String requestedProvider) {
        Set<String> providers = new LinkedHashSet<>();
        providers.add(requestedProvider);
        providers.addAll(listReadyBackupProviders(requestedProvider));
        return new ArrayList<>(providers);
    }

    private List<String> listReadyBackupProviders(String requestedProvider) {
        List<String> backups = new ArrayList<>();
        Map<String, AiConfig.ProviderConfig> providerConfigs = aiConfig.getProviders();
        if (providerConfigs == null || providerConfigs.isEmpty()) {
            return backups;
        }
        providerConfigs.entrySet().stream()
            .filter(entry -> !entry.getKey().equalsIgnoreCase(requestedProvider))
            .filter(entry -> entry.getValue() != null && entry.getValue().isEnabled())
            .map(Map.Entry::getKey)
            .sorted()
            .forEach(backups::add);
        return backups;
    }

    private boolean shouldSwitchProvider(String category) {
        if (!StringUtils.hasText(category)) {
            return false;
        }
        String normalized = category.toUpperCase();
        return "RATE_LIMIT".equals(normalized)
            || "TIMEOUT".equals(normalized)
            || "NETWORK".equals(normalized)
            || "CONFIG".equals(normalized)
            || "UNKNOWN".equals(normalized);
    }

    private boolean shouldRetry(RuntimeException exception) {
        String message = exception.getMessage();
        if (!StringUtils.hasText(message)) {
            return false;
        }

        String normalized = message.toLowerCase();
        return normalized.contains("429")
            || normalized.contains("overloaded")
            || normalized.contains("timed out")
            || normalized.contains("timeout")
            || normalized.contains("connection reset")
            || normalized.contains("connection refused")
            || normalized.contains("502")
            || normalized.contains("503")
            || normalized.contains("504");
    }

    private String classifyFailure(RuntimeException exception) {
        String message = exception != null ? exception.getMessage() : null;
        if (!StringUtils.hasText(message)) {
            return "UNKNOWN";
        }
        String normalized = message.toLowerCase();
        if (normalized.contains("未启用")
            || normalized.contains("未配置")
            || normalized.contains("不存在")
            || normalized.contains("不支持的ai模型提供商")) {
            return "CONFIG";
        }
        if (normalized.contains("api key") || normalized.contains("unauthorized") || normalized.contains("401") || normalized.contains("403")) {
            return "AUTH";
        }
        if (normalized.contains("rate") || normalized.contains("429") || normalized.contains("overloaded")) {
            return "RATE_LIMIT";
        }
        if (normalized.contains("timeout") || normalized.contains("timed out")) {
            return "TIMEOUT";
        }
        if (normalized.contains("connection reset")
            || normalized.contains("connection refused")
            || normalized.contains("network")
            || normalized.contains("502")
            || normalized.contains("503")
            || normalized.contains("504")
            || normalized.contains("i/o error")) {
            return "NETWORK";
        }
        if (normalized.contains("400") || normalized.contains("invalid")) {
            return "BAD_REQUEST";
        }
        return "UNKNOWN";
    }

    /**
     * 调用OpenAI API
     */
    private String callOpenAI(String prompt, AiConfig.ProviderConfig config) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(config.getApiKey());

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", config.getModel());
            requestBody.put("messages", List.of(
                Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("temperature", config.getTemperature());
            requestBody.put("max_tokens", config.getMaxTokens());

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                config.getApiUrl() + "/chat/completions",
                request,
                Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }
            }

            throw new RuntimeException("OpenAI API调用失败");
        } catch (Exception e) {
            log.error("调用OpenAI API失败", e);
            throw new RuntimeException("OpenAI API调用失败: " + e.getMessage());
        }
    }

    /**
     * 调用Kimi API (Moonshot)
     */
    private String callKimi(String prompt, AiConfig.ProviderConfig config) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(config.getApiKey());

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", config.getModel() != null ? config.getModel() : "moonshot-v1-8k");
            requestBody.put("messages", List.of(
                Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("temperature", config.getTemperature());

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            String apiUrl = config.getApiUrl() != null ? config.getApiUrl() : "https://api.moonshot.cn/v1";

            ResponseEntity<Map> response = restTemplate.postForEntity(
                apiUrl + "/chat/completions",
                request,
                Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }
            }

            throw new RuntimeException("Kimi API调用失败");
        } catch (Exception e) {
            log.error("调用Kimi API失败", e);
            throw new RuntimeException("Kimi API调用失败: " + e.getMessage());
        }
    }

    /**
     * 调用阿里云百炼 API
     */
    private String callBailian(String prompt, AiConfig.ProviderConfig config) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + config.getApiKey());

            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> input = new HashMap<>();
            input.put("messages", List.of(
                Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("model", config.getModel() != null ? config.getModel() : "qwen-max");
            requestBody.put("input", input);

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("result_format", "message");
            requestBody.put("parameters", parameters);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            String apiUrl = config.getApiUrl() != null ? config.getApiUrl() :
                "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";

            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                Map<String, Object> output = (Map<String, Object>) body.get("output");
                if (output != null) {
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) output.get("choices");
                    if (choices != null && !choices.isEmpty()) {
                        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                        return (String) message.get("content");
                    }
                }
            }

            throw new RuntimeException("百炼API调用失败");
        } catch (Exception e) {
            log.error("调用百炼API失败", e);
            throw new RuntimeException("百炼API调用失败: " + e.getMessage());
        }
    }

    /**
     * 调用MiniMax API
     */
    private String callMiniMax(String prompt, AiConfig.ProviderConfig config) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + config.getApiKey());

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", config.getModel() != null ? config.getModel() : "abab5.5-chat");
            requestBody.put("messages", List.of(
                Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("temperature", config.getTemperature());
            requestBody.put("tokens_to_generate", config.getMaxTokens());

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            String apiUrl = config.getApiUrl() != null ? config.getApiUrl() :
                "https://api.minimax.chat/v1/text/chatcompletion_v2";

            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    List<Map<String, Object>> messages = (List<Map<String, Object>>) choices.get(0).get("messages");
                    if (messages != null && !messages.isEmpty()) {
                        return (String) messages.get(0).get("text");
                    }
                }
            }

            throw new RuntimeException("MiniMax API调用失败");
        } catch (Exception e) {
            log.error("调用MiniMax API失败", e);
            throw new RuntimeException("MiniMax API调用失败: " + e.getMessage());
        }
    }

    /**
     * 调用通义千问 API
     */
    private String callQwen(String prompt, AiConfig.ProviderConfig config) {
        // 通义千问使用百炼平台，调用方式相同
        return callBailian(prompt, config);
    }

    /**
     * 调用通用API（兼容OpenAI格式）
     */
    private String callGenericApi(String prompt, AiConfig.ProviderConfig config) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(config.getApiKey());

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", config.getModel());
            requestBody.put("messages", List.of(
                Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("temperature", config.getTemperature());
            requestBody.put("max_tokens", config.getMaxTokens());

            // 添加额外参数
            if (config.getExtraParams() != null) {
                requestBody.putAll(config.getExtraParams());
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                config.getApiUrl(),
                request,
                Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }
            }

            throw new RuntimeException("通用API调用失败");
        } catch (Exception e) {
            log.error("调用通用API失败", e);
            throw new RuntimeException("通用API调用失败: " + e.getMessage());
        }
    }

    /**
     * 测试AI模型连接
     */
    public boolean testConnection(String provider) {
        try {
            AiConfig.ProviderConfig config = resolveProvider(provider);
            if (!StringUtils.hasText(config.getApiKey())) {
                return false;
            }

            String result = generateSql("请仅返回 OK。", provider);
            return result != null && !result.isEmpty();
        } catch (Exception e) {
            log.error("测试AI模型连接失败: {}", provider, e);
            return false;
        }
    }

    private AiConfig.ProviderConfig resolveProvider(String provider) {
        if (!aiConfig.isEnabled()) {
            throw new RuntimeException("AI功能未启用");
        }

        AiConfig.ProviderConfig config = aiConfig.getProvider(provider);
        if (config == null) {
            throw new RuntimeException("AI模型提供商不存在: " + provider);
        }
        if (!config.isEnabled()) {
            throw new RuntimeException("AI模型提供商未启用: " + provider);
        }
        if (!StringUtils.hasText(config.getApiUrl())) {
            throw new RuntimeException("AI模型提供商未配置 API 地址: " + provider);
        }
        if (!StringUtils.hasText(config.getApiKey())) {
            throw new RuntimeException("AI模型提供商未配置 API Key: " + provider);
        }
        return config;
    }
}
