package com.fisco.app.dto.credit;

import com.fisco.app.enums.CreditLimitStatus;
import com.fisco.app.enums.CreditLimitType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 冻结/解冻信用额度响应DTO
 */
@Data
@ApiModel(value = "冻结/解冻信用额度响应", description = "冻结或解冻额度操作结果")
public class CreditLimitFreezeResponse {

    @ApiModelProperty(value = "额度ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String id;

    @ApiModelProperty(value = "企业地址", example = "0x1234567890abcdef1234567890abcdef12345678")
    private String enterpriseAddress;

    @ApiModelProperty(value = "企业名称", example = "供应商A")
    private String enterpriseName;

    @ApiModelProperty(value = "额度类型", notes = "FINANCING-融资额度, GUARANTEE-担保额度, CREDIT-赊账额度", example = "FINANCING")
    private CreditLimitType limitType;

    @ApiModelProperty(value = "操作前状态", example = "ACTIVE")
    private CreditLimitStatus previousStatus;

    @ApiModelProperty(value = "操作后状态", example = "FROZEN")
    private CreditLimitStatus currentStatus;

    @ApiModelProperty(value = "总额度（元）", example = "1000000.00")
    private BigDecimal totalLimit;

    @ApiModelProperty(value = "已使用额度（元）", example = "300000.00")
    private BigDecimal usedLimit;

    @ApiModelProperty(value = "冻结额度（元）", example = "100000.00")
    private BigDecimal frozenLimit;

    @ApiModelProperty(value = "可用额度（元）", example = "600000.00")
    private BigDecimal availableLimit;

    @ApiModelProperty(value = "操作原因", example = "企业存在逾期记录，冻结额度")
    private String reason;

    @ApiModelProperty(value = "操作人地址", example = "0x9876543210fedcba9876543210fedcba98765432")
    private String operatorAddress;

    @ApiModelProperty(value = "操作时间", example = "2026-01-15T10:30:00")
    private LocalDateTime operationTime;

    @ApiModelProperty(value = "区块链交易哈希", example = "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890")
    private String txHash;
}
