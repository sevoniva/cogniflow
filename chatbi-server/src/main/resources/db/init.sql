-- ChatBI 企业版数据库初始化脚本
-- MySQL 8.0+

CREATE DATABASE IF NOT EXISTS chatbi
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE chatbi;

-- 禁用外键检查
SET FOREIGN_KEY_CHECKS = 0;

-- ==================== 系统管理模块 ====================

-- 用户表
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码',
    email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    avatar VARCHAR(500) DEFAULT NULL COMMENT '头像 URL',
    nick_name VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    gender TINYINT DEFAULT 0 COMMENT '性别 (0 未知 1 男 2 女)',
    dept_id BIGINT DEFAULT NULL COMMENT '部门 ID',
    status TINYINT DEFAULT 1 COMMENT '状态 (0 禁用 1 正常)',
    is_admin TINYINT DEFAULT 0 COMMENT '是否管理员',
    last_login_ip VARCHAR(50) DEFAULT NULL COMMENT '最后登录 IP',
    last_login_time DATETIME DEFAULT NULL COMMENT '最后登录时间',
    pwd_reset_time DATETIME DEFAULT NULL COMMENT '密码最后修改时间',
    created_by BIGINT DEFAULT NULL COMMENT '创建人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    KEY idx_dept (dept_id),
    KEY idx_status (status),
    KEY idx_deleted (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- 角色表
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    role_code VARCHAR(50) NOT NULL COMMENT '角色编码',
    role_name VARCHAR(100) NOT NULL COMMENT '角色名称',
    description VARCHAR(255) DEFAULT NULL COMMENT '描述',
    data_scope TINYINT DEFAULT 1 COMMENT '数据范围',
    status TINYINT DEFAULT 1 COMMENT '状态',
    sort_order INT DEFAULT 0 COMMENT '排序',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统角色表';

-- 权限表
DROP TABLE IF EXISTS sys_permission;
CREATE TABLE sys_permission (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    perm_code VARCHAR(100) NOT NULL COMMENT '权限编码',
    perm_name VARCHAR(100) NOT NULL COMMENT '权限名称',
    resource_type VARCHAR(20) DEFAULT NULL COMMENT '资源类型 (MENU/BUTTON/API)',
    resource_path VARCHAR(255) DEFAULT NULL COMMENT '资源路径',
    parent_id BIGINT DEFAULT 0 COMMENT '父 ID',
    sort_order INT DEFAULT 0 COMMENT '排序',
    icon VARCHAR(50) DEFAULT NULL COMMENT '图标',
    status TINYINT DEFAULT 1 COMMENT '状态',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_perm_code (perm_code),
    KEY idx_parent (parent_id),
    KEY idx_type (resource_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统权限表';

-- 用户角色关联表
DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role (
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    role_id BIGINT NOT NULL COMMENT '角色 ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (user_id, role_id),
    KEY idx_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- 角色权限关联表
DROP TABLE IF EXISTS sys_role_permission;
CREATE TABLE sys_role_permission (
    role_id BIGINT NOT NULL COMMENT '角色 ID',
    permission_id BIGINT NOT NULL COMMENT '权限 ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (role_id, permission_id),
    KEY idx_permission (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

-- 部门表
DROP TABLE IF EXISTS sys_dept;
CREATE TABLE sys_dept (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    dept_name VARCHAR(100) NOT NULL COMMENT '部门名称',
    dept_code VARCHAR(50) NOT NULL COMMENT '部门编码',
    parent_id BIGINT DEFAULT 0 COMMENT '父部门 ID',
    sort_order INT DEFAULT 0 COMMENT '排序',
    leader_id BIGINT DEFAULT NULL COMMENT '负责人 ID',
    phone VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
    email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    status TINYINT DEFAULT 1 COMMENT '状态',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_dept_code (dept_code),
    KEY idx_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统部门表';

-- ==================== 审计日志模块 ====================

DROP TABLE IF EXISTS audit_log;
CREATE TABLE audit_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    trace_id VARCHAR(64) DEFAULT NULL COMMENT '链路 ID',
    user_id BIGINT DEFAULT NULL COMMENT '用户 ID',
    username VARCHAR(50) DEFAULT NULL COMMENT '用户名',
    action VARCHAR(50) DEFAULT NULL COMMENT '操作类型',
    resource_type VARCHAR(50) DEFAULT NULL COMMENT '资源类型',
    resource_id BIGINT DEFAULT NULL COMMENT '资源 ID',
    request_method VARCHAR(10) DEFAULT NULL COMMENT '请求方法',
    request_uri VARCHAR(255) DEFAULT NULL COMMENT '请求 URI',
    request_body TEXT DEFAULT NULL COMMENT '请求体',
    response_status INT DEFAULT NULL COMMENT '响应状态',
    response_body TEXT DEFAULT NULL COMMENT '响应体',
    ip_address VARCHAR(50) DEFAULT NULL COMMENT 'IP 地址',
    user_agent VARCHAR(500) DEFAULT NULL COMMENT '用户代理',
    execute_time_ms INT DEFAULT NULL COMMENT '执行耗时',
    result VARCHAR(20) DEFAULT NULL COMMENT '结果',
    error_message TEXT DEFAULT NULL COMMENT '错误信息',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_user (user_id),
    KEY idx_action (action),
    KEY idx_time (created_at),
    KEY idx_trace (trace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审计日志表';

-- ==================== 数据源管理模块 ====================

DROP TABLE IF EXISTS data_source;
CREATE TABLE data_source (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    name VARCHAR(100) NOT NULL COMMENT '数据源名称',
    code VARCHAR(50) NOT NULL COMMENT '数据源编码',
    type VARCHAR(30) NOT NULL COMMENT '类型 (MYSQL/ORACLE/OB_ORACLE/OB_MYSQL/POSTGRESQL/CLICKHOUSE/SQLSERVER/HIVE)',
    host VARCHAR(200) DEFAULT NULL COMMENT '主机地址',
    port INT DEFAULT NULL COMMENT '端口',
    `database` VARCHAR(100) DEFAULT NULL COMMENT '数据库名',
    service VARCHAR(100) DEFAULT NULL COMMENT '服务名 (Oracle)',
    url VARCHAR(500) DEFAULT NULL COMMENT '连接 URL',
    username VARCHAR(100) DEFAULT NULL COMMENT '用户名',
    password_encrypted VARCHAR(500) DEFAULT NULL COMMENT '加密密码',
    driver_class VARCHAR(200) DEFAULT NULL COMMENT '驱动类',
    config_json JSON DEFAULT NULL COMMENT '扩展配置',
    status TINYINT DEFAULT 1 COMMENT '状态',
    health_status TINYINT DEFAULT 0 COMMENT '健康状态',
    last_check_time DATETIME DEFAULT NULL COMMENT '最后检查时间',
    created_by BIGINT DEFAULT NULL COMMENT '创建人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ds_code (code),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据源表';

-- ==================== 指标管理模块 ====================

DROP TABLE IF EXISTS metrics;
CREATE TABLE metrics (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    code VARCHAR(50) NOT NULL COMMENT '指标编码',
    name VARCHAR(100) NOT NULL COMMENT '指标名称',
    description TEXT DEFAULT NULL COMMENT '描述',
    definition TEXT DEFAULT NULL COMMENT '业务定义',
    data_source_id BIGINT DEFAULT NULL COMMENT '数据源 ID',
    table_name VARCHAR(100) DEFAULT NULL COMMENT '表名',
    field_name VARCHAR(100) DEFAULT NULL COMMENT '字段名',
    aggregation VARCHAR(20) DEFAULT NULL COMMENT '聚合方式',
    unit VARCHAR(20) DEFAULT NULL COMMENT '单位',
    `precision` INT DEFAULT 2 COMMENT '精度',
    category_id BIGINT DEFAULT NULL COMMENT '分类 ID',
    parent_id BIGINT DEFAULT NULL COMMENT '父指标 ID',
    status TINYINT DEFAULT 1 COMMENT '状态',
    version INT DEFAULT 1 COMMENT '版本号',
    created_by BIGINT DEFAULT NULL COMMENT '创建人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code),
    KEY idx_category (category_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='指标表';

-- 指标分类表
DROP TABLE IF EXISTS metric_category;
CREATE TABLE metric_category (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    name VARCHAR(100) NOT NULL COMMENT '分类名称',
    code VARCHAR(50) NOT NULL COMMENT '分类编码',
    parent_id BIGINT DEFAULT 0 COMMENT '父分类 ID',
    sort_order INT DEFAULT 0 COMMENT '排序',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_category_code (code),
    KEY idx_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='指标分类表';

-- ==================== 语义管理模块 ====================

DROP TABLE IF EXISTS synonym;
CREATE TABLE synonym (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    standard_word VARCHAR(100) NOT NULL COMMENT '标准词',
    metric_id BIGINT NOT NULL COMMENT '关联指标 ID',
    status TINYINT DEFAULT 1 COMMENT '状态',
    created_by BIGINT DEFAULT NULL COMMENT '创建人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (id),
    KEY idx_metric (metric_id),
    KEY idx_standard (standard_word)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='同义词表';

DROP TABLE IF EXISTS synonym_alias;
CREATE TABLE synonym_alias (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    synonym_id BIGINT NOT NULL COMMENT '同义词 ID',
    alias VARCHAR(100) NOT NULL COMMENT '别名',
    priority INT DEFAULT 0 COMMENT '优先级',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_synonym (synonym_id),
    KEY idx_alias (alias)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='同义词别名表';

-- ==================== 分享协作模块 ====================

DROP TABLE IF EXISTS share;
CREATE TABLE share (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    title VARCHAR(200) NOT NULL COMMENT '分享标题',
    type VARCHAR(20) NOT NULL COMMENT '分享类型 (DASHBOARD/REPORT/CHART)',
    resource_id BIGINT NOT NULL COMMENT '资源 ID',
    share_token VARCHAR(32) NOT NULL COMMENT '分享链接 Token',
    share_method VARCHAR(20) DEFAULT 'LINK' COMMENT '分享方式 (LINK/EMAIL)',
    validity_type VARCHAR(20) DEFAULT 'DAYS' COMMENT '有效期类型',
    validity_days INT DEFAULT NULL COMMENT '有效天数',
    expire_time DATETIME DEFAULT NULL COMMENT '过期时间',
    password VARCHAR(100) DEFAULT NULL COMMENT '密码保护',
    max_visits INT DEFAULT NULL COMMENT '访问次数限制',
    current_visits INT DEFAULT 0 COMMENT '当前访问次数',
    status TINYINT DEFAULT 1 COMMENT '状态',
    created_by BIGINT DEFAULT NULL COMMENT '创建人',
    creator_name VARCHAR(50) DEFAULT NULL COMMENT '创建人姓名',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_share_token (share_token),
    KEY idx_resource (type, resource_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分享表';

-- 订阅表
DROP TABLE IF EXISTS subscription;
CREATE TABLE subscription (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    title VARCHAR(200) NOT NULL COMMENT '订阅标题',
    type VARCHAR(20) NOT NULL COMMENT '订阅类型 (DASHBOARD/REPORT/METRIC)',
    resource_id BIGINT NOT NULL COMMENT '资源 ID',
    subscriber_id BIGINT NOT NULL COMMENT '订阅人 ID',
    subscriber_name VARCHAR(50) DEFAULT NULL COMMENT '订阅人姓名',
    push_method VARCHAR(20) DEFAULT 'EMAIL' COMMENT '推送方式 (EMAIL/DINGTALK/WECHAT)',
    frequency VARCHAR(20) DEFAULT 'DAILY' COMMENT '推送频率',
    push_time VARCHAR(10) DEFAULT NULL COMMENT '推送时间',
    push_day VARCHAR(20) DEFAULT NULL COMMENT '推送日期',
    status TINYINT DEFAULT 1 COMMENT '状态',
    created_by BIGINT DEFAULT NULL COMMENT '创建人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    last_push_time DATETIME DEFAULT NULL COMMENT '上次推送时间',
    push_count INT DEFAULT 0 COMMENT '推送次数',
    deleted_at DATETIME DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (id),
    KEY idx_subscriber (subscriber_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订阅表';

-- 恢复外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- ==================== 仪表板模块 ====================

-- 仪表板表
DROP TABLE IF EXISTS dashboard;
CREATE TABLE dashboard (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    name VARCHAR(200) NOT NULL COMMENT '仪表板名称',
    description VARCHAR(500) DEFAULT NULL COMMENT '仪表板描述',
    layout_config TEXT COMMENT '布局配置 (JSON)',
    charts_config LONGTEXT COMMENT '图表配置列表 (JSON)',
    cover_image VARCHAR(500) DEFAULT NULL COMMENT '封面图',
    created_by BIGINT DEFAULT NULL COMMENT '创建人',
    created_by_name VARCHAR(50) DEFAULT NULL COMMENT '创建人姓名',
    is_public TINYINT DEFAULT 0 COMMENT '是否公开',
    status TINYINT DEFAULT 0 COMMENT '状态 (0-草稿 1-发布)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (id),
    KEY idx_created_by (created_by),
    KEY idx_status (status),
    KEY idx_public (is_public)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='仪表板表';

-- ==================== 查询历史与收藏模块 ====================

-- 查询历史表
DROP TABLE IF EXISTS query_history;
CREATE TABLE query_history (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    username VARCHAR(50) DEFAULT NULL COMMENT '用户名',
    query_name VARCHAR(200) DEFAULT NULL COMMENT '查询名称',
    query_type VARCHAR(20) DEFAULT NULL COMMENT '查询类型 (SQL/NATURAL_LANGUAGE/VISUAL)',
    query_content TEXT COMMENT '查询内容 (SQL 语句或自然语言)',
    datasource_id BIGINT DEFAULT NULL COMMENT '数据源 ID',
    result_data LONGTEXT COMMENT '结果数据',
    duration BIGINT DEFAULT NULL COMMENT '执行时长 (毫秒)',
    status VARCHAR(20) DEFAULT NULL COMMENT '状态 (SUCCESS/FAILED)',
    error_msg TEXT COMMENT '错误信息',
    is_favorite TINYINT DEFAULT 0 COMMENT '是否收藏',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (id),
    KEY idx_user (user_id),
    KEY idx_favorite (is_favorite),
    KEY idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='查询历史表';

-- 查询收藏表
DROP TABLE IF EXISTS query_favorite;
CREATE TABLE query_favorite (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    query_history_id BIGINT NOT NULL COMMENT '查询历史 ID',
    favorite_name VARCHAR(200) DEFAULT NULL COMMENT '收藏名称',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_query (user_id, query_history_id),
    KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='查询收藏表';

-- ==================== 告警模块 ====================

-- 告警规则表
DROP TABLE IF EXISTS alert_rule;
CREATE TABLE alert_rule (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    rule_name VARCHAR(200) NOT NULL COMMENT '规则名称',
    metric_id BIGINT DEFAULT NULL COMMENT '指标 ID',
    datasource_id BIGINT DEFAULT NULL COMMENT '数据源 ID',
    alert_type VARCHAR(20) DEFAULT NULL COMMENT '告警类型 (THRESHOLD/FLUCTUATION/ANOMALY)',
    threshold_type VARCHAR(10) DEFAULT NULL COMMENT '阈值类型 (> / < / >= / <= / =)',
    threshold_value DOUBLE DEFAULT NULL COMMENT '阈值',
    fluctuation_rate DOUBLE DEFAULT NULL COMMENT '波动率 (%)',
    compare_period VARCHAR(20) DEFAULT NULL COMMENT '比较周期 (日/周/月)',
    check_frequency VARCHAR(20) DEFAULT NULL COMMENT '检查频率 (每小时/每天/每周)',
    push_method VARCHAR(20) DEFAULT NULL COMMENT '推送方式 (EMAIL/DINGTALK/WECHAT)',
    receiver VARCHAR(500) DEFAULT NULL COMMENT '接收人',
    status TINYINT DEFAULT 1 COMMENT '状态',
    last_alert_time DATETIME DEFAULT NULL COMMENT '最后告警时间',
    created_by BIGINT DEFAULT NULL COMMENT '创建人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (id),
    KEY idx_metric (metric_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='告警规则表';

-- ==================== 数据权限与脱敏模块 ====================

-- 数据权限规则表 (行级权限)
DROP TABLE IF EXISTS data_permission_rule;
CREATE TABLE data_permission_rule (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    rule_name VARCHAR(200) NOT NULL COMMENT '规则名称',
    table_name VARCHAR(100) NOT NULL COMMENT '适用表名',
    field_name VARCHAR(100) NOT NULL COMMENT '适用字段名',
    role_id BIGINT DEFAULT NULL COMMENT '角色 ID',
    user_id BIGINT DEFAULT NULL COMMENT '用户 ID',
    operator VARCHAR(10) DEFAULT NULL COMMENT '操作符 (= / != / > / < / IN / LIKE 等)',
    value VARCHAR(500) DEFAULT NULL COMMENT '值',
    value_type VARCHAR(20) DEFAULT NULL COMMENT '值类型 (CONSTANT/FIELD/USER_ATTR)',
    priority INT DEFAULT 0 COMMENT '优先级 (数字越小优先级越高)',
    status TINYINT DEFAULT 1 COMMENT '状态',
    created_by BIGINT DEFAULT NULL COMMENT '创建人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (id),
    KEY idx_table (table_name),
    KEY idx_role (role_id),
    KEY idx_user (user_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据权限规则表';

-- 数据脱敏规则表
DROP TABLE IF EXISTS data_masking_rule;
CREATE TABLE data_masking_rule (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    rule_name VARCHAR(200) NOT NULL COMMENT '规则名称',
    table_name VARCHAR(100) NOT NULL COMMENT '适用表名',
    field_name VARCHAR(100) NOT NULL COMMENT '适用字段名',
    role_id BIGINT DEFAULT NULL COMMENT '角色 ID',
    user_id BIGINT DEFAULT NULL COMMENT '用户 ID',
    mask_type VARCHAR(20) DEFAULT NULL COMMENT '脱敏类型 (HIDE/PARTIAL/HASH/ENCRYPT)',
    mask_pattern VARCHAR(100) DEFAULT NULL COMMENT '脱敏规则',
    priority INT DEFAULT 0 COMMENT '优先级',
    status TINYINT DEFAULT 1 COMMENT '状态',
    created_by BIGINT DEFAULT NULL COMMENT '创建人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (id),
    KEY idx_table (table_name),
    KEY idx_role (role_id),
    KEY idx_user (user_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据脱敏规则表';

-- 恢复外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- ==================== 初始化数据 ====================

-- 插入默认管理员用户 (密码：Admin@123)
INSERT INTO sys_user (username, password, nick_name, status, is_admin, email)
VALUES ('admin', '$2a$10$XQMLvF2ZlqfXKj3V.jPTO.qLQVlPmXhJv3lLbV8fQXjGvKZlYqZ1G', '超级管理员', 1, 1, 'admin@company.com');

-- 插入默认角色
INSERT INTO sys_role (role_code, role_name, description, status) VALUES
('SUPER_ADMIN', '超级管理员', '系统超级管理员角色', 1),
('ADMIN', '管理员', '系统管理员角色', 1),
('USER', '普通用户', '普通用户角色', 1);

-- 插入默认权限
INSERT INTO sys_permission (perm_code, perm_name, resource_type, resource_path, status) VALUES
-- 系统管理
('system', '系统管理', 'MENU', '/system', 1),
('system:user', '用户管理', 'MENU', '/system/user', 1),
('system:user:query', '查询用户', 'API', '/api/system/users', 1),
('system:user:add', '新增用户', 'API', '/api/system/users', 1),
('system:user:update', '修改用户', 'API', '/api/system/users/*', 1),
('system:user:delete', '删除用户', 'API', '/api/system/users/*', 1),
('system:role', '角色管理', 'MENU', '/system/role', 1),
('system:role:query', '查询角色', 'API', '/api/system/roles', 1),
('system:role:add', '新增角色', 'API', '/api/system/roles', 1),
('system:role:update', '修改角色', 'API', '/api/system/roles/*', 1),
('system:role:delete', '删除角色', 'API', '/api/system/roles/*', 1),
-- 指标管理
('metric', '指标管理', 'MENU', '/metric', 1),
('metric:query', '查询指标', 'API', '/api/metrics', 1),
('metric:add', '新增指标', 'API', '/api/metrics', 1),
('metric:update', '修改指标', 'API', '/api/metrics/*', 1),
('metric:delete', '删除指标', 'API', '/api/metrics/*', 1),
-- 数据源管理
('datasource', '数据源管理', 'MENU', '/datasource', 1),
('datasource:query', '查询数据源', 'API', '/api/datasources', 1),
('datasource:add', '新增数据源', 'API', '/api/datasources', 1),
('datasource:update', '修改数据源', 'API', '/api/datasources/*', 1),
('datasource:delete', '删除数据源', 'API', '/api/datasources/*', 1),
-- 审计日志
('audit', '审计日志', 'MENU', '/audit', 1),
('audit:query', '查询审计日志', 'API', '/api/audit/logs', 1);

-- 分配权限给超级管理员角色
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission;

-- 分配权限给管理员角色
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 2, id FROM sys_permission WHERE perm_code NOT LIKE '%delete%';

-- 分配基础权限给普通用户
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 3, id FROM sys_permission WHERE perm_code IN (
    'metric:query', 'datasource:query', 'audit:query'
);

-- 关联管理员用户到超级管理员角色
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);

-- 插入默认部门
INSERT INTO sys_dept (dept_name, dept_code, status) VALUES
('总公司', 'HEAD', 1),
('技术部', 'TECH', 1),
('销售部', 'SALES', 1),
('运营部', 'OPERATION', 1);

-- 插入默认指标分类
INSERT INTO metric_category (name, code, parent_id, sort_order) VALUES
('销售指标', 'SALES', 0, 1),
('财务指标', 'FINANCE', 0, 2),
('运营指标', 'OPERATION', 0, 3),
('客户指标', 'CUSTOMER', 0, 4);
