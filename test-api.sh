#!/bin/bash

# API 测试脚本

BASE_URL="http://localhost:8080"

echo "=========================================="
echo "  Passkeys Server - API 测试"
echo "=========================================="
echo ""

# 1. 健康检查
echo "1️⃣  测试健康检查接口..."
curl -s -X GET "${BASE_URL}/passkeys/health" | python3 -m json.tool
echo ""
echo ""

# 2. 开始注册
echo "2️⃣  测试开始注册接口..."
REGISTER_RESPONSE=$(curl -s -X POST "${BASE_URL}/passkeys/register/start" \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser@example.com","displayName":"测试用户"}')
echo "$REGISTER_RESPONSE" | python3 -m json.tool
echo ""
echo ""

# 3. 获取用户列表
echo "3️⃣  测试获取用户列表接口..."
curl -s -X GET "${BASE_URL}/passkeys/users" | python3 -m json.tool
echo ""
echo ""

# 4. 开始认证（需要先有注册的用户）
echo "4️⃣  测试开始认证接口..."
curl -s -X POST "${BASE_URL}/passkeys/authenticate/start" \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser@example.com"}' | python3 -m json.tool
echo ""
echo ""

echo "=========================================="
echo "  API 测试完成"
echo "=========================================="
echo ""
echo "注意: 完成注册和完成认证接口需要实际的 Android 设备配合，"
echo "      因此这里只测试了开始注册和开始认证接口。"
echo ""

