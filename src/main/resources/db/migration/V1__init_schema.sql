-- 供应链金融系统数据库表结构
-- Create Database: CREATE DATABASE supply_chain_finance DEFAULT CHARSET utf8mb4;

-- ============================================================
-- 企业征信管理表
-- ============================================================

-- 企业基本信息表
CREATE TABLE IF NOT EXISTS enterprise (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '企业ID',
    address VARCHAR(42) NOT NULL UNIQUE COMMENT '区块链地址',
    name VARCHAR(255) NOT NULL COMMENT '企业名称',
    credit_code VARCHAR(50) NOT NULL UNIQUE COMMENT '统一社会信用代码',
    enterprise_address VARCHAR(500) COMMENT '企业地址',
    role VARCHAR(20) NOT NULL COMMENT '企业角色：SUPPLIER,CORE_ENTERPRISE,FINANCIAL_INSTITUTION,REGULATOR',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING,ACTIVE,SUSPENDED,BLACKLISTED',
    credit_rating INT DEFAULT 60 COMMENT '信用评级 (0-100)',
    credit_limit DECIMAL(20,2) DEFAULT 0 COMMENT '授信额度',
    registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by VARCHAR(50) COMMENT '创建人',
    updated_by VARCHAR(50) COMMENT '更新人',
    INDEX idx_address (address),
    INDEX idx_credit_code (credit_code),
    INDEX idx_status (status),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业基本信息表';

-- 信用历史记录表
CREATE TABLE IF NOT EXISTS credit_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    record_id VARCHAR(64) NOT NULL UNIQUE COMMENT '记录ID',
    enterprise_address VARCHAR(42) NOT NULL COMMENT '企业地址',
    rating_change INT COMMENT '评级变化',
    old_rating INT COMMENT '原评级',
    new_rating INT COMMENT '新评级',
    reason VARCHAR(500) COMMENT '原因',
    operator VARCHAR(50) COMMENT '操作员',
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '时间戳',
    tx_hash VARCHAR(66) COMMENT '区块链交易哈希',
    INDEX idx_enterprise (enterprise_address),
    INDEX idx_timestamp (timestamp),
    FOREIGN KEY (enterprise_address) REFERENCES enterprise(address) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='信用历史记录表';

-- ============================================================
-- 应收账款管理表
-- ============================================================

-- 应收账款表
CREATE TABLE IF NOT EXISTS receivable (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '应收账款ID',
    receivable_id VARCHAR(64) NOT NULL UNIQUE COMMENT '应收账款ID',
    supplier_address VARCHAR(42) NOT NULL COMMENT '供应商地址',
    core_enterprise_address VARCHAR(42) NOT NULL COMMENT '核心企业地址',
    amount DECIMAL(20,2) NOT NULL COMMENT '金额',
    currency VARCHAR(10) DEFAULT 'CNY' COMMENT '币种',
    issue_date TIMESTAMP NOT NULL COMMENT '出票日期',
    due_date TIMESTAMP NOT NULL COMMENT '到期日期',
    description TEXT COMMENT '描述',
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED' COMMENT '状态：CREATED,CONFIRMED,FINANCED,REPAID,DEFAULTED,CANCELLED',
    current_holder VARCHAR(42) NOT NULL COMMENT '当前持有人',
    financier_address VARCHAR(42) COMMENT '资金方地址',
    finance_amount DECIMAL(20,2) COMMENT '融资金额',
    finance_rate INT COMMENT '融资利率（基点）',
    finance_date TIMESTAMP COMMENT '融资日期',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    tx_hash VARCHAR(66) COMMENT '区块链交易哈希',
    INDEX idx_receivable_id (receivable_id),
    INDEX idx_supplier (supplier_address),
    INDEX idx_core_enterprise (core_enterprise_address),
    INDEX idx_holder (current_holder),
    INDEX idx_financier (financier_address),
    INDEX idx_status (status),
    INDEX idx_due_date (due_date),
    FOREIGN KEY (supplier_address) REFERENCES enterprise(address) ON DELETE RESTRICT,
    FOREIGN KEY (core_enterprise_address) REFERENCES enterprise(address) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应收账款表';

-- 应收账款转让历史表
CREATE TABLE IF NOT EXISTS receivable_transfer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '转让记录ID',
    receivable_id VARCHAR(64) NOT NULL COMMENT '应收账款ID',
    from_address VARCHAR(42) NOT NULL COMMENT '转出方',
    to_address VARCHAR(42) NOT NULL COMMENT '转入方',
    amount DECIMAL(20,2) NOT NULL COMMENT '转让金额',
    transfer_type VARCHAR(20) NOT NULL COMMENT '转让类型：financing,transfer,repayment',
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '时间戳',
    tx_hash VARCHAR(66) COMMENT '区块链交易哈希',
    INDEX idx_receivable (receivable_id),
    INDEX idx_from (from_address),
    INDEX idx_to (to_address),
    INDEX idx_timestamp (timestamp),
    FOREIGN KEY (receivable_id) REFERENCES receivable(receivable_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应收账款转让历史表';

-- ============================================================
-- 票据/信用证管理表
-- ============================================================

-- 票据表
CREATE TABLE IF NOT EXISTS bill (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '票据ID',
    bill_id VARCHAR(64) NOT NULL UNIQUE COMMENT '票据ID',
    bill_type VARCHAR(20) NOT NULL COMMENT '票据类型：COMMERCIAL_BILL,BANK_BILL,LETTER_OF_CREDIT',
    issuer_address VARCHAR(42) NOT NULL COMMENT '出票人地址',
    acceptor_address VARCHAR(42) NOT NULL COMMENT '承兑人地址',
    beneficiary_address VARCHAR(42) NOT NULL COMMENT '受益人地址',
    current_holder VARCHAR(42) NOT NULL COMMENT '当前持票人',
    amount DECIMAL(20,2) NOT NULL COMMENT '票面金额',
    currency VARCHAR(10) DEFAULT 'CNY' COMMENT '币种',
    issue_date TIMESTAMP NOT NULL COMMENT '出票日期',
    due_date TIMESTAMP NOT NULL COMMENT '到期日期',
    payment_date TIMESTAMP COMMENT '付款日期',
    status VARCHAR(20) NOT NULL DEFAULT 'ISSUED' COMMENT '状态：ISSUED,ENDORSED,DISCOUNTED,ACCEPTED,PAID,DISHONORED,CANCELLED',
    description TEXT COMMENT '描述',
    endorsement_count INT DEFAULT 0 COMMENT '背书次数',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    tx_hash VARCHAR(66) COMMENT '区块链交易哈希',
    INDEX idx_bill_id (bill_id),
    INDEX idx_issuer (issuer_address),
    INDEX idx_acceptor (acceptor_address),
    INDEX idx_holder (current_holder),
    INDEX idx_status (status),
    INDEX idx_due_date (due_date),
    FOREIGN KEY (issuer_address) REFERENCES enterprise(address) ON DELETE RESTRICT,
    FOREIGN KEY (acceptor_address) REFERENCES enterprise(address) ON DELETE RESTRICT,
    FOREIGN KEY (beneficiary_address) REFERENCES enterprise(address) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='票据表';

-- 票据背书历史表
CREATE TABLE IF NOT EXISTS bill_endorsement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '背书记录ID',
    bill_id VARCHAR(64) NOT NULL COMMENT '票据ID',
    endorser VARCHAR(42) NOT NULL COMMENT '背书人',
    endorsee VARCHAR(42) NOT NULL COMMENT '被背书人',
    endorsement_type VARCHAR(20) NOT NULL COMMENT '背书类型：transfer,pledge,discount',
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '时间戳',
    tx_hash VARCHAR(66) COMMENT '区块链交易哈希',
    INDEX idx_bill (bill_id),
    INDEX idx_endorser (endorser),
    INDEX idx_endorsee (endorsee),
    INDEX idx_timestamp (timestamp),
    FOREIGN KEY (bill_id) REFERENCES bill(bill_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='票据背书历史表';

-- ============================================================
-- 仓单融资管理表
-- ============================================================

-- 仓单表
CREATE TABLE IF NOT EXISTS warehouse_receipt (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '仓单ID',
    receipt_id VARCHAR(64) NOT NULL UNIQUE COMMENT '仓单ID',
    owner_address VARCHAR(42) NOT NULL COMMENT '货主地址',
    warehouse_address VARCHAR(42) NOT NULL COMMENT '仓库地址',
    financial_institution VARCHAR(42) COMMENT '金融机构地址',
    goods_name VARCHAR(255) NOT NULL COMMENT '货物名称',
    goods_type VARCHAR(100) COMMENT '货物类型',
    quantity DECIMAL(20,2) NOT NULL COMMENT '数量',
    unit VARCHAR(20) NOT NULL COMMENT '单位',
    unit_price DECIMAL(20,2) NOT NULL COMMENT '单价（分）',
    total_price DECIMAL(20,2) NOT NULL COMMENT '总价（分）',
    quality VARCHAR(50) COMMENT '质量等级',
    origin VARCHAR(255) COMMENT '产地',
    warehouse_location VARCHAR(500) COMMENT '仓库物理位置',
    storage_date TIMESTAMP NOT NULL COMMENT '入库日期',
    expiry_date TIMESTAMP NOT NULL COMMENT '过期日期',
    release_date TIMESTAMP COMMENT '释放日期',
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED' COMMENT '状态：CREATED,VERIFIED,PLEDGED,FINANCED,RELEASED,LIQUIDATED,EXPIRED',
    pledge_amount DECIMAL(20,2) COMMENT '质押金额',
    finance_amount DECIMAL(20,2) COMMENT '融资金额',
    finance_rate INT COMMENT '融资利率',
    finance_date TIMESTAMP COMMENT '融资日期',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    tx_hash VARCHAR(66) COMMENT '区块链交易哈希',
    INDEX idx_receipt_id (receipt_id),
    INDEX idx_owner (owner_address),
    INDEX idx_warehouse (warehouse_address),
    INDEX idx_financier (financial_institution),
    INDEX idx_status (status),
    INDEX idx_expiry_date (expiry_date),
    FOREIGN KEY (owner_address) REFERENCES enterprise(address) ON DELETE RESTRICT,
    FOREIGN KEY (warehouse_address) REFERENCES enterprise(address) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仓单表';

-- 仓单质押历史表
CREATE TABLE IF NOT EXISTS warehouse_pledge (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '质押记录ID',
    receipt_id VARCHAR(64) NOT NULL COMMENT '仓单ID',
    owner VARCHAR(42) NOT NULL COMMENT '货主',
    financial_institution VARCHAR(42) NOT NULL COMMENT '金融机构',
    amount DECIMAL(20,2) NOT NULL COMMENT '金额',
    record_type VARCHAR(20) NOT NULL COMMENT '记录类型：pledge,release,liquidate',
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '时间戳',
    tx_hash VARCHAR(66) COMMENT '区块链交易哈希',
    INDEX idx_receipt (receipt_id),
    INDEX idx_owner (owner),
    INDEX idx_financier (financial_institution),
    INDEX idx_timestamp (timestamp),
    FOREIGN KEY (receipt_id) REFERENCES warehouse_receipt(receipt_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仓单质押历史表';

-- ============================================================
-- 系统用户管理表
-- ============================================================

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码（加密）',
    enterprise_address VARCHAR(42) COMMENT '关联企业地址',
    real_name VARCHAR(100) COMMENT '真实姓名',
    email VARCHAR(100) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    role VARCHAR(20) NOT NULL COMMENT '角色：ADMIN,USER',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE,DISABLED',
    last_login_at TIMESTAMP COMMENT '最后登录时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_enterprise (enterprise_address)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- ============================================================
-- 系统审计日志表
-- ============================================================

-- 操作日志表
CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    user_id BIGINT COMMENT '用户ID',
    username VARCHAR(50) COMMENT '用户名',
    operation VARCHAR(100) NOT NULL COMMENT '操作类型',
    module VARCHAR(50) COMMENT '模块名称',
    method VARCHAR(100) COMMENT '方法名',
    params TEXT COMMENT '参数',
    ip VARCHAR(50) COMMENT 'IP地址',
    status VARCHAR(20) NOT NULL COMMENT '状态：SUCCESS,FAILURE',
    error_msg TEXT COMMENT '错误信息',
    execution_time INT COMMENT '执行时间(ms)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user (user_id),
    INDEX idx_operation (operation),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';
