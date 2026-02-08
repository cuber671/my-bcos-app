package com.fisco.app.dto.receivable;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 应收账款合并审核请求DTO
 */
@Data
@ApiModel(value = "应收账款合并审核请求", description = "管理员审核应收账款合并申请的请求参数")
public class MergeApprovalRequest {

    @ApiModelProperty(value = "是否批准", required = true, example = "true")
    @NotBlank(message = "批准状态不能为空")
    @Pattern(regexp = "true|false", message = "批准状态只能是true或false")
    private String approved;

    @ApiModelProperty(value = "审核意见/原因", example = "合并规则验证通过，同意合并")
    private String reason;

    /**
     * 获取是否批准（布尔值）
     */
    public boolean isApproved() {
        return Boolean.parseBoolean(approved);
    }
}
