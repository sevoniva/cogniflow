#!/bin/bash
# ChatBI 冒烟测试脚本
# 用于快速验证系统基本功能是否正常

set -e

BASE_URL="${BASE_URL:-http://localhost:8080/api}"
ROOT_URL="${BASE_URL%/api}"
TOKEN=""
CHART_VALIDATION_MIN_TYPES="${CHART_VALIDATION_MIN_TYPES:-100}"
CHART_VALIDATION_MIN_COVERAGE="${CHART_VALIDATION_MIN_COVERAGE:-100}"
PERF_BUDGET_CHECKS="${PERF_BUDGET_CHECKS:-1}"
PERF_BUDGET_HEALTH_MS="${PERF_BUDGET_HEALTH_MS:-800}"
PERF_BUDGET_QUERY_MS="${PERF_BUDGET_QUERY_MS:-2200}"
PERF_BUDGET_CONVERSATION_MS="${PERF_BUDGET_CONVERSATION_MS:-2500}"
PERF_BUDGET_SAMPLE_ATTEMPTS="${PERF_BUDGET_SAMPLE_ATTEMPTS:-3}"

echo "======================================"
echo "ChatBI 冒烟测试"
echo "======================================"
echo "API 地址：$BASE_URL"
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 测试函数
test_api() {
    local name=$1
    local method=$2
    local path=$3
    local data=$4
    local expected_status=${5:-200}

    echo -n "测试：$name ... "

    local response
    local target_base="$BASE_URL"
    if [[ "$path" == /actuator/* ]]; then
        target_base="$ROOT_URL"
    fi
    if [ "$method" == "GET" ]; then
        response=$(curl -s -w "\n%{http_code}" -X GET "$target_base$path" \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer $TOKEN" 2>/dev/null)
    elif [ "$method" == "POST" ]; then
        response=$(curl -s -w "\n%{http_code}" -X POST "$target_base$path" \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer $TOKEN" \
            -d "$data" 2>/dev/null)
    elif [ "$method" == "PUT" ]; then
        response=$(curl -s -w "\n%{http_code}" -X PUT "$target_base$path" \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer $TOKEN" \
            -d "$data" 2>/dev/null)
    elif [ "$method" == "DELETE" ]; then
        response=$(curl -s -w "\n%{http_code}" -X DELETE "$target_base$path" \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer $TOKEN" 2>/dev/null)
    fi

    local status="${response##*$'\n'}"
    local body="${response%$'\n'*}"

    if [[ "$status" =~ ^($expected_status)$ ]]; then
        echo -e "${GREEN}通过${NC}"
        return 0
    else
        echo -e "${RED}失败 (HTTP $status)${NC}"
        echo "响应：$body"
        return 1
    fi
}

test_json_response() {
    local name=$1
    local method=$2
    local path=$3
    local data=$4
    local pattern=$5

    echo -n "测试：$name ... "

    local response
    local target_base="$BASE_URL"
    if [[ "$path" == /actuator/* ]]; then
        target_base="$ROOT_URL"
    fi

    if [ "$method" == "GET" ]; then
        response=$(curl -s -X GET "$target_base$path" \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer $TOKEN" 2>/dev/null)
    else
        response=$(curl -s -X POST "$target_base$path" \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer $TOKEN" \
            -d "$data" 2>/dev/null)
    fi

    if echo "$response" | grep -q "$pattern"; then
        echo -e "${GREEN}通过${NC}"
        return 0
    fi

    echo -e "${RED}失败${NC}"
    echo "响应：$response"
    return 1
}

assert_api_latency_budget() {
    local name=$1
    local method=$2
    local path=$3
    local data=$4
    local max_ms=$5
    local expected_status=${6:-200}

    echo -n "测试：$name 响应时延预算 <= ${max_ms}ms ... "

    local target_base="$BASE_URL"
    if [[ "$path" == /actuator/* ]]; then
        target_base="$ROOT_URL"
    fi

    # 预热请求：减少冷启动与短时抖动对预算校验的影响。
    for _ in 1 2; do
        if [ "$method" == "GET" ]; then
            curl -s -X GET "$target_base$path" \
                -H "Content-Type: application/json" \
                -H "Authorization: Bearer $TOKEN" >/dev/null 2>&1 || true
        else
            curl -s -X POST "$target_base$path" \
                -H "Content-Type: application/json" \
                -H "Authorization: Bearer $TOKEN" \
                -d "$data" >/dev/null 2>&1 || true
        fi
    done

    local response time_total status body time_ms
    local best_time_ms=999999
    local samples=""

    for attempt in $(seq 1 "$PERF_BUDGET_SAMPLE_ATTEMPTS"); do
        if [ "$method" == "GET" ]; then
            response=$(curl -s -w "\n%{http_code}\n%{time_total}" -X GET "$target_base$path" \
                -H "Content-Type: application/json" \
                -H "Authorization: Bearer $TOKEN" 2>/dev/null)
        else
            response=$(curl -s -w "\n%{http_code}\n%{time_total}" -X POST "$target_base$path" \
                -H "Content-Type: application/json" \
                -H "Authorization: Bearer $TOKEN" \
                -d "$data" 2>/dev/null)
        fi

        time_total=$(echo "$response" | tail -n1)
        status=$(echo "$response" | tail -n2 | head -n1)
        body=$(echo "$response" | sed '$d' | sed '$d')
        time_ms=$(awk "BEGIN {printf \"%d\", ${time_total} * 1000}")
        samples="${samples}${attempt}:${time_ms}ms "

        if [[ ! "$status" =~ ^($expected_status)$ ]]; then
            echo -e "${RED}失败 (HTTP $status)${NC}"
            echo "响应：$body"
            return 1
        fi

        if [ "$time_ms" -lt "$best_time_ms" ]; then
            best_time_ms=$time_ms
        fi
    done

    if [ "$best_time_ms" -gt "$max_ms" ]; then
        echo -e "${RED}失败 (${best_time_ms}ms > ${max_ms}ms)${NC}"
        echo "采样：$samples"
        return 1
    fi

    echo -e "${GREEN}通过 (${best_time_ms}ms)${NC}"
    return 0
}

assert_chart_validation_gate() {
    local path="/chart-catalog/validation?limit=0"
    local response
    local target_base="$BASE_URL"

    echo -n "测试：图表验证硬门禁 (types>=${CHART_VALIDATION_MIN_TYPES}, coverage>=${CHART_VALIDATION_MIN_COVERAGE}, invalid=0) ... "

    response=$(curl -s -X GET "$target_base$path" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $TOKEN" 2>/dev/null)

    if [ -z "$response" ]; then
        echo -e "${RED}失败${NC}"
        echo "响应为空，无法校验图表验证覆盖率"
        return 1
    fi

    if ! command -v node >/dev/null 2>&1; then
        echo -e "${RED}失败${NC}"
        echo "未检测到 node，无法解析图表验证响应"
        return 1
    fi

    local result
    result=$(RESPONSE_PAYLOAD="$response" CHART_VALIDATION_MIN_TYPES="$CHART_VALIDATION_MIN_TYPES" CHART_VALIDATION_MIN_COVERAGE="$CHART_VALIDATION_MIN_COVERAGE" node -e '
const payloadText = process.env.RESPONSE_PAYLOAD || "";
try {
  const payload = JSON.parse(payloadText);
  const minTypes = Number(process.env.CHART_VALIDATION_MIN_TYPES || "100");
  const minCoverage = Number(process.env.CHART_VALIDATION_MIN_COVERAGE || "100");
  if (!payload || payload.success !== true) {
    console.log("FAIL|success!=true");
    process.exit(1);
  }
  const data = payload.data || {};
  const validatedTypes = Number(data.validatedTypes ?? 0);
  const coverageRate = Number(data.coverageRate ?? 0);
  const invalid = Number(data.invalid ?? 0);
  if (Number.isNaN(validatedTypes) || validatedTypes < minTypes) {
    console.log(`FAIL|validatedTypes=${validatedTypes}|min=${minTypes}`);
    process.exit(1);
  }
  if (Number.isNaN(coverageRate) || coverageRate + 1e-9 < minCoverage) {
    console.log(`FAIL|coverageRate=${coverageRate}|min=${minCoverage}`);
    process.exit(1);
  }
  if (Number.isNaN(invalid) || invalid !== 0) {
    console.log(`FAIL|invalid=${invalid}|expected=0`);
    process.exit(1);
  }
  console.log(`PASS|validatedTypes=${validatedTypes}|coverageRate=${coverageRate}|invalid=${invalid}`);
} catch (error) {
  console.log(`FAIL|json-parse-error=${error.message}`);
  process.exit(1);
}
')
    local rc=$?

    if [ "$rc" -ne 0 ]; then
        echo -e "${RED}失败${NC}"
        echo "响应：$response"
        echo "校验详情：$result"
        return 1
    fi

    echo -e "${GREEN}通过${NC}"
    echo "校验详情：$result"
    return 0
}

# 1. 健康检查
echo "1. 健康检查"
test_api "健康检查" "GET" "/actuator/health" "" "200|503" || true

# 2. 登录获取 Token
echo ""
echo "2. 认证测试"
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"Admin@123"}' 2>/dev/null)

TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)

if [ -n "$TOKEN" ]; then
    echo -e "${GREEN}登录成功，获取 Token${NC}"
    echo "Token: ${TOKEN:0:20}..."
else
    echo -e "${YELLOW}登录失败，可能原因：服务未启动或测试数据未初始化${NC}"
    echo "响应：$LOGIN_RESPONSE"
fi

# 3. 指标管理测试
echo ""
echo "3. 指标管理测试"
test_api "获取指标列表" "GET" "/metrics" "" "200" || true
test_api "获取启用的指标" "GET" "/metrics/active" "" "200" || true

# 4. 同义词测试
echo ""
echo "4. 同义词测试"
test_api "获取同义词列表" "GET" "/synonyms" "" "200" || true

# 5. 数据源测试
echo ""
echo "5. 数据源测试"
test_api "获取数据源列表" "GET" "/datasources" "" "200" || true

# 6. 订阅测试
echo ""
echo "6. 订阅测试"
test_api "获取订阅列表" "GET" "/subscriptions" "" "200" || true

# 7. 分享测试
echo ""
echo "7. 分享测试"
test_api "获取分享列表" "GET" "/shares" "" "200" || true

# 8. 用户测试
echo ""
echo "8. 用户测试"
test_api "获取当前用户信息" "GET" "/auth/me" "" "200" || true

# 9. 权限测试
echo ""
echo "9. 权限测试"
test_api "获取启用角色列表" "GET" "/system/roles/active" "" "200" || true

# 10. 智能查询测试
echo ""
echo "10. 智能查询测试"
test_api "执行智能查询" "POST" "/query" '{"text":"查看营收数据"}' "200" || true
test_json_response "AI 对话返回业务解读" "POST" "/conversation/message" '{"message":"本月销售额","userId":1}' '"success":true' || true
test_json_response "AI 运行状态可读" "GET" "/ai-model/status" "" '"providerName"' || true
test_json_response "AI 可观测告警快照可读" "GET" "/ai-model/observability/alerts" "" '"healthStatus"' || true
test_api "图表目录摘要接口可访问" "GET" "/chart-catalog/summary" "" "200|401" || true
test_api "图表目录类型接口可访问" "GET" "/chart-catalog/types?limit=5" "" "200|401" || true
test_json_response "图表数据验证摘要可读" "GET" "/chart-catalog/validation?limit=5" "" '"coverageRate"' || true
assert_chart_validation_gate

# 11. 分析看板测试
echo ""
echo "11. 分析看板测试"
test_api "销售总览" "GET" "/analytics/sales/overview" "" "200" || true
test_api "销售趋势" "GET" "/analytics/sales/trend" "" "200" || true
test_api "运营总览" "GET" "/analytics/operation/overview" "" "200" || true
test_api "用户活跃趋势" "GET" "/analytics/operation/user-activity" "" "200" || true
test_api "用户注册趋势" "GET" "/analytics/operation/user-registration" "" "200" || true
test_api "用户留存分析" "GET" "/analytics/operation/user-retention" "" "200" || true
test_api "敏捷项目概览" "GET" "/agile/project-overview" "" "200" || true
test_api "敏捷提交统计" "GET" "/agile/commit-stats?projectId=1" "" "200" || true
test_api "敏捷部署成功率" "GET" "/agile/deployment-success-rate?projectId=1" "" "200" || true

if [[ "$PERF_BUDGET_CHECKS" == "1" ]]; then
    echo ""
    echo "12. 性能预算门禁"
    assert_api_latency_budget "健康检查" "GET" "/actuator/health" "" "$PERF_BUDGET_HEALTH_MS" "200|503"
    assert_api_latency_budget "智能查询" "POST" "/query" '{"text":"查看营收数据"}' "$PERF_BUDGET_QUERY_MS" "200"
    assert_api_latency_budget "AI 对话" "POST" "/conversation/message" '{"message":"本月销售额","userId":1}' "$PERF_BUDGET_CONVERSATION_MS" "200"
fi

# 总结
echo ""
echo "======================================"
echo "冒烟测试完成"
echo "======================================"
echo ""
echo "说明："
echo "- 绿色 (通过): API 返回预期状态码"
echo "- 红色 (失败): API 返回非预期状态码"
echo "- 黄色 (警告): 非关键问题"
echo "- 图表验证硬门禁：validatedTypes>=${CHART_VALIDATION_MIN_TYPES} 且 coverageRate>=${CHART_VALIDATION_MIN_COVERAGE} 且 invalid=0"
echo "- 性能预算门禁：health<=${PERF_BUDGET_HEALTH_MS}ms, query<=${PERF_BUDGET_QUERY_MS}ms, conversation<=${PERF_BUDGET_CONVERSATION_MS}ms"
echo ""
echo "如果大部分测试失败，请检查："
echo "1. 后端服务是否启动"
echo "2. 数据库连接是否正常"
echo "3. 测试数据是否已初始化"
