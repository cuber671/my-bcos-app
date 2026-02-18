package com.fisco.app.dto.bill;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 票据拆分请求DTO
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-09
 * @version 1.0
 */
@Data
@ApiModel(value = "票据拆分请求", description = "将一笔票据拆分为多笔小额票据的请求参数")
public class SplitBillRequest {

    @NotNull(message = "拆分方案不能为空")
    @ApiModelProperty(value = "拆分方案", required = true, notes = "EQUAL-等额拆分, CUSTOM-自定义拆分", example = "EQUAL")
    private SplitScheme splitScheme;

    @NotNull(message = "拆分数量不能为空")
    @Min(value = 2, message = "拆分数量至少为2")
    @ApiModelProperty(value = "拆分数量", required = true, example = "3", notes = "将票据拆分为多少份")
    private Integer splitCount;

    @ApiModelProperty(value = "拆分明细（自定义拆分时必填）", notes = "每张子票据的金额和数量信息")
    private List<SplitDetail> splitDetails;

    @Size(max = 500, message = "拆分原因不能超过500个字符")
    @ApiModelProperty(value = "拆分原因", example = "部分转让用于支付供应商")
    private String splitReason;

    /**
     * 拆分明细DTO
     */
    @Data
    @ApiModel(value = "拆分明细", description = "单张子票据的拆分信息")
    public static class SplitDetail {

        @NotNull(message = "金额不能为空")
        @ApiModelProperty(value = "子票据金额（分）", required = true, example = "333333")
        private Long amount;

        @ApiModelProperty(value = "子票据数量", example = "1", notes = "相同金额的子票据数量，默认为1")
        @Min(value = 1, message = "数量至少为1")
        private Integer count = 1;
    }

    /**
     * 拆分方案枚举
     */
    public enum SplitScheme {
        EQUAL,   // 等额拆分
        CUSTOM   // 自定义拆分
    }
}
