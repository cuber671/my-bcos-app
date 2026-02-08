package com.fisco.app.entity.system;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.PrePersist;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Persistable;

/**
 * 权限审计日志实体
 * 记录所有权限检查和访问控制事件
 */
@Data
@Entity
@Table(name = "permission_audit_log", indexes = {
    @Index(name = "idx_username", columnList = "username"),
    @Index(name = "idx_enterprise_id", columnList = "enterprise_id"),
    @Index(name = "idx_permission_type", columnList = "permission_type"),
    @Index(name = "idx_access_granted", columnList = "access_granted"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@ApiModel(value = "权限审计日志", description = "记录权限检查和访问控制事件")
public class PermissionAuditLog implements Persistable<String> {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    @ApiModelProperty(value = "日志ID（UUID格式）")
    private String id;

    @Column(name = "username", nullable = false, length = 100)
    @ApiModelProperty(value = "操作用户名")
    private String username;

    @Column(name = "enterprise_id", length = 36)
    @ApiModelProperty(value = "用户所属企业ID")
    private String enterpriseId;

    @Column(name = "user_role", length = 50)
    @ApiModelProperty(value = "用户角色")
    private String userRole;

    @Column(name = "login_type", length = 20)
    @ApiModelProperty(value = "登录类型（USER, ADMIN, ENTERPRISE）")
    private String loginType;

    @Column(name = "permission_type", nullable = false, length = 50)
    @ApiModelProperty(value = "权限检查类型（ENTERPRISE_ACCESS, ROLE_CHECK, USER_APPROVAL等）")
    private String permissionType;

    @Column(name = "target_resource", length = 255)
    @ApiModelProperty(value = "目标资源（如企业ID、用户ID等）")
    private String targetResource;

    @Column(name = "operation", length = 100)
    @ApiModelProperty(value = "执行的操作（如approveUser, rejectUser等）")
    private String operation;

    @Column(name = "access_granted", nullable = false)
    @ApiModelProperty(value = "是否授予权限")
    private Boolean accessGranted;

    @Column(name = " denial_reason", columnDefinition = "TEXT")
    @ApiModelProperty(value = "拒绝原因")
    private String denialReason;

    @Column(name = "ip_address", length = 50)
    @ApiModelProperty(value = "客户端IP地址")
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    @ApiModelProperty(value = "客户端User-Agent")
    private String userAgent;

    @Column(name = "request_method", length = 10)
    @ApiModelProperty(value = "HTTP请求方法（GET, POST, PUT等）")
    private String requestMethod;

    @Column(name = "request_uri", length = 500)
    @ApiModelProperty(value = "请求URI")
    private String requestUri;

    @Column(name = "details", columnDefinition = "TEXT")
    @ApiModelProperty(value = "详细信息（JSON格式）")
    private String details;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updatedAt;

    @Transient
    private boolean isNew = true;

    @Override
    public boolean isNew() {
        // 如果ID为null，则是新实体
        if (id == null) {
            return true;
        }
        // 否则检查数据库中是否存在
        return isNew;
    }

    @PrePersist
    protected void onCreate() {
        if (id == null || id.isEmpty()) {
            id = java.util.UUID.randomUUID().toString();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        isNew = false;
    }
}
