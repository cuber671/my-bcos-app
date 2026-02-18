package com.fisco.app.enums;

/**
 * 额度类型枚举
 */
public enum CreditLimitType {
    FINANCING("FINANCING", "融资额度"),
    GUARANTEE("GUARANTEE", "担保额度"),
    CREDIT("CREDIT", "赊账额度");

    private final String code;
    private final String description;

    CreditLimitType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static CreditLimitType fromCode(String code) {
        for (CreditLimitType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown CreditLimitType code: " + code);
    }
}
