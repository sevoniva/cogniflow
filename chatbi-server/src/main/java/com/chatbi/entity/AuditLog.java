package com.chatbi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 审计日志实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("audit_log")
public class AuditLog {

    /**
     * 主键 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 链路 ID
     */
    private String traceId;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 操作类型
     */
    private String action;

    /**
     * 资源类型
     */
    private String resourceType;

    /**
     * 资源 ID
     */
    private Long resourceId;

    /**
     * 请求方法
     */
    private String requestMethod;

    /**
     * 请求 URI
     */
    private String requestUri;

    /**
     * 请求体
     */
    private String requestBody;

    /**
     * 响应状态
     */
    private Integer responseStatus;

    /**
     * 响应体
     */
    private String responseBody;

    /**
     * IP 地址
     */
    private String ipAddress;

    /**
     * 用户代理
     */
    private String userAgent;

    /**
     * 执行耗时 (ms)
     */
    private Integer executeTimeMs;

    /**
     * 操作结果 (SUCCESS/FAILED)
     */
    private String result;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
