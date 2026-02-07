-- 消息通知系统表
-- 创建时间: 2026-02-03

-- 通知主表
CREATE TABLE notification (
    id VARCHAR(36) PRIMARY KEY,
    recipient_id VARCHAR(36) NOT NULL COMMENT '接收者用户ID',
    recipient_type VARCHAR(20) NOT NULL COMMENT '接收者类型: USER, ENTERPRISE, ROLE',
    sender_id VARCHAR(36) COMMENT '发送者用户ID',
    sender_type VARCHAR(20) COMMENT '发送者类型: SYSTEM, USER, ENTERPRISE',
    type VARCHAR(50) NOT NULL COMMENT '通知类型: SYSTEM, APPROVAL, RISK, WARNING, BUSINESS, REMINDER',
    category VARCHAR(50) COMMENT '通知分类: 用于更细粒度的分类',
    title VARCHAR(200) NOT NULL COMMENT '通知标题',
    content TEXT NOT NULL COMMENT '通知内容',
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '优先级: LOW, NORMAL, HIGH, URGENT',
    status VARCHAR(20) NOT NULL DEFAULT 'UNREAD' COMMENT '状态: UNREAD, READ, ARCHIVED, DELETED',
    action_type VARCHAR(50) COMMENT '操作类型: APPROVE, REJECT, VIEW, DOWNLOAD等',
    action_url VARCHAR(500) COMMENT '操作链接',
    action_params TEXT COMMENT '操作参数(JSON格式)',
    business_type VARCHAR(50) COMMENT '业务类型: BILL, RECEIVABLE, EWR, PLEDGE等',
    business_id VARCHAR(36) COMMENT '业务记录ID',
    extra_data TEXT COMMENT '额外数据(JSON格式)',
    is_sent BOOLEAN DEFAULT FALSE COMMENT '是否已发送',
    sent_at TIMESTAMP COMMENT '发送时间',
    read_at TIMESTAMP COMMENT '阅读时间',
    expire_at TIMESTAMP COMMENT '过期时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_recipient (recipient_id),
    INDEX idx_recipient_type (recipient_type),
    INDEX idx_sender (sender_id),
    INDEX idx_type (type),
    INDEX idx_status (status),
    INDEX idx_priority (priority),
    INDEX idx_business (business_type, business_id),
    INDEX idx_created_at (created_at),
    INDEX idx_recipient_status (recipient_id, status),
    INDEX idx_recipient_type_status (recipient_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表';

-- 通知模板表
CREATE TABLE notification_template (
    id VARCHAR(36) PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '模板代码',
    name VARCHAR(100) NOT NULL COMMENT '模板名称',
    type VARCHAR(50) NOT NULL COMMENT '通知类型',
    category VARCHAR(50) COMMENT '通知分类',
    title_template VARCHAR(200) NOT NULL COMMENT '标题模板',
    content_template TEXT NOT NULL COMMENT '内容模板',
    action_type VARCHAR(50) COMMENT '默认操作类型',
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '默认优先级',
    description VARCHAR(500) COMMENT '模板描述',
    is_enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_code (code),
    INDEX idx_type (type),
    INDEX idx_category (category),
    INDEX idx_enabled (is_enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知模板表';

-- 通知订阅表
CREATE TABLE notification_subscription (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL COMMENT '用户ID',
    notification_type VARCHAR(50) NOT NULL COMMENT '通知类型',
    is_subscribed BOOLEAN DEFAULT TRUE COMMENT '是否订阅',
    notify_email BOOLEAN DEFAULT FALSE COMMENT '是否邮件通知',
    notify_sms BOOLEAN DEFAULT FALSE COMMENT '是否短信通知',
    notify_push BOOLEAN DEFAULT TRUE COMMENT '是否推送通知',
    notify_in_app BOOLEAN DEFAULT TRUE COMMENT '是否应用内通知',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_type (user_id, notification_type),
    INDEX idx_user (user_id),
    INDEX idx_type (notification_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知订阅表';

-- 通知发送记录表
CREATE TABLE notification_send_log (
    id VARCHAR(36) PRIMARY KEY,
    notification_id VARCHAR(36) NOT NULL COMMENT '通知ID',
    recipient_id VARCHAR(36) NOT NULL COMMENT '接收者ID',
    channel VARCHAR(20) NOT NULL COMMENT '发送渠道: IN_APP, EMAIL, SMS, PUSH',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING, SUCCESS, FAILED',
    error_message TEXT COMMENT '错误信息',
    retry_count INT DEFAULT 0 COMMENT '重试次数',
    sent_at TIMESTAMP COMMENT '发送时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_notification (notification_id),
    INDEX idx_recipient (recipient_id),
    INDEX idx_channel (channel),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知发送记录表';

-- 插入默认通知模板
INSERT INTO notification_template (id, code, name, type, category, title_template, content_template, action_type, priority, description) VALUES
-- 系统通知模板
('tpl-001', 'SYSTEM_WELCOME', '欢迎通知', 'SYSTEM', 'SYSTEM', '欢迎使用供应链金融平台', '尊敬的用户${username}，欢迎您注册使用供应链金融平台！', NULL, 'NORMAL', '用户注册后的欢迎通知'),
('tpl-002', 'SYSTEM_MAINTENANCE', '系统维护通知', 'SYSTEM', 'SYSTEM', '系统维护通知', '系统将于${startTime}至${endTime}进行维护，请提前做好准备。', NULL, 'HIGH', '系统维护通知'),

-- 审批通知模板
('tpl-101', 'APPROVAL_PENDING', '待审批通知', 'APPROVAL', 'PENDING', '${businessType}待审批', '您有一个${businessType}申请待审批，申请人：${applicant}，申请时间：${applyTime}', 'VIEW', 'HIGH', '审批待处理通知'),
('tpl-102', 'APPROVAL_APPROVED', '审批通过通知', 'APPROVAL', 'APPROVED', '${businessType}审批通过', '您的${businessType}申请已通过审批，审批人：${approver}，审批时间：${approveTime}', 'VIEW', 'NORMAL', '审批通过通知'),
('tpl-103', 'APPROVAL_REJECTED', '审批拒绝通知', 'APPROVAL', 'REJECTED', '${businessType}审批拒绝', '您的${businessType}申请已被拒绝，审批人：${approver}，拒绝原因：${reason}', 'VIEW', 'HIGH', '审批拒绝通知'),

-- 风险通知模板
('tpl-201', 'RISK_OVERDUE', '逾期风险通知', 'RISK', 'OVERDUE', '应收账款逾期提醒', '您的应收账款${receivableId}已逾期${days}天，请及时跟进处理！', 'VIEW', 'HIGH', '应收账款逾期风险通知'),
('tpl-202', 'RISK_CREDIT_LIMIT', '信用额度预警', 'RISK', 'CREDIT_LIMIT', '信用额度使用率预警', '您的信用额度${creditLimitId}使用率已达到${usageRate}%，请关注！', 'VIEW', 'MEDIUM', '信用额度使用率预警'),
('tpl-203', 'RISK_BILL_DUE', '票据到期提醒', 'RISK', 'BILL_DUE', '票据即将到期提醒', '您的票据${billId}将在${dueDays}天后到期，请提前准备资金。', 'VIEW', 'MEDIUM', '票据到期提醒'),

-- 业务通知模板
('tpl-301', 'BUSINESS_BILL_CREATED', '票据开立通知', 'BUSINESS', 'BILL', '新票据开立', '企业${enterprise}开立了新票据，金额：${amount}，到期日：${dueDate}', 'VIEW', 'NORMAL', '票据开立成功通知'),
('tpl-302', 'BUSINESS_BILL_ENDORSED', '票据背书通知', 'BUSINESS', 'BILL', '票据背书转让通知', '票据${billId}已背书转让给您，转让方：${endorser}，金额：${amount}', 'VIEW', 'HIGH', '票据背书接收通知'),
('tpl-303', 'BUSINESS_RECEIVABLE_CONFIRMED', '应收账款确认通知', 'BUSINESS', 'RECEIVABLE', '应收账款确认', '您的应收账款${receivableId}已被付款方确认，金额：${amount}', 'VIEW', 'NORMAL', '应收账款确认通知'),
('tpl-304', 'BUSINESS_EWR_REGISTERED', '仓单注册通知', 'BUSINESS', 'EWR', '电子仓单注册成功', '您的电子仓单${ewrId}已成功注册，仓库：${warehouse}，货物：${goods}', 'VIEW', 'NORMAL', '仓单注册成功通知'),
('tpl-305', 'BUSINESS_PLEDGE_CREATED', '质押申请通知', 'BUSINESS', 'PLEDGE', '质押申请已提交', '您的质押申请已提交，质押物：${pledgeAsset}，申请金额：${amount}', 'VIEW', 'NORMAL', '质押申请提交通知'),
('tpl-306', 'BUSINESS_INVESTMENT_SUCCESS', '投资成功通知', 'BUSINESS', 'INVESTMENT', '票据投资成功', '您成功投资票据${billId}，投资金额：${amount}，预期收益：${expectedReturn}', 'VIEW', 'NORMAL', '票据投资成功通知'),

-- 预警通知模板
('tpl-401', 'WARNING_CREDIT_USAGE', '信用额度使用预警', 'WARNING', 'CREDIT', '信用额度使用预警', '您的信用额度${creditLimitId}使用率已达${level}级预警（${usageRate}%），请注意控制使用！', 'VIEW', 'HIGH', '信用额度使用率预警通知'),
('tpl-402', 'WARNING_BILL_DISCOUNT_RATE', '贴现率异常预警', 'WARNING', 'BILL', '贴现率异常预警', '票据${billId}的贴现率${rate}超出正常范围，请仔细核对！', 'VIEW', 'MEDIUM', '贴现率异常预警通知'),

-- 提醒通知模板
('tpl-501', 'REMIND_PAYMENT_DUE', '还款到期提醒', 'REMINDER', 'PAYMENT', '还款到期提醒', '您的${businessType}${businessId}将于${dueDays}天后到期还款，还款金额：${amount}', 'VIEW', 'HIGH', '还款到期提醒通知'),
('tpl-502', 'REMIND_DOCUMENT_EXPIRE', '文档到期提醒', 'REMINDER', 'DOCUMENT', '文档到期提醒', '您的文档${documentName}将于${expireDays}天后到期，请及时续期。', 'VIEW', 'MEDIUM', '文档到期提醒通知');
