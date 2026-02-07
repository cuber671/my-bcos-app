-- ============================================
-- 电子仓单表 - 完整详细定义
-- 表名: electronic_warehouse_receipt
-- 版本: 1.0
-- 创建时间: 2026-01-26
-- ============================================

CREATE TABLE IF NOT EXISTS electronic_warehouse_receipt (

    -- ============================================
    -- 一、基础信息字段 (8个)
    -- ============================================

    id VARCHAR(36) NOT NULL COMMENT 'UUID主键，36位字符',

    receipt_no VARCHAR(64) NOT NULL COMMENT '仓单编号（业务主键）|格式: EWR+yyyyMMdd+6位流水号|示例: EWR20260126000001|全局唯一',

    warehouse_id VARCHAR(36) NOT NULL COMMENT '仓储企业ID（UUID）|关联enterprise表|企业角色必须是WAREHOUSE_PROVIDER',

    warehouse_address VARCHAR(42) NOT NULL COMMENT '仓储方区块链地址|42位0x开头|用于区块链交易',

    warehouse_name VARCHAR(255) COMMENT '仓储方名称（冗余字段）|避免频繁JOIN查询|提高查询性能',

    owner_id VARCHAR(36) NOT NULL COMMENT '货主企业ID（UUID）|关联enterprise表|货物所有者',

    owner_address VARCHAR(42) NOT NULL COMMENT '货主区块链地址|42位0x开头|创建后不可变更',

    holder_address VARCHAR(42) NOT NULL COMMENT '持单人地址|42位0x开头|当前仓单持有者|可经背书转让变更|初始值等于owner_address',

    -- ============================================
    -- 二、货物信息字段 (9个)
    -- ============================================

    goods_name VARCHAR(255) NOT NULL COMMENT '货物名称|如: 螺纹钢、动力煤、小麦',

    goods_type VARCHAR(100) COMMENT '货物类型分类|枚举: 钢材、煤炭、粮食、化工、金属、建材、其他|便于统计和查询',

    goods_specification VARCHAR(200) COMMENT '货物规格型号|如: φ12mm、HRB400、I级、Q235|详细规格描述',

    goods_grade VARCHAR(50) COMMENT '货物等级/品质|如: 优等品、一级品、合格品|质检报告等级',

    quantity DECIMAL(20,2) NOT NULL COMMENT '货物数量|最大支持9999999999999999.99|精度2位小数',

    unit VARCHAR(20) NOT NULL COMMENT '计量单位|枚举: 吨、千克、立方米、平方米、件、箱、其他',

    unit_price DECIMAL(20,2) NOT NULL COMMENT '单价（元）|每单位货物的价格|精确到分',

    total_value DECIMAL(20,2) NOT NULL COMMENT '货物总价值（元）|计算公式: quantity × unit_price|用于估值和质押',

    market_price DECIMAL(20,2) COMMENT '市场参考价格（元）|用于评估当前市场价值|可定期更新',

    -- ============================================
    -- 三、仓储信息字段 (7个)
    -- ============================================

    warehouse_location VARCHAR(500) COMMENT '仓库详细地址|省市区+街道+门牌号|如: 北京市朝阳区XX路XX号',

    storage_location VARCHAR(200) COMMENT '存储位置|仓库内部定位|如: A区03栋12排5层货架|便于快速定位',

    storage_date DATETIME(6) NOT NULL COMMENT '入库时间|货物实际入库日期时间|精确到微秒',

    expiry_date DATETIME(6) NOT NULL COMMENT '仓单有效期（预计提货时间）|到期后仓单状态可能变为EXPIRED|默认6个月',

    actual_delivery_date DATETIME(6) COMMENT '实际提货时间|货物被提取的日期时间|状态变为DELIVERED时记录',

    warehouse_conditions TEXT COMMENT '仓储环境条件要求|JSON格式|示例: {"temp":"-18℃","humidity":"60%","ventilation":"良好"}',

    inspection_report TEXT COMMENT '货物检验报告|JSON格式|质检机构、检验结果、检验人等信息',

    -- ============================================
    -- 四、状态管理字段 (3个)
    -- ============================================

    receipt_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '仓单状态|枚举值: DRAFT-草稿|NORMAL-正常|PLEDGED-已质押|TRANSFERRED-已转让|FROZEN-已冻结|EXPIRED-已过期|DELIVERED-已提货|CANCELLED-已取消|默认值: DRAFT',

    parent_receipt_id VARCHAR(36) COMMENT '父仓单ID（UUID）|用于仓单拆分场景|指向拆分前的父仓单|NULL表示非拆分仓单',

    batch_no VARCHAR(64) COMMENT '批次号|同一批入库的货物使用相同批次号|便于批量管理和追溯|格式自定义',

    -- ============================================
    -- 五、融资信息字段 (6个)
    -- ============================================

    is_financed BOOLEAN DEFAULT FALSE COMMENT '是否已融资|true-已融资|false-未融资|默认值: false',

    finance_amount DECIMAL(20,2) COMMENT '融资金额（元）|实际融到的资金|is_financed=true时必填',

    finance_rate INT COMMENT '融资利率（基点）|1基点=0.01%|如: 500基点=5%|is_financed=true时必填',

    finance_date DATETIME(6) COMMENT '融资日期|融资成功的日期时间|is_financed=true时记录',

    financier_address VARCHAR(42) COMMENT '资金方区块链地址|提供融资的金融机构|is_financed=true时必填',

    pledge_contract_no VARCHAR(64) COMMENT '质押合同编号|融资合同的编号|关联融资业务|可为业务编号',

    -- ============================================
    -- 六、背书转让字段 (3个)
    -- ============================================

    endorsement_count INT DEFAULT 0 COMMENT '背书次数|记录仓单被背书转让的次数|每次背书+1|默认值: 0',

    last_endorsement_date DATETIME(6) COMMENT '最后背书时间|最近一次背书转让的时间|NULL表示从未背书',

    endorsement_chain TEXT COMMENT '背书链（JSON数组）|记录所有背书转让历史|完整追溯|格式: [{"from":"","to":"","timestamp":"","tx_hash":"","remarks":""},...]',

    -- ============================================
    -- 七、区块链相关字段 (4个)
    -- ============================================

    tx_hash VARCHAR(66) COMMENT '区块链交易哈希|66位0x开头|上链成功后记录|用于区块链查询',

    blockchain_status VARCHAR(20) DEFAULT 'PENDING' COMMENT '区块链上链状态|枚举: PENDING-待上链|SYNCED-已同步|FAILED-上链失败|VERIFIED-已验证|默认值: PENDING',

    block_number BIGINT COMMENT '区块高度|上链成功后的区块号|用于查询区块链交易',

    blockchain_timestamp DATETIME(6) COMMENT '区块链时间戳|区块链记录的交易时间|与本地时间可能有差异',

    -- ============================================
    -- 八、其他信息字段 (3个)
    -- ============================================

    remarks TEXT COMMENT '备注信息|文本格式的补充说明|如: 特殊注意事项',

    attachments TEXT COMMENT '附件列表（JSON数组）|相关文档、图片等|格式: [{"name":"","url":"","type":"","size":""},...]',

    version INT DEFAULT 0 COMMENT '乐观锁版本号|每次更新+1|防止并发修改冲突|默认值: 0',

    -- ============================================
    -- 九、审计字段 (6个)
    -- ============================================

    created_at DATETIME(6) NOT NULL COMMENT '创建时间|记录创建的日期时间|精确到微秒',

    updated_at DATETIME(6) COMMENT '更新时间|最后一次更新的日期时间|精确到微秒|每次修改自动更新',

    created_by VARCHAR(50) COMMENT '创建人|创建记录的用户名或系统标识|如: admin、SELF_REGISTER、SYSTEM',

    updated_by VARCHAR(50) COMMENT '更新人|最后一次修改的用户名|最后一次修改的操作者',

    deleted_at DATETIME(6) COMMENT '软删除时间|NULL表示未删除|有值表示已删除|可恢复',

    deleted_by VARCHAR(50) COMMENT '删除人|执行软删除的操作者|deleted_at非空时记录',

    -- ============================================
    -- 主键和唯一约束
    -- ============================================

    PRIMARY KEY (id),
    UNIQUE KEY uk_receipt_no (receipt_no),

    -- ============================================
    -- 普通索引
    -- ============================================

    KEY idx_warehouse (warehouse_address) COMMENT '按仓储方查询|查询某仓储方签发的所有仓单',

    KEY idx_owner (owner_address) COMMENT '按货主查询|查询某企业拥有的仓单',

    KEY idx_holder (holder_address) COMMENT '按持单人查询|查询当前持有的仓单|可能经过背书',

    KEY idx_status (receipt_status) COMMENT '按状态查询|如: 查询所有已质押的仓单',

    KEY idx_goods_type (goods_type) COMMENT '按货物类型查询|如: 查询所有钢材类仓单',

    KEY idx_expiry_date (expiry_date) COMMENT '按有效期查询|查询即将到期的仓单|预警提醒',

    KEY idx_storage_date (storage_date) COMMENT '按入库时间查询|按时间范围统计',

    KEY idx_financier (financier_address) COMMENT '按资金方查询|查询某金融机构提供融资的仓单',

    KEY idx_blockchain_status (blockchain_status) COMMENT '按上链状态查询|查询待上链或上链失败的记录',

    KEY idx_created_at (created_at) COMMENT '按创建时间查询|按时间范围筛选和排序',

    KEY idx_deleted_at (deleted_at) COMMENT '软删除查询|查询已删除的记录|用于恢复或审计',

    -- ============================================
    -- 外键约束
    -- ============================================

    CONSTRAINT fk_ewr_warehouse
        FOREIGN KEY (warehouse_id)
        REFERENCES enterprise(id)
        ON DELETE RESTRICT
        COMMENT '仓储方企业外键|禁止删除有仓单的企业',

    CONSTRAINT fk_ewr_owner
        FOREIGN KEY (owner_id)
        REFERENCES enterprise(id)
        ON DELETE RESTRICT
        COMMENT '货主企业外键|禁止删除拥有仓单的企业',

    CONSTRAINT fk_ewr_parent
        FOREIGN KEY (parent_receipt_id)
        REFERENCES electronic_warehouse_receipt(id)
        ON DELETE SET NULL
        COMMENT '父仓单外键|父仓单删除时子仓单的parent_receipt_id置为NULL'

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='电子仓单表-支持全生命周期管理';

-- ============================================
-- CHECK约束（数据有效性验证）
-- ============================================

-- 仓单状态枚举约束
ALTER TABLE electronic_warehouse_receipt
ADD CONSTRAINT chk_ewr_status
CHECK (receipt_status IN ('DRAFT', 'NORMAL', 'PLEDGED', 'TRANSFERRED', 'FROZEN', 'EXPIRED', 'DELIVERED', 'CANCELLED'))
COMMENT '仓单状态必须是预定义的8种状态之一';

-- 区块链状态枚举约束
ALTER TABLE electronic_warehouse_receipt
ADD CONSTRAINT chk_ewr_blockchain_status
CHECK (blockchain_status IN ('PENDING', 'SYNCED', 'FAILED', 'VERIFIED'))
COMMENT '区块链状态必须是预定义的4种状态之一';

-- 数量必须大于0
ALTER TABLE electronic_warehouse_receipt
ADD CONSTRAINT chk_ewr_quantity_positive
CHECK (quantity > 0)
COMMENT '货物数量必须大于0';

-- 总价值必须大于等于0
ALTER TABLE electronic_warehouse_receipt
ADD CONSTRAINT chk_ewr_total_value_non_negative
CHECK (total_value >= 0)
COMMENT '货物总价值必须大于等于0';

-- ============================================
-- 触发器（可选-自动维护审计字段）
-- ============================================

DELIMITER $$

-- 更新时间自动更新触发器
CREATE TRIGGER trg_ewr_update_timestamp
BEFORE UPDATE ON electronic_warehouse_receipt
FOR EACH ROW
BEGIN
    SET NEW.updated_at = NOW(6);
    SET NEW.version = OLD.version + 1;
END$$

-- 软删除时记录删除人
CREATE TRIGGER trg_ewr_soft_delete
BEFORE UPDATE ON electronic_warehouse_receipt
FOR EACH ROW
BEGIN
    IF NEW.deleted_at IS NOT NULL AND OLD.deleted_at IS NULL THEN
        SET NEW.deleted_by = CURRENT_USER();
    END IF;
END$$

DELIMITER ;

-- ============================================
-- 初始化示例数据（可选）
-- ============================================

-- INSERT INTO electronic_warehouse_receipt (
--     id, receipt_no, warehouse_id, warehouse_address, warehouse_name,
--     owner_id, owner_address, holder_address,
--     goods_name, goods_type, goods_specification, goods_grade,
--     quantity, unit, unit_price, total_value,
--     warehouse_location, storage_location, storage_date, expiry_date,
--     receipt_status, created_at
-- ) VALUES (
--     UUID(),
--     'EWR20260126000001',
--     'warehouse-enterprise-id',
--     '0x1234567890abcdef1234567890abcdef12345678',
--     'XX仓储有限公司',
--     'owner-enterprise-id',
--     '0xabcdefabcdefabcdefabcdefabcdefabcdefabcd',
--     '0xabcdefabcdefabcdefabcdefabcdefabcdefabcd',
--     '螺纹钢',
--     '钢材',
--     'φ12mm',
--     'HRB400',
--     1000.00,
--     '吨',
--     4500.00,
--     4500000.00,
--     '上海市浦东新区XX路XX号',
--     'A区03栋12排5层',
--     '2026-01-26 10:00:00.000000',
--     '2026-07-26 23:59:59.000000',
--     'NORMAL',
--     NOW(6)
-- );

-- ============================================
-- 常用查询示例
-- ============================================

-- 1. 查询某企业的所有仓单（作为货主）
-- SELECT * FROM electronic_warehouse_receipt
-- WHERE owner_address = '0x...' AND deleted_at IS NULL;

-- 2. 查询某企业当前持有的仓单
-- SELECT * FROM electronic_warehouse_receipt
-- WHERE holder_address = '0x...' AND receipt_status = 'NORMAL' AND deleted_at IS NULL;

-- 3. 查询已质押的仓单
-- SELECT * FROM electronic_warehouse_receipt
-- WHERE receipt_status = 'PLEDGED' AND deleted_at IS NULL;

-- 4. 查询即将到期的仓单（7天内）
-- SELECT * FROM electronic_warehouse_receipt
-- WHERE expiry_date BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL 7 DAY)
--   AND receipt_status IN ('NORMAL', 'TRANSFERRED')
--   AND deleted_at IS NULL;

-- 5. 查询待上链的仓单
-- SELECT * FROM electronic_warehouse_receipt
-- WHERE blockchain_status = 'PENDING' AND deleted_at IS NULL;

-- 6. 统计各类型货物数量和价值
-- SELECT goods_type,
--        COUNT(*) as count,
--        SUM(quantity) as total_quantity,
--        SUM(total_value) as total_value
-- FROM electronic_warehouse_receipt
-- WHERE deleted_at IS NULL
-- GROUP BY goods_type;

-- 7. 查询某仓储方的业务统计
-- SELECT warehouse_name,
--        COUNT(*) as total_receipts,
--        SUM(CASE WHEN receipt_status = 'NORMAL' THEN 1 ELSE 0 END) as normal_count,
--        SUM(CASE WHEN receipt_status = 'PLEDGED' THEN 1 ELSE 0 END) as pledged_count,
--        SUM(total_value) as total_value
-- FROM electronic_warehouse_receipt
-- WHERE warehouse_address = '0x...' AND deleted_at IS NULL
-- GROUP BY warehouse_name;
