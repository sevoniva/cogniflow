package com.chatbi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatbi.entity.DataPermissionRule;
import com.chatbi.entity.SysUser;
import com.chatbi.repository.DataPermissionRuleMapper;
import com.chatbi.repository.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;
import net.sf.jsqlparser.schema.Column;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * 数据权限服务 - 行级权限。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataPermissionService {

    private final DataPermissionRuleMapper dataPermissionRuleMapper;
    private final SysUserMapper sysUserMapper;

    /**
     * 获取所有规则
     */
    public List<DataPermissionRule> list() {
        LambdaQueryWrapper<DataPermissionRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DataPermissionRule::getStatus, 1)
            .orderByAsc(DataPermissionRule::getPriority);
        return dataPermissionRuleMapper.selectList(wrapper);
    }

    /**
     * 根据 ID 查询
     */
    public DataPermissionRule getById(Long id) {
        return dataPermissionRuleMapper.selectById(id);
    }

    /**
     * 创建规则
     */
    @Transactional
    public DataPermissionRule create(DataPermissionRule rule) {
        dataPermissionRuleMapper.insert(rule);
        return rule;
    }

    /**
     * 更新规则
     */
    @Transactional
    public DataPermissionRule update(Long id, DataPermissionRule rule) {
        DataPermissionRule existing = dataPermissionRuleMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("规则不存在");
        }
        rule.setId(id);
        dataPermissionRuleMapper.updateById(rule);
        return rule;
    }

    /**
     * 删除规则
     */
    @Transactional
    public void delete(Long id) {
        dataPermissionRuleMapper.deleteById(id);
    }

    /**
     * 获取用户在某表上的权限规则。
     */
    public List<DataPermissionRule> getUserRules(Long userId, String tableName) {
        LambdaQueryWrapper<DataPermissionRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DataPermissionRule::getTableName, tableName)
            .eq(DataPermissionRule::getStatus, 1)
            .orderByAsc(DataPermissionRule::getPriority);

        List<DataPermissionRule> rules = dataPermissionRuleMapper.selectList(wrapper);
        List<Long> roleIds = userId == null ? List.of() : sysUserMapper.selectRoleIdsByUserId(userId);
        return rules.stream()
            .filter(rule -> appliesToUser(rule, userId, roleIds))
            .toList();
    }

    /**
     * 兼容旧接口，生成 where 条件片段。
     */
    public String generateWhereClause(Long userId, String tableName) {
        List<Expression> expressions = buildExpressions(userId, tableName, tableName);
        if (expressions.isEmpty()) {
            return "";
        }
        return " AND " + expressions.stream().map(Expression::toString).reduce((left, right) -> left + " AND " + right).orElse("");
    }

    /**
     * 按表构建 AST 级权限表达式，用于统一注入到查询执行链路。
     */
    public List<Expression> buildExpressions(Long userId, String tableName, String tableAlias) {
        List<DataPermissionRule> rules = getUserRules(userId, tableName);
        List<Expression> expressions = new ArrayList<>();
        for (DataPermissionRule rule : rules) {
            Expression expression = toExpression(rule, tableAlias, userId);
            if (expression != null) {
                expressions.add(expression);
            }
        }
        return expressions;
    }

    private boolean appliesToUser(DataPermissionRule rule, Long userId, List<Long> roleIds) {
        if (rule.getUserId() != null && !Objects.equals(rule.getUserId(), userId)) {
            return false;
        }
        if (rule.getRoleId() != null && !roleIds.contains(rule.getRoleId())) {
            return false;
        }
        return rule.getUserId() != null || rule.getRoleId() != null || (rule.getUserId() == null && rule.getRoleId() == null);
    }

    private Expression toExpression(DataPermissionRule rule, String tableAlias, Long userId) {
        String operator = rule.getOperatorSymbol() == null ? "=" : rule.getOperatorSymbol().trim().toUpperCase(Locale.ROOT);
        Column left = new Column(tableAlias + "." + rule.getFieldName());

        if ("IS NULL".equals(operator) || "IS NOT NULL".equals(operator)) {
            IsNullExpression expression = new IsNullExpression();
            expression.setLeftExpression(left);
            expression.setNot("IS NOT NULL".equals(operator));
            return expression;
        }

        if ("FIELD".equalsIgnoreCase(rule.getValueType())) {
            Column right = new Column(tableAlias + "." + rule.getRuleValue());
            return buildBinaryExpression(operator, left, right);
        }

        String rawValue = resolveRuleValue(rule, userId);
        if (("IN".equals(operator) || "NOT IN".equals(operator)) && rawValue != null) {
            ParenthesedExpressionList<Expression> values = new ParenthesedExpressionList<>();
            for (String item : rawValue.split(",")) {
                String trimmed = item.trim();
                if (!trimmed.isEmpty()) {
                    values.add(toLiteral(trimmed));
                }
            }
            if (values.isEmpty()) {
                return null;
            }
            InExpression inExpression = new InExpression(left, values);
            inExpression.setNot("NOT IN".equals(operator));
            return inExpression;
        }

        if (rawValue == null || rawValue.isBlank()) {
            log.warn("数据权限规则值为空，已忽略 - ruleId: {}, ruleName: {}", rule.getId(), rule.getRuleName());
            return null;
        }

        return buildBinaryExpression(operator, left, toLiteral(rawValue));
    }

    private Expression buildBinaryExpression(String operator, Expression left, Expression right) {
        return switch (operator) {
            case "=", "EQ" -> new EqualsTo(left, right);
            case "!=", "<>", "NE" -> new NotEqualsTo(left, right);
            case ">", "GT" -> new GreaterThan().withLeftExpression(left).withRightExpression(right);
            case ">=", "GTE" -> new GreaterThanEquals().withLeftExpression(left).withRightExpression(right);
            case "<", "LT" -> new MinorThan().withLeftExpression(left).withRightExpression(right);
            case "<=", "LTE" -> new MinorThanEquals().withLeftExpression(left).withRightExpression(right);
            case "LIKE" -> new LikeExpression().withLeftExpression(left).withRightExpression(right);
            default -> {
                log.warn("暂不支持的数据权限操作符：{}", operator);
                yield null;
            }
        };
    }

    private Expression toLiteral(String value) {
        if (value.matches("^-?\\d+$")) {
            return new LongValue(value);
        }
        if (value.matches("^-?\\d+\\.\\d+$")) {
            return new DoubleValue(value);
        }
        return new StringValue(value);
    }

    private String resolveRuleValue(DataPermissionRule rule, Long userId) {
        if (rule.getRuleValue() == null) {
            return null;
        }
        if ("USER_ATTR".equalsIgnoreCase(rule.getValueType())) {
            return getUserAttributeValue(userId, rule.getRuleValue());
        }
        return rule.getRuleValue();
    }

    private String getUserAttributeValue(Long userId, String attr) {
        if (userId == null || attr == null || attr.isBlank()) {
            return null;
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        return switch (attr.trim().toLowerCase(Locale.ROOT)) {
            case "id", "userid", "user_id" -> String.valueOf(user.getId());
            case "deptid", "dept_id" -> user.getDeptId() == null ? null : String.valueOf(user.getDeptId());
            case "username" -> user.getUsername();
            case "email" -> user.getEmail();
            case "phone", "mobile" -> user.getPhone();
            case "nickname", "nick_name" -> user.getNickName();
            default -> null;
        };
    }
}
