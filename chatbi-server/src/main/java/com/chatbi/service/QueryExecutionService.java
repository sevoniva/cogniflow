package com.chatbi.service;

import com.chatbi.entity.DataSource;
import com.chatbi.utils.EncryptionUtils;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 查询执行服务
 * 用于执行动态 SQL 查询和提取数据库元数据
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryExecutionService {

    @Value("${app.masking.encrypt-key}")
    private String encryptionKey;

    private final javax.sql.DataSource defaultDataSource;
    private final QueryCacheService queryCacheService;
    private final QueryGovernanceService queryGovernanceService;
    private final DataMaskingService dataMaskingService;
    private final MeterRegistry meterRegistry;
    private final QueryProgressService queryProgressService;

    @Value("${app.query-governance.max-rows:500}")
    private int maxRows;

    @Value("${app.query-governance.query-timeout-seconds:30}")
    private int queryTimeoutSeconds;

    // 数据源连接池缓存（改造：Caffeine 替代 ConcurrentHashMap）
    private Cache<Long, HikariDataSource> dataSourceCache;

    @Value("${app.datasource.pool.max-size:10}")
    private int poolMaxSize;

    @Value("${app.datasource.pool.min-idle:2}")
    private int poolMinIdle;

    @Value("${app.datasource.pool.connection-timeout:30000}")
    private long poolConnectionTimeout;

    @Value("${app.datasource.pool.idle-timeout:600000}")
    private long poolIdleTimeout;

    @Value("${app.datasource.pool.max-lifetime:1800000}")
    private long poolMaxLifetime;

    @Value("${app.datasource.pool.cache-expire-minutes:60}")
    private long poolCacheExpireMinutes;

    @PostConstruct
    public void init() {
        this.dataSourceCache = Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterAccess(poolCacheExpireMinutes, TimeUnit.MINUTES)
                .removalListener((key, value, cause) -> {
                    if (value instanceof HikariDataSource) {
                        ((HikariDataSource) value).close();
                        log.info("数据源连接池已清理 - ID: {}, cause: {}", key, cause);
                    }
                })
                .build();
        log.info("QueryExecutionService 初始化完成 - poolMaxSize: {}, poolMinIdle: {}, cacheExpireMinutes: {}",
                poolMaxSize, poolMinIdle, poolCacheExpireMinutes);
    }

    /**
     * 执行 SQL 查询（使用默认数据源）
     */
    public List<Map<String, Object>> execute(String sql) {
        QueryGovernanceService.GovernedQuery governedQuery = queryGovernanceService.govern(null, sql);
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(defaultDataSource);
            tuneJdbcTemplate(jdbcTemplate);
            List<Map<String, Object>> result = jdbcTemplate.queryForList(governedQuery.governedSql());
            sample.stop(Timer.builder("sql.query.duration")
                    .tag("datasource", "default")
                    .tag("status", "success")
                    .register(meterRegistry));
            return dataMaskingService.maskRows(null, governedQuery, result);
        } catch (Exception e) {
            sample.stop(Timer.builder("sql.query.duration")
                    .tag("datasource", "default")
                    .tag("status", "error")
                    .tag("error", e.getClass().getSimpleName())
                    .register(meterRegistry));
            log.error("SQL 执行失败：{}", governedQuery.governedSql(), e);
            throw new RuntimeException("SQL 执行失败：" + e.getMessage(), e);
        }
    }

    /**
     * 执行 SQL 查询（指定数据源）
     */
    @Observed(name = "sql.query", contextualName = "execute-sql-query",
              lowCardinalityKeyValues = {"type", "datasource"})
    public List<Map<String, Object>> executeQuery(DataSource dataSource, String sql) {
        return executeQuery(dataSource, sql, null);
    }

    /**
     * 执行 SQL 查询（带 WebSocket 进度推送）
     */
    public List<Map<String, Object>> executeQueryWithProgress(DataSource dataSource, String sql, Long userId) {
        queryProgressService.executing();
        try {
            List<Map<String, Object>> result = executeQuery(dataSource, sql, userId);
            queryProgressService.buildingResult();
            return result;
        } catch (Exception e) {
            queryProgressService.error(e.getMessage());
            throw e;
        }
    }

    public List<Map<String, Object>> executeQuery(DataSource dataSource, String sql, Long userId) {
        QueryGovernanceService.GovernedQuery governedQuery = queryGovernanceService.govern(userId, sql);
        String effectiveSql = governedQuery.governedSql();
        String cacheScopeKey = governedQuery.cacheScopeKey();

        if (queryCacheService.shouldCache(effectiveSql)) {
            List<Map<String, Object>> cachedResult = queryCacheService.getCachedResult(effectiveSql, dataSource.getId(), cacheScopeKey);
            if (cachedResult != null) {
                meterRegistry.counter("sql.query.cache.hit",
                        "datasource", dataSource.getName()).increment();
                return cachedResult;
            }
        }

        HikariDataSource hikariDataSource = null;
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            hikariDataSource = getOrCreateDataSource(dataSource);
            JdbcTemplate jdbcTemplate = new JdbcTemplate(hikariDataSource);
            tuneJdbcTemplate(jdbcTemplate);

            log.info("执行SQL查询 - 数据源: {}, userId: {}, SQL: {}", dataSource.getName(), userId, effectiveSql);
            List<Map<String, Object>> result = jdbcTemplate.queryForList(effectiveSql);
            List<Map<String, Object>> maskedResult = dataMaskingService.maskRows(userId, governedQuery, result);
            log.info("查询成功 - 返回 {} 条记录", maskedResult.size());

            sample.stop(Timer.builder("sql.query.duration")
                    .tag("datasource", dataSource.getName())
                    .tag("status", "success")
                    .register(meterRegistry));
            meterRegistry.counter("sql.query.rows",
                    "datasource", dataSource.getName()).increment(maskedResult.size());

            if (queryCacheService.shouldCache(effectiveSql)) {
                long ttl = queryCacheService.decideCacheTtl(effectiveSql);
                queryCacheService.cacheResult(effectiveSql, dataSource.getId(), cacheScopeKey, maskedResult, ttl);
            }

            return maskedResult;
        } catch (Exception e) {
            sample.stop(Timer.builder("sql.query.duration")
                    .tag("datasource", dataSource.getName())
                    .tag("status", "error")
                    .tag("error", e.getClass().getSimpleName())
                    .register(meterRegistry));
            log.error("SQL 执行失败 - 数据源: {}, userId: {}, SQL: {}", dataSource.getName(), userId, effectiveSql, e);
            throw new RuntimeException("SQL 执行失败：" + e.getMessage(), e);
        }
    }

    /**
     * 提取数据库表结构信息
     */
    @Observed(name = "schema.extract", contextualName = "extract-table-schemas")
    public List<AiQueryService.TableSchema> extractTableSchemas(DataSource dataSource) {
        try {
            HikariDataSource hikariDataSource = getOrCreateDataSource(dataSource);
            List<AiQueryService.TableSchema> schemas = new ArrayList<>();

            try (Connection connection = hikariDataSource.getConnection()) {
                DatabaseMetaData metaData = connection.getMetaData();
                String catalog = firstNonBlank(connection.getCatalog(), dataSource.getDatabase());
                String schemaPattern = connection.getSchema();

                log.info("提取表结构 - 数据源: {}, catalog: {}, schema: {}", dataSource.getName(), catalog, schemaPattern);

                try (ResultSet tables = metaData.getTables(catalog, schemaPattern, "%", new String[]{"TABLE"})) {
                    int count = 0;
                    while (tables.next() && count < 50) {
                        String tableName = tables.getString("TABLE_NAME");
                        String tableSchema = tables.getString("TABLE_SCHEM");
                        String tableCatalog = tables.getString("TABLE_CAT");

                        AiQueryService.TableSchema schema = new AiQueryService.TableSchema();
                        schema.setTableName(tableName);
                        schema.setDescription(tables.getString("REMARKS"));
                        schema.setColumns(readColumns(metaData, tableCatalog, tableSchema, tableName));

                        schemas.add(schema);
                        count++;
                    }
                }
            }

            log.info("成功提取 {} 张表的结构信息", schemas.size());
            return schemas;
        } catch (Exception e) {
            log.error("提取表结构失败 - 数据源: {}", dataSource.getName(), e);
            throw new RuntimeException("提取表结构失败：" + e.getMessage(), e);
        }
    }

    /**
     * 测试数据源连接
     */
    public boolean testConnection(DataSource dataSource) {
        HikariDataSource hikariDataSource = null;
        try {
            hikariDataSource = createDataSource(dataSource);
            JdbcTemplate jdbcTemplate = new JdbcTemplate(hikariDataSource);
            tuneJdbcTemplate(jdbcTemplate);
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            log.info("数据源连接测试成功 - {}", dataSource.getName());
            return true;
        } catch (Exception e) {
            log.error("数据源连接测试失败 - {}", dataSource.getName(), e);
            return false;
        } finally {
            if (hikariDataSource != null) {
                hikariDataSource.close();
            }
        }
    }

    /**
     * 获取或创建数据源连接池（改造：使用 Caffeine 缓存）
     */
    private HikariDataSource getOrCreateDataSource(DataSource dataSource) {
        HikariDataSource cached = dataSourceCache.getIfPresent(dataSource.getId());
        if (cached != null && !cached.isClosed()) {
            return cached;
        }
        // 缓存未命中或连接池已关闭，创建新的
        HikariDataSource ds = createDataSource(dataSource);
        dataSourceCache.put(dataSource.getId(), ds);
        return ds;
    }

    /**
     * 创建数据源连接池
     */
    private HikariDataSource createDataSource(DataSource ds) {
        HikariConfig config = new HikariConfig();

        if (ds.getUrl() != null && !ds.getUrl().isEmpty()) {
            config.setJdbcUrl(ds.getUrl());
        } else {
            config.setJdbcUrl(buildJdbcUrl(ds));
        }

        config.setUsername(ds.getUsername());
        String password = resolvePassword(ds);
        if (password != null) {
            config.setPassword(password);
        }

        if (ds.getDriverClass() != null && !ds.getDriverClass().isEmpty()) {
            config.setDriverClassName(ds.getDriverClass());
        } else {
            config.setDriverClassName(getDriverClassName(ds.getType()));
        }

        // 参数化配置（改造前为硬编码）
        config.setMaximumPoolSize(poolMaxSize);
        config.setMinimumIdle(poolMinIdle);
        config.setConnectionTimeout(poolConnectionTimeout);
        config.setIdleTimeout(poolIdleTimeout);
        config.setMaxLifetime(poolMaxLifetime);
        config.setPoolName("ChatBI-" + ds.getName());

        // 健康检查配置
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(5000);

        return new HikariDataSource(config);
    }

    /**
     * 构建JDBC URL
     */
    private String buildJdbcUrl(DataSource ds) {
        String type = ds.getType().toLowerCase();
        String host = ds.getHost();
        Integer port = ds.getPort();
        String database = ds.getDatabase();

        return switch (type) {
            case "mysql" -> String.format(
                "jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true",
                host,
                port != null ? port : 3306,
                database
            );
            case "postgresql" -> String.format("jdbc:postgresql://%s:%d/%s", host, port != null ? port : 5432, database);
            case "oracle" -> String.format("jdbc:oracle:thin:@%s:%d:%s", host, port != null ? port : 1521, database);
            case "sqlserver" -> String.format("jdbc:sqlserver://%s:%d;databaseName=%s", host, port != null ? port : 1433, database);
            case "clickhouse" -> String.format("jdbc:clickhouse://%s:%d/%s", host, port != null ? port : 8123, database);
            case "h2" -> String.format(
                "jdbc:h2:file:./data/%s;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
                database
            );
            default -> throw new RuntimeException("不支持的数据源类型：" + type);
        };
    }

    /**
     * 获取驱动类名
     */
    private String getDriverClassName(String type) {
        return switch (type.toLowerCase()) {
            case "mysql" -> "com.mysql.cj.jdbc.Driver";
            case "postgresql" -> "org.postgresql.Driver";
            case "oracle" -> "oracle.jdbc.OracleDriver";
            case "sqlserver" -> "com.microsoft.sqlserver.jdbc.SQLServerDriver";
            case "clickhouse" -> "com.clickhouse.jdbc.ClickHouseDriver";
            case "h2" -> "org.h2.Driver";
            default -> throw new RuntimeException("不支持的数据源类型：" + type);
        };
    }

    /**
     * 清理指定数据源的连接池
     */
    public void clearDataSourceCache(Long dataSourceId) {
        HikariDataSource hikariDataSource = dataSourceCache.getIfPresent(dataSourceId);
        if (hikariDataSource != null) {
            hikariDataSource.close();
            dataSourceCache.invalidate(dataSourceId);
            log.info("已清理数据源连接池 - ID: {}", dataSourceId);
        }
    }

    /**
     * 清理所有数据源连接池
     */
    public void clearAllDataSourceCache() {
        dataSourceCache.asMap().values().forEach(HikariDataSource::close);
        dataSourceCache.invalidateAll();
        log.info("已清理所有数据源连接池");
    }

    /**
     * 定时健康检查：清理已关闭或异常的连接池
     */
    public void healthCheck() {
        int cleaned = 0;
        for (Map.Entry<Long, HikariDataSource> entry : dataSourceCache.asMap().entrySet()) {
            HikariDataSource ds = entry.getValue();
            if (ds.isClosed()) {
                dataSourceCache.invalidate(entry.getKey());
                cleaned++;
            }
        }
        if (cleaned > 0) {
            log.info("数据源连接池健康检查 - 清理 {} 个失效连接池", cleaned);
        }
    }

    /**
     * 获取连接池缓存统计
     */
    public com.github.benmanes.caffeine.cache.stats.CacheStats getPoolStats() {
        return dataSourceCache.stats();
    }

    /**
     * 获取连接池缓存当前条目数
     */
    public long getDataSourceCacheSize() {
        return dataSourceCache.estimatedSize();
    }

    private List<AiQueryService.TableSchema.Column> readColumns(
        DatabaseMetaData metaData,
        String catalog,
        String schema,
        String tableName
    ) throws Exception {
        Set<String> primaryKeys = new HashSet<>();
        try (ResultSet pkSet = metaData.getPrimaryKeys(catalog, schema, tableName)) {
            while (pkSet.next()) {
                primaryKeys.add(pkSet.getString("COLUMN_NAME"));
            }
        }

        List<AiQueryService.TableSchema.Column> columnList = new ArrayList<>();
        try (ResultSet columns = metaData.getColumns(catalog, schema, tableName, "%")) {
            while (columns.next()) {
                AiQueryService.TableSchema.Column column = new AiQueryService.TableSchema.Column();
                String columnName = columns.getString("COLUMN_NAME");
                column.setName(columnName);
                column.setType(columns.getString("TYPE_NAME"));
                column.setDescription(columns.getString("REMARKS"));
                column.setPrimaryKey(primaryKeys.contains(columnName));
                columnList.add(column);
            }
        }
        return columnList;
    }

    private void tuneJdbcTemplate(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.setMaxRows(maxRows);
        jdbcTemplate.setQueryTimeout(queryTimeoutSeconds);
    }

    private String resolvePassword(DataSource dataSource) {
        String password = dataSource.getPasswordEncrypted();
        if (password == null || password.isBlank()) {
            return null;
        }
        try {
            return EncryptionUtils.decrypt(password, encryptionKey);
        } catch (Exception e) {
            log.debug("数据源密码不是密文，按明文使用 - dataSource: {}", dataSource.getName());
            return password;
        }
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }
}
