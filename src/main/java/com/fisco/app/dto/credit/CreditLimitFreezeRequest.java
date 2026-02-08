package com.fisco.app.dto.credit;

import lombok.Data;
import lombok.Getter;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import org.springframework.lang.NonNull;

/**
 * 冻结/解冻信用额度请求DTO
 */
@Data
@ApiModel(value = "冻结/解冻信用额度请求", description = "用于冻结或解冻企业信用额度的请求参数")
public class CreditLimitFreezeRequest {

    @ApiModelProperty(value = "额度ID", required = true, example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    @NotBlank(message = "额度ID不能为空")
    @Getter(onMethod_ = @__({@NonNull}))
    private String creditLimitId;

    @ApiModelProperty(value = "冻结/解冻原因", required = true, example = "企业存在逾期记录，冻结额度")
    @NotBlank(message = "原因不能为空")
    @Size(max = 500, message = "原因长度不能超过500")
    @Getter(onMethod_ = @__({@NonNull}))
    private String reason;

    @ApiModelProperty(value = "操作类型（系统自动判断）", hidden = true)
    private Boolean freeze;
}
