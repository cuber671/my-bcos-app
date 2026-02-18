package com.fisco.app.dto.blockchain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 交易日志DTO
 * 用于返回智能合约事件日志信息
 */
@Data
@ApiModel(value = "交易日志", description = "智能合约事件日志")
public class TransactionLogDTO {

    @ApiModelProperty(value = "日志地址（合约地址）", example = "0x123...")
    private String address;

    @ApiModelProperty(value = "主题列表", example = "[\"0x...\", \"0x...\"]")
    private List<String> topics;

    @ApiModelProperty(value = "日志数据", example = "0x...")
    private String data;

    @ApiModelProperty(value = "区块号", example = "12345")
    private Long blockNumber;

    @ApiModelProperty(value = "交易哈希", example = "0xabc...")
    private String transactionHash;

    @ApiModelProperty(value = "日志索引", example = "0")
    private Integer logIndex;
}
