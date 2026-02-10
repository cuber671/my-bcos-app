-- ============================================================
-- 仓单合并和变更历史功能表
-- 版本: V24
-- 创建时间: 2026-02-09
-- 说明: 为仓单管理添加合并、变更历史功能
-- ============================================================

-- ============================================================
-- 1. 仓单合并申请表
-- ============================================================
CREATE TABLE IF NOT EXISTS receipt_merge_application (
    id VARCHAR(36) NOT NULL PRIMARY KEY COMMENT '申请ID（UUID格式）',
    source_receipt_ids TEXT NOT NULL COMMENT '源仓单ID列表（JSON数组格式）',
    merged_receipt_id VARCHAR(36) COMMENT '合并后的仓单ID（审核通过后生成）',
    applicant_id VARCHAR(36) NOT NULL COMMENT '申请人ID（货主企业）',
    applicant_name VARCHAR(100) COMMENT '申请人姓名',
    merge_type VARCHAR(20) NOT NULL COMMENT '合并类型: QUANTITY-数量合并, VALUE-价值合并, FULL-完全合并',
    total_quantity DECIMAL(20,2) COMMENT '合并后总数量',
    total_value DECIMAL(20,2) COMMENT '合并后总价值',
    merge_details TEXT NOT NULL COMMENT '合并明细（JSON格式）|包含每个源仓单的合并分配信息',
    request_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '申请状态: PENDING-待审核, APPROVED-已通过, REJECTED-已拒绝, CANCELLED-已取消',
    reviewer_id VARCHAR(36) COMMENT '审核人ID',
    reviewer_name VARCHAR(100) COMMENT '审核人姓名',
    review_time DATETIME(6) COMMENT '审核时间',
    review_comments TEXT COMMENT '审核意见',
    merge_tx_hash VARCHAR(128) COMMENT '合并上链交易哈希',
    block_number BIGINT COMMENT '区块号',
    remarks TEXT COMMENT '备注',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '申请时间',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
    INDEX idx_applicant (applicant_id),
    INDEX idx_merged_receipt (merged_receipt_id),
    INDEX idx_status (request_status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仓单合并申请表';

-- ============================================================
-- 2. 扩展电子仓单表，添加合并相关字段
-- ============================================================
ALTER TABLE electronic_warehouse_receipt
ADD COLUMN merge_count INT COMMENT '合并数量|合并前的源仓单数量|只有合并生成的仓单有值' AFTER updated_by,
ADD COLUMN merge_time DATETIME(6) COMMENT '合并时间|仓单合并生成的时间' AFTER merge_count,
ADD COLUMN source_receipt_ids TEXT COMMENT '源仓单ID列表（JSON格式）|追溯合并来源' AFTER merge_time;

-- ============================================================
-- 3. 仓单变更历史表
-- ============================================================
CREATE TABLE IF NOT EXISTS receipt_change_history (
    id VARCHAR(36) NOT NULL PRIMARY KEY COMMENT '变更记录ID（UUID格式）',
    receipt_id VARCHAR(36) NOT NULL COMMENT '仓单ID',
    receipt_no VARCHAR(64) NOT NULL COMMENT '仓单编号',
    change_type VARCHAR(50) NOT NULL COMMENT '变更类型: PRICE_ADJUSTMENT-价格调整, LOCATION_CHANGE-位置变更, EXPIRY_EXTENSION-有效期延长, INFO_UPDATE-信息更新',
    change_reason TEXT NOT NULL COMMENT '变更原因',
    before_value TEXT COMMENT '变更前值（JSON格式）',
    after_value TEXT COMMENT '变更后值（JSON格式）',
    changed_fields TEXT COMMENT '变更字段列表（JSON数组）',
    operator_id VARCHAR(36) NOT NULL COMMENT '操作人ID',
    operator_name VARCHAR(100) COMMENT '操作人姓名',
    change_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '变更时间',
    remarks TEXT COMMENT '备注',
    INDEX idx_receipt (receipt_id),
    INDEX idx_change_time (change_time),
    INDEX idx_operator (operator_id),
    INDEX idx_receipt_no (receipt_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仓单变更历史表';

-- ============================================================
-- 4. 添加表注释
-- ============================================================
ALTER TABLE receipt_merge_application COMMENT = '仓单合并申请表 - 记录仓单合并申请和处理结果';

ALTER TABLE receipt_change_history COMMENT = '仓单变更历史表 - 记录仓单信息变更的完整历史';
