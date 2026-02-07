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

-- 插入默认管理员账户
-- 密码为: admin123（BCrypt加密后的值）
-- 用户名为: admin
-- 角色为: SUPER_ADMIN（超级管理员）
INSERT INTO `admin` (
    `id`,
    `username`,
    `email`,
    `real_name`,
    `role`,
    `status`,
    `password`,
    `remarks`,
    `created_at`,
    `updated_at`,
    `created_by`
) VALUES (
    UUID(),
    'admin',
    'admin@system.com',
    '系统管理员',
    'SUPER_ADMIN',
    'ACTIVE',
    '$2a$12$vbEz3HPDEzYFTDFnvd9I/.cHxC5P5y0Cc7TqrbzrnxtllvhMWrBoS',
    '系统默认超级管理员账户',
    NOW(),
    NOW(),
    'system'
) ON DUPLICATE KEY UPDATE `updated_at` = NOW();

-- 插入测试审核员账户
-- 密码为: auditor123
-- 用户名为: auditor
-- 角色为: AUDITOR（审核员）
INSERT INTO `admin` (
    `id`,
    `username`,
    `email`,
    `real_name`,
    `role`,
    `status`,
    `password`,
    `remarks`,
    `created_at`,
    `updated_at`,
    `created_by`
) VALUES (
    UUID(),
    'auditor',
    'auditor@system.com',
    '测试审核员',
    'AUDITOR',
    'ACTIVE',
    '$2a$12$ThH2U92L1Rnqaq0DJuiYE.Tkx2pYzoewv1NXoTsOsyBKw8hmMvyXu',
    '测试审核员账户',
    NOW(),
    NOW(),
    'admin'
) ON DUPLICATE KEY UPDATE `updated_at` = NOW();

-- 插入测试管理员账户
-- 密码为: manager123
-- 用户名为: manager
-- 角色为: ADMIN（管理员）
INSERT INTO `admin` (
    `id`,
    `username`,
    `email`,
    `real_name`,
    `role`,
    `status`,
    `password`,
    `remarks`,
    `created_at`,
    `updated_at`,
    `created_by`
) VALUES (
    UUID(),
    'manager',
    'manager@system.com',
    '测试管理员',
    'ADMIN',
    'ACTIVE',
    '$2a$12$6OueNFiUop.1iIQUvVoR7eZIGrZx8HvTem2EPr89aychqy/e1ruOW',
    '测试管理员账户',
    NOW(),
    NOW(),
    'admin'
) ON DUPLICATE KEY UPDATE `updated_at` = NOW();

-- 创建索引（如果不存在）
-- 注意：MySQL 5.7 以下版本不支持 CREATE INDEX IF NOT EXISTS
-- 如果索引已存在，会忽略错误
SET @exist_idx = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'admin' AND index_name = 'idx_role');
SET @sql = IF(@exist_idx = 0, 'CREATE INDEX `idx_role` ON `admin` (`role`)', 'SELECT "Index already exists"');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
