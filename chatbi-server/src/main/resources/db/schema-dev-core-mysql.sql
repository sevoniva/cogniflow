CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(20),
    avatar VARCHAR(500),
    nick_name VARCHAR(50),
    gender INT DEFAULT 0,
    dept_id BIGINT,
    status INT DEFAULT 1,
    is_admin INT DEFAULT 0,
    last_login_ip VARCHAR(50),
    last_login_time TIMESTAMP,
    pwd_reset_time TIMESTAMP,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    UNIQUE KEY uk_sys_user_username (username)
);

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    role_code VARCHAR(50) NOT NULL,
    role_name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    data_scope INT DEFAULT 1,
    status INT DEFAULT 1,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    UNIQUE KEY uk_sys_role_code (role_code)
);

CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    perm_code VARCHAR(100) NOT NULL,
    perm_name VARCHAR(100) NOT NULL,
    resource_type VARCHAR(20),
    resource_path VARCHAR(255),
    parent_id BIGINT DEFAULT 0,
    sort_order INT DEFAULT 0,
    icon VARCHAR(50),
    status INT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    UNIQUE KEY uk_sys_permission_code (perm_code)
);

CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    UNIQUE KEY uk_sys_user_role (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    UNIQUE KEY uk_sys_role_permission (role_id, permission_id)
);

CREATE TABLE IF NOT EXISTS data_source (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL,
    type VARCHAR(30) NOT NULL,
    host VARCHAR(200),
    port INT,
    `database` VARCHAR(100),
    service VARCHAR(100),
    url VARCHAR(500),
    username VARCHAR(100),
    password_encrypted VARCHAR(500),
    driver_class VARCHAR(200),
    config_json LONGTEXT,
    status INT DEFAULT 1,
    health_status INT DEFAULT 1,
    last_check_time TIMESTAMP,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    UNIQUE KEY uk_data_source_code (code)
);

CREATE TABLE IF NOT EXISTS ai_runtime_setting (
    id BIGINT PRIMARY KEY,
    enabled INT DEFAULT 0,
    default_provider VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ai_provider_setting (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    provider_key VARCHAR(50) NOT NULL,
    provider_name VARCHAR(100),
    api_url VARCHAR(255),
    api_key_encrypted LONGTEXT,
    model VARCHAR(100),
    temperature DOUBLE DEFAULT 0.7,
    max_tokens INT DEFAULT 2000,
    enabled INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ai_provider_setting_key (provider_key)
);

CREATE TABLE IF NOT EXISTS metrics (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    definition LONGTEXT,
    data_type VARCHAR(20),
    data_source_id BIGINT,
    table_name VARCHAR(100),
    field_name VARCHAR(100),
    aggregation VARCHAR(20),
    status VARCHAR(20) DEFAULT 'active',
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    UNIQUE KEY uk_metrics_code (code)
);

CREATE TABLE IF NOT EXISTS synonyms (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    standard_word VARCHAR(100) NOT NULL,
    aliases LONGTEXT,
    description VARCHAR(255),
    status INT DEFAULT 1,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    UNIQUE KEY uk_synonyms_standard_word (standard_word)
);

CREATE TABLE IF NOT EXISTS query_history (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(50),
    query_name VARCHAR(100),
    query_type VARCHAR(30),
    query_content LONGTEXT,
    datasource_id BIGINT,
    result_data LONGTEXT,
    duration BIGINT,
    status VARCHAR(20),
    error_msg LONGTEXT,
    is_favorite TINYINT(1) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS query_favorite (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    query_history_id BIGINT,
    favorite_name VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    UNIQUE KEY uk_query_favorite (user_id, query_history_id)
);

CREATE TABLE IF NOT EXISTS dashboard (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    layout_config LONGTEXT,
    charts_config LONGTEXT,
    cover_image VARCHAR(500),
    created_by BIGINT,
    created_by_name VARCHAR(100),
    is_public TINYINT(1) DEFAULT 1,
    status INT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS data_permission_rule (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    rule_name VARCHAR(100) NOT NULL,
    table_name VARCHAR(100) NOT NULL,
    field_name VARCHAR(100) NOT NULL,
    role_id BIGINT,
    user_id BIGINT,
    operator_symbol VARCHAR(20),
    rule_value VARCHAR(255),
    value_type VARCHAR(20),
    priority INT DEFAULT 1,
    status INT DEFAULT 1,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS data_masking_rule (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    rule_name VARCHAR(100) NOT NULL,
    table_name VARCHAR(100) NOT NULL,
    field_name VARCHAR(100) NOT NULL,
    role_id BIGINT,
    user_id BIGINT,
    mask_type VARCHAR(20),
    mask_pattern VARCHAR(50),
    priority INT DEFAULT 1,
    status INT DEFAULT 1,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS alert_rule (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    rule_name VARCHAR(100) NOT NULL,
    metric_id BIGINT,
    datasource_id BIGINT,
    alert_type VARCHAR(20),
    threshold_type VARCHAR(20),
    threshold_value DOUBLE,
    fluctuation_rate DOUBLE,
    compare_period VARCHAR(20),
    check_frequency VARCHAR(20),
    push_method VARCHAR(20),
    receiver VARCHAR(255),
    status INT DEFAULT 1,
    last_alert_time TIMESTAMP,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS subscription (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    type VARCHAR(20) NOT NULL,
    resource_id BIGINT NOT NULL,
    subscriber_id BIGINT,
    subscriber_name VARCHAR(100),
    push_method VARCHAR(20),
    receiver VARCHAR(255),
    frequency VARCHAR(20),
    push_time VARCHAR(10),
    push_day VARCHAR(20),
    status INT DEFAULT 1,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_push_time TIMESTAMP,
    push_count INT DEFAULT 0,
    deleted_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS share (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    type VARCHAR(20) NOT NULL,
    resource_id BIGINT NOT NULL,
    share_token VARCHAR(32) NOT NULL,
    share_method VARCHAR(20),
    validity_type VARCHAR(20),
    validity_days INT,
    expire_time TIMESTAMP,
    password VARCHAR(100),
    max_visits INT,
    current_visits INT DEFAULT 0,
    status INT DEFAULT 1,
    created_by BIGINT,
    creator_name VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    UNIQUE KEY uk_share_token (share_token)
);

CREATE TABLE IF NOT EXISTS embedded_access_log (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    share_id BIGINT NOT NULL,
    share_token VARCHAR(64) NOT NULL,
    origin VARCHAR(255),
    visitor_key VARCHAR(255),
    ip_address VARCHAR(64),
    user_agent VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    trace_id VARCHAR(64),
    user_id BIGINT,
    username VARCHAR(50),
    action VARCHAR(100),
    resource_type VARCHAR(50),
    resource_id BIGINT,
    request_method VARCHAR(10),
    request_uri VARCHAR(255),
    request_body LONGTEXT,
    response_status INT,
    response_body LONGTEXT,
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    execute_time_ms INT,
    result VARCHAR(20),
    error_message LONGTEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS prompt_version (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    version_tag VARCHAR(100) NOT NULL,
    template LONGTEXT NOT NULL,
    variables LONGTEXT,
    status VARCHAR(20) DEFAULT 'draft',
    gray_scale_percent INT DEFAULT 0,
    description VARCHAR(500),
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    UNIQUE KEY uk_prompt_version_tag (version_tag)
);

CREATE TABLE IF NOT EXISTS feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id VARCHAR(64),
    conversation_id VARCHAR(64),
    user_id BIGINT,
    question LONGTEXT,
    generated_sql LONGTEXT,
    rating INT DEFAULT 0,
    correct_sql LONGTEXT,
    comment LONGTEXT,
    exported BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_feedback_message_id (message_id),
    INDEX idx_feedback_conversation_id (conversation_id),
    INDEX idx_feedback_exported (exported)
);

-- Month 3 Week 1: Headless BI MetricCube 字段扩展
ALTER TABLE metrics ADD COLUMN IF NOT EXISTS cube_sql LONGTEXT;
ALTER TABLE metrics ADD COLUMN IF NOT EXISTS dimensions LONGTEXT;
ALTER TABLE metrics ADD COLUMN IF NOT EXISTS measures LONGTEXT;

CREATE TABLE IF NOT EXISTS usage_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    resource_type VARCHAR(50),
    action VARCHAR(100),
    cost INT DEFAULT 1,
    reference_id VARCHAR(64),
    metadata LONGTEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_usage_user_type (user_id, resource_type),
    INDEX idx_usage_created_at (created_at)
);
