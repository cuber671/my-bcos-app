package com.fisco.app.Common.Enums;

import lombok.Getter;

/**
 * 异步任务状态枚举
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Getter
public enum AsyncTaskStatus {

    PENDING("PENDING", "待处理"),
    PROCESSING("PROCESSING", "处理中"),
    SUCCESS("SUCCESS", "成功"),
    FAILED("FAILED", "失败"),
    TIMEOUT("TIMEOUT", "超时");

    private final String code;
    private final String description;

    AsyncTaskStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
