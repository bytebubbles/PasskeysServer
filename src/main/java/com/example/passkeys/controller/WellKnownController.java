package com.example.passkeys.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * .well-known 路径控制器
 * 提供 Digital Asset Links 文件
 * 
 * 注意：这个控制器不使用 @RequestMapping("/api")
 * 因为 assetlinks.json 必须在根路径可访问
 */
@RestController
public class WellKnownController {
    
    /**
     * 提供 assetlinks.json 文件
     * 
     * 路径：/.well-known/assetlinks.json
     * 这个路径必须在根路径，不能有 /api 前缀
     */
    @GetMapping(value = "/.well-known/assetlinks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getAssetLinks() throws IOException {
        Resource resource = new ClassPathResource("static/.well-known/assetlinks.json");
        
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        
        // 使用 InputStream 而不是 getFile()，这样在 JAR 中也能工作
        String content = new String(
            resource.getInputStream().readAllBytes(), 
            StandardCharsets.UTF_8
        );
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setCacheControl("no-cache");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(content);
    }
}

