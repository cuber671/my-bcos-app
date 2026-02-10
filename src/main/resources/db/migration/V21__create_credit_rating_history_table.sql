-- 创建信用评级历史表
-- 记录企业信用评级的变更历史

CREATE TABLE IF NOT EXISTS credit_rating_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    enterprise_address VARCHAR(42) NOT NULL COMMENT '企业区块链地址',
    enterprise_name VARCHAR(255) COMMENT '企业名称',
    old_rating INT NOT NULL COMMENT '原评级',
    new_rating INT NOT NULL COMMENT '新评级',
    change_reason TEXT COMMENT '变更原因',
    changed_by VARCHAR(100) NOT NULL COMMENT '操作人用户名',
    changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '变更时间',
    tx_hash VARCHAR(128) COMMENT '区块链交易哈希',

    INDEX idx_rating_enterprise (enterprise_address),
    INDEX idx_rating_changed_by (changed_by),
    INDEX idx_rating_changed_at (changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业信用评级历史表';

-- 添加表注释
ALTER TABLE credit_rating_history COMMENT = '企业信用评级变更历史记录表，用于追踪企业信用评级的所有变更';
