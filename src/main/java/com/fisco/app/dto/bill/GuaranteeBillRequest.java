package com.fisco.app.dto.bill;

import com.fisco.app.entity.bill.BillGuarantee;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 票据担保请求DTO
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-09
 * @version 1.0
 */
@Data
@ApiModel(value = "票据担保请求", description = "第三方为票据提供担保的请求参数")
public class GuaranteeBillRequest {

    @NotNull(message = "担保类型不能为空")
    @ApiModelProperty(value = "担保类型", required = true, notes = "FULL-全额担保, PARTIAL-部分担保, JOINT-联合担保", example = "FULL")
    private BillGuarantee.GuaranteeType guaranteeType;

    @NotNull(message = "担保金额不能为空")
    @ApiModelProperty(value = "担保金额", required = true, example = "1000000.00")
    private BigDecimal guaranteeAmount;

    @ApiModelProperty(value = "担保费率（%）", example = "2.5", notes = "可选，系统根据风险评估自动计算")
    private BigDecimal guaranteeRate;

    @ApiModelProperty(value = "担保期限（天）", example = "90", notes = "可选，默认为票据剩余期限")
    private Integer guaranteePeriod;

    @Size(max = 1000, message = "担保条件不能超过1000个字符")
    @ApiModelProperty(value = "担保条件", example = "担保人需在票据到期日前履行担保义务")
    private String guaranteeConditions;

    @ApiModelProperty(value = "反担保措施JSON", example = "{\"type\":\"pledge\",\"details\":\"房产抵押\"}")
    private String collateralInfo;

    @Size(max = 500, message = "备注不能超过500个字符")
    @ApiModelProperty(value = "备注")
    private String remarks;
}
