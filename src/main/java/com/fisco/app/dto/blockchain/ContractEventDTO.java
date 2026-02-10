package com.fisco.app.dto.blockchain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 合约事件DTO
 *
 * @author FISCO BCOS Team
 * @since 2026-02-10
 */
@Data
@ApiModel(value = "合约事件", description = "合约触发的事件信息")
public class ContractEventDTO {

    @ApiModelProperty(value = "事件名称")
    private String eventName;

    @ApiModelProperty(value = "区块号")
    private Long blockNumber;

    @ApiModelProperty(value = "交易哈希")
    private String transactionHash;

    @ApiModelProperty(value = "事件时间戳")
    private LocalDateTime eventTimestamp;

    @ApiModelProperty(value = "解码后的参数")
    private Map<String, Object> decodedParams;
}
