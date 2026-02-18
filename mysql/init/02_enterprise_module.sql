-- ============================================
-- 企业模块表结构
-- 说明：企业信息、信用评级历史、企业审核日志
-- 包含表：t_enterprise, t_credit_rating_history, t_enterprise_audit_log
-- ============================================

USE `supply_chain_finance`;

-- ============================================
-- 表名：t_enterprise
-- 说明：企业表
-- ============================================
DROP TABLE IF EXISTS `enterprise`;
CREATE TABLE `enterprise` (
  `id` varchar(36) NOT NULL COMMENT '企业ID（UUID格式，主键）',
  `address` varchar(42) NOT NULL COMMENT '区块链地址（42位）',
  `name` varchar(255) NOT NULL COMMENT '企业名称',
  `credit_code` varchar(50) NOT NULL COMMENT '统一社会信用代码',
  `username` varchar(100) NOT NULL COMMENT '用户名',
  `email` varchar(150) DEFAULT NULL COMMENT '企业邮箱',
  `phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `enterprise_address` varchar(500) DEFAULT NULL COMMENT '企业地址',
  `role` varchar(30) NOT NULL DEFAULT 'SUPPLIER' COMMENT '企业角色：SUPPLIER-供应商, CORE_ENTERPRISE-核心企业, FINANCIAL_INSTITUTION-金融机构, REGULATOR-监管机构, WAREHOUSE_PROVIDER-仓储方',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING-待审核, ACTIVE-激活, SUSPENDED-暂停, BLACKLISTED-黑名单, PENDING_DELETION-待删除, DELETED-已删除',
  `credit_rating` int DEFAULT NULL COMMENT '信用评级（0-100）',
  `credit_limit` decimal(20,2) DEFAULT NULL COMMENT '授信额度',
  `registered_at` datetime DEFAULT NULL COMMENT '注册时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  `created_by` varchar(50) DEFAULT NULL COMMENT '创建者',
  `updated_by` varchar(50) DEFAULT NULL COMMENT '更新人',
  `password` varchar(255) DEFAULT NULL COMMENT '登录密码',
  `api_key` varchar(64) DEFAULT NULL COMMENT 'API密钥',
  `remarks` text COMMENT '备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_address` (`address`),
  UNIQUE KEY `uk_credit_code` (`credit_code`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_email` (`email`),
  UNIQUE KEY `uk_phone` (`phone`),
  KEY `idx_role` (`role`),
  KEY `idx_status` (`status`),
  KEY `idx_credit_rating` (`credit_rating`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='企业表';

-- ============================================
-- 表名：t_credit_rating_history
-- 说明：信用评级历史表
-- ============================================
DROP TABLE IF EXISTS `credit_rating_history`;
CREATE TABLE `credit_rating_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `enterprise_address` varchar(42) NOT NULL COMMENT '企业区块链地址',
  `enterprise_name` varchar(255) DEFAULT NULL COMMENT '企业名称',
  `old_rating` int NOT NULL COMMENT '原评级',
  `new_rating` int NOT NULL COMMENT '新评级',
  `change_reason` text COMMENT '变更原因',
  `changed_by` varchar(100) NOT NULL COMMENT '操作人',
  `changed_at` datetime NOT NULL COMMENT '变更时间',
  `tx_hash` varchar(128) DEFAULT NULL COMMENT '交易哈希',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_enterprise_address` (`enterprise_address`),
  KEY `idx_changed_at` (`changed_at`),
  KEY `idx_new_rating` (`new_rating`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='信用评级历史表';

-- ============================================
-- 表名：t_enterprise_audit_log
-- 说明：企业审核日志表
-- ============================================
DROP TABLE IF EXISTS `enterprise_audit_log`;
CREATE TABLE `enterprise_audit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `enterprise_address` varchar(64) NOT NULL COMMENT '企业地址',
  `enterprise_name` varchar(255) DEFAULT NULL COMMENT '企业名称',
  `auditor` varchar(100) NOT NULL COMMENT '审核人',
  `action` varchar(20) NOT NULL COMMENT '审核动作：APPROVE-通过, REJECT-拒绝, SUSPEND-暂停, ACTIVATE-激活, BLACKLIST-黑名单, REQUEST_DELETE-请求删除, APPROVE_DELETE-批准删除, REJECT_DELETE-拒绝删除',
  `reason` text COMMENT '审核理由',
  `audit_time` datetime NOT NULL COMMENT '审核时间',
  `ip_address` varchar(50) DEFAULT NULL COMMENT 'IP地址',
  `tx_hash` varchar(128) DEFAULT NULL COMMENT '交易哈希',
  `remarks` text COMMENT '备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_enterprise_address` (`enterprise_address`),
  KEY `idx_auditor` (`auditor`),
  KEY `idx_action` (`action`),
  KEY `idx_audit_time` (`audit_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='企业审核日志表';
