package com.fisco.app.enums;

/**
 * 额度状态枚举
 */
public enum CreditLimitStatus {
    ACTIVE("ACTIVE", "生效中"),
    FROZEN("FROZEN", "已冻结"),
    EXPIRED("EXPIRED", "已失效"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String description;

    CreditLimitStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static CreditLimitStatus fromCode(String code) {
        for (CreditLimitStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown CreditLimitStatus code: " + code);
    }
}
