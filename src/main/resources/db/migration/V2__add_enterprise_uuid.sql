-- 为enterprise表添加uuid字段
-- Migration: V2__add_enterprise_uuid.sql
-- Author: Claude
-- Date: 2025-01-19

-- 步骤1: 添加uuid列（允许NULL，先为现有数据生成UUID）
ALTER TABLE `enterprise`
ADD COLUMN `uuid` VARCHAR(36) NULL COMMENT '企业UUID（对外API使用）'
AFTER `id`;

-- 步骤2: 为现有数据生成UUID
UPDATE `enterprise`
SET `uuid` = UUID()
WHERE `uuid` IS NULL;

-- 步骤3: 添加unique索引和NOT NULL约束
ALTER TABLE `enterprise`
MODIFY COLUMN `uuid` VARCHAR(36) NOT NULL COMMENT '企业UUID（对外API使用）',
ADD UNIQUE KEY `uk_uuid` (`uuid`);

-- 步骤4: 添加索引以提升查询性能
CREATE INDEX `idx_uuid` ON `enterprise` (`uuid`);

-- 验证迁移结果
-- SELECT COUNT(*) as total, COUNT(uuid) as with_uuid FROM enterprise;
