-- ============================================
-- 数据库创建脚本
-- 数据库名：supply_chain_finance
-- 字符集：utf8mb4
-- 排序规则：utf8mb4_unicode_ci
-- 存储引擎：InnoDB
-- 说明：基于FISCO BCOS区块链的供应链金融系统
-- ============================================

-- 创建数据库
DROP DATABASE IF EXISTS `supply_chain_finance`;
CREATE DATABASE `supply_chain_finance`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE `supply_chain_finance`;

-- ============================================
-- 数据库配置说明
-- ============================================
-- 1. 字符集：utf8mb4 - 支持emoji和全量Unicode字符
-- 2. 排序规则：utf8mb4_unicode_ci - 不区分大小写，Unicode标准排序
-- 3. 时区：建议设置为 Asia/Shanghai
-- 4. SQL模式：建议设置为 STRICT_TRANS_TABLES,NO_ZERO_DATE,NO_ZERO_IN_DATE
--
-- 执行方法：
-- mysql -u root -p < 00_create_database.sql
--
-- 前置条件：
-- - MySQL 8.0+
-- - 拥有CREATE DATABASE权限
-- ============================================
