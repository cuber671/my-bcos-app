package com.fisco.app.dto.credit;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;


/**
 * 信用额度使用记录查询响应DTO
 */
@Data
@ApiModel(value = "信用额度使用记录查询响应", description = "额度使用记录查询结果")
public class CreditLimitUsageQueryResponse {

    @ApiModelProperty(value = "使用记录列表")
    private List<CreditLimitUsageDTO> content;

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
    private CreditLimitUsageStatistics statistics;

    /**
     * 使用记录统计信息
     */
    @Data
    @ApiModel(value = "使用记录统计信息")
    public static class CreditLimitUsageStatistics {
        @ApiModelProperty(value = "使用记录总数", example = "50")
        private long totalCount;

        @ApiModelProperty(value = "总使用金额（元）", example = "5000000.00")
        private java.math.BigDecimal totalUsageAmount;

        @ApiModelProperty(value = "总释放金额（元）", example = "3000000.00")
        private java.math.BigDecimal totalReleaseAmount;

        @ApiModelProperty(value = "净使用金额（元）", example = "2000000.00", notes = "使用金额 - 释放金额")
        private java.math.BigDecimal netUsageAmount;

        @ApiModelProperty(value = "总冻结金额（元）", example = "500000.00")
        private java.math.BigDecimal totalFreezeAmount;

        @ApiModelProperty(value = "总解冻金额（元）", example = "300000.00")
        private java.math.BigDecimal totalUnfreezeAmount;

        @ApiModelProperty(value = "当前冻结金额（元）", example = "200000.00", notes = "冻结金额 - 解冻金额")
        private java.math.BigDecimal currentFrozenAmount;
    }
}
