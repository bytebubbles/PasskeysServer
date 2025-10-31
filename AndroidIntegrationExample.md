# Android 集成示例

这个文档展示了如何在 Android 应用中集成通行密钥功能，与 Passkeys Server 进行交互。

## 1. 添加依赖

在 `build.gradle` (Module: app) 中添加以下依赖：

```gradle
dependencies {
    // Credential Manager
    implementation "androidx.credentials:credentials:1.2.0"
    implementation "androidx.credentials:credentials-play-services-auth:1.2.0"
    
    // 网络请求
    implementation "com.squareup.retrofit2:retrofit:2.9.0"
    implementation "com.squareup.retrofit2:converter-gson:2.9.0"
    implementation "com.squareup.okhttp3:okhttp:4.11.0"
    implementation "com.squareup.okhttp3:logging-interceptor:4.11.0"
    
    // Kotlin 协程
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3"
    
    // Lifecycle
    implementation "androidx.lifecycle:lifecycle-runtime-ktx:2.6.2"
}
```

## 2. 配置 AndroidManifest.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    
    <!-- 网络权限 -->
    <uses-permission android:name="android.permission.INTERNET" />
    
    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/Theme.AppCompat">
        
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        
    </application>
    
</manifest>
```

## 3. 创建 API 接口

创建 `PasskeysApiService.kt`：

```kotlin
package com.example.passkeysapp.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface PasskeysApiService {
    
    @POST("passkeys/register/start")
    suspend fun startRegistration(
        @Body request: RegistrationStartRequest
    ): Response<RegistrationStartResponse>
    
    @POST("passkeys/register/finish")
    suspend fun finishRegistration(
        @Body request: RegistrationFinishRequest
    ): Response<RegistrationFinishResponse>
    
    @POST("passkeys/authenticate/start")
    suspend fun startAuthentication(
        @Body request: AuthenticationStartRequest
    ): Response<AuthenticationStartResponse>
    
    @POST("passkeys/authenticate/finish")
    suspend fun finishAuthentication(
        @Body request: AuthenticationFinishRequest
    ): Response<AuthenticationFinishResponse>
}

// 请求和响应数据类
data class RegistrationStartRequest(
    val username: String,
    val displayName: String
)

data class RegistrationStartResponse(
    val success: Boolean,
    val options: String // JSON 字符串
)

data class RegistrationFinishRequest(
    val username: String,
    val credential: String // JSON 字符串
)

data class RegistrationFinishResponse(
    val success: Boolean,
    val message: String,
    val username: String,
    val credentialId: String
)

data class AuthenticationStartRequest(
    val username: String? = null
)

data class AuthenticationStartResponse(
    val success: Boolean,
    val options: String // JSON 字符串
)

data class AuthenticationFinishRequest(
    val credential: String // JSON 字符串
)

data class AuthenticationFinishResponse(
    val success: Boolean,
    val username: String,
    val userId: String,
    val displayName: String,
    val credentialId: String
)
```

## 4. 创建 Retrofit 客户端

创建 `ApiClient.kt`：

```kotlin
package com.example.passkeysapp.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    
    // 修改为你的服务器地址
    private const val BASE_URL = "http://10.0.2.2:8080/api/"
    // 注意: 10.0.2.2 是 Android 模拟器访问主机 localhost 的特殊 IP
    // 如果使用真机，请使用实际的服务器 IP 地址
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val passkeysApi: PasskeysApiService = retrofit.create(PasskeysApiService::class.java)
}
```

## 5. 创建通行密钥管理类

创建 `PasskeysManager.kt`：

```kotlin
package com.example.passkeysapp

import android.content.Context
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import com.example.passkeysapp.api.ApiClient
import com.example.passkeysapp.api.AuthenticationFinishRequest
import com.example.passkeysapp.api.AuthenticationStartRequest
import com.example.passkeysapp.api.RegistrationFinishRequest
import com.example.passkeysapp.api.RegistrationStartRequest
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PasskeysManager(private val context: Context) {
    
    private val credentialManager = CredentialManager.create(context)
    private val gson = Gson()
    
    /**
     * 注册通行密钥
     */
    suspend fun register(username: String, displayName: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. 从服务器获取注册选项
                val startResponse = ApiClient.passkeysApi.startRegistration(
                    RegistrationStartRequest(username, displayName)
                )
                
                if (!startResponse.isSuccessful || startResponse.body()?.success != true) {
                    return@withContext Result.failure(
                        Exception("获取注册选项失败: ${startResponse.message()}")
                    )
                }
                
                val optionsJson = startResponse.body()!!.options
                
                // 2. 使用 Credential Manager 创建凭证
                val createRequest = CreatePublicKeyCredentialRequest(optionsJson)
                val credential = withContext(Dispatchers.Main) {
                    credentialManager.createCredential(
                        request = createRequest,
                        context = context
                    ) as CreatePublicKeyCredentialResponse
                }
                
                // 3. 将凭证发送到服务器完成注册
                val finishResponse = ApiClient.passkeysApi.finishRegistration(
                    RegistrationFinishRequest(
                        username = username,
                        credential = credential.registrationResponseJson
                    )
                )
                
                if (!finishResponse.isSuccessful || finishResponse.body()?.success != true) {
                    return@withContext Result.failure(
                        Exception("完成注册失败: ${finishResponse.message()}")
                    )
                }
                
                Result.success("注册成功！凭证 ID: ${finishResponse.body()!!.credentialId}")
                
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * 使用通行密钥登录
     */
    suspend fun authenticate(username: String? = null): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. 从服务器获取认证选项
                val startResponse = ApiClient.passkeysApi.startAuthentication(
                    AuthenticationStartRequest(username)
                )
                
                if (!startResponse.isSuccessful || startResponse.body()?.success != true) {
                    return@withContext Result.failure(
                        Exception("获取认证选项失败: ${startResponse.message()}")
                    )
                }
                
                val optionsJson = startResponse.body()!!.options
                
                // 2. 使用 Credential Manager 获取凭证
                val getPublicKeyCredentialOption = GetPublicKeyCredentialOption(optionsJson)
                val getCredRequest = GetCredentialRequest(
                    listOf(getPublicKeyCredentialOption)
                )
                
                val credential = withContext(Dispatchers.Main) {
                    val credentialResponse = credentialManager.getCredential(
                        request = getCredRequest,
                        context = context
                    )
                    credentialResponse.credential as PublicKeyCredential
                }
                
                // 3. 将凭证发送到服务器完成认证
                val finishResponse = ApiClient.passkeysApi.finishAuthentication(
                    AuthenticationFinishRequest(
                        credential = credential.authenticationResponseJson
                    )
                )
                
                if (!finishResponse.isSuccessful || finishResponse.body()?.success != true) {
                    return@withContext Result.failure(
                        Exception("认证失败: ${finishResponse.message()}")
                    )
                }
                
                val userData = finishResponse.body()!!
                Result.success("登录成功！用户: ${userData.username} (${userData.displayName})")
                
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
```

## 6. 在 Activity 中使用

创建或修改 `MainActivity.kt`：

```kotlin
package com.example.passkeysapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private lateinit var passkeysManager: PasskeysManager
    
    private lateinit var usernameInput: EditText
    private lateinit var displayNameInput: EditText
    private lateinit var registerButton: Button
    private lateinit var loginButton: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // 初始化 PasskeysManager
        passkeysManager = PasskeysManager(this)
        
        // 初始化视图
        usernameInput = findViewById(R.id.username_input)
        displayNameInput = findViewById(R.id.display_name_input)
        registerButton = findViewById(R.id.register_button)
        loginButton = findViewById(R.id.login_button)
        
        // 注册按钮点击事件
        registerButton.setOnClickListener {
            val username = usernameInput.text.toString()
            val displayName = displayNameInput.text.toString()
            
            if (username.isEmpty()) {
                Toast.makeText(this, "请输入用户名", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            registerPasskey(username, displayName.ifEmpty { username })
        }
        
        // 登录按钮点击事件
        loginButton.setOnClickListener {
            val username = usernameInput.text.toString()
            authenticateWithPasskey(username.ifEmpty { null })
        }
    }
    
    /**
     * 注册通行密钥
     */
    private fun registerPasskey(username: String, displayName: String) {
        lifecycleScope.launch {
            try {
                Toast.makeText(this@MainActivity, "正在注册...", Toast.LENGTH_SHORT).show()
                
                val result = passkeysManager.register(username, displayName)
                
                if (result.isSuccess) {
                    Toast.makeText(
                        this@MainActivity,
                        result.getOrNull() ?: "注册成功",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "注册失败: ${result.exceptionOrNull()?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "注册错误: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    /**
     * 使用通行密钥登录
     */
    private fun authenticateWithPasskey(username: String?) {
        lifecycleScope.launch {
            try {
                Toast.makeText(this@MainActivity, "正在登录...", Toast.LENGTH_SHORT).show()
                
                val result = passkeysManager.authenticate(username)
                
                if (result.isSuccess) {
                    Toast.makeText(
                        this@MainActivity,
                        result.getOrNull() ?: "登录成功",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "登录失败: ${result.exceptionOrNull()?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "登录错误: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
```

## 7. 创建布局文件

创建 `res/layout/activity_main.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="24dp"
    android:gravity="center">
    
    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="通行密钥 Demo"
        android:textSize="24sp"
        android:textStyle="bold"
        android:layout_marginBottom="32dp" />
    
    <com.google.android.material.textfield.TextInputLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginBottom="16dp"
        android:hint="用户名">
        
        <EditText
            android:id="@+id/username_input"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:inputType="textEmailAddress" />
    </com.google.android.material.textfield.TextInputLayout>
    
    <com.google.android.material.textfield.TextInputLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginBottom="24dp"
        android:hint="显示名称（可选）">
        
        <EditText
            android:id="@+id/display_name_input"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:inputType="textPersonName" />
    </com.google.android.material.textfield.TextInputLayout>
    
    <Button
        android:id="@+id/register_button"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginBottom="16dp"
        android:text="注册通行密钥"
        android:textSize="16sp" />
    
    <Button
        android:id="@+id/login_button"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="使用通行密钥登录"
        android:textSize="16sp" />
    
</LinearLayout>
```

## 8. 配置网络安全（HTTP 访问）

如果你的服务器使用 HTTP（非 HTTPS），需要配置网络安全。

创建 `res/xml/network_security_config.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">10.0.2.2</domain>
        <domain includeSubdomains="true">localhost</domain>
        <!-- 添加你的服务器 IP -->
        <domain includeSubdomains="true">192.168.x.x</domain>
    </domain-config>
</network-security-config>
```

在 `AndroidManifest.xml` 中引用：

```xml
<application
    android:networkSecurityConfig="@xml/network_security_config"
    ...>
    ...
</application>
```

## 9. 注意事项

### 服务器地址配置

- **模拟器**: 使用 `10.0.2.2:8080` 访问主机的 localhost
- **真机**: 使用服务器的实际 IP 地址，如 `192.168.1.100:8080`
- **生产环境**: 使用 HTTPS 和实际域名

### 获取应用签名

为了配置服务器端的 Android 应用来源（origin），需要获取应用的 SHA-256 签名：

```bash
# Debug 签名
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android

# Release 签名
keytool -list -v -keystore /path/to/your/release.keystore -alias your-alias
```

将输出的 SHA256 指纹配置到服务器的 `WebAuthnConfig.java` 中。

### 测试流程

1. 启动 Passkeys Server
2. 运行 Android 应用
3. 输入用户名和显示名称
4. 点击"注册通行密钥"
5. 使用设备生物识别（指纹或面部识别）完成注册
6. 点击"使用通行密钥登录"
7. 使用设备生物识别完成登录

## 10. 常见问题

### Q: 无法连接到服务器？

A: 检查以下几点：
- 服务器是否正在运行
- 网络地址是否正确（模拟器使用 10.0.2.2，真机使用实际 IP）
- 防火墙是否允许 8080 端口
- 网络安全配置是否正确

### Q: 注册或登录失败？

A: 可能的原因：
- 设备不支持生物识别
- Android 版本过低（需要 Android 9+）
- Google Play Services 未更新
- 应用签名未正确配置

### Q: 如何调试？

A: 
- 查看 Logcat 日志
- 使用 OkHttp 的日志拦截器查看网络请求
- 在服务器端查看日志

## 参考资料

- [Android Credential Manager 文档](https://developer.android.com/training/sign-in/passkeys)
- [Google 通行密钥指南](https://developers.google.com/identity/passkeys)
- [WebAuthn 规范](https://www.w3.org/TR/webauthn-2/)

