package com.chatbi.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DataGeneratorService 单元测试
 */
@SpringBootTest
class DataGeneratorServiceTest {

    @Autowired
    private DataGeneratorService dataGenerator;

    @Test
    void testGenerateSalesData() {
        List<Map<String, Object>> data = dataGenerator.generateSalesData("部门", 8);

        assertNotNull(data);
        assertFalse(data.isEmpty());
        assertTrue(data.size() <= 8);

        // 验证数据结构
        Map<String, Object> firstItem = data.get(0);
        assertTrue(firstItem.containsKey("部门"));
        assertTrue(firstItem.containsKey("销售额"));
    }

    @Test
    void testGenerateExpenseData() {
        List<Map<String, Object>> data = dataGenerator.generateExpenseData(8);

        assertNotNull(data);
        assertFalse(data.isEmpty());

        Map<String, Object> firstItem = data.get(0);
        assertTrue(firstItem.containsKey("部门"));
        assertTrue(firstItem.containsKey("费用金额"));
        assertTrue(firstItem.containsKey("预算执行率"));
    }

    @Test
    void testGenerateProjectData() {
        List<Map<String, Object>> data = dataGenerator.generateProjectData(5);

        assertNotNull(data);
        assertFalse(data.isEmpty());

        Map<String, Object> firstItem = data.get(0);
        assertTrue(firstItem.containsKey("项目类型"));
        assertTrue(firstItem.containsKey("项目数"));
        assertTrue(firstItem.containsKey("及时率"));
    }

    @Test
    void testGenerateComplaintData() {
        List<Map<String, Object>> data = dataGenerator.generateComplaintData(6);

        assertNotNull(data);
        assertFalse(data.isEmpty());

        Map<String, Object> firstItem = data.get(0);
        assertTrue(firstItem.containsKey("区域"));
        assertTrue(firstItem.containsKey("投诉量"));
        assertTrue(firstItem.containsKey("环比"));
    }

    @Test
    void testGenerateWorkHourData() {
        List<Map<String, Object>> data = dataGenerator.generateWorkHourData(6);

        assertNotNull(data);
        assertFalse(data.isEmpty());

        Map<String, Object> firstItem = data.get(0);
        assertTrue(firstItem.containsKey("团队"));
        assertTrue(firstItem.containsKey("总工时"));
        assertTrue(firstItem.containsKey("利用率"));
    }

    @Test
    void testGenerateTimeSeriesData() {
        List<Map<String, Object>> data = dataGenerator.generateTimeSeriesData("销售额", 12);

        assertNotNull(data);
        assertEquals(12, data.size());

        Map<String, Object> firstItem = data.get(0);
        assertTrue(firstItem.containsKey("月份"));
        assertTrue(firstItem.containsKey("销售额"));
    }

    @Test
    void testGenerateDashboardStats() {
        Map<String, Object> stats = dataGenerator.generateDashboardStats();

        assertNotNull(stats);
        assertTrue(stats.containsKey("totalSales"));
        assertTrue(stats.containsKey("totalOrders"));
        assertTrue(stats.containsKey("salesGrowth"));

        // 验证数据类型
        assertTrue(stats.get("totalSales") instanceof Integer);
        assertTrue(stats.get("salesGrowth") instanceof String);
    }

    @Test
    void testGenerateSalesDataWithDifferentDimensions() {
        // 测试不同维度
        List<Map<String, Object>> deptData = dataGenerator.generateSalesData("部门", 5);
        List<Map<String, Object>> regionData = dataGenerator.generateSalesData("区域", 5);
        List<Map<String, Object>> productData = dataGenerator.generateSalesData("产品", 5);

        assertNotNull(deptData);
        assertNotNull(regionData);
        assertNotNull(productData);

        assertTrue(deptData.get(0).containsKey("部门"));
        assertTrue(regionData.get(0).containsKey("区域"));
        assertTrue(productData.get(0).containsKey("产品类别"));
    }
}
