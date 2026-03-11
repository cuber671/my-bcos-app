package com.fisco.app.Common.Service.impl;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fisco.app.Common.Service.EncryptionService;
import com.fisco.app.Common.Utils.AesUtil;
import com.fisco.app.Common.Utils.RsaUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * 敏感数据加密服务实现类
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Slf4j
@Service
public class EncryptionServiceImpl implements EncryptionService {

    /**
     * RSA密钥对缓存
     */
    private volatile RsaUtil.KeyPairHolder rsaKeyPairHolder;

    @Value("${encrypt.rsa.enabled:true}")
    private boolean rsaEnabled;

    @Value("${encrypt.aes.key:}")
    private String aesKey;

    @Override
    public String getRsaPublicKey() {
        if (!rsaEnabled) {
            log.warn("RSA加密未启用");
            return null;
        }

        if (rsaKeyPairHolder == null) {
            synchronized (this) {
                if (rsaKeyPairHolder == null) {
                    rsaKeyPairHolder = RsaUtil.createKeyPairHolder();
                    log.info("RSA密钥对已生成");
                }
            }
        }
        return rsaKeyPairHolder.getPublicKeyString();
    }

    @Override
    public String decryptWithPrivateKey(String encryptedData) {
        if (!rsaEnabled) {
            log.warn("RSA加密未启用");
            throw new IllegalStateException("RSA encryption is not enabled");
        }

        if (rsaKeyPairHolder == null) {
            throw new IllegalStateException("RSA key pair not initialized");
        }

        // 私钥解密仅在内存中操作，不持久化
        return RsaUtil.decrypt(encryptedData, rsaKeyPairHolder.getPrivateKey());
    }

    @Override
    public Map<String, String> generateRsaKeyPair() {
        RsaUtil.KeyPairHolder newKeyPair = RsaUtil.createKeyPairHolder();

        // 替换旧的密钥对
        synchronized (this) {
            rsaKeyPairHolder = newKeyPair;
        }

        Map<String, String> result = new HashMap<>();
        result.put("publicKey", newKeyPair.getPublicKeyString());
        result.put("privateKey", newKeyPair.getPrivateKeyString());

        log.info("RSA密钥对已重新生成");
        return result;
    }

    @Override
    public String encryptWithAes(String data) {
        return AesUtil.encrypt(data, aesKey);
    }

    @Override
    public String decryptWithAes(String encryptedData) {
        return AesUtil.decrypt(encryptedData, aesKey);
    }

    @Override
    public String generateAesKey() {
        return AesUtil.generateKey();
    }
}
