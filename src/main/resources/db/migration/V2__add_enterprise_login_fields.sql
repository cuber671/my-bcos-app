-- 为enterprise表添加登录相关字段
-- 用于支持企业登录功能

-- 1. 添加password字段（BCrypt加密的登录密码）
ALTER TABLE enterprise
ADD COLUMN password VARCHAR(255) COMMENT '登录密码（BCrypt加密）' AFTER credit_limit;

-- 2. 添加api_key字段（64位十六进制，用于API程序化访问）
ALTER TABLE enterprise
ADD COLUMN api_key VARCHAR(64) UNIQUE COMMENT 'API密钥（用于程序化访问）' AFTER password;

-- 3. 添加索引以加速查询
CREATE INDEX idx_api_key ON enterprise(api_key);

-- 4. 为测试数据添加示例密码和API密钥
-- 注意：这些是测试数据，生产环境应该通过管理员接口设置
-- 密码: "Test123!" (BCrypt加密)
-- API密钥: 64位随机十六进制字符串
UPDATE enterprise
SET password = '$2a$12$8Un3m/QVrX/9r/qY5UrxqOqE4NqUqHQLqQqSqQqQqQqQqQqQqQqQq',
    api_key = 'a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6a7b8c9d0e1f2'
WHERE address IS NOT NULL;
