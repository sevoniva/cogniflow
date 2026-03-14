package com.chatbi.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 智能图表推荐服务
 * 根据数据特征自动推荐最合适的图表类型
 */
@Slf4j
@Service
public class ChartRecommendationService {

    /**
     * 推荐图表类型
     */
    public ChartRecommendation recommendChart(String query, List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) {
            return withEnterpriseType(new ChartRecommendation("table", "数据为空，使用表格展示", new HashMap<>()));
        }

        Map<String, Object> firstRow = data.get(0);
        int rowCount = data.size();
        int columnCount = firstRow.size();

        // 分析数据特征
        DataCharacteristics characteristics = analyzeData(data);

        // 根据特征推荐图表
        return withEnterpriseType(selectChartType(query, characteristics, rowCount, columnCount));
    }

    private ChartRecommendation withEnterpriseType(ChartRecommendation recommendation) {
        Map<String, Object> config = new HashMap<>(recommendation.getConfig() == null ? Map.of() : recommendation.getConfig());
        config.put("enterpriseChartType", toEnterpriseChartType(recommendation.getChartType()));
        return new ChartRecommendation(recommendation.getChartType(), recommendation.getReason(), config);
    }

    private String toEnterpriseChartType(String chartType) {
        if (chartType == null || chartType.isBlank()) {
            return "line.enterprise";
        }
        return switch (chartType) {
            case "bar", "line", "area", "pie", "scatter", "radar", "gauge", "funnel", "treemap", "sunburst",
                "sankey", "heatmap", "candlestick", "boxplot", "waterfall", "graph", "tree" -> chartType + ".enterprise";
            case "barHorizontal" -> "bar.enterprise";
            case "number" -> "gauge.enterprise";
            case "map" -> "heatmap.enterprise";
            case "table", "filter" -> chartType;
            default -> "line.enterprise";
        };
    }

    /**
     * 分析数据特征
     */
    private DataCharacteristics analyzeData(List<Map<String, Object>> data) {
        DataCharacteristics characteristics = new DataCharacteristics();
        Map<String, Object> firstRow = data.get(0);

        for (Map.Entry<String, Object> entry : firstRow.entrySet()) {
            String columnName = entry.getKey();
            Object value = entry.getValue();

            // 判断列类型
            if (isNumeric(value)) {
                characteristics.numericColumns.add(columnName);
            } else if (isDate(value)) {
                characteristics.dateColumns.add(columnName);
            } else {
                characteristics.categoryColumns.add(columnName);
            }
        }

        // 检查是否有时间序列
        characteristics.hasTimeSeries = !characteristics.dateColumns.isEmpty() ||
            characteristics.categoryColumns.stream().anyMatch(col ->
                col.contains("date") || col.contains("time") ||
                col.contains("month") || col.contains("year") ||
                col.contains("日期") || col.contains("时间") ||
                col.contains("月份") || col.contains("年份"));

        // 检查数据分布
        if (!characteristics.numericColumns.isEmpty()) {
            String firstNumericCol = characteristics.numericColumns.get(0);
            List<Double> values = data.stream()
                .map(row -> row.get(firstNumericCol))
                .filter(this::isNumeric)
                .map(v -> ((Number) v).doubleValue())
                .collect(Collectors.toList());

            if (values.size() > 1) {
                double max = values.stream().mapToDouble(Double::doubleValue).max().orElse(0);
                double min = values.stream().mapToDouble(Double::doubleValue).min().orElse(0);
                characteristics.valueRange = max - min;
            }
        }

        return characteristics;
    }

    /**
     * 选择图表类型
     */
    private ChartRecommendation selectChartType(String query, DataCharacteristics characteristics,
                                                 int rowCount, int columnCount) {
        Map<String, Object> config = new HashMap<>();

        // 1. 单值结果 - 数字卡片
        if (rowCount == 1 && columnCount == 1) {
            return new ChartRecommendation("number", "单一数值，使用数字卡片展示", config);
        }

        // 2. 时间序列数据 - 折线图
        if (characteristics.hasTimeSeries && !characteristics.numericColumns.isEmpty()) {
            config.put("xAxis", characteristics.dateColumns.isEmpty() ?
                characteristics.categoryColumns.get(0) : characteristics.dateColumns.get(0));
            config.put("yAxis", characteristics.numericColumns.get(0));
            config.put("smooth", true);
            return new ChartRecommendation("line", "时间序列数据，使用折线图展示趋势", config);
        }

        // 3. 大数据集 - 表格
        if (rowCount > 50 || columnCount > 5) {
            config.put("pagination", true);
            config.put("pageSize", 20);
            return new ChartRecommendation("table", "数据量较大或维度较多，使用表格展示", config);
        }

        // 4. 排名数据 - 条形图
        if (query.contains("排名") || query.contains("TOP") || query.contains("前") ||
            query.contains("最高") || query.contains("最低")) {

            if (!characteristics.categoryColumns.isEmpty() && !characteristics.numericColumns.isEmpty()) {
                config.put("xAxis", characteristics.numericColumns.get(0));
                config.put("yAxis", characteristics.categoryColumns.get(0));
                config.put("sort", "desc");
                return new ChartRecommendation("barHorizontal", "排名数据，使用横向条形图展示", config);
            }
        }

        // 5. 分类对比 - 柱状图或饼图
        if (columnCount == 2 && !characteristics.categoryColumns.isEmpty() &&
            !characteristics.numericColumns.isEmpty()) {

            config.put("xAxis", characteristics.categoryColumns.get(0));
            config.put("yAxis", characteristics.numericColumns.get(0));

            // 少量分类用饼图
            if (rowCount <= 6 && query.contains("占比") || query.contains("比例") ||
                query.contains("分布") || query.contains("构成")) {
                return new ChartRecommendation("pie", "分类占比数据，使用饼图展示", config);
            }

            // 多分类用柱状图
            return new ChartRecommendation("bar", "分类对比数据，使用柱状图展示", config);
        }

        // 6. 地理数据 - 地图
        if (characteristics.categoryColumns.stream().anyMatch(col ->
            col.contains("region") || col.contains("province") || col.contains("city") ||
            col.contains("地区") || col.contains("省份") || col.contains("城市"))) {

            config.put("region", characteristics.categoryColumns.stream()
                .filter(col -> col.contains("region") || col.contains("province") ||
                    col.contains("地区") || col.contains("省份"))
                .findFirst()
                .orElse(characteristics.categoryColumns.get(0)));

            if (!characteristics.numericColumns.isEmpty()) {
                config.put("value", characteristics.numericColumns.get(0));
            }

            return new ChartRecommendation("map", "地理分布数据，使用地图展示", config);
        }

        // 7. 多维数据 - 散点图
        if (characteristics.numericColumns.size() >= 2) {
            if (rowCount <= 50 && columnCount <= 4) {
                config.put("xAxis", characteristics.numericColumns.get(0));
                config.put("yAxis", characteristics.numericColumns.get(1));
                return new ChartRecommendation("scatter", "多维数值数据，使用散点图展示相关性", config);
            }
        }

        // 8. 多指标对比 - 雷达图
        if (characteristics.numericColumns.size() >= 3 && rowCount <= 10) {
            config.put("indicators", characteristics.numericColumns);
            config.put("dimension", characteristics.categoryColumns.isEmpty() ?
                "item" : characteristics.categoryColumns.get(0));
            return new ChartRecommendation("radar", "多指标对比，使用雷达图展示", config);
        }

        // 9. 默认 - 柱状图
        if (!characteristics.categoryColumns.isEmpty() && !characteristics.numericColumns.isEmpty()) {
            config.put("xAxis", characteristics.categoryColumns.get(0));
            config.put("yAxis", characteristics.numericColumns.get(0));
            return new ChartRecommendation("bar", "通用数据，使用柱状图展示", config);
        }

        // 10. 兜底 - 表格
        return new ChartRecommendation("table", "数据结构复杂，使用表格展示", config);
    }

    /**
     * 判断是否为数值
     */
    private boolean isNumeric(Object value) {
        return value instanceof Number;
    }

    /**
     * 判断是否为日期
     */
    private boolean isDate(Object value) {
        return value instanceof Date || value instanceof LocalDate || value instanceof LocalDateTime;
    }

    /**
     * 数据特征
     */
    private static class DataCharacteristics {
        List<String> numericColumns = new ArrayList<>();
        List<String> categoryColumns = new ArrayList<>();
        List<String> dateColumns = new ArrayList<>();
        boolean hasTimeSeries = false;
        double valueRange = 0;
    }

    /**
     * 图表推荐结果
     */
    public static class ChartRecommendation {
        private String chartType;
        private String reason;
        private Map<String, Object> config;

        public ChartRecommendation(String chartType, String reason, Map<String, Object> config) {
            this.chartType = chartType;
            this.reason = reason;
            this.config = config;
        }

        public String getChartType() {
            return chartType;
        }

        public String getReason() {
            return reason;
        }

        public Map<String, Object> getConfig() {
            return config;
        }
    }
}
