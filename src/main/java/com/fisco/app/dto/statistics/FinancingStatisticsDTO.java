package com.fisco.app.dto.statistics;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 融资统计DTO
 */
@Data
@ApiModel(value = "融资统计", description = "融资活动统计信息")
public class FinancingStatisticsDTO {

    @ApiModelProperty(value = "统计周期", example = "2026年1月")
    private String period;

    // 融资总额统计
    @ApiModelProperty(value = "融资总额（分）", example = "500000000")
    private Long totalFinancingAmount;

    @ApiModelProperty(value = "票据融资金额（分）", example = "300000000")
    private Long billFinancingAmount;

    @ApiModelProperty(value = "应收账款融资金额（分）", example = "150000000")
    private Long receivableFinancingAmount;

    @ApiModelProperty(value = "仓单质押融资金额（分）", example = "50000000")
    private Long pledgeFinancingAmount;

    // 融资笔数统计
    @ApiModelProperty(value = "融资总笔数", example = "80")
    private Long totalFinancingCount;

    @ApiModelProperty(value = "票据融资笔数", example = "50")
    private Long billFinancingCount;

    @ApiModelProperty(value = "应收账款融资笔数", example = "25")
    private Long receivableFinancingCount;

    @ApiModelProperty(value = "质押融资笔数", example = "5")
    private Long pledgeFinancingCount;

    // 平均融资利率
    @ApiModelProperty(value = "平均融资利率（基点）", example = "500")
    private Integer averageFinancingRate;

    @ApiModelProperty(value = "票据平均利率（基点）", example = "450")
    private Integer billAverageRate;

    @ApiModelProperty(value = "应收账款平均利率（基点）", example = "600")
    private Integer receivableAverageRate;

    // 融资期限统计
    @ApiModelProperty(value = "平均融资期限（天）", example = "90")
    private Integer averageFinancingTerm;

    @ApiModelProperty(value = "最短融资期限（天）", example = "30")
    private Integer minFinancingTerm;

    @ApiModelProperty(value = "最长融资期限（天）", example = "180")
    private Integer maxFinancingTerm;

    // 还款统计
    @ApiModelProperty(value = "已还款金额（分）", example = "200000000")
    private Long repaidAmount;

    @ApiModelProperty(value = "待还款金额（分）", example = "300000000")
    private Long outstandingAmount;

    @ApiModelProperty(value = "逾期金额（分）", example = "10000000")
    private Long overdueAmount;

    @ApiModelProperty(value = "还款率", example = "0.4")
    private BigDecimal repaymentRate;

    // 融资渠道分布
    @ApiModelProperty(value = "融资机构分布", example = "{\"BANK_A\": 300000000, \"BANK_B\": 200000000}")
    private Map<String, Long> financierDistribution;

    // 时间序列数据
    @ApiModelProperty(value = "融资金额趋势", example = "[{\"date\": \"2026-01-01\", \"amount\": 50000000}]")
    private List<TrendData> financingAmountTrend;

    @ApiModelProperty(value = "利率趋势", example = "[{\"date\": \"2026-01-01\", \"rate\": 500}]")
    private List<RateTrendData> rateTrend;

    /**
     * 趋势数据
     */
    @Data
    @ApiModel(value = "趋势数据")
    public static class TrendData {
        @ApiModelProperty(value = "日期", example = "2026-01-01")
        private String date;

        @ApiModelProperty(value = "金额（分）", example = "50000000")
        private Long amount;
    }

    /**
     * 利率趋势数据
     */
    @Data
    @ApiModel(value = "利率趋势数据")
    public static class RateTrendData {
        @ApiModelProperty(value = "日期", example = "2026-01-01")
        private String date;

        @ApiModelProperty(value = "利率（基点）", example = "500")
        private Integer rate;
    }
}
