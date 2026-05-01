package com.chatbi.config;

import com.chatbi.entity.Metric;
import com.chatbi.entity.Synonym;
import com.chatbi.repository.MetricMapper;
import com.chatbi.repository.SynonymMapper;
import com.chatbi.service.AiProviderSettingService;
import com.chatbi.utils.EncryptionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数据初始化 - 应用启动时插入默认数据
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final Pattern MYSQL_URL_PATTERN = Pattern.compile("^jdbc:mysql://([^:/?#]+)(?::(\\d+))?/([^?]+).*$");

    private final MetricMapper metricMapper;
    private final SynonymMapper synonymMapper;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final DataSource dataSource;
    private final AiProviderSettingService aiProviderSettingService;
    @Value("${app.masking.encrypt-key}")
    private String datasourceEncryptionKey;
    @Value("${spring.datasource.url:}")
    private String primaryDatasourceUrl;
    @Value("${spring.datasource.username:}")
    private String primaryDatasourceUsername;
    @Value("${spring.datasource.password:}")
    private String primaryDatasourcePassword;
    @Value("${spring.datasource.driver-class-name:}")
    private String primaryDatasourceDriverClass;
    @Value("${app.security.ensure-default-admin:false}")
    private boolean ensureDefaultAdmin;
    @Value("${app.demo.load-large-datasets:false}")
    private boolean loadLargeDatasets;

    @Override
    public void run(String... args) {
        if (ensureDefaultAdmin) {
            ensureAdminUser();
        }
        ensureDefaultDatasource();
        initMetrics();
        ensureSynonymsTable();
        initSynonyms();
        initBusinessScenarioData();
        aiProviderSettingService.initializeRuntimeSettings();
    }

    private void ensureAdminUser() {
        try {
            String encodedPassword = passwordEncoder.encode("Admin@123");
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM sys_user WHERE username = 'admin' AND deleted_at IS NULL",
                Integer.class
            );

            if (count == null || count == 0) {
                jdbcTemplate.update(
                    "INSERT INTO sys_user (username, password, nick_name, status, is_admin, email) VALUES (?, ?, ?, ?, ?, ?)",
                    "admin", encodedPassword, "超级管理员", 1, 1, "admin@chatbi.com"
                );
                log.info("已创建默认管理员账号：admin");
            } else {
                jdbcTemplate.update(
                    "UPDATE sys_user SET password = ?, status = 1, is_admin = 1, deleted_at = NULL WHERE username = 'admin'",
                    encodedPassword
                );
                log.info("已重置管理员账号密码：admin");
            }
        } catch (BadSqlGrammarException ex) {
            log.warn("sys_user 表不存在，跳过默认管理员账号初始化");
        }
    }

    private void ensureSynonymsTable() {
        // 兼容历史库结构：部分库只有 synonym/synonym_alias，没有 synonyms 表
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS synonyms (
                id BIGINT NOT NULL AUTO_INCREMENT,
                standard_word VARCHAR(100) NOT NULL,
                aliases TEXT,
                description VARCHAR(255),
                status INT DEFAULT 1,
                created_by BIGINT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                deleted_at TIMESTAMP,
                PRIMARY KEY (id),
                UNIQUE KEY uk_standard_word (standard_word)
            )
            """);
    }

    private void ensureDefaultDatasource() {
        try {
            DatasourceSnapshot snapshot = resolveDatasourceSnapshot();
            if (snapshot == null) {
                log.info("未识别出主数据源信息，跳过默认数据源注册");
                return;
            }

            int updated = jdbcTemplate.update("""
                UPDATE data_source
                   SET name = ?, code = ?, type = ?, host = ?, port = ?, `database` = ?,
                       url = ?, username = ?, password_encrypted = ?, driver_class = ?,
                       status = 1, health_status = 1, last_check_time = CURRENT_TIMESTAMP,
                       created_by = COALESCE(created_by, 1), deleted_at = NULL, updated_at = CURRENT_TIMESTAMP
                 WHERE id = 1
                """,
                snapshot.name(),
                snapshot.code(),
                snapshot.type(),
                snapshot.host(),
                snapshot.port(),
                snapshot.database(),
                snapshot.url(),
                snapshot.username(),
                snapshot.encryptedPassword(),
                snapshot.driverClass()
            );

            if (updated == 0) {
                jdbcTemplate.update("""
                    INSERT INTO data_source
                    (id, name, code, type, host, port, `database`, url, username, password_encrypted,
                     driver_class, status, health_status, last_check_time, created_by, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """,
                    1L,
                    snapshot.name(),
                    snapshot.code(),
                    snapshot.type(),
                    snapshot.host(),
                    snapshot.port(),
                    snapshot.database(),
                    snapshot.url(),
                    snapshot.username(),
                    snapshot.encryptedPassword(),
                    snapshot.driverClass()
                );
                log.info("已注册默认数据源：{} -> {}", snapshot.code(), snapshot.url());
                return;
            }

            log.info("已刷新默认数据源：{} -> {}", snapshot.code(), snapshot.url());
        } catch (BadSqlGrammarException ex) {
            log.warn("data_source 表不存在，跳过默认数据源注册");
        }
    }

    private DatasourceSnapshot resolveDatasourceSnapshot() {
        if (primaryDatasourceUrl == null || primaryDatasourceUrl.isBlank()) {
            return null;
        }

        Matcher mysqlMatcher = MYSQL_URL_PATTERN.matcher(primaryDatasourceUrl);
        if (mysqlMatcher.matches()) {
            String host = mysqlMatcher.group(1);
            String portValue = mysqlMatcher.group(2);
            String database = mysqlMatcher.group(3);
            Integer port = portValue == null || portValue.isBlank() ? 3306 : Integer.parseInt(portValue);
            return new DatasourceSnapshot(
                "本地 MySQL 业务仓",
                "LOCAL_MYSQL",
                "MYSQL",
                host,
                port,
                database,
                primaryDatasourceUrl,
                primaryDatasourceUsername,
                encryptPassword(primaryDatasourcePassword),
                firstNonBlank(primaryDatasourceDriverClass, "com.mysql.cj.jdbc.Driver")
            );
        }

        return new DatasourceSnapshot(
            "默认业务仓",
            "PRIMARY_DS",
            inferType(primaryDatasourceUrl),
            null,
            null,
            null,
            primaryDatasourceUrl,
            primaryDatasourceUsername,
            encryptPassword(primaryDatasourcePassword),
            primaryDatasourceDriverClass
        );
    }

    private String encryptPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isBlank()) {
            return null;
        }
        return EncryptionUtils.encrypt(plainPassword, datasourceEncryptionKey);
    }

    private String inferType(String jdbcUrl) {
        if (jdbcUrl == null) {
            return "UNKNOWN";
        }
        if (jdbcUrl.startsWith("jdbc:mysql:")) {
            return "MYSQL";
        }
        if (jdbcUrl.startsWith("jdbc:h2:")) {
            return "H2";
        }
        if (jdbcUrl.startsWith("jdbc:postgresql:")) {
            return "POSTGRESQL";
        }
        return "UNKNOWN";
    }

    private String firstNonBlank(String first, String fallback) {
        return first != null && !first.isBlank() ? first : fallback;
    }

    private void initMetrics() {
        try {
            log.info("初始化默认指标数据（幂等）...");

            List<Metric> metrics = List.of(
                createMetric("EXPENSE_DEPT", "部门费用支出", "各部门月度费用支出金额"),
                createMetric("PROJECT_ONTIME", "项目交付及时率", "按时交付项目数/总项目数"),
                createMetric("COMPLAINT_CNT", "客户投诉量", "月度客户投诉案件数量"),
                createMetric("R&D_UTIL", "研发工时利用率", "有效工时/总工时"),
                createMetric("APPROVAL_TIME", "审批平均时长", "流程发起至完成的平均时长"),
                createMetric("SALES_REVENUE", "销售额", "按时间周期统计销售收入"),
                createMetric("GROSS_MARGIN", "毛利率", "收入减成本后的毛利占比"),
                createMetric("CASH_COLLECTION", "回款额", "销售订单实际回款金额"),
                createMetric("INVENTORY_TURNOVER", "库存周转天数", "库存资金占用效率"),
                createMetric("DELIVERY_FULFILLMENT", "订单履约率", "按时履约订单占比")
            );

            int inserted = 0;
            for (Metric metric : metrics) {
                try {
                    metricMapper.insert(metric);
                    inserted++;
                } catch (DuplicateKeyException ex) {
                    log.debug("默认指标已存在，跳过 code={}", metric.getCode());
                }
            }

            log.info("默认指标初始化完成，新增 {} 条，跳过 {} 条", inserted, metrics.size() - inserted);
        } catch (BadSqlGrammarException ex) {
            log.warn("metrics 表不存在，跳过默认指标初始化");
        }
    }

    private void initSynonyms() {
        try {
            log.info("初始化默认同义词数据（幂等）...");

            List<Synonym> synonyms = List.of(
                createSynonym("部门费用支出", List.of("花费", "支出", "成本", "开销", "费用")),
                createSynonym("项目交付及时率", List.of("交付率", "及时率", "按时交付")),
                createSynonym("客户投诉量", List.of("投诉", "客诉", "投诉量", "客户问题")),
                createSynonym("研发工时利用率", List.of("工时率", "利用率", "研发效率", "研发产能")),
                createSynonym("销售额", List.of("营收", "收入", "营业额", "销售收入", "业绩")),
                createSynonym("毛利率", List.of("毛利", "利润率", "毛利水平", "利润表现")),
                createSynonym("回款额", List.of("回款", "到账金额", "收款额")),
                createSynonym("库存周转天数", List.of("库存周转", "周转天数", "库存效率", "库存")),
                createSynonym("订单履约率", List.of("履约率", "交付履约率", "按时履约", "履约"))
            );

            int inserted = 0;
            for (Synonym synonym : synonyms) {
                try {
                    synonymMapper.insert(synonym);
                    inserted++;
                } catch (DuplicateKeyException ex) {
                    log.debug("默认同义词已存在，跳过 standardWord={}", synonym.getStandardWord());
                }
            }

            log.info("默认同义词初始化完成，新增 {} 条，跳过 {} 条", inserted, synonyms.size() - inserted);
        } catch (BadSqlGrammarException ex) {
            log.warn("synonyms 表不存在，跳过默认同义词初始化");
        }
    }

    private Metric createMetric(String code, String name, String definition) {
        Metric metric = new Metric();
        metric.setCode(code);
        metric.setName(name);
        metric.setDefinition(definition);
        metric.setDataType("NUMERIC");
        metric.setDatasourceId(1L);
        metric.setStatus("active");
        return metric;
    }

    private Synonym createSynonym(String standard, List<String> aliases) {
        Synonym synonym = new Synonym();
        synonym.setStandardWord(standard);
        synonym.setAliases(aliases);
        synonym.setStatus(1);
        return synonym;
    }

    private void initBusinessScenarioData() {
        if (!loadLargeDatasets) {
            log.info("当前环境未启用大体量演示数据导入，跳过业务场景初始化");
            return;
        }
        runScriptWhenTableBelowThreshold("sales_order", 1500, "db/business_test_data.sql");
        runScriptWhenTableBelowThreshold("employee", 200, "db/business_employee_data.sql");
        runScriptWhenTableBelowThreshold("inventory", 100, "db/business_inventory_data.sql");
        runScriptWhenTableBelowThreshold("service_ticket", 300, "db/business_service_ticket_data.sql");
        runScriptWhenTableBelowThreshold("agile_project", 5, "db/agile_test_data.sql");
    }

    private void runScriptWhenTableBelowThreshold(String tableName, int minimumRows, String scriptPath) {
        try {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM " + tableName, Integer.class);
            if (count != null && count >= minimumRows) {
                log.info("表 {} 已有 {} 条数据，达到阈值 {}，跳过 {}", tableName, count, minimumRows, scriptPath);
                return;
            }

            Resource script = new ClassPathResource(scriptPath);
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator(false, false, "UTF-8", script);
            DatabasePopulatorUtils.execute(populator, dataSource);
            log.info("表 {} 仅有 {} 条数据，已补充演示数据脚本：{}", tableName, count, scriptPath);
        } catch (BadSqlGrammarException ex) {
            log.warn("表 {} 不存在，跳过演示数据脚本 {}", tableName, scriptPath);
        } catch (Exception ex) {
            log.error("导入演示数据脚本失败：{}", scriptPath, ex);
        }
    }

    private record DatasourceSnapshot(
        String name,
        String code,
        String type,
        String host,
        Integer port,
        String database,
        String url,
        String username,
        String encryptedPassword,
        String driverClass
    ) {
    }
}
