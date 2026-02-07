-- 应收账款拆分合并功能表
-- 创建时间: 2026-02-03

-- 添加应收账款拆分合并相关状态
ALTER TABLE `receivable` MODIFY COLUMN `status` VARCHAR(20) COMMENT '状态: DRAFT-草稿, PENDING_CONFIRMATION-待确认, CONFIRMED-已确认, FINANCING-融资中, FINANCED-已融资, PAID-已付款, OVERDUE-逾期, WRITE_OFF-已核销, CANCELLED-已取消, SPLITTING-拆分中, SPLIT-已拆分, MERGING-合并中, MERGED-已合并';

-- 添加应收账款拆分合并相关字段
ALTER TABLE `receivable`
ADD COLUMN `parent_receivable_id` VARCHAR(36) DEFAULT NULL COMMENT '父应收账款ID|拆分或合并后的父应收账款|用于追溯原始应收账款' AFTER `status`,
ADD COLUMN `split_count` INT DEFAULT NULL COMMENT '拆分数量|拆分后的子应收账款数量|只有已拆分的应收账款有值' AFTER `parent_receivable_id`,
ADD COLUMN `merge_count` INT DEFAULT NULL COMMENT '合并数量|合并前的应收账款数量|只有合并后的应收账款有值' AFTER `split_count`,
ADD COLUMN `split_time` DATETIME(6) DEFAULT NULL COMMENT '拆分时间|应收账款被拆分的时间|状态变为SPLIT时记录' AFTER `merge_count`,
ADD COLUMN `merge_time` DATETIME(6) DEFAULT NULL COMMENT '合并时间|应收账款被合并的时间|状态变为MERGED时记录' AFTER `split_time`;

-- 添加索引
CREATE INDEX `idx_parent_receivable` ON `receivable` (`parent_receivable_id`) COMMENT '按父应收账款查询';
CREATE INDEX `idx_split_time` ON `receivable` (`split_time`) COMMENT '按拆分时间查询';
CREATE INDEX `idx_merge_time` ON `receivable` (`merge_time`) COMMENT '按合并时间查询';

-- 应收账款拆分申请表
CREATE TABLE receivable_split_application (
    id VARCHAR(36) PRIMARY KEY,
    receivable_id VARCHAR(36) NOT NULL COMMENT '原应收账款ID',
    applicant_id VARCHAR(36) NOT NULL COMMENT '申请人ID（供应商）',
    split_scheme VARCHAR(20) NOT NULL COMMENT '拆分方案: EQUAL-等额, CUSTOM-自定义',
    split_count INT NOT NULL COMMENT '拆分数量',
    split_details TEXT NOT NULL COMMENT '拆分明细（JSON格式）|包含每个子应收账款的信息',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING-待审核, APPROVED-已通过, REJECTED-已拒绝',
    approver_id VARCHAR(36) COMMENT '审核人ID',
    approval_time DATETIME(6) COMMENT '审核时间',
    rejection_reason TEXT COMMENT '拒绝原因',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    INDEX idx_receivable (receivable_id),
    INDEX idx_applicant (applicant_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应收账款拆分申请表';

-- 应收账款合并申请表
CREATE TABLE receivable_merge_application (
    id VARCHAR(36) PRIMARY KEY,
    source_receivable_ids TEXT NOT NULL COMMENT '源应收账款ID列表（JSON数组）',
    target_receivable_id VARCHAR(36) COMMENT '合并后的应收账款ID（审核通过后生成）',
    applicant_id VARCHAR(36) NOT NULL COMMENT '申请人ID（供应商）',
    merge_type VARCHAR(20) NOT NULL COMMENT '合并类型: AMOUNT-金额合并, PERIOD-期限合并, FULL-完全合并',
    total_amount BIGINT NOT NULL COMMENT '合并后总金额（分）',
    merge_details TEXT NOT NULL COMMENT '合并明细（JSON格式）',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING-待审核, APPROVED-已通过, REJECTED-已拒绝',
    approver_id VARCHAR(36) COMMENT '审核人ID',
    approval_time DATETIME(6) COMMENT '审核时间',
    rejection_reason TEXT COMMENT '拒绝原因',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    INDEX idx_applicant (applicant_id),
    INDEX idx_target_receivable (target_receivable_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应收账款合并申请表';
