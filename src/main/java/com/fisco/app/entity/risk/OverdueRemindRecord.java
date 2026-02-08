package com.fisco.app.entity.risk;
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

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 逾期催收记录实体类
 */
@Data
@Entity
@Table(name = "overdue_remind_record", indexes = {
    @Index(name = "idx_receivable_id", columnList = "receivable_id"),
    @Index(name = "idx_remind_type", columnList = "remind_type"),
    @Index(name = "idx_remind_date", columnList = "remind_date"),
    @Index(name = "idx_operator", columnList = "operator_address")
})
@ApiModel(value = "OverdueRemindRecord", description = "逾期催收记录实体")
public class OverdueRemindRecord {

    @Id
    @Column(name = "id", nullable = false, unique = true, length = 36)
    @ApiModelProperty(value = "记录ID（UUID格式）", required = true)
    private String id;

    @Column(name = "receivable_id", nullable = false, length = 36)
    @ApiModelProperty(value = "应收账款ID", required = true)
    private String receivableId;

    @Enumerated(EnumType.STRING)
    @Column(name = "remind_type", nullable = false, length = 20)
    @ApiModelProperty(value = "催收类型", notes = "EMAIL-邮件, SMS-短信, PHONE-电话, LETTER-函件, LEGAL-法律", required = true)
    private RemindType remindType;

    @Enumerated(EnumType.STRING)
    @Column(name = "remind_level", length = 20)
    @ApiModelProperty(value = "催收级别", notes = "NORMAL-普通, URGENT-紧急, SEVERE-严重")
    private RemindLevel remindLevel;

    @Column(name = "remind_date", nullable = false)
    @ApiModelProperty(value = "催收日期", required = true)
    private LocalDateTime remindDate;

    @Column(name = "operator_address", length = 42)
    @ApiModelProperty(value = "操作人地址", example = "0x1234567890abcdef")
    private String operatorAddress;

    @Column(name = "remind_content", columnDefinition = "TEXT")
    @ApiModelProperty(value = "催收内容")
    private String remindContent;

    @Enumerated(EnumType.STRING)
    @Column(name = "remind_result", length = 20)
    @ApiModelProperty(value = "催收结果", notes = "SUCCESS-成功, FAILED-失败, PENDING-待处理")
    private RemindResult remindResult;

    @Column(name = "next_remind_date")
    @ApiModelProperty(value = "下次催收日期")
    private LocalDateTime nextRemindDate;

    @Column(name = "remark", columnDefinition = "TEXT")
    @ApiModelProperty(value = "备注")
    private String remark;

    @Column(name = "tx_hash", length = 66)
    @ApiModelProperty(value = "区块链交易哈希", example = "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef12")
    private String txHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    @ApiModelProperty(value = "创建时间", hidden = true)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @ApiModelProperty(value = "更新时间", hidden = true)
    private LocalDateTime updatedAt;

    /**
     * 催收类型枚举
     */
    public enum RemindType {
        EMAIL,      // 邮件
        SMS,        // 短信
        PHONE,      // 电话
        LETTER,     // 函件
        LEGAL       // 法律
    }

    /**
     * 催收级别枚举
     */
    public enum RemindLevel {
        NORMAL,     // 普通
        URGENT,     // 紧急
        SEVERE      // 严重
    }

    /**
     * 催收结果枚举
     */
    public enum RemindResult {
        SUCCESS,    // 成功
        FAILED,     // 失败
        PENDING     // 待处理
    }

    @PrePersist
    protected void onCreate() {
        if (id == null || id.isEmpty()) {
            id = java.util.UUID.randomUUID().toString();
        }
        if (remindDate == null) {
            remindDate = LocalDateTime.now();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
