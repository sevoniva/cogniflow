package com.chatbi.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 数据源类型枚举
 */
@Getter
@AllArgsConstructor
public enum DataSourceType {

    /**
     * MySQL
     */
    MYSQL("MySQL", "com.mysql.cj.jdbc.Driver", "jdbc:mysql://{host}:{port}/{database}?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=utf8"),

    /**
     * Oracle
     */
    ORACLE("Oracle", "oracle.jdbc.OracleDriver", "jdbc:oracle:thin:@{host}:{port}:{service}"),

    /**
     * Oracle 简写
     */
    OB_ORACLE("OceanBase Oracle", "oracle.jdbc.OracleDriver", "jdbc:oracle:thin:@{host}:{port}:{service}"),

    /**
     * OceanBase MySQL 模式
     */
    OB_MYSQL("OceanBase MySQL", "com.mysql.cj.jdbc.Driver", "jdbc:mysql://{host}:{port}/{database}?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true"),

    /**
     * PostgreSQL
     */
    POSTGRESQL("PostgreSQL", "org.postgresql.Driver", "jdbc:postgresql://{host}:{port}/{database}"),

    /**
     * SQL Server
     */
    SQLSERVER("SQL Server", "com.microsoft.sqlserver.jdbc.SQLServerDriver", "jdbc:sqlserver://{host}:{port};databaseName={database}"),

    /**
     * ClickHouse
     */
    CLICKHOUSE("ClickHouse", "com.clickhouse.jdbc.ClickHouseDriver", "jdbc:clickhouse://{host}:{port}/{database}"),

    /**
     * Hive
     */
    HIVE("Hive", "org.apache.hive.jdbc.HiveDriver", "jdbc:hive2://{host}:{port}/{database}"),

    /**
     * MariaDB
     */
    MARIADB("MariaDB", "org.mariadb.jdbc.Driver", "jdbc:mariadb://{host}:{port}/{database}?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8"),

    /**
     * DB2
     */
    DB2("DB2", "com.ibm.db2.jcc.DB2Driver", "jdbc:db2://{host}:{port}/{database}"),

    /**
     * 达梦
     */
    DM("达梦数据库", "dm.jdbc.driver.DmDriver", "jdbc:dm://{host}:{port}/{database}"),

    /**
     * 人大金仓
     */
    KINGBASE("人大金仓", "com.kingbase8.Driver", "jdbc:kingbase8://{host}:{port}/{database}");

    private final String name;
    private final String driverClass;
    private final String urlTemplate;

    /**
     * 构建 JDBC URL
     */
    public String buildUrl(String host, int port, String database, String service) {
        return urlTemplate
                .replace("{host}", host)
                .replace("{port}", String.valueOf(port))
                .replace("{database}", database != null ? database : "")
                .replace("{service}", service != null ? service : "");
    }
}
