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
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.springframework.lang.NonNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fisco.app.entity.enterprise.Enterprise;
import com.fisco.app.entity.user.User;

import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 电子仓单实体类
 * 支持44个字段，包含完整的企业信息、操作人信息、融资信息、背书统计等
 */
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "electronic_warehouse_receipt", indexes = {
    @Index(name = "idx_warehouse", columnList = "warehouse_address"),
    @Index(name = "idx_owner", columnList = "owner_address"),
    @Index(name = "idx_holder", columnList = "holder_address"),
    @Index(name = "idx_status", columnList = "receipt_status"),
    @Index(name = "idx_expiry_date", columnList = "expiry_date"),
    @Index(name = "idx_storage_date", columnList = "storage_date"),
    @Index(name = "idx_financier", columnList = "financier_address"),
    @Index(name = "idx_blockchain_status", columnList = "blockchain_status"),
    @Index(name = "idx_created_at", columnList = "created_at"),
    @Index(name = "idx_deleted_at", columnList = "deleted_at"),
    @Index(name = "idx_owner_operator", columnList = "owner_operator_id"),
    @Index(name = "idx_warehouse_operator", columnList = "warehouse_operator_id")
})
@Schema(name = "ElectronicWarehouseReceipt", description = "电子仓单")
public class ElectronicWarehouseReceipt {

    // ==================== 基础信息 (8个字段) ====================

    @Id
    @Column(name = "id", nullable = false, unique = true, length = 36)
    @NonNull
    @ApiModelProperty(value = "仓单ID（UUID格式）", required = true, example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String id;

    @Column(name = "receipt_no", nullable = false, unique = true, length = 64)
    @ApiModelProperty(value = "仓单编号", required = true, example = "EWR20260126000001", notes = "格式: EWR+yyyyMMdd+6位流水号")
    private String receiptNo;

    @Column(name = "warehouse_id", nullable = false, length = 36)
    @ApiModelProperty(value = "仓储企业ID（UUID）", required = true)
    private String warehouseId;

    @Column(name = "warehouse_address", nullable = false, length = 42)
    @ApiModelProperty(value = "仓储方区块链地址", required = true, example = "0x1234567890abcdef1234567890abcdef12345678")
    @Size(min = 42, max = 42, message = "仓储方地址必须是42位")
    @Pattern(regexp = "^0x[a-fA-F0-9]{40}$", message = "仓储方地址格式不正确")
    private String warehouseAddress;

    @Column(name = "warehouse_name", length = 255)
    @ApiModelProperty(value = "仓储方名称（冗余字段）", example = "XX仓储有限公司")
    private String warehouseName;

    @Column(name = "owner_id", nullable = false, length = 36)
    @ApiModelProperty(value = "货主企业ID（UUID）", required = true)
    private String ownerId;

    @Column(name = "owner_address", nullable = false, length = 42)
    @ApiModelProperty(value = "货主区块链地址", required = true, example = "0xabcdabcdabcdabcdabcdabcdabcdabcdabcdabcd")
    @Size(min = 42, max = 42, message = "货主地址必须是42位")
    @Pattern(regexp = "^0x[a-fA-F0-9]{40}$", message = "货主地址格式不正确")
    private String ownerAddress;

    @Column(name = "holder_address", nullable = false, length = 42)
    @ApiModelProperty(value = "持单人地址（可背书转让）", required = true, example = "0xabcdabcdabcdabcdabcdabcdabcdabcdabcdabcd")
    @Size(min = 42, max = 42, message = "持单人地址必须是42位")
    @Pattern(regexp = "^0x[a-fA-F0-9]{40}$", message = "持单人地址格式不正确")
    private String holderAddress;

    // ==================== 货物信息 (4个字段) ====================

    @Column(name = "goods_name", nullable = false)
    @ApiModelProperty(value = "货物名称", required = true, example = "螺纹钢")
    @NotBlank(message = "货物名称不能为空")
    private String goodsName;

    @Column(name = "unit", nullable = false, length = 20)
    @ApiModelProperty(value = "计量单位", required = true, example = "吨", notes = "吨、千克、立方米、平方米、件、箱等")
    @NotBlank(message = "计量单位不能为空")
    @Size(max = 20, message = "计量单位长度不能超过20")
    private String unit;

    @Column(name = "quantity", nullable = false, precision = 20, scale = 2)
    @ApiModelProperty(value = "货物数量", required = true, example = "1000.00", notes = "必须大于0")
    @DecimalMin(value = "0.01", message = "货物数量必须大于0")
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, precision = 20, scale = 2)
    @ApiModelProperty(value = "单价（元）", required = true, example = "4500.00")
    @NotNull(message = "单价不能为空")
    @DecimalMin(value = "0.01", message = "单价必须大于0")
    private BigDecimal unitPrice;

    @Column(name = "total_value", nullable = false, precision = 20, scale = 2)
    @ApiModelProperty(value = "货物总价值（元）", required = true, example = "4500000.00")
    @NotNull(message = "总价值不能为空")
    @DecimalMin(value = "0", message = "总价值不能为负数")
    private BigDecimal totalValue;

    @Column(name = "market_price", precision = 20, scale = 2)
    @ApiModelProperty(value = "市场参考价格（元）", example = "4600.00", notes = "用于评估当前市场价值，可定期更新")
    @DecimalMin(value = "0", message = "市场价格不能为负数")
    private BigDecimal marketPrice;

    // ==================== 仓储信息 (4个字段) ====================

    @Column(name = "warehouse_location", length = 500)
    @ApiModelProperty(value = "仓库详细地址", example = "上海市浦东新区XX路XX号")
    private String warehouseLocation;

    @Column(name = "storage_location", length = 200)
    @ApiModelProperty(value = "存储位置", example = "A区03栋12排5层货架")
    private String storageLocation;

    @Column(name = "storage_date", nullable = false)
    @ApiModelProperty(value = "入库时间", required = true, example = "2026-01-26T10:00:00")
    @NotNull(message = "入库时间不能为空")
    private LocalDateTime storageDate;

    @Column(name = "expiry_date", nullable = false)
    @ApiModelProperty(value = "仓单有效期", required = true, example = "2026-07-26T23:59:59")
    @NotNull(message = "有效期不能为空")
    private LocalDateTime expiryDate;

    @Column(name = "actual_delivery_date")
    @ApiModelProperty(value = "实际提货时间", example = "2026-06-15T14:30:00", notes = "货物被提取的日期时间，状态变为DELIVERED时记录")
    private LocalDateTime actualDeliveryDate;

    @Column(name = "delivery_person_name", length = 100)
    @ApiModelProperty(value = "提货人姓名", example = "张三")
    private String deliveryPersonName;

    @Column(name = "delivery_person_contact", length = 50)
    @ApiModelProperty(value = "提货人联系方式", example = "13800138000")
    private String deliveryPersonContact;

    @Column(name = "delivery_no", length = 64)
    @ApiModelProperty(value = "提货单号", example = "DEL202601150001")
    private String deliveryNo;

    @Column(name = "vehicle_plate", length = 20)
    @ApiModelProperty(value = "运输车牌号", example = "沪A12345")
    private String vehiclePlate;

    @Column(name = "driver_name", length = 100)
    @ApiModelProperty(value = "司机姓名", example = "李四")
    private String driverName;

    // ==================== 状态管理 (3个字段) ====================

    @Enumerated(EnumType.STRING)
    @Column(name = "receipt_status", nullable = false, length = 20)
    @ApiModelProperty(value = "仓单状态", required = true, example = "NORMAL", notes = "DRAFT-草稿, NORMAL-正常, PLEDGED-已质押, TRANSFERRED-已转让, FROZEN-已冻结, EXPIRED-已过期, DELIVERED-已提货, CANCELLED-已取消")
    private ReceiptStatus receiptStatus = ReceiptStatus.DRAFT;

    @Column(name = "parent_receipt_id", length = 36)
    @ApiModelProperty(value = "父仓单ID（用于拆分）")
    private String parentReceiptId;

    @Column(name = "batch_no", length = 64)
    @ApiModelProperty(value = "批次号", example = "BATCH20260126001")
    private String batchNo;

    // ==================== 企业和操作人 (5个字段) ====================

    @Column(name = "owner_name", length = 255)
    @ApiModelProperty(value = "货主企业名称（冗余）", example = "XX贸易有限公司")
    private String ownerName;

    @Column(name = "owner_operator_id", length = 36)
    @ApiModelProperty(value = "货主企业操作人ID", example = "user-uuid-001")
    private String ownerOperatorId;

    @Column(name = "owner_operator_name", length = 100)
    @ApiModelProperty(value = "货主企业操作人姓名", example = "张三（货主企业业务员）")
    private String ownerOperatorName;

    @Column(name = "warehouse_operator_id", length = 36)
    @ApiModelProperty(value = "仓储方操作人ID", example = "user-uuid-002")
    private String warehouseOperatorId;

    @Column(name = "warehouse_operator_name", length = 100)
    @ApiModelProperty(value = "仓储方操作人姓名", example = "李四（仓储方仓库管理员）")
    private String warehouseOperatorName;

    // ==================== 融资信息 (6个字段) ====================

    @Column(name = "is_financed")
    @ApiModelProperty(value = "是否已融资", example = "false")
    private Boolean isFinanced = false;

    @Column(name = "finance_amount", precision = 20, scale = 2)
    @ApiModelProperty(value = "融资金额（元）", example = "4000000.00")
    private BigDecimal financeAmount;

    @Column(name = "finance_rate")
    @ApiModelProperty(value = "融资利率（基点）", example = "500", notes = "1基点=0.01%, 500基点=5%")
    private Integer financeRate;

    @Column(name = "finance_date")
    @ApiModelProperty(value = "融资日期", example = "2026-01-26T14:00:00")
    private LocalDateTime financeDate;

    @Column(name = "financier_address", length = 42)
    @ApiModelProperty(value = "资金方地址", example = "0x9876543210987654321098765432109876543210")
    private String financierAddress;

    @Column(name = "pledge_contract_no", length = 64)
    @ApiModelProperty(value = "质押合同编号", example = "CONTRACT20260126001")
    private String pledgeContractNo;

    // ==================== 背书统计 (3个字段) ====================

    @Column(name = "endorsement_count")
    @ApiModelProperty(value = "背书次数", example = "0")
    private Integer endorsementCount = 0;

    @Column(name = "last_endorsement_date")
    @ApiModelProperty(value = "最后背书时间", example = "2026-01-26T15:30:00")
    private LocalDateTime lastEndorsementDate;

    @Column(name = "current_holder", length = 42)
    @ApiModelProperty(value = "当前持单人（冗余）")
    private String currentHolder;

    // ==================== 区块链 (4个字段) ====================

    @Column(name = "tx_hash", length = 66)
    @ApiModelProperty(value = "区块链交易哈希", example = "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef")
    private String txHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "blockchain_status", length = 20)
    @ApiModelProperty(value = "区块链上链状态", example = "PENDING", notes = "PENDING-待上链, SYNCED-已同步, FAILED-上链失败, VERIFIED-已验证")
    private BlockchainStatus blockchainStatus = BlockchainStatus.PENDING;

    @Column(name = "block_number")
    @ApiModelProperty(value = "区块高度", example = "12345")
    private Long blockNumber;

    @Column(name = "blockchain_timestamp")
    @ApiModelProperty(value = "区块链时间戳", example = "2026-01-26T10:05:00")
    private LocalDateTime blockchainTimestamp;

    // ==================== 其他 (1个字段) ====================

    @Column(name = "remarks", columnDefinition = "TEXT")
    @ApiModelProperty(value = "备注信息", example = "备注：货物为一级品")
    private String remarks;

    // ==================== 拆分相关字段（2个） ====================

    @Column(name = "split_time", columnDefinition = "DATETIME(6)")
    @ApiModelProperty(value = "拆分时间", example = "2026-02-02T10:30:00")
    private LocalDateTime splitTime;

    @ApiModelProperty(value = "子仓单数量", example = "2")
    private Integer splitCount;

    // ==================== 合并相关字段（4个） ✨ 新增 ====================

    @Column(name = "merge_count")
    @ApiModelProperty(value = "合并仓单数量", example = "3")
    private Integer mergeCount;

    @Column(name = "merge_time", columnDefinition = "DATETIME(6)")
    @ApiModelProperty(value = "合并时间", example = "2026-02-09T15:00:00")
    private LocalDateTime mergeTime;

    @Column(name = "source_receipt_ids", columnDefinition = "TEXT")
    @ApiModelProperty(value = "源仓单ID列表（JSON格式）", example = "[\"id1\",\"id2\",\"id3\"]")
    private String sourceReceiptIds;

    // ==================== 货物类型字段（1个） ✨ 新增 ====================

    @Column(name = "goods_type", length = 100)
    @ApiModelProperty(value = "货物类型/分类", example = "建材-钢材-螺纹钢")
    private String goodsType;

    // ==================== 入库日期别名（1个） ✨ 新增 ====================

    @ApiModelProperty(value = "入库日期（与storageDate相同，为兼容性添加）", example = "2026-01-26T10:00:00")
    private LocalDateTime warehouseEntryDate;

    /**
     * 获取入库日期（别名方法，与storageDate相同）
     */
    public LocalDateTime getWarehouseEntryDate() {
        return this.storageDate;
    }

    /**
     * 设置入库日期（同时设置storageDate）
     */
    public void setWarehouseEntryDate(LocalDateTime warehouseEntryDate) {
        this.storageDate = warehouseEntryDate;
    }

    // ==================== 作废相关字段 (5个字段) ✨ 新增 ====================

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    @ApiModelProperty(value = "作废原因", example = "货物质量问题")
    private String cancelReason;

    @Column(name = "cancel_type", length = 50)
    @ApiModelProperty(value = "作废类型", example = "QUALITY_ISSUE")
    private String cancelType;

    @Column(name = "cancel_time", columnDefinition = "DATETIME(6)")
    @ApiModelProperty(value = "作废时间", example = "2026-02-02T11:00:00")
    private LocalDateTime cancelTime;

    @Column(name = "cancelled_by", length = 36)
    @ApiModelProperty(value = "作废操作人ID", example = "admin-uuid-001")
    private String cancelledBy;

    @Column(name = "reference_no", length = 100)
    @ApiModelProperty(value = "参考编号（如法律文书号）", example = "2026民初字第001号")
    private String referenceNo;

    // ==================== 审计 (6个字段) ====================

    @Column(name = "created_at", nullable = false)
    @ApiModelProperty(value = "创建时间", hidden = true)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @ApiModelProperty(value = "更新时间", hidden = true)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 50)
    @ApiModelProperty(value = "创建人", hidden = true, example = "admin")
    private String createdBy;

    @Column(name = "updated_by", length = 50)
    @ApiModelProperty(value = "更新人", hidden = true)
    private String updatedBy;

    @Column(name = "deleted_at")
    @ApiModelProperty(value = "软删除时间", hidden = true)
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by", length = 50)
    @ApiModelProperty(value = "删除人", hidden = true)
    private String deletedBy;

    // ==================== 关联关系 ====================

    /**
     * 仓储企业
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", insertable = false, updatable = false)
    @JsonIgnore
    @ApiModelProperty(hidden = true)
    private Enterprise warehouse;

    /**
     * 货主企业
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", insertable = false, updatable = false)
    @JsonIgnore
    @ApiModelProperty(hidden = true)
    private Enterprise owner;

    /**
     * 货主企业操作人
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_operator_id", insertable = false, updatable = false)
    @JsonIgnore
    @ApiModelProperty(hidden = true)
    private User ownerOperator;

    /**
     * 仓储方操作人
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_operator_id", insertable = false, updatable = false)
    @JsonIgnore
    @ApiModelProperty(hidden = true)
    private User warehouseOperator;

    /**
     * 父仓单（用于拆分）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_receipt_id", insertable = false, updatable = false)
    @JsonIgnore
    @ApiModelProperty(hidden = true)
    private ElectronicWarehouseReceipt parentReceipt;

    /**
     * 子仓单列表（用于拆分）
     */
    @OneToMany(mappedBy = "parentReceipt", fetch = FetchType.LAZY)
    @JsonIgnore
    @ApiModelProperty(hidden = true)
    private java.util.List<ElectronicWarehouseReceipt> childReceipts;

    /**
     * 背书链列表
     */
    @OneToMany(mappedBy = "receipt", fetch = FetchType.LAZY)
    @JsonIgnore
    @ApiModelProperty(hidden = true)
    private java.util.List<EwrEndorsementChain> endorsementChain;

    // ==================== 生命周期回调 ====================

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (receiptStatus == null) {
            receiptStatus = ReceiptStatus.DRAFT;
        }
        if (isFinanced == null) {
            isFinanced = false;
        }
        if (endorsementCount == null) {
            endorsementCount = 0;
        }
        if (blockchainStatus == null) {
            blockchainStatus = BlockchainStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ==================== 枚举定义 ====================

    /**
     * 仓单状态枚举
     */
    public enum ReceiptStatus {
        DRAFT,            // 草稿
        PENDING_ONCHAIN,  // 待上链（审核通过，正在上链中）
        NORMAL,           // 正常（已审核且已上链）
        ONCHAIN_FAILED,   // 上链失败（审核通过但上链失败，可重试）
        PLEDGED,          // 已质押
        TRANSFERRED,      // 已转让
        FROZEN,           // 已冻结
        SPLITTING,        // 拆分中（拆分申请已提交，正在审核）
        SPLIT,            // 已拆分（拆分完成，父仓单状态）
        MERGING,          // 合并中（合并申请已提交，正在审核）
        MERGED,           // 已合并（合并完成，源仓单状态）
        CANCELLING,       // 作废中 ✨ 新增（作废申请已提交，正在审核）
        CANCELLED,        // 已作废（作废完成）
        EXPIRED,          // 已过期
        DELIVERED         // 已提货
    }

    /**
     * 区块链状态枚举
     */
    public enum BlockchainStatus {
        PENDING,         // 待上链
        SYNCED,          // 已同步
        FAILED,          // 上链失败
        VERIFIED         // 已验证
    }
}
