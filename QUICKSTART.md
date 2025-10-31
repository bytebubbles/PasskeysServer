# 快速开始指南

## 🚀 10 分钟快速上手

### 第一步：启动服务器

**MacOS/Linux:**
```bash
./start.sh
```

**Windows:**
```bash
start.bat
```

**或者使用 Maven:**
```bash
mvn spring-boot:run
```

### 第二步：验证服务

打开浏览器或使用 curl 访问：

```bash
curl http://localhost:8080/api/passkeys/health
```

看到以下响应表示服务已成功启动：

```json
{
  "status": "UP",
  "service": "Passkeys Server",
  "timestamp": 1234567890123
}
```

### 第三步：测试 API

运行测试脚本：

```bash
./test-api.sh
```

或者使用 Postman 导入 `Passkeys-API.postman_collection.json` 进行测试。

## 📱 Android 集成

### 方法 1: 参考完整示例

查看 `AndroidIntegrationExample.md` 获取完整的 Android 集成代码示例。

### 方法 2: 核心步骤

1. **添加依赖**
```gradle
implementation "androidx.credentials:credentials:1.2.0"
implementation "androidx.credentials:credentials-play-services-auth:1.2.0"
```

2. **注册通行密钥**
```kotlin
// 获取注册选项
val options = apiService.startRegistration(username, displayName)

// 创建凭证
val credential = credentialManager.createCredential(
    CreatePublicKeyCredentialRequest(options),
    context
)

// 完成注册
apiService.finishRegistration(username, credential)
```

3. **使用通行密钥登录**
```kotlin
// 获取认证选项
val options = apiService.startAuthentication(username)

// 获取凭证
val credential = credentialManager.getCredential(
    GetCredentialRequest(listOf(
        GetPublicKeyCredentialOption(options)
    )),
    context
)

// 完成认证
apiService.finishAuthentication(credential)
```

## 🔧 配置说明

### 服务器端配置

编辑 `src/main/resources/application.properties`：

```properties
# 端口号
server.port=8080

# WebAuthn 配置
webauthn.rp.id=localhost
webauthn.rp.name=你的应用名称
```

### Android 端配置

修改 `ApiClient.kt` 中的服务器地址：

```kotlin
// 模拟器
private const val BASE_URL = "http://10.0.2.2:8080/api/"

// 真机（使用你的实际 IP）
private const val BASE_URL = "http://192.168.1.100:8080/api/"
```

## 📊 API 端点

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/passkeys/health` | 健康检查 |
| POST | `/passkeys/register/start` | 开始注册 |
| POST | `/passkeys/register/finish` | 完成注册 |
| POST | `/passkeys/authenticate/start` | 开始认证 |
| POST | `/passkeys/authenticate/finish` | 完成认证 |
| GET | `/passkeys/users` | 获取用户列表 |

## ⚠️ 常见问题

### 服务器无法启动？

**检查 Java 版本：**
```bash
java -version  # 需要 Java 17+
```

**检查端口占用：**
```bash
# MacOS/Linux
lsof -i :8080

# Windows
netstat -ano | findstr :8080
```

### Android 无法连接服务器？

1. **检查网络地址**
   - 模拟器使用 `10.0.2.2`
   - 真机使用实际 IP（如 `192.168.1.100`）

2. **配置网络安全**（允许 HTTP）
   - 创建 `network_security_config.xml`
   - 在 `AndroidManifest.xml` 中引用

3. **检查防火墙**
   - 确保 8080 端口未被防火墙阻止

### 注册或登录失败？

1. **设备要求**
   - Android 9+ (API 28+)
   - 支持生物识别
   - Google Play Services 已更新

2. **检查日志**
   - 服务器日志：查看终端输出
   - Android 日志：查看 Logcat

## 📚 更多资源

- **详细文档**: `README.md`
- **Android 集成**: `AndroidIntegrationExample.md`
- **API 测试**: `Passkeys-API.postman_collection.json`
- **官方文档**: https://developers.google.com/identity/passkeys

## 🎯 下一步

1. ✅ 启动服务器
2. ✅ 测试 API
3. ⬜ 创建 Android 应用
4. ⬜ 集成通行密钥功能
5. ⬜ 在真机上测试

## 💡 提示

- 这是一个 **Demo 项目**，使用内存存储
- **生产环境**需要：
  - 使用数据库（MySQL/PostgreSQL）
  - 配置 HTTPS
  - 添加认证授权
  - 实施安全最佳实践

---

**祝你使用愉快！** 🎉

如有问题，请参考 `README.md` 或提交 Issue。

