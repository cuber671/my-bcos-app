-- 回滚：删除enterprise表的uuid字段
-- Rollback: V2__rollback_enterprise_uuid.sql

-- 步骤1: 删除索引
DROP INDEX `idx_uuid` ON `enterprise`;
DROP INDEX `uk_uuid` ON `enterprise`;

-- 步骤2: 删除uuid列
ALTER TABLE `enterprise`
DROP COLUMN `uuid`;
