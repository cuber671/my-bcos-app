package com.fisco.app.dto.audit;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 审计日志统计DTO
 */
@Data
@ApiModel(value = "审计日志统计", description = "审计日志的统计信息")
@Schema(name = "审计日志统计")
public class AuditLogStatistics {

    @ApiModelProperty(value = "总操作次数")
    private Long totalOperations;

    @ApiModelProperty(value = "成功操作次数")
    private Long successCount;

    @ApiModelProperty(value = "失败操作次数")
    private Long failureCount;

    @ApiModelProperty(value = "成功率（%）")
    private Double successRate;

    @ApiModelProperty(value = "平均操作时长（毫秒）")
    private Long averageDuration;

    @ApiModelProperty(value = "各模块操作统计")
    private Map<String, Long> moduleStats;

    @ApiModelProperty(value = "各操作类型统计")
    private Map<String, Long> actionTypeStats;

    @ApiModelProperty(value = "用户操作排名（前10）")
    private java.util.List<UserOperationStats> topUsers;

    @ApiModelProperty(value = "统计开始时间")
    private LocalDateTime startTime;

    @ApiModelProperty(value = "统计结束时间")
    private LocalDateTime endTime;

    @Data
    @Schema(name = "用户操作统计")
    public static class UserOperationStats {
        @ApiModelProperty(value = "用户地址")
        private String userAddress;

        @ApiModelProperty(value = "用户姓名")
        private String userName;

        @ApiModelProperty(value = "操作次数")
        private Long operationCount;
    }
}
