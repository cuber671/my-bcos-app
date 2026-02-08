package com.fisco.app.entity.risk;

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

import javax.persistence.*;

/**
 * 坏账记录实体类
 */
@Data
@Entity
@Table(name = "bad_debt_record", indexes = {
    @Index(name = "idx_receivable_id", columnList = "receivable_id"),
    @Index(name = "idx_bad_debt_type", columnList = "bad_debt_type"),
    @Index(name = "idx_recovery_status", columnList = "recovery_status"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@ApiModel(value = "BadDebtRecord", description = "坏账记录实体")
public class BadDebtRecord {

    @Id
    @Column(name = "id", nullable = false, unique = true, length = 36)
    @ApiModelProperty(value = "记录ID（UUID格式）", required = true)
    private String id;

    @Column(name = "receivable_id", nullable = false, unique = true, length = 36)
    @ApiModelProperty(value = "应收账款ID（唯一）", required = true)
    private String receivableId;

    @Enumerated(EnumType.STRING)
    @Column(name = "bad_debt_type", nullable = false, length = 20)
    @ApiModelProperty(value = "坏账类型", notes = "OVERDUE_180-逾期180天+, BANKRUPTCY-破产, DISPUTE-争议, OTHER-其他", required = true)
    private BadDebtType badDebtType;

    @Column(name = "principal_amount", nullable = false, precision = 20, scale = 2)
    @ApiModelProperty(value = "本金金额", required = true, example = "500000.00")
    private BigDecimal principalAmount;

    @Column(name = "overdue_days", nullable = false)
    @ApiModelProperty(value = "逾期天数", required = true, example = "200")
    private Integer overdueDays;

    @Column(name = "total_penalty_amount", precision = 20, scale = 2)
    @ApiModelProperty(value = "累计罚息金额", example = "50000.00")
    private BigDecimal totalPenaltyAmount;

    @Column(name = "total_loss_amount", nullable = false, precision = 20, scale = 2)
    @ApiModelProperty(value = "总损失金额（本金+罚息）", required = true, example = "550000.00")
    private BigDecimal totalLossAmount;

    @Column(name = "bad_debt_reason", columnDefinition = "TEXT")
    @ApiModelProperty(value = "坏账原因")
    private String badDebtReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "recovery_status", nullable = false, length = 20)
    @ApiModelProperty(value = "回收状态", notes = "NOT_RECOVERED-未回收, PARTIAL_RECOVERED-部分回收, FULL_RECOVERED-全额回收", required = true)
    private RecoveryStatus recoveryStatus = RecoveryStatus.NOT_RECOVERED;

    @Column(name = "recovered_amount", precision = 20, scale = 2)
    @ApiModelProperty(value = "已回收金额", example = "0.00")
    private BigDecimal recoveredAmount = BigDecimal.ZERO;

    @Column(name = "recovery_date")
    @ApiModelProperty(value = "回收日期")
    private LocalDateTime recoveryDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    @ApiModelProperty(value = "创建时间", hidden = true)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @ApiModelProperty(value = "更新时间", hidden = true)
    private LocalDateTime updatedAt;

    /**
     * 坏账类型枚举
     */
    public enum BadDebtType {
        OVERDUE_180,    // 逾期180天+
        BANKRUPTCY,     // 破产
        DISPUTE,        // 争议
        OTHER           // 其他
    }

    /**
     * 回收状态枚举
     */
    public enum RecoveryStatus {
        NOT_RECOVERED,      // 未回收
        PARTIAL_RECOVERED,  // 部分回收
        FULL_RECOVERED      // 全额回收
    }

    @PrePersist
    protected void onCreate() {
        if (id == null || id.isEmpty()) {
            id = java.util.UUID.randomUUID().toString();
        }
        if (recoveryStatus == null) {
            recoveryStatus = RecoveryStatus.NOT_RECOVERED;
        }
        if (recoveredAmount == null) {
            recoveredAmount = BigDecimal.ZERO;
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
