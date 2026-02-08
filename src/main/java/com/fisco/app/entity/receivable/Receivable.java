package com.fisco.app.entity.receivable;

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
 * 应收账款实体类
 */
@Data
@Entity
@Table(name = "receivable", indexes = {
    @Index(name = "idx_supplier", columnList = "supplier_address"),
    @Index(name = "idx_core_enterprise", columnList = "core_enterprise_address"),
    @Index(name = "idx_holder", columnList = "current_holder"),
    @Index(name = "idx_financier", columnList = "financier_address"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_due_date", columnList = "due_date")
})
@ApiModel(value = "Receivable", description = "应收账款实体")
@Schema(name = "应收账款")
public class Receivable {

    @Id
    @Column(name = "id", nullable = false, unique = true, length = 36)
    @ApiModelProperty(value = "应收账款ID（UUID格式）", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
    private String id;

    @Column(name = "supplier_address", nullable = false, length = 42)
    @ApiModelProperty(value = "供应商地址", required = true, example = "0x1234567890abcdef")
    private String supplierAddress;

    @Column(name = "core_enterprise_address", nullable = false, length = 42)
    @ApiModelProperty(value = "核心企业地址", required = true, example = "0xabcdef1234567890")
    private String coreEnterpriseAddress;

    @Column(name = "amount", nullable = false, precision = 20, scale = 2)
    @ApiModelProperty(value = "应收金额", required = true, example = "500000.00")
    private BigDecimal amount;

    @Column(name = "currency", length = 10)
    @ApiModelProperty(value = "币种", example = "CNY")
    private String currency = "CNY";

    @Column(name = "issue_date", nullable = false)
    @ApiModelProperty(value = "出票日期", required = true, example = "2024-01-13T10:00:00")
    private LocalDateTime issueDate;

    @Column(name = "due_date", nullable = false)
    @ApiModelProperty(value = "到期日期", required = true, example = "2024-04-13T10:00:00")
    private LocalDateTime dueDate;

    @Column(name = "description", columnDefinition = "TEXT")
    @ApiModelProperty(value = "描述", example = "原材料采购款")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @ApiModelProperty(value = "状态", notes = "CREATED-已创建, CONFIRMED-已确认, FINANCED-已融资, REPAID-已还款, DEFAULTED-已违约, CANCELLED-已取消", example = "CONFIRMED")
    private ReceivableStatus status = ReceivableStatus.CREATED;

    @Column(name = "current_holder", nullable = false, length = 42)
    @ApiModelProperty(value = "当前持有人地址", example = "0x1234567890abcdef")
    private String currentHolder;

    @Column(name = "financier_address", length = 42)
    @ApiModelProperty(value = "资金方地址", example = "0x567890abcdef1234")
    private String financierAddress;

    @Column(name = "finance_amount", precision = 20, scale = 2)
    @ApiModelProperty(value = "融资金额", example = "450000.00")
    private BigDecimal financeAmount;

    @Column(name = "finance_rate")
    @ApiModelProperty(value = "融资利率(基点)", example = "500")
    private Integer financeRate;

    @Column(name = "finance_date")
    @ApiModelProperty(value = "融资日期", example = "2024-01-13T15:00:00")
    private LocalDateTime financeDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    @ApiModelProperty(value = "创建时间", hidden = true)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @ApiModelProperty(value = "更新时间", hidden = true)
    private LocalDateTime updatedAt;

    @Column(name = "tx_hash", length = 66)
    @ApiModelProperty(value = "区块链交易哈希", example = "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef12")
    private String txHash;

    @Column(name = "overdue_level", length = 20)
    @ApiModelProperty(value = "逾期等级", notes = "MILD-轻度(1-30天), MODERATE-中度(31-90天), SEVERE-重度(91-179天), BAD_DEBT-坏账(180天+)", example = "MILD")
    private String overdueLevel;

    @Column(name = "overdue_days")
    @ApiModelProperty(value = "逾期天数", example = "45")
    private Integer overdueDays;

    @Column(name = "penalty_amount", precision = 20, scale = 2)
    @ApiModelProperty(value = "累计罚息金额", example = "12500.00")
    private BigDecimal penaltyAmount;

    @Column(name = "last_remind_date")
    @ApiModelProperty(value = "最后催收日期", example = "2024-02-01T10:00:00")
    private LocalDateTime lastRemindDate;

    @Column(name = "remind_count")
    @ApiModelProperty(value = "催收次数", example = "3")
    private Integer remindCount;

    @Column(name = "bad_debt_date")
    @ApiModelProperty(value = "坏账认定日期", example = "2024-07-01T00:00:00")
    private LocalDateTime badDebtDate;

    @Column(name = "bad_debt_reason", columnDefinition = "TEXT")
    @ApiModelProperty(value = "坏账原因", example = "逾期180天以上，债务人失联")
    private String badDebtReason;

    @Column(name = "overdue_calculated_date")
    @ApiModelProperty(value = "逾期信息计算日期", example = "2024-02-01T10:00:00")
    private LocalDateTime overdueCalculatedDate;

    @Column(name = "parent_receivable_id", length = 36)
    @ApiModelProperty(value = "父应收账款ID（用于拆分合并追溯）")
    private String parentReceivableId;

    @Column(name = "split_count")
    @ApiModelProperty(value = "拆分数量（拆分后的子应收账款数量）")
    private Integer splitCount;

    @Column(name = "merge_count")
    @ApiModelProperty(value = "合并数量（合并前的应收账款数量）")
    private Integer mergeCount;

    @Column(name = "split_time")
    @ApiModelProperty(value = "拆分时间")
    private LocalDateTime splitTime;

    @Column(name = "merge_time")
    @ApiModelProperty(value = "合并时间")
    private LocalDateTime mergeTime;

    public enum ReceivableStatus {
        CREATED,         // 已创建
        CONFIRMED,       // 已确认（核心企业确认）
        FINANCED,        // 已融资
        REPAID,          // 已还款
        DEFAULTED,       // 已违约
        CANCELLED,       // 已取消
        SPLITTING,       // 拆分中
        SPLIT,           // 已拆分
        MERGING,         // 合并中
        MERGED           // 已合并
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
