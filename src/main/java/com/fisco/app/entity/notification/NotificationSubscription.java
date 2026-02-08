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
 * 通知订阅实体类
 * 管理用户通知订阅偏好
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "notification_subscription", indexes = {
    @Index(name = "idx_user", columnList = "user_id"),
    @Index(name = "idx_type", columnList = "notification_type")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_type", columnNames = {"user_id", "notification_type"})
})
@Schema(name = "通知订阅")
public class NotificationSubscription {

    @Id
    @Column(name = "id", nullable = false, unique = true, length = 36)
    @ApiModelProperty(value = "订阅ID", required = true)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    @ApiModelProperty(value = "用户ID", required = true)
    private String userId;

    @Column(name = "notification_type", nullable = false, length = 50)
    @ApiModelProperty(value = "通知类型", required = true)
    private String notificationType;

    @Column(name = "is_subscribed")
    @ApiModelProperty(value = "是否订阅")
    private Boolean isSubscribed;

    @Column(name = "notify_email")
    @ApiModelProperty(value = "是否邮件通知")
    private Boolean notifyEmail;

    @Column(name = "notify_sms")
    @ApiModelProperty(value = "是否短信通知")
    private Boolean notifySms;

    @Column(name = "notify_push")
    @ApiModelProperty(value = "是否推送通知")
    private Boolean notifyPush;

    @Column(name = "notify_in_app")
    @ApiModelProperty(value = "是否应用内通知")
    private Boolean notifyInApp;

    @Column(name = "created_at", nullable = false, updatable = false)
    @ApiModelProperty(value = "创建时间", required = true)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isSubscribed == null) {
            isSubscribed = true;
        }
        if (notifyEmail == null) {
            notifyEmail = false;
        }
        if (notifySms == null) {
            notifySms = false;
        }
        if (notifyPush == null) {
            notifyPush = true;
        }
        if (notifyInApp == null) {
            notifyInApp = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
