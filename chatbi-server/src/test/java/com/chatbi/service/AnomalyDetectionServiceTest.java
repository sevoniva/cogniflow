package com.chatbi.service;

import com.chatbi.config.AiConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 异常检测服务测试
 */
@ExtendWith(MockitoExtension.class)
class AnomalyDetectionServiceTest {

    @Mock
    private AiConfig aiConfig;

    @InjectMocks
    private AnomalyDetectionService anomalyDetectionService;

    private List<Map<String, Object>> testData;

    @BeforeEach
    void setUp() {
        testData = new ArrayList<>();

        // 创建测试数据（包含异常值）
        for (int i = 0; i < 10; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", i + 1);
            row.put("value", i * 10.0); // 正常值
            testData.add(row);
        }

        // 添加异常值
        Map<String, Object> anomalyRow = new HashMap<>();
        anomalyRow.put("id", 11);
        anomalyRow.put("value", 500.0); // 异常值
        testData.add(anomalyRow);
    }

    @Test
    void testDetectAnomalies_WithAnomalies() {
        // 执行
        List<AnomalyDetectionService.AnomalyResult> anomalies =
            anomalyDetectionService.detectAnomalies(testData, "测试场景");

        // 验证
        assertNotNull(anomalies);
        assertTrue(anomalies.size() > 0, "应该检测到异常");

        AnomalyDetectionService.AnomalyResult firstAnomaly = anomalies.get(0);
        assertNotNull(firstAnomaly.getType());
        assertNotNull(firstAnomaly.getDescription());
        assertNotNull(firstAnomaly.getSeverity());
    }

    @Test
    void testDetectAnomalies_EmptyData() {
        // 执行
        List<AnomalyDetectionService.AnomalyResult> anomalies =
            anomalyDetectionService.detectAnomalies(new ArrayList<>(), "测试场景");

        // 验证
        assertNotNull(anomalies);
        assertEquals(0, anomalies.size(), "空数据不应检测到异常");
    }

    @Test
    void testDetectAnomalies_InsufficientData() {
        // 准备数据（少于3条）
        List<Map<String, Object>> smallData = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        row.put("value", 10.0);
        smallData.add(row);

        // 执行
        List<AnomalyDetectionService.AnomalyResult> anomalies =
            anomalyDetectionService.detectAnomalies(smallData, "测试场景");

        // 验证
        assertNotNull(anomalies);
        assertEquals(0, anomalies.size(), "数据不足不应检测到异常");
    }

    @Test
    void testDetectAnomalies_TrendAnomaly() {
        // 创建突增数据
        List<Map<String, Object>> trendData = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("value", 100.0);
            trendData.add(row);
        }

        // 添加突增
        Map<String, Object> spikeRow = new HashMap<>();
        spikeRow.put("value", 300.0);
        trendData.add(spikeRow);

        // 执行
        List<AnomalyDetectionService.AnomalyResult> anomalies =
            anomalyDetectionService.detectAnomalies(trendData, "测试场景");

        // 验证
        assertNotNull(anomalies);
        assertTrue(anomalies.stream().anyMatch(a -> "spike".equals(a.getType())),
            "应该检测到突增异常");
    }
}
