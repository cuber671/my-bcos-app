-- ============================================================================
-- 票据融资申请表创建脚本
-- 版本：V14
-- 创建时间：2026-02-02
-- 功能：创建票据融资申请表
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 创建票据融资申请表 (bill_finance_application)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `bill_finance_application` (
    -- 主键
    `id` VARCHAR(36) PRIMARY KEY COMMENT '融资申请ID',

    -- 关联票据信息
    `bill_id` VARCHAR(36) NOT NULL COMMENT '票据ID',
    `bill_no` VARCHAR(50) NOT NULL COMMENT '票据编号',
    `bill_face_value` DECIMAL(20, 2) NOT NULL COMMENT '票据面值',

    -- 申请信息
    `applicant_id` VARCHAR(36) NOT NULL COMMENT '申请人ID（企业）',
    `applicant_name` VARCHAR(200) NOT NULL COMMENT '申请人名称',
    `financial_institution_id` VARCHAR(36) NOT NULL COMMENT '金融机构ID',
    `financial_institution_name` VARCHAR(200) NOT NULL COMMENT '金融机构名称',

    -- 融资申请详情
    `finance_amount` DECIMAL(20, 2) NOT NULL COMMENT '申请融资金额',
    `finance_rate` DECIMAL(10, 4) NOT NULL COMMENT '申请融资利率（%）',
    `finance_period` INT NOT NULL COMMENT '融资期限（天）',
    `pledge_agreement` TEXT COMMENT '质押协议内容',

    -- 审核信息
    `status` VARCHAR(50) NOT NULL DEFAULT 'PENDING' COMMENT '申请状态：PENDING-待审核, APPROVED-已批准, REJECTED-已拒绝, ACTIVE-已放款, REPAID-已还款, CANCELLED-已取消',
    `approved_amount` DECIMAL(20, 2) COMMENT '批准金额',
    `approved_rate` DECIMAL(10, 4) COMMENT '批准利率（%）',
    `actual_amount` DECIMAL(20, 2) COMMENT '实际放款金额',
    `apply_date` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '申请日期',
    `approve_date` DATETIME(6) COMMENT '审核日期',
    `approval_comments` TEXT COMMENT '审核意见',
    `rejection_reason` VARCHAR(500) COMMENT '拒绝原因',

    -- 放款与还款信息
    `disbursement_date` DATETIME(6) COMMENT '放款日期',
    `repayment_date` DATETIME(6) COMMENT '还款日期',
    `actual_repayment_amount` DECIMAL(20, 2) COMMENT '实际还款金额',

    -- 区块链信息
    `tx_hash` VARCHAR(100) COMMENT '区块链交易哈希',

    -- 审计字段
    `created_by` VARCHAR(36) COMMENT '创建人ID',
    `updated_by` VARCHAR(36) COMMENT '更新人ID',
    `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',

    -- 索引
    INDEX `idx_bill_id` (`bill_id`),
    INDEX `idx_applicant_id` (`applicant_id`),
    INDEX `idx_financial_institution_id` (`financial_institution_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_apply_date` (`apply_date`),
    INDEX `idx_status_apply_date` (`status`, `apply_date`),

    -- 外键约束
    CONSTRAINT `fk_finance_app_bill` FOREIGN KEY (`bill_id`) REFERENCES `bill`(`bill_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_finance_app_applicant` FOREIGN KEY (`applicant_id`) REFERENCES `enterprise`(`enterprise_id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_finance_app_institution` FOREIGN KEY (`financial_institution_id`) REFERENCES `enterprise`(`enterprise_id`) ON DELETE RESTRICT

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='票据融资申请表';

-- ----------------------------------------------------------------------------
-- 添加注释
-- ----------------------------------------------------------------------------
ALTER TABLE `bill_finance_application` COMMENT = '票据融资申请表，用于管理票据融资申请、审核、放款和还款全流程';
