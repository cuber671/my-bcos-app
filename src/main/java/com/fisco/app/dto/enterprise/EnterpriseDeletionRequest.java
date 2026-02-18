package com.fisco.app.dto.enterprise;
import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 企业注销请求DTO
 */
@Data
@ApiModel(value = "企业注销请求", description = "企业注销申请请求参数")
public class EnterpriseDeletionRequest {

    @ApiModelProperty(value = "注销理由", required = true, example = "业务调整，申请注销企业")
    @NotBlank(message = "注销理由不能为空")
    @Size(max = 500, message = "注销理由长度不能超过500字符")
    private String reason;
}
