package com.chatbi.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 智能推荐服务测试
 */
@ExtendWith(MockitoExtension.class)
class SmartRecommendationServiceTest {

    @Mock
    private QueryHistoryService queryHistoryService;

    @InjectMocks
    private SmartRecommendationService smartRecommendationService;

    @Test
    void testGetNextStepRecommendations_SalesQuery() {
        // 执行
        List<String> recommendations =
            smartRecommendationService.getNextStepRecommendations("本月销售额是多少", new ArrayList<>());

        // 验证
        assertNotNull(recommendations);
        assertFalse(recommendations.isEmpty(), "应该返回推荐");
        assertTrue(recommendations.stream().anyMatch(r -> r.contains("对比") || r.contains("地区") || r.contains("增长") || r.contains("详细")),
            "销售额查询应推荐对比、地区、增长或详细分析");
    }

    @Test
    void testGetNextStepRecommendations_CountQuery() {
        // 执行
        List<String> recommendations =
            smartRecommendationService.getNextStepRecommendations("订单数量", new ArrayList<>());

        // 验证
        assertNotNull(recommendations);
        assertFalse(recommendations.isEmpty());
        assertTrue(recommendations.stream().anyMatch(r -> r.contains("占比") || r.contains("增长")),
            "数量查询应推荐占比或增长分析");
    }

    @Test
    void testGetNextStepRecommendations_TrendQuery() {
        // 执行
        List<String> recommendations =
            smartRecommendationService.getNextStepRecommendations("销售趋势", new ArrayList<>());

        // 验证
        assertNotNull(recommendations);
        assertFalse(recommendations.isEmpty());
        assertTrue(recommendations.stream().anyMatch(r -> r.contains("预测") || r.contains("波动")),
            "趋势查询应推荐预测或波动分析");
    }

    @Test
    void testGetNextStepRecommendations_ComparisonQuery() {
        // 执行
        List<String> recommendations =
            smartRecommendationService.getNextStepRecommendations("对比分析", new ArrayList<>());

        // 验证
        assertNotNull(recommendations);
        assertFalse(recommendations.isEmpty());
        assertTrue(recommendations.stream().anyMatch(r -> r.contains("详细") || r.contains("原因")),
            "对比查询应推荐详细数据或原因分析");
    }

    @Test
    void testGetNextStepRecommendations_WithLargeResult() {
        // 准备大结果集
        List<Map<String, Object>> largeResult = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            largeResult.add(new HashMap<>());
        }

        // 执行
        List<String> recommendations =
            smartRecommendationService.getNextStepRecommendations("查询数据", largeResult);

        // 验证
        assertNotNull(recommendations);
        assertTrue(recommendations.stream().anyMatch(r -> r.contains("筛选") || r.contains("前")),
            "大结果集应推荐筛选");
    }

    @Test
    void testGetNextStepRecommendations_WithSingleResult() {
        // 准备单条结果
        List<Map<String, Object>> singleResult = new ArrayList<>();
        singleResult.add(new HashMap<>());

        // 执行
        List<String> recommendations =
            smartRecommendationService.getNextStepRecommendations("查询数据", singleResult);

        // 验证
        assertNotNull(recommendations);
        assertTrue(recommendations.stream().anyMatch(r -> r.contains("详细") || r.contains("明细")),
            "单条结果应推荐查看详细");
    }

    @Test
    void testGetSimilarQueries() {
        // 执行
        List<String> similar =
            smartRecommendationService.getSimilarQueries("本月销售额");

        // 验证
        assertNotNull(similar);
        assertFalse(similar.isEmpty());
        assertTrue(similar.stream().anyMatch(q -> q.contains("上月") || q.contains("季度")),
            "应该推荐相似的时间维度查询");
    }

    @Test
    void testGetSimilarQueries_WithRegion() {
        // 执行
        List<String> similar =
            smartRecommendationService.getSimilarQueries("北京地区销售额");

        // 验证
        assertNotNull(similar);
        assertFalse(similar.isEmpty());
        assertTrue(similar.stream().anyMatch(q -> q.contains("产品") || q.contains("客户")),
            "应该推荐其他维度的查询");
    }

    @Test
    void testGetDefaultRecommendations() {
        // 通过反射调用私有方法或通过公共方法间接测试
        List<String> recommendations =
            smartRecommendationService.getPersonalizedRecommendations(999L); // 不存在的用户

        // 验证
        assertNotNull(recommendations);
        assertFalse(recommendations.isEmpty(), "应该返回默认推荐");
        assertTrue(recommendations.size() <= 10, "推荐数量应该限制在10个以内");
    }
}
