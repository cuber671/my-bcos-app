package com.fisco.app.dto.credit;

import com.fisco.app.enums.CreditUsageType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 信用额度使用记录查询请求DTO
 */
@Data
@ApiModel(value = "信用额度使用记录查询请求", description = "用于查询额度使用记录的请求参数")
public class CreditLimitUsageQueryRequest {

    @ApiModelProperty(value = "额度ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String creditLimitId;

    @ApiModelProperty(value = "企业地址", example = "0x1234567890abcdef1234567890abcdef12345678")
    private String enterpriseAddress;

    @ApiModelProperty(value = "使用类型", notes = "USE-使用, RELEASE-释放, FREEZE-冻结, UNFREEZE-解冻", example = "USE")
    private CreditUsageType usageType;

    @ApiModelProperty(value = "业务类型", example = "FINANCING_APPLICATION")
    private String businessType;

    @ApiModelProperty(value = "业务ID", example = "c3d4e5f6-g7h8-9012-cdef-234567890123")
    private String businessId;

    @ApiModelProperty(value = "操作人地址", example = "0x1234567890abcdef1234567890abcdef12345678")
    private String operatorAddress;

    @ApiModelProperty(value = "使用日期开始", example = "2026-01-01T00:00:00")
    private LocalDateTime usageDateStart;

    @ApiModelProperty(value = "使用日期结束", example = "2026-12-31T23:59:59")
    private LocalDateTime usageDateEnd;

    @ApiModelProperty(value = "金额最小值（单位：分）", example = "1000")
    private Long amountMin;

    @ApiModelProperty(value = "金额最大值（单位：分）", example = "10000000")
    private Long amountMax;

    @ApiModelProperty(value = "页码（从0开始）", example = "0")
    private Integer page = 0;

    @ApiModelProperty(value = "每页大小", example = "10")
    private Integer size = 10;

    @ApiModelProperty(value = "排序字段", example = "usageDate", notes = "可选值: usageDate, amount, beforeAvailable, afterAvailable")
    private String sortBy = "usageDate";

    @ApiModelProperty(value = "排序方向", notes = "ASC-升序, DESC-降序", example = "DESC")
    private String sortDirection = "DESC";
}
