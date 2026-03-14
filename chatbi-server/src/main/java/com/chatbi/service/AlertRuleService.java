package com.chatbi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatbi.entity.AlertRule;
import com.chatbi.repository.AlertRuleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 告警规则服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertRuleService {

    private final AlertRuleMapper alertRuleMapper;
    private final EmailPushService emailPushService;
    private final DingTalkPushService dingTalkPushService;
    private final WeChatPushService weChatPushService;

    /**
     * 获取所有告警规则
     */
    public List<AlertRule> list() {
        LambdaQueryWrapper<AlertRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlertRule::getStatus, 1)
                .orderByDesc(AlertRule::getCreatedAt);
        return alertRuleMapper.selectList(wrapper);
    }

    /**
     * 根据 ID 查询
     */
    public AlertRule getById(Long id) {
        return alertRuleMapper.selectById(id);
    }

    /**
     * 创建告警规则
     */
    @Transactional
    public AlertRule create(AlertRule alertRule) {
        alertRuleMapper.insert(alertRule);
        return alertRule;
    }

    /**
     * 更新告警规则
     */
    @Transactional
    public AlertRule update(Long id, AlertRule alertRule) {
        AlertRule existing = alertRuleMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("告警规则不存在");
        }
        alertRule.setId(id);
        alertRuleMapper.updateById(alertRule);
        return alertRule;
    }

    /**
     * 删除告警规则
     */
    @Transactional
    public void delete(Long id) {
        alertRuleMapper.deleteById(id);
    }

    /**
     * 启用/禁用告警规则
     */
    @Transactional
    public void toggleStatus(Long id, Integer status) {
        AlertRule alertRule = alertRuleMapper.selectById(id);
        if (alertRule != null) {
            alertRule.setStatus(status);
            alertRuleMapper.updateById(alertRule);
        }
    }

    /**
     * 检查并触发告警
     */
    @Transactional
    public void checkAndAlert(AlertRule rule, Double currentValue) {
        boolean shouldAlert = false;

        if ("THRESHOLD".equals(rule.getAlertType())) {
            shouldAlert = checkThreshold(rule.getThresholdType(), currentValue, rule.getThresholdValue());
        } else if ("FLUCTUATION".equals(rule.getAlertType())) {
            // 波动率检查逻辑
            shouldAlert = checkFluctuation(currentValue, rule.getFluctuationRate());
        }

        if (shouldAlert) {
            sendAlert(rule, currentValue);
            rule.setLastAlertTime(LocalDateTime.now());
            alertRuleMapper.updateById(rule);
        }
    }

    private boolean checkThreshold(String thresholdType, Double currentValue, Double thresholdValue) {
        switch (thresholdType) {
            case ">":
                return currentValue > thresholdValue;
            case "<":
                return currentValue < thresholdValue;
            case ">=":
                return currentValue >= thresholdValue;
            case "<=":
                return currentValue <= thresholdValue;
            case "=":
                return currentValue.equals(thresholdValue);
            default:
                return false;
        }
    }

    private boolean checkFluctuation(Double currentValue, Double fluctuationRate) {
        // 简化实现：实际应该对比历史数据
        return false;
    }

    private void sendAlert(AlertRule rule, Double currentValue) {
        String title = "告警通知：" + rule.getRuleName();
        String content = String.format("指标当前值：%.2f，触发告警规则：%s", currentValue, rule.getRuleName());

        if ("EMAIL".equalsIgnoreCase(rule.getPushMethod())) {
            emailPushService.push(rule.getReceiver(), title, content);
        } else if ("DINGTALK".equalsIgnoreCase(rule.getPushMethod())) {
            dingTalkPushService.push(rule.getReceiver(), title, content);
        } else if ("WECHAT".equalsIgnoreCase(rule.getPushMethod())) {
            weChatPushService.push(rule.getReceiver(), title, content);
        }
    }
}
