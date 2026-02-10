package com.fisco.app.entity.bill;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 票据担保记录实体类
 *
 * 功能：记录第三方为票据提供担保的详细信息
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-09
 * @version 1.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bill_guarantee", indexes = {
    @Index(name = "idx_bill", columnList = "bill_id"),
    @Index(name = "idx_guarantor", columnList = "guarantor_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_guarantee_end_date", columnList = "guarantee_end_date"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@ApiModel(value = "票据担保记录", description = "第三方为票据提供担保的详细信息")
public class BillGuarantee {

    // ==================== 主键 ====================

    @Id
    @Column(name = "id", nullable = false, unique = true, length = 36)
    @ApiModelProperty(value = "担保记录ID（UUID格式）", required = true)
    private String id;

    // ==================== 票据信息 ====================

    @Column(name = "bill_id", nullable = false, length = 36)
    @ApiModelProperty(value = "票据ID", required = true)
    private String billId;

    @Column(name = "bill_no", nullable = false, length = 50)
    @ApiModelProperty(value = "票据编号", required = true)
    private String billNo;

    // ==================== 担保人信息 ====================

    @Column(name = "guarantor_id", nullable = false, length = 36)
    @ApiModelProperty(value = "担保人ID", required = true)
    private String guarantorId;

    @Column(name = "guarantor_name", nullable = false, length = 200)
    @ApiModelProperty(value = "担保人名称", required = true)
    private String guarantorName;

    @Column(name = "guarantor_address", length = 42)
    @ApiModelProperty(value = "担保人区块链地址")
    private String guarantorAddress;

    // ==================== 担保信息 ====================

    @Column(name = "guarantee_type", nullable = false, length = 20)
    @ApiModelProperty(value = "担保类型", required = true, notes = "FULL-全额担保, PARTIAL-部分担保, JOINT-联合担保")
    private String guaranteeType;

    @Column(name = "guarantee_amount", nullable = false, precision = 20, scale = 2)
    @ApiModelProperty(value = "担保金额", required = true)
    private BigDecimal guaranteeAmount;

    @Column(name = "guarantee_rate", precision = 10, scale = 6)
    @ApiModelProperty(value = "担保费率（%）", example = "2.500000")
    private BigDecimal guaranteeRate;

    @Column(name = "guarantee_fee", precision = 20, scale = 2)
    @ApiModelProperty(value = "担保费用", example = "25000.00")
    private BigDecimal guaranteeFee;

    @Column(name = "guarantee_period")
    @ApiModelProperty(value = "担保期限（天）", example = "90")
    private Integer guaranteePeriod;

    @Column(name = "guarantee_start_date")
    @ApiModelProperty(value = "担保开始日期")
    private LocalDateTime guaranteeStartDate;

    @Column(name = "guarantee_end_date")
    @ApiModelProperty(value = "担保结束日期")
    private LocalDateTime guaranteeEndDate;

    // ==================== 风险评估 ====================

    @Column(name = "risk_level", length = 20)
    @ApiModelProperty(value = "风险等级", notes = "LOW-低, MEDIUM-中, HIGH-高")
    private String riskLevel;

    @Column(name = "credit_score")
    @ApiModelProperty(value = "信用评分（0-100）", example = "75")
    private Integer creditScore;

    @Column(name = "risk_assessment", columnDefinition = "TEXT")
    @ApiModelProperty(value = "风险评估详情JSON")
    private String riskAssessment;

    // ==================== 担保条件 ====================

    @Column(name = "guarantee_conditions", columnDefinition = "TEXT")
    @ApiModelProperty(value = "担保条件")
    private String guaranteeConditions;

    @Column(name = "collateral_info", columnDefinition = "TEXT")
    @ApiModelProperty(value = "反担保措施JSON")
    private String collateralInfo;

    // ==================== 状态信息 ====================

    @Column(name = "status", nullable = false, length = 20)
    @ApiModelProperty(value = "状态", required = true, notes = "ACTIVE-有效, EXPIRED-已过期, CLAIMED-已索赔, CANCELLED-已取消")
    private String status;

    @Column(name = "claim_amount", precision = 20, scale = 2)
    @ApiModelProperty(value = "已索赔金额")
    private BigDecimal claimAmount;

    @Column(name = "claim_date")
    @ApiModelProperty(value = "索赔日期")
    private LocalDateTime claimDate;

    // ==================== 区块链信息 ====================

    @Column(name = "tx_hash", length = 100)
    @ApiModelProperty(value = "区块链交易哈希")
    private String txHash;

    // ==================== 审计信息 ====================

    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(6)")
    @ApiModelProperty(value = "创建时间", required = true)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6)")
    @ApiModelProperty(value = "更新时间", required = true)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 36)
    @ApiModelProperty(value = "创建人ID")
    private String createdBy;

    @Column(name = "remarks", length = 500)
    @ApiModelProperty(value = "备注")
    private String remarks;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = "ACTIVE";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 担保类型枚举
     */
    public enum GuaranteeType {
        FULL,     // 全额担保
        PARTIAL,  // 部分担保
        JOINT     // 联合担保
    }

    /**
     * 风险等级枚举
     */
    public enum RiskLevel {
        LOW,      // 低风险
        MEDIUM,   // 中等风险
        HIGH      // 高风险
    }

    /**
     * 担保状态枚举
     */
    public enum GuaranteeStatus {
        ACTIVE,     // 有效
        EXPIRED,    // 已过期
        CLAIMED,    // 已索赔
        CANCELLED   // 已取消
    }
}
