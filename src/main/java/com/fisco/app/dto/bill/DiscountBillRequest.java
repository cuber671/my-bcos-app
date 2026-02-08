package com.fisco.app.dto.bill;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * 票据贴现请求DTO
 */
@Data
@ApiModel(value = "票据贴现请求", description = "用于票据贴现的请求参数")
@Schema(name = "票据贴现请求")
public class DiscountBillRequest {

    @NotBlank(message = "金融机构地址不能为空")
    @ApiModelProperty(value = "金融机构地址", required = true, example = "0x1234567890abcdef")
    private String financialInstitutionAddress;

    @NotNull(message = "贴现金额不能为空")
    @DecimalMin(value = "0.01", message = "贴现金额必须大于0")
    @ApiModelProperty(value = "贴现金额（元）", required = true, example = "980000.00")
    private BigDecimal discountAmount;

    @NotNull(message = "贴现率不能为空")
    @DecimalMin(value = "0.0001", message = "贴现率必须大于0")
    @ApiModelProperty(value = "贴现率（百分比，如 5.5 表示 5.5%）", required = true, example = "5.5")
    private BigDecimal discountRate;

    @ApiModelProperty(value = "贴现备注", example = "流动性贴现")
    private String remark;
}
