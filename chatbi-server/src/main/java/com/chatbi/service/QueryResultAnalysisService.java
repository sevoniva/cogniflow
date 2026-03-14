package com.chatbi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 查询结果智能解读服务
 * 自动分析查询结果，生成文字解读
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryResultAnalysisService {

    /**
     * 生成智能解读
     */
    public String generateInterpretation(String query, List<Map<String, Object>> data, boolean isFollowUp) {
        if (data == null || data.isEmpty()) {
            return "未查询到相关数据";
        }

        StringBuilder interpretation = new StringBuilder();

        if (isFollowUp) {
            interpretation.append("根据您的追问，");
        }

        // 1. 基础统计
        interpretation.append(analyzeBasicStats(data));

        // 2. 数值分析
        String numericAnalysis = analyzeNumericData(data);
        if (!numericAnalysis.isEmpty()) {
            interpretation.append("\n\n").append(numericAnalysis);
        }

        // 3. 趋势分析
        String trendAnalysis = analyzeTrend(data);
        if (!trendAnalysis.isEmpty()) {
            interpretation.append("\n\n").append(trendAnalysis);
        }

        // 4. 异常检测
        String anomalyAnalysis = detectAnomalies(data);
        if (!anomalyAnalysis.isEmpty()) {
            interpretation.append("\n\n").append(anomalyAnalysis);
        }

        // 5. 建议
        String suggestions = generateSuggestions(query, data);
        if (!suggestions.isEmpty()) {
            interpretation.append("\n\n").append(suggestions);
        }

        return interpretation.toString();
    }

    /**
     * 基础统计分析
     */
    private String analyzeBasicStats(List<Map<String, Object>> data) {
        StringBuilder result = new StringBuilder();

        int rowCount = data.size();
        result.append("查询到 ").append(rowCount).append(" 条数据");

        // 单值结果
        if (rowCount == 1 && data.get(0).size() == 1) {
            Object value = data.get(0).values().iterator().next();
            result.append("，结果为：").append(formatNumber(value));
        }
        // 聚合结果
        else if (rowCount == 1 && data.get(0).size() > 1) {
            result.append("，聚合结果如下：");
        }
        // 列表结果
        else if (rowCount <= 10) {
            result.append("。");
        }
        // 大数据集
        else {
            result.append("，以下是前 10 条记录。");
        }

        return result.toString();
    }

    /**
     * 数值数据分析
     */
    private String analyzeNumericData(List<Map<String, Object>> data) {
        if (data.size() <= 1) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        Map<String, Object> firstRow = data.get(0);

        // 找到数值列
        for (Map.Entry<String, Object> entry : firstRow.entrySet()) {
            String columnName = entry.getKey();
            Object value = entry.getValue();

            if (isNumeric(value)) {
                // 提取该列的所有数值
                List<Double> values = data.stream()
                    .map(row -> row.get(columnName))
                    .filter(this::isNumeric)
                    .map(this::toDouble)
                    .collect(Collectors.toList());

                if (values.size() > 1) {
                    // 计算统计指标
                    double sum = values.stream().mapToDouble(Double::doubleValue).sum();
                    double avg = sum / values.size();
                    double max = values.stream().mapToDouble(Double::doubleValue).max().orElse(0);
                    double min = values.stream().mapToDouble(Double::doubleValue).min().orElse(0);

                    result.append("📊 ").append(columnName).append("分析：\n");
                    result.append("- 总计：").append(formatNumber(sum)).append("\n");
                    result.append("- 平均值：").append(formatNumber(avg)).append("\n");
                    result.append("- 最大值：").append(formatNumber(max)).append("\n");
                    result.append("- 最小值：").append(formatNumber(min));

                    // 只分析第一个数值列
                    break;
                }
            }
        }

        return result.toString();
    }

    /**
     * 趋势分析
     */
    private String analyzeTrend(List<Map<String, Object>> data) {
        if (data.size() < 3) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        Map<String, Object> firstRow = data.get(0);

        // 找到数值列
        for (Map.Entry<String, Object> entry : firstRow.entrySet()) {
            String columnName = entry.getKey();
            Object value = entry.getValue();

            if (isNumeric(value)) {
                List<Double> values = data.stream()
                    .map(row -> row.get(columnName))
                    .filter(this::isNumeric)
                    .map(this::toDouble)
                    .collect(Collectors.toList());

                if (values.size() >= 3) {
                    String trend = detectTrend(values);
                    if (!trend.isEmpty()) {
                        result.append("📈 趋势分析：").append(trend);
                        break;
                    }
                }
            }
        }

        return result.toString();
    }

    /**
     * 检测趋势
     */
    private String detectTrend(List<Double> values) {
        if (values.size() < 3) {
            return "";
        }

        // 计算增长率
        List<Double> growthRates = new ArrayList<>();
        for (int i = 1; i < values.size(); i++) {
            double prev = values.get(i - 1);
            double curr = values.get(i);
            if (prev != 0) {
                double rate = ((curr - prev) / prev) * 100;
                growthRates.add(rate);
            }
        }

        if (growthRates.isEmpty()) {
            return "";
        }

        double avgGrowth = growthRates.stream().mapToDouble(Double::doubleValue).average().orElse(0);

        if (avgGrowth > 5) {
            return "数据呈上升趋势，平均增长率为 " + String.format("%.1f%%", avgGrowth);
        } else if (avgGrowth < -5) {
            return "数据呈下降趋势，平均下降率为 " + String.format("%.1f%%", Math.abs(avgGrowth));
        } else {
            return "数据相对稳定，波动较小";
        }
    }

    /**
     * 异常检测
     */
    private String detectAnomalies(List<Map<String, Object>> data) {
        if (data.size() < 5) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        Map<String, Object> firstRow = data.get(0);

        // 找到数值列
        for (Map.Entry<String, Object> entry : firstRow.entrySet()) {
            String columnName = entry.getKey();
            Object value = entry.getValue();

            if (isNumeric(value)) {
                List<Double> values = data.stream()
                    .map(row -> row.get(columnName))
                    .filter(this::isNumeric)
                    .map(this::toDouble)
                    .collect(Collectors.toList());

                if (values.size() >= 5) {
                    List<String> anomalies = findAnomalies(values, data, columnName);
                    if (!anomalies.isEmpty()) {
                        result.append("⚠️ 异常提醒：\n");
                        anomalies.forEach(anomaly -> result.append("- ").append(anomaly).append("\n"));
                        break;
                    }
                }
            }
        }

        return result.toString().trim();
    }

    /**
     * 查找异常值
     */
    private List<String> findAnomalies(List<Double> values, List<Map<String, Object>> data, String columnName) {
        List<String> anomalies = new ArrayList<>();

        // 计算平均值和标准差
        double avg = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double variance = values.stream()
            .mapToDouble(v -> Math.pow(v - avg, 2))
            .average()
            .orElse(0);
        double stdDev = Math.sqrt(variance);

        // 3σ原则检测异常
        for (int i = 0; i < values.size(); i++) {
            double value = values.get(i);
            double zScore = Math.abs((value - avg) / stdDev);

            if (zScore > 2) { // 超过2个标准差
                Map<String, Object> row = data.get(i);
                String label = getRowLabel(row);
                String anomalyDesc = label + " 的 " + columnName + " 为 " + formatNumber(value) +
                    "，" + (value > avg ? "明显高于" : "明显低于") + "平均值";
                anomalies.add(anomalyDesc);
            }
        }

        return anomalies.stream().limit(3).collect(Collectors.toList());
    }

    /**
     * 获取行标签
     */
    private String getRowLabel(Map<String, Object> row) {
        // 尝试找到名称、标题等字段
        for (String key : Arrays.asList("name", "title", "label", "category", "region", "product")) {
            if (row.containsKey(key)) {
                return String.valueOf(row.get(key));
            }
        }
        // 返回第一个非数值字段
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (!isNumeric(entry.getValue())) {
                return String.valueOf(entry.getValue());
            }
        }
        return "某项";
    }

    /**
     * 生成建议
     */
    private String generateSuggestions(String query, List<Map<String, Object>> data) {
        StringBuilder result = new StringBuilder();

        // 根据查询内容生成建议
        if (query.contains("销售") || query.contains("营收")) {
            result.append("💡 建议：\n");
            result.append("- 关注销售额变化趋势，及时调整策略\n");
            result.append("- 分析高销售额地区的成功经验\n");
            result.append("- 对低销售额地区制定改进计划");
        } else if (query.contains("客户") || query.contains("用户")) {
            result.append("💡 建议：\n");
            result.append("- 加强客户关系维护，提高留存率\n");
            result.append("- 分析流失客户原因，优化服务\n");
            result.append("- 开展客户满意度调查");
        }

        return result.toString();
    }

    /**
     * 判断是否为数值
     */
    private boolean isNumeric(Object value) {
        if (value == null) {
            return false;
        }
        return value instanceof Number;
    }

    /**
     * 转换为Double
     */
    private Double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }

    /**
     * 格式化数字
     */
    private String formatNumber(Object value) {
        if (value instanceof Number) {
            double num = ((Number) value).doubleValue();
            if (num >= 100000000) {
                return String.format("%.2f亿", num / 100000000);
            } else if (num >= 10000) {
                return String.format("%.2f万", num / 10000);
            } else if (num >= 1000) {
                return String.format("%.2f千", num / 1000);
            } else {
                BigDecimal bd = new BigDecimal(num);
                bd = bd.setScale(2, RoundingMode.HALF_UP);
                return bd.toString();
            }
        }
        return value.toString();
    }
}
