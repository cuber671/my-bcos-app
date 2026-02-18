-- ============================================================================
-- 票据模块数据表创建脚本
-- 版本：V13
-- 创建时间：2026-02-02
-- 功能：创建票据相关的所有数据表
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. 创建票据主表 (bill)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `bill` (
    -- 主键
    `bill_id` VARCHAR(36) PRIMARY KEY COMMENT '票据ID',

    -- 基础信息
    `bill_no` VARCHAR(50) NOT NULL UNIQUE COMMENT '票据编号',
    `bill_type` VARCHAR(50) NOT NULL COMMENT '票据类型：BANK_ACCEPTANCE_BILL-银行承兑汇票, COMMERCIAL_ACCEPTANCE_BILL-商业承兑汇票, BANK_NOTE-银行本票',
    `face_value` DECIMAL(20, 2) NOT NULL COMMENT '票面金额',
    `currency` VARCHAR(10) NOT NULL DEFAULT 'CNY' COMMENT '货币类型',
    `issue_date` DATETIME(6) NOT NULL COMMENT '开票日期',
    `due_date` DATETIME(6) NOT NULL COMMENT '到期日期',

    -- 参与方信息
    `drawer_id` VARCHAR(36) NOT NULL COMMENT '出票人ID',
    `drawer_name` VARCHAR(200) NOT NULL COMMENT '出票人名称',
    `drawer_address` VARCHAR(42) COMMENT '出票人区块链地址',
    `drawer_account` VARCHAR(100) COMMENT '出票人银行账号',

    `drawee_id` VARCHAR(36) NOT NULL COMMENT '承兑人ID',
    `drawee_name` VARCHAR(200) NOT NULL COMMENT '承兑人名称',
    `drawee_address` VARCHAR(42) COMMENT '承兑人区块链地址',
    `drawee_account` VARCHAR(100) COMMENT '承兑人银行账号',

    `payee_id` VARCHAR(36) NOT NULL COMMENT '收款人ID',
    `payee_name` VARCHAR(200) NOT NULL COMMENT '收款人名称',
    `payee_address` VARCHAR(42) COMMENT '收款人区块链地址',
    `payee_account` VARCHAR(100) COMMENT '收款人银行账号',

    -- 当前持票人信息
    `current_holder_id` VARCHAR(36) NOT NULL COMMENT '当前持票人ID',
    `current_holder_name` VARCHAR(200) NOT NULL COMMENT '当前持票人名称',
    `current_holder_address` VARCHAR(42) COMMENT '当前持票人区块链地址',

    -- 状态信息
    `bill_status` VARCHAR(50) NOT NULL DEFAULT 'DRAFT' COMMENT '票据状态：DRAFT-草稿, PENDING_ISSUANCE-待开票, ISSUED-已开票, NORMAL-正常, ENDORSED-已背书, PLEDGED-已质押, DISCOUNTED-已贴现, FINANCED-已融资, FROZEN-已冻结, EXPIRED-已过期, DISHONORED-已拒付, CANCELLED-已作废, PAID-已付款, SETTLED-已结算',
    `blockchain_status` VARCHAR(50) NOT NULL DEFAULT 'NOT_ONCHAIN' COMMENT '区块链状态：NOT_ONCHAIN-未上链, PENDING-待上链, ONCHAIN-已上链, FAILED-上链失败',
    `blockchain_tx_hash` VARCHAR(100) COMMENT '区块链交易哈希',
    `blockchain_time` DATETIME(6) COMMENT '上链时间',

    -- 融资信息
    `discount_rate` DECIMAL(10, 6) COMMENT '贴现率（%）',
    `discount_amount` DECIMAL(20, 2) COMMENT '贴现金额',
    `discount_date` DATETIME(6) COMMENT '贴现日期',
    `discount_institution_id` VARCHAR(36) COMMENT '贴现机构ID',

    `pledge_amount` DECIMAL(20, 2) COMMENT '质押金额',
    `pledge_institution_id` VARCHAR(36) COMMENT '质押机构ID',
    `pledge_period` INT COMMENT '质押期限（天）',
    `pledge_date` DATETIME(6) COMMENT '质押日期',

    -- 仓单联动信息
    `receipt_pledge_id` VARCHAR(36) COMMENT '关联的仓单质押ID',
    `backed_receipt_id` VARCHAR(36) COMMENT '担保仓单ID',
    `receipt_pledge_value` DECIMAL(20, 2) COMMENT '仓单担保价值',

    -- 追索信息
    `dishonored` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否拒付',
    `dishonored_date` DATETIME(6) COMMENT '拒付日期',
    `dishonored_reason` TEXT COMMENT '拒付原因',
    `recourse_status` VARCHAR(50) COMMENT '追索状态：NOT_INITIATED-未发起, INITIATED-已发起, IN_PROGRESS-进行中, COMPLETED-已完成, FAILED-失败',

    -- 结算信息
    `settlement_id` VARCHAR(36) COMMENT '结算编号',
    `related_debts` TEXT COMMENT '关联债务（JSON格式）',
    `settlement_date` DATETIME(6) COMMENT '结算日期',

    -- 交易背景信息
    `trade_contract_id` VARCHAR(36) COMMENT '贸易合同ID',
    `trade_amount` DECIMAL(20, 2) COMMENT '贸易金额',
    `goods_description` VARCHAR(500) COMMENT '货物描述',
    `trade_date` DATE COMMENT '贸易日期',

    -- 审计信息
    `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
    `created_by` VARCHAR(36) COMMENT '创建人ID',
    `updated_by` VARCHAR(36) COMMENT '更新人ID',
    `remarks` VARCHAR(1000) COMMENT '备注',

    -- 索引
    INDEX `idx_bill_no` (`bill_no`),
    INDEX `idx_bill_type` (`bill_type`),
    INDEX `idx_bill_status` (`bill_status`),
    INDEX `idx_drawer_id` (`drawer_id`),
    INDEX `idx_drawee_id` (`drawee_id`),
    INDEX `idx_payee_id` (`payee_id`),
    INDEX `idx_current_holder_id` (`current_holder_id`),
    INDEX `idx_due_date` (`due_date`),
    INDEX `idx_backed_receipt_id` (`backed_receipt_id`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='票据主表';

-- ----------------------------------------------------------------------------
-- 2. 创建票据背书记录表 (bill_endorsement)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `bill_endorsement` (
    -- 主键
    `endorsement_id` VARCHAR(36) PRIMARY KEY COMMENT '背书ID',

    -- 关联票据
    `bill_id` VARCHAR(36) NOT NULL COMMENT '票据ID',
    `bill_no` VARCHAR(50) NOT NULL COMMENT '票据编号',

    -- 背书人信息
    `endorser_id` VARCHAR(36) NOT NULL COMMENT '背书人ID',
    `endorser_name` VARCHAR(200) NOT NULL COMMENT '背书人名称',
    `endorser_address` VARCHAR(42) COMMENT '背书人区块链地址',

    -- 被背书人信息
    `endorsee_id` VARCHAR(36) NOT NULL COMMENT '被背书人ID',
    `endorsee_name` VARCHAR(200) NOT NULL COMMENT '被背书人名称',
    `endorsee_address` VARCHAR(42) COMMENT '被背书人区块链地址',

    -- 背书信息
    `endorsement_type` VARCHAR(50) NOT NULL COMMENT '背书类型：TRANSFER-转让, PLEDGE-质押, COLLECTION-委托收款',
    `endorsement_reason` VARCHAR(500) COMMENT '背书原因',
    `related_contract` VARCHAR(36) COMMENT '关联合同ID',
    `endorsement_date` DATETIME(6) NOT NULL COMMENT '背书日期',

    -- 仓单联动信息
    `related_receipt_id` VARCHAR(36) COMMENT '关联仓单ID',
    `receipt_delivery` BOOLEAN DEFAULT FALSE COMMENT '是否涉及仓单交付',

    -- 区块链信息
    `blockchain_status` VARCHAR(50) NOT NULL DEFAULT 'NOT_ONCHAIN' COMMENT '区块链状态',
    `blockchain_tx_hash` VARCHAR(100) COMMENT '区块链交易哈希',
    `blockchain_time` DATETIME(6) COMMENT '上链时间',

    -- 审计信息
    `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    `created_by` VARCHAR(36) COMMENT '创建人ID',
    `remarks` VARCHAR(500) COMMENT '备注',

    -- 索引
    INDEX `idx_bill_id` (`bill_id`),
    INDEX `idx_bill_no` (`bill_no`),
    INDEX `idx_endorser_id` (`endorser_id`),
    INDEX `idx_endorsee_id` (`endorsee_id`),
    INDEX `idx_endorsement_type` (`endorsement_type`),
    INDEX `idx_endorsement_date` (`endorsement_date`),

    -- 外键约束
    FOREIGN KEY (`bill_id`) REFERENCES `bill`(`bill_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='票据背书记录表';

-- ----------------------------------------------------------------------------
-- 3. 创建票据质押融资申请表 (bill_pledge_application)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `bill_pledge_application` (
    -- 主键
    `application_id` VARCHAR(36) PRIMARY KEY COMMENT '申请ID',

    -- 关联票据
    `bill_id` VARCHAR(36) NOT NULL COMMENT '票据ID',
    `bill_no` VARCHAR(50) NOT NULL COMMENT '票据编号',
    `bill_type` VARCHAR(50) NOT NULL COMMENT '票据类型',
    `face_value` DECIMAL(20, 2) NOT NULL COMMENT '票面金额',

    -- 质押信息
    `pledge_amount` DECIMAL(20, 2) NOT NULL COMMENT '申请质押金额',
    `pledge_period` INT NOT NULL COMMENT '质押期限（天）',
    `pledge_purpose` VARCHAR(500) COMMENT '质押用途',
    `financial_institution_id` VARCHAR(36) NOT NULL COMMENT '金融机构ID',
    `financial_institution_name` VARCHAR(200) NOT NULL COMMENT '金融机构名称',

    -- 申请人信息
    `applicant_id` VARCHAR(36) NOT NULL COMMENT '申请人ID',
    `applicant_name` VARCHAR(200) NOT NULL COMMENT '申请人名称',
    `applicant_address` VARCHAR(42) COMMENT '申请人区块链地址',

    -- 额外担保物信息
    `collateral_info` TEXT COMMENT '额外担保物信息（JSON格式）',
    `guarantor_id` VARCHAR(36) COMMENT '担保人ID',
    `guarantor_name` VARCHAR(200) COMMENT '担保人名称',

    -- 风险评估
    `risk_assessment` TEXT COMMENT '风险评估结果（JSON格式）',
    `credit_score` INT COMMENT '信用评分',
    `suggested_pledge_ratio` DECIMAL(5, 4) COMMENT '建议质押率',
    `risk_level` VARCHAR(50) COMMENT '风险等级：LOW-低, MEDIUM-中, HIGH-高, CRITICAL-严重',

    -- 审核信息
    `application_status` VARCHAR(50) NOT NULL DEFAULT 'PENDING' COMMENT '申请状态：PENDING-待审核, APPROVED-已批准, REJECTED-已拒绝',
    `reviewer_id` VARCHAR(36) COMMENT '审核人ID',
    `reviewer_name` VARCHAR(200) COMMENT '审核人名称',
    `approval_comments` TEXT COMMENT '审核意见',
    `approval_date` DATETIME(6) COMMENT '审核日期',

    -- 审计信息
    `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '申请时间',
    `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
    `remarks` VARCHAR(500) COMMENT '备注',

    -- 索引
    INDEX `idx_bill_id` (`bill_id`),
    INDEX `idx_application_status` (`application_status`),
    INDEX `idx_financial_institution_id` (`financial_institution_id`),
    INDEX `idx_applicant_id` (`applicant_id`),
    INDEX `idx_created_at` (`created_at`),

    -- 外键约束
    FOREIGN KEY (`bill_id`) REFERENCES `bill`(`bill_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='票据质押融资申请表';

-- ----------------------------------------------------------------------------
-- 4. 创建票据追索记录表 (bill_recourse)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `bill_recourse` (
    -- 主键
    `recourse_id` VARCHAR(36) PRIMARY KEY COMMENT '追索ID',

    -- 关联票据
    `bill_id` VARCHAR(36) NOT NULL COMMENT '票据ID',
    `bill_no` VARCHAR(50) NOT NULL COMMENT '票据编号',
    `face_value` DECIMAL(20, 2) NOT NULL COMMENT '票面金额',

    -- 拒付信息
    `dishonored_date` DATETIME(6) NOT NULL COMMENT '拒付日期',
    `dishonored_reason` TEXT NOT NULL COMMENT '拒付原因',
    `dishonored_proof` VARCHAR(100) COMMENT '拒付证明文件编号',
    `acceptor_dishonor_reason` VARCHAR(500) COMMENT '承兑人拒付原因',

    -- 追索信息
    `recourse_amount` DECIMAL(20, 2) NOT NULL COMMENT '追索金额',
    `penalty_amount` DECIMAL(20, 2) COMMENT '罚息金额',
    `expense_amount` DECIMAL(20, 2) COMMENT '费用金额',
    `total_recourse_amount` DECIMAL(20, 2) COMMENT '追索总额',

    -- 追索状态
    `recourse_status` VARCHAR(50) NOT NULL DEFAULT 'INITIATED' COMMENT '追索状态：INITIATED-已发起, IN_PROGRESS-进行中, COMPLETED-已完成, FAILED-失败, PARTIAL-部分追回',

    -- 追索通知
    `notified_parties` TEXT COMMENT '已通知的前手（JSON格式）',
    `notification_date` DATETIME(6) COMMENT '通知日期',
    `notification_proof` VARCHAR(100) COMMENT '通知证明文件',

    -- 追索结果
    `recourse_results` TEXT COMMENT '追索结果详情（JSON格式）',
    `settled_amount` DECIMAL(20, 2) COMMENT '已追回金额',
    `settlement_date` DATETIME(6) COMMENT '追回日期',
    `settlement_proof` VARCHAR(100) COMMENT '追回证明文件',

    -- 法律诉讼
    `legal_action` BOOLEAN DEFAULT FALSE COMMENT '是否提起法律诉讼',
    `case_number` VARCHAR(100) COMMENT '案件编号',
    `court_name` VARCHAR(200) COMMENT '法院名称',

    -- 追索发起人
    `initiator_id` VARCHAR(36) NOT NULL COMMENT '追索发起人ID',
    `initiator_name` VARCHAR(200) NOT NULL COMMENT '追索发起人名称',

    -- 审计信息
    `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
    `completed_at` DATETIME(6) COMMENT '完成时间',
    `remarks` VARCHAR(1000) COMMENT '备注',

    -- 索引
    INDEX `idx_bill_id` (`bill_id`),
    INDEX `idx_recourse_status` (`recourse_status`),
    INDEX `idx_dishonored_date` (`dishonored_date`),
    INDEX `idx_initiator_id` (`initiator_id`),
    INDEX `idx_created_at` (`created_at`),

    -- 外键约束
    FOREIGN KEY (`bill_id`) REFERENCES `bill`(`bill_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='票据追索记录表';

-- ----------------------------------------------------------------------------
-- 5. 创建票据贴现记录表 (bill_discount)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `bill_discount` (
    -- 主键
    `discount_id` VARCHAR(36) PRIMARY KEY COMMENT '贴现ID',

    -- 关联票据
    `bill_id` VARCHAR(36) NOT NULL COMMENT '票据ID',
    `bill_no` VARCHAR(50) NOT NULL COMMENT '票据编号',
    `bill_type` VARCHAR(50) NOT NULL COMMENT '票据类型',
    `face_value` DECIMAL(20, 2) NOT NULL COMMENT '票面金额',

    -- 贴现信息
    `discount_rate` DECIMAL(10, 6) NOT NULL COMMENT '贴现率（%）',
    `discount_period` INT NOT NULL COMMENT '贴现期限（天）',
    `discount_interest` DECIMAL(20, 2) NOT NULL COMMENT '贴现利息',
    `net_amount` DECIMAL(20, 2) NOT NULL COMMENT '实付金额（票面金额 - 贴现利息）',

    -- 贴现机构
    `discount_institution_id` VARCHAR(36) NOT NULL COMMENT '贴现机构ID',
    `discount_institution_name` VARCHAR(200) NOT NULL COMMENT '贴现机构名称',

    -- 申请人信息
    `applicant_id` VARCHAR(36) NOT NULL COMMENT '申请人ID',
    `applicant_name` VARCHAR(200) NOT NULL COMMENT '申请人名称',
    `applicant_address` VARCHAR(42) COMMENT '申请人区块链地址',

    -- 申请信息
    `application_purpose` VARCHAR(500) COMMENT '贴现用途',
    `application_date` DATETIME(6) NOT NULL COMMENT '申请日期',
    `approval_date` DATETIME(6) COMMENT '批准日期',
    `payment_date` DATETIME(6) COMMENT '付款日期',

    -- 状态
    `application_status` VARCHAR(50) NOT NULL DEFAULT 'PENDING' COMMENT '申请状态：PENDING-待审核, APPROVED-已批准, REJECTED-已拒绝, PAID-已付款',

    -- 审核信息
    `reviewer_id` VARCHAR(36) COMMENT '审核人ID',
    `reviewer_name` VARCHAR(200) COMMENT '审核人名称',
    `approval_comments` TEXT COMMENT '审核意见',

    -- 资金信息
    `payment_account` VARCHAR(100) COMMENT '收款账号',
    `payment_voucher` VARCHAR(100) COMMENT '付款凭证编号',
    `payment_proof` VARCHAR(100) COMMENT '付款证明文件',

    -- 审计信息
    `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
    `remarks` VARCHAR(500) COMMENT '备注',

    -- 索引
    INDEX `idx_bill_id` (`bill_id`),
    INDEX `idx_application_status` (`application_status`),
    INDEX `idx_discount_institution_id` (`discount_institution_id`),
    INDEX `idx_applicant_id` (`applicant_id`),
    INDEX `idx_application_date` (`application_date`),

    -- 外键约束
    FOREIGN KEY (`bill_id`) REFERENCES `bill`(`bill_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='票据贴现记录表';

-- ----------------------------------------------------------------------------
-- 6. 创建票据结算记录表 (bill_settlement)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `bill_settlement` (
    -- 主键
    `settlement_id` VARCHAR(36) PRIMARY KEY COMMENT '结算ID',

    -- 关联票据
    `bill_id` VARCHAR(36) NOT NULL COMMENT '票据ID',
    `bill_no` VARCHAR(50) NOT NULL COMMENT '票据编号',
    `face_value` DECIMAL(20, 2) NOT NULL COMMENT '票面金额',

    -- 结算信息
    `settlement_type` VARCHAR(50) NOT NULL COMMENT '结算类型：DEBT_SETTLEMENT-债务结算, MULTILATERAL-多方结算, TRIANGULAR-三角债结算',
    `settlement_amount` DECIMAL(20, 2) NOT NULL COMMENT '结算金额',
    `settlement_date` DATETIME(6) NOT NULL COMMENT '结算日期',

    -- 参与方
    `initiator_id` VARCHAR(36) NOT NULL COMMENT '发起人ID',
    `initiator_name` VARCHAR(200) NOT NULL COMMENT '发起人名称',
    `participants` TEXT NOT NULL COMMENT '参与方信息（JSON格式）',

    -- 债权债务关系
    `related_debts` TEXT NOT NULL COMMENT '关联债务信息（JSON格式）',
    `debt_proof_documents` TEXT COMMENT '债务证明文件（JSON格式）',

    -- 仓单联动
    `related_receipts` TEXT COMMENT '关联仓单信息（JSON格式）',
    `receipt_transfer` BOOLEAN DEFAULT FALSE COMMENT '是否涉及仓单转让',

    -- 结算结果
    `settlement_status` VARCHAR(50) NOT NULL DEFAULT 'PENDING' COMMENT '结算状态：PENDING-待结算, COMPLETED-已完成, FAILED-失败, PARTIAL-部分结算',
    `settlement_proof` VARCHAR(100) COMMENT '结算证明文件',
    `completion_date` DATETIME(6) COMMENT '完成日期',

    -- 审计信息
    `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
    `created_by` VARCHAR(36) COMMENT '创建人ID',
    `remarks` VARCHAR(1000) COMMENT '备注',

    -- 索引
    INDEX `idx_bill_id` (`bill_id`),
    INDEX `idx_settlement_type` (`settlement_type`),
    INDEX `idx_settlement_status` (`settlement_status`),
    INDEX `idx_settlement_date` (`settlement_date`),
    INDEX `idx_initiator_id` (`initiator_id`),

    -- 外键约束
    FOREIGN KEY (`bill_id`) REFERENCES `bill`(`bill_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='票据结算记录表';

-- ============================================================================
-- 脚本执行完成
-- ============================================================================
