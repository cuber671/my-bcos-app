package com.fisco.app.dto.bill;


import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotBlank;

/**
 * 票据冻结请求DTO
 */
@Data
@ApiModel(value = "票据冻结请求", description = "用于冻结票据的请求参数")
public class FreezeBillRequest {

    @NotBlank(message = "冻结原因不能为空")
    @ApiModelProperty(value = "冻结原因", required = true, example = "法律纠纷")
    private String freezeReason;

    @ApiModelProperty(value = "相关凭证号", example = "法院文书号20260126001")
    private String referenceNo;

    @ApiModelProperty(value = "证据文件/说明", example = "法院冻结通知书")
    private String evidence;
}
