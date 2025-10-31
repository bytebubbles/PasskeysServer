package com.example.passkeys.dto;

/**
 * 注册请求 DTO
 */
public class RegistrationRequest {
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 显示名称
     */
    private String displayName;
    
    // Getters and Setters
    
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
}

