-- 添加新的仓单状态：PENDING_ONCHAIN 和 ONCHAIN_FAILED
-- 修改 ReceiptStatus 枚举类型（PostgreSQL）

-- 1. 添加新的枚举值
ALTER TYPE receipt_status ADD VALUE 'PENDING_ONCHAIN' AFTER 'DRAFT';
ALTER TYPE receipt_status ADD VALUE 'ONCHAIN_FAILED' AFTER 'NORMAL';

-- 2. 更新现有上链失败的仓单状态（如果有的话）
UPDATE electronic_warehouse_receipt
SET receipt_status = 'ONCHAIN_FAILED'
WHERE blockchain_status = 'FAILED'
  AND receipt_status = 'NORMAL';

-- 3. 创建索引以提高查询性能
CREATE INDEX idx_ewr_receipt_status_new
ON electronic_warehouse_receipt(receipt_status);

-- 4. 添加注释
COMMENT ON COLUMN electronic_warehouse_receipt.receipt_status IS '仓单状态: DRAFT-草稿, PENDING_ONCHAIN-待上链, NORMAL-正常, ONCHAIN_FAILED-上链失败, PLEDGED-已质押, TRANSFERRED-已转让, FROZEN-已冻结, EXPIRED-已过期, DELIVERED-已提货, CANCELLED-已取消';
