INSERT INTO project_delivery (id, project_code, project_name, owner_team, region, planned_delivery_date, actual_delivery_date, delivery_status, created_at)
SELECT 1, 'DEL-001', '经营分析中台一期', '数据产品组', '华东', '2026-01-20', '2026-01-18', 'ON_TIME', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM project_delivery WHERE project_code = 'DEL-001');
INSERT INTO project_delivery (id, project_code, project_name, owner_team, region, planned_delivery_date, actual_delivery_date, delivery_status, created_at)
SELECT 2, 'DEL-002', '区域经营驾驶舱', 'BI研发组', '华南', '2026-02-15', '2026-02-17', 'DELAYED', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM project_delivery WHERE project_code = 'DEL-002');
INSERT INTO project_delivery (id, project_code, project_name, owner_team, region, planned_delivery_date, actual_delivery_date, delivery_status, created_at)
SELECT 3, 'DEL-003', '零售数据治理项目', '治理实施组', '华北', '2026-02-28', '2026-02-26', 'ON_TIME', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM project_delivery WHERE project_code = 'DEL-003');
INSERT INTO project_delivery (id, project_code, project_name, owner_team, region, planned_delivery_date, actual_delivery_date, delivery_status, created_at)
SELECT 4, 'DEL-004', '研发效能平台升级', '平台工程组', '华东', '2026-03-08', '2026-03-08', 'ON_TIME', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM project_delivery WHERE project_code = 'DEL-004');
INSERT INTO project_delivery (id, project_code, project_name, owner_team, region, planned_delivery_date, actual_delivery_date, delivery_status, created_at)
SELECT 5, 'DEL-005', '服务运营告警中心', '运维支撑组', '西南', '2026-03-12', '2026-03-15', 'DELAYED', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM project_delivery WHERE project_code = 'DEL-005');

INSERT INTO approval_record (id, approval_no, department, process_type, applicant_name, start_time, end_time, duration_hours, status, created_at)
SELECT 1, 'APR-202603-001', '财务部', '费用报销', '张敏', '2026-03-01 09:00:00', '2026-03-02 11:00:00', 26.00, 'APPROVED', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM approval_record WHERE approval_no = 'APR-202603-001');
INSERT INTO approval_record (id, approval_no, department, process_type, applicant_name, start_time, end_time, duration_hours, status, created_at)
SELECT 2, 'APR-202603-002', '市场部', '采购申请', '李璐', '2026-03-02 10:30:00', '2026-03-02 18:30:00', 8.00, 'APPROVED', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM approval_record WHERE approval_no = 'APR-202603-002');
INSERT INTO approval_record (id, approval_no, department, process_type, applicant_name, start_time, end_time, duration_hours, status, created_at)
SELECT 3, 'APR-202603-003', '销售部', '合同审批', '王凯', '2026-03-03 08:10:00', '2026-03-04 16:10:00', 32.00, 'APPROVED', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM approval_record WHERE approval_no = 'APR-202603-003');
INSERT INTO approval_record (id, approval_no, department, process_type, applicant_name, start_time, end_time, duration_hours, status, created_at)
SELECT 4, 'APR-202603-004', '运营部', '资源申请', '赵娜', '2026-03-04 13:00:00', '2026-03-04 17:00:00', 4.00, 'APPROVED', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM approval_record WHERE approval_no = 'APR-202603-004');
INSERT INTO approval_record (id, approval_no, department, process_type, applicant_name, start_time, end_time, duration_hours, status, created_at)
SELECT 5, 'APR-202603-005', '产品部', '版本发布', '陈涛', '2026-03-05 09:20:00', '2026-03-06 12:20:00', 27.00, 'APPROVED', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM approval_record WHERE approval_no = 'APR-202603-005');
INSERT INTO approval_record (id, approval_no, department, process_type, applicant_name, start_time, end_time, duration_hours, status, created_at)
SELECT 6, 'APR-202602-001', '财务部', '差旅报销', '林悦', '2026-02-03 09:15:00', '2026-02-04 14:15:00', 29.00, 'APPROVED', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM approval_record WHERE approval_no = 'APR-202602-001');
INSERT INTO approval_record (id, approval_no, department, process_type, applicant_name, start_time, end_time, duration_hours, status, created_at)
SELECT 7, 'APR-202602-002', '市场部', '活动预算', '周岚', '2026-02-06 11:00:00', '2026-02-07 08:00:00', 21.00, 'APPROVED', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM approval_record WHERE approval_no = 'APR-202602-002');
INSERT INTO approval_record (id, approval_no, department, process_type, applicant_name, start_time, end_time, duration_hours, status, created_at)
SELECT 8, 'APR-202602-003', '销售部', '合同折扣审批', '许辰', '2026-02-10 08:40:00', '2026-02-11 17:40:00', 33.00, 'APPROVED', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM approval_record WHERE approval_no = 'APR-202602-003');
INSERT INTO approval_record (id, approval_no, department, process_type, applicant_name, start_time, end_time, duration_hours, status, created_at)
SELECT 9, 'APR-202602-004', '运营部', '资源采购', '韩雪', '2026-02-14 13:20:00', '2026-02-14 19:20:00', 6.00, 'APPROVED', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM approval_record WHERE approval_no = 'APR-202602-004');
INSERT INTO approval_record (id, approval_no, department, process_type, applicant_name, start_time, end_time, duration_hours, status, created_at)
SELECT 10, 'APR-202602-005', '产品部', '版本冻结申请', '宋洋', '2026-02-18 10:10:00', '2026-02-19 14:10:00', 28.00, 'APPROVED', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM approval_record WHERE approval_no = 'APR-202602-005');

INSERT INTO financial_record (record_no, record_date, category, sub_category, amount, type, department, project_name, description, status, created_at)
SELECT 'FR-EX-001', '2026-03-01', 'REVENUE', '回款', 480000.00, 'INCOME', '销售部', '华东大区续费', '补充回款演示数据', 'CONFIRMED', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM financial_record WHERE record_no = 'FR-EX-001');
INSERT INTO financial_record (record_no, record_date, category, sub_category, amount, type, department, project_name, description, status, created_at)
SELECT 'FR-EX-002', '2026-03-02', 'EXPENSE', '差旅', 56000.00, 'EXPENSE', '销售部', '客户拜访', '补充费用演示数据', 'CONFIRMED', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM financial_record WHERE record_no = 'FR-EX-002');
INSERT INTO service_ticket (ticket_no, customer_id, customer_name, ticket_type, priority, status, channel, subject, assignee_id, assignee_name, created_date, first_response_date, resolved_date, response_time_minutes, resolution_time_hours, satisfaction_score, created_at)
SELECT 'T-EX-001', 1, '客户1', 'COMPLAINT', 'HIGH', 'CLOSED', 'PHONE', '华东区发票投诉', 101, '客服A', '2026-03-01 08:20:00', '2026-03-01 08:35:00', '2026-03-01 12:10:00', 15, 3.58, 4, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM service_ticket WHERE ticket_no = 'T-EX-001');
INSERT INTO service_ticket (ticket_no, customer_id, customer_name, ticket_type, priority, status, channel, subject, assignee_id, assignee_name, created_date, first_response_date, resolved_date, response_time_minutes, resolution_time_hours, satisfaction_score, created_at)
SELECT 'T-EX-002', 2, '客户2', 'COMPLAINT', 'MEDIUM', 'CLOSED', 'APP', '华南区物流延误投诉', 102, '客服B', '2026-03-02 10:05:00', '2026-03-02 10:20:00', '2026-03-02 16:40:00', 15, 6.33, 3, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM service_ticket WHERE ticket_no = 'T-EX-002');

INSERT INTO app_user (user_no, username, nickname, gender, age, city, province, register_channel, register_date, last_login_date, login_count, status, created_at)
SELECT 'U90000001', 'demo_user_01', '演示用户01', 'MALE', 32, '上海', '上海', 'ORGANIC', '2026-03-02', '2026-03-10', 6, 'ACTIVE', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE user_no = 'U90000001');
INSERT INTO app_user (user_no, username, nickname, gender, age, city, province, register_channel, register_date, last_login_date, login_count, status, created_at)
SELECT 'U90000002', 'demo_user_02', '演示用户02', 'FEMALE', 28, '深圳', '广东', 'SOCIAL', '2026-03-03', '2026-03-10', 4, 'ACTIVE', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE user_no = 'U90000002');

INSERT INTO user_behavior (user_id, session_id, event_type, page_url, page_title, referrer, device_type, os, browser, channel, duration, ip_address, city, province, event_time, created_at)
SELECT 90000001, 'S-DEMO-001', 'PAGE_VIEW', '/sales-dashboard', '销售分析', '/', 'PC', 'macOS', 'Chrome', 'ORGANIC', 180, '127.0.0.1', '上海', '上海', '2026-03-10 09:12:00', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM user_behavior WHERE session_id = 'S-DEMO-001');
INSERT INTO user_behavior (user_id, session_id, event_type, page_url, page_title, referrer, device_type, os, browser, channel, duration, ip_address, city, province, event_time, created_at)
SELECT 90000001, 'S-DEMO-001', 'PURCHASE', '/checkout', '确认支付', '/cart', 'PC', 'macOS', 'Chrome', 'ORGANIC', 45, '127.0.0.1', '上海', '上海', '2026-03-10 09:28:00', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM user_behavior WHERE session_id = 'S-DEMO-001' AND event_type = 'PURCHASE');
INSERT INTO user_behavior (user_id, session_id, event_type, page_url, page_title, referrer, device_type, os, browser, channel, duration, ip_address, city, province, event_time, created_at)
SELECT 90000002, 'S-DEMO-002', 'PAGE_VIEW', '/operation-dashboard', '运营分析', '/', 'MOBILE', 'iOS', 'Safari', 'SOCIAL', 210, '127.0.0.1', '深圳', '广东', '2026-03-10 10:05:00', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM user_behavior WHERE session_id = 'S-DEMO-002');

INSERT INTO sales_order (order_no, customer_id, customer_name, product_id, product_name, product_category, region, sales_person_id, sales_person_name, quantity, unit_price, sales_amount, cost_amount, profit_amount, discount_amount, order_date, delivery_date, status, created_at)
SELECT 'SO-EX-001', 1, '客户1', 1001, '企业版驾驶舱许可', '软件服务', '华东', 11, '销售11', 3, 68000.00, 204000.00, 108000.00, 96000.00, 0.00, '2026-03-03', '2026-03-05', 'DELIVERED', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sales_order WHERE order_no = 'SO-EX-001');
INSERT INTO sales_order (order_no, customer_id, customer_name, product_id, product_name, product_category, region, sales_person_id, sales_person_name, quantity, unit_price, sales_amount, cost_amount, profit_amount, discount_amount, order_date, delivery_date, status, created_at)
SELECT 'SO-EX-002', 2, '客户2', 1002, '经营洞察订阅', '软件服务', '华南', 12, '销售12', 5, 36000.00, 180000.00, 98000.00, 82000.00, 0.00, '2026-03-04', '2026-03-06', 'DELIVERED', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sales_order WHERE order_no = 'SO-EX-002');
INSERT INTO sales_order (order_no, customer_id, customer_name, product_id, product_name, product_category, region, sales_person_id, sales_person_name, quantity, unit_price, sales_amount, cost_amount, profit_amount, discount_amount, order_date, delivery_date, status, created_at)
SELECT 'SO-EX-003', 3, '客户3', 1003, '数据治理咨询包', '咨询服务', '华北', 13, '销售13', 2, 92000.00, 184000.00, 106000.00, 78000.00, 0.00, '2026-03-06', '2026-03-11', 'SHIPPED', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sales_order WHERE order_no = 'SO-EX-003');

INSERT INTO agile_sprint (id, project_id, sprint_name, sprint_goal, status, start_date, end_date, planned_story_points, completed_story_points, velocity, created_at, updated_at)
SELECT 1001, 1, 'Sprint 2026-03', '完成企业版交付能力升级', 'ACTIVE', '2026-03-01', '2026-03-14', 72, 40, 40.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM agile_sprint WHERE id = 1001);

INSERT INTO agile_story (id, project_id, sprint_id, story_code, story_title, story_type, priority, status, story_points, assignee_id, reporter_id, estimated_hours, actual_hours, created_at, updated_at, completed_at)
SELECT 2001, 1, 1001, 'STORY-2026-001', '重构管理后台数据访问层', 'FEATURE', 'HIGH', 'DONE', 8, 1001, 1005, 18.00, 17.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '2026-03-06 18:00:00'
WHERE NOT EXISTS (SELECT 1 FROM agile_story WHERE id = 2001);
INSERT INTO agile_story (id, project_id, sprint_id, story_code, story_title, story_type, priority, status, story_points, assignee_id, reporter_id, estimated_hours, actual_hours, created_at, updated_at, completed_at)
SELECT 2002, 1, 1001, 'STORY-2026-002', '补齐企业图表主题体系', 'FEATURE', 'HIGH', 'DONE', 13, 1002, 1005, 28.00, 26.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '2026-03-08 20:10:00'
WHERE NOT EXISTS (SELECT 1 FROM agile_story WHERE id = 2002);
INSERT INTO agile_story (id, project_id, sprint_id, story_code, story_title, story_type, priority, status, story_points, assignee_id, reporter_id, estimated_hours, actual_hours, created_at, updated_at, completed_at)
SELECT 2003, 1, 1001, 'STORY-2026-003', '完善看板冒烟测试链路', 'TASK', 'MEDIUM', 'DONE', 5, 1009, 1005, 12.00, 11.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '2026-03-09 16:30:00'
WHERE NOT EXISTS (SELECT 1 FROM agile_story WHERE id = 2003);

INSERT INTO agile_test_execution (id, project_id, test_case_id, sprint_id, execution_date, result, executor_id, execution_time_minutes, defects_found, created_at)
SELECT 3001, 1, 1, 1001, '2026-03-07', 'PASS', 1003, 18, 0, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM agile_test_execution WHERE id = 3001);
INSERT INTO agile_test_execution (id, project_id, test_case_id, sprint_id, execution_date, result, executor_id, execution_time_minutes, defects_found, created_at)
SELECT 3002, 1, 2, 1001, '2026-03-08', 'PASS', 1004, 24, 0, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM agile_test_execution WHERE id = 3002);
INSERT INTO agile_test_execution (id, project_id, test_case_id, sprint_id, execution_date, result, executor_id, execution_time_minutes, defects_found, created_at)
SELECT 3003, 1, 7, 1001, '2026-03-09', 'FAIL', 1003, 36, 1, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM agile_test_execution WHERE id = 3003);
INSERT INTO agile_test_execution (id, project_id, test_case_id, sprint_id, execution_date, result, executor_id, execution_time_minutes, defects_found, created_at)
SELECT 3004, 1, 7, 1001, '2026-03-10', 'PASS', 1003, 28, 0, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM agile_test_execution WHERE id = 3004);

INSERT INTO agile_code_commit (id, project_id, commit_hash, author_id, commit_message, files_changed, lines_added, lines_deleted, commit_date, branch, created_at)
SELECT 4001, 1, 'demo20260301', 1001, 'feat: 接入企业演示数据底座', 14, 520, 98, '2026-03-06 11:20:00', 'feature/enterprise-data', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM agile_code_commit WHERE id = 4001);
INSERT INTO agile_code_commit (id, project_id, commit_hash, author_id, commit_message, files_changed, lines_added, lines_deleted, commit_date, branch, created_at)
SELECT 4002, 1, 'demo20260302', 1002, 'refactor: 统一查询结果页图表结构', 9, 308, 76, '2026-03-08 15:40:00', 'refactor/query-ux', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM agile_code_commit WHERE id = 4002);
INSERT INTO agile_code_commit (id, project_id, commit_hash, author_id, commit_message, files_changed, lines_added, lines_deleted, commit_date, branch, created_at)
SELECT 4003, 1, 'demo20260303', 1009, 'test: 完善前后端冒烟校验', 6, 146, 22, '2026-03-10 09:10:00', 'test/full-smoke', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM agile_code_commit WHERE id = 4003);

INSERT INTO agile_deployment (id, project_id, version, environment, status, deploy_date, deployer_id, duration_minutes, build_number, created_at)
SELECT 5001, 1, 'v2.6.0', 'TEST', 'SUCCESS', '2026-03-08 19:30:00', 1007, 12, 'BUILD-260', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM agile_deployment WHERE id = 5001);
INSERT INTO agile_deployment (id, project_id, version, environment, status, deploy_date, deployer_id, duration_minutes, build_number, created_at)
SELECT 5002, 1, 'v2.6.0', 'UAT', 'SUCCESS', '2026-03-10 20:15:00', 1007, 16, 'BUILD-260', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM agile_deployment WHERE id = 5002);

INSERT INTO agile_quality_metrics (id, project_id, metric_date, code_coverage, unit_test_pass_rate, integration_test_pass_rate, defect_density, defect_removal_efficiency, mean_time_to_repair, technical_debt_ratio, created_at)
SELECT 6001, 1, '2026-03-07', 86.40, 97.20, 95.10, 0.42, 95.50, 7.80, 5.90, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM agile_quality_metrics WHERE id = 6001);
INSERT INTO agile_quality_metrics (id, project_id, metric_date, code_coverage, unit_test_pass_rate, integration_test_pass_rate, defect_density, defect_removal_efficiency, mean_time_to_repair, technical_debt_ratio, created_at)
SELECT 6002, 1, '2026-03-10', 87.80, 98.10, 96.30, 0.38, 96.20, 6.90, 5.60, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM agile_quality_metrics WHERE id = 6002);
