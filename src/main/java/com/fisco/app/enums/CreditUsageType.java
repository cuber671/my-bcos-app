package com.fisco.app.enums;

/**
 * 额度使用类型枚举
 */
public enum CreditUsageType {
    USE("USE", "使用"),
    RELEASE("RELEASE", "释放"),
    FREEZE("FREEZE", "冻结"),
    UNFREEZE("UNFREEZE", "解冻");

    private final String code;
    private final String description;

    CreditUsageType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static CreditUsageType fromCode(String code) {
        for (CreditUsageType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown CreditUsageType code: " + code);
    }
}
