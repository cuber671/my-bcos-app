package com.fisco.app.entity.bill;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import javax.persistence.*;

/**
 * 票据背书记录实体类
 * 记录票据的每一次背书转让历史
 */
@Data
@Entity
@Table(name = "endorsement", indexes = {
    @Index(name = "idx_endorsement_bill_id", columnList = "bill_id"),
    @Index(name = "idx_endorsement_from", columnList = "endorser_address"),
    @Index(name = "idx_endorsement_to", columnList = "endorsee_address"),
    @Index(name = "idx_endorsement_date", columnList = "endorsement_date")
})
@ApiModel(value = "背书记录", description = "票据背书转让记录实体")
@Schema(name = "背书记录")
public class Endorsement {

    @Id
    @Column(name = "id", nullable = false, unique = true, length = 36)
    @ApiModelProperty(value = "背书ID（UUID格式）", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
    private String id;

    @Column(name = "bill_id", nullable = false, length = 36)
    @ApiModelProperty(value = "票据ID（UUID格式）", required = true, example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String billId;

    /**
     * 背书人地址（当前持票人）
     */
    @Column(name = "endorser_address", nullable = false, length = 42)
    @ApiModelProperty(value = "背书人地址", required = true, example = "0x1234567890abcdef1234567890abcdef12345678")
    private String endorserAddress;

    /**
     * 被背书人地址（新持票人）
     */
    @Column(name = "endorsee_address", nullable = false, length = 42)
    @ApiModelProperty(value = "被背书人地址", required = true, example = "0xabcdefabcdefabcdefabcdefabcdefabcdefabcd")
    private String endorseeAddress;

    /**
     * 背书类型
     * NORMAL - 普通背书
     * DISCOUNT - 贴现背书
     * PLEDGE - 质押背书
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "endorsement_type", nullable = false, length = 20)
    @ApiModelProperty(value = "背书类型", required = true, example = "NORMAL", notes = "NORMAL-普通背书, DISCOUNT-贴现背书, PLEDGE-质押背书")
    private EndorsementType endorsementType;

    /**
     * 背书金额（如果是部分背书）
     * null 表示全额背书
     */
    @Column(name = "endorsement_amount", precision = 20, scale = 2)
    @ApiModelProperty(value = "背书金额", example = "50000.00", notes = "全额背书时为null")
    private Long endorsementAmount;

    /**
     * 背书日期
     */
    @Column(name = "endorsement_date", nullable = false)
    @ApiModelProperty(value = "背书日期", required = true, example = "2024-02-01T10:00:00")
    private LocalDateTime endorsementDate;

    /**
     * 背书备注
     */
    @Column(name = "remark", columnDefinition = "TEXT")
    @ApiModelProperty(value = "背书备注", example = "转让给供应商A")
    private String remark;

    /**
     * 区块链交易哈希
     */
    @Column(name = "tx_hash", length = 66)
    @ApiModelProperty(value = "区块链交易哈希", example = "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890")
    private String txHash;

    /**
     * 背书序号（该票据的第几次背书）
     */
    @Column(name = "endorsement_sequence")
    @ApiModelProperty(value = "背书序号", example = "1", notes = "表示该票据的第几次背书")
    private Integer endorsementSequence;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @ApiModelProperty(value = "创建时间", required = true, example = "2024-02-01T10:00:00")
    private LocalDateTime createdAt;

    /**
     * 背书类型枚举
     */
    public enum EndorsementType {
        NORMAL,      // 普通背书
        DISCOUNT,    // 贴现背书
        PLEDGE       // 质押背书
    }

    @PrePersist
    protected void onCreate() {
        if (id == null || id.isEmpty()) {
            id = UUID.randomUUID().toString();
        }
        createdAt = LocalDateTime.now();
        if (endorsementDate == null) {
            endorsementDate = LocalDateTime.now();
        }
    }
}
