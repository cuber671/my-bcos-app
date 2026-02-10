package com.fisco.app.entity.blockchain;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 交易池管理实体
 * 用于管理待处理的区块链交易
 *
 * @author FISCO BCOS Team
 * @since 2026-02-10
 */
@Data
@Entity
@Table(name = "blockchain_transaction_pool", indexes = {
    @Index(name = "idx_transaction_hash", columnList = "transaction_hash"),
    @Index(name = "idx_from_address", columnList = "from_address"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_submitted_at", columnList = "submitted_at")
})
public class TransactionPool {

    @Id
    @Column(name = "id", nullable = false, unique = true, length = 36)
    private String id;

    @Column(name = "transaction_hash", nullable = false, unique = true, length = 66)
    private String transactionHash;

    @Column(name = "from_address", nullable = false, length = 42)
    private String fromAddress;

    @Column(name = "to_address", length = 42)
    private String toAddress;

    @Column(name = "gas_price")
    private Long gasPrice;

    @Column(name = "gas_limit")
    private Long gasLimit;

    @Column(name = "nonce")
    private Long nonce;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "status", length = 20)
    @Enumerated(EnumType.STRING)
    private PoolStatus status = PoolStatus.PENDING;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "last_retry_at")
    private LocalDateTime lastRetryAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 交易池状态枚举
     */
    public enum PoolStatus {
        PENDING,       // 待处理
        QUEUED,        // 排队中
        PROCESSING,    // 处理中
        CONFIRMED      // 已确认
    }

    @PrePersist
    protected void onCreate() {
        if (id == null || id.isEmpty()) {
            id = java.util.UUID.randomUUID().toString();
        }
        if (submittedAt == null) {
            submittedAt = LocalDateTime.now();
        }
    }
}
