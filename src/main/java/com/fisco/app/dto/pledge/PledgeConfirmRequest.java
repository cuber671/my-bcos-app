package com.fisco.app.dto.pledge;
import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import java.math.BigDecimal;

import javax.validation.constraints.*;

/**
 * 质押确认请求DTO
 */
@Data
@ApiModel(value = "质押确认请求", description = "金融机构确认或拒绝质押的请求参数")
public class PledgeConfirmRequest {

    @ApiModelProperty(value = "背书ID", required = true, example = "b2c3d4e5-f6g7-8901-bcde-f23456789012")
    @NotBlank(message = "背书ID不能为空")
    private String endorsementId;

    @ApiModelProperty(value = "确认结果", required = true, example = "CONFIRMED", notes = "CONFIRMED-批准, CANCELLED-拒绝")
    @NotBlank(message = "确认结果不能为空")
    @Pattern(regexp = "CONFIRMED|CANCELLED", message = "确认结果必须是CONFIRMED或CANCELLED")
    private String confirmResult;

    @ApiModelProperty(value = "批准金额（元）", example = "100000.00", notes = "批准时必填")
    @DecimalMin(value = "0.01", message = "批准金额必须大于0")
    private BigDecimal approvedAmount;

    @ApiModelProperty(value = "年化利率（%）", example = "5.50", notes = "批准时必填")
    @DecimalMin(value = "0.01", message = "利率必须大于0")
    @DecimalMax(value = "100", message = "利率不能超过100%")
    private BigDecimal interestRate;

    @ApiModelProperty(value = "拒绝原因", example = "仓单价值不足")
    private String rejectionReason;

    @ApiModelProperty(value = "备注", example = "审核通过")
    private String remark;
}
