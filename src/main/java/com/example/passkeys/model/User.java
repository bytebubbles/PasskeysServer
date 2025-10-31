package com.example.passkeys.model;

import com.yubico.webauthn.data.UserIdentity;
import com.yubico.webauthn.data.ByteArray;

import java.util.HashSet;
import java.util.Set;

/**
 * 用户实体类
 * 存储用户的基本信息和关联的认证器
 */
public class User {
    
    /**
     * 用户唯一标识符（用户句柄）
     */
    private String id;
    
    /**
     * 用户名（用于显示）
     */
    private String username;
    
    /**
     * 用户显示名称
     */
    private String displayName;
    
    /**
     * 用户关联的认证器集合
     */
    private Set<Authenticator> authenticators = new HashSet<>();
    
    /**
     * 创建时间戳
     */
    private long createdAt;
    
    public User(String id, String username, String displayName) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.createdAt = System.currentTimeMillis();
    }
    
    // Getters and Setters
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public Set<Authenticator> getAuthenticators() {
        return authenticators;
    }
    
    public void setAuthenticators(Set<Authenticator> authenticators) {
        this.authenticators = authenticators;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    /**
     * 转换为 WebAuthn UserIdentity
     */
    public UserIdentity toUserIdentity() {
        try {
            return UserIdentity.builder()
                    .name(username)
                    .displayName(displayName)
                    .id(ByteArray.fromBase64Url(id))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create UserIdentity", e);
        }
    }
    
    /**
     * 添加认证器
     */
    public void addAuthenticator(Authenticator authenticator) {
        this.authenticators.add(authenticator);
    }
}

