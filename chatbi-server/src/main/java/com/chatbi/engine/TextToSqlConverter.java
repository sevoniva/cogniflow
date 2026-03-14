package com.chatbi.engine;

import com.chatbi.entity.Metric;
import com.chatbi.entity.Synonym;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文本到 SQL 转换器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TextToSqlConverter {

    private static final Pattern DATE_PATTERN = Pattern.compile("(今日|今天|本周|本月|本季度|本年|昨天|上周|上月|去年|最近\\d+天|最近\\d+月|\\d{4}-\\d{2}-\\d{2})");
    private static final Pattern METRIC_PATTERN = Pattern.compile("([\\u4e00-\\u9fa5a-zA-Z_]+)(的)?(总和|合计|平均值|平均|数量|个数|最大值|最大|最小值|最小|求和)");
    private static final Pattern RANGE_PATTERN = Pattern.compile("(\\d+)到(\\d+)|between\\s+(\\d+)\\s+and\\s+(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern TOP_PATTERN = Pattern.compile("(前|top|最高|最低|排名前)\\s*(\\d+)\\s*(名|个|条)?");
    private static final Pattern COMPARISON_PATTERN = Pattern.compile("([\\u4e00-\\u9fa5a-zA-Z_]+)\\s*(大于|小于|等于|不等于|不低于|不超过|超过|低于)\\s*(\\d+(\\.\\d+)?)");
    private static final Pattern PERCENTAGE_PATTERN = Pattern.compile("(占比|百分比|比例|份额)");
    private static final Pattern GROWTH_PATTERN = Pattern.compile("(增长率|增长|同比|环比|变化)");
    private static final Pattern JOIN_PATTERN = Pattern.compile("(关联|连接|join)\\s*(\\w+)", Pattern.CASE_INSENSITIVE);

    /**
     * 将自然语言转换为 SQL
     */
    public String convert(String naturalLanguage, List<Metric> metrics, List<Synonym> synonyms) {
        // 1. 同义词替换
        String normalizedText = normalizeText(naturalLanguage, synonyms);

        // 2. 解析查询意图
        QueryStatement statement = parseQuery(normalizedText, metrics);

        // 3. 生成 SQL
        return generateSql(statement);
    }

    /**
     * 同义词替换和文本标准化
     */
    private String normalizeText(String text, List<Synonym> synonyms) {
        String result = text;

        // 替换同义词
        for (Synonym synonym : synonyms) {
            if (synonym.getAliases() != null) {
                for (String alias : synonym.getAliases()) {
                    result = result.replaceAll("\\b" + alias + "\\b", synonym.getStandardWord());
                }
            }
        }

        // 标准化时间表达
        result = normalizeDateExpression(result);

        log.debug("文本标准化：{} -> {}", text, result);
        return result;
    }

    /**
     * 标准化时间表达
     */
    private String normalizeDateExpression(String text) {
        String result = text;

        // 替换相对时间表达
        result = result.replaceAll("今日", "CURDATE()");
        result = result.replaceAll("本月", "DATE_FORMAT(CURDATE(), '%Y-%m')");
        result = result.replaceAll("本年", "YEAR(CURDATE())");

        log.debug("时间表达标准化：{}", result);
        return result;
    }

    /**
     * 解析查询语句
     */
    private QueryStatement parseQuery(String text, List<Metric> metrics) {
        QueryStatement statement = new QueryStatement();

        // 查找指标
        Map<String, Metric> metricMap = new HashMap<>();
        for (Metric metric : metrics) {
            metricMap.put(metric.getName().toLowerCase(), metric);
            metricMap.put(metric.getCode().toLowerCase(), metric);
        }

        // 检查是否需要去重
        if (text.contains("去重") || text.contains("distinct") || text.contains("不重复")) {
            statement.setDistinct(true);
        }

        // 解析聚合函数
        parseAggregations(text, metrics, statement);

        // 解析维度
        parseDimensions(text, statement);

        // 解析条件
        parseConditions(text, statement);

        // 解析JOIN
        parseJoins(text, statement);

        // 解析排序
        parseOrderBy(text, statement);

        // 解析LIMIT和OFFSET
        parseLimit(text, statement);
        parseOffset(text, statement);

        // 解析HAVING
        parseHaving(text, statement);

        // 解析特殊查询（TOP N、百分比、增长率等）
        parseSpecialQueries(text, statement);

        // 设置表名
        if (statement.getTable() == null && !metrics.isEmpty()) {
            statement.setTable(metrics.get(0).getTableName());
        }

        return statement;
    }

    /**
     * 解析聚合字段
     */
    private void parseAggregations(String text, List<Metric> metrics, QueryStatement statement) {
        // 先尝试使用正则匹配
        Matcher matcher = METRIC_PATTERN.matcher(text);
        boolean foundAggregation = false;

        while (matcher.find()) {
            String metricName = matcher.group(1);
            String aggFunction = matcher.group(3);

            // 查找匹配的指标
            for (Metric metric : metrics) {
                if (metric.getName().equalsIgnoreCase(metricName) ||
                        metric.getCode().equalsIgnoreCase(metricName)) {

                    String sqlFunction = mapAggFunction(aggFunction);
                    String alias = metricName + "_" + aggFunction;

                    statement.getAggregations().add(
                            new QueryStatement.AggregationColumn(
                                    metric.getCode(),
                                    sqlFunction,
                                    alias
                            )
                    );

                    if (statement.getTable() == null) {
                        statement.setTable(metric.getTableName());
                    }
                    foundAggregation = true;
                    break;
                }
            }
        }

        // 如果正则没匹配到，尝试关键词匹配
        if (!foundAggregation) {
            String lowerText = text.toLowerCase();

            // 检测聚合函数关键词
            String aggFunction = null;
            if (lowerText.contains("总和") || lowerText.contains("合计") || lowerText.contains("求和")) {
                aggFunction = "SUM";
            } else if (lowerText.contains("平均") || lowerText.contains("平均值")) {
                aggFunction = "AVG";
            } else if (lowerText.contains("数量") || lowerText.contains("个数") || lowerText.contains("count")) {
                aggFunction = "COUNT";
            } else if (lowerText.contains("最大") || lowerText.contains("最高")) {
                aggFunction = "MAX";
            } else if (lowerText.contains("最小") || lowerText.contains("最低")) {
                aggFunction = "MIN";
            }

            // 查找指标
            if (aggFunction != null) {
                for (Metric metric : metrics) {
                    if (lowerText.contains(metric.getName().toLowerCase()) ||
                        lowerText.contains(metric.getCode().toLowerCase())) {

                        statement.getAggregations().add(
                            new QueryStatement.AggregationColumn(
                                metric.getCode(),
                                aggFunction,
                                metric.getName() + "_result"
                            )
                        );

                        if (statement.getTable() == null) {
                            statement.setTable(metric.getTableName());
                        }
                        foundAggregation = true;
                        break;
                    }
                }
            }
        }

        // 如果没有找到聚合函数，检查是否有明确的指标名
        if (!foundAggregation && statement.getAggregations().isEmpty()) {
            for (Metric metric : metrics) {
                if (text.contains(metric.getName()) || text.contains(metric.getCode())) {
                    statement.getColumns().add(metric.getCode());
                    if (statement.getTable() == null) {
                        statement.setTable(metric.getTableName());
                    }
                }
            }
        }
    }

    /**
     * 映射聚合函数
     */
    private String mapAggFunction(String chineseFunction) {
        return switch (chineseFunction) {
            case "总和", "合计", "求和" -> "SUM";
            case "平均值", "平均" -> "AVG";
            case "数量", "个数" -> "COUNT";
            case "最大", "最大值" -> "MAX";
            case "最小", "最小值" -> "MIN";
            default -> "SUM";
        };
    }

    /**
     * 解析维度
     */
    private void parseDimensions(String text, QueryStatement statement) {
        // 检查常见维度关键词
        String[] dimensionKeywords = {"按", "分组", "group by"};

        for (String keyword : dimensionKeywords) {
            int index = text.indexOf(keyword);
            if (index != -1) {
                // 提取维度字段
                String afterKeyword = text.substring(index + keyword.length()).trim();
                String[] parts = afterKeyword.split(" | 查询 | 统计 | 哪里|何时");

                if (parts.length > 0) {
                    String dimension = parts[0].trim();
                    if (!dimension.isEmpty() && !isReservedWord(dimension)) {
                        statement.getGroupBy().add(dimension);
                    }
                }
            }
        }
    }

    /**
     * 解析条件
     */
    private void parseConditions(String text, QueryStatement statement) {
        // 解析时间条件
        Matcher dateMatcher = DATE_PATTERN.matcher(text);
        while (dateMatcher.find()) {
            String dateExpr = dateMatcher.group(1);
            parseDateCondition(dateExpr, statement);
        }

        // 解析比较条件
        parseComparisonConditions(text, statement);
    }

    /**
     * 解析时间条件
     */
    private void parseDateCondition(String dateExpr, QueryStatement statement) {
        String dateCondition = switch (dateExpr) {
            case "今日", "今天" -> "DATE(created_at) = CURDATE()";
            case "昨天" -> "DATE(created_at) = DATE_SUB(CURDATE(), INTERVAL 1 DAY)";
            case "本周" -> "YEARWEEK(created_at, 1) = YEARWEEK(CURDATE(), 1)";
            case "上周" -> "YEARWEEK(created_at, 1) = YEARWEEK(DATE_SUB(CURDATE(), INTERVAL 1 WEEK), 1)";
            case "本月" -> "DATE_FORMAT(created_at, '%Y-%m') = DATE_FORMAT(CURDATE(), '%Y-%m')";
            case "上月" -> "DATE_FORMAT(created_at, '%Y-%m') = DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 1 MONTH), '%Y-%m')";
            case "本季度" -> "QUARTER(created_at) = QUARTER(CURDATE()) AND YEAR(created_at) = YEAR(CURDATE())";
            case "本年", "今年" -> "YEAR(created_at) = YEAR(CURDATE())";
            case "去年" -> "YEAR(created_at) = YEAR(DATE_SUB(CURDATE(), INTERVAL 1 YEAR))";
            default -> {
                // 处理"最近N天"、"最近N月"
                if (dateExpr.startsWith("最近")) {
                    if (dateExpr.contains("天")) {
                        String days = dateExpr.replaceAll("[^0-9]", "");
                        yield "created_at >= DATE_SUB(CURDATE(), INTERVAL " + days + " DAY)";
                    } else if (dateExpr.contains("月")) {
                        String months = dateExpr.replaceAll("[^0-9]", "");
                        yield "created_at >= DATE_SUB(CURDATE(), INTERVAL " + months + " MONTH)";
                    }
                }
                // 处理具体日期
                if (dateExpr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    yield "DATE(created_at) = '" + dateExpr + "'";
                }
                yield null;
            }
        };

        if (dateCondition != null) {
            statement.getConditions().add(new QueryStatement.Condition("created_at", "CUSTOM", dateCondition));
        }
    }

    /**
     * 解析比较条件
     */
    private void parseComparisonConditions(String text, QueryStatement statement) {
        // 解析范围条件 (BETWEEN)
        Matcher rangeMatcher = RANGE_PATTERN.matcher(text);
        if (rangeMatcher.find()) {
            String start = rangeMatcher.group(1) != null ? rangeMatcher.group(1) : rangeMatcher.group(3);
            String end = rangeMatcher.group(2) != null ? rangeMatcher.group(2) : rangeMatcher.group(4);
            // 需要根据上下文确定字段名，这里暂时使用通用字段
            statement.getConditions().add(
                new QueryStatement.Condition("value", "BETWEEN", start + " AND " + end)
            );
        }

        // 解析比较条件
        Matcher compMatcher = COMPARISON_PATTERN.matcher(text);
        while (compMatcher.find()) {
            String field = compMatcher.group(1);
            String operatorChinese = compMatcher.group(2);
            String value = compMatcher.group(3);

            String operator = switch (operatorChinese) {
                case "大于", "超过" -> ">";
                case "小于", "低于" -> "<";
                case "等于" -> "=";
                case "不等于" -> "!=";
                case "不低于" -> ">=";
                case "不超过" -> "<=";
                default -> "=";
            };

            statement.getConditions().add(new QueryStatement.Condition(field, operator, value));
        }

        // 解析 IN 条件
        Pattern inPattern = Pattern.compile("(\\w+)(在|属于|包含于)\\s*\\(([^)]+)\\)");
        Matcher inMatcher = inPattern.matcher(text);
        if (inMatcher.find()) {
            String field = inMatcher.group(1);
            String values = inMatcher.group(3);
            statement.getConditions().add(new QueryStatement.Condition(field, "IN", "(" + values + ")"));
        }

        // 解析 LIKE 条件
        Pattern likePattern = Pattern.compile("(\\w+)(包含|含有|like)\\s*['\"]?([^'\"\\s]+)['\"]?");
        Matcher likeMatcher = likePattern.matcher(text);
        if (likeMatcher.find()) {
            String field = likeMatcher.group(1);
            String value = likeMatcher.group(3);
            statement.getConditions().add(new QueryStatement.Condition(field, "LIKE", "'%" + value + "%'"));
        }
    }

    /**
     * 解析排序
     */
    private void parseOrderBy(String text, QueryStatement statement) {
        if (text.contains("排序") || text.contains("order by")) {
            // 查找排序字段和方向
            Pattern pattern = Pattern.compile("(\\w+)(升序 | 降序 | 从高到低 | 从低到高)");
            Matcher matcher = pattern.matcher(text);

            if (matcher.find()) {
                String field = matcher.group(1);
                String direction = matcher.group(2);

                String sqlDirection = switch (direction) {
                    case "升序", "从低到高" -> "ASC";
                    case "降序", "从高到低" -> "DESC";
                    default -> "DESC";
                };

                statement.getOrderBy().add(new QueryStatement.OrderBy(field, sqlDirection));
            }
        }
    }

    /**
     * 解析 LIMIT
     */
    private void parseLimit(String text, QueryStatement statement) {
        Pattern pattern = Pattern.compile("(前|取|显示|top)\\s*(\\d+)\\s*(条|个|名)?");
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            statement.setLimit(Integer.parseInt(matcher.group(2)));
        } else if (statement.getLimit() == null) {
            // 默认限制100条
            statement.setLimit(100);
        }
    }

    /**
     * 解析 OFFSET
     */
    private void parseOffset(String text, QueryStatement statement) {
        Pattern pattern = Pattern.compile("跳过(\\d+)(条|个)?");
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            statement.setOffset(Integer.parseInt(matcher.group(1)));
        }
    }

    /**
     * 解析 JOIN
     */
    private void parseJoins(String text, QueryStatement statement) {
        Matcher matcher = JOIN_PATTERN.matcher(text);

        while (matcher.find()) {
            String tableName = matcher.group(2);
            // 默认使用 INNER JOIN
            statement.getJoins().add(
                new QueryStatement.JoinClause("INNER", tableName, "id = " + tableName + "_id")
            );
        }
    }

    /**
     * 解析 HAVING 子句
     */
    private void parseHaving(String text, QueryStatement statement) {
        if (text.contains("having") || text.contains("聚合后") || text.contains("分组后")) {
            // 解析 HAVING 条件
            Pattern pattern = Pattern.compile("(sum|avg|count|max|min)\\s*\\(\\s*(\\w+)\\s*\\)\\s*(>|<|=|>=|<=)\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(text);

            while (matcher.find()) {
                String function = matcher.group(1).toUpperCase();
                String field = matcher.group(2);
                String operator = matcher.group(3);
                String value = matcher.group(4);

                String havingField = function + "(" + field + ")";
                statement.getHaving().add(new QueryStatement.Condition(havingField, operator, value));
            }
        }
    }

    /**
     * 解析特殊查询（TOP N、百分比、增长率等）
     */
    private void parseSpecialQueries(String text, QueryStatement statement) {
        // TOP N 查询
        Matcher topMatcher = TOP_PATTERN.matcher(text);
        if (topMatcher.find()) {
            int topN = Integer.parseInt(topMatcher.group(2));
            statement.setLimit(topN);

            // 如果是"最高"或"最低"，添加排序
            String direction = topMatcher.group(1);
            if (direction.contains("最高") || direction.contains("前")) {
                // 需要根据聚合字段排序
                if (!statement.getAggregations().isEmpty()) {
                    String firstAgg = statement.getAggregations().get(0).getAlias();
                    if (firstAgg != null) {
                        statement.getOrderBy().add(new QueryStatement.OrderBy(firstAgg, "DESC"));
                    }
                }
            } else if (direction.contains("最低")) {
                if (!statement.getAggregations().isEmpty()) {
                    String firstAgg = statement.getAggregations().get(0).getAlias();
                    if (firstAgg != null) {
                        statement.getOrderBy().add(new QueryStatement.OrderBy(firstAgg, "ASC"));
                    }
                }
            }
        }

        // 百分比查询
        if (PERCENTAGE_PATTERN.matcher(text).find()) {
            // 添加百分比计算
            if (!statement.getAggregations().isEmpty()) {
                QueryStatement.AggregationColumn firstAgg = statement.getAggregations().get(0);
                String percentageExpr = firstAgg.getFunction() + "(" + firstAgg.getColumn() + ") * 100.0 / (SELECT " +
                    firstAgg.getFunction() + "(" + firstAgg.getColumn() + ") FROM " + statement.getTable() + ")";
                firstAgg.setAlias("percentage");
            }
        }
    }

    /**
     * 判断是否为保留字
     */
    private boolean isReservedWord(String word) {
        Set<String> reservedWords = Set.of(
                "的", "和", "或", "与", "查询", "统计", "显示", "查看", "分析",
                "排序", "分组", "升序", "降序", "从", "到", "高", "低"
        );
        return reservedWords.contains(word);
    }

    /**
     * 生成 SQL
     */
    private String generateSql(QueryStatement statement) {
        StringBuilder sql = new StringBuilder();

        // SELECT 子句
        sql.append("SELECT ");

        if (statement.isDistinct()) {
            sql.append("DISTINCT ");
        }

        List<String> selectParts = new ArrayList<>();

        // 添加普通列
        for (String column : statement.getColumns()) {
            selectParts.add(column);
        }

        // 添加聚合列
        for (QueryStatement.AggregationColumn agg : statement.getAggregations()) {
            String aggExpr = agg.getFunction() + "(" + agg.getColumn() + ")";
            if (agg.getAlias() != null) {
                aggExpr += " AS " + agg.getAlias();
            }
            selectParts.add(aggExpr);
        }

        if (selectParts.isEmpty()) {
            selectParts.add("*");
        }

        sql.append(String.join(", ", selectParts));

        // FROM 子句
        if (statement.getTable() != null) {
            sql.append(" FROM ").append(statement.getTable());
        }

        // JOIN 子句
        for (QueryStatement.JoinClause join : statement.getJoins()) {
            sql.append(" ").append(join.getJoinType()).append(" JOIN ").append(join.getTable());
            if (join.getAlias() != null) {
                sql.append(" AS ").append(join.getAlias());
            }
            sql.append(" ON ").append(join.getOnCondition());
        }

        // WHERE 子句
        if (!statement.getConditions().isEmpty()) {
            sql.append(" WHERE ");
            List<String> conditionStrs = new ArrayList<>();
            for (QueryStatement.Condition cond : statement.getConditions()) {
                String condStr;
                if ("CUSTOM".equals(cond.getOperator())) {
                    // 自定义条件（如复杂的时间条件）
                    condStr = cond.getValue().toString();
                } else if ("BETWEEN".equals(cond.getOperator())) {
                    condStr = cond.getField() + " BETWEEN " + cond.getValue();
                } else if ("IN".equals(cond.getOperator())) {
                    condStr = cond.getField() + " IN " + cond.getValue();
                } else if ("LIKE".equals(cond.getOperator())) {
                    condStr = cond.getField() + " LIKE " + cond.getValue();
                } else {
                    condStr = cond.getField() + " " + cond.getOperator() + " ";
                    if (cond.getValue() instanceof String && !cond.getValue().toString().startsWith("'")) {
                        condStr += "'" + cond.getValue() + "'";
                    } else {
                        condStr += cond.getValue();
                    }
                }
                conditionStrs.add(condStr);
            }
            sql.append(String.join(" AND ", conditionStrs));
        }

        // GROUP BY 子句
        if (!statement.getGroupBy().isEmpty()) {
            sql.append(" GROUP BY ").append(String.join(", ", statement.getGroupBy()));
        }

        // HAVING 子句
        if (!statement.getHaving().isEmpty()) {
            sql.append(" HAVING ");
            List<String> havingStrs = new ArrayList<>();
            for (QueryStatement.Condition cond : statement.getHaving()) {
                String havingStr = cond.getField() + " " + cond.getOperator() + " " + cond.getValue();
                havingStrs.add(havingStr);
            }
            sql.append(String.join(" AND ", havingStrs));
        }

        // ORDER BY 子句
        if (!statement.getOrderBy().isEmpty()) {
            sql.append(" ORDER BY ");
            List<String> orderParts = new ArrayList<>();
            for (QueryStatement.OrderBy order : statement.getOrderBy()) {
                orderParts.add(order.getColumn() + " " + order.getDirection());
            }
            sql.append(String.join(", ", orderParts));
        }

        // LIMIT 子句
        if (statement.getLimit() != null) {
            sql.append(" LIMIT ").append(statement.getLimit());
        }

        // OFFSET 子句
        if (statement.getOffset() != null) {
            sql.append(" OFFSET ").append(statement.getOffset());
        }

        return sql.toString();
    }
}
