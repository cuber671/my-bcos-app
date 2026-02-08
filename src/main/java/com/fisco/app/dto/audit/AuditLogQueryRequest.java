package com.fisco.app.dto.audit;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 审计日志查询请求DTO
 */
@Data
@ApiModel(value = "审计日志查询请求", description = "审计日志的查询条件")
@Schema(name = "审计日志查询请求")
public class AuditLogQueryRequest {

    @ApiModelProperty(value = "操作人地址")
    private String userAddress;

    @ApiModelProperty(value = "操作模块（BILL, RECEIVABLE, WAREHOUSE_RECEIPT等）")
    private String module;

    @ApiModelProperty(value = "操作类型（CREATE, UPDATE, DELETE等）")
    private String actionType;

    @ApiModelProperty(value = "实体类型")
    private String entityType;

    @ApiModelProperty(value = "实体ID")
    private String entityId;

    @ApiModelProperty(value = "是否成功")
    private Boolean isSuccess;

    @ApiModelProperty(value = "开始时间")
    private LocalDateTime startDate;

    @ApiModelProperty(value = "结束时间")
    private LocalDateTime endDate;

    @ApiModelProperty(value = "页码（从0开始）", example = "0")
    private Integer page = 0;

    @ApiModelProperty(value = "每页大小", example = "20")
    private Integer size = 20;

    @ApiModelProperty(value = "排序字段", example = "createdAt")
    private String sortField = "createdAt";

    @ApiModelProperty(value = "排序方向（ASC, DESC）", example = "DESC")
    private String sortDirection = "DESC";
}
