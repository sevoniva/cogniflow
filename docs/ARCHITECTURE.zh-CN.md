# Language | 语言

[English](./ARCHITECTURE.md) | [中文](./ARCHITECTURE.zh-CN.md)

# ChatBI 企业级架构设计文档

## 1. 架构概述

### 1.1 系统定位

ChatBI 是一款企业级智能商业智能平台，通过自然语言交互实现数据查询、分析和可视化。

### 1.2 架构原则

- **安全性优先**：符合等保 2.0 三级、金融行业安全规范
- **高可用性**：99.9% 可用性 SLA
- **可扩展性**：水平扩展、微服务架构
- **可维护性**：清晰的模块边界、完善的文档
- **合规性**：满足 GDPR、个人信息保护法要求

### 1.3 整体架构图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                            接入层 (Access Layer)                         │
├─────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │
│  │   Web SPA   │  │  Mobile App │  │  第三方 API  │  │  管理控制台  │    │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘    │
└─────────┼────────────────┼────────────────┼────────────────┼───────────┘
          │                │                │                │
          └────────────────┴────────┬───────┴────────────────┘
                                    │
                          ┌────────▼────────┐
                          │   API Gateway   │
                          │  (Kong/SCG)     │
                          │  • 鉴权         │
                          │  • 限流         │
                          │  • 路由         │
                          └────────┬────────┘
                                   │
┌──────────────────────────────────┼──────────────────────────────────────┐
│                        应用层 (Application Layer)                        │
├──────────────────────────────────┼──────────────────────────────────────┤
│  ┌───────────────────────────────▼────────────────────────────────┐     │
│  │                    认证授权服务 (Auth Service)                   │     │
│  │         Spring Security + JWT + OAuth2 + LDAP/SSO               │     │
│  └────────────────────────────────────────────────────────────────┘     │
│  ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐           │
│  │  查询服务       │ │  指标服务       │ │  数据源服务     │           │
│  │  Query Service  │ │  Metric Service │ │  DataSource Svc │           │
│  │  • NLU 引擎      │ │  • CRUD         │ │  • 连接管理     │           │
│  │  • SQL 生成      │ │  • 版本管理     │ │  • 连接池       │           │
│  │  • 结果缓存     │ │  • 审批流       │ │  • 健康检查     │           │
│  └─────────────────┘ └─────────────────┘ └─────────────────┘           │
│  ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐           │
│  │  可视化服务     │ │  权限服务       │ │  审计日志服务   │           │
│  │  Visual Service │ │  Permission Svc │ │  Audit Service  │           │
│  │  • 图表渲染     │ │  • RBAC         │ │  • 操作日志     │           │
│  │  • 仪表板       │ │  • 数据权限     │ │  • 变更追溯     │           │
│  │  • 报告生成     │ │  • 行/列级控制  │ │  • 合规报告     │           │
│  └─────────────────┘ └─────────────────┘ └─────────────────┘           │
└─────────────────────────────────────────────────────────────────────────┘
                                   │
┌──────────────────────────────────┼──────────────────────────────────────┐
│                        数据层 (Data Layer)                               │
├──────────────────────────────────┼──────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │
│  │   MySQL     │  │   Redis     │  │ Elasticsearch│  │  MinIO      │    │
│  │   主从复制   │  │   集群      │  │   日志存储   │  │  文件存储   │    │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘    │
└─────────────────────────────────────────────────────────────────────────┘
```

### 1.4 技术栈选型

| 层级 | 技术 | 选型理由 |
|------|------|----------|
| **前端** | Vue 3 + TypeScript | 类型安全、生态成熟 |
| **UI 框架** | Element Plus | 企业级组件库 |
| **图表库** | Apache ECharts | 强大的可视化能力 |
| **后端框架** | Spring Boot 3.2 | 企业级、生态完善 |
| **安全框架** | Spring Security | 功能完整、可扩展 |
| **ORM** | MyBatis Plus | 灵活、性能优 |
| **数据库** | MySQL 8.0 | 成熟、可靠 |
| **缓存** | Redis 7.x | 高性能、多数据结构 |
| **消息队列** | RabbitMQ/Kafka | 异步解耦 |
| **搜索引擎** | Elasticsearch | 全文检索、分析 |
| **API 网关** | Spring Cloud Gateway | 轻量、易集成 |
| **容器编排** | Kubernetes | 行业标准 |
| **监控** | Prometheus + Grafana | 开源、强大 |
| **日志** | ELK Stack | 集中式日志管理 |

---

## 2. 核心模块设计

### 2.1 认证授权模块

#### 2.1.1 认证流程

```
┌──────┐     ┌──────────┐     ┌─────────────┐     ┌──────────┐
│ 用户 │     │  前端    │     │  认证服务    │     │  数据库  │
└──┬───┘     └────┬─────┘     └──────┬──────┘     └────┬─────┘
   │              │                   │                  │
   │ 1. 输入账号密码│                   │                  │
   │─────────────>│                   │                  │
   │              │ 2. POST /auth/login│                  │
   │              │──────────────────>│                  │
   │              │                   │ 3. 验证用户       │
   │              │                   │─────────────────>│
   │              │                   │<─────────────────│
   │              │                   │ 4. 生成 JWT       │
   │              │ 5. 返回 Token     │                  │
   │              │<──────────────────│                  │
   │ 6. 存储 Token │                   │                  │
   │<─────────────│                   │                  │
   │              │                   │                  │
   │ 7. 后续请求携带 Token              │                  │
   │─────────────>│                   │                  │
   │              │ 8. 验证 Token      │                  │
   │              │──────────────────>│                  │
```

#### 2.1.2 JWT Token 结构

```json
{
  "header": {
    "alg": "HS256",
    "typ": "JWT"
  },
  "payload": {
    "sub": "user123",
    "username": "zhangsan",
    "roles": ["ROLE_USER", "ROLE_ADMIN"],
    "permissions": ["metric:read", "metric:write"],
    "iat": 1699999999,
    "exp": 1700003599,
    "jti": "uuid-1234-5678"
  }
}
```

### 2.2 查询引擎模块

#### 2.2.1 Text-to-SQL 流程

```
用户输入："本月华东区销售额按产品类别"
    │
    ▼
┌─────────────────┐
│  词法分析/分词   │
│  - 时间：本月    │
│  - 区域：华东区  │
│  - 指标：销售额  │
│  - 维度：产品类别│
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  意图识别       │
│  - 查询类型：明细│
│  - 聚合：按维度  │
│  - 过滤：时间 + 区域│
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  SQL 生成        │
│  SELECT 产品类别，│
│         SUM(销售额)│
│  FROM fact_sales│
│  WHERE 时间 = 本月│
│    AND 区域 = '华东'│
│  GROUP BY 产品类别│
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  SQL 执行与优化  │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  结果返回        │
└─────────────────┘
```

### 2.3 权限管理模块

#### 2.3.1 RBAC 模型

```
┌──────────┐     ┌──────────┐     ┌──────────┐
│   用户    │────▶│   角色    │────▶│   权限   │
│  User    │ N:1 │  Role    │ M:N │Permission│
└──────────┘     └──────────┘     └──────────┘
                      │
                      ▼
               ┌──────────┐
               │  数据权限  │
               │  - 行级   │
               │  - 列级   │
               └──────────┘
```

#### 2.3.2 数据权限示例

```yaml
行级权限:
  - 条件：department_id = CURRENT_USER.dept_id
  - 条件：region IN ('华东', '华南')

列级权限:
  - 敏感列 (手机号、身份证): 仅 ADMIN 角色可见
  - 脱敏规则：手机号中间 4 位掩码
```

---

## 3. 数据库设计

### 3.1 核心表结构

#### 用户表 (sys_user)
```sql
CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  email VARCHAR(100),
  phone VARCHAR(20),
  status TINYINT DEFAULT 1,
  dept_id BIGINT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_username (username),
  INDEX idx_dept (dept_id)
);
```

#### 角色表 (sys_role)
```sql
CREATE TABLE sys_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_code VARCHAR(50) NOT NULL UNIQUE,
  role_name VARCHAR(100) NOT NULL,
  description VARCHAR(255),
  status TINYINT DEFAULT 1,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

#### 权限表 (sys_permission)
```sql
CREATE TABLE sys_permission (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  perm_code VARCHAR(100) NOT NULL UNIQUE,
  perm_name VARCHAR(100) NOT NULL,
  resource_type VARCHAR(20), -- MENU, BUTTON, API
  resource_path VARCHAR(255),
  parent_id BIGINT DEFAULT 0,
  sort_order INT DEFAULT 0
);
```

#### 用户角色关联表 (sys_user_role)
```sql
CREATE TABLE sys_user_role (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY (user_id, role_id)
);
```

#### 角色权限关联表 (sys_role_permission)
```sql
CREATE TABLE sys_role_permission (
  role_id BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  PRIMARY KEY (role_id, permission_id)
);
```

#### 数据源表 (data_source)
```sql
CREATE TABLE data_source (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  type VARCHAR(20) NOT NULL, -- MYSQL, ORACLE, HIVE
  url VARCHAR(500) NOT NULL,
  username VARCHAR(100),
  password_encrypted VARCHAR(500),
  config_json JSON,
  status TINYINT DEFAULT 1,
  created_by BIGINT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

#### 指标表 (metric)
```sql
CREATE TABLE metric (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(50) NOT NULL UNIQUE,
  name VARCHAR(100) NOT NULL,
  definition TEXT,
  data_source_id BIGINT,
  table_name VARCHAR(100),
  field_name VARCHAR(100),
  aggregation VARCHAR(20), -- SUM, COUNT, AVG
  status TINYINT DEFAULT 1,
  version INT DEFAULT 1,
  created_by BIGINT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

#### 审计日志表 (audit_log)
```sql
CREATE TABLE audit_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT,
  username VARCHAR(50),
  action VARCHAR(50),
  resource_type VARCHAR(50),
  resource_id BIGINT,
  request_method VARCHAR(10),
  request_uri VARCHAR(255),
  request_body TEXT,
  response_status INT,
  response_body TEXT,
  ip_address VARCHAR(50),
  user_agent VARCHAR(500),
  execute_time_ms INT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_user (user_id),
  INDEX idx_action (action),
  INDEX idx_time (created_at)
);
```

---

## 4. 安全设计

### 4.1 安全架构

```
┌─────────────────────────────────────────────────────────┐
│                      安全防护体系                        │
├─────────────────────────────────────────────────────────┤
│  网络安全          │  应用安全          │  数据安全      │
│  • HTTPS/TLS      │  • 输入验证        │  • 加密存储    │
│  • WAF            │  • SQL 注入防护     │  • 数据脱敏    │
│  • DDoS 防护       │  • XSS 防护        │  • 访问控制    │
│  • 网络隔离        │  • CSRF 防护       │  • 审计日志    │
└─────────────────────────────────────────────────────────┘
```

### 4.2 密码学方案

| 场景 | 算法 | 说明 |
|------|------|------|
| 密码存储 | BCrypt | 自适应哈希 |
| JWT 签名 | HMAC-SHA256 | 对称签名 |
| 敏感数据加密 | AES-256-GCM | 对称加密 |
| 数据传输 | TLS 1.3 | 传输层加密 |
| 国密支持 | SM2/SM3/SM4 | 金融行业要求 |

### 4.3 OWASP Top 10 防护

1. **注入攻击**：参数化查询、ORM 框架
2. **认证失效**：强密码策略、MFA、会话超时
3. **敏感数据泄露**：加密存储、传输加密
4. **XXE 攻击**：禁用外部实体
5. **访问控制失效**：RBAC、最小权限原则
6. **安全配置错误**：安全基线、自动化检查
7. **XSS 攻击**：输入过滤、输出编码、CSP
8. **反序列化漏洞**：类型检查、版本升级
9. **组件漏洞**：依赖扫描、及时更新
10. **日志不足**：完整审计日志、告警

---

## 5. 高可用设计

### 5.1 架构高可用

```
                    ┌──────────┐
                    │  LVS/Nginx│
                    └────┬─────┘
                         │
         ┌───────────────┼───────────────┐
         │               │               │
   ┌────▼────┐    ┌────▼────┐    ┌────▼────┐
   │  Node 1 │    │  Node 2 │    │  Node 3 │
   │  Pod    │    │  Pod    │    │  Pod    │
   └────┬────┘    └────┬────┘    └────┬────┘
        │              │              │
         ┌─────────────┴──────────────┘
                   │
         ┌────────▼────────┐
         │    MySQL 主     │
         └────────┬────────┘
                  │
    ┌─────────────┼─────────────┐
    │             │             │
┌───▼───┐   ┌────▼────┐   ┌────▼───┐
│从 1   │   │  从 2    │   │  从 3   │
└───────┘   └─────────┘   └────────┘
```

### 5.2 故障转移策略

| 组件 | 策略 | RTO | RPO |
|------|------|-----|-----|
| 应用服务 | K8s 健康检查 + 自动重启 | < 30s | 0 |
| MySQL | MHA 主从切换 | < 60s | < 5s |
| Redis | Sentinel 自动故障转移 | < 30s | 0 |
| 网关 | 多实例 + 健康检查 | < 10s | 0 |

---

## 6. 性能设计

### 6.1 缓存策略

```
┌─────────────────────────────────────────────────────┐
│                   多级缓存架构                       │
├─────────────────────────────────────────────────────┤
│  L1: 本地缓存 (Caffeine)                            │
│      • 热点配置数据                                  │
│      • 命中率：95%+                                  │
├─────────────────────────────────────────────────────┤
│  L2: 分布式缓存 (Redis)                             │
│      • 查询结果缓存 (TTL: 5-30min)                  │
│      • Session 存储                                  │
│      • 命中率：80%+                                  │
├─────────────────────────────────────────────────────┤
│  L3: 数据库 (MySQL)                                 │
│      • 持久化存储                                    │
│      • 索引优化                                      │
└─────────────────────────────────────────────────────┘
```

### 6.2 性能指标

| 指标 | 目标值 | 说明 |
|------|--------|------|
| P95 响应时间 | < 500ms | 简单查询 |
| P99 响应时间 | < 2s | 复杂查询 |
| 并发用户数 | 1000+ | 同时在线 |
| QPS | 500+ | 每秒查询数 |
| 可用性 | 99.9% | SLA 目标 |

---

## 7. 合规性设计

### 7.1 等保 2.0 三级合规

| 要求 | 实现方案 | 状态 |
|------|----------|------|
| 身份鉴别 | 双因素认证、密码复杂度 | ✅ |
| 访问控制 | RBAC、最小权限 | ✅ |
| 安全审计 | 完整操作日志 | ✅ |
| 入侵防范 | WAF、异常检测 | ✅ |
| 数据完整性 | 校验和、数字签名 | ✅ |
| 数据保密性 | 加密存储传输 | ✅ |
| 备份恢复 | 定时备份、DR 方案 | ✅ |

### 7.2 个人信息保护法合规

- **最小化原则**：仅收集必要信息
- **明示同意**：隐私政策、用户授权
- **访问权**：用户可查询个人信息
- **删除权**：用户可申请删除数据
- **可携带权**：支持数据导出

---

## 8. 部署架构

### 8.1 生产环境部署

```yaml
# Kubernetes 部署配置
namespace: chatbi-prod

replicas:
  api: 3
  gateway: 2

resources:
  api:
    requests:
      cpu: 500m
      memory: 1Gi
    limits:
      cpu: 2000m
      memory: 4Gi

affinity:
  podAntiAffinity:
    requiredDuringSchedulingIgnoredDuringExecution:
      - labelSelector:
          matchExpressions:
            - key: app
              operator: In
              values:
                - chatbi-api
        topologyKey: kubernetes.io/hostname
```

### 8.2 环境规划

| 环境 | 用途 | 配置 |
|------|------|------|
| 开发环境 | 日常开发 | 单节点 |
| 测试环境 | 功能测试 | 2 节点 |
| 预发环境 | 集成测试 | 生产配置 |
| 生产环境 | 线上服务 | 高可用配置 |

---

## 9. 监控与告警

### 9.1 监控指标

| 类别 | 指标 | 阈值 |
|------|------|------|
| 系统 | CPU 使用率 | > 80% |
| 系统 | 内存使用率 | > 85% |
| 应用 | 接口响应时间 P95 | > 1s |
| 应用 | 错误率 | > 1% |
| 业务 | 查询量 | 突增 50% |
| 业务 | 活跃用户 | 突降 30% |

### 9.2 告警渠道

- 邮件：一般告警
- 短信：紧急告警
- 钉钉/企微：实时通知
- PagerDuty：on-call 轮值

---

## 10. 演进路线

### Phase 1 (1-3 月)：基础能力建设
- [x] 认证授权系统
- [ ] 数据源管理
- [ ] Text-to-SQL 基础
- [ ] RBAC 权限

### Phase 2 (4-6 月)：产品化完善
- [ ] 图表可视化
- [ ] 仪表板
- [ ] 审计日志
- [ ] 监控体系

### Phase 3 (7-12 月)：智能化升级
- [ ] LLM 集成
- [ ] 智能推荐
- [ ] 预测分析
- [ ] 知识图谱

---

*文档版本：1.0.0*
*最后更新：2026-03-10*
*作者：Carson <chuncheng.carson@gmail.com>*
