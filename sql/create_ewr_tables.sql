-- ============================================================
-- 电子仓单表 - 完整DDL脚本
-- 版本: 1.0
-- 创建时间: 2026-02-01
-- 说明: 根据实体类 ElectronicWarehouseReceipt.java 创建
-- ============================================================

-- 删除表（如果存在）
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS ewr_endorsement_chain;
DROP TABLE IF EXISTS electronic_warehouse_receipt;
SET FOREIGN_KEY_CHECKS = 1;

-- 创建电子仓单主表
CREATE TABLE `electronic_warehouse_receipt` (
    -- ==================== 基础信息 (8个字段) ====================
    `id` VARCHAR(36) NOT NULL COMMENT 'UUID主键',
    `receipt_no` VARCHAR(64) NOT NULL COMMENT '仓单编号: EWR+yyyyMMdd+6位流水号',
    `warehouse_id` VARCHAR(36) NOT NULL COMMENT '仓储企业ID（UUID）',
    `warehouse_address` VARCHAR(42) NOT NULL COMMENT '仓储方区块链地址|42位0x开头',
    `warehouse_name` VARCHAR(255) DEFAULT NULL COMMENT '仓储方名称（冗余字段）|避免频繁JOIN',
    `owner_id` VARCHAR(36) NOT NULL COMMENT '货主企业ID（UUID）',
    `owner_address` VARCHAR(42) NOT NULL COMMENT '货主区块链地址|42位0x开头|创建后不可变更',
    `holder_address` VARCHAR(42) NOT NULL COMMENT '持单人地址|42位0x开头|可经背书转让|初始值等于owner_address',

    -- ==================== 货物信息 (6个字段) ====================
    `goods_name` VARCHAR(255) NOT NULL COMMENT '货物名称',
    `unit` VARCHAR(20) NOT NULL DEFAULT '吨' COMMENT '计量单位|吨、千克、立方米、平方米、件、箱等',
    `quantity` DECIMAL(20,2) NOT NULL COMMENT '货物数量|必须大于0',
    `unit_price` DECIMAL(20,2) NOT NULL COMMENT '单价（元）|精确到分',
    `total_value` DECIMAL(20,2) NOT NULL COMMENT '货物总价值（元）|计算公式: quantity × unit_price',
    `market_price` DECIMAL(20,2) DEFAULT NULL COMMENT '市场参考价格（元）|用于评估当前市场价值，可定期更新',

    -- ==================== 仓储信息 (6个字段) ====================
    `warehouse_location` VARCHAR(500) DEFAULT NULL COMMENT '仓库详细地址|省市区+街道+门牌号',
    `storage_location` VARCHAR(200) DEFAULT NULL COMMENT '存储位置|仓库内部定位|如: A区03栋12排5层货架',
    `storage_date` DATETIME(6) NOT NULL COMMENT '入库时间|精确到微秒',
    `expiry_date` DATETIME(6) NOT NULL COMMENT '仓单有效期（预计提货时间）|到期后状态可能变为EXPIRED',

    -- ==================== 提货信息 (6个字段) ====================
    `actual_delivery_date` DATETIME(6) DEFAULT NULL COMMENT '实际提货时间|货物被提取的日期时间，状态变为DELIVERED时记录',
    `delivery_person_name` VARCHAR(100) DEFAULT NULL COMMENT '提货人姓名|实际提取货物的人员姓名',
    `delivery_person_contact` VARCHAR(50) DEFAULT NULL COMMENT '提货人联系方式|手机号或电话',
    `delivery_no` VARCHAR(64) DEFAULT NULL COMMENT '提货单号|提货凭证编号',
    `vehicle_plate` VARCHAR(20) DEFAULT NULL COMMENT '运输车牌号|提货车辆车牌号',
    `driver_name` VARCHAR(100) DEFAULT NULL COMMENT '司机姓名|驾驶员姓名',

    -- ==================== 状态管理 (3个字段) ====================
    `receipt_status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '仓单状态|DRAFT-草稿,PENDING_ONCHAIN-待上链,NORMAL-正常,ONCHAIN_FAILED-上链失败,PLEDGED-已质押,TRANSFERRED-已转让,FROZEN-已冻结,EXPIRED-已过期,DELIVERED-已提货,CANCELLED-已取消',
    `parent_receipt_id` VARCHAR(36) DEFAULT NULL COMMENT '父仓单ID（UUID）|用于仓单拆分场景|NULL表示非拆分仓单',
    `batch_no` VARCHAR(64) DEFAULT NULL COMMENT '批次号|同一批入库的货物使用相同批次号',

    -- ==================== 企业和操作人 (5个字段) ====================
    `owner_name` VARCHAR(255) DEFAULT NULL COMMENT '货主企业名称（冗余字段）|避免频繁JOIN查询',
    `owner_operator_id` VARCHAR(36) DEFAULT NULL COMMENT '货主企业操作人ID（UUID）|关联user表|货主企业中执行存入操作的员工',
    `owner_operator_name` VARCHAR(100) DEFAULT NULL COMMENT '货主企业操作人姓名|如: 张三（货主企业业务员）',
    `warehouse_operator_id` VARCHAR(36) DEFAULT NULL COMMENT '仓储方操作人ID（UUID）|关联user表|仓储方中执行入库操作的员工',
    `warehouse_operator_name` VARCHAR(100) DEFAULT NULL COMMENT '仓储方操作人姓名|如: 李四（仓储方仓库管理员）',

    -- ==================== 融资信息 (6个字段) ====================
    `is_financed` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已融资|1-已融资|0-未融资',
    `finance_amount` DECIMAL(20,2) DEFAULT NULL COMMENT '融资金额（元）|实际融到的资金|is_financed=1时必填',
    `finance_rate` INT DEFAULT NULL COMMENT '融资利率（基点）|1基点=0.01%|如: 500基点=5%|is_financed=1时必填',
    `finance_date` DATETIME(6) DEFAULT NULL COMMENT '融资日期|融资成功的日期时间|is_financed=1时记录',
    `financier_address` VARCHAR(42) DEFAULT NULL COMMENT '资金方区块链地址|提供融资的金融机构|is_financed=1时必填',
    `pledge_contract_no` VARCHAR(64) DEFAULT NULL COMMENT '质押合同编号|融资合同的编号|关联融资业务',

    -- ==================== 背书统计 (3个字段) ====================
    `endorsement_count` INT NOT NULL DEFAULT 0 COMMENT '背书次数|记录仓单被背书转让的次数|每次背书+1',
    `last_endorsement_date` DATETIME(6) DEFAULT NULL COMMENT '最后背书时间|最近一次背书转让的时间|NULL表示从未背书',
    `current_holder` VARCHAR(42) DEFAULT NULL COMMENT '当前持单人冗余字段|便于快速查询当前持单人名称',

    -- ==================== 区块链 (4个字段) ====================
    `tx_hash` VARCHAR(66) DEFAULT NULL COMMENT '区块链交易哈希|66位0x开头|上链成功后记录|用于区块链查询',
    `blockchain_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '区块链上链状态|PENDING-待上链,SYNCED-已同步,FAILED-上链失败,VERIFIED-已验证',
    `block_number` BIGINT DEFAULT NULL COMMENT '区块高度|上链成功后的区块号|用于查询区块链交易',
    `blockchain_timestamp` DATETIME(6) DEFAULT NULL COMMENT '区块链时间戳|区块链记录的交易时间|与本地时间可能有差异',

    -- ==================== 其他 (1个字段) ====================
    `remarks` TEXT COMMENT '备注信息|文本格式的补充说明',

    -- ==================== 审计 (6个字段) ====================
    `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间|精确到微秒',
    `updated_at` DATETIME(6) DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间|精确到微秒|每次修改自动更新',
    `created_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人|创建记录的用户名或系统标识|如: admin、SELF_REGISTER、SYSTEM',
    `updated_by` VARCHAR(50) DEFAULT NULL COMMENT '更新人|最后一次修改的操作者',
    `deleted_at` DATETIME(6) DEFAULT NULL COMMENT '软删除时间|NULL表示未删除|有值表示已删除|可恢复',
    `deleted_by` VARCHAR(50) DEFAULT NULL COMMENT '删除人|执行软删除的操作者|deleted_at非空时记录',

    -- ==================== 主键和唯一键 ====================
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_receipt_no` (`receipt_no`),

    -- ==================== 索引 ====================
    KEY `idx_warehouse` (`warehouse_address`) COMMENT '按仓储方查询|查询某仓储方签发的所有仓单',
    KEY `idx_owner` (`owner_address`) COMMENT '按货主查询|查询某企业拥有的仓单',
    KEY `idx_holder` (`holder_address`) COMMENT '按持单人查询|查询当前持有的仓单|可能经过背书',
    KEY `idx_status` (`receipt_status`) COMMENT '按状态查询|如: 查询所有已质押的仓单',
    KEY `idx_expiry_date` (`expiry_date`) COMMENT '按有效期查询|查询即将到期的仓单|预警提醒',
    KEY `idx_storage_date` (`storage_date`) COMMENT '按入库时间查询|按时间范围统计',
    KEY `idx_financier` (`financier_address`) COMMENT '按资金方查询|查询某金融机构提供融资的仓单',
    KEY `idx_blockchain_status` (`blockchain_status`) COMMENT '按上链状态查询|查询待上链或上链失败的记录',
    KEY `idx_created_at` (`created_at`) COMMENT '按创建时间查询|按时间范围筛选和排序',
    KEY `idx_deleted_at` (`deleted_at`) COMMENT '软删除查询|查询已删除的记录|用于恢复或审计',
    KEY `idx_owner_operator` (`owner_operator_id`) COMMENT '按货主企业操作人查询|查询某货主企业员工操作的所有仓单',
    KEY `idx_warehouse_operator` (`warehouse_operator_id`) COMMENT '按仓储方操作人查询|查询某仓储方员工操作的所有仓单',
    KEY `idx_delivery_no` (`delivery_no`) COMMENT '按提货单号查询',
    KEY `fk_ewr_warehouse` (`warehouse_id`),
    KEY `fk_ewr_owner` (`owner_id`),
    KEY `fk_ewr_parent` (`parent_receipt_id`),

    -- ==================== 外键约束 ====================
    CONSTRAINT `fk_ewr_owner` FOREIGN KEY (`owner_id`) REFERENCES `enterprise` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_ewr_warehouse` FOREIGN KEY (`warehouse_id`) REFERENCES `enterprise` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_ewr_parent` FOREIGN KEY (`parent_receipt_id`) REFERENCES `electronic_warehouse_receipt` (`id`) ON DELETE SET NULL,

    -- ==================== 检查约束 ====================
    CONSTRAINT `chk_ewr_quantity_positive` CHECK (`quantity` > 0),
    CONSTRAINT `chk_ewr_unit_price_positive` CHECK (`unit_price` > 0),
    CONSTRAINT `chk_ewr_total_value_positive` CHECK (`total_value` >= 0),
    CONSTRAINT `chk_ewr_endorsement_count_positive` CHECK (`endorsement_count` >= 0),
    CONSTRAINT `chk_ewr_market_price_positive` CHECK (`market_price` IS NULL OR `market_price` >= 0),
    CONSTRAINT `chk_ewr_blockchain_status` CHECK (`blockchain_status` IN ('PENDING', 'SYNCED', 'FAILED', 'VERIFIED')),
    CONSTRAINT `chk_ewr_receipt_status` CHECK (`receipt_status` IN (
        'DRAFT',             -- 草稿
        'PENDING_ONCHAIN',   -- 待上链
        'NORMAL',            -- 正常
        'ONCHAIN_FAILED',    -- 上链失败
        'PLEDGED',           -- 已质押
        'TRANSFERRED',       -- 已转让
        'FROZEN',            -- 已冻结
        'EXPIRED',           -- 已过期
        'DELIVERED',         -- 已提货
        'CANCELLED'          -- 已取消
    ))

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='电子仓单主表-支持全生命周期管理';

-- ============================================================
-- 创建背书链表（如果需要）
-- ============================================================
CREATE TABLE `ewr_endorsement_chain` (
    `id` VARCHAR(36) NOT NULL COMMENT 'UUID主键',
    `receipt_id` VARCHAR(36) NOT NULL COMMENT '仓单ID（UUID）|关联electronic_warehouse_receipt表',
    `receipt_no` VARCHAR(64) NOT NULL COMMENT '仓单编号（冗余字段）|便于查询|如: EWR20260126000001',
    `endorsement_no` VARCHAR(64) NOT NULL COMMENT '背书编号: END+yyyyMMdd+6位流水号|全局唯一',
    `endorse_from` VARCHAR(42) NOT NULL COMMENT '背书企业地址（转出方）|42位0x开头',
    `endorse_from_name` VARCHAR(255) DEFAULT NULL COMMENT '背书企业名称（冗余字段）|避免频繁JOIN查询',
    `endorse_to` VARCHAR(42) NOT NULL COMMENT '被背书企业地址（转入方）|42位0x开头',
    `endorse_to_name` VARCHAR(255) DEFAULT NULL COMMENT '被背书企业名称（冗余字段）|避免频繁JOIN查询',
    `operator_from_id` VARCHAR(36) DEFAULT NULL COMMENT '转出方经手人ID（UUID）|关联user表|实际操作的员工',
    `operator_from_name` VARCHAR(100) DEFAULT NULL COMMENT '转出方经手人姓名|冗余字段|如: 张三',
    `operator_to_id` VARCHAR(36) DEFAULT NULL COMMENT '转入方经手人ID（UUID）|关联user表|确认背书的员工',
    `operator_to_name` VARCHAR(100) DEFAULT NULL COMMENT '转入方经手人姓名|冗余字段|如: 李四',
    `endorsement_type` VARCHAR(20) NOT NULL DEFAULT 'TRANSFER' COMMENT '背书类型|TRANSFER-转让,PLEDGE-质押,RELEASE-解押,CANCEL-撤销',
    `endorsement_reason` VARCHAR(500) DEFAULT NULL COMMENT '背书原因说明|如: 货物转让、融资质押等',
    `goods_snapshot` TEXT COMMENT '背书时的货物信息快照（JSON格式）|防止后续修改造成追溯困难',
    `transfer_price` DECIMAL(20,2) DEFAULT NULL COMMENT '转让价格（元）|如有偿转让时的单价',
    `transfer_amount` DECIMAL(20,2) DEFAULT NULL COMMENT '转让金额|实际交易金额|如: transfer_price × quantity',
    `tx_hash` VARCHAR(66) DEFAULT NULL COMMENT '背书交易哈希|66位0x开头|区块链背书记录的交易哈希值',
    `block_number` BIGINT DEFAULT NULL COMMENT '区块高度|上链成功后的区块号|用于查询区块链交易',
    `blockchain_timestamp` DATETIME(6) DEFAULT NULL COMMENT '区块链时间戳|区块链记录的交易时间|可能与本地时间有差异',
    `endorsement_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '背书状态|PENDING-待确认,CONFIRMED-已确认,CANCELLED-已撤销',
    `remarks` TEXT COMMENT '备注信息|文本格式的补充说明',
    `endorsement_time` DATETIME(6) NOT NULL COMMENT '背书发起时间|背书请求创建的时间|精确到微秒',
    `confirmed_time` DATETIME(6) DEFAULT NULL COMMENT '确认时间|背书被确认的时间|endorsement_status=CONFIRMED时记录',
    `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间|精确到微秒',
    `updated_at` DATETIME(6) DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间|精确到微秒|每次修改自动更新',
    `created_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人|创建记录的用户名或系统标识|如: admin、SYSTEM、AUTO_ENDORSEMENT',
    `updated_by` VARCHAR(50) DEFAULT NULL COMMENT '更新人|最后一次修改的操作者',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_endorsement_no` (`endorsement_no`),
    KEY `idx_receipt_id` (`receipt_id`) COMMENT '按仓单ID查询背书记录',
    KEY `fk_endorsement_receipt` (`receipt_id`),
    CONSTRAINT `fk_endorsement_receipt` FOREIGN KEY (`receipt_id`) REFERENCES `electronic_warehouse_receipt` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='电子仓单背书链表-记录所有背书转让历史';

-- ============================================================
-- 验证脚本（执行后运行此脚本验证）
-- ============================================================
-- 查看表结构
-- DESCRIBE electronic_warehouse_receipt;

-- 查看所有字段
-- SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT, COLUMN_COMMENT
-- FROM information_schema.columns
-- WHERE table_schema = 'bcos_supply_chain'
-- AND table_name = 'electronic_warehouse_receipt'
-- ORDER BY ORDINAL_POSITION;

-- 验证枚举约束
-- SELECT CONSTRAINT_NAME, CHECK_CLAUSE
-- FROM information_schema.check_constraints
-- WHERE CONSTRAINT_SCHEMA = 'bcos_supply_chain'
-- AND TABLE_NAME = 'electronic_warehouse_receipt';

-- 验证新字段是否存在
-- SELECT COUNT(*) as new_fields_count
-- FROM information_schema.columns
-- WHERE table_schema = 'bcos_supply_chain'
-- AND table_name = 'electronic_warehouse_receipt'
-- AND COLUMN_NAME IN ('unit', 'market_price', 'actual_delivery_date', 'delivery_person_name', 'delivery_no', 'vehicle_plate');
