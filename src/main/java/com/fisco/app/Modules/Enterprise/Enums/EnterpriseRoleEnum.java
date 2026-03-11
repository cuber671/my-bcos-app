package com.fisco.app.Modules.Enterprise.Enums;

import com.baomidou.mybatisplus.annotation.IEnum;

import lombok.Getter;

@Getter
public enum EnterpriseRoleEnum implements IEnum<Integer> {
    
    CORE(1, "核心企业"),       // 资产源头/买方：签发凭证
    TRADING(2, "现货交易平台"), // 交易见证/撮合：撮合订单、确认合同（新增）
    SUPPLIER(3, "供应商"),     // 资产流转/卖方：持有、流转凭证
    INSTITUTION(6, "金融机构"), // 资金提供：审核融资、发放贷款
    WAREHOUSE(9, "仓储方"),    // 实物监管：货权确权
    LOGISTICS(12, "物流方");   // 贸易真实性：物流跟踪


    private final int value;
    private final String desc;

    EnterpriseRoleEnum(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    @Override
    public Integer getValue() {
        return this.value;
    }
}
