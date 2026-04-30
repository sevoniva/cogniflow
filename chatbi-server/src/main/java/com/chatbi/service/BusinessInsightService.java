package com.chatbi.service;

import com.chatbi.support.SqlDialectHelper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 企业演示分析服务。
 * 所有查询均基于持久化 H2 业务数据表，不返回运行期 mock 数据。
 */
@Service
@RequiredArgsConstructor
public class BusinessInsightService {

    private static final List<String> REGIONS = List.of("华东", "华南", "华北", "华中", "西南", "东北", "西北");

    private final JdbcTemplate jdbcTemplate;
    private final SqlDialectHelper sqlDialectHelper;

    public QueryPlan queryMetric(String metricName, String queryText) {
        return switch (metricName) {
            case "销售额" -> salesAmount(queryText, detectDimension(queryText, "区域"));
            case "毛利率" -> grossMargin(queryText, detectDimension(queryText, "区域"));
            case "回款额" -> cashCollection(queryText, detectDimension(queryText, "部门"));
            case "库存周转天数" -> inventoryTurnover(queryText, detectInventoryDimension(queryText));
            case "订单履约率" -> orderFulfillment(queryText, detectDimension(queryText, "区域"));
            case "部门费用支出" -> expenseAmount(queryText, detectDimension(queryText, "部门"));
            case "项目交付及时率" -> projectDelivery(queryText, detectProjectDimension(queryText));
            case "客户投诉量" -> complaintCount(queryText, detectDimension(queryText, "区域"));
            case "研发工时利用率" -> workHourUtilization(queryText, detectWorkHourDimension(queryText));
            case "审批平均时长" -> approvalDuration(queryText, detectDimension(queryText, "部门"));
            default -> salesAmount(queryText, detectDimension(queryText, "区域"));
        };
    }

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        BigDecimal totalSales = decimal("SELECT COALESCE(SUM(sales_amount), 0) FROM sales_order WHERE status <> 'CANCELLED'");
        BigDecimal totalOrders = decimal("SELECT COUNT(*) FROM sales_order WHERE status <> 'CANCELLED'");
        BigDecimal totalCustomers = decimal("SELECT COUNT(*) FROM customer WHERE status = 'ACTIVE'");
        BigDecimal avgOrderValue = decimal("SELECT COALESCE(AVG(sales_amount), 0) FROM sales_order WHERE status <> 'CANCELLED'");
        BigDecimal currentMonthSales = salesForMonth(YearMonth.now());
        BigDecimal lastMonthSales = salesForMonth(YearMonth.now().minusMonths(1));
        BigDecimal currentMonthUsers = decimal(
            "SELECT COUNT(*) FROM app_user WHERE register_date BETWEEN DATE '%s' AND DATE '%s'"
                .formatted(firstDay(YearMonth.now()), lastDay(YearMonth.now()))
        );
        BigDecimal lastMonthUsers = decimal(
            "SELECT COUNT(*) FROM app_user WHERE register_date BETWEEN DATE '%s' AND DATE '%s'"
                .formatted(firstDay(YearMonth.now().minusMonths(1)), lastDay(YearMonth.now().minusMonths(1)))
        );
        BigDecimal purchaseEvents = decimal("SELECT COUNT(*) FROM user_behavior WHERE event_type = 'PURCHASE'");
        BigDecimal pageViews = decimal("SELECT COUNT(*) FROM user_behavior WHERE event_type = 'PAGE_VIEW'");
        BigDecimal satisfaction = decimal("SELECT COALESCE(AVG(satisfaction_score), 0) FROM service_ticket WHERE satisfaction_score IS NOT NULL");

        stats.put("totalSales", totalSales.setScale(0, RoundingMode.HALF_UP).intValue());
        stats.put("totalOrders", totalOrders.intValue());
        stats.put("totalCustomers", totalCustomers.intValue());
        stats.put("avgOrderValue", avgOrderValue.setScale(0, RoundingMode.HALF_UP).intValue());
        stats.put("salesGrowth", growthRate(currentMonthSales, lastMonthSales) + "%");
        stats.put("customerGrowth", growthRate(currentMonthUsers, lastMonthUsers) + "%");
        stats.put("conversionRate", percent(purchaseEvents, pageViews) + "%");
        stats.put("satisfactionScore", satisfaction.setScale(1, RoundingMode.HALF_UP).doubleValue());
        return stats;
    }

    public List<Map<String, Object>> getOverviewRows() {
        Map<String, Object> stats = getDashboardStats();
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(overviewRow("累计销售额", stats.get("totalSales"), "元"));
        rows.add(overviewRow("订单总量", stats.get("totalOrders"), "单"));
        rows.add(overviewRow("活跃客户数", stats.get("totalCustomers"), "家"));
        rows.add(overviewRow("平均客单价", stats.get("avgOrderValue"), "元"));
        rows.add(overviewRow("销售增长率", stats.get("salesGrowth"), "%"));
        rows.add(overviewRow("客户增长率", stats.get("customerGrowth"), "%"));
        rows.add(overviewRow("转化率", stats.get("conversionRate"), "%"));
        rows.add(overviewRow("满意度", stats.get("satisfactionScore"), "分"));
        return rows;
    }

    public List<Map<String, Object>> getChartData(String chartType, String dimension) {
        String normalizedDimension = normalizeDimension(dimension, "区域");
        return switch (chartType.toLowerCase(Locale.ROOT)) {
            case "sales" -> salesAmount("趋势".equals(normalizedDimension) ? "销售趋势" : "销售额", normalizedDimension).getData();
            case "expense" -> expenseAmount("部门费用支出", normalizeDimension(dimension, "部门")).getData();
            case "project" -> projectDelivery("项目交付及时率", normalizeDimension(dimension, "团队")).getData();
            case "complaint" -> complaintCount("客户投诉量", normalizeDimension(dimension, "区域")).getData();
            case "workhour" -> workHourUtilization("研发工时利用率", normalizeDimension(dimension, "成员")).getData();
            case "timeseries" -> salesAmount("销售趋势", "月份").getData();
            default -> salesAmount("销售额", normalizeDimension(dimension, "区域")).getData();
        };
    }

    private QueryPlan salesAmount(String queryText, String dimension) {
        DateRange range = resolveDateRange(queryText, "月份".equals(dimension));
        String groupExpr = salesGroupExpr(dimension);
        String label = salesDimensionLabel(dimension);
        StringBuilder sql = new StringBuilder(
            "SELECT " + groupExpr + " AS " + quote(label) + ", SUM(sales_amount) AS \"销售额\" " +
                "FROM sales_order WHERE status <> 'CANCELLED'"
        );
        appendSalesFilters(sql, queryText, range);
        sql.append(" GROUP BY ").append(groupExpr).append(" ORDER BY 2 DESC");
        return plan(sql.toString(), label);
    }

    private QueryPlan grossMargin(String queryText, String dimension) {
        DateRange range = resolveDateRange(queryText, "月份".equals(dimension));
        String groupExpr = salesGroupExpr(dimension);
        String label = salesDimensionLabel(dimension);
        StringBuilder sql = new StringBuilder(
            "SELECT " + groupExpr + " AS " + quote(label) + ", " +
                "ROUND(CASE WHEN SUM(sales_amount) = 0 THEN 0 ELSE SUM(profit_amount) * 100.0 / SUM(sales_amount) END, 2) AS \"毛利率\" " +
                "FROM sales_order WHERE status <> 'CANCELLED'"
        );
        appendSalesFilters(sql, queryText, range);
        sql.append(" GROUP BY ").append(groupExpr).append(" ORDER BY 2 DESC");
        return plan(sql.toString(), label);
    }

    private QueryPlan cashCollection(String queryText, String dimension) {
        DateRange range = resolveDateRange(queryText, "月份".equals(dimension));
        String groupExpr = "月份".equals(dimension)
            ? sqlDialectHelper.monthBucket("record_date")
            : "department";
        String label = "月份".equals(dimension) ? "月份" : "部门";
        String sql = (
            "SELECT %s AS %s, SUM(amount) AS \"回款额\" " +
                "FROM financial_record WHERE type = 'INCOME' AND category IN ('REVENUE', 'AR') %s GROUP BY %s ORDER BY 2 DESC"
        ).formatted(groupExpr, quote(label), dateFilter("record_date", range), groupExpr);
        return plan(sql, label);
    }

    private QueryPlan inventoryTurnover(String queryText, String dimension) {
        String groupExpr = "产品类别".equals(dimension) ? "product_category" : "warehouse";
        String label = "产品类别".equals(dimension) ? "产品类别" : "仓库";
        String turnoverExpr = sqlDialectHelper.daysBetween("last_in_date", "COALESCE(last_out_date, CURRENT_DATE)");
        String sql = (
            "SELECT %s AS %s, ROUND(AVG(ABS(%s)), 2) AS \"库存周转天数\" " +
                "FROM inventory GROUP BY %s ORDER BY 2 DESC"
        ).formatted(groupExpr, quote(label), turnoverExpr, groupExpr);
        return plan(sql, label);
    }

    private QueryPlan orderFulfillment(String queryText, String dimension) {
        DateRange range = resolveDateRange(queryText, "月份".equals(dimension));
        String groupExpr = salesGroupExpr(dimension);
        String label = salesDimensionLabel(dimension);
        StringBuilder sql = new StringBuilder(
            "SELECT " + groupExpr + " AS " + quote(label) + ", " +
                "ROUND(SUM(CASE WHEN status = 'DELIVERED' THEN 1 ELSE 0 END) * 100.0 / NULLIF(SUM(CASE WHEN status <> 'CANCELLED' THEN 1 ELSE 0 END), 0), 2) AS \"订单履约率\" " +
                "FROM sales_order WHERE status <> 'CANCELLED'"
        );
        appendSalesFilters(sql, queryText, range);
        sql.append(" GROUP BY ").append(groupExpr).append(" ORDER BY 2 DESC");
        return plan(sql.toString(), label);
    }

    private QueryPlan expenseAmount(String queryText, String dimension) {
        DateRange range = resolveDateRange(queryText, "月份".equals(dimension));
        String groupExpr = "月份".equals(dimension)
            ? sqlDialectHelper.monthBucket("record_date")
            : "department";
        String label = "月份".equals(dimension) ? "月份" : "部门";
        String sql = (
            "SELECT %s AS %s, SUM(amount) AS \"费用金额\" " +
                "FROM financial_record WHERE type = 'EXPENSE' %s GROUP BY %s ORDER BY 2 DESC"
        ).formatted(groupExpr, quote(label), dateFilter("record_date", range), groupExpr);
        return plan(sql, label);
    }

    private QueryPlan projectDelivery(String queryText, String dimension) {
        DateRange range = resolveDateRange(queryText, "月份".equals(dimension));
        String groupExpr = switch (dimension) {
            case "区域" -> "region";
            case "月份" -> sqlDialectHelper.monthBucket("planned_delivery_date");
            default -> "owner_team";
        };
        String label = switch (dimension) {
            case "区域" -> "区域";
            case "月份" -> "月份";
            default -> "团队";
        };
        String sql = (
            "SELECT %s AS %s, ROUND(SUM(CASE WHEN actual_delivery_date <= planned_delivery_date THEN 1 ELSE 0 END) * 100.0 / NULLIF(COUNT(*), 0), 2) AS \"项目交付及时率\" " +
                "FROM project_delivery WHERE 1 = 1 %s GROUP BY %s ORDER BY 2 DESC"
        ).formatted(groupExpr, quote(label), dateFilter("planned_delivery_date", range), groupExpr);
        return plan(sql, label);
    }

    private QueryPlan complaintCount(String queryText, String dimension) {
        DateRange range = resolveDateRange(queryText, "月份".equals(dimension));
        String groupExpr = switch (dimension) {
            case "渠道" -> "st.channel";
            case "月份" -> sqlDialectHelper.monthBucket("st.created_date");
            default -> "COALESCE(c.region, '未知区域')";
        };
        String label = switch (dimension) {
            case "渠道" -> "渠道";
            case "月份" -> "月份";
            default -> "区域";
        };
        String regionFilter = detectRegion(queryText);
        String sql = (
            "SELECT %s AS %s, COUNT(*) AS \"投诉量\" " +
                "FROM service_ticket st LEFT JOIN customer c ON st.customer_id = c.id " +
                "WHERE st.ticket_type = 'COMPLAINT' %s %s GROUP BY %s ORDER BY 2 DESC"
        ).formatted(
            groupExpr,
            quote(label),
            dateTimeFilter("st.created_date", range),
            regionFilter == null ? "" : " AND c.region = '" + regionFilter + "'",
            groupExpr
        );
        return plan(sql, label);
    }

    private QueryPlan workHourUtilization(String queryText, String dimension) {
        DateRange range = resolveDateRange(queryText, "月份".equals(dimension));
        String groupExpr = switch (dimension) {
            case "角色" -> "tm.role";
            case "月份" -> sqlDialectHelper.monthBucket("COALESCE(s.completed_at, s.created_at)");
            default -> "tm.real_name";
        };
        String label = switch (dimension) {
            case "角色" -> "角色";
            case "月份" -> "月份";
            default -> "成员";
        };
        String sql = (
            "SELECT %s AS %s, ROUND(SUM(s.actual_hours) * 100.0 / NULLIF(SUM(s.estimated_hours), 0), 2) AS \"研发工时利用率\" " +
                "FROM agile_story s JOIN agile_team_member tm ON s.assignee_id = tm.user_id " +
                "WHERE s.estimated_hours > 0 AND s.actual_hours IS NOT NULL %s GROUP BY %s ORDER BY 2 DESC"
        ).formatted(groupExpr, quote(label), dateTimeFilter("COALESCE(s.completed_at, s.created_at)", range), groupExpr);
        return plan(sql, label);
    }

    private QueryPlan approvalDuration(String queryText, String dimension) {
        DateRange range = resolveDateRange(queryText, "月份".equals(dimension));
        String groupExpr = "月份".equals(dimension)
            ? sqlDialectHelper.monthBucket("start_time")
            : "department";
        String label = "月份".equals(dimension) ? "月份" : "部门";
        String sql = (
            "SELECT %s AS %s, ROUND(AVG(duration_hours), 2) AS \"审批平均时长\" " +
                "FROM approval_record WHERE status = 'APPROVED' %s GROUP BY %s ORDER BY 2 DESC"
        ).formatted(groupExpr, quote(label), dateTimeFilter("start_time", range), groupExpr);
        return plan(sql, label);
    }

    private QueryPlan plan(String sql, String dimension) {
        return new QueryPlan(sql, dimension, jdbcTemplate.queryForList(sql));
    }

    private void appendSalesFilters(StringBuilder sql, String queryText, DateRange range) {
        sql.append(dateFilter("order_date", range));
        String region = detectRegion(queryText);
        if (region != null) {
            sql.append(" AND region = '").append(region).append("'");
        }
    }

    private String salesGroupExpr(String dimension) {
        return switch (dimension) {
            case "产品类别" -> "product_category";
            case "月份" -> sqlDialectHelper.monthBucket("order_date");
            case "销售人员" -> "sales_person_name";
            default -> "region";
        };
    }

    private String salesDimensionLabel(String dimension) {
        return switch (dimension) {
            case "产品类别" -> "产品类别";
            case "月份" -> "月份";
            case "销售人员" -> "销售人员";
            default -> "区域";
        };
    }

    private String detectDimension(String queryText, String fallback) {
        if (queryText.contains("趋势") || queryText.contains("按月") || queryText.contains("月度")) {
            return "月份";
        }
        if (queryText.contains("产品") || queryText.contains("品类") || queryText.contains("类别")) {
            return "产品类别";
        }
        if (queryText.contains("销售") && queryText.contains("人员")) {
            return "销售人员";
        }
        if (queryText.contains("渠道")) {
            return "渠道";
        }
        return fallback;
    }

    private String detectInventoryDimension(String queryText) {
        if (queryText.contains("产品") || queryText.contains("类别")) {
            return "产品类别";
        }
        return "仓库";
    }

    private String detectProjectDimension(String queryText) {
        if (queryText.contains("区域") || queryText.contains("地区")) {
            return "区域";
        }
        if (queryText.contains("趋势") || queryText.contains("月")) {
            return "月份";
        }
        return "团队";
    }

    private String detectWorkHourDimension(String queryText) {
        if (queryText.contains("角色")) {
            return "角色";
        }
        if (queryText.contains("趋势") || queryText.contains("月")) {
            return "月份";
        }
        return "成员";
    }

    private String normalizeDimension(String dimension, String fallback) {
        if (dimension == null || dimension.isBlank()) {
            return fallback;
        }
        return dimension;
    }

    private String detectRegion(String queryText) {
        for (String region : REGIONS) {
            if (queryText.contains(region)) {
                return region;
            }
        }
        return null;
    }

    private DateRange resolveDateRange(String queryText, boolean preferTrendWindow) {
        LocalDate now = LocalDate.now();
        if (preferTrendWindow) {
            return new DateRange(now.minusMonths(11).withDayOfMonth(1), now.with(TemporalAdjusters.lastDayOfMonth()));
        }
        if (queryText.contains("本月")) {
            YearMonth month = YearMonth.now();
            return new DateRange(month.atDay(1), month.atEndOfMonth());
        }
        if (queryText.contains("上月")) {
            YearMonth month = YearMonth.now().minusMonths(1);
            return new DateRange(month.atDay(1), month.atEndOfMonth());
        }
        if (queryText.contains("今年")) {
            return new DateRange(LocalDate.of(now.getYear(), 1, 1), LocalDate.of(now.getYear(), 12, 31));
        }
        if (queryText.contains("本季度")) {
            int currentQuarter = (now.getMonthValue() - 1) / 3;
            LocalDate start = LocalDate.of(now.getYear(), currentQuarter * 3 + 1, 1);
            return new DateRange(start, start.plusMonths(2).with(TemporalAdjusters.lastDayOfMonth()));
        }
        if (queryText.contains("上季度")) {
            LocalDate base = now.minusMonths(3);
            int quarter = (base.getMonthValue() - 1) / 3;
            LocalDate start = LocalDate.of(base.getYear(), quarter * 3 + 1, 1);
            return new DateRange(start, start.plusMonths(2).with(TemporalAdjusters.lastDayOfMonth()));
        }
        return new DateRange(now.minusMonths(2).withDayOfMonth(1), now.with(TemporalAdjusters.lastDayOfMonth()));
    }

    private String dateFilter(String column, DateRange range) {
        if (range == null) {
            return "";
        }
        return " AND " + column + " BETWEEN DATE '" + range.start() + "' AND DATE '" + range.end() + "'";
    }

    private String dateTimeFilter(String column, DateRange range) {
        if (range == null) {
            return "";
        }
        return " AND CAST(" + column + " AS DATE) BETWEEN DATE '" + range.start() + "' AND DATE '" + range.end() + "'";
    }

    private BigDecimal salesForMonth(YearMonth month) {
        return decimal(
            "SELECT COALESCE(SUM(sales_amount), 0) FROM sales_order WHERE status <> 'CANCELLED' AND order_date BETWEEN DATE '%s' AND DATE '%s'"
                .formatted(firstDay(month), lastDay(month))
        );
    }

    private String firstDay(YearMonth month) {
        return month.atDay(1).toString();
    }

    private String lastDay(YearMonth month) {
        return month.atEndOfMonth().toString();
    }

    private int growthRate(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ? 100 : 0;
        }
        return current.subtract(previous)
            .multiply(BigDecimal.valueOf(100))
            .divide(previous, 0, RoundingMode.HALF_UP)
            .intValue();
    }

    private int percent(BigDecimal numerator, BigDecimal denominator) {
        if (denominator.compareTo(BigDecimal.ZERO) == 0) {
            return 0;
        }
        return numerator.multiply(BigDecimal.valueOf(100))
            .divide(denominator, 0, RoundingMode.HALF_UP)
            .intValue();
    }

    private BigDecimal decimal(String sql) {
        Number number = jdbcTemplate.queryForObject(sql, Number.class);
        if (number == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(number.toString());
    }

    private Map<String, Object> overviewRow(String metric, Object value, String unit) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("指标", metric);
        row.put("数值", normalizeOverviewValue(value));
        row.put("单位", unit);
        return row;
    }

    private Number normalizeOverviewValue(Object value) {
        if (value instanceof Number number) {
            if (number instanceof Double || number instanceof Float || value.toString().contains(".")) {
                return BigDecimal.valueOf(number.doubleValue()).setScale(1, RoundingMode.HALF_UP).doubleValue();
            }
            return number.longValue();
        }
        if (value instanceof String text) {
            String normalized = text.replace("%", "").replace(",", "").trim();
            if (normalized.isBlank()) {
                return 0;
            }
            return normalized.contains(".")
                ? BigDecimal.valueOf(Double.parseDouble(normalized)).setScale(1, RoundingMode.HALF_UP).doubleValue()
                : Long.parseLong(normalized);
        }
        return 0;
    }

    private String quote(String name) {
        return '"' + name + '"';
    }

    @Data
    @AllArgsConstructor
    public static class QueryPlan {
        private String sql;
        private String dimension;
        private List<Map<String, Object>> data;
    }

    private record DateRange(LocalDate start, LocalDate end) {}
}
