package com.fisco.app.Common.Service;

import java.util.Map;

/**
 * 敏感数据加密服务接口
 * 提供RSA和AES加密解密功能
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
public interface EncryptionService {

    // ==================== RSA 相关 ====================

    /**
     * 获取RSA公钥（供前端加密使用）
     *
     * @return Base64编码的RSA公钥
     */
    String getRsaPublicKey();

    /**
     * RSA私钥解密（用于解密前端传来的私钥/助记词）
     *
     * @param encryptedData Base64编码的加密数据
     * @return 解密后的明文
     */
    String decryptWithPrivateKey(String encryptedData);

    /**
     * 生成新的RSA密钥对
     *
     * @return 包含公钥和私钥的Map
     */
    Map<String, String> generateRsaKeyPair();

    // ==================== AES 相关 ====================

    /**
     * AES加密（用于交易哈希/资产ID加密）
     *
     * @param data 待加密数据
     * @return Base64编码的加密数据
     */
    String encryptWithAes(String data);

    /**
     * AES解密
     *
     * @param encryptedData Base64编码的加密数据
     * @return 解密后的明文
     */
    String decryptWithAes(String encryptedData);

    /**
     * 生成AES密钥
     *
     * @return Base64编码的AES密钥
     */
    String generateAesKey();
}
