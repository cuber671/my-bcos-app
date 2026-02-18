package com.fisco.app.entity.receivable;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 应收账款还款记录实体类
 *
 * 记录应收账款的还款详情，支持部分还款、提前还款、逾期还款等场景
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-09
 * @version 1.0
 */
@Data
@Entity
@Table(name = "receivable_repayment_record", indexes = {
    @Index(name = "idx_receivable_id", columnList = "receivable_id"),
    @Index(name = "idx_payer", columnList = "payer_address"),
    @Index(name = "idx_receiver", columnList = "receiver_address"),
    @Index(name = "idx_payment_date", columnList = "payment_date"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_repayment_type", columnList = "repayment_type")
})
@ApiModel(value = "ReceivableRepaymentRecord", description = "应收账款还款记录")
public class ReceivableRepaymentRecord {

    @Id
    @Column(name = "id", length = 36)
    @ApiModelProperty(value = "记录ID（UUID）", example = "550e8400-e29b-41d4-a716-446655440000")
    private String id;

    @Column(name = "receivable_id", length = 36, nullable = false)
    @ApiModelProperty(value = "应收账款ID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
    private String receivableId;

    @Enumerated(EnumType.STRING)
    @Column(name = "repayment_type", length = 20, nullable = false)
    @ApiModelProperty(value = "还款类型", required = true, notes = "PARTIAL-部分还款, FULL-全额还款, EARLY-提前还款, OVERDUE-逾期还款", example = "PARTIAL")
    private RepaymentType repaymentType;

    @Column(name = "repayment_amount", precision = 20, scale = 2, nullable = false)
    @ApiModelProperty(value = "还款总金额", required = true, example = "500000.00")
    private BigDecimal repaymentAmount;

    @Column(name = "principal_amount", precision = 20, scale = 2, nullable = false)
    @ApiModelProperty(value = "本金金额", required = true, example = "500000.00")
    private BigDecimal principalAmount;

    @Column(name = "interest_amount", precision = 20, scale = 2)
    @ApiModelProperty(value = "利息金额", example = "5000.00")
    private BigDecimal interestAmount = BigDecimal.ZERO;

    @Column(name = "penalty_amount", precision = 20, scale = 2)
    @ApiModelProperty(value = "罚息金额", example = "1250.00")
    private BigDecimal penaltyAmount = BigDecimal.ZERO;

    @Column(name = "payer_address", length = 42, nullable = false)
    @ApiModelProperty(value = "还款人地址（核心企业）", required = true, example = "0xabcdef1234567890")
    private String payerAddress;

    @Column(name = "receiver_address", length = 42, nullable = false)
    @ApiModelProperty(value = "收款人地址（供应商或金融机构）", required = true, example = "0x1234567890abcdef")
    private String receiverAddress;

    @Column(name = "payment_date", nullable = false)
    @ApiModelProperty(value = "还款日期", required = true, example = "2026-02-09")
    private LocalDate paymentDate;

    @Column(name = "actual_payment_time", nullable = false)
    @ApiModelProperty(value = "实际还款时间", required = true, example = "2026-02-09T14:30:00")
    private LocalDateTime actualPaymentTime;

    @Column(name = "payment_method", length = 20)
    @ApiModelProperty(value = "支付方式", notes = "BANK-银行转账, ALIPAY-支付宝, WECHAT-微信, OTHER-其他", example = "BANK")
    private String paymentMethod;

    @Column(name = "payment_account", length = 100)
    @ApiModelProperty(value = "支付账号", example = "6222021234567890")
    private String paymentAccount;

    @Column(name = "transaction_no", length = 64)
    @ApiModelProperty(value = "交易流水号", example = "TXN202602091234567890")
    private String transactionNo;

    @Column(name = "voucher_url", length = 500)
    @ApiModelProperty(value = "凭证URL", example = "https://example.com/voucher/abc123.pdf")
    private String voucherUrl;

    @Column(name = "early_payment_days")
    @ApiModelProperty(value = "提前还款天数", example = "15")
    private Integer earlyPaymentDays;

    @Column(name = "overdue_days")
    @ApiModelProperty(value = "逾期天数", example = "30")
    private Integer overdueDays;

    @Column(name = "remark", columnDefinition = "TEXT")
    @ApiModelProperty(value = "备注", example = "第一期还款")
    private String remark;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @ApiModelProperty(value = "状态", required = true, notes = "PENDING-待确认, CONFIRMED-已确认, FAILED-失败", example = "CONFIRMED")
    private RepaymentStatus status = RepaymentStatus.PENDING;

    @Column(name = "tx_hash", length = 66)
    @ApiModelProperty(value = "区块链交易哈希", example = "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef12")
    private String txHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    @ApiModelProperty(value = "创建时间", hidden = true)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @ApiModelProperty(value = "更新时间", hidden = true)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 42)
    @ApiModelProperty(value = "创建人地址", example = "0xabcdef1234567890")
    private String createdBy;

    @Column(name = "updated_by", length = 42)
    @ApiModelProperty(value = "更新人地址", example = "0xabcdef1234567890")
    private String updatedBy;

    /**
     * 还款类型枚举
     */
    public enum RepaymentType {
        PARTIAL("部分还款"),
        FULL("全额还款"),
        EARLY("提前还款"),
        OVERDUE("逾期还款");

        private final String description;

        RepaymentType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 还款状态枚举
     */
    public enum RepaymentStatus {
        PENDING("待确认"),
        CONFIRMED("已确认"),
        FAILED("失败");

        private final String description;

        RepaymentStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    @PrePersist
    protected void onCreate() {
        if (id == null || id.isEmpty()) {
            id = UUID.randomUUID().toString();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
