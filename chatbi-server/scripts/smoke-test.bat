@echo off
REM ChatBI 冒烟测试脚本 (Windows 版本)
REM 用于快速验证系统基本功能是否正常

setlocal enabledelayedexpansion

set BASE_URL=%BASE_URL:-http://localhost:8080/api%
set TOKEN=

echo ======================================
echo ChatBI 冒烟测试
echo ======================================
echo API 地址：%BASE_URL%
echo.

REM 1. 健康检查
echo 1. 健康检查
curl -s -X GET "%BASE_URL%/actuator/health" -H "Content-Type: application/json"
echo.

REM 2. 登录获取 Token
echo.
echo 2. 认证测试
set LOGIN_RESPONSE=$(curl -s -X POST "%BASE_URL%/auth/login" -H "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"admin123\"}")
echo 登录响应：%LOGIN_RESPONSE%

REM 3-10. 其他测试（需要 Token）
REM 由于 Windows batch 处理 JSON 较复杂，建议使用 Maven 测试

echo.
echo ======================================
echo 建议使用 Maven 运行完整测试：
echo mvn test
echo ======================================
