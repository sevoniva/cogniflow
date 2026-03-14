# Architecture Overview

[English](./ARCHITECTURE.md) | [中文](./ARCHITECTURE.zh-CN.md)

Chat BI is organized as a frontend-backend split with explicit AI and chart governance capabilities.

## High-Level Components

- `src/`: Vue 3 frontend (chat workflow, BI dashboards, chart rendering)
- `chatbi-server/`: Spring Boot backend (query, auth, metadata, AI observability)
- `scripts/`: quality gate and release guardrail scripts
- `e2e/`: Playwright end-to-end checks

## Runtime Topology (Local)

- Frontend: Vite (`http://localhost:18090`)
- Backend: Spring Boot (`http://localhost:8080`)
- Optional infra: MySQL + Redis via Docker Compose

## Quality and Release Controls

- Unified gate: `npm run quality:gate`
- Release checks: `npm run release:readiness`, `npm run release:drill`, `npm run release:security`, `npm run release:support`

## Notes

- Detailed Chinese architecture design remains available at [ARCHITECTURE.zh-CN.md](./ARCHITECTURE.zh-CN.md).
