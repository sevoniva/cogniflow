INSERT INTO data_source (id, name, code, type, host, port, `database`, url, username, password_encrypted, driver_class, status, health_status, last_check_time, created_by, created_at, updated_at)
SELECT 1, '本地 MySQL 业务仓', 'LOCAL_MYSQL', 'MYSQL', 'localhost', 3306, 'chatbi', 'jdbc:mysql://localhost:3306/chatbi?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true', 'chatbi', NULL, 'com.mysql.cj.jdbc.Driver', 1, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM data_source WHERE id = 1 AND deleted_at IS NULL);

INSERT INTO query_history (id, user_id, username, query_name, query_type, query_content, datasource_id, result_data, duration, status, error_msg, is_favorite, created_at, updated_at)
SELECT 1, 1, 'admin', '本月销售额', 'NATURAL_LANGUAGE', '本月销售额', 1, '[{"区域":"华东","销售额":1580000}]', 248, 'SUCCESS', NULL, TRUE, '2026-03-01 09:15:00', '2026-03-01 09:15:00'
WHERE NOT EXISTS (SELECT 1 FROM query_history WHERE id = 1 AND deleted_at IS NULL);
INSERT INTO query_history (id, user_id, username, query_name, query_type, query_content, datasource_id, result_data, duration, status, error_msg, is_favorite, created_at, updated_at)
SELECT 2, 1, 'admin', '华东区毛利率趋势', 'NATURAL_LANGUAGE', '华东区毛利率趋势', 1, '[{"月份":"2026-01","毛利率":23.5}]', 332, 'SUCCESS', NULL, TRUE, '2026-03-02 10:20:00', '2026-03-02 10:20:00'
WHERE NOT EXISTS (SELECT 1 FROM query_history WHERE id = 2 AND deleted_at IS NULL);
INSERT INTO query_history (id, user_id, username, query_name, query_type, query_content, datasource_id, result_data, duration, status, error_msg, is_favorite, created_at, updated_at)
SELECT 3, 1, 'admin', '客户投诉量', 'NATURAL_LANGUAGE', '客户投诉量', 1, '[{"区域":"华南","投诉量":26}]', 296, 'SUCCESS', NULL, FALSE, '2026-03-03 11:10:00', '2026-03-03 11:10:00'
WHERE NOT EXISTS (SELECT 1 FROM query_history WHERE id = 3 AND deleted_at IS NULL);
INSERT INTO query_history (id, user_id, username, query_name, query_type, query_content, datasource_id, result_data, duration, status, error_msg, is_favorite, created_at, updated_at)
SELECT 4, 1, 'admin', '研发工时利用率', 'NATURAL_LANGUAGE', '研发工时利用率', 1, '[{"成员":"张三","利用率":92.6}]', 274, 'SUCCESS', NULL, FALSE, '2026-03-04 14:05:00', '2026-03-04 14:05:00'
WHERE NOT EXISTS (SELECT 1 FROM query_history WHERE id = 4 AND deleted_at IS NULL);
INSERT INTO query_history (id, user_id, username, query_name, query_type, query_content, datasource_id, result_data, duration, status, error_msg, is_favorite, created_at, updated_at)
SELECT 5, 1, 'admin', '订单履约率', 'NATURAL_LANGUAGE', '订单履约率', 1, '[{"区域":"全国","履约率":96.2}]', 301, 'SUCCESS', NULL, FALSE, '2026-03-05 08:48:00', '2026-03-05 08:48:00'
WHERE NOT EXISTS (SELECT 1 FROM query_history WHERE id = 5 AND deleted_at IS NULL);

INSERT INTO dashboard (id, name, description, layout_config, charts_config, cover_image, created_by, created_by_name, is_public, status, created_at, updated_at)
SELECT 1, '经营总览驾驶舱', '面向管理层的营收、利润、客户与履约总览', '{"columns":12,"rowHeight":120}', '[{"type":"sales-trend","title":"销售趋势"},{"type":"gross-margin","title":"毛利率对比"},{"type":"customer-region","title":"客户区域分布"}]', NULL, 1, 'Carson', TRUE, 1, '2026-03-01 09:00:00', '2026-03-06 09:30:00'
WHERE NOT EXISTS (SELECT 1 FROM dashboard WHERE id = 1 AND deleted_at IS NULL);
INSERT INTO dashboard (id, name, description, layout_config, charts_config, cover_image, created_by, created_by_name, is_public, status, created_at, updated_at)
SELECT 2, '销售作战室', '覆盖区域销售、产品排行、销售团队达成与履约质量', '{"columns":12,"rowHeight":120}', '[{"type":"region-sales","title":"区域销售"},{"type":"product-ranking","title":"产品排行"},{"type":"order-fulfillment","title":"履约率"}]', NULL, 1, 'Carson', TRUE, 1, '2026-03-02 10:00:00', '2026-03-07 11:00:00'
WHERE NOT EXISTS (SELECT 1 FROM dashboard WHERE id = 2 AND deleted_at IS NULL);
INSERT INTO dashboard (id, name, description, layout_config, charts_config, cover_image, created_by, created_by_name, is_public, status, created_at, updated_at)
SELECT 3, '研发效能看板', '展示迭代速度、测试通过率、缺陷与代码提交表现', '{"columns":12,"rowHeight":120}', '[{"type":"velocity","title":"迭代速度"},{"type":"test-pass-rate","title":"测试通过率"},{"type":"defect-status","title":"缺陷分布"}]', NULL, 1, 'Carson', TRUE, 1, '2026-03-03 11:00:00', '2026-03-08 10:20:00'
WHERE NOT EXISTS (SELECT 1 FROM dashboard WHERE id = 3 AND deleted_at IS NULL);

INSERT INTO data_permission_rule (id, rule_name, table_name, field_name, role_id, user_id, operator_symbol, rule_value, value_type, priority, status, created_by, created_at, updated_at)
SELECT 1, '销售区域仅限华东华南', 'sales_order', 'region', 2, NULL, 'IN', '华东,华南', 'CONST', 1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM data_permission_rule WHERE id = 1 AND deleted_at IS NULL);
INSERT INTO data_permission_rule (id, rule_name, table_name, field_name, role_id, user_id, operator_symbol, rule_value, value_type, priority, status, created_by, created_at, updated_at)
SELECT 2, '客户等级只看VIP及A类', 'customer', 'level', 2, NULL, 'IN', 'VIP,A', 'CONST', 2, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM data_permission_rule WHERE id = 2 AND deleted_at IS NULL);

INSERT INTO data_masking_rule (id, rule_name, table_name, field_name, role_id, user_id, mask_type, mask_pattern, priority, status, created_by, created_at, updated_at)
SELECT 1, '客户手机号脱敏', 'customer', 'contact_phone', 2, NULL, 'PARTIAL', '前 3 后 4', 1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM data_masking_rule WHERE id = 1 AND deleted_at IS NULL);
INSERT INTO data_masking_rule (id, rule_name, table_name, field_name, role_id, user_id, mask_type, mask_pattern, priority, status, created_by, created_at, updated_at)
SELECT 2, '客户邮箱脱敏', 'customer', 'email', 2, NULL, 'PARTIAL', '前 2 后 6', 2, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM data_masking_rule WHERE id = 2 AND deleted_at IS NULL);

INSERT INTO alert_rule (id, rule_name, metric_id, datasource_id, alert_type, threshold_type, threshold_value, fluctuation_rate, compare_period, check_frequency, push_method, receiver, status, created_by, created_at, updated_at)
SELECT 1, '销售额低于目标预警', NULL, 1, 'THRESHOLD', '<', 1200000, NULL, 'MONTH', 'DAILY', 'EMAIL', 'ops@chatbi.local', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM alert_rule WHERE id = 1 AND deleted_at IS NULL);
INSERT INTO alert_rule (id, rule_name, metric_id, datasource_id, alert_type, threshold_type, threshold_value, fluctuation_rate, compare_period, check_frequency, push_method, receiver, status, created_by, created_at, updated_at)
SELECT 2, '投诉量异常波动', NULL, 1, 'FLUCTUATION', NULL, NULL, 20, 'WEEK', 'HOURLY', 'DINGTALK', 'https://example.com/dingtalk/webhook', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM alert_rule WHERE id = 2 AND deleted_at IS NULL);
INSERT INTO alert_rule (id, rule_name, metric_id, datasource_id, alert_type, threshold_type, threshold_value, fluctuation_rate, compare_period, check_frequency, push_method, receiver, status, created_by, created_at, updated_at)
SELECT 3, '研发测试通过率关注', NULL, 1, 'THRESHOLD', '<', 95, NULL, 'DAY', 'DAILY', 'WECHAT', 'wecom://chatbi/qa-alert', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM alert_rule WHERE id = 3 AND deleted_at IS NULL);

INSERT INTO subscription (id, title, type, resource_id, subscriber_id, subscriber_name, push_method, receiver, frequency, push_time, push_day, status, created_by, created_at, updated_at, last_push_time, push_count)
SELECT 1, '经营晨会日报', 'DASHBOARD', 1, 1, '管理层', 'EMAIL', 'board@chatbi.local', 'DAILY', '08:30', NULL, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '2026-03-10 08:30:00', 18
WHERE NOT EXISTS (SELECT 1 FROM subscription WHERE id = 1 AND deleted_at IS NULL);
INSERT INTO subscription (id, title, type, resource_id, subscriber_id, subscriber_name, push_method, receiver, frequency, push_time, push_day, status, created_by, created_at, updated_at, last_push_time, push_count)
SELECT 2, '销售周报推送', 'DASHBOARD', 2, 1, '销售中心', 'EMAIL', 'sales@chatbi.local', 'WEEKLY', '09:00', 'MONDAY', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '2026-03-09 09:00:00', 6
WHERE NOT EXISTS (SELECT 1 FROM subscription WHERE id = 2 AND deleted_at IS NULL);

INSERT INTO share (id, title, type, resource_id, share_token, share_method, validity_type, validity_days, expire_time, password, max_visits, current_visits, status, created_by, creator_name, created_at, updated_at)
SELECT 1, '经营总览对外分享', 'DASHBOARD', 1, 'BOARD202603', 'LINK', 'DAYS', 30, '2026-04-10 23:59:59', NULL, 999, 36, 1, 1, 'Carson', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM share WHERE id = 1 AND deleted_at IS NULL);
INSERT INTO share (id, title, type, resource_id, share_token, share_method, validity_type, validity_days, expire_time, password, max_visits, current_visits, status, created_by, creator_name, created_at, updated_at)
SELECT 2, '研发看板安全分享', 'DASHBOARD', 3, 'AGILE202603', 'PASSWORD', 'DAYS', 15, '2026-03-26 23:59:59', '1234', 200, 12, 1, 1, 'Carson', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM share WHERE id = 2 AND deleted_at IS NULL);

INSERT INTO audit_log (id, trace_id, user_id, username, action, resource_type, resource_id, request_method, request_uri, request_body, response_status, response_body, ip_address, user_agent, execute_time_ms, result, error_message, created_at)
SELECT 1, 'trace-demo-001', 1, 'admin', '查询看板', 'dashboard', 1, 'GET', '/api/dashboards/1', NULL, 200, '{"success":true}', '127.0.0.1', 'Codex', 42, 'SUCCESS', NULL, '2026-03-10 09:00:00'
WHERE NOT EXISTS (SELECT 1 FROM audit_log WHERE id = 1);
