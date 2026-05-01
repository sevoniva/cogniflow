package com.chatbi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Prompt 版本管理实体
 *
 * 支持多版本 Prompt 管理、A/B 测试灰度发布。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("prompt_version")
public class PromptVersion {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 版本名称（如：v1-标准模板、v2-CoT增强）
     */
    private String name;

    /**
     * 版本标签（唯一标识，如：sql-gen-v2）
     */
    private String versionTag;

    /**
     * Prompt 模板内容（LangChain4j PromptTemplate 格式）
     */
    private String template;

    /**
     * 模板变量说明（JSON 格式，如：{"tableSchema":"表结构","question":"用户问题"}）
     */
    private String variables;

    /**
     * 状态：active（生效中）、deprecated（已废弃）、draft（草稿）
     */
    private String status;

    /**
     * 灰度比例（0-100），仅 active 状态有效
     */
    private Integer grayScalePercent;

    /**
     * 描述
     */
    private String description;

    /**
     * 创建人
     */
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private LocalDateTime deletedAt;
}
