package com.fisco.app.dto.bill;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 票据融资还款请求DTO
 */
@Data
@ApiModel(value = "票据融资还款请求", description = "用于票据融资到期还款")
public class RepayFinanceRequest {

    @ApiModelProperty(value = "还款金额（元）", required = true, example = "1000000.00")
    @NotNull(message = "还款金额不能为空")
    private BigDecimal repayAmount;

    @ApiModelProperty(value = "还款类型", notes = "FULL-全额还款, PARTIAL-部分还款")
    private RepayType repayType = RepayType.FULL;

    @ApiModelProperty(value = "还款凭证", example = "转账凭证号")
    private String repaymentProof;

    /**
     * 还款类型枚举
     */
    public enum RepayType {
        FULL,      // 全额还款
        PARTIAL    // 部分还款
    }
}
