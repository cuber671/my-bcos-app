package com.fisco.app.dto.warehouse;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * 货物信息DTO
 */
@Data
@ApiModel(value = "货物信息", description = "仓单货物的详细信息")
@Schema(name = "货物信息")
public class GoodsInfo {

    @NotBlank(message = "货物名称不能为空")
    @ApiModelProperty(value = "货物名称", required = true, example = "钢材")
    private String goodsName;

    @ApiModelProperty(value = "货物类型", example = "建材")
    private String goodsType;

    @NotNull(message = "数量不能为空")
    @Positive(message = "数量必须大于0")
    @ApiModelProperty(value = "数量", required = true, example = "100.00")
    private BigDecimal quantity;

    @NotBlank(message = "单位不能为空")
    @ApiModelProperty(value = "单位", required = true, example = "吨")
    private String unit;

    @NotNull(message = "单价不能为空")
    @Positive(message = "单价必须大于0")
    @ApiModelProperty(value = "单价（分）", required = true, example = "5000.00")
    private BigDecimal unitPrice;

    @NotNull(message = "总价不能为空")
    @Positive(message = "总价必须大于0")
    @ApiModelProperty(value = "总价（分）", required = true, example = "500000.00")
    private BigDecimal totalPrice;

    @ApiModelProperty(value = "质量等级", example = "一级")
    private String quality;

    @ApiModelProperty(value = "产地", example = "上海")
    private String origin;
}
