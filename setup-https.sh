#!/bin/bash

# HTTPS 快速设置脚本（使用自签名证书）

echo "=========================================="
echo "  Passkeys Server - HTTPS 快速设置"
echo "=========================================="
echo ""

# 检查 keytool 是否可用
if ! command -v keytool &> /dev/null; then
    echo "❌ 错误: keytool 未找到"
    echo "请确保已安装 Java JDK"
    exit 1
fi

# 创建证书目录
echo "📁 创建证书目录..."
mkdir -p src/main/resources/keystore

# 检查证书是否已存在
if [ -f "src/main/resources/keystore/keystore.p12" ]; then
    echo "⚠️  证书已存在"
    read -p "是否覆盖现有证书？(y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "已取消"
        exit 0
    fi
    rm -f src/main/resources/keystore/keystore.p12
fi

# 生成自签名证书
echo ""
echo "🔐 生成自签名证书..."
keytool -genkeypair \
  -alias passkeys-server \
  -keyalg RSA \
  -keysize 2048 \
  -storetype PKCS12 \
  -keystore src/main/resources/keystore/keystore.p12 \
  -validity 3650 \
  -storepass password \
  -keypass password \
  -dname "CN=localhost, OU=Development, O=Passkeys Demo, L=Beijing, ST=Beijing, C=CN" \
  2>&1

if [ $? -ne 0 ]; then
    echo ""
    echo "❌ 证书生成失败"
    exit 1
fi

echo ""
echo "✅ 证书生成成功！"
echo ""

# 导出证书（用于导入到系统）
echo "📤 导出证书..."
keytool -exportcert \
  -alias passkeys-server \
  -keystore src/main/resources/keystore/keystore.p12 \
  -storepass password \
  -file localhost.crt \
  2>&1

echo ""
echo "✅ 证书已导出到: localhost.crt"
echo ""

# 更新配置文件
echo "📝 更新配置文件..."

# 备份原配置
cp src/main/resources/application.properties src/main/resources/application.properties.backup

# 创建 HTTPS 配置
cat > src/main/resources/application-https.properties << 'EOF'
# 服务器配置（HTTPS）
server.port=8443
server.servlet.context-path=/api

# SSL/TLS 配置
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore/keystore.p12
server.ssl.key-store-password=password
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=passkeys-server
server.ssl.key-password=password

# 应用配置
spring.application.name=passkeys-server

# JSON 配置
spring.jackson.serialization.indent-output=true
spring.jackson.default-property-inclusion=non_null

# 日志配置
logging.level.root=INFO
logging.level.com.example.passkeys=DEBUG
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %msg%n

# WebAuthn 配置
webauthn.rp.id=localhost
webauthn.rp.name=Passkeys Demo Server
EOF

echo "✅ HTTPS 配置文件已创建: application-https.properties"
echo ""

# 重新构建项目
echo "📦 重新构建项目..."
mvn clean package -DskipTests -q

if [ $? -ne 0 ]; then
    echo "❌ 构建失败"
    exit 1
fi

echo ""
echo "✅ 项目构建成功"
echo ""

echo "=========================================="
echo "  🎉 HTTPS 设置完成！"
echo "=========================================="
echo ""
echo "📋 下一步操作："
echo ""
echo "1. 启动 HTTPS 服务器："
echo "   java -jar target/passkeys-server-1.0.0.jar --spring.profiles.active=https"
echo ""
echo "2. 访问测试："
echo "   curl -k https://localhost:8443/api/passkeys/health"
echo ""
echo "3. 浏览器访问："
echo "   https://localhost:8443/.well-known/assetlinks.json"
echo "   （首次访问需要接受证书警告）"
echo ""
echo "4. 信任证书（可选，MacOS）："
echo "   sudo security add-trusted-cert -d -r trustRoot -k /Library/Keychains/System.keychain localhost.crt"
echo ""
echo "5. 如需恢复 HTTP，使用备份的配置："
echo "   cp src/main/resources/application.properties.backup src/main/resources/application.properties"
echo ""
echo "📚 详细文档: HTTPS_SETUP.md"
echo ""

