#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RELEASE_NOTES="$ROOT_DIR/docs/RELEASE_NOTES_BETA.md"
RUNBOOK_DOC="$ROOT_DIR/docs/INCIDENT_SUPPORT_RUNBOOK.md"

check_file_exists() {
  local file="$1"
  if [[ ! -f "$file" ]]; then
    echo "[FAIL] Missing required file: ${file#$ROOT_DIR/}"
    exit 1
  fi
  echo "[ OK ] Found file: ${file#$ROOT_DIR/}"
}

check_required_phrase() {
  local file="$1"
  local phrase="$2"
  if ! rg -q "$phrase" "$file"; then
    echo "[FAIL] Missing required content in ${file#$ROOT_DIR/}: $phrase"
    exit 1
  fi
  echo "[ OK ] Content check passed: $phrase"
}

echo "======================================"
echo "ChatBI Release Support Check"
echo "======================================"

check_file_exists "$RELEASE_NOTES"
check_file_exists "$RUNBOOK_DOC"

echo "[STEP] Validate release notes sections"
check_required_phrase "$RELEASE_NOTES" "能力范围"
check_required_phrase "$RELEASE_NOTES" "已知限制"
check_required_phrase "$RELEASE_NOTES" "升级路径"

echo "[STEP] Validate incident support runbook sections"
check_required_phrase "$RUNBOOK_DOC" "AI 上游异常"
check_required_phrase "$RUNBOOK_DOC" "数据库连接异常"
check_required_phrase "$RUNBOOK_DOC" "E2E 回归失败"
check_required_phrase "$RUNBOOK_DOC" "值班与升级联系人"

echo "======================================"
echo "Release support check passed"
echo "======================================"
