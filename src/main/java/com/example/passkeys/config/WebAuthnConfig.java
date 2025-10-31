package com.example.passkeys.config;

import com.example.passkeys.repository.UserRepository;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
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
        origins.add("http://localhost:8080");
        origins.add("https://localhost:8080");
        // Android 应用的来源格式
        origins.add("android:apk-key-hash:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        
        return RelyingParty.builder()
                .identity(rpIdentity)
                .credentialRepository(userRepository)
                .origins(origins)
                .allowOriginPort(true)
                .allowOriginSubdomain(true)
                .build();
    }
}

