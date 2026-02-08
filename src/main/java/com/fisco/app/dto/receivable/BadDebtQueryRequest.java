package com.fisco.app.dto.receivable;

import com.fisco.app.entity.risk.BadDebtRecord;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 坏账查询请求DTO
 */
@Data
@ApiModel(value = "坏账查询请求", description = "用于查询坏账记录的请求参数")
public class BadDebtQueryRequest {

    @ApiModelProperty(value = "坏账类型", notes = "OVERDUE_180-逾期180天+, BANKRUPTCY-破产, DISPUTE-争议, OTHER-其他", example = "OVERDUE_180")
    private BadDebtRecord.BadDebtType badDebtType;

    @ApiModelProperty(value = "回收状态", notes = "NOT_RECOVERED-未回收, PARTIAL_RECOVERED-部分回收, FULL_RECOVERED-全额回收", example = "NOT_RECOVERED")
    private BadDebtRecord.RecoveryStatus recoveryStatus;

    @ApiModelProperty(value = "供应商地址", example = "0x1234567890abcdef")
    private String supplierAddress;

    @ApiModelProperty(value = "资金方地址", example = "0x567890abcdef1234")
    private String financierAddress;

    @ApiModelProperty(value = "逾期天数最小值", example = "180")
    private Integer overdueDaysMin;

    @ApiModelProperty(value = "创建日期开始", example = "2024-01-01T00:00:00")
    private LocalDateTime createdDateStart;

    @ApiModelProperty(value = "创建日期结束", example = "2024-12-31T23:59:59")
    private LocalDateTime createdDateEnd;

    @ApiModelProperty(value = "页码（从0开始）", example = "0")
    private Integer page = 0;

    @ApiModelProperty(value = "每页大小", example = "10")
    private Integer size = 10;

    @ApiModelProperty(value = "排序字段", example = "createdAt")
    private String sortBy = "createdAt";

    @ApiModelProperty(value = "排序方向", notes = "ASC-升序, DESC-降序", example = "DESC")
    private String sortDirection = "DESC";
}
