package com.fisco.app.dto.bill;

import com.fisco.app.entity.bill.RepaymentRecord;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 票据还款请求DTO
 */
@Data
@ApiModel(value = "票据还款请求", description = "用于贴现票据还款的请求参数")
@Schema(name = "票据还款请求")
public class RepayBillRequest {

    @NotNull(message = "还款类型不能为空")
    @ApiModelProperty(value = "还款类型", required = true, example = "FULL_PAYMENT")
    private RepaymentRecord.PaymentType paymentType;

    @NotNull(message = "还款金额不能为空")
    @ApiModelProperty(value = "还款金额", required = true, example = "1000000.00")
    private BigDecimal paymentAmount;

    @ApiModelProperty(value = "利息金额", example = "50000.00")
    private BigDecimal interestAmount;

    @ApiModelProperty(value = "逾期利息金额", example = "10000.00")
    private BigDecimal penaltyInterestAmount;

    @ApiModelProperty(value = "还款备注", example = "全额还款")
    private String remark;
}
