-- ============================================================
-- 仓单拆分功能 - 添加SPLITTING和SPLIT状态
-- 版本: V11
-- 创建时间: 2026-02-02
-- 说明: 为支持仓单拆分功能，在receipt_status枚举中添加两个新状态
-- ============================================================

-- 由于表中可能已有数据，我们需要先删除旧的检查约束，然后添加新的约束
-- 如果表中没有数据，可以直接修改

-- 第1步：删除旧的receipt_status检查约束
ALTER TABLE `electronic_warehouse_receipt` DROP CONSTRAINT IF EXISTS `chk_ewr_receipt_status`;

-- 第2步：添加新的receipt_status检查约束（包含SPLITTING和SPLIT状态）
ALTER TABLE `electronic_warehouse_receipt` ADD CONSTRAINT `chk_ewr_receipt_status` CHECK (
    `receipt_status` IN (
        'DRAFT',             -- 草稿
        'PENDING_ONCHAIN',   -- 待上链
        'NORMAL',            -- 正常
        'ONCHAIN_FAILED',    -- 上链失败
        'PLEDGED',           -- 已质押
        'TRANSFERRED',       -- 已转让
        'FROZEN',            -- 已冻结
        'SPLITTING',         -- 拆分中 ✨ 新增
        'SPLIT',             -- 已拆分 ✨ 新增
        'EXPIRED',           -- 已过期
        'DELIVERED',         -- 已提货
        'CANCELLED'          -- 已取消
    )
);

-- 第3步：添加split_time字段（记录拆分时间）
ALTER TABLE `electronic_warehouse_receipt`
ADD COLUMN `split_time` DATETIME(6) DEFAULT NULL COMMENT '拆分时间|仓单被拆分的时间|状态变为SPLIT时记录' AFTER `remarks`;

-- 第4步：添加split_count字段（记录子仓单数量）
ALTER TABLE `electronic_warehouse_receipt`
ADD COLUMN `split_count` INT DEFAULT NULL COMMENT '子仓单数量|拆分后的子仓单数量|只有已拆分的仓单有值' AFTER `split_time`;

-- 第5步：为拆分相关字段添加索引
CREATE INDEX `idx_split_time` ON `electronic_warehouse_receipt` (`split_time`) COMMENT '按拆分时间查询';
CREATE INDEX `idx_parent_receipt` ON `electronic_warehouse_receipt` (`parent_receipt_id`) COMMENT '按父仓单查询子仓单';

-- 第6步：添加split_count检查约束（必须大于0）
ALTER TABLE `electronic_warehouse_receipt`
ADD CONSTRAINT `chk_ewr_split_count_positive` CHECK (
    `split_count` IS NULL OR `split_count` >= 2
) COMMENT '如果已拆分，至少要有2个子仓单';

-- ============================================================
-- 验证脚本
-- ============================================================

-- 验证新约束是否添加成功
SELECT
    CONSTRAINT_NAME,
    CONSTRAINT_TYPE,
    CHECK_CLAUSE
FROM
    INFORMATION_SCHEMA.CHECK_CONSTRAINTS
WHERE
    TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'electronic_warehouse_receipt'
    AND CONSTRAINT_NAME = 'chk_ewr_receipt_status';

-- 验证新字段是否添加成功
DESCRIBE `electronic_warehouse_receipt`;

-- ============================================================
-- 说明
-- ============================================================

/*
状态流转说明：

1. 拆分申请提交：
   NORMAL → SPLITTING

2. 拆分审核通过：
   父仓单：SPLITTING → SPLIT
   子仓单：SPLITTING → NORMAL

3. 拆分审核拒绝：
   SPLITTING → NORMAL

拆分相关字段说明：

- split_time: 记录仓单被拆分的时间
- split_count: 记录拆分后的子仓单数量（至少为2）
- parent_receipt_id: 已存在，用于关联父仓单（子仓单使用）

使用示例：

1. 查询已拆分的仓单：
   SELECT * FROM electronic_warehouse_receipt WHERE receipt_status = 'SPLIT';

2. 查询某个父仓单的所有子仓单：
   SELECT * FROM electronic_warehouse_receipt WHERE parent_receipt_id = 'xxx';

3. 查询正在拆分中的仓单：
   SELECT * FROM electronic_warehouse_receipt WHERE receipt_status = 'SPLITTING';
*/
