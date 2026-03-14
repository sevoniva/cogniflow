package com.chatbi.service;

import com.chatbi.config.AiObservabilityNotifyProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AI 可观测性告警通知服务。
 */
@Slf4j
@Service
public class AiObservabilityNotificationService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MAX_FINGERPRINT_CACHE_SIZE = 5_000;

    private final EmailPushService emailPushService;
    private final DingTalkPushService dingTalkPushService;
    private final WeChatPushService weChatPushService;
    private final AiObservabilityNotifyProperties properties;
    private final Clock clock;
    private final ConcurrentMap<String, Long> lastNotifyAtByFingerprint = new ConcurrentHashMap<>();

    @Autowired
    public AiObservabilityNotificationService(EmailPushService emailPushService,
                                              DingTalkPushService dingTalkPushService,
                                              WeChatPushService weChatPushService,
                                              AiObservabilityNotifyProperties properties) {
        this(emailPushService, dingTalkPushService, weChatPushService, properties, Clock.systemDefaultZone());
    }

    AiObservabilityNotificationService(EmailPushService emailPushService,
                                       DingTalkPushService dingTalkPushService,
                                       WeChatPushService weChatPushService,
                                       AiObservabilityNotifyProperties properties,
                                       Clock clock) {
        this.emailPushService = emailPushService;
        this.dingTalkPushService = dingTalkPushService;
        this.weChatPushService = weChatPushService;
        this.properties = properties;
        this.clock = clock;
    }

    public void notifyEvent(String action, String status, int score, List<Map<String, Object>> alerts, String summary) {
        if (!properties.isEnabled()) {
            return;
        }
        Set<String> channels = resolveChannels(properties.getChannels());
        if (channels.isEmpty()) {
            return;
        }

        String subject = buildSubject(action, status, score);
        String content = buildContent(action, status, score, alerts, summary);
        if (shouldThrottle(action, status, summary, alerts)) {
            return;
        }

        if (channels.contains("EMAIL") && StringUtils.hasText(properties.getEmailTo())) {
            String[] recipients = properties.getEmailTo().split(",");
            for (String recipient : recipients) {
                String to = recipient == null ? "" : recipient.trim();
                if (!to.isEmpty()) {
                    safeSend(() -> emailPushService.push(to, subject, content), "EMAIL", to);
                }
            }
        }

        if (channels.contains("DINGTALK") && StringUtils.hasText(properties.getDingtalkWebhook())) {
            safeSend(() -> dingTalkPushService.push(properties.getDingtalkWebhook(), subject, content), "DINGTALK", "webhook");
        }

        if (channels.contains("WECHAT") && StringUtils.hasText(properties.getWechatWebhook())) {
            safeSend(() -> weChatPushService.push(properties.getWechatWebhook(), subject, content), "WECHAT", "webhook");
        }
    }

    private Set<String> resolveChannels(String channelText) {
        Set<String> channels = new LinkedHashSet<>();
        if (!StringUtils.hasText(channelText)) {
            return channels;
        }
        String[] tokens = channelText.split(",");
        for (String token : tokens) {
            if (token != null && !token.isBlank()) {
                channels.add(token.trim().toUpperCase(Locale.ROOT));
            }
        }
        return channels;
    }

    private String buildSubject(String action, String status, int score) {
        String prefix = StringUtils.hasText(properties.getSubjectPrefix()) ? properties.getSubjectPrefix().trim() : "[ChatBI AI告警]";
        String event = "AI_OBSERVABILITY_RECOVERY".equals(action) ? "恢复" : "告警";
        return prefix + " " + event + " status=" + status + " score=" + score;
    }

    private String buildContent(String action, String status, int score, List<Map<String, Object>> alerts, String summary) {
        StringBuilder builder = new StringBuilder();
        builder.append("事件: ").append(action).append('\n');
        builder.append("状态: ").append(status).append('\n');
        builder.append("评分: ").append(score).append('\n');
        builder.append("时间: ").append(LocalDateTime.now(clock).format(TIME_FORMATTER)).append('\n');
        builder.append("摘要: ").append(summary == null ? "" : summary).append('\n');
        if (alerts != null && !alerts.isEmpty()) {
            builder.append("明细:\n");
            alerts.stream().limit(5).forEach(alert -> builder
                .append("- ")
                .append(alert.getOrDefault("level", "unknown"))
                .append(" | ")
                .append(alert.getOrDefault("code", ""))
                .append(" | ")
                .append(alert.getOrDefault("title", ""))
                .append('\n'));
        }
        return builder.toString();
    }

    private boolean shouldThrottle(String action, String status, String summary, List<Map<String, Object>> alerts) {
        int minIntervalSeconds = Math.max(properties.getNotifyMinIntervalSeconds(), 0);
        if (minIntervalSeconds <= 0) {
            return false;
        }
        if (lastNotifyAtByFingerprint.size() > MAX_FINGERPRINT_CACHE_SIZE) {
            lastNotifyAtByFingerprint.clear();
        }
        long now = clock.millis();
        long minIntervalMillis = minIntervalSeconds * 1000L;
        String fingerprint = buildFingerprint(action, status, summary, alerts);
        AtomicBoolean throttled = new AtomicBoolean(false);
        lastNotifyAtByFingerprint.compute(fingerprint, (key, lastAt) -> {
            if (lastAt != null && now - lastAt < minIntervalMillis) {
                throttled.set(true);
                return lastAt;
            }
            return now;
        });
        if (throttled.get()) {
            log.info("AI 可观测性通知已限频跳过 - fingerprint: {}, minIntervalSeconds: {}", fingerprint, minIntervalSeconds);
            return true;
        }
        return false;
    }

    private String buildFingerprint(String action, String status, String summary, List<Map<String, Object>> alerts) {
        String safeAction = action == null ? "" : action.trim();
        String safeStatus = status == null ? "" : status.trim();
        String safeSummary = summary == null ? "" : summary.trim();
        String alertFingerprint = "";
        if (alerts != null && !alerts.isEmpty()) {
            alertFingerprint = alerts.stream()
                .limit(5)
                .map(alert -> (alert.getOrDefault("code", "") + ":" + alert.getOrDefault("level", "")).toString())
                .sorted()
                .reduce((left, right) -> left + "|" + right)
                .orElse("");
        }
        return String.join("::", Arrays.asList(safeAction, safeStatus, safeSummary, alertFingerprint));
    }

    private void safeSend(Runnable sendAction, String channel, String target) {
        try {
            sendAction.run();
        } catch (Exception ex) {
            log.error("AI 可观测性通知发送失败 - channel: {}, target: {}, reason: {}", channel, target, ex.getMessage());
        }
    }
}
