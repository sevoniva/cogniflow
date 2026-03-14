package com.chatbi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatbi.entity.DataMaskingRule;
import com.chatbi.repository.DataMaskingRuleMapper;
import com.chatbi.repository.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 数据脱敏服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataMaskingService {

    private final DataMaskingRuleMapper dataMaskingRuleMapper;
    private final SysUserMapper sysUserMapper;

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$");

    /**
     * 获取所有规则
     */
    public List<DataMaskingRule> list() {
        LambdaQueryWrapper<DataMaskingRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DataMaskingRule::getStatus, 1)
            .orderByAsc(DataMaskingRule::getPriority);
        return dataMaskingRuleMapper.selectList(wrapper);
    }

    /**
     * 根据 ID 查询
     */
    public DataMaskingRule getById(Long id) {
        return dataMaskingRuleMapper.selectById(id);
    }

    /**
     * 创建规则
     */
    @Transactional
    public DataMaskingRule create(DataMaskingRule rule) {
        dataMaskingRuleMapper.insert(rule);
        return rule;
    }

    /**
     * 更新规则
     */
    @Transactional
    public DataMaskingRule update(Long id, DataMaskingRule rule) {
        DataMaskingRule existing = dataMaskingRuleMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("规则不存在");
        }
        rule.setId(id);
        dataMaskingRuleMapper.updateById(rule);
        return rule;
    }

    /**
     * 删除规则
     */
    @Transactional
    public void delete(Long id) {
        dataMaskingRuleMapper.deleteById(id);
    }

    /**
     * 获取用户在某字段上的脱敏规则
     */
    public DataMaskingRule getRule(Long userId, String tableName, String fieldName) {
        LambdaQueryWrapper<DataMaskingRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DataMaskingRule::getTableName, tableName)
            .eq(DataMaskingRule::getFieldName, fieldName)
            .eq(DataMaskingRule::getStatus, 1)
            .orderByAsc(DataMaskingRule::getPriority);
        List<DataMaskingRule> rules = dataMaskingRuleMapper.selectList(wrapper);
        List<Long> roleIds = userId == null ? List.of() : sysUserMapper.selectRoleIdsByUserId(userId);
        return rules.stream()
            .filter(rule -> appliesToUser(rule, userId, roleIds))
            .findFirst()
            .orElse(null);
    }

    /**
     * 按查询血缘对结果集做脱敏，确保脱敏规则在真实执行链路中生效。
     */
    public List<Map<String, Object>> maskRows(Long userId,
                                              QueryGovernanceService.GovernedQuery governedQuery,
                                              List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty() || governedQuery == null) {
            return rows;
        }

        Map<String, QueryGovernanceService.ColumnBinding> exactBindings = governedQuery.columnBindings();
        Set<String> wildcardTables = governedQuery.wildcardTables();
        String singleTable = governedQuery.tableNames().size() == 1 ? governedQuery.tableNames().get(0) : null;

        return rows.stream().map(row -> {
            Map<String, Object> maskedRow = new LinkedHashMap<>();
            row.forEach((columnName, value) -> {
                if (value == null) {
                    maskedRow.put(columnName, null);
                    return;
                }

                String normalizedColumn = normalizeIdentifier(columnName);
                QueryGovernanceService.ColumnBinding exactBinding = exactBindings.get(normalizedColumn);
                if (exactBinding != null) {
                    maskedRow.put(columnName, maskValue(userId, exactBinding.tableName(), exactBinding.fieldName(), String.valueOf(value)));
                    return;
                }

                if (singleTable != null) {
                    maskedRow.put(columnName, maskValue(userId, singleTable, columnName, String.valueOf(value)));
                    return;
                }

                for (String tableName : wildcardTables) {
                    String maskedValue = maskValue(userId, tableName, columnName, String.valueOf(value));
                    if (!Objects.equals(maskedValue, String.valueOf(value))) {
                        maskedRow.put(columnName, maskedValue);
                        return;
                    }
                }

                maskedRow.put(columnName, value);
            });
            return maskedRow;
        }).toList();
    }

    /**
     * 脱敏数据值
     */
    public String maskValue(Long userId, String tableName, String fieldName, String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        DataMaskingRule rule = getRule(userId, tableName, fieldName);
        if (rule == null) {
            return value;
        }

        return applyMask(rule.getMaskType(), rule.getMaskPattern(), value);
    }

    private boolean appliesToUser(DataMaskingRule rule, Long userId, List<Long> roleIds) {
        if (rule.getUserId() != null && !Objects.equals(rule.getUserId(), userId)) {
            return false;
        }
        if (rule.getRoleId() != null && !roleIds.contains(rule.getRoleId())) {
            return false;
        }
        return true;
    }

    private String applyMask(String maskType, String pattern, String value) {
        String effectiveType = maskType == null || maskType.isBlank() ? autoDetectType(value) : maskType.toUpperCase(Locale.ROOT);
        if ("HIDE".equals(effectiveType)) {
            return "***";
        }
        if ("PARTIAL".equals(effectiveType)) {
            return applyPartialMask(pattern, value);
        }
        if ("HASH".equals(effectiveType)) {
            return String.valueOf(value.hashCode());
        }
        if ("ENCRYPT".equals(effectiveType)) {
            return new StringBuilder(value).reverse().toString();
        }
        return value;
    }

    private String autoDetectType(String value) {
        if (PHONE_PATTERN.matcher(value).matches()) {
            return "PARTIAL";
        }
        if (ID_CARD_PATTERN.matcher(value).matches()) {
            return "PARTIAL";
        }
        if (EMAIL_PATTERN.matcher(value).matches()) {
            return "PARTIAL";
        }
        return "HIDE";
    }

    private String applyPartialMask(String pattern, String value) {
        if (pattern == null || pattern.isEmpty()) {
            return defaultPartialMask(value);
        }

        try {
            if (pattern.contains("前") && pattern.contains("后")) {
                String[] parts = pattern.split("后");
                int prefixLen = Integer.parseInt(parts[0].replace("前", "").trim());
                int suffixLen = Integer.parseInt(parts[1].trim());

                if (value.length() <= prefixLen + suffixLen) {
                    return value;
                }

                String prefix = value.substring(0, prefixLen);
                String suffix = value.substring(value.length() - suffixLen);
                String mask = "*".repeat(value.length() - prefixLen - suffixLen);
                return prefix + mask + suffix;
            }
        } catch (Exception e) {
            log.warn("脱敏规则解析失败：{}", pattern, e);
        }

        return defaultPartialMask(value);
    }

    private String defaultPartialMask(String value) {
        if (PHONE_PATTERN.matcher(value).matches()) {
            return value.substring(0, 3) + "****" + value.substring(7);
        }
        if (EMAIL_PATTERN.matcher(value).matches()) {
            int index = value.indexOf('@');
            if (index > 1) {
                return value.substring(0, 1) + "***" + value.substring(index - 1);
            }
        }
        if (value.length() <= 2) {
            return "**";
        }
        return value.substring(0, 1) + "***" + value.substring(value.length() - 1);
    }

    private String normalizeIdentifier(String identifier) {
        return identifier == null ? null : identifier.replace("`", "").trim().toLowerCase(Locale.ROOT);
    }
}
