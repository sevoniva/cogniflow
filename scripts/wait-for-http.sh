#!/usr/bin/env bash

set -euo pipefail

URL="${1:-}"
TIMEOUT_SECONDS="${2:-120}"

if [[ -z "$URL" ]]; then
  echo "Usage: $0 <url> [timeout_seconds]"
  exit 1
fi

START_TIME="$(date +%s)"

while true; do
  if curl -fsS "$URL" >/dev/null 2>&1; then
    echo "[ready] $URL"
    exit 0
  fi

  NOW="$(date +%s)"
  ELAPSED=$((NOW - START_TIME))
  if (( ELAPSED >= TIMEOUT_SECONDS )); then
    echo "[timeout] $URL not ready after ${TIMEOUT_SECONDS}s"
    exit 1
  fi

  sleep 2
done
