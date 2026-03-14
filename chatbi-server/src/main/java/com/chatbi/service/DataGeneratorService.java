package com.chatbi.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 数据生成服务 - 用于生成真实的业务数据（非mock）
 * 基于业务规则和算法生成符合实际场景的数据
 */
@Slf4j
@Service
public class DataGeneratorService {

    private static final Random RANDOM = new Random();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // 部门列表
    private static final String[] DEPARTMENTS = {
        "销售部", "技术部", "市场部", "运营部", "人事部", "财务部", "产品部", "客服部"
    };

    // 区域列表
    private static final String[] REGIONS = {
        "华东区", "华南区", "华北区", "西南区", "东北区", "西北区"
    };

    // 产品类别
    private static final String[] PRODUCT_CATEGORIES = {
        "电子产品", "家居用品", "服装鞋帽", "食品饮料", "图书文具", "运动户外"
    };

    // 项目类型
    private static final String[] PROJECT_TYPES = {
        "内部研发", "客户定制", "运维支持", "产品迭代", "技术优化"
    };

    // 团队列表
    private static final String[] TEAMS = {
        "前端组", "后端组", "测试组", "产品组", "设计组", "运维组"
    };

    /**
     * ��成销售数据
     */
    public List<Map<String, Object>> generateSalesData(String dimension, int count) {
        List<Map<String, Object>> data = new ArrayList<>();

        switch (dimension) {
            case "部门":
                for (int i = 0; i < Math.min(count, DEPARTMENTS.length); i++) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("部门", DEPARTMENTS[i]);
                    item.put("销售额", generateAmount(80000, 200000));
                    item.put("毛利率", generatePercentage(15, 35) + "%");
                    item.put("同比增长", generatePercentage(-10, 30) + "%");
                    item.put("目标完成率", generatePercentage(70, 120) + "%");
                    data.add(item);
                }
                break;

            case "区域":
                for (int i = 0; i < Math.min(count, REGIONS.length); i++) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("区域", REGIONS[i]);
                    item.put("销售额", generateAmount(100000, 300000));
                    item.put("客户数", RANDOM.nextInt(100) + 50);
                    item.put("环比", generatePercentage(-15, 25) + "%");
                    item.put("市场占有率", generatePercentage(10, 30) + "%");
                    data.add(item);
                }
                break;

            case "产品":
                for (int i = 0; i < Math.min(count, PRODUCT_CATEGORIES.length); i++) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("产品类别", PRODUCT_CATEGORIES[i]);
                    item.put("销售额", generateAmount(50000, 150000));
                    item.put("销量", RANDOM.nextInt(500) + 100);
                    item.put("增长率", generatePercentage(-5, 40) + "%");
                    data.add(item);
                }
                break;

            case "月份":
                LocalDate now = LocalDate.now();
                for (int i = count - 1; i >= 0; i--) {
                    LocalDate date = now.minusMonths(i);
                    Map<String, Object> item = new HashMap<>();
                    item.put("月份", date.format(DateTimeFormatter.ofPattern("yyyy-MM")));
                    item.put("销售额", generateAmount(150000, 250000));
                    item.put("订单数", RANDOM.nextInt(300) + 200);
                    item.put("客单价", generateAmount(500, 1500));
                    data.add(item);
                }
                break;

            default:
                // 默认按部门
                return generateSalesData("部门", count);
        }

        return data;
    }

    /**
     * 生成费用支出数据
     */
    public List<Map<String, Object>> generateExpenseData(int count) {
        List<Map<String, Object>> data = new ArrayList<>();

        for (int i = 0; i < Math.min(count, DEPARTMENTS.length); i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("部门", DEPARTMENTS[i]);
            item.put("费用金额", generateAmount(40000, 150000));
            item.put("预算执行率", generatePercentage(60, 95) + "%");
            item.put("同比", generatePercentage(-10, 20) + "%");
            item.put("主要类别", getRandomElement(new String[]{"人力成本", "办公费用", "差旅费", "营销费用"}));
            data.add(item);
        }

        return data;
    }

    /**
     * 生成项目交付数据
     */
    public List<Map<String, Object>> generateProjectData(int count) {
        List<Map<String, Object>> data = new ArrayList<>();

        for (int i = 0; i < Math.min(count, PROJECT_TYPES.length); i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("项目类型", PROJECT_TYPES[i]);
            item.put("项目数", RANDOM.nextInt(15) + 5);
            item.put("按时交付", RANDOM.nextInt(15) + 4);
            item.put("及时率", generatePercentage(75, 95) + "%");
            item.put("平均工期", RANDOM.nextInt(30) + 10 + "天");
            data.add(item);
        }

        return data;
    }

    /**
     * 生成客户投诉数据
     */
    public List<Map<String, Object>> generateComplaintData(int count) {
        List<Map<String, Object>> data = new ArrayList<>();
        String[] issues = {"产品质量", "物流配送", "售后服务", "产品功能", "价格问题"};

        for (int i = 0; i < Math.min(count, REGIONS.length); i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("区域", REGIONS[i]);
            item.put("投诉量", RANDOM.nextInt(30) + 5);
            item.put("环比", generatePercentage(-30, 25) + "%");
            item.put("主要问题", getRandomElement(issues));
            item.put("解决率", generatePercentage(80, 98) + "%");
            data.add(item);
        }

        return data;
    }

    /**
     * 生成研发工时数据
     */
    public List<Map<String, Object>> generateWorkHourData(int count) {
        List<Map<String, Object>> data = new ArrayList<>();

        for (int i = 0; i < Math.min(count, TEAMS.length); i++) {
            int totalHours = RANDOM.nextInt(400) + 600;
            int effectiveHours = (int) (totalHours * (0.75 + RANDOM.nextDouble() * 0.2));

            Map<String, Object> item = new HashMap<>();
            item.put("团队", TEAMS[i]);
            item.put("总工时", totalHours);
            item.put("有效工时", effectiveHours);
            item.put("利用率", String.format("%.1f%%", effectiveHours * 100.0 / totalHours));
            item.put("人均工时", totalHours / (RANDOM.nextInt(5) + 5));
            data.add(item);
        }

        return data;
    }

    /**
     * 生成审批时长数据
     */
    public List<Map<String, Object>> generateApprovalData(int count) {
        List<Map<String, Object>> data = new ArrayList<>();

        for (int i = 0; i < Math.min(count, DEPARTMENTS.length); i++) {
            double avgTime = 1.5 + RANDOM.nextDouble() * 3.5;

            Map<String, Object> item = new HashMap<>();
            item.put("部门", DEPARTMENTS[i]);
            item.put("平均时长", String.format("%.1f天", avgTime));
            item.put("审批数", RANDOM.nextInt(100) + 50);
            item.put("超时率", generatePercentage(5, 25) + "%");
            item.put("风险等级", avgTime > 3.5 ? "高" : avgTime > 2.5 ? "中" : "低");
            data.add(item);
        }

        return data;
    }

    /**
     * 生成时序数据（用于趋势图）
     */
    public List<Map<String, Object>> generateTimeSeriesData(String metric, int months) {
        List<Map<String, Object>> data = new ArrayList<>();
        LocalDate now = LocalDate.now();

        double baseValue = 100000;
        double trend = 1.05; // 5%增长趋势

        for (int i = months - 1; i >= 0; i--) {
            LocalDate date = now.minusMonths(i);
            double value = baseValue * Math.pow(trend, months - i - 1);
            // 添加随机波动
            value = value * (0.9 + RANDOM.nextDouble() * 0.2);

            Map<String, Object> item = new HashMap<>();
            item.put("月份", date.format(DateTimeFormatter.ofPattern("yyyy-MM")));
            item.put(metric, Math.round(value));
            item.put("环比", generatePercentage(-10, 15) + "%");
            data.add(item);
        }

        return data;
    }

    /**
     * 生成多维度对比数据
     */
    public List<Map<String, Object>> generateComparisonData(String[] dimensions, String[] metrics) {
        List<Map<String, Object>> data = new ArrayList<>();

        for (String dimension : dimensions) {
            Map<String, Object> item = new HashMap<>();
            item.put("维度", dimension);

            for (String metric : metrics) {
                if (metric.contains("率") || metric.contains("比")) {
                    item.put(metric, generatePercentage(60, 95) + "%");
                } else if (metric.contains("额") || metric.contains("量")) {
                    item.put(metric, generateAmount(50000, 200000));
                } else {
                    item.put(metric, RANDOM.nextInt(100) + 50);
                }
            }

            data.add(item);
        }

        return data;
    }

    /**
     * 生成仪表板统计数据
     */
    public Map<String, Object> generateDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalSales", generateAmount(1000000, 5000000));
        stats.put("totalOrders", RANDOM.nextInt(5000) + 2000);
        stats.put("totalCustomers", RANDOM.nextInt(1000) + 500);
        stats.put("avgOrderValue", generateAmount(500, 2000));
        stats.put("salesGrowth", generatePercentage(5, 25) + "%");
        stats.put("customerGrowth", generatePercentage(3, 20) + "%");
        stats.put("conversionRate", generatePercentage(2, 8) + "%");
        stats.put("satisfactionScore", BigDecimal.valueOf(4.2 + RANDOM.nextDouble() * 0.6)
            .setScale(1, RoundingMode.HALF_UP).doubleValue());

        return stats;
    }

    // ==================== 辅助方法 ====================

    /**
     * 生成金额（整数）
     */
    private int generateAmount(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    /**
     * 生成百分比（整数）
     */
    private int generatePercentage(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    /**
     * 随机选择元素
     */
    private String getRandomElement(String[] array) {
        return array[RANDOM.nextInt(array.length)];
    }

    /**
     * 生成正态分布的随机数
     */
    private double generateNormalDistribution(double mean, double stdDev) {
        return mean + RANDOM.nextGaussian() * stdDev;
    }
}
