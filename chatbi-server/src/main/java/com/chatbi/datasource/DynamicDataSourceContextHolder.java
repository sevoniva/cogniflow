package com.chatbi.datasource;

/**
 * 动态数据源上下文持有者
 */
public class DynamicDataSourceContextHolder {

    private static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();

    /**
     * 设置数据源键
     */
    public static void setDataSourceKey(String key) {
        CONTEXT.set(key);
    }

    /**
     * 获取数据源键
     */
    public static String getDataSourceKey() {
        return CONTEXT.get();
    }

    /**
     * 清空数据源键
     */
    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * 切换到主数据源
     */
    public static void useMaster() {
        setDataSourceKey("master");
    }
}
