package com.chatbi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.chatbi.common.Result;
import com.chatbi.support.SqlDialectHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

/**
 * 销售分析控制器
 */
@Slf4j
@Tag(name = "销售分析", description = "销售分析控制器")
@RestController
@RequestMapping("/api/analytics/sales")
@RequiredArgsConstructor
public class SalesAnalyticsController {

    private final JdbcTemplate jdbcTemplate;
    private final SqlDialectHelper sqlDialectHelper;

    /**
     * 销售额趋势（按月）
     */
    @Operation(summary = "销售额趋势（按月）")
    @GetMapping("/trend")
    public Result<List<Map<String, Object>>> getSalesTrend(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            String monthExpr = sqlDialectHelper.monthBucket("order_date");
            StringBuilder sql = new StringBuilder("SELECT " + monthExpr + " as period_label, " +
                    "SUM(sales_amount) as sales_amount, " +
                    "SUM(profit_amount) as profit_amount, " +
                    "COUNT(*) as order_count " +
                    "FROM sales_order " +
                    "WHERE status <> 'CANCELLED' ");
            List<Object> params = new ArrayList<>();

            if (startDate != null && endDate != null) {
                sql.append("AND order_date BETWEEN ? AND ? ");
                params.add(Date.valueOf(startDate));
                params.add(Date.valueOf(endDate));
            }

            sql.append("GROUP BY ").append(monthExpr).append(' ')
                    .append("ORDER BY period_label DESC LIMIT 12");

            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql.toString(), params.toArray());
            return Result.ok(result);
        } catch (Exception e) {
            log.error("获取销售趋势失败", e);
            return Result.error("获取销售趋势失败：" + e.getMessage());
        }
    }

    /**
     * 产品销售排行
     */
    @Operation(summary = "产品销售排行")
    @GetMapping("/product-ranking")
    public Result<List<Map<String, Object>>> getProductRanking(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            String sql = "SELECT product_name, product_category, " +
                        "SUM(quantity) as total_quantity, " +
                        "SUM(sales_amount) as total_sales, " +
                        "SUM(profit_amount) as total_profit, " +
                        "COUNT(*) as order_count " +
                        "FROM sales_order " +
                        "WHERE status != 'CANCELLED' " +
                        "GROUP BY product_name, product_category " +
                        "ORDER BY total_sales DESC " +
                        "LIMIT " + limit;

            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);
            return Result.ok(result);
        } catch (Exception e) {
            log.error("获取产品排行失败", e);
            return Result.error("获取产品排行失败：" + e.getMessage());
        }
    }

    /**
     * 地区销售分布
     */
    @Operation(summary = "地区销售分布")
    @GetMapping("/region-distribution")
    public Result<List<Map<String, Object>>> getRegionDistribution() {
        try {
            String sql = "SELECT region, " +
                        "SUM(sales_amount) as total_sales, " +
                        "SUM(profit_amount) as total_profit, " +
                        "COUNT(*) as order_count, " +
                        "COUNT(DISTINCT customer_id) as customer_count " +
                        "FROM sales_order " +
                        "WHERE status != 'CANCELLED' " +
                        "GROUP BY region " +
                        "ORDER BY total_sales DESC";

            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);
            return Result.ok(result);
        } catch (Exception e) {
            log.error("获取地区分布失败", e);
            return Result.error("获取地区分布失败：" + e.getMessage());
        }
    }

    /**
     * 销售人员业绩排行
     */
    @Operation(summary = "销售人员业绩排行")
    @GetMapping("/salesperson-ranking")
    public Result<List<Map<String, Object>>> getSalespersonRanking(
            @RequestParam(defaultValue = "20") int limit) {
        try {
            String sql = "SELECT sales_person_name, " +
                        "SUM(sales_amount) as total_sales, " +
                        "SUM(profit_amount) as total_profit, " +
                        "COUNT(*) as order_count, " +
                        "COUNT(DISTINCT customer_id) as customer_count, " +
                        "AVG(sales_amount) as avg_order_amount " +
                        "FROM sales_order " +
                        "WHERE status != 'CANCELLED' " +
                        "GROUP BY sales_person_name " +
                        "ORDER BY total_sales DESC " +
                        "LIMIT " + limit;

            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);
            return Result.ok(result);
        } catch (Exception e) {
            log.error("获取销售人员排行失败", e);
            return Result.error("获取销售人员排行失败：" + e.getMessage());
        }
    }

    /**
     * 客户分析
     */
    @Operation(summary = "客户分析")
    @GetMapping("/customer-analysis")
    public Result<List<Map<String, Object>>> getCustomerAnalysis() {
        try {
            String sql = "SELECT customer_type, level, " +
                        "COUNT(*) as customer_count, " +
                        "SUM(total_purchase_amount) as total_amount, " +
                        "AVG(total_purchase_amount) as avg_amount, " +
                        "SUM(purchase_count) as total_orders " +
                        "FROM customer " +
                        "WHERE status = 'ACTIVE' " +
                        "GROUP BY customer_type, level " +
                        "ORDER BY total_amount DESC";

            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);
            return Result.ok(result);
        } catch (Exception e) {
            log.error("获取客户分析失败", e);
            return Result.error("获取客户分析失败：" + e.getMessage());
        }
    }

    /**
     * 订单状态统计
     */
    @Operation(summary = "订单状态统计")
    @GetMapping("/order-status")
    public Result<List<Map<String, Object>>> getOrderStatus() {
        try {
            String sql = "SELECT status, " +
                        "COUNT(*) as count, " +
                        "SUM(sales_amount) as total_amount " +
                        "FROM sales_order " +
                        "GROUP BY status " +
                        "ORDER BY count DESC";

            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);
            return Result.ok(result);
        } catch (Exception e) {
            log.error("获取订单状态统计失败", e);
            return Result.error("获取订单状态统计失败：" + e.getMessage());
        }
    }

    /**
     * 产品类别分析
     */
    @Operation(summary = "产品类别分析")
    @GetMapping("/category-analysis")
    public Result<List<Map<String, Object>>> getCategoryAnalysis() {
        try {
            String sql = "SELECT product_category, " +
                        "COUNT(DISTINCT product_name) as product_count, " +
                        "SUM(quantity) as total_quantity, " +
                        "SUM(sales_amount) as total_sales, " +
                        "SUM(profit_amount) as total_profit, " +
                        "AVG(profit_amount / sales_amount * 100) as profit_rate " +
                        "FROM sales_order " +
                        "WHERE status != 'CANCELLED' " +
                        "GROUP BY product_category " +
                        "ORDER BY total_sales DESC";

            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);
            return Result.ok(result);
        } catch (Exception e) {
            log.error("获取产品类别分析失败", e);
            return Result.error("获取产品类别分析失败：" + e.getMessage());
        }
    }

    /**
     * 销售概览（汇总数据）
     */
    @Operation(summary = "销售概览（汇总数据）")
    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview() {
        try {
            Map<String, Object> overview = new HashMap<>();
            LocalDate today = LocalDate.now();
            LocalDate currentMonthStart = today.withDayOfMonth(1);
            LocalDate nextMonthStart = currentMonthStart.plusMonths(1);
            LocalDate lastMonthStart = currentMonthStart.minusMonths(1);

            // 总销售额
            String salesSql = "SELECT SUM(sales_amount) as total_sales, " +
                             "SUM(profit_amount) as total_profit, " +
                             "COUNT(*) as total_orders " +
                             "FROM sales_order WHERE status <> 'CANCELLED'";
            Map<String, Object> salesData = jdbcTemplate.queryForMap(salesSql);
            overview.putAll(salesData);

            // 客户数
            String customerSql = "SELECT COUNT(*) as total_customers FROM customer WHERE status = 'ACTIVE'";
            Map<String, Object> customerData = jdbcTemplate.queryForMap(customerSql);
            overview.putAll(customerData);

            // 本月销售额
            String monthSql = "SELECT SUM(sales_amount) as month_sales " +
                             "FROM sales_order " +
                             "WHERE status <> 'CANCELLED' " +
                             "AND order_date >= ? AND order_date < ?";
            Map<String, Object> monthData = jdbcTemplate.queryForMap(
                    monthSql,
                    Date.valueOf(currentMonthStart),
                    Date.valueOf(nextMonthStart)
            );
            overview.putAll(monthData);

            // 上月销售额（用于计算环比）
            String lastMonthSql = "SELECT SUM(sales_amount) as last_month_sales " +
                                 "FROM sales_order " +
                                 "WHERE status <> 'CANCELLED' " +
                                 "AND order_date >= ? AND order_date < ?";
            Map<String, Object> lastMonthData = jdbcTemplate.queryForMap(
                    lastMonthSql,
                    Date.valueOf(lastMonthStart),
                    Date.valueOf(currentMonthStart)
            );
            overview.putAll(lastMonthData);

            return Result.ok(overview);
        } catch (Exception e) {
            log.error("获取销售概览失败", e);
            return Result.error("获取销售概览失败：" + e.getMessage());
        }
    }
}
