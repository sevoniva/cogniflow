package com.chatbi.health;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * AI Provider 健康检查指示器
 *
 * 暴露每个 LLM Provider 的 CircuitBreaker 状态到 /actuator/health。
 * 降级链健康度：Kimi（主）→ OpenAI（备）→ 通义千问（备）→ 语义引擎（兜底）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiProviderHealthIndicator implements HealthIndicator {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    private static final String[] MONITORED_PROVIDERS = {"kimi", "openai", "qianwen", "qwen"};

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();
        int upCount = 0;
        int downCount = 0;

        for (String provider : MONITORED_PROVIDERS) {
            try {
                CircuitBreaker cb = circuitBreakerRegistry.find(provider).orElse(null);
                if (cb == null) {
                    details.put(provider, Map.of("status", "UNKNOWN", "state", "NOT_CONFIGURED"));
                    continue;
                }
                CircuitBreaker.State state = cb.getState();
                Map<String, Object> providerDetail = new HashMap<>();
                providerDetail.put("state", state.name());
                providerDetail.put("failureRate", String.format("%.1f%%", cb.getMetrics().getFailureRate() * 100));
                providerDetail.put("slowCallRate", String.format("%.1f%%", cb.getMetrics().getSlowCallRate() * 100));
                providerDetail.put("numberOfCalls", cb.getMetrics().getNumberOfSuccessfulCalls() + cb.getMetrics().getNumberOfFailedCalls());

                if (state == CircuitBreaker.State.CLOSED) {
                    providerDetail.put("status", "UP");
                    upCount++;
                } else {
                    providerDetail.put("status", "DOWN");
                    downCount++;
                }
                details.put(provider, providerDetail);
            } catch (Exception e) {
                details.put(provider, Map.of("status", "UNKNOWN", "error", e.getMessage()));
            }
        }

        Health.Builder builder;
        if (downCount == 0) {
            builder = Health.up();
        } else if (upCount == 0) {
            builder = Health.down().withDetail("summary", "所有AI提供商熔断器均已开启");
        } else {
            builder = Health.status("DEGRADED").withDetail("summary",
                    String.format("%d UP, %d DOWN", upCount, downCount));
        }

        details.forEach(builder::withDetail);
        return builder.build();
    }
}
