package com.fisco.app.dto.receivable;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 账龄分析响应DTO
 *
 * 按账龄段统计分析应收账款，用于风险评估
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-09
 * @version 1.0
 */
@Data
@ApiModel(value = "账龄分析响应", description = "应收账款账龄分析数据")
public class AgedAnalysisResponse {

    @ApiModelProperty(value = "未到期（0-30天）")
    private AgedBucket current;

    @ApiModelProperty(value = "逾期1-30天")
    private AgedBucket overdue30;

    @ApiModelProperty(value = "逾期31-60天")
    private AgedBucket overdue60;

    @ApiModelProperty(value = "逾期61-90天")
    private AgedBucket overdue90;

    @ApiModelProperty(value = "逾期90天以上")
    private AgedBucket overdue90Plus;

    @ApiModelProperty(value = "分析时间")
    private LocalDateTime analysisTime;

    /**
     * 账龄段DTO
     */
    @Data
    @ApiModel(value = "账龄段", description = "账龄段统计数据")
    public static class AgedBucket {

        @ApiModelProperty(value = "数量", example = "50")
        private Long count;

        @ApiModelProperty(value = "金额（元）", example = "5000000.00")
        private BigDecimal amount;

        @ApiModelProperty(value = "占比（%）", example = "33.33")
        private Double percentage;
    }
}
