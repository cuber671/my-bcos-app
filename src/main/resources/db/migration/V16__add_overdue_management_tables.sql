-- ============================================
-- 应收账款逾期管理模块 - 数据库迁移脚本
-- Version: V16
-- Date: 2026-02-03
-- ============================================

-- ============================================
-- 1. 扩展 receivable 表，添加逾期管理字段
-- ============================================

ALTER TABLE receivable ADD COLUMN IF NOT EXISTS overdue_level VARCHAR(20) COMMENT '逾期等级: MILD-轻度(1-30天), MODERATE-中度(31-90天), SEVERE-重度(91-179天), BAD_DEBT-坏账(180天+)';
ALTER TABLE receivable ADD COLUMN IF NOT EXISTS overdue_days INT COMMENT '逾期天数';
ALTER TABLE receivable ADD COLUMN IF NOT EXISTS penalty_amount DECIMAL(20,2) DEFAULT 0 COMMENT '累计罚息金额';
ALTER TABLE receivable ADD COLUMN IF NOT EXISTS last_remind_date TIMESTAMP COMMENT '最后催收日期';
ALTER TABLE receivable ADD COLUMN IF NOT EXISTS remind_count INT DEFAULT 0 COMMENT '催收次数';
ALTER TABLE receivable ADD COLUMN IF NOT EXISTS bad_debt_date TIMESTAMP COMMENT '坏账认定日期';
ALTER TABLE receivable ADD COLUMN IF NOT EXISTS bad_debt_reason TEXT COMMENT '坏账原因';
ALTER TABLE receivable ADD COLUMN IF NOT EXISTS overdue_calculated_date TIMESTAMP COMMENT '逾期信息计算日期';

-- 添加索引
CREATE INDEX IF NOT EXISTS idx_overdue_level ON receivable(overdue_level);
CREATE INDEX IF NOT EXISTS idx_bad_debt_date ON receivable(bad_debt_date);

-- ============================================
-- 2. 创建催收记录表 (overdue_remind_record)
-- ============================================

CREATE TABLE IF NOT EXISTS overdue_remind_record (
    id VARCHAR(36) PRIMARY KEY COMMENT '记录ID（UUID）',
    receivable_id VARCHAR(36) NOT NULL COMMENT '应收账款ID',
    remind_type VARCHAR(20) NOT NULL COMMENT '催收类型: EMAIL-邮件, SMS-短信, PHONE-电话, LETTER-函件, LEGAL-法律',
    remind_level VARCHAR(20) COMMENT '催收级别: NORMAL-普通, URGENT-紧急, SEVERE-严重',
    remind_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '催收日期',
    operator_address VARCHAR(42) COMMENT '操作人地址',
    remind_content TEXT COMMENT '催收内容',
    remind_result VARCHAR(20) COMMENT '催收结果: SUCCESS-成功, FAILED-失败, PENDING-待处理',
    next_remind_date TIMESTAMP COMMENT '下次催收日期',
    remark TEXT COMMENT '备注',
    tx_hash VARCHAR(66) COMMENT '区块链交易哈希',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_receivable_id (receivable_id),
    INDEX idx_remind_type (remind_type),
    INDEX idx_remind_date (remind_date),
    INDEX idx_operator (operator_address)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='逾期催收记录表';

-- ============================================
-- 3. 创建罚息记录表 (overdue_penalty_record)
-- ============================================

CREATE TABLE IF NOT EXISTS overdue_penalty_record (
    id VARCHAR(36) PRIMARY KEY COMMENT '记录ID（UUID）',
    receivable_id VARCHAR(36) NOT NULL COMMENT '应收账款ID',
    penalty_type VARCHAR(20) NOT NULL COMMENT '罚息类型: AUTO-自动计算, MANUAL-手动计算',
    principal_amount DECIMAL(20,2) NOT NULL COMMENT '本金金额',
    overdue_days INT NOT NULL COMMENT '逾期天数',
    daily_rate DECIMAL(10,6) NOT NULL COMMENT '日利率（如0.0005表示0.05%）',
    penalty_amount DECIMAL(20,2) NOT NULL COMMENT '本次罚息金额',
    total_penalty_amount DECIMAL(20,2) NOT NULL COMMENT '累计罚息金额',
    calculate_start_date TIMESTAMP NOT NULL COMMENT '计算起始日期',
    calculate_end_date TIMESTAMP NOT NULL COMMENT '计算结束日期',
    calculate_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '计算日期',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_receivable_id (receivable_id),
    INDEX idx_penalty_type (penalty_type),
    INDEX idx_calculate_date (calculate_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='逾期罚息记录表';

-- ============================================
-- 4. 创建坏账记录表 (bad_debt_record)
-- ============================================

CREATE TABLE IF NOT EXISTS bad_debt_record (
    id VARCHAR(36) PRIMARY KEY COMMENT '记录ID（UUID）',
    receivable_id VARCHAR(36) NOT NULL UNIQUE COMMENT '应收账款ID（唯一）',
    bad_debt_type VARCHAR(20) NOT NULL COMMENT '坏账类型: OVERDUE_180-逾期180天+, BANKRUPTCY-破产, DISPUTE-争议, OTHER-其他',
    principal_amount DECIMAL(20,2) NOT NULL COMMENT '本金金额',
    overdue_days INT NOT NULL COMMENT '逾期天数',
    total_penalty_amount DECIMAL(20,2) DEFAULT 0 COMMENT '累计罚息金额',
    total_loss_amount DECIMAL(20,2) NOT NULL COMMENT '总损失金额（本金+罚息）',
    bad_debt_reason TEXT COMMENT '坏账原因',
    recovery_status VARCHAR(20) NOT NULL DEFAULT 'NOT_RECOVERED' COMMENT '回收状态: NOT_RECOVERED-未回收, PARTIAL_RECOVERED-部分回收, FULL_RECOVERED-全额回收',
    recovered_amount DECIMAL(20,2) DEFAULT 0 COMMENT '已回收金额',
    recovery_date TIMESTAMP COMMENT '回收日期',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_receivable_id (receivable_id),
    INDEX idx_bad_debt_type (bad_debt_type),
    INDEX idx_recovery_status (recovery_status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='坏账记录表';
