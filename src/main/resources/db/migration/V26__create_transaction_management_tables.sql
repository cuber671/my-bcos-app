-- ============================================================
-- 交易管理系统表结构
-- Version: V26
-- Author: FISCO BCOS Team
-- Date: 2026-02-10
-- Description: 创建区块链交易管理相关表，提供交易全生命周期管理
-- ============================================================

-- 交易记录表
CREATE TABLE IF NOT EXISTS blockchain_transaction (
    id VARCHAR(36) PRIMARY KEY COMMENT '交易ID（UUID）',
    transaction_hash VARCHAR(66) NOT NULL UNIQUE COMMENT '交易哈希',
    block_number BIGINT COMMENT '区块号',
    block_hash VARCHAR(66) COMMENT '区块哈希',
    transaction_index INT COMMENT '交易索引',
    from_address VARCHAR(42) NOT NULL COMMENT '发送地址',
    to_address VARCHAR(42) COMMENT '接收地址',
    value VARCHAR(50) DEFAULT '0' COMMENT '交易值（wei）',
    gas_price BIGINT COMMENT 'Gas价格',
    gas_limit BIGINT COMMENT 'Gas限制',
    gas_used BIGINT COMMENT '已使用Gas',
    input_data TEXT COMMENT '交易输入数据',
    method_id VARCHAR(10) COMMENT '方法签名',
    status INT NOT NULL DEFAULT 0 COMMENT '交易状态：-2-已取消, -1-失败, 0-待处理, 1-已打包, 2-成功',
    transaction_type VARCHAR(20) DEFAULT 'CONTRACT_CALL' COMMENT '交易类型：CONTRACT_CALL, CONTRACT_DEPLOY, VALUE_TRANSFER',
    nonce BIGINT COMMENT '交易nonce值',
    error_message TEXT COMMENT '错误信息（失败时）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_transaction_hash (transaction_hash),
    INDEX idx_from_address (from_address),
    INDEX idx_to_address (to_address),
    INDEX idx_block_number (block_number),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='区块链交易记录表';

-- 交易回执表
CREATE TABLE IF NOT EXISTS blockchain_transaction_receipt (
    id VARCHAR(36) PRIMARY KEY COMMENT '回执ID（UUID）',
    transaction_hash VARCHAR(66) NOT NULL UNIQUE COMMENT '交易哈希',
    block_number BIGINT COMMENT '区块号',
    block_hash VARCHAR(66) COMMENT '区块哈希',
    transaction_index INT COMMENT '交易索引',
    gas_used BIGINT COMMENT '已使用Gas',
    cumulative_gas_used BIGINT COMMENT '累计Gas使用',
    contract_address VARCHAR(42) COMMENT '合约地址（合约创建时）',
    status INT NOT NULL COMMENT '执行状态：0-成功, 1-失败',
    revert_reason VARCHAR(255) COMMENT '回滚原因',
    logs_bloom VARCHAR(1024) COMMENT '日志布隆过滤器',
    transaction_created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '交易创建时间',
    receipt_obtained_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '回执获取时间',

    INDEX idx_transaction_hash (transaction_hash),
    INDEX idx_block_number (block_number),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交易回执表';

-- 交易池管理表
CREATE TABLE IF NOT EXISTS blockchain_transaction_pool (
    id VARCHAR(36) PRIMARY KEY COMMENT '记录ID（UUID）',
    transaction_hash VARCHAR(66) NOT NULL UNIQUE COMMENT '交易哈希',
    from_address VARCHAR(42) NOT NULL COMMENT '发送地址',
    to_address VARCHAR(42) COMMENT '接收地址',
    gas_price BIGINT COMMENT 'Gas价格',
    gas_limit BIGINT COMMENT 'Gas限制',
    nonce BIGINT COMMENT '交易nonce',
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态：PENDING-待处理, QUEUED-排队中, PROCESSING-处理中, CONFIRMED-已确认',
    retry_count INT DEFAULT 0 COMMENT '重试次数',
    last_retry_at TIMESTAMP NULL COMMENT '最后重试时间',
    error_message TEXT COMMENT '错误信息',

    INDEX idx_transaction_hash (transaction_hash),
    INDEX idx_from_address (from_address),
    INDEX idx_status (status),
    INDEX idx_submitted_at (submitted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交易池管理表';
