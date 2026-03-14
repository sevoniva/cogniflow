# ChatBI Server

ChatBI 企业版后端服务，负责认证授权、指标管理、语义配置、AI 运行时、查询执行和分析看板接口。当前运行态默认使用 MySQL 8 持久化，不依赖 H2 或 mock 数据。

## 技术栈

- Spring Boot 3.1
- Spring Security + JWT
- MyBatis Plus
- MySQL 8
- Redis 7（本地可降级）
- Maven Wrapper

## 快速启动

### 方式一：完整容器栈

在仓库根目录执行：

```bash
export CHATBI_WEB_HOST_PORT=18090
export CHATBI_API_HOST_PORT=19091
export CHATBI_AI_ENABLED=true
export KIMI_ENABLED=true
export KIMI_API_KEY=你的KimiKey

docker compose -f chatbi-server/docker-compose.yml up -d --build
```

启动后默认地址：

- Web: `http://localhost:18090`
- API: `http://localhost:19091/api`
- 健康检查: `http://localhost:19091/actuator/health`
- MySQL: `127.0.0.1:3306`

如果首次构建依赖拉取较慢，可先设置代理：

```bash
export https_proxy=http://127.0.0.1:7897
export http_proxy=http://127.0.0.1:7897
export all_proxy=socks5://127.0.0.1:7897
```

### 方式二：本地 Spring Boot 开发

先启动 MySQL：

```bash
docker compose up -d mysql
```

再启动后端：

```bash
./mvnw spring-boot:run
```

如需改端口：

```bash
SERVER_PORT=19081 ./mvnw spring-boot:run
```

## 运行配置

默认 `dev` 配置使用本地 MySQL：

- Host: `127.0.0.1`
- Port: `3306`
- Database: `chatbi`
- Username: `chatbi`
- Password: `ChatBI@2026`

关键环境变量：

- `SERVER_PORT`: 后端端口，默认 `8081`
- `DB_HOST/DB_PORT/DB_NAME/DB_USERNAME/DB_PASSWORD`: MySQL 连接参数
- `REDIS_HOST/REDIS_PORT/REDIS_PASSWORD`: Redis 连接参数
- `CHATBI_AI_ENABLED`: 是否启用 AI 运行时
- `KIMI_ENABLED`: 是否启用 Kimi 提供商
- `KIMI_API_KEY`: Kimi API Key
- `JWT_SECRET`: JWT 密钥
- `PASSWORD_SALT`: 密码盐

## 数据说明

- 运行态数据库为 MySQL，默认数据源为 `LOCAL_MYSQL`
- 启动时会自动执行 `src/main/resources/db/schema-dev-*-mysql.sql`
- 演示数据来自：
  - `security-demo.sql`
  - `platform-demo-mysql.sql`
  - `extra-demo-mysql.sql`
- 指标、同义词、AI 设置、查询历史和仪表板配置都会持久化到 MySQL
- H2 仅用于测试环境

## 默认账号

- 用户名：`admin`
- 密码：`Admin@123`

## AI 能力验证

当前版本已经验证 Kimi 真实链路可用。接口示例：

```bash
curl http://localhost:19091/api/ai-model/status
curl -X POST http://localhost:19091/api/conversation/message \
  -H 'Content-Type: application/json' \
  -d '{"message":"本月销售额","userId":1}'
```

预期：

- `/api/ai-model/status` 返回 `runtimeEnabled=true`
- `/api/conversation/message` 返回 `source=business-insight-ai`
- 返回内容包含真实业务数据和 AI 中文解读

## 测试命令

```bash
./mvnw test
```

最近一次本地全量执行结果：

- 后端单元测试：`143 / 143` 通过

API 冒烟：

```bash
BASE_URL=http://localhost:19091/api bash scripts/smoke-test.sh
```

最近一次冒烟覆盖：

- 登录鉴权
- 指标/同义词/数据源/订阅/分享
- AI 状态
- AI 对话
- 销售/运营/敏捷分析接口

## 常用排查

### API 健康检查失败

```bash
curl http://localhost:8081/actuator/health
```

### 查看容器状态

```bash
docker compose -f docker-compose.yml ps
```

### 查看后端日志

```bash
docker logs -f chatbi-local-api
```

## 相关文档

- [根目录 README](../README.md)
- [部署手册](../docs/DEPLOYMENT.md)
- [测试报告](../docs/TEST_REPORT.md)
- [开发指南](../docs/DEVELOPMENT_GUIDE.md)
