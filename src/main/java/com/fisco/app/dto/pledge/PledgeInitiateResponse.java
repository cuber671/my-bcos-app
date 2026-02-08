package com.fisco.app.dto.pledge;

import lombok.Builder;
import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 质押发起响应DTO
 */
@Data
@Builder
@ApiModel(value = "质押发起响应", description = "质押发起成功后的响应信息")
public class PledgeInitiateResponse {

    @ApiModelProperty(value = "背书ID", example = "b2c3d4e5-f6g7-8901-bcde-f23456789012")
    private String endorsementId;

    @ApiModelProperty(value = "背书编号", example = "END20260126000001")
    private String endorsementNo;

    @ApiModelProperty(value = "仓单ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String receiptId;

    @ApiModelProperty(value = "仓单编号", example = "EWR202601270001")
    private String receiptNo;

    @ApiModelProperty(value = "仓单状态", example = "FROZEN", notes = "NORMAL->FROZEN")
    private String receiptStatus;

    @ApiModelProperty(value = "质押金额", example = "100000.00")
    private BigDecimal pledgeAmount;

    @ApiModelProperty(value = "金融机构ID", example = "fin-001")
    private String financialInstitutionId;

    @ApiModelProperty(value = "金融机构名称", example = "XX银行")
    private String financialInstitutionName;

    @ApiModelProperty(value = "背书状态", example = "PENDING")
    private String endorsementStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "发起时间", example = "2026-01-27 10:30:00")
    private LocalDateTime initiateTime;
}
