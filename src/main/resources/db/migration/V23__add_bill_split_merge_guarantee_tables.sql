-- ============================================
-- 票据拆分合并保理功能表
-- 版本: V23
-- 创建时间: 2026-02-09
-- 说明: 为票据管理添加拆分、合并、承兑、担保功能
-- ============================================

-- ============================================
-- 1. 扩展bill表，添加拆分合并相关字段
-- ============================================
ALTER TABLE bill
ADD COLUMN parent_bill_id VARCHAR(36) COMMENT '父票据ID（拆分或合并后）' AFTER updated_at,
ADD COLUMN split_count INT COMMENT '拆分数量' AFTER parent_bill_id,
ADD COLUMN merge_count INT COMMENT '合并前票据数量' AFTER split_count,
ADD COLUMN split_time DATETIME(6) COMMENT '拆分时间' AFTER merge_count,
ADD COLUMN merge_time DATETIME(6) COMMENT '合并时间' AFTER split_time,
ADD INDEX idx_parent_bill (parent_bill_id);

-- ============================================
-- 2. 添加承兑相关字段
-- ============================================
ALTER TABLE bill
ADD COLUMN acceptance_time DATETIME(6) COMMENT '承兑时间' AFTER merge_time,
ADD COLUMN acceptance_remarks VARCHAR(500) COMMENT '承兑备注' AFTER acceptance_time;

-- ============================================
-- 3. 添加担保相关字段
-- ============================================
ALTER TABLE bill
ADD COLUMN guarantee_id VARCHAR(36) COMMENT '担保记录ID' AFTER acceptance_remarks,
ADD COLUMN has_guarantee BOOLEAN DEFAULT FALSE COMMENT '是否有担保' AFTER guarantee_id,
ADD INDEX idx_guarantee (guarantee_id);

-- ============================================
-- 4. 票据拆分申请表
-- ============================================
CREATE TABLE bill_split_application (
    id VARCHAR(36) PRIMARY KEY,
    parent_bill_id VARCHAR(36) NOT NULL COMMENT '父票据ID',
    applicant_id VARCHAR(36) NOT NULL COMMENT '申请人ID（当前持票人）',
    split_scheme VARCHAR(20) NOT NULL COMMENT '拆分方案: EQUAL-等额, CUSTOM-自定义',
    split_count INT NOT NULL COMMENT '拆分数量',
    split_details TEXT NOT NULL COMMENT '拆分明细（JSON格式）|包含每个子票据的信息',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING-待处理, COMPLETED-已完成, FAILED-失败',
    processor_id VARCHAR(36) COMMENT '处理人ID',
    processed_time DATETIME(6) COMMENT '处理时间',
    failure_reason TEXT COMMENT '失败原因',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    INDEX idx_parent_bill (parent_bill_id),
    INDEX idx_applicant (applicant_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='票据拆分申请表';

-- ============================================
-- 5. 票据合并申请表
-- ============================================
CREATE TABLE bill_merge_application (
    id VARCHAR(36) PRIMARY KEY,
    source_bill_ids TEXT NOT NULL COMMENT '源票据ID列表（JSON数组）',
    merged_bill_id VARCHAR(36) COMMENT '合并后的票据ID（处理完成后生成）',
    applicant_id VARCHAR(36) NOT NULL COMMENT '申请人ID',
    merge_type VARCHAR(20) NOT NULL COMMENT '合并类型: AMOUNT-金额合并, PERIOD-期限合并, FULL-完全合并',
    total_amount DECIMAL(20,2) NOT NULL COMMENT '合并后总金额',
    merge_details TEXT NOT NULL COMMENT '合并明细（JSON格式）',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING-待处理, COMPLETED-已完成, FAILED-失败',
    processor_id VARCHAR(36) COMMENT '处理人ID',
    processed_time DATETIME(6) COMMENT '处理时间',
    failure_reason TEXT COMMENT '失败原因',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    INDEX idx_applicant (applicant_id),
    INDEX idx_merged_bill (merged_bill_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='票据合并申请表';

-- ============================================
-- 6. 票据担保记录表
-- ============================================
CREATE TABLE bill_guarantee (
    id VARCHAR(36) PRIMARY KEY,
    bill_id VARCHAR(36) NOT NULL COMMENT '票据ID',
    bill_no VARCHAR(50) NOT NULL COMMENT '票据编号',

    -- 担保人信息
    guarantor_id VARCHAR(36) NOT NULL COMMENT '担保人ID',
    guarantor_name VARCHAR(200) NOT NULL COMMENT '担保人名称',
    guarantor_address VARCHAR(42) COMMENT '担保人区块链地址',

    -- 担保信息
    guarantee_type VARCHAR(20) NOT NULL COMMENT '担保类型: FULL-全额担保, PARTIAL-部分担保, JOINT-联合担保',
    guarantee_amount DECIMAL(20,2) NOT NULL COMMENT '担保金额',
    guarantee_rate DECIMAL(10,6) COMMENT '担保费率（%）',
    guarantee_fee DECIMAL(20,2) COMMENT '担保费用',
    guarantee_period INT COMMENT '担保期限（天）',
    guarantee_start_date DATETIME(6) COMMENT '担保开始日期',
    guarantee_end_date DATETIME(6) COMMENT '担保结束日期',

    -- 风险评估
    risk_level VARCHAR(20) COMMENT '风险等级: LOW-低, MEDIUM-中, HIGH-高',
    credit_score INT COMMENT '信用评分（0-100）',
    risk_assessment TEXT COMMENT '风险评估详情JSON',

    -- 担保条件
    guarantee_conditions TEXT COMMENT '担保条件',
    collateral_info TEXT COMMENT '反担保措施JSON',

    -- 状态信息
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE-有效, EXPIRED-已过期, CLAIMED-已索赔, CANCELLED-已取消',
    claim_amount DECIMAL(20,2) COMMENT '已索赔金额',
    claim_date DATETIME(6) COMMENT '索赔日期',

    -- 区块链信息
    tx_hash VARCHAR(100) COMMENT '区块链交易哈希',

    -- 审计信息
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by VARCHAR(36) COMMENT '创建人ID',
    remarks VARCHAR(500) COMMENT '备注',

    INDEX idx_bill (bill_id),
    INDEX idx_guarantor (guarantor_id),
    INDEX idx_status (status),
    INDEX idx_guarantee_end_date (guarantee_end_date),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='票据担保记录表';

-- ============================================
-- 7. 添加表注释
-- ============================================
ALTER TABLE bill COMMENT = '票据主表（扩展版）- 支持拆分、合并、承兑、担保功能';

ALTER TABLE bill_split_application COMMENT = '票据拆分申请表 - 记录票据拆分申请和处理结果';

ALTER TABLE bill_merge_application COMMENT = '票据合并申请表 - 记录票据合并申请和处理结果';

ALTER TABLE bill_guarantee COMMENT = '票据担保记录表 - 记录第三方为票据提供担保的详细信息';
