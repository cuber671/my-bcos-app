-- ============================================================================
-- 票据投资表创建脚本
-- 版本：V15
-- 创建时间：2026-02-03
-- 功能：创建票据投资记录表
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 创建票据投资表 (bill_investment)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `bill_investment` (
    -- 主键
    `id` VARCHAR(36) PRIMARY KEY COMMENT '投资记录ID',

    -- 关联票据信息
    `bill_id` VARCHAR(36) NOT NULL COMMENT '票据ID',
    `bill_no` VARCHAR(50) NOT NULL COMMENT '票据编号',
    `bill_face_value` DECIMAL(20, 2) NOT NULL COMMENT '票据面值',

    -- 投资方信息
    `investor_id` VARCHAR(36) NOT NULL COMMENT '投资机构ID',
    `investor_name` VARCHAR(200) NOT NULL COMMENT '投资机构名称',
    `investor_address` VARCHAR(42) COMMENT '投资机构区块链地址',

    -- 原持票人信息
    `original_holder_id` VARCHAR(36) COMMENT '原持票人ID',
    `original_holder_name` VARCHAR(200) COMMENT '原持票人名称',
    `original_holder_address` VARCHAR(42) COMMENT '原持票人地址',

    -- 投资详情
    `invest_amount` DECIMAL(20, 2) NOT NULL COMMENT '投资金额（实际支付）',
    `invest_rate` DECIMAL(10, 4) NOT NULL COMMENT '投资利率（%）',
    `expected_return` DECIMAL(20, 2) COMMENT '预期收益',
    `investment_days` INT COMMENT '投资天数（票据剩余天数）',

    -- 投资状态
    `status` VARCHAR(50) NOT NULL DEFAULT 'PENDING' COMMENT '投资状态：PENDING-待确认, CONFIRMED-已确认, COMPLETED-已完成, CANCELLED-已取消, FAILED-失败',
    `investment_date` DATETIME(6) COMMENT '投资日期',
    `confirmation_date` DATETIME(6) COMMENT '确认日期',
    `completion_date` DATETIME(6) COMMENT '完成日期',
    `cancellation_date` DATETIME(6) COMMENT '撤销日期',

    -- 收益结算
    `maturity_amount` DECIMAL(20, 2) COMMENT '到期金额（票据面值）',
    `actual_return` DECIMAL(20, 2) COMMENT '实际收益',
    `settlement_date` DATETIME(6) COMMENT '结算日期',

    -- 备注信息
    `investment_notes` TEXT COMMENT '投资备注',
    `rejection_reason` VARCHAR(500) COMMENT '拒绝原因',

    -- 区块链信息
    `endorsement_id` VARCHAR(36) COMMENT '关联的背书ID',
    `tx_hash` VARCHAR(100) COMMENT '区块链交易哈希',
    `blockchain_time` DATETIME(6) COMMENT '区块链确认时间',

    -- 审计字段
    `created_by` VARCHAR(36) COMMENT '创建人',
    `updated_by` VARCHAR(36) COMMENT '更新人',
    `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',

    -- 索引
    INDEX `idx_invest_bill_id` (`bill_id`),
    INDEX `idx_invest_investor` (`investor_id`),
    INDEX `idx_invest_status` (`status`),
    INDEX `idx_invest_date` (`investment_date`),
    INDEX `idx_invest_bill_status` (`bill_id`, `status`),

    -- 外键约束
    CONSTRAINT `fk_invest_bill` FOREIGN KEY (`bill_id`) REFERENCES `bill`(`bill_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_invest_investor` FOREIGN KEY (`investor_id`) REFERENCES `enterprise`(`enterprise_id`) ON DELETE RESTRICT,

    -- 唯一约束：同一票据同时只能有一个PENDING状态的投资
    UNIQUE KEY `uk_bill_pending` (`bill_id`, `status`)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='票据投资记录表';

-- ----------------------------------------------------------------------------
-- 添加注释
-- ----------------------------------------------------------------------------
ALTER TABLE `bill_investment` COMMENT = '票据投资记录表，记录金融机构通过票据池投资票据的完整信息';

-- ----------------------------------------------------------------------------
-- 创建投资状态枚举注释
-- ----------------------------------------------------------------------------
-- PENDING: 待确认 - 投资请求已创建，等待背书确认
-- CONFIRMED: 已确认 - 背书已执行，投资已确认
-- COMPLETED: 已完成 - 票据已到期并完成结算
-- CANCELLED: 已取消 - 投资被用户取消或系统自动取消
-- FAILED: 失败 - 投资过程中发生错误
