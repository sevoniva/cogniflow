package com.chatbi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 可观测性阈值配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "chatbi.ai.observability.thresholds")
public class AiObservabilityThresholdProperties {

    /**
     * 健康评估窗口（分钟）。
     */
    private int windowMinutes = 10;

    /**
     * 触发失败率告警的最小样本量。
     */
    private int minSampleSizeForAlert = 5;

    /**
     * 失败率预警阈值（0-1）。
     */
    private double failureRateWarning = 0.20;

    /**
     * 失败率严重阈值（0-1）。
     */
    private double failureRateCritical = 0.40;

    /**
     * 平均延迟预警阈值（毫秒）。
     */
    private long latencyWarningMs = 3000L;

    /**
     * 平均延迟严重阈值（毫秒）。
     */
    private long latencyCriticalMs = 6000L;

    /**
     * 连续失败预警阈值。
     */
    private int consecutiveFailureWarning = 3;

    /**
     * 连续失败严重阈值。
     */
    private int consecutiveFailureCritical = 6;

    /**
     * 分类失败峰值预警阈值。
     */
    private int categorySpikeWarning = 2;

    /**
     * 分类失败峰值严重阈值。
     */
    private int categorySpikeCritical = 4;

    /**
     * 最近调用记录最大保留条数。
     */
    private int recentCallLimit = 50;

    /**
     * 最近主备切换事件最大保留条数。
     */
    private int switchEventLimit = 30;
}
