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
 * 票据结算记录实体类
 *
 * 功能：记录票据的多方债权债务清算
 * - 债务结算：两方债权债务抵消
 * - 多方结算：多方参与的债权债务清算
 * - 三角债结算：三方循环债务清算
 *
 * 结算场景：
 * 1. A欠B 100万，B欠C 100万，C欠A 100万
 * 2. 通过票据背书转让完成三方债务闭环
 * 3. 降低实际资金占用，提高资金效率
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-02
 */
@Data
@Entity
@Table(name = "bill_settlement", indexes = {
    @Index(name = "idx_bill_id", columnList = "bill_id"),
    @Index(name = "idx_settlement_type", columnList = "settlement_type"),
    @Index(name = "idx_settlement_status", columnList = "settlement_status"),
    @Index(name = "idx_settlement_date", columnList = "settlement_date"),
    @Index(name = "idx_initiator_id", columnList = "initiator_id")
})
@ApiModel(value = "票据结算记录", description = "票据债权债务清算记录实体")
public class BillSettlement {

    // ==================== 主键 ====================

    @ApiModelProperty(value = "结算ID", example = "s1a2b3c4-d5e6-7890-abcd-ef1234567890")
    @Id
    @Column(name = "settlement_id", length = 36)
    private String settlementId;

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

    // ==================== 结算信息 ====================

    @ApiModelProperty(value = "结算类型",
            notes = "DEBT_SETTLEMENT-债务结算, MULTILATERAL-多方结算, TRIANGULAR-三角债结算")
    @Column(name = "settlement_type", length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    private SettlementType settlementType;

    @ApiModelProperty(value = "结算金额", example = "1000000.00")
    @Column(name = "settlement_amount", precision = 20, scale = 2, nullable = false)
    private BigDecimal settlementAmount;

    @ApiModelProperty(value = "结算日期")
    @Column(name = "settlement_date", columnDefinition = "DATETIME(6)", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime settlementDate;

    // ==================== 参与方 ====================

    @ApiModelProperty(value = "发起人ID")
    @Column(name = "initiator_id", length = 36, nullable = false)
    private String initiatorId;

    @ApiModelProperty(value = "发起人名称", example = "XX贸易公司")
    @Column(name = "initiator_name", length = 200, nullable = false)
    private String initiatorName;

    @ApiModelProperty(value = "参与方信息（JSON格式）", notes = "包含所有参与方的ID、名称、债务金额等信息")
    @Column(name = "participants", columnDefinition = "TEXT", nullable = false)
    private String participants;

    // ==================== 债权债务关系 ====================

    @ApiModelProperty(value = "关联债务信息（JSON格式）", notes = "包含债务合同、金额、到期日等")
    @Column(name = "related_debts", columnDefinition = "TEXT", nullable = false)
    private String relatedDebts;

    @ApiModelProperty(value = "债务证明文件（JSON格式）", notes = "包含合同编号、证明文件路径等")
    @Column(name = "debt_proof_documents", columnDefinition = "TEXT")
    private String debtProofDocuments;

    // ==================== 仓单联动 ====================

    @ApiModelProperty(value = "关联仓单信息（JSON格式）", notes = "如果结算涉及仓单转让")
    @Column(name = "related_receipts", columnDefinition = "TEXT")
    private String relatedReceipts;

    @ApiModelProperty(value = "是否涉及仓单转让")
    @Column(name = "receipt_transfer")
    private Boolean receiptTransfer = false;

    // ==================== 结算结果 ====================

    @ApiModelProperty(value = "结算状态", notes = "PENDING-待结算, COMPLETED-已完成, FAILED-失败, PARTIAL-部分结算")
    @Column(name = "settlement_status", length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    private SettlementStatus settlementStatus = SettlementStatus.PENDING;

    @ApiModelProperty(value = "结算证明文件", example = "SETTLE-PROOF-20260202-001.pdf")
    @Column(name = "settlement_proof", length = 100)
    private String settlementProof;

    @ApiModelProperty(value = "完成日期")
    @Column(name = "completion_date", columnDefinition = "DATETIME(6)")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completionDate;

    // ==================== 审计信息 ====================

    @ApiModelProperty(value = "创建时间")
    @Column(name = "created_at", columnDefinition = "DATETIME(6)", nullable = false, updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @ApiModelProperty(value = "更新时间")
    @Column(name = "updated_at", columnDefinition = "DATETIME(6)", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @ApiModelProperty(value = "创建人ID")
    @Column(name = "created_by", length = 36)
    private String createdBy;

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
        if (this.settlementDate == null) {
            this.settlementDate = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ==================== 枚举定义 ====================

    /**
     * 结算类型枚举
     */
    public enum SettlementType {
        DEBT_SETTLEMENT,  // 债务结算 - 两方债权债务抵消
        MULTILATERAL,     // 多方结算 - 多方参与的债权债务清算
        TRIANGULAR        // 三角债结算 - 三方循环债务清算
    }

    /**
     * 结算状态枚举
     */
    public enum SettlementStatus {
        PENDING,    // 待结算
        COMPLETED,  // 已完成
        FAILED,     // 失败
        PARTIAL     // 部分结算
    }
}
