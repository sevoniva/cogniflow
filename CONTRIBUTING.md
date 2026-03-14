[English](./CONTRIBUTING.md) | [中文](./CONTRIBUTING.zh-CN.md)

# Contributing Guide

Thanks for contributing to this repository.

## Development Prerequisites

1. Node.js 20+
2. JDK 17+
3. Docker / Docker Compose (recommended for MySQL/Redis and integrated local run)

## Branch and Commit Rules

1. Create branches with `codex/` prefix.
2. Keep each commit focused on one objective.
3. Use clear commit messages, preferably with scope (`feat:`, `fix:`, `docs:`).

## Local Validation (Required)

Before opening a PR, run:

```bash
npm run quality:gate
```

PRs failing quality gate should not be merged.

## Pull Request Checklist

1. No hard-coded secrets, tokens, or private keys.
2. No runtime artifacts committed (logs, test reports, temporary files).
3. Documentation updated for behavior changes.
4. Backward compatibility and migration impact explained (if applicable).

## Code of Conduct

By participating, you agree to follow `CODE_OF_CONDUCT.md`.
