# Language | 语言

[English](./DATA_DICTIONARY.md) | [中文](./DATA_DICTIONARY.zh-CN.md)

# ChatBI 数据字典

## 1. 系统管理模块

### 1.1 用户表 (sys_user)

| 字段 | 类型 | 长度 | 必填 | 说明 |
|------|------|------|------|------|
| id | BIGINT | - | Y | 主键 ID |
| username | VARCHAR | 50 | Y | 用户名（唯一） |
| password | VARCHAR | 255 | Y | 密码（BCrypt 加密） |
| email | VARCHAR | 100 | N | 邮箱 |
| phone | VARCHAR | 20 | N | 手机号 |
| avatar | VARCHAR | 500 | N | 头像 URL |
| nick_name | VARCHAR | 50 | N | 昵称 |
| gender | TINYINT | - | N | 性别 (0 女 1 男 2 未知) |
| dept_id | BIGINT | - | N | 部门 ID |
| status | TINYINT | - | N | 状态 (0 禁用 1 正常) |
| is_admin | TINYINT | - | N | 是否管理员 (0 否 1 是) |
| last_login_ip | VARCHAR | 50 | N | 最后登录 IP |
| last_login_time | DATETIME | - | N | 最后登录时间 |
| pwd_reset_time | DATETIME | - | N | 密码最后修改时间 |
| created_by | BIGINT | - | N | 创建人 |
| created_at | DATETIME | - | N | 创建时间 |
| updated_at | DATETIME | - | N | 更新时间 |
| deleted_at | DATETIME | - | N | 删除时间（软删除） |

### 1.2 角色表 (sys_role)

| 字段 | 类型 | 长度 | 必填 | 说明 |
|------|------|------|------|------|
| id | BIGINT | - | Y | 主键 ID |
| role_code | VARCHAR | 50 | Y | 角色编码（唯一） |
| role_name | VARCHAR | 100 | Y | 角色名称 |
| description | VARCHAR | 255 | N | 描述 |
| data_scope | TINYINT | - | N | 数据范围 (1 全部 2 本部门 3 本人) |
| status | TINYINT | - | N | 状态 (0 禁用 1 正常) |
| sort_order | INT | - | N | 排序 |
| created_at | DATETIME | - | N | 创建时间 |
| updated_at | DATETIME | - | N | 更新时间 |

### 1.3 权限表 (sys_permission)

| 字段 | 类型 | 长度 | 必填 | 说明 |
|------|------|------|------|------|
| id | BIGINT | - | Y | 主键 ID |
| perm_code | VARCHAR | 100 | Y | 权限编码（唯一） |
| perm_name | VARCHAR | 100 | Y | 权限名称 |
| resource_type | VARCHAR | 20 | Y | 资源类型 (MENU/BUTTON/API) |
| resource_path | VARCHAR | 255 | N | 资源路径 |
| parent_id | BIGINT | - | N | 父 ID |
| sort_order | INT | - | N | 排序 |
| icon | VARCHAR | 50 | N | 图标 |
| status | TINYINT | - | N | 状态 |
| created_at | DATETIME | - | N | 创建时间 |

### 1.4 用户角色关联表 (sys_user_role)

| 字段 | 类型 | 长度 | 必填 | 说明 |
|------|------|------|------|------|
| user_id | BIGINT | - | Y | 用户 ID |
| role_id | BIGINT | - | Y | 角色 ID |
| created_at | DATETIME | - | N | 创建时间 |

### 1.5 角色权限关联表 (sys_role_permission)

| 字段 | 类型 | 长度 | 必填 | 说明 |
|------|------|------|------|------|
| role_id | BIGINT | - | Y | 角色 ID |
| permission_id | BIGINT | - | Y | 权限 ID |
| created_at | DATETIME | - | N | 创建时间 |

### 1.6 部门表 (sys_dept)

| 字段 | 类型 | 长度 | 必填 | 说明 |
|------|------|------|------|------|
| id | BIGINT | - | Y | 主键 ID |
| dept_name | VARCHAR | 100 | Y | 部门名称 |
| dept_code | VARCHAR | 50 | Y | 部门编码 |
| parent_id | BIGINT | - | N | 父部门 ID |
| sort_order | INT | - | N | 排序 |
| leader_id | BIGINT | - | N | 负责人 ID |
| phone | VARCHAR | 20 | N | 联系电话 |
| email | VARCHAR | 100 | N | 邮箱 |
| status | TINYINT | - | N | 状态 |
| created_at | DATETIME | - | N | 创建时间 |
| updated_at | DATETIME | - | N | 更新时间 |

---

## 2. 数据源管理模块

### 2.1 数据源表 (data_source)

| 字段 | 类型 | 长度 | 必填 | 说明 |
|------|------|------|------|------|
| id | BIGINT | - | Y | 主键 ID |
| name | VARCHAR | 100 | Y | 数据源名称 |
| code | VARCHAR | 50 | Y | 数据源编码（唯一） |
| type | VARCHAR | 20 | Y | 类型 (MYSQL/ORACLE/HIVE/PG) |
| url | VARCHAR | 500 | Y | 连接 URL |
| username | VARCHAR | 100 | N | 用户名 |
| password_encrypted | VARCHAR | 500 | N | 加密密码 |
| driver_class | VARCHAR | 100 | N | 驱动类 |
| config_json | JSON | - | N | 扩展配置 |
| status | TINYINT | - | N | 状态 (0 禁用 1 正常) |
| health_status | TINYINT | - | N | 健康状态 (0 异常 1 正常) |
| last_check_time | DATETIME | - | N | 最后检查时间 |
| created_by | BIGINT | - | N | 创建人 |
| created_at | DATETIME | - | N | 创建时间 |
| updated_at | DATETIME | - | N | 更新时间 |

---

## 3. 指标管理模块

### 3.1 指标表 (metric)

| 字段 | 类型 | 长度 | 必填 | 说明 |
|------|------|------|------|------|
| id | BIGINT | - | Y | 主键 ID |
| code | VARCHAR | 50 | Y | 指标编码（唯一） |
| name | VARCHAR | 100 | Y | 指标名称 |
| description | TEXT | - | N | 描述 |
| definition | TEXT | - | N | 业务定义 |
| data_source_id | BIGINT | - | Y | 数据源 ID |
| table_name | VARCHAR | 100 | Y | 表名 |
| field_name | VARCHAR | 100 | N | 字段名 |
| aggregation | VARCHAR | 20 | N | 聚合方式 (SUM/COUNT/AVG/MAX/MIN) |
| unit | VARCHAR | 20 | N | 单位 |
| precision | INT | - | N | 精度（小数位） |
| category_id | BIGINT | - | N | 分类 ID |
| parent_id | BIGINT | - | N | 父指标 ID |
| status | TINYINT | - | N | 状态 (0 禁用 1 正常) |
| version | INT | - | N | 版本号 |
| created_by | BIGINT | - | N | 创建人 |
| created_at | DATETIME | - | N | 创建时间 |
| updated_at | DATETIME | - | N | 更新时间 |

### 3.2 指标分类表 (metric_category)

| 字段 | 类型 | 长度 | 必填 | 说明 |
|------|------|------|------|------|
| id | BIGINT | - | Y | 主键 ID |
| name | VARCHAR | 100 | Y | 分类名称 |
| code | VARCHAR | 50 | Y | 分类编码 |
| parent_id | BIGINT | - | N | 父分类 ID |
| sort_order | INT | - | N | 排序 |
| created_at | DATETIME | - | N | 创建时间 |

### 3.3 指标版本表 (metric_version)

| 字段 | 类型 | 长度 | 必填 | 说明 |
|------|------|------|------|------|
| id | BIGINT | - | Y | 主键 ID |
| metric_id | BIGINT | - | Y | 指标 ID |
| version | INT | - | Y | 版本号 |
| change_type | VARCHAR | 20 | Y | 变更类型 |
| change_content | TEXT | - | N | 变更内容 |
| change_reason | VARCHAR | 500 | N | 变更原因 |
| old_value | TEXT | - | N | 旧值 |
| new_value | TEXT | - | N | 新值 |
| created_by | BIGINT | - | N | 操作人 |
| created_at | DATETIME | - | N | 操作时间 |

---

## 4. 语义管理模块

### 4.1 同义词表 (synonym)

| 字段 | 类型 | 长度 | 必填 | 说明 |
|------|------|------|------|------|
| id | BIGINT | - | Y | 主键 ID |
| standard_word | VARCHAR | 100 | Y | 标准词 |
| metric_id | BIGINT | - | Y | 关联指标 ID |
| status | TINYINT | - | N | 状态 |
| created_by | BIGINT | - | N | 创建人 |
| created_at | DATETIME | - | N | 创建时间 |
| updated_at | DATETIME | - | N | 更新时间 |

### 4.2 同义词别名表 (synonym_alias)

| 字段 | 类型 | 长度 | 必填 | 说明 |
|------|------|------|------|------|
| id | BIGINT | - | Y | 主键 ID |
| synonym_id | BIGINT | - | Y | 同义词 ID |
| alias | VARCHAR | 100 | Y | 别名 |
| priority | INT | - | N | 优先级 |
| created_at | DATETIME | - | N | 创建时间 |

### 4.3 查询规则表 (query_rule)

| 字段 | 类型 | 长度 | 必填 | 说明 |
|------|------|------|------|------|
| id | BIGINT | - | Y | 主键 ID |
| name | VARCHAR | 100 | Y | 规则名称 |
| pattern | VARCHAR | 500 | Y | 匹配模式 |
| rule_type | VARCHAR | 20 | Y | 规则类型 |
| sql_template | TEXT | - | N | SQL 模板 |
| priority | INT | - | N | 优先级 |
| status | TINYINT | - | N | 状态 |
| created_at | DATETIME | - | N | 创建时间 |

---

## 5. 查询管理模块

### 5.1 查询历史表 (query_history)

| 字段 | 类型 | 长度 | 必填 | 说明 |
|------|------|------|------|------|
| id | BIGINT | - | Y | 主键 ID |
| user_id | BIGINT | - | Y | 用户 ID |
| query_text | VARCHAR | 500 | Y | 查询语句 |
| sql_text | TEXT | - | N | 生成的 SQL |
| matched_metrics | VARCHAR | 500 | N | 匹配的指标 |
| execute_time_ms | INT | - | N | 执行耗时 |
| result_count | INT | - | N | 结果数量 |
| status | TINYINT | - | N | 状态 (0 失败 1 成功) |
| error_message | VARCHAR | 500 | N | 错误信息 |
| created_at | DATETIME | - | N | 创建时间 |

### 5.2 查询收藏表 (query_favorite)

| 字段 | 类型 | 长度 | 必填 | 说明 |
|------|------|------|------|------|
| id | BIGINT | - | Y | 主键 ID |
| user_id | BIGINT | - | Y | 用户 ID |
| query_id | BIGINT | - | Y | 查询历史 ID |
| name | VARCHAR | 200 | Y | 收藏名称 |
| sort_order | INT | - | N | 排序 |
| created_at | DATETIME | - | N | 创建时间 |
| updated_at | DATETIME | - | N | 更新时间 |

---

## 6. 可视化模块

### 6.1 仪表板表 (dashboard)

| 字段 | 类型 | 长度 | 必填 | 说明 |
|------|------|------|------|------|
| id | BIGINT | - | Y | 主键 ID |
| name | VARCHAR | 100 | Y | 仪表板名称 |
| code | VARCHAR | 50 | Y | 编码 |
| description | TEXT | - | N | 描述 |
| layout_json | JSON | - | N | 布局配置 |
| config_json | JSON | - | N | 配置信息 |
| status | TINYINT | - | N | 状态 |
| is_public | TINYINT | - | N | 是否公开 |
| created_by | BIGINT | - | N | 创建人 |
| created_at | DATETIME | - | N | 创建时间 |
| updated_at | DATETIME | - | N | 更新时间 |

### 6.2 仪表板组件表 (dashboard_widget)

| 字段 | 类型 | 长度 | 必填 | 说明 |
|------|------|------|------|------|
| id | BIGINT | - | Y | 主键 ID |
| dashboard_id | BIGINT | - | Y | 仪表板 ID |
| widget_type | VARCHAR | 20 | Y | 组件类型 |
| title | VARCHAR | 100 | Y | 标题 |
| config_json | JSON | - | N | 配置 |
| data_source_json | JSON | - | N | 数据源配置 |
| position_json | JSON | - | N | 位置配置 |
| sort_order | INT | - | N | 排序 |
| created_at | DATETIME | - | N | 创建时间 |

---

## 7. 审计日志模块

### 7.1 审计日志表 (audit_log)

| 字段 | 类型 | 长度 | 必填 | 说明 |
|------|------|------|------|------|
| id | BIGINT | - | Y | 主键 ID |
| trace_id | VARCHAR | 64 | N | 链路 ID |
| user_id | BIGINT | - | N | 用户 ID |
| username | VARCHAR | 50 | N | 用户名 |
| action | VARCHAR | 50 | Y | 操作类型 |
| resource_type | VARCHAR | 50 | N | 资源类型 |
| resource_id | BIGINT | - | N | 资源 ID |
| request_method | VARCHAR | 10 | N | 请求方法 |
| request_uri | VARCHAR | 255 | N | 请求 URI |
| request_body | TEXT | - | N | 请求体 |
| response_status | INT | - | N | 响应状态 |
| response_body | TEXT | - | N | 响应体 |
| ip_address | VARCHAR | 50 | N | IP 地址 |
| user_agent | VARCHAR | 500 | N | 用户代理 |
| execute_time_ms | INT | - | N | 执行耗时 |
| result | VARCHAR | 20 | N | 结果 (SUCCESS/FAILED) |
| error_message | TEXT | - | N | 错误信息 |
| created_at | DATETIME | - | N | 创建时间 |

**索引：**
- idx_user_id (user_id)
- idx_action (action)
- idx_resource (resource_type, resource_id)
- idx_time (created_at)
- idx_trace (trace_id)

### 7.2 登录日志表 (login_log)

| 字段 | 类型 | 长度 | 必填 | 说明 |
|------|------|------|------|------|
| id | BIGINT | - | Y | 主键 ID |
| user_id | BIGINT | - | N | 用户 ID |
| username | VARCHAR | 50 | Y | 用户名 |
| login_type | VARCHAR | 20 | N | 登录类型 |
| ip_address | VARCHAR | 50 | N | IP 地址 |
| user_agent | VARCHAR | 500 | N | 用户代理 |
| login_time | DATETIME | - | N | 登录时间 |
| logout_time | DATETIME | - | N | 登出时间 |
| status | TINYINT | - | N | 状态 (0 失败 1 成功) |
| fail_reason | VARCHAR | 255 | N | 失败原因 |
| browser | VARCHAR | 50 | N | 浏览器 |
| os | VARCHAR | 50 | N | 操作系统 |

---

## 8. 系统配置模块

### 8.1 系统配置表 (sys_config)

| 字段 | 类型 | 长度 | 必填 | 说明 |
|------|------|------|------|------|
| id | BIGINT | - | Y | 主键 ID |
| config_key | VARCHAR | 100 | Y | 配置键 |
| config_value | TEXT | - | N | 配置值 |
| config_type | VARCHAR | 20 | N | 配置类型 |
| description | VARCHAR | 255 | N | 描述 |
| is_encrypted | TINYINT | - | N | 是否加密 |
| created_by | BIGINT | - | N | 创建人 |
| created_at | DATETIME | - | N | 创建时间 |
| updated_at | DATETIME | - | N | 更新时间 |

### 8.2 操作日志表 (operation_log)

| 字段 | 类型 | 长度 | 必填 | 说明 |
|------|------|------|------|------|
| id | BIGINT | - | Y | 主键 ID |
| module | VARCHAR | 50 | N | 模块 |
| business_type | VARCHAR | 50 | N | 业务类型 |
| method | VARCHAR | 200 | N | 方法 |
| request_method | VARCHAR | 10 | N | 请求方式 |
| operator_type | VARCHAR | 20 | N | 操作人类别 |
| oper_name | VARCHAR | 50 | N | 操作人员 |
| dept_name | VARCHAR | 50 | N | 部门名称 |
| oper_url | VARCHAR | 255 | N | 请求 URL |
| oper_ip | VARCHAR | 50 | N | 主机地址 |
| oper_location | VARCHAR | 255 | N | 操作地点 |
| oper_param | VARCHAR | 2000 | N | 请求参数 |
| json_result | VARCHAR | 2000 | N | 返回参数 |
| status | TINYINT | - | N | 状态 |
| error_msg | VARCHAR | 2000 | N | 错误消息 |
| oper_time | DATETIME | - | N | 操作时间 |

---

## 9. 数据权限模块

### 9.1 数据权限表 (data_permission)

| 字段 | 类型 | 长度 | 必填 | 说明 |
|------|------|------|------|------|
| id | BIGINT | - | Y | 主键 ID |
| role_id | BIGINT | - | Y | 角色 ID |
| resource_type | VARCHAR | 50 | Y | 资源类型 (表名) |
| permission_type | VARCHAR | 20 | Y | 权限类型 (ROW/COLUMN) |
| rule_expression | TEXT | - | N | 规则表达式 |
| rule_type | VARCHAR | 20 | N | 规则类型 (ALLOW/DENY) |
| columns | VARCHAR | 500 | N | 列权限 (逗号分隔) |
| created_at | DATETIME | - | N | 创建时间 |

---

## 10. 定时任务模块

### 10.1 定时任务表 (schedule_job)

| 字段 | 类型 | 长度 | 必填 | 说明 |
|------|------|------|------|------|
| id | BIGINT | - | Y | 主键 ID |
| job_name | VARCHAR | 100 | Y | 任务名称 |
| job_group | VARCHAR | 50 | Y | 任务组 |
| job_class | VARCHAR | 255 | Y | 任务类 |
| cron_expression | VARCHAR | 100 | N | Cron 表达式 |
| trigger_type | VARCHAR | 20 | N | 触发类型 |
| trigger_interval | BIGINT | - | N | 触发间隔 (ms) |
| job_data | JSON | - | N | 任务参数 |
| status | TINYINT | - | N | 状态 |
| misfire_policy | VARCHAR | 20 | N | 错失执行策略 |
| concurrent | TINYINT | - | N | 是否并发 |
| last_execute_time | DATETIME | - | N | 最后执行时间 |
| next_execute_time | DATETIME | - | N | 下次执行时间 |
| created_at | DATETIME | - | N | 创建时间 |
| updated_at | DATETIME | - | N | 更新时间 |

### 10.2 任务执行日志表 (schedule_job_log)

| 字段 | 类型 | 长度 | 必填 | 说明 |
|------|------|------|------|------|
| id | BIGINT | - | Y | 主键 ID |
| job_id | BIGINT | - | Y | 任务 ID |
| job_name | VARCHAR | 100 | Y | 任务名称 |
| job_group | VARCHAR | 50 | Y | 任务组 |
| execute_time | DATETIME | - | N | 执行时间 |
| duration_ms | BIGINT | - | N | 执行耗时 |
| status | TINYINT | - | N | 状态 |
| error_message | TEXT | - | N | 错误信息 |
| created_at | DATETIME | - | N | 创建时间 |

---

*文档版本：1.0.0*
*最后更新：2026-03-10*
