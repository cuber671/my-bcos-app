package com.fisco.app.dto.warehouse;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import javax.validation.constraints.*;

/**
 * 更新电子仓单请求DTO
 */
@Data
@ApiModel(value = "更新电子仓单请求", description = "更新电子仓单请求参数")
public class ElectronicWarehouseReceiptUpdateRequest {

    @ApiModelProperty(value = "仓单ID", required = true, example = "ewr-uuid-001")
    @NotBlank(message = "仓单ID不能为空")
    @Size(max = 36, message = "仓单ID长度不能超过36")
    private String id;

    // ==================== 货物信息（可更新） ====================

    @ApiModelProperty(value = "货物数量", example = "1000.00")
    @DecimalMin(value = "0.01", message = "货物数量必须大于0")
    @Digits(integer = 18, fraction = 2, message = "货物数量格式不正确")
    private BigDecimal quantity;

    @ApiModelProperty(value = "单价（元）", example = "4500.00")
    @DecimalMin(value = "0.01", message = "单价必须大于0")
    @Digits(integer = 18, fraction = 2, message = "单价格式不正确")
    private BigDecimal unitPrice;

    @ApiModelProperty(value = "货物总价值（元）", example = "4500000.00")
    @DecimalMin(value = "0", message = "货物总价值不能为负数")
    @Digits(integer = 18, fraction = 2, message = "货物总价值格式不正确")
    private BigDecimal totalValue;

    // ==================== 仓储信息（可更新） ====================

    @ApiModelProperty(value = "仓库详细地址", example = "上海市浦东新区XX路XX号")
    @Size(max = 500, message = "仓库地址长度不能超过500")
    private String warehouseLocation;

    @ApiModelProperty(value = "存储位置", example = "A区03栋12排5层货架")
    @Size(max = 200, message = "存储位置长度不能超过200")
    private String storageLocation;

    @ApiModelProperty(value = "仓单有效期", example = "2026-07-26T23:59:59")
    private LocalDateTime expiryDate;

    // ==================== 状态管理（可更新） ====================

    @ApiModelProperty(value = "仓单状态", example = "NORMAL", notes = "DRAFT-草稿, NORMAL-正常, PLEDGED-已质押, TRANSFERRED-已转让, FROZEN-已冻结, EXPIRED-已过期, DELIVERED-已提货, CANCELLED-已取消")
    @Pattern(regexp = "^(DRAFT|NORMAL|PLEDGED|TRANSFERRED|FROZEN|EXPIRED|DELIVERED|CANCELLED)$", message = "仓单状态不正确")
    private String receiptStatus;

    // ==================== 融资信息（可更新） ====================

    @ApiModelProperty(value = "是否已融资", example = "true")
    private Boolean isFinanced;

    @ApiModelProperty(value = "融资金额（元）", example = "4000000.00")
    @Digits(integer = 18, fraction = 2, message = "融资金额格式不正确")
    private BigDecimal financeAmount;

    @ApiModelProperty(value = "融资利率（基点）", example = "500")
    private Integer financeRate;

    @ApiModelProperty(value = "融资日期", example = "2026-01-26T14:00:00")
    private LocalDateTime financeDate;

    @ApiModelProperty(value = "资金方地址", example = "0x9876543210987654321098765432109876543210")
    @Size(min = 42, max = 42, message = "资金方地址必须是42位")
    @Pattern(regexp = "^0x[a-fA-F0-9]{40}$", message = "资金方地址格式不正确")
    private String financierAddress;

    @ApiModelProperty(value = "质押合同编号", example = "CONTRACT20260126001")
    @Size(max = 64, message = "质押合同编号长度不能超过64")
    private String pledgeContractNo;

    // ==================== 区块链信息（可更新） ====================

    @ApiModelProperty(value = "区块链交易哈希", example = "0x1234567890abcdef...")
    @Size(max = 66, message = "交易哈希长度不能超过66")
    private String txHash;

    @ApiModelProperty(value = "区块链上链状态", example = "SYNCED", notes = "PENDING-待上链, SYNCED-已同步, FAILED-上链失败, VERIFIED-已验证")
    @Pattern(regexp = "^(PENDING|SYNCED|FAILED|VERIFIED)$", message = "区块链状态不正确")
    private String blockchainStatus;

    @ApiModelProperty(value = "区块高度", example = "12345")
    private Long blockNumber;

    @ApiModelProperty(value = "区块链时间戳", example = "2026-01-26T10:05:00")
    private LocalDateTime blockchainTimestamp;

    // ==================== 其他信息（可更新） ====================

    @ApiModelProperty(value = "备注信息", example = "备注：货物已更新为一级品")
    @Size(max = 1000, message = "备注长度不能超过1000")
    private String remarks;
}
