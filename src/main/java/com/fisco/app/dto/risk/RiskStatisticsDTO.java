package com.fisco.app.dto.risk;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;
import java.util.Map;

/**
 * 风险统计DTO
 */
@Data
@ApiModel(value = "风险统计", description = "风险监控统计信息")
public class RiskStatisticsDTO {

    @ApiModelProperty(value = "统计周期", example = "2026年1月")
    private String period;

    // 逾期统计
    @ApiModelProperty(value = "逾期应收账款数量", example = "10")
    private Long overdueReceivablesCount;

    @ApiModelProperty(value = "逾期金额（分）", example = "50000000")
    private Long overdueAmount;

    @ApiModelProperty(value = "逾期率", example = "0.05")
    private Double overdueRate;

    @ApiModelProperty(value = "轻度逾期数量（1-30天）", example = "5")
    private Long mildOverdueCount;

    @ApiModelProperty(value = "中度逾期数量（31-90天）", example = "3")
    private Long moderateOverdueCount;

    @ApiModelProperty(value = "重度逾期数量（91-179天）", example = "1")
    private Long severeOverdueCount;

    @ApiModelProperty(value = "坏账数量（180天+）", example = "1")
    private Long badDebtCount;

    // 罚息统计
    @ApiModelProperty(value = "累计罚息金额（分）", example = "500000")
    private Long totalPenaltyAmount;

    @ApiModelProperty(value = "本月罚息金额（分）", example = "100000")
    private Long monthlyPenaltyAmount;

    // 坏账统计
    @ApiModelProperty(value = "坏账总金额（分）", example = "10000000")
    private Long badDebtAmount;

    @ApiModelProperty(value = "已回收坏账金额（分）", example = "2000000")
    private Long recoveredBadDebtAmount;

    @ApiModelProperty(value = "坏账回收率", example = "0.2")
    private Double badDebtRecoveryRate;

    // 信用额度预警统计
    @ApiModelProperty(value = "信用额度预警次数", example = "15")
    private Long creditLimitWarningCount;

    @ApiModelProperty(value = "低风险预警次数（80-90%）", example = "8")
    private Long lowWarningCount;

    @ApiModelProperty(value = "中风险预警次数（90-95%）", example = "5")
    private Long mediumWarningCount;

    @ApiModelProperty(value = "高风险预警次数（95%+）", example = "2")
    private Long highWarningCount;

    // 票据风险统计
    @ApiModelProperty(value = "票据拒付数量", example = "2")
    private Long billRejectionCount;

    @ApiModelProperty(value = "票据拒付金额（分）", example = "2000000")
    private Long billRejectionAmount;

    // 风险企业统计
    @ApiModelProperty(value = "高风险企业数量", example = "3")
    private Long highRiskEnterpriseCount;

    @ApiModelProperty(value = "风险企业名单", example = "[\"0x1234...\", \"0x5678...\"]")
    private List<String> highRiskEnterprises;

    // 风险趋势
    @ApiModelProperty(value = "逾期金额趋势", example = "[{\"date\": \"2026-01-01\", \"amount\": 5000000}]")
    private List<TrendData> overdueAmountTrend;

    @ApiModelProperty(value = "逾期率趋势", example = "[{\"date\": \"2026-01-01\", \"rate\": 0.05}]")
    private List<RateTrendData> overdueRateTrend;

    // 风险等级分布
    @ApiModelProperty(value = "企业风险等级分布", example = "{\"LOW\": 50, \"MEDIUM\": 30, \"HIGH\": 10}")
    private Map<String, Long> enterpriseRiskLevelDistribution;

    /**
     * 趋势数据
     */
    @Data
    @ApiModel(value = "趋势数据")
    public static class TrendData {
        @ApiModelProperty(value = "日期", example = "2026-01-01")
        private String date;

        @ApiModelProperty(value = "金额（分）", example = "5000000")
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

        @ApiModelProperty(value = "比率", example = "0.05")
        private Double rate;
    }
}
