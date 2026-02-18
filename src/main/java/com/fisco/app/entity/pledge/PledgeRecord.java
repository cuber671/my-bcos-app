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
import java.time.LocalDateTime;


import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import javax.persistence.*;

/**
 * 仓单质押记录实体类
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ewr_pledge_record", indexes = {
    @Index(name = "idx_receipt_id", columnList = "receipt_id"),
    @Index(name = "idx_endorsement_id", columnList = "endorsement_id"),
    @Index(name = "idx_financial_institution_id", columnList = "financial_institution_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_pledge_time", columnList = "pledge_time")
})
@ApiModel(value = "仓单质押记录", description = "仓单质押的历史记录")
public class PledgeRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(value = "质押记录ID", example = "1")
    private Long id;

    @Column(name = "receipt_id", nullable = false, length = 36)
    @ApiModelProperty(value = "仓单ID", required = true)
    private String receiptId;

    @Column(name = "receipt_no", length = 64)
    @ApiModelProperty(value = "仓单编号", example = "EWR202601270001")
    private String receiptNo;

    @Column(name = "endorsement_id", length = 36)
    @ApiModelProperty(value = "背书ID（链接到ewr_endorsement_chain）", example = "b2c3d4e5-f6g7-8901-bcde-f23456789012")
    private String endorsementId;

    @Column(name = "endorsement_no", length = 64)
    @ApiModelProperty(value = "背书编号", example = "END20260126000001")
    private String endorsementNo;

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

    @Column(name = "previous_holder_address", length = 42)
    @ApiModelProperty(value = "质押前持有人地址（原货主）", example = "0xabcdef1234567890abcdef1234567890abcdef12")
    private String previousHolderAddress;

    @Column(name = "pledge_amount", nullable = false, precision = 19, scale = 2)
    @ApiModelProperty(value = "质押金额（元）", required = true, example = "100000.00")
    private BigDecimal pledgeAmount;

    @Column(name = "interest_rate", precision = 5, scale = 2)
    @ApiModelProperty(value = "年化利率（%）", example = "5.50")
    private BigDecimal interestRate;

    @Column(name = "pledge_start_date")
    @ApiModelProperty(value = "质押开始日期", example = "2026-01-27")
    private java.time.LocalDate pledgeStartDate;

    @Column(name = "pledge_end_date")
    @ApiModelProperty(value = "质押结束日期", example = "2026-04-27")
    private java.time.LocalDate pledgeEndDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @ApiModelProperty(value = "质押状态", required = true, notes = "ACTIVE-质押中, RELEASED-已释放, LIQUIDATED-已清算")
    @lombok.Builder.Default
    private PledgeStatus status = PledgeStatus.ACTIVE;

    @Column(name = "pledge_time", nullable = false)
    @ApiModelProperty(value = "质押时间", required = true)
    private LocalDateTime pledgeTime;

    @Column(name = "release_time")
    @ApiModelProperty(value = "释放时间")
    private LocalDateTime releaseTime;

    @Column(name = "liquidation_time")
    @ApiModelProperty(value = "清算时间")
    private LocalDateTime liquidationTime;

    @Column(name = "tx_hash", length = 128)
    @ApiModelProperty(value = "质押上链交易哈希")
    private String txHash;

    @Column(name = "block_number")
    @ApiModelProperty(value = "质押区块号")
    private Long blockNumber;

    @Column(name = "release_tx_hash", length = 128)
    @ApiModelProperty(value = "释放上链交易哈希")
    private String releaseTxHash;

    @Column(name = "release_block_number")
    @ApiModelProperty(value = "释放区块号")
    private Long releaseBlockNumber;

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
     * 质押状态枚举
     */
    public enum PledgeStatus {
        ACTIVE,      // 质押中
        RELEASED,    // 已释放
        LIQUIDATED   // 已清算
    }
}
