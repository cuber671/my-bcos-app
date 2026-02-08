package com.fisco.app.dto.bill;


import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.Future;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import javax.validation.constraints.*;

/**
 * 票据投资请求DTO
 * 用于金融机构投资票据
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-03
 */
@Data
@ApiModel(value = "票据投资请求", description = "金融机构通过票据池投资票据")
public class BillInvestRequest {

    @NotBlank(message = "票据ID不能为空")
    @ApiModelProperty(value = "票据ID", required = true, example = "bill-uuid-001")
    private String billId;

    @NotNull(message = "投资金额不能为空")
    @DecimalMin(value = "0.01", message = "投资金额必须大于0")
    @ApiModelProperty(value = "投资金额（元）", required = true, example = "950000.00")
    private BigDecimal investAmount;

    @NotNull(message = "投资利率不能为空")
    @DecimalMin(value = "0.01", message = "投资利率必须大于0")
    @ApiModelProperty(value = "投资利率（%）", required = true, example = "5.5")
    private BigDecimal investRate;

    @Future(message = "投资日期必须是未来时间")
    @ApiModelProperty(value = "投资日期（可选，默认为当前时间）")
    private LocalDateTime investDate;

    @ApiModelProperty(value = "投资备注", example = "看好该票据，决定投资")
    @Size(max = 500, message = "投资备注不能超过500个字符")
    private String investmentNotes;
}
