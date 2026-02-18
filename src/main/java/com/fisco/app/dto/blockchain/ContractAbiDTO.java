package com.fisco.app.dto.blockchain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 合约ABI DTO
 *
 * @author FISCO BCOS Team
 * @since 2026-02-10
 */
@Data
@ApiModel(value = "合约ABI信息", description = "合约的ABI、字节码等元数据")
public class ContractAbiDTO {

    @ApiModelProperty(value = "合约地址")
    private String contractAddress;

    @ApiModelProperty(value = "合约名称")
    private String contractName;

    @ApiModelProperty(value = "合约类型")
    private String contractType;

    @ApiModelProperty(value = "合约版本")
    private String contractVersion;

    @ApiModelProperty(value = "合约ABI（JSON字符串）")
    private String abi;

    @ApiModelProperty(value = "合约字节码")
    private String bytecode;

    @ApiModelProperty(value = "编译器版本")
    private String compilerVersion;

    @ApiModelProperty(value = "合约源代码")
    private String sourceCode;

    @ApiModelProperty(value = "构造函数参数")
    private List<String> constructorParams;
}
