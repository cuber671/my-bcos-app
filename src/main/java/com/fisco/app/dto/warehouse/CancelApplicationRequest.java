package com.fisco.app.dto.warehouse;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 仓单作废申请请求DTO
 */
@Data
@ApiModel(value = "仓单作废申请请求", description = "货主企业申请作废仓单")
public class CancelApplicationRequest {

    @ApiModelProperty(value = "仓单ID", required = true, example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    @NotBlank(message = "仓单ID不能为空")
    private String receiptId;

    @ApiModelProperty(value = "作废原因", required = true, example = "货物质量问题，无法继续使用")
    @NotBlank(message = "作废原因不能为空")
    @Size(max = 500, message = "作废原因长度不能超过500")
    private String cancelReason;

    @ApiModelProperty(value = "作废类型", required = true, example = "QUALITY_ISSUE",
                      notes = "QUALITY_ISSUE-质量问题, DAMAGED-货物损坏, WRONG_INFO-信息错误, " +
                              "LEGAL_DISPUTE-法律纠纷, VOLUNTARY-主动申请")
    @NotBlank(message = "作废类型不能为空")
    @Size(max = 50, message = "作废类型长度不能超过50")
    private String cancelType;

    @ApiModelProperty(value = "证明材料", example = "质量检验报告编号：QC20260126001")
    @Size(max = 500, message = "证明材料长度不能超过500")
    private String evidence;

    @ApiModelProperty(value = "参考编号", example = "法律文书号：2026民初字第001号")
    @Size(max = 100, message = "参考编号长度不能超过100")
    private String referenceNo;

    @ApiModelProperty(value = "备注", example = "已联系仓储方确认货物状态")
    @Size(max = 500, message = "备注长度不能超过500")
    private String remarks;
}
