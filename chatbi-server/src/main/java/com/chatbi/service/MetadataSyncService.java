package com.chatbi.service;

import com.chatbi.entity.DataSource;
import com.chatbi.repository.DataSourceMapper;
import com.chatbi.repository.MetricMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.util.*;

/**
 * 元数据同步服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetadataSyncService {

    private final DataSourceMapper dataSourceMapper;
    private final MetricMapper metricMapper;

    /**
     * 同步数据源的元数据
     */
    @Transactional
    public void syncDatasourceMetadata(Long datasourceId) {
        DataSource datasource = dataSourceMapper.selectById(datasourceId);
        if (datasource == null) {
            throw new IllegalArgumentException("数据源不存在：" + datasourceId);
        }

        Connection conn = null;
        try {
            // 获取数据库连接
            conn = getConnection(datasource);

            // 获取所有表
            List<String> tables = getTables(conn, datasource.getType());
            log.info("数据源 {} 共有 {} 个表", datasource.getName(), tables.size());

            // 同步每个表的元数据
            for (String table : tables) {
                syncTableMetadata(datasourceId, table, conn);
            }

            log.info("元数据同步完成：{}", datasource.getName());

        } catch (Exception e) {
            log.error("元数据同步失败：{}", e.getMessage(), e);
            throw new RuntimeException("元数据同步失败：" + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    // ignore
                }
            }
        }
    }

    /**
     * 同步单个表的元数据
     */
    private void syncTableMetadata(Long datasourceId, String tableName, Connection conn) {
        try {
            // 获取表的所有字段
            List<Map<String, Object>> columns = getColumns(conn, tableName);
            log.info("同步表 {} 的元数据，共 {} 个字段", tableName, columns.size());

            // 为每个字段创建指标定义
            for (Map<String, Object> column : columns) {
                String columnName = (String) column.get("COLUMN_NAME");
                String columnType = (String) column.get("TYPE_NAME");
                String remarks = (String) column.get("REMARKS");

                log.debug("字段：{} - 类型：{} - 注释：{}", columnName, columnType, remarks);
            }

        } catch (Exception e) {
            log.error("同步表 {} 元数据失败：{}", tableName, e.getMessage(), e);
        }
    }

    /**
     * 获取数据库的所有表
     */
    private List<String> getTables(Connection conn, String datasourceType) throws SQLException {
        List<String> tables = new ArrayList<>();
        DatabaseMetaData metaData = conn.getMetaData();

        // 获取表信息
        ResultSet tableRs = metaData.getTables(null, null, null, new String[]{"TABLE"});
        while (tableRs.next()) {
            String tableName = tableRs.getString("TABLE_NAME");
            tables.add(tableName);
        }
        tableRs.close();

        return tables;
    }

    /**
     * 获取表的列信息
     */
    private List<Map<String, Object>> getColumns(Connection conn, String tableName) throws SQLException {
        List<Map<String, Object>> columns = new ArrayList<>();
        DatabaseMetaData metaData = conn.getMetaData();

        ResultSet columnRs = metaData.getColumns(null, null, tableName, null);
        while (columnRs.next()) {
            Map<String, Object> column = new HashMap<>();
            column.put("COLUMN_NAME", columnRs.getString("COLUMN_NAME"));
            column.put("TYPE_NAME", columnRs.getString("TYPE_NAME"));
            column.put("COLUMN_SIZE", columnRs.getInt("COLUMN_SIZE"));
            column.put("NULLABLE", columnRs.getInt("NULLABLE"));
            column.put("REMARKS", columnRs.getString("REMARKS"));
            columns.add(column);
        }
        columnRs.close();

        return columns;
    }

    /**
     * 获取数据库连接
     */
    private Connection getConnection(DataSource datasource) throws Exception {
        String url = datasource.getUrl();
        String username = datasource.getUsername();
        String password = datasource.getPasswordEncrypted();

        // 加载驱动
        String driverClass = datasource.getDriverClass();
        if (driverClass == null || driverClass.isEmpty()) {
            driverClass = getDriverClass(datasource.getType());
        }
        Class.forName(driverClass);

        return DriverManager.getConnection(url, username, password);
    }

    /**
     * 根据数据库类型获取驱动类
     */
    private String getDriverClass(String dbType) {
        return switch (dbType.toLowerCase()) {
            case "mysql" -> "com.mysql.cj.jdbc.Driver";
            case "oracle" -> "oracle.jdbc.OracleDriver";
            case "postgresql" -> "org.postgresql.Driver";
            case "sqlserver" -> "com.microsoft.sqlserver.jdbc.SQLServerDriver";
            case "clickhouse" -> "com.clickhouse.jdbc.ClickHouseDriver";
            case "hive" -> "org.apache.hive.jdbc.HiveDriver";
            default -> "com.mysql.cj.jdbc.Driver";
        };
    }

    /**
     * 识别指标类型
     */
    private String identifyMetricType(String columnType, String columnName) {
        String upperType = columnType.toUpperCase();
        String upperName = columnName.toUpperCase();

        // 数值类型可以作为度量
        if (upperType.contains("INT") || upperType.contains("DECIMAL") ||
            upperType.contains("NUMERIC") || upperType.contains("DOUBLE") ||
            upperType.contains("FLOAT")) {

            // 检查是否是 ID 字段
            if (upperName.endsWith("ID") || upperName.equals("ID")) {
                return "DIMENSION"; // 维度
            }

            return "MEASURE"; // 度量
        }

        // 日期类型
        if (upperType.contains("DATE") || upperType.contains("TIME")) {
            return "DATE_DIMENSION"; // 日期维度
        }

        return "DIMENSION"; // 默认为维度
    }
}
