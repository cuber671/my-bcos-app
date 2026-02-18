package com.fisco.app.dto.blockchain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 通用合约部署请求DTO
 *
 * @author FISCO BCOS Team
 * @since 2026-02-10
 */
@Data
@ApiModel(value = "通用合约部署请求", description = "部署自定义智能合约的请求参数")
public class DeployGenericContractRequest {

    @NotBlank(message = "合约ABI不能为空")
    @Size(max = 100000, message = "ABI长度不能超过100KB")
    @ApiModelProperty(value = "合约ABI（JSON字符串）", required = true, example = "[{\"inputs\":[],\"type\":\"constructor\"}]")
    private String abi;

    @NotBlank(message = "合约字节码不能为空")
    @Size(max = 50000, message = "字节码长度不能超过50KB")
    @Pattern(regexp = "^0x[0-9a-fA-F]+$", message = "字节码必须是有效的十六进制格式")
    @ApiModelProperty(value = "合约字节码（十六进制，以0x开头）", required = true)
    private String bytecode;

    @ApiModelProperty(value = "构造函数参数列表")
    private List<String> constructorParams;

    @NotBlank(message = "合约名称不能为空")
    @ApiModelProperty(value = "合约名称", required = true, example = "MyCustomContract")
    private String contractName;

    @ApiModelProperty(value = "合约类型（可选，默认Generic）", example = "Generic")
    private String contractType;

    @ApiModelProperty(value = "合约版本", example = "1.0.0")
    private String contractVersion;

    @ApiModelProperty(value = "合约描述")
    private String description;

    @ApiModelProperty(value = "编译器版本", example = "0.8.0")
    private String compilerVersion;

    @ApiModelProperty(value = "是否启用优化")
    private Boolean optimizationEnabled = false;
}
