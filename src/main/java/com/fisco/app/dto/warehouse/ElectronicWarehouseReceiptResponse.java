package com.fisco.app.dto.warehouse;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fisco.app.entity.warehouse.ElectronicWarehouseReceipt;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 电子仓单响应DTO
 */
@Data
@ApiModel(value = "电子仓单响应", description = "电子仓单信息响应")
public class ElectronicWarehouseReceiptResponse {

    // ==================== 基础信息 ====================

    @ApiModelProperty(value = "仓单ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String id;

    @ApiModelProperty(value = "仓单编号", example = "EWR20260126000001")
    private String receiptNo;

    @ApiModelProperty(value = "仓储企业ID", example = "warehouse-uuid-001")
    private String warehouseId;

    @ApiModelProperty(value = "仓储方区块链地址", example = "0x1234567890abcdef1234567890abcdef12345678")
    private String warehouseAddress;

    @ApiModelProperty(value = "仓储方名称", example = "XX仓储有限公司")
    private String warehouseName;

    @ApiModelProperty(value = "货主企业ID", example = "owner-uuid-001")
    private String ownerId;

    @ApiModelProperty(value = "货主区块链地址", example = "0xabcdabcdabcdabcdabcdabcdabcdabcdabcdabcd")
    private String ownerAddress;

    @ApiModelProperty(value = "货主企业名称", example = "XX贸易有限公司")
    private String ownerName;

    @ApiModelProperty(value = "持单人地址", example = "0xabcdabcdabcdabcdabcdabcdabcdabcdabcdabcd")
    private String holderAddress;

    // ==================== 货物信息 ====================

    @ApiModelProperty(value = "货物名称", example = "螺纹钢")
    private String goodsName;

    @ApiModelProperty(value = "计量单位", example = "吨")
    private String unit;

    @ApiModelProperty(value = "货物数量", example = "1000.00")
    private BigDecimal quantity;

    @ApiModelProperty(value = "单价（元）", example = "4500.00")
    private BigDecimal unitPrice;

    @ApiModelProperty(value = "货物总价值（元）", example = "4500000.00")
    private BigDecimal totalValue;

    @ApiModelProperty(value = "市场参考价格（元）", example = "4600.00")
    private BigDecimal marketPrice;

    // ==================== 仓储信息 ====================

    @ApiModelProperty(value = "仓库详细地址", example = "上海市浦东新区XX路XX号")
    private String warehouseLocation;

    @ApiModelProperty(value = "存储位置", example = "A区03栋12排5层货架")
    private String storageLocation;

    @ApiModelProperty(value = "入库时间", example = "2026-01-26T10:00:00")
    private LocalDateTime storageDate;

    @ApiModelProperty(value = "仓单有效期", example = "2026-07-26T23:59:59")
    private LocalDateTime expiryDate;

    @ApiModelProperty(value = "实际提货时间", example = "2026-06-15T14:30:00")
    private LocalDateTime actualDeliveryDate;

    @ApiModelProperty(value = "提货人姓名", example = "张三")
    private String deliveryPersonName;

    @ApiModelProperty(value = "提货人联系方式", example = "13800138000")
    private String deliveryPersonContact;

    @ApiModelProperty(value = "提货单号", example = "DEL202601150001")
    private String deliveryNo;

    @ApiModelProperty(value = "运输车牌号", example = "沪A12345")
    private String vehiclePlate;

    @ApiModelProperty(value = "司机姓名", example = "李四")
    private String driverName;

    // ==================== 状态管理 ====================

    @ApiModelProperty(value = "仓单状态", example = "NORMAL")
    private String receiptStatus;

    @ApiModelProperty(value = "仓单状态描述", example = "正常")
    private String receiptStatusDesc;

    @ApiModelProperty(value = "父仓单ID")
    private String parentReceiptId;

    @ApiModelProperty(value = "批次号", example = "BATCH20260126001")
    private String batchNo;

    // ==================== 企业和操作人信息 ====================

    @ApiModelProperty(value = "货主企业操作人ID", example = "user-uuid-001")
    private String ownerOperatorId;

    @ApiModelProperty(value = "货主企业操作人姓名", example = "张三（货主企业业务员）")
    private String ownerOperatorName;

    @ApiModelProperty(value = "仓储方操作人ID", example = "user-uuid-002")
    private String warehouseOperatorId;

    @ApiModelProperty(value = "仓储方操作人姓名", example = "李四（仓储方仓库管理员）")
    private String warehouseOperatorName;

    // ==================== 融资信息 ====================

    @ApiModelProperty(value = "是否已融资", example = "false")
    private Boolean isFinanced;

    @ApiModelProperty(value = "融资金额（元）", example = "4000000.00")
    private BigDecimal financeAmount;

    @ApiModelProperty(value = "融资利率（基点）", example = "500")
    private Integer financeRate;

    @ApiModelProperty(value = "融资日期", example = "2026-01-26T14:00:00")
    private LocalDateTime financeDate;

    @ApiModelProperty(value = "资金方地址", example = "0x9876543210987654321098765432109876543210")
    private String financierAddress;

    @ApiModelProperty(value = "质押合同编号", example = "CONTRACT20260126001")
    private String pledgeContractNo;

    // ==================== 背书统计 ====================

    @ApiModelProperty(value = "背书次数", example = "3")
    private Integer endorsementCount;

    @ApiModelProperty(value = "最后背书时间", example = "2026-01-26T15:30:00")
    private LocalDateTime lastEndorsementDate;

    @ApiModelProperty(value = "当前持单人（冗余）")
    private String currentHolder;

    // ==================== 区块链信息 ====================

    @ApiModelProperty(value = "区块链交易哈希", example = "0x1234567890abcdef...")
    private String txHash;

    @ApiModelProperty(value = "区块链上链状态", example = "SYNCED")
    private String blockchainStatus;

    @ApiModelProperty(value = "区块链状态描述", example = "已同步")
    private String blockchainStatusDesc;

    @ApiModelProperty(value = "区块高度", example = "12345")
    private Long blockNumber;

    @ApiModelProperty(value = "区块链时间戳", example = "2026-01-26T10:05:00")
    private LocalDateTime blockchainTimestamp;

    // ==================== 其他信息 ====================

    @ApiModelProperty(value = "备注信息", example = "备注：货物为一级品")
    private String remarks;

    // ==================== 审计信息 ====================

    @ApiModelProperty(value = "创建时间", example = "2026-01-26T10:00:00")
    private LocalDateTime createdAt;

    @ApiModelProperty(value = "更新时间", example = "2026-01-26T11:00:00")
    private LocalDateTime updatedAt;

    @ApiModelProperty(value = "创建人", example = "admin")
    private String createdBy;

    @ApiModelProperty(value = "更新人", example = "admin")
    private String updatedBy;

    // ==================== 拆分相关字段 ====================

    @ApiModelProperty(value = "拆分时间", example = "2026-02-02T10:30:00")
    private LocalDateTime splitTime;

    @ApiModelProperty(value = "子仓单数量", example = "2")
    private Integer splitCount;

    // ==================== 作废相关字段 ====================

    @ApiModelProperty(value = "作废原因", example = "货物质量问题")
    private String cancelReason;

    @ApiModelProperty(value = "作废类型", example = "QUALITY_ISSUE")
    private String cancelType;

    @ApiModelProperty(value = "作废时间", example = "2026-02-02T11:00:00")
    private LocalDateTime cancelTime;

    @ApiModelProperty(value = "作废操作人ID", example = "admin-uuid-001")
    private String cancelledBy;

    @ApiModelProperty(value = "参考编号（如法律文书号）", example = "2026民初字第001号")
    private String referenceNo;

    // ==================== 软删除相关字段 ====================

    @ApiModelProperty(value = "软删除时间")
    private LocalDateTime deletedAt;

    @ApiModelProperty(value = "删除人")
    private String deletedBy;

    // ==================== 辅助方法 ====================

    /**
     * 从实体转换为响应DTO
     */
    public static ElectronicWarehouseReceiptResponse fromEntity(ElectronicWarehouseReceipt entity) {
        ElectronicWarehouseReceiptResponse response = new ElectronicWarehouseReceiptResponse();

        // 基础信息
        response.setId(entity.getId());
        response.setReceiptNo(entity.getReceiptNo());
        response.setWarehouseId(entity.getWarehouseId());
        response.setWarehouseAddress(entity.getWarehouseAddress());
        response.setWarehouseName(entity.getWarehouseName());
        response.setOwnerId(entity.getOwnerId());
        response.setOwnerAddress(entity.getOwnerAddress());
        response.setOwnerName(entity.getOwnerName());
        response.setHolderAddress(entity.getHolderAddress());

        // 货物信息
        response.setGoodsName(entity.getGoodsName());
        response.setUnit(entity.getUnit());
        response.setQuantity(entity.getQuantity());
        response.setUnitPrice(entity.getUnitPrice());
        response.setTotalValue(entity.getTotalValue());
        response.setMarketPrice(entity.getMarketPrice());

        // 仓储信息
        response.setWarehouseLocation(entity.getWarehouseLocation());
        response.setStorageLocation(entity.getStorageLocation());
        response.setStorageDate(entity.getStorageDate());
        response.setExpiryDate(entity.getExpiryDate());
        response.setActualDeliveryDate(entity.getActualDeliveryDate());
        response.setDeliveryPersonName(entity.getDeliveryPersonName());
        response.setDeliveryPersonContact(entity.getDeliveryPersonContact());
        response.setDeliveryNo(entity.getDeliveryNo());
        response.setVehiclePlate(entity.getVehiclePlate());
        response.setDriverName(entity.getDriverName());

        // 状态管理
        response.setReceiptStatus(entity.getReceiptStatus().name());
        response.setReceiptStatusDesc(getReceiptStatusDesc(entity.getReceiptStatus()));
        response.setParentReceiptId(entity.getParentReceiptId());
        response.setBatchNo(entity.getBatchNo());

        // 企业和操作人信息
        response.setOwnerOperatorId(entity.getOwnerOperatorId());
        response.setOwnerOperatorName(entity.getOwnerOperatorName());
        response.setWarehouseOperatorId(entity.getWarehouseOperatorId());
        response.setWarehouseOperatorName(entity.getWarehouseOperatorName());

        // 融资信息
        response.setIsFinanced(entity.getIsFinanced());
        response.setFinanceAmount(entity.getFinanceAmount());
        response.setFinanceRate(entity.getFinanceRate());
        response.setFinanceDate(entity.getFinanceDate());
        response.setFinancierAddress(entity.getFinancierAddress());
        response.setPledgeContractNo(entity.getPledgeContractNo());

        // 背书统计
        response.setEndorsementCount(entity.getEndorsementCount());
        response.setLastEndorsementDate(entity.getLastEndorsementDate());
        response.setCurrentHolder(entity.getCurrentHolder());

        // 区块链信息
        response.setTxHash(entity.getTxHash());
        response.setBlockchainStatus(entity.getBlockchainStatus().name());
        response.setBlockchainStatusDesc(getBlockchainStatusDesc(entity.getBlockchainStatus()));
        response.setBlockNumber(entity.getBlockNumber());
        response.setBlockchainTimestamp(entity.getBlockchainTimestamp());

        // 其他信息
        response.setRemarks(entity.getRemarks());

        // 审计信息
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        response.setCreatedBy(entity.getCreatedBy());
        response.setUpdatedBy(entity.getUpdatedBy());

        return response;
    }

    /**
     * 获取仓单状态描述
     */
    private static String getReceiptStatusDesc(ElectronicWarehouseReceipt.ReceiptStatus status) {
        switch (status) {
            case DRAFT:
                return "草稿";
            case PENDING_ONCHAIN:
                return "待上链";
            case NORMAL:
                return "正常";
            case ONCHAIN_FAILED:
                return "上链失败";
            case PLEDGED:
                return "已质押";
            case TRANSFERRED:
                return "已转让";
            case FROZEN:
                return "已冻结";
            case EXPIRED:
                return "已过期";
            case DELIVERED:
                return "已提货";
            case CANCELLED:
                return "已取消";
            default:
                return "未知";
        }
    }

    /**
     * 获取区块链状态描述
     */
    private static String getBlockchainStatusDesc(ElectronicWarehouseReceipt.BlockchainStatus status) {
        switch (status) {
            case PENDING:
                return "待上链";
            case SYNCED:
                return "已同步";
            case FAILED:
                return "上链失败";
            case VERIFIED:
                return "已验证";
            default:
                return "未知";
        }
    }
}
