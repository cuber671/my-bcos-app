package com.fisco.app.dto.receivable;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 仓单拆分详情请求DTO
 */
@Data
@ApiModel(value = "仓单拆分详情", description = "单个子仓单的拆分详情")
public class SplitDetailRequest {

    @ApiModelProperty(value = "货物名称", required = true, example = "螺纹钢")
    @NotBlank(message = "货物名称不能为空")
    @Size(max = 255, message = "货物名称长度不能超过255")
    private String goodsName;

    @ApiModelProperty(value = "数量", required = true, example = "300.00")
    @NotNull(message = "数量不能为空")
    @DecimalMin(value = "0.01", message = "数量必须大于0")
    private BigDecimal quantity;

    @ApiModelProperty(value = "单价（元）", required = true, example = "4500.00")
    @NotNull(message = "单价不能为空")
    @DecimalMin(value = "0.01", message = "单价必须大于0")
    private BigDecimal unitPrice;

    @ApiModelProperty(value = "总价值（元）", required = true, example = "1350000.00")
    @NotNull(message = "总价值不能为空")
    @DecimalMin(value = "0.01", message = "总价值必须大于0")
    private BigDecimal totalValue;

    @ApiModelProperty(value = "存储位置", required = true, example = "A区03栋12排5层货架")
    @NotBlank(message = "存储位置不能为空")
    @Size(max = 200, message = "存储位置长度不能超过200")
    private String storageLocation;

    @ApiModelProperty(value = "计量单位", required = true, example = "吨")
    @NotBlank(message = "计量单位不能为空")
    @Size(max = 20, message = "计量单位长度不能超过20")
    private String unit;

    @ApiModelProperty(value = "备注", example = "一级品，存放于常温区")
    private String remarks;
}
