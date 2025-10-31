#!/bin/bash

# 通行密钥服务器启动脚本

echo "=========================================="
echo "  Passkeys Server - 启动脚本"
echo "=========================================="
echo ""

# 检查 Java 是否安装
if ! command -v java &> /dev/null; then
    echo "❌ 错误: 未检测到 Java"
    echo "请先安装 Java 17 或更高版本"
    exit 1
fi

# 显示 Java 版本
echo "Java 版本:"
java -version
echo ""

# 检查 Maven 是否安装
if ! command -v mvn &> /dev/null; then
    echo "❌ 错误: 未检测到 Maven"
    echo "请先安装 Maven 3.6 或更高版本"
    exit 1
fi

echo "Maven 版本:"
mvn -version | head -n 1
echo ""

# 清理并构建项目
echo "📦 正在构建项目..."
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "❌ 构建失败"
    exit 1
fi

echo ""
echo "✅ 构建成功"
echo ""

# 启动服务
echo "🚀 正在启动服务..."
echo ""

java -jar target/passkeys-server-1.0.0.jar

