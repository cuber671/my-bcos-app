-- =====================================================
-- 企业用户名登录功能 - 数据库升级脚本
-- 版本: v2.0
-- 日期: 2026-01-19
-- 说明: 添加username/email/phone字段，支持用户名+密码登录
-- =====================================================

-- 1. 添加username字段（用于登录）
ALTER TABLE enterprise
ADD COLUMN username VARCHAR(100) NULL UNIQUE COMMENT '用户名（用于登录）'
AFTER credit_code;

-- 2. 添加email字段（用于登录和通知）
ALTER TABLE enterprise
ADD COLUMN email VARCHAR(150) NULL UNIQUE COMMENT '企业邮箱（用于登录和通知）'
AFTER username;

-- 3. 添加phone字段（用于登录和通知）
ALTER TABLE enterprise
ADD COLUMN phone VARCHAR(20) NULL UNIQUE COMMENT '企业联系电话（用于登录和通知）'
AFTER email;

-- 4. 创建索引提升查询性能
CREATE INDEX idx_username ON enterprise(username);
CREATE INDEX idx_email ON enterprise(email);
CREATE INDEX idx_phone ON enterprise(phone);

-- 5. 为已存在的企业生成默认username（可选）
-- 使用"enterprise_"前缀 + 企业ID作为默认username
UPDATE enterprise
SET username = CONCAT('enterprise_', LPAD(id, 6, '0'))
WHERE username IS NULL;

-- 6. 添加注释
ALTER TABLE enterprise
MODIFY COLUMN username VARCHAR(100) NOT NULL COMMENT '用户名（用于登录，必填）';

-- 7. 验证数据
SELECT
    id,
    name,
    username,
    email,
    phone,
    credit_code,
    address
FROM enterprise
LIMIT 5;

-- =====================================================
-- 回滚脚本（如需回滚，请执行以下语句）
-- =====================================================

-- DROP INDEX idx_phone ON enterprise;
-- DROP INDEX idx_email ON enterprise;
-- DROP INDEX idx_username ON enterprise;
-- ALTER TABLE enterprise DROP COLUMN phone;
-- ALTER TABLE enterprise DROP COLUMN email;
-- ALTER TABLE enterprise DROP COLUMN username;
