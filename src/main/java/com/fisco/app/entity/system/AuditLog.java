package com.fisco.app.entity.system;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
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
 * 审计日志实体类
 * 记录系统中所有重要操作的审计追踪信息
 */
@Data
@Entity
@Table(name = "audit_log", indexes = {
    @Index(name = "idx_audit_user", columnList = "user_address"),
    @Index(name = "idx_audit_entity_type", columnList = "entity_type"),
    @Index(name = "idx_audit_entity_id", columnList = "entity_id"),
    @Index(name = "idx_audit_action", columnList = "action_type"),
    @Index(name = "idx_audit_time", columnList = "created_at"),
    @Index(name = "idx_audit_module", columnList = "module")
})
@ApiModel(value = "审计日志", description = "系统操作审计日志实体")
@Schema(name = "审计日志")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(value = "主键ID", hidden = true)
    private Long id;

    /**
     * 操作人地址
     */
    @Column(name = "user_address", length = 42)
    @ApiModelProperty(value = "操作人地址", example = "0x1234567890abcdef1234567890abcdef12345678")
    private String userAddress;

    /**
     * 操作人姓名（冗余字段）
     */
    @Column(name = "user_name", length = 100)
    @ApiModelProperty(value = "操作人姓名", example = "张三")
    private String userName;

    /**
     * 操作模块
     */
    @Column(name = "module", nullable = false, length = 50)
    @ApiModelProperty(value = "操作模块", required = true, example = "BILL", notes = "如：BILL, RECEIVABLE, WAREHOUSE_RECEIPT, ENTERPRISE, USER")
    private String module;

    /**
     * 操作类型（CREATE, UPDATE, DELETE, QUERY等）
     */
    @Column(name = "action_type", nullable = false, length = 20)
    @ApiModelProperty(value = "操作类型", required = true, example = "CREATE", notes = "CREATE-创建, UPDATE-更新, DELETE-删除, QUERY-查询")
    private String actionType;

    /**
     * 操作描述
     */
    @Column(name = "action_desc", nullable = false, length = 200)
    @ApiModelProperty(value = "操作描述", required = true, example = "创建票据")
    private String actionDesc;

    /**
     * 实体类型（Bill, WarehouseReceipt, Enterprise等）
     */
    @Column(name = "entity_type", length = 50)
    @ApiModelProperty(value = "实体类型", example = "Bill")
    private String entityType;

    /**
     * 实体ID
     */
    @Column(name = "entity_id", length = 64)
    @ApiModelProperty(value = "实体ID", example = "bill001")
    private String entityId;

    /**
     * 操作前的数据（JSON格式）
     */
    @Column(name = "old_value", columnDefinition = "TEXT")
    @ApiModelProperty(value = "操作前的数据", notes = "JSON格式，记录操作前的数据状态")
    private String oldValue;

    /**
     * 操作后的数据（JSON格式）
     */
    @Column(name = "new_value", columnDefinition = "TEXT")
    @ApiModelProperty(value = "操作后的数据", notes = "JSON格式，记录操作后的数据状态")
    private String newValue;

    /**
     * 变更的字段（JSON格式）
     */
    @Column(name = "changed_fields", columnDefinition = "TEXT")
    @ApiModelProperty(value = "变更的字段", notes = "JSON格式，记录具体变更的字段列表")
    private String changedFields;

    /**
     * 请求方法（GET, POST, PUT, DELETE等）
     */
    @Column(name = "request_method", length = 10)
    @ApiModelProperty(value = "请求方法", example = "POST")
    private String requestMethod;

    /**
     * 请求URL
     */
    @Column(name = "request_url", length = 500)
    @ApiModelProperty(value = "请求URL", example = "/api/bill/create")
    private String requestUrl;

    /**
     * 请求IP地址
     */
    @Column(name = "request_ip", length = 50)
    @ApiModelProperty(value = "请求IP地址", example = "192.168.1.100")
    private String requestIp;

    /**
     * User-Agent
     */
    @Column(name = "user_agent", length = 500)
    @ApiModelProperty(value = "用户代理", example = "Mozilla/5.0...")
    private String userAgent;

    /**
     * 操作结果（SUCCESS, FAILURE）
     */
    @Column(name = "result", length = 20)
    @ApiModelProperty(value = "操作结果", example = "SUCCESS", notes = "SUCCESS-成功, FAILURE-失败")
    private String result;

    /**
     * 错误信息
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    @ApiModelProperty(value = "错误信息", notes = "操作失败时记录错误详情")
    private String errorMessage;

    /**
     * 操作时长（毫秒）
     */
    @Column(name = "duration")
    @ApiModelProperty(value = "操作时长（毫秒）", example = "150")
    private Long duration;

    /**
     * 区块链交易哈希（如果是区块链操作）
     */
    @Column(name = "tx_hash", length = 66)
    @ApiModelProperty(value = "区块链交易哈希", example = "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890")
    private String txHash;

    /**
     * 操作是否成功
     */
    @Column(name = "is_success", nullable = false)
    @ApiModelProperty(value = "操作是否成功", required = true, example = "true")
    private Boolean isSuccess = true;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @ApiModelProperty(value = "创建时间", required = true, example = "2024-01-01T10:00:00")
    private LocalDateTime createdAt;

    /**
     * 租户ID（用于多租户系统）
     */
    @Column(name = "tenant_id", length = 64)
    @ApiModelProperty(value = "租户ID", example = "tenant001")
    private String tenantId;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isSuccess == null) {
            isSuccess = true;
        }
    }
}
