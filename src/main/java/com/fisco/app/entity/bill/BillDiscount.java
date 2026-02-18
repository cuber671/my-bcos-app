package com.fisco.app.entity.bill;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;


import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import com.fasterxml.jackson.annotation.JsonFormat;

import javax.persistence.*;

/**
 * 票据贴现记录实体类
 *
 * 功能：记录票据贴现融资的完整流程
 * - 贴现申请
 * - 贴现审核
 * - 贴现计算
 * - 资金支付
 * - 票据权利转移
 *
 * 贴现计算公式：
 * 贴现利息 = 面值 × 贴现率 × 剩余天数 / 360
 * 实付金额 = 面值 - 贴现利息
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-02
 */
@Data
@Entity
@Table(name = "bill_discount", indexes = {
    @Index(name = "idx_bill_id", columnList = "bill_id"),
    @Index(name = "idx_application_status", columnList = "application_status"),
    @Index(name = "idx_discount_institution_id", columnList = "discount_institution_id"),
    @Index(name = "idx_applicant_id", columnList = "applicant_id"),
    @Index(name = "idx_application_date", columnList = "application_date")
})
@ApiModel(value = "票据贴现记录", description = "票据贴现融资记录实体")
public class BillDiscount {

    // ==================== 主键 ====================

    @ApiModelProperty(value = "贴现ID", example = "d1a2b3c4-d5e6-7890-abcd-ef1234567890")
    @Id
    @Column(name = "discount_id", length = 36)
    private String discountId;

    // ==================== 关联票据 ====================

    @ApiModelProperty(value = "票据ID")
    @Column(name = "bill_id", length = 36, nullable = false)
    private String billId;

    @ApiModelProperty(value = "票据编号", example = "BIL20260200000001")
    @Column(name = "bill_no", length = 50, nullable = false)
    private String billNo;

    @ApiModelProperty(value = "票据类型", notes = "BANK_ACCEPTANCE_BILL-银行承兑汇票, COMMERCIAL_ACCEPTANCE_BILL-商业承兑汇票, BANK_NOTE-银行本票")
    @Column(name = "bill_type", length = 50, nullable = false)
    private String billType;

    @ApiModelProperty(value = "票面金额", example = "1000000.00")
    @Column(name = "face_value", precision = 20, scale = 2, nullable = false)
    private BigDecimal faceValue;

    // ==================== 贴现信息 ====================

    @ApiModelProperty(value = "贴现率（%）", example = "4.500000")
    @Column(name = "discount_rate", precision = 10, scale = 6, nullable = false)
    private BigDecimal discountRate;

    @ApiModelProperty(value = "贴现期限（天）", example = "90")
    @Column(name = "discount_period", nullable = false)
    private Integer discountPeriod;

    @ApiModelProperty(value = "贴现利息", example = "11250.00")
    @Column(name = "discount_interest", precision = 20, scale = 2, nullable = false)
    private BigDecimal discountInterest;

    @ApiModelProperty(value = "实付金额", notes = "票面金额 - 贴现利息", example = "988750.00")
    @Column(name = "net_amount", precision = 20, scale = 2, nullable = false)
    private BigDecimal netAmount;

    // ==================== 贴现机构 ====================

    @ApiModelProperty(value = "贴现机构ID")
    @Column(name = "discount_institution_id", length = 36, nullable = false)
    private String discountInstitutionId;

    @ApiModelProperty(value = "贴现机构名称", example = "XX银行")
    @Column(name = "discount_institution_name", length = 200, nullable = false)
    private String discountInstitutionName;

    // ==================== 申请人信息 ====================

    @ApiModelProperty(value = "申请人ID", notes = "持票人")
    @Column(name = "applicant_id", length = 36, nullable = false)
    private String applicantId;

    @ApiModelProperty(value = "申请人名称", example = "XX贸易公司")
    @Column(name = "applicant_name", length = 200, nullable = false)
    private String applicantName;

    @ApiModelProperty(value = "申请人区块链地址")
    @Column(name = "applicant_address", length = 42)
    private String applicantAddress;

    // ==================== 申请信息 ====================

    @ApiModelProperty(value = "贴现用途", example = "流动资金需求")
    @Column(name = "application_purpose", length = 500)
    private String applicationPurpose;

    @ApiModelProperty(value = "申请日期")
    @Column(name = "application_date", columnDefinition = "DATETIME(6)", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime applicationDate;

    @ApiModelProperty(value = "批准日期")
    @Column(name = "approval_date", columnDefinition = "DATETIME(6)")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approvalDate;

    @ApiModelProperty(value = "付款日期")
    @Column(name = "payment_date", columnDefinition = "DATETIME(6)")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime paymentDate;

    // ==================== 状态 ====================

    @ApiModelProperty(value = "申请状态",
            notes = "PENDING-待审核, APPROVED-已批准, REJECTED-已拒绝, PAID-已付款")
    @Column(name = "application_status", length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    private ApplicationStatus applicationStatus = ApplicationStatus.PENDING;

    // ==================== 审核信息 ====================

    @ApiModelProperty(value = "审核人ID")
    @Column(name = "reviewer_id", length = 36)
    private String reviewerId;

    @ApiModelProperty(value = "审核人名称", example = "张三")
    @Column(name = "reviewer_name", length = 200)
    private String reviewerName;

    @ApiModelProperty(value = "审核意见", example = "同意贴现，贴现率4.5%")
    @Column(name = "approval_comments", columnDefinition = "TEXT")
    private String approvalComments;

    // ==================== 资金信息 ====================

    @ApiModelProperty(value = "收款账号", example = "6222021234567890123")
    @Column(name = "payment_account", length = 100)
    private String paymentAccount;

    @ApiModelProperty(value = "付款凭证编号", example = "PAY-20260202-001")
    @Column(name = "payment_voucher", length = 100)
    private String paymentVoucher;

    @ApiModelProperty(value = "付款证明文件", example = "PROOF-20260202-001.pdf")
    @Column(name = "payment_proof", length = 100)
    private String paymentProof;

    // ==================== 审计信息 ====================

    @ApiModelProperty(value = "创建时间")
    @Column(name = "created_at", columnDefinition = "DATETIME(6)", nullable = false, updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @ApiModelProperty(value = "更新时间")
    @Column(name = "updated_at", columnDefinition = "DATETIME(6)", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @ApiModelProperty(value = "备注")
    @Column(name = "remarks", length = 500)
    private String remarks;

    // ==================== 生命周期回调 ====================

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.applicationDate == null) {
            this.applicationDate = now;
        }

        // 自动计算贴现利息
        if (this.faceValue != null && this.discountRate != null && this.discountPeriod != null
                && this.discountInterest == null) {
            // 贴现利息 = 面值 × 贴现率 × 天数 / 360
            this.discountInterest = this.faceValue
                    .multiply(this.discountRate)
                    .multiply(new BigDecimal(this.discountPeriod))
                    .divide(new BigDecimal(360), 2, java.math.RoundingMode.HALF_UP);
        }

        // 自动计算实付金额
        if (this.faceValue != null && this.discountInterest != null && this.netAmount == null) {
            this.netAmount = this.faceValue.subtract(this.discountInterest);
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ==================== 枚举定义 ====================

    /**
     * 申请状态枚举
     */
    public enum ApplicationStatus {
        PENDING,   // 待审核
        APPROVED,  // 已批准
        REJECTED,  // 已拒绝
        PAID       // 已付款
    }
}
