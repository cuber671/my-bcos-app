package com.fisco.app.dto.receivable;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;


/**
 * 逾期账款查询响应DTO
 */
@Data
@ApiModel(value = "逾期账款查询响应", description = "逾期账款查询结果")
public class OverdueQueryResponse {

    @ApiModelProperty(value = "逾期账款列表")
    private List<OverdueReceivableDTO> content;

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
    private OverdueStatistics statistics;

    /**
     * 逾期统计信息
     */
    @Data
    @ApiModel(value = "逾期统计信息")
    public static class OverdueStatistics {
        @ApiModelProperty(value = "逾期账款总数", example = "50")
        private long totalCount;

        @ApiModelProperty(value = "逾期总金额", example = "15000000.00")
        private java.math.BigDecimal totalAmount;

        @ApiModelProperty(value = "轻度逾期数量", example = "20")
        private long mildCount;

        @ApiModelProperty(value = "中度逾期数量", example = "15")
        private long moderateCount;

        @ApiModelProperty(value = "重度逾期数量", example = "10")
        private long severeCount;

        @ApiModelProperty(value = "坏账数量", example = "5")
        private long badDebtCount;

        @ApiModelProperty(value = "累计罚息总额", example = "250000.00")
        private java.math.BigDecimal totalPenaltyAmount;
    }
}
