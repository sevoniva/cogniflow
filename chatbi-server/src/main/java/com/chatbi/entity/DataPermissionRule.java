package com.chatbi.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 数据权限规则实体 - 行级权限
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("data_permission_rule")
public class DataPermissionRule {

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
     * 操作符 (= / != / > / < / IN / LIKE 等)
     */
    @TableField("operator_symbol")
    @JsonProperty("operator")
    @JsonAlias("operator")
    private String operatorSymbol;

    /**
     * 值 (可以是具体值或字段名)
     */
    @TableField("rule_value")
    @JsonProperty("value")
    @JsonAlias("value")
    private String ruleValue;

    /**
     * 值类型 (CONSTANT 常量 / FIELD 字段 / USER_ATTR 用户属性)
     */
    private String valueType;

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
