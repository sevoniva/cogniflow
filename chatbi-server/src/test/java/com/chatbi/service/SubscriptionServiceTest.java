package com.chatbi.service;

import com.chatbi.entity.Subscription;
import com.chatbi.repository.SubscriptionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionMapper subscriptionMapper;
    @Mock
    private EmailPushService emailPushService;
    @Mock
    private DingTalkPushService dingTalkPushService;
    @Mock
    private WeChatPushService weChatPushService;

    private static String currentMinute() {
        LocalDateTime now = LocalDateTime.now();
        return String.format("%02d:%02d", now.getHour(), now.getMinute());
    }

    private static String nonTodayWeekday() {
        return switch (LocalDateTime.now().getDayOfWeek()) {
            case MONDAY -> "TUESDAY";
            case TUESDAY -> "WEDNESDAY";
            case WEDNESDAY -> "THURSDAY";
            case THURSDAY -> "FRIDAY";
            case FRIDAY -> "SATURDAY";
            case SATURDAY -> "SUNDAY";
            case SUNDAY -> "MONDAY";
        };
    }

    @Test
    void shouldUpdatePushCountWhenPushSuccess() {
        Subscription subscription = Subscription.builder()
            .id(1L)
            .title("日报订阅")
            .subscriberName("Carson")
            .pushMethod("EMAIL")
            .receiver("ops@chatbi.com")
            .status(1)
            .pushCount(2)
            .frequency("DAILY")
            .pushTime(currentMinute())
            .build();
        when(subscriptionMapper.selectList(any())).thenReturn(List.of(subscription));
        SubscriptionService service = new SubscriptionService(
            subscriptionMapper, emailPushService, dingTalkPushService, weChatPushService);

        service.checkAndPush();

        verify(emailPushService).push("ops@chatbi.com", "日报订阅", "您订阅的 \"日报订阅\" 已更新，请及时查看。");
        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionMapper).updateById(captor.capture());
        assertEquals(3, captor.getValue().getPushCount());
    }

    @Test
    void shouldSkipFailedSubscriptionAndContinue() {
        Subscription failed = Subscription.builder()
            .id(1L)
            .title("失败订阅")
            .subscriberName("A")
            .pushMethod("EMAIL")
            .receiver("a@chatbi.com")
            .status(1)
            .frequency("DAILY")
            .pushTime(currentMinute())
            .build();
        Subscription success = Subscription.builder()
            .id(2L)
            .title("成功订阅")
            .subscriberName("B")
            .pushMethod("DINGTALK")
            .receiver("https://oapi.dingtalk.com/robot/send?access_token=x")
            .status(1)
            .frequency("DAILY")
            .pushTime(currentMinute())
            .build();
        when(subscriptionMapper.selectList(any())).thenReturn(List.of(failed, success));
        doThrow(new IllegalStateException("mail down"))
            .when(emailPushService).push(any(), any(), any());
        SubscriptionService service = new SubscriptionService(
            subscriptionMapper, emailPushService, dingTalkPushService, weChatPushService);

        service.checkAndPush();

        verify(dingTalkPushService).push(success.getReceiver(), "成功订阅", "您订阅的 \"成功订阅\" 已更新，请及时查看。");
        verify(subscriptionMapper, never()).updateById(failed);
        verify(subscriptionMapper).updateById(success);
    }

    @Test
    void shouldSkipWhenPushTimeNotMatched() {
        Subscription subscription = Subscription.builder()
            .id(3L)
            .title("错过时间窗口")
            .pushMethod("EMAIL")
            .receiver("ops@chatbi.com")
            .status(1)
            .frequency("DAILY")
            .pushTime("00:00")
            .build();
        when(subscriptionMapper.selectList(any())).thenReturn(List.of(subscription));
        SubscriptionService service = new SubscriptionService(
            subscriptionMapper, emailPushService, dingTalkPushService, weChatPushService);

        service.checkAndPush();

        verify(emailPushService, never()).push(any(), any(), any());
        verify(subscriptionMapper, never()).updateById(any());
    }

    @Test
    void shouldSkipWeeklyWhenTodayNotTargetDay() {
        Subscription subscription = Subscription.builder()
            .id(4L)
            .title("周报")
            .pushMethod("EMAIL")
            .receiver("ops@chatbi.com")
            .status(1)
            .frequency("WEEKLY")
            .pushDay(nonTodayWeekday())
            .pushTime(currentMinute())
            .build();
        when(subscriptionMapper.selectList(any())).thenReturn(List.of(subscription));
        SubscriptionService service = new SubscriptionService(
            subscriptionMapper, emailPushService, dingTalkPushService, weChatPushService);

        service.checkAndPush();

        verify(emailPushService, never()).push(any(), any(), any());
    }

    @Test
    void shouldSkipMonthlyWhenNotTargetDay() {
        Subscription subscription = Subscription.builder()
            .id(5L)
            .title("月报")
            .pushMethod("EMAIL")
            .receiver("ops@chatbi.com")
            .status(1)
            .frequency("MONTHLY")
            .pushDay("31")
            .pushTime(currentMinute())
            .build();
        when(subscriptionMapper.selectList(any())).thenReturn(List.of(subscription));
        SubscriptionService service = new SubscriptionService(
            subscriptionMapper, emailPushService, dingTalkPushService, weChatPushService);

        service.checkAndPush();

        verify(emailPushService, never()).push(any(), any(), any());
    }

    @Test
    void shouldPushCustomIntervalWhenLastPushExpired() {
        Subscription subscription = Subscription.builder()
            .id(6L)
            .title("自定义间隔")
            .pushMethod("EMAIL")
            .receiver("ops@chatbi.com")
            .status(1)
            .frequency("CUSTOM")
            .pushDay("2")
            .lastPushTime(LocalDateTime.now().minusDays(3))
            .pushTime(currentMinute())
            .build();
        when(subscriptionMapper.selectList(any())).thenReturn(List.of(subscription));
        SubscriptionService service = new SubscriptionService(
            subscriptionMapper, emailPushService, dingTalkPushService, weChatPushService);

        service.checkAndPush();

        verify(emailPushService).push(any(), eq("自定义间隔"), any());
        verify(subscriptionMapper).updateById(subscription);
    }
}
