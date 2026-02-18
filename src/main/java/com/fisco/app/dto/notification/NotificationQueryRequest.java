package com.fisco.app.dto.notification;

import com.fisco.app.entity.notification.Notification;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.time.LocalDateTime;

/**
 * 通知查询请求DTO
 */
@Data
@ApiModel(value = "通知查询请求", description = "用于查询通知的请求参数")
public class NotificationQueryRequest {

    @ApiModelProperty(value = "接收者ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String recipientId;

    @ApiModelProperty(value = "接收者类型", notes = "USER-用户, ENTERPRISE-企业, ROLE-角色", example = "USER")
    private Notification.RecipientType recipientType;

    @ApiModelProperty(value = "发送者ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String senderId;

    @ApiModelProperty(value = "发送者类型", notes = "SYSTEM-系统, USER-用户, ENTERPRISE-企业", example = "SYSTEM")
    private Notification.SenderType senderType;

    @ApiModelProperty(value = "通知类型", notes = "SYSTEM-系统通知, APPROVAL-审批通知, RISK-风险通知, WARNING-预警通知, BUSINESS-业务通知, REMINDER-提醒通知", example = "BUSINESS")
    private Notification.NotificationType type;

    @ApiModelProperty(value = "通知分类", example = "BILL")
    private String category;

    @ApiModelProperty(value = "通知状态", notes = "UNREAD-未读, READ-已读, ARCHIVED-已归档, DELETED-已删除", example = "UNREAD")
    private Notification.NotificationStatus status;

    @ApiModelProperty(value = "优先级", notes = "LOW-低, NORMAL-普通, HIGH-高, URGENT-紧急", example = "HIGH")
    private Notification.NotificationPriority priority;

    @ApiModelProperty(value = "业务类型", example = "BILL")
    private String businessType;

    @ApiModelProperty(value = "业务ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String businessId;

    @ApiModelProperty(value = "是否已发送", example = "true")
    private Boolean isSent;

    @ApiModelProperty(value = "创建时间开始", example = "2026-01-01T00:00:00")
    private LocalDateTime createdAtStart;

    @ApiModelProperty(value = "创建时间结束", example = "2026-12-31T23:59:59")
    private LocalDateTime createdAtEnd;

    @ApiModelProperty(value = "标题关键字（模糊搜索）", example = "票据")
    private String titleKeyword;

    @ApiModelProperty(value = "内容关键字（模糊搜索）", example = "投资")
    private String contentKeyword;

    @ApiModelProperty(value = "页码（从0开始）", example = "0")
    private Integer page = 0;

    @ApiModelProperty(value = "每页大小", example = "10")
    private Integer size = 10;

    @ApiModelProperty(value = "排序字段", example = "createdAt", notes = "可选值: createdAt, priority, sentAt, readAt")
    private String sortBy = "createdAt";

    @ApiModelProperty(value = "排序方向", notes = "ASC-升序, DESC-降序", example = "DESC")
    private String sortDirection = "DESC";
}
