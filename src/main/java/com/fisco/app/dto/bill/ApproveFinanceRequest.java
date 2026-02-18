package com.fisco.app.dto.bill;
import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 审核票据融资请求DTO
 */
@Data
@ApiModel(value = "审核票据融资请求", description = "金融机构审核票据融资申请")
public class ApproveFinanceRequest {

    @NotBlank(message = "申请ID不能为空")
    @ApiModelProperty(value = "融资申请ID", required = true, example = "app-uuid-001")
    private String applicationId;

    @NotNull(message = "审核结果不能为空")
    @ApiModelProperty(value = "审核结果", required = true, notes = "APPROVED-批准, REJECTED-拒绝")
    private ApprovalResult approvalResult;

    @ApiModelProperty(value = "批准金额", example = "950000.00")
    private BigDecimal approvedAmount;

    @ApiModelProperty(value = "批准利率", example = "5.5")
    private BigDecimal approvedRate;

    @ApiModelProperty(value = "审核意见", example = "审核通过")
    private String approvalComments;

    /**
     * 审核结果枚举
     */
    public enum ApprovalResult {
        APPROVED,   // 批准
        REJECTED    // 拒绝
    }
}
