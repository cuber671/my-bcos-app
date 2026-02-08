package com.fisco.app.dto.notification;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 通知统计DTO
 */
@Data
@ApiModel(value = "通知统计", description = "通知统计信息")
public class NotificationStatisticsDTO {

    @ApiModelProperty(value = "总通知数", example = "100")
    private Long totalCount;

    @ApiModelProperty(value = "未读通知数", example = "10")
    private Long unreadCount;

    @ApiModelProperty(value = "已读通知数", example = "80")
    private Long readCount;

    @ApiModelProperty(value = "已归档通知数", example = "8")
    private Long archivedCount;

    @ApiModelProperty(value = "已删除通知数", example = "2")
    private Long deletedCount;

    @ApiModelProperty(value = "紧急通知数", example = "1")
    private Long urgentCount;

    @ApiModelProperty(value = "高优先级通知数", example = "5")
    private Long highPriorityCount;

    @ApiModelProperty(value = "普通优先级通知数", example = "85")
    private Long normalPriorityCount;

    @ApiModelProperty(value = "低优先级通知数", example = "9")
    private Long lowPriorityCount;

    @ApiModelProperty(value = "系统通知数", example = "20")
    private Long systemNotificationCount;

    @ApiModelProperty(value = "审批通知数", example = "15")
    private Long approvalNotificationCount;

    @ApiModelProperty(value = "风险通知数", example = "10")
    private Long riskNotificationCount;

    @ApiModelProperty(value = "预警通知数", example = "25")
    private Long warningNotificationCount;

    @ApiModelProperty(value = "业务通知数", example = "25")
    private Long businessNotificationCount;

    @ApiModelProperty(value = "提醒通知数", example = "5")
    private Long reminderNotificationCount;
}
