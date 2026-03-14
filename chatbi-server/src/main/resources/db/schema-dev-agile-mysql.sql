CREATE TABLE IF NOT EXISTS agile_project (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    project_code VARCHAR(50) NOT NULL,
    project_name VARCHAR(200) NOT NULL,
    project_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    start_date DATE,
    end_date DATE,
    team_size INT,
    budget DECIMAL(15, 2),
    actual_cost DECIMAL(15, 2),
    progress INT DEFAULT 0,
    priority VARCHAR(20),
    owner_id BIGINT,
    department VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS agile_sprint (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    sprint_name VARCHAR(100) NOT NULL,
    sprint_goal LONGTEXT,
    status VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    planned_story_points INT,
    completed_story_points INT,
    velocity DECIMAL(10, 2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS agile_story (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    sprint_id BIGINT,
    story_code VARCHAR(50) NOT NULL,
    story_title VARCHAR(500) NOT NULL,
    story_type VARCHAR(20) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    story_points INT,
    assignee_id BIGINT,
    reporter_id BIGINT,
    estimated_hours DECIMAL(10, 2),
    actual_hours DECIMAL(10, 2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS agile_defect (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    defect_code VARCHAR(50) NOT NULL,
    defect_title VARCHAR(500) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    environment VARCHAR(50),
    found_in_version VARCHAR(50),
    fixed_in_version VARCHAR(50),
    assignee_id BIGINT,
    reporter_id BIGINT,
    found_date TIMESTAMP,
    resolved_date TIMESTAMP,
    resolution_time_hours DECIMAL(10, 2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS agile_test_case (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    case_code VARCHAR(50) NOT NULL,
    case_title VARCHAR(500) NOT NULL,
    case_type VARCHAR(20) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    automation_status VARCHAR(20),
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS agile_test_execution (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    test_case_id BIGINT NOT NULL,
    sprint_id BIGINT,
    execution_date DATE NOT NULL,
    result VARCHAR(20) NOT NULL,
    executor_id BIGINT,
    execution_time_minutes INT,
    defects_found INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS agile_code_commit (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    commit_hash VARCHAR(100) NOT NULL,
    author_id BIGINT NOT NULL,
    commit_message LONGTEXT,
    files_changed INT,
    lines_added INT,
    lines_deleted INT,
    commit_date TIMESTAMP NOT NULL,
    branch VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS agile_deployment (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    version VARCHAR(50) NOT NULL,
    environment VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    deploy_date TIMESTAMP NOT NULL,
    deployer_id BIGINT,
    duration_minutes INT,
    build_number VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS agile_team_member (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    username VARCHAR(100) NOT NULL,
    real_name VARCHAR(100),
    role VARCHAR(50) NOT NULL,
    department VARCHAR(100),
    email VARCHAR(200),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    join_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS agile_quality_metrics (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    metric_date DATE NOT NULL,
    code_coverage DECIMAL(5, 2),
    unit_test_pass_rate DECIMAL(5, 2),
    integration_test_pass_rate DECIMAL(5, 2),
    defect_density DECIMAL(10, 4),
    defect_removal_efficiency DECIMAL(5, 2),
    mean_time_to_repair DECIMAL(10, 2),
    technical_debt_ratio DECIMAL(5, 2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
