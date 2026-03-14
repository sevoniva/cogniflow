package com.chatbi.controller;

import com.chatbi.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

/**
 * 敏捷研发指标控制器
 */
@Tag(name = "敏捷研发指标", description = "敏捷研发管理相关指标查询接口")
@RestController
@RequestMapping("/api/agile")
@RequiredArgsConstructor
public class AgileMetricsController {

    private final DataSource dataSource;

    /**
     * 获取项目概览
     */
    @Operation(summary = "获取项目概览")
    @GetMapping("/project-overview")
    public Result<Map<String, Object>> getProjectOverview() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        Map<String, Object> result = new HashMap<>();

        // 项目总数
        Integer totalProjects = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM agile_project", Integer.class);
        result.put("totalProjects", totalProjects);

        // 活跃项目数
        Integer activeProjects = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM agile_project WHERE status = 'ACTIVE'", Integer.class);
        result.put("activeProjects", activeProjects);

        // 团队总人数
        Integer totalMembers = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM agile_team_member WHERE status = 'ACTIVE'", Integer.class);
        result.put("totalMembers", totalMembers);

        // 进行中的迭代数
        Integer activeSprints = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM agile_sprint WHERE status = 'ACTIVE'", Integer.class);
        result.put("activeSprints", activeSprints);

        return Result.ok(result);
    }

    /**
     * 获取迭代速率趋势
     */
    @Operation(summary = "获取迭代速率趋势")
    @GetMapping("/sprint-velocity")
    public Result<List<Map<String, Object>>> getSprintVelocity(@RequestParam Long projectId) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        String sql = "SELECT sprint_name, planned_story_points, completed_story_points, velocity " +
                     "FROM agile_sprint WHERE project_id = ? AND status = 'COMPLETED' " +
                     "ORDER BY start_date DESC LIMIT 10";

        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, projectId);
        return Result.ok(result);
    }

    /**
     * 获取缺陷统计
     */
    @Operation(summary = "获取缺陷统计")
    @GetMapping("/defect-stats")
    public Result<Map<String, Object>> getDefectStats(@RequestParam Long projectId) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        Map<String, Object> result = new HashMap<>();

        // 按严重程度统计
        String severitySql = "SELECT severity, COUNT(*) as count FROM agile_defect " +
                            "WHERE project_id = ? GROUP BY severity";
        List<Map<String, Object>> severityStats = jdbcTemplate.queryForList(severitySql, projectId);
        result.put("bySeverity", severityStats);

        // 按状态统计
        String statusSql = "SELECT status, COUNT(*) as count FROM agile_defect " +
                          "WHERE project_id = ? GROUP BY status";
        List<Map<String, Object>> statusStats = jdbcTemplate.queryForList(statusSql, projectId);
        result.put("byStatus", statusStats);

        // 平均修复时间
        Double avgResolutionTime = jdbcTemplate.queryForObject(
            "SELECT AVG(resolution_time_hours) FROM agile_defect " +
            "WHERE project_id = ? AND status = 'CLOSED'", Double.class, projectId);
        result.put("avgResolutionTime", avgResolutionTime);

        return Result.ok(result);
    }

    /**
     * 获取测试通过率趋势
     */
    @Operation(summary = "获取测试通过率趋势")
    @GetMapping("/test-pass-rate")
    public Result<List<Map<String, Object>>> getTestPassRate(@RequestParam Long projectId) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        String sql = "SELECT execution_date as date, " +
                     "SUM(CASE WHEN result = 'PASS' THEN 1 ELSE 0 END) * 100.0 / COUNT(*) as pass_rate " +
                     "FROM agile_test_execution WHERE project_id = ? " +
                     "GROUP BY execution_date ORDER BY date DESC LIMIT 30";

        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, projectId);
        return Result.ok(result);
    }

    /**
     * 获取代码提交统计
     */
    @Operation(summary = "获取代码提交统计")
    @GetMapping("/commit-stats")
    public Result<Map<String, Object>> getCommitStats(@RequestParam Long projectId) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        LocalDate startDate = LocalDate.now().minusDays(29);

        Map<String, Object> result = new HashMap<>();

        // 最近30天提交趋势
        String trendSql = "SELECT CAST(commit_date AS DATE) as date, COUNT(*) as count, " +
                         "SUM(lines_added) as added, SUM(lines_deleted) as deleted " +
                         "FROM agile_code_commit WHERE project_id = ? " +
                         "AND CAST(commit_date AS DATE) >= ? " +
                         "GROUP BY CAST(commit_date AS DATE) ORDER BY date";
        List<Map<String, Object>> trend = jdbcTemplate.queryForList(trendSql, projectId, Date.valueOf(startDate));
        result.put("trend", trend);

        // 按开发者统计
        String authorSql = "SELECT tm.real_name, COUNT(*) as commits, " +
                          "SUM(cc.lines_added) as added, SUM(cc.lines_deleted) as deleted " +
                          "FROM agile_code_commit cc " +
                          "JOIN agile_team_member tm ON cc.author_id = tm.user_id " +
                          "WHERE cc.project_id = ? " +
                          "AND CAST(cc.commit_date AS DATE) >= ? " +
                          "GROUP BY tm.real_name ORDER BY commits DESC LIMIT 10";
        List<Map<String, Object>> byAuthor = jdbcTemplate.queryForList(authorSql, projectId, Date.valueOf(startDate));
        result.put("byAuthor", byAuthor);

        return Result.ok(result);
    }

    /**
     * 获取部署成功率
     */
    @Operation(summary = "获取部署成功率")
    @GetMapping("/deployment-success-rate")
    public Result<Map<String, Object>> getDeploymentSuccessRate(@RequestParam Long projectId) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        LocalDate startDate = LocalDate.now().minusDays(29);

        Map<String, Object> result = new HashMap<>();

        // 按环境统计
        String envSql = "SELECT environment, " +
                       "SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) * 100.0 / COUNT(*) as success_rate, " +
                       "COUNT(*) as total_deployments " +
                       "FROM agile_deployment WHERE project_id = ? " +
                       "GROUP BY environment";
        List<Map<String, Object>> byEnv = jdbcTemplate.queryForList(envSql, projectId);
        result.put("byEnvironment", byEnv);

        // 最近30天趋势
        String trendSql = "SELECT CAST(deploy_date AS DATE) as date, " +
                         "SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) * 100.0 / COUNT(*) as success_rate " +
                         "FROM agile_deployment WHERE project_id = ? " +
                         "AND CAST(deploy_date AS DATE) >= ? " +
                         "GROUP BY CAST(deploy_date AS DATE) ORDER BY date";
        List<Map<String, Object>> trend = jdbcTemplate.queryForList(trendSql, projectId, Date.valueOf(startDate));
        result.put("trend", trend);

        return Result.ok(result);
    }

    /**
     * 获取质量指标趋势
     */
    @Operation(summary = "获取质量指标趋势")
    @GetMapping("/quality-metrics")
    public Result<List<Map<String, Object>>> getQualityMetrics(@RequestParam Long projectId) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        String sql = "SELECT metric_date, code_coverage, unit_test_pass_rate, " +
                     "integration_test_pass_rate, defect_density, defect_removal_efficiency, " +
                     "mean_time_to_repair, technical_debt_ratio " +
                     "FROM agile_quality_metrics WHERE project_id = ? " +
                     "ORDER BY metric_date DESC LIMIT 30";

        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, projectId);
        return Result.ok(result);
    }

    /**
     * 获取需求完成情况
     */
    @Operation(summary = "获取需求完成情况")
    @GetMapping("/story-completion")
    public Result<Map<String, Object>> getStoryCompletion(@RequestParam Long projectId) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        Map<String, Object> result = new HashMap<>();

        // 按状态统计
        String statusSql = "SELECT status, COUNT(*) as count FROM agile_story " +
                          "WHERE project_id = ? GROUP BY status";
        List<Map<String, Object>> byStatus = jdbcTemplate.queryForList(statusSql, projectId);
        result.put("byStatus", byStatus);

        // 按类型统计
        String typeSql = "SELECT story_type, COUNT(*) as count FROM agile_story " +
                        "WHERE project_id = ? GROUP BY story_type";
        List<Map<String, Object>> byType = jdbcTemplate.queryForList(typeSql, projectId);
        result.put("byType", byType);

        // 按优先级统计
        String prioritySql = "SELECT priority, COUNT(*) as count FROM agile_story " +
                            "WHERE project_id = ? GROUP BY priority";
        List<Map<String, Object>> byPriority = jdbcTemplate.queryForList(prioritySql, projectId);
        result.put("byPriority", byPriority);

        return Result.ok(result);
    }

    /**
     * 获取团队效能指标
     */
    @Operation(summary = "获取团队效能指标")
    @GetMapping("/team-efficiency")
    public Result<Map<String, Object>> getTeamEfficiency(@RequestParam Long projectId) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        LocalDate startDate = LocalDate.now().minusDays(29);

        Map<String, Object> result = new HashMap<>();

        // 人均故事点
        String storyPointsSql = "SELECT tm.real_name, SUM(s.story_points) as total_points " +
                               "FROM agile_story s " +
                               "JOIN agile_team_member tm ON s.assignee_id = tm.user_id " +
                               "WHERE s.project_id = ? AND s.status = 'DONE' " +
                               "AND CAST(s.completed_at AS DATE) >= ? " +
                               "GROUP BY tm.real_name ORDER BY total_points DESC";
        List<Map<String, Object>> storyPoints = jdbcTemplate.queryForList(storyPointsSql, projectId, Date.valueOf(startDate));
        result.put("storyPointsByMember", storyPoints);

        // 工时利用率
        String hoursSql = "SELECT tm.real_name, " +
                         "SUM(s.actual_hours) as actual_hours, " +
                         "SUM(s.estimated_hours) as estimated_hours, " +
                         "SUM(s.actual_hours) * 100.0 / NULLIF(SUM(s.estimated_hours), 0) as utilization " +
                         "FROM agile_story s " +
                         "JOIN agile_team_member tm ON s.assignee_id = tm.user_id " +
                         "WHERE s.project_id = ? AND s.status = 'DONE' " +
                         "AND CAST(s.completed_at AS DATE) >= ? " +
                         "GROUP BY tm.real_name";
        List<Map<String, Object>> hours = jdbcTemplate.queryForList(hoursSql, projectId, Date.valueOf(startDate));
        result.put("utilizationByMember", hours);

        return Result.ok(result);
    }
}
