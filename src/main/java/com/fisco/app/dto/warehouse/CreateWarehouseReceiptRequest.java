package com.fisco.app.dto.warehouse;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.springframework.lang.NonNull;

import java.time.LocalDateTime;

import javax.validation.Valid;

/**
 * 创建仓单请求DTO
 */
@Data
@ApiModel(value = "创建仓单请求", description = "用于创建仓单的请求参数")
@Schema(name = "创建仓单请求")
public class CreateWarehouseReceiptRequest {

    @NonNull
    @NotBlank(message = "仓单ID不能为空")
    @ApiModelProperty(value = "仓单ID（UUID格式）", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
    private String id;

    @NotBlank(message = "仓库地址不能为空")
    @ApiModelProperty(value = "仓库地址", required = true, example = "0x567890abcdef1234")
    private String warehouseAddress;

    @NotNull(message = "货物信息不能为空")
    @Valid
    @ApiModelProperty(value = "货物信息", required = true)
    private GoodsInfo goods;

    @NotBlank(message = "仓库位置不能为空")
    @ApiModelProperty(value = "仓库物理位置", required = true, example = "上海市浦东新区XX仓库")
    private String warehouseLocation;

    @NotNull(message = "入库日期不能为空")
    @ApiModelProperty(value = "入库日期", required = true, example = "2024-01-13T10:00:00")
    private LocalDateTime storageDate;

    @NotNull(message = "过期日期不能为空")
    @ApiModelProperty(value = "过期日期", required = true, example = "2024-07-13T10:00:00")
    private LocalDateTime expiryDate;
}
