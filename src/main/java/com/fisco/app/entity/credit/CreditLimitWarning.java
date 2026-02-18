package com.fisco.app.entity.credit;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fisco.app.enums.CreditWarningLevel;

import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 信用额度预警记录实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "credit_limit_warning", indexes = {
    @Index(name = "idx_credit_limit_id", columnList = "credit_limit_id"),
    @Index(name = "idx_warning_level", columnList = "warning_level"),
    @Index(name = "idx_warning_date", columnList = "warning_date"),
    @Index(name = "idx_is_resolved", columnList = "is_resolved")
})
@Schema(name = "信用额度预警记录")
public class CreditLimitWarning {

    /**
     * 预警ID（UUID格式，主键）
     */
    @Id
    @Column(name = "id", nullable = false, unique = true, length = 36)
    @ApiModelProperty(value = "预警ID（UUID格式）", required = true, example = "e5f6g7h8-i9j0-1234-efgh-456789012345")
    private String id;

    /**
     * 额度ID（外键）
     */
    @Column(name = "credit_limit_id", nullable = false, length = 36)
    @ApiModelProperty(value = "额度ID", required = true, example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    @NotNull(message = "额度ID不能为空")
    private String creditLimitId;

    /**
     * 预警级别
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "warning_level", nullable = false, length = 20)
    @ApiModelProperty(value = "预警级别", required = true, notes = "LOW-低风险, MEDIUM-中风险, HIGH-高风险, CRITICAL-紧急", example = "MEDIUM")
    @NotNull(message = "预警级别不能为空")
    private CreditWarningLevel warningLevel;

    /**
     * 预警类型（使用率过高/额度即将到期/风险等级提升）
     */
    @Column(name = "warning_type", nullable = false, length = 50)
    @ApiModelProperty(value = "预警类型", required = true, notes = "USAGE_HIGH-使用率过高, EXPIRY_SOON-额度即将到期, RISK_UP-风险等级提升, OVERDUE-存在逾期", example = "USAGE_HIGH")
    @NotBlank(message = "预警类型不能为空")
    private String warningType;

    /**
     * 当前使用率（百分比）
     */
    @Column(name = "current_usage_rate", nullable = false)
    @ApiModelProperty(value = "当前使用率（百分比）", required = true, example = "85.5", notes = "85.5表示85.5%")
    @NotNull(message = "当前使用率不能为空")
    @DecimalMin(value = "0.0", message = "使用率不能小于0")
    @DecimalMax(value = "100.0", message = "使用率不能大于100")
    private Double currentUsageRate;

    /**
     * 预警阈值（百分比）
     */
    @Column(name = "warning_threshold", nullable = false)
    @ApiModelProperty(value = "预警阈值（百分比）", required = true, example = "80.0", notes = "80.0表示80%")
    @NotNull(message = "预警阈值不能为空")
    @DecimalMin(value = "0.0", message = "预警阈值不能小于0")
    @DecimalMax(value = "100.0", message = "预警阈值不能大于100")
    private Double warningThreshold;

    /**
     * 预警标题
     */
    @Column(name = "warning_title", nullable = false, length = 200)
    @ApiModelProperty(value = "预警标题", required = true, example = "融资额度使用率超过80%")
    @NotBlank(message = "预警标题不能为空")
    @Size(max = 200, message = "预警标题长度不能超过200")
    private String warningTitle;

    /**
     * 预警内容
     */
    @Column(name = "warning_content", nullable = false, columnDefinition = "TEXT")
    @ApiModelProperty(value = "预警内容", required = true, example = "企业的融资额度使用率已达到85.5%，超过预警阈值80%，请注意控制额度使用")
    @NotBlank(message = "预警内容不能为空")
    @Size(max = 2000, message = "预警内容长度不能超过2000")
    private String warningContent;

    /**
     * 预警日期
     */
    @Column(name = "warning_date", nullable = false)
    @ApiModelProperty(value = "预警日期", required = true, example = "2026-01-15T10:30:00")
    @NotNull(message = "预警日期不能为空")
    private LocalDateTime warningDate;

    /**
     * 是否已处理
     */
    @Column(name = "is_resolved", nullable = false)
    @ApiModelProperty(value = "是否已处理", required = true, example = "false", notes = "true-已处理，false-未处理")
    @NotNull(message = "是否已处理不能为空")
    private Boolean isResolved = false;

    /**
     * 处理人地址（区块链地址）
     */
    @Column(name = "resolved_by_address", length = 42)
    @ApiModelProperty(value = "处理人地址", example = "0x9876543210fedcba9876543210fedcba98765432")
    @Size(min = 42, max = 42, message = "处理人地址必须是42位")
    private String resolvedByAddress;

    /**
     * 处理人姓名（冗余字段，方便查询）
     */
    @Column(name = "resolved_by_name", length = 100)
    @ApiModelProperty(value = "处理人姓名", example = "李四")
    @Size(max = 100, message = "处理人姓名长度不能超过100")
    private String resolvedByName;

    /**
     * 处理日期
     */
    @Column(name = "resolved_date")
    @ApiModelProperty(value = "处理日期", example = "2026-01-16T09:00:00")
    private LocalDateTime resolvedDate;

    /**
     * 处理措施
     */
    @Column(name = "resolution", columnDefinition = "TEXT")
    @ApiModelProperty(value = "处理措施", example = "已通知企业控制额度使用，并安排风险经理跟进")
    private String resolution;

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
     * 关联的信用额度（多对一关联）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_limit_id", insertable = false, updatable = false)
    @JsonIgnore
    @ApiModelProperty(value = "关联的信用额度", hidden = true)
    private CreditLimit creditLimit;

    /**
     * 区块链交易哈希
     */
    @Column(name = "tx_hash", length = 66)
    @ApiModelProperty(value = "区块链交易哈希", example = "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890")
    @Size(max = 66, message = "交易哈希最多66位")
    private String txHash;

    @PrePersist
    protected void onCreate() {
        if (id == null || id.isEmpty()) {
            id = java.util.UUID.randomUUID().toString();
        }
        if (warningDate == null) {
            warningDate = LocalDateTime.now();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 预警类型枚举
     */
    public enum WarningType {
        USAGE_HIGH,        // 使用率过高
        EXPIRY_SOON,       // 额度即将到期
        RISK_UP,           // 风险等级提升
        OVERDUE            // 存在逾期
    }
}
