package com.chatbi.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 预测服务测试
 */
@ExtendWith(MockitoExtension.class)
class PredictionServiceTest {

    @InjectMocks
    private PredictionService predictionService;

    private List<Map<String, Object>> testData;

    @BeforeEach
    void setUp() {
        testData = new ArrayList<>();

        // 创建线性增长的测试数据
        for (int i = 0; i < 10; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("period", i + 1);
            row.put("value", 100.0 + i * 10.0);
            testData.add(row);
        }
    }

    @Test
    void testPredict_Success() {
        // 执行
        PredictionService.PredictionResult result =
            predictionService.predict(testData, "value", 3);

        // 验证
        assertNotNull(result);
        assertEquals("value", result.getColumn());
        assertNotNull(result.getHistoricalValues());
        assertNotNull(result.getPredictedValues());
        assertEquals(3, result.getPredictedValues().size(), "应该预测3期");
        assertNotNull(result.getMethod());
        assertTrue(result.getConfidence() >= 0 && result.getConfidence() <= 1,
            "置信度应该在0-1之间");
        assertNotNull(result.getTrend());
    }

    @Test
    void testPredict_InsufficientData() {
        // 准备不足的数据
        List<Map<String, Object>> smallData = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        row.put("value", 100.0);
        smallData.add(row);

        // 执行
        PredictionService.PredictionResult result =
            predictionService.predict(smallData, "value", 3);

        // 验证
        assertNotNull(result);
        assertNull(result.getPredictedValues(), "数据不足不应生成预测");
    }

    @Test
    void testPredict_IncreasingTrend() {
        // 执行
        PredictionService.PredictionResult result =
            predictionService.predict(testData, "value", 3);

        // 验证
        assertNotNull(result);
        assertEquals("increasing", result.getTrend(), "应该检测到上升趋势");

        // 验证预测值递增
        List<Double> predicted = result.getPredictedValues();
        for (int i = 1; i < predicted.size(); i++) {
            assertTrue(predicted.get(i) >= predicted.get(i - 1),
                "预测值应该递增");
        }
    }

    @Test
    void testPredict_DecreasingTrend() {
        // 创建下降趋势数据
        List<Map<String, Object>> decreasingData = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("value", 200.0 - i * 10.0);
            decreasingData.add(row);
        }

        // 执行
        PredictionService.PredictionResult result =
            predictionService.predict(decreasingData, "value", 3);

        // 验证
        assertNotNull(result);
        assertEquals("decreasing", result.getTrend(), "应该检测到下降趋势");
    }

    @Test
    void testPredict_StableTrend() {
        // 创建稳定数据
        List<Map<String, Object>> stableData = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("value", 100.0 + (i % 2 == 0 ? 1 : -1));
            stableData.add(row);
        }

        // 执行
        PredictionService.PredictionResult result =
            predictionService.predict(stableData, "value", 3);

        // 验证
        assertNotNull(result);
        assertEquals("stable", result.getTrend(), "应该检测到稳定趋势");
    }

    @Test
    void testPredict_NonNumericColumn() {
        // 执行
        PredictionService.PredictionResult result =
            predictionService.predict(testData, "period", 3);

        // 验证
        assertNotNull(result);
        // 应该能处理数值列
        assertNotNull(result.getPredictedValues());
    }
}
