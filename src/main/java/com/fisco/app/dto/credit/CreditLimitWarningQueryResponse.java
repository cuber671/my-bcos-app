package com.fisco.app.dto.credit;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;

/**
 * 信用额度预警记录查询响应DTO
 */
@Data
@ApiModel(value = "信用额度预警记录查询响应", description = "额度预警记录查询结果")
public class CreditLimitWarningQueryResponse {

    @ApiModelProperty(value = "预警记录列表")
    private List<CreditLimitWarningDTO> content;

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
    private CreditLimitWarningStatistics statistics;

    /**
     * 预警记录统计信息
     */
    @Data
    @ApiModel(value = "预警记录统计信息")
    public static class CreditLimitWarningStatistics {
        @ApiModelProperty(value = "预警记录总数", example = "50")
        private long totalCount;

        @ApiModelProperty(value = "未处理预警数量", example = "15")
        private long unresolvedCount;

        @ApiModelProperty(value = "已处理预警数量", example = "35")
        private long resolvedCount;

        @ApiModelProperty(value = "低风险预警数量", example = "10")
        private long lowCount;

        @ApiModelProperty(value = "中风险预警数量", example = "20")
        private long mediumCount;

        @ApiModelProperty(value = "高风险预警数量", example = "15")
        private long highCount;

        @ApiModelProperty(value = "紧急预警数量", example = "5")
        private long criticalCount;

        @ApiModelProperty(value = "使用率过高预警数量", example = "25")
        private long usageHighCount;

        @ApiModelProperty(value = "额度即将到期预警数量", example = "10")
        private long expirySoonCount;

        @ApiModelProperty(value = "风险等级提升预警数量", example = "10")
        private long riskUpCount;

        @ApiModelProperty(value = "存在逾期预警数量", example = "5")
        private long overdueCount;
    }
}
