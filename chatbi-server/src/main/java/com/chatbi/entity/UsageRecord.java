package com.chatbi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用量记录实体
 *
 * 记录用户的 API 调用、查询执行等计费事件。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("usage_record")
public class UsageRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 资源类型：ai_call / query / export / login
     */
    private String resourceType;

    /**
     * 具体操作：如 generateText / executeQuery / exportExcel
     */
    private String action;

    /**
     * 消耗配额（如 token 数、查询次数）
     */
    private Integer cost;

    /**
     * 关联业务 ID（如 conversationId, queryHistoryId）
     */
    private String referenceId;

    /**
     * 元数据 JSON（如 provider、model、sqlLength 等）
     */
    private String metadata;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
