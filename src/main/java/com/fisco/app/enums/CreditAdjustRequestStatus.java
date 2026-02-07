package com.fisco.app.enums;

/**
 * 调整申请状态枚举
 */
public enum CreditAdjustRequestStatus {
    PENDING("PENDING", "待审批"),
    APPROVED("APPROVED", "已通过"),
    REJECTED("REJECTED", "已拒绝");

    private final String code;
    private final String description;

    CreditAdjustRequestStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static CreditAdjustRequestStatus fromCode(String code) {
        for (CreditAdjustRequestStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown CreditAdjustRequestStatus code: " + code);
    }
}
