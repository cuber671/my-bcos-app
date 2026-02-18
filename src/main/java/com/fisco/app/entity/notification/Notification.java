package com.fisco.app.entity.notification;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 通知实体类
 * 管理系统各类通知消息
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "notification", indexes = {
    @Index(name = "idx_recipient", columnList = "recipient_id"),
    @Index(name = "idx_recipient_type", columnList = "recipient_type"),
    @Index(name = "idx_sender", columnList = "sender_id"),
    @Index(name = "idx_type", columnList = "type"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_priority", columnList = "priority"),
    @Index(name = "idx_business", columnList = "business_type,business_id"),
    @Index(name = "idx_created_at", columnList = "created_at"),
    @Index(name = "idx_recipient_status", columnList = "recipient_id,status"),
    @Index(name = "idx_recipient_type_status", columnList = "recipient_type,status")
})
@Schema(name = "通知")
public class Notification {

    @Id
    @Column(name = "id", nullable = false, unique = true, length = 36)
    @ApiModelProperty(value = "通知ID", required = true)
    private String id;

    @Column(name = "recipient_id", nullable = false, length = 36)
    @ApiModelProperty(value = "接收者用户ID", required = true)
    private String recipientId;

    @Column(name = "recipient_type", nullable = false, length = 20)
    @ApiModelProperty(value = "接收者类型: USER, ENTERPRISE, ROLE", required = true)
    private String recipientType;

    @Column(name = "sender_id", length = 36)
    @ApiModelProperty(value = "发送者用户ID")
    private String senderId;

    @Column(name = "sender_type", length = 20)
    @ApiModelProperty(value = "发送者类型: SYSTEM, USER, ENTERPRISE")
    private String senderType;

    @Column(name = "type", nullable = false, length = 50)
    @ApiModelProperty(value = "通知类型: SYSTEM, APPROVAL, RISK, WARNING, BUSINESS, REMINDER", required = true)
    private String type;

    @Column(name = "category", length = 50)
    @ApiModelProperty(value = "通知分类: 用于更细粒度的分类")
    private String category;

    @Column(name = "title", nullable = false, length = 200)
    @ApiModelProperty(value = "通知标题", required = true)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    @ApiModelProperty(value = "通知内容", required = true)
    private String content;

    @Column(name = "priority", nullable = false, length = 20)
    @ApiModelProperty(value = "优先级: LOW, NORMAL, HIGH, URGENT", required = true)
    private String priority;

    @Column(name = "status", nullable = false, length = 20)
    @ApiModelProperty(value = "状态: UNREAD, READ, ARCHIVED, DELETED", required = true)
    private String status;

    @Column(name = "action_type", length = 50)
    @ApiModelProperty(value = "操作类型: APPROVE, REJECT, VIEW, DOWNLOAD等")
    private String actionType;

    @Column(name = "action_url", length = 500)
    @ApiModelProperty(value = "操作链接")
    private String actionUrl;

    @Column(name = "action_params", columnDefinition = "TEXT")
    @ApiModelProperty(value = "操作参数(JSON格式)")
    private String actionParams;

    @Column(name = "business_type", length = 50)
    @ApiModelProperty(value = "业务类型: BILL, RECEIVABLE, EWR, PLEDGE等")
    private String businessType;

    @Column(name = "business_id", length = 36)
    @ApiModelProperty(value = "业务记录ID")
    private String businessId;

    @Column(name = "extra_data", columnDefinition = "TEXT")
    @ApiModelProperty(value = "额外数据(JSON格式)")
    private String extraData;

    @Column(name = "is_sent")
    @ApiModelProperty(value = "是否已发送")
    private Boolean isSent;

    @Column(name = "sent_at")
    @ApiModelProperty(value = "发送时间")
    private LocalDateTime sentAt;

    @Column(name = "read_at")
    @ApiModelProperty(value = "阅读时间")
    private LocalDateTime readAt;

    @Column(name = "expire_at")
    @ApiModelProperty(value = "过期时间")
    private LocalDateTime expireAt;

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
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 通知类型枚举
     */
    public enum NotificationType {
        SYSTEM("系统通知"),
        APPROVAL("审批通知"),
        RISK("风险通知"),
        WARNING("预警通知"),
        BUSINESS("业务通知"),
        REMINDER("提醒通知");

        private final String description;

        NotificationType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 通知优先级枚举
     */
    public enum NotificationPriority {
        LOW("低"),
        NORMAL("普通"),
        HIGH("高"),
        URGENT("紧急");

        private final String description;

        NotificationPriority(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 通知状态枚举
     */
    public enum NotificationStatus {
        UNREAD("未读"),
        READ("已读"),
        ARCHIVED("已归档"),
        DELETED("已删除");

        private final String description;

        NotificationStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 接收者类型枚举
     */
    public enum RecipientType {
        USER("用户"),
        ENTERPRISE("企业"),
        ROLE("角色");

        private final String description;

        RecipientType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 发送者类型枚举
     */
    public enum SenderType {
        SYSTEM("系统"),
        USER("用户"),
        ENTERPRISE("企业");

        private final String description;

        SenderType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
