package com.fisco.app.dto.warehouse;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 合并审核请求DTO
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-09
 * @version 1.0
 */
@Data
@ApiModel(value = "合并审核请求", description = "仓单合并审核请求参数")
public class MergeApprovalRequest {

    @NotNull(message = "申请ID不能为空")
    @ApiModelProperty(value = "合并申请ID", required = true)
    private String applicationId;

    @NotNull(message = "审核结果不能为空")
    @ApiModelProperty(value = "审核结果", required = true, notes = "true-通过, false-拒绝")
    private Boolean approved;

    @ApiModelProperty(value = "审核意见", example = "合并规则验证通过，同意合并")
    private String reviewComments;
}
