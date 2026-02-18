package com.fisco.app.dto.statistics;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;
import java.util.Map;

/**
 * 业务统计DTO
 */
@Data
@ApiModel(value = "业务统计", description = "业务活动统计信息")
public class BusinessStatisticsDTO {

    @ApiModelProperty(value = "统计周期", example = "2026年1月")
    private String period;

    // 票据统计
    @ApiModelProperty(value = "票据总数", example = "100")
    private Long totalBills;

    @ApiModelProperty(value = "已承兑票据数", example = "85")
    private Long acceptedBills;

    @ApiModelProperty(value = "已贴现票据数", example = "30")
    private Long discountedBills;

    @ApiModelProperty(value = "已投资票据数", example = "20")
    private Long investedBills;

    @ApiModelProperty(value = "票据总金额（分）", example = "100000000")
    private Long totalBillAmount;

    // 应收账款统计
    @ApiModelProperty(value = "应收账款总数", example = "150")
    private Long totalReceivables;

    @ApiModelProperty(value = "已确认应收账款数", example = "120")
    private Long confirmedReceivables;

    @ApiModelProperty(value = "已融资应收账款数", example = "40")
    private Long financedReceivables;

    @ApiModelProperty(value = "应收账款总金额（分）", example = "150000000")
    private Long totalReceivableAmount;

    // 仓单统计
    @ApiModelProperty(value = "仓单总数", example = "50")
    private Long totalWarehouseReceipts;

    @ApiModelProperty(value = "已质押仓单数", example = "15")
    private Long pledgedReceipts;

    @ApiModelProperty(value = "仓单总价值（分）", example = "80000000")
    private Long totalReceiptValue;

    // 质押统计
    @ApiModelProperty(value = "质押申请总数", example = "25")
    private Long totalPledgeApplications;

    @ApiModelProperty(value = "已批准质押申请数", example = "20")
    private Long approvedPledgeApplications;

    @ApiModelProperty(value = "质押总金额（分）", example = "50000000")
    private Long totalPledgeAmount;

    // 时间序列数据
    @ApiModelProperty(value = "票据金额趋势", example = "[{\"date\": \"2026-01-01\", \"amount\": 10000000}]")
    private List<TrendData> billAmountTrend;

    @ApiModelProperty(value = "应收账款金额趋势", example = "[{\"date\": \"2026-01-01\", \"amount\": 15000000}]")
    private List<TrendData> receivableAmountTrend;

    @ApiModelProperty(value = "业务类型分布", example = "{\"BILL\": 100, \"RECEIVABLE\": 150, \"EWR\": 50}")
    private Map<String, Long> businessTypeDistribution;

    /**
     * 趋势数据
     */
    @Data
    @ApiModel(value = "趋势数据")
    public static class TrendData {
        @ApiModelProperty(value = "日期", example = "2026-01-01")
        private String date;

        @ApiModelProperty(value = "金额（分）", example = "10000000")
        private Long amount;

        @ApiModelProperty(value = "数量", example = "10")
        private Integer count;
    }
}
