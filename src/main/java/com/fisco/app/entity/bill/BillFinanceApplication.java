package com.fisco.app.entity.bill;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 票据融资申请实体类
 * 记录票据融资申请的完整信息
 */
@Data
@Entity
@Table(name = "bill_finance_application", indexes = {
    @Index(name = "idx_finance_bill_id", columnList = "bill_id"),
    @Index(name = "idx_finance_applicant", columnList = "applicant_id"),
    @Index(name = "idx_finance_institution", columnList = "financial_institution_id"),
    @Index(name = "idx_finance_status", columnList = "status"),
    @Index(name = "idx_finance_apply_date", columnList = "apply_date")
})
@ApiModel(value = "票据融资申请", description = "票据融资申请实体")
public class BillFinanceApplication {

    @ApiModelProperty(value = "融资申请ID（UUID格式）", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
    @Id
    @Column(name = "id", nullable = false, unique = true, length = 36)
    private String id;

    @ApiModelProperty(value = "票据ID", required = true, example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    @Column(name = "bill_id", nullable = false, length = 36)
    private String billId;

    @ApiModelProperty(value = "申请人ID（票据持有人）", required = true)
    @Column(name = "applicant_id", nullable = false, length = 36)
    private String applicantId;

    @ApiModelProperty(value = "金融机构ID", required = true)
    @Column(name = "financial_institution_id", nullable = false, length = 36)
    private String financialInstitutionId;

    @ApiModelProperty(value = "融资金额（元）", required = true, example = "1000000.00")
    @Column(name = "finance_amount", precision = 20, scale = 2, nullable = false)
    private BigDecimal financeAmount;

    @ApiModelProperty(value = "融资利率（%）", required = true, example = "5.5")
    @Column(name = "finance_rate", precision = 10, scale = 6, nullable = false)
    private BigDecimal financeRate;

    @ApiModelProperty(value = "融资期限（天）", required = true, example = "90")
    @Column(name = "finance_period", nullable = false)
    private Integer financePeriod;

    @ApiModelProperty(value = "质押协议内容", example = "质押协议条款...")
    @Column(name = "pledge_agreement", columnDefinition = "TEXT")
    private String pledgeAgreement;

    @ApiModelProperty(value = "申请状态", notes = "PENDING-待审核, APPROVED-已批准, REJECTED-已拒绝, ACTIVE-融资中, REPAID-已还款, CANCELLED-已取消")
    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @ApiModelProperty(value = "批准金额", example = "950000.00")
    @Column(name = "approved_amount", precision = 20, scale = 2)
    private BigDecimal approvedAmount;

    @ApiModelProperty(value = "批准利率", example = "5.5")
    @Column(name = "approved_rate", precision = 10, scale = 6)
    private BigDecimal approvedRate;

    @ApiModelProperty(value = "实际放款金额", example = "950000.00")
    @Column(name = "actual_amount", precision = 20, scale = 2)
    private BigDecimal actualAmount;

    @ApiModelProperty(value = "放款日期")
    @Column(name = "disbursement_date", columnDefinition = "DATETIME(6)")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime disbursementDate;

    @ApiModelProperty(value = "申请日期")
    @Column(name = "apply_date", nullable = false, columnDefinition = "DATETIME(6)")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime applyDate;

    @ApiModelProperty(value = "审核日期")
    @Column(name = "approve_date", columnDefinition = "DATETIME(6)")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approveDate;

    @ApiModelProperty(value = "审核意见", example = "审核通过")
    @Column(name = "approval_comments", length = 500)
    private String approvalComments;

    @ApiModelProperty(value = "拒绝原因", example = "票据信用等级不足")
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @ApiModelProperty(value = "还款日期")
    @Column(name = "repayment_date", columnDefinition = "DATETIME(6)")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime repaymentDate;

    @ApiModelProperty(value = "实际还款金额")
    @Column(name = "actual_repayment_amount", precision = 20, scale = 2)
    private BigDecimal actualRepaymentAmount;

    @ApiModelProperty(value = "区块链交易哈希")
    @Column(name = "tx_hash", length = 100)
    private String txHash;

    @ApiModelProperty(value = "创建人ID")
    @Column(name = "created_by", length = 36)
    private String createdBy;

    @ApiModelProperty(value = "更新人ID")
    @Column(name = "updated_by", length = 36)
    private String updatedBy;

    @ApiModelProperty(value = "创建时间")
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @ApiModelProperty(value = "更新时间")
    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6)")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    // ==================== 生命周期回调 ====================

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (id == null || id.isEmpty()) {
            id = java.util.UUID.randomUUID().toString();
        }
        if (applyDate == null) {
            applyDate = now;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ==================== 枚举定义 ====================

    /**
     * 融资申请状态枚举
     */
    public enum FinanceStatus {
        PENDING,    // 待审核
        APPROVED,   // 已批准
        REJECTED,   // 已拒绝
        ACTIVE,     // 融资中（已放款）
        REPAID,      // 已还款
        CANCELLED    // 已取消
    }
}
