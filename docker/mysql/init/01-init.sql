-- =====================================================
-- FISCO 供应链金融系统 - MySQL 初始化脚本
-- =====================================================
-- 此脚本在MySQL容器首次启动时自动执行

-- 创建数据库（如不存在）
CREATE DATABASE IF NOT EXISTS fisco_data DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 授权
GRANT ALL PRIVILEGES ON fisco_data.* TO 'fisco_user'@'%';
FLUSH PRIVILEGES;

-- 设置时区
SET GLOBAL time_zone = '+08:00';
