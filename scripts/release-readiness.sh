#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RELEASE_PLAN_DOC="$ROOT_DIR/docs/RELEASE_CLOSURE_PLAN.md"
PROJECT_STATUS_DOC="$ROOT_DIR/docs/PROJECT_STATUS.md"
GAP_PRIORITY_DOC="$ROOT_DIR/docs/ENTERPRISE_CHATBI_GAP_PRIORITY.md"

echo "======================================"
echo "ChatBI Release Readiness Check"
echo "======================================"
echo "DATE: $(date '+%Y-%m-%d %H:%M:%S %z')"
echo "BRANCH: $(git -C "$ROOT_DIR" rev-parse --abbrev-ref HEAD)"
echo "COMMIT: $(git -C "$ROOT_DIR" rev-parse --short HEAD)"
echo "======================================"

for doc in "$RELEASE_PLAN_DOC" "$PROJECT_STATUS_DOC" "$GAP_PRIORITY_DOC"; do
  if [[ ! -f "$doc" ]]; then
    echo "[FAIL] Missing required doc: $doc"
    exit 1
  fi
  echo "[ OK ] Found doc: ${doc#$ROOT_DIR/}"
done

echo "[RUN ] Full quality gate"
(cd "$ROOT_DIR" && npm run quality:gate)

echo "======================================"
echo "Release readiness passed"
echo "======================================"
