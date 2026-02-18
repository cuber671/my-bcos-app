package com.fisco.app.dto.statistics;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 综合报表DTO
 */
@Data
@ApiModel(value = "综合报表", description = "平台综合运营报表")
public class ComprehensiveReportDTO {

    @ApiModelProperty(value = "报表周期", example = "2026年1月")
    private String period;

    @ApiModelProperty(value = "报告生成时间", example = "2026-02-03T10:30:00")
    private String reportGeneratedTime;

    // 核心指标
    @ApiModelProperty(value = "平台总交易额（分）", example = "1000000000")
    private Long totalTransactionAmount;

    @ApiModelProperty(value = "本月交易额（分）", example = "100000000")
    private Long monthlyTransactionAmount;

    @ApiModelProperty(value = "交易额环比增长率", example = "0.15")
    private BigDecimal transactionGrowthRate;

    @ApiModelProperty(value = "注册企业数量", example = "200")
    private Long totalEnterprises;

    @ApiModelProperty(value = "本月新增企业数量", example = "10")
    private Long monthlyNewEnterprises;

    @ApiModelProperty(value = "活跃企业数量", example = "150")
    private Long activeEnterprises;

    // 业务量统计
    @ApiModelProperty(value = "票据业务量", example = "500")
    private Long billBusinessVolume;

    @ApiModelProperty(value = "应收账款业务量", example = "800")
    private Long receivableBusinessVolume;

    @ApiModelProperty(value = "仓单业务量", example = "300")
    private Long warehouseReceiptBusinessVolume;

    @ApiModelProperty(value = "总业务量", example = "1600")
    private Long totalBusinessVolume;

    // 融资统计
    @ApiModelProperty(value = "融资总额（分）", example = "500000000")
    private Long totalFinancingAmount;

    @ApiModelProperty(value = "融资笔数", example = "100")
    private Long financingCount;

    @ApiModelProperty(value = "平均融资金额（分）", example = "5000000")
    private Long averageFinancingAmount;

    @ApiModelProperty(value = "融资成功率", example = "0.85")
    private BigDecimal financingSuccessRate;

    // 资金统计
    @ApiModelProperty(value = "平台资金池规模（分）", example = "2000000000")
    private Long platformFundPool;

    @ApiModelProperty(value = "已放款金额（分）", example = "1500000000")
    private Long loanedAmount;

    @ApiModelProperty(value = "已还款金额（分）", example = "1000000000")
    private Long repaidAmount;

    @ApiModelProperty(value = "资金利用率", example = "0.75")
    private BigDecimal fundUtilizationRate;

    // 风险指标
    @ApiModelProperty(value = "逾期金额（分）", example = "50000000")
    private Long overdueAmount;

    @ApiModelProperty(value = "逾期率", example = "0.03")
    private BigDecimal overdueRate;

    @ApiModelProperty(value = "坏账金额（分）", example = "10000000")
    private Long badDebtAmount;

    @ApiModelProperty(value = "坏账率", example = "0.01")
    private BigDecimal badDebtRate;

    @ApiModelProperty(value = "风险准备金（分）", example = "50000000")
    private Long riskReserve;

    // 收入统计
    @ApiModelProperty(value = "手续费收入（分）", example = "5000000")
    private Long feeIncome;

    @ApiModelProperty(value = "利息收入（分）", example = "10000000")
    private Long interestIncome;

    @ApiModelProperty(value = "罚息收入（分）", example = "500000")
    private Long penaltyIncome;

    @ApiModelProperty(value = "总收入（分）", example = "15500000")
    private Long totalIncome;

    // 地域分布
    @ApiModelProperty(value = "地域业务分布", example = "{\"北京\": 300, \"上海\": 250, \"深圳\": 200}")
    private Map<String, Long> regionalDistribution;

    // 行业分布
    @ApiModelProperty(value = "行业业务分布", example = "{\"制造业\": 400, \"贸易\": 300, \"建筑\": 200}")
    private Map<String, Long> industryDistribution;

    // 时间序列数据
    @ApiModelProperty(value = "月度交易额趋势", example = "[{\"month\": \"2026-01\", \"amount\": 100000000}]")
    private List<MonthlyTrendData> monthlyTransactionTrend;

    @ApiModelProperty(value = "月度融资趋势", example = "[{\"month\": \"2026-01\", \"amount\": 50000000}]")
    private List<MonthlyTrendData> monthlyFinancingTrend;

    @ApiModelProperty(value = "月度风险趋势", example = "[{\"month\": \"2026-01\", \"overdueRate\": 0.03}]")
    private List<MonthlyRiskTrendData> monthlyRiskTrend;

    // Top数据
    @ApiModelProperty(value = "交易额Top10企业")
    private List<TopEnterpriseData> topEnterprisesByTransaction;

    @ApiModelProperty(value = "融资额Top10企业")
    private List<TopEnterpriseData> topEnterprisesByFinancing;

    /**
     * 月度趋势数据
     */
    @Data
    @ApiModel(value = "月度趋势数据")
    public static class MonthlyTrendData {
        @ApiModelProperty(value = "月份", example = "2026-01")
        private String month;

        @ApiModelProperty(value = "金额（分）", example = "100000000")
        private Long amount;
    }

    /**
     * 月度风险趋势数据
     */
    @Data
    @ApiModel(value = "月度风险趋势数据")
    public static class MonthlyRiskTrendData {
        @ApiModelProperty(value = "月份", example = "2026-01")
        private String month;

        @ApiModelProperty(value = "逾期率", example = "0.03")
        private BigDecimal overdueRate;

        @ApiModelProperty(value = "坏账率", example = "0.01")
        private BigDecimal badDebtRate;
    }

    /**
     * Top企业数据
     */
    @Data
    @ApiModel(value = "Top企业数据")
    public static class TopEnterpriseData {
        @ApiModelProperty(value = "企业地址", example = "0x1234567890abcdef")
        private String enterpriseAddress;

        @ApiModelProperty(value = "企业名称", example = "某某科技有限公司")
        private String enterpriseName;

        @ApiModelProperty(value = "金额（分）", example = "100000000")
        private Long amount;

        @ApiModelProperty(value = "排名", example = "1")
        private Integer rank;
    }
}
