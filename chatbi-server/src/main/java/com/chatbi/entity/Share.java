package com.chatbi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 分享实体
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("share")
public class Share {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 分享标题
     */
    private String title;

    /**
     * 分享类型 (DASHBOARD/REPORT/CHART)
     */
    private String type;

    /**
     * 资源 ID
     */
    private Long resourceId;

    /**
     * 分享链接 Token
     */
    private String shareToken;

    /**
     * 分享方式 (LINK/EMAIL)
     */
    private String shareMethod;

    /**
     * 有效期类型 (PERMANENT/DAYS/DATE_RANGE)
     */
    private String validityType;

    /**
     * 有效天数
     */
    private Integer validityDays;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 密码保护
     */
    private String password;

    /**
     * 访问次数限制
     */
    private Integer maxVisits;

    /**
     * 当前访问次数
     */
    private Integer currentVisits;

    /**
     * 状态 (0 禁用 1 正常)
     */
    private Integer status;

    /**
     * 创建人
     */
    private Long createdBy;

    /**
     * 创建人姓名
     */
    private String creatorName;

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
     * 删除标记
     */
    @TableLogic
    private LocalDateTime deletedAt;
}
