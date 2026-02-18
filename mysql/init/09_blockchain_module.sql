-- ============================================
-- 区块链模块表结构
-- 说明：区块链交易、交易回执、合约事件、合约元数据、交易池
-- 包含表：t_blockchain_transaction, t_blockchain_transaction_receipt,
--        t_blockchain_contract_event, t_blockchain_contract_metadata,
--        t_blockchain_transaction_pool
-- ============================================

USE `supply_chain_finance`;

-- ============================================
-- 表名：t_blockchain_transaction
-- 说明：区块链交易表
-- ============================================
DROP TABLE IF EXISTS `blockchain_transaction`;
CREATE TABLE `blockchain_transaction` (
  `id` varchar(36) NOT NULL COMMENT 'ID（UUID格式，主键）',
  `transaction_hash` varchar(66) NOT NULL COMMENT '交易哈希',
  `block_number` bigint DEFAULT NULL COMMENT '区块号',
  `block_hash` varchar(66) DEFAULT NULL COMMENT '区块哈希',
  `transaction_index` int DEFAULT NULL COMMENT '交易索引',
  `from_address` varchar(42) NOT NULL COMMENT '发送地址',
  `to_address` varchar(42) DEFAULT NULL COMMENT '接收地址',
  `value` varchar(50) DEFAULT NULL COMMENT '价值',
  `gas_price` bigint DEFAULT NULL COMMENT 'Gas价格',
  `gas_limit` bigint DEFAULT NULL COMMENT 'Gas限制',
  `gas_used` bigint DEFAULT NULL COMMENT '已用Gas',
  `input_data` text COMMENT '输入数据',
  `method_id` varchar(10) DEFAULT NULL COMMENT '方法ID',
  `status` int NOT NULL COMMENT '状态：CANCELLED(-2), FAILED(-1), PENDING(0), PACKED(1), SUCCESS(2)',
  `transaction_type` varchar(20) DEFAULT NULL COMMENT '交易类型：CONTRACT_CALL-合约调用, CONTRACT_DEPLOY-合约部署, VALUE_TRANSFER-价值转移',
  `nonce` bigint DEFAULT NULL COMMENT 'Nonce',
  `error_message` text COMMENT '错误信息',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_transaction_hash` (`transaction_hash`),
  KEY `idx_from_address` (`from_address`),
  KEY `idx_to_address` (`to_address`),
  KEY `idx_block_number` (`block_number`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='区块链交易表';

-- ============================================
-- 表名：t_blockchain_transaction_receipt
-- 说明：区块链交易回执表
-- ============================================
DROP TABLE IF EXISTS `blockchain_transaction_receipt`;
CREATE TABLE `blockchain_transaction_receipt` (
  `id` varchar(36) NOT NULL COMMENT 'ID（UUID格式，主键）',
  `transaction_hash` varchar(66) NOT NULL COMMENT '交易哈希',
  `block_number` bigint DEFAULT NULL COMMENT '区块号',
  `block_hash` varchar(66) DEFAULT NULL COMMENT '区块哈希',
  `transaction_index` int DEFAULT NULL COMMENT '交易索引',
  `gas_used` bigint DEFAULT NULL COMMENT '已用Gas',
  `cumulative_gas_used` bigint DEFAULT NULL COMMENT '累计已用Gas',
  `contract_address` varchar(42) DEFAULT NULL COMMENT '合约地址',
  `status` int NOT NULL COMMENT '状态',
  `revert_reason` varchar(255) DEFAULT NULL COMMENT '回滚原因',
  `logs_bloom` varchar(1024) DEFAULT NULL COMMENT 'Logs Bloom',
  `transaction_created_at` datetime DEFAULT NULL COMMENT '交易创建时间',
  `receipt_obtained_at` datetime DEFAULT NULL COMMENT '回执获取时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_transaction_hash` (`transaction_hash`),
  KEY `idx_block_number` (`block_number`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='区块链交易回执表';

-- ============================================
-- 表名：t_blockchain_contract_event
-- 说明：区块链合约事件表
-- ============================================
DROP TABLE IF EXISTS `blockchain_contract_event`;
CREATE TABLE `blockchain_contract_event` (
  `id` varchar(36) NOT NULL COMMENT 'ID（UUID格式，主键）',
  `contract_address` varchar(42) NOT NULL COMMENT '合约地址',
  `event_name` varchar(100) NOT NULL COMMENT '事件名称',
  `event_signature` varchar(100) DEFAULT NULL COMMENT '事件签名',
  `block_number` bigint NOT NULL COMMENT '区块号',
  `block_hash` varchar(66) DEFAULT NULL COMMENT '区块哈希',
  `transaction_hash` varchar(66) NOT NULL COMMENT '交易哈希',
  `transaction_index` int DEFAULT NULL COMMENT '交易索引',
  `log_index` int DEFAULT NULL COMMENT '日志索引',
  `event_data` text COMMENT '事件数据',
  `topics` text COMMENT '主题',
  `decoded_params` text COMMENT '解析参数',
  `event_timestamp` datetime DEFAULT NULL COMMENT '事件时间戳',
  `processed` tinyint(1) DEFAULT 0 COMMENT '是否已处理',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_contract_address` (`contract_address`),
  KEY `idx_event_name` (`event_name`),
  KEY `idx_transaction_hash` (`transaction_hash`),
  KEY `idx_block_number` (`block_number`),
  KEY `idx_event_timestamp` (`event_timestamp`),
  KEY `idx_processed` (`processed`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='区块链合约事件表';

-- ============================================
-- 表名：t_blockchain_contract_metadata
-- 说明：区块链合约元数据表
-- ============================================
DROP TABLE IF EXISTS `blockchain_contract_metadata`;
CREATE TABLE `blockchain_contract_metadata` (
  `id` varchar(36) NOT NULL COMMENT 'ID（UUID格式，主键）',
  `contract_address` varchar(42) NOT NULL COMMENT '合约地址',
  `contract_name` varchar(100) NOT NULL COMMENT '合约名称',
  `contract_type` varchar(50) NOT NULL COMMENT '合约类型',
  `contract_version` varchar(20) DEFAULT NULL COMMENT '合约版本',
  `abi` text COMMENT 'ABI',
  `bytecode` text COMMENT '字节码',
  `source_code` text COMMENT '源代码',
  `compiler_version` varchar(50) DEFAULT NULL COMMENT '编译器版本',
  `optimization_enabled` tinyint(1) DEFAULT NULL COMMENT '是否启用优化',
  `constructor_params` text COMMENT '构造函数参数',
  `deploy_transaction_hash` varchar(66) DEFAULT NULL COMMENT '部署交易哈希',
  `deployer_address` varchar(42) DEFAULT NULL COMMENT '部署者地址',
  `deploy_block_number` bigint DEFAULT NULL COMMENT '部署区块号',
  `deployment_timestamp` datetime DEFAULT NULL COMMENT '部署时间戳',
  `status` varchar(20) NOT NULL COMMENT '状态：ACTIVE-活跃, DEPRECATED-已弃用, DESTROYED-已销毁',
  `description` text COMMENT '描述',
  `tags` varchar(500) DEFAULT NULL COMMENT '标签',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_contract_address` (`contract_address`),
  KEY `idx_contract_type` (`contract_type`),
  KEY `idx_status` (`status`),
  KEY `idx_deployment_timestamp` (`deployment_timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='区块链合约元数据表';

-- ============================================
-- 表名：t_blockchain_transaction_pool
-- 说明：区块链交易池表
-- ============================================
DROP TABLE IF EXISTS `blockchain_transaction_pool`;
CREATE TABLE `blockchain_transaction_pool` (
  `id` varchar(36) NOT NULL COMMENT 'ID（UUID格式，主键）',
  `transaction_hash` varchar(66) NOT NULL COMMENT '交易哈希',
  `from_address` varchar(42) NOT NULL COMMENT '发送地址',
  `to_address` varchar(42) DEFAULT NULL COMMENT '接收地址',
  `gas_price` bigint DEFAULT NULL COMMENT 'Gas价格',
  `gas_limit` bigint DEFAULT NULL COMMENT 'Gas限制',
  `nonce` bigint DEFAULT NULL COMMENT 'Nonce',
  `submitted_at` datetime DEFAULT NULL COMMENT '提交时间',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING-待处理, QUEUED-队列中, PROCESSING-处理中, CONFIRMED-已确认',
  `retry_count` int DEFAULT 0 COMMENT '重试次数',
  `last_retry_at` datetime DEFAULT NULL COMMENT '最后重试时间',
  `error_message` text COMMENT '错误信息',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_transaction_hash` (`transaction_hash`),
  KEY `idx_from_address` (`from_address`),
  KEY `idx_status` (`status`),
  KEY `idx_submitted_at` (`submitted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='区块链交易池表';
