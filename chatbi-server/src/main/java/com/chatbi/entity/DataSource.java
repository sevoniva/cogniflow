package com.chatbi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 数据源实体
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("data_source")
public class DataSource {

    /**
     * 主键 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 数据源名称
     */
    private String name;

    /**
     * 数据源编码
     */
    private String code;

    /**
     * 数据源类型 (MYSQL/ORACLE/HIVE/PG/OB_ORACLE/OB_MYSQL/CLICKHOUSE/SQLSERVER/POSTGRESQL)
     */
    private String type;

    /**
     * 主机地址
     */
    private String host;

    /**
     * 端口
     */
    private Integer port;

    /**
     * 数据库名
     */
    @TableField("`database`")
    private String database;

    /**
     * 服务名 (Oracle SID/Service)
     */
    private String service;

    /**
     * 连接 URL
     */
    private String url;

    /**
     * 用户名
     */
    private String username;

    /**
     * 加密密码
     */
    private String passwordEncrypted;

    /**
     * 驱动类
     */
    private String driverClass;

    /**
     * 扩展配置
     */
    private String configJson;

    /**
     * 状态 (0 禁用 1 正常)
     */
    private Integer status;

    /**
     * 健康状态 (0 异常 1 正常)
     */
    private Integer healthStatus;

    /**
     * 最后检查时间
     */
    private LocalDateTime lastCheckTime;

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
