package com.fisco.app.dto.notification;

import com.fisco.app.entity.notification.Notification;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 通知DTO
 */
@Data
@ApiModel(value = "通知信息", description = "通知详细信息")
public class NotificationDTO {

    @ApiModelProperty(value = "通知ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String id;

    @ApiModelProperty(value = "接收者用户ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String recipientId;

    @ApiModelProperty(value = "接收者类型", example = "USER")
    private Notification.RecipientType recipientType;

    @ApiModelProperty(value = "发送者用户ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String senderId;

    @ApiModelProperty(value = "发送者类型", example = "SYSTEM")
    private Notification.SenderType senderType;

    @ApiModelProperty(value = "通知类型", example = "BUSINESS")
    private Notification.NotificationType type;

    @ApiModelProperty(value = "通知分类", example = "BILL")
    private String category;

    @ApiModelProperty(value = "通知标题", example = "票据投资成功")
    private String title;

    @ApiModelProperty(value = "通知内容", example = "您成功投资票据BILL-001，投资金额：100000元，预期收益：5000元")
    private String content;

    @ApiModelProperty(value = "优先级", example = "NORMAL")
    private Notification.NotificationPriority priority;

    @ApiModelProperty(value = "状态", example = "UNREAD")
    private Notification.NotificationStatus status;

    @ApiModelProperty(value = "操作类型", example = "VIEW")
    private String actionType;

    @ApiModelProperty(value = "操作链接", example = "/api/bill/pool/BILL-001")
    private String actionUrl;

    @ApiModelProperty(value = "操作参数", example = "{\"billId\": \"BILL-001\"}")
    private Map<String, Object> actionParams;

    @ApiModelProperty(value = "业务类型", example = "BILL")
    private String businessType;

    @ApiModelProperty(value = "业务记录ID", example = "BILL-001")
    private String businessId;

    @ApiModelProperty(value = "额外数据", example = "{}")
    private Map<String, Object> extraData;

    @ApiModelProperty(value = "是否已发送", example = "true")
    private Boolean isSent;

    @ApiModelProperty(value = "发送时间", example = "2026-02-03T10:30:00")
    private LocalDateTime sentAt;

    @ApiModelProperty(value = "阅读时间", example = "2026-02-03T11:00:00")
    private LocalDateTime readAt;

    @ApiModelProperty(value = "过期时间", example = "2026-03-03T10:30:00")
    private LocalDateTime expireAt;

    @ApiModelProperty(value = "创建时间", example = "2026-02-03T10:30:00")
    private LocalDateTime createdAt;

    @ApiModelProperty(value = "更新时间", example = "2026-02-03T11:00:00")
    private LocalDateTime updatedAt;
}
