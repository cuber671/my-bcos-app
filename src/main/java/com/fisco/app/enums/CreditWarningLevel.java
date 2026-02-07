package com.fisco.app.enums;

/**
 * 预警级别枚举
 */
public enum CreditWarningLevel {
    LOW("LOW", "低风险"),
    MEDIUM("MEDIUM", "中风险"),
    HIGH("HIGH", "高风险"),
    CRITICAL("CRITICAL", "紧急");

    private final String code;
    private final String description;

    CreditWarningLevel(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static CreditWarningLevel fromCode(String code) {
        for (CreditWarningLevel level : values()) {
            if (level.code.equals(code)) {
                return level;
            }
        }
        throw new IllegalArgumentException("Unknown CreditWarningLevel code: " + code);
    }
}
