package com.fisco.app.dto.warehouse;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.springframework.lang.NonNull;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 冻结申请审核请求DTO
 * 管理员使用
 */
@Data
@ApiModel(value = "冻结申请审核请求", description = "管理员审核冻结申请")
public class FreezeApplicationReviewRequest {

    @ApiModelProperty(value = "申请ID", required = true, example = "app-uuid-001")
    @NonNull
    @NotNull(message = "申请ID不能为空")
    private String applicationId;

    @ApiModelProperty(value = "审核结果", required = true, example = "APPROVED",
            notes = "APPROVED-批准, REJECTED-拒绝")
    @NonNull
    @NotNull(message = "审核结果不能为空")
    @Pattern(regexp = "^(APPROVED|REJECTED)$", message = "审核结果只能是APPROVED或REJECTED")
    private String reviewResult;

    @ApiModelProperty(value = "审核意见", example = "同意冻结申请")
    @Size(max = 500, message = "审核意见长度不能超过500")
    private String reviewComments;

    @ApiModelProperty(value = "拒绝原因（审核拒绝时必填）", example = "冻结理由不充分")
    @Size(max = 500, message = "拒绝原因长度不能超过500")
    private String rejectionReason;

    @ApiModelProperty(value = "备注", example = "请及时处理货物问题")
    @Size(max = 200, message = "备注长度不能超过200")
    private String remarks;
}
