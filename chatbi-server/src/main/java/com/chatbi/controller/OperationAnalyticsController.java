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
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;

/**
 * 运营分析控制器
 */
@Slf4j
@Tag(name = "运营分析", description = "运营分析控制器")
@RestController
@RequestMapping("/api/analytics/operation")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class OperationAnalyticsController {

    private final JdbcTemplate jdbcTemplate;
    private final SqlDialectHelper sqlDialectHelper;

    /**
     * 用户活跃度趋势（DAU/MAU）
     */
    @Operation(summary = "用户活跃度趋势（DAU/MAU）")
    @GetMapping("/user-activity")
    public Result<List<Map<String, Object>>> getUserActivity() {
        try {
            String dayExpr = sqlDialectHelper.dayBucket("event_time");
            String sql = "SELECT " + dayExpr + " as activity_date, " +
                        "COUNT(DISTINCT user_id) as dau, " +
                        "COUNT(*) as event_count " +
                        "FROM user_behavior " +
                        "WHERE event_time >= ? " +
                        "GROUP BY " + dayExpr + ' ' +
                        "ORDER BY activity_date DESC";

            List<Map<String, Object>> result = jdbcTemplate.queryForList(
                    sql,
                    Timestamp.valueOf(LocalDate.now().minusDays(29).atStartOfDay())
            );
            return Result.ok(result);
        } catch (Exception e) {
            log.error("获取用户活跃度失败", e);
            return Result.error("获取用户活跃度失败：" + e.getMessage());
        }
    }

    /**
     * 用户注册趋势
     */
    @Operation(summary = "用户注册趋势")
    @GetMapping("/user-registration")
    public Result<List<Map<String, Object>>> getUserRegistration() {
        try {
            String monthExpr = sqlDialectHelper.monthBucket("register_date");
            String sql = "SELECT " + monthExpr + " as period_label, " +
                        "COUNT(*) as new_users, " +
                        "register_channel " +
                        "FROM app_user " +
                        "GROUP BY " + monthExpr + ", register_channel " +
                        "ORDER BY period_label DESC LIMIT 60";

            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);
            return Result.ok(result);
        } catch (Exception e) {
            log.error("获取用户注册趋势失败", e);
            return Result.error("获取用户注册趋势失败：" + e.getMessage());
        }
    }

    /**
     * 渠道分析
     */
    @Operation(summary = "渠道分析")
    @GetMapping("/channel-analysis")
    public Result<List<Map<String, Object>>> getChannelAnalysis() {
        try {
            String sql = "SELECT channel, " +
                        "COUNT(DISTINCT user_id) as user_count, " +
                        "COUNT(*) as event_count, " +
                        "AVG(duration) as avg_duration, " +
                        "SUM(CASE WHEN event_type = 'PURCHASE' THEN 1 ELSE 0 END) as purchase_count " +
                        "FROM user_behavior " +
                        "GROUP BY channel " +
                        "ORDER BY user_count DESC";

            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);
            return Result.ok(result);
        } catch (Exception e) {
            log.error("获取渠道分析失败", e);
            return Result.error("获取渠道分析失败：" + e.getMessage());
        }
    }

    /**
     * 设备分析
     */
    @Operation(summary = "设备分析")
    @GetMapping("/device-analysis")
    public Result<List<Map<String, Object>>> getDeviceAnalysis() {
        try {
            String sql = "SELECT device_type, os, browser, " +
                        "COUNT(DISTINCT user_id) as user_count, " +
                        "COUNT(*) as event_count, " +
                        "AVG(duration) as avg_duration " +
                        "FROM user_behavior " +
                        "GROUP BY device_type, os, browser " +
                        "ORDER BY user_count DESC " +
                        "LIMIT 20";

            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);
            return Result.ok(result);
        } catch (Exception e) {
            log.error("获取设备分析失败", e);
            return Result.error("获取设备分析失败：" + e.getMessage());
        }
    }

    /**
     * 地域分析
     */
    @Operation(summary = "地域分析")
    @GetMapping("/region-analysis")
    public Result<List<Map<String, Object>>> getRegionAnalysis() {
        try {
            String sql = "SELECT province, city, " +
                        "COUNT(DISTINCT user_id) as user_count, " +
                        "COUNT(*) as event_count " +
                        "FROM user_behavior " +
                        "GROUP BY province, city " +
                        "ORDER BY user_count DESC " +
                        "LIMIT 30";

            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);
            return Result.ok(result);
        } catch (Exception e) {
            log.error("获取地域分析失败", e);
            return Result.error("获取地域分析失败：" + e.getMessage());
        }
    }

    /**
     * 事件类型分析
     */
    @Operation(summary = "事件类型分析")
    @GetMapping("/event-analysis")
    public Result<List<Map<String, Object>>> getEventAnalysis() {
        try {
            String sql = "SELECT event_type, " +
                        "COUNT(*) as event_count, " +
                        "COUNT(DISTINCT user_id) as user_count, " +
                        "AVG(duration) as avg_duration " +
                        "FROM user_behavior " +
                        "GROUP BY event_type " +
                        "ORDER BY event_count DESC";

            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);
            return Result.ok(result);
        } catch (Exception e) {
            log.error("获取事件分析失败", e);
            return Result.error("获取事件分析失败：" + e.getMessage());
        }
    }

    /**
     * 用户留存分析
     */
    @Operation(summary = "用户留存分析")
    @GetMapping("/user-retention")
    public Result<Map<String, Object>> getUserRetention() {
        try {
            Map<String, Object> retention = new HashMap<>();
            LocalDate startDate = LocalDate.now().minusDays(29);
            String eventDateExpr = sqlDialectHelper.toDate("t2.event_time");
            String nextDayExpr = sqlDialectHelper.addDays("t1.register_date", 1);

            // 次日留存
            String day1Sql = "SELECT COUNT(DISTINCT t1.id) as retained_users " +
                            "FROM app_user t1 " +
                            "INNER JOIN user_behavior t2 ON t1.user_no = CONCAT('U', LPAD(t2.user_id, 8, '0')) " +
                            "WHERE " + eventDateExpr + " = " + nextDayExpr;

            String totalUsersSql = "SELECT COUNT(*) as total_users FROM app_user " +
                                  "WHERE register_date >= ?";

            Map<String, Object> day1Data = jdbcTemplate.queryForMap(day1Sql);
            Map<String, Object> totalData = jdbcTemplate.queryForMap(totalUsersSql, Date.valueOf(startDate));

            retention.put("day1_retention", day1Data);
            retention.put("total_users", totalData);

            return Result.ok(retention);
        } catch (Exception e) {
            log.error("获取用户留存失败", e);
            return Result.error("获取用户留存失败：" + e.getMessage());
        }
    }

    /**
     * 运营概览
     */
    @Operation(summary = "运营概览")
    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview() {
        try {
            Map<String, Object> overview = new HashMap<>();
            LocalDate today = LocalDate.now();
            LocalDate currentMonthStart = today.withDayOfMonth(1);
            LocalDate nextMonthStart = currentMonthStart.plusMonths(1);

            // 总用户数
            String userSql = "SELECT COUNT(*) as total_users FROM app_user WHERE status = 'ACTIVE'";
            Map<String, Object> userData = jdbcTemplate.queryForMap(userSql);
            overview.putAll(userData);

            // 今日活跃用户
            String dauSql = "SELECT COUNT(DISTINCT user_id) as dau " +
                           "FROM user_behavior " +
                           "WHERE CAST(event_time AS DATE) = ?";
            Map<String, Object> dauData = jdbcTemplate.queryForMap(dauSql, Date.valueOf(today));
            overview.putAll(dauData);

            // 本月新增用户
            String newUserSql = "SELECT COUNT(*) as month_new_users " +
                               "FROM app_user " +
                               "WHERE register_date >= ? AND register_date < ?";
            Map<String, Object> newUserData = jdbcTemplate.queryForMap(
                    newUserSql,
                    Date.valueOf(currentMonthStart),
                    Date.valueOf(nextMonthStart)
            );
            overview.putAll(newUserData);

            // 总事件数
            String eventSql = "SELECT COUNT(*) as total_events FROM user_behavior";
            Map<String, Object> eventData = jdbcTemplate.queryForMap(eventSql);
            overview.putAll(eventData);

            return Result.ok(overview);
        } catch (Exception e) {
            log.error("获取运营概览失败", e);
            return Result.error("获取运营概览失败：" + e.getMessage());
        }
    }
}
