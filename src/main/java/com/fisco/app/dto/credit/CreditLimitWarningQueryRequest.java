package com.fisco.app.dto.credit;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.time.LocalDateTime;

import com.fisco.app.enums.CreditWarningLevel;

/**
 * 信用额度预警记录查询请求DTO
 */
@Data
@ApiModel(value = "信用额度预警记录查询请求", description = "用于查询额度预警记录的请求参数")
public class CreditLimitWarningQueryRequest {

    @ApiModelProperty(value = "额度ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String creditLimitId;

    @ApiModelProperty(value = "企业地址", example = "0x1234567890abcdef1234567890abcdef12345678")
    private String enterpriseAddress;

    @ApiModelProperty(value = "预警级别", notes = "LOW-低风险, MEDIUM-中风险, HIGH-高风险, CRITICAL-紧急", example = "MEDIUM")
    private CreditWarningLevel warningLevel;

    @ApiModelProperty(value = "预警类型", example = "USAGE_HIGH", notes = "USAGE_HIGH-使用率过高, EXPIRY_SOON-额度即将到期, RISK_UP-风险等级提升, OVERDUE-存在逾期")
    private String warningType;

    @ApiModelProperty(value = "是否已处理", example = "false", notes = "true-已处理，false-未处理")
    private Boolean isResolved;

    @ApiModelProperty(value = "预警日期开始", example = "2026-01-01T00:00:00")
    private LocalDateTime warningDateStart;

    @ApiModelProperty(value = "预警日期结束", example = "2026-12-31T23:59:59")
    private LocalDateTime warningDateEnd;

    @ApiModelProperty(value = "页码（从0开始）", example = "0")
    private Integer page = 0;

    @ApiModelProperty(value = "每页大小", example = "10")
    private Integer size = 10;

    @ApiModelProperty(value = "排序字段", example = "warningDate", notes = "可选值: warningDate, warningLevel, currentUsageRate")
    private String sortBy = "warningDate";

    @ApiModelProperty(value = "排序方向", notes = "ASC-升序, DESC-降序", example = "DESC")
    private String sortDirection = "DESC";
}
