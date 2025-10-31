# 常见问题排查指南

## 编译和构建问题

### 1. ByteArray.fromBytes() 方法找不到

**问题描述：**
```
找不到符号：方法 fromBytes(byte[])
位置：类 com.yubico.webauthn.data.ByteArray
```

**原因：**
Yubico WebAuthn 库的 API 在不同版本中有所变化。`ByteArray.fromBytes()` 静态方法在某些版本中不存在。

**解决方案：**
使用 `new ByteArray(bytes)` 构造函数代替：

```java
// ❌ 错误写法
return ByteArray.fromBytes(bytes).getBase64Url();

// ✅ 正确写法
return new ByteArray(bytes).getBase64Url();
```

**已修复：** 项目中已经使用了正确的 API。

---

### 1.5. Lombok 注解处理器问题

**问题描述：**
```
找不到符号：方法 getUsername()
找不到符号：变量 log
```

**原因：**
Lombok 的注解处理器（`@Data`、`@Slf4j`、`@RequiredArgsConstructor`）在某些开发环境中可能无法正常工作。

**解决方案：**

**方法 1：手动添加 getter/setter 方法（推荐用于 Demo）**

项目已采用此方案，移除了所有 Lombok 注解，手动添加了 getter/setter 和构造函数。

**方法 2：配置 Lombok（适合复杂项目）**

如果需要使用 Lombok，需要：

1. 在 IDE 中安装 Lombok 插件
2. 启用注解处理器：
   - IntelliJ IDEA: `Settings > Build > Compiler > Annotation Processors > Enable annotation processing`
   - Eclipse: 安装 Lombok jar 到 Eclipse 目录

3. 确保 `pom.xml` 中 Lombok 配置正确：

```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

**已修复：** 项目已移除 Lombok 依赖，使用标准 Java getter/setter 方法。

---

### 2. Maven 依赖下载失败

**问题描述：**
```
Could not resolve dependencies for project...
```

**解决方案：**

1. **清理 Maven 缓存：**
```bash
mvn clean
rm -rf ~/.m2/repository/com/yubico
mvn install
```

2. **配置 Maven 镜像（国内用户）：**

编辑 `~/.m2/settings.xml`，添加阿里云镜像：

```xml
<settings>
  <mirrors>
    <mirror>
      <id>aliyun</id>
      <name>Aliyun Maven</name>
      <url>https://maven.aliyun.com/repository/public</url>
      <mirrorOf>central</mirrorOf>
    </mirror>
  </mirrors>
</settings>
```

---

### 3. Java 版本不兼容

**问题描述：**
```
java.lang.UnsupportedClassVersionError
```

**解决方案：**

确保使用 Java 17 或更高版本：

```bash
# 检查 Java 版本
java -version

# 如果版本过低，需要升级 Java
# MacOS
brew install openjdk@17

# Ubuntu
sudo apt install openjdk-17-jdk

# Windows
# 从 https://adoptium.net/ 下载安装
```

设置 `JAVA_HOME` 环境变量：

```bash
# MacOS/Linux
export JAVA_HOME=/path/to/java17
export PATH=$JAVA_HOME/bin:$PATH

# Windows
setx JAVA_HOME "C:\Program Files\Java\jdk-17"
```

---

## 运行时问题

### 4. 端口 8080 已被占用

**问题描述：**
```
Web server failed to start. Port 8080 was already in use.
```

**解决方案：**

**方法 1：** 关闭占用端口的进程

```bash
# MacOS/Linux
lsof -ti:8080 | xargs kill -9

# Windows
netstat -ano | findstr :8080
taskkill /PID <进程ID> /F
```

**方法 2：** 修改服务器端口

编辑 `src/main/resources/application.properties`：

```properties
server.port=8081  # 改为其他端口
```

---

### 5. 注册或认证时出现 Challenge 验证失败

**问题描述：**
```
AssertionFailedException: Challenge validation failed
```

**原因：**
- Challenge 已过期
- 客户端和服务器时间不同步
- 请求匹配错误

**解决方案：**

1. **同步系统时间：**
```bash
# MacOS
sudo sntp -sS time.apple.com

# Linux
sudo ntpdate pool.ntp.org

# Windows
net start w32time
w32tm /resync
```

2. **增加超时时间：**

在 `WebAuthnService.java` 中修改：

```java
StartRegistrationOptions registrationOptions = StartRegistrationOptions.builder()
    .user(user.toUserIdentity())
    .timeout(120000) // 从 60 秒增加到 120 秒
    .authenticatorSelection(...)
    .build();
```

3. **改进 Challenge 存储（生产环境）：**

当前使用内存存储，建议使用 Redis 等持久化存储：

```java
// 使用 Redis 存储 Challenge
@Autowired
private RedisTemplate<String, PublicKeyCredentialCreationOptions> redisTemplate;

public void saveRegistrationRequest(String key, PublicKeyCredentialCreationOptions options) {
    redisTemplate.opsForValue().set(key, options, 60, TimeUnit.SECONDS);
}
```

---

### 6. Origin 验证失败

**问题描述：**
```
RegistrationFailedException: Origin validation failed
```

**原因：**
客户端的 Origin 与服务器配置的 Origin 不匹配。

**解决方案：**

在 `WebAuthnConfig.java` 中添加正确的 Origin：

```java
@Bean
public RelyingParty relyingParty(UserRepository userRepository) {
    // ... 其他配置
    
    Set<String> origins = new HashSet<>();
    
    // Web 来源
    origins.add("http://localhost:8080");
    origins.add("https://localhost:8080");
    origins.add("https://yourdomain.com");
    
    // Android 来源（需要替换为实际的应用签名）
    origins.add("android:apk-key-hash:YOUR_ACTUAL_APK_KEY_HASH");
    
    return RelyingParty.builder()
        .identity(rpIdentity)
        .credentialRepository(userRepository)
        .origins(origins)
        .allowOriginPort(true)
        .allowOriginSubdomain(true)
        .build();
}
```

**获取 Android 应用签名：**

```bash
# Debug 签名
keytool -list -v -keystore ~/.android/debug.keystore \
    -alias androiddebugkey \
    -storepass android \
    -keypass android

# 从输出中找到 SHA256 指纹
# 将其转换为 Base64 格式并添加到 origins 中
```

---

## Android 集成问题

### 7. Android 无法连接服务器

**问题描述：**
```
java.net.ConnectException: Failed to connect to /10.0.2.2:8080
```

**解决方案：**

1. **检查网络地址：**

```kotlin
// ApiClient.kt
object ApiClient {
    // ✅ 模拟器使用 10.0.2.2
    private const val BASE_URL = "http://10.0.2.2:8080/api/"
    
    // ✅ 真机使用实际 IP
    // private const val BASE_URL = "http://192.168.1.100:8080/api/"
}
```

2. **配置网络安全（允许 HTTP）：**

创建 `res/xml/network_security_config.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">10.0.2.2</domain>
        <domain includeSubdomains="true">192.168.1.100</domain>
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

3. **检查防火墙：**

```bash
# MacOS - 允许端口 8080
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --add java
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --unblock java

# Linux - 使用 ufw
sudo ufw allow 8080/tcp

# Windows - 在防火墙设置中允许端口 8080
```

4. **测试连接：**

```bash
# 从 Android 设备/模拟器访问服务器
# 在 Android Studio 的 Terminal 中执行
adb shell curl http://10.0.2.2:8080/api/passkeys/health
```

---

### 8. Credential Manager 不可用

**问题描述：**
```
java.lang.IllegalStateException: Credential Manager is not available
```

**原因：**
- Android 版本过低（需要 Android 9+）
- Google Play Services 未更新
- 设备不支持

**解决方案：**

1. **检查 Android 版本：**
```kotlin
if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
    // Android 9 (API 28) 以下
    Toast.makeText(context, "设备不支持通行密钥", Toast.LENGTH_LONG).show()
    return
}
```

2. **更新 Google Play Services：**
- 打开 Google Play Store
- 搜索 "Google Play Services"
- 更新到最新版本

3. **检查设备兼容性：**
```kotlin
val credentialManager = CredentialManager.create(context)
try {
    // 尝试获取支持的凭证类型
    val supportedTypes = credentialManager.getSupportedCredentialTypes()
    Log.d("Passkeys", "支持的凭证类型: $supportedTypes")
} catch (e: Exception) {
    Log.e("Passkeys", "设备不支持 Credential Manager", e)
}
```

---

### 9. 生物识别不可用

**问题描述：**
用户点击注册/登录后，没有弹出生物识别界面。

**解决方案：**

1. **检查生物识别设置：**
   - 打开设备设置
   - 进入"安全与隐私"
   - 设置指纹或面部识别

2. **添加生物识别检查：**

```kotlin
import androidx.biometric.BiometricManager

fun checkBiometricAvailability(context: Context): Boolean {
    val biometricManager = BiometricManager.from(context)
    return when (biometricManager.canAuthenticate(
        BiometricManager.Authenticators.BIOMETRIC_STRONG
    )) {
        BiometricManager.BIOMETRIC_SUCCESS -> true
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
            Log.e("Passkeys", "设备不支持生物识别")
            false
        }
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
            Log.e("Passkeys", "未设置生物识别")
            false
        }
        else -> false
    }
}
```

---

## 性能和存储问题

### 10. 内存存储数据丢失

**问题描述：**
服务器重启后，所有用户和凭证数据丢失。

**原因：**
Demo 使用内存存储（`ConcurrentHashMap`），数据不持久化。

**解决方案（生产环境）：**

**方法 1：使用 H2 数据库（简单）**

1. 添加依赖：
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

2. 配置：
```properties
spring.datasource.url=jdbc:h2:file:./data/passkeys
spring.jpa.hibernate.ddl-auto=update
```

**方法 2：使用 PostgreSQL（推荐）**

1. 添加依赖：
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

2. 配置：
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/passkeys
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
```

3. 修改实体类，添加 JPA 注解：
```java
@Entity
@Table(name = "users")
public class User {
    @Id
    private String id;
    // ... 其他字段
}
```

---

## 日志和调试

### 11. 查看详细日志

**启用调试日志：**

编辑 `application.properties`：

```properties
# 详细日志
logging.level.root=INFO
logging.level.com.example.passkeys=DEBUG
logging.level.com.yubico.webauthn=DEBUG

# 输出到文件
logging.file.name=logs/passkeys-server.log
logging.file.max-size=10MB
logging.file.max-history=10
```

**查看 HTTP 请求日志：**

添加日志拦截器：

```java
@Configuration
public class WebConfig {
    @Bean
    public CommonsRequestLoggingFilter logFilter() {
        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
        filter.setIncludeQueryString(true);
        filter.setIncludePayload(true);
        filter.setMaxPayloadLength(10000);
        filter.setIncludeHeaders(true);
        return filter;
    }
}
```

---

## 获取帮助

如果以上方法都无法解决您的问题：

1. **查看日志：**
   - 服务器日志：终端输出或 `logs/passkeys-server.log`
   - Android 日志：Logcat

2. **参考官方文档：**
   - [WebAuthn 规范](https://www.w3.org/TR/webauthn-2/)
   - [Yubico Java WebAuthn Server](https://github.com/Yubico/java-webauthn-server)
   - [Android Credential Manager](https://developer.android.com/training/sign-in/passkeys)

3. **在线资源：**
   - [passkeys.dev](https://passkeys.dev)
   - [awesome-webauthn](https://github.com/herrjemand/awesome-webauthn)

4. **社区支持：**
   - Stack Overflow (标签: `webauthn`, `passkeys`)
   - GitHub Issues

---

**祝您开发顺利！** 🚀

