package com.fisco.app.entity.bill;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.PrePersist;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 票据背书记录实体类
 *
 * 功能：记录票据的所有背书转让历史
 * - 转让背书：票据权利转让
 * - 质押背书：票据质押融资
 * - 委托收款背书：委托银行收款
 *
 * 背书连续性要求：
 * - 背书链必须完整连续
 * - 签章必须真实有效
 * - 记载必须清晰完整
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-02
 */
@Data
@Entity
@Table(name = "bill_endorsement", indexes = {
    @Index(name = "idx_bill_id", columnList = "bill_id"),
    @Index(name = "idx_bill_no", columnList = "bill_no"),
    @Index(name = "idx_endorser_id", columnList = "endorser_id"),
    @Index(name = "idx_endorsee_id", columnList = "endorsee_id"),
    @Index(name = "idx_endorsement_type", columnList = "endorsement_type"),
    @Index(name = "idx_endorsement_date", columnList = "endorsement_date")
})
@ApiModel(value = "票据背书记录", description = "票据背书转让记录实体")
public class BillEndorsement {

    // ==================== 主键 ====================

    @ApiModelProperty(value = "背书ID", example = "e1f2a3b4-c5d6-7890-abcd-ef1234567890")
    @Id
    @Column(name = "endorsement_id", length = 36)
    private String endorsementId;

    // ==================== 关联票据 ====================

    @ApiModelProperty(value = "票据ID")
    @Column(name = "bill_id", length = 36, nullable = false)
    private String billId;

    @ApiModelProperty(value = "票据编号", example = "BIL20260200000001")
    @Column(name = "bill_no", length = 50, nullable = false)
    private String billNo;

    // ==================== 背书人信息 ====================

    @ApiModelProperty(value = "背书人ID", notes = "当前持票人")
    @Column(name = "endorser_id", length = 36, nullable = false)
    private String endorserId;

    @ApiModelProperty(value = "背书人名称", example = "XX贸易公司")
    @Column(name = "endorser_name", length = 200, nullable = false)
    private String endorserName;

    @ApiModelProperty(value = "背书人区块链地址")
    @Column(name = "endorser_address", length = 42)
    private String endorserAddress;

    // ==================== 被背书人信息 ====================

    @ApiModelProperty(value = "被背书人ID", notes = "新持票人")
    @Column(name = "endorsee_id", length = 36, nullable = false)
    private String endorseeId;

    @ApiModelProperty(value = "被背书人名称", example = "YY供应商")
    @Column(name = "endorsee_name", length = 200, nullable = false)
    private String endorseeName;

    @ApiModelProperty(value = "被背书人区块链地址")
    @Column(name = "endorsee_address", length = 42)
    private String endorseeAddress;

    // ==================== 背书信息 ====================

    @ApiModelProperty(value = "背书类型", notes = "TRANSFER-转让, PLEDGE-质押, COLLECTION-委托收款")
    @Column(name = "endorsement_type", length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    private EndorsementType endorsementType;

    @ApiModelProperty(value = "背书原因", example = "货款支付")
    @Column(name = "endorsement_reason", length = 500)
    private String endorsementReason;

    @ApiModelProperty(value = "关联合同ID", example = "contract-uuid-001")
    @Column(name = "related_contract", length = 36)
    private String relatedContract;

    @ApiModelProperty(value = "背书日期")
    @Column(name = "endorsement_date", columnDefinition = "DATETIME(6)", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endorsementDate;

    // ==================== 仓单联动信息 ====================

    @ApiModelProperty(value = "关联仓单ID", notes = "如果背书涉及仓单交付")
    @Column(name = "related_receipt_id", length = 36)
    private String relatedReceiptId;

    @ApiModelProperty(value = "是否涉及仓单交付")
    @Column(name = "receipt_delivery")
    private Boolean receiptDelivery = false;

    // ==================== 区块链信息 ====================

    @ApiModelProperty(value = "区块链状态", notes = "NOT_ONCHAIN-未上链, PENDING-待上链, ONCHAIN-已上链, FAILED-上链失败")
    @Column(name = "blockchain_status", length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    private BlockchainStatus blockchainStatus = BlockchainStatus.NOT_ONCHAIN;

    @ApiModelProperty(value = "区块链交易哈希")
    @Column(name = "blockchain_tx_hash", length = 100)
    private String blockchainTxHash;

    @ApiModelProperty(value = "上链时间")
    @Column(name = "blockchain_time", columnDefinition = "DATETIME(6)")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime blockchainTime;

    // ==================== 审计信息 ====================

    @ApiModelProperty(value = "创建时间")
    @Column(name = "created_at", columnDefinition = "DATETIME(6)", nullable = false, updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @ApiModelProperty(value = "创建人ID")
    @Column(name = "created_by", length = 36)
    private String createdBy;

    @ApiModelProperty(value = "备注")
    @Column(name = "remarks", length = 500)
    private String remarks;

    // ==================== 生命周期回调 ====================

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.endorsementDate == null) {
            this.endorsementDate = LocalDateTime.now();
        }
    }

    // ==================== 枚举定义 ====================

    /**
     * 背书类型枚举
     */
    public enum EndorsementType {
        TRANSFER,   // 转让背书 - 票据权利转让
        PLEDGE,     // 质押背书 - 票据质押融资
        COLLECTION  // 委托收款背书 - 委托银行收款
    }

    /**
     * 区块链状态枚举
     */
    public enum BlockchainStatus {
        NOT_ONCHAIN,  // 未上链
        PENDING,      // 待上链
        ONCHAIN,      // 已上链
        FAILED        // 上链失败
    }
}
