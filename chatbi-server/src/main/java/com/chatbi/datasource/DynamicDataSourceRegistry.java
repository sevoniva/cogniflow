package com.chatbi.datasource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态数据源注册表
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicDataSourceRegistry {

    private final Map<String, DataSource> dataSources = new ConcurrentHashMap<>();

    /**
     * 注册数据源
     */
    public void registerDataSource(String key, DataSource dataSource) {
        dataSources.put(key, dataSource);
        log.info("注册数据源：{}", key);
    }

    /**
     * 移除数据源
     */
    public void removeDataSource(String key) {
        dataSources.remove(key);
        log.info("移除数据源：{}", key);
    }

    /**
     * 获取数据源
     */
    public DataSource getDataSource(String key) {
        return dataSources.get(key);
    }

    /**
     * 创建 JDBC 数据源
     */
    public DriverManagerDataSource createDataSource(String driverClass, String url, String username, String password) {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName(driverClass);
        ds.setUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        return ds;
    }
}
