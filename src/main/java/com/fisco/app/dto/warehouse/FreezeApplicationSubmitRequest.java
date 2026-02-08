package com.fisco.app.dto.warehouse;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.springframework.lang.NonNull;

/**
 * 提交冻结申请请求DTO
 * 仓储方使用
 */
@Data
@ApiModel(value = "提交冻结申请请求", description = "仓储方提交仓单冻结申请")
public class FreezeApplicationSubmitRequest {

    @ApiModelProperty(value = "仓单ID", required = true, example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    @NonNull
    @NotNull(message = "仓单ID不能为空")
    private String receiptId;

    @ApiModelProperty(value = "仓储企业ID", required = true, example = "warehouse-uuid-001")
    @NonNull
    @NotNull(message = "仓储企业ID不能为空")
    private String warehouseId;

    @ApiModelProperty(value = "冻结原因", required = true, example = "货物质量异常，需要冻结处理")
    @NotBlank(message = "冻结原因不能为空")
    @Size(max = 500, message = "冻结原因长度不能超过500")
    private String freezeReason;

    @ApiModelProperty(value = "冻结类型", required = true, example = "BUSINESS",
            notes = "LEGAL-法律冻结, BUSINESS-业务冻结, RISK-风险冻结")
    @NonNull
    @NotNull(message = "冻结类型不能为空")
    @Pattern(regexp = "^(LEGAL|BUSINESS|RISK)$", message = "冻结类型只能是LEGAL、BUSINESS或RISK")
    private String freezeType;

    @ApiModelProperty(value = "相关文件编号", example = "质检报告编号：QC20260127001")
    @Size(max = 100, message = "相关文件编号长度不能超过100")
    private String referenceNo;

    @ApiModelProperty(value = "备注", example = "预计处理期限7天")
    @Size(max = 200, message = "备注长度不能超过200")
    private String remarks;
}
