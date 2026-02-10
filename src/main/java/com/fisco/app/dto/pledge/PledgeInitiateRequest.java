package com.fisco.app.dto.pledge;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

import javax.validation.constraints.*;

/**
 * 质押发起请求DTO
 */
@Data
@ApiModel(value = "质押发起请求", description = "货主发起仓单质押的请求参数")
public class PledgeInitiateRequest {

    @ApiModelProperty(value = "仓单ID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
    @NotBlank(message = "仓单ID不能为空")
    @Size(max = 100, message = "仓单ID长度不能超过100个字符")
    private String receiptId;

    @ApiModelProperty(value = "金融机构ID", required = true, example = "fin-001")
    @NotBlank(message = "金融机构ID不能为空")
    @Size(max = 50, message = "金融机构ID长度不能超过50个字符")
    private String financialInstitutionId;

    @ApiModelProperty(value = "质押金额（元）", required = true, example = "100000.00", notes = "范围: 10,000 - 1,000,000,000")
    @NotNull(message = "质押金额不能为空")
    @DecimalMin(value = "10000", message = "质押金额不能低于10,000元")
    @DecimalMax(value = "1000000000", message = "质押金额不能超过1亿元")
    @Digits(integer = 12, fraction = 2, message = "质押金额格式不正确，最多2位小数")
    private BigDecimal pledgeAmount;

    @ApiModelProperty(value = "质押率（0-1之间）", required = true, example = "0.70", notes = "如0.7表示70%，范围: 10% - 90%")
    @NotNull(message = "质押率不能为空")
    @DecimalMin(value = "0.10", message = "质押率不能低于10%")
    @DecimalMax(value = "0.90", message = "质押率不能超过90%")
    @Digits(integer = 1, fraction = 4, message = "质押率格式不正确，最多4位小数")
    private BigDecimal pledgeRatio;

    @ApiModelProperty(value = "质押开始日期", required = true, example = "2026-01-27", notes = "必须是今天或未来日期")
    @NotNull(message = "质押开始日期不能为空")
    @Future(message = "质押开始日期必须是今天或未来日期")
    private LocalDate pledgeStartDate;

    @ApiModelProperty(value = "质押结束日期", required = true, example = "2026-04-27", notes = "必须是未来日期")
    @NotNull(message = "质押结束日期不能为空")
    @Future(message = "质押结束日期必须是未来日期")
    private LocalDate pledgeEndDate;

    @ApiModelProperty(value = "质押原因", example = "用于原材料采购资金周转")
    @Size(max = 500, message = "质押原因长度不能超过500个字符")
    private String pledgeReason;

    public void setPledgeStartDate(LocalDate pledgeStartDate) {
        this.pledgeStartDate = pledgeStartDate;
        // 验证结束日期必须晚于开始日期
        if (pledgeEndDate != null && pledgeEndDate.isBefore(pledgeStartDate)) {
            throw new IllegalArgumentException("质押结束日期必须晚于开始日期");
        }
    }

    public void setPledgeEndDate(LocalDate pledgeEndDate) {
        this.pledgeEndDate = pledgeEndDate;
        // 验证结束日期必须晚于开始日期
        if (pledgeStartDate != null && pledgeEndDate.isBefore(pledgeStartDate)) {
            throw new IllegalArgumentException("质押结束日期必须晚于开始日期");
        }
    }
}
