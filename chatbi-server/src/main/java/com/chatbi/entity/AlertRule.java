package com.chatbi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 告警规则实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("alert_rule")
public class AlertRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 指标 ID
     */
    private Long metricId;

    /**
     * 数据源 ID
     */
    private Long datasourceId;

    /**
     * 告警类型 (THRESHOLD/FLUCTUATION/ANOMALY)
     */
    private String alertType;

    /**
     * 阈值 (大于/小于/等于)
     */
    private String thresholdType;

    /**
     * 阈值
     */
    private Double thresholdValue;

    /**
     * 波动率 (%)
     */
    private Double fluctuationRate;

    /**
     * 比较周期 (日/周/月)
     */
    private String comparePeriod;

    /**
     * 检查频率 (每小时/每天/每周)
     */
    private String checkFrequency;

    /**
     * 推送方式 (EMAIL/DINGTALK/WECHAT)
     */
    private String pushMethod;

    /**
     * 接收人
     */
    private String receiver;

    /**
     * 是否启用
     */
    private Integer status;

    /**
     * 最后告警时间
     */
    private LocalDateTime lastAlertTime;

    /**
     * 创建人
     */
    private Long createdBy;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 删除时间
     */
    @TableLogic
    private LocalDateTime deletedAt;
}
