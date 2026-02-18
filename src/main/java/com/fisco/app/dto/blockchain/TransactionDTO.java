package com.fisco.app.dto.blockchain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 交易信息DTO
 * 用于返回区块链交易的完整信息
 */
@Data
@ApiModel(value = "交易信息", description = "区块链交易详细信息")
public class TransactionDTO {

    @ApiModelProperty(value = "交易哈希", example = "0xabc123...")
    private String transactionHash;

    @ApiModelProperty(value = "区块号", example = "12345")
    private BigInteger blockNumber;

    @ApiModelProperty(value = "区块哈希", example = "0xdef456...")
    private String blockHash;

    @ApiModelProperty(value = "交易索引", example = "0")
    private Integer transactionIndex;

    @ApiModelProperty(value = "发送地址", example = "0x123...")
    private String from;

    @ApiModelProperty(value = "接收地址（合约创建时为null）", example = "0x456...")
    private String to;

    @ApiModelProperty(value = "交易值（wei）", example = "1000000000000000000")
    private BigInteger value;

    @ApiModelProperty(value = "Gas价格", example = "1000000000")
    private BigInteger gasPrice;

    @ApiModelProperty(value = "Gas限制", example = "21000")
    private BigInteger gasLimit;

    @ApiModelProperty(value = "已使用Gas", example = "21000")
    private BigInteger gasUsed;

    @ApiModelProperty(value = "交易输入数据", example = "0x...")
    private String input;

    @ApiModelProperty(value = "方法签名", example = "transfer(address,uint256)")
    private String methodId;

    @ApiModelProperty(value = "交易状态（0=失败, 1=成功）", example = "1")
    private Integer status;

    @ApiModelProperty(value = "交易时间戳", example = "2026-02-09T10:30:00")
    private LocalDateTime timestamp;

    @ApiModelProperty(value = "日志列表")
    private List<TransactionLogDTO> logs;
}
