package com.fisco.app.dto.receivable;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;

import java.util.List;

import javax.validation.Valid;

/**
 * 仓单拆分申请请求DTO
 */
@Data
@ApiModel(value = "仓单拆分申请请求", description = "货主申请拆分仓单")
public class SplitApplicationRequest {

    @ApiModelProperty(value = "父仓单ID", required = true, example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    @NotBlank(message = "父仓单ID不能为空")
    private String parentReceiptId;

    @ApiModelProperty(value = "拆分原因", required = true, example = "部分货物用于质押融资")
    @NotBlank(message = "拆分原因不能为空")
    private String splitReason;

    @ApiModelProperty(value = "拆分详情列表", required = true)
    @NotEmpty(message = "至少需要拆分成2个子仓单")
    @Valid
    private List<SplitDetailRequest> splits;

    @ApiModelProperty(value = "备注", example = "按仓库区域拆分，便于管理")
    private String remarks;
}
