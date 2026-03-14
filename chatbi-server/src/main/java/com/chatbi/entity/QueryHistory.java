package com.chatbi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 查询历史实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("query_history")
public class QueryHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 查询名称
     */
    private String queryName;

    /**
     * 查询类型 (SQL/NATURAL_LANGUAGE/VISUAL)
     */
    private String queryType;

    /**
     * 查询内容 (SQL 语句或自然语言)
     */
    private String queryContent;

    /**
     * 数据源 ID
     */
    private Long datasourceId;

    /**
     * 结果数据
     */
    private String resultData;

    /**
     * 执行时长 (毫秒)
     */
    private Long duration;

    /**
     * 状态 (SUCCESS/FAILED)
     */
    private String status;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 是否收藏
     */
    private Boolean isFavorite;

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
