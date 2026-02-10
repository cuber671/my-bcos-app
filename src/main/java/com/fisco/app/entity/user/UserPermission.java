package com.fisco.app.entity.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 用户权限实体类
 * 管理用户的细粒度权限
 */
@Data
@Entity
@Table(name = "user_permission", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_permission_code", columnList = "permission_code"),
    @Index(name = "idx_resource_type", columnList = "resource_type"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@ApiModel(value = "用户权限", description = "用户权限实体")
@Schema(name = "用户权限")
public class UserPermission {

    @Id
    @Column(name = "id", nullable = false, unique = true, length = 36)
    @ApiModelProperty(value = "权限ID（UUID格式）", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
    private String id;

    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false, length = 36)
    @ApiModelProperty(value = "用户ID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
    private String userId;

    /**
     * 权限代码
     */
    @Column(name = "permission_code", nullable = false, length = 100)
    @ApiModelProperty(value = "权限代码", required = true, example = "bill:create",
            notes = "格式: 模块:操作，如 bill:create, bill:read, bill:update, bill:delete")
    private String permissionCode;

    /**
     * 权限名称
     */
    @Column(name = "permission_name", nullable = false, length = 100)
    @ApiModelProperty(value = "权限名称", required = true, example = "创建票据")
    private String permissionName;

    /**
     * 资源类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 50)
    @ApiModelProperty(value = "资源类型", required = true, notes = "BILL-票据, RECEIVABLE-应收账款, WAREHOUSE_RECEIPT-仓单, ENTERPRISE-企业, USER-用户, CREDIT-授信")
    private ResourceType resourceType;

    /**
     * 操作类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "operation", nullable = false, length = 20)
    @ApiModelProperty(value = "操作类型", required = true, notes = "CREATE-创建, READ-读取, UPDATE-更新, DELETE-删除, APPROVE-审核, EXPORT-导出, IMPORT-导入")
    private Operation operation;

    /**
     * 权限范围
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "scope", length = 20)
    @ApiModelProperty(value = "权限范围", example = "ALL",
            notes = "ALL-全部, OWN-仅自己, DEPARTMENT-部门, ENTERPRISE-企业")
    private PermissionScope scope = PermissionScope.OWN;

    /**
     * 是否启用
     */
    @Column(name = "is_enabled", nullable = false)
    @ApiModelProperty(value = "是否启用", required = true, example = "true")
    private Boolean isEnabled = true;

    /**
     * 是否过期
     */
    @Column(name = "is_expired", nullable = false)
    @ApiModelProperty(value = "是否过期", required = true, example = "false")
    private Boolean isExpired = false;

    /**
     * 过期时间
     */
    @Column(name = "expire_at")
    @ApiModelProperty(value = "过期时间", example = "2024-12-31T23:59:59")
    private LocalDateTime expireAt;

    /**
     * 备注
     */
    @Column(name = "remarks", length = 500)
    @ApiModelProperty(value = "备注", example = "财务部门票据创建权限")
    private String remarks;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @ApiModelProperty(value = "创建时间", required = true, example = "2024-01-01T10:00:00")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    @ApiModelProperty(value = "更新时间", example = "2024-01-01T10:00:00")
    private LocalDateTime updatedAt;

    /**
     * 创建者
     */
    @Column(name = "created_by", length = 50)
    @ApiModelProperty(value = "创建者", example = "admin")
    private String createdBy;

    /**
     * 更新者
     */
    @Column(name = "updated_by", length = 50)
    @ApiModelProperty(value = "更新者", example = "admin")
    private String updatedBy;

    /**
     * 资源类型枚举
     */
    public enum ResourceType {
        BILL,              // 票据
        RECEIVABLE,        // 应收账款
        WAREHOUSE_RECEIPT, // 仓单
        ENTERPRISE,        // 企业
        USER,              // 用户
        CREDIT,            // 授信
        ENDORSEMENT,       // 背书
        PLEDGE,            // 质押
        RISK,              // 风险
        AUDIT,             // 审计
        SYSTEM             // 系统
    }

    /**
     * 操作类型枚举
     */
    public enum Operation {
        CREATE,    // 创建
        READ,      // 读取
        UPDATE,    // 更新
        DELETE,    // 删除
        APPROVE,   // 审核
        EXPORT,    // 导出
        IMPORT,    // 导入
        MANAGE     // 管理
    }

    /**
     * 权限范围枚举
     */
    public enum PermissionScope {
        ALL,        // 全部
        OWN,        // 仅自己
        DEPARTMENT, // 部门
        ENTERPRISE  // 企业
    }

    @PrePersist
    protected void onCreate() {
        if (id == null || id.isEmpty()) {
            id = UUID.randomUUID().toString();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isEnabled == null) {
            isEnabled = true;
        }
        if (isExpired == null) {
            isExpired = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        // 检查是否过期
        if (expireAt != null && LocalDateTime.now().isAfter(expireAt)) {
            isExpired = true;
        }
    }

    /**
     * 检查权限是否有效
     */
    public boolean isValid() {
        return isEnabled && !isExpired &&
               (expireAt == null || LocalDateTime.now().isBefore(expireAt));
    }
}
