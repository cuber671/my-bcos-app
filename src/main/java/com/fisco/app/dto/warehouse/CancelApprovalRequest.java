package com.fisco.app.dto.warehouse;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 仓单作废审核请求DTO
 */
@Data
@ApiModel(value = "仓单作废审核请求", description = "管理员/仓储方审核作废申请")
public class CancelApprovalRequest {

    @ApiModelProperty(value = "申请ID", required = true, example = "app-uuid-001")
    @NotBlank(message = "申请ID不能为空")
    private String applicationId;

    @ApiModelProperty(value = "审核结果", required = true, example = "APPROVED",
                      notes = "APPROVED-批准, REJECTED-拒绝")
    @NotBlank(message = "审核结果不能为空")
    @Pattern(regexp = "^(APPROVED|REJECTED)$", message = "审核结果只能是APPROVED或REJECTED")
    private String approvalResult;

    @ApiModelProperty(value = "审核意见", example = "审核通过，同意作废该仓单")
    private String approvalComments;
}
