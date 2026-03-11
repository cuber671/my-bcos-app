package com.fisco.app.Modules.User.Enums;

import com.baomidou.mybatisplus.annotation.IEnum;

import lombok.Getter;

/**
 * 用户状态枚举
 * 映射数据库字段类型: TINYINT / INTEGER
 */
@Getter
public enum UserStatusEnum implements IEnum<Integer> {

    PENDING(1, "注册中（待审核）"),
    NORMAL(2, "正常"),
    FROZEN(3, "冻结"),
    CANCELLING(4, "注销中"),
    PENDING_CANCEL(6, "注销待审核"),
    CANCELLED(5, "已注销");

    private final int value;
    private final String desc;

    UserStatusEnum(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    @Override
    public Integer getValue() {
        return this.value;
    }

    /**
     * 判断用户是否可以正常登录系统
     */
    public boolean canLogin() {
        return this == NORMAL;
    }

    /**
     * 判断用户是否处于流程锁定状态（如注销中或已注销，不可修改资料）
     */
    public boolean isLocked() {
        return this == CANCELLING || this == CANCELLED || this == FROZEN || this == PENDING_CANCEL;
    }

    /**
     * 判断用户是否可以申请注销
     */
    public boolean canApplyCancellation() {
        return this == NORMAL;
    }
}