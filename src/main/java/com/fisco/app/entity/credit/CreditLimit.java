package com.fisco.app.entity.credit;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fisco.app.enums.CreditLimitStatus;
import com.fisco.app.enums.CreditLimitType;

import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 信用额度实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "credit_limit", indexes = {
    @Index(name = "idx_enterprise_address", columnList = "enterprise_address"),
    @Index(name = "idx_limit_type", columnList = "limit_type"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_risk_level", columnList = "risk_level"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Schema(name = "信用额度")
public class CreditLimit {

    /**
     * 额度ID（UUID格式，主键）
     */
    @Id
    @Column(name = "id", nullable = false, unique = true, length = 36)
    @ApiModelProperty(value = "额度ID（UUID格式）", required = true, example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String id;

    /**
     * 企业地址（区块链地址）
     */
    @Column(name = "enterprise_address", nullable = false, length = 42)
    @ApiModelProperty(value = "企业区块链地址", required = true, example = "0x1234567890abcdef1234567890abcdef12345678")
    @Size(min = 42, max = 42, message = "区块链地址必须是42位")
    @Pattern(regexp = "^0x[a-fA-F0-9]{40}$", message = "区块链地址格式不正确")
    private String enterpriseAddress;

    /**
     * 企业名称（冗余字段，方便查询）
     */
    @Column(name = "enterprise_name", length = 200)
    @ApiModelProperty(value = "企业名称", example = "供应商A")
    private String enterpriseName;

    /**
     * 额度类型（融资额度/担保额度/赊账额度）
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "limit_type", nullable = false, length = 20)
    @ApiModelProperty(value = "额度类型", required = true, notes = "FINANCING-融资额度, GUARANTEE-担保额度, CREDIT-赊账额度", example = "FINANCING")
    @NotNull(message = "额度类型不能为空")
    private CreditLimitType limitType;

    /**
     * 总额度（单位：分，例如1000000分 = 10000.00元）
     */
    @Column(name = "total_limit", nullable = false)
    @ApiModelProperty(value = "总额度（单位：分）", required = true, example = "100000000", notes = "100000000分 = 1000000.00元")
    @NotNull(message = "总额度不能为空")
    @Min(value = 0, message = "总额度不能为负数")
    private Long totalLimit = 0L;

    /**
     * 已使用额度（单位：分）
     */
    @Column(name = "used_limit", nullable = false)
    @ApiModelProperty(value = "已使用额度（单位：分）", required = true, example = "30000000", notes = "30000000分 = 300000.00元")
    @Min(value = 0, message = "已使用额度不能为负数")
    private Long usedLimit = 0L;

    /**
     * 冻结额度（单位：分）
     */
    @Column(name = "frozen_limit", nullable = false)
    @ApiModelProperty(value = "冻结额度（单位：分）", required = true, example = "10000000", notes = "10000000分 = 100000.00元")
    @Min(value = 0, message = "冻结额度不能为负数")
    private Long frozenLimit = 0L;

    /**
     * 预警阈值（百分比，例如80表示80%）
     */
    @Column(name = "warning_threshold", nullable = false)
    @ApiModelProperty(value = "预警阈值（百分比）", required = true, example = "80", notes = "当使用率达到80%时触发预警")
    @Min(value = 1, message = "预警阈值必须大于0")
    @Max(value = 100, message = "预警阈值不能超过100")
    private Integer warningThreshold = 80;

    /**
     * 生效日期
     */
    @Column(name = "effective_date", nullable = false)
    @ApiModelProperty(value = "生效日期", required = true, example = "2026-01-01T00:00:00")
    @NotNull(message = "生效日期不能为空")
    private LocalDateTime effectiveDate;

    /**
     * 失效日期（可选，为空表示永久有效）
     */
    @Column(name = "expiry_date")
    @ApiModelProperty(value = "失效日期", example = "2027-01-01T00:00:00", notes = "为空表示永久有效")
    private LocalDateTime expiryDate;

    /**
     * 额度状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @ApiModelProperty(value = "额度状态", required = true, notes = "ACTIVE-生效中, FROZEN-已冻结, EXPIRED-已失效, CANCELLED-已取消", example = "ACTIVE")
    private CreditLimitStatus status = CreditLimitStatus.ACTIVE;

    /**
     * 审批人地址（区块链地址）
     */
    @Column(name = "approver_address", length = 42)
    @ApiModelProperty(value = "审批人地址", example = "0x9876543210fedcba9876543210fedcba98765432")
    @Size(min = 42, max = 42, message = "审批人地址必须是42位")
    private String approverAddress;

    /**
     * 审批原因
     */
    @Column(name = "approve_reason", columnDefinition = "TEXT")
    @ApiModelProperty(value = "审批原因", example = "信用评级提升，增加额度")
    private String approveReason;

    /**
     * 审批时间
     */
    @Column(name = "approve_time")
    @ApiModelProperty(value = "审批时间", example = "2026-01-01T10:00:00")
    private LocalDateTime approveTime;

    /**
     * 逾期次数（用于风险评估）
     */
    @Column(name = "overdue_count", nullable = false)
    @ApiModelProperty(value = "逾期次数", required = true, example = "2", notes = "用于动态调整额度")
    @Min(value = 0, message = "逾期次数不能为负数")
    private Integer overdueCount = 0;

    /**
     * 坏账次数（用于风险评估）
     */
    @Column(name = "bad_debt_count", nullable = false)
    @ApiModelProperty(value = "坏账次数", required = true, example = "0", notes = "用于动态调整额度")
    @Min(value = 0, message = "坏账次数不能为负数")
    private Integer badDebtCount = 0;

    /**
     * 风险等级
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", length = 20)
    @ApiModelProperty(value = "风险等级", notes = "LOW-低风险, MEDIUM-中风险, HIGH-高风险", example = "LOW")
    private RiskLevel riskLevel = RiskLevel.LOW;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @ApiModelProperty(value = "创建时间", hidden = true)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at", nullable = false)
    @ApiModelProperty(value = "更新时间", hidden = true)
    private LocalDateTime updatedAt;

    /**
     * 区块链交易哈希
     */
    @Column(name = "tx_hash", length = 66)
    @ApiModelProperty(value = "区块链交易哈希", example = "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890")
    @Size(min = 66, max = 66, message = "交易哈希必须是66位")
    private String txHash;

    /**
     * 额度使用记录（一对多关联）
     */
    @OneToMany(mappedBy = "creditLimit", fetch = FetchType.LAZY)
    @JsonIgnore
    @ApiModelProperty(value = "额度使用记录", hidden = true)
    private java.util.List<CreditLimitUsage> usageRecords;

    /**
     * 额度调整申请记录（一对多关联）
     */
    @OneToMany(mappedBy = "creditLimit", fetch = FetchType.LAZY)
    @JsonIgnore
    @ApiModelProperty(value = "额度调整申请记录", hidden = true)
    private java.util.List<CreditLimitAdjustRequest> adjustRequests;

    /**
     * 额度预警记录（一对多关联）
     */
    @OneToMany(mappedBy = "creditLimit", fetch = FetchType.LAZY)
    @JsonIgnore
    @ApiModelProperty(value = "额度预警记录", hidden = true)
    private java.util.List<CreditLimitWarning> warnings;

    /**
     * 风险等级枚举
     */
    public enum RiskLevel {
        LOW,      // 低风险
        MEDIUM,   // 中风险
        HIGH      // 高风险
    }

    @PrePersist
    protected void onCreate() {
        if (id == null || id.isEmpty()) {
            id = java.util.UUID.randomUUID().toString();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (effectiveDate == null) {
            effectiveDate = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 获取可用额度
     * @return 可用额度（总额度 - 已使用额度 - 冻结额度）
     */
    @Transient
    @ApiModelProperty(value = "可用额度（单位：分）", example = "60000000", notes = "总额度 - 已使用额度 - 冻结额度")
    public Long getAvailableLimit() {
        return totalLimit - usedLimit - frozenLimit;
    }

    /**
     * 获取使用率
     * @return 使用率（百分比）
     */
    @Transient
    @ApiModelProperty(value = "使用率（百分比）", example = "30.0", notes = "已使用额度 / 总额度 * 100")
    public Double getUsageRate() {
        if (totalLimit == 0) {
            return 0.0;
        }
        return (usedLimit * 100.0) / totalLimit;
    }

    /**
     * 是否需要预警
     * @return true-需要预警，false-不需要预警
     */
    @Transient
    @ApiModelProperty(value = "是否需要预警", example = "false", notes = "使用率 >= 预警阈值时返回true")
    public boolean needsWarning() {
        return getUsageRate() >= warningThreshold;
    }
}
