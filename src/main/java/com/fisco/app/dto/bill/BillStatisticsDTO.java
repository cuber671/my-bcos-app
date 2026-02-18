package com.fisco.app.dto.bill;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 票据统计DTO
 * 包含票据的整体统计数据，支持多维度聚合分析
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-09
 * @version 1.0
 */
@Data
@ApiModel(value = "票统计分析", description = "票据整体统计数据")
public class BillStatisticsDTO {

    // ==================== 基础统计 ====================

    @ApiModelProperty(value = "票据总数", example = "150")
    private Long totalBills;

    @ApiModelProperty(value = "总金额（分）", example = "150000000")
    private Long totalAmount;

    @ApiModelProperty(value = "平均金额（分）", example = "1000000")
    private Long averageAmount;

    @ApiModelProperty(value = "最小金额（分）", example = "100000")
    private Long minAmount;

    @ApiModelProperty(value = "最大金额（分）", example = "5000000")
    private Long maxAmount;

    // ==================== 状态分布 ====================

    @ApiModelProperty(value = "状态分布统计")
    private List<StatusDistribution> statusDistribution;

    @ApiModelProperty(value = "状态分布数据（键值对）")
    private Map<String, Long> statusCounts;

    // ==================== 类型分布 ====================

    @ApiModelProperty(value = "类型分布统计")
    private List<TypeDistribution> typeDistribution;

    @ApiModelProperty(value = "类型分布数据（键值对）")
    private Map<String, Long> typeCounts;

    // ==================== 融资统计 ====================

    @ApiModelProperty(value = "融资统计")
    private FinancingStatistics financing;

    // ==================== 风险统计 ====================

    @ApiModelProperty(value = "风险统计")
    private RiskStatistics risk;

    // ==================== 时间趋势 ====================

    @ApiModelProperty(value = "时间趋势数据")
    private List<TrendData> timeTrend;

    // ==================== 持票人统计 ====================

    @ApiModelProperty(value = "持票人统计")
    private List<HolderStatistics> topHolders;

    // ==================== 统计时间范围 ====================

    @ApiModelProperty(value = "统计开始时间")
    private LocalDateTime startTime;

    @ApiModelProperty(value = "统计结束时间")
    private LocalDateTime endTime;

    @ApiModelProperty(value = "统计生成时间")
    private LocalDateTime generatedAt;

    // ==================== 内部DTO类 ====================

    /**
     * 状态分布DTO
     */
    @Data
    @ApiModel(value = "状态分布", description = "票据状态分布数据")
    public static class StatusDistribution {

        @ApiModelProperty(value = "状态", example = "NORMAL")
        private String status;

        @ApiModelProperty(value = "状态名称", example = "正常")
        private String statusName;

        @ApiModelProperty(value = "数量", example = "100")
        private Long count;

        @ApiModelProperty(value = "占比（%）", example = "66.67")
        private Double percentage;

        @ApiModelProperty(value = "金额（分）", example = "100000000")
        private Long amount;
    }

    /**
     * 类型分布DTO
     */
    @Data
    @ApiModel(value = "类型分布", description = "票据类型分布数据")
    public static class TypeDistribution {

        @ApiModelProperty(value = "类型", example = "BANK_ACCEPTANCE_BILL")
        private String type;

        @ApiModelProperty(value = "类型名称", example = "银行承兑汇票")
        private String typeName;

        @ApiModelProperty(value = "数量", example = "80")
        private Long count;

        @ApiModelProperty(value = "占比（%）", example = "53.33")
        private Double percentage;

        @ApiModelProperty(value = "金额（分）", example = "80000000")
        private Long amount;
    }

    /**
     * 融资统计DTO
     */
    @Data
    @ApiModel(value = "融资统计", description = "票据融资统计数据")
    public static class FinancingStatistics {

        @ApiModelProperty(value = "已贴现票据数", example = "30")
        private Long discountedCount;

        @ApiModelProperty(value = "贴现总金额（分）", example = "30000000")
        private Long discountAmount;

        @ApiModelProperty(value = "已质押票据数", example = "20")
        private Long pledgedCount;

        @ApiModelProperty(value = "质押总金额（分）", example = "20000000")
        private Long pledgeAmount;

        @ApiModelProperty(value = "融资率（%）", example = "33.33")
        private Double financingRate;
    }

    /**
     * 风险统计DTO
     */
    @Data
    @ApiModel(value = "风险统计", description = "票据风险统计数据")
    public static class RiskStatistics {

        @ApiModelProperty(value = "已冻结票据数", example = "2")
        private Long frozenCount;

        @ApiModelProperty(value = "已作废票据数", example = "5")
        private Long cancelledCount;

        @ApiModelProperty(value = "风险票据总数", example = "8")
        private Long totalRiskCount;

        @ApiModelProperty(value = "风险率（%）", example = "5.33")
        private Double riskRate;
    }

    /**
     * 趋势数据DTO
     */
    @Data
    @ApiModel(value = "趋势数据", description = "时间趋势数据点")
    public static class TrendData {

        @ApiModelProperty(value = "日期", example = "2026-02-08")
        private String date;

        @ApiModelProperty(value = "数量", example = "10")
        private Long count;

        @ApiModelProperty(value = "金额（分）", example = "10000000")
        private Long amount;
    }

    /**
     * 持票人统计DTO
     */
    @Data
    @ApiModel(value = "持票人统计", description = "持票人统计数据")
    public static class HolderStatistics {

        @ApiModelProperty(value = "持票人ID")
        private String holderId;

        @ApiModelProperty(value = "持票人名称")
        private String holderName;

        @ApiModelProperty(value = "持有票据数", example = "25")
        private Long billCount;

        @ApiModelProperty(value = "持有总金额（分）", example = "25000000")
        private Long totalAmount;
    }
}
