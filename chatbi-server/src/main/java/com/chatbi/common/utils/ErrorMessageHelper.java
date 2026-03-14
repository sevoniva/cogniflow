package com.chatbi.common.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户友好的错误提示工具
 */
public class ErrorMessageHelper {

    private static final Map<String, String> ERROR_MESSAGES = new HashMap<>();

    static {
        // 数据源相关
        ERROR_MESSAGES.put("datasource.not.configured", "未配置数据源，请先在管理后台配置数据源");
        ERROR_MESSAGES.put("datasource.connection.failed", "数据源连接失败，请检查配置信息");
        ERROR_MESSAGES.put("datasource.not.found", "数据源不存在，请检查数据源ID");
        ERROR_MESSAGES.put("datasource.test.failed", "数据源连接测试失败：{0}");

        // SQL相关
        ERROR_MESSAGES.put("sql.syntax.error", "SQL语法错误，请检查查询语句");
        ERROR_MESSAGES.put("sql.execution.failed", "SQL执行失败：{0}");
        ERROR_MESSAGES.put("sql.generation.failed", "无法生成SQL，请尝试更明确的表达");
        ERROR_MESSAGES.put("sql.validation.failed", "SQL验证失败：{0}");
        ERROR_MESSAGES.put("sql.dangerous.operation", "检测到危险操作，已拒绝执行");

        // 查询相关
        ERROR_MESSAGES.put("query.text.empty", "查询内容不能为空");
        ERROR_MESSAGES.put("query.no.metric.found", "未找到匹配的指标，请检查查询内容");
        ERROR_MESSAGES.put("query.no.result", "查询无结果");
        ERROR_MESSAGES.put("query.timeout", "查询超时，请简化查询条件");

        // LLM相关
        ERROR_MESSAGES.put("llm.not.configured", "未配置AI服务，将使用规则引擎");
        ERROR_MESSAGES.put("llm.call.failed", "AI服务调用失败���已切换到规则引擎");
        ERROR_MESSAGES.put("llm.response.invalid", "AI服务响应异常，已切换到规则引擎");

        // 权限相关
        ERROR_MESSAGES.put("permission.denied", "权限不足，无法执行此操作");
        ERROR_MESSAGES.put("permission.datasource.denied", "无权访问该数据源");
        ERROR_MESSAGES.put("permission.dashboard.denied", "无权访问该仪表板");

        // 数据相关
        ERROR_MESSAGES.put("data.not.found", "数据不存在");
        ERROR_MESSAGES.put("data.duplicate", "数据已存在，请勿重复添加");
        ERROR_MESSAGES.put("data.in.use", "该数据正在被使用，无法删除");
        ERROR_MESSAGES.put("data.invalid", "数据格式不正确");

        // 导出相关
        ERROR_MESSAGES.put("export.failed", "导出失败：{0}");
        ERROR_MESSAGES.put("export.format.unsupported", "不支持的导出格式：{0}");
        ERROR_MESSAGES.put("export.data.empty", "没有可导出的数据");

        // 分享相关
        ERROR_MESSAGES.put("share.expired", "分享链接已过期");
        ERROR_MESSAGES.put("share.limit.exceeded", "分享链接访问次数已达上限");
        ERROR_MESSAGES.put("share.not.found", "分享链接不存在或已失效");

        // 系统相关
        ERROR_MESSAGES.put("system.error", "系统内部错误，请稍后重试");
        ERROR_MESSAGES.put("system.busy", "系统繁忙，请稍后重试");
        ERROR_MESSAGES.put("network.error", "网络错误，请检查网络连接");
    }

    /**
     * 获取错误消息
     */
    public static String getMessage(String key) {
        return ERROR_MESSAGES.getOrDefault(key, "操作失败");
    }

    /**
     * 获��错误消息（带参数）
     */
    public static String getMessage(String key, Object... args) {
        String message = ERROR_MESSAGES.getOrDefault(key, "操作失败");
        if (args != null && args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                message = message.replace("{" + i + "}", String.valueOf(args[i]));
            }
        }
        return message;
    }

    /**
     * 根据异常获取友好的错误消息
     */
    public static String getMessageFromException(Exception e) {
        String message = e.getMessage();
        if (message == null) {
            return getMessage("system.error");
        }

        // 数据库相关
        if (message.contains("Duplicate entry")) {
            return getMessage("data.duplicate");
        } else if (message.contains("foreign key constraint")) {
            return getMessage("data.in.use");
        } else if (message.contains("syntax error")) {
            return getMessage("sql.syntax.error");
        } else if (message.contains("doesn't exist")) {
            return "表或字段不存在，请检查数据源配置";
        } else if (message.contains("Access denied")) {
            return "数据库访问权限不足";
        } else if (message.contains("Communications link failure")) {
            return getMessage("datasource.connection.failed");
        }

        // LLM相关
        else if (message.contains("LLM") || message.contains("OpenAI")) {
            return getMessage("llm.call.failed");
        }

        // 未配置
        else if (message.contains("未配置")) {
            return message;
        }

        // 未找到
        else if (message.contains("未找到") || message.contains("not found")) {
            return message;
        }

        // 默认返回原始消息
        return message;
    }

    /**
     * 获取SQL错误的友好提示
     */
    public static String getSqlErrorMessage(String sqlError) {
        if (sqlError == null) {
            return getMessage("sql.execution.failed", "未知错误");
        }

        if (sqlError.contains("Unknown column")) {
            return "字段不存在，请检查查询条件";
        } else if (sqlError.contains("Unknown table")) {
            return "表不存在，请检查数据源配置";
        } else if (sqlError.contains("syntax error")) {
            return getMessage("sql.syntax.error");
        } else if (sqlError.contains("timeout")) {
            return getMessage("query.timeout");
        } else if (sqlError.contains("lock")) {
            return "数据被锁定，请稍后重试";
        } else {
            return getMessage("sql.execution.failed", sqlError);
        }
    }

    /**
     * 获取查询建议
     */
    public static String getQuerySuggestion(String query) {
        StringBuilder suggestion = new StringBuilder();
        suggestion.append("查询提示：\n");

        if (!query.contains("时间") && !query.contains("日期")) {
            suggestion.append("- 可以添加时间范围，如：本月、最近7天\n");
        }

        if (!query.contains("按") && !query.contains("分组")) {
            suggestion.append("- 可以按维度分组，如：按地区、按类别\n");
        }

        if (!query.contains("前") && !query.contains("top")) {
            suggestion.append("- 可以限制结果数量，如：前10名、top 5\n");
        }

        if (!query.contains("排序")) {
            suggestion.append("- 可以指定排序方式，如：降序、从高到低\n");
        }

        return suggestion.toString();
    }
}
