package com.fisco.app.entity.user;

import com.fisco.app.entity.enterprise.Enterprise;

import lombok.Data;
import lombok.EqualsAndHashCode;

import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;


import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;

import javax.persistence.*;

/**
 * 用户实体类
 * 管理系统用户（企业员工、管理员等）
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "user", indexes = {
    @Index(name = "idx_username", columnList = "username"),
    @Index(name = "idx_email", columnList = "email"),
    @Index(name = "idx_enterprise_id", columnList = "enterprise_id"),
    @Index(name = "idx_status", columnList = "status")
})
@Schema(name = "用户")
public class User {

    @Id
    @Column(name = "id", nullable = false, unique = true, length = 36)
    @ApiModelProperty(value = "用户ID（UUID格式）", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
    private String id;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    @ApiModelProperty(value = "用户名（登录账号）", required = true, example = "zhangsan")
    private String username;

    @Column(name = "password", nullable = false, length = 255)
    @ApiModelProperty(value = "登录密码（BCrypt加密）", hidden = true)
    private String password;

    @Column(name = "real_name", length = 100)
    @ApiModelProperty(value = "真实姓名", example = "张三")
    private String realName;

    @Column(name = "email", length = 100)
    @ApiModelProperty(value = "电子邮箱", example = "zhangsan@example.com")
    private String email;

    @Column(name = "phone", length = 20)
    @ApiModelProperty(value = "手机号码", example = "13800138000")
    private String phone;

    @Column(name = "enterprise_id", length = 36)
    @ApiModelProperty(value = "所属企业ID（UUID）", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String enterpriseId;

    /**
     * 关联的企业对象（只读，通过enterpriseId映射）
     * 使用LAZY加载避免N+1查询问题
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enterprise_id", insertable = false, updatable = false)
    @ApiModelProperty(value = "所属企业对象", hidden = true)
    private Enterprise enterprise;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false, length = 20)
    @ApiModelProperty(value = "用户类型", required = true, notes = "ADMIN-系统管理员, ENTERPRISE_ADMIN-企业管理员, ENTERPRISE_USER-企业用户, AUDITOR-审计员, OPERATOR-操作员")
    private UserType userType = UserType.ENTERPRISE_USER;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @ApiModelProperty(value = "用户状态", required = true, notes = "ACTIVE-正常, DISABLED-禁用, LOCKED-锁定")
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "department", length = 100)
    @ApiModelProperty(value = "部门", example = "财务部")
    private String department;

    @Column(name = "position", length = 100)
    @ApiModelProperty(value = "职位", example = "财务经理")
    private String position;

    @Column(name = "avatar_url", length = 500)
    @ApiModelProperty(value = "头像URL")
    private String avatarUrl;

    @Column(name = "last_login_time")
    @ApiModelProperty(value = "最后登录时间", hidden = true)
    private LocalDateTime lastLoginTime;

    @Column(name = "last_login_ip", length = 50)
    @ApiModelProperty(value = "最后登录IP", hidden = true)
    private String lastLoginIp;

    @Column(name = "login_count")
    @ApiModelProperty(value = "登录次数", hidden = true)
    private Integer loginCount = 0;

    @Column(name = "password_changed_at")
    @ApiModelProperty(value = "密码修改时间", hidden = true)
    private LocalDateTime passwordChangedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @ApiModelProperty(value = "创建时间", hidden = true)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @ApiModelProperty(value = "更新时间", hidden = true)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 50)
    @ApiModelProperty(value = "创建者标识", hidden = true,
        notes = "记录创建用户的操作者。公开注册：固定为'SELF_REGISTER'；管理员代注册：管理员用户名",
        example = "SELF_REGISTER")
    private String createdBy;

    @Column(name="updated_by", length = 50)
    @ApiModelProperty(value = "更新人", hidden = true)
    private String updatedBy;

    @Column(name = "invitation_code", length = 32)
    @ApiModelProperty(value = "注册时使用的邀请码", example = "INV2024011812345678")
    private String invitationCode;

    @Column(name = "registration_remarks", columnDefinition = "TEXT")
    @ApiModelProperty(value = "注册备注信息", example = "用户注册申请备注")
    private String registrationRemarks;

    /**
     * 用户类型枚举
     */
    public enum UserType {
        ADMIN,              // 系统管理员
        ENTERPRISE_ADMIN,   // 企业管理员 - 管理企业内部用户、邀请码等
        ENTERPRISE_USER,    // 企业用户 - 普通企业员工
        AUDITOR,            // 审计员
        OPERATOR            // 操作员
    }

    /**
     * 用户状态枚举
     */
    public enum UserStatus {
        ACTIVE,     // 正常
        DISABLED,   // 禁用
        LOCKED,     // 锁定
        PENDING     // 待审核
    }

    @PrePersist
    protected void onCreate() {
        if (id == null || id.isEmpty()) {
            id = UUID.randomUUID().toString();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (passwordChangedAt == null) {
            passwordChangedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 检查账户是否被锁定
     */
    public boolean isLocked() {
        return status == UserStatus.LOCKED;
    }

    /**
     * 检查账户是否被禁用
     */
    public boolean isDisabled() {
        return status == UserStatus.DISABLED;
    }

    /**
     * 检查账户是否可用
     */
    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }
}
