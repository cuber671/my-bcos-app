package com.fisco.app.dto.credit;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.time.LocalDateTime;


/**
 * 信用额度调整申请响应DTO
 */
@Data
@ApiModel(value = "信用额度调整申请响应", description = "额度调整申请结果")
public class CreditLimitAdjustResponse {

    @ApiModelProperty(value = "申请ID", example = "d4e5f6g7-h8i9-0123-defg-345678901234")
    private String id;

    @ApiModelProperty(value = "额度ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String creditLimitId;

    @ApiModelProperty(value = "调整类型", notes = "INCREASE-增加, DECREASE-减少, RESET-重置", example = "INCREASE")
    private String adjustType;

    @ApiModelProperty(value = "当前额度（元）", example = "1000000.00")
    private java.math.BigDecimal currentLimit;

    @ApiModelProperty(value = "调整后额度（元）", example = "1500000.00")
    private java.math.BigDecimal newLimit;

    @ApiModelProperty(value = "调整金额（元）", example = "500000.00")
    private java.math.BigDecimal adjustAmount;

    @ApiModelProperty(value = "申请原因", example = "企业信用评级提升，申请增加融资额度")
    private String requestReason;

    @ApiModelProperty(value = "申请人地址", example = "0x1234567890abcdef1234567890abcdef12345678")
    private String requesterAddress;

    @ApiModelProperty(value = "申请人姓名", example = "张三")
    private String requesterName;

    @ApiModelProperty(value = "申请日期", example = "2026-01-15T10:30:00")
    private LocalDateTime requestDate;

    @ApiModelProperty(value = "申请状态", notes = "PENDING-待审批, APPROVED-已通过, REJECTED-已拒绝", example = "PENDING")
    private String requestStatus;

    @ApiModelProperty(value = "审批人地址", example = "0x9876543210fedcba9876543210fedcba98765432")
    private String approverAddress;

    @ApiModelProperty(value = "审批人姓名", example = "李四")
    private String approverName;

    @ApiModelProperty(value = "审批日期", example = "2026-01-16T14:00:00")
    private LocalDateTime approveDate;

    @ApiModelProperty(value = "审批意见", example = "同意增加额度，企业信用良好")
    private String approveReason;

    @ApiModelProperty(value = "拒绝原因", example = "企业存在逾期记录，暂不符合增加额度条件")
    private String rejectReason;

    @ApiModelProperty(value = "区块链交易哈希", example = "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890")
    private String txHash;
}
