package com.fisco.app.dto.blockchain;

import java.time.LocalDateTime;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 通用合约部署响应DTO
 *
 * @author FISCO BCOS Team
 * @since 2026-02-10
 */
@Data
@ApiModel(value = "通用合约部署响应", description = "合约部署成功后返回的信息")
public class DeployGenericContractResponse {

    @ApiModelProperty(value = "合约地址")
    private String contractAddress;

    @ApiModelProperty(value = "部署交易哈希")
    private String transactionHash;

    @ApiModelProperty(value = "区块号")
    private Long blockNumber;

    @ApiModelProperty(value = "Gas使用量")
    private Long gasUsed;

    @ApiModelProperty(value = "部署者地址")
    private String deployer;

    @ApiModelProperty(value = "部署时间")
    private LocalDateTime deploymentTimestamp;
}
