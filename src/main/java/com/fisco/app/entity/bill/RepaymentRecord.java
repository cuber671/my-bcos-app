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
 * 票据还款记录实体类
 * 记录贴现票据的还款操作历史
 */
@Data
@Entity
@Table(name = "repayment_record", indexes = {
    @Index(name = "idx_repayment_bill_id", columnList = "bill_id"),
    @Index(name = "idx_repayment_payer", columnList = "payer_address"),
    @Index(name = "idx_repayment_institution", columnList = "financial_institution_address"),
    @Index(name = "idx_repayment_date", columnList = "payment_date")
})
@ApiModel(value = "还款记录", description = "票据还款操作记录实体")
@Schema(name = "还款记录")
public class RepaymentRecord {

    @Id
    @Column(name = "id", nullable = false, unique = true, length = 36)
    @ApiModelProperty(value = "还款记录ID（UUID格式）", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
    private String id;

    @Column(name = "bill_id", nullable = false, length = 36)
    @ApiModelProperty(value = "票据ID（UUID格式）", required = true, example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String billId;

    /**
     * 还款人地址（票据承兑人/付款人）
     */
    @Column(name = "payer_address", nullable = false, length = 42)
    @ApiModelProperty(value = "还款人地址", required = true, example = "0x1234567890abcdef1234567890abcdef12345678")
    private String payerAddress;

    /**
     * 金融机构地址（贴现方）
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
     * 贴现金额
     */
    @Column(name = "discount_amount", precision = 20, scale = 2)
    @ApiModelProperty(value = "贴现金额", example = "95000.00")
    private BigDecimal discountAmount;

    /**
     * 还款金额
     */
    @Column(name = "payment_amount", nullable = false, precision = 20, scale = 2)
    @ApiModelProperty(value = "还款金额", required = true, example = "98000.00")
    private BigDecimal paymentAmount;

    /**
     * 还款类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 20)
    @ApiModelProperty(value = "还款类型", required = true, example = "MATURITY_PAYMENT", notes = "FULL_PAYMENT-全额还款, PARTIAL_PAYMENT-部分还款, MATURITY_PAYMENT-到期还款, EARLY_PAYMENT-提前还款, OVERDUE_PAYMENT-逾期还款")
    private PaymentType paymentType;

    /**
     * 正常还款金额
     */
    @Column(name = "principal_amount", precision = 20, scale = 2)
    @ApiModelProperty(value = "正常还款金额", example = "95000.00")
    private BigDecimal principalAmount;

    /**
     * 利息金额
     */
    @Column(name = "interest_amount", precision = 20, scale = 2)
    @ApiModelProperty(value = "利息金额", example = "3000.00")
    private BigDecimal interestAmount;

    /**
     * 逾期利息
     */
    @Column(name = "penalty_interest_amount", precision = 20, scale = 2)
    @ApiModelProperty(value = "逾期利息", example = "0.00")
    private BigDecimal penaltyInterestAmount;

    /**
     * 逾期天数
     */
    @Column(name = "overdue_days")
    @ApiModelProperty(value = "逾期天数", example = "0")
    private Integer overdueDays;

    /**
     * 还款日期
     */
    @Column(name = "payment_date", nullable = false)
    @ApiModelProperty(value = "还款日期", required = true, example = "2024-06-30T10:00:00")
    private LocalDateTime paymentDate;

    /**
     * 到期日期
     */
    @Column(name = "due_date", nullable = false)
    @ApiModelProperty(value = "到期日期", required = true, example = "2024-06-30T00:00:00")
    private LocalDateTime dueDate;

    /**
     * 还款状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @ApiModelProperty(value = "还款状态", required = true, example = "COMPLETED", notes = "PENDING-待处理, COMPLETED-已完成, FAILED-已失败, CANCELLED-已取消")
    private PaymentStatus status = PaymentStatus.COMPLETED;

    /**
     * 还款备注
     */
    @Column(name = "remark", columnDefinition = "TEXT")
    @ApiModelProperty(value = "还款备注", example = "到期自动还款")
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
     * 还款类型枚举
     */
    public enum PaymentType {
        FULL_PAYMENT,        // 全额还款
        PARTIAL_PAYMENT,     // 部分还款
        MATURITY_PAYMENT,    // 到期还款
        EARLY_PAYMENT,       // 提前还款
        OVERDUE_PAYMENT      // 逾期还款
    }

    /**
     * 还款状态枚举
     */
    public enum PaymentStatus {
        PENDING,     // 待处理
        COMPLETED,   // 已完成
        FAILED,      // 已失败
        CANCELLED    // 已取消
    }

    @PrePersist
    protected void onCreate() {
        if (id == null || id.isEmpty()) {
            id = UUID.randomUUID().toString();
        }
        createdAt = LocalDateTime.now();
        if (paymentDate == null) {
            paymentDate = LocalDateTime.now();
        }

        // 如果是逾期还款，计算逾期天数
        if (dueDate != null && paymentDate != null && paymentDate.isAfter(dueDate)) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(dueDate, paymentDate);
            overdueDays = (int) days;
        }
    }
}
