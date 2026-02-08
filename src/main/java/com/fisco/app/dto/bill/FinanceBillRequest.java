package com.fisco.app.dto.bill;


import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 票据融资申请请求DTO
 */
@Data
@ApiModel(value = "票据融资申请请求", description = "用于申请票据融资的请求参数")
public class FinanceBillRequest {

    @NotNull(message = "金融机构ID不能为空")
    @ApiModelProperty(value = "金融机构ID", required = true, example = "bank-uuid-001")
    private String financialInstitutionId;

    @NotNull(message = "融资金额不能为空")
    @ApiModelProperty(value = "融资金额（元）", required = true, example = "1000000.00")
    private BigDecimal financeAmount;

    @NotNull(message = "融资利率不能为空")
    @ApiModelProperty(value = "融资利率（%）", required = true, example = "5.5")
    private BigDecimal financeRate;

    @NotNull(message = "融资期限不能为空")
    @ApiModelProperty(value = "融资期限（天）", required = true, example = "90")
    private Integer financePeriod;

    @ApiModelProperty(value = "质押协议内容", example = "质押协议条款...")
    private String pledgeAgreement;
}
