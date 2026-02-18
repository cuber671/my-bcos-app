-- 将enterprise表的id从自增BIGINT改为UUID
-- Migration: V4__convert_enterprise_id_to_uuid.sql
-- Author: Claude
-- Date: 2026-01-19

-- 注意：由于这是一个破坏性变更，如果表中有数据，需要先备份数据
-- 此脚本假设表为空或可以清空

-- 步骤1: 备份现有数据（如果有）
-- CREATE TABLE enterprise_backup AS SELECT * FROM enterprise;

-- 步骤2: 删除外键约束（user表的enterprise_id）
ALTER TABLE `user` DROP FOREIGN KEY IF EXISTS fk_user_enterprise;

-- 步骤3: 删除user表的enterprise_id索引（如果存在）
DROP INDEX IF EXISTS idx_user_enterprise_id ON `user`;

-- 步骤4: 删除uuid列（不再需要单独的uuid列）
ALTER TABLE `enterprise` DROP COLUMN IF EXISTS `uuid`;

-- 步骤5: 删除自增ID列
ALTER TABLE `enterprise` DROP COLUMN `id`;

-- 步骤6: 添加新的UUID类型的id列
ALTER TABLE `enterprise`
ADD COLUMN `id` VARCHAR(36) NOT NULL COMMENT '企业ID（UUID格式，主键）' FIRST;

-- 步骤7: 添加主键约束
ALTER TABLE `enterprise` ADD PRIMARY KEY (`id`);

-- 步骤8: 添加唯一索引
CREATE UNIQUE INDEX `uk_enterprise_id` ON `enterprise` (`id`);

-- 步骤9: 修改user表的enterprise_id列类型
ALTER TABLE `user`
MODIFY COLUMN `enterprise_id` VARCHAR(36) NULL COMMENT '所属企业ID（UUID）';

-- 步骤10: 重新添加user表的索引
CREATE INDEX `idx_enterprise_id` ON `user` (`enterprise_id`);

-- 步骤11: 重新添加外键约束（如果需要）
-- ALTER TABLE `user` ADD CONSTRAINT fk_user_enterprise FOREIGN KEY (`enterprise_id`) REFERENCES `enterprise`(`id`) ON DELETE CASCADE;

-- 验证
-- SELECT 'Migration completed' as status;
-- DESC enterprise;
-- DESC user;
