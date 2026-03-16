-- Test Database Schema for FISCO BCOS Supply Chain Finance Platform
-- Used for unit testing with H2 in-memory database

-- ==================== Enterprise Table ====================
CREATE TABLE IF NOT EXISTS t_enterprise (
    enterprise_id BIGINT NOT NULL COMMENT '企业ID',
    username VARCHAR(50) NOT NULL COMMENT '登录账号',
    password VARCHAR(255) NOT NULL COMMENT '登录密码(BCrypt加密)',
    pay_password VARCHAR(255) COMMENT '支付密码(BCrypt加密)',
    enterprise_name VARCHAR(100) NOT NULL COMMENT '企业名称',
    org_code VARCHAR(50) NOT NULL COMMENT '统一社会信用代码',
    blockchain_address VARCHAR(100) COMMENT '区块链地址',
    encrypted_private_key TEXT COMMENT '加密的区块链私钥',
    status INT NOT NULL DEFAULT 0 COMMENT '企业状态: 0-待审核, 1-正常, 2-冻结, 3-注销中, 4-已注销, 6-待注销审批',
    credit_rating VARCHAR(10) COMMENT '信用评级: AAA, AA, A, BBB, BB, B, C, D',
    credit_limit DECIMAL(20,2) DEFAULT 0 COMMENT '授信额度',
    available_credit DECIMAL(20,2) DEFAULT 0 COMMENT '可用额度',
    credit_score INT DEFAULT 100 COMMENT '信用分',
    last_login_time DATETIME COMMENT '最后登录时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (enterprise_id),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_org_code (org_code)
);

-- ==================== Invitation Code Table ====================
CREATE TABLE IF NOT EXISTS t_invitation_code (
    id BIGINT NOT NULL AUTO_INCREMENT,
    enterprise_id BIGINT NOT NULL COMMENT '企业ID',
    invite_code VARCHAR(20) NOT NULL COMMENT '邀请码',
    expire_time DATETIME COMMENT '过期时间',
    max_uses INT DEFAULT 1 COMMENT '最大使用次数',
    used_count INT DEFAULT 0 COMMENT '已使用次数',
    status INT DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_invite_code (invite_code)
);

-- ==================== User Table ====================
CREATE TABLE IF NOT EXISTS t_user (
    user_id BIGINT NOT NULL COMMENT '用户ID',
    enterprise_id BIGINT NOT NULL COMMENT '所属企业ID',
    real_name VARCHAR(50) NOT NULL COMMENT '用户真实姓名',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) COMMENT '企业邮箱',
    username VARCHAR(50) NOT NULL COMMENT '登录账号',
    password VARCHAR(255) NOT NULL COMMENT '登录密码(BCrypt加密)',
    user_role VARCHAR(20) NOT NULL DEFAULT 'OPERATOR' COMMENT '职能角色',
    status INT NOT NULL DEFAULT 2 COMMENT '账户状态: 1-待审核, 2-正常, 3-冻结, 4-注销中, 5-已注销',
    last_login_time DATETIME COMMENT '最后登录时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_phone (phone)
);

-- ==================== Warehouse Table ====================
CREATE TABLE IF NOT EXISTS t_warehouse (
    warehouse_id BIGINT NOT NULL AUTO_INCREMENT,
    warehouse_name VARCHAR(100) NOT NULL COMMENT '仓库名称',
    warehouse_address VARCHAR(255) COMMENT '仓库地址',
    warehouse_type VARCHAR(20) COMMENT '仓库类型',
    capacity DECIMAL(20,2) COMMENT '容量',
    status INT DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (warehouse_id)
);

-- ==================== Stock Order Table ====================
CREATE TABLE IF NOT EXISTS t_stock_order (
    stock_order_id BIGINT NOT NULL AUTO_INCREMENT,
    stock_no VARCHAR(30) NOT NULL COMMENT '入库单号',
    enterprise_id BIGINT NOT NULL COMMENT '企业ID',
    warehouse_id BIGINT NOT NULL COMMENT '仓库ID',
    goods_name VARCHAR(100) NOT NULL COMMENT '货物名称',
    goods_type VARCHAR(50) COMMENT '货物类型',
    weight DECIMAL(20,3) COMMENT '重量(吨)',
    unit VARCHAR(20) COMMENT '单位',
    unit_price DECIMAL(20,2) COMMENT '单价',
    total_amount DECIMAL(20,2) COMMENT '总金额',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态: PENDING, CONFIRMED, CANCELLED',
    data_hash VARCHAR(128) COMMENT '数据哈希',
    chain_tx_hash VARCHAR(128) COMMENT '区块链交易哈希',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (stock_order_id),
    UNIQUE KEY uk_stock_no (stock_no)
);

-- ==================== Warehouse Receipt Table ====================
CREATE TABLE IF NOT EXISTS t_warehouse_receipt (
    receipt_id BIGINT NOT NULL AUTO_INCREMENT,
    receipt_no VARCHAR(50) NOT NULL COMMENT '仓单编号',
    enterprise_id BIGINT NOT NULL COMMENT '企业ID',
    warehouse_id BIGINT NOT NULL COMMENT '仓库ID',
    owner_name VARCHAR(100) NOT NULL COMMENT '货主名称',
    goods_detail_hash VARCHAR(128) COMMENT '货物详情哈希',
    location_photo_hash VARCHAR(128) COMMENT '位置照片哈希',
    weight DECIMAL(20,3) NOT NULL COMMENT '重量',
    unit VARCHAR(20) NOT NULL COMMENT '单位',
    quantity INT NOT NULL COMMENT '数量',
    storage_date DATE NOT NULL COMMENT '存储日期',
    expiry_date DATE COMMENT '到期日期',
    status INT DEFAULT 1 COMMENT '状态: 1-在库, 2-待转让, 3-运输中, 4-已质押, 5-已解锁, 6-已出库, 7-已合并拆分',
    is_locked BOOLEAN DEFAULT FALSE COMMENT '是否已质押',
    loan_id BIGINT COMMENT '关联的贷款ID',
    chain_receipt_id VARCHAR(128) COMMENT '链上仓单ID',
    chain_tx_hash VARCHAR(128) COMMENT '区块链交易哈希',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (receipt_id),
    UNIQUE KEY uk_receipt_no (receipt_no)
);

-- ==================== Receipt Endorsement Table ====================
CREATE TABLE IF NOT EXISTS t_receipt_endorsement (
    endorsement_id BIGINT NOT NULL AUTO_INCREMENT,
    receipt_id BIGINT NOT NULL COMMENT '仓单ID',
    from_enterprise_id BIGINT NOT NULL COMMENT '转让方企业ID',
    to_enterprise_id BIGINT NOT NULL COMMENT '受让方企业ID',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态: PENDING, CONFIRMED, REVOKED',
    chain_tx_hash VARCHAR(128) COMMENT '区块链交易哈希',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (endorsement_id)
);

-- ==================== Logistics Delegate Table ====================
CREATE TABLE IF NOT EXISTS t_logistics_delegate (
    delegate_id BIGINT NOT NULL AUTO_INCREMENT,
    voucher_no VARCHAR(50) NOT NULL COMMENT '委托单号',
    enterprise_id BIGINT NOT NULL COMMENT '企业ID',
    receipt_id BIGINT NOT NULL COMMENT '仓单ID',
    driver_name VARCHAR(50) COMMENT '司机姓名',
    driver_phone VARCHAR(20) COMMENT '司机电话',
    vehicle_no VARCHAR(20) COMMENT '车牌号',
    start_warehouse_id BIGINT NOT NULL COMMENT '起始仓库ID',
    end_warehouse_id BIGINT NOT NULL COMMENT '目的仓库ID',
    estimated_arrival DATETIME COMMENT '预计到达时间',
    actual_arrival DATETIME COMMENT '实际到达时间',
    status VARCHAR(20) DEFAULT 'CREATED' COMMENT '状态: CREATED, ASSIGNED, PICKED_UP, IN_TRANSIT, DELIVERED, CANCELLED',
    chain_tx_hash VARCHAR(128) COMMENT '区块链交易哈希',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (delegate_id),
    UNIQUE KEY uk_voucher_no (voucher_no)
);

-- ==================== Logistics Track Table ====================
CREATE TABLE IF NOT EXISTS t_logistics_track (
    track_id BIGINT NOT NULL AUTO_INCREMENT,
    delegate_id BIGINT NOT NULL COMMENT '委托单ID',
    latitude DECIMAL(10,6) COMMENT '纬度',
    longitude DECIMAL(10,6) COMMENT '经度',
    location VARCHAR(255) COMMENT '位置描述',
    action_type VARCHAR(20) COMMENT '操作类型',
    remark VARCHAR(500) COMMENT '备注',
    chain_tx_hash VARCHAR(128) COMMENT '区块链交易哈希',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (track_id)
);

-- ==================== Credit Profile Table ====================
CREATE TABLE IF NOT EXISTS t_enterprise_credit_profile (
    profile_id BIGINT NOT NULL AUTO_INCREMENT,
    enterprise_id BIGINT NOT NULL COMMENT '企业ID',
    credit_score INT DEFAULT 100 COMMENT '信用分',
    credit_rating VARCHAR(10) COMMENT '信用评级',
    credit_limit DECIMAL(20,2) DEFAULT 0 COMMENT '授信额度',
    used_credit DECIMAL(20,2) DEFAULT 0 COMMENT '已用额度',
    available_credit DECIMAL(20,2) DEFAULT 0 COMMENT '可用额度',
    chain_credit_limit VARCHAR(128) COMMENT '链上授信额度',
    chain_used_credit VARCHAR(128) COMMENT '链上已用额度',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (profile_id),
    UNIQUE KEY uk_enterprise_id (enterprise_id)
);

-- ==================== Credit Event Table ====================
CREATE TABLE IF NOT EXISTS t_credit_event (
    event_id BIGINT NOT NULL AUTO_INCREMENT,
    enterprise_id BIGINT NOT NULL COMMENT '企业ID',
    event_type VARCHAR(50) NOT NULL COMMENT '事件类型',
    event_level VARCHAR(20) DEFAULT 'LOW' COMMENT '事件级别: LOW, MEDIUM, HIGH, SEVERE',
    event_score INT DEFAULT 0 COMMENT '事件分值',
    event_desc VARCHAR(500) COMMENT '事件描述',
    source_module VARCHAR(50) COMMENT '来源模块',
    chain_tx_hash VARCHAR(128) COMMENT '区块链交易哈希',
    event_time DATETIME NOT NULL COMMENT '事件时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (event_id),
    KEY idx_enterprise_id (enterprise_id)
);

-- ==================== Receivable Table ====================
CREATE TABLE IF NOT EXISTS t_finance_receivable (
    receivable_id BIGINT NOT NULL AUTO_INCREMENT,
    receivable_no VARCHAR(50) NOT NULL COMMENT '应收款编号',
    enterprise_id BIGINT NOT NULL COMMENT '企业ID(债权人)',
    debtor_enterprise_id BIGINT NOT NULL COMMENT '债务企业ID',
    delegate_id BIGINT COMMENT '物流委托单ID',
    receipt_id BIGINT COMMENT '仓单ID',
    amount DECIMAL(20,2) NOT NULL COMMENT '应收款金额',
    paid_amount DECIMAL(20,2) DEFAULT 0 COMMENT '已还金额',
    status VARCHAR(20) DEFAULT 'pending' COMMENT '状态: pending, active, financed, settled',
    due_date DATE COMMENT '到期日期',
    chain_receivable_id VARCHAR(128) COMMENT '链上应收款ID',
    chain_tx_hash VARCHAR(128) COMMENT '区块链交易哈希',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (receivable_id),
    UNIQUE KEY uk_receivable_no (receivable_no)
);

-- ==================== Repayment Record Table ====================
CREATE TABLE IF NOT EXISTS t_finance_repayment_record (
    record_id BIGINT NOT NULL AUTO_INCREMENT,
    receivable_id BIGINT NOT NULL COMMENT '应收款ID',
    repayment_type VARCHAR(20) NOT NULL COMMENT '还款类型: cash, collateral',
    amount DECIMAL(20,2) NOT NULL COMMENT '还款金额',
    status VARCHAR(20) DEFAULT 'pending' COMMENT '状态: pending, completed, failed',
    chain_tx_hash VARCHAR(128) COMMENT '区块链交易哈希',
    repayment_time DATETIME COMMENT '还款时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (record_id),
    KEY idx_receivable_id (receivable_id)
);

-- ==================== Blockchain Transaction Record Table ====================
CREATE TABLE IF NOT EXISTS t_blockchain_transaction_record (
    record_id BIGINT NOT NULL AUTO_INCREMENT,
    tx_type VARCHAR(50) NOT NULL COMMENT '交易类型',
    tx_hash VARCHAR(128) NOT NULL COMMENT '交易哈希',
    from_address VARCHAR(100) COMMENT '发起方地址',
    to_address VARCHAR(100) COMMENT '接收方地址',
    tx_data TEXT COMMENT '交易数据(JSON)',
    block_number BIGINT COMMENT '区块高度',
    status VARCHAR(20) DEFAULT 'pending' COMMENT '状态: pending, confirmed, failed',
    error_msg VARCHAR(500) COMMENT '错误信息',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (record_id),
    KEY idx_tx_hash (tx_hash),
    KEY idx_status (status)
);
