# Deployment Guide

[English](./DEPLOYMENT.md) | [中文](./DEPLOYMENT.zh-CN.md)

This guide describes the practical deployment path for local demo and external environment rollout.

## Quick Start (Docker Compose)

1. Prepare env file:

```bash
cp chatbi-server/.env.compose.example chatbi-server/.env.compose
```

2. Start stack:

```bash
docker compose --env-file chatbi-server/.env.compose -f chatbi-server/docker-compose.yml up -d --build
```

3. Validate:

```bash
npm run quality:gate
```

## Default Access

- Web: `http://localhost:18090`
- API health: `http://localhost:19091/actuator/health`

## Notes

- Detailed operations (backup, rollback, drill, troubleshooting) are documented in [DEPLOYMENT.zh-CN.md](./DEPLOYMENT.zh-CN.md).
