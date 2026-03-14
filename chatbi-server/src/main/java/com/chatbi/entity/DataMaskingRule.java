package com.chatbi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 数据脱敏规则实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("data_masking_rule")
public class DataMaskingRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 适用表名
     */
    private String tableName;

    /**
     * 适用字段名
     */
    private String fieldName;

    /**
     * 角色 ID
     */
    private Long roleId;

    /**
     * 用户 ID (为空表示适用于整个角色)
     */
    private Long userId;

    /**
     * 脱敏类型 (HIDE / PARTIAL / HASH / ENCRYPT)
     */
    private String maskType;

    /**
     * 脱敏规则 (如显示前 3 后 4)
     */
    private String maskPattern;

    /**
     * 优先级 (数字越小优先级越高)
     */
    private Integer priority;

    /**
     * 状态 (0-禁用 1-启用)
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
     * 删除时间
     */
    @TableLogic
    private LocalDateTime deletedAt;
}
