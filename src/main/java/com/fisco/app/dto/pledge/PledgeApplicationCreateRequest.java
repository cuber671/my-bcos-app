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
 * 质押申请创建请求DTO
 */
@Data
@ApiModel(value = "质押申请创建请求", description = "货主发起仓单质押申请的请求参数")
public class PledgeApplicationCreateRequest {

    @ApiModelProperty(value = "仓单ID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
    @NotBlank(message = "仓单ID不能为空")
    private String receiptId;

    @ApiModelProperty(value = "金融机构ID", required = true, example = "fin-001")
    @NotBlank(message = "金融机构ID不能为空")
    private String financialInstitutionId;

    @ApiModelProperty(value = "质押金额（元）", required = true, example = "100000.00")
    @NotNull(message = "质押金额不能为空")
    @DecimalMin(value = "0.01", message = "质押金额必须大于0")
    private BigDecimal pledgeAmount;

    @ApiModelProperty(value = "质押率（0-1之间）", required = true, example = "0.70", notes = "如0.7表示70%")
    @NotNull(message = "质押率不能为空")
    @DecimalMin(value = "0.01", message = "质押率必须大于0")
    @DecimalMax(value = "1.00", message = "质押率不能超过100%")
    private BigDecimal pledgeRatio;

    @ApiModelProperty(value = "质押开始日期", required = true, example = "2026-01-27")
    @NotNull(message = "质押开始日期不能为空")
    private LocalDate pledgeStartDate;

    @ApiModelProperty(value = "质押结束日期", required = true, example = "2026-04-27")
    @NotNull(message = "质押结束日期不能为空")
    private LocalDate pledgeEndDate;

    @ApiModelProperty(value = "备注", example = "用于原材料采购资金周转")
    private String remark;

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
