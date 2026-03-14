#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ENV_FILE:-$ROOT_DIR/chatbi-server/.env.compose}"
ENV_EXAMPLE="$ROOT_DIR/chatbi-server/.env.compose.example"
BASE_URL="${BASE_URL:-http://localhost:19091/api}"
LOGIN_USERNAME="${LOGIN_USERNAME:-admin}"
LOGIN_PASSWORD="${LOGIN_PASSWORD:-Admin@123}"
STRICT_DEFAULTS="${STRICT_DEFAULTS:-0}"

print_header() {
  echo "======================================"
  echo "ChatBI Release Security Check"
  echo "======================================"
  echo "ENV_FILE: $ENV_FILE"
  echo "BASE_URL: $BASE_URL"
  echo "STRICT_DEFAULTS: $STRICT_DEFAULTS"
  echo "======================================"
}

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

assert_env_key_exists() {
  local key="$1"
  if ! rg -q "^[[:space:]]*${key}=" "$ENV_FILE"; then
    echo "[FAIL] Missing required config key: $key"
    exit 1
  fi
  echo "[ OK ] Config key exists: $key"
}

check_default_secret() {
  local key="$1"
  local default_value="$2"
  local value
  value="$(read_env_var "$key" || true)"
  if [[ -z "$value" ]]; then
    echo "[FAIL] Secret key is empty: $key"
    exit 1
  fi
  if [[ "$value" == "$default_value" ]]; then
    if [[ "$STRICT_DEFAULTS" == "1" ]]; then
      echo "[FAIL] Secret key uses insecure default in strict mode: $key"
      exit 1
    fi
    echo "[WARN] Secret key uses default value (acceptable for local drill): $key"
    return
  fi
  echo "[ OK ] Secret key customized: $key"
}

scan_repo_secret_leak() {
  echo "[STEP] Scan tracked files for obvious secret leaks"

  local patterns=(
    "sk-[A-Za-z0-9]{20,}"
    "AKIA[0-9A-Z]{16}"
    "-----BEGIN (RSA|OPENSSH|EC) PRIVATE KEY-----"
    "xox[baprs]-[A-Za-z0-9-]{20,}"
  )

  local found=0
  for pattern in "${patterns[@]}"; do
    if rg -n --hidden -g '!.git/*' -g '!node_modules/*' -g '!docs/*.md' -g '!chatbi-server/.env.compose.example' -g '!*.lock' -e "$pattern" "$ROOT_DIR" >/tmp/chatbi-secret-scan.out 2>/dev/null; then
      found=1
      echo "[FAIL] Potential secret leak detected (pattern: $pattern)"
      cat /tmp/chatbi-secret-scan.out
      break
    fi
  done

  rm -f /tmp/chatbi-secret-scan.out
  if [[ "$found" == "1" ]]; then
    exit 1
  fi
  echo "[ OK ] No obvious secret leak in tracked files"
}

http_status() {
  local method="$1"
  local url="$2"
  local token="${3:-}"
  local data="${4:-}"

  if [[ "$method" == "GET" ]]; then
    curl -s -o /tmp/chatbi-security-http.body -w "%{http_code}" \
      -X GET "$url" \
      -H "Content-Type: application/json" \
      ${token:+-H "Authorization: Bearer $token"}
    return
  fi

  curl -s -o /tmp/chatbi-security-http.body -w "%{http_code}" \
    -X "$method" "$url" \
    -H "Content-Type: application/json" \
    ${token:+-H "Authorization: Bearer $token"} \
    -d "$data"
}

assert_status_matches() {
  local status="$1"
  local expected_regex="$2"
  local message="$3"
  if [[ "$status" =~ ^($expected_regex)$ ]]; then
    echo "[ OK ] $message (HTTP $status)"
    return
  fi
  echo "[FAIL] $message (HTTP $status)"
  echo "[FAIL] Response body:"
  cat /tmp/chatbi-security-http.body
  exit 1
}

run_authz_and_audit_checks() {
  echo "[STEP] Permission regression and audit traceability"

  local unauth_status
  unauth_status="$(http_status "GET" "$BASE_URL/system/permissions/tree")"
  local unauth_business_code
  unauth_business_code="$(grep -o '"code":[0-9]*' /tmp/chatbi-security-http.body | head -n1 | cut -d: -f2 || true)"
  if [[ "$unauth_status" =~ ^(401|403)$ || "$unauth_business_code" =~ ^(401|403)$ ]]; then
    echo "[ OK ] Unauthorized access is blocked for /system/permissions/tree (HTTP $unauth_status, code ${unauth_business_code:-N/A})"
  else
    echo "[FAIL] Unauthorized access is not blocked for /system/permissions/tree (HTTP $unauth_status, code ${unauth_business_code:-N/A})"
    cat /tmp/chatbi-security-http.body
    exit 1
  fi

  local login_status
  login_status="$(http_status "POST" "$BASE_URL/auth/login" "" "{\"username\":\"$LOGIN_USERNAME\",\"password\":\"$LOGIN_PASSWORD\"}")"
  assert_status_matches "$login_status" "200" "Admin login for security check"

  local token
  token="$(grep -o '"accessToken":"[^"]*"' /tmp/chatbi-security-http.body | head -n1 | cut -d'"' -f4)"
  if [[ -z "$token" ]]; then
    echo "[FAIL] Failed to extract access token from login response"
    cat /tmp/chatbi-security-http.body
    exit 1
  fi
  echo "[ OK ] Access token acquired"

  local me_status
  me_status="$(http_status "GET" "$BASE_URL/auth/me" "$token")"
  assert_status_matches "$me_status" "200" "Authorized access for /auth/me"
  if ! rg -q '"success":true' /tmp/chatbi-security-http.body; then
    echo "[FAIL] Authorized /auth/me query did not return success=true"
    cat /tmp/chatbi-security-http.body
    exit 1
  fi

  local audit_status
  audit_status="$(http_status "GET" "$BASE_URL/audit/logs?current=1&size=5" "$token")"
  assert_status_matches "$audit_status" "200" "Audit log query is available"
  if ! rg -q '"data"' /tmp/chatbi-security-http.body; then
    echo "[FAIL] Audit log response missing data payload"
    cat /tmp/chatbi-security-http.body
    exit 1
  fi
  echo "[ OK ] Audit log payload present"

  local access_alert_status
  access_alert_status="$(http_status "GET" "$BASE_URL/audit/access-alerts/options" "$token")"
  assert_status_matches "$access_alert_status" "200" "Access alert options query is available"
}

if [[ ! -f "$ENV_FILE" ]]; then
  if [[ -f "$ENV_EXAMPLE" ]]; then
    ENV_FILE="$ENV_EXAMPLE"
    echo "[WARN] ENV file missing, fallback to example: $ENV_FILE"
  else
    echo "[FAIL] Missing env file: $ENV_FILE"
    exit 1
  fi
fi

print_header

echo "[STEP] Config completeness checks"
assert_env_key_exists "DB_NAME"
assert_env_key_exists "DB_USERNAME"
assert_env_key_exists "MYSQL_PASSWORD"
assert_env_key_exists "MYSQL_ROOT_PASSWORD"
assert_env_key_exists "REDIS_PASSWORD"
assert_env_key_exists "CHATBI_AI_ENABLED"
assert_env_key_exists "JWT_SECRET"
assert_env_key_exists "PASSWORD_SALT"
assert_env_key_exists "AI_OBSERVABILITY_NOTIFY_ENABLED"
assert_env_key_exists "AI_OBSERVABILITY_NOTIFY_CHANNELS"

check_default_secret "JWT_SECRET" "chatbi-enterprise-secret-key-min-256-bit-for-jwt-signing"
check_default_secret "PASSWORD_SALT" "chatbi-salt-2026"

scan_repo_secret_leak
run_authz_and_audit_checks

rm -f /tmp/chatbi-security-http.body

echo "======================================"
echo "Release security check passed"
echo "======================================"
