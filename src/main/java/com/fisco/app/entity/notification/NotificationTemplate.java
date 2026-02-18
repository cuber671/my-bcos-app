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
 * 通知模板实体类
 * 管理系统通知模板
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "notification_template", indexes = {
    @Index(name = "idx_code", columnList = "code"),
    @Index(name = "idx_type", columnList = "type"),
    @Index(name = "idx_category", columnList = "category"),
    @Index(name = "idx_enabled", columnList = "is_enabled")
})
@Schema(name = "通知模板")
public class NotificationTemplate {

    @Id
    @Column(name = "id", nullable = false, unique = true, length = 36)
    @ApiModelProperty(value = "模板ID", required = true)
    private String id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    @ApiModelProperty(value = "模板代码", required = true)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    @ApiModelProperty(value = "模板名称", required = true)
    private String name;

    @Column(name = "type", nullable = false, length = 50)
    @ApiModelProperty(value = "通知类型", required = true)
    private String type;

    @Column(name = "category", length = 50)
    @ApiModelProperty(value = "通知分类")
    private String category;

    @Column(name = "title_template", nullable = false, length = 200)
    @ApiModelProperty(value = "标题模板", required = true)
    private String titleTemplate;

    @Column(name = "content_template", nullable = false, columnDefinition = "TEXT")
    @ApiModelProperty(value = "内容模板", required = true)
    private String contentTemplate;

    @Column(name = "action_type", length = 50)
    @ApiModelProperty(value = "默认操作类型")
    private String actionType;

    @Column(name = "priority", nullable = false, length = 20)
    @ApiModelProperty(value = "默认优先级", required = true)
    private String priority;

    @Column(name = "description", length = 500)
    @ApiModelProperty(value = "模板描述")
    private String description;

    @Column(name = "is_enabled")
    @ApiModelProperty(value = "是否启用")
    private Boolean isEnabled;

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
}
