package com.fisco.app.entity.warehouse;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.springframework.lang.NonNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fisco.app.entity.user.User;

import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 电子仓单背书链实体类
 * 支持28个字段，包含背书企业信息、经手人信息、区块链信息等
 */
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ewr_endorsement_chain", indexes = {
    @Index(name = "idx_receipt_id", columnList = "receipt_id"),
    @Index(name = "idx_receipt_no", columnList = "receipt_no"),
    @Index(name = "idx_endorse_from", columnList = "endorse_from"),
    @Index(name = "idx_endorse_to", columnList = "endorse_to"),
    @Index(name = "idx_operator_from", columnList = "operator_from_id"),
    @Index(name = "idx_operator_to", columnList = "operator_to_id"),
    @Index(name = "idx_endorsement_time", columnList = "endorsement_time"),
    @Index(name = "idx_endorsement_status", columnList = "endorsement_status"),
    @Index(name = "idx_endorsement_type", columnList = "endorsement_type"),
    @Index(name = "idx_tx_hash", columnList = "tx_hash")
})
@Schema(name = "EwrEndorsementChain", description = "电子仓单背书链")
public class EwrEndorsementChain {

    // ==================== 基础字段 (4个) ====================

    @Id
    @Column(name = "id", nullable = false, unique = true, length = 36)
    @ApiModelProperty(value = "背书ID（UUID格式）", required = true, example = "b2c3d4e5-f6g7-8901-bcde-f23456789012")
    private String id;

    @Column(name = "receipt_id", nullable = false, length = 36)
    @NonNull
    @ApiModelProperty(value = "仓单ID（UUID）", required = true)
    private String receiptId;

    @Column(name = "receipt_no", nullable = false, length = 64)
    @ApiModelProperty(value = "仓单编号（冗余）", required = true, example = "EWR20260126000001")
    private String receiptNo;

    @Column(name = "endorsement_no", nullable = false, unique = true, length = 64)
    @ApiModelProperty(value = "背书编号", required = true, example = "END20260126000001", notes = "格式: END+yyyyMMdd+6位流水号")
    private String endorsementNo;

    // ==================== 背书企业信息 (4个字段) ====================

    @Column(name = "endorse_from", nullable = false, length = 42)
    @ApiModelProperty(value = "背书企业地址（转出方）", required = true, example = "0xabcdabcdabcdabcdabcdabcdabcdabcdabcdabcd")
    @Size(min = 42, max = 42, message = "背书企业地址必须是42位")
    @Pattern(regexp = "^0x[a-fA-F0-9]{40}$", message = "背书企业地址格式不正确")
    private String endorseFrom;

    @Column(name = "endorse_from_name", length = 255)
    @ApiModelProperty(value = "背书企业名称（冗余）", example = "XX贸易有限公司")
    private String endorseFromName;

    @Column(name = "endorse_to", nullable = false, length = 42)
    @ApiModelProperty(value = "被背书企业地址（转入方）", required = true, example = "0x1234567890abcdef1234567890abcdef12345678")
    @Size(min = 42, max = 42, message = "被背书企业地址必须是42位")
    @Pattern(regexp = "^0x[a-fA-F0-9]{40}$", message = "被背书企业地址格式不正确")
    private String endorseTo;

    @Column(name = "endorse_to_name", length = 255)
    @ApiModelProperty(value = "被背书企业名称（冗余）", example = "YY物流有限公司")
    private String endorseToName;

    // ==================== 经手人信息 (4个字段) ====================

    @Column(name = "operator_from_id", length = 36)
    @ApiModelProperty(value = "转出方经手人ID", example = "user-uuid-001")
    private String operatorFromId;

    @Column(name = "operator_from_name", length = 100)
    @ApiModelProperty(value = "转出方经手人姓名", example = "王五（转出企业员工）")
    private String operatorFromName;

    @Column(name = "operator_to_id", length = 36)
    @ApiModelProperty(value = "转入方经手人ID", example = "user-uuid-002")
    private String operatorToId;

    @Column(name = "operator_to_name", length = 100)
    @ApiModelProperty(value = "转入方经手人姓名", example = "赵六（转入企业员工）")
    private String operatorToName;

    // ==================== 背书类型和原因 (2个字段) ====================

    @Enumerated(EnumType.STRING)
    @Column(name = "endorsement_type", nullable = false, length = 20)
    @ApiModelProperty(value = "背书类型", required = true, example = "TRANSFER", notes = "TRANSFER-转让, PLEDGE-质押, RELEASE-解押, CANCEL-撤销")
    private EndorsementType endorsementType = EndorsementType.TRANSFER;

    @Column(name = "endorsement_reason", length = 500)
    @ApiModelProperty(value = "背书原因说明", example = "货物所有权转让")
    private String endorsementReason;

    // ==================== 货物信息快照 (1个字段) ====================

    @Column(name = "goods_snapshot", columnDefinition = "TEXT")
    @ApiModelProperty(value = "背书时的货物信息快照（JSON格式）", example = "{\"goods_name\":\"螺纹钢\",\"quantity\":1000,\"unit_price\":4500}")
    private String goodsSnapshot;

    // ==================== 价格和金额 (2个字段) ====================

    @Column(name = "transfer_price", precision = 20, scale = 2)
    @ApiModelProperty(value = "转让价格（元）", example = "4600.00")
    private BigDecimal transferPrice;

    @Column(name = "transfer_amount", precision = 20, scale = 2)
    @ApiModelProperty(value = "转让金额", example = "4600000.00")
    private BigDecimal transferAmount;

    // ==================== 区块链信息 (3个字段) ====================

    @Column(name = "tx_hash", length = 66)
    @ApiModelProperty(value = "背书交易哈希", example = "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890")
    private String txHash;

    @Column(name = "block_number")
    @ApiModelProperty(value = "区块高度", example = "12346")
    private Long blockNumber;

    @Column(name = "blockchain_timestamp")
    @ApiModelProperty(value = "区块链时间戳", example = "2026-01-26T14:35:00")
    private LocalDateTime blockchainTimestamp;

    // ==================== 状态信息 (2个字段) ====================

    @Enumerated(EnumType.STRING)
    @Column(name = "endorsement_status", nullable = false, length = 20)
    @ApiModelProperty(value = "背书状态", required = true, example = "PENDING", notes = "PENDING-待确认, CONFIRMED-已确认, CANCELLED-已撤销")
    private EndorsementStatus endorsementStatus = EndorsementStatus.PENDING;

    @Column(name = "remarks", columnDefinition = "TEXT")
    @ApiModelProperty(value = "备注信息", example = "备注：背书协议已签署")
    private String remarks;

    // ==================== 时间戳 (4个字段) ====================

    @Column(name = "endorsement_time", nullable = false)
    @ApiModelProperty(value = "背书发起时间", required = true, example = "2026-01-26T14:30:00")
    @NotNull(message = "背书发起时间不能为空")
    private LocalDateTime endorsementTime;

    @Column(name = "confirmed_time")
    @ApiModelProperty(value = "确认时间", example = "2026-01-26T15:00:00")
    private LocalDateTime confirmedTime;

    @Column(name = "created_at", nullable = false)
    @ApiModelProperty(value = "创建时间", hidden = true)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @ApiModelProperty(value = "更新时间", hidden = true)
    private LocalDateTime updatedAt;

    // ==================== 操作人信息 (2个字段) ====================

    @Column(name = "created_by", length = 50)
    @ApiModelProperty(value = "创建人（系统用户名）", hidden = true, example = "admin")
    private String createdBy;

    @Column(name = "updated_by", length = 50)
    @ApiModelProperty(value = "更新人（系统用户名）", hidden = true)
    private String updatedBy;

    // ==================== 关联关系 ====================

    /**
     * 仓单
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_id", insertable = false, updatable = false)
    @JsonIgnore
    @ApiModelProperty(hidden = true)
    private ElectronicWarehouseReceipt receipt;

    /**
     * 转出方经手人
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_from_id", insertable = false, updatable = false)
    @JsonIgnore
    @ApiModelProperty(hidden = true)
    private User operatorFrom;

    /**
     * 转入方经手人
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_to_id", insertable = false, updatable = false)
    @JsonIgnore
    @ApiModelProperty(hidden = true)
    private User operatorTo;

    // ==================== 生命周期回调 ====================

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (endorsementStatus == null) {
            endorsementStatus = EndorsementStatus.PENDING;
        }
        if (endorsementType == null) {
            endorsementType = EndorsementType.TRANSFER;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ==================== 枚举定义 ====================

    /**
     * 背书类型枚举
     */
    public enum EndorsementType {
        TRANSFER,        // 转让
        PLEDGE,          // 质押
        RELEASE,         // 解押
        CANCEL           // 撤销
    }

    /**
     * 背书状态枚举
     */
    public enum EndorsementStatus {
        PENDING,         // 待确认
        CONFIRMED,       // 已确认
        CANCELLED        // 已撤销
    }
}
