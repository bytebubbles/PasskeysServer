package com.example.passkeys.model;

import com.yubico.webauthn.data.AttestedCredentialData;
import com.yubico.webauthn.data.AuthenticatorAttestationResponse;
import com.yubico.webauthn.data.ByteArray;

import java.util.Optional;

/**
 * 认证器实体类
 * 存储用户的通行密钥凭证信息
 */
public class Authenticator {
    
    /**
     * 凭证 ID（Base64Url 编码）
     */
    private String credentialId;
    
    /**
     * 用户 ID
     */
    private String userId;
    
    /**
     * 公钥（Base64Url 编码）
     */
    private String publicKey;
    
    /**
     * 签名计数器
     */
    private long signCount;
    
    /**
     * AAGUID（认证器唯一标识）
     */
    private String aaguid;
    
    /**
     * 创建时间戳
     */
    private long createdAt;
    
    /**
     * 最后使用时间戳
     */
    private long lastUsedAt;
    
    /**
     * 认证器名称（可选）
     */
    private String name;
    
    public Authenticator() {
        this.createdAt = System.currentTimeMillis();
        this.lastUsedAt = this.createdAt;
    }
    
    // Getters and Setters
    
    public String getCredentialId() {
        return credentialId;
    }
    
    public void setCredentialId(String credentialId) {
        this.credentialId = credentialId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getPublicKey() {
        return publicKey;
    }
    
    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }
    
    public long getSignCount() {
        return signCount;
    }
    
    public void setSignCount(long signCount) {
        this.signCount = signCount;
    }
    
    public String getAaguid() {
        return aaguid;
    }
    
    public void setAaguid(String aaguid) {
        this.aaguid = aaguid;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    public long getLastUsedAt() {
        return lastUsedAt;
    }
    
    public void setLastUsedAt(long lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * 从注册响应创建认证器
     */
    public static Authenticator fromRegistrationResponse(
            String userId,
            AuthenticatorAttestationResponse response,
            ByteArray credentialId,
            long signCount) {
        
        Authenticator authenticator = new Authenticator();
        authenticator.setUserId(userId);
        authenticator.setCredentialId(credentialId.getBase64Url());
        authenticator.setSignCount(signCount);
        
        // 提取公钥和 AAGUID
        Optional<AttestedCredentialData> attestedCredentialData = 
                response.getAttestation().getAuthenticatorData().getAttestedCredentialData();
        
        if (attestedCredentialData.isPresent()) {
            AttestedCredentialData credData = attestedCredentialData.get();
            authenticator.setPublicKey(credData.getCredentialPublicKey().getBase64Url());
            authenticator.setAaguid(credData.getAaguid().getBase64Url());
        }
        
        return authenticator;
    }
    
    /**
     * 更新签名计数器
     */
    public void updateSignCount(long newSignCount) {
        this.signCount = newSignCount;
        this.lastUsedAt = System.currentTimeMillis();
    }
}

