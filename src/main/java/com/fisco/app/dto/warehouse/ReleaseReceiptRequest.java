package com.fisco.app.dto.warehouse;
import com.fisco.app.entity.pledge.ReleaseRecord;


import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 仓单释放请求DTO
 */
@Data
@ApiModel(value = "仓单释放请求", description = "用于仓单质押释放的请求参数")
@Schema(name = "仓单释放请求")
public class ReleaseReceiptRequest {

    @NotNull(message = "释放类型不能为空")
    @ApiModelProperty(value = "释放类型", required = true, example = "FULL_REPAYMENT")
    private ReleaseRecord.ReleaseType releaseType;

    @ApiModelProperty(value = "还款金额（全额还款时可选）", example = "1000000.00")
    private BigDecimal repaymentAmount;

    @ApiModelProperty(value = "利息金额", example = "50000.00")
    private BigDecimal interestAmount;

    @ApiModelProperty(value = "释放备注", example = "全额还款，解除质押")
    private String remark;
}
