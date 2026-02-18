-- ============================================
-- 用户模块表结构
-- 说明：用户、管理员、邀请码、用户活动、用户权限管理
-- 包含表：t_user, t_admin, t_invitation_code, t_user_activity, t_user_permission
-- ============================================

USE `supply_chain_finance`;

-- ============================================
-- 表名：t_user
-- 说明：用户表
-- ============================================
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` varchar(36) NOT NULL COMMENT '用户ID（UUID格式，主键）',
  `username` varchar(50) NOT NULL COMMENT '用户名（登录账号）',
  `password` varchar(255) DEFAULT NULL COMMENT '登录密码（BCrypt加密）',
  `real_name` varchar(100) DEFAULT NULL COMMENT '真实姓名',
  `email` varchar(100) DEFAULT NULL COMMENT '电子邮箱',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号码',
  `enterprise_id` varchar(36) DEFAULT NULL COMMENT '所属企业ID（UUID）',
  `user_type` varchar(20) NOT NULL DEFAULT 'ENTERPRISE_USER' COMMENT '用户类型：ADMIN-系统管理员, ENTERPRISE_ADMIN-企业管理员, ENTERPRISE_USER-企业用户, AUDITOR-审计员, OPERATOR-操作员',
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '用户状态：ACTIVE-激活, DISABLED-禁用, LOCKED-锁定, PENDING-待审核',
  `department` varchar(100) DEFAULT NULL COMMENT '部门',
  `position` varchar(100) DEFAULT NULL COMMENT '职位',
  `avatar_url` varchar(500) DEFAULT NULL COMMENT '头像URL',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` varchar(50) DEFAULT NULL COMMENT '最后登录IP',
  `login_count` int DEFAULT 0 COMMENT '登录次数',
  `password_changed_at` datetime DEFAULT NULL COMMENT '密码修改时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` varchar(50) DEFAULT NULL COMMENT '创建者标识',
  `updated_by` varchar(50) DEFAULT NULL COMMENT '更新人',
  `invitation_code` varchar(32) DEFAULT NULL COMMENT '注册时使用的邀请码',
  `registration_remarks` text COMMENT '注册备注信息',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_enterprise_id` (`enterprise_id`),
  KEY `idx_user_type` (`user_type`),
  KEY `idx_status` (`status`),
  KEY `idx_email` (`email`),
  KEY `idx_phone` (`phone`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================
-- 表名：t_admin
-- 说明：管理员表
-- ============================================
DROP TABLE IF EXISTS `admin`;
CREATE TABLE `admin` (
  `id` varchar(36) NOT NULL COMMENT '管理员ID（UUID格式，主键）',
  `username` varchar(100) NOT NULL COMMENT '用户名',
  `email` varchar(150) DEFAULT NULL COMMENT '邮箱',
  `real_name` varchar(100) DEFAULT NULL COMMENT '真实姓名',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `role` varchar(30) NOT NULL DEFAULT 'ADMIN' COMMENT '管理员角色：SUPER_ADMIN-超级管理员, ADMIN-管理员, AUDITOR-审计员',
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE-激活, DISABLED-禁用, LOCKED-锁定',
  `password` varchar(255) DEFAULT NULL COMMENT '密码（加密存储）',
  `last_login_at` datetime DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` varchar(50) DEFAULT NULL COMMENT '登录IP',
  `login_count` int DEFAULT 0 COMMENT '登录次数',
  `failed_login_attempts` int DEFAULT 0 COMMENT '失败登录尝试',
  `locked_until` datetime DEFAULT NULL COMMENT '锁定到期时间',
  `remarks` text COMMENT '备注',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  `created_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `updated_by` varchar(50) DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_email` (`email`),
  UNIQUE KEY `uk_phone` (`phone`),
  KEY `idx_role` (`role`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员表';

-- ============================================
-- 表名：t_invitation_code
-- 说明：邀请码表
-- ============================================
DROP TABLE IF EXISTS `invitation_code`;
CREATE TABLE `invitation_code` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` varchar(32) NOT NULL COMMENT '邀请码',
  `enterprise_id` varchar(36) NOT NULL COMMENT '所属企业ID（UUID）',
  `enterprise_name` varchar(200) DEFAULT NULL COMMENT '企业名称',
  `created_by` varchar(50) NOT NULL COMMENT '创建者标识',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `expires_at` datetime DEFAULT NULL COMMENT '过期时间',
  `max_uses` int DEFAULT NULL COMMENT '最大使用次数',
  `used_count` int NOT NULL DEFAULT 0 COMMENT '已使用次数',
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '邀请码状态：ACTIVE-有效, EXPIRED-已过期, DISABLED-已禁用',
  `remarks` text COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_enterprise_id` (`enterprise_id`),
  KEY `idx_status` (`status`),
  KEY `idx_expires_at` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邀请码表';

-- ============================================
-- 表名：t_user_activity
-- 说明：用户活动记录表
-- ============================================
DROP TABLE IF EXISTS `user_activity`;
CREATE TABLE `user_activity` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` varchar(36) NOT NULL COMMENT '用户ID',
  `username` varchar(50) DEFAULT NULL COMMENT '用户名（冗余）',
  `real_name` varchar(100) DEFAULT NULL COMMENT '真实姓名（冗余）',
  `activity_type` varchar(30) NOT NULL COMMENT '活动类型：LOGIN-登录, LOGOUT-登出, CREATE-创建, UPDATE-更新, DELETE-删除, QUERY-查询, EXPORT-导出, IMPORT-导入, APPROVE-审批, REJECT-拒绝, UNLOCK-解锁, LOCK-锁定, RESET_PASSWORD-重置密码, CHANGE_PASSWORD-修改密码, OTHER-其他',
  `description` varchar(500) NOT NULL COMMENT '活动描述',
  `module` varchar(50) DEFAULT NULL COMMENT '操作模块',
  `result` varchar(20) DEFAULT NULL COMMENT '操作结果：SUCCESS-成功, FAILURE-失败, PARTIAL-部分成功',
  `request_method` varchar(10) DEFAULT NULL COMMENT '请求方法',
  `request_url` varchar(500) DEFAULT NULL COMMENT '请求URL',
  `ip_address` varchar(50) DEFAULT NULL COMMENT 'IP地址',
  `user_agent` varchar(500) DEFAULT NULL COMMENT 'User-Agent',
  `browser` varchar(50) DEFAULT NULL COMMENT '浏览器类型',
  `os` varchar(50) DEFAULT NULL COMMENT '操作系统',
  `device` varchar(50) DEFAULT NULL COMMENT '设备类型',
  `location` varchar(200) DEFAULT NULL COMMENT '位置信息',
  `failure_reason` text COMMENT '失败原因',
  `duration` bigint DEFAULT NULL COMMENT '操作时长',
  `extra_data` text COMMENT '额外数据（JSON格式）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_activity_type` (`activity_type`),
  KEY `idx_module` (`module`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_result` (`result`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户活动记录表';

-- ============================================
-- 表名：t_user_permission
-- 说明：用户权限表
-- ============================================
DROP TABLE IF EXISTS `user_permission`;
CREATE TABLE `user_permission` (
  `id` varchar(36) NOT NULL COMMENT '权限ID（UUID）',
  `user_id` varchar(36) NOT NULL COMMENT '用户ID',
  `permission_code` varchar(100) NOT NULL COMMENT '权限代码',
  `permission_name` varchar(100) NOT NULL COMMENT '权限名称',
  `resource_type` varchar(50) NOT NULL COMMENT '资源类型：BILL-票据, RECEIVABLE-应收账款, WAREHOUSE_RECEIPT-仓单, ENTERPRISE-企业, USER-用户, CREDIT-信用, ENDORSEMENT-背书, PLEDGE-质押, RISK-风险, AUDIT-审计, SYSTEM-系统',
  `operation` varchar(20) NOT NULL COMMENT '操作类型：CREATE-创建, READ-读取, UPDATE-更新, DELETE-删除, APPROVE-审批, EXPORT-导出, IMPORT-导入, MANAGE-管理',
  `scope` varchar(20) NOT NULL COMMENT '权限范围：ALL-全部, OWN-自己的, DEPARTMENT-部门, ENTERPRISE-企业',
  `is_enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用：1-启用，0-禁用',
  `is_expired` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否过期：1-已过期，0-未过期',
  `expire_at` datetime DEFAULT NULL COMMENT '过期时间',
  `remarks` varchar(500) DEFAULT NULL COMMENT '备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` varchar(50) DEFAULT NULL COMMENT '创建者',
  `updated_by` varchar(50) DEFAULT NULL COMMENT '更新者',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_permission` (`user_id`, `permission_code`, `resource_type`, `operation`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_permission_code` (`permission_code`),
  KEY `idx_resource_type` (`resource_type`),
  KEY `idx_is_enabled` (`is_enabled`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户权限表';
