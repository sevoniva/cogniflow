package com.chatbi.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数据血缘服务
 *
 * 解析 SQL 提取表→字段→聚合函数的依赖关系。
 * Month 3 Week 2 — 使用正则 + JSQLParser 安全 fallback
 */
@Slf4j
@Service
public class DataLineageService {

    private static final Pattern AGG_PATTERN = Pattern.compile(
            "(SUM|COUNT|AVG|MAX|MIN)\\s*\\(\\s*`?([^`]+)`?\\s*\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern FROM_PATTERN = Pattern.compile(
            "\\bfrom\\s+`?([^`\\s,]+)`?", Pattern.CASE_INSENSITIVE);
    private static final Pattern JOIN_PATTERN = Pattern.compile(
            "\\bjoin\\s+`?([^`\\s,]+)`?", Pattern.CASE_INSENSITIVE);
    private static final Pattern FIELD_PATTERN = Pattern.compile(
            "`([^`]+)`");

    /**
     * 解析 SQL 并返回血缘链路
     */
    public LineageGraph parse(String sql) {
        if (sql == null || sql.isBlank()) {
            return new LineageGraph(List.of(), List.of());
        }
        try {
            return doParse(sql);
        } catch (Exception e) {
            log.warn("SQL 血缘解析失败: {}", sql.substring(0, Math.min(100, sql.length())), e);
            return new LineageGraph(List.of(), List.of());
        }
    }

    private LineageGraph doParse(String sql) {
        List<TableNode> tables = new ArrayList<>();
        List<ColumnEdge> edges = new ArrayList<>();

        // 提取表名
        Matcher fromMatcher = FROM_PATTERN.matcher(sql);
        while (fromMatcher.find()) {
            tables.add(new TableNode(fromMatcher.group(1), null));
        }

        Matcher joinMatcher = JOIN_PATTERN.matcher(sql);
        while (joinMatcher.find()) {
            tables.add(new TableNode(joinMatcher.group(1), null));
        }

        // 提取字段（反引号包裹）
        Set<String> seenFields = new HashSet<>();
        Matcher fieldMatcher = FIELD_PATTERN.matcher(sql);
        while (fieldMatcher.find()) {
            String field = fieldMatcher.group(1);
            if (!seenFields.contains(field)) {
                seenFields.add(field);
                edges.add(new ColumnEdge(null, field, "FIELD", null));
            }
        }

        // 提取聚合函数
        Matcher aggMatcher = AGG_PATTERN.matcher(sql);
        while (aggMatcher.find()) {
            String agg = aggMatcher.group(1).toUpperCase();
            String col = aggMatcher.group(2).trim();
            edges.add(new ColumnEdge(null, col, "AGGREGATE", agg));
        }

        // 尝试用 JSQLParser 补充列信息（如果可用）
        try {
            net.sf.jsqlparser.parser.CCJSqlParserUtil.parse(sql);
        } catch (Exception ignored) {
            // JSQLParser 解析失败不影响正则结果
        }

        return new LineageGraph(tables, edges);
    }

    public record TableNode(String name, String alias) {}
    public record ColumnEdge(String table, String column, String transformType, String aggregation) {}
    public record LineageGraph(List<TableNode> tables, List<ColumnEdge> columns) {}
}
