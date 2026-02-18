-- ============================================
-- 系统模块表结构
-- 说明：审计日志、权限审计日志、通知、通知模板
-- 包含表：t_audit_log, t_permission_audit_log, t_notification, t_notification_template
-- ============================================

USE `supply_chain_finance`;

-- ============================================
-- 表名：t_audit_log
-- 说明：审计日志表
-- ============================================
DROP TABLE IF EXISTS `audit_log`;
CREATE TABLE `audit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_address` varchar(42) DEFAULT NULL COMMENT '操作人地址',
  `user_name` varchar(100) DEFAULT NULL COMMENT '操作人姓名（冗余字段）',
  `module` varchar(50) NOT NULL COMMENT '操作模块',
  `action_type` varchar(20) NOT NULL COMMENT '操作类型',
  `action_desc` varchar(200) NOT NULL COMMENT '操作描述',
  `entity_type` varchar(50) DEFAULT NULL COMMENT '实体类型',
  `entity_id` varchar(64) DEFAULT NULL COMMENT '实体ID',
  `old_value` text COMMENT '操作前的数据（JSON格式）',
  `new_value` text COMMENT '操作后的数据（JSON格式）',
  `changed_fields` text COMMENT '变更的字段（JSON格式）',
  `request_method` varchar(10) DEFAULT NULL COMMENT '请求方法',
  `request_url` varchar(500) DEFAULT NULL COMMENT '请求URL',
  `request_ip` varchar(50) DEFAULT NULL COMMENT '请求IP地址',
  `user_agent` varchar(500) DEFAULT NULL COMMENT 'User-Agent',
  `result` varchar(20) DEFAULT NULL COMMENT '操作结果',
  `error_message` text COMMENT '错误信息',
  `duration` bigint DEFAULT NULL COMMENT '操作时长（毫秒）',
  `tx_hash` varchar(66) DEFAULT NULL COMMENT '区块链交易哈希',
  `is_success` tinyint(1) NOT NULL COMMENT '操作是否成功',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `tenant_id` varchar(64) DEFAULT NULL COMMENT '租户ID',
  PRIMARY KEY (`id`),
  KEY `idx_user_address` (`user_address`),
  KEY `idx_module` (`module`),
  KEY `idx_action_type` (`action_type`),
  KEY `idx_entity_type` (`entity_type`),
  KEY `idx_entity_id` (`entity_id`),
  KEY `idx_audit_time` (`created_at`),
  KEY `idx_result` (`result`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审计日志表';

-- ============================================
-- 表名：t_permission_audit_log
-- 说明：权限审计日志表
-- ============================================
DROP TABLE IF EXISTS `permission_audit_log`;
CREATE TABLE `permission_audit_log` (
  `id` varchar(36) NOT NULL COMMENT '日志ID（UUID格式，主键）',
  `username` varchar(100) NOT NULL COMMENT '操作用户名',
  `enterprise_id` varchar(36) DEFAULT NULL COMMENT '用户所属企业ID',
  `user_role` varchar(50) DEFAULT NULL COMMENT '用户角色',
  `login_type` varchar(20) DEFAULT NULL COMMENT '登录类型：USER-用户, ADMIN-管理员, ENTERPRISE-企业',
  `permission_type` varchar(50) NOT NULL COMMENT '权限检查类型',
  `target_resource` varchar(255) DEFAULT NULL COMMENT '目标资源',
  `operation` varchar(100) DEFAULT NULL COMMENT '执行的操作',
  `access_granted` tinyint(1) NOT NULL COMMENT '是否授予权限',
  `denial_reason` text COMMENT '拒绝原因',
  `ip_address` varchar(50) DEFAULT NULL COMMENT '客户端IP地址',
  `user_agent` varchar(500) DEFAULT NULL COMMENT '客户端User-Agent',
  `request_method` varchar(10) DEFAULT NULL COMMENT 'HTTP请求方法',
  `request_uri` varchar(500) DEFAULT NULL COMMENT '请求URI',
  `details` text COMMENT '详细信息（JSON格式）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_username` (`username`),
  KEY `idx_enterprise_id` (`enterprise_id`),
  KEY `idx_permission_type` (`permission_type`),
  KEY `idx_access_granted` (`access_granted`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限审计日志表';

-- ============================================
-- 表名：t_notification
-- 说明：通知表
-- ============================================
DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification` (
  `id` varchar(36) NOT NULL COMMENT '通知ID',
  `recipient_id` varchar(36) NOT NULL COMMENT '接收者用户ID',
  `recipient_type` varchar(20) NOT NULL COMMENT '接收者类型：USER-用户, ENTERPRISE-企业, ROLE-角色',
  `sender_id` varchar(36) DEFAULT NULL COMMENT '发送者用户ID',
  `sender_type` varchar(20) DEFAULT NULL COMMENT '发送者类型：SYSTEM-系统, USER-用户, ENTERPRISE-企业',
  `type` varchar(50) NOT NULL COMMENT '通知类型：SYSTEM-系统, APPROVAL-审批, RISK-风险, WARNING-预警, BUSINESS-业务, REMINDER-提醒',
  `category` varchar(50) DEFAULT NULL COMMENT '通知分类',
  `title` varchar(200) NOT NULL COMMENT '通知标题',
  `content` text NOT NULL COMMENT '通知内容',
  `priority` varchar(20) NOT NULL DEFAULT 'NORMAL' COMMENT '优先级：LOW-低, NORMAL-中, HIGH-高, URGENT-紧急',
  `status` varchar(20) NOT NULL DEFAULT 'UNREAD' COMMENT '状态：UNREAD-未读, READ-已读, ARCHIVED-已归档, DELETED-已删除',
  `action_type` varchar(50) DEFAULT NULL COMMENT '操作类型',
  `action_url` varchar(500) DEFAULT NULL COMMENT '操作链接',
  `action_params` text DEFAULT NULL COMMENT '操作参数(JSON格式)',
  `business_type` varchar(50) DEFAULT NULL COMMENT '业务类型',
  `business_id` varchar(36) DEFAULT NULL COMMENT '业务记录ID',
  `extra_data` text COMMENT '额外数据(JSON格式)',
  `is_sent` tinyint(1) DEFAULT 0 COMMENT '是否已发送',
  `sent_at` datetime DEFAULT NULL COMMENT '发送时间',
  `read_at` datetime DEFAULT NULL COMMENT '阅读时间',
  `expire_at` datetime DEFAULT NULL COMMENT '过期时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_recipient_id` (`recipient_id`),
  KEY `idx_recipient_type` (`recipient_type`),
  KEY `idx_sender_id` (`sender_id`),
  KEY `idx_type` (`type`),
  KEY `idx_status` (`status`),
  KEY `idx_priority` (`priority`),
  KEY `idx_business` (`business_type`, `business_id`),
  KEY `idx_recipient_status` (`recipient_id`, `status`),
  KEY `idx_recipient_type_status` (`recipient_type`, `status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';

-- ============================================
-- 表名：t_notification_template
-- 说明：通知模板表
-- ============================================
DROP TABLE IF EXISTS `notification_template`;
CREATE TABLE `notification_template` (
  `id` varchar(36) NOT NULL COMMENT '模板ID',
  `code` varchar(50) NOT NULL COMMENT '模板代码',
  `name` varchar(100) NOT NULL COMMENT '模板名称',
  `type` varchar(50) NOT NULL COMMENT '通知类型：SYSTEM-系统, APPROVAL-审批, RISK-风险, WARNING-预警, BUSINESS-业务, REMINDER-提醒',
  `category` varchar(50) DEFAULT NULL COMMENT '通知分类',
  `title_template` varchar(200) NOT NULL COMMENT '标题模板',
  `content_template` text NOT NULL COMMENT '内容模板',
  `action_type` varchar(50) DEFAULT NULL COMMENT '默认操作类型',
  `priority` varchar(20) NOT NULL DEFAULT 'NORMAL' COMMENT '默认优先级：LOW-低, NORMAL-中, HIGH-高, URGENT-紧急',
  `description` varchar(500) DEFAULT NULL COMMENT '模板描述',
  `is_enabled` tinyint(1) DEFAULT 1 COMMENT '是否启用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_type` (`type`),
  KEY `idx_category` (`category`),
  KEY `idx_is_enabled` (`is_enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知模板表';

-- ============================================
-- 表名：notification_send_log
-- 说明：通知发送日志表
-- ============================================
DROP TABLE IF EXISTS `notification_send_log`;
CREATE TABLE `notification_send_log` (
  `id` varchar(36) NOT NULL COMMENT '日志ID',
  `notification_id` varchar(36) NOT NULL COMMENT '通知ID',
  `recipient_id` varchar(36) NOT NULL COMMENT '接收者ID',
  `channel` varchar(20) NOT NULL COMMENT '发送渠道: IN_APP, EMAIL, SMS, PUSH',
  `status` varchar(20) NOT NULL COMMENT '状态: PENDING, SUCCESS, FAILED',
  `error_message` text COMMENT '错误信息',
  `retry_count` int DEFAULT 0 COMMENT '重试次数',
  `sent_at` datetime DEFAULT NULL COMMENT '发送时间',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_notification` (`notification_id`),
  KEY `idx_recipient` (`recipient_id`),
  KEY `idx_channel` (`channel`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知发送日志表';

-- ============================================
-- 表名：notification_subscription
-- 说明：通知订阅表
-- ============================================
DROP TABLE IF EXISTS `notification_subscription`;
CREATE TABLE `notification_subscription` (
  `id` varchar(36) NOT NULL COMMENT '订阅ID',
  `user_id` varchar(36) NOT NULL COMMENT '用户ID',
  `notification_type` varchar(50) NOT NULL COMMENT '通知类型',
  `is_subscribed` tinyint(1) DEFAULT 1 COMMENT '是否订阅',
  `notify_email` tinyint(1) DEFAULT 0 COMMENT '是否邮件通知',
  `notify_sms` tinyint(1) DEFAULT 0 COMMENT '是否短信通知',
  `notify_push` tinyint(1) DEFAULT 1 COMMENT '是否推送通知',
  `notify_in_app` tinyint(1) DEFAULT 1 COMMENT '是否应用内通知',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_type` (`user_id`, `notification_type`),
  KEY `idx_user` (`user_id`),
  KEY `idx_type` (`notification_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知订阅表';
