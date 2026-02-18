package com.fisco.app.dto.notification;

import lombok.Data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;

/**
 * 通知订阅更新请求DTO
 */
@Data
@ApiModel(value = "通知订阅更新请求", description = "用于更新通知订阅的请求参数")
public class NotificationSubscriptionRequest {

    @ApiModelProperty(value = "订阅的通知类型列表", example = "[\"APPROVAL\", \"RISK\", \"WARNING\"]")
    private List<String> subscribedTypes;

    @ApiModelProperty(value = "取消订阅的通知类型列表", example = "[\"SYSTEM\", \"BUSINESS\"]")
    private List<String> unsubscribedTypes;

    @ApiModelProperty(value = "是否启用邮件通知", example = "true")
    private Boolean notifyEmail;

    @ApiModelProperty(value = "是否启用短信通知", example = "false")
    private Boolean notifySms;

    @ApiModelProperty(value = "是否启用推送通知", example = "true")
    private Boolean notifyPush;

    @ApiModelProperty(value = "是否启用应用内通知", example = "true")
    private Boolean notifyInApp;
}
