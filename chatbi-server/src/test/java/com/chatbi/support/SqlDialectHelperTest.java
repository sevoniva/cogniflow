package com.chatbi.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("SqlDialectHelper 测试")
class SqlDialectHelperTest {

    @Test
    @DisplayName("MySQL 方言应输出可移植日期表达式")
    void mysqlDialectShouldUsePortableExpressions() {
        SqlDialectHelper helper = new SqlDialectHelper(SqlDialectHelper.Dialect.MYSQL);

        assertEquals("CONCAT(EXTRACT(YEAR FROM order_date), '-', LPAD(EXTRACT(MONTH FROM order_date), 2, '0'))", helper.monthBucket("order_date"));
        assertEquals("CONCAT(EXTRACT(YEAR FROM event_time), '-', LPAD(EXTRACT(MONTH FROM event_time), 2, '0'), '-', LPAD(EXTRACT(DAY FROM event_time), 2, '0'))", helper.dayBucket("event_time"));
        assertEquals("CAST(order_time AS DATE)", helper.toDate("order_time"));
        assertEquals("TIMESTAMPADD(DAY, 1, register_date)", helper.addDays("register_date", 1));
        assertEquals("TIMESTAMPDIFF(DAY, start_date, end_date)", helper.daysBetween("start_date", "end_date"));
    }

    @Test
    @DisplayName("H2 方言应输出可移植日期表达式")
    void h2DialectShouldUsePortableExpressions() {
        SqlDialectHelper helper = new SqlDialectHelper(SqlDialectHelper.Dialect.H2);

        assertEquals("CONCAT(EXTRACT(YEAR FROM order_date), '-', LPAD(EXTRACT(MONTH FROM order_date), 2, '0'))", helper.monthBucket("order_date"));
        assertEquals("CONCAT(EXTRACT(YEAR FROM event_time), '-', LPAD(EXTRACT(MONTH FROM event_time), 2, '0'), '-', LPAD(EXTRACT(DAY FROM event_time), 2, '0'))", helper.dayBucket("event_time"));
        assertEquals("CAST(order_time AS DATE)", helper.toDate("order_time"));
        assertEquals("TIMESTAMPADD(DAY, 1, register_date)", helper.addDays("register_date", 1));
        assertEquals("TIMESTAMPDIFF(DAY, start_date, end_date)", helper.daysBetween("start_date", "end_date"));
    }

    @Test
    @DisplayName("H2 MySQL 兼容模式应复用同一套表达式")
    void h2MysqlModeShouldUseMysqlSyntax() {
        SqlDialectHelper helper = new SqlDialectHelper(null, "jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1");

        assertEquals("CONCAT(EXTRACT(YEAR FROM order_date), '-', LPAD(EXTRACT(MONTH FROM order_date), 2, '0'))", helper.monthBucket("order_date"));
        assertEquals("TIMESTAMPADD(DAY, 1, register_date)", helper.addDays("register_date", 1));
    }
}
