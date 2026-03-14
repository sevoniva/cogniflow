# Development Guide

[English](./DEVELOPMENT_GUIDE.md) | [中文](./DEVELOPMENT_GUIDE.zh-CN.md)

## Prerequisites

- Node.js 20+
- JDK 17+
- Docker / Docker Compose

## Typical Local Workflow

1. Start backend + dependencies:

```bash
docker compose --env-file chatbi-server/.env.compose -f chatbi-server/docker-compose.yml up -d --build
```

2. Start frontend:

```bash
npm install
npm run dev -- --host 127.0.0.1 --port 18090
```

3. Run full checks:

```bash
npm run quality:gate
```

## Notes

- For existing Chinese details, see [DEVELOPMENT_GUIDE.zh-CN.md](./DEVELOPMENT_GUIDE.zh-CN.md).
