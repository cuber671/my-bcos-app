package com.fisco.app.dto.blockchain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 交易回执DTO
 *
 * @author FISCO BCOS Team
 * @since 2026-02-10
 */
@Data
@ApiModel(value = "交易回执", description = "交易执行回执信息")
public class TransactionReceiptDTO {

    @ApiModelProperty(value = "交易哈希")
    private String transactionHash;

    @ApiModelProperty(value = "区块号")
    private Long blockNumber;

    @ApiModelProperty(value = "区块哈希")
    private String blockHash;

    @ApiModelProperty(value = "交易索引")
    private Integer transactionIndex;

    @ApiModelProperty(value = "已使用Gas")
    private Long gasUsed;

    @ApiModelProperty(value = "累计Gas使用")
    private Long cumulativeGasUsed;

    @ApiModelProperty(value = "合约地址（合约创建时）")
    private String contractAddress;

    @ApiModelProperty(value = "执行状态（0-成功, 1-失败）")
    private Integer status;

    @ApiModelProperty(value = "回滚原因（失败时）")
    private String revertReason;

    @ApiModelProperty(value = "回执获取时间")
    private LocalDateTime receiptObtainedAt;
}
