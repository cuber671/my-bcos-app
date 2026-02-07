-- ============================================================
-- 电子仓单表 - 添加操作人字段和实用字段
-- 版本: V10
-- 创建时间: 2026-01-27
-- 说明: 添加实体类中存在但数据库表缺失的字段
-- ============================================================

-- 添加操作人相关字段
ALTER TABLE electronic_warehouse_receipt
ADD COLUMN owner_operator_id VARCHAR(36) COMMENT '货主企业操作人ID（UUID）|关联user表|货主企业的业务操作员',
ADD COLUMN owner_operator_name VARCHAR(100) COMMENT '货主企业操作人姓名|冗余字段|便于快速展示',
ADD COLUMN warehouse_operator_id VARCHAR(36) COMMENT '仓储方操作人ID（UUID）|关联user表|仓储企业的业务操作员',
ADD COLUMN warehouse_operator_name VARCHAR(100) COMMENT '仓储方操作人姓名|冗余字段|便于快速展示';

-- 添加索引
ALTER TABLE electronic_warehouse_receipt
ADD INDEX idx_owner_operator (owner_operator_id) COMMENT '按货主操作人查询|查询某操作员处理的仓单',
ADD INDEX idx_warehouse_operator (warehouse_operator_id) COMMENT '按仓储操作人查询|查询某操作员处理的仓单';

-- ============================================================
-- 说明：已存在的实体字段映射说明
-- ============================================================
-- current_holder VARCHAR(42) - 当前持单人名称（冗余）
--   对应实体类中的 currentHolder 字段
--   用于快速查询当前持单人，避免频繁关联查询
