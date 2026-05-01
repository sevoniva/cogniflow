package com.chatbi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chatbi.entity.Subscription;
import com.chatbi.repository.SubscriptionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;

/**
 * 订阅服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionMapper subscriptionMapper;
    private final EmailPushService emailPushService;
    private final DingTalkPushService dingTalkPushService;
    private final WeChatPushService weChatPushService;

    /**
     * 分页查询订阅列表
     */
    public Page<Subscription> page(Long subscriberId, int current, int size) {
        LambdaQueryWrapper<Subscription> wrapper = new LambdaQueryWrapper<>();
        if (subscriberId != null) {
            wrapper.eq(Subscription::getSubscriberId, subscriberId);
        }
        wrapper.orderByDesc(Subscription::getCreatedAt);
        return subscriptionMapper.selectPage(new Page<>(current, size), wrapper);
    }

    /**
     * 查询所有订阅
     */
    public List<Subscription> list() {
        return subscriptionMapper.selectList(null);
    }

    /**
     * 根据 ID 查询订阅
     */
    public Subscription getById(Long id) {
        return subscriptionMapper.selectById(id);
    }

    /**
     * 创建订阅
     */
    @Transactional
    public Subscription create(Subscription subscription) {
        subscriptionMapper.insert(subscription);
        log.info("创建订阅成功：{} - {}", subscription.getTitle(), subscription.getSubscriberName());
        return subscription;
    }

    /**
     * 更新订阅
     */
    @Transactional
    public Subscription update(Long id, Subscription subscription) {
        Subscription existing = getById(id);
        if (existing == null) {
            throw new RuntimeException("订阅不存在");
        }

        subscription.setId(id);
        subscriptionMapper.updateById(subscription);
        log.info("更新订阅成功：{}", subscription.getTitle());
        return subscription;
    }

    /**
     * 删除订阅
     */
    @Transactional
    public void delete(Long id) {
        subscriptionMapper.deleteById(id);
        log.info("删除订阅成功：{}", id);
    }

    /**
     * 获取有效的订阅
     */
    public List<Subscription> getActiveSubscriptions() {
        LambdaQueryWrapper<Subscription> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Subscription::getStatus, 1);
        return subscriptionMapper.selectList(wrapper);
    }

    /**
     * 定时任务：检查并发送订阅推送
     * 每分钟执行一次
     */
    public void checkAndPush() {
        List<Subscription> subscriptions = getActiveSubscriptions();
        LocalDateTime now = LocalDateTime.now();

        for (Subscription subscription : subscriptions) {
            // 检查推送时间
            if (shouldPush(subscription, now)) {
                try {
                    pushSubscription(subscription);
                } catch (Exception ex) {
                    log.error("推送订阅失败，已跳过本条并继续处理后续订阅 - id: {}, title: {}, reason: {}",
                        subscription.getId(), subscription.getTitle(), ex.getMessage());
                }
            }
        }
    }

    /**
     * 判断是否应该推送
     */
    private boolean shouldPush(Subscription subscription, LocalDateTime now) {
        if (!isWithinScheduleMinute(subscription.getPushTime(), now)) {
            return false;
        }

        String frequency = StringUtils.hasText(subscription.getFrequency())
            ? subscription.getFrequency().trim().toUpperCase(Locale.ROOT)
            : "DAILY";

        LocalDateTime lastPush = subscription.getLastPushTime();

        return switch (frequency) {
            case "DAILY" -> shouldPushDaily(lastPush, now);
            case "WEEKLY" -> shouldPushWeekly(lastPush, now, subscription.getPushDay());
            case "MONTHLY" -> shouldPushMonthly(lastPush, now, subscription.getPushDay());
            case "CUSTOM" -> shouldPushCustom(lastPush, now, subscription.getPushDay());
            default -> {
                log.warn("未知订阅频率，跳过推送 - id: {}, frequency: {}", subscription.getId(), subscription.getFrequency());
                yield false;
            }
        };
    }

    private boolean isWithinScheduleMinute(String pushTime, LocalDateTime now) {
        if (!StringUtils.hasText(pushTime)) {
            return true;
        }
        try {
            LocalTime schedule = LocalTime.parse(pushTime.trim());
            return now.getHour() == schedule.getHour() && now.getMinute() == schedule.getMinute();
        } catch (Exception ex) {
            log.warn("订阅 pushTime 格式非法，跳过推送 - pushTime: {}", pushTime);
            return false;
        }
    }

    private boolean shouldPushDaily(LocalDateTime lastPush, LocalDateTime now) {
        return lastPush == null || !lastPush.toLocalDate().isEqual(now.toLocalDate());
    }

    private boolean shouldPushWeekly(LocalDateTime lastPush, LocalDateTime now, String pushDay) {
        DayOfWeek targetDay = parseWeekday(pushDay);
        if (targetDay != null && now.getDayOfWeek() != targetDay) {
            return false;
        }
        if (lastPush == null) {
            return true;
        }
        WeekFields weekFields = WeekFields.ISO;
        int nowWeek = now.get(weekFields.weekOfWeekBasedYear());
        int lastWeek = lastPush.get(weekFields.weekOfWeekBasedYear());
        return now.getYear() != lastPush.getYear() || nowWeek != lastWeek;
    }

    private boolean shouldPushMonthly(LocalDateTime lastPush, LocalDateTime now, String pushDay) {
        if (StringUtils.hasText(pushDay)) {
            try {
                int day = Integer.parseInt(pushDay.trim());
                if (now.getDayOfMonth() != day) {
                    return false;
                }
            } catch (NumberFormatException ex) {
                log.warn("订阅 pushDay 格式非法（MONTHLY），跳过推送 - pushDay: {}", pushDay);
                return false;
            }
        }
        if (lastPush == null) {
            return true;
        }
        return now.getYear() != lastPush.getYear() || now.getMonthValue() != lastPush.getMonthValue();
    }

    /**
     * CUSTOM 约定：
     * - pushDay 为数字：表示“每 N 天”执行一次
     * - pushDay 为空：按 daily 执行
     */
    private boolean shouldPushCustom(LocalDateTime lastPush, LocalDateTime now, String pushDay) {
        if (!StringUtils.hasText(pushDay)) {
            return shouldPushDaily(lastPush, now);
        }
        try {
            int intervalDays = Integer.parseInt(pushDay.trim());
            if (intervalDays <= 0) {
                return false;
            }
            if (lastPush == null) {
                return true;
            }
            return !lastPush.plusDays(intervalDays).isAfter(now);
        } catch (NumberFormatException ex) {
            log.warn("订阅 pushDay 格式非法（CUSTOM），跳过推送 - pushDay: {}", pushDay);
            return false;
        }
    }

    private DayOfWeek parseWeekday(String pushDay) {
        if (!StringUtils.hasText(pushDay)) {
            return null;
        }
        String normalized = pushDay.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "MONDAY", "周一", "星期一", "1" -> DayOfWeek.MONDAY;
            case "TUESDAY", "周二", "星期二", "2" -> DayOfWeek.TUESDAY;
            case "WEDNESDAY", "周三", "星期三", "3" -> DayOfWeek.WEDNESDAY;
            case "THURSDAY", "周四", "星期四", "4" -> DayOfWeek.THURSDAY;
            case "FRIDAY", "周五", "星期五", "5" -> DayOfWeek.FRIDAY;
            case "SATURDAY", "周六", "星期六", "6" -> DayOfWeek.SATURDAY;
            case "SUNDAY", "周日", "星期日", "7" -> DayOfWeek.SUNDAY;
            default -> null;
        };
    }

    /**
     * 推送订阅
     */
    private void pushSubscription(Subscription subscription) {
        log.info("推送订阅：{} - {}", subscription.getTitle(), subscription.getSubscriberName());

        String pushMethod = subscription.getPushMethod();
        String receiver = subscription.getReceiver();
        String title = subscription.getTitle();
        String content = "您订阅的 \"" + title + "\" 已更新，请及时查看。";

        // 根据 pushMethod 推送（EMAIL/DINGTALK/WECHAT）
        if ("EMAIL".equalsIgnoreCase(pushMethod)) {
            emailPushService.push(receiver, title, content);
        } else if ("DINGTALK".equalsIgnoreCase(pushMethod)) {
            dingTalkPushService.push(receiver, title, content);
        } else if ("WECHAT".equalsIgnoreCase(pushMethod)) {
            weChatPushService.push(receiver, title, content);
        } else {
            throw new IllegalArgumentException("未知的推送方式: " + pushMethod);
        }

        // 更新最后推送时间
        subscription.setLastPushTime(LocalDateTime.now());
        subscription.setPushCount(subscription.getPushCount() == null ? 1 : subscription.getPushCount() + 1);
        subscriptionMapper.updateById(subscription);

        log.info("推送订阅完成：{} - {}", subscription.getTitle(), subscription.getSubscriberName());
    }
}
