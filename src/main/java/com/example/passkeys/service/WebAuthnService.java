package com.example.passkeys.service;

import com.example.passkeys.model.Authenticator;
import com.example.passkeys.model.User;
import com.example.passkeys.repository.UserRepository;
import com.yubico.webauthn.*;
import com.yubico.webauthn.data.*;
import com.yubico.webauthn.exception.AssertionFailedException;
import com.yubico.webauthn.exception.RegistrationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebAuthn 服务类
 * 
 * 实现通行密钥的注册和认证功能
 * 这是服务器端的核心业务逻辑
 */
@Service
public class WebAuthnService {
    
    private static final Logger log = LoggerFactory.getLogger(WebAuthnService.class);
    
    private final RelyingParty relyingParty;
    private final UserRepository userRepository;
    private final SecureRandom random = new SecureRandom();
    
    public WebAuthnService(RelyingParty relyingParty, UserRepository userRepository) {
        this.relyingParty = relyingParty;
        this.userRepository = userRepository;
    }
    
    // 临时存储注册请求（实际生产环境应使用 Redis 等）
    private final Map<String, PublicKeyCredentialCreationOptions> registrationRequests = 
            new ConcurrentHashMap<>();
    
    // 临时存储认证请求（实际生产环境应使用 Redis 等）
    private final Map<String, AssertionRequest> assertionRequests = 
            new ConcurrentHashMap<>();
    
    /**
     * 开始注册流程
     * 
     * @param username 用户名
     * @param displayName 显示名称
     * @return 注册选项（PublicKeyCredentialCreationOptions）
     */
    public PublicKeyCredentialCreationOptions startRegistration(String username, String displayName) {
        log.info("开始注册流程 - 用户名: {}", username);
        
        // 检查用户是否已存在
        Optional<User> existingUser = userRepository.findByUsername(username);
        User user;
        
        if (existingUser.isPresent()) {
            // 用户已存在，为现有用户添加新的通行密钥
            user = existingUser.get();
            log.info("为现有用户添加新通行密钥: {}", username);
        } else {
            // 创建新用户
            String userId = generateUserId();
            user = new User(userId, username, displayName);
            userRepository.saveUser(user);
            log.info("创建新用户 - ID: {}, 用户名: {}", userId, username);
        }
        
        // 构建注册选项
        StartRegistrationOptions registrationOptions = StartRegistrationOptions.builder()
                .user(user.toUserIdentity())
                .authenticatorSelection(AuthenticatorSelectionCriteria.builder()
                        // 要求使用平台认证器（如 Android 生物识别）
                        .authenticatorAttachment(AuthenticatorAttachment.PLATFORM)
                        // 要求用户验证
                        .userVerification(UserVerificationRequirement.REQUIRED)
                        // 要求可发现的凭证（Discoverable Credential / Resident Key）
                        .residentKey(ResidentKeyRequirement.REQUIRED)
                        .build())
                .build();
        
        PublicKeyCredentialCreationOptions options = relyingParty.startRegistration(registrationOptions);
        
        // 保存注册请求用于后续验证
        String requestId = generateRequestId();
        registrationRequests.put(requestId, options);
        
        log.info("生成注册选项 - 请求ID: {}, Challenge: {}", 
                requestId, options.getChallenge().getBase64Url());
        
        return options;
    }
    
    /**
     * 完成注册流程
     * 
     * @param credential 客户端返回的凭证
     * @param username 用户名
     * @return 注册结果
     */
    public RegistrationResult finishRegistration(
            PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> credential,
            String username) throws RegistrationFailedException, IOException {
        
        log.info("完成注册流程 - 用户名: {}, 凭证ID: {}", 
                username, credential.getId().getBase64Url());
        
        // 查找用户
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + username));
        
        // 查找对应的注册请求
        PublicKeyCredentialCreationOptions requestOptions = findRegistrationRequest(credential);
        
        // 验证并完成注册
        FinishRegistrationOptions options = FinishRegistrationOptions.builder()
                .request(requestOptions)
                .response(credential)
                .build();
        
        RegistrationResult result = relyingParty.finishRegistration(options);
        
        // 保存认证器信息
        Authenticator authenticator = Authenticator.fromRegistrationResponse(
                user.getId(),
                credential.getResponse(),
                credential.getId(),
                result.getSignatureCount()
        );
        authenticator.setName("Android 设备"); // 可以根据需要自定义
        
        userRepository.saveAuthenticator(authenticator);
        
        log.info("注册成功 - 用户: {}, 凭证ID: {}, 签名计数: {}", 
                username, authenticator.getCredentialId(), authenticator.getSignCount());
        
        return result;
    }
    
    /**
     * 开始认证流程
     * 
     * @param username 用户名（可选，如果为空则使用可发现凭证）
     * @return 认证选项（AssertionRequest）
     */
    public AssertionRequest startAuthentication(String username) {
        log.info("开始认证流程 - 用户名: {}", username != null ? username : "可发现凭证");
        
        StartAssertionOptions.StartAssertionOptionsBuilder optionsBuilder = 
                StartAssertionOptions.builder();
        
        if (username != null && !username.isEmpty()) {
            // 指定用户名的认证
            optionsBuilder.username(username);
        }
        
        // 要求用户验证
        optionsBuilder.userVerification(UserVerificationRequirement.REQUIRED);
        
        AssertionRequest request = relyingParty.startAssertion(optionsBuilder.build());
        
        // 保存认证请求用于后续验证
        String requestId = generateRequestId();
        assertionRequests.put(requestId, request);
        
        log.info("生成认证选项 - 请求ID: {}, Challenge: {}", 
                requestId, request.getPublicKeyCredentialRequestOptions().getChallenge().getBase64Url());
        
        return request;
    }
    
    /**
     * 完成认证流程
     * 
     * @param credential 客户端返回的凭证
     * @return 认证结果和用户信息
     */
    public Map<String, Object> finishAuthentication(
            PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> credential) 
            throws AssertionFailedException {
        
        log.info("完成认证流程 - 凭证ID: {}", credential.getId().getBase64Url());
        
        // 查找对应的认证请求
        AssertionRequest request = findAssertionRequest(credential);
        
        // 验证并完成认证
        FinishAssertionOptions options = FinishAssertionOptions.builder()
                .request(request)
                .response(credential)
                .build();
        
        AssertionResult result = relyingParty.finishAssertion(options);
        
        if (!result.isSuccess()) {
            throw new AssertionFailedException("认证失败");
        }
        
        // 更新认证器的签名计数器
        String credentialId = credential.getId().getBase64Url();
        Authenticator authenticator = userRepository.findAuthenticatorByCredentialId(credentialId)
                .orElseThrow(() -> new IllegalArgumentException("认证器不存在"));
        
        authenticator.updateSignCount(result.getSignatureCount());
        userRepository.saveAuthenticator(authenticator);
        
        // 获取用户信息
        User user = userRepository.findById(authenticator.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        
        log.info("认证成功 - 用户: {}, 凭证ID: {}, 新签名计数: {}", 
                user.getUsername(), credentialId, result.getSignatureCount());
        
        // 返回认证结果
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("username", user.getUsername());
        response.put("userId", user.getId());
        response.put("displayName", user.getDisplayName());
        response.put("credentialId", credentialId);
        
        return response;
    }
    
    /**
     * 获取用户列表
     */
    public List<Map<String, Object>> getUserList() {
        List<Map<String, Object>> userList = new ArrayList<>();
        
        for (User user : userRepository.findAllUsers()) {
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("username", user.getUsername());
            userInfo.put("displayName", user.getDisplayName());
            userInfo.put("userId", user.getId());
            userInfo.put("authenticatorCount", user.getAuthenticators().size());
            userInfo.put("createdAt", user.getCreatedAt());
            userList.add(userInfo);
        }
        
        return userList;
    }
    
    // ========== 辅助方法 ==========
    
    /**
     * 生成用户 ID
     */
    private String generateUserId() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return new ByteArray(bytes).getBase64Url();
    }
    
    /**
     * 生成请求 ID
     */
    private String generateRequestId() {
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    
    /**
     * 查找注册请求
     */
    private PublicKeyCredentialCreationOptions findRegistrationRequest(
            PublicKeyCredential<AuthenticatorAttestationResponse, ?> credential) {
        
        // 简化处理：查找匹配的请求
        // 生产环境应该通过 session 或其他机制精确匹配
        for (PublicKeyCredentialCreationOptions options : registrationRequests.values()) {
            return options; // 返回第一个（Demo 简化处理）
        }
        throw new IllegalArgumentException("找不到对应的注册请求");
    }
    
    /**
     * 查找认证请求
     */
    private AssertionRequest findAssertionRequest(
            PublicKeyCredential<AuthenticatorAssertionResponse, ?> credential) {
        
        // 简化处理：查找匹配的请求
        // 生产环境应该通过 session 或其他机制精确匹配
        for (AssertionRequest request : assertionRequests.values()) {
            return request; // 返回第一个（Demo 简化处理）
        }
        throw new IllegalArgumentException("找不到对应的认证请求");
    }
}

