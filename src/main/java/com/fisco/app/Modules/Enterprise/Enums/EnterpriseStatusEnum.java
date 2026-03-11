package com.fisco.app.Modules.Enterprise.Enums;

import com.baomidou.mybatisplus.annotation.IEnum;

import lombok.Getter;

/**
 * 企业状态枚举
 * 映射数据库字段类型: TINYINT
 */
@Getter
public enum EnterpriseStatusEnum implements IEnum<Integer> {
    
    PENDING(0, "待审核"),
    NORMAL(1, "正常"),
    FROZEN(2, "已冻结"),
    CANCELLING(3, "申请注销中"),
    PENDING_CANCEL(5, "注销待审核"),
    CANCELLED(4, "已注销");      

    private final int value;
    private final String desc;

    EnterpriseStatusEnum(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    @Override
    public Integer getValue() {
        return this.value;
    }

    /**
     * 判断企业当前是否可以执行区块链交易
     * 只有正常状态下才允许发起签名操作
     */
    public boolean canExecuteTransaction() {
        return this == NORMAL;
    }

    /**
     * 判断企业是否可以申请注销
     */
    public boolean canApplyCancellation() {
        return this == NORMAL;
    }
}