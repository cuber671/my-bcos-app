package com.fisco.app.dto.statistics;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * 统计查询请求DTO
 */
@Data
@ApiModel(value = "统计查询请求", description = "用于查询统计数据的请求参数")
public class StatisticsQueryRequest {

    @NotNull(message = "开始时间不能为空")
    @ApiModelProperty(value = "统计开始时间", required = true, example = "2026-01-01T00:00:00")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    @ApiModelProperty(value = "统计结束时间", required = true, example = "2026-12-31T23:59:59")
    private LocalDateTime endTime;

    @ApiModelProperty(value = "企业地址（可选，用于筛选特定企业的数据）", example = "0x1234567890abcdef")
    private String enterpriseAddress;

    @ApiModelProperty(value = "统计粒度", notes = "DAY-按天, WEEK-按周, MONTH-按月, QUARTER-按季度, YEAR-按年", example = "MONTH")
    private StatisticsGranularity granularity;

    @ApiModelProperty(value = "业务类型", notes = "用于筛选特定业务类型的数据", example = "BILL")
    private String businessType;

    /**
     * 统计粒度枚举
     */
    public enum StatisticsGranularity {
        DAY("按天"),
        WEEK("按周"),
        MONTH("按月"),
        QUARTER("按季度"),
        YEAR("按年");

        private final String description;

        StatisticsGranularity(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
