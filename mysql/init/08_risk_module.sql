-- ============================================
-- 风险管理模块表结构
-- 说明：风险评估、坏账记录、融资记录、逾期罚息、逾期催收
-- 包含表：t_risk_assessment, t_bad_debt_record, t_ewr_financing_record,
--        t_overdue_penalty_record, t_overdue_remind_record
-- ============================================

USE `supply_chain_finance`;

-- ============================================
-- 表名：t_risk_assessment
-- 说明：风险评估表
-- ============================================
DROP TABLE IF EXISTS `risk_assessment`;
CREATE TABLE `risk_assessment` (
  `id` varchar(36) NOT NULL COMMENT '评估ID（UUID格式，主键）',
  `enterprise_address` varchar(42) NOT NULL COMMENT '企业地址',
  `enterprise_name` varchar(200) DEFAULT NULL COMMENT '企业名称',
  `assessment_type` varchar(20) NOT NULL COMMENT '评估类型',
  `assessment_time` datetime NOT NULL COMMENT '评估时间',
  `risk_level` varchar(20) NOT NULL COMMENT '风险等级：VERY_LOW-极低风险, LOW-低风险, MEDIUM-中等风险, HIGH-高风险, VERY_HIGH-极高风险',
  `risk_score` int DEFAULT NULL COMMENT '风险评分（0-100）',
  `credit_score` int DEFAULT NULL COMMENT '信用评分',
  `overdue_count` int DEFAULT NULL COMMENT '逾期次数',
  `overdue_amount` bigint DEFAULT NULL COMMENT '逾期金额（分）',
  `overdue_rate` decimal(10,2) DEFAULT NULL COMMENT '逾期率',
  `total_liability` bigint DEFAULT NULL COMMENT '总负债（分）',
  `transaction_count` int DEFAULT NULL COMMENT '交易次数',
  `warning_count` int DEFAULT NULL COMMENT '风险预警数量',
  `risk_factors` text COMMENT '风险因素权重分析（JSON格式）',
  `recommendations` text COMMENT '改进建议（JSON格式）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_enterprise_address` (`enterprise_address`),
  KEY `idx_assessment_time` (`assessment_time`),
  KEY `idx_risk_level` (`risk_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='风险评估表';

-- ============================================
-- 表名：t_bad_debt_record
-- 说明：坏账记录表
-- ============================================
DROP TABLE IF EXISTS `bad_debt_record`;
CREATE TABLE `bad_debt_record` (
  `id` varchar(36) NOT NULL COMMENT '记录ID（UUID格式，主键）',
  `receivable_id` varchar(36) NOT NULL COMMENT '应收账款ID',
  `bad_debt_type` varchar(20) NOT NULL COMMENT '坏账类型：OVERDUE_180-逾期180天, BANKRUPTCY-破产, DISPUTE-争议, OTHER-其他',
  `principal_amount` decimal(20,2) NOT NULL COMMENT '本金金额',
  `overdue_days` int NOT NULL COMMENT '逾期天数',
  `total_penalty_amount` decimal(20,2) DEFAULT 0.00 COMMENT '累计罚息金额',
  `total_loss_amount` decimal(20,2) NOT NULL COMMENT '总损失金额（本金+罚息）',
  `bad_debt_reason` text COMMENT '坏账原因',
  `recovery_status` varchar(20) NOT NULL DEFAULT 'NOT_RECOVERED' COMMENT '回收状态：NOT_RECOVERED-未回收, PARTIAL_RECOVERED-部分回收, FULL_RECOVERED-全额回收',
  `recovered_amount` decimal(20,2) DEFAULT 0.00 COMMENT '已回收金额',
  `recovery_date` datetime DEFAULT NULL COMMENT '回收日期',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_receivable_id` (`receivable_id`),
  KEY `idx_bad_debt_type` (`bad_debt_type`),
  KEY `idx_recovery_status` (`recovery_status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='坏账记录表';

-- ============================================
-- 表名：t_ewr_financing_record
-- 说明：仓单融资记录表
-- ============================================
DROP TABLE IF EXISTS `ewr_financing_record`;
CREATE TABLE `ewr_financing_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '融资记录ID（自增主键）',
  `financing_no` varchar(64) DEFAULT NULL COMMENT '融资编号',
  `receipt_id` varchar(36) NOT NULL COMMENT '仓单ID',
  `receipt_no` varchar(64) DEFAULT NULL COMMENT '仓单编号',
  `endorsement_id` varchar(36) DEFAULT NULL COMMENT '背书ID（链接到ewr_endorsement_chain）',
  `endorsement_no` varchar(64) DEFAULT NULL COMMENT '背书编号',
  `owner_id` varchar(36) NOT NULL COMMENT '货主企业ID',
  `owner_name` varchar(128) DEFAULT NULL COMMENT '货主企业名称',
  `financial_institution_id` varchar(36) NOT NULL COMMENT '金融机构ID',
  `financial_institution_name` varchar(128) DEFAULT NULL COMMENT '金融机构名称',
  `financing_amount` decimal(19,2) NOT NULL COMMENT '融资金额（元）',
  `principal_amount` decimal(19,2) DEFAULT NULL COMMENT '本金金额',
  `interest_rate` decimal(5,2) DEFAULT NULL COMMENT '年化利率（%）',
  `total_interest` decimal(19,2) DEFAULT NULL COMMENT '总利息',
  `repayment_amount` decimal(19,2) DEFAULT NULL COMMENT '应还金额（本金+利息）',
  `financing_date` date NOT NULL COMMENT '融资日期',
  `due_date` date DEFAULT NULL COMMENT '到期日期',
  `actual_repayment_date` date DEFAULT NULL COMMENT '实际还款日期',
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '融资状态：ACTIVE-生效中, PAID_OFF-已还清, OVERDUE-已逾期, LIQUIDATED-已清算',
  `financing_time` datetime NOT NULL COMMENT '放款时间',
  `repayment_time` datetime DEFAULT NULL COMMENT '还款时间',
  `overdue_days` int DEFAULT NULL COMMENT '逾期天数',
  `overdue_interest` decimal(19,2) DEFAULT NULL COMMENT '逾期利息',
  `late_fee` decimal(19,2) DEFAULT NULL COMMENT '滞纳金',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_financing_no` (`financing_no`),
  KEY `idx_receipt_id` (`receipt_id`),
  KEY `idx_financial_institution_id` (`financial_institution_id`),
  KEY `idx_status` (`status`),
  KEY `idx_financing_time` (`financing_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='仓单融资记录表';

-- ============================================
-- 表名：t_overdue_penalty_record
-- 说明：逾期罚息记录表
-- ============================================
DROP TABLE IF EXISTS `overdue_penalty_record`;
CREATE TABLE `overdue_penalty_record` (
  `id` varchar(36) NOT NULL COMMENT '记录ID（UUID格式，主键）',
  `receivable_id` varchar(36) NOT NULL COMMENT '应收账款ID',
  `penalty_type` varchar(20) NOT NULL COMMENT '罚息类型：AUTO-自动计算, MANUAL-手动计算',
  `principal_amount` decimal(20,2) NOT NULL COMMENT '本金金额',
  `overdue_days` int NOT NULL COMMENT '逾期天数',
  `daily_rate` decimal(10,6) NOT NULL COMMENT '日利率（如0.0005表示0.05%）',
  `penalty_amount` decimal(20,2) NOT NULL COMMENT '本次罚息金额',
  `total_penalty_amount` decimal(20,2) NOT NULL COMMENT '累计罚息金额',
  `calculate_start_date` datetime NOT NULL COMMENT '计算起始日期',
  `calculate_end_date` datetime NOT NULL COMMENT '计算结束日期',
  `calculate_date` datetime NOT NULL COMMENT '计算日期',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_receivable_id` (`receivable_id`),
  KEY `idx_penalty_type` (`penalty_type`),
  KEY `idx_calculate_date` (`calculate_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='逾期罚息记录表';

-- ============================================
-- 表名：t_overdue_remind_record
-- 说明：逾期催收记录表
-- ============================================
DROP TABLE IF EXISTS `overdue_remind_record`;
CREATE TABLE `overdue_remind_record` (
  `id` varchar(36) NOT NULL COMMENT '记录ID（UUID格式，主键）',
  `receivable_id` varchar(36) NOT NULL COMMENT '应收账款ID',
  `remind_type` varchar(20) NOT NULL COMMENT '催收类型：EMAIL-邮件, SMS-短信, PHONE-电话, LETTER-信函, LEGAL-法律途径',
  `remind_level` varchar(20) DEFAULT NULL COMMENT '催收级别：NORMAL-普通, URGENT-紧急, SEVERE-严重',
  `remind_date` datetime NOT NULL COMMENT '催收日期',
  `operator_address` varchar(42) DEFAULT NULL COMMENT '操作人地址',
  `remind_content` text COMMENT '催收内容',
  `remind_result` varchar(20) DEFAULT NULL COMMENT '催收结果：SUCCESS-成功, FAILED-失败, PENDING-待处理',
  `next_remind_date` datetime DEFAULT NULL COMMENT '下次催收日期',
  `remark` text COMMENT '备注',
  `tx_hash` varchar(66) DEFAULT NULL COMMENT '区块链交易哈希',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_receivable_id` (`receivable_id`),
  KEY `idx_remind_type` (`remind_type`),
  KEY `idx_remind_level` (`remind_level`),
  KEY `idx_remind_date` (`remind_date`),
  KEY `idx_remind_result` (`remind_result`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='逾期催收记录表';
