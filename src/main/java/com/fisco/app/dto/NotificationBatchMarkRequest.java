package com.fisco.app.dto;

import com.fisco.app.entity.Notification;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 批量标记通知请求DTO
 */
@Data
@ApiModel(value = "批量标记通知请求", description = "用于批量标记通知的请求参数")
public class NotificationBatchMarkRequest {

    @NotEmpty(message = "通知ID列表不能为空")
    @ApiModelProperty(value = "通知ID列表", required = true)
    private List<String> notificationIds;

    @NotNull(message = "目标状态不能为空")
    @ApiModelProperty(value = "目标状态", required = true, notes = "READ-标记为已读, ARCHIVED-标记为已归档, DELETED-标记为已删除", example = "READ")
    private Notification.NotificationStatus targetStatus;
}
