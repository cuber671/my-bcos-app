-- ============================================================
-- V17: 企业信用额度管理表
-- 作者: System
-- 日期: 2026-02-03
-- 描述: 创建企业信用额度管理相关的4张表（方案B：独立表设计）
-- ============================================================

-- ============================================
-- 表1: credit_limit（信用额度主表）
-- ============================================
CREATE TABLE IF NOT EXISTS credit_limit (
    -- 主键
    id VARCHAR(36) PRIMARY KEY COMMENT '额度ID（UUID）',

    -- 企业信息
    enterprise_address VARCHAR(42) NOT NULL COMMENT '企业地址',
    enterprise_name VARCHAR(200) COMMENT '企业名称（冗余，方便查询）',

    -- 额度类型
    limit_type VARCHAR(20) NOT NULL COMMENT '额度类型：FINANCING-融资额度, GUARANTEE-担保额度, CREDIT-赊账额度',

    -- 额度金额（单位：分）
    total_limit BIGINT NOT NULL DEFAULT 0 COMMENT '总额度（分）',
    used_limit BIGINT NOT NULL DEFAULT 0 COMMENT '已使用额度（分）',
    frozen_limit BIGINT NOT NULL DEFAULT 0 COMMENT '冻结额度（分）',

    -- 额度配置
    warning_threshold INT NOT NULL DEFAULT 80 COMMENT '预警阈值（百分比，如80表示80%）',
    effective_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生效日期',
    expiry_date TIMESTAMP COMMENT '失效日期（NULL表示永久有效）',

    -- 额度状态
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '额度状态：ACTIVE-生效中, FROZEN-已冻结, EXPIRED-已失效, CANCELLED-已取消',

    -- 审批信息
    approver_address VARCHAR(42) COMMENT '审批人地址',
    approve_reason TEXT COMMENT '审批原因/说明',
    approve_time TIMESTAMP COMMENT '审批时间',

    -- 风险控制
    overdue_count INT NOT NULL DEFAULT 0 COMMENT '逾期次数',
    bad_debt_count INT NOT NULL DEFAULT 0 COMMENT '坏账次数',
    risk_level VARCHAR(20) DEFAULT 'LOW' COMMENT '风险等级：LOW-低风险, MEDIUM-中风险, HIGH-高风险',

    -- 元数据
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    -- 区块链信息
    tx_hash VARCHAR(66) COMMENT '上链交易哈希',

    -- 索引
    INDEX idx_enterprise (enterprise_address),
    INDEX idx_type (limit_type),
    INDEX idx_status (status),
    INDEX idx_expiry (expiry_date),
    INDEX idx_risk (risk_level),
    UNIQUE KEY uk_enterprise_type_status (enterprise_address, limit_type, status)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='企业信用额度表';

-- ============================================
-- 表2: credit_limit_usage（额度使用记录表）
-- ============================================
CREATE TABLE IF NOT EXISTS credit_limit_usage (
    -- 主键
    id VARCHAR(36) PRIMARY KEY COMMENT '记录ID（UUID）',

    -- 关联信息
    limit_id VARCHAR(36) NOT NULL COMMENT '额度ID',
    enterprise_address VARCHAR(42) NOT NULL COMMENT '企业地址（冗余，方便查询）',
    receivable_id VARCHAR(36) COMMENT '关联的应收账款ID',

    -- 使用信息
    usage_type VARCHAR(20) NOT NULL COMMENT '使用类型：USE-使用, RELEASE-释放, FREEZE-冻结, UNFREEZE-解冻',
    amount BIGINT NOT NULL COMMENT '金额（分，正数）',

    -- 额度快照（记录操作前后的额度状态）
    before_total_limit BIGINT NOT NULL COMMENT '操作前总额度（分）',
    before_used_limit BIGINT NOT NULL COMMENT '操作前已使用额度（分）',
    before_frozen_limit BIGINT NOT NULL COMMENT '操作前冻结额度（分）',
    after_total_limit BIGINT NOT NULL COMMENT '操作后总额度（分）',
    after_used_limit BIGINT NOT NULL COMMENT '操作后已使用额度（分）',
    after_frozen_limit BIGINT NOT NULL COMMENT '操作后冻结额度（分）',
    before_available_limit BIGINT NOT NULL COMMENT '操作前可用额度（分）',
    after_available_limit BIGINT NOT NULL COMMENT '操作后可用额度（分）',

    -- 操作信息
    operator_address VARCHAR(42) COMMENT '操作人地址',
    operator_name VARCHAR(100) COMMENT '操作人姓名（冗余）',
    reason TEXT COMMENT '操作原因',

    -- 系统信息
    auto_generated BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否系统自动生成',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    -- 区块链信息
    tx_hash VARCHAR(66) COMMENT '上链交易哈希',

    -- 索引
    INDEX idx_limit (limit_id),
    INDEX idx_enterprise (enterprise_address),
    INDEX idx_receivable (receivable_id),
    INDEX idx_type (usage_type),
    INDEX idx_time (create_time),
    INDEX idx_operator (operator_address),
    CONSTRAINT fk_usage_limit FOREIGN KEY (limit_id) REFERENCES credit_limit(id) ON DELETE CASCADE

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='额度使用记录表';

-- ============================================
-- 表3: credit_limit_adjust_request（额度调整申请表）
-- ============================================
CREATE TABLE IF NOT EXISTS credit_limit_adjust_request (
    -- 主键
    id VARCHAR(36) PRIMARY KEY COMMENT '申请ID（UUID）',

    -- 企业信息
    enterprise_address VARCHAR(42) NOT NULL COMMENT '企业地址',
    enterprise_name VARCHAR(200) COMMENT '企业名称（冗余）',

    -- 调整信息
    limit_type VARCHAR(20) NOT NULL COMMENT '额度类型',
    adjust_type VARCHAR(20) NOT NULL COMMENT '调整类型：INCREASE-增加, DECREASE-减少, RESET-重置',
    adjust_amount BIGINT NOT NULL COMMENT '调整金额（分，正数）',

    -- 当前额度信息（快照）
    current_limit BIGINT NOT NULL COMMENT '当前总额度（分）',
    new_limit BIGINT COMMENT '调整后总额度（分，审批通过后填充）',

    -- 申请信息
    reason TEXT NOT NULL COMMENT '调整原因',
    request_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',

    -- 申请人信息
    requester_address VARCHAR(42) NOT NULL COMMENT '申请人地址',
    requester_name VARCHAR(100) COMMENT '申请人姓名',
    requester_role VARCHAR(50) COMMENT '申请人角色：ENTERPRISE_ADMIN等',

    -- 审批信息
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '申请状态：PENDING-待审批, APPROVED-已通过, REJECTED-已拒绝',
    approver_address VARCHAR(42) COMMENT '审批人地址',
    approver_name VARCHAR(100) COMMENT '审批人姓名',
    approve_reason TEXT COMMENT '审批意见',
    approve_time TIMESTAMP COMMENT '审批时间',

    -- 审批优先级
    priority INT NOT NULL DEFAULT 5 COMMENT '优先级：1-最高, 5-普通, 10-最低',

    -- 系统信息
    auto_generated BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否系统自动生成',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    -- 区块链信息
    tx_hash VARCHAR(66) COMMENT '上链交易哈希',

    -- 索引
    INDEX idx_enterprise (enterprise_address),
    INDEX idx_status (status),
    INDEX idx_type (limit_type),
    INDEX idx_time (request_time),
    INDEX idx_priority (priority),
    INDEX idx_requester (requester_address)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='额度调整申请表';

-- ============================================
-- 表4: credit_limit_warning（额度预警记录表）
-- ============================================
CREATE TABLE IF NOT EXISTS credit_limit_warning (
    -- 主键
    id VARCHAR(36) PRIMARY KEY COMMENT '预警ID（UUID）',

    -- 企业信息
    enterprise_address VARCHAR(42) NOT NULL COMMENT '企业地址',
    enterprise_name VARCHAR(200) COMMENT '企业名称',
    limit_id VARCHAR(36) NOT NULL COMMENT '额度ID',
    limit_type VARCHAR(20) NOT NULL COMMENT '额度类型',

    -- 预警信息
    warning_level VARCHAR(20) NOT NULL COMMENT '预警级别：LOW-低, MEDIUM-中, HIGH-高, CRITICAL-紧急',
    warning_type VARCHAR(50) NOT NULL COMMENT '预警类型：USAGE_RATE-使用率, OVERDUE-逾期, RISK-风险, EXPIRY-到期',
    warning_message TEXT NOT NULL COMMENT '预警消息',

    -- 预警数据
    current_usage_rate DECIMAL(5,2) COMMENT '当前使用率（百分比）',
    warning_threshold INT COMMENT '预警阈值',

    -- 额度快照
    total_limit BIGINT COMMENT '总额度（分）',
    used_limit BIGINT COMMENT '已使用额度（分）',
    frozen_limit BIGINT COMMENT '冻结额度（分）',
    available_limit BIGINT COMMENT '可用额度（分）',

    -- 风险指标
    overdue_count INT COMMENT '逾期次数',
    bad_debt_count INT COMMENT '坏账次数',
    overdue_amount BIGINT COMMENT '逾期金额（分）',

    -- 处理信息
    is_handled BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否已处理',
    handled_by VARCHAR(42) COMMENT '处理人地址',
    handled_time TIMESTAMP COMMENT '处理时间',
    handle_note TEXT COMMENT '处理说明',

    -- 系统信息
    auto_generated BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否系统自动生成',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    -- 索引
    INDEX idx_enterprise (enterprise_address),
    INDEX idx_level (warning_level),
    INDEX idx_type (warning_type),
    INDEX idx_handled (is_handled),
    INDEX idx_time (created_at)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='额度预警记录表';

-- ============================================
-- 创建视图：额度使用情况汇总
-- ============================================
CREATE OR REPLACE VIEW v_credit_limit_summary AS
SELECT
    cl.id,
    cl.enterprise_address,
    cl.enterprise_name,
    cl.limit_type,
    cl.total_limit,
    cl.used_limit,
    cl.frozen_limit,
    (cl.total_limit - cl.used_limit - cl.frozen_limit) AS available_limit,
    CASE
        WHEN cl.total_limit > 0 THEN
            ROUND((cl.used_limit + cl.frozen_limit) * 100.0 / cl.total_limit, 2)
        ELSE 0
    END AS usage_rate,
    cl.warning_threshold,
    cl.status,
    cl.risk_level,
    cl.overdue_count,
    cl.bad_debt_count,
    cl.effective_date,
    cl.expiry_date,
    (SELECT COUNT(*) FROM credit_limit_usage WHERE limit_id = cl.id) AS usage_count
FROM credit_limit cl
WHERE cl.status = 'ACTIVE';

-- ============================================
-- 初始化数据：为现有企业创建默认额度（可选）
-- ============================================
-- 注意：这部分可以根据实际业务需求决定是否执行

-- 为资金方创建融资额度
INSERT INTO credit_limit (
    id,
    enterprise_address,
    enterprise_name,
    limit_type,
    total_limit,
    used_limit,
    frozen_limit,
    warning_threshold,
    status,
    risk_level,
    approve_reason
)
SELECT
    UUID(),
    e.address,
    e.name,
    'FINANCING',
    100000000,  -- 默认100万额度（1000000元 = 100000000分）
    0,
    0,
    80,
    'ACTIVE',
    'LOW',
    '系统初始化默认融资额度'
FROM enterprise e
WHERE e.role = 'FINANCIAL_INSTITUTION'
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

-- 为供应商创建融资额度
INSERT INTO credit_limit (
    id,
    enterprise_address,
    enterprise_name,
    limit_type,
    total_limit,
    used_limit,
    frozen_limit,
    warning_threshold,
    status,
    risk_level,
    approve_reason
)
SELECT
    UUID(),
    e.address,
    e.name,
    'FINANCING',
    50000000,   -- 默认50万额度（500000元 = 50000000分）
    0,
    0,
    80,
    'ACTIVE',
    'LOW',
    '系统初始化默认融资额度'
FROM enterprise e
WHERE e.role = 'SUPPLIER'
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

-- ============================================
-- 创建存储过程：检查并冻结逾期额度
-- ============================================
DELIMITER $$

CREATE PROCEDURE sp_freeze_overdue_limits()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_receivable_id VARCHAR(36);
    DECLARE v_supplier_address VARCHAR(42);
    DECLARE v_financier_address VARCHAR(42);
    DECLARE v_finance_amount BIGINT;
    DECLARE v_overdue_days INT;
    DECLARE v_limit_id VARCHAR(36);
    DECLARE v_freeze_amount BIGINT;
    DECLARE v_current_usage_rate DECIMAL(5,2);

    -- 声明游标：查找逾期超过30天的应收账款
    DECLARE cursor_overdue CURSOR FOR
        SELECT r.id, r.supplier_address, r.financier_address,
               r.finance_amount,
               DATEDIFF(NOW(), r.due_date) as overdue_days
        FROM receivable r
        WHERE r.status IN ('FINANCED', 'DEFAULTED')
          AND r.due_date < DATE_SUB(NOW(), INTERVAL 30 DAY)
          AND NOT EXISTS (
              SELECT 1 FROM credit_limit_usage
              WHERE receivable_id = r.id
                AND usage_type = 'FREEZE'
          );

    -- 定义异常处理器
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN cursor_overdue;

    read_loop: LOOP
        FETCH cursor_overdue INTO v_receivable_id, v_supplier_address,
                                   v_financier_address, v_finance_amount, v_overdue_days;
        IF done THEN
            LEAVE read_loop;
        END IF;

        -- 冻结供应商的融资额度
        SELECT id INTO v_limit_id
        FROM credit_limit
        WHERE enterprise_address = v_supplier_address
          AND limit_type = 'FINANCING'
          AND status = 'ACTIVE'
        LIMIT 1;

        IF v_limit_id IS NOT NULL THEN
            -- 计算冻结金额
            SELECT total_limit, used_limit, frozen_limit
            INTO @total_limit, @used_limit, @frozen_limit
            FROM credit_limit
            WHERE id = v_limit_id;

            SET v_freeze_amount = LEAST(v_finance_amount,
                (@total_limit - @used_limit - @frozen_limit));

            IF v_freeze_amount > 0 THEN
                -- 记录当前使用率
                SET v_current_usage_rate = (@used_limit + @frozen_limit) * 100.0 / @total_limit;

                -- IF 使用率超过90%或逾期超过90天，冻结全部额度
                IF v_current_usage_rate >= 90 OR v_overdue_days >= 90 THEN
                    SET v_freeze_amount = (@total_limit - @used_limit - @frozen_limit);
                END IF;

                -- 更新额度（增加冻结金额）
                UPDATE credit_limit
                SET frozen_limit = frozen_limit + v_freeze_amount,
                    updated_at = NOW(),
                    -- 根据使用率和逾期天数更新风险等级
                    risk_level = CASE
                        WHEN v_current_usage_rate >= 90 OR v_overdue_days >= 90 THEN 'HIGH'
                        WHEN v_current_usage_rate >= 80 OR v_overdue_days >= 60 THEN 'MEDIUM'
                        ELSE 'LOW'
                    END
                WHERE id = v_limit_id;

                -- 记录使用记录
                INSERT INTO credit_limit_usage (
                    id, limit_id, enterprise_address, receivable_id,
                    usage_type, amount,
                    before_total_limit, before_used_limit, before_frozen_limit,
                    after_total_limit, after_used_limit, after_frozen_limit,
                    before_available_limit, after_available_limit,
                    reason, auto_generated
                )
                VALUES (
                    UUID(),
                    v_limit_id,
                    v_supplier_address,
                    v_receivable_id,
                    'FREEZE',
                    v_freeze_amount,
                    @total_limit,
                    @used_limit,
                    @frozen_limit,
                    @total_limit,
                    @used_limit,
                    @frozen_limit + v_freeze_amount,
                    @total_limit - @used_limit - @frozen_limit,
                    @total_limit - @used_limit - (@frozen_limit + v_freeze_amount),
                    CONCAT('逾期', v_overdue_days, '天，自动冻结额度', v_freeze_amount, '分'),
                    TRUE
                );

                -- 创建预警记录
                INSERT INTO credit_limit_warning (
                    id, enterprise_address, enterprise_name, limit_id, limit_type,
                    warning_level, warning_type, warning_message,
                    current_usage_rate, warning_threshold,
                    total_limit, used_limit, frozen_limit, available_limit,
                    overdue_count, auto_generated
                )
                SELECT
                    UUID(),
                    v_supplier_address,
                    e.name,
                    v_limit_id,
                    'FINANCING',
                    CASE
                        WHEN v_current_usage_rate >= 90 OR v_overdue_days >= 90 THEN 'HIGH'
                        WHEN v_current_usage_rate >= 80 OR v_overdue_days >= 60 THEN 'MEDIUM'
                        ELSE 'LOW'
                    END,
                    'OVERDUE',
                    CONCAT('企业逾期', v_overdue_days, '天，已自动冻结额度', v_freeze_amount, '分'),
                    v_current_usage_rate,
                    80,
                    @total_limit,
                    @used_limit,
                    @frozen_limit + v_freeze_amount,
                    (@total_limit - @used_limit - (@frozen_limit + v_freeze_amount)),
                    (SELECT COUNT(*) FROM receivable WHERE supplier_address = v_supplier_address AND status = 'DEFAULTED'),
                    TRUE
                FROM enterprise e
                WHERE e.address = v_supplier_address;
            END IF;
        END IF;
    END LOOP;

    CLOSE cursor_overdue;
END$$

DELIMITER ;

-- ============================================
-- 优化表：添加缺失的外键约束
-- ============================================
-- 注意：外键约束可能影响性能，根据实际情况决定是否启用

-- ALTER TABLE credit_limit_adjust_request
-- ADD CONSTRAINT fk_request_enterprise FOREIGN KEY (enterprise_address)
-- REFERENCES enterprise(address) ON DELETE CASCADE;

-- ALTER TABLE credit_limit_warning
-- ADD CONSTRAINT fk_warning_limit FOREIGN KEY (limit_id)
-- REFERENCES credit_limit(id) ON DELETE CASCADE;

-- ============================================
-- 完成标记
-- ============================================
-- 表创建完成时间: 2026-02-03
-- 表数量: 4 张
-- 视图数量: 1 个
-- 存储过程: 1 个
-- 初始化数据: 已为 FINANCIAL_INSTITUTION 和 SUPPLIER 创建默认额度
