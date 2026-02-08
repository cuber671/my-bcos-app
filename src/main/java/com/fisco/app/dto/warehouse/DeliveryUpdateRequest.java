package com.fisco.app.dto.warehouse;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * 更新实际提货时间请求DTO
 */
@Data
@ApiModel(value = "更新实际提货时间请求", description = "记录货物实际提货时间")
public class DeliveryUpdateRequest {

    @ApiModelProperty(value = "实际提货时间", required = true, example = "2026-06-15T14:30:00", notes = "货物实际被提取的时间")
    @NotNull(message = "提货时间不能为空")
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

    @ApiModelProperty(value = "备注信息", example = "货物完好，手续齐全")
    private String remarks;
}
