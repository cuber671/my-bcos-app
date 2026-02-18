package com.fisco.app.dto.pledge;
import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import java.math.BigDecimal;

import javax.validation.constraints.*;

/**
 * 质押审核请求DTO
 */
@Data
@ApiModel(value = "质押审核请求", description = "金融机构审核质押申请的请求参数")
public class PledgeApprovalRequest {

    @ApiModelProperty(value = "申请ID", required = true, example = "1")
    @NotNull(message = "申请ID不能为空")
    private Long applicationId;

    @ApiModelProperty(value = "审核结果", required = true, notes = "APPROVED-批准, REJECTED-拒绝")
    @NotBlank(message = "审核结果不能为空")
    @Pattern(regexp = "APPROVED|REJECTED", message = "审核结果只能是APPROVED或REJECTED")
    private String approvalResult;

    @ApiModelProperty(value = "实际批准金额（元）", example = "100000.00", notes = "批准时可调整金额")
    @DecimalMin(value = "0.01", message = "批准金额必须大于0")
    private BigDecimal approvedAmount;

    @ApiModelProperty(value = "年化利率（%）", example = "5.50", notes = "批准时需要设置利率")
    @DecimalMin(value = "0.01", message = "利率必须大于0")
    @DecimalMax(value = "100", message = "利率不能超过100%")
    private BigDecimal interestRate;

    @ApiModelProperty(value = "拒绝原因", example = "仓单价值评估不足", notes = "拒绝时必填")
    private String rejectionReason;

    @ApiModelProperty(value = "审核意见", example = "审核通过，符合质押条件")
    private String remark;

    /**
     * 验证业务规则
     */
    public void validate() {
        if ("APPROVED".equals(approvalResult)) {
            if (approvedAmount == null) {
                throw new IllegalArgumentException("批准时必须指定批准金额");
            }
            if (interestRate == null) {
                throw new IllegalArgumentException("批准时必须指定利率");
            }
        }
        if ("REJECTED".equals(approvalResult)) {
            if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
                throw new IllegalArgumentException("拒绝时必须说明拒绝原因");
            }
        }
    }
}
