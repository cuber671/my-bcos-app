package com.fisco.app.Common.Annotation.impl;

import java.util.regex.Pattern;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.fisco.app.Common.Annotation.ValidHexAddress;

/**
 * 区块链地址校验器实现
 * 校验区块链地址是否符合42位hex格式（0x开头 + 40位十六进制字符）
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
public class HexAddressValidator implements ConstraintValidator<ValidHexAddress, String> {

    /**
     * 区块链地址正则：0x开头 + 40位十六进制字符
     * 例如：0x1234567890abcdef1234567890abcdef12345678
     */
    private static final Pattern HEX_ADDRESS_PATTERN = Pattern.compile("^0[xX][0-9a-fA-F]{40}$");

    /**
     * 最短hex地址长度（不含0x前缀）
     */
    private static final int MIN_HEX_LENGTH = 40;

    /**
     * 最长hex地址长度（不含0x前缀）
     */
    private static final int MAX_HEX_LENGTH = 40;

    /**
     * 是否允许为空
     */
    private boolean allowNull;

    @Override
    public void initialize(ValidHexAddress constraintAnnotation) {
        this.allowNull = constraintAnnotation.allowNull();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // 如果允许为空，且值为空，则校验通过
        if (allowNull && (value == null || value.isEmpty())) {
            return true;
        }

        // 如果不允许为空，且值为空，则校验失败
        if (!allowNull && (value == null || value.isEmpty())) {
            return false;
        }

        // 使用正则校验
        boolean valid = isValidHexAddress(value);

        // 如果校验失败，定制错误消息
        if (!valid && context != null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Invalid blockchain address: must be 42 characters (0x + 40 hex characters)"
            ).addConstraintViolation();
        }

        return valid;
    }

    /**
     * 校验是否为有效的区块链地址
     *
     * @param address 待校验的地址
     * @return true=有效
     */
    private boolean isValidHexAddress(String address) {
        if (address == null) {
            return false;
        }

        // 首先检查是否符合基本格式：0x开头 + 40位十六进制
        if (!HEX_ADDRESS_PATTERN.matcher(address).matches()) {
            return false;
        }

        // 检查十六进制部分长度
        String hexPart = address.substring(2); // 去掉"0x"前缀
        int hexLength = hexPart.length();

        return hexLength >= MIN_HEX_LENGTH && hexLength <= MAX_HEX_LENGTH;
    }
}
