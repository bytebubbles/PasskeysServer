package com.example.passkeys.repository;

import com.example.passkeys.model.Authenticator;
import com.example.passkeys.model.User;
import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 用户和凭证存储仓库
 * 
 * 使用内存存储（适合 Demo 演示）
 * 生产环境应该使用数据库持久化存储
 */
@Repository
public class UserRepository implements CredentialRepository {
    
    // 用户存储：username -> User
    private final Map<String, User> usersByUsername = new ConcurrentHashMap<>();
    
    // 用户存储：userId -> User
    private final Map<String, User> usersById = new ConcurrentHashMap<>();
    
    // 凭证存储：credentialId -> Authenticator
    private final Map<String, Authenticator> authenticators = new ConcurrentHashMap<>();
    
    /**
     * 根据用户名查找用户
     */
    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(usersByUsername.get(username));
    }
    
    /**
     * 根据用户 ID 查找用户
     */
    public Optional<User> findById(String userId) {
        return Optional.ofNullable(usersById.get(userId));
    }
    
    /**
     * 保存用户
     */
    public void saveUser(User user) {
        usersByUsername.put(user.getUsername(), user);
        usersById.put(user.getId(), user);
    }
    
    /**
     * 保存认证器
     */
    public void saveAuthenticator(Authenticator authenticator) {
        authenticators.put(authenticator.getCredentialId(), authenticator);
        
        // 同时更新用户的认证器列表
        findById(authenticator.getUserId()).ifPresent(user -> {
            user.addAuthenticator(authenticator);
            saveUser(user);
        });
    }
    
    /**
     * 根据凭证 ID 查找认证器
     */
    public Optional<Authenticator> findAuthenticatorByCredentialId(String credentialId) {
        return Optional.ofNullable(authenticators.get(credentialId));
    }
    
    /**
     * 获取所有用户
     */
    public List<User> findAllUsers() {
        return new ArrayList<>(usersByUsername.values());
    }
    
    // ========== 实现 CredentialRepository 接口 ==========
    
    @Override
    public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
        return findByUsername(username)
                .map(user -> user.getAuthenticators().stream()
                        .map(auth -> PublicKeyCredentialDescriptor.builder()
                                .id(parseBase64Url(auth.getCredentialId()))
                                .build())
                        .collect(Collectors.toSet()))
                .orElse(Collections.emptySet());
    }
    
    @Override
    public Optional<ByteArray> getUserHandleForUsername(String username) {
        return findByUsername(username)
                .map(user -> parseBase64Url(user.getId()));
    }
    
    @Override
    public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
        return findById(userHandle.getBase64Url())
                .map(User::getUsername);
    }
    
    @Override
    public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
        return findAuthenticatorByCredentialId(credentialId.getBase64Url())
                .filter(auth -> auth.getUserId().equals(userHandle.getBase64Url()))
                .map(auth -> RegisteredCredential.builder()
                        .credentialId(parseBase64Url(auth.getCredentialId()))
                        .userHandle(parseBase64Url(auth.getUserId()))
                        .publicKeyCose(parseBase64Url(auth.getPublicKey()))
                        .signatureCount(auth.getSignCount())
                        .build());
    }
    
    @Override
    public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
        return findAuthenticatorByCredentialId(credentialId.getBase64Url())
                .map(auth -> Collections.singleton(
                        RegisteredCredential.builder()
                                .credentialId(parseBase64Url(auth.getCredentialId()))
                                .userHandle(parseBase64Url(auth.getUserId()))
                                .publicKeyCose(parseBase64Url(auth.getPublicKey()))
                                .signatureCount(auth.getSignCount())
                                .build()))
                .orElse(Collections.emptySet());
    }
    
    /**
     * 辅助方法：解析 Base64Url 字符串
     */
    private ByteArray parseBase64Url(String base64Url) {
        try {
            return ByteArray.fromBase64Url(base64Url);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Base64Url: " + base64Url, e);
        }
    }
}

