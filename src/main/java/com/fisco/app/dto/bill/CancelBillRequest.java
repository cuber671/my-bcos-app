package com.fisco.app.dto.bill;


import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotBlank;

/**
 * 票据作废请求DTO
 */
@Data
@ApiModel(value = "票据作废请求", description = "用于作废票据的请求参数")
public class CancelBillRequest {

    @NotBlank(message = "作废原因不能为空")
    @ApiModelProperty(value = "作废原因", required = true, example = "票据丢失")
    private String cancelReason;

    @NotBlank(message = "作废类型不能为空")
    @ApiModelProperty(value = "作废类型", required = true, example = "LOST", notes = "LOST-丢失, WRONG-错误开票, DAMAGED-损毁, OTHER-其他")
    private String cancelType;

    @ApiModelProperty(value = "相关凭证号", example = "报案编号20260126001")
    private String referenceNo;
}
