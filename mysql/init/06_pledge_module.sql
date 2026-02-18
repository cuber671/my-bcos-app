-- ============================================
-- 质押模块表结构
-- 说明：仓单质押记录、质押申请、释放记录
-- 包含表：t_ewr_pledge_record, t_ewr_pledge_application, t_release_record
-- ============================================

USE `supply_chain_finance`;

-- ============================================
-- 表名：t_ewr_pledge_record
-- 说明：仓单质押记录表
-- ============================================
DROP TABLE IF EXISTS `ewr_pledge_record`;
CREATE TABLE `ewr_pledge_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '质押记录ID（自增主键）',
  `receipt_id` varchar(36) NOT NULL COMMENT '仓单ID',
  `receipt_no` varchar(64) DEFAULT NULL COMMENT '仓单编号',
  `endorsement_id` varchar(36) DEFAULT NULL COMMENT '背书ID（链接到ewr_endorsement_chain）',
  `endorsement_no` varchar(64) DEFAULT NULL COMMENT '背书编号',
  `owner_id` varchar(36) NOT NULL COMMENT '货主企业ID',
  `owner_name` varchar(128) DEFAULT NULL COMMENT '货主企业名称',
  `financial_institution_id` varchar(36) NOT NULL COMMENT '金融机构ID',
  `financial_institution_name` varchar(128) DEFAULT NULL COMMENT '金融机构名称',
  `financial_institution_address` varchar(42) DEFAULT NULL COMMENT '金融机构区块链地址',
  `previous_holder_address` varchar(42) DEFAULT NULL COMMENT '质押前持有人地址（原货主）',
  `pledge_amount` decimal(19,2) NOT NULL COMMENT '质押金额',
  `interest_rate` decimal(5,2) DEFAULT NULL COMMENT '年化利率（%）',
  `receipt_value` decimal(19,2) DEFAULT NULL COMMENT '仓单总价值',
  `pledge_start_date` date DEFAULT NULL COMMENT '质押开始日期',
  `pledge_end_date` date DEFAULT NULL COMMENT '质押结束日期',
  `pledge_time` datetime NOT NULL COMMENT '质押时间',
  `release_time` datetime DEFAULT NULL COMMENT '释放时间',
  `liquidation_time` datetime DEFAULT NULL COMMENT '清算时间',
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '质押状态：ACTIVE-质押中, RELEASED-已释放, LIQUIDATED-已清算',
  `tx_hash` varchar(128) DEFAULT NULL COMMENT '质押上链交易哈希',
  `block_number` bigint DEFAULT NULL COMMENT '质押区块号',
  `release_tx_hash` varchar(128) DEFAULT NULL COMMENT '释放上链交易哈希',
  `release_block_number` bigint DEFAULT NULL COMMENT '释放区块号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_receipt_id` (`receipt_id`),
  KEY `idx_endorsement_id` (`endorsement_id`),
  KEY `idx_financial_institution_id` (`financial_institution_id`),
  KEY `idx_status` (`status`),
  KEY `idx_pledge_time` (`pledge_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='仓单质押记录表';

-- ============================================
-- 表名：t_ewr_pledge_application
-- 说明：仓单质押申请表
-- ============================================
DROP TABLE IF EXISTS `ewr_pledge_application`;
CREATE TABLE `ewr_pledge_application` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '质押申请ID（自增主键）',
  `application_no` varchar(64) NOT NULL COMMENT '申请编号',
  `receipt_id` varchar(36) NOT NULL COMMENT '仓单ID',
  `receipt_no` varchar(64) DEFAULT NULL COMMENT '仓单编号',
  `owner_id` varchar(36) NOT NULL COMMENT '货主企业ID',
  `owner_name` varchar(128) DEFAULT NULL COMMENT '货主企业名称',
  `financial_institution_id` varchar(36) NOT NULL COMMENT '金融机构ID',
  `financial_institution_name` varchar(128) DEFAULT NULL COMMENT '金融机构名称',
  `financial_institution_address` varchar(42) DEFAULT NULL COMMENT '金融机构区块链地址',
  `pledge_amount` decimal(19,2) NOT NULL COMMENT '质押金额',
  `pledge_ratio` decimal(5,2) DEFAULT NULL COMMENT '质押率（0-1）',
  `receipt_value` decimal(19,2) DEFAULT NULL COMMENT '仓单总价值',
  `pledge_start_date` date DEFAULT NULL COMMENT '质押开始日期',
  `pledge_end_date` date DEFAULT NULL COMMENT '质押结束日期',
  `approved_amount` decimal(19,2) DEFAULT NULL COMMENT '实际批准金额',
  `interest_rate` decimal(5,2) DEFAULT NULL COMMENT '年化利率',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '申请状态：PENDING-待审核, APPROVED-已批准, REJECTED-已拒绝, RELEASED-已释放',
  `apply_time` datetime NOT NULL COMMENT '申请时间',
  `approval_time` datetime DEFAULT NULL COMMENT '审核时间',
  `approver_id` varchar(36) DEFAULT NULL COMMENT '审核人ID',
  `approver_name` varchar(64) DEFAULT NULL COMMENT '审核人姓名',
  `rejection_reason` varchar(500) DEFAULT NULL COMMENT '拒绝原因',
  `tx_hash` varchar(128) DEFAULT NULL COMMENT '区块链交易哈希',
  `block_number` bigint DEFAULT NULL COMMENT '区块号',
  `blockchain_time` datetime DEFAULT NULL COMMENT '上链时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_application_no` (`application_no`),
  KEY `idx_receipt_id` (`receipt_id`),
  KEY `idx_owner_id` (`owner_id`),
  KEY `idx_financial_institution_id` (`financial_institution_id`),
  KEY `idx_status` (`status`),
  KEY `idx_apply_time` (`apply_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='仓单质押申请表';

-- ============================================
-- 表名：t_release_record
-- 说明：质押释放记录表
-- ============================================
DROP TABLE IF EXISTS `release_record`;
CREATE TABLE `release_record` (
  `id` varchar(36) NOT NULL COMMENT '释放记录ID（UUID格式，主键）',
  `receipt_id` varchar(36) NOT NULL COMMENT '仓单ID',
  `owner_address` varchar(42) NOT NULL COMMENT '仓单所有者地址',
  `financial_institution_address` varchar(42) DEFAULT NULL COMMENT '金融机构地址',
  `pledge_amount` decimal(20,2) DEFAULT NULL COMMENT '质押金额',
  `finance_amount` decimal(20,2) DEFAULT NULL COMMENT '融资金额',
  `finance_rate` int DEFAULT NULL COMMENT '融资利率（基点）',
  `finance_date` datetime DEFAULT NULL COMMENT '融资日期',
  `release_date` datetime NOT NULL COMMENT '释放日期',
  `release_type` varchar(20) NOT NULL COMMENT '释放类型：FULL_REPAYMENT-全额还款释放, PARTIAL_REPAYMENT-部分还款释放, MATURITY-到期释放, MANUAL-手动释放, LIQUIDATION-清算释放',
  `repayment_amount` decimal(20,2) DEFAULT NULL COMMENT '还款金额',
  `interest_amount` decimal(20,2) DEFAULT NULL COMMENT '利息金额',
  `remark` text COMMENT '释放备注',
  `tx_hash` varchar(66) DEFAULT NULL COMMENT '区块链交易哈希',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_receipt_id` (`receipt_id`),
  KEY `idx_owner_address` (`owner_address`),
  KEY `idx_financial_institution_address` (`financial_institution_address`),
  KEY `idx_release_date` (`release_date`),
  KEY `idx_release_type` (`release_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='质押释放记录表';
