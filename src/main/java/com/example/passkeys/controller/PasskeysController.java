package com.example.passkeys.controller;

import com.example.passkeys.dto.AuthenticationRequest;
import com.example.passkeys.dto.RegistrationRequest;
import com.example.passkeys.service.WebAuthnService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.RegistrationResult;
import com.yubico.webauthn.data.*;
import com.yubico.webauthn.exception.AssertionFailedException;
import com.yubico.webauthn.exception.RegistrationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通行密钥 REST API 控制器
 * 
 * 提供注册和认证的 HTTP 接口
 */
@RestController
@RequestMapping("/passkeys")
@CrossOrigin(origins = "*") // 允许跨域（生产环境需要配置具体的域名）
public class PasskeysController {
    
    private static final Logger log = LoggerFactory.getLogger(PasskeysController.class);
    
    private final WebAuthnService webAuthnService;
    private final ObjectMapper objectMapper;
    
    public PasskeysController(WebAuthnService webAuthnService, ObjectMapper objectMapper) {
        this.webAuthnService = webAuthnService;
        this.objectMapper = objectMapper;
    }
    
    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Passkeys Server");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 开始注册流程
     * 
     * POST /api/passkeys/register/start
     * Body: { "username": "user@example.com", "displayName": "User Name" }
     */
    @PostMapping("/register/start")
    public ResponseEntity<Map<String, Object>> startRegistration(
            @RequestBody RegistrationRequest request) {
        
        try {
            log.info("收到注册请求 - 用户名: {}", request.getUsername());
            
            if (request.getUsername() == null || request.getUsername().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("用户名不能为空"));
            }
            
            String displayName = request.getDisplayName() != null ? 
                    request.getDisplayName() : request.getUsername();
            
            PublicKeyCredentialCreationOptions options = 
                    webAuthnService.startRegistration(request.getUsername(), displayName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("options", options);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("注册开始失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("注册开始失败: " + e.getMessage()));
        }
    }
    
    /**
     * 完成注册流程
     * 
     * POST /api/passkeys/register/finish
     * Body: { "username": "user@example.com", "credential": {...} }
     */
    @PostMapping("/register/finish")
    public ResponseEntity<Map<String, Object>> finishRegistration(
            @RequestBody JsonNode requestBody) {
        
        try {
            String username = requestBody.get("username").asText();
            JsonNode credentialNode = requestBody.get("credential");
            
            log.info("完成注册 - 用户名: {}", username);
            
            // 解析凭证
            PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> credential = 
                    parseRegistrationCredential(credentialNode);
            
            RegistrationResult result = webAuthnService.finishRegistration(credential, username);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "注册成功");
            response.put("username", username);
            response.put("credentialId", credential.getId().getBase64Url());
            
            return ResponseEntity.ok(response);
            
        } catch (RegistrationFailedException e) {
            log.error("注册验证失败", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("注册验证失败: " + e.getMessage()));
        } catch (Exception e) {
            log.error("注册完成失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("注册完成失败: " + e.getMessage()));
        }
    }
    
    /**
     * 开始认证流程
     * 
     * POST /api/passkeys/authenticate/start
     * Body: { "username": "user@example.com" } 或 {} (使用可发现凭证)
     */
    @PostMapping("/authenticate/start")
    public ResponseEntity<Map<String, Object>> startAuthentication(
            @RequestBody(required = false) AuthenticationRequest request) {
        
        try {
            String username = request != null ? request.getUsername() : null;
            log.info("收到认证请求 - 用户名: {}", username != null ? username : "可发现凭证");
            
            Map<String, Object> result = webAuthnService.startAuthentication(username);
            String requestId = (String) result.get("requestId");
            AssertionRequest assertionRequest = (AssertionRequest) result.get("request");
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("requestId", requestId);
            response.put("options", assertionRequest.getPublicKeyCredentialRequestOptions());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("认证开始失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("认证开始失败: " + e.getMessage()));
        }
    }
    
    /**
     * 完成认证流程
     * 
     * POST /api/passkeys/authenticate/finish
     * Body: { "requestId": "...", "credential": {...} }
     */
    @PostMapping("/authenticate/finish")
    public ResponseEntity<Map<String, Object>> finishAuthentication(
            @RequestBody JsonNode requestBody) {
        
        try {
            String requestId = requestBody.get("requestId").asText();
            JsonNode credentialNode = requestBody.get("credential");
            
            log.info("完成认证 - 请求ID: {}", requestId);
            
            // 解析凭证
            PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> credential = 
                    parseAuthenticationCredential(credentialNode);
            
            Map<String, Object> result = webAuthnService.finishAuthentication(credential, requestId);
            
            return ResponseEntity.ok(result);
            
        } catch (AssertionFailedException e) {
            log.error("认证验证失败", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("认证验证失败: " + e.getMessage()));
        } catch (Exception e) {
            log.error("认证完成失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("认证完成失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取用户列表
     * 
     * GET /api/passkeys/users
     */
    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getUserList() {
        try {
            List<Map<String, Object>> users = webAuthnService.getUserList();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("users", users);
            response.put("count", users.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取用户列表失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("获取用户列表失败: " + e.getMessage()));
        }
    }
    
    // ========== 辅助方法 ==========
    
    /**
     * 解析注册凭证
     */
    private PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> 
            parseRegistrationCredential(JsonNode credentialNode) throws JsonProcessingException, IOException {
        
        log.info("🔍 解析注册凭证前 - 原始字段: {}", credentialNode.fieldNames());
        log.info("🔍 是否包含 extensionsOutputs: {}", credentialNode.has("extensionsOutputs"));
        log.info("🔍 是否包含 clientExtensionResults: {}", credentialNode.has("clientExtensionResults"));
        
        // 预处理：将 extensionsOutputs 字段重命名为 clientExtensionResults
        // 这是为了兼容某些客户端实现（如 Android）使用不同的字段名
        JsonNode processedNode = normalizeExtensionsField(credentialNode);
        
        log.info("🔍 解析注册凭证后 - 处理后字段: {}", processedNode.fieldNames());
        
        String credentialJson = objectMapper.writeValueAsString(processedNode);
        log.info("🔍 最终 JSON: {}", credentialJson);
        
        return PublicKeyCredential.parseRegistrationResponseJson(credentialJson);
    }
    
    /**
     * 解析认证凭证
     */
    private PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> 
            parseAuthenticationCredential(JsonNode credentialNode) throws JsonProcessingException, IOException {
        
        // 预处理：将 extensionsOutputs 字段重命名为 clientExtensionResults
        JsonNode processedNode = normalizeExtensionsField(credentialNode);
        
        String credentialJson = objectMapper.writeValueAsString(processedNode);
        return PublicKeyCredential.parseAssertionResponseJson(credentialJson);
    }
    
    /**
     * 创建错误响应
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
    
    /**
     * 规范化扩展字段名
     * 移除非标准的 extensionsOutputs 字段，保留标准的 clientExtensionResults 字段
     */
    private JsonNode normalizeExtensionsField(JsonNode credentialNode) {
        if (credentialNode.has("extensionsOutputs")) {
            try {
                // 使用 deepCopy 创建可变副本
                com.fasterxml.jackson.databind.node.ObjectNode mutableNode = 
                        ((com.fasterxml.jackson.databind.node.ObjectNode) credentialNode).deepCopy();
                
                // 移除非标准的 extensionsOutputs 字段
                // 如果 clientExtensionResults 不存在，则将 extensionsOutputs 重命名为 clientExtensionResults
                // 如果 clientExtensionResults 已存在，则直接移除 extensionsOutputs
                if (!mutableNode.has("clientExtensionResults")) {
                    JsonNode extensionsOutputs = mutableNode.remove("extensionsOutputs");
                    if (extensionsOutputs != null) {
                        mutableNode.set("clientExtensionResults", extensionsOutputs);
                        log.info("✅ 已将 extensionsOutputs 重命名为 clientExtensionResults");
                    }
                } else {
                    mutableNode.remove("extensionsOutputs");
                    log.info("✅ 已移除重复的 extensionsOutputs 字段（保留 clientExtensionResults）");
                }
                
                return mutableNode;
            } catch (Exception e) {
                log.error("⚠️ 字段转换失败，使用原始节点", e);
            }
        }
        return credentialNode;
    }
}

