package com.fisco.app.entity.pledge;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import javax.persistence.*;

/**
 * 仓单释放记录实体类
 * 记录仓单质押释放的操作历史
 */
@Data
@Entity
@Table(name = "release_record", indexes = {
    @Index(name = "idx_release_receipt_id", columnList = "receipt_id"),
    @Index(name = "idx_release_owner", columnList = "owner_address"),
    @Index(name = "idx_release_institution", columnList = "financial_institution_address"),
    @Index(name = "idx_release_date", columnList = "release_date")
})
@ApiModel(value = "仓单释放记录", description = "仓单质押释放操作记录实体")
@Schema(name = "仓单释放记录")
public class ReleaseRecord {

    @Id
    @Column(name = "id", nullable = false, unique = true, length = 36)
    @ApiModelProperty(value = "释放记录ID（UUID格式）", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
    private String id;

    @Column(name = "receipt_id", nullable = false, length = 36)
    @ApiModelProperty(value = "仓单ID（UUID格式）", required = true, example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String receiptId;

    /**
     * 仓单所有者地址
     */
    @Column(name = "owner_address", nullable = false, length = 42)
    @ApiModelProperty(value = "仓单所有者地址", required = true, example = "0x1234567890abcdef1234567890abcdef12345678")
    private String ownerAddress;

    /**
     * 金融机构地址
     */
    @Column(name = "financial_institution_address", length = 42)
    @ApiModelProperty(value = "金融机构地址", example = "0x9876543210987654321098765432109876543210")
    private String financialInstitutionAddress;

    /**
     * 质押金额
     */
    @Column(name = "pledge_amount", precision = 20, scale = 2)
    @ApiModelProperty(value = "质押金额", example = "400000.00")
    private BigDecimal pledgeAmount;

    /**
     * 融资金额
     */
    @Column(name = "finance_amount", precision = 20, scale = 2)
    @ApiModelProperty(value = "融资金额", example = "350000.00")
    private BigDecimal financeAmount;

    /**
     * 融资利率
     */
    @Column(name = "finance_rate")
    @ApiModelProperty(value = "融资利率（基点）", example = "500", notes = "500表示5%")
    private Integer financeRate;

    /**
     * 融资日期
     */
    @Column(name = "finance_date")
    @ApiModelProperty(value = "融资日期", example = "2024-02-01T00:00:00")
    private LocalDateTime financeDate;

    /**
     * 释放日期
     */
    @Column(name = "release_date", nullable = false)
    @ApiModelProperty(value = "释放日期", required = true, example = "2024-06-30T00:00:00")
    private LocalDateTime releaseDate;

    /**
     * 释放类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "release_type", nullable = false, length = 20)
    @ApiModelProperty(value = "释放类型", required = true, example = "FULL_REPAYMENT", notes = "FULL_REPAYMENT-全额还款释放, PARTIAL_REPAYMENT-部分还款释放, MATURITY-到期释放, MANUAL-手动释放, LIQUIDATION-清算释放")
    private ReleaseType releaseType;

    /**
     * 还款金额
     */
    @Column(name = "repayment_amount", precision = 20, scale = 2)
    @ApiModelProperty(value = "还款金额", example = "355000.00")
    private BigDecimal repaymentAmount;

    /**
     * 利息金额
     */
    @Column(name = "interest_amount", precision = 20, scale = 2)
    @ApiModelProperty(value = "利息金额", example = "5000.00")
    private BigDecimal interestAmount;

    /**
     * 释放备注
     */
    @Column(name = "remark", columnDefinition = "TEXT")
    @ApiModelProperty(value = "释放备注", example = "全额还款后释放仓单")
    private String remark;

    /**
     * 区块链交易哈希
     */
    @Column(name = "tx_hash", length = 66)
    @ApiModelProperty(value = "区块链交易哈希", example = "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890")
    private String txHash;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @ApiModelProperty(value = "创建时间", required = true, example = "2024-06-30T10:00:00")
    private LocalDateTime createdAt;

    /**
     * 释放类型枚举
     */
    public enum ReleaseType {
        FULL_REPAYMENT,      // 全额还款释放
        PARTIAL_REPAYMENT,   // 部分还款释放
        MATURITY,            // 到期释放
        MANUAL,              // 手动释放
        LIQUIDATION          // 清算释放
    }

    @PrePersist
    protected void onCreate() {
        if (id == null || id.isEmpty()) {
            id = UUID.randomUUID().toString();
        }
        createdAt = LocalDateTime.now();
        if (releaseDate == null) {
            releaseDate = LocalDateTime.now();
        }
    }
}
