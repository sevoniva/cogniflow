package com.chatbi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.Limit;
import net.sf.jsqlparser.statement.select.ParenthesedSelect;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.select.WithItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 查询治理服务。
 *
 * <p>负责统一做 SQL 只读校验、系统表访问拦截、行级权限注入以及结果列血缘提取。</p>
 *
 * <p>改造说明：</p>
 * <ul>
 *   <li>支持子查询、UNION、CTE（WITH 子句）</li>
 *   <li>递归遍历 AST，对所有 PlainSelect 注入权限和 LIMIT</li>
 *   <li>递归收集所有表引用进行安全校验</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryGovernanceService {

    private static final Set<String> PROTECTED_TABLES = Set.of(
        "sys_user",
        "sys_role",
        "sys_permission",
        "sys_user_role",
        "sys_role_permission",
        "audit_log",
        "ai_provider_setting",
        "ai_runtime_setting"
    );

    private final DataPermissionService dataPermissionService;

    @Value("${app.query-governance.max-rows:500}")
    private long maxRows;

    public ValidationResult validate(String sql) {
        String normalizedSql = normalizeSql(sql);
        if (normalizedSql == null) {
            return ValidationResult.invalid("SQL 不能为空");
        }
        if (containsForbiddenTokens(normalizedSql)) {
            return ValidationResult.invalid("仅支持单条只读 SELECT 查询，禁止注释和多语句执行");
        }

        Statement statement;
        try {
            statement = CCJSqlParserUtil.parse(normalizedSql);
        } catch (JSQLParserException e) {
            log.warn("SQL 解析失败 - sql: {}", normalizedSql, e);
            return ValidationResult.invalid("SQL 解析失败，请检查语法是否正确");
        }

        if (!(statement instanceof Select select)) {
            return ValidationResult.invalid("仅支持 SELECT 查询");
        }

        // 改造：递归收集所有表引用进行校验（支持子查询/UNION/CTE）
        List<PlainSelect> allPlainSelects = collectPlainSelects(select);
        if (allPlainSelects.isEmpty()) {
            return ValidationResult.invalid("未识别到有效的查询语句");
        }

        Map<String, TableRef> allTables = new LinkedHashMap<>();
        for (PlainSelect plainSelect : allPlainSelects) {
            Map<String, TableRef> tables = resolveTables(plainSelect);
            allTables.putAll(tables);
        }

        ValidationResult tableValidation = validateTables(allTables.values());
        if (!tableValidation.valid()) {
            return tableValidation;
        }

        return ValidationResult.passed();
    }

    public GovernedQuery govern(Long userId, String sql) {
        ValidationResult validation = validate(sql);
        if (!validation.valid()) {
            throw new IllegalArgumentException(validation.message());
        }

        String normalizedSql = normalizeSql(sql);
        try {
            Select select = (Select) CCJSqlParserUtil.parse(normalizedSql);

            // 改造：递归收集所有 PlainSelect，对每个注入权限和 LIMIT
            List<PlainSelect> allPlainSelects = collectPlainSelects(select);
            Set<String> allTableNames = new LinkedHashSet<>();
            Map<String, ColumnBinding> allBindings = new LinkedHashMap<>();
            Set<String> allWildcardTables = new LinkedHashSet<>();

            for (PlainSelect plainSelect : allPlainSelects) {
                Map<String, TableRef> tableRefByAlias = resolveTables(plainSelect);
                applyPermissionConditions(userId, plainSelect, tableRefByAlias.values());
                enforceLimit(plainSelect);

                QueryLineage lineage = buildLineage(plainSelect, tableRefByAlias);
                allTableNames.addAll(lineage.tableNames());
                allBindings.putAll(lineage.columnBindings());
                allWildcardTables.addAll(lineage.wildcardTables());
            }

            String governedSql = select.toString();
            String cacheScopeKey = userId == null ? "public" : "user:" + userId;

            return new GovernedQuery(
                normalizedSql,
                governedSql,
                cacheScopeKey,
                new ArrayList<>(allTableNames),
                allBindings,
                allWildcardTables
            );
        } catch (JSQLParserException e) {
            log.error("SQL 治理失败 - sql: {}", normalizedSql, e);
            throw new IllegalArgumentException("SQL 治理失败，请调整查询后重试");
        }
    }

    /**
     * 改造：递归收集 Select 中的所有 PlainSelect（支持子查询/UNION/CTE）
     */
    private List<PlainSelect> collectPlainSelects(Select select) {
        List<PlainSelect> result = new ArrayList<>();
        if (select == null) return result;

        // 收集主查询体中的 PlainSelect
        collectPlainSelects(select.getSelectBody(), result);

        // 收集 CTE（WITH 子句）中的 PlainSelect
        List<WithItem> withItems = select.getWithItemsList();
        if (withItems != null) {
            for (WithItem withItem : withItems) {
                collectPlainSelects(withItem.getSelectBody(), result);
            }
        }

        return result;
    }

    private void collectPlainSelects(Select selectBody, List<PlainSelect> result) {
        if (selectBody instanceof PlainSelect plainSelect) {
            result.add(plainSelect);

            // 递归收集 FROM 子句中的子查询
            if (plainSelect.getFromItem() instanceof ParenthesedSelect parenthesedSelect) {
                collectPlainSelects(parenthesedSelect.getSelect(), result);
            }

            // 递归收集 JOIN 中的子查询
            if (plainSelect.getJoins() != null) {
                for (Join join : plainSelect.getJoins()) {
                    if (join.getRightItem() instanceof ParenthesedSelect parenthesedSelect) {
                        collectPlainSelects(parenthesedSelect.getSelect(), result);
                    }
                }
            }
        } else if (selectBody instanceof SetOperationList setOpList) {
            // UNION / INTERSECT / EXCEPT
            for (Select body : setOpList.getSelects()) {
                collectPlainSelects(body, result);
            }
        }
    }

    private ValidationResult validateTables(Collection<TableRef> tables) {
        if (tables.isEmpty()) {
            return ValidationResult.invalid("未识别到可查询的数据表");
        }

        for (TableRef tableRef : tables) {
            String tableName = normalizeIdentifier(tableRef.tableName());
            if (PROTECTED_TABLES.contains(tableName) || tableName.startsWith("sys_")) {
                return ValidationResult.invalid("禁止查询系统治理表：" + tableRef.tableName());
            }
        }
        return ValidationResult.passed();
    }

    private void applyPermissionConditions(Long userId, PlainSelect plainSelect, Collection<TableRef> tableRefs) {
        Expression permissionExpression = null;
        for (TableRef tableRef : tableRefs) {
            List<Expression> expressions = dataPermissionService.buildExpressions(
                userId,
                tableRef.tableName(),
                tableRef.aliasOrName()
            );
            for (Expression expression : expressions) {
                permissionExpression = permissionExpression == null
                    ? expression
                    : new AndExpression(permissionExpression, expression);
            }
        }

        if (permissionExpression == null) {
            return;
        }

        if (plainSelect.getWhere() == null) {
            plainSelect.setWhere(permissionExpression);
        } else {
            plainSelect.setWhere(new AndExpression(plainSelect.getWhere(), permissionExpression));
        }
    }

    private void enforceLimit(PlainSelect plainSelect) {
        Limit limit = plainSelect.getLimit();
        if (limit == null) {
            Limit governedLimit = new Limit();
            governedLimit.setRowCount(new LongValue(maxRows));
            plainSelect.setLimit(governedLimit);
            return;
        }

        Expression rowCount = limit.getRowCount();
        if (!(rowCount instanceof LongValue longValue)) {
            limit.setRowCount(new LongValue(maxRows));
            return;
        }

        if (longValue.getValue() > maxRows) {
            limit.setRowCount(new LongValue(maxRows));
        }
    }

    private QueryLineage buildLineage(PlainSelect plainSelect, Map<String, TableRef> tableRefByAlias) {
        Set<String> tableNames = new LinkedHashSet<>();
        tableRefByAlias.values().forEach(tableRef -> tableNames.add(tableRef.tableName()));

        Map<String, ColumnBinding> bindings = new LinkedHashMap<>();
        Set<String> wildcardTables = new LinkedHashSet<>();
        List<SelectItem<?>> selectItems = plainSelect.getSelectItems();
        if (selectItems == null) {
            return new QueryLineage(tableNames, bindings, wildcardTables);
        }

        for (SelectItem<?> selectItem : selectItems) {
            Expression expression = selectItem.getExpression();
            if (expression instanceof AllColumns) {
                if (tableNames.size() == 1) {
                    wildcardTables.add(tableNames.iterator().next());
                }
                continue;
            }

            if (expression instanceof AllTableColumns allTableColumns) {
                TableRef tableRef = resolveTableRef(allTableColumns.getTable(), tableRefByAlias);
                if (tableRef != null) {
                    wildcardTables.add(tableRef.tableName());
                }
                continue;
            }
            if (!(expression instanceof Column column)) {
                continue;
            }

            TableRef tableRef = resolveTableRef(column.getTable(), tableRefByAlias);
            if (tableRef == null && tableNames.size() == 1) {
                String onlyTable = tableNames.iterator().next();
                tableRef = new TableRef(onlyTable, onlyTable);
            }
            if (tableRef == null) {
                continue;
            }

            String resultColumn = selectItem.getAlias() != null && selectItem.getAlias().getName() != null
                ? selectItem.getAlias().getName()
                : column.getColumnName();
            bindings.put(normalizeIdentifier(resultColumn), new ColumnBinding(resultColumn, tableRef.tableName(), column.getColumnName()));
        }

        return new QueryLineage(tableNames, bindings, wildcardTables);
    }

    private Map<String, TableRef> resolveTables(PlainSelect plainSelect) {
        Map<String, TableRef> tableRefs = new LinkedHashMap<>();
        registerTable(plainSelect.getFromItem(), tableRefs);
        if (plainSelect.getJoins() != null) {
            for (Join join : plainSelect.getJoins()) {
                registerTable(join.getRightItem(), tableRefs);
            }
        }
        return tableRefs;
    }

    /**
     * 改造：支持子查询（ParenthesedSelect / SubSelect），递归解析其中的表
     */
    private void registerTable(FromItem fromItem, Map<String, TableRef> tableRefs) {
        if (fromItem == null) {
            return;
        }
        if (fromItem instanceof ParenthesedSelect parenthesedSelect) {
            // 子查询：递归解析
            Select subBody = parenthesedSelect.getSelect();
            if (subBody instanceof PlainSelect subPlainSelect) {
                Map<String, TableRef> subTables = resolveTables(subPlainSelect);
                tableRefs.putAll(subTables);
            }
            return;
        }
        if (!(fromItem instanceof Table table)) {
            throw new IllegalArgumentException("当前仅支持真实表查询，不支持临时表或函数表");
        }

        String tableName = table.getName();
        String alias = resolveAlias(table.getAlias(), tableName);
        tableRefs.put(normalizeIdentifier(alias), new TableRef(tableName, alias));
        tableRefs.putIfAbsent(normalizeIdentifier(tableName), new TableRef(tableName, alias));
    }

    private TableRef resolveTableRef(Table table, Map<String, TableRef> tableRefByAlias) {
        if (table == null) {
            return null;
        }
        String key = normalizeIdentifier(table.getName());
        if (key == null) {
            return null;
        }
        return tableRefByAlias.get(key);
    }

    private String resolveAlias(Alias alias, String tableName) {
        if (alias != null && alias.getName() != null && !alias.getName().isBlank()) {
            return alias.getName();
        }
        return tableName;
    }

    private boolean containsForbiddenTokens(String sql) {
        String trimmed = sql.trim();
        String withoutTrailingSemicolon = trimmed.replaceAll(";+$", "");
        if (withoutTrailingSemicolon.contains(";")) {
            return true;
        }
        return withoutTrailingSemicolon.contains("--")
            || withoutTrailingSemicolon.contains("/*")
            || withoutTrailingSemicolon.contains("#");
    }

    private String normalizeSql(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return null;
        }
        return sql.trim().replaceAll(";+$", "");
    }

    private String normalizeIdentifier(String identifier) {
        if (identifier == null) {
            return null;
        }
        return identifier.replace("`", "").trim().toLowerCase(Locale.ROOT);
    }

    public record ValidationResult(boolean valid, String message) {
        public static ValidationResult passed() {
            return new ValidationResult(true, "SQL 安全");
        }

        public static ValidationResult invalid(String message) {
            return new ValidationResult(false, message);
        }
    }

    public record ColumnBinding(String resultColumn, String tableName, String fieldName) {
    }

    public record GovernedQuery(
        String originalSql,
        String governedSql,
        String cacheScopeKey,
        List<String> tableNames,
        Map<String, ColumnBinding> columnBindings,
        Set<String> wildcardTables
    ) {
    }

    private record TableRef(String tableName, String aliasOrName) {
    }

    private record QueryLineage(
        Set<String> tableNames,
        Map<String, ColumnBinding> columnBindings,
        Set<String> wildcardTables
    ) {
    }
}
