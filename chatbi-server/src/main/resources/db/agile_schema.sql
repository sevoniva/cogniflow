-- ============================================
-- ChatBI 敏捷研发管理数据库
-- ============================================

-- 1. 项目表
CREATE TABLE IF NOT EXISTS `agile_project` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '项目ID',
  `project_code` VARCHAR(50) NOT NULL COMMENT '项目编码',
  `project_name` VARCHAR(200) NOT NULL COMMENT '项目名称',
  `project_type` VARCHAR(50) NOT NULL COMMENT '项目类型：SCRUM/KANBAN/WATERFALL',
  `status` VARCHAR(20) NOT NULL COMMENT '状态：PLANNING/ACTIVE/SUSPENDED/CLOSED',
  `start_date` DATE COMMENT '开始日期',
  `end_date` DATE COMMENT '结束日期',
  `team_size` INT COMMENT '团队规模',
  `budget` DECIMAL(15,2) COMMENT '预算',
  `actual_cost` DECIMAL(15,2) COMMENT '实际成本',
  `progress` INT DEFAULT 0 COMMENT '进度百分比',
  `priority` VARCHAR(20) COMMENT '优先级：HIGH/MEDIUM/LOW',
  `owner_id` BIGINT COMMENT '负责人ID',
  `department` VARCHAR(100) COMMENT '所属部门',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_code` (`project_code`),
  KEY `idx_status` (`status`),
  KEY `idx_start_date` (`start_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='敏捷项目表';

-- 2. 迭代/冲刺表
CREATE TABLE IF NOT EXISTS `agile_sprint` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '迭代ID',
  `project_id` BIGINT NOT NULL COMMENT '项目ID',
  `sprint_name` VARCHAR(100) NOT NULL COMMENT '迭代名称',
  `sprint_goal` TEXT COMMENT '迭代目标',
  `status` VARCHAR(20) NOT NULL COMMENT '状态：PLANNING/ACTIVE/COMPLETED/CANCELLED',
  `start_date` DATE NOT NULL COMMENT '开始日期',
  `end_date` DATE NOT NULL COMMENT '结束日期',
  `planned_story_points` INT COMMENT '计划故事点',
  `completed_story_points` INT COMMENT '完成故事点',
  `velocity` DECIMAL(10,2) COMMENT '速率',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_status` (`status`),
  KEY `idx_start_date` (`start_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='敏捷迭代表';

-- 3. 需求/用户故事表
CREATE TABLE IF NOT EXISTS `agile_story` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '故事ID',
  `project_id` BIGINT NOT NULL COMMENT '项目ID',
  `sprint_id` BIGINT COMMENT '迭代ID',
  `story_code` VARCHAR(50) NOT NULL COMMENT '故事编号',
  `story_title` VARCHAR(500) NOT NULL COMMENT '故事标题',
  `story_type` VARCHAR(20) NOT NULL COMMENT '类型：FEATURE/BUG/TASK/TECH_DEBT',
  `priority` VARCHAR(20) NOT NULL COMMENT '优先级：CRITICAL/HIGH/MEDIUM/LOW',
  `status` VARCHAR(20) NOT NULL COMMENT '状态：TODO/IN_PROGRESS/TESTING/DONE/BLOCKED',
  `story_points` INT COMMENT '故事点',
  `assignee_id` BIGINT COMMENT '指派人ID',
  `reporter_id` BIGINT COMMENT '报告人ID',
  `estimated_hours` DECIMAL(10,2) COMMENT '预估工时',
  `actual_hours` DECIMAL(10,2) COMMENT '实际工时',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `completed_at` DATETIME COMMENT '完成时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_story_code` (`story_code`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_sprint_id` (`sprint_id`),
  KEY `idx_status` (`status`),
  KEY `idx_story_type` (`story_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户故事表';

-- 4. 缺陷表
CREATE TABLE IF NOT EXISTS `agile_defect` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '缺陷ID',
  `project_id` BIGINT NOT NULL COMMENT '项目ID',
  `defect_code` VARCHAR(50) NOT NULL COMMENT '缺陷编号',
  `defect_title` VARCHAR(500) NOT NULL COMMENT '缺陷标题',
  `severity` VARCHAR(20) NOT NULL COMMENT '严重程度：BLOCKER/CRITICAL/MAJOR/MINOR/TRIVIAL',
  `priority` VARCHAR(20) NOT NULL COMMENT '优先级：P0/P1/P2/P3/P4',
  `status` VARCHAR(20) NOT NULL COMMENT '状态：OPEN/IN_PROGRESS/RESOLVED/CLOSED/REOPENED',
  `environment` VARCHAR(50) COMMENT '环境：DEV/TEST/UAT/PROD',
  `found_in_version` VARCHAR(50) COMMENT '发现版本',
  `fixed_in_version` VARCHAR(50) COMMENT '修复版本',
  `assignee_id` BIGINT COMMENT '指派人ID',
  `reporter_id` BIGINT COMMENT '报告人ID',
  `found_date` DATETIME COMMENT '发现时间',
  `resolved_date` DATETIME COMMENT '解决时间',
  `resolution_time_hours` DECIMAL(10,2) COMMENT '解决耗时(小时)',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_defect_code` (`defect_code`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_severity` (`severity`),
  KEY `idx_status` (`status`),
  KEY `idx_environment` (`environment`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='缺陷表';

-- 5. 测试用例表
CREATE TABLE IF NOT EXISTS `agile_test_case` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用例ID',
  `project_id` BIGINT NOT NULL COMMENT '项目ID',
  `case_code` VARCHAR(50) NOT NULL COMMENT '用例编号',
  `case_title` VARCHAR(500) NOT NULL COMMENT '用例标题',
  `case_type` VARCHAR(20) NOT NULL COMMENT '类型：FUNCTIONAL/INTEGRATION/PERFORMANCE/SECURITY',
  `priority` VARCHAR(20) NOT NULL COMMENT '优先级：HIGH/MEDIUM/LOW',
  `status` VARCHAR(20) NOT NULL COMMENT '状态：DRAFT/REVIEW/APPROVED/DEPRECATED',
  `automation_status` VARCHAR(20) COMMENT '自动化状态：MANUAL/AUTOMATED/SEMI_AUTOMATED',
  `created_by` BIGINT COMMENT '创建人ID',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_case_code` (`case_code`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_case_type` (`case_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测试用例表';

-- 6. 测试执行表
CREATE TABLE IF NOT EXISTS `agile_test_execution` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '执行ID',
  `project_id` BIGINT NOT NULL COMMENT '项目ID',
  `test_case_id` BIGINT NOT NULL COMMENT '测试用例ID',
  `sprint_id` BIGINT COMMENT '迭代ID',
  `execution_date` DATE NOT NULL COMMENT '执行日期',
  `result` VARCHAR(20) NOT NULL COMMENT '结果：PASS/FAIL/BLOCKED/SKIP',
  `executor_id` BIGINT COMMENT '执行人ID',
  `execution_time_minutes` INT COMMENT '执行耗时(分钟)',
  `defects_found` INT DEFAULT 0 COMMENT '发现缺陷数',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_test_case_id` (`test_case_id`),
  KEY `idx_execution_date` (`execution_date`),
  KEY `idx_result` (`result`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测试执行表';

-- 7. 代码提交表
CREATE TABLE IF NOT EXISTS `agile_code_commit` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '提交ID',
  `project_id` BIGINT NOT NULL COMMENT '项目ID',
  `commit_hash` VARCHAR(100) NOT NULL COMMENT '提交哈希',
  `author_id` BIGINT NOT NULL COMMENT '作者ID',
  `commit_message` TEXT COMMENT '提交信息',
  `files_changed` INT COMMENT '变更文件数',
  `lines_added` INT COMMENT '新增行数',
  `lines_deleted` INT COMMENT '删除行数',
  `commit_date` DATETIME NOT NULL COMMENT '提交时间',
  `branch` VARCHAR(100) COMMENT '分支',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_commit_hash` (`commit_hash`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_commit_date` (`commit_date`),
  KEY `idx_author_id` (`author_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码提交表';

-- 8. 构建部署表
CREATE TABLE IF NOT EXISTS `agile_deployment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '部署ID',
  `project_id` BIGINT NOT NULL COMMENT '项目ID',
  `version` VARCHAR(50) NOT NULL COMMENT '版本号',
  `environment` VARCHAR(20) NOT NULL COMMENT '环境：DEV/TEST/UAT/PROD',
  `status` VARCHAR(20) NOT NULL COMMENT '状态：SUCCESS/FAILED/ROLLBACK',
  `deploy_date` DATETIME NOT NULL COMMENT '部署时间',
  `deployer_id` BIGINT COMMENT '部署人ID',
  `duration_minutes` INT COMMENT '部署耗时(分钟)',
  `build_number` VARCHAR(50) COMMENT '构建号',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_environment` (`environment`),
  KEY `idx_deploy_date` (`deploy_date`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部署记录表';

-- 9. 团队成员表
CREATE TABLE IF NOT EXISTS `agile_team_member` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '成员ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `username` VARCHAR(100) NOT NULL COMMENT '用户名',
  `real_name` VARCHAR(100) COMMENT '真实姓名',
  `role` VARCHAR(50) NOT NULL COMMENT '角色：DEV/QA/PM/PO/SM/ARCHITECT',
  `department` VARCHAR(100) COMMENT '部门',
  `email` VARCHAR(200) COMMENT '邮箱',
  `status` VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/INACTIVE',
  `join_date` DATE COMMENT '入职日期',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_role` (`role`),
  KEY `idx_department` (`department`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='团队成员表';

-- 10. 质量指标表
CREATE TABLE IF NOT EXISTS `agile_quality_metrics` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '指标ID',
  `project_id` BIGINT NOT NULL COMMENT '项目ID',
  `metric_date` DATE NOT NULL COMMENT '指标日期',
  `code_coverage` DECIMAL(5,2) COMMENT '代码覆盖率',
  `unit_test_pass_rate` DECIMAL(5,2) COMMENT '单元测试通过率',
  `integration_test_pass_rate` DECIMAL(5,2) COMMENT '集成测试通过率',
  `defect_density` DECIMAL(10,4) COMMENT '缺陷密度(个/千行)',
  `defect_removal_efficiency` DECIMAL(5,2) COMMENT '缺陷移除效率',
  `mean_time_to_repair` DECIMAL(10,2) COMMENT '平均修复时间(小时)',
  `technical_debt_ratio` DECIMAL(5,2) COMMENT '技术债务比率',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_metric_date` (`metric_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='质量指标表';
