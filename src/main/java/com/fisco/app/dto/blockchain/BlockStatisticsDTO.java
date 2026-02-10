package com.fisco.app.dto.blockchain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;

/**
 * 区块链统计DTO
 * 用于返回区块链网络的统计信息
 */
@Data
@ApiModel(value = "区块链统计", description = "区块链网络统计信息")
public class BlockStatisticsDTO {

    @ApiModelProperty(value = "最新区块号", example = "12345")
    private BigInteger latestBlockNumber;

    @ApiModelProperty(value = "总交易数", example = "500000")
    private BigInteger totalTransactions;

    @ApiModelProperty(value = "平均出块时间（秒）", example = "1.5")
    private BigDecimal averageBlockTime;

    @ApiModelProperty(value = "交易吞吐量（TPS）", example = "1500")
    private BigDecimal transactionsPerSecond;

    @ApiModelProperty(value = "平均Gas使用量", example = "50000")
    private BigInteger averageGasUsed;

    @ApiModelProperty(value = "Gas使用率（%）", example = "50.0")
    private BigDecimal gasUtilizationRate;

    @ApiModelProperty(value = "最近24小时交易数", example = "129600")
    private Long transactionsLast24h;

    @ApiModelProperty(value = "最近24小时区块数", example = "57600")
    private Long blocksLast24h;

    @ApiModelProperty(value = "节点数量", example = "4")
    private Integer nodeCount;

    @ApiModelProperty(value = "统计时间", example = "2026-02-09T10:30:00")
    private LocalDateTime calculatedAt;
}
