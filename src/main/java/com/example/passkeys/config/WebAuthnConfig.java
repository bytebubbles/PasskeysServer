package com.example.passkeys.config;

import com.example.passkeys.Base64UrlUtils;
import com.example.passkeys.repository.UserRepository;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.Set;

/**
 * WebAuthn 配置类
 * 配置依赖方（Relying Party）信息
 */
@Configuration
public class WebAuthnConfig {

    static String host = "localhost";

    @Value("${webauthn.rp.id:192.168.2.191}")
    private String rpId;

    @Value("${webauthn.rp.name:Passkeys Demo Server}")
    private String rpName;

    @Value("${webauthn.rp.origin:http://localhost:8080}")
    private String rpOrigin;

    private static final Logger log = LoggerFactory.getLogger(WebAuthnConfig.class);

    /**
     * 配置 RelyingParty（依赖方）
     * 这是 WebAuthn 服务器的核心配置
     */
    @Bean
    public RelyingParty relyingParty(UserRepository userRepository) {
        RelyingPartyIdentity rpIdentity = RelyingPartyIdentity.builder()
                .id(rpId)
                .name(rpName)
                .build();

        // 支持多个来源（Web 和 Android）
        Set<String> origins = new HashSet<>();
        //origins.add("http://localhost:8080");
        //origins.add("https://localhost:8080");

        // Android 应用的来源格式
        // rpOrigin 可能是完整的 android:apk-key-hash:... 格式，也可能只是哈希值
//        if (rpOrigin != null && !rpOrigin.isEmpty()) {
//            if (rpOrigin.startsWith("android:apk-key-hash:")) {
//                // 如果已经是完整格式，直接添加
//                origins.add(rpOrigin);
//            } else if (rpOrigin.startsWith("http://") || rpOrigin.startsWith("https://")) {
//                // 如果是 HTTP/HTTPS origin，直接添加
//                origins.add(rpOrigin);
//            } else {
//                // 如果只是哈希值，添加前缀
//                origins.add("android:apk-key-hash:" + rpOrigin);
//            }
//        }
//
        // 添加 Android 应用的实际哈希（Base64 URL 编码格式）
        // 从错误信息中获取的实际 origin
        origins.add("android:apk-key-hash:RLzsOERGQxO/pMp7NrqhLJ9as+BkYS5L2cCrGPQ9TY4");
        origins.add("android:apk-key-hash:RLzsOERGQxO_pMp7NrqhLJ9as-BkYS5L2cCrGPQ9TY4");
        String baseurl = Base64UrlUtils.apkKeyHashToBase64Url("35:9E:3B:E6:83:AC:EC:78:AF:A6:23:C7:76:13:E1:0C:3F:D2:27:B6:49:2D:4E:5D:E5:B8:B9:01:AD:51:61:94");
        log.info("baseurl: {}", baseurl);
        origins.add("android:apk-key-hash:"+baseurl);

        return RelyingParty.builder()
                .identity(rpIdentity)
                .credentialRepository(userRepository)
                .origins(origins)
                .allowOriginPort(true)
                .allowOriginSubdomain(true)
                .build();
    }
}

