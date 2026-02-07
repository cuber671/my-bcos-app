# 登录相关数据库表说明

## 📊 当前状态

### ⚠️ 问题：数据库表结构与Java实体类不匹配

**数据库现状：**
- 有 `enterprise` 表（**缺少** `password` 和 `api_key` 字段）
- 有旧的 `sys_user` 表（功能简单）
- **缺少** 新的 `user` 表（支持多角色用户）

**Java实体类：**
- ✅ `Enterprise.java` - 需要 `password` 和 `api_key` 字段
- ✅ `User.java` - 需要完整的用户表支持

---

## 📋 需要的数据库表

### 1. enterprise 表（企业表）

**当前结构** - 需要添加字段：
```sql
-- 现有字段
id BIGINT AUTO_INCREMENT PRIMARY KEY
address VARCHAR(42) NOT NULL UNIQUE  -- 区块链地址
name VARCHAR(255) NOT NULL           -- 企业名称
credit_code VARCHAR(50) NOT NULL     -- 统一社会信用代码
role VARCHAR(20) NOT NULL            -- 企业角色
status VARCHAR(20) NOT NULL          -- 企业状态
credit_rating INT                    -- 信用评级
credit_limit DECIMAL(20,2)           -- 授信额度

-- ❌ 缺少以下字段：
password VARCHAR(255)                -- 登录密码（BCrypt加密）
api_key VARCHAR(64)                  -- API密钥（64位十六进制）
```

### 2. user 表（用户表） - **新表，需要创建**

```sql
CREATE TABLE user (
    -- 主键
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',

    -- 登录凭据
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名（登录账号）',
    password VARCHAR(255) NOT NULL COMMENT '登录密码（BCrypt加密）',

    -- 基本信息
    real_name VARCHAR(100) COMMENT '真实姓名',
    email VARCHAR(100) COMMENT '电子邮箱',
    phone VARCHAR(20) COMMENT '手机号码',

    -- 企业关联
    enterprise_id BIGINT COMMENT '所属企业ID',

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

    -- 索引
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_enterprise_id (enterprise_id),
    INDEX idx_status (status),
    FOREIGN KEY (enterprise_id) REFERENCES enterprise(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
```

---

## 🔧 数据库迁移脚本

### 迁移1：为enterprise表添加登录字段

```sql
-- 文件: V2__add_enterprise_login_fields.sql

-- 1. 为enterprise表添加password字段
ALTER TABLE enterprise
ADD COLUMN password VARCHAR(255) COMMENT '登录密码（BCrypt加密）' AFTER credit_limit;

-- 2. 为enterprise表添加api_key字段
ALTER TABLE enterprise
ADD COLUMN api_key VARCHAR(64) UNIQUE COMMENT 'API密钥（用于程序化访问）' AFTER password;

-- 3. 添加索引
CREATE INDEX idx_api_key ON enterprise(api_key);
```

### 迁移2：创建新的user表

```sql
-- 文件: V3__create_user_table.sql

-- 1. 创建新的user表（支持多角色用户）
CREATE TABLE IF NOT EXISTS user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名（登录账号）',
    password VARCHAR(255) NOT NULL COMMENT '登录密码（BCrypt加密）',
    real_name VARCHAR(100) COMMENT '真实姓名',
    email VARCHAR(100) COMMENT '电子邮箱',
    phone VARCHAR(20) COMMENT '手机号码',
    enterprise_id BIGINT COMMENT '所属企业ID',
    user_type VARCHAR(20) NOT NULL DEFAULT 'ENTERPRISE_USER' COMMENT '用户类型',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '用户状态',
    department VARCHAR(100) COMMENT '部门',
    position VARCHAR(100) COMMENT '职位',
    avatar_url VARCHAR(500) COMMENT '头像URL',
    last_login_time TIMESTAMP NULL COMMENT '最后登录时间',
    last_login_ip VARCHAR(50) COMMENT '最后登录IP',
    login_count INT DEFAULT 0 COMMENT '登录次数',
    password_changed_at TIMESTAMP NULL COMMENT '密码修改时间',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by VARCHAR(50) COMMENT '创建人',
    updated_by VARCHAR(50) COMMENT '更新人',

    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_enterprise_id (enterprise_id),
    INDEX idx_status (status),
    FOREIGN KEY (enterprise_id) REFERENCES enterprise(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 2. 插入默认管理员账户（密码: Admin123!@#，BCrypt加密）
INSERT INTO user (username, password, real_name, email, user_type, status, created_by)
VALUES (
    'admin',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj9SjKEq7.7W',
    '系统管理员',
    'admin@example.com',
    'ADMIN',
    'ACTIVE',
    'SYSTEM'
);
```

---

## 📊 表结构对比

### 企业登录（enterprise表）

| 字段 | 类型 | 说明 | 是否已存在 |
|------|------|------|-----------|
| id | BIGINT | 主键 | ✅ |
| address | VARCHAR(42) | 区块链地址（登录用） | ✅ |
| password | VARCHAR(255) | 登录密码（BCrypt） | ❌ 需要添加 |
| api_key | VARCHAR(64) | API密钥 | ❌ 需要添加 |
| name | VARCHAR(255) | 企业名称 | ✅ |
| role | VARCHAR(20) | 企业角色 | ✅ |
| status | VARCHAR(20) | 企业状态 | ✅ |

### 用户登录（user表）

| 字段 | 类型 | 说明 | 表名 |
|------|------|------|------|
| id | BIGINT | 主键 | user (新) |
| username | VARCHAR(50) | 用户名（登录用） | user (新) |
| password | VARCHAR(255) | 登录密码（BCrypt） | user (新) |
| real_name | VARCHAR(100) | 真实姓名 | user (新) |
| enterprise_id | BIGINT | 所属企业ID | user (新) |
| user_type | VARCHAR(20) | 用户类型 | user (新) |
| status | VARCHAR(20) | 用户状态 | user (新) |
| department | VARCHAR(100) | 部门 | user (新) |
| position | VARCHAR(100) | 职位 | user (新) |
| last_login_time | TIMESTAMP | 最后登录时间 | user (新) |
| login_count | INT | 登录次数 | user (新) |

---

## 🚀 执行迁移步骤

### 步骤1：创建迁移脚本文件

创建文件：`src/main/resources/db/migration/V2__add_enterprise_login_fields.sql`

```sql
-- 为enterprise表添加登录相关字段
ALTER TABLE enterprise
ADD COLUMN password VARCHAR(255) COMMENT '登录密码（BCrypt加密）' AFTER credit_limit,
ADD COLUMN api_key VARCHAR(64) UNIQUE COMMENT 'API密钥（用于程序化访问）' AFTER password;

CREATE INDEX idx_api_key ON enterprise(api_key);
```

### 步骤2：创建user表

创建文件：`src/main/resources/db/migration/V3__create_user_table.sql`

```sql
-- 创建新的user表
CREATE TABLE IF NOT EXISTS user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名（登录账号）',
    password VARCHAR(255) NOT NULL COMMENT '登录密码（BCrypt加密）',
    real_name VARCHAR(100) COMMENT '真实姓名',
    email VARCHAR(100) COMMENT '电子邮箱',
    phone VARCHAR(20) COMMENT '手机号码',
    enterprise_id BIGINT COMMENT '所属企业ID',
    user_type VARCHAR(20) NOT NULL DEFAULT 'ENTERPRISE_USER' COMMENT '用户类型',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '用户状态',
    department VARCHAR(100) COMMENT '部门',
    position VARCHAR(100) COMMENT '职位',
    avatar_url VARCHAR(500) COMMENT '头像URL',
    last_login_time TIMESTAMP NULL COMMENT '最后登录时间',
    last_login_ip VARCHAR(50) COMMENT '最后登录IP',
    login_count INT DEFAULT 0 COMMENT '登录次数',
    password_changed_at TIMESTAMP NULL COMMENT '密码修改时间',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by VARCHAR(50) COMMENT '创建人',
    updated_by VARCHAR(50) COMMENT '更新人',

    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_enterprise_id (enterprise_id),
    INDEX idx_status (status),
    FOREIGN KEY (enterprise_id) REFERENCES enterprise(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 插入默认管理员账户
INSERT INTO user (username, password, real_name, email, user_type, status, created_by)
VALUES (
    'admin',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj9SjKEq7.7W',
    '系统管理员',
    'admin@example.com',
    'ADMIN',
    'ACTIVE',
    'SYSTEM'
);
```

### 步骤3：重启应用

应用启动时，Flyway会自动执行迁移脚本：
```bash
mvn spring-boot:run
```

### 步骤4：验证表结构

```sql
-- 查看enterprise表结构
DESCRIBE enterprise;

-- 查看user表结构
DESCRIBE user;

-- 验证管理员账户已创建
SELECT id, username, real_name, user_type, status FROM user WHERE username = 'admin';
```

---

## 📝 测试登录

### 测试1：管理员登录

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "Admin123!@#"
  }'
```

**预期响应：**
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "...",
    "userType": "ADMIN",
    "realName": "系统管理员"
  }
}
```

### 测试2：创建企业用户

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Authorization: Bearer {admin_token}" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "zhangsan",
    "password": "ZhangSan123!",
    "realName": "张三",
    "email": "zhangsan@example.com",
    "enterpriseId": 1,
    "userType": "ENTERPRISE_USER",
    "department": "财务部",
    "position": "财务经理"
  }'
```

### 测试3：用户登录

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "zhangsan",
    "password": "ZhangSan123!"
  }'
```

---

## 🔐 密码加密说明

### BCrypt密码生成

系统中所有密码都使用BCrypt加密存储：

```java
// 加密密码
String encodedPassword = PasswordUtil.encode("rawPassword");

// 验证密码
boolean matches = PasswordUtil.matches("rawPassword", encodedPassword);
```

### BCrypt特性

- ✅ 自动加盐（每次加密结果不同）
- ✅ 强度12（推荐值）
- ✅ 单向加密（不可解密）
- ✅ 验证时通过匹配判断

### 示例

```
原始密码: "Admin123!@#"
BCrypt加密后: "$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj9SjKEq7.7W"

注意：每次加密同样的密码，结果都会不同（因为随机盐）
```

---

## 📊 数据流示意图

```
┌─────────────────────────────────────────────────────────────┐
│                        登录数据流                            │
└─────────────────────────────────────────────────────────────┘

企业登录流程：
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ 企业提交     │ -> │ enterprise   │ -> │ JWT令牌      │
│ address+pwd  │    │ 表验证密码   │    │ 返回给客户端  │
└──────────────┘    └──────────────┘    └──────────────┘

用户登录流程：
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ 用户提交     │ -> │ user         │ -> │ JWT令牌      │
│ username+pwd │    │ 表验证密码   │    │ 返回给客户端  │
└──────────────┘    └──────────────┘    └──────────────┘

数据表关系：
enterprise (企业表)
    ├─ address (登录用)
    ├─ password (BCrypt加密)
    └─ api_key (API认证)

user (用户表)
    ├─ username (登录用)
    ├─ password (BCrypt加密)
    └─ enterprise_id (外键 -> enterprise.id)
```

---

## ⚠️ 注意事项

### 1. 表名冲突

问题：SQL关键字 `user` 作为表名

解决方案：
- ✅ MySQL中可以用反引号：`CREATE TABLE user`
- ✅ 或者使用其他表名：`sys_user`, `app_user`
- ✅ 当前实现使用 `user`，JPA会自动处理

### 2. 密码安全

- ✅ 永远不要在日志中输出明文密码
- ✅ 密码字段不要在API响应中返回
- ✅ 使用强密码策略
- ✅ 定期更换密码

### 3. API密钥安全

- ✅ 生成足够长的随机密钥（64位十六进制）
- ✅ 妥善保管，不要泄露
- ✅ 可以随时重置
- ✅ 在日志中掩码显示

---

## 📝 总结

### 当前状态

| 表名 | 状态 | 说明 |
|------|------|------|
| enterprise | ⚠️ 需要迁移 | 缺少password和api_key字段 |
| user | ❌ 需要创建 | 完整的用户表（多角色支持） |
| sys_user | ⚠️ 旧表 | 简单用户表，功能有限 |

### 下一步

1. ✅ 创建V2迁移脚本 - 添加enterprise登录字段
2. ✅ 创建V3迁移脚本 - 创建user表
3. ✅ 重启应用执行迁移
4. ✅ 测试企业登录功能
5. ✅ 测试用户登录功能
6. ✅ 验证权限控制

---

**文档版本：** v1.0
**最后更新：** 2026-01-16
**相关文档：**
- [ENTERPRISE_LOGIN_GUIDE.md](ENTERPRISE_LOGIN_GUIDE.md) - 企业登录指南
- [USER_LOGIN_GUIDE.md](USER_LOGIN_GUIDE.md) - 用户登录指南
- [PERMISSION_AND_AUTH_SYSTEM.md](PERMISSION_AND_AUTH_SYSTEM.md) - 权限体系说明
