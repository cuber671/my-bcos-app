package com.fisco.app.dto.warehouse;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import javax.validation.constraints.*;

/**
 * 创建电子仓单请求DTO
 */
@Data
@ApiModel(value = "创建电子仓单请求", description = "创建电子仓单请求参数")
public class ElectronicWarehouseReceiptCreateRequest {

    // ==================== 基础信息 ====================

    @ApiModelProperty(value = "仓储企业ID", required = true, example = "warehouse-uuid-001")
    @NotBlank(message = "仓储企业ID不能为空")
    @Size(max = 36, message = "仓储企业ID长度不能超过36")
    private String warehouseId;

    @ApiModelProperty(value = "货主企业ID", required = true, example = "owner-uuid-001")
    @NotBlank(message = "货主企业ID不能为空")
    @Size(max = 36, message = "货主企业ID长度不能超过36")
    private String ownerId;

    @ApiModelProperty(value = "仓单编号", required = true, example = "EWR20260126000001")
    @NotBlank(message = "仓单编号不能为空")
    @Size(max = 64, message = "仓单编号长度不能超过64")
    private String receiptNo;

    // ==================== 企业和地址信息 ====================

    @ApiModelProperty(value = "仓储方区块链地址", required = true, example = "0x1234567890abcdef1234567890abcdef12345678")
    @NotBlank(message = "仓储方地址不能为空")
    @Size(min = 42, max = 42, message = "仓储方地址必须是42位")
    @Pattern(regexp = "^0x[a-fA-F0-9]{40}$", message = "仓储方地址格式不正确")
    private String warehouseAddress;

    @ApiModelProperty(value = "仓储方名称", example = "XX仓储有限公司")
    @Size(max = 255, message = "仓储方名称长度不能超过255")
    private String warehouseName;

    @ApiModelProperty(value = "货主区块链地址", required = true, example = "0xabcdabcdabcdabcdabcdabcdabcdabcdabcdabcd")
    @NotBlank(message = "货主地址不能为空")
    @Size(min = 42, max = 42, message = "货主地址必须是42位")
    @Pattern(regexp = "^0x[a-fA-F0-9]{40}$", message = "货主地址格式不正确")
    private String ownerAddress;

    @ApiModelProperty(value = "货主企业名称", example = "XX贸易有限公司")
    @Size(max = 255, message = "货主企业名称长度不能超过255")
    private String ownerName;

    @ApiModelProperty(value = "持单人地址（初始为货主）", required = true, example = "0xabcdabcdabcdabcdabcdabcdabcdabcdabcdabcd")
    @NotBlank(message = "持单人地址不能为空")
    @Size(min = 42, max = 42, message = "持单人地址必须是42位")
    @Pattern(regexp = "^0x[a-fA-F0-9]{40}$", message = "持单人地址格式不正确")
    private String holderAddress;

    // ==================== 货物信息 ====================

    @ApiModelProperty(value = "货物名称", required = true, example = "螺纹钢")
    @NotBlank(message = "货物名称不能为空")
    @Size(max = 255, message = "货物名称长度不能超过255")
    private String goodsName;

    @ApiModelProperty(value = "计量单位", required = true, example = "吨", notes = "吨、千克、立方米、平方米、件、箱等")
    @NotBlank(message = "计量单位不能为空")
    @Size(max = 20, message = "计量单位长度不能超过20")
    private String unit;

    @ApiModelProperty(value = "货物数量", required = true, example = "1000.00")
    @NotNull(message = "货物数量不能为空")
    @DecimalMin(value = "0.01", message = "货物数量必须大于0")
    @Digits(integer = 18, fraction = 2, message = "货物数量格式不正确")
    private BigDecimal quantity;

    @ApiModelProperty(value = "单价（元）", required = true, example = "4500.00")
    @NotNull(message = "单价不能为空")
    @DecimalMin(value = "0.01", message = "单价必须大于0")
    @Digits(integer = 18, fraction = 2, message = "单价格式不正确")
    private BigDecimal unitPrice;

    @ApiModelProperty(value = "货物总价值（元）", required = true, example = "4500000.00")
    @NotNull(message = "货物总价值不能为空")
    @DecimalMin(value = "0", message = "货物总价值不能为负数")
    @Digits(integer = 18, fraction = 2, message = "货物总价值格式不正确")
    private BigDecimal totalValue;

    @ApiModelProperty(value = "市场参考价格（元）", example = "4600.00", notes = "用于评估当前市场价值，可定期更新")
    @DecimalMin(value = "0", message = "市场价格不能为负数")
    @Digits(integer = 18, fraction = 2, message = "市场价格格式不正确")
    private BigDecimal marketPrice;

    // ==================== 仓储信息 ====================

    @ApiModelProperty(value = "仓库详细地址", example = "上海市浦东新区XX路XX号")
    @Size(max = 500, message = "仓库地址长度不能超过500")
    private String warehouseLocation;

    @ApiModelProperty(value = "存储位置", example = "A区03栋12排5层货架")
    @Size(max = 200, message = "存储位置长度不能超过200")
    private String storageLocation;

    @ApiModelProperty(value = "入库时间", required = true, example = "2026-01-26T10:00:00")
    @NotNull(message = "入库时间不能为空")
    private LocalDateTime storageDate;

    @ApiModelProperty(value = "仓单有效期", required = true, example = "2026-07-26T23:59:59")
    @NotNull(message = "有效期不能为空")
    private LocalDateTime expiryDate;

    // ==================== 状态管理 ====================

    @ApiModelProperty(value = "批次号", example = "BATCH20260126001")
    @Size(max = 64, message = "批次号长度不能超过64")
    private String batchNo;

    // ==================== 企业操作人信息 ====================

    @ApiModelProperty(value = "货主企业操作人ID", example = "user-uuid-001")
    @Size(max = 36, message = "操作人ID长度不能超过36")
    private String ownerOperatorId;

    @ApiModelProperty(value = "货主企业操作人姓名", example = "张三（货主企业业务员）")
    @Size(max = 100, message = "操作人姓名长度不能超过100")
    private String ownerOperatorName;

    @ApiModelProperty(value = "仓储方操作人ID", example = "user-uuid-002")
    @Size(max = 36, message = "操作人ID长度不能超过36")
    private String warehouseOperatorId;

    @ApiModelProperty(value = "仓储方操作人姓名", example = "李四（仓储方仓库管理员）")
    @Size(max = 100, message = "操作人姓名长度不能超过100")
    private String warehouseOperatorName;

    // ==================== 其他信息 ====================

    @ApiModelProperty(value = "备注信息", example = "备注：货物为一级品")
    @Size(max = 1000, message = "备注长度不能超过1000")
    private String remarks;
}
