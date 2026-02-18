package com.fisco.app.dto.notification;

import com.fisco.app.entity.notification.Notification;
import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 创建通知请求DTO
 */
@Data
@ApiModel(value = "创建通知请求", description = "用于创建通知的请求参数")
public class NotificationCreateRequest {

    @NotBlank(message = "接收者ID不能为空")
    @ApiModelProperty(value = "接收者用户ID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
    private String recipientId;

    @NotNull(message = "接收者类型不能为空")
    @ApiModelProperty(value = "接收者类型", required = true, notes = "USER-用户, ENTERPRISE-企业, ROLE-角色", example = "USER")
    private Notification.RecipientType recipientType;

    @ApiModelProperty(value = "发送者用户ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String senderId;

    @ApiModelProperty(value = "发送者类型", notes = "SYSTEM-系统, USER-用户, ENTERPRISE-企业", example = "SYSTEM")
    private Notification.SenderType senderType;

    @NotNull(message = "通知类型不能为空")
    @ApiModelProperty(value = "通知类型", required = true, notes = "SYSTEM-系统通知, APPROVAL-审批通知, RISK-风险通知, WARNING-预警通知, BUSINESS-业务通知, REMINDER-提醒通知", example = "BUSINESS")
    private Notification.NotificationType type;

    @ApiModelProperty(value = "通知分类", example = "BILL")
    private String category;

    @NotBlank(message = "通知标题不能为空")
    @ApiModelProperty(value = "通知标题", required = true, example = "票据投资成功")
    private String title;

    @NotBlank(message = "通知内容不能为空")
    @ApiModelProperty(value = "通知内容", required = true, example = "您成功投资票据BILL-001，投资金额：100000元，预期收益：5000元")
    private String content;

    @ApiModelProperty(value = "优先级", notes = "LOW-低, NORMAL-普通, HIGH-高, URGENT-紧急", example = "NORMAL")
    private Notification.NotificationPriority priority;

    @ApiModelProperty(value = "操作类型", example = "VIEW")
    private String actionType;

    @ApiModelProperty(value = "操作链接", example = "/api/bill/pool/BILL-001")
    private String actionUrl;

    @ApiModelProperty(value = "操作参数(JSON格式)", example = "{\"billId\": \"BILL-001\"}")
    private Map<String, Object> actionParams;

    @ApiModelProperty(value = "业务类型", example = "BILL")
    private String businessType;

    @ApiModelProperty(value = "业务记录ID", example = "BILL-001")
    private String businessId;

    @ApiModelProperty(value = "额外数据(JSON格式)", example = "{}")
    private Map<String, Object> extraData;

    @ApiModelProperty(value = "是否立即发送", example = "true")
    private Boolean sendImmediately = true;

    @ApiModelProperty(value = "过期时间", example = "2026-03-03T10:30:00")
    private LocalDateTime expireAt;
}
