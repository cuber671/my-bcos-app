package com.fisco.app.entity.blockchain;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 交易回执实体
 * 用于存储区块链交易执行的回执信息
 *
 * @author FISCO BCOS Team
 * @since 2026-02-10
 */
@Data
@Entity
@Table(name = "blockchain_transaction_receipt", indexes = {
    @Index(name = "idx_transaction_hash", columnList = "transaction_hash"),
    @Index(name = "idx_block_number", columnList = "block_number"),
    @Index(name = "idx_status", columnList = "status")
})
public class TransactionReceiptEntity {

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

    @Column(name = "gas_used")
    private Long gasUsed;

    @Column(name = "cumulative_gas_used")
    private Long cumulativeGasUsed;

    @Column(name = "contract_address", length = 42)
    private String contractAddress;

    @Column(name = "status", nullable = false)
    private Integer status;

    @Column(name = "revert_reason", length = 255)
    private String revertReason;

    @Column(name = "logs_bloom", length = 1024)
    private String logsBloom;

    @Column(name = "transaction_created_at")
    private LocalDateTime transactionCreatedAt;

    @Column(name = "receipt_obtained_at")
    private LocalDateTime receiptObtainedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null || id.isEmpty()) {
            id = java.util.UUID.randomUUID().toString();
        }
        receiptObtainedAt = LocalDateTime.now();
    }
}
