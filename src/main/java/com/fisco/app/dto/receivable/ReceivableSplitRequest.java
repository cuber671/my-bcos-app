package com.fisco.app.dto.receivable;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * 应收账款拆分请求DTO
 */
@Data
@ApiModel(value = "应收账款拆分请求", description = "用于拆分应收账款的请求参数")
public class ReceivableSplitRequest {

    @NotBlank(message = "应收账款ID不能为空")
    @ApiModelProperty(value = "应收账款ID", required = true, example = "REC-001")
    private String receivableId;

    @NotNull(message = "拆分数量不能为空")
    @Min(value = 2, message = "拆分数量必须大于等于2")
    @ApiModelProperty(value = "拆分数量", required = true, example = "3", notes = "最少拆分为2份")
    private Integer splitCount;

    @NotNull(message = "拆分方案不能为空")
    @ApiModelProperty(value = "拆分方案", required = true, notes = "EQUAL-等额拆分, CUSTOM-自定义金额")
    private SplitScheme splitScheme;

    @ApiModelProperty(value = "拆分明细（自定义方案时必填）", example = "[{\"amount\": 500000, \"ratio\": 0.5}, {\"amount\": 500000, \"ratio\": 0.5}]")
    private List<SplitDetail> splitDetails;

    @ApiModelProperty(value = "备注", example = "根据付款方要求拆分")
    private String remark;

    /**
     * 拆分方案枚举
     */
    public enum SplitScheme {
        EQUAL("等额拆分"),
        CUSTOM("自定义金额");

        private final String description;

        SplitScheme(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 拆分明细
     */
    @Data
    @ApiModel(value = "拆分明细")
    public static class SplitDetail {
        @ApiModelProperty(value = "金额（分）", required = true, example = "500000")
        private Long amount;

        @ApiModelProperty(value = "金额占比", required = true, example = "0.5")
        private BigDecimal ratio;

        @ApiModelProperty(value = "备注", example = "第一期")
        private String remark;
    }
}
