package com.example.passkeys.dto;

/**
 * 认证请求 DTO
 */
public class AuthenticationRequest {
    
    /**
     * 用户名（可选，为空时使用可发现凭证）
     */
    private String username;
    
    // Getters and Setters
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
}

