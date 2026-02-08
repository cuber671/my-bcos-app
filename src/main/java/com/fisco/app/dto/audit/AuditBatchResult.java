package com.fisco.app.dto.audit;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 批量审核结果DTO
 */
@Data
@ApiModel(value = "批量审核结果", description = "批量审核操作的执行结果")
@Schema(name = "批量审核结果")
public class AuditBatchResult {

    @ApiModelProperty(value = "总数", required = true)
    private Integer totalCount;

    @ApiModelProperty(value = "成功数量", required = true)
    private Integer successCount;

    @ApiModelProperty(value = "失败数量", required = true)
    private Integer failCount;

    @ApiModelProperty(value = "失败的企业地址列表")
    private List<String> failedAddresses;

    @ApiModelProperty(value = "错误信息映射（地址 -> 错误信息）")
    private Map<String, String> errorMessages;

    @ApiModelProperty(value = "审核人", required = true)
    private String auditor;

    @ApiModelProperty(value = "审核时间", required = true)
    private LocalDateTime auditTime;

    @ApiModelProperty(value = "成功率（%）")
    public Double getSuccessRate() {
        if (totalCount == 0) return 0.0;
        return (successCount * 100.0) / totalCount;
    }
}
