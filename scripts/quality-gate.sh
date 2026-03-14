#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SERVER_DIR="$ROOT_DIR/chatbi-server"

FRONTEND_TESTS="${FRONTEND_TESTS:-1}"
FRONTEND_COVERAGE="${FRONTEND_COVERAGE:-1}"
BACKEND_TESTS="${BACKEND_TESTS:-1}"
E2E_TESTS="${E2E_TESTS:-1}"
VISUAL_BASELINE_CHECKS="${VISUAL_BASELINE_CHECKS:-1}"
SMOKE_TESTS="${SMOKE_TESTS:-1}"
SMOKE_BASE_URL="${SMOKE_BASE_URL:-http://localhost:19091/api}"
E2E_TIMEOUT_SECONDS="${E2E_TIMEOUT_SECONDS:-900}"
CHART_VALIDATION_MIN_TYPES="${CHART_VALIDATION_MIN_TYPES:-100}"
CHART_VALIDATION_MIN_COVERAGE="${CHART_VALIDATION_MIN_COVERAGE:-100}"
PERF_BUDGET_CHECKS="${PERF_BUDGET_CHECKS:-1}"
PERF_BUDGET_HEALTH_MS="${PERF_BUDGET_HEALTH_MS:-800}"
PERF_BUDGET_QUERY_MS="${PERF_BUDGET_QUERY_MS:-2200}"
PERF_BUDGET_CONVERSATION_MS="${PERF_BUDGET_CONVERSATION_MS:-2500}"
VISUAL_BASELINE_LAYOUT_TOLERANCE="${VISUAL_BASELINE_LAYOUT_TOLERANCE:-16}"
SEMANTIC_BENCHMARK_CHECKS="${SEMANTIC_BENCHMARK_CHECKS:-1}"
SEMANTIC_BENCHMARK_MAX_DROP_PERCENT="${SEMANTIC_BENCHMARK_MAX_DROP_PERCENT:-5}"

ensure_java17_for_backend() {
  if [[ "$BACKEND_TESTS" != "1" ]]; then
    return
  fi

  local current_java major
  current_java="$(mvn -v 2>/dev/null | awk -F': ' '/Java version/ {print $2}' | cut -d'.' -f1 | tr -dc '0-9')"
  if [[ -n "$current_java" && "$current_java" -le 17 ]]; then
    return
  fi

  if [[ -z "${JAVA_HOME:-}" && -d "/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home" ]]; then
    export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
    export PATH="$JAVA_HOME/bin:$PATH"
    echo "[INFO] Switched JAVA_HOME to Homebrew openjdk@17 for backend checks"
  fi
}

print_header() {
  echo "======================================"
  echo "ChatBI Quality Gate"
  echo "======================================"
  echo "ROOT: $ROOT_DIR"
  echo "SMOKE_BASE_URL: $SMOKE_BASE_URL"
  echo "JAVA_HOME: ${JAVA_HOME:-<unset>}"
  echo "FRONTEND_TESTS=$FRONTEND_TESTS FRONTEND_COVERAGE=$FRONTEND_COVERAGE BACKEND_TESTS=$BACKEND_TESTS E2E_TESTS=$E2E_TESTS VISUAL_BASELINE_CHECKS=$VISUAL_BASELINE_CHECKS SMOKE_TESTS=$SMOKE_TESTS E2E_TIMEOUT_SECONDS=$E2E_TIMEOUT_SECONDS"
  echo "CHART_VALIDATION_MIN_TYPES=$CHART_VALIDATION_MIN_TYPES CHART_VALIDATION_MIN_COVERAGE=$CHART_VALIDATION_MIN_COVERAGE"
  echo "PERF_BUDGET_CHECKS=$PERF_BUDGET_CHECKS PERF_BUDGET_HEALTH_MS=$PERF_BUDGET_HEALTH_MS PERF_BUDGET_QUERY_MS=$PERF_BUDGET_QUERY_MS PERF_BUDGET_CONVERSATION_MS=$PERF_BUDGET_CONVERSATION_MS"
  echo "VISUAL_BASELINE_LAYOUT_TOLERANCE=$VISUAL_BASELINE_LAYOUT_TOLERANCE"
  echo "SEMANTIC_BENCHMARK_CHECKS=$SEMANTIC_BENCHMARK_CHECKS SEMANTIC_BENCHMARK_MAX_DROP_PERCENT=$SEMANTIC_BENCHMARK_MAX_DROP_PERCENT"
  echo "======================================"
}

run_frontend_checks() {
  if [[ "$FRONTEND_TESTS" != "1" ]]; then
    echo "[SKIP] Frontend tests"
    return
  fi
  if [[ "$FRONTEND_COVERAGE" == "1" ]]; then
    echo "[RUN ] Frontend unit tests with coverage thresholds"
    (cd "$ROOT_DIR" && npm run test:coverage -- --run)
  else
    echo "[RUN ] Frontend unit tests"
    (cd "$ROOT_DIR" && npm run test -- --run)
  fi
  echo "[RUN ] Frontend type check"
  (cd "$ROOT_DIR" && npm run type-check)
}

run_backend_checks() {
  if [[ "$BACKEND_TESTS" != "1" ]]; then
    echo "[SKIP] Backend tests"
    return
  fi
  echo "[RUN ] Backend unit tests"
  (cd "$SERVER_DIR" && mvn test)
}

run_e2e_checks() {
  if [[ "$E2E_TESTS" != "1" ]]; then
    echo "[SKIP] E2E tests"
    return
  fi
  echo "[RUN ] Playwright E2E"
  if command -v timeout >/dev/null 2>&1; then
    (cd "$ROOT_DIR" && timeout "$E2E_TIMEOUT_SECONDS" npm run test:e2e -- --reporter=line)
    return
  fi
  if command -v gtimeout >/dev/null 2>&1; then
    (cd "$ROOT_DIR" && gtimeout "$E2E_TIMEOUT_SECONDS" npm run test:e2e -- --reporter=line)
    return
  fi
  (cd "$ROOT_DIR" && npm run test:e2e -- --reporter=line)
}

run_visual_baseline_checks() {
  if [[ "$VISUAL_BASELINE_CHECKS" != "1" ]]; then
    echo "[SKIP] Visual baseline checks"
    return
  fi
  echo "[RUN ] Visual baseline checks"
  (cd "$ROOT_DIR" && VISUAL_BASELINE_BASE_URL="${BASE_URL:-http://127.0.0.1:18090}" VISUAL_BASELINE_LAYOUT_TOLERANCE="$VISUAL_BASELINE_LAYOUT_TOLERANCE" node scripts/visual-baseline-check.mjs)
}

run_smoke_checks() {
  if [[ "$SMOKE_TESTS" != "1" ]]; then
    echo "[SKIP] Smoke tests"
    return
  fi
  echo "[RUN ] API smoke tests"
  (cd "$SERVER_DIR" && BASE_URL="$SMOKE_BASE_URL" CHART_VALIDATION_MIN_TYPES="$CHART_VALIDATION_MIN_TYPES" CHART_VALIDATION_MIN_COVERAGE="$CHART_VALIDATION_MIN_COVERAGE" PERF_BUDGET_CHECKS="$PERF_BUDGET_CHECKS" PERF_BUDGET_HEALTH_MS="$PERF_BUDGET_HEALTH_MS" PERF_BUDGET_QUERY_MS="$PERF_BUDGET_QUERY_MS" PERF_BUDGET_CONVERSATION_MS="$PERF_BUDGET_CONVERSATION_MS" bash scripts/smoke-test.sh)
}

run_semantic_benchmark_checks() {
  if [[ "$SEMANTIC_BENCHMARK_CHECKS" != "1" ]]; then
    echo "[SKIP] Semantic benchmark checks"
    return
  fi
  echo "[RUN ] Semantic benchmark report"
  (cd "$ROOT_DIR" && SEMANTIC_BENCHMARK_MAX_DROP_PERCENT="$SEMANTIC_BENCHMARK_MAX_DROP_PERCENT" bash scripts/semantic-benchmark-report.sh)
}

ensure_java17_for_backend
print_header
run_frontend_checks
run_backend_checks
run_semantic_benchmark_checks
run_e2e_checks
run_visual_baseline_checks
run_smoke_checks

echo "======================================"
echo "Quality Gate Passed"
echo "======================================"
