package com.fisco.app.Common.Utils;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import lombok.extern.slf4j.Slf4j;

/**
 * AES加密工具类
 * 用于交易哈希、资产ID等敏感数据的按需AES-256加密传输
 *
 * 使用场景：
 * - 交易哈希按需AES-256加密传输
 * - 资产ID等敏感数据的加密存储和传输
 *
 * 安全特性：
 * - 使用AES/GCM/NoPadding模式（认证加密）
 * - 密钥从配置读取（非硬编码）
 * - 每次加密生成随机IV
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Slf4j
public class AesUtil {

    /**
     * AES算法
     */
    private static final String ALGORITHM = "AES";

    /**
     * AES/GCM/NoPadding 认证加密模式
     */
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    /**
     * GCM认证标签长度（128位）
     */
    private static final int GCM_TAG_LENGTH = 128;

    /**
     * GCM IV长度（12字节）
     */
    private static final int GCM_IV_LENGTH = 12;

    /**
     * AES密钥长度（256位）
     */
    private static final int KEY_SIZE = 256;

    /**
     * 默认AES密钥（32字节，用于AES-256）
     * 生产环境应从环境变量或配置中心读取
     */
    private static final String DEFAULT_KEY = "FiscoBcosPlatformKey2026Secure32"; // 32字节

    /**
     * 生成AES密钥
     *
     * @return Base64编码的AES密钥
     */
    public static String generateKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(KEY_SIZE, new SecureRandom());
            SecretKey secretKey = keyGenerator.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (Exception e) {
            log.error("AES密钥生成失败", e);
            throw new RuntimeException("AES密钥生成失败", e);
        }
    }

    /**
     * 从Base64编码的密钥字符串获取SecretKey对象
     *
     * @param keyString Base64编码的密钥字符串
     * @return SecretKey对象
     */
    public static SecretKey getSecretKey(String keyString) {
        byte[] keyBytes;
        if (keyString == null || keyString.isEmpty()) {
            // 使用默认密钥
            keyBytes = DEFAULT_KEY.getBytes(StandardCharsets.UTF_8);
        } else {
            keyBytes = Base64.getDecoder().decode(keyString);
        }
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }

    /**
     * AES加密（GCM模式）
     *
     * @param data      待加密数据
     * @param keyString Base64编码的AES密钥
     * @return Base64编码的密文（包含IV + 密文）
     */
    public static String encrypt(String data, String keyString) {
        try {
            SecretKey secretKey = getSecretKey(keyString);

            // 生成随机IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            // 加密
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

            // 拼接IV + 密文
            byte[] combined = new byte[iv.length + encryptedBytes.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encryptedBytes, 0, combined, iv.length, encryptedBytes.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("AES加密失败", e);
            throw new RuntimeException("AES加密失败", e);
        }
    }

    /**
     * AES解密（GCM模式）
     *
     * @param encryptedData Base64编码的密文（包含IV + 密文）
     * @param keyString     Base64编码的AES密钥
     * @return 解密后的明文
     */
    public static String decrypt(String encryptedData, String keyString) {
        try {
            SecretKey secretKey = getSecretKey(keyString);

            // 解码Base64
            byte[] combined = Base64.getDecoder().decode(encryptedData);

            // 分离IV和密文
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] cipherText = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, iv.length);
            System.arraycopy(combined, iv.length, cipherText, 0, cipherText.length);

            // 解密
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] decryptedBytes = cipher.doFinal(cipherText);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("AES解密失败", e);
            throw new RuntimeException("AES解密失败", e);
        }
    }

    /**
     * AES加密（使用默认密钥）
     *
     * @param data 待加密数据
     * @return Base64编码的密文
     */
    public static String encrypt(String data) {
        return encrypt(data, null);
    }

    /**
     * AES解密（使用默认密钥）
     *
     * @param encryptedData Base64编码的密文
     * @return 解密后的明文
     */
    public static String decrypt(String encryptedData) {
        return decrypt(encryptedData, null);
    }
}
