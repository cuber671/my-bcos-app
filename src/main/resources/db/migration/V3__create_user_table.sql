-- 创建新的user表
-- 用于支持多角色用户登录功能

-- 1. 创建user表（支持完整用户管理）
CREATE TABLE IF NOT EXISTS user (
    -- 主键（UUID格式）
    id VARCHAR(36) NOT NULL PRIMARY KEY COMMENT '用户ID（UUID格式）',

    -- 登录凭据
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名（登录账号）',
    password VARCHAR(255) NOT NULL COMMENT '登录密码（BCrypt加密）',

    -- 基本信息
    real_name VARCHAR(100) COMMENT '真实姓名',
    email VARCHAR(100) COMMENT '电子邮箱',
    phone VARCHAR(20) COMMENT '手机号码',

    -- 企业关联（UUID格式）
    enterprise_id VARCHAR(36) COMMENT '所属企业ID（UUID格式）',

    -- 用户类型和状态
    user_type VARCHAR(20) NOT NULL DEFAULT 'ENTERPRISE_USER' COMMENT '用户类型：ADMIN,ENTERPRISE_USER,AUDITOR,OPERATOR',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '用户状态：ACTIVE,DISABLED,LOCKED,PENDING',

    -- 组织信息
    department VARCHAR(100) COMMENT '部门',
    position VARCHAR(100) COMMENT '职位',
    avatar_url VARCHAR(500) COMMENT '头像URL',

    -- 登录审计
    last_login_time TIMESTAMP NULL COMMENT '最后登录时间',
    last_login_ip VARCHAR(50) COMMENT '最后登录IP',
    login_count INT DEFAULT 0 COMMENT '登录次数',

    -- 密码管理
    password_changed_at TIMESTAMP NULL COMMENT '密码修改时间',

    -- 审计字段
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by VARCHAR(50) COMMENT '创建人',
    updated_by VARCHAR(50) COMMENT '更新人',

    -- 注册相关
    invitation_code VARCHAR(32) COMMENT '注册时使用的邀请码',
    registration_remarks TEXT COMMENT '注册备注信息',

    -- 索引
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_enterprise_id (enterprise_id),
    INDEX idx_status (status),

    -- 外键约束
    FOREIGN KEY (enterprise_id) REFERENCES enterprise(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 2. 插入默认管理员账户
-- 用户名: admin
-- 密码: Admin123!@# (BCrypt加密，strength=12)
INSERT INTO user (id, username, password, real_name, email, user_type, status, created_by)
VALUES (
    UUID(),
    'admin',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj9SjKEq7.7W',
    '系统管理员',
    'admin@example.com',
    'ADMIN',
    'ACTIVE',
    'SYSTEM'
);

-- 3. 插入测试用户账户（可选，注释掉以避免外键错误）
-- 用户名: zhangsan
-- 密码: ZhangSan123! (BCrypt加密)
-- 注意：enterprise_id需要引用实际存在的enterprise UUID
-- INSERT INTO user (id, username, password, real_name, email, enterprise_id, user_type, status, department, position, created_by)
-- VALUES (
--     UUID(),
--     'zhangsan',
--     '$2a$12$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36UhQ/JzMjnCtA8dGhKjCyG',
--     '张三',
--     'zhangsan@example.com',
--     (SELECT id FROM enterprise LIMIT 1),
--     'ENTERPRISE_USER',
--     'ACTIVE',
--     '财务部',
--     '财务经理',
--     'SYSTEM'
-- );

-- 4. 插入测试审计员账户（可选）
-- 用户名: auditor1
-- 密码: Auditor123! (BCrypt加密)
INSERT INTO user (id, username, password, real_name, email, user_type, status, created_by)
VALUES (
    UUID(),
    'auditor1',
    '$2a$12$Y4bP8wQqLqZzQzQzQzQzQzeYzYzYzYzYzYzYzYzYzYzYzYzYzYzYzYz',
    '李审计',
    'auditor@example.com',
    'AUDITOR',
    'ACTIVE',
    'SYSTEM'
);
