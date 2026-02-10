package com.fisco.app.dto.user;

import com.fisco.app.entity.user.UserActivity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户活动日志查询请求DTO
 */
@Data
@ApiModel(value = "用户活动日志查询请求", description = "用户活动日志查询条件")
public class UserActivityQueryRequest {

    @ApiModelProperty(value = "用户ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String userId;

    @ApiModelProperty(value = "用户名", example = "zhangsan")
    private String username;

    @ApiModelProperty(value = "活动类型", example = "LOGIN")
    private UserActivity.ActivityType activityType;

    @ApiModelProperty(value = "操作模块", example = "USER")
    private String module;

    @ApiModelProperty(value = "操作结果", example = "SUCCESS")
    private UserActivity.ActivityResult result;

    @ApiModelProperty(value = "IP地址", example = "192.168.1.100")
    private String ipAddress;

    @ApiModelProperty(value = "开始时间", example = "2024-01-01T00:00:00")
    private LocalDateTime startDate;

    @ApiModelProperty(value = "结束时间", example = "2024-12-31T23:59:59")
    private LocalDateTime endDate;

    @ApiModelProperty(value = "页码", example = "0")
    private Integer page = 0;

    @ApiModelProperty(value = "每页大小", example = "10")
    private Integer size = 10;

    @ApiModelProperty(value = "排序字段", example = "createdAt")
    private String sortField = "createdAt";

    @ApiModelProperty(value = "排序方向", example = "DESC")
    private String sortDirection = "DESC";
}
