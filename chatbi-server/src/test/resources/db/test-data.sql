MERGE INTO sys_user (id, username, password, nick_name, email, status, is_admin, created_at, updated_at)
KEY(id) VALUES
(1, 'admin', '$2a$10$Q0M4Cii9Y8L0qB6a3hILdeV4O1v6X6JdJ5v4q0e2B0l8dXnVf7v5O', '系统管理员', 'admin@chatbi.local', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'analyst', '$2a$10$Q0M4Cii9Y8L0qB6a3hILdeV4O1v6X6JdJ5v4q0e2B0l8dXnVf7v5O', '分析师', 'analyst@chatbi.local', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO sys_role (id, role_code, role_name, description, data_scope, status, sort_oder, created_at, updated_at)
KEY(id) VALUES
(1, 'SUPER_ADMIN', '超级管理员', '拥有全部权限', 1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'ANALYST', '分析师', '拥有分析与报表权限', 1, 1, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO sys_permission (id, perm_code, perm_name, resource_type, resource_path, parent_id, sort_oder, icon, status, created_at)
KEY(id) VALUES
(1, 'dashboard:view', '查看仪表板', 'MENU', '/dashboard', 0, 1, 'DataBoard', 1, CURRENT_TIMESTAMP),
(2, 'metric:query', '查看指标', 'API', '/api/metrics', 0, 2, 'DataLine', 1, CURRENT_TIMESTAMP),
(3, 'datasource:query', '查看数据源', 'API', '/api/datasources', 0, 3, 'Coin', 1, CURRENT_TIMESTAMP);

MERGE INTO sys_user_role (user_id, role_id, created_at)
KEY(user_id, role_id) VALUES
(1, 1, CURRENT_TIMESTAMP),
(2, 2, CURRENT_TIMESTAMP);

MERGE INTO sys_role_permission (role_id, permission_id, created_at)
KEY(role_id, permission_id) VALUES
(1, 1, CURRENT_TIMESTAMP),
(1, 2, CURRENT_TIMESTAMP),
(1, 3, CURRENT_TIMESTAMP),
(2, 1, CURRENT_TIMESTAMP),
(2, 2, CURRENT_TIMESTAMP);

MERGE INTO metrics (id, code, name, definition, data_type, table_name, field_name, aggregation, status, created_at, updated_at)
KEY(id) VALUES
(1, 'SALES_REVENUE', '销售额', '按时间统计销售收入', 'NUMERIC', 'sales_order', 'sales_amount', 'SUM', 'active', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'ORDER_COUNT', '订单数', '订单总量', 'NUMERIC', 'sales_order', 'id', 'COUNT', 'active', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'GROSS_MARGIN', '毛利率', '收入减成本后的毛利率', 'NUMERIC', 'sales_order', 'profit_amount', 'AVG', 'active', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO synonyms (id, standard_word, aliases, description, status, created_at, updated_at)
KEY(id) VALUES
(1, '销售额', '["营收","收入","营业额","revenue"]', '销售额相关同义词', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, '订单数', '["单量","订单数量"]', '订单数相关同义词', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO subscription (id, title, type, resource_id, subscriber_id, subscriber_name, push_method, receiver, frequency, push_time, push_day, status, created_by, created_at, updated_at, push_count)
KEY(id) VALUES
(1, '销售日报', 'DASHBOARD', 1, 2, '分析师', 'EMAIL', 'analyst@chatbi.local', 'DAILY', '09:00', '每天', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

MERGE INTO share (id, title, type, resource_id, share_token, share_method, validity_type, validity_days, status, created_by, creator_name, created_at, updated_at, current_visits)
KEY(id) VALUES
(1, '销售经营看板', 'DASHBOARD', 1, 'SHARE001', 'LINK', 'DAYS', 30, 1, 1, '系统管理员', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

MERGE INTO customer (id, customer_no, customer_name, customer_type, industry, region, level, contact_person, contact_phone, email, first_purchase_date, last_purchase_date, total_purchase_amount, purchase_count, status, created_at)
KEY(id) VALUES
(1, 'C001', '华东智造', 'ENTERPRISE', '制造', '华东', 'A', '张三', '13800000001', 'east@chatbi.local', DATE '2025-10-01', DATE '2026-03-05', 198000, 12, 'ACTIVE', CURRENT_TIMESTAMP),
(2, 'C002', '华南零售', 'ENTERPRISE', '零售', '华南', 'A', '李四', '13800000002', 'south@chatbi.local', DATE '2025-11-15', DATE '2026-03-08', 168000, 9, 'ACTIVE', CURRENT_TIMESTAMP),
(3, 'C003', '华北科技', 'ENTERPRISE', '科技', '华北', 'B', '王五', '13800000003', 'north@chatbi.local', DATE '2025-09-20', DATE '2026-02-18', 98000, 7, 'ACTIVE', CURRENT_TIMESTAMP);

MERGE INTO sales_order (id, order_no, customer_id, customer_name, product_id, product_name, product_category, region, sales_person_id, sales_person_name, quantity, unit_price, sales_amount, cost_amount, profit_amount, discount_amount, order_date, delivery_date, status, created_at)
KEY(id) VALUES
(1, 'SO2026030101', 1, '华东智造', 101, '工业平板', '电子产品', '华东', 11, '陈顾问', 12, 5000, 60000, 42000, 18000, 0, DATE '2026-03-03', DATE '2026-03-06', 'DELIVERED', CURRENT_TIMESTAMP),
(2, 'SO2026030102', 2, '华南零售', 102, '智能终端', '电子产品', '华南', 12, '林顾问', 10, 4800, 48000, 33600, 14400, 0, DATE '2026-03-05', DATE '2026-03-07', 'DELIVERED', CURRENT_TIMESTAMP),
(3, 'SO2026030103', 3, '华北科技', 103, '云控网关', '企业软件', '华北', 13, '赵顾问', 8, 5500, 44000, 30800, 13200, 0, DATE '2026-03-07', DATE '2026-03-11', 'SHIPPED', CURRENT_TIMESTAMP),
(4, 'SO2026020101', 1, '华东智造', 101, '工业平板', '电子产品', '华东', 11, '陈顾问', 9, 5000, 45000, 31500, 13500, 0, DATE '2026-02-12', DATE '2026-02-15', 'DELIVERED', CURRENT_TIMESTAMP),
(5, 'SO2026020102', 2, '华南零售', 104, '门店屏', '智能家居', '华南', 12, '林顾问', 6, 4000, 24000, 16800, 7200, 0, DATE '2026-02-18', DATE '2026-02-21', 'DELIVERED', CURRENT_TIMESTAMP),
(6, 'SO2026010101', 3, '华北科技', 105, '客流探针', '智能家居', '华北', 13, '赵顾问', 5, 3600, 18000, 12600, 5400, 0, DATE '2026-01-21', DATE '2026-01-25', 'DELIVERED', CURRENT_TIMESTAMP);

MERGE INTO app_user (id, user_no, username, nickname, gender, age, city, province, register_channel, register_date, last_login_date, login_count, status, created_at)
KEY(id) VALUES
(1, 'U001', 'east_ops', '华东用户', '男', 30, '上海', '上海', '官网', DATE '2026-03-02', DATE '2026-03-10', 8, 'ACTIVE', CURRENT_TIMESTAMP),
(2, 'U002', 'south_ops', '华南用户', '女', 29, '深圳', '广东', '渠道', DATE '2026-03-06', DATE '2026-03-10', 6, 'ACTIVE', CURRENT_TIMESTAMP),
(3, 'U003', 'north_ops', '华北用户', '男', 33, '北京', '北京', '官网', DATE '2026-02-08', DATE '2026-03-08', 12, 'ACTIVE', CURRENT_TIMESTAMP),
(4, 'U004', 'west_ops', '西南用户', '女', 27, '成都', '四川', '活动', DATE '2026-02-19', DATE '2026-03-09', 5, 'ACTIVE', CURRENT_TIMESTAMP);

MERGE INTO user_behavior (id, user_id, session_id, event_type, page_url, page_title, referrer, device_type, os, browser, channel, duration, ip_address, city, province, event_time, created_at)
KEY(id) VALUES
(1, 1, 'S001', 'PAGE_VIEW', '/home', '首页', 'direct', 'desktop', 'macOS', 'Chrome', '官网', 120, '127.0.0.1', '上海', '上海', TIMESTAMP '2026-03-10 10:00:00', CURRENT_TIMESTAMP),
(2, 1, 'S001', 'PURCHASE', '/checkout', '下单页', 'direct', 'desktop', 'macOS', 'Chrome', '官网', 60, '127.0.0.1', '上海', '上海', TIMESTAMP '2026-03-10 10:05:00', CURRENT_TIMESTAMP),
(3, 2, 'S002', 'PAGE_VIEW', '/campaign', '活动页', 'ad', 'mobile', 'iOS', 'Safari', '广告', 90, '127.0.0.1', '深圳', '广东', TIMESTAMP '2026-03-10 11:00:00', CURRENT_TIMESTAMP),
(4, 3, 'S003', 'PAGE_VIEW', '/home', '首页', 'search', 'desktop', 'Windows', 'Edge', '搜索', 100, '127.0.0.1', '北京', '北京', TIMESTAMP '2026-03-09 16:30:00', CURRENT_TIMESTAMP),
(5, 4, 'S004', 'PURCHASE', '/checkout', '下单页', 'campaign', 'mobile', 'Android', 'Chrome', '活动', 45, '127.0.0.1', '成都', '四川', TIMESTAMP '2026-03-09 18:20:00', CURRENT_TIMESTAMP),
(6, 4, 'S004', 'PAGE_VIEW', '/pricing', '报价页', 'campaign', 'mobile', 'Android', 'Chrome', '活动', 50, '127.0.0.1', '成都', '四川', TIMESTAMP '2026-03-09 18:00:00', CURRENT_TIMESTAMP);

MERGE INTO financial_record (id, record_no, record_date, category, sub_category, amount, type, department, project_id, project_name, description, status, created_at)
KEY(id) VALUES
(1, 'FR20260301', DATE '2026-03-03', 'REVENUE', 'AR', 52000, 'INCOME', '销售中心', 1, '春季大促', '回款记录', 'CONFIRMED', CURRENT_TIMESTAMP),
(2, 'FR20260302', DATE '2026-03-08', 'REVENUE', 'AR', 43000, 'INCOME', '渠道中心', 2, '渠道拓展', '回款记录', 'CONFIRMED', CURRENT_TIMESTAMP),
(3, 'FR20260303', DATE '2026-03-06', 'OPEX', 'MARKETING', 12000, 'EXPENSE', '市场部', 3, '品牌活动', '费用支出', 'CONFIRMED', CURRENT_TIMESTAMP),
(4, 'FR20260304', DATE '2026-03-09', 'OPEX', 'IT', 18000, 'EXPENSE', '研发部', 4, '平台升级', '费用支出', 'CONFIRMED', CURRENT_TIMESTAMP),
(5, 'FR20260201', DATE '2026-02-17', 'REVENUE', 'AR', 39000, 'INCOME', '销售中心', 1, '节后回款', '回款记录', 'CONFIRMED', CURRENT_TIMESTAMP);

MERGE INTO inventory (id, product_id, product_name, product_category, warehouse, quantity, unit_cost, total_value, safety_stock, last_in_date, last_out_date, status, created_at, updated_at)
KEY(id) VALUES
(1, 101, '工业平板', '电子产品', '华东仓', 120, 2800, 336000, 60, DATE '2026-02-26', DATE '2026-03-08', 'NORMAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 102, '智能终端', '电子产品', '华南仓', 95, 2600, 247000, 50, DATE '2026-02-25', DATE '2026-03-06', 'NORMAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 103, '云控网关', '企业软件', '华北仓', 40, 3100, 124000, 20, DATE '2026-02-20', DATE '2026-03-02', 'NORMAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO service_ticket (id, ticket_no, customer_id, customer_name, ticket_type, priority, status, channel, subject, assignee_id, assignee_name, created_date, first_response_date, resolved_date, response_time_minutes, resolution_time_hours, satisfaction_score, created_at)
KEY(id) VALUES
(1, 'TK20260301', 1, '华东智造', 'COMPLAINT', 'HIGH', 'CLOSED', 'PHONE', '交付延迟', 21, '客服一', TIMESTAMP '2026-03-04 09:00:00', TIMESTAMP '2026-03-04 09:20:00', TIMESTAMP '2026-03-04 16:00:00', 20, 7.00, 4, CURRENT_TIMESTAMP),
(2, 'TK20260302', 2, '华南零售', 'COMPLAINT', 'MEDIUM', 'RESOLVED', 'EMAIL', '安装问题', 22, '客服二', TIMESTAMP '2026-03-06 11:00:00', TIMESTAMP '2026-03-06 11:35:00', TIMESTAMP '2026-03-07 10:00:00', 35, 23.00, 3, CURRENT_TIMESTAMP),
(3, 'TK20260201', 3, '华北科技', 'TECHNICAL', 'LOW', 'CLOSED', 'APP', '功能咨询', 23, '客服三', TIMESTAMP '2026-02-18 14:00:00', TIMESTAMP '2026-02-18 14:10:00', TIMESTAMP '2026-02-18 18:00:00', 10, 4.00, 5, CURRENT_TIMESTAMP);

MERGE INTO project_delivery (id, project_code, project_name, owner_team, region, planned_delivery_date, actual_delivery_date, delivery_status, created_at)
KEY(id) VALUES
(1, 'PRJ001', '零售中台升级', '交付一组', '华东', DATE '2026-03-05', DATE '2026-03-04', 'DELIVERED', CURRENT_TIMESTAMP),
(2, 'PRJ002', '智能门店方案', '交付二组', '华南', DATE '2026-03-09', DATE '2026-03-11', 'DELIVERED', CURRENT_TIMESTAMP),
(3, 'PRJ003', '企业网关部署', '交付三组', '华北', DATE '2026-02-26', DATE '2026-02-25', 'DELIVERED', CURRENT_TIMESTAMP);

MERGE INTO approval_record (id, approval_no, department, process_type, applicant_name, start_time, end_time, duration_hours, status, created_at)
KEY(id) VALUES
(1, 'AP20260301', '市场部', '合同审批', '赵一', TIMESTAMP '2026-03-04 09:00:00', TIMESTAMP '2026-03-04 14:00:00', 5.00, 'APPROVED', CURRENT_TIMESTAMP),
(2, 'AP20260302', '研发部', '采购审批', '钱二', TIMESTAMP '2026-03-06 10:00:00', TIMESTAMP '2026-03-06 18:00:00', 8.00, 'APPROVED', CURRENT_TIMESTAMP),
(3, 'AP20260201', '销售中心', '折扣审批', '孙三', TIMESTAMP '2026-02-20 11:00:00', TIMESTAMP '2026-02-20 15:00:00', 4.00, 'APPROVED', CURRENT_TIMESTAMP);

MERGE INTO agile_team_member (id, user_id, username, real_name, role, department, email, status, join_date, created_at, updated_at)
KEY(id) VALUES
(1, 101, 'dev_chen', '陈开发', 'DEV', '研发部', 'dev1@chatbi.local', 'ACTIVE', DATE '2024-01-10', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 102, 'qa_lin', '林测试', 'QA', '研发部', 'qa1@chatbi.local', 'ACTIVE', DATE '2024-02-10', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 103, 'pm_wang', '王产品', 'PM', '产品部', 'pm1@chatbi.local', 'ACTIVE', DATE '2023-09-01', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO agile_story (id, project_id, sprint_id, story_code, story_title, story_type, priority, status, story_points, assignee_id, reporter_id, estimated_hours, actual_hours, created_at, updated_at, completed_at)
KEY(id) VALUES
(1, 1, 1, 'STORY001', '企业级看板重构', 'FEATURE', 'HIGH', 'DONE', 8, 101, 103, 24.00, 20.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, TIMESTAMP '2026-03-07 18:00:00'),
(2, 1, 1, 'STORY002', 'AI 对话链路修复', 'BUG', 'CRITICAL', 'DONE', 5, 102, 103, 16.00, 15.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, TIMESTAMP '2026-03-08 17:00:00'),
(3, 1, 2, 'STORY003', '查询工作台优化', 'TASK', 'MEDIUM', 'IN_PROGRESS', 3, 101, 103, 10.00, 6.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL);
