package com.fisco.app.entity.enterprise;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.PrePersist;
import javax.persistence.Table;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 企业审核日志实体类
 * 记录企业注册审核的历史操作
 */
@Data
@Entity
@Table(name = "enterprise_audit_log", indexes = {
    @Index(name = "idx_audit_enterprise", columnList = "enterprise_address"),
    @Index(name = "idx_audit_auditor", columnList = "auditor"),
    @Index(name = "idx_audit_time", columnList = "audit_time"),
    @Index(name = "idx_audit_action", columnList = "action")
})
@ApiModel(value = "企业审核日志", description = "企业注册审核操作记录实体")
@Schema(name = "企业审核日志")
public class EnterpriseAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(value = "主键ID", hidden = true)
    private Long id;

    @Column(name = "enterprise_address", nullable = false, length = 64)
    @ApiModelProperty(value = "企业区块链地址", required = true)
    private String enterpriseAddress;

    @Column(name = "enterprise_name", length = 255)
    @ApiModelProperty(value = "企业名称")
    private String enterpriseName;

    @Column(name = "auditor", nullable = false, length = 100)
    @ApiModelProperty(value = "审核人用户名", required = true)
    private String auditor;

    @Column(name = "action", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @ApiModelProperty(value = "审核动作", required = true)
    private AuditAction action;

    @Column(name = "reason", columnDefinition = "TEXT")
    @ApiModelProperty(value = "审核/拒绝理由")
    private String reason;

    @Column(name = "audit_time", nullable = false)
    @ApiModelProperty(value = "审核时间", required = true)
    private LocalDateTime auditTime;

    @Column(name = "ip_address", length = 50)
    @ApiModelProperty(value = "审核人IP地址")
    private String ipAddress;

    @Column(name = "tx_hash", length = 128)
    @ApiModelProperty(value = "区块链交易哈希")
    private String txHash;

    @Column(name = "remarks", columnDefinition = "TEXT")
    @ApiModelProperty(value = "备注信息")
    private String remarks;

    /**
     * 审核动作枚举
     */
    public enum AuditAction {
        APPROVE,            // 审核通过（注册）
        REJECT,             // 拒绝审核（注册）
        SUSPEND,            // 暂停企业
        ACTIVATE,           // 激活企业
        BLACKLIST,          // 拉黑企业
        REQUEST_DELETE,     // 请求注销企业
        APPROVE_DELETE,     // 审核通过注销企业
        REJECT_DELETE       // 拒绝注销企业
    }

    @PrePersist
    protected void onCreate() {
        if (auditTime == null) {
            auditTime = LocalDateTime.now();
        }
    }
}
