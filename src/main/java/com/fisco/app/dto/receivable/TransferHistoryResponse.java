package com.fisco.app.dto.receivable;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 应收账款转让历史响应DTO
 *
 * 返回应收账款的转让历史记录
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-09
 * @version 1.0
 */
@Data
@ApiModel(value = "转让历史响应", description = "应收账款转让历史记录")
public class TransferHistoryResponse {

    @ApiModelProperty(value = "转让记录ID", example = "1")
    private Long id;

    @ApiModelProperty(value = "应收账款ID", example = "REC20240113001")
    private String receivableId;

    @ApiModelProperty(value = "转出方地址", example = "0x1234567890abcdef")
    private String fromAddress;

    @ApiModelProperty(value = "转入方地址", example = "0xabcdef1234567890")
    private String toAddress;

    @ApiModelProperty(value = "转让金额", example = "500000.00")
    private BigDecimal amount;

    @ApiModelProperty(value = "转让类型", example = "financing")
    private String transferType;

    @ApiModelProperty(value = "转让类型名称", example = "融资")
    private String transferTypeName;

    @ApiModelProperty(value = "时间戳", example = "2026-02-09T14:30:00")
    private LocalDateTime timestamp;

    @ApiModelProperty(value = "区块链交易哈希", example = "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef12")
    private String txHash;
}
