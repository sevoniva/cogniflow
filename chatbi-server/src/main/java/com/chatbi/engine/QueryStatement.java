package com.chatbi.engine;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 查询语句表示
 */
@Data
@NoArgsConstructor
public class QueryStatement {

    /**
     * 查询类型：SELECT
     */
    private String queryType = "SELECT";

    /**
     * 查询的表
     */
    private String table;

    /**
     * 查询字段
     */
    private List<String> columns = new ArrayList<>();

    /**
     * 聚合字段
     */
    private List<AggregationColumn> aggregations = new ArrayList<>();

    /**
     * WHERE 条件
     */
    private List<Condition> conditions = new ArrayList<>();

    /**
     * GROUP BY 字段
     */
    private List<String> groupBy = new ArrayList<>();

    /**
     * ORDER BY 字段
     */
    private List<OrderBy> orderBy = new ArrayList<>();

    /**
     * LIMIT 限制
     */
    private Integer limit;

    /**
     * OFFSET 偏移
     */
    private Integer offset;

    /**
     * JOIN 子句
     */
    private List<JoinClause> joins = new ArrayList<>();

    /**
     * HAVING 条件
     */
    private List<Condition> having = new ArrayList<>();

    /**
     * 子查询
     */
    private QueryStatement subQuery;

    /**
     * 是否去重
     */
    private boolean distinct = false;

    /**
     * JOIN 子句
     */
    @Data
    @NoArgsConstructor
    public static class JoinClause {
        private String joinType; // INNER, LEFT, RIGHT, FULL
        private String table;
        private String alias;
        private String onCondition;

        public JoinClause(String joinType, String table, String onCondition) {
            this.joinType = joinType;
            this.table = table;
            this.onCondition = onCondition;
        }

        public JoinClause(String joinType, String table, String alias, String onCondition) {
            this.joinType = joinType;
            this.table = table;
            this.alias = alias;
            this.onCondition = onCondition;
        }
    }

    /**
     * 聚合字段
     */
    @Data
    @NoArgsConstructor
    public static class AggregationColumn {
        private String column;
        private String function; // SUM, COUNT, AVG, MAX, MIN
        private String alias;

        public AggregationColumn(String column, String function, String alias) {
            this.column = column;
            this.function = function;
            this.alias = alias;
        }
    }

    /**
     * 条件
     */
    @Data
    @NoArgsConstructor
    public static class Condition {
        private String field;
        private String operator; // =, !=, >, <, >=, <=, LIKE, IN, BETWEEN
        private Object value;
        private String logic; // AND, OR

        public Condition(String field, String operator, Object value) {
            this.field = field;
            this.operator = operator;
            this.value = value;
            this.logic = "AND";
        }

        public Condition(String field, String operator, Object value, String logic) {
            this.field = field;
            this.operator = operator;
            this.value = value;
            this.logic = logic;
        }
    }

    /**
     * 排序
     */
    @Data
    @NoArgsConstructor
    public static class OrderBy {
        private String column;
        private String direction; // ASC, DESC

        public OrderBy(String column, String direction) {
            this.column = column;
            this.direction = direction;
        }
    }
}
