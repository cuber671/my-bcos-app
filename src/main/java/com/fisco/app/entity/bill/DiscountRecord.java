package com.fisco.app.entity.bill;

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
 * 票据贴现记录实体类
 * 记录票据的贴现操作历史
 */
@Data
@Entity
@Table(name = "discount_record", indexes = {
    @Index(name = "idx_discount_bill_id", columnList = "bill_id"),
    @Index(name = "idx_discount_holder", columnList = "holder_address"),
    @Index(name = "idx_discount_institution", columnList = "financial_institution_address"),
    @Index(name = "idx_discount_date", columnList = "discount_date")
})
@ApiModel(value = "贴现记录", description = "票据贴现操作记录实体")
@Schema(name = "贴现记录")
public class DiscountRecord {

    @Id
    @Column(name = "id", nullable = false, unique = true, length = 36)
    @ApiModelProperty(value = "贴现记录ID（UUID格式）", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
    private String id;

    @Column(name = "bill_id", nullable = false, length = 36)
    @ApiModelProperty(value = "票据ID（UUID格式）", required = true, example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String billId;

    /**
     * 贴现申请人地址（当前持票人）
     */
    @Column(name = "holder_address", nullable = false, length = 42)
    @ApiModelProperty(value = "贴现申请人地址", required = true, example = "0x1234567890abcdef1234567890abcdef12345678")
    private String holderAddress;

    /**
     * 金融机构地址
     */
    @Column(name = "financial_institution_address", nullable = false, length = 42)
    @ApiModelProperty(value = "金融机构地址", required = true, example = "0x9876543210987654321098765432109876543210")
    private String financialInstitutionAddress;

    /**
     * 票据票面金额
     */
    @Column(name = "bill_amount", nullable = false, precision = 20, scale = 2)
    @ApiModelProperty(value = "票据票面金额", required = true, example = "100000.00")
    private BigDecimal billAmount;

    /**
     * 贴现金额（实际支付金额）
     */
    @Column(name = "discount_amount", nullable = false, precision = 20, scale = 2)
    @ApiModelProperty(value = "贴现金额（实际支付）", required = true, example = "95000.00")
    private BigDecimal discountAmount;

    /**
     * 贴现率（百分比，如 5.5 表示 5.5%）
     */
    @Column(name = "discount_rate", nullable = false, precision = 10, scale = 4)
    @ApiModelProperty(value = "贴现率（百分比）", required = true, example = "5.5", notes = "5.5表示5.5%")
    private BigDecimal discountRate;

    /**
     * 贴现利息
     */
    @Column(name = "discount_interest", precision = 20, scale = 2)
    @ApiModelProperty(value = "贴现利息", example = "5000.00")
    private BigDecimal discountInterest;

    /**
     * 贴现日期
     */
    @Column(name = "discount_date", nullable = false)
    @ApiModelProperty(value = "贴现日期", required = true, example = "2024-02-01T10:00:00")
    private LocalDateTime discountDate;

    /**
     * 到期日期
     */
    @Column(name = "maturity_date", nullable = false)
    @ApiModelProperty(value = "到期日期", required = true, example = "2024-06-30T00:00:00")
    private LocalDateTime maturityDate;

    /**
     * 贴现天数
     */
    @Column(name = "discount_days")
    @ApiModelProperty(value = "贴现天数", example = "150")
    private Integer discountDays;

    /**
     * 贴现备注
     */
    @Column(name = "remark", columnDefinition = "TEXT")
    @ApiModelProperty(value = "贴现备注", example = "提前贴现用于资金周转")
    private String remark;

    /**
     * 区块链交易哈希
     */
    @Column(name = "tx_hash", length = 66)
    @ApiModelProperty(value = "区块链交易哈希", example = "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890")
    private String txHash;

    /**
     * 贴现状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @ApiModelProperty(value = "贴现状态", required = true, example = "ACTIVE", notes = "ACTIVE-有效（未到期）, MATURED-已到期, REPAID-已还款, CANCELLED-已取消")
    private DiscountStatus status = DiscountStatus.ACTIVE;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @ApiModelProperty(value = "创建时间", required = true, example = "2024-02-01T10:00:00")
    private LocalDateTime createdAt;

    /**
     * 贴现状态枚举
     */
    public enum DiscountStatus {
        ACTIVE,      // 有效（未到期）
        MATURED,     // 已到期
        REPAID,      // 已还款
        CANCELLED    // 已取消
    }

    @PrePersist
    protected void onCreate() {
        if (id == null || id.isEmpty()) {
            id = UUID.randomUUID().toString();
        }
        createdAt = LocalDateTime.now();
        if (discountDate == null) {
            discountDate = LocalDateTime.now();
        }

        // 自动计算贴现利息
        if (billAmount != null && discountAmount != null && discountInterest == null) {
            discountInterest = billAmount.subtract(discountAmount);
        }

        // 自动计算贴现天数
        if (discountDate != null && maturityDate != null && discountDays == null) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(discountDate, maturityDate);
            discountDays = (int) days;
        }
    }
}
