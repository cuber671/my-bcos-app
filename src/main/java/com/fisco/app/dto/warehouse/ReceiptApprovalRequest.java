package com.fisco.app.dto.warehouse;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.springframework.lang.NonNull;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 仓单审核请求DTO
 */
@Data
@ApiModel(value = "仓单审核请求", description = "仓储方审核仓单入库")
public class ReceiptApprovalRequest {

    @ApiModelProperty(value = "仓单ID", required = true, example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    @NonNull
    @NotNull(message = "仓单ID不能为空")
    private String receiptId;

    @ApiModelProperty(value = "仓储企业ID（审核方）", required = true, example = "warehouse-uuid-001")
    @NonNull
    @NotNull(message = "仓储企业ID不能为空")
    private String warehouseId;

    @ApiModelProperty(value = "审核结果", required = true, example = "APPROVED", notes = "APPROVED-通过, REJECTED-拒绝")
    @NonNull
    @NotNull(message = "审核结果不能为空")
    @Pattern(regexp = "^(APPROVED|REJECTED)$", message = "审核结果只能是APPROVED或REJECTED")
    private String approvalResult;

    @ApiModelProperty(value = "审核意见", example = "货物已验收，数量质量符合要求")
    @Size(max = 500, message = "审核意见长度不能超过500")
    private String approvalComments;

    @ApiModelProperty(value = "拒绝原因（审核拒绝时必填）", example = "货物数量与单据不符")
    @Size(max = 500, message = "拒绝原因长度不能超过500")
    private String rejectionReason;
}
