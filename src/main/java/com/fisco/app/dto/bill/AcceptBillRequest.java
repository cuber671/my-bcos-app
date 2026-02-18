package com.fisco.app.dto.bill;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 票据承兑请求DTO
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-09
 * @version 1.0
 */
@Data
@ApiModel(value = "票据承兑请求", description = "承兑人确认承兑票据的请求参数")
public class AcceptBillRequest {

    @NotNull(message = "承兑类型不能为空")
    @ApiModelProperty(value = "承兑类型", required = true, notes = "FULL_ACCEPTANCE-全额承兑, PARTIAL_ACCEPTANCE-部分承兑", example = "FULL_ACCEPTANCE")
    private AcceptanceType acceptanceType;

    @ApiModelProperty(value = "部分承兑金额（部分承兑时必填）", example = "500000.00")
    private java.math.BigDecimal partialAmount;

    @Size(max = 500, message = "承兑备注不能超过500个字符")
    @ApiModelProperty(value = "承兑备注", example = "确认承兑该票据，同意到期付款")
    private String acceptanceRemarks;

    /**
     * 承兑类型枚举
     */
    public enum AcceptanceType {
        FULL_ACCEPTANCE,    // 全额承兑
        PARTIAL_ACCEPTANCE  // 部分承兑
    }
}
