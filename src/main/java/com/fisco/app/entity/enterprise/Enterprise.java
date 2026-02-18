package com.fisco.app.entity.enterprise;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fisco.app.entity.user.User;

import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 企业实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "enterprise", indexes = {
    @Index(name = "idx_address", columnList = "address"),
    @Index(name = "idx_credit_code", columnList = "credit_code"),
    @Index(name = "idx_username", columnList = "username"),
    @Index(name = "idx_email", columnList = "email"),
    @Index(name = "idx_phone", columnList = "phone"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_role", columnList = "role")
})
@Schema(name = "企业")
public class Enterprise {

    /**
     * 企业ID（UUID格式，主键）
     */
    @Id
    @Column(name = "id", nullable = false, unique = true, length = 36)
    @ApiModelProperty(value = "企业ID（UUID格式）", required = true, example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String id;

    @Column(name = "address", nullable = false, unique = true, length = 42)
    @ApiModelProperty(value = "区块链地址", required = true, example = "0x1234567890abcdef1234567890abcdef12345678", notes = "如果不提供，系统将自动生成随机地址")
    @Size(min = 42, max = 42, message = "区块链地址必须是42位")
    @Pattern(regexp = "^0x[a-fA-F0-9]{40}$", message = "区块链地址格式不正确，必须是0x开头的40位十六进制字符")
    private String address;

    @Column(name = "name", nullable = false)
    @ApiModelProperty(value = "企业名称", required = true, example = "供应商A")
    @NotBlank(message = "企业名称不能为空")
    @Size(min = 2, max = 255, message = "企业名称长度必须在2-255之间")
    private String name;

    @Column(name = "credit_code", nullable = false, unique = true, length = 50)
    @ApiModelProperty(value = "统一社会信用代码", required = true, example = "91110000MA001234XY")
    @NotBlank(message = "统一社会信用代码不能为空")
    @Size(min = 18, max = 18, message = "统一社会信用代码必须是18位")
    @Pattern(regexp = "^[0-9A-HJ-NPQ-TV-Z]{18}$", message = "统一社会信用代码格式不正确")
    private String creditCode;

    @Column(name = "username", nullable = false, unique = true, length = 100)
    @ApiModelProperty(value = "用户名（用于登录）", required = true, example = "enterprise_001", notes = "用于系统登录，唯一标识，建议使用英文、数字、下划线组合")
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    private String username;

    @Column(name = "email", unique = true, length = 150)
    @ApiModelProperty(value = "企业邮箱（用于登录和通知）", example = "contact@company.com", notes = "可用于登录，唯一标识")
    @Email(message = "邮箱格式不正确")
    @Size(max = 150, message = "邮箱长度不能超过150")
    private String email;

    @Column(name = "phone", unique = true, length = 20)
    @ApiModelProperty(value = "企业联系电话（用于登录和通知）", example = "13800138000", notes = "可用于登录，唯一标识")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Column(name = "enterprise_address", length = 500)
    @ApiModelProperty(value = "企业地址", example = "北京市朝阳区")
    @Size(max = 500, message = "企业地址长度不能超过500")
    private String enterpriseAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    @ApiModelProperty(value = "企业角色", required = true, notes = "SUPPLIER-供应商, CORE_ENTERPRISE-核心企业, FINANCIAL_INSTITUTION-金融机构, REGULATOR-监管机构, WAREHOUSE_PROVIDER-仓储方", example = "SUPPLIER")
    @NotNull(message = "企业角色不能为空")
    private EnterpriseRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @ApiModelProperty(value = "企业状态", notes = "PENDING-待审核, ACTIVE-已激活, SUSPENDED-已暂停, BLACKLISTED-已拉黑, PENDING_DELETION-待删除（注销审核中）, DELETED-已删除（区块链上标记为已删除）", example = "ACTIVE")
    private EnterpriseStatus status = EnterpriseStatus.PENDING;

    @Column(name = "credit_rating")
    @ApiModelProperty(value = "信用评级(0-100)", example = "75")
    private Integer creditRating = 60;

    @Column(name = "credit_limit", precision = 20, scale = 2)
    @ApiModelProperty(value = "授信额度", example = "1000000.00")
    private BigDecimal creditLimit = BigDecimal.ZERO;

    @Column(name = "metadata_hash", length = 66, nullable = true)
    @ApiModelProperty(value = "元数据哈希（V2合约双层存储）", notes = "存储扩展数据的哈希值，对应V2合约的metadataHash")
    private String metadataHash;

    @Column(name = "registered_at")
    @ApiModelProperty(value = "注册时间", hidden = true)
    private LocalDateTime registeredAt;

    @Column(name = "updated_at")
    @ApiModelProperty(value = "更新时间", hidden = true)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 50)
    @ApiModelProperty(value = "创建者标识", hidden = true,
        notes = "记录创建企业的操作者。公开注册：固定为'SELF_REGISTER'；管理员代注册：管理员用户名",
        example = "SELF_REGISTER")
    private String createdBy;

    @Column(name = "updated_by", length = 50)
    @ApiModelProperty(value = "更新人", hidden = true)
    private String updatedBy;

    @Column(name = "password", length = 255)
    @ApiModelProperty(value = "登录密码（加密存储）", hidden = true)
    @JsonIgnore  // 确保密码永远不会序列化到JSON响应中
    private String password;

    @Column(name = "api_key", length = 64)
    @ApiModelProperty(value = "API密钥（用于程序化访问）", hidden = true)
    private String apiKey;

    @Column(name = "remarks", columnDefinition = "TEXT")
    @ApiModelProperty(value = "备注信息", example = "企业审核备注")
    private String remarks;

    /**
     * 企业的用户列表（一对多关联）
     * 使用LAZY加载避免N+1查询问题
     */
    @OneToMany(mappedBy = "enterprise", fetch = FetchType.LAZY)
    @JsonIgnore  // 防止JSON序列化时触发懒加载，避免LazyInitializationException
    @ApiModelProperty(value = "企业的用户列表", hidden = true)
    private java.util.List<User> users;

    public enum EnterpriseRole {
        SUPPLIER,           // 供应商
        CORE_ENTERPRISE,    // 核心企业
        FINANCIAL_INSTITUTION, // 金融机构
        REGULATOR,          // 监管机构
        WAREHOUSE_PROVIDER  // 仓储方 - 提供仓储服务，管理电子仓单
    }

    public enum EnterpriseStatus {
        PENDING,            // 待审核
        ACTIVE,             // 已激活
        SUSPENDED,          // 已暂停
        BLACKLISTED,        // 已拉黑
        PENDING_DELETION,   // 待删除（注销审核中）
        DELETED             // 已删除（区块链上标记为已删除）
    }

    @PrePersist
    protected void onCreate() {
        // 自动生成UUID作为主键
        if (id == null || id.isEmpty()) {
            id = java.util.UUID.randomUUID().toString();
        }
        registeredAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
