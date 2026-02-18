-- ============================================
-- 仓单模块表结构
-- 说明：电子仓单、仓单拆分合并、仓单质押等仓单全生命周期管理
-- 包含表：t_electronic_warehouse_receipt, t_warehouse_receipt,
--        t_receipt_split_application, t_receipt_merge_application,
--        t_warehouse_receipt_pledge
-- ============================================

USE `supply_chain_finance`;

-- ============================================
-- 表名：t_electronic_warehouse_receipt
-- 说明：电子仓单主表（核心表）
-- ============================================
DROP TABLE IF EXISTS `electronic_warehouse_receipt`;
CREATE TABLE `electronic_warehouse_receipt` (
  `id` varchar(36) NOT NULL COMMENT '仓单ID（UUID格式，主键）',
  `receipt_no` varchar(64) NOT NULL COMMENT '仓单编号（格式：EWR+yyyyMMdd+6位流水号）',
  `warehouse_id` varchar(36) NOT NULL COMMENT '仓储企业ID（UUID格式）',
  `warehouse_address` varchar(42) NOT NULL COMMENT '仓储方区块链地址（42位）',
  `warehouse_name` varchar(255) DEFAULT NULL COMMENT '仓储方名称（冗余字段）',
  `owner_id` varchar(36) NOT NULL COMMENT '货主企业ID（UUID格式）',
  `owner_address` varchar(42) NOT NULL COMMENT '货主区块链地址（42位）',
  `owner_name` varchar(255) DEFAULT NULL COMMENT '货主企业名称（冗余字段）',
  `holder_address` varchar(42) DEFAULT NULL COMMENT '持单人地址（可背书转让，42位）',
  `goods_name` varchar(500) NOT NULL COMMENT '货物名称',
  `goods_type` varchar(100) DEFAULT NULL COMMENT '货物类型/分类',
  `unit` varchar(20) NOT NULL COMMENT '计量单位',
  `quantity` decimal(20,2) NOT NULL COMMENT '货物数量',
  `unit_price` decimal(20,2) NOT NULL COMMENT '单价',
  `total_value` decimal(20,2) NOT NULL COMMENT '货物总价值',
  `market_price` decimal(20,2) DEFAULT 0.00 COMMENT '市场参考价格',
  `warehouse_location` varchar(500) DEFAULT NULL COMMENT '仓库详细地址',
  `storage_location` varchar(200) DEFAULT NULL COMMENT '存储位置',
  `storage_date` datetime NOT NULL COMMENT '入库时间',
  `warehouse_entry_date` datetime DEFAULT NULL COMMENT '入库日期（与storage_date相同）',
  `expiry_date` datetime NOT NULL COMMENT '仓单有效期',
  `actual_delivery_date` datetime DEFAULT NULL COMMENT '实际提货时间',
  `delivery_person_name` varchar(100) DEFAULT NULL COMMENT '提货人姓名',
  `delivery_person_contact` varchar(50) DEFAULT NULL COMMENT '提货人联系方式',
  `delivery_no` varchar(64) DEFAULT NULL COMMENT '提货单号',
  `vehicle_plate` varchar(20) DEFAULT NULL COMMENT '运输车牌号',
  `driver_name` varchar(100) DEFAULT NULL COMMENT '司机姓名',
  `receipt_status` varchar(20) NOT NULL DEFAULT 'DRAFT' COMMENT '仓单状态：DRAFT-草稿, PENDING_ONCHAIN-待上链, NORMAL-正常, ONCHAIN_FAILED-上链失败, PLEDGED-已质押, TRANSFERRED-已转让, FROZEN-已冻结, SPLITTING-拆分中, SPLIT-已拆分, MERGING-合并中, MERGED-已合并, CANCELLING-作废中, CANCELLED-已作废, EXPIRED-已过期, DELIVERED-已交付',
  `parent_receipt_id` varchar(36) DEFAULT NULL COMMENT '父仓单ID（用于拆分）',
  `batch_no` varchar(64) DEFAULT NULL COMMENT '批次号',
  `split_time` datetime DEFAULT NULL COMMENT '拆分时间',
  `split_count` int DEFAULT 0 COMMENT '子仓单数量',
  `merge_count` int DEFAULT 0 COMMENT '合并仓单数量',
  `merge_time` datetime DEFAULT NULL COMMENT '合并时间',
  `source_receipt_ids` text DEFAULT NULL COMMENT '源仓单ID列表（JSON格式）',
  `owner_operator_id` varchar(36) DEFAULT NULL COMMENT '货主企业操作人ID',
  `owner_operator_name` varchar(100) DEFAULT NULL COMMENT '货主企业操作人姓名',
  `warehouse_operator_id` varchar(36) DEFAULT NULL COMMENT '仓储方操作人ID',
  `warehouse_operator_name` varchar(100) DEFAULT NULL COMMENT '仓储方操作人姓名',
  `is_financed` tinyint(1) DEFAULT 0 COMMENT '是否已融资',
  `finance_amount` decimal(20,2) DEFAULT NULL COMMENT '融资金额',
  `finance_rate` int DEFAULT NULL COMMENT '融资利率（基点）',
  `finance_date` datetime DEFAULT NULL COMMENT '融资日期',
  `financier_address` varchar(42) DEFAULT NULL COMMENT '资金方地址',
  `pledge_contract_no` varchar(64) DEFAULT NULL COMMENT '质押合同编号',
  `endorsement_count` int DEFAULT 0 COMMENT '背书次数',
  `last_endorsement_date` datetime DEFAULT NULL COMMENT '最后背书时间',
  `current_holder` varchar(42) DEFAULT NULL COMMENT '当前持单人（冗余）',
  `tx_hash` varchar(66) DEFAULT NULL COMMENT '区块链交易哈希',
  `blockchain_status` varchar(20) DEFAULT 'PENDING' COMMENT '区块链上链状态：PENDING-待上链, SYNCED-已同步, FAILED-失败, VERIFIED-已验证',
  `block_number` bigint DEFAULT NULL COMMENT '区块高度',
  `blockchain_timestamp` datetime DEFAULT NULL COMMENT '区块链时间戳',
  `cancel_reason` text COMMENT '作废原因',
  `cancel_type` varchar(50) DEFAULT NULL COMMENT '作废类型',
  `cancel_time` datetime DEFAULT NULL COMMENT '作废时间',
  `cancelled_by` varchar(36) DEFAULT NULL COMMENT '作废操作人ID',
  `reference_no` varchar(100) DEFAULT NULL COMMENT '参考编号（如法律文书号）',
  `remarks` text COMMENT '备注信息',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `updated_by` varchar(50) DEFAULT NULL COMMENT '更新人',
  `deleted_at` datetime DEFAULT NULL COMMENT '软删除时间',
  `deleted_by` varchar(50) DEFAULT NULL COMMENT '删除人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_receipt_no` (`receipt_no`),
  KEY `idx_warehouse_address` (`warehouse_address`),
  KEY `idx_owner_address` (`owner_address`),
  KEY `idx_holder_address` (`holder_address`),
  KEY `idx_receipt_status` (`receipt_status`),
  KEY `idx_storage_date` (`storage_date`),
  KEY `idx_expiry_date` (`expiry_date`),
  KEY `idx_parent_receipt_id` (`parent_receipt_id`),
  KEY `idx_batch_no` (`batch_no`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电子仓单主表';

-- ============================================
-- 表名：t_warehouse_receipt
-- 说明：仓单表（简化版本）
-- ============================================
DROP TABLE IF EXISTS `warehouse_receipt`;
CREATE TABLE `warehouse_receipt` (
  `id` varchar(36) NOT NULL COMMENT '仓单ID（UUID格式，主键）',
  `owner_address` varchar(42) NOT NULL COMMENT '所有者地址',
  `warehouse_address` varchar(42) NOT NULL COMMENT '仓库地址',
  `financial_institution` varchar(42) DEFAULT NULL COMMENT '金融机构地址',
  `goods_name` varchar(500) NOT NULL COMMENT '货物名称',
  `goods_type` varchar(100) DEFAULT NULL COMMENT '货物类型',
  `quantity` decimal(20,2) NOT NULL COMMENT '数量',
  `unit` varchar(20) NOT NULL COMMENT '单位',
  `unit_price` decimal(20,2) NOT NULL COMMENT '单价',
  `total_price` decimal(20,2) NOT NULL COMMENT '总价',
  `quality` varchar(50) DEFAULT NULL COMMENT '质量等级',
  `origin` varchar(500) DEFAULT NULL COMMENT '产地',
  `warehouse_location` varchar(500) DEFAULT NULL COMMENT '仓库位置',
  `storage_date` datetime NOT NULL COMMENT '入库日期',
  `expiry_date` datetime NOT NULL COMMENT '到期日期',
  `release_date` datetime DEFAULT NULL COMMENT '释放日期',
  `status` varchar(20) NOT NULL DEFAULT 'CREATED' COMMENT '仓单状态：CREATED-已创建, VERIFIED-已验证, PLEDGED-已质押, FINANCED-已融资, RELEASED-已释放, LIQUIDATED-已清算, EXPIRED-已过期',
  `pledge_amount` decimal(20,2) DEFAULT NULL COMMENT '质押金额',
  `finance_amount` decimal(20,2) DEFAULT NULL COMMENT '融资金额',
  `finance_rate` int DEFAULT NULL COMMENT '融资利率（基点）',
  `finance_date` datetime DEFAULT NULL COMMENT '融资日期',
  `tx_hash` varchar(66) DEFAULT NULL COMMENT '区块链交易哈希',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_owner_address` (`owner_address`),
  KEY `idx_warehouse_address` (`warehouse_address`),
  KEY `idx_status` (`status`),
  KEY `idx_expiry_date` (`expiry_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='仓单表';

-- ============================================
-- 表名：t_receipt_split_application
-- 说明：仓单拆分申请表
-- ============================================
DROP TABLE IF EXISTS `receipt_split_application`;
CREATE TABLE `receipt_split_application` (
  `id` varchar(36) NOT NULL COMMENT '申请ID（UUID格式，主键）',
  `parent_receipt_id` varchar(36) NOT NULL COMMENT '父仓单ID',
  `parent_receipt_no` varchar(64) NOT NULL COMMENT '父仓单编号',
  `split_reason` text NOT NULL COMMENT '拆分原因',
  `split_count` int NOT NULL COMMENT '子仓单数量',
  `split_details` text COMMENT '拆分详情（JSON格式）',
  `request_status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '申请状态：PENDING-待审核, APPROVED-已批准, REJECTED-已拒绝, CANCELLED-已取消',
  `applicant_id` varchar(36) DEFAULT NULL COMMENT '申请人ID',
  `applicant_name` varchar(100) DEFAULT NULL COMMENT '申请人姓名',
  `reviewer_id` varchar(36) DEFAULT NULL COMMENT '审核人ID',
  `reviewer_name` varchar(100) DEFAULT NULL COMMENT '审核人姓名',
  `review_time` datetime DEFAULT NULL COMMENT '审核时间',
  `review_comments` text COMMENT '审核意见',
  `split_tx_hash` varchar(128) DEFAULT NULL COMMENT '拆分上链交易哈希',
  `block_number` bigint DEFAULT NULL COMMENT '区块号',
  `remarks` text COMMENT '备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_parent_receipt_id` (`parent_receipt_id`),
  KEY `idx_request_status` (`request_status`),
  KEY `idx_applicant_id` (`applicant_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='仓单拆分申请表';

-- ============================================
-- 表名：t_receipt_merge_application
-- 说明：仓单合并申请表
-- ============================================
DROP TABLE IF EXISTS `receipt_merge_application`;
CREATE TABLE `receipt_merge_application` (
  `id` varchar(36) NOT NULL COMMENT '申请ID（UUID格式，主键）',
  `source_receipt_ids` text NOT NULL COMMENT '源仓单ID列表（JSON数组）',
  `merged_receipt_id` varchar(36) DEFAULT NULL COMMENT '合并后的仓单ID',
  `applicant_id` varchar(36) NOT NULL COMMENT '申请人ID',
  `applicant_name` varchar(100) DEFAULT NULL COMMENT '申请人姓名',
  `merge_type` varchar(20) NOT NULL COMMENT '合并类型：QUANTITY-数量合并, VALUE-价值合并, FULL-完全合并',
  `total_quantity` decimal(20,2) DEFAULT NULL COMMENT '合并后总数量',
  `total_value` decimal(20,2) DEFAULT NULL COMMENT '合并后总价值',
  `merge_details` text NOT NULL COMMENT '合并明细（JSON格式）',
  `request_status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '申请状态：PENDING-待审核, APPROVED-已批准, REJECTED-已拒绝, CANCELLED-已取消',
  `reviewer_id` varchar(36) DEFAULT NULL COMMENT '审核人ID',
  `reviewer_name` varchar(100) DEFAULT NULL COMMENT '审核人姓名',
  `review_time` datetime DEFAULT NULL COMMENT '审核时间',
  `review_comments` text COMMENT '审核意见',
  `merge_tx_hash` varchar(128) DEFAULT NULL COMMENT '合并上链交易哈希',
  `block_number` bigint DEFAULT NULL COMMENT '区块号',
  `remarks` text COMMENT '备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_applicant_id` (`applicant_id`),
  KEY `idx_request_status` (`request_status`),
  KEY `idx_merged_receipt_id` (`merged_receipt_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='仓单合并申请表';

-- ============================================
-- 表名：t_warehouse_receipt_pledge
-- 说明：仓单质押记录表
-- ============================================
DROP TABLE IF EXISTS `warehouse_receipt_pledge`;
CREATE TABLE `warehouse_receipt_pledge` (
  `id` varchar(36) NOT NULL COMMENT '质押记录ID（UUID格式，主键）',
  `receipt_id` varchar(36) NOT NULL COMMENT '仓单ID',
  `receipt_no` varchar(64) DEFAULT NULL COMMENT '仓单编号',
  `pledge_amount` decimal(20,2) NOT NULL COMMENT '质押金额',
  `pledge_rate` decimal(5,2) NOT NULL COMMENT '质押率（0-1）',
  `pledge_date` datetime NOT NULL COMMENT '质押日期',
  `expiry_date` datetime DEFAULT NULL COMMENT '到期日期',
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE-质押中, RELEASED-已释放, LIQUIDATED-已清算',
  `owner_id` varchar(36) NOT NULL COMMENT '货主企业ID',
  `owner_name` varchar(128) DEFAULT NULL COMMENT '货主企业名称',
  `financial_institution_id` varchar(36) NOT NULL COMMENT '金融机构ID',
  `financial_institution_name` varchar(128) DEFAULT NULL COMMENT '金融机构名称',
  `interest_rate` decimal(5,2) DEFAULT NULL COMMENT '年化利率（%）',
  `release_date` datetime DEFAULT NULL COMMENT '释放日期',
  `liquidation_date` datetime DEFAULT NULL COMMENT '清算日期',
  `tx_hash` varchar(128) DEFAULT NULL COMMENT '质押上链交易哈希',
  `release_tx_hash` varchar(128) DEFAULT NULL COMMENT '释放上链交易哈希',
  `block_number` bigint DEFAULT NULL COMMENT '区块号',
  `remarks` varchar(500) DEFAULT NULL COMMENT '备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_receipt_id` (`receipt_id`),
  KEY `idx_owner_id` (`owner_id`),
  KEY `idx_financial_institution_id` (`financial_institution_id`),
  KEY `idx_status` (`status`),
  KEY `idx_pledge_date` (`pledge_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='仓单质押记录表';
