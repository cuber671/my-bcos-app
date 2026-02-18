package com.fisco.app.dto.credit;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.time.LocalDateTime;

import com.fisco.app.enums.CreditWarningLevel;

/**
 * 信用额度预警记录DTO
 */
@Data
@ApiModel(value = "信用额度预警记录DTO", description = "额度预警记录详细信息")
public class CreditLimitWarningDTO {

    @ApiModelProperty(value = "预警ID", example = "e5f6g7h8-i9j0-1234-efgh-456789012345")
    private String id;

    @ApiModelProperty(value = "额度ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String creditLimitId;

    @ApiModelProperty(value = "企业地址", example = "0x1234567890abcdef1234567890abcdef12345678")
    private String enterpriseAddress;

    @ApiModelProperty(value = "企业名称", example = "供应商A")
    private String enterpriseName;

    @ApiModelProperty(value = "预警级别", notes = "LOW-低风险, MEDIUM-中风险, HIGH-高风险, CRITICAL-紧急", example = "MEDIUM")
    private CreditWarningLevel warningLevel;

    @ApiModelProperty(value = "预警类型", example = "USAGE_HIGH", notes = "USAGE_HIGH-使用率过高, EXPIRY_SOON-额度即将到期, RISK_UP-风险等级提升, OVERDUE-存在逾期")
    private String warningType;

    @ApiModelProperty(value = "当前使用率（百分比）", example = "85.5")
    private Double currentUsageRate;

    @ApiModelProperty(value = "预警阈值（百分比）", example = "80.0")
    private Double warningThreshold;

    @ApiModelProperty(value = "预警标题", example = "融资额度使用率超过80%")
    private String warningTitle;

    @ApiModelProperty(value = "预警内容", example = "企业的融资额度使用率已达到85.5%，超过预警阈值80%，请注意控制额度使用")
    private String warningContent;

    @ApiModelProperty(value = "预警日期", example = "2026-01-15T10:30:00")
    private LocalDateTime warningDate;

    @ApiModelProperty(value = "是否已处理", example = "false")
    private Boolean isResolved;

    @ApiModelProperty(value = "处理人地址", example = "0x9876543210fedcba9876543210fedcba98765432")
    private String resolvedByAddress;

    @ApiModelProperty(value = "处理人姓名", example = "李四")
    private String resolvedByName;

    @ApiModelProperty(value = "处理日期", example = "2026-01-16T09:00:00")
    private LocalDateTime resolvedDate;

    @ApiModelProperty(value = "处理措施", example = "已通知企业控制额度使用，并安排风险经理跟进")
    private String resolution;

    @ApiModelProperty(value = "创建时间", example = "2026-01-15T10:30:00")
    private LocalDateTime createdAt;

    @ApiModelProperty(value = "更新时间", example = "2026-01-16T09:00:00")
    private LocalDateTime updatedAt;

    @ApiModelProperty(value = "区块链交易哈希", example = "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890")
    private String txHash;
}
