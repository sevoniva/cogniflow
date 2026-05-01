package com.chatbi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 指标实体
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("metrics")
public class Metric {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 指标编码
     */
    private String code;

    /**
     * 指标名称
     */
    private String name;

    /**
     * 定义描述
     */
    private String definition;

    /**
     * 数据类型 (NUMERIC/STRING/DATE)
     */
    private String dataType;

    /**
     * 数据源 ID
     */
    @TableField("data_source_id")
    private Long datasourceId;

    /**
     * 表名
     */
    private String tableName;

    /**
     * 字段名
     */
    @TableField("field_name")
    private String columnName;

    /**
     * 聚合函数 (SUM/COUNT/AVG/MAX/MIN)
     */
    private String aggregation;

    /**
     * 指标 SQL 模板（Headless BI：cubeSql 包含占位符如 {{timeFilter}} {{dimension}}）
     */
    private String cubeSql;

    /**
     * 维度定义 JSON，如：[{"field":"region","name":"地区"},{"field":"created_at","name":"时间"}]
     */
    private String dimensions;

    /**
     * 度量定义 JSON，如：[{"field":"amount","aggregation":"SUM","name":"销售额"}]
     */
    private String measures;

    /**
     * 状态 (active/inactive)
     */
    @Builder.Default
    private String status = "active";

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
     * 删除标记
     */
    @TableLogic(value = "null", delval = "now()")
    private LocalDateTime deletedAt;
}
