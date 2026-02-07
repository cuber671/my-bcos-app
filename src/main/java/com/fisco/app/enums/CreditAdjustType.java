package com.fisco.app.enums;

/**
 * 额度调整类型枚举
 */
public enum CreditAdjustType {
    INCREASE("INCREASE", "增加额度"),
    DECREASE("DECREASE", "减少额度"),
    RESET("RESET", "重置额度");

    private final String code;
    private final String description;

    CreditAdjustType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static CreditAdjustType fromCode(String code) {
        for (CreditAdjustType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown CreditAdjustType code: " + code);
    }
}
