-- ============================================================
-- 修复电子仓单表的缺失字段和枚举状态
-- 版本: V12
-- 创建时间: 2026-02-01
-- 说明: 添加实体类中存在但数据库表缺失的字段
-- ============================================================

-- 1. 添加缺失的货物信息字段
ALTER TABLE electronic_warehouse_receipt
ADD COLUMN `unit` VARCHAR(20) NOT NULL DEFAULT '吨' COMMENT '计量单位|吨、千克、立方米、平方米、件、箱等' AFTER `goods_name`,
ADD COLUMN `market_price` DECIMAL(20,2) DEFAULT NULL COMMENT '市场参考价格（元）|用于评估当前市场价值，可定期更新' AFTER `total_value`;

-- 2. 添加缺失的提货信息字段（部分字段可能已存在，使用 IF NOT EXISTS 语法）
-- 注意：MySQL 不支持 IF NOT EXISTS，所以先检查字段是否存在
SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = 'bcos_supply_chain'
    AND table_name = 'electronic_warehouse_receipt'
    AND column_name = 'actual_delivery_date'
);

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE electronic_warehouse_receipt ADD COLUMN `actual_delivery_date` DATETIME(6) DEFAULT NULL COMMENT ''实际提货时间|货物被提取的日期时间，状态变为DELIVERED时记录'' AFTER `expiry_date`',
    'SELECT ''Column actual_delivery_date already exists'' AS message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加提货详细信息字段
SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = 'bcos_supply_chain'
    AND table_name = 'electronic_warehouse_receipt'
    AND column_name = 'delivery_person_name'
);

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE electronic_warehouse_receipt
     ADD COLUMN `delivery_person_name` VARCHAR(100) DEFAULT NULL COMMENT ''提货人姓名|实际提取货物的人员姓名'' AFTER `actual_delivery_date`,
     ADD COLUMN `delivery_person_contact` VARCHAR(50) DEFAULT NULL COMMENT ''提货人联系方式|手机号或电话'' AFTER `delivery_person_name`,
     ADD COLUMN `delivery_no` VARCHAR(64) DEFAULT NULL COMMENT ''提货单号|提货凭证编号'' AFTER `delivery_person_contact`,
     ADD COLUMN `vehicle_plate` VARCHAR(20) DEFAULT NULL COMMENT ''运输车牌号|提货车辆车牌号'' AFTER `delivery_no`,
     ADD COLUMN `driver_name` VARCHAR(100) DEFAULT NULL COMMENT ''司机姓名|驾驶员姓名'' AFTER `vehicle_plate`',
    'SELECT ''Delivery detail columns already exist'' AS message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 修改 receipt_status 枚举约束（删除旧约束，创建新约束）
ALTER TABLE electronic_warehouse_receipt
DROP CONSTRAINT chk_ewr_status;

ALTER TABLE electronic_warehouse_receipt
ADD CONSTRAINT `chk_ewr_status`
CHECK (
    `receipt_status` IN (
        'DRAFT',            -- 草稿
        'PENDING_ONCHAIN',  -- 待上链（新增）
        'NORMAL',           -- 正常
        'ONCHAIN_FAILED',   -- 上链失败（新增）
        'PLEDGED',          -- 已质押
        'TRANSFERRED',      -- 已转让
        'FROZEN',           -- 已冻结
        'EXPIRED',          -- 已过期
        'DELIVERED',        -- 已提货
        'CANCELLED'         -- 已取消
    )
);

-- 4. 添加提货信息索引（如果不存在）
SET @index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = 'bcos_supply_chain'
    AND table_name = 'electronic_warehouse_receipt'
    AND index_name = 'idx_delivery_no'
);

SET @sql = IF(@index_exists = 0,
    'CREATE INDEX idx_delivery_no ON electronic_warehouse_receipt(delivery_no) COMMENT ''按提货单号查询''',
    'SELECT ''Index idx_delivery_no already exists'' AS message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 5. 更新表注释
ALTER TABLE electronic_warehouse_receipt
COMMENT = '电子仓单主表-支持全生命周期管理-包含新增的PENDING_ONCHAIN和ONCHAIN_FAILED状态';

-- ============================================================
-- 验证脚本（可选）
-- ============================================================
-- 查看所有字段
-- SELECT COLUMN_NAME, DATA_TYPE, COLUMN_COMMENT
-- FROM information_schema.columns
-- WHERE table_schema = 'bcos_supply_chain'
-- AND table_name = 'electronic_warehouse_receipt'
-- ORDER BY ORDINAL_POSITION;

-- 查看枚举约束
-- SELECT CONSTRAINT_NAME, CHECK_CLAUSE
-- FROM information_schema.check_constraints
-- WHERE CONSTRAINT_SCHEMA = 'bcos_supply_chain'
-- AND CONSTRAINT_NAME = 'chk_ewr_status';
