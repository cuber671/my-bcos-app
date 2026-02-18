package com.fisco.app.entity.risk;

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
 * 仓单融资记录实体类
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ewr_financing_record", indexes = {
    @Index(name = "idx_receipt_id", columnList = "receipt_id"),
    @Index(name = "idx_endorsement_id", columnList = "endorsement_id"),
    @Index(name = "idx_financial_institution_id", columnList = "financial_institution_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_financing_time", columnList = "financing_time")
})
@ApiModel(value = "仓单融资记录", description = "仓单质押融资的记录")
public class FinancingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(value = "融资记录ID", example = "1")
    private Long id;

    @Column(name = "financing_no", unique = true, length = 64)
    @ApiModelProperty(value = "融资编号", example = "FIN202601270001")
    private String financingNo;

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

    @Column(name = "financing_amount", nullable = false, precision = 19, scale = 2)
    @ApiModelProperty(value = "融资金额（元）", required = true, example = "100000.00")
    private BigDecimal financingAmount;

    @Column(name = "principal_amount", precision = 19, scale = 2)
    @ApiModelProperty(value = "本金金额", example = "100000.00")
    private BigDecimal principalAmount;

    @Column(name = "interest_rate", precision = 5, scale = 2)
    @ApiModelProperty(value = "年化利率（%）", example = "5.50")
    private BigDecimal interestRate;

    @Column(name = "total_interest", precision = 19, scale = 2)
    @ApiModelProperty(value = "总利息", example = "1375.00")
    private BigDecimal totalInterest;

    @Column(name = "repayment_amount", precision = 19, scale = 2)
    @ApiModelProperty(value = "应还金额（本金+利息）", example = "101375.00")
    private BigDecimal repaymentAmount;

    @Column(name = "financing_date", nullable = false)
    @ApiModelProperty(value = "融资日期", required = true)
    private LocalDate financingDate;

    @Column(name = "due_date")
    @ApiModelProperty(value = "到期日期", example = "2026-04-27")
    private LocalDate dueDate;

    @Column(name = "actual_repayment_date")
    @ApiModelProperty(value = "实际还款日期")
    private LocalDate actualRepaymentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @ApiModelProperty(value = "融资状态", required = true, notes = "ACTIVE-融资中, PAID_OFF-已还清, OVERDUE-已逾期, LIQUIDATED-已清算")
    @lombok.Builder.Default
    private FinancingStatus status = FinancingStatus.ACTIVE;

    @Column(name = "financing_time", nullable = false)
    @ApiModelProperty(value = "放款时间", required = true)
    private LocalDateTime financingTime;

    @Column(name = "repayment_time")
    @ApiModelProperty(value = "还款时间")
    private LocalDateTime repaymentTime;

    @Column(name = "overdue_days")
    @ApiModelProperty(value = "逾期天数", example = "10")
    private Integer overdueDays;

    @Column(name = "overdue_interest", precision = 19, scale = 2)
    @ApiModelProperty(value = "逾期利息", example = "50.00")
    private BigDecimal overdueInterest;

    @Column(name = "late_fee", precision = 19, scale = 2)
    @ApiModelProperty(value = "滞纳金", example = "100.00")
    private BigDecimal lateFee;

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
     * 融资状态枚举
     */
    public enum FinancingStatus {
        ACTIVE,      // 融资中
        PAID_OFF,    // 已还清
        OVERDUE,     // 已逾期
        LIQUIDATED   // 已清算
    }
}
