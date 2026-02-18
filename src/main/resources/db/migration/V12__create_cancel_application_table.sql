-- =====================================================
-- 仓单作废申请表
-- =====================================================

CREATE TABLE IF NOT EXISTS `receipt_cancel_application` (
    `id` VARCHAR(36) NOT NULL COMMENT '申请ID（UUID格式）',
    `receipt_id` VARCHAR(36) NOT NULL COMMENT '仓单ID',
    `cancel_reason` TEXT NOT NULL COMMENT '作废原因',
    `cancel_type` VARCHAR(50) NOT NULL COMMENT '作废类型：QUALITY_ISSUE-质量问题, DAMAGED-货物损坏, WRONG_INFO-信息错误, LEGAL_DISPUTE-法律纠纷, VOLUNTARY-主动申请',
    `evidence` TEXT COMMENT '证明材料',
    `reference_no` VARCHAR(100) COMMENT '参考编号（如法律文书号）',
    `request_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '申请状态：PENDING-待审核, APPROVED-已批准, REJECTED-已拒绝, CANCELLED-已取消',
    `applicant_id` VARCHAR(36) COMMENT '申请人ID',
    `applicant_name` VARCHAR(100) COMMENT '申请人姓名',
    `reviewer_id` VARCHAR(36) COMMENT '审核人ID',
    `reviewer_name` VARCHAR(100) COMMENT '审核人姓名',
    `review_time` DATETIME(6) COMMENT '审核时间',
    `review_comments` TEXT COMMENT '审核意见',
    `cancel_tx_hash` VARCHAR(128) COMMENT '作废上链交易哈希',
    `block_number` BIGINT COMMENT '区块号',
    `remarks` TEXT COMMENT '备注',
    `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '申请时间',
    `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_receipt` (`receipt_id`),
    INDEX `idx_status` (`request_status`),
    INDEX `idx_created_at` (`created_at`),
    INDEX `idx_applicant` (`applicant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='仓单作废申请表';

-- =====================================================
-- 添加作废相关字段到电子仓单表
-- =====================================================

ALTER TABLE `electronic_warehouse_receipt`
ADD COLUMN IF NOT EXISTS `cancel_reason` TEXT COMMENT '作废原因' AFTER `split_count`,
ADD COLUMN IF NOT EXISTS `cancel_type` VARCHAR(50) COMMENT '作废类型' AFTER `cancel_reason`,
ADD COLUMN IF NOT EXISTS `cancel_time` DATETIME(6) COMMENT '作废时间' AFTER `cancel_type`,
ADD COLUMN IF NOT EXISTS `cancelled_by` VARCHAR(36) COMMENT '作废操作人ID' AFTER `cancel_time`,
ADD COLUMN IF NOT EXISTS `reference_no` VARCHAR(100) COMMENT '参考编号（如法律文书号）' AFTER `cancelled_by`;

-- =====================================================
-- 更新仓单状态枚举约束，添加CANCELLING状态
-- =====================================================

ALTER TABLE `electronic_warehouse_receipt`
DROP CONSTRAINT IF EXISTS `chk_ewr_receipt_status`;

ALTER TABLE `electronic_warehouse_receipt`
ADD CONSTRAINT `chk_ewr_receipt_status` CHECK (
    `receipt_status` IN (
        'DRAFT', 'PENDING_ONCHAIN', 'NORMAL', 'ONCHAIN_FAILED',
        'PLEDGED', 'TRANSFERRED', 'FROZEN', 'SPLITTING', 'SPLIT',
        'CANCELLING', 'CANCELLED',
        'EXPIRED', 'DELIVERED'
    )
);
