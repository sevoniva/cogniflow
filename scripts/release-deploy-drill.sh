#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="$ROOT_DIR/chatbi-server/docker-compose.yml"
ENV_FILE="${ENV_FILE:-$ROOT_DIR/chatbi-server/.env.compose}"
ENV_EXAMPLE="$ROOT_DIR/chatbi-server/.env.compose.example"
ROLLBACK_OVERRIDE=""
DRILL_CLEANUP="${DRILL_CLEANUP:-false}"

API_CONTAINER="chatbi-local-api"
WEB_CONTAINER="chatbi-local-web"
MYSQL_CONTAINER="chatbi-local-mysql"
REDIS_CONTAINER="chatbi-local-redis"

CHATBI_WEB_HOST_PORT=18090
CHATBI_API_HOST_PORT=19091
MYSQL_ROOT_PASSWORD=""
DB_NAME=""

read_env_var() {
  local key="$1"
  local value
  value="$(awk -F= -v target="$key" '
    $0 ~ "^[[:space:]]*" target "=" {
      sub(/^[[:space:]]*[^=]+=[[:space:]]*/, "", $0);
      print $0;
      exit 0;
    }
  ' "$ENV_FILE")"
  echo "$value"
}

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "[FAIL] Missing required command: $1"
    exit 1
  fi
}

compose() {
  docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" "$@"
}

cleanup() {
  if [[ -n "$ROLLBACK_OVERRIDE" && -f "$ROLLBACK_OVERRIDE" ]]; then
    rm -f "$ROLLBACK_OVERRIDE"
  fi
  if [[ "$DRILL_CLEANUP" == "true" ]]; then
    echo "[CLEANUP] DRILL_CLEANUP=true, stopping stack..."
    compose down
  fi
}

wait_http_ok() {
  local name="$1"
  local url="$2"
  local max_attempts="${3:-90}"
  local sleep_seconds="${4:-2}"

  for attempt in $(seq 1 "$max_attempts"); do
    if curl -fsS "$url" >/dev/null 2>&1; then
      echo "[ OK ] $name is ready: $url"
      return 0
    fi
    echo "[WAIT] $name not ready yet (${attempt}/${max_attempts}): $url"
    sleep "$sleep_seconds"
  done

  echo "[FAIL] $name readiness timeout: $url"
  return 1
}

wait_container_healthy() {
  local container="$1"
  local max_attempts="${2:-60}"
  local sleep_seconds="${3:-2}"

  for attempt in $(seq 1 "$max_attempts"); do
    local health_state
    health_state="$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "$container" 2>/dev/null || true)"
    if [[ "$health_state" == "healthy" || "$health_state" == "none" ]]; then
      echo "[ OK ] Container healthy: $container ($health_state)"
      return 0
    fi
    echo "[WAIT] Container health=$health_state (${attempt}/${max_attempts}): $container"
    sleep "$sleep_seconds"
  done

  echo "[FAIL] Container health timeout: $container"
  return 1
}

ensure_schema_compatibility() {
  local sql="
    SET @has_role_sort_order := (
      SELECT COUNT(1)
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = '${DB_NAME}'
        AND TABLE_NAME = 'sys_role'
        AND COLUMN_NAME = 'sort_order'
    );
    SET @ddl_role := IF(
      @has_role_sort_order = 0,
      'ALTER TABLE sys_role ADD COLUMN sort_order INT DEFAULT 0 COMMENT ''排序''',
      'SELECT 1'
    );
    PREPARE stmt_role FROM @ddl_role;
    EXECUTE stmt_role;
    DEALLOCATE PREPARE stmt_role;

    SET @has_perm_sort_order := (
      SELECT COUNT(1)
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = '${DB_NAME}'
        AND TABLE_NAME = 'sys_permission'
        AND COLUMN_NAME = 'sort_order'
    );
    SET @ddl_perm := IF(
      @has_perm_sort_order = 0,
      'ALTER TABLE sys_permission ADD COLUMN sort_order INT DEFAULT 0 COMMENT ''排序''',
      'SELECT 1'
    );
    PREPARE stmt_perm FROM @ddl_perm;
    EXECUTE stmt_perm;
    DEALLOCATE PREPARE stmt_perm;
  "
  echo "[STEP] Ensure schema compatibility for existing database volume"
  docker exec "$MYSQL_CONTAINER" \
    mysql -uroot "-p${MYSQL_ROOT_PASSWORD}" "${DB_NAME}" \
    -e "$sql"
}

assert_container_running() {
  local container="$1"
  local running
  running="$(docker inspect -f '{{.State.Running}}' "$container" 2>/dev/null || true)"
  if [[ "$running" != "true" ]]; then
    echo "[FAIL] Container is not running: $container"
    return 1
  fi
  echo "[ OK ] Container running: $container"
}

if [[ ! -f "$ENV_FILE" ]]; then
  if [[ -f "$ENV_EXAMPLE" ]]; then
    cp "$ENV_EXAMPLE" "$ENV_FILE"
    echo "[WARN] ENV file not found, generated from template: ${ENV_FILE#$ROOT_DIR/}"
  else
    echo "[FAIL] Missing env file: $ENV_FILE"
    exit 1
  fi
fi

require_cmd docker
require_cmd curl
require_cmd bash

if ! docker info >/dev/null 2>&1; then
  echo "[FAIL] Docker daemon is not running"
  exit 1
fi

CHATBI_WEB_HOST_PORT="$(read_env_var CHATBI_WEB_HOST_PORT || true)"
CHATBI_API_HOST_PORT="$(read_env_var CHATBI_API_HOST_PORT || true)"
MYSQL_ROOT_PASSWORD="$(read_env_var MYSQL_ROOT_PASSWORD || true)"
DB_NAME="$(read_env_var DB_NAME || true)"

CHATBI_WEB_HOST_PORT="${CHATBI_WEB_HOST_PORT:-18090}"
CHATBI_API_HOST_PORT="${CHATBI_API_HOST_PORT:-19091}"
MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-ChatBI@2026}"
DB_NAME="${DB_NAME:-chatbi}"

API_HEALTH_URL="http://127.0.0.1:${CHATBI_API_HOST_PORT}/actuator/health"
WEB_HEALTH_URL="http://127.0.0.1:${CHATBI_WEB_HOST_PORT}/"
API_BASE_URL="http://127.0.0.1:${CHATBI_API_HOST_PORT}/api"

trap cleanup EXIT

echo "======================================"
echo "ChatBI Release Deploy & Rollback Drill"
echo "======================================"
echo "DATE: $(date '+%Y-%m-%d %H:%M:%S %z')"
echo "ENV_FILE: $ENV_FILE"
echo "COMPOSE_FILE: $COMPOSE_FILE"
echo "API_BASE_URL: $API_BASE_URL"
echo "WEB_URL: http://127.0.0.1:${CHATBI_WEB_HOST_PORT}"
echo "======================================"

echo "[STEP] Baseline deploy from zero"
compose up -d --build mysql redis

assert_container_running "$MYSQL_CONTAINER"
assert_container_running "$REDIS_CONTAINER"
wait_container_healthy "$MYSQL_CONTAINER"
wait_container_healthy "$REDIS_CONTAINER"
ensure_schema_compatibility

compose up -d --build api web

assert_container_running "$API_CONTAINER"
assert_container_running "$WEB_CONTAINER"

wait_http_ok "API" "$API_HEALTH_URL"
wait_http_ok "Web" "$WEB_HEALTH_URL"

echo "[STEP] Baseline smoke checks"
(cd "$ROOT_DIR/chatbi-server" && BASE_URL="$API_BASE_URL" PERF_BUDGET_CHECKS=0 bash scripts/smoke-test.sh)

ts="$(date +%Y%m%d%H%M%S)"
BASELINE_API_TAG="chatbi-local-api:rollback-${ts}"
BASELINE_WEB_TAG="chatbi-local-web:rollback-${ts}"

BASELINE_API_IMAGE_ID="$(docker inspect --format '{{.Image}}' "$API_CONTAINER")"
BASELINE_WEB_IMAGE_ID="$(docker inspect --format '{{.Image}}' "$WEB_CONTAINER")"

docker image tag "$BASELINE_API_IMAGE_ID" "$BASELINE_API_TAG"
docker image tag "$BASELINE_WEB_IMAGE_ID" "$BASELINE_WEB_TAG"

echo "[INFO] Baseline image tags"
echo "       API => $BASELINE_API_TAG ($BASELINE_API_IMAGE_ID)"
echo "       WEB => $BASELINE_WEB_TAG ($BASELINE_WEB_IMAGE_ID)"

echo "[STEP] Simulate release upgrade (rebuild + recreate)"
compose up -d --build --force-recreate api web
wait_http_ok "API (after upgrade)" "$API_HEALTH_URL"
wait_http_ok "Web (after upgrade)" "$WEB_HEALTH_URL"

echo "[STEP] Post-upgrade smoke checks"
(cd "$ROOT_DIR/chatbi-server" && BASE_URL="$API_BASE_URL" PERF_BUDGET_CHECKS=0 bash scripts/smoke-test.sh)

ROLLBACK_OVERRIDE="$(mktemp "$ROOT_DIR/.release-rollback.override.XXXXXX.yml")"
cat > "$ROLLBACK_OVERRIDE" <<EOF
services:
  api:
    image: ${BASELINE_API_TAG}
    pull_policy: never
  web:
    image: ${BASELINE_WEB_TAG}
    pull_policy: never
EOF

echo "[STEP] Rollback to baseline image tags"
docker compose --env-file "$ENV_FILE" \
  -f "$COMPOSE_FILE" \
  -f "$ROLLBACK_OVERRIDE" \
  up -d --no-build --force-recreate api web

wait_http_ok "API (after rollback)" "$API_HEALTH_URL"
wait_http_ok "Web (after rollback)" "$WEB_HEALTH_URL"

echo "[STEP] Post-rollback smoke checks"
(cd "$ROOT_DIR/chatbi-server" && BASE_URL="$API_BASE_URL" PERF_BUDGET_CHECKS=0 bash scripts/smoke-test.sh)

echo "======================================"
echo "Release deploy & rollback drill passed"
echo "Rollback image tags:"
echo "  API: $BASELINE_API_TAG"
echo "  WEB: $BASELINE_WEB_TAG"
echo "======================================"
