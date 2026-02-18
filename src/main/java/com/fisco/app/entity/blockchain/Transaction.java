package com.fisco.app.entity.blockchain;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 区块链交易实体
 * 用于存储区块链交易的完整信息
 *
 * @author FISCO BCOS Team
 * @since 2026-02-10
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "blockchain_transaction", indexes = {
    @Index(name = "idx_transaction_hash", columnList = "transaction_hash"),
    @Index(name = "idx_from_address", columnList = "from_address"),
    @Index(name = "idx_to_address", columnList = "to_address"),
    @Index(name = "idx_block_number", columnList = "block_number"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
public class Transaction {

    @Id
    @Column(name = "id", nullable = false, unique = true, length = 36)
    private String id;

    @Column(name = "transaction_hash", nullable = false, unique = true, length = 66)
    private String transactionHash;

    @Column(name = "block_number")
    private Long blockNumber;

    @Column(name = "block_hash", length = 66)
    private String blockHash;

    @Column(name = "transaction_index")
    private Integer transactionIndex;

    @Column(name = "from_address", nullable = false, length = 42)
    private String fromAddress;

    @Column(name = "to_address", length = 42)
    private String toAddress;

    @Column(name = "value", length = 50)
    private String value;

    @Column(name = "gas_price")
    private Long gasPrice;

    @Column(name = "gas_limit")
    private Long gasLimit;

    @Column(name = "gas_used")
    private Long gasUsed;

    @Column(name = "input_data", columnDefinition = "TEXT")
    private String inputData;

    @Column(name = "method_id", length = 10)
    private String methodId;

    @Column(name = "status", nullable = false)
    private Integer status = 0;

    @Column(name = "transaction_type", length = 20)
    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;

    @Column(name = "nonce")
    private Long nonce;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 交易类型枚举
     */
    public enum TransactionType {
        CONTRACT_CALL,      // 合约调用
        CONTRACT_DEPLOY,    // 合约部署
        VALUE_TRANSFER      // 价值转移
    }

    /**
     * 交易状态枚举
     */
    public enum TransactionStatus {
        CANCELLED(-2, "已取消"),
        FAILED(-1, "失败"),
        PENDING(0, "待处理"),
        PACKED(1, "已打包"),
        SUCCESS(2, "成功");

        private final Integer code;
        private final String description;

        TransactionStatus(Integer code, String description) {
            this.code = code;
            this.description = description;
        }

        public Integer getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        /**
         * 根据状态码获取枚举
         */
        public static TransactionStatus fromCode(Integer code) {
            for (TransactionStatus status : values()) {
                if (status.code.equals(code)) {
                    return status;
                }
            }
            return PENDING;
        }
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
}
