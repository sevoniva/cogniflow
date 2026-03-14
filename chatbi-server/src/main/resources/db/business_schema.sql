-- ============================================
-- ChatBI 业务分析数据库表结构
-- 包含：销售、运营、财务、人力、供应链、客服 6大场景
-- ============================================

-- ============================================
-- 1. 销售分析场景
-- ============================================

-- 销售订单表
CREATE TABLE IF NOT EXISTS `sales_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no` VARCHAR(50) NOT NULL COMMENT '订单编号',
  `customer_id` BIGINT NOT NULL COMMENT '客户ID',
  `customer_name` VARCHAR(100) COMMENT '客户名称',
  `product_id` BIGINT NOT NULL COMMENT '产品ID',
  `product_name` VARCHAR(200) COMMENT '产品名称',
  `product_category` VARCHAR(50) COMMENT '产品类别',
  `region` VARCHAR(50) COMMENT '销售区域',
  `sales_person_id` BIGINT COMMENT '销售人员ID',
  `sales_person_name` VARCHAR(100) COMMENT '销售人员姓名',
  `quantity` INT COMMENT '数量',
  `unit_price` DECIMAL(15,2) COMMENT '单价',
  `sales_amount` DECIMAL(15,2) COMMENT '销售金额',
  `cost_amount` DECIMAL(15,2) COMMENT '成本金额',
  `profit_amount` DECIMAL(15,2) COMMENT '利润金额',
  `discount_amount` DECIMAL(15,2) DEFAULT 0 COMMENT '折扣金额',
  `order_date` DATE NOT NULL COMMENT '订单日期',
  `delivery_date` DATE COMMENT '交付日期',
  `status` VARCHAR(20) COMMENT '订单状态：PENDING/CONFIRMED/SHIPPED/DELIVERED/CANCELLED',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order_date` (`order_date`),
  KEY `idx_region` (`region`),
  KEY `idx_customer` (`customer_id`),
  KEY `idx_product` (`product_id`),
  KEY `idx_sales_person` (`sales_person_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='销���订单表';

-- 客户表
CREATE TABLE IF NOT EXISTS `customer` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '客户ID',
  `customer_no` VARCHAR(50) NOT NULL COMMENT '客户编号',
  `customer_name` VARCHAR(100) NOT NULL COMMENT '客户名称',
  `customer_type` VARCHAR(20) COMMENT '客户类型：ENTERPRISE/SMB/INDIVIDUAL',
  `industry` VARCHAR(50) COMMENT '所属行业',
  `region` VARCHAR(50) COMMENT '所在区域',
  `level` VARCHAR(20) COMMENT '客户等级：VIP/A/B/C',
  `contact_person` VARCHAR(100) COMMENT '联系人',
  `contact_phone` VARCHAR(50) COMMENT '联系电话',
  `email` VARCHAR(100) COMMENT '邮箱',
  `first_purchase_date` DATE COMMENT '首次购买日期',
  `last_purchase_date` DATE COMMENT '最近购买日期',
  `total_purchase_amount` DECIMAL(15,2) DEFAULT 0 COMMENT '累计购买金额',
  `purchase_count` INT DEFAULT 0 COMMENT '购买次数',
  `status` VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/INACTIVE',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_customer_no` (`customer_no`),
  KEY `idx_region` (`region`),
  KEY `idx_level` (`level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户表';

-- ============================================
-- 2. 运营分析场景
-- ============================================

-- 用户行为表
CREATE TABLE IF NOT EXISTS `user_behavior` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '行为ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `session_id` VARCHAR(100) COMMENT '会话ID',
  `event_type` VARCHAR(50) NOT NULL COMMENT '事件类型：PAGE_VIEW/CLICK/SEARCH/REGISTER/LOGIN/PURCHASE',
  `page_url` VARCHAR(500) COMMENT '页面URL',
  `page_title` VARCHAR(200) COMMENT '页面标题',
  `referrer` VARCHAR(500) COMMENT '来源页面',
  `device_type` VARCHAR(20) COMMENT '设备类型：PC/MOBILE/TABLET',
  `os` VARCHAR(50) COMMENT '操作系统',
  `browser` VARCHAR(50) COMMENT '��览器',
  `channel` VARCHAR(50) COMMENT '渠道：ORGANIC/PAID/SOCIAL/EMAIL/DIRECT',
  `duration` INT COMMENT '停留时长（秒）',
  `ip_address` VARCHAR(50) COMMENT 'IP地址',
  `city` VARCHAR(50) COMMENT '城市',
  `province` VARCHAR(50) COMMENT '省份',
  `event_time` DATETIME NOT NULL COMMENT '事件时间',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_event_type` (`event_type`),
  KEY `idx_event_time` (`event_time`),
  KEY `idx_channel` (`channel`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户行为表';

-- 用户表
CREATE TABLE IF NOT EXISTS `app_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `user_no` VARCHAR(50) NOT NULL COMMENT '用户编号',
  `username` VARCHAR(100) COMMENT '用户名',
  `nickname` VARCHAR(100) COMMENT '昵称',
  `gender` VARCHAR(10) COMMENT '性别：MALE/FEMALE/UNKNOWN',
  `age` INT COMMENT '年龄',
  `city` VARCHAR(50) COMMENT '城市',
  `province` VARCHAR(50) COMMENT '省份',
  `register_channel` VARCHAR(50) COMMENT '注册渠道',
  `register_date` DATE COMMENT '注册日期',
  `last_login_date` DATE COMMENT '最后登录日期',
  `login_count` INT DEFAULT 0 COMMENT '登录次数',
  `status` VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/INACTIVE/BANNED',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_no` (`user_no`),
  KEY `idx_register_date` (`register_date`),
  KEY `idx_city` (`city`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ============================================
-- 3. 财务分析场景
-- ============================================

-- 财务记录表
CREATE TABLE IF NOT EXISTS `financial_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `record_no` VARCHAR(50) NOT NULL COMMENT '记录编号',
  `record_date` DATE NOT NULL COMMENT '记录日期',
  `category` VARCHAR(50) NOT NULL COMMENT '类别：REVENUE/COST/EXPENSE/ASSET/LIABILITY',
  `sub_category` VARCHAR(50) COMMENT '子类别',
  `amount` DECIMAL(15,2) NOT NULL COMMENT '金额',
  `type` VARCHAR(20) NOT NULL COMMENT '类型：INCOME/EXPENSE',
  `department` VARCHAR(50) COMMENT '部门',
  `project_id` BIGINT COMMENT '项目ID',
  `project_name` VARCHAR(200) COMMENT '项目名称',
  `description` TEXT COMMENT '描述',
  `status` VARCHAR(20) DEFAULT 'CONFIRMED' COMMENT '状态：PENDING/CONFIRMED/CANCELLED',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_record_no` (`record_no`),
  KEY `idx_record_date` (`record_date`),
  KEY `idx_category` (`category`),
  KEY `idx_department` (`department`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='财务记录表';

-- ============================================
-- 4. 人力资源场景
-- ============================================

-- 员工表
CREATE TABLE IF NOT EXISTS `employee` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '员工ID',
  `employee_no` VARCHAR(50) NOT NULL COMMENT '员工编号',
  `name` VARCHAR(100) NOT NULL COMMENT '姓名',
  `gender` VARCHAR(10) COMMENT '性别',
  `birth_date` DATE COMMENT '出生日期',
  `age` INT COMMENT '年龄',
  `department` VARCHAR(50) COMMENT '部门',
  `position` VARCHAR(50) COMMENT '职位',
  `level` VARCHAR(20) COMMENT '职级',
  `hire_date` DATE COMMENT '入职日期',
  `resign_date` DATE COMMENT '离职日期',
  `status` VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/RESIGNED',
  `salary` DECIMAL(15,2) COMMENT '薪资',
  `performance_score` DECIMAL(5,2) COMMENT '绩效分数',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_employee_no` (`employee_no`),
  KEY `idx_department` (`department`),
  KEY `idx_status` (`status`),
  KEY `idx_hire_date` (`hire_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工表';

-- ============================================
-- 5. 供应链场景
-- ============================================

-- 库存表
CREATE TABLE IF NOT EXISTS `inventory` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '库存ID',
  `product_id` BIGINT NOT NULL COMMENT '产品ID',
  `product_name` VARCHAR(200) COMMENT '产品名称',
  `product_category` VARCHAR(50) COMMENT '产品类别',
  `warehouse` VARCHAR(50) COMMENT '仓库',
  `quantity` INT COMMENT '库存数量',
  `unit_cost` DECIMAL(15,2) COMMENT '单位成本',
  `total_value` DECIMAL(15,2) COMMENT '库存总值',
  `safety_stock` INT COMMENT '安全库存',
  `last_in_date` DATE COMMENT '最后入库日期',
  `last_out_date` DATE COMMENT '最后出库日期',
  `status` VARCHAR(20) DEFAULT 'NORMAL' COMMENT '状态：NORMAL/LOW/OUT_OF_STOCK',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_product` (`product_id`),
  KEY `idx_warehouse` (`warehouse`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存表';

-- 采购订单表
CREATE TABLE IF NOT EXISTS `purchase_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '采购订单ID',
  `order_no` VARCHAR(50) NOT NULL COMMENT '订单编号',
  `supplier_id` BIGINT NOT NULL COMMENT '供应商ID',
  `supplier_name` VARCHAR(100) COMMENT '供应商名称',
  `product_id` BIGINT NOT NULL COMMENT '产品ID',
  `product_name` VARCHAR(200) COMMENT '产品名称',
  `quantity` INT COMMENT '数量',
  `unit_price` DECIMAL(15,2) COMMENT '单价',
  `total_amount` DECIMAL(15,2) COMMENT '总金额',
  `order_date` DATE NOT NULL COMMENT '订单日期',
  `expected_delivery_date` DATE COMMENT '预计交付日期',
  `actual_delivery_date` DATE COMMENT '实际交付日期',
  `status` VARCHAR(20) COMMENT '状态：PENDING/CONFIRMED/SHIPPED/RECEIVED/CANCELLED',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_order_date` (`order_date`),
  KEY `idx_supplier` (`supplier_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购订单表';

-- ============================================
-- 6. 客户服务场景
-- ============================================

-- 工单表
CREATE TABLE IF NOT EXISTS `service_ticket` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '工单ID',
  `ticket_no` VARCHAR(50) NOT NULL COMMENT '工单编号',
  `customer_id` BIGINT NOT NULL COMMENT '客户ID',
  `customer_name` VARCHAR(100) COMMENT '客户名称',
  `ticket_type` VARCHAR(50) COMMENT '工单类型：COMPLAINT/INQUIRY/SUGGESTION/TECHNICAL',
  `priority` VARCHAR(20) COMMENT '优先级：LOW/MEDIUM/HIGH/URGENT',
  `status` VARCHAR(20) COMMENT '状态：OPEN/IN_PROGRESS/RESOLVED/CLOSED',
  `channel` VARCHAR(50) COMMENT '渠道：PHONE/EMAIL/CHAT/APP',
  `subject` VARCHAR(200) COMMENT '主题',
  `description` TEXT COMMENT '描述',
  `assignee_id` BIGINT COMMENT '��理人ID',
  `assignee_name` VARCHAR(100) COMMENT '处理人姓名',
  `created_date` DATETIME NOT NULL COMMENT '创建时间',
  `first_response_date` DATETIME COMMENT '首次响应时间',
  `resolved_date` DATETIME COMMENT '解决时间',
  `closed_date` DATETIME COMMENT '关闭时间',
  `response_time_minutes` INT COMMENT '响应时长（分钟）',
  `resolution_time_hours` DECIMAL(10,2) COMMENT '解决时长（小时）',
  `satisfaction_score` INT COMMENT '满意度评分（1-5）',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ticket_no` (`ticket_no`),
  KEY `idx_created_date` (`created_date`),
  KEY `idx_status` (`status`),
  KEY `idx_customer` (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客服工单表';
