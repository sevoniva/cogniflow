package com.chatbi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * NL2SQL 反馈实体
 *
 * 用户对 AI 生成 SQL 的反馈，用于持续优化模型。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("feedback")
public class Feedback {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联的消息 ID
     */
    private String messageId;

    /**
     * 关联的对话 ID
     */
    private String conversationId;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 用户自然语言问题
     */
    private String question;

    /**
     * AI 生成的 SQL
     */
    private String generatedSql;

    /**
     * 评分：1=点赞，-1=点踩，0=未评分
     */
    private Integer rating;

    /**
     * 用户标注的正确 SQL（可选）
     */
    private String correctSql;

    /**
     * 用户评论（可选）
     */
    private String comment;

    /**
     * 是否已导出到训练集
     */
    private Boolean exported;

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
}
