-- 添加企业级管理员角色支持
-- Date: 2026-01-23
-- Description: 扩展 user_type 枚举，添加 ENTERPRISE_ADMIN 角色

-- 更新 user_type 列的枚举约束，添加 ENTERPRISE_ADMIN
ALTER TABLE `user` MODIFY COLUMN `user_type` VARCHAR(20) NOT NULL
    COMMENT '用户类型: ADMIN-系统管理员, ENTERPRISE_ADMIN-企业管理员, ENTERPRISE_USER-企业用户, AUDITOR-审计员, OPERATOR-操作员';

-- 为现有数据添加注释说明
-- 默认值为 ENTERPRISE_USER（企业普通用户）
-- 只有企业管理员（ENTERPRISE_ADMIN 或 ADMIN 登录类型）可以将用户提升为 ENTERPRISE_ADMIN

-- 添加索引以提高基于用户类型的查询性能
CREATE INDEX idx_user_type ON `user`(`user_type`);

-- 记录迁移信息
INSERT INTO schema_version_log (version, description, executed_at)
VALUES ('V9__add_enterprise_admin_role', '添加企业级管理员角色', NOW())
ON DUPLICATE KEY UPDATE executed_at = NOW();
