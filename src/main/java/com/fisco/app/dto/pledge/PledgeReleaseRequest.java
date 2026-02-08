package com.fisco.app.dto.pledge;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 质押释放请求DTO
 */
@Data
@ApiModel(value = "质押释放请求", description = "还款后释放质押仓单的请求参数")
public class PledgeReleaseRequest {

    @ApiModelProperty(value = "仓单ID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
    @NotBlank(message = "仓单ID不能为空")
    private String receiptId;

    @ApiModelProperty(value = "背书ID", required = true, example = "b2c3d4e5-f6g7-8901-bcde-f23456789012")
    @NotBlank(message = "背书ID不能为空")
    private String endorsementId;

    /**
     * @deprecated 使用 endorsementId 替代
     */
    @Deprecated
    @ApiModelProperty(value = "质押申请ID（已废弃）", example = "1")
    private Long applicationId;

    @ApiModelProperty(value = "还款金额（元）", required = true, example = "101375.00", notes = "本金+利息")
    @NotNull(message = "还款金额不能为空")
    @DecimalMin(value = "0.01", message = "还款金额必须大于0")
    private BigDecimal repayAmount;

    @ApiModelProperty(value = "还款凭证URL", example = "https://example.com/receipt/123.pdf")
    private String transactionProof;

    @ApiModelProperty(value = "备注", example = "按期还款，申请释放质押")
    private String remark;
}
