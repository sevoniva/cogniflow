CREATE TABLE IF NOT EXISTS sales_order (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(50) NOT NULL,
    customer_id BIGINT NOT NULL,
    customer_name VARCHAR(100),
    product_id BIGINT NOT NULL,
    product_name VARCHAR(200),
    product_category VARCHAR(50),
    region VARCHAR(50),
    sales_person_id BIGINT,
    sales_person_name VARCHAR(100),
    quantity INT,
    unit_price DECIMAL(15, 2),
    sales_amount DECIMAL(15, 2),
    cost_amount DECIMAL(15, 2),
    profit_amount DECIMAL(15, 2),
    discount_amount DECIMAL(15, 2) DEFAULT 0,
    order_date DATE NOT NULL,
    delivery_date DATE,
    status VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS customer (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    customer_no VARCHAR(50) NOT NULL,
    customer_name VARCHAR(100) NOT NULL,
    customer_type VARCHAR(20),
    industry VARCHAR(50),
    region VARCHAR(50),
    level VARCHAR(20),
    contact_person VARCHAR(100),
    contact_phone VARCHAR(50),
    email VARCHAR(100),
    first_purchase_date DATE,
    last_purchase_date DATE,
    total_purchase_amount DECIMAL(15, 2) DEFAULT 0,
    purchase_count INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_behavior (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    session_id VARCHAR(100),
    event_type VARCHAR(50) NOT NULL,
    page_url VARCHAR(500),
    page_title VARCHAR(200),
    referrer VARCHAR(500),
    device_type VARCHAR(20),
    os VARCHAR(50),
    browser VARCHAR(50),
    channel VARCHAR(50),
    duration INT,
    ip_address VARCHAR(50),
    city VARCHAR(50),
    province VARCHAR(50),
    event_time TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS app_user (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_no VARCHAR(50) NOT NULL,
    username VARCHAR(100),
    nickname VARCHAR(100),
    gender VARCHAR(10),
    age INT,
    city VARCHAR(50),
    province VARCHAR(50),
    register_channel VARCHAR(50),
    register_date DATE,
    last_login_date DATE,
    login_count INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS financial_record (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    record_no VARCHAR(50) NOT NULL,
    record_date DATE NOT NULL,
    category VARCHAR(50) NOT NULL,
    sub_category VARCHAR(50),
    amount DECIMAL(15, 2) NOT NULL,
    type VARCHAR(20) NOT NULL,
    department VARCHAR(50),
    project_id BIGINT,
    project_name VARCHAR(200),
    description LONGTEXT,
    status VARCHAR(20) DEFAULT 'CONFIRMED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS employee (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    employee_no VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    gender VARCHAR(10),
    birth_date DATE,
    age INT,
    department VARCHAR(50),
    position VARCHAR(50),
    level VARCHAR(20),
    hire_date DATE,
    resign_date DATE,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    salary DECIMAL(15, 2),
    performance_score DECIMAL(5, 2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS inventory (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(200),
    product_category VARCHAR(50),
    warehouse VARCHAR(50),
    quantity INT,
    unit_cost DECIMAL(15, 2),
    total_value DECIMAL(15, 2),
    safety_stock INT,
    last_in_date DATE,
    last_out_date DATE,
    status VARCHAR(20) DEFAULT 'NORMAL',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS service_ticket (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    ticket_no VARCHAR(50) NOT NULL,
    customer_id BIGINT NOT NULL,
    customer_name VARCHAR(100),
    ticket_type VARCHAR(50),
    priority VARCHAR(20),
    status VARCHAR(20),
    channel VARCHAR(50),
    subject VARCHAR(200),
    assignee_id BIGINT,
    assignee_name VARCHAR(100),
    created_date TIMESTAMP NOT NULL,
    first_response_date TIMESTAMP,
    resolved_date TIMESTAMP,
    response_time_minutes INT,
    resolution_time_hours DECIMAL(10, 2),
    satisfaction_score INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS project_delivery (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    project_code VARCHAR(50) NOT NULL,
    project_name VARCHAR(200) NOT NULL,
    owner_team VARCHAR(100),
    region VARCHAR(50),
    planned_delivery_date DATE NOT NULL,
    actual_delivery_date DATE,
    delivery_status VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS approval_record (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    approval_no VARCHAR(50) NOT NULL,
    department VARCHAR(50) NOT NULL,
    process_type VARCHAR(50),
    applicant_name VARCHAR(100),
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    duration_hours DECIMAL(10, 2),
    status VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
