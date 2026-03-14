# API Reference

[English](./API.md) | [中文](./API.zh-CN.md)

This document defines external API conventions and representative endpoints for Chat BI.

## Base URLs

- Local dev via backend direct: `http://localhost:8080/api`
- Local dev via frontend proxy: `/api` (frontend at `http://localhost:18090`)
- Production example: `https://<your-domain>/api`

## Response Contract

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1700000000000,
  "traceId": "abc123"
}
```

## Core Endpoint Groups

- `POST /api/auth/*`: authentication and token refresh
- `GET|POST /api/query/*`: query execution, diagnostics, and history
- `GET /api/chart-catalog/*`: chart catalog and validation metadata
- `GET|POST /api/ai-model/*`: AI model configuration and observability
- `GET /api/health/*`: health and readiness probes

## Notes

- API is served by `chatbi-server`.
- For full field-level Chinese reference, see [API.zh-CN.md](./API.zh-CN.md).
