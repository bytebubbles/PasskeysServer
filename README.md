# Passkeys Server Demo

这是一个基于 Java 的通行密钥（Passkeys）后端服务 demo，支持 Android 端的通行密钥注册和登录功能。

## 项目简介

本项目实现了 [Google 通行密钥开发者指南](https://developers.google.com/identity/passkeys/developer-guides/server-introduction?hl=zh-cn) 中描述的服务器端功能，包括：

- ✅ 通行密钥注册（Passkey Registration）
- ✅ 通行密钥认证（Passkey Authentication）
- ✅ 用户管理
- ✅ RESTful API 接口

## 技术栈

- **Java 17**
- **Spring Boot 3.1.5**
- **Yubico WebAuthn Server Library 2.5.0** - FIDO2 服务器端库
- **Maven** - 依赖管理
- **内存存储** - 适合 Demo 演示（生产环境建议使用数据库）

## 项目结构

```
PasskeysServer/
├── pom.xml                                          # Maven 配置文件
├── README.md                                        # 项目说明文档
└── src/main/
    ├── java/com/example/passkeys/
    │   ├── PasskeysServerApplication.java           # 应用主类
    │   ├── config/
    │   │   └── WebAuthnConfig.java                  # WebAuthn 配置
    │   ├── controller/
    │   │   └── PasskeysController.java              # REST API 控制器
    │   ├── dto/
    │   │   ├── AuthenticationRequest.java           # 认证请求 DTO
    │   │   └── RegistrationRequest.java             # 注册请求 DTO
    │   ├── model/
    │   │   ├── Authenticator.java                   # 认证器实体
    │   │   └── User.java                            # 用户实体
    │   ├── repository/
    │   │   └── UserRepository.java                  # 用户和凭证存储
    │   └── service/
    │       └── WebAuthnService.java                 # WebAuthn 业务逻辑
    └── resources/
        └── application.properties                   # 应用配置文件
```

## 快速开始

### 前置条件

- Java 17 或更高版本
- Maven 3.6 或更高版本

### 安装和运行

1. **克隆或下载项目**

```bash
cd /Users/11022/dev/server/PasskeysServer
```

2. **构建项目**

```bash
mvn clean install
```

3. **运行服务**

```bash
mvn spring-boot:run
```

或者运行打包后的 JAR 文件：

```bash
java -jar target/passkeys-server-1.0.0.jar
```

4. **验证服务**

服务启动后，访问健康检查接口：

```bash
curl http://localhost:8080/api/passkeys/health
```

预期响应：

```json
{
  "status": "UP",
  "service": "Passkeys Server",
  "timestamp": 1234567890123
}
```

## API 接口文档

### 基础 URL

```
http://localhost:8080/api
```

### 1. 健康检查

**请求**

```
GET /passkeys/health
```

**响应**

```json
{
  "status": "UP",
  "service": "Passkeys Server",
  "timestamp": 1234567890123
}
```

### 2. 开始注册

**请求**

```
POST /passkeys/register/start
Content-Type: application/json

{
  "username": "user@example.com",
  "displayName": "示例用户"
}
```

**响应**

```json
{
  "success": true,
  "options": {
    "rp": {
      "name": "Passkeys Demo Server",
      "id": "localhost"
    },
    "user": {
      "name": "user@example.com",
      "displayName": "示例用户",
      "id": "..."
    },
    "challenge": "...",
    "pubKeyCredParams": [...],
    "timeout": 60000,
    "authenticatorSelection": {
      "authenticatorAttachment": "platform",
      "residentKey": "required",
      "userVerification": "required"
    }
  }
}
```

### 3. 完成注册

**请求**

```
POST /passkeys/register/finish
Content-Type: application/json

{
  "username": "user@example.com",
  "credential": {
    "id": "...",
    "rawId": "...",
    "response": {
      "attestationObject": "...",
      "clientDataJSON": "..."
    },
    "type": "public-key"
  }
}
```

**响应**

```json
{
  "success": true,
  "message": "注册成功",
  "username": "user@example.com",
  "credentialId": "..."
}
```

### 4. 开始认证

**请求**

```
POST /passkeys/authenticate/start
Content-Type: application/json

{
  "username": "user@example.com"
}
```

或者使用可发现凭证（Discoverable Credential）：

```
POST /passkeys/authenticate/start
Content-Type: application/json

{}
```

**响应**

```json
{
  "success": true,
  "options": {
    "challenge": "...",
    "timeout": 60000,
    "rpId": "localhost",
    "userVerification": "required",
    "allowCredentials": [...]
  }
}
```

### 5. 完成认证

**请求**

```
POST /passkeys/authenticate/finish
Content-Type: application/json

{
  "credential": {
    "id": "...",
    "rawId": "...",
    "response": {
      "authenticatorData": "...",
      "clientDataJSON": "...",
      "signature": "...",
      "userHandle": "..."
    },
    "type": "public-key"
  }
}
```

**响应**

```json
{
  "success": true,
  "username": "user@example.com",
  "userId": "...",
  "displayName": "示例用户",
  "credentialId": "..."
}
```

### 6. 获取用户列表

**请求**

```
GET /passkeys/users
```

**响应**

```json
{
  "success": true,
  "count": 1,
  "users": [
    {
      "username": "user@example.com",
      "displayName": "示例用户",
      "userId": "...",
      "authenticatorCount": 1,
      "createdAt": 1234567890123
    }
  ]
}
```

## Android 客户端集成

### 配置 Android 应用

在 Android 应用中使用 Credential Manager API 与此服务器交互：

1. **添加依赖**（build.gradle）：

```gradle
dependencies {
    implementation "androidx.credentials:credentials:1.2.0"
    implementation "androidx.credentials:credentials-play-services-auth:1.2.0"
}
```

2. **配置 Digital Asset Links**：

在服务器的 `.well-known/assetlinks.json` 中添加 Android 应用的签名：

```json
[{
  "relation": ["delegate_permission/common.handle_all_urls"],
  "target": {
    "namespace": "android_app",
    "package_name": "com.example.yourapp",
    "sha256_cert_fingerprints": ["YOUR_APP_SIGNATURE"]
  }
}]
```

3. **注册通行密钥**：

```kotlin
val createPublicKeyCredentialRequest = CreatePublicKeyCredentialRequest(
    requestJson = optionsJson // 从 /passkeys/register/start 获取
)

val result = credentialManager.createCredential(
    request = createPublicKeyCredentialRequest,
    context = context
)
```

4. **使用通行密钥登录**：

```kotlin
val getPublicKeyCredentialOption = GetPublicKeyCredentialOption(
    requestJson = optionsJson // 从 /passkeys/authenticate/start 获取
)

val getCredRequest = GetCredentialRequest(
    listOf(getPublicKeyCredentialOption)
)

val result = credentialManager.getCredential(
    request = getCredRequest,
    context = context
)
```

## 配置说明

### application.properties

```properties
# 服务器配置
server.port=8080
server.servlet.context-path=/api

# WebAuthn 配置
webauthn.rp.id=localhost                              # 依赖方 ID
webauthn.rp.name=Passkeys Demo Server                # 依赖方名称
webauthn.rp.origin=android:apk-key-hash:...          # Android 应用来源
```

### 生产环境配置

对于生产环境，需要修改以下配置：

1. **更新 RP ID**：使用实际的域名（如 `example.com`）
2. **配置 Origins**：在 `WebAuthnConfig.java` 中添加实际的 Android 应用签名
3. **使用数据库**：替换内存存储为 MySQL/PostgreSQL 等
4. **添加 HTTPS**：配置 SSL 证书
5. **添加认证和授权**：使用 Spring Security

## 获取 Android 应用签名

使用以下命令获取 Android 应用的 SHA-256 签名：

```bash
keytool -list -v -keystore your-keystore.jks -alias your-key-alias
```

然后将签名转换为 Base64 格式并配置到服务器中。

## 测试

### 使用 curl 测试注册

```bash
# 1. 开始注册
curl -X POST http://localhost:8080/api/passkeys/register/start \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","displayName":"Test User"}'

# 2. 完成注册（需要 Android 设备返回的凭证数据）
# 此步骤需要实际的 Android 设备配合

# 3. 查看用户列表
curl http://localhost:8080/api/passkeys/users
```

## 常见问题

### 1. 为什么要使用通行密钥？

- **更安全**：使用公钥加密，无需传输密码
- **更便捷**：使用生物识别（指纹、人脸）快速登录
- **防钓鱼**：凭证与域名绑定，无法跨站使用

### 2. 通行密钥与传统密码的区别？

| 特性 | 传统密码 | 通行密钥 |
|------|---------|---------|
| 安全性 | 可能被泄露 | 私钥永不离开设备 |
| 便捷性 | 需要记忆 | 生物识别自动登录 |
| 防钓鱼 | 容易被钓鱼 | 与域名绑定 |
| 跨设备 | 容易共享 | 通过云同步（如 Google 密码管理器） |

### 3. 如何在生产环境部署？

1. 使用数据库存储用户和凭证
2. 配置 HTTPS
3. 更新 RP ID 为实际域名
4. 配置 Android 应用的实际签名
5. 添加日志和监控
6. 实施备份和恢复策略

## 参考资料

- [Google 通行密钥开发者指南](https://developers.google.com/identity/passkeys/developer-guides/server-introduction?hl=zh-cn)
- [WebAuthn 规范](https://www.w3.org/TR/webauthn-2/)
- [FIDO2 规范](https://fidoalliance.org/fido2/)
- [Yubico Java WebAuthn Server](https://github.com/Yubico/java-webauthn-server)
- [Android Credential Manager](https://developer.android.com/training/sign-in/passkeys)

## 许可证

MIT License

## 作者

Passkeys Demo Server - 2024

---

**注意**：这是一个演示项目，仅用于学习和测试目的。生产环境需要额外的安全加固和功能完善。

