package com.fisco.app.Common.Utils;

/**
 * 数据脱敏工具类
 * 提供手机号、身份证号、钱包地址等敏感数据的脱敏功能
 *
 * 脱敏规则：
 * - 手机号：前3后4隐藏，示例 138****1234
 * - 身份证号：前6后4隐藏，示例 110101****1234
 * - 钱包地址：0x1234****abcd 格式
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
public class DataMaskingUtil {

    /**
     * 脱敏类型枚举
     */
    public enum MaskType {
        PHONE,      // 手机号
        ID_CARD,    // 身份证号
        WALLET_ADDRESS  // 钱包地址
    }

    /**
     * 手机号脱敏
     * 规则：前3后4隐藏
     * 示例：13812345678 -> 138****1234
     *
     * @param phone 手机号
     * @return 脱敏后的手机号
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return phone;
        }

        // 去除空格和连字符
        String cleanPhone = phone.replaceAll("[\\s-]", "");

        // 验证手机号格式（11位数字）
        if (!cleanPhone.matches("^1[3-9]\\d{9}$")) {
            return phone;
        }

        // 前3位 + **** + 后4位
        return cleanPhone.substring(0, 3) + "****" + cleanPhone.substring(7);
    }

    /**
     * 身份证号脱敏
     * 规则：前6后4隐藏
     * 示例：110101199001011234 -> 110101****1234
     *
     * @param idCard 身份证号
     * @return 脱敏后的身份证号
     */
    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.isEmpty()) {
            return idCard;
        }

        // 去除空格和连字符
        String cleanIdCard = idCard.replaceAll("[\\s-]", "");

        // 验证身份证号格式（15位或18位）
        if (!cleanIdCard.matches("^\\d{15}$") && !cleanIdCard.matches("^\\d{17}[\\dXx]$")) {
            return idCard;
        }

        int length = cleanIdCard.length();
        if (length == 15) {
            // 15位身份证：前6 + **** + 后4
            return cleanIdCard.substring(0, 6) + "****" + cleanIdCard.substring(11);
        } else {
            // 18位身份证：前6 + **** + 后4
            return cleanIdCard.substring(0, 6) + "****" + cleanIdCard.substring(14);
        }
    }

    /**
     * 钱包地址脱敏
     * 规则：保留前缀0x，前4位和后4位，中间用****替代
     * 示例：0x1234567890abcdef -> 0x1234****abcdef
     *
     * @param address 钱包地址
     * @return 脱敏后的钱包地址
     */
    public static String maskWalletAddress(String address) {
        if (address == null || address.isEmpty()) {
            return address;
        }

        // 去除空格
        String cleanAddress = address.trim();

        // 验证钱包地址格式（42位，以0x开头）
        if (!cleanAddress.matches("^0x[0-9a-fA-F]{40}$")) {
            return address;
        }

        // 0x + 前2位 + **** + 后4位
        return "0x" + cleanAddress.substring(2, 4) + "****" + cleanAddress.substring(38);
    }

    /**
     * 邮箱脱敏
     * 规则：保留@前的第1位和@后的域名
     * 示例：test@example.com -> t***@example.com
     *
     * @param email 邮箱
     * @return 脱敏后的邮箱
     */
    public static String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return email;
        }

        // 去除空格
        String cleanEmail = email.trim();

        // 验证邮箱格式
        if (!cleanEmail.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            return email;
        }

        int atIndex = cleanEmail.indexOf('@');
        if (atIndex <= 1) {
            return cleanEmail;
        }

        // 用户名只保留第1位
        String username = cleanEmail.substring(0, 1) + "***";
        String domain = cleanEmail.substring(atIndex);

        return username + domain;
    }

    /**
     * 银行卡号脱敏
     * 规则：前6后4隐藏
     * 示例：6222021234567890123 -> 622202****7890123
     *
     * @param cardNumber 银行卡号
     * @return 脱敏后的银行卡号
     */
    public static String maskBankCard(String cardNumber) {
        if (cardNumber == null || cardNumber.isEmpty()) {
            return cardNumber;
        }

        // 去除空格
        String cleanCardNumber = cardNumber.replaceAll("\\s", "");

        // 验证银行卡号格式（16-19位数字）
        if (!cleanCardNumber.matches("^\\d{16,19}$")) {
            return cardNumber;
        }

        int length = cleanCardNumber.length();
        // 前6 + **** + 后4
        return cleanCardNumber.substring(0, 6) + "****" + cleanCardNumber.substring(length - 4);
    }

    /**
     * 通用脱敏方法
     * 根据脱敏类型自动选择脱敏方式
     *
     * @param value  待脱敏的值
     * @param type   脱敏类型
     * @return 脱敏后的值
     */
    public static String mask(String value, MaskType type) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        switch (type) {
            case PHONE:
                return maskPhone(value);
            case ID_CARD:
                return maskIdCard(value);
            case WALLET_ADDRESS:
                return maskWalletAddress(value);
            default:
                return value;
        }
    }
}
