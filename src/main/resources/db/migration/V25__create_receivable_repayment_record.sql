-- 应收账款还款记录表
-- 创建时间: 2026-02-09
-- 功能: 记录应收账款的还款详情，支持部分还款、提前还款、逾期还款等场景

-- 创建应收账款还款记录表
CREATE TABLE receivable_repayment_record (
    id VARCHAR(36) PRIMARY KEY COMMENT '记录ID（UUID）',
    receivable_id VARCHAR(64) NOT NULL COMMENT '应收账款ID',
    repayment_type VARCHAR(20) NOT NULL COMMENT '还款类型: PARTIAL-部分还款, FULL-全额还款, EARLY-提前还款, OVERDUE-逾期还款',
    repayment_amount DECIMAL(20,2) NOT NULL COMMENT '还款总金额',
    principal_amount DECIMAL(20,2) NOT NULL COMMENT '本金金额',
    interest_amount DECIMAL(20,2) DEFAULT 0 COMMENT '利息金额',
    penalty_amount DECIMAL(20,2) DEFAULT 0 COMMENT '罚息金额',

    payer_address VARCHAR(42) NOT NULL COMMENT '还款人地址（核心企业）',
    receiver_address VARCHAR(42) NOT NULL COMMENT '收款人地址（供应商或金融机构）',
    payment_date DATE NOT NULL COMMENT '还款日期',
    actual_payment_time DATETIME(6) NOT NULL COMMENT '实际还款时间',

    payment_method VARCHAR(20) COMMENT '支付方式: BANK-银行转账, ALIPAY-支付宝, WECHAT-微信, OTHER-其他',
    payment_account VARCHAR(100) COMMENT '支付账号',
    transaction_no VARCHAR(64) COMMENT '交易流水号',
    voucher_url VARCHAR(500) COMMENT '凭证URL',

    early_payment_days INT COMMENT '提前还款天数（提前还款时记录）',
    overdue_days INT COMMENT '逾期天数（逾期还款时记录）',
    remark TEXT COMMENT '备注',

    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING-待确认, CONFIRMED-已确认, FAILED-失败',
    tx_hash VARCHAR(66) COMMENT '区块链交易哈希',

    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by VARCHAR(42) COMMENT '创建人地址',
    updated_by VARCHAR(42) COMMENT '更新人地址',

    INDEX idx_receivable_id (receivable_id) COMMENT '按应收账款ID查询',
    INDEX idx_payer (payer_address) COMMENT '按还款人查询',
    INDEX idx_receiver (receiver_address) COMMENT '按收款人查询',
    INDEX idx_payment_date (payment_date) COMMENT '按还款日期查询',
    INDEX idx_status (status) COMMENT '按状态查询',
    INDEX idx_repayment_type (repayment_type) COMMENT '按还款类型查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应收账款还款记录表';
