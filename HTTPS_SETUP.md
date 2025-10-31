# HTTPS 配置指南

## 📋 为什么需要 HTTPS？

### Localhost 的特殊情况

**重要说明**：`localhost` 和 `127.0.0.1` 被视为**安全上下文**，即使使用 HTTP 也可以正常使用通行密钥！

- ✅ `http://localhost:8080` - 通行密钥可用
- ✅ `http://127.0.0.1:8080` - 通行密钥可用
- ✅ `http://10.0.2.2:8080` (Android 模拟器) - 通行密钥可用
- ❌ `http://192.168.1.100` (局域网 IP) - 需要 HTTPS
- ❌ `http://yourdomain.com` (真实域名) - 需要 HTTPS

### 何时必须使用 HTTPS？

1. **使用局域网 IP 测试真机**（如 `192.168.1.100`）
2. **生产环境部署**（真实域名）
3. **模拟真实环境**

---

## 方案 1: 本地开发使用自签名证书（推荐）

### 步骤 1: 生成自签名证书

```bash
# 进入项目目录
cd /Users/11022/dev/server/PasskeysServer

# 创建证书目录
mkdir -p src/main/resources/keystore

# 生成自签名证书
keytool -genkeypair \
  -alias passkeys-server \
  -keyalg RSA \
  -keysize 2048 \
  -storetype PKCS12 \
  -keystore src/main/resources/keystore/keystore.p12 \
  -validity 3650 \
  -storepass password \
  -keypass password \
  -dname "CN=localhost, OU=Development, O=Passkeys Demo, L=City, ST=State, C=CN"
```

**参数说明：**
- `alias`: 证书别名
- `keyalg`: 加密算法 (RSA)
- `keysize`: 密钥长度 (2048 位)
- `storetype`: 密钥库类型 (PKCS12，推荐)
- `validity`: 有效期 (3650 天 ≈ 10 年)
- `storepass`: 密钥库密码
- `keypass`: 密钥密码
- `dname`: 证书主体信息 (CN=localhost 很重要)

---

### 步骤 2: 配置 Spring Boot

编辑 `src/main/resources/application.properties`，添加 HTTPS 配置：

```properties
# 服务器配置
server.port=8443
server.servlet.context-path=/api

# SSL/TLS 配置
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore/keystore.p12
server.ssl.key-store-password=password
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=passkeys-server
server.ssl.key-password=password

# 同时启用 HTTP 重定向到 HTTPS（可选）
# server.http.port=8080

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
```

---

### 步骤 3: 信任自签名证书

#### 在 MacOS 上信任证书

```bash
# 导出证书
keytool -exportcert \
  -alias passkeys-server \
  -keystore src/main/resources/keystore/keystore.p12 \
  -storepass password \
  -file localhost.crt

# 添加到系统钥匙串
sudo security add-trusted-cert -d -r trustRoot -k /Library/Keychains/System.keychain localhost.crt

# 或者打开钥匙串访问应用手动添加
open localhost.crt
```

#### 在浏览器中信任证书

**Chrome/Edge:**
1. 访问 `https://localhost:8443`
2. 点击地址栏的"不安全"
3. 点击"证书"
4. 点击"详细信息" → "复制到文件"
5. 导入到"受信任的根证书颁发机构"

**或者直接访问时选择"继续访问（不安全）"**

#### Android 设备信任证书

**方法 1: 在 Android 网络安全配置中允许**

创建 `res/xml/network_security_config.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <!-- 开发环境：信任所有证书（仅用于调试！） -->
    <debug-overrides>
        <trust-anchors>
            <certificates src="system" />
            <certificates src="user" />
        </trust-anchors>
    </debug-overrides>
    
    <!-- 或者指定域名 -->
    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="true">localhost</domain>
        <domain includeSubdomains="true">10.0.2.2</domain>
        <trust-anchors>
            <certificates src="system" />
            <certificates src="user" />
        </trust-anchors>
    </domain-config>
</network-security-config>
```

在 `AndroidManifest.xml` 中引用：

```xml
<application
    android:networkSecurityConfig="@xml/network_security_config"
    ...>
</application>
```

**方法 2: 在 Android 设备上安装证书**

1. 将 `localhost.crt` 复制到 Android 设备
2. 设置 → 安全 → 加密与凭据 → 从存储设备安装
3. 选择证书文件并安装

---

### 步骤 4: 更新 assetlinks.json 路径

现在访问路径变为：
```
https://localhost:8443/.well-known/assetlinks.json
```

---

### 步骤 5: 重新构建和启动

```bash
# 重新构建项目
mvn clean package -DskipTests

# 启动服务器
java -jar target/passkeys-server-1.0.0.jar

# 或使用启动脚本
./start.sh
```

---

### 步骤 6: 验证 HTTPS

```bash
# 测试 HTTPS 连接
curl -k https://localhost:8443/api/passkeys/health

# -k 参数表示忽略证书验证（仅用于自签名证书）
```

**在浏览器中访问：**
```
https://localhost:8443/api/passkeys/health
```

---

## 方案 2: 同时支持 HTTP 和 HTTPS

如果您想同时支持两个端口，需要添加额外配置。

### 创建 HTTP 连接器配置类

创建 `src/main/java/com/example/passkeys/config/HttpsConfig.java`：

```java
package com.example.passkeys.config;

import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HttpsConfig {
    
    @Bean
    public ServletWebServerFactory servletContainer() {
        TomchatServletWebServerFactory tomcat = new TomcatServletWebServerFactory() {
            @Override
            protected void postProcessContext(Context context) {
                SecurityConstraint securityConstraint = new SecurityConstraint();
                securityConstraint.setUserConstraint("CONFIDENTIAL");
                SecurityCollection collection = new SecurityCollection();
                collection.addPattern("/*");
                securityConstraint.addCollection(collection);
                context.addConstraint(securityConstraint);
            }
        };
        
        // 添加 HTTP 连接器，重定向到 HTTPS
        tomcat.addAdditionalTomcatConnectors(redirectConnector());
        return tomcat;
    }
    
    private Connector redirectConnector() {
        Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
        connector.setScheme("http");
        connector.setPort(8080);
        connector.setSecure(false);
        connector.setRedirectPort(8443);
        return connector;
    }
}
```

这样配置后：
- HTTPS: `https://localhost:8443`
- HTTP: `http://localhost:8080` (自动重定向到 HTTPS)

---

## 方案 3: 使用 mkcert（最简单，推荐）

`mkcert` 是一个零配置的本地 HTTPS 证书工具。

### 安装 mkcert

**MacOS:**
```bash
brew install mkcert
brew install nss  # Firefox 支持
```

**Windows:**
```bash
choco install mkcert
```

**Linux:**
```bash
# Ubuntu/Debian
sudo apt install libnss3-tools
wget https://github.com/FiloSottile/mkcert/releases/download/v1.4.4/mkcert-v1.4.4-linux-amd64
chmod +x mkcert-v1.4.4-linux-amd64
sudo mv mkcert-v1.4.4-linux-amd64 /usr/local/bin/mkcert
```

### 生成证书

```bash
# 安装本地 CA
mkcert -install

# 生成 localhost 证书
cd /Users/11022/dev/server/PasskeysServer
mkdir -p src/main/resources/keystore

mkcert -pkcs12 -p12-file src/main/resources/keystore/keystore.p12 localhost 127.0.0.1 ::1

# 设置密码（mkcert 生成的证书默认密码为 "changeit"）
```

### 配置 Spring Boot

```properties
server.port=8443
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore/keystore.p12
server.ssl.key-store-password=changeit
server.ssl.key-store-type=PKCS12
```

**优势：**
- ✅ 浏览器自动信任
- ✅ Android 设备自动信任（如果安装了 CA）
- ✅ 零配置
- ✅ 证书自动续期

---

## 方案 4: 生产环境使用 Let's Encrypt

### 前置条件

- 拥有公网域名（如 `example.com`）
- 服务器有公网 IP
- 域名已解析到服务器

### 使用 Certbot 获取证书

```bash
# 安装 Certbot
# MacOS
brew install certbot

# Ubuntu/Debian
sudo apt install certbot

# 获取证书
sudo certbot certonly --standalone -d yourdomain.com

# 证书位置
# /etc/letsencrypt/live/yourdomain.com/fullchain.pem
# /etc/letsencrypt/live/yourdomain.com/privkey.pem
```

### 转换为 PKCS12 格式

```bash
sudo openssl pkcs12 -export \
  -in /etc/letsencrypt/live/yourdomain.com/fullchain.pem \
  -inkey /etc/letsencrypt/live/yourdomain.com/privkey.pem \
  -out keystore.p12 \
  -name passkeys-server \
  -passout pass:your-password
```

### 配置 Spring Boot

```properties
server.port=443
server.ssl.enabled=true
server.ssl.key-store=file:/path/to/keystore.p12
server.ssl.key-store-password=your-password
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=passkeys-server

webauthn.rp.id=yourdomain.com
webauthn.rp.name=Your App Name
```

### 自动续期

```bash
# 添加续期任务
sudo crontab -e

# 每月 1 号凌晨 2 点续期
0 2 1 * * certbot renew --quiet && systemctl restart passkeys-server
```

---

## 🔧 更新 Android 应用配置

### 使用 HTTPS 后需要更新的地方

**1. API 基础 URL**

```kotlin
// ApiClient.kt
object ApiClient {
    // HTTP (仅 localhost)
    // private const val BASE_URL = "http://10.0.2.2:8080/api/"
    
    // HTTPS (推荐)
    private const val BASE_URL = "https://10.0.2.2:8443/api/"
    
    // 生产环境
    // private const val BASE_URL = "https://yourdomain.com/api/"
}
```

**2. assetlinks.json URL**

```
https://localhost:8443/.well-known/assetlinks.json
```

**3. AndroidManifest.xml**

```xml
<data 
    android:scheme="https"
    android:host="localhost"
    android:port="8443" />
```

---

## 📝 快速对比

| 方案 | 难度 | 适用场景 | 自动信任 |
|------|------|---------|---------|
| 方案 1: 自签名证书 | ⭐⭐ | 本地开发 | ❌ 需要手动信任 |
| 方案 2: HTTP+HTTPS | ⭐⭐⭐ | 开发调试 | ❌ 需要手动信任 |
| 方案 3: mkcert | ⭐ | 本地开发 | ✅ 自动信任 |
| 方案 4: Let's Encrypt | ⭐⭐⭐⭐ | 生产环境 | ✅ 自动信任 |

---

## 🎯 推荐方案

### 本地开发（localhost）

**方案 A: 继续使用 HTTP**
- 最简单，无需配置
- `http://localhost:8080` 或 `http://10.0.2.2:8080`
- ✅ 通行密钥完全可用

**方案 B: 使用 mkcert**
- 体验最好
- 自动信任证书
- 更接近生产环境

### 生产环境

**必须使用 Let's Encrypt 或商业证书**
- HTTPS 是强制要求
- 自动续期
- 所有设备自动信任

---

## ⚠️ 常见问题

### Q1: 必须使用 HTTPS 吗？

**A:** 对于 `localhost` 和 `127.0.0.1`，HTTP 可用。但真实域名或 IP 地址必须使用 HTTPS。

### Q2: 自签名证书安全吗？

**A:** 仅用于开发环境。生产环境必须使用可信 CA 签发的证书（如 Let's Encrypt）。

### Q3: Android 设备不信任自签名证书怎么办？

**A:** 
1. 在设备上安装证书
2. 使用网络安全配置允许
3. 使用 mkcert（推荐）

### Q4: 可以使用 IP 地址访问吗？

**A:** 
- `127.0.0.1` 和 `10.0.2.2` (Android 模拟器) - HTTP 可用
- 其他 IP（如 `192.168.1.100`）- 必须 HTTPS

---

**建议：本地开发先使用 HTTP，生产环境再配置 HTTPS！** 🚀

