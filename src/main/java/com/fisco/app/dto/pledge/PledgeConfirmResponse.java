package com.fisco.app.dto.pledge;
import lombok.Builder;
import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 质押确认响应DTO
 */
@Data
@Builder
@ApiModel(value = "质押确认响应", description = "质押确认（批准或拒绝）后的响应信息")
public class PledgeConfirmResponse {

    @ApiModelProperty(value = "背书ID", example = "b2c3d4e5-f6g7-8901-bcde-f23456789012")
    private String endorsementId;

    @ApiModelProperty(value = "确认结果", example = "CONFIRMED")
    private String confirmResult;

    @ApiModelProperty(value = "确认结果描述", example = "确认质押")
    private String confirmResultDesc;

    @ApiModelProperty(value = "仓单ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String receiptId;

    @ApiModelProperty(value = "仓单状态", example = "PLEDGED", notes = "FROZEN->PLEDGED（批准）或 FROZEN->NORMAL（拒绝）")
    private String receiptStatus;

    @ApiModelProperty(value = "质押记录ID", example = "1")
    private Long pledgeRecordId;

    @ApiModelProperty(value = "融资记录ID", example = "1")
    private Long financingRecordId;

    @ApiModelProperty(value = "融资金额", example = "100000.00")
    private BigDecimal financingAmount;

    @ApiModelProperty(value = "年化利率", example = "5.50")
    private BigDecimal interestRate;

    @ApiModelProperty(value = "应还金额", example = "101250.00")
    private BigDecimal repaymentAmount;

    @ApiModelProperty(value = "交易哈希", example = "0xabcdef...")
    private String txHash;

    @ApiModelProperty(value = "区块号", example = "12346")
    private Long blockNumber;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "确认时间", example = "2026-01-27 11:00:00")
    private LocalDateTime confirmTime;

    @ApiModelProperty(value = "原持有人地址", example = "0xabcd...")
    private String previousHolderAddress;

    @ApiModelProperty(value = "当前持有人地址", example = "0x1234...")
    private String currentHolderAddress;

    @ApiModelProperty(value = "响应消息", example = "质押确认成功，仓单所有权已转让给金融机构")
    private String message;
}
