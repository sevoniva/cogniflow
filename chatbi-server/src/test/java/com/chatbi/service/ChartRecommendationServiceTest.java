package com.chatbi.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 图表推荐服务测试
 */
@ExtendWith(MockitoExtension.class)
class ChartRecommendationServiceTest {

    @InjectMocks
    private ChartRecommendationService chartRecommendationService;

    @Test
    void testRecommendChart_SingleValue() {
        // 准备单值数据
        List<Map<String, Object>> data = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        row.put("total", 1000);
        data.add(row);

        // 执行
        ChartRecommendationService.ChartRecommendation recommendation =
            chartRecommendationService.recommendChart("总销售额", data);

        // 验证
        assertNotNull(recommendation);
        assertEquals("number", recommendation.getChartType(), "单值应推荐数字卡片");
        assertNotNull(recommendation.getReason());
    }

    @Test
    void testRecommendChart_TimeSeries() {
        // 准备时间序列数据
        List<Map<String, Object>> data = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("date", "2024-01-" + (i + 1));
            row.put("sales", 1000 + i * 100);
            data.add(row);
        }

        // 执行
        ChartRecommendationService.ChartRecommendation recommendation =
            chartRecommendationService.recommendChart("销售趋势", data);

        // 验证
        assertNotNull(recommendation);
        assertEquals("line", recommendation.getChartType(), "时间序列应推荐折线图");
        assertNotNull(recommendation.getConfig());
    }

    @Test
    void testRecommendChart_CategoryComparison() {
        // 准备分类对比数据
        List<Map<String, Object>> data = new ArrayList<>();
        String[] regions = {"北京", "上海", "广州", "深圳"};
        for (String region : regions) {
            Map<String, Object> row = new HashMap<>();
            row.put("region", region);
            row.put("sales", Math.random() * 1000);
            data.add(row);
        }

        // 执行
        ChartRecommendationService.ChartRecommendation recommendation =
            chartRecommendationService.recommendChart("各地区销售额", data);

        // 验证
        assertNotNull(recommendation);
        assertTrue(recommendation.getChartType().equals("bar") ||
                   recommendation.getChartType().equals("pie"),
            "分类对比应推荐柱状图或饼图");
    }

    @Test
    void testRecommendChart_PieChart() {
        // 准备少量分类数据（适合饼图）
        List<Map<String, Object>> data = new ArrayList<>();
        String[] categories = {"A", "B", "C"};
        for (String category : categories) {
            Map<String, Object> row = new HashMap<>();
            row.put("category", category);
            row.put("value", Math.random() * 100);
            data.add(row);
        }

        // 执行
        ChartRecommendationService.ChartRecommendation recommendation =
            chartRecommendationService.recommendChart("占比分布", data);

        // 验证
        assertNotNull(recommendation);
        assertEquals("pie", recommendation.getChartType(), "占比数据应推荐饼图");
    }

    @Test
    void testRecommendChart_Table() {
        // 准备大数据集
        List<Map<String, Object>> data = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", i);
            row.put("name", "Item" + i);
            row.put("value1", Math.random() * 100);
            row.put("value2", Math.random() * 100);
            row.put("value3", Math.random() * 100);
            data.add(row);
        }

        // 执行
        ChartRecommendationService.ChartRecommendation recommendation =
            chartRecommendationService.recommendChart("详细数据", data);

        // 验证
        assertNotNull(recommendation);
        assertEquals("table", recommendation.getChartType(), "大数据集应推荐表格");
    }

    @Test
    void testRecommendChart_EmptyData() {
        // 执行
        ChartRecommendationService.ChartRecommendation recommendation =
            chartRecommendationService.recommendChart("空数据", new ArrayList<>());

        // 验证
        assertNotNull(recommendation);
        assertEquals("table", recommendation.getChartType(), "空数据应默认推荐表格");
    }

    @Test
    void testRecommendChart_RankingData() {
        // 准备排名数据
        List<Map<String, Object>> data = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("product", "Product" + i);
            row.put("sales", 1000 - i * 50);
            data.add(row);
        }

        // 执行
        ChartRecommendationService.ChartRecommendation recommendation =
            chartRecommendationService.recommendChart("TOP10产品排名", data);

        // 验证
        assertNotNull(recommendation);
        assertEquals("barHorizontal", recommendation.getChartType(), "排名数据应推荐横向条形图");
    }
}
