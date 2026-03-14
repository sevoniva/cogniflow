package com.chatbi.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * 统一生成可在 MySQL 与 H2(MySQL 兼容模式) 中运行的日期表达式，避免分析 SQL 在不同环境下失效。
 */
@Slf4j
@Component
public class SqlDialectHelper {

    enum Dialect {
        MYSQL,
        H2
    }

    private final DataSource dataSource;
    private final String datasourceUrl;
    private volatile Dialect dialect;

    @Autowired
    public SqlDialectHelper(DataSource dataSource, @Value("${spring.datasource.url:}") String datasourceUrl) {
        this.dataSource = dataSource;
        this.datasourceUrl = datasourceUrl;
    }

    SqlDialectHelper(Dialect dialect) {
        this.dataSource = null;
        this.datasourceUrl = "";
        this.dialect = dialect;
    }

    public String monthBucket(String expression) {
        return "CONCAT(EXTRACT(YEAR FROM %1$s), '-', LPAD(EXTRACT(MONTH FROM %1$s), 2, '0'))"
            .formatted(expression);
    }

    public String dayBucket(String expression) {
        return "CONCAT(EXTRACT(YEAR FROM %1$s), '-', LPAD(EXTRACT(MONTH FROM %1$s), 2, '0'), '-', LPAD(EXTRACT(DAY FROM %1$s), 2, '0'))"
            .formatted(expression);
    }

    public String toDate(String expression) {
        return "CAST(%s AS DATE)".formatted(expression);
    }

    public String addDays(String expression, int days) {
        return "TIMESTAMPADD(DAY, %d, %s)".formatted(days, expression);
    }

    public String daysBetween(String startExpression, String endExpression) {
        return "TIMESTAMPDIFF(DAY, %s, %s)".formatted(startExpression, endExpression);
    }

    boolean isMySql() {
        return resolveDialect() == Dialect.MYSQL;
    }

    private Dialect resolveDialect() {
        Dialect cached = dialect;
        if (cached != null) {
            return cached;
        }

        synchronized (this) {
            if (dialect != null) {
                return dialect;
            }

            String url = datasourceUrl == null ? "" : datasourceUrl.toLowerCase();
            if (url.startsWith("jdbc:h2:") && url.contains("mode=mysql")) {
                dialect = Dialect.MYSQL;
                return dialect;
            }
            if (url.startsWith("jdbc:mysql:")) {
                dialect = Dialect.MYSQL;
                return dialect;
            }
            if (url.startsWith("jdbc:h2:")) {
                dialect = Dialect.H2;
                return dialect;
            }

            if (dataSource != null) {
                try (Connection connection = dataSource.getConnection()) {
                    String productName = connection.getMetaData().getDatabaseProductName();
                    if (productName != null && productName.toLowerCase().contains("mysql")) {
                        dialect = Dialect.MYSQL;
                        return dialect;
                    }
                } catch (Exception ex) {
                    log.warn("检测数据库方言失败，回退为 H2 方言: {}", ex.getMessage());
                }
            }

            dialect = Dialect.H2;
            return dialect;
        }
    }
}
