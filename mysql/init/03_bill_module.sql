-- ============================================
-- 票据模块表结构
-- 说明：票据、贴现、背书、融资、担保、投资、合并、质押、追索、结算等票据全生命周期管理
-- 包含表：t_bill, t_bill_discount, t_bill_endorsement, t_bill_finance_application,
--        t_bill_guarantee, t_bill_investment, t_bill_merge_application,
--        t_bill_pledge_application, t_bill_split_application,
--        t_discount_record, t_endorsement, t_repayment_record,
--        t_bill_recourse, t_bill_settlement
-- ============================================

USE `supply_chain_finance`;

-- ============================================
-- 表名：t_bill
-- 说明：票据主表（核心表）
-- ============================================
DROP TABLE IF EXISTS `bill`;
CREATE TABLE `bill` (
  `bill_id` varchar(36) NOT NULL COMMENT '票据ID（主键）',
  `bill_no` varchar(50) NOT NULL COMMENT '票据编号',
  `bill_type` varchar(50) NOT NULL COMMENT '票据类型：BANK_ACCEPTANCE_BILL-银行承兑汇票, COMMERCIAL_ACCEPTANCE_BILL-商业承兑汇票, BANK_NOTE-银行本票',
  `face_value` decimal(20,2) NOT NULL COMMENT '票面金额',
  `currency` varchar(10) DEFAULT 'CNY' COMMENT '货币类型',
  `issue_date` datetime(6) DEFAULT NULL COMMENT '开票日期',
  `due_date` datetime(6) DEFAULT NULL COMMENT '到期日期',
  `drawer_id` varchar(36) DEFAULT NULL COMMENT '出票人ID',
  `drawer_name` varchar(200) DEFAULT NULL COMMENT '出票人名称',
  `drawer_address` varchar(42) DEFAULT NULL COMMENT '出票人地址',
  `drawer_account` varchar(100) DEFAULT NULL COMMENT '出票人账户',
  `drawee_id` varchar(36) DEFAULT NULL COMMENT '承兑人ID',
  `drawee_name` varchar(200) DEFAULT NULL COMMENT '承兑人名称',
  `drawee_address` varchar(42) DEFAULT NULL COMMENT '承兑人地址',
  `drawee_account` varchar(100) DEFAULT NULL COMMENT '承兑人账户',
  `payee_id` varchar(36) DEFAULT NULL COMMENT '收款人ID',
  `payee_name` varchar(200) DEFAULT NULL COMMENT '收款人名称',
  `payee_address` varchar(42) DEFAULT NULL COMMENT '收款人地址',
  `payee_account` varchar(100) DEFAULT NULL COMMENT '收款人账户',
  `current_holder_id` varchar(36) DEFAULT NULL COMMENT '当前持票人ID',
  `current_holder_name` varchar(200) DEFAULT NULL COMMENT '当前持票人名称',
  `current_holder_address` varchar(42) DEFAULT NULL COMMENT '当前持票人地址',
  `bill_status` varchar(50) NOT NULL DEFAULT 'DRAFT' COMMENT '票据状态：DRAFT-草稿, PENDING_ISSUANCE-待签发, ISSUED-已签发, NORMAL-正常, ENDORSED-已背书, PLEDGED-已质押, DISCOUNTED-已贴现, FINANCED-已融资, FROZEN-已冻结, EXPIRED-已过期, DISHONORED-已拒付, CANCELLED-已取消, PAID-已付款, SETTLED-已结算',
  `blockchain_status` varchar(50) DEFAULT 'NOT_ONCHAIN' COMMENT '区块链状态：NOT_ONCHAIN-未上链, PENDING-待上链, ONCHAIN-已上链, FAILED-上链失败',
  `blockchain_tx_hash` varchar(100) DEFAULT NULL COMMENT '交易哈希',
  `blockchain_time` datetime(6) DEFAULT NULL COMMENT '上链时间',
  `discount_rate` decimal(10,6) DEFAULT NULL COMMENT '贴现率',
  `discount_amount` decimal(20,2) DEFAULT NULL COMMENT '贴现金额',
  `discount_date` datetime(6) DEFAULT NULL COMMENT '贴现日期',
  `discount_institution_id` varchar(36) DEFAULT NULL COMMENT '贴现机构ID',
  `pledge_amount` decimal(20,2) DEFAULT NULL COMMENT '质押金额',
  `pledge_institution_id` varchar(36) DEFAULT NULL COMMENT '质押机构ID',
  `pledge_period` int DEFAULT NULL COMMENT '质押期限',
  `pledge_date` datetime(6) DEFAULT NULL COMMENT '质押日期',
  `receipt_pledge_id` varchar(36) DEFAULT NULL COMMENT '关联仓单质押ID',
  `backed_receipt_id` varchar(36) DEFAULT NULL COMMENT '担保仓单ID',
  `receipt_pledge_value` decimal(20,2) DEFAULT NULL COMMENT '仓单担保价值',
  `parent_bill_id` varchar(36) DEFAULT NULL COMMENT '父票据ID',
  `split_count` int DEFAULT NULL COMMENT '拆分数量',
  `merge_count` int DEFAULT NULL COMMENT '合并前数量',
  `split_time` datetime(6) DEFAULT NULL COMMENT '拆分时间',
  `merge_time` datetime(6) DEFAULT NULL COMMENT '合并时间',
  `acceptance_time` datetime(6) DEFAULT NULL COMMENT '承兑时间',
  `acceptance_remarks` varchar(500) DEFAULT NULL COMMENT '承兑备注',
  `guarantee_id` varchar(36) DEFAULT NULL COMMENT '担保记录ID',
  `has_guarantee` tinyint(1) DEFAULT 0 COMMENT '是否有担保',
  `dishonored` tinyint(1) DEFAULT 0 COMMENT '是否拒付',
  `dishonored_date` datetime(6) DEFAULT NULL COMMENT '拒付日期',
  `dishonored_reason` text COMMENT '拒付原因',
  `recourse_status` varchar(50) DEFAULT NULL COMMENT '追索状态',
  `settlement_id` varchar(36) DEFAULT NULL COMMENT '结算编号',
  `related_debts` text COMMENT '关联债务',
  `settlement_date` datetime(6) DEFAULT NULL COMMENT '结算日期',
  `trade_contract_id` varchar(36) DEFAULT NULL COMMENT '贸易合同ID',
  `trade_amount` decimal(20,2) DEFAULT NULL COMMENT '贸易金额',
  `goods_description` varchar(500) DEFAULT NULL COMMENT '货物描述',
  `trade_date` date DEFAULT NULL COMMENT '贸易日期',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  `created_by` varchar(36) DEFAULT NULL COMMENT '创建人ID',
  `updated_by` varchar(36) DEFAULT NULL COMMENT '更新人ID',
  `remarks` varchar(1000) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`bill_id`),
  UNIQUE KEY `uk_bill_no` (`bill_no`),
  KEY `idx_bill_type` (`bill_type`),
  KEY `idx_current_holder` (`current_holder_address`),
  KEY `idx_bill_status` (`bill_status`),
  KEY `idx_due_date` (`due_date`),
  KEY `idx_blockchain_status` (`blockchain_status`),
  KEY `idx_drawer_address` (`drawer_address`),
  KEY `idx_drawee_address` (`drawee_address`),
  KEY `idx_payee_address` (`payee_address`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='票据主表';

-- ============================================
-- 表名：t_bill_discount
-- 说明：票据贴现表
-- ============================================
DROP TABLE IF EXISTS `bill_discount`;
CREATE TABLE `bill_discount` (
  `discount_id` varchar(36) NOT NULL COMMENT '贴现ID（主键）',
  `bill_id` varchar(36) NOT NULL COMMENT '票据ID',
  `bill_no` varchar(50) NOT NULL COMMENT '票据编号',
  `bill_type` varchar(50) NOT NULL COMMENT '票据类型',
  `face_value` decimal(20,2) NOT NULL COMMENT '票面金额',
  `discount_rate` decimal(10,6) NOT NULL COMMENT '贴现率',
  `discount_period` int NOT NULL COMMENT '贴现期限',
  `discount_interest` decimal(20,2) NOT NULL COMMENT '贴现利息',
  `net_amount` decimal(20,2) NOT NULL COMMENT '实付金额',
  `discount_institution_id` varchar(36) NOT NULL COMMENT '贴现机构ID',
  `discount_institution_name` varchar(200) NOT NULL COMMENT '贴现机构名称',
  `applicant_id` varchar(36) NOT NULL COMMENT '申请人ID',
  `applicant_name` varchar(200) NOT NULL COMMENT '申请人名称',
  `applicant_address` varchar(42) DEFAULT NULL COMMENT '申请人地址',
  `application_purpose` varchar(500) DEFAULT NULL COMMENT '贴现用途',
  `application_date` datetime(6) DEFAULT NULL COMMENT '申请日期',
  `approval_date` datetime(6) DEFAULT NULL COMMENT '批准日期',
  `payment_date` datetime(6) DEFAULT NULL COMMENT '付款日期',
  `application_status` varchar(50) NOT NULL DEFAULT 'PENDING' COMMENT '申请状态：PENDING-待审核, APPROVED-已批准, REJECTED-已拒绝, PAID-已付款',
  `reviewer_id` varchar(36) DEFAULT NULL COMMENT '审核人ID',
  `reviewer_name` varchar(200) DEFAULT NULL COMMENT '审核人名称',
  `approval_comments` text COMMENT '审核意见',
  `payment_account` varchar(100) DEFAULT NULL COMMENT '收款账号',
  `payment_voucher` varchar(100) DEFAULT NULL COMMENT '付款凭证',
  `payment_proof` varchar(100) DEFAULT NULL COMMENT '付款证明',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  `remarks` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`discount_id`),
  KEY `idx_bill_id` (`bill_id`),
  KEY `idx_discount_institution` (`discount_institution_id`),
  KEY `idx_applicant` (`applicant_id`),
  KEY `idx_application_status` (`application_status`),
  KEY `idx_application_date` (`application_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='票据贴现表';

-- ============================================
-- 表名：t_bill_endorsement
-- 说明：票据背书表
-- ============================================
DROP TABLE IF EXISTS `bill_endorsement`;
CREATE TABLE `bill_endorsement` (
  `endorsement_id` varchar(36) NOT NULL COMMENT '背书ID（主键）',
  `bill_id` varchar(36) NOT NULL COMMENT '票据ID',
  `bill_no` varchar(50) NOT NULL COMMENT '票据编号',
  `endorser_id` varchar(36) NOT NULL COMMENT '背书人ID',
  `endorser_name` varchar(200) NOT NULL COMMENT '背书人名称',
  `endorser_address` varchar(42) DEFAULT NULL COMMENT '背书人地址',
  `endorsee_id` varchar(36) NOT NULL COMMENT '被背书人ID',
  `endorsee_name` varchar(200) NOT NULL COMMENT '被背书人名称',
  `endorsee_address` varchar(42) DEFAULT NULL COMMENT '被背书人地址',
  `endorsement_type` varchar(50) NOT NULL COMMENT '背书类型：TRANSFER-转让, PLEDGE-质押, COLLECTION-托收',
  `endorsement_reason` varchar(500) DEFAULT NULL COMMENT '背书原因',
  `related_contract` varchar(36) DEFAULT NULL COMMENT '关联合同ID',
  `endorsement_date` datetime(6) DEFAULT NULL COMMENT '背书日期',
  `related_receipt_id` varchar(36) DEFAULT NULL COMMENT '关联仓单ID',
  `receipt_delivery` tinyint(1) DEFAULT 0 COMMENT '是否涉及仓单交付',
  `blockchain_status` varchar(50) DEFAULT 'NOT_ONCHAIN' COMMENT '区块链状态：NOT_ONCHAIN-未上链, PENDING-待上链, ONCHAIN-已上链, FAILED-上链失败',
  `blockchain_tx_hash` varchar(100) DEFAULT NULL COMMENT '交易哈希',
  `blockchain_time` datetime(6) DEFAULT NULL COMMENT '上链时间',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `created_by` varchar(36) DEFAULT NULL COMMENT '创建人ID',
  `remarks` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`endorsement_id`),
  KEY `idx_bill_id` (`bill_id`),
  KEY `idx_endorser` (`endorser_address`),
  KEY `idx_endorsee` (`endorsee_address`),
  KEY `idx_endorsement_type` (`endorsement_type`),
  KEY `idx_endorsement_date` (`endorsement_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='票据背书表';

-- ============================================
-- 表名：t_bill_finance_application
-- 说明：票据融资申请表
-- ============================================
DROP TABLE IF EXISTS `bill_finance_application`;
CREATE TABLE `bill_finance_application` (
  `id` varchar(36) NOT NULL COMMENT '融资申请ID（主键）',
  `bill_id` varchar(36) NOT NULL COMMENT '票据ID',
  `applicant_id` varchar(36) NOT NULL COMMENT '申请人ID',
  `financial_institution_id` varchar(36) NOT NULL COMMENT '金融机构ID',
  `finance_amount` decimal(20,2) NOT NULL COMMENT '融资金额',
  `finance_rate` decimal(10,6) NOT NULL COMMENT '融资利率',
  `finance_period` int NOT NULL COMMENT '融资期限',
  `pledge_agreement` text COMMENT '质押协议内容',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '申请状态：PENDING-待审核, APPROVED-已批准, REJECTED-已拒绝, ACTIVE-生效中, REPAID-已还款, CANCELLED-已取消',
  `approved_amount` decimal(20,2) DEFAULT NULL COMMENT '批准金额',
  `approved_rate` decimal(10,6) DEFAULT NULL COMMENT '批准利率',
  `actual_amount` decimal(20,2) DEFAULT NULL COMMENT '实际放款金额',
  `disbursement_date` datetime(6) DEFAULT NULL COMMENT '放款日期',
  `apply_date` datetime(6) DEFAULT NULL COMMENT '申请日期',
  `approve_date` datetime(6) DEFAULT NULL COMMENT '审核日期',
  `approval_comments` varchar(500) DEFAULT NULL COMMENT '审核意见',
  `rejection_reason` varchar(500) DEFAULT NULL COMMENT '拒绝原因',
  `repayment_date` datetime(6) DEFAULT NULL COMMENT '还款日期',
  `actual_repayment_amount` decimal(20,2) DEFAULT NULL COMMENT '实际还款金额',
  `tx_hash` varchar(100) DEFAULT NULL COMMENT '交易哈希',
  `created_by` varchar(36) DEFAULT NULL COMMENT '创建人ID',
  `updated_by` varchar(36) DEFAULT NULL COMMENT '更新人ID',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_bill_id` (`bill_id`),
  KEY `idx_applicant` (`applicant_id`),
  KEY `idx_financial_institution` (`financial_institution_id`),
  KEY `idx_status` (`status`),
  KEY `idx_apply_date` (`apply_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='票据融资申请表';

-- ============================================
-- 表名：t_bill_guarantee
-- 说明：票据担保表
-- ============================================
DROP TABLE IF EXISTS `bill_guarantee`;
CREATE TABLE `bill_guarantee` (
  `id` varchar(36) NOT NULL COMMENT '担保记录ID（主键）',
  `bill_id` varchar(36) NOT NULL COMMENT '票据ID',
  `bill_no` varchar(50) NOT NULL COMMENT '票据编号',
  `guarantor_id` varchar(36) NOT NULL COMMENT '担保人ID',
  `guarantor_name` varchar(200) NOT NULL COMMENT '担保人名称',
  `guarantor_address` varchar(42) DEFAULT NULL COMMENT '担保人地址',
  `guarantee_type` varchar(20) NOT NULL COMMENT '担保类型：FULL-全额担保, PARTIAL-部分担保, JOINT-联合担保',
  `guarantee_amount` decimal(20,2) NOT NULL COMMENT '担保金额',
  `guarantee_rate` decimal(10,6) DEFAULT NULL COMMENT '担保费率',
  `guarantee_fee` decimal(20,2) DEFAULT NULL COMMENT '担保费用',
  `guarantee_period` int DEFAULT NULL COMMENT '担保期限',
  `guarantee_start_date` datetime DEFAULT NULL COMMENT '担保开始日期',
  `guarantee_end_date` datetime DEFAULT NULL COMMENT '担保结束日期',
  `risk_level` varchar(20) DEFAULT NULL COMMENT '风险等级：LOW-低, MEDIUM-中, HIGH-高',
  `credit_score` int DEFAULT NULL COMMENT '信用评分',
  `risk_assessment` text COMMENT '风险评估',
  `guarantee_conditions` text COMMENT '担保条件',
  `collateral_info` text COMMENT '反担保措施',
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE-生效中, EXPIRED-已过期, CLAIMED-已索赔, CANCELLED-已取消',
  `claim_amount` decimal(20,2) DEFAULT NULL COMMENT '已索赔金额',
  `claim_date` datetime DEFAULT NULL COMMENT '索赔日期',
  `tx_hash` varchar(100) DEFAULT NULL COMMENT '交易哈希',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  `created_by` varchar(36) DEFAULT NULL COMMENT '创建人ID',
  `remarks` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_bill_id` (`bill_id`),
  KEY `idx_guarantor` (`guarantor_address`),
  KEY `idx_status` (`status`),
  KEY `idx_guarantee_type` (`guarantee_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='票据担保表';

-- ============================================
-- 表名：t_bill_investment
-- 说明：票据投资表
-- ============================================
DROP TABLE IF EXISTS `bill_investment`;
CREATE TABLE `bill_investment` (
  `id` varchar(36) NOT NULL COMMENT '投资记录ID（主键）',
  `bill_id` varchar(36) NOT NULL COMMENT '票据ID',
  `bill_no` varchar(50) NOT NULL COMMENT '票据编号',
  `bill_face_value` decimal(20,2) NOT NULL COMMENT '票据面值',
  `investor_id` varchar(36) NOT NULL COMMENT '投资机构ID',
  `investor_name` varchar(200) NOT NULL COMMENT '投资机构名称',
  `investor_address` varchar(42) DEFAULT NULL COMMENT '投资机构地址',
  `original_holder_id` varchar(36) DEFAULT NULL COMMENT '原持票人ID',
  `original_holder_name` varchar(200) DEFAULT NULL COMMENT '原持票人名称',
  `original_holder_address` varchar(42) DEFAULT NULL COMMENT '原持票人地址',
  `invest_amount` decimal(20,2) NOT NULL COMMENT '投资金额',
  `invest_rate` decimal(10,4) NOT NULL COMMENT '投资利率',
  `expected_return` decimal(20,2) DEFAULT NULL COMMENT '预期收益',
  `investment_days` int DEFAULT NULL COMMENT '投资天数',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '投资状态：PENDING-待确认, CONFIRMED-已确认, COMPLETED-已完成, CANCELLED-已撤销, FAILED-失败',
  `investment_date` datetime(6) DEFAULT NULL COMMENT '投资日期',
  `confirmation_date` datetime(6) DEFAULT NULL COMMENT '确认日期',
  `completion_date` datetime(6) DEFAULT NULL COMMENT '完成日期',
  `cancellation_date` datetime(6) DEFAULT NULL COMMENT '撤销日期',
  `maturity_amount` decimal(20,2) DEFAULT NULL COMMENT '到期金额',
  `actual_return` decimal(20,2) DEFAULT NULL COMMENT '实际收益',
  `settlement_date` datetime(6) DEFAULT NULL COMMENT '结算日期',
  `investment_notes` text COMMENT '投资备注',
  `rejection_reason` varchar(500) DEFAULT NULL COMMENT '拒绝原因',
  `endorsement_id` varchar(36) DEFAULT NULL COMMENT '关联背书ID',
  `tx_hash` varchar(100) DEFAULT NULL COMMENT '交易哈希',
  `blockchain_time` datetime(6) DEFAULT NULL COMMENT '区块链确认时间',
  `created_by` varchar(36) DEFAULT NULL COMMENT '创建人ID',
  `updated_by` varchar(36) DEFAULT NULL COMMENT '更新人ID',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_bill_id` (`bill_id`),
  KEY `idx_investor` (`investor_address`),
  KEY `idx_status` (`status`),
  KEY `idx_investment_date` (`investment_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='票据投资表';

-- ============================================
-- 表名：t_bill_merge_application
-- 说明：票据合并申请表
-- ============================================
DROP TABLE IF EXISTS `bill_merge_application`;
CREATE TABLE `bill_merge_application` (
  `id` varchar(36) NOT NULL COMMENT '申请ID（主键）',
  `source_bill_ids` text COMMENT '源票据ID列表（JSON数组）',
  `merged_bill_id` varchar(36) DEFAULT NULL COMMENT '合并后票据ID',
  `applicant_id` varchar(36) NOT NULL COMMENT '申请人ID',
  `merge_type` varchar(20) NOT NULL COMMENT '合并类型：AMOUNT-金额合并, PERIOD-期限合并, FULL-完全合并',
  `total_amount` decimal(20,2) NOT NULL COMMENT '合并后总金额',
  `merge_details` text NOT NULL COMMENT '合并明细（JSON格式）',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING-待处理, COMPLETED-已完成, FAILED-失败',
  `processor_id` varchar(36) DEFAULT NULL COMMENT '处理人ID',
  `processed_time` datetime DEFAULT NULL COMMENT '处理时间',
  `failure_reason` text COMMENT '失败原因',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_applicant` (`applicant_id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='票据合并申请表';

-- ============================================
-- 表名：t_bill_pledge_application
-- 说明：票据质押申请表
-- ============================================
DROP TABLE IF EXISTS `bill_pledge_application`;
CREATE TABLE `bill_pledge_application` (
  `application_id` varchar(36) NOT NULL COMMENT '申请ID（主键）',
  `bill_id` varchar(36) NOT NULL COMMENT '票据ID',
  `bill_no` varchar(50) NOT NULL COMMENT '票据编号',
  `bill_type` varchar(50) NOT NULL COMMENT '票据类型',
  `face_value` decimal(20,2) NOT NULL COMMENT '票面金额',
  `pledge_amount` decimal(20,2) NOT NULL COMMENT '质押金额',
  `pledge_period` int NOT NULL COMMENT '质押期限',
  `pledge_purpose` varchar(500) DEFAULT NULL COMMENT '质押用途',
  `financial_institution_id` varchar(36) NOT NULL COMMENT '金融机构ID',
  `financial_institution_name` varchar(200) NOT NULL COMMENT '金融机构名称',
  `applicant_id` varchar(36) NOT NULL COMMENT '申请人ID',
  `applicant_name` varchar(200) NOT NULL COMMENT '申请人名称',
  `applicant_address` varchar(42) DEFAULT NULL COMMENT '申请人地址',
  `collateral_info` text COMMENT '担保物信息',
  `guarantor_id` varchar(36) DEFAULT NULL COMMENT '担保人ID',
  `guarantor_name` varchar(200) DEFAULT NULL COMMENT '担保人名称',
  `risk_assessment` text COMMENT '风险评估',
  `credit_score` int DEFAULT NULL COMMENT '信用评分',
  `suggested_pledge_ratio` decimal(5,4) DEFAULT NULL COMMENT '建议质押率',
  `risk_level` varchar(50) NOT NULL COMMENT '风险等级：LOW-低风险, MEDIUM-中风险, HIGH-高风险, CRITICAL-极高风险',
  `application_status` varchar(50) NOT NULL DEFAULT 'PENDING' COMMENT '申请状态：PENDING-待审核, APPROVED-已批准, REJECTED-已拒绝',
  `reviewer_id` varchar(36) DEFAULT NULL COMMENT '审核人ID',
  `reviewer_name` varchar(200) DEFAULT NULL COMMENT '审核人名称',
  `approval_comments` text COMMENT '审核意见',
  `approval_date` datetime(6) DEFAULT NULL COMMENT '审核日期',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  `remarks` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`application_id`),
  KEY `idx_bill_id` (`bill_id`),
  KEY `idx_applicant` (`applicant_id`),
  KEY `idx_financial_institution` (`financial_institution_id`),
  KEY `idx_application_status` (`application_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='票据质押申请表';

-- ============================================
-- 表名：t_bill_split_application
-- 说明：票据拆分申请表
-- ============================================
DROP TABLE IF EXISTS `bill_split_application`;
CREATE TABLE `bill_split_application` (
  `id` varchar(36) NOT NULL COMMENT '申请ID（主键）',
  `parent_bill_id` varchar(36) NOT NULL COMMENT '父票据ID',
  `applicant_id` varchar(36) NOT NULL COMMENT '申请人ID',
  `split_scheme` varchar(20) NOT NULL COMMENT '拆分方案：EQUAL-均分, CUSTOM-自定义',
  `split_count` int NOT NULL COMMENT '拆分数量',
  `split_details` text COMMENT '拆分明细（JSON格式）',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING-待处理, COMPLETED-已完成, FAILED-失败',
  `processor_id` varchar(36) DEFAULT NULL COMMENT '处理人ID',
  `processed_time` datetime DEFAULT NULL COMMENT '处理时间',
  `failure_reason` text COMMENT '失败原因',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_parent_bill` (`parent_bill_id`),
  KEY `idx_applicant` (`applicant_id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='票据拆分申请表';

-- ============================================
-- 表名：t_discount_record
-- 说明：贴现记录表
-- ============================================
DROP TABLE IF EXISTS `discount_record`;
CREATE TABLE `discount_record` (
  `id` varchar(36) NOT NULL COMMENT '贴现记录ID（主键）',
  `bill_id` varchar(36) NOT NULL COMMENT '票据ID',
  `holder_address` varchar(42) NOT NULL COMMENT '贴现申请人地址',
  `financial_institution_address` varchar(42) NOT NULL COMMENT '金融机构地址',
  `bill_amount` decimal(20,2) NOT NULL COMMENT '票据票面金额',
  `discount_amount` decimal(20,2) NOT NULL COMMENT '贴现金额',
  `discount_rate` decimal(10,4) NOT NULL COMMENT '贴现率',
  `discount_interest` decimal(20,2) DEFAULT NULL COMMENT '贴现利息',
  `discount_date` datetime NOT NULL COMMENT '贴现日期',
  `maturity_date` datetime NOT NULL COMMENT '到期日期',
  `discount_days` int DEFAULT NULL COMMENT '贴现天数',
  `remark` text COMMENT '贴现备注',
  `tx_hash` varchar(66) DEFAULT NULL COMMENT '交易哈希',
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '贴现状态：ACTIVE-生效中, MATURED-已到期, REPAID-已还款, CANCELLED-已取消',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_bill_id` (`bill_id`),
  KEY `idx_holder` (`holder_address`),
  KEY `idx_financial_institution` (`financial_institution_address`),
  KEY `idx_discount_date` (`discount_date`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='贴现记录表';

-- ============================================
-- 表名：t_endorsement
-- 说明：背书记录表
-- ============================================
DROP TABLE IF EXISTS `endorsement`;
CREATE TABLE `endorsement` (
  `id` varchar(36) NOT NULL COMMENT '背书ID（主键）',
  `bill_id` varchar(36) NOT NULL COMMENT '票据ID',
  `endorser_address` varchar(42) NOT NULL COMMENT '背书人地址',
  `endorsee_address` varchar(42) NOT NULL COMMENT '被背书人地址',
  `endorsement_type` varchar(20) NOT NULL COMMENT '背书类型：NORMAL-普通背书, DISCOUNT-贴现背书, PLEDGE-质押背书',
  `endorsement_amount` bigint DEFAULT NULL COMMENT '背书金额',
  `endorsement_date` datetime NOT NULL COMMENT '背书日期',
  `remark` text COMMENT '背书备注',
  `tx_hash` varchar(66) DEFAULT NULL COMMENT '交易哈希',
  `endorsement_sequence` int DEFAULT NULL COMMENT '背书序号',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_bill_id` (`bill_id`),
  KEY `idx_endorser` (`endorser_address`),
  KEY `idx_endorsee` (`endorsee_address`),
  KEY `idx_endorsement_date` (`endorsement_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='背书记录表';

-- ============================================
-- 表名：t_repayment_record
-- 说明：还款记录表
-- ============================================
DROP TABLE IF EXISTS `repayment_record`;
CREATE TABLE `repayment_record` (
  `id` varchar(36) NOT NULL COMMENT '还款记录ID（主键）',
  `bill_id` varchar(36) NOT NULL COMMENT '票据ID',
  `payer_address` varchar(42) NOT NULL COMMENT '还款人地址',
  `financial_institution_address` varchar(42) NOT NULL COMMENT '金融机构地址',
  `bill_amount` decimal(20,2) NOT NULL COMMENT '票据票面金额',
  `discount_amount` decimal(20,2) DEFAULT NULL COMMENT '贴现金额',
  `payment_amount` decimal(20,2) NOT NULL COMMENT '还款金额',
  `payment_type` varchar(20) NOT NULL COMMENT '还款类型：FULL_PAYMENT-全额还款, PARTIAL_PAYMENT-部分还款, MATURITY_PAYMENT-到期还款, EARLY_PAYMENT-提前还款, OVERDUE_PAYMENT-逾期还款',
  `principal_amount` decimal(20,2) DEFAULT NULL COMMENT '正常还款金额',
  `interest_amount` decimal(20,2) DEFAULT NULL COMMENT '利息金额',
  `penalty_interest_amount` decimal(20,2) DEFAULT NULL COMMENT '逾期利息',
  `overdue_days` int DEFAULT NULL COMMENT '逾期天数',
  `payment_date` datetime NOT NULL COMMENT '还款日期',
  `due_date` datetime NOT NULL COMMENT '到期日期',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '还款状态：PENDING-待确认, COMPLETED-已完成, FAILED-失败, CANCELLED-已取消',
  `remark` text COMMENT '还款备注',
  `tx_hash` varchar(66) DEFAULT NULL COMMENT '交易哈希',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_bill_id` (`bill_id`),
  KEY `idx_payer` (`payer_address`),
  KEY `idx_financial_institution` (`financial_institution_address`),
  KEY `idx_payment_date` (`payment_date`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='还款记录表';

-- ============================================
-- 表名：t_bill_recourse
-- 说明：票据追索表
-- ============================================
DROP TABLE IF EXISTS `bill_recourse`;
CREATE TABLE `bill_recourse` (
  `recourse_id` varchar(36) NOT NULL COMMENT '追索ID（主键）',
  `bill_id` varchar(36) NOT NULL COMMENT '票据ID',
  `bill_no` varchar(50) NOT NULL COMMENT '票据编号',
  `face_value` decimal(20,2) NOT NULL COMMENT '票面金额',
  `dishonored_date` datetime DEFAULT NULL COMMENT '拒付日期',
  `dishonored_reason` text COMMENT '拒付原因',
  `dishonored_proof` varchar(100) DEFAULT NULL COMMENT '拒付证明文件',
  `acceptor_dishonor_reason` varchar(500) DEFAULT NULL COMMENT '承兑人拒付原因',
  `recourse_amount` decimal(20,2) NOT NULL COMMENT '追索金额',
  `penalty_amount` decimal(20,2) DEFAULT NULL COMMENT '罚息金额',
  `expense_amount` decimal(20,2) DEFAULT NULL COMMENT '费用金额',
  `total_recourse_amount` decimal(20,2) NOT NULL COMMENT '追索总额',
  `recourse_status` varchar(50) NOT NULL DEFAULT 'INITIATED' COMMENT '追索状态：INITIATED-已发起, IN_PROGRESS-进行中, COMPLETED-已完成, FAILED-失败, PARTIAL-部分追回',
  `notified_parties` text COMMENT '已通知的前手（JSON格式）',
  `notification_date` datetime DEFAULT NULL COMMENT '通知日期',
  `notification_proof` varchar(100) DEFAULT NULL COMMENT '通知证明文件',
  `recourse_results` text COMMENT '追索结果（JSON格式）',
  `settled_amount` decimal(20,2) DEFAULT NULL COMMENT '已追回金额',
  `settlement_date` datetime DEFAULT NULL COMMENT '追回日期',
  `settlement_proof` varchar(100) DEFAULT NULL COMMENT '追回证明文件',
  `legal_action` tinyint(1) DEFAULT 0 COMMENT '是否提起法律诉讼',
  `case_number` varchar(100) DEFAULT NULL COMMENT '案件编号',
  `court_name` varchar(200) DEFAULT NULL COMMENT '法院名称',
  `initiator_id` varchar(36) NOT NULL COMMENT '追索发起人ID',
  `initiator_name` varchar(200) NOT NULL COMMENT '追索发起人名称',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  `completed_at` datetime DEFAULT NULL COMMENT '完成时间',
  `remarks` varchar(1000) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`recourse_id`),
  KEY `idx_bill_id` (`bill_id`),
  KEY `idx_recourse_status` (`recourse_status`),
  KEY `idx_initiator` (`initiator_id`),
  KEY `idx_dishonored_date` (`dishonored_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='票据追索表';

-- ============================================
-- 表名：t_bill_settlement
-- 说明：票据结算表
-- ============================================
DROP TABLE IF EXISTS `bill_settlement`;
CREATE TABLE `bill_settlement` (
  `settlement_id` varchar(36) NOT NULL COMMENT '结算ID（主键）',
  `bill_id` varchar(36) NOT NULL COMMENT '票据ID',
  `bill_no` varchar(50) NOT NULL COMMENT '票据编号',
  `face_value` decimal(20,2) NOT NULL COMMENT '票面金额',
  `settlement_type` varchar(50) NOT NULL COMMENT '结算类型：DEBT_SETTLEMENT-债务结算, MULTILATERAL-多方结算, TRIANGULAR-三角结算',
  `settlement_amount` decimal(20,2) NOT NULL COMMENT '结算金额',
  `settlement_date` datetime DEFAULT NULL COMMENT '结算日期',
  `initiator_id` varchar(36) NOT NULL COMMENT '发起人ID',
  `initiator_name` varchar(200) NOT NULL COMMENT '发起人名称',
  `participants` text COMMENT '参与方信息（JSON格式）',
  `related_debts` text COMMENT '关联债务信息（JSON格式）',
  `debt_proof_documents` text COMMENT '债务证明文件（JSON格式）',
  `related_receipts` text COMMENT '关联仓单信息（JSON格式）',
  `receipt_transfer` tinyint(1) DEFAULT 0 COMMENT '是否涉及仓单转让',
  `settlement_status` varchar(50) NOT NULL DEFAULT 'PENDING' COMMENT '结算状态：PENDING-待处理, COMPLETED-已完成, FAILED-失败, PARTIAL-部分结算',
  `settlement_proof` varchar(100) DEFAULT NULL COMMENT '结算证明文件',
  `completion_date` datetime DEFAULT NULL COMMENT '完成日期',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  `created_by` varchar(36) DEFAULT NULL COMMENT '创建人ID',
  `remarks` varchar(1000) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`settlement_id`),
  KEY `idx_bill_id` (`bill_id`),
  KEY `idx_initiator` (`initiator_id`),
  KEY `idx_settlement_status` (`settlement_status`),
  KEY `idx_settlement_date` (`settlement_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='票据结算表';
