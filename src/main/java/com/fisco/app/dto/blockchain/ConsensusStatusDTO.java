package com.fisco.app.dto.blockchain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 共识状态DTO
 * 描述区块链网络的共识机制状态
 *
 * @author FISCO BCOS Team
 * @since 2026-02-10
 */
@Data
@ApiModel(value = "共识状态", description = "区块链网络共识机制状态信息")
public class ConsensusStatusDTO {

    @ApiModelProperty(value = "共识算法", notes = "PBFT/RBFT等", example = "pbft")
    private String consensusAlgorithm;

    @ApiModelProperty(value = "共识节点数量", example = "4")
    private Integer sealerCount;

    @ApiModelProperty(value = "观察节点数量", example = "1")
    private Integer observerCount;

    @ApiModelProperty(value = "区块最大交易数", example = "1000")
    private Integer maxTransactions;

    @ApiModelProperty(value = "共识超时时间（毫秒）", example = "3000")
    private Integer consensusTimeout;

    @ApiModelProperty(value = "当前区块高度", example = "12345")
    private String currentBlockNumber;

    @ApiModelProperty(value = "共识状态", notes = "RUNNING/STOPPED/RECOVERING", example = "RUNNING")
    private String status;

    @ApiModelProperty(value = "群组ID", example = "group0")
    private String groupId;
}
