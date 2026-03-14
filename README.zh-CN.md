# Chat BI

[English](./README.md) | [中文](./README.zh-CN.md)

企业级 Chat BI 平台（Chat + BI 一体化）。

面向企业经营与运营分析场景，提供从自然语言提问、语义识别、SQL 治理、结果可视化到 AI 可观测与告警闭环的完整能力，支持本地开发、单机演示与对外发布。

## 目录

- [1. 产品概览](#1-产品概览)
- [2. 产品定位](#2-产品定位)
- [3. 核心能力总览](#3-核心能力总览)
- [4. 功能矩阵](#4-功能矩阵)
- [5. 技术架构](#5-技术架构)
- [6. 代码结构](#6-代码结构)
- [7. 安装与启动](#7-安装与启动)
- [8. 配置说明](#8-配置说明)
- [9. 部署说明](#9-部署说明)
- [10. 质量门禁与验收](#10-质量门禁与验收)
- [11. 常见问题排查](#11-常见问题排查)
- [12. 文档地图](#12-文档地图)
- [13. 开发与贡献](#13-开发与贡献)
- [14. Roadmap](#14-roadmap)
- [15. 开源治理](#15-开源治理)

## 1. 产品概览

Chat BI 是一个企业级智能分析系统，重点解决以下问题：

- 业务方不会写 SQL，无法快速拿到数据结论
- 数据分析链路长，问题追问成本高
- AI 结果“看起来聪明”，但不可治理、不可观测、不可审计
- 图表类型多但难管理，质量门禁缺失导致回归风险高

本项目当前版本已打通“真实数据查询 + AI 对话分析 + 图表展示 + 质量门禁”的主链路，并通过统一门禁脚本保证交付质量。

## 2. 产品定位

- 平台定位：企业级智能分析平台（Chat + BI）
- 使用对象：业务分析师、运营负责人、管理层、数据与研发团队
- 核心价值：
  - 降低分析门槛：自然语言直达数据
  - 提升分析效率：支持多轮追问、结果页续问
  - 保证企业可控：查询治理、审计、可观测、告警
  - 保障上线质量：单测/E2E/视觉/冒烟/性能/图表门禁一体化

## 3. 核心能力总览

- 智能查询链路：自然语言 -> 指标识别 -> SQL 治理 -> 数据返回 -> 图表渲染
- AI 对话链路：多轮上下文、失败可恢复、诊断信息透出
- 图表体系：119 图表类型，17 图表家族，支持结果页/对话页联动
- 企业治理：SQL 安全校验、权限注入、脱敏、审计追踪
- AI 可观测：状态、告警、历史、恢复事件、通知通道
- 发布门禁：`npm run quality:gate` 一键全量验证

## 4. 功能矩阵

| 功能域 | 子功能 | 当前状态 | 说明 |
|---|---|---|---|
| 智能查询 | 首页查询/结果页追问 | 已完成 | 真实数据查询可用 |
| 对话分析 | 多轮上下文追问 | 已完成 | 支持连续提问与上下文承接 |
| 可恢复机制 | 异常降级 + 诊断建议 | 已完成 | 不阻断会话链路 |
| 图表能力 | 119 类型 + 17 家族 | 已完成 | 支持多场景切换与联动 |
| 图表门禁 | types/coverage/invalid | 已完成 | `types>=100, coverage=100, invalid=0` |
| AI 可观测 | 状态/告警/历史/恢复 | 已完成 | 支持快照、历史与恢复留痕 |
| 安全治理 | SQL/权限/脱敏/审计 | 已完成 | 执行态治理，不是展示层假能力 |
| 仪表板 | 工作台/编辑器/嵌入页 | 已完成 | 支持可视化运营展示 |
| 质量门禁 | 单测/E2E/视觉/冒烟/性能 | 已完成 | CI 与本地门禁一致 |
| 高阶分析 | 跨图联动/钻取/故事线 | 规划中（P1） | 后续增强 |

## 5. 技术架构

### 5.1 前后端技术栈

| 层级 | 技术 |
|---|---|
| 前端 | Vue 3 + TypeScript + Vite + Element Plus + ECharts |
| 后端 | Spring Boot 3 + MyBatis Plus + MySQL 8 |
| 测试 | Vitest + Playwright + Maven Surefire |
| 运行依赖 | MySQL（本地通过 Docker Compose） |

### 5.2 逻辑链路

```text
用户问题
  -> 语义识别/指标匹配
  -> 查询治理(SQL 校验/权限/脱敏)
  -> 数据执行(MySQL)
  -> 结果分析与图表映射
  -> 对话/结果页展示
  -> 观测留痕与告警
```

## 6. 代码结构

```text
ChatBI/
├── src/                    # 前端源码
├── e2e/                    # Playwright E2E
├── scripts/                # 质量门禁与辅助脚本
├── chatbi-server/          # 后端服务
│   ├── src/main/java/      # Java 主代码
│   ├── src/main/resources/ # 配置与 SQL
│   ├── src/test/           # 后端测试
│   └── scripts/            # 冒烟脚本
├── docs/                   # 对外文档
└── README.md
```

## 7. 安装与启动

### 7.1 环境要求

- Node.js 20.x
- npm 10.x
- JDK 17
- Maven 3.8+
- MySQL 8（推荐 Docker 方式）
- Docker 24+ / Docker Compose v2+

### 7.2 启动依赖（MySQL）

```bash
cd chatbi-server
docker compose up -d mysql
```

默认连接信息：
- DB: `chatbi`
- User: `chatbi`
- Password: `ChatBI@2026`

### 7.3 启动后端

```bash
cd chatbi-server
./mvnw spring-boot:run
```

默认地址：
- API: `http://localhost:8081/api`
- Health: `http://localhost:8081/actuator/health`

### 7.4 启动前端

```bash
npm install
npm run dev
```

默认地址：`http://localhost:8080`

### 7.5 首次自检（建议顺序）

1. 打开首页，执行“本月销售额”查询
2. 进入对话页，做 2-3 轮连续追问
3. 打开图表市场，确认图表能力可浏览
4. 打开管理台 AI 页面，确认可观测面板可读

## 8. 配置说明

### 8.1 前端关键配置

- `VITE_DEV_PROXY_TARGET`：开发代理后端地址（默认 `http://localhost:19091`）

示例：

```bash
VITE_DEV_PROXY_TARGET=http://localhost:8081 npm run dev
```

### 8.2 后端关键配置（环境变量）

- `SERVER_PORT`
- `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USERNAME` / `DB_PASSWORD`
- `chatbi.ai.observability.*`（AI 可观测阈值与通知）

## 9. 部署说明

- 本地开发：前后端本地启动 + Docker MySQL
- 单机演示：使用 `chatbi-server/docker-compose.yml`
- 对外发布建议：
  - 外层反向代理 + HTTPS
  - 独立数据库与密钥管理
  - 按环境拆分配置

## 10. 质量门禁与验收

### 10.1 一键门禁

```bash
npm run quality:gate
```

### 10.2 门禁覆盖项

- 前端单测 + 覆盖率阈值
- 前端类型检查
- 后端单测
- 语义基准回归
- Playwright E2E
- 视觉基线检查
- API 冒烟
- 图表硬门禁
- 性能预算门禁

### 10.3 当前门禁硬指标

- 图表验证：`validatedTypes>=100`、`coverageRate>=100`、`invalid=0`
- 当前已验证基线：`validatedTypes=119`、`coverageRate=100`、`invalid=0`

## 11. 常见问题排查

### 11.1 前端打开 404 或空白

- 确认在项目根目录执行 `npm run dev`
- 确认 `index.html` 存在
- 确认端口未冲突

### 11.2 E2E 报连接失败（`ERR_CONNECTION_REFUSED`）

先单独启动前端服务：

```bash
npm run dev -- --host 127.0.0.1 --port 18090
```

再执行：

```bash
npm run quality:gate
```

### 11.3 后端连不上数据库

- 检查 `docker compose up -d mysql` 是否成功
- 检查数据库连接环境变量与口令是否一致

## 12. 文档地图

- 文档导航：`docs/README.md`（英文）/ `docs/README.zh-CN.md`（中文）
- 部署：`docs/DEPLOYMENT.md` / `docs/DEPLOYMENT.zh-CN.md`
- 架构：`docs/ARCHITECTURE.md` / `docs/ARCHITECTURE.zh-CN.md`
- 元数据字典：`docs/DATA_DICTIONARY.md` / `docs/DATA_DICTIONARY.zh-CN.md`
- API：`docs/API.md` / `docs/API.zh-CN.md`
- 开发指南：`docs/DEVELOPMENT_GUIDE.md` / `docs/DEVELOPMENT_GUIDE.zh-CN.md`
- 测试指南：`docs/TESTING_GUIDE.md` / `docs/TESTING_GUIDE.zh-CN.md`
- 用户手册：`docs/USER_GUIDE.md` / `docs/USER_GUIDE.zh-CN.md`
- 版本说明：`CHANGELOG.md` / `CHANGELOG.zh-CN.md`

## 13. 开发与贡献

### 13.1 常用命令

前端：

```bash
npm run dev
npm run type-check
npm run test -- --run
npm run build
```

后端：

```bash
cd chatbi-server
./mvnw test
./mvnw spring-boot:run
```

全量门禁：

```bash
npm run quality:gate
```

### 13.2 贡献流程

1. Fork / 新建分支
2. 完成开发与测试
3. 通过 `quality:gate`
4. 提交 PR

## 14. Roadmap

### P1（短中期）

1. 图表分析产品化增强（跨图联动/钻取/批注/故事线）
2. 跨页面交互一致性收敛（设计令牌、空态/错误态）
3. 性能治理可视化（趋势看板与阈值告警）

### P2（中长期）

1. 多租户与组织级隔离
2. 跨数据源编排与血缘追踪
3. 高可用与灾备演练
4. 企业级集成（SSO/LDAP/细粒度 ABAC）

## 15. 开源治理

- License: `Apache-2.0`（见 `LICENSE`）
- Security：`SECURITY.md` / `SECURITY.zh-CN.md`
- Contributing：`CONTRIBUTING.md` / `CONTRIBUTING.zh-CN.md`
- Code of Conduct：`CODE_OF_CONDUCT.md` / `CODE_OF_CONDUCT.zh-CN.md`
