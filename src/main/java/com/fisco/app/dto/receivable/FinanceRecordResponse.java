package com.fisco.app.dto.receivable;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 融资记录响应DTO
 *
 * 返回应收账款的融资记录信息
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-09
 * @version 1.0
 */
@Data
@ApiModel(value = "融资记录响应", description = "融资记录详细信息")
public class FinanceRecordResponse {

    @ApiModelProperty(value = "应收账款ID", example = "REC20240113001")
    private String receivableId;

    @ApiModelProperty(value = "融资金额", example = "450000.00")
    private BigDecimal financeAmount;

    @ApiModelProperty(value = "融资利率（基点）", example = "500")
    private Integer financeRate;

    @ApiModelProperty(value = "融资日期", example = "2026-02-09T14:30:00")
    private LocalDateTime financeDate;

    @ApiModelProperty(value = "资金方地址", example = "0x567890abcdef1234")
    private String financierAddress;

    @ApiModelProperty(value = "融资本金（应收账款金额）", example = "500000.00")
    private BigDecimal principalAmount;

    @ApiModelProperty(value = "融资比例（%）", example = "90.00")
    private BigDecimal financeRatio;

    @ApiModelProperty(value = "区块链交易哈希", example = "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef12")
    private String txHash;

    @ApiModelProperty(value = "融资状态", example = "FINANCED")
    private String status;

    @ApiModelProperty(value = "融资状态名称", example = "已融资")
    private String statusName;
}
