# Chat BI

[English](./README.md) | [中文](./README.zh-CN.md)

Enterprise-grade Chat BI platform (Chat + BI in one).

Chat BI provides an end-to-end analytics workflow from natural language questions to governed SQL, chart rendering, AI diagnostics, and observability/alerting, suitable for local development, demo environments, and production rollout.

## Table of Contents

- [1. Product Overview](#1-product-overview)
- [2. Product Positioning](#2-product-positioning)
- [3. Capability Highlights](#3-capability-highlights)
- [4. Feature Matrix](#4-feature-matrix)
- [5. Architecture](#5-architecture)
- [6. Project Structure](#6-project-structure)
- [7. Installation & Startup](#7-installation--startup)
- [8. Configuration](#8-configuration)
- [9. Deployment Modes](#9-deployment-modes)
- [10. Quality Gate & Release Acceptance](#10-quality-gate--release-acceptance)
- [11. Troubleshooting](#11-troubleshooting)
- [12. Documentation Map](#12-documentation-map)
- [13. Development & Contribution](#13-development--contribution)
- [14. Roadmap](#14-roadmap)
- [15. Open Source Governance](#15-open-source-governance)

## 1. Product Overview

Chat BI is designed to solve enterprise analytics bottlenecks:

- Business users cannot write SQL efficiently.
- Analysis cycles are long and follow-up questions are expensive.
- AI outputs are hard to govern, audit, and observe.
- Chart ecosystems grow fast but lack quality gates.

Current release baseline already includes a real data query path, multi-turn AI analysis, chart rendering, and strict quality gates.

## 2. Product Positioning

- Platform type: Enterprise intelligent analytics (Chat + BI)
- Primary users: Business analysts, operations managers, leadership, data/engineering teams
- Core values:
  - Lower analytics threshold with natural language interaction
  - Increase analysis velocity via multi-turn conversations
  - Ensure enterprise control with governance, auditing, and observability
  - Protect release quality with unified quality gates

## 3. Capability Highlights

- Smart query pipeline: NL question -> metric matching -> SQL governance -> data response -> chart output
- Conversational analytics: multi-turn context, recovery mode, actionable diagnostics
- Chart system: 119 chart types across 17 families
- Enterprise governance: SQL safety, permission injection, masking, audit traceability
- AI observability: runtime status, alerts, history, recovery events, notification channels
- Release quality gate: `npm run quality:gate`

## 4. Feature Matrix

| Domain | Capability | Status | Notes |
|---|---|---|---|
| Smart Query | Home query + follow-up from result page | Done | Real data path enabled |
| AI Conversation | Multi-turn context | Done | Continuous analysis supported |
| Recovery | Fallback + diagnosis hints | Done | Non-blocking failure recovery |
| Charts | 119 chart types / 17 families | Done | Market + conversation/result usage |
| Chart Gate | types/coverage/invalid validation | Done | `types>=100, coverage=100, invalid=0` |
| AI Observability | status/alerts/history/recovery | Done | Snapshot + history + event trace |
| Security Governance | SQL/permission/masking/audit | Done | Runtime-level governance |
| Dashboard | workspace/editor/embed | Done | End-to-end dashboard workflow |
| Quality Gate | unit/E2E/visual/smoke/perf | Done | Local and CI aligned |
| Advanced Analytics | cross-chart linkage/drill/storyline | Planned (P1) | Next-stage enhancement |

## 5. Architecture

### 5.1 Tech Stack

| Layer | Technology |
|---|---|
| Frontend | Vue 3 + TypeScript + Vite + Element Plus + ECharts |
| Backend | Spring Boot 3 + MyBatis Plus + MySQL 8 |
| Testing | Vitest + Playwright + Maven Surefire |
| Runtime dependency | MySQL (Docker Compose for local setup) |

### 5.2 Logical Flow

```text
User Question
  -> Semantic matching / metric resolution
  -> Query governance (SQL validation / permission / masking)
  -> Query execution (MySQL)
  -> Result analysis + chart mapping
  -> Conversation / result-page rendering
  -> Observability + alerting trace
```

## 6. Project Structure

```text
ChatBI/
├── src/                    # Frontend source
├── e2e/                    # Playwright E2E
├── scripts/                # Quality gate and utility scripts
├── chatbi-server/          # Backend service
│   ├── src/main/java/      # Java source
│   ├── src/main/resources/ # Config and SQL
│   ├── src/test/           # Backend tests
│   └── scripts/            # Smoke test scripts
├── docs/                   # Public docs
├── README.md               # English (default)
└── README.zh-CN.md         # Chinese
```

## 7. Installation & Startup

### 7.1 Prerequisites

- Node.js 20.x
- npm 10.x
- JDK 17
- Maven 3.8+
- MySQL 8 (Docker recommended)
- Docker 24+ / Docker Compose v2+

### 7.2 Start MySQL

```bash
cd chatbi-server
docker compose up -d mysql
```

Default database credentials:
- DB: `chatbi`
- User: `chatbi`
- Password: `ChatBI@2026`

### 7.3 Start Backend

```bash
cd chatbi-server
./mvnw spring-boot:run
```

Default endpoints:
- API: `http://localhost:8081/api`
- Health: `http://localhost:8081/actuator/health`

### 7.4 Start Frontend

```bash
npm install
npm run dev
```

Default URL: `http://localhost:8080`

### 7.5 Quick Sanity Path

1. Query `monthly sales` on home page
2. Open conversation page and ask follow-up questions
3. Open chart market and validate chart availability
4. Open Admin AI page and check observability panel

## 8. Configuration

### 8.1 Frontend

- `VITE_DEV_PROXY_TARGET`: backend proxy target in dev mode (default `http://localhost:19091`)

Example:

```bash
VITE_DEV_PROXY_TARGET=http://localhost:8081 npm run dev
```

### 8.2 Backend

- `SERVER_PORT`
- `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USERNAME` / `DB_PASSWORD`
- `chatbi.ai.observability.*` (threshold and notification policies)

## 9. Deployment Modes

- Local dev: local FE/BE + Docker MySQL
- Single-host demo: `chatbi-server/docker-compose.yml`
- External release recommendation:
  - Reverse proxy + HTTPS
  - Dedicated database and secret management
  - Environment-specific configuration isolation

## 10. Quality Gate & Release Acceptance

### 10.1 One-command gate

```bash
npm run quality:gate
```

### 10.2 Gate coverage

- Frontend unit tests + coverage threshold
- Frontend type check
- Backend unit tests
- Semantic benchmark regression
- Playwright E2E
- Visual baseline
- API smoke tests
- Chart hard gate
- Performance budget gate

### 10.3 Chart hard gate baseline

- Required: `validatedTypes>=100`, `coverageRate>=100`, `invalid=0`
- Current baseline: `validatedTypes=119`, `coverageRate=100`, `invalid=0`

## 11. Troubleshooting

### 11.1 Frontend shows 404/blank page

- Ensure `npm run dev` runs at repository root
- Ensure `index.html` exists
- Ensure the target port is not occupied

### 11.2 E2E fails with `ERR_CONNECTION_REFUSED`

Start frontend explicitly:

```bash
npm run dev -- --host 127.0.0.1 --port 18090
```

Then rerun:

```bash
npm run quality:gate
```

### 11.3 Backend cannot connect to DB

- Verify `docker compose up -d mysql` succeeded
- Check DB env vars and credentials

## 12. Documentation Map

- Docs hub: `docs/README.md` (English) / `docs/README.zh-CN.md` (Chinese)
- Deployment: `docs/DEPLOYMENT.md` / `docs/DEPLOYMENT.zh-CN.md`
- Architecture: `docs/ARCHITECTURE.md` / `docs/ARCHITECTURE.zh-CN.md`
- Data dictionary: `docs/DATA_DICTIONARY.md` / `docs/DATA_DICTIONARY.zh-CN.md`
- API: `docs/API.md` / `docs/API.zh-CN.md`
- Development guide: `docs/DEVELOPMENT_GUIDE.md` / `docs/DEVELOPMENT_GUIDE.zh-CN.md`
- Testing guide: `docs/TESTING_GUIDE.md` / `docs/TESTING_GUIDE.zh-CN.md`
- User guide: `docs/USER_GUIDE.md` / `docs/USER_GUIDE.zh-CN.md`
- Release notes: `CHANGELOG.md` / `CHANGELOG.zh-CN.md`

## 13. Development & Contribution

### 13.1 Common commands

Frontend:

```bash
npm run dev
npm run type-check
npm run test -- --run
npm run build
```

Backend:

```bash
cd chatbi-server
./mvnw test
./mvnw spring-boot:run
```

Full quality gate:

```bash
npm run quality:gate
```

### 13.2 Contribution flow

1. Fork / create branch
2. Implement and test
3. Ensure `quality:gate` passes
4. Submit PR

## 14. Roadmap

### P1 (Short to Mid-term)

1. Advanced chart productization (cross-chart linkage/drill/storyline)
2. Cross-page interaction consistency (design tokens, empty/error states)
3. Performance governance visualization (trend dashboard + threshold alerts)

### P2 (Mid to Long-term)

1. Multi-tenant and org-level isolation
2. Cross-data-source orchestration and lineage
3. High availability and DR drills
4. Enterprise integration (SSO/LDAP/fine-grained ABAC)

## 15. Open Source Governance

- License: `Apache-2.0` (see `LICENSE`)
- Security: `SECURITY.md` / `SECURITY.zh-CN.md`
- Contributing: `CONTRIBUTING.md` / `CONTRIBUTING.zh-CN.md`
- Code of Conduct: `CODE_OF_CONDUCT.md` / `CODE_OF_CONDUCT.zh-CN.md`
