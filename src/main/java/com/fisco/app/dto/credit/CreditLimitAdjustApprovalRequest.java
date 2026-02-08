package com.fisco.app.dto.credit;

import com.fisco.app.enums.CreditAdjustRequestStatus;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.lang.NonNull;

/**
 * 信用额度调整申请审批请求DTO
 */
@Data
@ApiModel(value = "信用额度调整申请审批请求", description = "用于审批额度调整申请的请求参数")
public class CreditLimitAdjustApprovalRequest {

    @ApiModelProperty(value = "申请ID", required = true, example = "d4e5f6g7-h8i9-0123-defg-345678901234")
    @NotBlank(message = "申请ID不能为空")
    @NonNull
    private String requestId;

    @ApiModelProperty(value = "审批结果", required = true, notes = "APPROVED-通过, REJECTED-拒绝", example = "APPROVED")
    @NotNull(message = "审批结果不能为空")
    @NonNull
    private CreditAdjustRequestStatus approvalResult;

    @ApiModelProperty(value = "审批意见", example = "同意增加额度，企业信用良好", notes = "审批通过时必填")
    private String approveReason;

    @ApiModelProperty(value = "拒绝原因", example = "企业存在逾期记录，暂不符合增加额度条件", notes = "审批拒绝时必填")
    private String rejectReason;

    @ApiModelProperty(value = "审批人地址（系统自动获取，无需传入）", hidden = true)
    private String approverAddress;
}
