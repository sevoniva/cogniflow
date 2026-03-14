# Testing Guide

[English](./TESTING_GUIDE.md) | [中文](./TESTING_GUIDE.zh-CN.md)

## Test Layers

- Frontend unit/integration: `npm run test -- --run`
- Backend unit/integration: `cd chatbi-server && ./mvnw test`
- E2E: `npm run test:e2e`
- Unified release gate: `npm run quality:gate`

## CI Gate Principle

A change is considered releasable only when `quality:gate` passes.

## Notes

- For expanded Chinese examples and troubleshooting, see [TESTING_GUIDE.zh-CN.md](./TESTING_GUIDE.zh-CN.md).
