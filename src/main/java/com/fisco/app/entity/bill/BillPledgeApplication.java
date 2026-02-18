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
 * 票据质押融资申请实体类
 *
 * 功能：记录票据质押融资的完整申请流程
 * - 质押申请提交
 * - 风险评估
 * - 审核批准/拒绝
 * - 质押执行
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-02
 */
@Data
@Entity
@Table(name = "bill_pledge_application", indexes = {
    @Index(name = "idx_bill_id", columnList = "bill_id"),
    @Index(name = "idx_application_status", columnList = "application_status"),
    @Index(name = "idx_financial_institution_id", columnList = "financial_institution_id"),
    @Index(name = "idx_applicant_id", columnList = "applicant_id"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@ApiModel(value = "票据质押融资申请", description = "票据质押融资申请实体")
public class BillPledgeApplication {

    // ==================== 主键 ====================

    @ApiModelProperty(value = "申请ID", example = "b1a2b3c4-d5e6-7890-abcd-ef1234567890")
    @Id
    @Column(name = "application_id", length = 36)
    private String applicationId;

    // ==================== 关联票据信息 ====================

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

    // ==================== 质押信息 ====================

    @ApiModelProperty(value = "申请质押金额", example = "900000.00")
    @Column(name = "pledge_amount", precision = 20, scale = 2, nullable = false)
    private BigDecimal pledgeAmount;

    @ApiModelProperty(value = "质押期限（天）", example = "180")
    @Column(name = "pledge_period", nullable = false)
    private Integer pledgePeriod;

    @ApiModelProperty(value = "质押用途", example = "流动资金贷款")
    @Column(name = "pledge_purpose", length = 500)
    private String pledgePurpose;

    @ApiModelProperty(value = "金融机构ID")
    @Column(name = "financial_institution_id", length = 36, nullable = false)
    private String financialInstitutionId;

    @ApiModelProperty(value = "金融机构名称", example = "XX银行")
    @Column(name = "financial_institution_name", length = 200, nullable = false)
    private String financialInstitutionName;

    // ==================== 申请人信息 ====================

    @ApiModelProperty(value = "申请人ID")
    @Column(name = "applicant_id", length = 36, nullable = false)
    private String applicantId;

    @ApiModelProperty(value = "申请人名称", example = "XX贸易公司")
    @Column(name = "applicant_name", length = 200, nullable = false)
    private String applicantName;

    @ApiModelProperty(value = "申请人区块链地址")
    @Column(name = "applicant_address", length = 42)
    private String applicantAddress;

    // ==================== 额外担保物信息 ====================

    @ApiModelProperty(value = "额外担保物信息（JSON格式）", notes = "包括抵押物、质押物等")
    @Column(name = "collateral_info", columnDefinition = "TEXT")
    private String collateralInfo;

    @ApiModelProperty(value = "担保人ID")
    @Column(name = "guarantor_id", length = 36)
    private String guarantorId;

    @ApiModelProperty(value = "担保人名称", example = "XX担保公司")
    @Column(name = "guarantor_name", length = 200)
    private String guarantorName;

    // ==================== 风险评估 ====================

    @ApiModelProperty(value = "风险评估结果（JSON格式）", notes = "详细的风险分析数据")
    @Column(name = "risk_assessment", columnDefinition = "TEXT")
    private String riskAssessment;

    @ApiModelProperty(value = "信用评分", notes = "0-100分", example = "85")
    @Column(name = "credit_score")
    private Integer creditScore;

    @ApiModelProperty(value = "建议质押率", notes = "如0.85表示85%", example = "0.8500")
    @Column(name = "suggested_pledge_ratio", precision = 5, scale = 4)
    private BigDecimal suggestedPledgeRatio;

    @ApiModelProperty(value = "风险等级", notes = "LOW-低, MEDIUM-中, HIGH-高, CRITICAL-严重")
    @Column(name = "risk_level", length = 50)
    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;

    // ==================== 审核信息 ====================

    @ApiModelProperty(value = "申请状态", notes = "PENDING-待审核, APPROVED-已批准, REJECTED-已拒绝")
    @Column(name = "application_status", length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    private ApplicationStatus applicationStatus = ApplicationStatus.PENDING;

    @ApiModelProperty(value = "审核人ID")
    @Column(name = "reviewer_id", length = 36)
    private String reviewerId;

    @ApiModelProperty(value = "审核人名称", example = "张三")
    @Column(name = "reviewer_name", length = 200)
    private String reviewerName;

    @ApiModelProperty(value = "审核意见", example = "同意质押，质押率85%")
    @Column(name = "approval_comments", columnDefinition = "TEXT")
    private String approvalComments;

    @ApiModelProperty(value = "审核日期")
    @Column(name = "approval_date", columnDefinition = "DATETIME(6)")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approvalDate;

    // ==================== 审计信息 ====================

    @ApiModelProperty(value = "申请时间")
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
        REJECTED   // 已拒绝
    }

    /**
     * 风险等级枚举
     */
    public enum RiskLevel {
        LOW,       // 低风险
        MEDIUM,    // 中等风险
        HIGH,      // 高风险
        CRITICAL   // 严重风险
    }
}
