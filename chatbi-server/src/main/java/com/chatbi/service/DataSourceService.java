package com.chatbi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chatbi.common.exception.BusinessException;
import com.chatbi.datasource.DynamicDataSourceRegistry;
import com.chatbi.entity.DataSource;
import com.chatbi.enums.DataSourceType;
import com.chatbi.repository.DataSourceMapper;
import com.chatbi.utils.EncryptionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据源服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataSourceService {

    private final DataSourceMapper dataSourceMapper;
    private final DynamicDataSourceRegistry dynamicDataSourceRegistry;

    /**
     * 加密密钥（生产环境应使用环境变量）
     */
    private static final String ENCRYPTION_KEY = "chatbi-encryption-key-2026";

    /**
     * 分页查询数据源列表
     */
    public Page<DataSource> page(String keyword, Integer status, int current, int size) {
        LambdaQueryWrapper<DataSource> wrapper = new LambdaQueryWrapper<>();

        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w
                    .like(DataSource::getName, keyword)
                    .or()
                    .like(DataSource::getCode, keyword)
            );
        }

        if (status != null) {
            wrapper.eq(DataSource::getStatus, status);
        }

        wrapper.orderByDesc(DataSource::getCreatedAt);

        return dataSourceMapper.selectPage(new Page<>(current, size), wrapper);
    }

    /**
     * 查询所有数据源
     */
    public List<DataSource> list() {
        return dataSourceMapper.selectList(null);
    }

    /**
     * 根据 ID 查询数据源
     */
    public DataSource getById(Long id) {
        DataSource dataSource = dataSourceMapper.selectById(id);
        if (dataSource == null) {
            throw BusinessException.dataNotFound();
        }
        return dataSource;
    }

    /**
     * 根据编码查询数据源
     */
    public DataSource getByCode(String code) {
        LambdaQueryWrapper<DataSource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DataSource::getCode, code);
        return dataSourceMapper.selectOne(wrapper);
    }

    /**
     * 创建数据源
     */
    @Transactional(rollbackFor = Exception.class)
    public DataSource create(DataSource dataSource) {
        // 检查编码是否已存在
        DataSource existing = getByCode(dataSource.getCode());
        if (existing != null) {
            throw BusinessException.dataDuplicate("数据源编码");
        }

        // 加密密码
        if (dataSource.getPasswordEncrypted() != null) {
            String encrypted = EncryptionUtils.encrypt(dataSource.getPasswordEncrypted(), ENCRYPTION_KEY);
            dataSource.setPasswordEncrypted(encrypted);
        }

        dataSourceMapper.insert(dataSource);
        log.info("创建数据源成功：{}", dataSource.getCode());
        return dataSource;
    }

    /**
     * 更新数据源
     */
    @Transactional(rollbackFor = Exception.class)
    public DataSource update(Long id, DataSource dataSource) {
        DataSource existing = getById(id);

        // 如果密码被修改，重新加密
        if (dataSource.getPasswordEncrypted() != null && !dataSource.getPasswordEncrypted().startsWith("***")) {
            String encrypted = EncryptionUtils.encrypt(dataSource.getPasswordEncrypted(), ENCRYPTION_KEY);
            dataSource.setPasswordEncrypted(encrypted);
        } else {
            // 保留原密码
            dataSource.setPasswordEncrypted(existing.getPasswordEncrypted());
        }

        dataSource.setId(id);
        dataSourceMapper.updateById(dataSource);
        log.info("更新数据源成功：{}", dataSource.getCode());

        return dataSource;
    }

    /**
     * 删除数据源
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        DataSource dataSource = getById(id);

        // 从注册表移除
        dynamicDataSourceRegistry.removeDataSource(dataSource.getCode());

        dataSourceMapper.deleteById(id);
        log.info("删除数据源成功：{}", dataSource.getCode());
    }

    /**
     * 测试数据源连接
     */
    public ConnectionTestResult testConnection(DataSource dataSource) {
        long startTime = System.currentTimeMillis();

        try {
            // 获取驱动类
            String driverClass = getDriverClass(dataSource.getType());
            if (driverClass == null) {
                return ConnectionTestResult.failure("不支持的数据源类型：" + dataSource.getType());
            }

            // 加载驱动
            Class.forName(driverClass);

            // 获取 JDBC URL
            String jdbcUrl = dataSource.getUrl();
            if (jdbcUrl == null || jdbcUrl.isEmpty()) {
                jdbcUrl = buildJdbcUrl(dataSource);
            }

            // 获取用户名密码
            String username = dataSource.getUsername();
            String password = resolvePassword(dataSource.getPasswordEncrypted());

            // 尝试连接
            Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
            long endTime = System.currentTimeMillis();

            conn.close();

            log.info("数据源连接测试成功：{}", dataSource.getCode());
            return ConnectionTestResult.success(endTime - startTime);

        } catch (ClassNotFoundException e) {
            log.error("数据源驱动未找到：{}", dataSource.getType(), e);
            return ConnectionTestResult.failure("数据库驱动未找到：" + e.getMessage());
        } catch (Exception e) {
            log.error("数据源连接测试失败：{}", dataSource.getCode(), e);
            return ConnectionTestResult.failure("连接失败：" + e.getMessage());
        }
    }

    /**
     * 构建 JDBC URL
     */
    private String buildJdbcUrl(DataSource dataSource) {
        try {
            DataSourceType type = DataSourceType.valueOf(dataSource.getType().toUpperCase());
            return type.buildUrl(
                    dataSource.getHost(),
                    dataSource.getPort(),
                    dataSource.getDatabase(),
                    dataSource.getService()
            );
        } catch (IllegalArgumentException e) {
            return dataSource.getUrl();
        }
    }

    /**
     * 获取驱动类名称
     */
    private String getDriverClass(String type) {
        try {
            DataSourceType dataSourceType = DataSourceType.valueOf(type.toUpperCase());
            return dataSourceType.getDriverClass();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 获取数据源元数据（表、视图等）
     */
    public DataSourceMetadata getMetadata(Long id) throws Exception {
        DataSource dataSource = getById(id);

        String jdbcUrl = dataSource.getUrl();
        String username = dataSource.getUsername();
        String password = resolvePassword(dataSource.getPasswordEncrypted());

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
            DataSourceMetadata metadata = new DataSourceMetadata();

            // 获取表信息
            var tables = conn.getMetaData().getTables(null, null, "%", new String[]{"TABLE"});
            while (tables.next()) {
                if (isSystemSchema(tables.getString("TABLE_SCHEM"))) {
                    continue;
                }
                metadata.addTable(tables.getString("TABLE_NAME"), tables.getString("REMARKS"));
            }
            tables.close();

            // 获取视图信息
            var views = conn.getMetaData().getTables(null, null, "%", new String[]{"VIEW"});
            while (views.next()) {
                if (isSystemSchema(views.getString("TABLE_SCHEM"))) {
                    continue;
                }
                metadata.addView(views.getString("TABLE_NAME"), views.getString("REMARKS"));
            }
            views.close();

            return metadata;
        }
    }

    /**
     * 获取表结构信息
     */
    public TableSchema getTableSchema(Long datasourceId, String tableName) throws Exception {
        DataSource dataSource = getById(datasourceId);

        String jdbcUrl = dataSource.getUrl();
        String username = dataSource.getUsername();
        String password = resolvePassword(dataSource.getPasswordEncrypted());

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
            TableSchema schema = new TableSchema();
            schema.setTableName(tableName);

            // 获取列信息
            var columns = conn.getMetaData().getColumns(null, null, tableName, "%");
            while (columns.next()) {
                ColumnInfo column = new ColumnInfo();
                column.setColumnName(columns.getString("COLUMN_NAME"));
                column.setDataType(columns.getString("TYPE_NAME"));
                column.setColumnSize(columns.getInt("COLUMN_SIZE"));
                column.setNullable(columns.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                column.setRemarks(columns.getString("REMARKS"));
                schema.addColumn(column);
            }
            columns.close();

            return schema;
        }
    }

    /**
     * 连接测试结果
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ConnectionTestResult {
        private boolean success;
        private String message;
        private Long responseTime;

        public static ConnectionTestResult success(Long responseTime) {
            return new ConnectionTestResult(true, "连接成功", responseTime);
        }

        public static ConnectionTestResult failure(String message) {
            return new ConnectionTestResult(false, message, 0L);
        }
    }

    /**
     * 数据源元数据
     */
    @lombok.Data
    public static class DataSourceMetadata {
        private List<TableInfo> tables;
        private List<TableInfo> views;

        public DataSourceMetadata() {
            this.tables = new java.util.ArrayList<>();
            this.views = new java.util.ArrayList<>();
        }

        public void addTable(String name, String remarks) {
            tables.add(new TableInfo(name, remarks, "TABLE"));
        }

        public void addView(String name, String remarks) {
            views.add(new TableInfo(name, remarks, "VIEW"));
        }

        @lombok.Data
        @lombok.AllArgsConstructor
        public static class TableInfo {
            private String name;
            private String remarks;
            private String type;
        }
    }

    /**
     * 表结构信息
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    public static class TableSchema {
        private String tableName;
        private List<ColumnInfo> columns;

        public void addColumn(ColumnInfo column) {
            if (columns == null) {
                columns = new java.util.ArrayList<>();
            }
            columns.add(column);
        }
    }

    /**
     * 列信息
     */
    @lombok.Data
    public static class ColumnInfo {
        private String columnName;
        private String dataType;
        private Integer columnSize;
        private boolean nullable;
        private String remarks;
    }

    private String resolvePassword(String encryptedOrPlainText) {
        if (encryptedOrPlainText == null || encryptedOrPlainText.isBlank()) {
            return "";
        }
        try {
            return EncryptionUtils.decrypt(encryptedOrPlainText, ENCRYPTION_KEY);
        } catch (Exception ex) {
            log.debug("数据源密码不是密文，按明文处理");
            return encryptedOrPlainText;
        }
    }

    private boolean isSystemSchema(String schemaName) {
        if (schemaName == null || schemaName.isBlank()) {
            return false;
        }

        String normalized = schemaName.trim().toUpperCase();
        return normalized.startsWith("INFORMATION_SCHEMA")
                || normalized.startsWith("PG_")
                || normalized.startsWith("SYS")
                || normalized.startsWith("MYSQL")
                || normalized.startsWith("PERFORMANCE_SCHEMA");
    }
}
