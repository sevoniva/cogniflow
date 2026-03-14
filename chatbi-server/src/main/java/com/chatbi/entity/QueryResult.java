package com.chatbi.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 查询结果实体 - 与前端 QueryResult 接口对应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryResult {
    private String query;           // 原始查询语句
    private String sql;             // 生成的SQL语句
    private String metric;          // 匹配的指标名称
    private String timeRange;       // 时间范围
    private String dimension;       // 维度
    private Integer total;          // 记录数
    private List<Map<String, Object>> data;  // 数据列表
    private String summary;         // 查询结果摘要
    private String source;          // 结果来源
    private List<String> suggestions; // 建议追问
    private List<String> candidateMetrics; // 候选指标
    private Boolean disambiguation; // 是否处于指标澄清场景
    private Map<String, Object> aiStatus; // AI 运行时状态
    private Map<String, Object> diagnosis; // 查询诊断信息（原因码/说明/建议）
}
