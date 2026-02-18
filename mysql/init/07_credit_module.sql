-- ============================================
-- 信用额度模块表结构
-- 说明：信用额度管理、额度使用记录、额度调整申请、额度预警
-- 包含表：t_credit_limit, t_credit_limit_usage, t_credit_limit_adjust_request, t_credit_limit_warning
-- ============================================

USE `supply_chain_finance`;

-- ============================================
-- 表名：t_credit_limit
-- 说明：信用额度表
-- ============================================
DROP TABLE IF EXISTS `credit_limit`;
CREATE TABLE `credit_limit` (
  `id` varchar(36) NOT NULL COMMENT '额度ID（UUID格式，主键）',
  `enterprise_address` varchar(42) NOT NULL COMMENT '企业地址（区块链地址）',
  `enterprise_name` varchar(200) DEFAULT NULL COMMENT '企业名称（冗余字段，方便查询）',
  `limit_type` varchar(20) NOT NULL COMMENT '额度类型：FINANCING-融资额度, GUARANTEE-担保额度, CREDIT-赊账额度',
  `total_limit` bigint NOT NULL COMMENT '总额度（单位：分）',
  `used_limit` bigint NOT NULL DEFAULT 0 COMMENT '已使用额度（单位：分）',
  `frozen_limit` bigint NOT NULL DEFAULT 0 COMMENT '冻结额度（单位：分）',
  `warning_threshold` int NOT NULL COMMENT '预警阈值（百分比，1-100）',
  `effective_date` datetime NOT NULL COMMENT '生效日期',
  `expiry_date` datetime DEFAULT NULL COMMENT '失效日期（可选，为空表示永久有效）',
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '额度状态：ACTIVE-生效中, EXPIRED-已过期, FROZEN-已冻结, CANCELLED-已取消',
  `approver_address` varchar(42) DEFAULT NULL COMMENT '审批人地址（区块链地址）',
  `approve_reason` text COMMENT '审批原因',
  `approve_time` datetime DEFAULT NULL COMMENT '审批时间',
  `overdue_count` int NOT NULL DEFAULT 0 COMMENT '逾期次数（用于风险评估）',
  `bad_debt_count` int NOT NULL DEFAULT 0 COMMENT '坏账次数（用于风险评估）',
  `risk_level` varchar(20) DEFAULT NULL COMMENT '风险等级：LOW-低, MEDIUM-中, HIGH-高',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `tx_hash` varchar(66) DEFAULT NULL COMMENT '区块链交易哈希',
  PRIMARY KEY (`id`),
  KEY `idx_enterprise_address` (`enterprise_address`),
  KEY `idx_limit_type` (`limit_type`),
  KEY `idx_status` (`status`),
  KEY `idx_risk_level` (`risk_level`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='信用额度表';

-- ============================================
-- 表名：t_credit_limit_usage
-- 说明：信用额度使用记录表
-- ============================================
DROP TABLE IF EXISTS `credit_limit_usage`;
CREATE TABLE `credit_limit_usage` (
  `id` varchar(36) NOT NULL COMMENT '记录ID（UUID格式，主键）',
  `credit_limit_id` varchar(36) NOT NULL COMMENT '额度ID（外键）',
  `usage_type` varchar(20) NOT NULL COMMENT '使用类型：USE-使用, RELEASE-释放, FREEZE-冻结, UNFREEZE-解冻',
  `business_type` varchar(50) NOT NULL COMMENT '业务类型（融资申请/担保申请/赊账等）',
  `business_id` varchar(36) NOT NULL COMMENT '业务ID（关联业务表的主键）',
  `amount` bigint NOT NULL COMMENT '使用金额（单位：分，正数表示增加使用或冻结，负数表示释放或解冻）',
  `before_available` bigint NOT NULL COMMENT '使用前可用额度（单位：分）',
  `after_available` bigint NOT NULL COMMENT '使用后可用额度（单位：分）',
  `before_used` bigint NOT NULL COMMENT '使用前已使用额度（单位：分）',
  `after_used` bigint NOT NULL COMMENT '使用后已使用额度（单位：分）',
  `before_frozen` bigint NOT NULL COMMENT '使用前冻结额度（单位：分）',
  `after_frozen` bigint NOT NULL COMMENT '使用后冻结额度（单位：分）',
  `operator_address` varchar(42) DEFAULT NULL COMMENT '操作人地址（区块链地址）',
  `operator_name` varchar(100) DEFAULT NULL COMMENT '操作人姓名（冗余字段，方便查询）',
  `usage_date` datetime NOT NULL COMMENT '使用日期',
  `remark` text COMMENT '备注说明',
  `tx_hash` varchar(66) DEFAULT NULL COMMENT '区块链交易哈希',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_credit_limit_id` (`credit_limit_id`),
  KEY `idx_usage_type` (`usage_type`),
  KEY `idx_business_type` (`business_type`),
  KEY `idx_business_id` (`business_id`),
  KEY `idx_usage_date` (`usage_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='信用额度使用记录表';

-- ============================================
-- 表名：t_credit_limit_adjust_request
-- 说明：信用额度调整申请表
-- ============================================
DROP TABLE IF EXISTS `credit_limit_adjust_request`;
CREATE TABLE `credit_limit_adjust_request` (
  `id` varchar(36) NOT NULL COMMENT '申请ID（UUID格式，主键）',
  `credit_limit_id` varchar(36) NOT NULL COMMENT '额度ID（外键）',
  `adjust_type` varchar(20) NOT NULL COMMENT '调整类型：INCREASE-增加, DECREASE-减少, RESET-重置',
  `current_limit` bigint NOT NULL COMMENT '当前额度（单位：分）',
  `new_limit` bigint NOT NULL COMMENT '调整后额度（单位：分）',
  `adjust_amount` bigint NOT NULL COMMENT '调整金额（单位：分，可为正数或负数）',
  `request_reason` text NOT NULL COMMENT '申请原因',
  `requester_address` varchar(42) NOT NULL COMMENT '申请人地址（区块链地址）',
  `requester_name` varchar(100) NOT NULL COMMENT '申请人姓名（冗余字段，方便查询）',
  `request_date` datetime NOT NULL COMMENT '申请日期',
  `request_status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '申请状态：PENDING-待审批, APPROVED-已通过, REJECTED-已拒绝',
  `approver_address` varchar(42) DEFAULT NULL COMMENT '审批人地址（区块链地址）',
  `approver_name` varchar(100) DEFAULT NULL COMMENT '审批人姓名（冗余字段，方便查询）',
  `approve_date` datetime DEFAULT NULL COMMENT '审批日期',
  `approve_reason` text COMMENT '审批意见',
  `reject_reason` text COMMENT '拒绝原因',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `tx_hash` varchar(66) DEFAULT NULL COMMENT '区块链交易哈希',
  PRIMARY KEY (`id`),
  KEY `idx_credit_limit_id` (`credit_limit_id`),
  KEY `idx_adjust_type` (`adjust_type`),
  KEY `idx_request_status` (`request_status`),
  KEY `idx_requester_address` (`requester_address`),
  KEY `idx_request_date` (`request_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='信用额度调整申请表';

-- ============================================
-- 表名：t_credit_limit_warning
-- 说明：信用额度预警记录表
-- ============================================
DROP TABLE IF EXISTS `credit_limit_warning`;
CREATE TABLE `credit_limit_warning` (
  `id` varchar(36) NOT NULL COMMENT '预警ID（UUID格式，主键）',
  `credit_limit_id` varchar(36) NOT NULL COMMENT '额度ID（外键）',
  `warning_level` varchar(20) NOT NULL COMMENT '预警级别：LOW-低, MEDIUM-中, HIGH-高, CRITICAL-严重',
  `warning_type` varchar(50) NOT NULL COMMENT '预警类型：USAGE_HIGH-使用率过高, EXPIRY_SOON-额度即将到期, RISK_UP-风险等级提升, OVERDUE-逾期',
  `current_usage_rate` double NOT NULL COMMENT '当前使用率（百分比）',
  `warning_threshold` double NOT NULL COMMENT '预警阈值（百分比）',
  `warning_title` varchar(200) NOT NULL COMMENT '预警标题',
  `warning_content` text NOT NULL COMMENT '预警内容',
  `warning_date` datetime NOT NULL COMMENT '预警日期',
  `is_resolved` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已处理',
  `resolved_by_address` varchar(42) DEFAULT NULL COMMENT '处理人地址（区块链地址）',
  `resolved_by_name` varchar(100) DEFAULT NULL COMMENT '处理人姓名（冗余字段，方便查询）',
  `resolved_date` datetime DEFAULT NULL COMMENT '处理日期',
  `resolution` text COMMENT '处理措施',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `tx_hash` varchar(66) DEFAULT NULL COMMENT '区块链交易哈希',
  PRIMARY KEY (`id`),
  KEY `idx_credit_limit_id` (`credit_limit_id`),
  KEY `idx_warning_level` (`warning_level`),
  KEY `idx_warning_type` (`warning_type`),
  KEY `idx_is_resolved` (`is_resolved`),
  KEY `idx_warning_date` (`warning_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='信用额度预警记录表';
