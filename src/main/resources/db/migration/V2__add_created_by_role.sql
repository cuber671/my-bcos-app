-- 添加 created_by_role 字段到 enterprise 表
-- 用于记录创建企业的角色类型（ADMIN, AUDITOR, SELF_REGISTER, ENTERPRISE_USER）

ALTER TABLE enterprise
ADD COLUMN created_by_role VARCHAR(30) DEFAULT 'SELF_REGISTER' COMMENT '创建者角色类型: ADMIN, AUDITOR, SUPER_ADMIN, SELF_REGISTER, ENTERPRISE_USER';

-- 添加索引以优化查询
CREATE INDEX idx_created_by_role ON enterprise(created_by_role);

-- 更新历史数据的created_by_role字段
-- 管理员创建的
UPDATE enterprise e
SET created_by_role = (
    SELECT COALESCE(a.role, 'SELF_REGISTER')
    FROM admin a
    WHERE a.username = e.created_by
)
WHERE e.created_by != 'SELF_REGISTER';

-- 自主注册的
UPDATE enterprise
SET created_by_role = 'SELF_REGISTER'
WHERE created_by = 'SELF_REGISTER';
