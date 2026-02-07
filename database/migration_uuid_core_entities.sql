-- ====================================================================
-- 核心业务实体UUID迁移脚本
-- 将主键从BIGINT自增改为UUID，并删除冗余的业务ID字段
-- ====================================================================
-- 执行前注意事项：
-- 1. 在测试环境先验证
-- 2. 备份所有相关表
-- 3. 在低峰期执行
-- 4. 准备回滚方案
-- ====================================================================

-- ====================================================================
-- 第0阶段：系统管理表
-- ====================================================================

-- ====================================================================
-- 0. User（用户）表迁移
-- ====================================================================

-- 步骤1: 备份表
CREATE TABLE IF NOT EXISTS user_backup AS SELECT * FROM user;

-- 步骤2: 添加新的UUID列
ALTER TABLE user ADD COLUMN new_id VARCHAR(36);

-- 步骤3: 生成UUID并填充新列
UPDATE user SET new_id = UUID() WHERE new_id IS NULL;

-- 步骤4: 删除旧主键约束
ALTER TABLE user DROP PRIMARY KEY;

-- 步骤5: 设置新主键
ALTER TABLE user ADD PRIMARY KEY (new_id);

-- 步骤6: 重命名列（保留旧ID作为old_id备份）
ALTER TABLE user CHANGE COLUMN id old_id BIGINT;
ALTER TABLE user CHANGE COLUMN new_id id VARCHAR(36);

-- 步骤7: 删除old_id列（确认无误后执行）
-- ALTER TABLE user DROP COLUMN old_id;


-- ====================================================================
-- 第一阶段：核心资产表（按顺序执行）
-- ====================================================================

-- ====================================================================
-- 1. Bill（票据）表迁移
-- ====================================================================

-- 步骤1: 备份表
CREATE TABLE IF NOT EXISTS bill_backup AS SELECT * FROM bill;

-- 步骤2: 添加新的UUID列
ALTER TABLE bill ADD COLUMN new_id VARCHAR(36);

-- 步骤3: 生成UUID并填充新列
UPDATE bill SET new_id = UUID() WHERE new_id IS NULL;

-- 步骤4: 删除旧主键约束
ALTER TABLE bill DROP PRIMARY KEY;

-- 步骤5: 设置新主键
ALTER TABLE bill ADD PRIMARY KEY (new_id);

-- 步骤6: 重命名列（保留旧ID作为old_id备份）
ALTER TABLE bill CHANGE COLUMN id old_id BIGINT;
ALTER TABLE bill CHANGE COLUMN new_id id VARCHAR(36);

-- 步骤7: 删除bill_id列（冗余业务ID）
-- 注意：如果有外键引用bill_id，需要先处理外键关系
ALTER TABLE bill DROP COLUMN bill_id;

-- 步骤8: 删除old_id列（确认无误后执行）
-- ALTER TABLE bill DROP COLUMN old_id;

-- 步骤9: 修改索引（如果有idx_bill_id索引需要删除）
-- DROP INDEX idx_bill_id ON bill;


-- ====================================================================
-- 2. WarehouseReceipt（仓单）表迁移
-- ====================================================================

-- 步骤1: 备份表
CREATE TABLE IF NOT EXISTS warehouse_receipt_backup AS SELECT * FROM warehouse_receipt;

-- 步骤2: 添加新的UUID列
ALTER TABLE warehouse_receipt ADD COLUMN new_id VARCHAR(36);

-- 步骤3: 生成UUID并填充新列
UPDATE warehouse_receipt SET new_id = UUID() WHERE new_id IS NULL;

-- 步骤4: 删除旧主键约束
ALTER TABLE warehouse_receipt DROP PRIMARY KEY;

-- 步骤5: 设置新主键
ALTER TABLE warehouse_receipt ADD PRIMARY KEY (new_id);

-- 步骤6: 重命名列
ALTER TABLE warehouse_receipt CHANGE COLUMN id old_id BIGINT;
ALTER TABLE warehouse_receipt CHANGE COLUMN new_id id VARCHAR(36);

-- 步骤7: 删除receipt_id列（冗余业务ID）
ALTER TABLE warehouse_receipt DROP COLUMN receipt_id;

-- 步骤8: 删除old_id列（确认无误后执行）
-- ALTER TABLE warehouse_receipt DROP COLUMN old_id;

-- 步骤9: 删除索引
-- DROP INDEX idx_receipt_id ON warehouse_receipt;


-- ====================================================================
-- 3. Receivable（应收账款）表迁移
-- ====================================================================

-- 步骤1: 备份表
CREATE TABLE IF NOT EXISTS receivable_backup AS SELECT * FROM receivable;

-- 步骤2: 添加新的UUID列
ALTER TABLE receivable ADD COLUMN new_id VARCHAR(36);

-- 步骤3: 生成UUID并填充新列
UPDATE receivable SET new_id = UUID() WHERE new_id IS NULL;

-- 步骤4: 删除旧主键约束
ALTER TABLE receivable DROP PRIMARY KEY;

-- 步骤5: 设置新主键
ALTER TABLE receivable ADD PRIMARY KEY (new_id);

-- 步骤6: 重命名列
ALTER TABLE receivable CHANGE COLUMN id old_id BIGINT;
ALTER TABLE receivable CHANGE COLUMN new_id id VARCHAR(36);

-- 步骤7: 删除receivable_id列（冗余业务ID）
ALTER TABLE receivable DROP COLUMN receivable_id;

-- 步骤8: 删除old_id列（确认无误后执行）
-- ALTER TABLE receivable DROP COLUMN old_id;

-- 步骤9: 删除索引
-- DROP INDEX idx_receivable_id ON receivable;


-- ====================================================================
-- 第二阶段：交易记录表
-- ====================================================================

-- ====================================================================
-- 4. Endorsement（背书记录）表迁移
-- ====================================================================

-- 步骤1: 备份表
CREATE TABLE IF NOT EXISTS endorsement_backup AS SELECT * FROM endorsement;

-- 步骤2: 添加新的UUID列
ALTER TABLE endorsement ADD COLUMN new_id VARCHAR(36);

-- 步骤3: 生成UUID并填充新列
UPDATE endorsement SET new_id = UUID() WHERE new_id IS NULL;

-- 步骤4: 删除旧主键约束
ALTER TABLE endorsement DROP PRIMARY KEY;

-- 步骤5: 设置新主键
ALTER TABLE endorsement ADD PRIMARY KEY (new_id);

-- 步骤6: 重命名列
ALTER TABLE endorsement CHANGE COLUMN id old_id BIGINT;
ALTER TABLE endorsement CHANGE COLUMN new_id id VARCHAR(36);

-- 步骤7: 修改bill_id列的长度为36（匹配新的UUID格式）
ALTER TABLE endorsement MODIFY COLUMN bill_id VARCHAR(36) NOT NULL;

-- 步骤8: 删除old_id列（确认无误后执行）
-- ALTER TABLE endorsement DROP COLUMN old_id;


-- ====================================================================
-- 5. DiscountRecord（贴现记录）表迁移
-- ====================================================================

-- 步骤1: 备份表
CREATE TABLE IF NOT EXISTS discount_record_backup AS SELECT * FROM discount_record;

-- 步骤2: 添加新的UUID列
ALTER TABLE discount_record ADD COLUMN new_id VARCHAR(36);

-- 步骤3: 生成UUID并填充新列
UPDATE discount_record SET new_id = UUID() WHERE new_id IS NULL;

-- 步骤4: 删除旧主键约束
ALTER TABLE discount_record DROP PRIMARY KEY;

-- 步骤5: 设置新主键
ALTER TABLE discount_record ADD PRIMARY KEY (new_id);

-- 步骤6: 重命名列
ALTER TABLE discount_record CHANGE COLUMN id old_id BIGINT;
ALTER TABLE discount_record CHANGE COLUMN new_id id VARCHAR(36);

-- 步骤7: 修改bill_id列的长度为36（匹配新的UUID格式）
ALTER TABLE discount_record MODIFY COLUMN bill_id VARCHAR(36) NOT NULL;

-- 步骤8: 删除old_id列（确认无误后执行）
-- ALTER TABLE discount_record DROP COLUMN old_id;


-- ====================================================================
-- 6. RepaymentRecord（还款记录）表迁移
-- ====================================================================

-- 步骤1: 备份表
CREATE TABLE IF NOT EXISTS repayment_record_backup AS SELECT * FROM repayment_record;

-- 步骤2: 添加新的UUID列
ALTER TABLE repayment_record ADD COLUMN new_id VARCHAR(36);

-- 步骤3: 生成UUID并填充新列
UPDATE repayment_record SET new_id = UUID() WHERE new_id IS NULL;

-- 步骤4: 删除旧主键约束
ALTER TABLE repayment_record DROP PRIMARY KEY;

-- 步骤5: 设置新主键
ALTER TABLE repayment_record ADD PRIMARY KEY (new_id);

-- 步骤6: 重命名列
ALTER TABLE repayment_record CHANGE COLUMN id old_id BIGINT;
ALTER TABLE repayment_record CHANGE COLUMN new_id id VARCHAR(36);

-- 步骤7: 修改bill_id列的长度为36（匹配新的UUID格式）
ALTER TABLE repayment_record MODIFY COLUMN bill_id VARCHAR(36) NOT NULL;

-- 步骤8: 删除old_id列（确认无误后执行）
-- ALTER TABLE repayment_record DROP COLUMN old_id;


-- ====================================================================
-- 第三阶段：外键关系处理（如果有）
-- ====================================================================

-- 如果有其他表引用这些表的ID，需要修改外键约束
-- 例如：如果有其他表引用bill表的bill_id

-- 示例：修改外键列长度
-- ALTER TABLE some_other_table MODIFY COLUMN bill_id VARCHAR(36);

-- 示例：重新创建外键约束（如果需要）
-- ALTER TABLE some_other_table
--     ADD CONSTRAINT fk_some_other_table_bill
--     FOREIGN KEY (bill_id) REFERENCES bill(id);


-- ====================================================================
-- 第四阶段：清理工作（执行确认无误后）
-- ====================================================================

-- 删除所有备份表（确认迁移成功后执行）
-- DROP TABLE IF EXISTS bill_backup;
-- DROP TABLE IF EXISTS warehouse_receipt_backup;
-- DROP TABLE IF EXISTS receivable_backup;
-- DROP TABLE IF EXISTS endorsement_backup;
-- DROP TABLE IF EXISTS discount_record_backup;
-- DROP TABLE IF EXISTS repayment_record_backup;

-- 删除所有old_id列（确认迁移成功后执行）
-- ALTER TABLE bill DROP COLUMN old_id;
-- ALTER TABLE warehouse_receipt DROP COLUMN old_id;
-- ALTER TABLE receivable DROP COLUMN old_id;
-- ALTER TABLE endorsement DROP COLUMN old_id;
-- ALTER TABLE discount_record DROP COLUMN old_id;
-- ALTER TABLE repayment_record DROP COLUMN old_id;


-- ====================================================================
-- 验证SQL（用于检查迁移结果）
-- ====================================================================

-- 验证UUID格式是否正确
-- SELECT
--     'bill' as table_name,
--     COUNT(*) as total_rows,
--     SUM(CASE WHEN id REGEXP '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$' THEN 1 ELSE 0 END) as valid_uuids
-- FROM bill
-- UNION ALL
-- SELECT
--     'warehouse_receipt' as table_name,
--     COUNT(*) as total_rows,
--     SUM(CASE WHEN id REGEXP '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$' THEN 1 ELSE 0 END) as valid_uuids
-- FROM warehouse_receipt;

-- 检查是否有重复的UUID
-- SELECT id, COUNT(*) as count FROM bill GROUP BY id HAVING COUNT(*) > 1;


-- ====================================================================
-- 回滚脚本（如果迁移失败需要回滚）
-- ====================================================================

/*
-- 回滚Bill表
ALTER TABLE bill DROP PRIMARY KEY;
ALTER TABLE bill ADD PRIMARY KEY (old_id);
ALTER TABLE bill CHANGE COLUMN id new_id VARCHAR(36);
ALTER TABLE bill CHANGE COLUMN old_id id BIGINT AUTO_INCREMENT;
ALTER TABLE bill ADD COLUMN bill_id VARCHAR(64) UNIQUE;
UPDATE bill SET bill_id = CONCAT('bill', LPAD(id, 6, '0'));
ALTER TABLE bill DROP COLUMN new_id;
DROP TABLE IF EXISTS bill_backup;

-- 回滚其他表（类似操作）
*/
