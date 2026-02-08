package com.fisco.app.dto.credit;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;


/**
 * 信用额度查询响应DTO
 */
@Data
@ApiModel(value = "信用额度查询响应", description = "信用额度查询结果")
public class CreditLimitQueryResponse {

    @ApiModelProperty(value = "信用额度列表")
    private List<CreditLimitDTO> content;

    @ApiModelProperty(value = "当前页码", example = "0")
    private int pageNumber;

    @ApiModelProperty(value = "每页大小", example = "10")
    private int pageSize;

    @ApiModelProperty(value = "总页数", example = "5")
    private int totalPages;

    @ApiModelProperty(value = "总记录数", example = "50")
    private long totalElements;

    @ApiModelProperty(value = "是否第一页", example = "true")
    private boolean first;

    @ApiModelProperty(value = "是否最后一页", example = "false")
    private boolean last;

    @ApiModelProperty(value = "统计信息")
    private CreditLimitStatistics statistics;

    /**
     * 信用额度统计信息
     */
    @Data
    @ApiModel(value = "信用额度统计信息")
    public static class CreditLimitStatistics {
        @ApiModelProperty(value = "额度总数", example = "50")
        private long totalCount;

        @ApiModelProperty(value = "总额度（元）", example = "50000000.00")
        private java.math.BigDecimal totalLimit;

        @ApiModelProperty(value = "总已使用额度（元）", example = "15000000.00")
        private java.math.BigDecimal totalUsedLimit;

        @ApiModelProperty(value = "总冻结额度（元）", example = "2000000.00")
        private java.math.BigDecimal totalFrozenLimit;

        @ApiModelProperty(value = "总可用额度（元）", example = "33000000.00")
        private java.math.BigDecimal totalAvailableLimit;

        @ApiModelProperty(value = "平均使用率（百分比）", example = "30.0")
        private Double averageUsageRate;

        @ApiModelProperty(value = "需要预警的额度数量", example = "5")
        private long needsWarningCount;

        @ApiModelProperty(value = "融资额度数量", example = "20")
        private long financingCount;

        @ApiModelProperty(value = "担保额度数量", example = "15")
        private long guaranteeCount;

        @ApiModelProperty(value = "赊账额度数量", example = "15")
        private long creditCount;

        @ApiModelProperty(value = "活跃额度数量", example = "45")
        private long activeCount;

        @ApiModelProperty(value = "冻结额度数量", example = "3")
        private long frozenCount;

        @ApiModelProperty(value = "失效额度数量", example = "2")
        private long expiredCount;
    }
}
