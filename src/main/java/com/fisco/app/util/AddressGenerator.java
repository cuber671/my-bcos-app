package com.fisco.app.util;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.fisco.bcos.sdk.v3.crypto.CryptoSuite;
import org.fisco.bcos.sdk.v3.model.CryptoType;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 区块链地址生成器
 * 使用混合方案生成区块链地址
 *
 * 混合方案包括：
 * 1. 企业特定信息（统一社会信用代码、企业名称、角色）
 * 2. 时间戳（精确到秒）
 * 3. 密码学安全随机数（128位）
 * 4. Keccak-256哈希算法
 *
 * 生成的地址格式：0x开头的40位十六进制字符串
 *
 * @author FISCO BCOS
 * @since 2025-01-19
 */
@Slf4j
@Component
public class AddressGenerator {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final int RANDOM_BYTES_LENGTH = 16; // 128位随机数
    private static final int ADDRESS_LENGTH = 20; // 160位（40个十六进制字符）
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final CryptoSuite cryptoSuite;

    public AddressGenerator() {
        // 使用标准ECDSA套件（Keccak-256哈希）
        this.cryptoSuite = new CryptoSuite(CryptoType.ECDSA_TYPE);
    }

    /**
     * 为企业生成区块链地址
     *
     * 混合方案：
     * - 企业统一社会信用代码（18位）
     * - 企业名称（UTF-8编码）
     * - 企业角色
     * - 当前时间戳（精确到秒）
     * - 128位安全随机数
     *
     * 所有数据使用"|"分隔符连接，然后进行Keccak-256哈希
     * 取哈希值的最后20字节（160位）作为地址
     *
     * @param creditCode 统一社会信用代码（18位）
     * @param name 企业名称
     * @param role 企业角色
     * @return 0x开头的40位十六进制地址
     * @throws IllegalArgumentException 如果参数为空
     */
    public String generateEnterpriseAddress(String creditCode, String name, String role) {
        // 参数校验
        if (creditCode == null || creditCode.trim().isEmpty()) {
            throw new IllegalArgumentException("统一社会信用代码不能为空");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("企业名称不能为空");
        }
        if (role == null || role.trim().isEmpty()) {
            throw new IllegalArgumentException("企业角色不能为空");
        }

        // 生成时间戳
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);

        // 生成安全随机数（16字节 = 128位）
        byte[] randomBytes = new byte[RANDOM_BYTES_LENGTH];
        SECURE_RANDOM.nextBytes(randomBytes);
        String randomHex = bytesToHex(randomBytes);

        // 混合所有输入数据
        String mixedData = String.join("|",
            creditCode.trim(),
            name.trim(),
            role.trim(),
            timestamp,
            randomHex
        );

        log.debug("生成地址的混合数据: {}", mixedData);

        // 计算Keccak-256哈希
        byte[] dataBytes = mixedData.getBytes(StandardCharsets.UTF_8);
        byte[] hash = cryptoSuite.hash(dataBytes);

        // 取哈希值的最后20字节作为地址（Ethereum/FISCO BCOS标准）
        byte[] addressBytes = new byte[ADDRESS_LENGTH];
        System.arraycopy(hash, hash.length - ADDRESS_LENGTH, addressBytes, 0, ADDRESS_LENGTH);

        // 转换为十六进制字符串，并添加0x前缀
        String address = "0x" + bytesToHex(addressBytes);

        log.info("生成企业区块链地址: creditCode={}, name={}, role={}, address={}",
                 maskCreditCode(creditCode), name, role, address);

        return address;
    }

    /**
     * 生成随机区块链地址（不依赖企业信息）
     *
     * 用于测试或其他不需要企业特定信息的场景
     *
     * @return 0x开头的40位十六进制地址
     */
    public String generateRandomAddress() {
        // 生成32字节随机数（256位）
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);

        // 对随机数进行哈希
        byte[] hash = cryptoSuite.hash(randomBytes);

        // 取最后20字节作为地址
        byte[] addressBytes = new byte[ADDRESS_LENGTH];
        System.arraycopy(hash, hash.length - ADDRESS_LENGTH, addressBytes, 0, ADDRESS_LENGTH);

        String address = "0x" + bytesToHex(addressBytes);

        log.debug("生成随机区块链地址: {}", address);

        return address;
    }

    /**
     * 验证地址格式是否正确
     *
     * @param address 待验证的地址
     * @return true 如果地址格式正确
     */
    public static boolean isValidAddress(String address) {
        if (address == null || address.length() != 42) {
            return false;
        }
        if (!address.startsWith("0x")) {
            return false;
        }
        // 检查剩余40个字符是否都是十六进制
        String hexPart = address.substring(2);
        return hexPart.matches("[0-9a-fA-F]{40}");
    }

    /**
     * 将字节数组转换为十六进制字符串（小写）
     *
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    /**
     * 掩码统一社会信用代码（用于日志，保护隐私）
     *
     * @param creditCode 完整的统一社会信用代码
     * @return 掩码后的代码（只显示前4位和后4位）
     */
    private static String maskCreditCode(String creditCode) {
        if (creditCode == null || creditCode.length() < 8) {
            return "***";
        }
        return creditCode.substring(0, 4) + "****" + creditCode.substring(creditCode.length() - 4);
    }

    /**
     * 地址生成器的主方法（用于测试）
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        AddressGenerator generator = new AddressGenerator();

        // 测试1: 生成企业地址
        System.out.println("=== 测试1: 生成企业地址 ===");
        String address1 = generator.generateEnterpriseAddress(
            "91110000MA001234XY",
            "测试企业A",
            "SUPPLIER"
        );
        System.out.println("生成的地址: " + address1);
        System.out.println("地址验证: " + isValidAddress(address1));

        // 测试2: 生成另一个企业地址（应该不同）
        System.out.println("\n=== 测试2: 生成另一个企业地址 ===");
        try {
            Thread.sleep(1000); // 等待1秒，确保时间戳不同
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        String address2 = generator.generateEnterpriseAddress(
            "91110000MA001234XY",
            "测试企业A",
            "SUPPLIER"
        );
        System.out.println("生成的地址: " + address2);
        System.out.println("两个地址不同: " + !address1.equals(address2));

        // 测试3: 不同企业的地址应该不同
        System.out.println("\n=== 测试3: 不同企业的地址 ===");
        String address3 = generator.generateEnterpriseAddress(
            "91110000MA005678XY",
            "测试企业B",
            "CORE_ENTERPRISE"
        );
        System.out.println("生成的地址: " + address3);
        System.out.println("与address1不同: " + !address1.equals(address3));

        // 测试4: 生成随机地址
        System.out.println("\n=== 测试4: 生成随机地址 ===");
        String randomAddress = generator.generateRandomAddress();
        System.out.println("随机地址: " + randomAddress);
        System.out.println("地址验证: " + isValidAddress(randomAddress));

        // 测试5: 无效地址验证
        System.out.println("\n=== 测试5: 无效地址验证 ===");
        System.out.println("null地址: " + isValidAddress(null));
        System.out.println("空地址: " + isValidAddress(""));
        System.out.println("短地址: " + isValidAddress("0x123"));
        System.out.println("无0x前缀: " + isValidAddress("1234567890abcdef1234567890abcdef12345678"));
        System.out.println("非法字符: " + isValidAddress("0x1234567890ghij1234567890abcdef12345678"));

        System.out.println("\n=== 所有测试完成 ===");
    }
}
