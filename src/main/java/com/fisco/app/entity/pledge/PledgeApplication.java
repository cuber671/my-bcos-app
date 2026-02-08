package com.fisco.app.entity.pledge;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;


import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import javax.persistence.*;

/**
 * 仓单质押申请实体类
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ewr_pledge_application", indexes = {
    @Index(name = "idx_receipt_id", columnList = "receipt_id"),
    @Index(name = "idx_owner_id", columnList = "owner_id"),
    @Index(name = "idx_financial_institution_id", columnList = "financial_institution_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_apply_time", columnList = "apply_time")
})
@ApiModel(value = "仓单质押申请", description = "货主发起的仓单质押申请记录")
public class PledgeApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(value = "质押申请ID", example = "1")
    private Long id;

    @Column(name = "application_no", unique = true, length = 64)
    @ApiModelProperty(value = "申请编号", required = true, example = "PLG202601270001")
    private String applicationNo;

    @Column(name = "receipt_id", nullable = false, length = 36)
    @ApiModelProperty(value = "仓单ID", required = true)
    private String receiptId;

    @Column(name = "receipt_no", length = 64)
    @ApiModelProperty(value = "仓单编号", example = "EWR202601270001")
    private String receiptNo;

    @Column(name = "owner_id", nullable = false, length = 36)
    @ApiModelProperty(value = "货主企业ID", required = true)
    private String ownerId;

    @Column(name = "owner_name", length = 128)
    @ApiModelProperty(value = "货主企业名称")
    private String ownerName;

    @Column(name = "financial_institution_id", nullable = false, length = 36)
    @ApiModelProperty(value = "金融机构ID", required = true)
    private String financialInstitutionId;

    @Column(name = "financial_institution_name", length = 128)
    @ApiModelProperty(value = "金融机构名称")
    private String financialInstitutionName;

    @Column(name = "financial_institution_address", length = 42)
    @ApiModelProperty(value = "金融机构区块链地址", example = "0x1234567890abcdef1234567890abcdef12345678")
    private String financialInstitutionAddress;

    @Column(name = "pledge_amount", nullable = false, precision = 19, scale = 2)
    @ApiModelProperty(value = "质押金额（元）", required = true, example = "100000.00")
    private BigDecimal pledgeAmount;

    @Column(name = "pledge_ratio", precision = 5, scale = 2)
    @ApiModelProperty(value = "质押率（0-1之间，如0.7表示70%）", example = "0.70")
    private BigDecimal pledgeRatio;

    @Column(name = "receipt_value", precision = 19, scale = 2)
    @ApiModelProperty(value = "仓单总价值", example = "150000.00")
    private BigDecimal receiptValue;

    @Column(name = "pledge_start_date")
    @ApiModelProperty(value = "质押开始日期", example = "2026-01-27")
    private LocalDate pledgeStartDate;

    @Column(name = "pledge_end_date")
    @ApiModelProperty(value = "质押结束日期", example = "2026-04-27")
    private LocalDate pledgeEndDate;

    @Column(name = "approved_amount", precision = 19, scale = 2)
    @ApiModelProperty(value = "实际批准金额（元）", example = "100000.00")
    private BigDecimal approvedAmount;

    @Column(name = "interest_rate", precision = 5, scale = 2)
    @ApiModelProperty(value = "年化利率（如5.5表示5.5%）", example = "5.50")
    private BigDecimal interestRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @ApiModelProperty(value = "申请状态", required = true, notes = "PENDING-待审核, APPROVED-已批准, REJECTED-已拒绝, RELEASED-已释放")
    @lombok.Builder.Default
    private ApplicationStatus status = ApplicationStatus.PENDING;

    @Column(name = "apply_time", nullable = false)
    @ApiModelProperty(value = "申请时间", required = true)
    private LocalDateTime applyTime;

    @Column(name = "approval_time")
    @ApiModelProperty(value = "审核时间")
    private LocalDateTime approvalTime;

    @Column(name = "approver_id", length = 36)
    @ApiModelProperty(value = "审核人ID")
    private String approverId;

    @Column(name = "approver_name", length = 64)
    @ApiModelProperty(value = "审核人姓名")
    private String approverName;

    @Column(name = "rejection_reason", length = 500)
    @ApiModelProperty(value = "拒绝原因")
    private String rejectionReason;

    @Column(name = "tx_hash", length = 128)
    @ApiModelProperty(value = "区块链交易哈希")
    private String txHash;

    @Column(name = "block_number")
    @ApiModelProperty(value = "区块号")
    private Long blockNumber;

    @Column(name = "blockchain_time")
    @ApiModelProperty(value = "上链时间")
    private LocalDateTime blockchainTime;

    @Column(name = "remark", length = 500)
    @ApiModelProperty(value = "备注")
    private String remark;

    @Column(name = "created_at", nullable = false, updatable = false)
    @ApiModelProperty(value = "创建时间")
    @lombok.Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @ApiModelProperty(value = "更新时间")
    @lombok.Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "deleted", nullable = false)
    @ApiModelProperty(value = "是否删除")
    @lombok.Builder.Default
    private Boolean deleted = false;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 申请状态枚举
     */
    public enum ApplicationStatus {
        PENDING,     // 待审核
        APPROVED,    // 已批准
        REJECTED,    // 已拒绝
        RELEASED     // 已释放
    }
}
