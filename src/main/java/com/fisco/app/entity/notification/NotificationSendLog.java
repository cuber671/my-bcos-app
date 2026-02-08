package com.fisco.app.entity.notification;

import lombok.Data;
import lombok.EqualsAndHashCode;

import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import java.time.LocalDateTime;

import javax.persistence.*;

/**
 * 通知发送日志实体类
 * 记录通知发送历史
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "notification_send_log", indexes = {
    @Index(name = "idx_notification", columnList = "notification_id"),
    @Index(name = "idx_recipient", columnList = "recipient_id"),
    @Index(name = "idx_channel", columnList = "channel"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Schema(name = "通知发送日志")
public class NotificationSendLog {

    @Id
    @Column(name = "id", nullable = false, unique = true, length = 36)
    @ApiModelProperty(value = "日志ID", required = true)
    private String id;

    @Column(name = "notification_id", nullable = false, length = 36)
    @ApiModelProperty(value = "通知ID", required = true)
    private String notificationId;

    @Column(name = "recipient_id", nullable = false, length = 36)
    @ApiModelProperty(value = "接收者ID", required = true)
    private String recipientId;

    @Column(name = "channel", nullable = false, length = 20)
    @ApiModelProperty(value = "发送渠道: IN_APP, EMAIL, SMS, PUSH", required = true)
    private String channel;

    @Column(name = "status", nullable = false, length = 20)
    @ApiModelProperty(value = "状态: PENDING, SUCCESS, FAILED", required = true)
    private String status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    @ApiModelProperty(value = "错误信息")
    private String errorMessage;

    @Column(name = "retry_count")
    @ApiModelProperty(value = "重试次数")
    private Integer retryCount;

    @Column(name = "sent_at")
    @ApiModelProperty(value = "发送时间")
    private LocalDateTime sentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @ApiModelProperty(value = "创建时间", required = true)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = "PENDING";
        }
        if (retryCount == null) {
            retryCount = 0;
        }
    }

    /**
     * 发送渠道枚举
     */
    public enum SendChannel {
        IN_APP("应用内"),
        EMAIL("邮件"),
        SMS("短信"),
        PUSH("推送");

        private final String description;

        SendChannel(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 发送状态枚举
     */
    public enum SendStatus {
        PENDING("待发送"),
        SUCCESS("成功"),
        FAILED("失败");

        private final String description;

        SendStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
