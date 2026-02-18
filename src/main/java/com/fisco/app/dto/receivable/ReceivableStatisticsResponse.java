package com.fisco.app.dto.receivable;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 应收账款统计响应DTO
 *
 * 提供应收账款的多维度统计数据
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-09
 * @version 1.0
 */
@Data
@ApiModel(value = "应收账款统计响应", description = "应收账款多维度统计数据")
public class ReceivableStatisticsResponse {

    // ==================== 基础统计 ====================

    @ApiModelProperty(value = "应收账款总数", example = "150")
    private Long totalCount;

    @ApiModelProperty(value = "应收账款总金额（元）", example = "15000000.00")
    private BigDecimal totalAmount;

    @ApiModelProperty(value = "已还总金额（元）", example = "5000000.00")
    private BigDecimal totalRepaidAmount;

    @ApiModelProperty(value = "未还总金额（元）", example = "10000000.00")
    private BigDecimal totalOutstandingAmount;

    @ApiModelProperty(value = "平均融资金额（元）", example = "300000.00")
    private BigDecimal avgFinanceAmount;

    // ==================== 状态分布统计 ====================

    @ApiModelProperty(value = "状态分布统计")
    private List<StatusStatistics> statusDistribution;

    // ==================== 融资统计 ====================

    @ApiModelProperty(value = "已融资数量", example = "100")
    private Long financedCount;

    @ApiModelProperty(value = "已融资金额（元）", example = "9000000.00")
    private BigDecimal financedAmount;

    @ApiModelProperty(value = "未融资数量", example = "50")
    private Long unfinancedCount;

    @ApiModelProperty(value = "未融资金额（元）", example = "6000000.00")
    private BigDecimal unfinancedAmount;

    @ApiModelProperty(value = "融资率（%）", example = "60.00")
    private BigDecimal financeRate;

    // ==================== 逾期统计 ====================

    @ApiModelProperty(value = "逾期数量", example = "20")
    private Long overdueCount;

    @ApiModelProperty(value = "逾期金额（元）", example = "2000000.00")
    private BigDecimal overdueAmount;

    @ApiModelProperty(value = "逾期率（%）", example = "13.33")
    private BigDecimal overdueRate;

    // ==================== 统计时间 ====================

    @ApiModelProperty(value = "统计生成时间")
    private LocalDateTime generatedAt;

    /**
     * 状态统计DTO
     */
    @Data
    @ApiModel(value = "状态统计", description = "应收账款状态分布数据")
    public static class StatusStatistics {

        @ApiModelProperty(value = "状态", example = "FINANCED")
        private String status;

        @ApiModelProperty(value = "状态名称", example = "已融资")
        private String statusName;

        @ApiModelProperty(value = "数量", example = "100")
        private Long count;

        @ApiModelProperty(value = "总金额（元）", example = "9000000.00")
        private BigDecimal totalAmount;

        @ApiModelProperty(value = "占比（%）", example = "66.67")
        private Double percentage;
    }
}
