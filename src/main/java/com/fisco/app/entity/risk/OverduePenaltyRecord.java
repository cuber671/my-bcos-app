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
 * 逾期罚息记录实体类
 */
@Data
@Entity
@Table(name = "overdue_penalty_record", indexes = {
    @Index(name = "idx_receivable_id", columnList = "receivable_id"),
    @Index(name = "idx_penalty_type", columnList = "penalty_type"),
    @Index(name = "idx_calculate_date", columnList = "calculate_date")
})
@ApiModel(value = "OverduePenaltyRecord", description = "逾期罚息记录实体")
public class OverduePenaltyRecord {

    @Id
    @Column(name = "id", nullable = false, unique = true, length = 36)
    @ApiModelProperty(value = "记录ID（UUID格式）", required = true)
    private String id;

    @Column(name = "receivable_id", nullable = false, length = 36)
    @ApiModelProperty(value = "应收账款ID", required = true)
    private String receivableId;

    @Enumerated(EnumType.STRING)
    @Column(name = "penalty_type", nullable = false, length = 20)
    @ApiModelProperty(value = "罚息类型", notes = "AUTO-自动计算, MANUAL-手动计算", required = true)
    private PenaltyType penaltyType;

    @Column(name = "principal_amount", nullable = false, precision = 20, scale = 2)
    @ApiModelProperty(value = "本金金额", required = true, example = "500000.00")
    private BigDecimal principalAmount;

    @Column(name = "overdue_days", nullable = false)
    @ApiModelProperty(value = "逾期天数", required = true, example = "45")
    private Integer overdueDays;

    @Column(name = "daily_rate", nullable = false, precision = 10, scale = 6)
    @ApiModelProperty(value = "日利率（如0.0005表示0.05%）", required = true, example = "0.0005")
    private BigDecimal dailyRate;

    @Column(name = "penalty_amount", nullable = false, precision = 20, scale = 2)
    @ApiModelProperty(value = "本次罚息金额", required = true, example = "1125.00")
    private BigDecimal penaltyAmount;

    @Column(name = "total_penalty_amount", nullable = false, precision = 20, scale = 2)
    @ApiModelProperty(value = "累计罚息金额", required = true, example = "12500.00")
    private BigDecimal totalPenaltyAmount;

    @Column(name = "calculate_start_date", nullable = false)
    @ApiModelProperty(value = "计算起始日期", required = true)
    private LocalDateTime calculateStartDate;

    @Column(name = "calculate_end_date", nullable = false)
    @ApiModelProperty(value = "计算结束日期", required = true)
    private LocalDateTime calculateEndDate;

    @Column(name = "calculate_date", nullable = false)
    @ApiModelProperty(value = "计算日期", required = true)
    private LocalDateTime calculateDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    @ApiModelProperty(value = "创建时间", hidden = true)
    private LocalDateTime createdAt;

    /**
     * 罚息类型枚举
     */
    public enum PenaltyType {
        AUTO,       // 自动计算
        MANUAL      // 手动计算
    }

    @PrePersist
    protected void onCreate() {
        if (id == null || id.isEmpty()) {
            id = java.util.UUID.randomUUID().toString();
        }
        if (calculateDate == null) {
            calculateDate = LocalDateTime.now();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
