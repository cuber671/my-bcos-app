package com.fisco.app.entity.blockchain;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 智能合约事件实体
 * 用于存储合约触发的事件日志
 *
 * @author FISCO BCOS Team
 * @since 2026-02-10
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "blockchain_contract_event", indexes = {
    @Index(name = "idx_contract_address", columnList = "contract_address"),
    @Index(name = "idx_event_name", columnList = "event_name"),
    @Index(name = "idx_transaction_hash", columnList = "transaction_hash"),
    @Index(name = "idx_block_number", columnList = "block_number"),
    @Index(name = "idx_event_timestamp", columnList = "event_timestamp"),
    @Index(name = "idx_contract_event", columnList = "contract_address, event_name, block_number")
})
public class ContractEvent {

    @Id
    @Column(name = "id", nullable = false, unique = true, length = 36)
    private String id;

    @Column(name = "contract_address", nullable = false, length = 42)
    private String contractAddress;

    @Column(name = "event_name", nullable = false, length = 100)
    private String eventName;

    @Column(name = "event_signature", length = 100)
    private String eventSignature;

    @Column(name = "block_number", nullable = false)
    private Long blockNumber;

    @Column(name = "block_hash", length = 66)
    private String blockHash;

    @Column(name = "transaction_hash", nullable = false, length = 66)
    private String transactionHash;

    @Column(name = "transaction_index")
    private Integer transactionIndex;

    @Column(name = "log_index")
    private Integer logIndex;

    @Column(name = "event_data", columnDefinition = "TEXT")
    private String eventData;

    @Column(name = "topics", columnDefinition = "TEXT")
    private String topics;

    @Column(name = "decoded_params", columnDefinition = "TEXT")
    private String decodedParams;

    @Column(name = "event_timestamp")
    private LocalDateTime eventTimestamp;

    @Column(name = "processed")
    private Boolean processed = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null || id.isEmpty()) {
            id = java.util.UUID.randomUUID().toString();
        }
        createdAt = LocalDateTime.now();
    }
}
