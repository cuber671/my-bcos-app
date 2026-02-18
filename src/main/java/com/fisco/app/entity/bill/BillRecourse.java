package com.fisco.app.entity.bill;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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
 * 票据追索记录实体类
 *
 * 功能：记录票据拒付后的追索流程
 * - 追索发起
 * - 前手通知
 * - 追索执行
 * - 追索完成/失败
 *
 * 追索流程：
 * 1. 票据到期被拒付
 * 2. 持票人取得拒付证明
 * 3. 通知前手（背书人）
 * 4. 行使追索权
 * 5. 前手付款后再向其前手追索
 *
 * 追索顺序：持票人 → 最后背书人 → ... → 第一背书人 → 出票人 → 承兑人
 * 追索时效：票据到期日起2年
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-02
 */
@Data
@Entity
@Table(name = "bill_recourse", indexes = {
    @Index(name = "idx_bill_id", columnList = "bill_id"),
    @Index(name = "idx_recourse_status", columnList = "recourse_status"),
    @Index(name = "idx_dishonored_date", columnList = "dishonored_date"),
    @Index(name = "idx_initiator_id", columnList = "initiator_id"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@ApiModel(value = "票据追索记录", description = "票据追索记录实体")
public class BillRecourse {

    // ==================== 主键 ====================

    @ApiModelProperty(value = "追索ID", example = "r1a2b3c4-d5e6-7890-abcd-ef1234567890")
    @Id
    @Column(name = "recourse_id", length = 36)
    private String recourseId;

    // ==================== 关联票据 ====================

    @ApiModelProperty(value = "票据ID")
    @Column(name = "bill_id", length = 36, nullable = false)
    private String billId;

    @ApiModelProperty(value = "票据编号", example = "BIL20260200000001")
    @Column(name = "bill_no", length = 50, nullable = false)
    private String billNo;

    @ApiModelProperty(value = "票面金额", example = "1000000.00")
    @Column(name = "face_value", precision = 20, scale = 2, nullable = false)
    private BigDecimal faceValue;

    // ==================== 拒付信息 ====================

    @ApiModelProperty(value = "拒付日期")
    @Column(name = "dishonored_date", columnDefinition = "DATETIME(6)", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dishonoredDate;

    @ApiModelProperty(value = "拒付原因", example = "承兑人账户余额不足")
    @Column(name = "dishonored_reason", columnDefinition = "TEXT", nullable = false)
    private String dishonoredReason;

    @ApiModelProperty(value = "拒付证明文件编号", example = "DISHONOR-20260202-001")
    @Column(name = "dishonored_proof", length = 100)
    private String dishonoredProof;

    @ApiModelProperty(value = "承兑人拒付原因", example = "账户冻结")
    @Column(name = "acceptor_dishonor_reason", length = 500)
    private String acceptorDishonorReason;

    // ==================== 追索信息 ====================

    @ApiModelProperty(value = "追索金额", example = "1000000.00")
    @Column(name = "recourse_amount", precision = 20, scale = 2, nullable = false)
    private BigDecimal recourseAmount;

    @ApiModelProperty(value = "罚息金额", example = "5000.00")
    @Column(name = "penalty_amount", precision = 20, scale = 2)
    private BigDecimal penaltyAmount;

    @ApiModelProperty(value = "费用金额", example = "1000.00")
    @Column(name = "expense_amount", precision = 20, scale = 2)
    private BigDecimal expenseAmount;

    @ApiModelProperty(value = "追索总额", notes = "追索金额 + 罚息 + 费用", example = "1006000.00")
    @Column(name = "total_recourse_amount", precision = 20, scale = 2)
    private BigDecimal totalRecourseAmount;

    // ==================== 追索状态 ====================

    @ApiModelProperty(value = "追索状态",
            notes = "INITIATED-已发起, IN_PROGRESS-进行中, COMPLETED-已完成, FAILED-失败, PARTIAL-部分追回")
    @Column(name = "recourse_status", length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    private RecourseStatus recourseStatus = RecourseStatus.INITIATED;

    // ==================== 追索通知 ====================

    @ApiModelProperty(value = "已通知的前手（JSON格式）", notes = "包含所有被通知的前手信息")
    @Column(name = "notified_parties", columnDefinition = "TEXT")
    private String notifiedParties;

    @ApiModelProperty(value = "通知日期")
    @Column(name = "notification_date", columnDefinition = "DATETIME(6)")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime notificationDate;

    @ApiModelProperty(value = "通知证明文件", example = "NOTICE-20260202-001")
    @Column(name = "notification_proof", length = 100)
    private String notificationProof;

    // ==================== 追索结果 ====================

    @ApiModelProperty(value = "追索结果详情（JSON格式）", notes = "包含每个前手的追索结果")
    @Column(name = "recourse_results", columnDefinition = "TEXT")
    private String recourseResults;

    @ApiModelProperty(value = "已追回金额", example = "1000000.00")
    @Column(name = "settled_amount", precision = 20, scale = 2)
    private BigDecimal settledAmount;

    @ApiModelProperty(value = "追回日期")
    @Column(name = "settlement_date", columnDefinition = "DATETIME(6)")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime settlementDate;

    @ApiModelProperty(value = "追回证明文件", example = "SETTLE-20260202-001")
    @Column(name = "settlement_proof", length = 100)
    private String settlementProof;

    // ==================== 法律诉讼 ====================

    @ApiModelProperty(value = "是否提起法律诉讼")
    @Column(name = "legal_action")
    private Boolean legalAction = false;

    @ApiModelProperty(value = "案件编号", example = "2026民初字第001号")
    @Column(name = "case_number", length = 100)
    private String caseNumber;

    @ApiModelProperty(value = "法院名称", example = "XX市中级人民法院")
    @Column(name = "court_name", length = 200)
    private String courtName;

    // ==================== 追索发起人 ====================

    @ApiModelProperty(value = "追索发起人ID", notes = "持票人")
    @Column(name = "initiator_id", length = 36, nullable = false)
    private String initiatorId;

    @ApiModelProperty(value = "追索发起人名称", example = "XX公司")
    @Column(name = "initiator_name", length = 200, nullable = false)
    private String initiatorName;

    // ==================== 审计信息 ====================

    @ApiModelProperty(value = "创建时间")
    @Column(name = "created_at", columnDefinition = "DATETIME(6)", nullable = false, updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @ApiModelProperty(value = "更新时间")
    @Column(name = "updated_at", columnDefinition = "DATETIME(6)", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @ApiModelProperty(value = "完成时间")
    @Column(name = "completed_at", columnDefinition = "DATETIME(6)")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completedAt;

    @ApiModelProperty(value = "备注")
    @Column(name = "remarks", length = 1000)
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

        // 自动计算追索总额
        if (this.totalRecourseAmount == null) {
            BigDecimal total = this.recourseAmount;
            if (this.penaltyAmount != null) {
                total = total.add(this.penaltyAmount);
            }
            if (this.expenseAmount != null) {
                total = total.add(this.expenseAmount);
            }
            this.totalRecourseAmount = total;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();

        // 重新计算追索总额
        if (this.recourseAmount != null) {
            BigDecimal total = this.recourseAmount;
            if (this.penaltyAmount != null) {
                total = total.add(this.penaltyAmount);
            }
            if (this.expenseAmount != null) {
                total = total.add(this.expenseAmount);
            }
            this.totalRecourseAmount = total;
        }
    }

    // ==================== 枚举定义 ====================

    /**
     * 追索状态枚举
     */
    public enum RecourseStatus {
        INITIATED,   // 已发起
        IN_PROGRESS, // 进行中
        COMPLETED,   // 已完成
        FAILED,      // 失败
        PARTIAL      // 部分追回
    }
}
