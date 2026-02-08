package com.fisco.app.entity.user;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 管理员实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "admin", indexes = {
    @Index(name = "idx_username", columnList = "username"),
    @Index(name = "idx_email", columnList = "email"),
    @Index(name = "idx_status", columnList = "status")
})
public class Admin {

    /**
     * 管理员ID（UUID格式，主键）
     */
    @Id
    @Column(name = "id", nullable = false, unique = true, length = 36)
    @ApiModelProperty(value = "管理员ID（UUID格式）", required = true, example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String id;

    @Column(name = "username", nullable = false, unique = true, length = 100)
    @ApiModelProperty(value = "管理员用户名（用于登录）", required = true, example = "admin")
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    private String username;

    @Column(name = "email", unique = true, length = 150)
    @ApiModelProperty(value = "管理员邮箱", example = "admin@system.com")
    @Email(message = "邮箱格式不正确")
    @Size(max = 150, message = "邮箱长度不能超过150")
    private String email;

    @Column(name = "real_name", length = 100)
    @ApiModelProperty(value = "真实姓名", example = "张三")
    @Size(max = 100, message = "真实姓名长度不能超过100")
    private String realName;

    @Column(name = "phone", unique = true, length = 20)
    @ApiModelProperty(value = "联系电话", example = "13800138000")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    @ApiModelProperty(value = "管理员角色", required = true, notes = "SUPER_ADMIN-超级管理员, ADMIN-管理员, AUDITOR-审核员", example = "ADMIN")
    @NotNull(message = "管理员角色不能为空")
    private AdminRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @ApiModelProperty(value = "管理员状态", notes = "ACTIVE-已激活, DISABLED-已禁用, LOCKED-已锁定", example = "ACTIVE")
    private AdminStatus status = AdminStatus.ACTIVE;

    @Column(name = "password", length = 255)
    @ApiModelProperty(value = "登录密码（加密存储）", hidden = true)
    @JsonIgnore
    private String password;

    @Column(name = "last_login_at")
    @ApiModelProperty(value = "最后登录时间", hidden = true)
    private LocalDateTime lastLoginAt;

    @Column(name = "last_login_ip", length = 50)
    @ApiModelProperty(value = "最后登录IP", hidden = true)
    private String lastLoginIp;

    @Column(name = "login_count")
    @ApiModelProperty(value = "登录次数", hidden = true)
    private Integer loginCount = 0;

    @Column(name = "failed_login_attempts")
    @ApiModelProperty(value = "失败登录尝试次数", hidden = true)
    private Integer failedLoginAttempts = 0;

    @Column(name = "locked_until")
    @ApiModelProperty(value = "锁定到期时间", hidden = true)
    private LocalDateTime lockedUntil;

    @Column(name = "remarks", columnDefinition = "TEXT")
    @ApiModelProperty(value = "备注信息", example = "系统管理员")
    private String remarks;

    @Column(name = "created_at")
    @ApiModelProperty(value = "创建时间", hidden = true)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @ApiModelProperty(value = "更新时间", hidden = true)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 50)
    @ApiModelProperty(value = "创建人", hidden = true)
    private String createdBy;

    @Column(name = "updated_by", length = 50)
    @ApiModelProperty(value = "更新人", hidden = true)
    private String updatedBy;

    /**
     * 管理员角色枚举
     */
    public enum AdminRole {
        SUPER_ADMIN,  // 超级管理员 - 拥有所有权限
        ADMIN,        // 管理员 - 拥有大部分管理权限
        AUDITOR       // 审核员 - 仅负责企业审核
    }

    /**
     * 管理员状态枚举
     */
    public enum AdminStatus {
        ACTIVE,   // 已激活
        DISABLED, // 已禁用
        LOCKED    // 已锁定
    }

    @PrePersist
    protected void onCreate() {
        if (id == null || id.isEmpty()) {
            id = java.util.UUID.randomUUID().toString();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 检查账户是否被锁定
     */
    public boolean isLocked() {
        if (status == AdminStatus.LOCKED && lockedUntil != null) {
            // 如果锁定时间已过，自动解锁
            if (LocalDateTime.now().isAfter(lockedUntil)) {
                status = AdminStatus.ACTIVE;
                lockedUntil = null;
                failedLoginAttempts = 0;
                return false;
            }
            return true;
        }
        return status == AdminStatus.LOCKED;
    }

    /**
     * 检查账户是否可用
     */
    public boolean isAvailable() {
        return status == AdminStatus.ACTIVE && !isLocked();
    }

    /**
     * 增加失败登录次数
     */
    public void incrementFailedAttempts() {
        this.failedLoginAttempts = (this.failedLoginAttempts == null ? 0 : this.failedLoginAttempts) + 1;
        // 失败5次锁定账户30分钟
        if (this.failedLoginAttempts >= 5) {
            this.status = AdminStatus.LOCKED;
            this.lockedUntil = LocalDateTime.now().plusMinutes(30);
        }
    }

    /**
     * 重置失败登录次数
     */
    public void resetFailedAttempts() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
        if (this.status == AdminStatus.LOCKED) {
            this.status = AdminStatus.ACTIVE;
        }
    }

    /**
     * 更新登录信息
     */
    public void updateLoginInfo(String ip) {
        this.lastLoginAt = LocalDateTime.now();
        this.lastLoginIp = ip;
        this.loginCount = (this.loginCount == null ? 0 : this.loginCount) + 1;
        this.resetFailedAttempts();
    }
}
