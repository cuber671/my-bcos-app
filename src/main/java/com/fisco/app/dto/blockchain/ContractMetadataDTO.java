package com.fisco.app.dto.blockchain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 合约元数据DTO
 *
 * @author FISCO BCOS Team
 * @since 2026-02-10
 */
@Data
@ApiModel(value = "合约元数据", description = "合约基本信息")
public class ContractMetadataDTO {

    @ApiModelProperty(value = "合约地址")
    private String contractAddress;

    @ApiModelProperty(value = "合约名称")
    private String contractName;

    @ApiModelProperty(value = "合约类型")
    private String contractType;

    @ApiModelProperty(value = "合约版本")
    private String contractVersion;

    @ApiModelProperty(value = "部署者地址")
    private String deployer;

    @ApiModelProperty(value = "部署时间")
    private LocalDateTime deploymentTimestamp;

    @ApiModelProperty(value = "合约状态")
    private String status;

    @ApiModelProperty(value = "合约描述")
    private String description;

    @ApiModelProperty(value = "区块号")
    private Long deployBlockNumber;

    @ApiModelProperty(value = "部署交易哈希")
    private String deployTransactionHash;
}
