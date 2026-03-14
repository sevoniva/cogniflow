package com.chatbi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 仪表板实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("dashboard")
public class Dashboard {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 仪表板名称
     */
    private String name;

    /**
     * 仪表板描述
     */
    private String description;

    /**
     * 布局配置 (JSON)
     */
    private String layoutConfig;

    /**
     * 图表配置列表 (JSON)
     */
    private String chartsConfig;

    /**
     * 封面图
     */
    private String coverImage;

    /**
     * 创建人
     */
    private Long createdBy;

    /**
     * 创建人姓名
     */
    private String createdByName;

    /**
     * 是否公开
     */
    private Boolean isPublic;

    /**
     * 状态 (0-草稿 1-发布)
     */
    private Integer status;

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
