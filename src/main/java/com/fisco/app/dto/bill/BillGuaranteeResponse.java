package com.fisco.app.dto.bill;

import com.fisco.app.entity.bill.BillGuarantee;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 票据担保响应DTO
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-09
 * @version 1.0
 */
@Data
@ApiModel(value = "票据担保响应", description = "票据担保操作结果")
public class BillGuaranteeResponse {

    @ApiModelProperty(value = "担保记录ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String guaranteeId;

    @ApiModelProperty(value = "票据ID", example = "b1a2b3c4-d5e6-7890-abcd-ef1234567890")
    private String billId;

    @ApiModelProperty(value = "票据编号", example = "BIL20260200000001")
    private String billNo;

    @ApiModelProperty(value = "担保人ID", example = "g1a2b3c4-d5e6-7890-abcd-ef1234567890")
    private String guarantorId;

    @ApiModelProperty(value = "担保人名称", example = "担保公司A")
    private String guarantorName;

    @ApiModelProperty(value = "担保人地址", example = "0x1234567890abcdef1234567890abcdef12345678")
    private String guarantorAddress;

    @ApiModelProperty(value = "担保类型", example = "FULL")
    private BillGuarantee.GuaranteeType guaranteeType;

    @ApiModelProperty(value = "担保金额", example = "1000000.00")
    private BigDecimal guaranteeAmount;

    @ApiModelProperty(value = "担保费率（%）", example = "2.500000")
    private BigDecimal guaranteeRate;

    @ApiModelProperty(value = "担保费用", example = "25000.00")
    private BigDecimal guaranteeFee;

    @ApiModelProperty(value = "担保期限（天）", example = "90")
    private Integer guaranteePeriod;

    @ApiModelProperty(value = "担保开始日期", example = "2026-02-09T10:00:00")
    private LocalDateTime guaranteeStartDate;

    @ApiModelProperty(value = "担保结束日期", example = "2026-05-10T10:00:00")
    private LocalDateTime guaranteeEndDate;

    @ApiModelProperty(value = "风险等级", example = "LOW")
    private BillGuarantee.RiskLevel riskLevel;

    @ApiModelProperty(value = "信用评分", example = "75")
    private Integer creditScore;

    @ApiModelProperty(value = "区块链交易哈希", example = "0xabcdef...")
    private String txHash;

    @ApiModelProperty(value = "操作结果", example = "success")
    private String result;

    @ApiModelProperty(value = "消息", example = "票据担保成功")
    private String message;
}
