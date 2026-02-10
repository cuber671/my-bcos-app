package com.fisco.app.entity.blockchain;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 智能合约元数据实体
 * 用于存储已部署合约的完整元数据信息
 *
 * @author FISCO BCOS Team
 * @since 2026-02-10
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "blockchain_contract_metadata", indexes = {
    @Index(name = "idx_contract_address", columnList = "contract_address"),
    @Index(name = "idx_contract_type", columnList = "contract_type"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_deployment_timestamp", columnList = "deployment_timestamp")
})
public class ContractMetadata {

    @Id
    @Column(name = "id", nullable = false, unique = true, length = 36)
    private String id;

    @Column(name = "contract_address", nullable = false, unique = true, length = 42)
    private String contractAddress;

    @Column(name = "contract_name", nullable = false, length = 100)
    private String contractName;

    @Column(name = "contract_type", nullable = false, length = 50)
    private String contractType;

    @Column(name = "contract_version", length = 20)
    private String contractVersion;

    @Column(name = "abi", columnDefinition = "TEXT")
    private String abi;

    @Column(name = "bytecode", columnDefinition = "TEXT")
    private String bytecode;

    @Column(name = "source_code", columnDefinition = "TEXT")
    private String sourceCode;

    @Column(name = "compiler_version", length = 50)
    private String compilerVersion;

    @Column(name = "optimization_enabled")
    private Boolean optimizationEnabled = false;

    @Column(name = "constructor_params", columnDefinition = "TEXT")
    private String constructorParams;

    @Column(name = "deploy_transaction_hash", length = 66)
    private String deployTransactionHash;

    @Column(name = "deployer_address", length = 42)
    private String deployerAddress;

    @Column(name = "deploy_block_number")
    private Long deployBlockNumber;

    @Column(name = "deployment_timestamp")
    private LocalDateTime deploymentTimestamp;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "tags", length = 500)
    private String tags;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 合约状态枚举
     */
    public enum ContractStatus {
        ACTIVE("ACTIVE", "活跃"),
        DEPRECATED("DEPRECATED", "已弃用"),
        DESTROYED("DESTROYED", "已销毁");

        private final String code;
        private final String description;

        ContractStatus(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
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
