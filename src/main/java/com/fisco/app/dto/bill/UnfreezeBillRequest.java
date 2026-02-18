package com.fisco.app.dto.bill;


import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotBlank;

/**
 * 票据解冻请求DTO
 */
@Data
@ApiModel(value = "票据解冻请求", description = "用于解冻票据的请求参数")
public class UnfreezeBillRequest {

    @NotBlank(message = "解冻原因不能为空")
    @ApiModelProperty(value = "解冻原因", required = true, example = "纠纷已解决")
    private String unfreezeReason;

    @ApiModelProperty(value = "相关凭证号", example = "法院解冻通知书20260126001")
    private String referenceNo;
}
