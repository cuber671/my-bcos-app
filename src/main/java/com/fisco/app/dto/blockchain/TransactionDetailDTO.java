package com.fisco.app.dto.blockchain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 交易详情响应DTO
 *
 * @author FISCO BCOS Team
 * @since 2026-02-10
 */
@Data
@ApiModel(value = "交易详情", description = "区块链交易完整信息")
public class TransactionDetailDTO {

    @ApiModelProperty(value = "交易哈希")
    private String transactionHash;

    @ApiModelProperty(value = "区块号")
    private Long blockNumber;

    @ApiModelProperty(value = "区块哈希")
    private String blockHash;

    @ApiModelProperty(value = "交易索引")
    private Integer transactionIndex;

    @ApiModelProperty(value = "发送地址")
    private String fromAddress;

    @ApiModelProperty(value = "接收地址")
    private String toAddress;

    @ApiModelProperty(value = "交易值（wei）")
    private String value;

    @ApiModelProperty(value = "Gas价格")
    private Long gasPrice;

    @ApiModelProperty(value = "Gas限制")
    private Long gasLimit;

    @ApiModelProperty(value = "已使用Gas")
    private Long gasUsed;

    @ApiModelProperty(value = "交易输入数据")
    private String inputData;

    @ApiModelProperty(value = "方法签名")
    private String methodId;

    @ApiModelProperty(value = "交易状态")
    private String status;

    @ApiModelProperty(value = "交易类型")
    private String transactionType;

    @ApiModelProperty(value = "错误信息（失败时）")
    private String errorMessage;

    @ApiModelProperty(value = "交易时间戳")
    private LocalDateTime createdAt;

    @ApiModelProperty(value = "交易回执")
    private TransactionReceiptDTO receipt;

    @ApiModelProperty(value = "交易日志")
    private List<TransactionLogDTO> logs;
}
