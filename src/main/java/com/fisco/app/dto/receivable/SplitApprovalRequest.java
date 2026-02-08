package com.fisco.app.dto.receivable;
import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 仓单拆分审核请求DTO
 */
@Data
@ApiModel(value = "仓单拆分审核请求", description = "管理员/仓储方审核拆分申请")
public class SplitApprovalRequest {

    @ApiModelProperty(value = "申请ID", required = true, example = "app-uuid-001")
    @NotBlank(message = "申请ID不能为空")
    private String applicationId;

    @ApiModelProperty(value = "审核结果", required = true, example = "APPROVED")
    @NotBlank(message = "审核结果不能为空")
    @Pattern(regexp = "^(APPROVED|REJECTED)$", message = "审核结果只能是APPROVED或REJECTED")
    private String approvalResult;

    @ApiModelProperty(value = "审核意见", example = "拆分规则验证通过，同意拆分")
    private String approvalComments;

    @ApiModelProperty(value = "审核原因/意见", example = "拆分规则验证通过，同意拆分")
    private String reason;

    /**
     * 获取是否批准（布尔值）
     */
    public boolean isApproved() {
        return "APPROVED".equals(approvalResult);
    }
}
