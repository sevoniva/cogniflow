package com.chatbi.service;

import com.chatbi.entity.Metric;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@DisplayName("BusinessInsightService 测试")
class BusinessInsightServiceTest {

    @Autowired
    private BusinessInsightService businessInsightService;

    @Test
    @DisplayName("queryMetric 应基于指标配置执行真实查询")
    void queryMetricShouldExecuteOnH2() {
        Metric metric = new Metric();
        metric.setName("订单总量");
        metric.setTableName("metrics");
        metric.setColumnName("id");
        metric.setAggregation("COUNT");
        metric.setStatus("active");

        BusinessInsightService.QueryPlan plan = businessInsightService.queryMetric(metric, "查询订单总量");

        assertNotNull(plan);
        assertNotNull(plan.getSql());
        assertNotNull(plan.getData());
        assertFalse(plan.getData().isEmpty(), "订单总量 未返回任何数据");
    }

    @Test
    @DisplayName("getOverviewRows 应基于真实 active 指标返回概览数据")
    void getOverviewRowsShouldReturnRealDataFromActiveMetrics() {
        var rows = businessInsightService.getOverviewRows();
        assertNotNull(rows);
        // test-data.sql 已预置 3 个带数据口径的 active 指标，概览应能查真实数据
        assertFalse(rows.isEmpty(), "有 active 指标且配置了数据口径时应返回真实概览数据");
        List<String> metrics = rows.stream().map(r -> (String) r.get("指标")).toList();
        assertTrue(metrics.contains("销售额"), "概览应包含销售额");
    }
}
