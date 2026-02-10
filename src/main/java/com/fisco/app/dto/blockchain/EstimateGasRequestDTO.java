package com.fisco.app.dto.blockchain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * Gas估算请求DTO
 *
 * @author FISCO BCOS Team
 * @since 2026-02-10
 */
@Data
@ApiModel(value = "Gas估算请求", description = "估算交易Gas消耗的请求参数")
public class EstimateGasRequestDTO {

    @ApiModelProperty(value = "接收地址", required = true)
    @NotBlank(message = "接收地址不能为空")
    @Pattern(regexp = "^0x[a-fA-F0-9]{40}$", message = "地址格式不正确")
    private String toAddress;

    @ApiModelProperty(value = "交易值（wei）", example = "0")
    private String value = "0";

    @ApiModelProperty(value = "交易输入数据")
    private String data;
}
