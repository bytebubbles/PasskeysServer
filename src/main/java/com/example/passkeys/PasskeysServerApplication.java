package com.example.passkeys;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 通行密钥服务器应用主类
 * 
 * 这是一个简单的 Passkeys 后端服务 demo，支持 Android 端的通行密钥注册和登录功能
 */
@SpringBootApplication
public class PasskeysServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PasskeysServerApplication.class, args);
        System.out.println("\n========================================");
        System.out.println("通行密钥服务器已启动！");
        System.out.println("访问地址: http://localhost:8080/api");
        System.out.println("========================================\n");
    }
}

