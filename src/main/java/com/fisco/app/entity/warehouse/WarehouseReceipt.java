package com.fisco.app.entity.warehouse;

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
 * 仓单实体类
 */
@Data
@Entity
@Table(name = "warehouse_receipt", indexes = {
    @Index(name = "idx_owner", columnList = "owner_address"),
    @Index(name = "idx_warehouse", columnList = "warehouse_address"),
    @Index(name = "idx_financier", columnList = "financial_institution"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_expiry_date", columnList = "expiry_date")
})
@ApiModel(value = "仓单", description = "仓单信息实体")
@Schema(name = "仓单")
public class WarehouseReceipt {

    @Id
    @Column(name = "id", nullable = false, unique = true, length = 36)
    @ApiModelProperty(value = "仓单ID（UUID格式）", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
    private String id;

    @Column(name = "owner_address", nullable = false, length = 42)
    @ApiModelProperty(value = "所有者地址", required = true, example = "0x1234567890abcdef1234567890abcdef12345678")
    private String ownerAddress;

    @Column(name = "warehouse_address", nullable = false, length = 42)
    @ApiModelProperty(value = "仓库地址", required = true, example = "0xabcdefabcdefabcdefabcdefabcdefabcdefabcd")
    private String warehouseAddress;

    @Column(name = "financial_institution", length = 42)
    @ApiModelProperty(value = "金融机构地址", example = "0x9876543210987654321098765432109876543210")
    private String financialInstitution;

    @Column(name = "goods_name", nullable = false)
    @ApiModelProperty(value = "货物名称", required = true, example = "钢材")
    private String goodsName;

    @Column(name = "goods_type", length = 100)
    @ApiModelProperty(value = "货物类型", example = "螺纹钢")
    private String goodsType;

    @Column(name = "quantity", nullable = false, precision = 20, scale = 2)
    @ApiModelProperty(value = "数量", required = true, example = "100.00")
    private BigDecimal quantity;

    @Column(name = "unit", nullable = false, length = 20)
    @ApiModelProperty(value = "单位", required = true, example = "吨")
    private String unit;

    @Column(name = "unit_price", nullable = false, precision = 20, scale = 2)
    @ApiModelProperty(value = "单价", required = true, example = "5000.00")
    private BigDecimal unitPrice;

    @Column(name = "total_price", nullable = false, precision = 20, scale = 2)
    @ApiModelProperty(value = "总价", required = true, example = "500000.00")
    private BigDecimal totalPrice;

    @Column(name = "quality", length = 50)
    @ApiModelProperty(value = "质量等级", example = "一级")
    private String quality;

    @Column(name = "origin")
    @ApiModelProperty(value = "产地", example = "上海")
    private String origin;

    @Column(name = "warehouse_location", length = 500)
    @ApiModelProperty(value = "仓库位置", example = "上海市宝山区仓库A区01号货架")
    private String warehouseLocation;

    @Column(name = "storage_date", nullable = false)
    @ApiModelProperty(value = "入库日期", required = true, example = "2024-01-01T00:00:00")
    private LocalDateTime storageDate;

    @Column(name = "expiry_date", nullable = false)
    @ApiModelProperty(value = "到期日期", required = true, example = "2024-12-31T23:59:59")
    private LocalDateTime expiryDate;

    @Column(name = "release_date")
    @ApiModelProperty(value = "释放日期", example = "2024-06-30T00:00:00")
    private LocalDateTime releaseDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @ApiModelProperty(value = "仓单状态", required = true, example = "CREATED", notes = "CREATED-已创建, VERIFIED-已验证, PLEDGED-已质押, FINANCED-已融资, RELEASED-已释放, LIQUIDATED-已清算, EXPIRED-已过期")
    private ReceiptStatus status = ReceiptStatus.CREATED;

    @Column(name = "pledge_amount", precision = 20, scale = 2)
    @ApiModelProperty(value = "质押金额", example = "400000.00")
    private BigDecimal pledgeAmount;

    @Column(name = "finance_amount", precision = 20, scale = 2)
    @ApiModelProperty(value = "融资金额", example = "350000.00")
    private BigDecimal financeAmount;

    @Column(name = "finance_rate")
    @ApiModelProperty(value = "融资利率（基点）", example = "500", notes = "500表示5%")
    private Integer financeRate;

    @Column(name = "finance_date")
    @ApiModelProperty(value = "融资日期", example = "2024-02-01T00:00:00")
    private LocalDateTime financeDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    @ApiModelProperty(value = "创建时间", required = true, example = "2024-01-01T10:00:00")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @ApiModelProperty(value = "更新时间", example = "2024-01-02T15:30:00")
    private LocalDateTime updatedAt;

    @Column(name = "tx_hash", length = 66)
    @ApiModelProperty(value = "区块链交易哈希", example = "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890")
    private String txHash;

    public enum ReceiptStatus {
        CREATED,      // 已创建
        VERIFIED,     // 已验证（仓库确认）
        PLEDGED,      // 已质押
        FINANCED,     // 已融资
        RELEASED,     // 已释放
        LIQUIDATED,   // 已清算
        EXPIRED       // 已过期
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
