package com.fisco.app.Modules.User.Enums;

import com.baomidou.mybatisplus.annotation.IEnum;

import lombok.Getter;

/**
 * 用户职能角色枚举
 * 映射内部职能：ADMIN, FINANCE, OPERATOR
 */
@Getter
public enum UserRoleEnum implements IEnum<String> {

    ADMIN("ADMIN", "管理员"),
    FINANCE("FINANCE", "财务"),
    OPERATOR("OPERATOR", "业务员");

    private final String value; 
    private final String desc;

    UserRoleEnum(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    /**
     * 实现 IEnum 接口，让 MyBatis Plus 知道如何存储
     */
    @Override
    public String getValue() {
        return this.value;
    }

    /**
     * 校验当前角色是否具有审批权限（财务或管理员）
     */
    public boolean hasApprovalPower() {
        return this == ADMIN || this == FINANCE;
    }
}