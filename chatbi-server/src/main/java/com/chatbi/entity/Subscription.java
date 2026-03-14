package com.chatbi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 订阅实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("subscription")
public class Subscription {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 订阅标题
     */
    private String title;

    /**
     * 订阅类型 (DASHBOARD/REPORT/METRIC)
     */
    private String type;

    /**
     * 资源 ID
     */
    private Long resourceId;

    /**
     * 订阅人 ID
     */
    private Long subscriberId;

    /**
     * 订阅人姓名
     */
    private String subscriberName;

    /**
     * 推送方式 (EMAIL/DINGTALK/WECHAT)
     */
    private String pushMethod;

    /**
     * 接收人（邮箱/钉钉 webhook/企业微信 webhook）
     */
    private String receiver;

    /**
     * 推送频率 (DAILY/WEEKLY/MONTHLY/CUSTOM)
     */
    private String frequency;

    /**
     * 推送时间 (HH:mm)
     */
    private String pushTime;

    /**
     * 推送日期 (周一/1 号等)
     */
    private String pushDay;

    /**
     * 状态 (0 禁用 1 正常)
     */
    private Integer status;

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
     * 上次推送时间
     */
    private LocalDateTime lastPushTime;

    /**
     * 推送次数
     */
    private Integer pushCount;

    /**
     * 删除标记
     */
    @TableLogic
    private LocalDateTime deletedAt;
}
