# Language | 语言

[English](./DEPLOYMENT.md) | [中文](./DEPLOYMENT.zh-CN.md)

# ChatBI Docker Compose 部署手册

这份手册面向“在另一台机器上完整部署并可直接使用”的场景，包含：

- 前端 Web 容器
- 后端 API 容器
- MySQL 数据库容器
- Redis 缓存容器
- AI 配置与启用方式
- 启动、验证、升级、备份和排障步骤

## 1. 部署结果说明

当前 `docker-compose` 方案会部署 4 个服务：

- `web`: Nginx，承载前端静态资源，并反向代理 `/api` 到后端
- `api`: Spring Boot 后端服务
- `mysql`: MySQL 8，保存指标、同义词、查询历史、AI 设置和演示业务数据
- `redis`: Redis 7，提供缓存和运行时依赖

容器关系：

```text
Browser -> web -> api -> mysql
                 \-> redis
```

## 2. 适用场景

适用于：

- 本地完整部署
- 测试服务器部署
- 单机演示环境部署
- 内网可访问环境部署

不建议直接把当前默认配置原样暴露在公网。若用于公网环境，请至少补齐：

- 强密码与自定义密钥
- 外部 HTTPS 反向代理
- 防火墙 / 安全组
- 域名与访问控制

## 3. 服务器要求

最低建议：

- CPU: 2 核
- 内存: 4 GB
- 磁盘: 20 GB
- 操作系统: Linux / macOS / Windows + Docker Desktop

推荐：

- CPU: 4 核
- 内存: 8 GB+
- SSD: 40 GB+

软件要求：

- Docker Engine 24+ 或 Docker Desktop 最新稳定版
- Docker Compose v2+
- Git

## 4. 目录与关键文件

仓库内与部署直接相关的文件：

- `chatbi-server/docker-compose.yml`
- `chatbi-server/Dockerfile`
- `Dockerfile`
- `deploy/nginx/default.conf`
- `chatbi-server/.env.compose.example`

## 5. 在新机器上的完整部署步骤

### 5.1 拉取代码

```bash
git clone https://gitee.com/carson_fan/ChatBI.git
cd ChatBI
```

### 5.2 准备环境变量文件

复制模板：

```bash
cp chatbi-server/.env.compose.example chatbi-server/.env.compose
```

编辑：`chatbi-server/.env.compose`

至少检查这些变量：

- `CHATBI_WEB_HOST_PORT`
- `CHATBI_API_HOST_PORT`
- `CHATBI_DB_HOST_PORT`
- `CHATBI_REDIS_HOST_PORT`
- `MYSQL_ROOT_PASSWORD`
- `MYSQL_PASSWORD`
- `DB_NAME`
- `DB_USERNAME`
- `REDIS_PASSWORD`
- `JWT_SECRET`
- `PASSWORD_SALT`
- `CHATBI_AI_ENABLED`
- `KIMI_ENABLED`
- `KIMI_API_KEY`

### 5.3 如遇网络受限，先设置代理

```bash
export https_proxy=http://127.0.0.1:7897
export http_proxy=http://127.0.0.1:7897
export all_proxy=socks5://127.0.0.1:7897
```

仅在当前机器确实需要代理时执行。

### 5.4 启动完整服务栈

```bash
docker compose --env-file chatbi-server/.env.compose -f chatbi-server/docker-compose.yml up -d --build
```

首次启动会自动完成：

- 构建前端镜像
- 构建后端镜像
- 启动 MySQL / Redis
- 初始化数据库结构与演示数据
- 启动 API 和 Web

## 6. 环境变量说明

### 6.1 端口映射

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `CHATBI_WEB_HOST_PORT` | 前端对外端口 | `18090` |
| `CHATBI_API_HOST_PORT` | 后端对外端口 | `19091` |
| `CHATBI_DB_HOST_PORT` | MySQL 对外端口 | `3306` |
| `CHATBI_REDIS_HOST_PORT` | Redis 对外端口 | `6379` |

如果你希望直接使用 `80/443` 暴露前端，建议由外部 Nginx/Caddy 做 HTTPS，内部仍保留当前映射方案。

### 6.2 数据库与缓存

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `MYSQL_ROOT_PASSWORD` | MySQL root 密码 | `ChatBI@2026` |
| `MYSQL_PASSWORD` | `chatbi` 用户密码 | `ChatBI@2026` |
| `DB_NAME` | 业务数据库名 | `chatbi` |
| `DB_USERNAME` | 业务数据库用户名 | `chatbi` |
| `REDIS_PASSWORD` | Redis 密码 | `ChatBI@2026` |

系统默认会创建：

- 数据库：`chatbi`
- 用户：`chatbi`

### 6.3 后端运行参数

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `SPRING_PROFILES` | Spring Profile | `dev` |
| `SERVER_PORT` | API 容器内部端口 | `8081` |
| `LOG_FILE_PATH` | 日志文件路径 | `/app/logs/chatbi.log` |
| `JAVA_OPTS` | JVM 参数 | 见模板 |

说明：

- 当前 `docker-compose` 推荐使用 `dev` Profile 完成单机部署
- 该模式已经验证可以完整启动并可用
- 如切换到 `prod`，需要你自行确认安全、域名、CORS、日志和反向代理配置满足环境要求

### 6.4 安全变量

| 变量 | 说明 |
|------|------|
| `JWT_SECRET` | JWT 签名密钥，生产环境必须替换 |
| `PASSWORD_SALT` | 密码盐，生产环境必须替换 |

### 6.5 AI 相关变量

当前项目已验证 Kimi 可用，推荐优先使用 Kimi。

#### Kimi

| 变量 | 说明 |
|------|------|
| `CHATBI_AI_ENABLED` | AI 总开关 |
| `KIMI_ENABLED` | 是否启用 Kimi |
| `KIMI_API_KEY` | Kimi API Key |

建议配置：

```env
CHATBI_AI_ENABLED=true
KIMI_ENABLED=true
KIMI_API_KEY=你的KimiKey
```

### 6.6 AI Key 使用规范（必须遵守）

- API Key 只允许通过以下方式注入：
  - `chatbi-server/.env.compose` 环境变量
  - 管理后台 `AI 设置` 页面运行时保存（后端持久化）
- 严禁将真实 Key 明文写入以下位置：
  - Git 仓库代码、脚本、文档
  - 前端源码或 `.env.example`
  - 日志与截图
- 对外展示时统一脱敏：
  - 仅展示前 4 位和后 4 位（如 `sk-****...****`）
- 轮换建议：
  - 每次公开演示后立即轮换 Key
  - 若发现误泄露，立刻在供应商后台吊销并重置

### 6.7 外部模型验证规范（Kimi）

- 可以使用运维提供的有效 Kimi Token 做连通性验证，但只允许用于运行时注入，不得写入仓库文件。
- 推荐通过管理后台 `/admin/ai` 完成配置，或通过 API 在运行态注入。
- 验证脚本建议使用环境变量占位，不要在脚本文件中硬编码真实 Key。

示例（仅本地会话有效）：

```bash
BASE_URL=http://localhost:19091/api
KIMI_API_KEY='<你的KimiToken>'

TOKEN=$(curl -sS -X POST "$BASE_URL/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Admin@123"}' | jq -r '.data.accessToken')

curl -sS -X PUT "$BASE_URL/ai-model/runtime" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"enabled":true}'

curl -sS -X PUT "$BASE_URL/ai-model/default-provider?provider=kimi" \
  -H "Authorization: Bearer $TOKEN"

curl -sS -X PUT "$BASE_URL/ai-model/providers/kimi" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"enabled\":true,\"model\":\"moonshot-v1-32k\",\"apiUrl\":\"https://api.moonshot.cn/v1\",\"apiKey\":\"$KIMI_API_KEY\"}"
```

#### 其他可选 AI 提供商

当前 `docker-compose` 也支持把以下变量注入后端容器：

- `OPENAI_ENABLED`
- `OPENAI_API_KEY`
- `QWEN_ENABLED`
- `QWEN_API_KEY`
- `BAILIAN_ENABLED`
- `BAILIAN_API_KEY`
- `MINIMAX_ENABLED`
- `MINIMAX_API_KEY`
- `GENERIC_ENABLED`
- `GENERIC_API_KEY`
- `GENERIC_API_URL`
- `GENERIC_MODEL`

### 6.8 订阅推送通道配置（邮件/钉钉/企微）

当前版本订阅推送已支持真实通道发送：

- `EMAIL`: 使用 SMTP（`JavaMailSender`）
- `DINGTALK`: 使用机器人 Webhook
- `WECHAT`: 使用企业微信机器人 Webhook

#### SMTP（EMAIL）配置

在 `chatbi-server/.env.compose` 中增加：

```env
SPRING_MAIL_HOST=smtp.example.com
SPRING_MAIL_PORT=465
SPRING_MAIL_USERNAME=alert@example.com
SPRING_MAIL_PASSWORD=your-mail-password
SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true
SPRING_MAIL_PROPERTIES_MAIL_SMTP_SSL_ENABLE=true
```

注意：

- 未配置 `spring.mail` 时，`EMAIL` 推送会返回“邮件服务未启用”。
- 生产环境建议使用专用告警邮箱，不要使用个人邮箱账号。

#### Webhook（DINGTALK / WECHAT）配置

- 在订阅管理里把 `receiver` 填写为完整机器人 Webhook URL。
- 系统会按 JSON 格式调用对应 Webhook，非 2xx 会视为失败并记录日志。
- 调度任务默认“失败隔离”：单条失败不影响后续订阅发送。
- 调度判定规则：
  - `DAILY`：每天仅推送一次
  - `WEEKLY`：按 `pushDay`（如 `MONDAY` / `周一` / `1`）每周推送一次
  - `MONTHLY`：按 `pushDay`（1-31）每月推送一次
  - `CUSTOM`：`pushDay` 解释为“每 N 天推送一次”

### 6.9 AI 可观测性告警通知配置

可选开启 AI 告警自动触达（告警与恢复事件）：

```env
AI_OBSERVABILITY_NOTIFY_ENABLED=true
AI_OBSERVABILITY_NOTIFY_CHANNELS=EMAIL,DINGTALK,WECHAT
AI_OBSERVABILITY_NOTIFY_EMAIL_TO=ops@example.com,owner@example.com
AI_OBSERVABILITY_NOTIFY_DINGTALK_WEBHOOK=https://oapi.dingtalk.com/robot/send?access_token=xxxx
AI_OBSERVABILITY_NOTIFY_WECHAT_WEBHOOK=https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=xxxx
AI_OBSERVABILITY_NOTIFY_SUBJECT_PREFIX=[ChatBI AI告警]
AI_OBSERVABILITY_NOTIFY_MIN_INTERVAL_SECONDS=300
```

说明：

- 支持多通道并行发送，单通道失败不会阻断其他通道。
- 告警消息包含状态、评分、摘要和前几条告警明细。
- 同类告警会按 `AI_OBSERVABILITY_NOTIFY_MIN_INTERVAL_SECONDS` 限频，避免告警风暴刷屏。
- 建议生产环境至少配置一个即时通道（DINGTALK 或 WECHAT）。

说明：

- 当前本地完整验证过的是 Kimi
- 其他提供商的运行能力由后端配置支持，但是否可用取决于你填入的有效凭据和上游服务状态

## 7. 数据初始化与持久化

### 7.1 初始化数据来源

容器首次启动时会自动导入：

- `schema-dev-core-mysql.sql`
- `schema-dev-business-mysql.sql`
- `schema-dev-agile-mysql.sql`
- `security-demo.sql`
- `platform-demo-mysql.sql`
- `extra-demo-mysql.sql`

这些数据包括：

- 默认管理员账号
- 基础权限与角色
- 指标与同义词
- 演示业务数据
- 查询历史、分享、订阅等演示记录

### 7.2 默认账号

- 用户名：`admin`
- 密码：`Admin@123`

### 7.3 持久化说明

Docker Compose 使用命名卷保存：

- `mysql_data`
- `redis_data`

后端日志与上传目录映射到宿主机：

- `chatbi-server/logs`
- `chatbi-server/uploads`

注意：

- 重新 `up -d` 不会丢失 MySQL 数据
- 执行 `docker compose down -v` 会删除数据库和缓存卷

## 8. 部署完成后的验证步骤

### 8.1 检查容器状态

```bash
docker compose --env-file chatbi-server/.env.compose -f chatbi-server/docker-compose.yml ps
```

预期：

- `mysql` healthy
- `redis` healthy
- `api` healthy
- `web` healthy

### 8.2 检查健康接口

```bash
curl http://localhost:19091/actuator/health
```

### 8.3 检查 AI 状态

```bash
curl http://localhost:19091/api/ai-model/status
```

如果 Kimi 已正确配置，预期返回中会看到：

- `runtimeEnabled=true`
- `providerName=Kimi`

### 8.4 验证 Kimi 外部调用（推荐）

```bash
curl -X POST http://localhost:19091/api/ai-model/test/kimi
curl http://localhost:19091/api/ai-model/observability
curl -X POST http://localhost:19091/api/conversation/message \
  -H 'Content-Type: application/json' \
  -d '{"message":"帮我看一下这个平台怎么样","userId":1}'
```

预期：

- `test/kimi` 返回 `data=true`
- `observability` 中 `recentCalls` 出现 `provider=kimi` 且 `success=true`
- `apiKeyConfigured=true`
- 对话接口返回 `source=llm`

### 8.5 运行 API 冒烟测试

```bash
BASE_URL=http://localhost:19091/api bash chatbi-server/scripts/smoke-test.sh
```

### 8.6 页面访问验证

浏览器访问：

- 前端首页：`http://localhost:18090`
- 管理后台：`http://localhost:18090/admin`
- AI 对话页：`http://localhost:18090/chatbi/conversation`
- 图表应用市场：`http://localhost:18090/chatbi/chart-market`

## 9. 部署后怎么用

### 9.1 登录后台

- 账号：`admin`
- 密码：`Admin@123`

### 9.2 配置 AI

如果已经在 `.env.compose` 中填写了 Kimi 变量并重启容器，后端会自动读取。

你也可以登录系统后到：

- `/admin/ai`

查看当前 AI 运行状态和提供商配置。

### 9.3 体验查询

推荐先测试这些问法：

- `本月销售额`
- `毛利率趋势`
- `哪个地区贡献最大？`
- `客户投诉量`

## 10. 升级部署

在服务器上拉取最新代码后执行：

```bash
git pull --rebase

docker compose --env-file chatbi-server/.env.compose -f chatbi-server/docker-compose.yml up -d --build
```

如果只是后端变更，也可以只重建后端：

```bash
docker compose --env-file chatbi-server/.env.compose -f chatbi-server/docker-compose.yml up -d --build api
```

### 10.1 一键部署 + 回滚演练（发布前必做）

仓库已提供可执行演练脚本（真实拉起容器、真实健康检查、真实冒烟、真实回滚）：

```bash
npm run release:drill
```

脚本执行内容：

1. 按 `chatbi-server/.env.compose` 拉起完整服务栈（mysql/redis/api/web）。
2. 校验 API 与 Web 健康检查，并执行 `chatbi-server/scripts/smoke-test.sh`。
3. 为当前 API/Web 镜像打“回滚标签”（`chatbi-local-*:rollback-<timestamp>`）。
4. 强制重建 API/Web（模拟发布升级）。
5. 通过临时 compose override 回滚到基线镜像标签并再次验活 + 冒烟。

可选参数：

- `ENV_FILE=/path/to/.env.compose npm run release:drill`：指定环境文件。
- `DRILL_CLEANUP=true npm run release:drill`：演练结束自动 `docker compose down`。

### 10.2 回滚策略（版本 / 数据库 / 验证）

版本回滚策略：

1. 发布前给当前可用镜像打基线标签（脚本自动完成）。
2. 升级失败时，使用回滚 override 重新拉起 `api/web` 到基线标签。
3. 回滚后执行健康检查与冒烟验证，确认对话、图表、管理接口可用。

数据库回滚策略：

1. 本项目数据库脚本采用幂等初始化策略，避免重复执行破坏已有数据。
2. 发布前必须先执行 MySQL 备份（见“11.1 备份 MySQL”）。
3. 若发布包含破坏性 DDL（新增场景必须提前评审），回滚时先恢复数据库备份，再执行镜像回滚。

回滚验证命令（手动执行）：

```bash
curl -fsS http://127.0.0.1:19091/actuator/health
curl -fsS http://127.0.0.1:18090/
cd chatbi-server && BASE_URL=http://127.0.0.1:19091/api bash scripts/smoke-test.sh
```

### 10.3 配置与安全复核（发布前必做）

仓库已提供发布安全复核脚本（配置完整性 + 密钥泄露扫描 + 权限回归 + 审计追溯）：

```bash
npm run release:security
```

脚本执行内容：

1. 校验关键配置项存在（数据库、缓存、AI 开关、JWT/盐值、可观测通知配置）。
2. 校验密钥字段非空，并提示是否仍为默认值（本地演练默认允许，`STRICT_DEFAULTS=1` 时强制阻断）。
3. 扫描仓库已追踪文件中的明显密钥泄露模式（AKIA/sk-/私钥头等）。
4. 验证权限回归：未登录访问 `system/permissions/tree` 必须被拦截（401/403）。
5. 验证审计可追溯：管理员登录后可访问 `/api/audit/logs` 与 `/api/audit/access-alerts/options`。

可选参数：

- `ENV_FILE=/path/to/.env.compose npm run release:security`
- `BASE_URL=http://127.0.0.1:29091/api npm run release:security`
- `STRICT_DEFAULTS=1 npm run release:security`

## 11. 数据备份

### 11.1 备份 MySQL

```bash
docker exec chatbi-local-mysql sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" chatbi' > chatbi-backup.sql
```

### 11.2 恢复 MySQL

```bash
cat chatbi-backup.sql | docker exec -i chatbi-local-mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" chatbi'
```

## 12. 常用运维命令

### 查看日志

```bash
docker logs -f chatbi-local-api
docker logs -f chatbi-local-web
docker logs -f chatbi-local-mysql
docker logs -f chatbi-local-redis
```

### 重启服务

```bash
docker compose --env-file chatbi-server/.env.compose -f chatbi-server/docker-compose.yml restart api
docker compose --env-file chatbi-server/.env.compose -f chatbi-server/docker-compose.yml restart web
```

### 停止服务

```bash
docker compose --env-file chatbi-server/.env.compose -f chatbi-server/docker-compose.yml down
```

### 删除服务和数据卷

```bash
docker compose --env-file chatbi-server/.env.compose -f chatbi-server/docker-compose.yml down -v
```

## 13. 常见问题

### 13.1 端口被占用

修改：

- `CHATBI_WEB_HOST_PORT`
- `CHATBI_API_HOST_PORT`
- `CHATBI_DB_HOST_PORT`
- `CHATBI_REDIS_HOST_PORT`

### 13.2 前端容器不健康

先看：

```bash
docker logs chatbi-local-web
```

当前版本已修复前端健康检查，正常情况下不应再因 `localhost` 探测失败而持续 `unhealthy`。

### 13.3 API 启动失败

先检查：

- MySQL 是否 healthy
- `.env.compose` 中数据库密码是否一致
- `JWT_SECRET` / `PASSWORD_SALT` 是否为空

然后看日志：

```bash
docker logs chatbi-local-api
```

### 13.4 Kimi 没生效

检查：

- `CHATBI_AI_ENABLED=true`
- `KIMI_ENABLED=true`
- `KIMI_API_KEY` 是否有效
- `/api/ai-model/status` 是否返回 `runtimeEnabled=true`

如果环境变量修改后仍未生效，执行：

```bash
docker compose --env-file chatbi-server/.env.compose -f chatbi-server/docker-compose.yml up -d --build api
```

## 14. 本方案当前已验证结果

截至 2026-03-11，当前 `docker-compose` 部署链路已验证：

- 完整容器栈可启动
- `mysql/api/redis/web` 全部 healthy
- 后端单元测试 `143 / 143` 通过
- 前端单元测试 `24 / 24` 通过
- API 冒烟测试通过
- Playwright 冒烟 `9 / 9` 通过
- Kimi 运行态可用
- AI 对话返回真实业务数据和 AI 解读
