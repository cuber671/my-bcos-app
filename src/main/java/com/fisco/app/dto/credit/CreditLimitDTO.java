package com.fisco.app.dto.credit;

import com.fisco.app.entity.credit.CreditLimit;
import com.fisco.app.enums.CreditLimitStatus;
import com.fisco.app.enums.CreditLimitType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 信用额度DTO
 */
@Data
@ApiModel(value = "信用额度DTO", description = "信用额度详细信息")
public class CreditLimitDTO {

    @ApiModelProperty(value = "额度ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String id;

    @ApiModelProperty(value = "企业地址", example = "0x1234567890abcdef1234567890abcdef12345678")
    private String enterpriseAddress;

    @ApiModelProperty(value = "企业名称", example = "供应商A")
    private String enterpriseName;

    @ApiModelProperty(value = "额度类型", notes = "FINANCING-融资额度, GUARANTEE-担保额度, CREDIT-赊账额度", example = "FINANCING")
    private CreditLimitType limitType;

    @ApiModelProperty(value = "总额度（元）", example = "1000000.00", notes = "自动从分转换为元显示")
    private BigDecimal totalLimit;

    @ApiModelProperty(value = "已使用额度（元）", example = "300000.00")
    private BigDecimal usedLimit;

    @ApiModelProperty(value = "冻结额度（元）", example = "100000.00")
    private BigDecimal frozenLimit;

    @ApiModelProperty(value = "可用额度（元）", example = "600000.00", notes = "总额度 - 已使用额度 - 冻结额度")
    private BigDecimal availableLimit;

    @ApiModelProperty(value = "使用率（百分比）", example = "30.0", notes = "已使用额度 / 总额度 * 100")
    private Double usageRate;

    @ApiModelProperty(value = "预警阈值（百分比）", example = "80.0")
    private Integer warningThreshold;

    @ApiModelProperty(value = "是否需要预警", example = "false", notes = "使用率 >= 预警阈值时为true")
    private Boolean needsWarning;

    @ApiModelProperty(value = "生效日期", example = "2026-01-01T00:00:00")
    private LocalDateTime effectiveDate;

    @ApiModelProperty(value = "失效日期", example = "2027-01-01T00:00:00")
    private LocalDateTime expiryDate;

    @ApiModelProperty(value = "额度状态", notes = "ACTIVE-生效中, FROZEN-已冻结, EXPIRED-已失效, CANCELLED-已取消", example = "ACTIVE")
    private CreditLimitStatus status;

    @ApiModelProperty(value = "审批人地址", example = "0x9876543210fedcba9876543210fedcba98765432")
    private String approverAddress;

    @ApiModelProperty(value = "审批原因", example = "信用评级提升，增加额度")
    private String approveReason;

    @ApiModelProperty(value = "审批时间", example = "2026-01-01T10:00:00")
    private LocalDateTime approveTime;

    @ApiModelProperty(value = "逾期次数", example = "2")
    private Integer overdueCount;

    @ApiModelProperty(value = "坏账次数", example = "0")
    private Integer badDebtCount;

    @ApiModelProperty(value = "风险等级", notes = "LOW-低风险, MEDIUM-中风险, HIGH-高风险", example = "LOW")
    private CreditLimit.RiskLevel riskLevel;

    @ApiModelProperty(value = "创建时间", example = "2026-01-01T10:00:00")
    private LocalDateTime createdAt;

    @ApiModelProperty(value = "更新时间", example = "2026-01-15T14:30:00")
    private LocalDateTime updatedAt;

    @ApiModelProperty(value = "区块链交易哈希", example = "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890")
    private String txHash;
}
