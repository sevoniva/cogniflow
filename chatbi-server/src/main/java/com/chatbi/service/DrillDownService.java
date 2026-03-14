package com.chatbi.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.*;

/**
 * 下钻/上卷分析服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DrillDownService {

    private final DataSource dataSource;

    /**
     * 层级配置
     */
    @Data
    public static class HierarchyConfig {
        private String name;
        private String table;
        private List<Level> levels;
    }

    /**
     * 层级
     */
    @Data
    public static class Level {
        private String name;
        private String field;
        private String parentField;
        private int order;
    }

    /**
     * 下钻请求
     */
    @Data
    public static class DrillDownRequest {
        private String baseUrl;
        private String dimension;
        private String value;
        private String currentValue;
        private Map<String, String> filters;
    }

    /**
     * 下钻响应
     */
    @Data
    public static class DrillDownResponse {
        private String dimension;
        private String value;
        private List<Map<String, Object>> data;
        private List<DrillPath> paths;
        private boolean hasChildren;
    }

    /**
     * 下钻路径
     */
    @Data
    public static class DrillPath {
        private String level;
        private String field;
        private String value;
    }

    // 预定义的层级配置
    private static final Map<String, HierarchyConfig> HIERARCHIES = new HashMap<>();

    static {
        // 时间层级
        HierarchyConfig timeHierarchy = new HierarchyConfig();
        timeHierarchy.setName("时间");
        timeHierarchy.setTable("date_dim");
        List<Level> timeLevels = new ArrayList<>();
        timeLevels.add(createLevel("年", "year", null, 1));
        timeLevels.add(createLevel("季度", "quarter", "year", 2));
        timeLevels.add(createLevel("月", "month", "quarter", 3));
        timeLevels.add(createLevel("日", "day", "month", 4));
        timeHierarchy.setLevels(timeLevels);
        HIERARCHIES.put("time", timeHierarchy);

        // 地理层级
        HierarchyConfig geoHierarchy = new HierarchyConfig();
        geoHierarchy.setName("地理");
        geoHierarchy.setTable("region_dim");
        List<Level> geoLevels = new ArrayList<>();
        geoLevels.add(createLevel("国家", "country", null, 1));
        geoLevels.add(createLevel("省份", "province", "country", 2));
        geoLevels.add(createLevel("城市", "city", "province", 3));
        geoLevels.add(createLevel("区县", "district", "city", 4));
        geoHierarchy.setLevels(geoLevels);
        HIERARCHIES.put("geo", geoHierarchy);

        // 产品层级
        HierarchyConfig productHierarchy = new HierarchyConfig();
        productHierarchy.setName("产品");
        productHierarchy.setTable("product_dim");
        List<Level> productLevels = new ArrayList<>();
        productLevels.add(createLevel("品类", "category", null, 1));
        productLevels.add(createLevel("子品类", "subcategory", "category", 2));
        productLevels.add(createLevel("品牌", "brand", "subcategory", 3));
        productLevels.add(createLevel("产品", "product", "brand", 4));
        productHierarchy.setLevels(productLevels);
        HIERARCHIES.put("product", productHierarchy);

        // 组织层级
        HierarchyConfig orgHierarchy = new HierarchyConfig();
        orgHierarchy.setName("组织");
        orgHierarchy.setTable("org_dim");
        List<Level> orgLevels = new ArrayList<>();
        orgLevels.add(createLevel("集团", "group", null, 1));
        orgLevels.add(createLevel("大区", "region", "group", 2));
        orgLevels.add(createLevel("分公司", "branch", "region", 3));
        orgLevels.add(createLevel("部门", "department", "branch", 4));
        orgHierarchy.setLevels(orgLevels);
        HIERARCHIES.put("org", orgHierarchy);
    }

    private static Level createLevel(String name, String field, String parentField, int order) {
        Level level = new Level();
        level.setName(name);
        level.setField(field);
        level.setParentField(parentField);
        level.setOrder(order);
        return level;
    }

    /**
     * 执行下钻分析
     */
    public DrillDownResponse drillDown(DrillDownRequest request) {
        String dimension = request.getDimension();
        String value = request.getValue();

        log.info("下钻分析：{} -> {}", dimension, value);

        // 获取层级配置
        HierarchyConfig hierarchy = findHierarchy(dimension);
        if (hierarchy == null) {
            log.warn("未找到层级配置：{}", dimension);
            return createErrorResponse(dimension, value);
        }

        // 构建下钻 SQL
        String drillSql = buildDrillDownSql(hierarchy, request);

        // 执行查询
        List<Map<String, Object>> data = executeQuery(drillSql);

        // 构建响应
        DrillDownResponse response = new DrillDownResponse();
        response.setDimension(dimension);
        response.setValue(value);
        response.setData(data);
        response.setHasChildren(!data.isEmpty());
        response.setPaths(buildDrillPaths(hierarchy, request));

        return response;
    }

    /**
     * 执行上卷分析
     */
    public DrillDownResponse rollUp(DrillDownRequest request) {
        String dimension = request.getDimension();
        String currentValue = request.getCurrentValue();

        log.info("上卷分析：{} <- {}", dimension, currentValue);

        // 获取层级配置
        HierarchyConfig hierarchy = findHierarchy(dimension);
        if (hierarchy == null) {
            log.warn("未找到层级配置：{}", dimension);
            return createErrorResponse(dimension, currentValue);
        }

        // 构建上卷 SQL
        String rollupSql = buildRollUpSql(hierarchy, request);

        // 执行查询
        List<Map<String, Object>> data = executeQuery(rollupSql);

        // 构建响应
        DrillDownResponse response = new DrillDownResponse();
        response.setDimension(dimension);
        response.setValue(currentValue);
        response.setData(data);
        response.setHasChildren(false);
        response.setPaths(buildRollUpPaths(hierarchy, request));

        return response;
    }

    /**
     * 获取层级选项
     */
    public List<HierarchyConfig> getHierarchies() {
        return new ArrayList<>(HIERARCHIES.values());
    }

    /**
     * 查找层级配置
     */
    private HierarchyConfig findHierarchy(String dimension) {
        // 根据维度名称查找对应的层级配置
        for (HierarchyConfig config : HIERARCHIES.values()) {
            for (Level level : config.getLevels()) {
                if (level.getField().equalsIgnoreCase(dimension)) {
                    return config;
                }
            }
        }
        return null;
    }

    /**
     * 构建下钻 SQL
     */
    private String buildDrillDownSql(HierarchyConfig hierarchy, DrillDownRequest request) {
        StringBuilder sql = new StringBuilder("SELECT ");

        // 找到当前层级
        Level currentLevel = findLevel(hierarchy, request.getDimension());
        if (currentLevel == null) {
            return "SELECT 1 WHERE 1=0";
        }

        // 找到下一级
        Level nextLevel = findNextLevel(hierarchy, currentLevel.getOrder());
        if (nextLevel == null) {
            return "SELECT 1 WHERE 1=0"; // 已经是最底层
        }

        // 构建 SELECT 子句
        sql.append(nextLevel.getField()).append(" AS dimension, ");
        sql.append("SUM(amount) AS value ");

        // 构建 FROM 和 WHERE 子句
        sql.append("FROM ").append(hierarchy.getTable());
        sql.append(" WHERE ").append(currentLevel.getField()).append(" = '").append(escapeSql(request.getValue())).append("'");

        // 添加其他过滤条件
        if (request.getFilters() != null && !request.getFilters().isEmpty()) {
            for (Map.Entry<String, String> entry : request.getFilters().entrySet()) {
                sql.append(" AND ").append(entry.getKey()).append(" = '").append(escapeSql(entry.getValue())).append("'");
            }
        }

        // GROUP BY
        sql.append(" GROUP BY ").append(nextLevel.getField());
        sql.append(" ORDER BY value DESC");

        log.info("下钻 SQL: {}", sql);
        return sql.toString();
    }

    /**
     * 构建上卷 SQL
     */
    private String buildRollUpSql(HierarchyConfig hierarchy, DrillDownRequest request) {
        StringBuilder sql = new StringBuilder("SELECT ");

        // 找到当前层级
        Level currentLevel = findLevel(hierarchy, request.getDimension());
        if (currentLevel == null) {
            return "SELECT 1 WHERE 1=0";
        }

        // 找到上一级
        Level prevLevel = findPrevLevel(hierarchy, currentLevel.getOrder());
        if (prevLevel == null) {
            return "SELECT 1 WHERE 1=0"; // 已经是最高层
        }

        // 构建 SELECT 子句
        sql.append(prevLevel.getField()).append(" AS dimension, ");
        sql.append("SUM(amount) AS value ");

        // 构建 FROM 和 WHERE 子句
        sql.append("FROM ").append(hierarchy.getTable());

        // 上卷时查询父级的汇总数据
        sql.append(" WHERE ").append(prevLevel.getField()).append(" IS NOT NULL");

        // 添加其他过滤条件
        if (request.getFilters() != null && !request.getFilters().isEmpty()) {
            for (Map.Entry<String, String> entry : request.getFilters().entrySet()) {
                sql.append(" AND ").append(entry.getKey()).append(" = '").append(escapeSql(entry.getValue())).append("'");
            }
        }

        // GROUP BY
        sql.append(" GROUP BY ").append(prevLevel.getField());
        sql.append(" ORDER BY value DESC");

        log.info("上卷 SQL: {}", sql);
        return sql.toString();
    }

    /**
     * 查找层级
     */
    private Level findLevel(HierarchyConfig hierarchy, String dimension) {
        for (Level level : hierarchy.getLevels()) {
            if (level.getField().equalsIgnoreCase(dimension)) {
                return level;
            }
        }
        return null;
    }

    /**
     * 找到下一级
     */
    private Level findNextLevel(HierarchyConfig hierarchy, int currentOrder) {
        Level nextLevel = null;
        for (Level level : hierarchy.getLevels()) {
            if (level.getOrder() == currentOrder + 1) {
                nextLevel = level;
                break;
            }
        }
        return nextLevel;
    }

    /**
     * 找到上一级
     */
    private Level findPrevLevel(HierarchyConfig hierarchy, int currentOrder) {
        Level prevLevel = null;
        for (Level level : hierarchy.getLevels()) {
            if (level.getOrder() == currentOrder - 1) {
                prevLevel = level;
                break;
            }
        }
        return prevLevel;
    }

    /**
     * 构建下钻路径
     */
    private List<DrillPath> buildDrillPaths(HierarchyConfig hierarchy, DrillDownRequest request) {
        List<DrillPath> paths = new ArrayList<>();

        Level currentLevel = findLevel(hierarchy, request.getDimension());
        if (currentLevel != null) {
            DrillPath path = new DrillPath();
            path.setLevel(currentLevel.getName());
            path.setField(currentLevel.getField());
            path.setValue(request.getValue());
            paths.add(path);
        }

        // 添加下一级
        if (currentLevel != null) {
            Level nextLevel = findNextLevel(hierarchy, currentLevel.getOrder());
            if (nextLevel != null) {
                DrillPath nextPath = new DrillPath();
                nextPath.setLevel(nextLevel.getName());
                nextPath.setField(nextLevel.getField());
                nextPath.setValue("点击下钻");
                paths.add(nextPath);
            }
        }

        return paths;
    }

    /**
     * 构建上卷路径
     */
    private List<DrillPath> buildRollUpPaths(HierarchyConfig hierarchy, DrillDownRequest request) {
        List<DrillPath> paths = new ArrayList<>();

        Level currentLevel = findLevel(hierarchy, request.getDimension());
        if (currentLevel != null) {
            Level prevLevel = findPrevLevel(hierarchy, currentLevel.getOrder());
            if (prevLevel != null) {
                DrillPath prevPath = new DrillPath();
                prevPath.setLevel(prevLevel.getName());
                prevPath.setField(prevLevel.getField());
                prevPath.setValue("点击查看父级");
                paths.add(prevPath);
            }
        }

        return paths;
    }

    /**
     * 创建错误响应
     */
    private DrillDownResponse createErrorResponse(String dimension, String value) {
        DrillDownResponse response = new DrillDownResponse();
        response.setDimension(dimension);
        response.setValue(value);
        response.setData(Collections.emptyList());
        response.setHasChildren(false);
        return response;
    }

    /**
     * 执行查询
     */
    private List<Map<String, Object>> executeQuery(String sql) {
        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            return jdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            log.error("查询执行失败：{}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * SQL 转义
     */
    private String escapeSql(String value) {
        if (value == null) return "";
        return value.replace("'", "''");
    }
}
