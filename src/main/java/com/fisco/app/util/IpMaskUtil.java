package com.fisco.app.util;

import lombok.extern.slf4j.Slf4j;

/**
 * IP地址脱敏工具类
 * 用于统一处理敏感IP地址的脱敏显示
 *
 * @author FISCO BCOS Team
 * @since 2026-02-10
 */
@Slf4j
public class IpMaskUtil {

    private static final String MASK = "***";
    private static final String IP_MASK_PATTERN = "*.*";

    /**
     * 脱敏IPv4地址
     * 示例: 192.168.1.100 -> 192.168.*.*
     *
     * @param ipAddress 原始IP地址
     * @return 脱敏后的IP地址
     */
    public static String maskIpAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return MASK;
        }

        try {
            // 如果已经是脱敏格式，直接返回
            if (ipAddress.contains(IP_MASK_PATTERN)) {
                return ipAddress;
            }

            String[] parts = ipAddress.split("\\.");
            if (parts.length != 4) {
                // 非标准IPv4格式，返回脱敏标记
                return MASK;
            }

            // 保留前两段，后两段脱敏
            return parts[0] + "." + parts[1] + ".*.*";
        } catch (Exception e) {
            log.warn("Failed to mask IP address: {}", ipAddress, e);
            return MASK;
        }
    }

    /**
     * 脱敏IPv4地址（自定义保留段数）
     *
     * @param ipAddress 原始IP地址
     * @param keepSegments 保留的段数（1-3）
     * @return 脱敏后的IP地址
     */
    public static String maskIpAddress(String ipAddress, int keepSegments) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return MASK;
        }

        if (keepSegments < 1 || keepSegments > 3) {
            throw new IllegalArgumentException("keepSegments must be between 1 and 3");
        }

        try {
            String[] parts = ipAddress.split("\\.");
            if (parts.length != 4) {
                return MASK;
            }

            StringBuilder masked = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                if (i < keepSegments) {
                    masked.append(parts[i]);
                } else {
                    masked.append("*");
                }

                if (i < 3) {
                    masked.append(".");
                }
            }

            return masked.toString();
        } catch (Exception e) {
            log.warn("Failed to mask IP address with custom segments: {}", ipAddress, e);
            return MASK;
        }
    }

    /**
     * 脱敏IP地址和端口
     * 示例: 192.168.1.100:20200 -> 192.168.*.*:****
     *
     * @param ipAddressWithPort IP地址:端口
     * @return 脱敏后的地址
     */
    public static String maskIpAddressWithPort(String ipAddressWithPort) {
        if (ipAddressWithPort == null || ipAddressWithPort.isEmpty()) {
            return MASK;
        }

        try {
            String[] parts = ipAddressWithPort.split(":");
            if (parts.length != 2) {
                return maskIpAddress(ipAddressWithPort);
            }

            String maskedIp = maskIpAddress(parts[0]);
            String maskedPort = "****";

            return maskedIp + ":" + maskedPort;
        } catch (Exception e) {
            log.warn("Failed to mask IP address with port: {}", ipAddressWithPort, e);
            return MASK;
        }
    }

    /**
     * 批量脱敏IP地址列表
     *
     * @param ipAddresses IP地址列表
     * @return 脱敏后的IP地址列表
     */
    public static String[] maskIpAddresses(String[] ipAddresses) {
        if (ipAddresses == null) {
            return new String[0];
        }

        String[] masked = new String[ipAddresses.length];
        for (int i = 0; i < ipAddresses.length; i++) {
            masked[i] = maskIpAddress(ipAddresses[i]);
        }

        return masked;
    }

    /**
     * 检查IP地址是否为本地地址
     * 本地地址包括: 127.0.0.1, localhost, ::1, 0.0.0.0
     *
     * @param ipAddress IP地址
     * @return true如果是本地地址
     */
    public static boolean isLocalAddress(String ipAddress) {
        if (ipAddress == null) {
            return false;
        }

        return ipAddress.equals("127.0.0.1")
                || ipAddress.equals("localhost")
                || ipAddress.equals("::1")
                || ipAddress.equals("0.0.0.0")
                || ipAddress.startsWith("127.");
    }

    /**
     * 验证IP地址格式是否有效
     *
     * @param ipAddress IP地址
     * @return true如果格式有效
     */
    public static boolean isValidIpFormat(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return false;
        }

        String[] parts = ipAddress.split("\\.");
        if (parts.length != 4) {
            return false;
        }

        try {
            for (String part : parts) {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) {
                    return false;
                }
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
