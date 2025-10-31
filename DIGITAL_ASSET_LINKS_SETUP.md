# Digital Asset Links 配置指南

## 什么是 Digital Asset Links？

Digital Asset Links 是 Android 用来验证应用和网站之间关系的机制。对于通行密钥（Passkeys），这个配置告诉 Android 系统：**您的 Android 应用有权使用该域名的通行密钥**。

## 📋 为什么需要配置？

Android 的 Credential Manager API 需要验证：
1. ✅ Android 应用确实属于该域名
2. ✅ 域名授权该应用访问通行密钥
3. ✅ 防止钓鱼攻击和未授权访问

## 🔧 配置步骤

### 第 1 步：获取 Android 应用的 SHA-256 指纹

**使用 Debug 密钥库（开发测试）：**

```bash
keytool -list -v -keystore ~/.android/debug.keystore \
  -alias androiddebugkey \
  -storepass android \
  -keypass android
```

**使用 Release 密钥库（生产环境）：**

```bash
keytool -list -v -keystore /path/to/your/release.keystore \
  -alias your-key-alias
```

**输出示例：**
```
Certificate fingerprints:
     SHA1: AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF:12
     SHA256: 12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD
```

**复制 SHA256 的值**（包括冒号）。

---

### 第 2 步：格式化 SHA-256 指纹

将 SHA-256 指纹转换为正确格式（移除冒号，全部大写）：

**原始格式：**
```
12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD
```

**转换后格式：**
```
1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890ABCD
```

**快速转换命令：**

```bash
# 方法 1: 使用命令行
echo "12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD" | tr -d ':' | tr '[:lower:]' '[:upper:]'

# 方法 2: 直接从 keytool 获取并格式化
keytool -list -v -keystore ~/.android/debug.keystore \
  -alias androiddebugkey \
  -storepass android \
  -keypass android | \
  grep SHA256 | \
  cut -d ' ' -f 3 | \
  tr -d ':' | \
  tr '[:lower:]' '[:upper:]'
```

---

### 第 3 步：更新 assetlinks.json

编辑 `src/main/resources/static/.well-known/assetlinks.json`：

```json
[
  {
    "relation": [
      "delegate_permission/common.handle_all_urls",
      "delegate_permission/common.get_login_creds"
    ],
    "target": {
      "namespace": "android_app",
      "package_name": "com.example.yourapp",
      "sha256_cert_fingerprints": [
        "YOUR_ACTUAL_SHA256_FINGERPRINT_HERE"
      ]
    }
  }
]
```

**替换以下内容：**
1. `com.example.yourapp` → 您的 Android 应用包名
2. `YOUR_ACTUAL_SHA256_FINGERPRINT_HERE` → 您的 SHA-256 指纹（已格式化）

**示例（已填写）：**

```json
[
  {
    "relation": [
      "delegate_permission/common.handle_all_urls",
      "delegate_permission/common.get_login_creds"
    ],
    "target": {
      "namespace": "android_app",
      "package_name": "com.example.passkeysapp",
      "sha256_cert_fingerprints": [
        "1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890ABCD"
      ]
    }
  }
]
```

---

### 第 4 步：配置 Android 应用

在您的 Android 应用的 `AndroidManifest.xml` 中添加：

```xml
<activity android:name=".MainActivity">
    <!-- 其他配置 -->
    
    <!-- 添加以下内容 -->
    <intent-filter android:autoVerify="true">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        
        <!-- 替换为您的实际域名 -->
        <data 
            android:scheme="https"
            android:host="yourdomain.com" />
    </intent-filter>
</activity>
```

---

### 第 5 步：验证配置

**重启服务器：**

```bash
./stop.sh
./start.sh
```

**访问 assetlinks.json：**

```bash
# 本地测试
curl http://localhost:8080/.well-known/assetlinks.json

# 或在浏览器中访问
# http://localhost:8080/.well-known/assetlinks.json
```

**验证 JSON 格式：**

使用 Google 的验证工具：
https://developers.google.com/digital-asset-links/tools/generator

---

## 🌐 生产环境配置

### 使用真实域名

如果您有实际的域名（如 `example.com`），需要：

1. **部署到服务器**

2. **使用 HTTPS**（必需）
   ```bash
   # 使用 Let's Encrypt 获取免费 SSL 证书
   sudo certbot --nginx -d example.com
   ```

3. **更新 WebAuthn 配置**

编辑 `src/main/resources/application.properties`：

```properties
webauthn.rp.id=example.com
webauthn.rp.name=Your App Name
```

4. **确保文件可访问**
   ```
   https://example.com/.well-known/assetlinks.json
   ```

5. **更新 Android 应用配置**

在 `AndroidManifest.xml` 中：

```xml
<data 
    android:scheme="https"
    android:host="example.com" />
```

---

## 🧪 本地开发测试

### 方法 1: 使用模拟器（推荐）

Android 模拟器可以通过 `10.0.2.2` 访问主机的 localhost：

**服务器配置不变**（使用 localhost）

**Android 应用中使用：**
```kotlin
private const val BASE_URL = "http://10.0.2.2:8080/api/"
```

**assetlinks.json 使用 localhost**（开发环境可以这样）

---

### 方法 2: 使用真机 + 局域网 IP

1. **获取您的局域网 IP：**
   ```bash
   # MacOS/Linux
   ifconfig | grep "inet " | grep -v 127.0.0.1
   
   # 例如：192.168.1.100
   ```

2. **更新 application.properties：**
   ```properties
   webauthn.rp.id=192.168.1.100
   ```

3. **在真机上访问：**
   ```
   http://192.168.1.100:8080/.well-known/assetlinks.json
   ```

---

## 📝 多个应用支持

如果您有多个 Android 应用（如 debug 版和 release 版），可以添加多个配置：

```json
[
  {
    "relation": [
      "delegate_permission/common.handle_all_urls",
      "delegate_permission/common.get_login_creds"
    ],
    "target": {
      "namespace": "android_app",
      "package_name": "com.example.passkeysapp",
      "sha256_cert_fingerprints": [
        "DEBUG_VERSION_SHA256_FINGERPRINT"
      ]
    }
  },
  {
    "relation": [
      "delegate_permission/common.handle_all_urls",
      "delegate_permission/common.get_login_creds"
    ],
    "target": {
      "namespace": "android_app",
      "package_name": "com.example.passkeysapp",
      "sha256_cert_fingerprints": [
        "RELEASE_VERSION_SHA256_FINGERPRINT"
      ]
    }
  }
]
```

---

## ⚠️ 常见问题

### 1. 通行密钥注册失败，提示 "origin validation failed"

**原因：** assetlinks.json 配置不正确或无法访问

**解决方案：**
- 验证 JSON 格式是否正确
- 确保服务器可以访问 `/.well-known/assetlinks.json`
- 检查 SHA-256 指纹是否正确
- 确保包名匹配

### 2. Android 应用无法找到 assetlinks.json

**原因：** 网络问题或路径错误

**解决方案：**
- 确保服务器正在运行
- 在浏览器或 curl 中测试 URL
- 检查防火墙设置
- 确保使用正确的协议（HTTP/HTTPS）

### 3. SHA-256 指纹格式错误

**原因：** 包含冒号或大小写不一致

**解决方案：**
- 移除所有冒号
- 确保全部大写
- 不要有空格或换行符

---

## 🔍 验证清单

在测试 Android 应用之前，确保：

- [ ] ✅ 已获取正确的 SHA-256 指纹
- [ ] ✅ 指纹已格式化（无冒号，全大写）
- [ ] ✅ assetlinks.json 中的包名正确
- [ ] ✅ assetlinks.json 中的指纹正确
- [ ] ✅ 服务器可以访问 `/.well-known/assetlinks.json`
- [ ] ✅ JSON 格式正确（使用在线工具验证）
- [ ] ✅ AndroidManifest.xml 配置正确
- [ ] ✅ Android 应用和服务器使用相同的域名/IP

---

## 📚 参考资料

- [Android App Links 文档](https://developer.android.com/training/app-links)
- [Digital Asset Links 规范](https://developers.google.com/digital-asset-links/v1/getting-started)
- [Google Passkeys 开发指南](https://developers.google.com/identity/passkeys)
- [验证工具](https://developers.google.com/digital-asset-links/tools/generator)

---

**配置完成后，您的 Android 应用就可以安全地使用通行密钥了！** 🎉

