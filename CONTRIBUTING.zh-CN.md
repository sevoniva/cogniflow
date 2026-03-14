# 贡献指南

[English](./CONTRIBUTING.md) | [中文](./CONTRIBUTING.zh-CN.md)

感谢你参与本项目。

## 开发前提

1. Node.js 20+
2. JDK 17+
3. Docker / Docker Compose（建议用于 MySQL/Redis 与本地联调）

## 分支与提交规则

1. 新分支使用 `codex/` 前缀。
2. 每次提交聚焦单一目标。
3. 提交信息清晰可读，建议带作用域（`feat:`、`fix:`、`docs:`）。

## 本地验证（必做）

提交 PR 前必须执行：

```bash
npm run quality:gate
```

未通过门禁的改动不应合并。
