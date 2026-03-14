# Language | 语言

[English](./API.md) | [中文](./API.zh-CN.md)

# ChatBI API 接口文档

## 1. 接口规范

### 1.1 基础 URL

| 环境 | URL |
|------|-----|
| 开发 | http://localhost:8080/api |
| 生产 | https://chatbi.company.com/api |

### 1.2 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1699999999000,
  "traceId": "abc123"
}
```

### 1.3 错误码定义

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未认证 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |
| 1001 | 用户不存在 |
| 1002 | 密码错误 |
| 1003 | Token 失效 |
| 1004 | Token 过期 |

---

## 2. 认证授权接口

### 2.1 用户登录

```http
POST /api/auth/login
Content-Type: application/json
```

**请求：**
```json
{
  "username": "zhangsan",
  "password": "Password123!",
  "captcha": "abcd"
}
```

**响应：**
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4...",
    "expiresIn": 3600,
    "tokenType": "Bearer",
    "user": {
      "id": 1,
      "username": "zhangsan",
      "nickName": "张三",
      "avatar": "https://...",
      "roles": ["ROLE_USER"],
      "permissions": ["metric:read", "query:execute"]
    }
  }
}
```

### 2.2 刷新 Token

```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4..."
}
```

### 2.3 用户登出

```http
POST /api/auth/logout
Authorization: Bearer {accessToken}
```

### 2.4 获取当前用户信息

```http
GET /api/auth/me
Authorization: Bearer {accessToken}
```

**响应：**
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "username": "zhangsan",
    "nickName": "张三",
    "email": "zhangsan@company.com",
    "phone": "138****1234",
    "dept": {
      "id": 1,
      "name": "技术部"
    },
    "roles": [
      {
        "id": 1,
        "roleCode": "USER",
        "roleName": "普通用户"
      }
    ],
    "permissions": ["metric:read", "query:execute"]
  }
}
```

---

## 3. 用户管理接口

### 3.1 用户列表

```http
GET /api/system/users?page=1&size=10&username=zhangsan&status=1
Authorization: Bearer {accessToken}
```

**响应：**
```json
{
  "code": 200,
  "data": {
    "total": 100,
    "list": [
      {
        "id": 1,
        "username": "zhangsan",
        "nickName": "张三",
        "email": "zhangsan@company.com",
        "phone": "13812341234",
        "gender": 1,
        "avatar": "https://...",
        "dept": {
          "id": 1,
          "name": "技术部"
        },
        "roles": [{"id": 1, "roleName": "普通用户"}],
        "status": 1,
        "createdAt": "2026-01-01 10:00:00"
      }
    ],
    "page": 1,
    "size": 10
  }
}
```

### 3.2 创建用户

```http
POST /api/system/users
Authorization: Bearer {accessToken}
```

**请求：**
```json
{
  "username": "lisi",
  "password": "Password123!",
  "nickName": "李四",
  "email": "lisi@company.com",
  "phone": "13812345678",
  "gender": 1,
  "deptId": 1,
  "roleIds": [1, 2],
  "status": 1
}
```

### 3.3 更新用户

```http
PUT /api/system/users/{id}
Authorization: Bearer {accessToken}
```

### 3.4 删除用户

```http
DELETE /api/system/users/{id}
Authorization: Bearer {accessToken}
```

### 3.5 重置密码

```http
POST /api/system/users/{id}/password/reset
Authorization: Bearer {accessToken}
```

**请求：**
```json
{
  "newPassword": "NewPassword123!"
}
```

---

## 4. 角色管理接口

### 4.1 角色列表

```http
GET /api/system/roles
Authorization: Bearer {accessToken}
```

### 4.2 创建角色

```http
POST /api/system/roles
Authorization: Bearer {accessToken}
```

**请求：**
```json
{
  "roleCode": "MANAGER",
  "roleName": "部门经理",
  "description": "部门经理角色",
  "dataScope": 2,
  "permissionIds": [1, 2, 3],
  "status": 1
}
```

### 4.3 更新角色

```http
PUT /api/system/roles/{id}
Authorization: Bearer {accessToken}
```

### 4.4 删除角色

```http
DELETE /api/system/roles/{id}
Authorization: Bearer {accessToken}
```

### 4.5 分配权限

```http
POST /api/system/roles/{id}/permissions
Authorization: Bearer {accessToken}
```

**请求：**
```json
{
  "permissionIds": [1, 2, 3, 4, 5]
}
```

---

## 5. 权限管理接口

### 5.1 权限树

```http
GET /api/system/permissions/tree
Authorization: Bearer {accessToken}
```

**响应：**
```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "permCode": "metric",
      "permName": "指标管理",
      "resourceType": "MENU",
      "icon": "data_analysis",
      "children": [
        {
          "id": 2,
          "permCode": "metric:read",
          "permName": "查看指标",
          "resourceType": "BUTTON"
        },
        {
          "id": 3,
          "permCode": "metric:write",
          "permName": "编辑指标",
          "resourceType": "BUTTON"
        }
      ]
    }
  ]
}
```

---

## 6. 数据源管理接口

### 6.1 数据源列表

```http
GET /api/datasources
Authorization: Bearer {accessToken}
```

### 6.2 创建数据源

```http
POST /api/datasources
Authorization: Bearer {accessToken}
```

**请求：**
```json
{
  "name": "生产数据库",
  "code": "prod_mysql",
  "type": "MYSQL",
  "url": "jdbc:mysql://192.168.1.100:3306/prod?useSSL=true",
  "username": "chatbi",
  "password": "secure_password",
  "driverClass": "com.mysql.cj.jdbc.Driver",
  "config": {
    "connectionTimeout": 30000,
    "idleTimeout": 600000,
    "maxLifetime": 1800000,
    "maximumPoolSize": 20,
    "minimumIdle": 5
  }
}
```

### 6.3 测试数据源连接

```http
POST /api/datasources/test
Authorization: Bearer {accessToken}
```

**请求：** 同创建数据源

**响应：**
```json
{
  "code": 200,
  "message": "连接成功",
  "data": {
    "success": true,
    "database": "prod",
    "version": "8.0.32",
    "connectTime": 45
  }
}
```

### 6.4 更新数据源

```http
PUT /api/datasources/{id}
Authorization: Bearer {accessToken}
```

### 6.5 删除数据源

```http
DELETE /api/datasources/{id}
Authorization: Bearer {accessToken}
```

### 6.6 获取数据源表

```http
GET /api/datasources/{id}/tables
Authorization: Bearer {accessToken}
```

**响应：**
```json
{
  "code": 200,
  "data": [
    {
      "tableName": "user",
      "tableComment": "用户表",
      "columns": [
        {
          "columnName": "id",
          "dataType": "BIGINT",
          "nullable": false,
          "comment": "主键 ID"
        }
      ]
    }
  ]
}
```

---

## 7. 指标管理接口

### 7.1 指标列表

```http
GET /api/metrics?page=1&size=10&keyword=销售&status=1
Authorization: Bearer {accessToken}
```

**响应：**
```json
{
  "code": 200,
  "data": {
    "total": 50,
    "list": [
      {
        "id": 1,
        "code": "SALES_AMOUNT",
        "name": "销售金额",
        "description": "月度销售金额统计",
        "definition": "SUM(order.amount)",
        "dataSource": {
          "id": 1,
          "name": "生产数据库"
        },
        "tableName": "fact_order",
        "fieldName": "amount",
        "aggregation": "SUM",
        "unit": "元",
        "precision": 2,
        "category": {
          "id": 1,
          "name": "销售指标"
        },
        "status": 1,
        "version": 3,
        "createdBy": "admin",
        "createdAt": "2026-01-01 10:00:00",
        "updatedAt": "2026-03-01 15:30:00"
      }
    ]
  }
}
```

### 7.2 创建指标

```http
POST /api/metrics
Authorization: Bearer {accessToken}
```

**请求：**
```json
{
  "code": "SALES_AMOUNT",
  "name": "销售金额",
  "description": "月度销售金额统计",
  "definition": "SUM(order.amount)",
  "dataSourceId": 1,
  "tableName": "fact_order",
  "fieldName": "amount",
  "aggregation": "SUM",
  "unit": "元",
  "precision": 2,
  "categoryId": 1,
  "status": 1
}
```

### 7.3 更新指标

```http
PUT /api/metrics/{id}
Authorization: Bearer {accessToken}
```

### 7.4 删除指标

```http
DELETE /api/metrics/{id}
Authorization: Bearer {accessToken}
```

### 7.5 获取指标详情

```http
GET /api/metrics/{id}
Authorization: Bearer {accessToken}
```

### 7.6 指标版本历史

```http
GET /api/metrics/{id}/versions
Authorization: Bearer {accessToken}
```

### 7.7 回滚指标版本

```http
POST /api/metrics/{id}/versions/{versionId}/rollback
Authorization: Bearer {accessToken}
```

---

## 8. 同义词管理接口

### 8.1 同义词列表

```http
GET /api/synonyms?keyword=销售&page=1&size=10
Authorization: Bearer {accessToken}
```

**响应：**
```json
{
  "code": 200,
  "data": {
    "total": 20,
    "list": [
      {
        "id": 1,
        "standardWord": "销售金额",
        "metric": {
          "id": 1,
          "code": "SALES_AMOUNT",
          "name": "销售金额"
        },
        "aliases": [
          {"id": 1, "alias": "销售额", "priority": 1},
          {"id": 2, "alias": "营收", "priority": 2},
          {"id": 3, "alias": "收入", "priority": 3}
        ],
        "status": 1,
        "createdAt": "2026-01-01 10:00:00"
      }
    ]
  }
}
```

### 8.2 创建同义词

```http
POST /api/synonyms
Authorization: Bearer {accessToken}
```

**请求：**
```json
{
  "standardWord": "销售金额",
  "metricId": 1,
  "aliases": ["销售额", "营收", "收入"],
  "status": 1
}
```

### 8.3 更新同义词

```http
PUT /api/synonyms/{id}
Authorization: Bearer {accessToken}
```

### 8.4 删除同义词

```http
DELETE /api/synonyms/{id}
Authorization: Bearer {accessToken}
```

---

## 9. 查询接口

### 9.1 执行查询

```http
POST /api/query/execute
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**请求：**
```json
{
  "text": "本月华东区销售额按产品类别",
  "filters": {
    "timeRange": {
      "type": "MONTH",
      "value": "CURRENT"
    },
    "region": "华东"
  },
  "groupBy": ["product_category"],
  "limit": 100
}
```

**响应：**
```json
{
  "code": 200,
  "data": {
    "queryId": "q-123456",
    "queryText": "本月华东区销售额按产品类别",
    "generatedSql": "SELECT product_category, SUM(amount) as total FROM fact_sales WHERE region = '华东' AND sale_date >= DATE_TRUNC('month', CURRENT_DATE) GROUP BY product_category",
    "matchedMetrics": [
      {
        "id": 1,
        "code": "SALES_AMOUNT",
        "name": "销售金额",
        "matchType": "SYNONYM",
        "matchedWord": "销售额"
      }
    ],
    "dimensions": ["product_category"],
    "measures": [
      {
        "name": "total",
        "type": "NUMBER",
        "unit": "元"
      }
    ],
    "data": [
      {
        "product_category": "电子产品",
        "total": 1250000.00
      },
      {
        "product_category": "家居用品",
        "total": 890000.00
      }
    ],
    "total": 2,
    "executeTimeMs": 156,
    "cacheHit": false,
    "createdAt": "2026-03-10T10:30:00Z"
  }
}
```

### 9.2 查询历史

```http
GET /api/query/history?page=1&size=20
Authorization: Bearer {accessToken}
```

### 9.3 收藏查询

```http
POST /api/query/favorites
Authorization: Bearer {accessToken}
```

**请求：**
```json
{
  "queryId": 123,
  "name": "我的常用查询"
}
```

### 9.4 收藏列表

```http
GET /api/query/favorites
Authorization: Bearer {accessToken}
```

---

## 10. 仪表板接口

### 10.1 仪表板列表

```http
GET /api/dashboards?page=1&size=10
Authorization: Bearer {accessToken}
```

### 10.2 创建仪表板

```http
POST /api/dashboards
Authorization: Bearer {accessToken}
```

**请求：**
```json
{
  "name": "销售驾驶舱",
  "code": "SALES_DASHBOARD",
  "description": "销售数据概览",
  "layout": {
    "cols": 12,
    "rows": 8
  },
  "config": {
    "refreshInterval": 300,
    "theme": "light"
  },
  "isPublic": true,
  "widgetIds": [1, 2, 3, 4]
}
```

### 10.3 更新仪表板

```http
PUT /api/dashboards/{id}
Authorization: Bearer {accessToken}
```

### 10.4 删除仪表板

```http
DELETE /api/dashboards/{id}
Authorization: Bearer {accessToken}
```

### 10.5 获取仪表板详情

```http
GET /api/dashboards/{id}
Authorization: Bearer {accessToken}
```

### 10.6 获取仪表板数据

```http
GET /api/dashboards/{id}/data
Authorization: Bearer {accessToken}
```

---

## 11. 审计日志接口

### 11.1 审计日志列表

```http
GET /api/audit/logs?userId=1&action=LOGIN&startDate=2026-01-01&endDate=2026-03-10&page=1&size=20
Authorization: Bearer {accessToken}
```

**响应：**
```json
{
  "code": 200,
  "data": {
    "total": 1000,
    "list": [
      {
        "id": 1,
        "traceId": "abc123",
        "userId": 1,
        "username": "zhangsan",
        "action": "LOGIN",
        "resourceType": "USER",
        "requestMethod": "POST",
        "requestUri": "/api/auth/login",
        "ipAddress": "192.168.1.100",
        "userAgent": "Mozilla/5.0...",
        "executeTimeMs": 150,
        "result": "SUCCESS",
        "createdAt": "2026-03-10 10:00:00"
      }
    ]
  }
}
```

### 11.2 登录日志列表

```http
GET /api/audit/login-logs?username=zhangsan&startDate=2026-01-01&endDate=2026-03-10
Authorization: Bearer {accessToken}
```

### 11.3 导出审计日志

```http
POST /api/audit/logs/export
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**请求：**
```json
{
  "userId": 1,
  "action": "LOGIN",
  "startDate": "2026-01-01",
  "endDate": "2026-03-10",
  "format": "EXCEL"
}
```

---

## 12. 系统管理接口

### 12.1 系统配置

```http
GET /api/system/configs?group=security
Authorization: Bearer {accessToken}
```

### 12.2 更新配置

```http
PUT /api/system/configs/{key}
Authorization: Bearer {accessToken}
```

**请求：**
```json
{
  "configValue": "new_value",
  "description": "配置描述"
}
```

### 12.3 获取系统信息

```http
GET /api/system/info
Authorization: Bearer {accessToken}
```

**响应：**
```json
{
  "code": 200,
  "data": {
    "application": {
      "name": "ChatBI",
      "version": "1.0.0",
      "buildTime": "2026-03-10 10:00:00"
    },
    "server": {
      "os": "Linux",
      "arch": "x86_64",
      "cpu": "8 核",
      "memory": "16GB"
    },
    "jvm": {
      "version": "17.0.1",
      "vendor": "Eclipse Adoptium",
      "heapUsed": "512MB",
      "heapMax": "2GB"
    },
    "database": {
      "name": "MySQL",
      "version": "8.0.32",
      "connections": {
        "active": 10,
        "idle": 5,
        "max": 50
      }
    }
  }
}
```

---

## 13. 健康检查接口

### 13.1 健康检查

```http
GET /api/health
```

**响应：**
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "MySQL",
        "validationQuery": "SELECT 1"
      }
    },
    "redis": {
      "status": "UP",
      "details": {
        "version": "7.0.0"
      }
    },
    "disk": {
      "status": "UP",
      "details": {
        "total": "500GB",
        "free": "200GB",
        "threshold": "10%"
      }
    }
  }
}
```

### 13.2 就绪检查

```http
GET /api/health/ready
```

### 13.3 存活检查

```http
GET /api/health/live
```

---

*文档版本：1.0.0*
*最后更新：2026-03-10*
