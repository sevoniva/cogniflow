package com.chatbi.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 企业级图表目录服务
 * 统一维护图表家族与样式变体，供 API、推荐引擎和前端能力发现复用。
 */
@Service
public class EnterpriseChartCatalogService {

    private static final List<String> FAMILIES = List.of(
        "bar",
        "line",
        "area",
        "pie",
        "scatter",
        "radar",
        "gauge",
        "funnel",
        "treemap",
        "sunburst",
        "sankey",
        "heatmap",
        "candlestick",
        "boxplot",
        "waterfall",
        "graph",
        "tree"
    );

    private static final List<String> VARIANTS = List.of(
        "classic",
        "enterprise",
        "minimal",
        "contrast",
        "soft",
        "dark-grid",
        "light-grid"
    );

    private static final Set<String> SPECIAL_TYPES = Set.of("table", "filter", "number", "barHorizontal", "map");

    public List<Map<String, Object>> getCatalog() {
        List<Map<String, Object>> catalog = new ArrayList<>();
        for (String family : FAMILIES) {
            for (String variant : VARIANTS) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("type", family + "." + variant);
                item.put("family", family);
                item.put("variant", variant);
                item.put("displayName", family + "-" + variant);
                catalog.add(item);
            }
        }
        return catalog;
    }

    public Map<String, Object> getSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", getCatalog().size());
        summary.put("familyCount", FAMILIES.size());
        summary.put("families", FAMILIES);
        summary.put("variants", VARIANTS);
        return summary;
    }

    public List<String> getFamilies() {
        return FAMILIES;
    }

    public List<String> getVariants() {
        return VARIANTS;
    }

    public List<Map<String, Object>> getFeaturedTypes(int limit) {
        int safeLimit = Math.max(1, limit);
        List<Map<String, Object>> catalog = getCatalog();
        if (safeLimit >= catalog.size()) {
            return catalog;
        }
        return catalog.subList(0, safeLimit);
    }

    public String toEnterpriseType(String chartType) {
        if (chartType == null || chartType.isBlank()) {
            return "line.enterprise";
        }

        String normalized = chartType.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains(".")) {
            String[] parts = normalized.split("\\.");
            if (parts.length == 2 && FAMILIES.contains(parts[0]) && VARIANTS.contains(parts[1])) {
                return normalized;
            }
            return "line.enterprise";
        }

        if (SPECIAL_TYPES.contains(normalized)) {
            return normalized;
        }

        if (FAMILIES.contains(normalized)) {
            return normalized + ".enterprise";
        }

        return "line.enterprise";
    }

    public Map<String, String> getFamilyAliasMap() {
        Map<String, String> aliasMap = new LinkedHashMap<>();
        for (String family : FAMILIES) {
            aliasMap.put(family, family + ".enterprise");
        }
        aliasMap.put("barHorizontal", "bar.enterprise");
        aliasMap.put("number", "gauge.enterprise");
        aliasMap.put("map", "heatmap.enterprise");
        aliasMap.put("table", "table");
        aliasMap.put("filter", "filter");
        return aliasMap;
    }

    public Set<String> getAllSupportedTypes() {
        Set<String> supported = new LinkedHashSet<>();
        getCatalog().forEach(item -> supported.add(String.valueOf(item.get("type"))));
        supported.addAll(SPECIAL_TYPES);
        return supported;
    }
}

