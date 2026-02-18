-- ============================================
-- 应收账款模块表结构
-- 说明：应收账款、还款记录、转让记录
-- 包含表：t_receivable, t_receivable_repayment_record, t_receivable_transfer
-- ============================================

USE `supply_chain_finance`;

-- ============================================
-- 表名：t_receivable
-- 说明：应收账款表
-- ============================================
DROP TABLE IF EXISTS `receivable`;
CREATE TABLE `receivable` (
  `id` varchar(36) NOT NULL COMMENT '应收账款ID（UUID格式，主键）',
  `supplier_address` varchar(42) NOT NULL COMMENT '供应商地址',
  `core_enterprise_address` varchar(42) NOT NULL COMMENT '核心企业地址',
  `amount` decimal(20,2) NOT NULL COMMENT '应收金额',
  `currency` varchar(10) DEFAULT 'CNY' COMMENT '币种',
  `issue_date` datetime NOT NULL COMMENT '出票日期',
  `due_date` datetime NOT NULL COMMENT '到期日期',
  `description` text COMMENT '描述',
  `status` varchar(20) NOT NULL DEFAULT 'CREATED' COMMENT '状态：CREATED-已创建, CONFIRMED-已确认, FINANCED-已融资, REPAID-已还款, DEFAULTED-已违约, CANCELLED-已取消, SPLITTING-拆分中, SPLIT-已拆分, MERGING-合并中, MERGED-已合并',
  `current_holder` varchar(42) NOT NULL COMMENT '当前持有人地址',
  `financier_address` varchar(42) DEFAULT NULL COMMENT '资金方地址',
  `finance_amount` decimal(20,2) DEFAULT NULL COMMENT '融资金额',
  `finance_rate` int DEFAULT NULL COMMENT '融资利率(基点)',
  `finance_date` datetime DEFAULT NULL COMMENT '融资日期',
  `tx_hash` varchar(66) DEFAULT NULL COMMENT '区块链交易哈希',
  `overdue_level` varchar(20) DEFAULT NULL COMMENT '逾期等级：MILD-轻微, MODERATE-中等, SEVERE-严重, BAD_DEBT-坏账',
  `overdue_days` int DEFAULT 0 COMMENT '逾期天数',
  `penalty_amount` decimal(20,2) DEFAULT 0.00 COMMENT '累计罚息金额',
  `last_remind_date` datetime DEFAULT NULL COMMENT '最后催收日期',
  `remind_count` int DEFAULT 0 COMMENT '催收次数',
  `bad_debt_date` datetime DEFAULT NULL COMMENT '坏账认定日期',
  `bad_debt_reason` text COMMENT '坏账原因',
  `overdue_calculated_date` datetime DEFAULT NULL COMMENT '逾期信息计算日期',
  `parent_receivable_id` varchar(36) DEFAULT NULL COMMENT '父应收账款ID（用于拆分合并追溯）',
  `split_count` int DEFAULT 0 COMMENT '拆分数量（拆分后的子应收账款数量）',
  `merge_count` int DEFAULT 0 COMMENT '合并数量（合并前的应收账款数量）',
  `split_time` datetime DEFAULT NULL COMMENT '拆分时间',
  `merge_time` datetime DEFAULT NULL COMMENT '合并时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_supplier_address` (`supplier_address`),
  KEY `idx_core_enterprise_address` (`core_enterprise_address`),
  KEY `idx_current_holder` (`current_holder`),
  KEY `idx_financier_address` (`financier_address`),
  KEY `idx_status` (`status`),
  KEY `idx_due_date` (`due_date`),
  KEY `idx_overdue_level` (`overdue_level`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='应收账款表';

-- ============================================
-- 表名：t_receivable_repayment_record
-- 说明：应收账款还款记录表
-- ============================================
DROP TABLE IF EXISTS `receivable_repayment_record`;
CREATE TABLE `receivable_repayment_record` (
  `id` varchar(36) NOT NULL COMMENT '记录ID（UUID格式，主键）',
  `receivable_id` varchar(36) NOT NULL COMMENT '应收账款ID',
  `repayment_type` varchar(20) NOT NULL COMMENT '还款类型：PARTIAL-部分还款, FULL-全额还款, EARLY-提前还款, OVERDUE-逾期还款',
  `repayment_amount` decimal(20,2) NOT NULL COMMENT '还款总金额',
  `principal_amount` decimal(20,2) NOT NULL COMMENT '本金金额',
  `interest_amount` decimal(20,2) DEFAULT 0.00 COMMENT '利息金额',
  `penalty_amount` decimal(20,2) DEFAULT 0.00 COMMENT '罚息金额',
  `payer_address` varchar(42) NOT NULL COMMENT '还款人地址（核心企业）',
  `receiver_address` varchar(42) NOT NULL COMMENT '收款人地址（供应商或金融机构）',
  `payment_date` date NOT NULL COMMENT '还款日期',
  `actual_payment_time` datetime NOT NULL COMMENT '实际还款时间',
  `payment_method` varchar(20) DEFAULT NULL COMMENT '支付方式：BANK-银行转账, ALIPAY-支付宝, WECHAT-微信, OTHER-其他',
  `payment_account` varchar(100) DEFAULT NULL COMMENT '支付账号',
  `transaction_no` varchar(64) DEFAULT NULL COMMENT '交易流水号',
  `early_payment_days` int DEFAULT NULL COMMENT '提前还款天数',
  `overdue_days` int DEFAULT NULL COMMENT '逾期天数',
  `remark` text COMMENT '备注',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING-待确认, CONFIRMED-已确认, FAILED-失败',
  `tx_hash` varchar(66) DEFAULT NULL COMMENT '区块链交易哈希',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` varchar(42) DEFAULT NULL COMMENT '创建人地址',
  `updated_by` varchar(42) DEFAULT NULL COMMENT '更新人地址',
  PRIMARY KEY (`id`),
  KEY `idx_receivable_id` (`receivable_id`),
  KEY `idx_payer_address` (`payer_address`),
  KEY `idx_receiver_address` (`receiver_address`),
  KEY `idx_payment_date` (`payment_date`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='应收账款还款记录表';

-- ============================================
-- 表名：t_receivable_transfer
-- 说明：应收账款转让记录表
-- ============================================
DROP TABLE IF EXISTS `receivable_transfer`;
CREATE TABLE `receivable_transfer` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '转让记录ID（自增主键）',
  `receivable_id` varchar(64) NOT NULL COMMENT '应收账款ID',
  `from_address` varchar(42) NOT NULL COMMENT '转出方地址',
  `to_address` varchar(42) NOT NULL COMMENT '转入方地址',
  `amount` decimal(20,2) NOT NULL COMMENT '转让金额',
  `transfer_type` varchar(20) NOT NULL COMMENT '转让类型',
  `timestamp` datetime NOT NULL COMMENT '时间戳',
  `tx_hash` varchar(66) DEFAULT NULL COMMENT '区块链交易哈希',
  PRIMARY KEY (`id`),
  KEY `idx_receivable_id` (`receivable_id`),
  KEY `idx_from_address` (`from_address`),
  KEY `idx_to_address` (`to_address`),
  KEY `idx_timestamp` (`timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='应收账款转让记录表';
