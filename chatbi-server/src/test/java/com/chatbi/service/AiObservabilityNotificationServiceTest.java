package com.chatbi.service;

import com.chatbi.config.AiObservabilityNotifyProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AiObservabilityNotificationServiceTest {

    @Mock
    private EmailPushService emailPushService;
    @Mock
    private DingTalkPushService dingTalkPushService;
    @Mock
    private WeChatPushService weChatPushService;

    @Test
    void shouldSkipWhenDisabled() {
        AiObservabilityNotifyProperties properties = new AiObservabilityNotifyProperties();
        properties.setEnabled(false);
        AiObservabilityNotificationService service = new AiObservabilityNotificationService(
            emailPushService, dingTalkPushService, weChatPushService, properties
        );

        service.notifyEvent("AI_OBSERVABILITY_ALERT", "critical", 30, List.of(), "failure");

        verify(emailPushService, never()).push(any(), any(), any());
        verify(dingTalkPushService, never()).push(any(), any(), any());
        verify(weChatPushService, never()).push(any(), any(), any());
    }

    @Test
    void shouldPushToConfiguredChannels() {
        AiObservabilityNotifyProperties properties = new AiObservabilityNotifyProperties();
        properties.setEnabled(true);
        properties.setChannels("EMAIL,DINGTALK,WECHAT");
        properties.setEmailTo("a@chatbi.com,b@chatbi.com");
        properties.setDingtalkWebhook("https://oapi.dingtalk.com/robot/send?access_token=test");
        properties.setWechatWebhook("https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=test");
        AiObservabilityNotificationService service = new AiObservabilityNotificationService(
            emailPushService, dingTalkPushService, weChatPushService, properties
        );

        service.notifyEvent(
            "AI_OBSERVABILITY_ALERT",
            "critical",
            20,
            List.of(Map.of("code", "failure-rate", "level", "critical", "title", "失败率过高")),
            "failure-rate"
        );

        verify(emailPushService).push(eq("a@chatbi.com"), contains("status=critical"), any());
        verify(emailPushService).push(eq("b@chatbi.com"), contains("status=critical"), any());
        verify(dingTalkPushService).push(eq(properties.getDingtalkWebhook()), contains("status=critical"), any());
        verify(weChatPushService).push(eq(properties.getWechatWebhook()), contains("status=critical"), any());
    }

    @Test
    void shouldThrottleDuplicateEventsWithinInterval() {
        AiObservabilityNotifyProperties properties = new AiObservabilityNotifyProperties();
        properties.setEnabled(true);
        properties.setChannels("EMAIL");
        properties.setEmailTo("ops@chatbi.com");
        properties.setNotifyMinIntervalSeconds(300);
        MutableClock clock = new MutableClock(Instant.parse("2026-03-12T00:00:00Z"), ZoneId.of("Asia/Shanghai"));
        AiObservabilityNotificationService service = new AiObservabilityNotificationService(
            emailPushService, dingTalkPushService, weChatPushService, properties, clock
        );

        service.notifyEvent(
            "AI_OBSERVABILITY_ALERT",
            "critical",
            20,
            List.of(Map.of("code", "failure-rate", "level", "critical", "title", "失败率过高")),
            "failure-rate"
        );
        service.notifyEvent(
            "AI_OBSERVABILITY_ALERT",
            "critical",
            20,
            List.of(Map.of("code", "failure-rate", "level", "critical", "title", "失败率过高")),
            "failure-rate"
        );
        verify(emailPushService, times(1)).push(eq("ops@chatbi.com"), contains("status=critical"), any());

        clock.plusSeconds(301);
        service.notifyEvent(
            "AI_OBSERVABILITY_ALERT",
            "critical",
            20,
            List.of(Map.of("code", "failure-rate", "level", "critical", "title", "失败率过高")),
            "failure-rate"
        );
        verify(emailPushService, times(2)).push(eq("ops@chatbi.com"), contains("status=critical"), any());
    }

    private static final class MutableClock extends Clock {
        private Instant instant;
        private final ZoneId zoneId;

        private MutableClock(Instant instant, ZoneId zoneId) {
            this.instant = instant;
            this.zoneId = zoneId;
        }

        @Override
        public ZoneId getZone() {
            return zoneId;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }

        private void plusSeconds(long seconds) {
            instant = instant.plusSeconds(seconds);
        }
    }
}
