package com.fisco.app.entity.user;

import lombok.Data;
import lombok.EqualsAndHashCode;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import java.time.LocalDateTime;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import javax.persistence.*;

/**
 * 邀请码实体类
 * 用于企业邀请用户注册
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "invitation_code", indexes = {
    @Index(name = "idx_code", columnList = "code"),
    @Index(name = "idx_enterprise_id", columnList = "enterprise_id"),
    @Index(name = "idx_status", columnList = "status")
})
@Schema(name = "邀请码")
@ApiModel(value = "邀请码", description = "企业邀请用户注册的邀请码实体")
public class InvitationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @ApiModelProperty(value = "邀请码ID", hidden = true)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 32)
    @ApiModelProperty(value = "邀请码（唯一标识）", required = true, example = "INV2024011812345678")
    private String code;

    @Column(name = "enterprise_id", nullable = false, length = 36)
    @ApiModelProperty(value = "所属企业ID（UUID格式）", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
    private String enterpriseId;

    @Column(name = "enterprise_name", length = 200)
    @ApiModelProperty(value = "企业名称（冗余字段）", example = "供应商A")
    private String enterpriseName;

    @Column(name = "created_by", nullable = false, length = 50)
    @ApiModelProperty(value = "创建者标识", required = true,
        notes = "从JWT Token中提取。管理员生成：admin.username（如'admin'）；企业生成：enterprise.address（如'0x123...'）",
        example = "admin")
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    @ApiModelProperty(value = "创建时间", hidden = true)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    @ApiModelProperty(value = "过期时间", example = "2024-12-31T23:59:59")
    private LocalDateTime expiresAt;

    @Column(name = "max_uses")
    @ApiModelProperty(value = "最大使用次数", example = "100")
    private Integer maxUses;

    @Column(name = "used_count", nullable = false)
    @ApiModelProperty(value = "已使用次数", example = "5")
    private Integer usedCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @ApiModelProperty(value = "邀请码状态", required = true, notes = "ACTIVE-有效, EXPIRED-已过期, DISABLED-已禁用")
    private InvitationCodeStatus status = InvitationCodeStatus.ACTIVE;

    @Column(name = "remarks", columnDefinition = "TEXT")
    @ApiModelProperty(value = "备注信息", example = "用于招聘财务人员")
    private String remarks;

    /**
     * 邀请码状态枚举
     */
    public enum InvitationCodeStatus {
        ACTIVE,     // 有效
        EXPIRED,    // 已过期
        DISABLED    // 已禁用
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (usedCount == null) {
            usedCount = 0;
        }
        if (status == null) {
            status = InvitationCodeStatus.ACTIVE;
        }
    }

    /**
     * 检查邀请码是否有效
     */
    public boolean isValid() {
        if (status != InvitationCodeStatus.ACTIVE) {
            return false;
        }
        if (expiresAt != null && LocalDateTime.now().isAfter(expiresAt)) {
            return false;
        }
        if (maxUses != null && usedCount >= maxUses) {
            return false;
        }
        return true;
    }

    /**
     * 检查邀请码是否已过期
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * 检查是否已达到使用上限
     */
    public boolean isMaxUsesReached() {
        return maxUses != null && usedCount >= maxUses;
    }

    /**
     * 增加使用次数
     */
    public void incrementUsedCount() {
        this.usedCount++;
    }
}
