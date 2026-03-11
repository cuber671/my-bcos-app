package com.fisco.app.Common.Utils;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;

import lombok.extern.slf4j.Slf4j;

/**
 * RSA加密工具类
 * 用于私钥/助记词等敏感数据的加密传输
 *
 * 使用场景：
 * - 前端使用RSA公钥加密敏感数据后传输
 * - 服务端使用RSA私钥解密，仅在内存中操作，不持久化存储
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Slf4j
public class RsaUtil {

    /**
     * RSA加密算法
     */
    private static final String RSA_ALGORITHM = "RSA";

    /**
     * RSA密钥长度
     */
    private static final int KEY_SIZE = 2048;

    /**
     * RSA/ECB/PKCS1Padding 填充方式
     */
    private static final String TRANSFORMATION = "RSA/ECB/PKCS1Padding";

    /**
     * 生成RSA密钥对
     *
     * @return 密钥对对象，包含公钥和私钥
     */
    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(RSA_ALGORITHM);
            keyPairGenerator.initialize(KEY_SIZE);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            log.info("RSA密钥对生成成功，密钥长度：{}位", KEY_SIZE);
            return keyPair;
        } catch (Exception e) {
            log.error("RSA密钥对生成失败", e);
            throw new RuntimeException("RSA密钥对生成失败", e);
        }
    }

    /**
     * 获取密钥对的Base64编码公钥字符串
     *
     * @param keyPair 密钥对
     * @return Base64编码的公钥字符串
     */
    public static String getPublicKeyString(KeyPair keyPair) {
        PublicKey publicKey = keyPair.getPublic();
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    /**
     * 获取密钥对的Base64编码私钥字符串
     *
     * @param keyPair 密钥对
     * @return Base64编码的私钥字符串
     */
    public static String getPrivateKeyString(KeyPair keyPair) {
        PrivateKey privateKey = keyPair.getPrivate();
        return Base64.getEncoder().encodeToString(privateKey.getEncoded());
    }

    /**
     * 从Base64编码的公钥字符串获取PublicKey对象
     *
     * @param publicKeyString Base64编码的公钥字符串
     * @return PublicKey对象
     */
    public static PublicKey getPublicKey(String publicKeyString) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(publicKeyString);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
            return keyFactory.generatePublic(keySpec);
        } catch (Exception e) {
            log.error("公钥解析失败", e);
            throw new RuntimeException("公钥解析失败", e);
        }
    }

    /**
     * 从Base64编码的私钥字符串获取PrivateKey对象
     *
     * @param privateKeyString Base64编码的私钥字符串
     * @return PrivateKey对象
     */
    public static PrivateKey getPrivateKey(String privateKeyString) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(privateKeyString);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
            return keyFactory.generatePrivate(keySpec);
        } catch (Exception e) {
            log.error("私钥解析失败", e);
            throw new RuntimeException("私钥解析失败", e);
        }
    }

    /**
     * RSA公钥加密
     *
     * @param data      待加密数据
     * @param publicKey 公钥
     * @return Base64编码的密文
     */
    public static String encrypt(String data, PublicKey publicKey) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedBytes = cipher.doFinal(data.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            log.error("RSA加密失败", e);
            throw new RuntimeException("RSA加密失败", e);
        }
    }

    /**
     * RSA公钥加密（使用Base64编码的公钥字符串）
     *
     * @param data            待加密数据
     * @param publicKeyString Base64编码的公钥字符串
     * @return Base64编码的密文
     */
    public static String encrypt(String data, String publicKeyString) {
        PublicKey publicKey = getPublicKey(publicKeyString);
        return encrypt(data, publicKey);
    }

    /**
     * RSA私钥解密
     *
     * @param encryptedData Base64编码的密文
     * @param privateKey    私钥
     * @return 解密后的明文
     */
    public static String decrypt(String encryptedData, PrivateKey privateKey) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
            return new String(decryptedBytes, "UTF-8");
        } catch (Exception e) {
            log.error("RSA解密失败", e);
            throw new RuntimeException("RSA解密失败", e);
        }
    }

    /**
     * RSA私钥解密（使用Base64编码的私钥字符串）
     *
     * @param encryptedData    Base64编码的密文
     * @param privateKeyString Base64编码的私钥字符串
     * @return 解密后的明文
     */
    public static String decrypt(String encryptedData, String privateKeyString) {
        PrivateKey privateKey = getPrivateKey(privateKeyString);
        return decrypt(encryptedData, privateKey);
    }

    /**
     * 密钥对内部类
     */
    public static class KeyPairHolder {
        private final PublicKey publicKey;
        private final PrivateKey privateKey;
        private final String publicKeyString;
        private final String privateKeyString;

        public KeyPairHolder(KeyPair keyPair) {
            this.publicKey = keyPair.getPublic();
            this.privateKey = keyPair.getPrivate();
            this.publicKeyString = Base64.getEncoder().encodeToString(publicKey.getEncoded());
            this.privateKeyString = Base64.getEncoder().encodeToString(privateKey.getEncoded());
        }

        public PublicKey getPublicKey() {
            return publicKey;
        }

        public PrivateKey getPrivateKey() {
            return privateKey;
        }

        public String getPublicKeyString() {
            return publicKeyString;
        }

        public String getPrivateKeyString() {
            return privateKeyString;
        }
    }

    /**
     * 创建密钥对并返回持有对象
     *
     * @return 密钥对持有对象
     */
    public static KeyPairHolder createKeyPairHolder() {
        return new KeyPairHolder(generateKeyPair());
    }
}
