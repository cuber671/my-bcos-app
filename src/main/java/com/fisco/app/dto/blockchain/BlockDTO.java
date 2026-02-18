package com.fisco.app.dto.blockchain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 区块信息DTO
 * 用于返回区块链区块的完整信息
 */
@Data
@ApiModel(value = "区块信息", description = "区块链区块详细信息")
public class BlockDTO {

    @ApiModelProperty(value = "区块号", example = "12345")
    private BigInteger blockNumber;

    @ApiModelProperty(value = "区块哈希", example = "0xabc123...")
    private String blockHash;

    @ApiModelProperty(value = "父区块哈希", example = "0xdef456...")
    private String parentHash;

    @ApiModelProperty(value = "交易根哈希", example = "0x789abc...")
    private String transactionRoot;

    @ApiModelProperty(value = "状态根哈希", example = "0xdef789...")
    private String stateRoot;

    @ApiModelProperty(value = "收据根哈希", example = "0x456def...")
    private String receiptsRoot;

    @ApiModelProperty(value = "Gas上限", example = "30000000")
    private BigInteger gasLimit;

    @ApiModelProperty(value = "已使用Gas", example = "15000000")
    private BigInteger gasUsed;

    @ApiModelProperty(value = "时间戳", example = "2026-02-09T10:30:00")
    private LocalDateTime timestamp;

    @ApiModelProperty(value = "出块节点地址", example = "0x123...")
    private String sealer;

    @ApiModelProperty(value = "交易数量", example = "150")
    private Integer transactionCount;

    @ApiModelProperty(value = "交易哈希列表（精简）", example = "[\"0x...\", \"0x...\"]")
    private List<String> transactionHashes;

    @ApiModelProperty(value = "区块大小（字节）", example = "15000")
    private Long blockSize;

    @ApiModelProperty(value = "额外数据", example = "0x...")
    private String extraData;
}
