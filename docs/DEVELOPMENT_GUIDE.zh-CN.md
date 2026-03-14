# Language | 语言

[English](./DEVELOPMENT_GUIDE.md) | [中文](./DEVELOPMENT_GUIDE.zh-CN.md)

# ChatBI 开发指南

**版本**: 2.0.0-ENTERPRISE
**更新日期**: 2026-03-11

## 1. 开发环境

### 1.1 前置要求

- JDK 17+
- Node.js 18+
- Docker / Docker Compose
- Git
- 可选：Kimi API Key

### 1.2 推荐启动方式

后端、数据库和前端联调，优先使用当前仓库内置的 MySQL + 容器栈：

```bash
export CHATBI_WEB_HOST_PORT=18090
export CHATBI_API_HOST_PORT=19091
export CHATBI_AI_ENABLED=true
export KIMI_ENABLED=true
export KIMI_API_KEY=你的KimiKey

docker compose -f chatbi-server/docker-compose.yml up -d --build
```

如果只做代码开发：

```bash
cd chatbi-server
docker compose up -d mysql
./mvnw spring-boot:run

cd ..
npm install
npm run dev
```

## 2. 当前运行基线

- 前端：Vue 3 + TypeScript + Vite + Element Plus + ECharts
- 后端：Spring Boot + MyBatis Plus + MySQL
- 运行态数据库：MySQL
- 测试数据库：H2
- HTTP 客户端：原生 `fetch`
- 系统运行态不允许使用 mock 数据

## 3. 关键目录

```text
src/                       前端源码
e2e/                       Playwright 冒烟测试
chatbi-server/src/main/    后端源码
chatbi-server/src/test/    后端测试
chatbi-server/scripts/     脚本与 API 冒烟
```

## 4. 数据与 AI 约束

### 4.1 数据约束

- 运行态默认数据源为 `LOCAL_MYSQL`
- 指标、同义词、AI 设置和查询历史必须落库
- 演示数据来自 SQL 初始化脚本，不允许前端静态 mock

### 4.2 AI 约束

- 优先命中真实业务指标和同义词
- 指标问答命中后再触发 AI 解读
- 外部模型不可用时可以降级解释，但不能让查询链路不可用

## 5. SQL 开发约束

分析看板 SQL 需要兼容 MySQL 与测试环境 H2，避免直接写死单一方言函数。当前项目统一要求：

- 日期分桶通过 `SqlDialectHelper` 生成
- 日期别名不要使用保留字，如 `date`、`month`
- 涉及加减天或月份差，优先使用可移植表达式

最近一次修复已覆盖：

- 销售趋势接口
- 用户活跃趋势接口
- 用户注册趋势接口
- 用户留存分析接口
- 经营洞察服务

## 6. 本地验证命令

### 6.1 前端

```bash
npm run type-check
npm run test -- --run
npm run build
```

### 6.2 后端

```bash
cd chatbi-server
./mvnw -B test
```

### 6.3 API 冒烟

```bash
cd ..
BASE_URL=http://localhost:19091/api bash chatbi-server/scripts/smoke-test.sh
```

### 6.4 页面冒烟

```bash
BASE_URL=http://localhost:18090 npx playwright test e2e/smoke.spec.ts --project=chromium
```

## 7. Git 规范

```bash
git config user.name "Carson"
git config user.email "chuncheng.carson@gmail.com"
```

提交信息建议使用中文，并按变更类型组织，例如：

- `fix(chatbi): 修复分析看板 SQL 兼容性问题`
- `docs(chatbi): 更新部署与测试文档`

## 8. 最近一次验收结果

2026-03-11 本地执行结果：

- 后端单测：`143 / 143` 通过
- 前端单测：`24 / 24` 通过
- 前端构建：通过
- API 冒烟：通过
- Playwright 冒烟：`9 / 9` 通过
- Kimi 运行态：已验证可用
