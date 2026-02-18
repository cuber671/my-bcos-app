-- 将user表的主键从BIGINT改为UUID
-- 这个迁移脚本将user表结构与实体类同步

-- 步骤1: 备份表
CREATE TABLE IF NOT EXISTS user_backup AS SELECT * FROM user;

-- 步骤2: 删除外键约束（如果有）
ALTER TABLE user DROP FOREIGN KEY IF EXISTS fk_user_enterprise;

-- 步骤3: 修改enterprise_id列类型为VARCHAR(36)以匹配UUID格式
ALTER TABLE user MODIFY COLUMN enterprise_id VARCHAR(36) COMMENT '所属企业ID（UUID）';

-- 步骤4: 添加新的UUID列
ALTER TABLE user ADD COLUMN new_id VARCHAR(36);

-- 步骤5: 为所有现有用户生成UUID
UPDATE user SET new_id = UUID() WHERE new_id IS NULL;

-- 步骤6: 删除旧主键约束
ALTER TABLE user DROP PRIMARY KEY;

-- 步骤7: 设置新主键
ALTER TABLE user ADD PRIMARY KEY (new_id);

-- 步骤8: 重命名列（保留旧ID作为old_id备份）
ALTER TABLE user CHANGE COLUMN id old_id BIGINT;
ALTER TABLE user CHANGE COLUMN new_id id VARCHAR(36);

-- 步骤9: 删除old_id列（确认无误后执行）
-- ALTER TABLE user DROP COLUMN old_id;

-- 步骤10: 重建外键约束
ALTER TABLE user ADD CONSTRAINT fk_user_enterprise
    FOREIGN KEY (enterprise_id) REFERENCES enterprise(id) ON DELETE SET NULL;

-- 步骤11: 更新索引（如果需要）
-- DROP INDEX idx_enterprise_id ON user;
-- CREATE INDEX idx_enterprise_id ON user(enterprise_id);
