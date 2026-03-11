package com.fisco.app.Modules.Enterprise.Enums;

import lombok.Getter;

@Getter
public enum InvitationTypeEnum {
    SINGLE(1, "禁用"),
    MULTI(2, "启用"),
    INFINITE(3, "已用罄");

    private final Integer code;
    private final String name;

    InvitationTypeEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }
}