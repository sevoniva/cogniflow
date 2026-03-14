-- ============================================
-- ChatBI 敏捷研发管理测试数据
-- ============================================

-- 1. 插入团队成员数据
INSERT INTO `agile_team_member` (`user_id`, `username`, `real_name`, `role`, `department`, `email`, `join_date`) VALUES
(1001, 'zhangsan', '张三', 'DEV', '研发部', 'zhangsan@company.com', '2023-01-15'),
(1002, 'lisi', '李四', 'DEV', '研发部', 'lisi@company.com', '2023-02-20'),
(1003, 'wangwu', '王五', 'QA', '测试部', 'wangwu@company.com', '2023-03-10'),
(1004, 'zhaoliu', '赵六', 'QA', '测试部', 'zhaoliu@company.com', '2023-04-05'),
(1005, 'sunqi', '孙七', 'PM', '产品部', 'sunqi@company.com', '2023-01-01'),
(1006, 'zhouba', '周八', 'PO', '产品部', 'zhouba@company.com', '2023-02-01'),
(1007, 'wujiu', '吴九', 'SM', '研发部', 'wujiu@company.com', '2023-01-20'),
(1008, 'zhengshi', '郑十', 'ARCHITECT', '研发部', 'zhengshi@company.com', '2022-12-01'),
(1009, 'liuyi', '刘一', 'DEV', '研发部', 'liuyi@company.com', '2023-05-15'),
(1010, 'chener', '陈二', 'DEV', '研发部', 'chener@company.com', '2023-06-01');

-- 2. 插入项目数据
INSERT INTO `agile_project` (`project_code`, `project_name`, `project_type`, `status`, `start_date`, `end_date`, `team_size`, `budget`, `actual_cost`, `progress`, `priority`, `owner_id`, `department`) VALUES
('PRJ001', 'ChatBI智能分析平台', 'SCRUM', 'ACTIVE', '2024-01-01', '2024-12-31', 8, 1000000.00, 650000.00, 65, 'HIGH', 1005, '研发部'),
('PRJ002', '电商平台升级', 'SCRUM', 'ACTIVE', '2024-03-01', '2024-09-30', 12, 1500000.00, 800000.00, 53, 'HIGH', 1005, '研发部'),
('PRJ003', '移动APP开发', 'KANBAN', 'ACTIVE', '2024-02-15', '2024-08-31', 6, 800000.00, 450000.00, 56, 'MEDIUM', 1006, '研发部'),
('PRJ004', '数据中台建设', 'SCRUM', 'PLANNING', '2024-07-01', '2025-06-30', 10, 2000000.00, 0.00, 5, 'HIGH', 1008, '研发部'),
('PRJ005', '客服系统优化', 'KANBAN', 'ACTIVE', '2024-04-01', '2024-10-31', 5, 500000.00, 280000.00, 56, 'MEDIUM', 1005, '研发部');

-- 3. 插入迭代数据（最近6个月）
INSERT INTO `agile_sprint` (`project_id`, `sprint_name`, `sprint_goal`, `status`, `start_date`, `end_date`, `planned_story_points`, `completed_story_points`, `velocity`) VALUES
-- ChatBI项目迭代
(1, 'Sprint 1', '完成基础架构搭建', 'COMPLETED', '2024-01-01', '2024-01-14', 50, 48, 48.00),
(1, 'Sprint 2', '实现数据源管理', 'COMPLETED', '2024-01-15', '2024-01-28', 55, 52, 52.00),
(1, 'Sprint 3', '开发查询引擎', 'COMPLETED', '2024-01-29', '2024-02-11', 60, 58, 58.00),
(1, 'Sprint 4', '实现��视化功能', 'COMPLETED', '2024-02-12', '2024-02-25', 65, 60, 60.00),
(1, 'Sprint 5', '完善权限管理', 'COMPLETED', '2024-02-26', '2024-03-10', 58, 55, 55.00),
(1, 'Sprint 6', 'AI功能集成', 'ACTIVE', '2024-03-11', '2024-03-24', 62, 45, 45.00),
-- 电商平台迭代
(2, 'Sprint 1', '商品模块重构', 'COMPLETED', '2024-03-01', '2024-03-14', 70, 65, 65.00),
(2, 'Sprint 2', '订单系统优化', 'COMPLETED', '2024-03-15', '2024-03-28', 68, 64, 64.00),
(2, 'Sprint 3', '支付流程改进', 'ACTIVE', '2024-03-29', '2024-04-11', 72, 50, 50.00);

-- 4. 插入用户故事数据（最近3个月）
INSERT INTO `agile_story` (`project_id`, `sprint_id`, `story_code`, `story_title`, `story_type`, `priority`, `status`, `story_points`, `assignee_id`, `reporter_id`, `estimated_hours`, `actual_hours`, `completed_at`) VALUES
-- Sprint 4 已完成
(1, 4, 'STORY-001', '实现柱状图组件', 'FEATURE', 'HIGH', 'DONE', 8, 1001, 1005, 16.0, 14.5, '2024-02-18 17:30:00'),
(1, 4, 'STORY-002', '实现折线图组件', 'FEATURE', 'HIGH', 'DONE', 8, 1002, 1005, 16.0, 15.0, '2024-02-19 16:00:00'),
(1, 4, 'STORY-003', '实现饼图组件', 'FEATURE', 'MEDIUM', 'DONE', 5, 1001, 1005, 10.0, 9.5, '2024-02-20 14:30:00'),
(1, 4, 'STORY-004', '图表配置功能', 'FEATURE', 'HIGH', 'DONE', 13, 1009, 1005, 26.0, 28.0, '2024-02-23 18:00:00'),
(1, 4, 'STORY-005', '图表数据绑定', 'FEATURE', 'HIGH', 'DONE', 10, 1002, 1005, 20.0, 19.0, '2024-02-24 16:30:00'),
-- Sprint 5 已完成
(1, 5, 'STORY-006', '用户角色管理', 'FEATURE', 'HIGH', 'DONE', 13, 1001, 1005, 26.0, 25.0, '2024-03-03 17:00:00'),
(1, 5, 'STORY-007', '权限配置界面', 'FEATURE', 'HIGH', 'DONE', 10, 1009, 1005, 20.0, 22.0, '2024-03-05 16:00:00'),
(1, 5, 'STORY-008', '数据权限控制', 'FEATURE', 'MEDIUM', 'DONE', 8, 1002, 1005, 16.0, 17.5, '2024-03-07 15:30:00'),
(1, 5, 'STORY-009', '审计日志功能', 'FEATURE', 'MEDIUM', 'DONE', 8, 1001, 1005, 16.0, 14.0, '2024-03-09 17:30:00'),
-- Sprint 6 进行中
(1, 6, 'STORY-010', 'LLM接口集成', 'FEATURE', 'HIGH', 'DONE', 13, 1008, 1005, 26.0, 24.0, '2024-03-15 18:00:00'),
(1, 6, 'STORY-011', 'Text-to-SQL引擎', 'FEATURE', 'HIGH', 'DONE', 21, 1001, 1005, 42.0, 45.0, '2024-03-20 19:00:00'),
(1, 6, 'STORY-012', 'AI模型配置管理', 'FEATURE', 'MEDIUM', 'IN_PROGRESS', 8, 1002, 1005, 16.0, 10.0, NULL),
(1, 6, 'STORY-013', '智能查询优化', 'FEATURE', 'MEDIUM', 'TODO', 10, 1009, 1005, 20.0, 0.0, NULL),
-- 技术债务和Bug
(1, 6, 'STORY-014', '代码重构-查询模块', 'TECH_DEBT', 'LOW', 'TODO', 5, 1001, 1008, 10.0, 0.0, NULL),
(1, 6, 'BUG-001', '图表渲染性能问题', 'BUG', 'HIGH', 'IN_PROGRESS', 3, 1002, 1003, 6.0, 4.0, NULL);

-- 5. 插入缺陷数据（最近3个月）
INSERT INTO `agile_defect` (`project_id`, `defect_code`, `defect_title`, `severity`, `priority`, `status`, `environment`, `found_in_version`, `fixed_in_version`, `assignee_id`, `reporter_id`, `found_date`, `resolved_date`, `resolution_time_hours`) VALUES
(1, 'BUG-2024-001', '数据源连接超时', 'MAJOR', 'P1', 'CLOSED', 'TEST', 'v1.0.0', 'v1.0.1', 1001, 1003, '2024-01-15 10:30:00', '2024-01-15 16:45:00', 6.25),
(1, 'BUG-2024-002', '查询结果分页错误', 'MINOR', 'P2', 'CLOSED', 'TEST', 'v1.0.1', 'v1.0.2', 1002, 1004, '2024-01-20 14:20:00', '2024-01-21 11:30:00', 21.17),
(1, 'BUG-2024-003', '图表导出功能异常', 'CRITICAL', 'P0', 'CLOSED', 'UAT', 'v1.1.0', 'v1.1.1', 1001, 1003, '2024-02-10 09:15:00', '2024-02-10 15:30:00', 6.25),
(1, 'BUG-2024-004', '权限验证绕过漏洞', 'BLOCKER', 'P0', 'CLOSED', 'TEST', 'v1.2.0', 'v1.2.1', 1008, 1003, '2024-02-28 16:00:00', '2024-02-29 10:00:00', 18.00),
(1, 'BUG-2024-005', 'SQL注入风险', 'CRITICAL', 'P0', 'CLOSED', 'TEST', 'v1.2.1', 'v1.2.2', 1001, 1004, '2024-03-05 11:20:00', '2024-03-05 17:45:00', 6.42),
(1, 'BUG-2024-006', '内存泄漏问题', 'MAJOR', 'P1', 'CLOSED', 'PROD', 'v1.2.2', 'v1.2.3', 1002, 1003, '2024-03-08 08:30:00', '2024-03-09 14:20:00', 29.83),
(1, 'BUG-2024-007', 'UI样式错乱', 'MINOR', 'P3', 'CLOSED', 'TEST', 'v1.3.0', 'v1.3.1', 1009, 1004, '2024-03-12 15:40:00', '2024-03-13 09:30:00', 17.83),
(1, 'BUG-2024-008', '图表数据刷新延迟', 'MAJOR', 'P2', 'IN_PROGRESS', 'TEST', 'v1.3.1', NULL, 1002, 1003, '2024-03-18 10:15:00', NULL, NULL),
(1, 'BUG-2024-009', '导出Excel格式错误', 'MINOR', 'P3', 'OPEN', 'TEST', 'v1.3.1', NULL, NULL, 1004, '2024-03-20 14:30:00', NULL, NULL),
(2, 'BUG-2024-010', '订单状态更新失败', 'CRITICAL', 'P0', 'CLOSED', 'PROD', 'v2.1.0', 'v2.1.1', 1001, 1003, '2024-03-15 20:30:00', '2024-03-15 23:45:00', 3.25);

-- 6. 插入测试用例数据
INSERT INTO `agile_test_case` (`project_id`, `case_code`, `case_title`, `case_type`, `priority`, `status`, `automation_status`, `created_by`) VALUES
(1, 'TC-001', '数据源连接测试', 'FUNCTIONAL', 'HIGH', 'APPROVED', 'AUTOMATED', 1003),
(1, 'TC-002', '查询功能测试', 'FUNCTIONAL', 'HIGH', 'APPROVED', 'AUTOMATED', 1003),
(1, 'TC-003', '图表渲染测试', 'FUNCTIONAL', 'HIGH', 'APPROVED', 'SEMI_AUTOMATED', 1004),
(1, 'TC-004', '权限验证测试', 'FUNCTIONAL', 'HIGH', 'APPROVED', 'AUTOMATED', 1003),
(1, 'TC-005', '性能压力测试', 'PERFORMANCE', 'MEDIUM', 'APPROVED', 'AUTOMATED', 1004),
(1, 'TC-006', '安全渗透测试', 'SECURITY', 'HIGH', 'APPROVED', 'MANUAL', 1003),
(1, 'TC-007', '接口集成测试', 'INTEGRATION', 'HIGH', 'APPROVED', 'AUTOMATED', 1004),
(1, 'TC-008', '数据导出测试', 'FUNCTIONAL', 'MEDIUM', 'APPROVED', 'SEMI_AUTOMATED', 1003),
(1, 'TC-009', 'AI查询测试', 'FUNCTIONAL', 'HIGH', 'REVIEW', 'MANUAL', 1004),
(1, 'TC-010', '并发访问测试', 'PERFORMANCE', 'MEDIUM', 'APPROVED', 'AUTOMATED', 1003);

-- 7. 插入测试执行数据（最近3个月）
INSERT INTO `agile_test_execution` (`project_id`, `test_case_id`, `sprint_id`, `execution_date`, `result`, `executor_id`, `execution_time_minutes`, `defects_found`) VALUES
-- Sprint 4 测试
(1, 1, 4, '2024-02-12', 'PASS', 1003, 15, 0),
(1, 2, 4, '2024-02-13', 'PASS', 1003, 25, 0),
(1, 3, 4, '2024-02-14', 'FAIL', 1004, 30, 1),
(1, 3, 4, '2024-02-15', 'PASS', 1004, 28, 0),
(1, 4, 4, '2024-02-16', 'PASS', 1003, 20, 0),
(1, 5, 4, '2024-02-20', 'PASS', 1004, 120, 0),
(1, 7, 4, '2024-02-22', 'PASS', 1003, 45, 0),
(1, 8, 4, '2024-02-23', 'PASS', 1004, 35, 0),
-- Sprint 5 测试
(1, 1, 5, '2024-02-26', 'PASS', 1003, 12, 0),
(1, 2, 5, '2024-02-27', 'PASS', 1003, 22, 0),
(1, 4, 5, '2024-02-28', 'FAIL', 1004, 25, 1),
(1, 4, 5, '2024-02-29', 'PASS', 1004, 23, 0),
(1, 6, 5, '2024-03-01', 'PASS', 1003, 180, 0),
(1, 7, 5, '2024-03-05', 'PASS', 1004, 50, 0),
(1, 8, 5, '2024-03-08', 'PASS', 1003, 32, 0),
-- Sprint 6 测试
(1, 1, 6, '2024-03-11', 'PASS', 1003, 10, 0),
(1, 2, 6, '2024-03-12', 'FAIL', 1004, 28, 1),
(1, 2, 6, '2024-03-13', 'PASS', 1004, 26, 0),
(1, 3, 6, '2024-03-14', 'FAIL', 1003, 32, 1),
(1, 4, 6, '2024-03-15', 'PASS', 1004, 22, 0),
(1, 7, 6, '2024-03-18', 'PASS', 1003, 48, 0),
(1, 9, 6, '2024-03-19', 'PASS', 1004, 40, 0),
(1, 10, 6, '2024-03-20', 'PASS', 1003, 150, 0);

-- 8. 插入代码提交数据（最近3个月）
INSERT INTO `agile_code_commit` (`project_id`, `commit_hash`, `author_id`, `commit_message`, `files_changed`, `lines_added`, `lines_deleted`, `commit_date`, `branch`) VALUES
(1, 'a1b2c3d4e5f6', 1001, 'feat: 实现柱状图组件', 8, 320, 15, '2024-02-15 10:30:00', 'feature/chart-bar'),
(1, 'b2c3d4e5f6g7', 1002, 'feat: 实现折线图组件', 7, 280, 12, '2024-02-16 14:20:00', 'feature/chart-line'),
(1, 'c3d4e5f6g7h8', 1001, 'feat: 实现饼图组件', 6, 245, 8, '2024-02-17 16:45:00', 'feature/chart-pie'),
(1, 'd4e5f6g7h8i9', 1009, 'feat: 图表配置功能', 12, 450, 35, '2024-02-20 11:15:00', 'feature/chart-config'),
(1, 'e5f6g7h8i9j0', 1002, 'feat: 图表数据绑定', 10, 380, 28, '2024-02-22 15:30:00', 'feature/chart-data'),
(1, 'f6g7h8i9j0k1', 1001, 'feat: 用户角色管理', 15, 520, 42, '2024-02-28 09:45:00', 'feature/role-management'),
(1, 'g7h8i9j0k1l2', 1009, 'feat: 权限配置界面', 11, 410, 25, '2024-03-02 13:20:00', 'feature/permission-ui'),
(1, 'h8i9j0k1l2m3', 1002, 'feat: 数据权限控制', 9, 350, 30, '2024-03-05 10:50:00', 'feature/data-permission'),
(1, 'i9j0k1l2m3n4', 1001, 'feat: 审计日志功能', 8, 290, 18, '2024-03-07 16:10:00', 'feature/audit-log'),
(1, 'j0k1l2m3n4o5', 1008, 'feat: LLM接口集成', 14, 580, 45, '2024-03-12 14:35:00', 'feature/llm-integration'),
(1, 'k1l2m3n4o5p6', 1001, 'feat: Text-to-SQL引擎', 18, 720, 65, '2024-03-18 17:20:00', 'feature/text-to-sql'),
(1, 'l2m3n4o5p6q7', 1002, 'fix: 修复图表渲染bug', 5, 85, 92, '2024-03-19 11:40:00', 'bugfix/chart-render'),
(1, 'm3n4o5p6q7r8', 1001, 'refactor: 重构查询模块', 12, 420, 380, '2024-03-20 15:55:00', 'refactor/query-module');

-- 9. 插入部署记录（最近3个月）
INSERT INTO `agile_deployment` (`project_id`, `version`, `environment`, `status`, `deploy_date`, `deployer_id`, `duration_minutes`, `build_number`) VALUES
(1, 'v1.0.0', 'DEV', 'SUCCESS', '2024-01-15 09:00:00', 1007, 8, 'BUILD-001'),
(1, 'v1.0.1', 'DEV', 'SUCCESS', '2024-01-16 10:30:00', 1007, 7, 'BUILD-002'),
(1, 'v1.0.1', 'TEST', 'SUCCESS', '2024-01-17 14:00:00', 1007, 10, 'BUILD-002'),
(1, 'v1.1.0', 'DEV', 'SUCCESS', '2024-02-12 09:15:00', 1007, 9, 'BUILD-010'),
(1, 'v1.1.0', 'TEST', 'SUCCESS', '2024-02-13 15:30:00', 1007, 12, 'BUILD-010'),
(1, 'v1.1.1', 'TEST', 'SUCCESS', '2024-02-14 10:45:00', 1007, 8, 'BUILD-011'),
(1, 'v1.1.1', 'UAT', 'SUCCESS', '2024-02-15 16:20:00', 1007, 15, 'BUILD-011'),
(1, 'v1.2.0', 'DEV', 'SUCCESS', '2024-02-26 09:30:00', 1007, 10, 'BUILD-020'),
(1, 'v1.2.0', 'TEST', 'SUCCESS', '2024-02-27 14:15:00', 1007, 11, 'BUILD-020'),
(1, 'v1.2.1', 'TEST', 'SUCCESS', '2024-02-29 11:00:00', 1007, 9, 'BUILD-021'),
(1, 'v1.2.2', 'TEST', 'SUCCESS', '2024-03-06 10:20:00', 1007, 8, 'BUILD-022'),
(1, 'v1.2.3', 'TEST', 'SUCCESS', '2024-03-09 15:40:00', 1007, 10, 'BUILD-023'),
(1, 'v1.2.3', 'UAT', 'SUCCESS', '2024-03-10 09:50:00', 1007, 14, 'BUILD-023'),
(1, 'v1.3.0', 'DEV', 'SUCCESS', '2024-03-11 09:00:00', 1007, 11, 'BUILD-030'),
(1, 'v1.3.0', 'TEST', 'SUCCESS', '2024-03-12 14:30:00', 1007, 13, 'BUILD-030'),
(1, 'v1.3.1', 'TEST', 'SUCCESS', '2024-03-13 10:15:00', 1007, 9, 'BUILD-031'),
(1, 'v1.3.1', 'UAT', 'FAILED', '2024-03-14 15:45:00', 1007, 18, 'BUILD-031'),
(1, 'v1.3.1', 'UAT', 'SUCCESS', '2024-03-15 10:30:00', 1007, 16, 'BUILD-031');

-- 10. 插入质量指标数据（最近3个月）
INSERT INTO `agile_quality_metrics` (`project_id`, `metric_date`, `code_coverage`, `unit_test_pass_rate`, `integration_test_pass_rate`, `defect_density`, `defect_removal_efficiency`, `mean_time_to_repair`, `technical_debt_ratio`) VALUES
(1, '2024-01-31', 75.50, 92.30, 88.50, 0.85, 89.50, 12.50, 8.20),
(1, '2024-02-29', 78.20, 94.10, 90.20, 0.72, 91.30, 10.80, 7.50),
(1, '2024-03-10', 82.30, 95.80, 92.50, 0.65, 93.20, 9.20, 6.80),
(1, '2024-03-20', 85.10, 96.50, 94.10, 0.58, 94.80, 8.50, 6.20),
(2, '2024-03-31', 72.40, 90.20, 85.30, 1.20, 87.50, 15.30, 10.50);
