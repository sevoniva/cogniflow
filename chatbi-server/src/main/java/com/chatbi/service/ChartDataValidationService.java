package com.chatbi.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 图表数据验证服务。
 * 用真实业务数据校验“图表类型是否具备可渲染数据形态”。
 */
@Service
public class ChartDataValidationService {

    private final EnterpriseChartCatalogService enterpriseChartCatalogService;
    private final BusinessInsightService businessInsightService;

    public ChartDataValidationService(
        EnterpriseChartCatalogService enterpriseChartCatalogService,
        BusinessInsightService businessInsightService
    ) {
        this.enterpriseChartCatalogService = enterpriseChartCatalogService;
        this.businessInsightService = businessInsightService;
    }

    public Map<String, Object> getValidationSummary(int limit, boolean onlyFailed) {
        List<Map<String, Object>> catalog = enterpriseChartCatalogService.getCatalog();
        Map<String, FamilyValidationSnapshot> familySnapshots = new LinkedHashMap<>();
        for (String family : enterpriseChartCatalogService.getFamilies()) {
            familySnapshots.put(family, validateFamily(family));
        }

        List<Map<String, Object>> typeValidations = new ArrayList<>();
        for (Map<String, Object> item : catalog) {
            String family = String.valueOf(item.get("family"));
            FamilyValidationSnapshot snapshot = familySnapshots.get(family);
            if (snapshot == null) {
                snapshot = validateFamily(family);
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("type", item.get("type"));
            row.put("family", family);
            row.put("variant", item.get("variant"));
            row.put("valid", snapshot.valid());
            row.put("sampleRows", snapshot.sampleRows());
            row.put("dataPoints", snapshot.dataPoints());
            row.put("reason", snapshot.reason());
            typeValidations.add(row);
        }

        int totalTypes = typeValidations.size();
        int validatedTypes = (int) typeValidations.stream()
            .filter(item -> Boolean.TRUE.equals(item.get("valid")))
            .count();

        List<Map<String, Object>> filtered = typeValidations;
        if (onlyFailed) {
            filtered = typeValidations.stream()
                .filter(item -> !Boolean.TRUE.equals(item.get("valid")))
                .toList();
        }
        if (limit > 0 && filtered.size() > limit) {
            filtered = filtered.subList(0, limit);
        }

        List<Map<String, Object>> familyValidation = familySnapshots.entrySet().stream()
            .map(entry -> {
                FamilyValidationSnapshot snapshot = entry.getValue();
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("family", entry.getKey());
                row.put("validated", snapshot.valid());
                row.put("sampleRows", snapshot.sampleRows());
                row.put("dataPoints", snapshot.dataPoints());
                row.put("reason", snapshot.reason());
                return row;
            })
            .sorted(Comparator.comparing(entry -> String.valueOf(entry.get("family"))))
            .toList();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("validatedTypes", validatedTypes);
        summary.put("totalTypes", totalTypes);
        summary.put("coverageRate", totalTypes == 0 ? 100.0 : round((double) validatedTypes * 100 / totalTypes));
        summary.put("validatedFamilies", familyValidation.stream().filter(item -> Boolean.TRUE.equals(item.get("validated"))).count());
        summary.put("totalFamilies", familyValidation.size());
        summary.put("familyValidation", familyValidation);
        summary.put("typeValidation", filtered);
        summary.put("onlyFailed", onlyFailed);
        summary.put("generatedAt", Instant.now().toString());
        return summary;
    }

    private FamilyValidationSnapshot validateFamily(String family) {
        List<Map<String, Object>> rows = loadFamilyRows(family);
        if (rows == null || rows.isEmpty()) {
            return new FamilyValidationSnapshot(false, 0, 0, "未查询到可用数据");
        }

        FieldMapping mapping = resolveFieldMapping(rows);
        if (mapping.dimensionField() == null || mapping.metricField() == null) {
            return new FamilyValidationSnapshot(false, rows.size(), 0, "缺少维度或数值字段");
        }

        int dataPoints = computeDataPointsForFamily(family, rows, mapping);
        if (dataPoints <= 0) {
            return new FamilyValidationSnapshot(false, rows.size(), 0, "当前数据无法生成该图表族渲染结构");
        }

        return new FamilyValidationSnapshot(true, rows.size(), dataPoints, "通过");
    }

    private List<Map<String, Object>> loadFamilyRows(String family) {
        return switch (family) {
            case "line", "area", "candlestick", "boxplot", "heatmap" ->
                businessInsightService.getChartData("timeseries", "月份");
            case "gauge" ->
                businessInsightService.getChartData("workhour", "成员");
            case "funnel", "sankey", "graph", "tree" ->
                businessInsightService.getChartData("complaint", "区域");
            default ->
                businessInsightService.getChartData("sales", "区域");
        };
    }

    private FieldMapping resolveFieldMapping(List<Map<String, Object>> rows) {
        Set<String> orderedFields = new LinkedHashSet<>();
        rows.forEach(row -> orderedFields.addAll(row.keySet()));

        String metricField = null;
        String dimensionField = null;
        for (String field : orderedFields) {
            if (metricField == null && hasNumeric(rows, field)) {
                metricField = field;
            }
            if (dimensionField == null && !hasNumeric(rows, field)) {
                dimensionField = field;
            }
        }
        if (dimensionField == null && !orderedFields.isEmpty()) {
            dimensionField = orderedFields.iterator().next();
        }
        return new FieldMapping(dimensionField, metricField);
    }

    private boolean hasNumeric(List<Map<String, Object>> rows, String field) {
        return rows.stream().anyMatch(row -> toNumber(row.get(field)) != null);
    }

    private Integer toNumber(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue()).intValue();
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        text = text.replace("%", "").replace(",", "");
        try {
            return new BigDecimal(text).intValue();
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private int computeDataPointsForFamily(String family, List<Map<String, Object>> rows, FieldMapping mapping) {
        long numericRows = rows.stream()
            .filter(row -> toNumber(row.get(mapping.metricField())) != null)
            .count();

        return switch (family) {
            case "sankey", "graph", "tree" -> numericRows >= 2 ? (int) numericRows : 0;
            case "gauge" -> numericRows >= 1 ? 1 : 0;
            case "radar", "heatmap", "candlestick", "boxplot" -> numericRows >= 3 ? (int) numericRows : 0;
            default -> (int) numericRows;
        };
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record FieldMapping(
        String dimensionField,
        String metricField
    ) {}

    private record FamilyValidationSnapshot(
        boolean valid,
        int sampleRows,
        int dataPoints,
        String reason
    ) {}
}
