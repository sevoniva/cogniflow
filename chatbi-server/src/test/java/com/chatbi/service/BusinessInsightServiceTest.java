package com.chatbi.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Transactional
@DisplayName("BusinessInsightService 测试")
class BusinessInsightServiceTest {

    @Autowired
    private BusinessInsightService businessInsightService;

    @ParameterizedTest(name = "{0} 指标查询应返回真实结果")
    @CsvSource({
        "销售额, 本月销售趋势",
        "毛利率, 本月毛利率趋势",
        "回款额, 本月回款额趋势",
        "库存周转天数, 查看库存周转天数",
        "订单履约率, 本月订单履约率趋势",
        "部门费用支出, 本月部门费用支出趋势",
        "项目交付及时率, 本月项目交付及时率趋势",
        "客户投诉量, 本月客户投诉量趋势",
        "研发工时利用率, 本月研发工时利用率趋势",
        "审批平均时长, 本月审批平均时长趋势"
    })
    void queryMetricShouldExecuteOnH2(String metric, String queryText) {
        BusinessInsightService.QueryPlan plan = businessInsightService.queryMetric(metric, queryText);

        assertNotNull(plan);
        assertNotNull(plan.getSql());
        assertNotNull(plan.getData());
        assertFalse(plan.getData().isEmpty(), () -> metric + " 未返回任何数据");
    }
}
