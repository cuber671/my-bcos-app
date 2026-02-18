-- 管理员表创建脚本
-- 创建管理员表
CREATE TABLE IF NOT EXISTS `admin` (
    `id` VARCHAR(36) NOT NULL COMMENT '管理员ID（UUID）',
    `username` VARCHAR(100) NOT NULL COMMENT '管理员用户名',
    `email` VARCHAR(150) DEFAULT NULL COMMENT '管理员邮箱',
    `real_name` VARCHAR(100) DEFAULT NULL COMMENT '真实姓名',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
    `role` VARCHAR(30) NOT NULL COMMENT '管理员角色：SUPER_ADMIN-超级管理员, ADMIN-管理员, AUDITOR-审核员',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '管理员状态：ACTIVE-已激活, DISABLED-已禁用, LOCKED-已锁定',
    `password` VARCHAR(255) DEFAULT NULL COMMENT '登录密码（BCrypt加密）',
    `last_login_at` DATETIME DEFAULT NULL COMMENT '最后登录时间',
    `last_login_ip` VARCHAR(50) DEFAULT NULL COMMENT '最后登录IP',
    `login_count` INT DEFAULT 0 COMMENT '登录次数',
    `failed_login_attempts` INT DEFAULT 0 COMMENT '失败登录尝试次数',
    `locked_until` DATETIME DEFAULT NULL COMMENT '锁定到期时间',
    `remarks` TEXT COMMENT '备注信息',
    `created_at` DATETIME DEFAULT NULL COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT NULL COMMENT '更新时间',
    `created_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人',
    `updated_by` VARCHAR(50) DEFAULT NULL COMMENT '更新人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `idx_username` (`username`),
    UNIQUE KEY `idx_email` (`email`),
    KEY `idx_status` (`status`),
    KEY `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员表';

-- ============================================================================
-- SECURITY WARNING: Default accounts removed for production security
-- ============================================================================
-- The default admin accounts (admin/admin123, auditor/auditor123, manager/manager123)
-- have been removed from this script for security reasons.
--
-- For production deployment:
-- 1. Create admin accounts programmatically during first-time setup
-- 2. Enforce strong password policy (min 12 chars, uppercase, lowercase, numbers, symbols)
-- 3. Force password change on first login
-- 4. Use unique randomly generated passwords for each environment
--
-- For development/testing only:
-- If you need to create test accounts, use the commands below:
--
-- -- Create SUPER_ADMIN account (password: Admin@123456)
-- INSERT INTO `admin` (`id`, `username`, `email`, `real_name`, `role`, `status`, `password`, `remarks`, `created_at`, `updated_at`, `created_by`)
-- VALUES (UUID(), 'admin', 'admin@localhost', 'Development Admin', 'SUPER_ADMIN', 'ACTIVE',
--         '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYzpLaEmc0W', -- Admin@123456
--         'Development only - REMOVE IN PRODUCTION', NOW(), NOW(), 'system')
-- ON DUPLICATE KEY UPDATE `updated_at` = NOW();
--
-- IMPORTANT: These test accounts MUST be removed before production deployment!
-- ============================================================================

-- 创建索引（如果不存在）
-- 注意：MySQL 5.7 以下版本不支持 CREATE INDEX IF NOT EXISTS
-- 如果索引已存在，会忽略错误
SET @exist_idx = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'admin' AND index_name = 'idx_role');
SET @sql = IF(@exist_idx = 0, 'CREATE INDEX `idx_role` ON `admin` (`role`)', 'SELECT "Index already exists"');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
