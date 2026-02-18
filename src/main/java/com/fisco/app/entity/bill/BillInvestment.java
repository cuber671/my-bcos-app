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
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;

import javax.persistence.*;

/**
 * 票据投资实体类
 * 记录金融机构通过票据池投资票据的完整信息
 *
 * 功能：
 * 1. 票据投资记录
 * 2. 收益结算
 * 3. 投资状态跟踪
 * 4. 区块链集成
 *
 * 投资类型：
 * - DISCOUNT: 贴现投资（低于面值购买）
 * - FULL: 全额投资（按面值购买）
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-03
 */
@Data
@Entity
@Table(name = "bill_investment", indexes = {
    @Index(name = "idx_invest_bill_id", columnList = "bill_id"),
    @Index(name = "idx_invest_investor", columnList = "investor_id"),
    @Index(name = "idx_invest_status", columnList = "status"),
    @Index(name = "idx_invest_date", columnList = "investment_date"),
    @Index(name = "idx_invest_bill_status", columnList = "bill_id, status")
})
@ApiModel(value = "票据投资", description = "票据投资记录实体")
public class BillInvestment {

    // ==================== 主键 ====================

    @ApiModelProperty(value = "投资记录ID（UUID格式）", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
    @Id
    @Column(name = "id", nullable = false, unique = true, length = 36)
    private String id;

    // ==================== 关联票据信息 ====================

    @ApiModelProperty(value = "票据ID", required = true)
    @Column(name = "bill_id", nullable = false, length = 36)
    private String billId;

    @ApiModelProperty(value = "票据编号", required = true)
    @Column(name = "bill_no", nullable = false, length = 50)
    private String billNo;

    @ApiModelProperty(value = "票据面值", required = true)
    @Column(name = "bill_face_value", precision = 20, scale = 2, nullable = false)
    private BigDecimal billFaceValue;

    // ==================== 投资方信息 ====================

    @ApiModelProperty(value = "投资机构ID", required = true)
    @Column(name = "investor_id", nullable = false, length = 36)
    private String investorId;

    @ApiModelProperty(value = "投资机构名称", required = true)
    @Column(name = "investor_name", nullable = false, length = 200)
    private String investorName;

    @ApiModelProperty(value = "投资机构区块链地址")
    @Column(name = "investor_address", length = 42)
    private String investorAddress;

    // ==================== 原持票人信息 ====================

    @ApiModelProperty(value = "原持票人ID")
    @Column(name = "original_holder_id", length = 36)
    private String originalHolderId;

    @ApiModelProperty(value = "原持票人名称")
    @Column(name = "original_holder_name", length = 200)
    private String originalHolderName;

    @ApiModelProperty(value = "原持票人地址")
    @Column(name = "original_holder_address", length = 42)
    private String originalHolderAddress;

    // ==================== 投资详情 ====================

    @ApiModelProperty(value = "投资金额（实际支付金额）", required = true)
    @Column(name = "invest_amount", precision = 20, scale = 2, nullable = false)
    private BigDecimal investAmount;

    @ApiModelProperty(value = "投资利率（%）", required = true)
    @Column(name = "invest_rate", precision = 10, scale = 4, nullable = false)
    private BigDecimal investRate;

    @ApiModelProperty(value = "预期收益")
    @Column(name = "expected_return", precision = 20, scale = 2)
    private BigDecimal expectedReturn;

    @ApiModelProperty(value = "投资天数（票据剩余天数）")
    @Column(name = "investment_days")
    private Integer investmentDays;

    // ==================== 投资状态 ====================

    @ApiModelProperty(value = "投资状态", notes = "PENDING-待确认, CONFIRMED-已确认, COMPLETED-已完成, CANCELLED-已取消, FAILED-失败")
    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @ApiModelProperty(value = "投资日期")
    @Column(name = "investment_date", columnDefinition = "DATETIME(6)")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime investmentDate;

    @ApiModelProperty(value = "确认日期")
    @Column(name = "confirmation_date", columnDefinition = "DATETIME(6)")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime confirmationDate;

    @ApiModelProperty(value = "完成日期")
    @Column(name = "completion_date", columnDefinition = "DATETIME(6)")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completionDate;

    @ApiModelProperty(value = "撤销日期")
    @Column(name = "cancellation_date", columnDefinition = "DATETIME(6)")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime cancellationDate;

    // ==================== 收益结算 ====================

    @ApiModelProperty(value = "到期金额（票据面值）")
    @Column(name = "maturity_amount", precision = 20, scale = 2)
    private BigDecimal maturityAmount;

    @ApiModelProperty(value = "实际收益")
    @Column(name = "actual_return", precision = 20, scale = 2)
    private BigDecimal actualReturn;

    @ApiModelProperty(value = "结算日期")
    @Column(name = "settlement_date", columnDefinition = "DATETIME(6)")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime settlementDate;

    // ==================== 备注信息 ====================

    @ApiModelProperty(value = "投资备注")
    @Column(name = "investment_notes", columnDefinition = "TEXT")
    private String investmentNotes;

    @ApiModelProperty(value = "拒绝原因（如果被拒绝）")
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    // ==================== 区块链信息 ====================

    @ApiModelProperty(value = "关联的背书ID")
    @Column(name = "endorsement_id", length = 36)
    private String endorsementId;

    @ApiModelProperty(value = "区块链交易哈希")
    @Column(name = "tx_hash", length = 100)
    private String txHash;

    @ApiModelProperty(value = "区块链确认时间")
    @Column(name = "blockchain_time", columnDefinition = "DATETIME(6)")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime blockchainTime;

    // ==================== 审计字段 ====================

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
            id = UUID.randomUUID().toString();
        }
        if (status == null || status.isEmpty()) {
            status = "PENDING";
        }
        if (investmentDate == null) {
            investmentDate = now;
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

    // ==================== 投资状态枚举 ====================

    /**
     * 投资状态枚举
     */
    public enum InvestmentStatus {
        PENDING,      // 待确认
        CONFIRMED,    // 已确认
        COMPLETED,    // 已完成
        CANCELLED,    // 已取消
        FAILED        // 失败
    }
}
