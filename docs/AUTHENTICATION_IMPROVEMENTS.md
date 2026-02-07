# JWT认证机制改进说明

## 📋 问题回顾

### ❌ 之前的实现（存在严重安全漏洞）

```java
POST /api/auth/login
{
  "address": "0x1234567890abcdef1234567890abcdef12345678"
}

// 任何人只要知道地址就能获取JWT令牌
```

**问题：**
1. 没有身份验证，任何人都可以冒充其他企业
2. 无法证明请求者真的是该地址的所有者
3. 存在严重的安全风险

---

## ✅ 改进后的实现

### 1. 添加密码字段

在`Enterprise`实体中添加：
- `password` - 登录密码（BCrypt加密存储）
- `apiKey` - API密钥（用于程序化访问）

### 2. 三种认证方式

#### 方式1：密码登录（推荐）

```bash
POST /api/auth/login
Content-Type: application/json

{
  "address": "0x1234567890abcdef1234567890abcdef12345678",
  "password": "your_password"
}
```

**响应：**
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "type": "Bearer",
    "address": "0x1234567890abcdef1234567890abcdef12345678",
    "enterpriseName": "供应商A",
    "role": "SUPPLIER"
  }
}
```

#### 方式2：API密钥认证（用于系统间调用）

```bash
POST /api/auth/api-key
Content-Type: application/json

{
  "apiKey": "a1b2c3d4e5f6...1234567890abcdef"
}
```

**响应：**
```json
{
  "code": 200,
  "message": "认证成功",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "type": "Bearer",
    "address": "0x1234567890abcdef1234567890abcdef12345678",
    "enterpriseName": "供应商A",
    "role": "SUPPLIER"
  }
}
```

---

## 🔐 安全特性

### 1. 密码加密

```java
// 使用BCrypt算法（强度12）
String encodedPassword = PasswordUtil.encode(rawPassword);

// 验证密码
boolean matches = PasswordUtil.matches(rawPassword, encodedPassword);
```

**特点：**
- BCrypt自动加盐
- 每次加密结果都不同
- 抗彩虹表攻击
- 计算密集，防暴力破解

### 2. API密钥管理

```java
// 生成64位随机API密钥
String apiKey = PasswordUtil.generateApiKey();
// 示例: a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6a7b8c9d0e1f2
```

**特点：**
- 64位十六进制字符串
- 随机生成，不可猜测
- 适用于程序化访问
- 可以随时重置

### 3. 敏感信息保护

```java
// 注册时自动生成
enterprise.setPassword(encodedPassword);
enterprise.setApiKey(apiKey);

// 返回时隐藏敏感信息
saved.setPassword(null);
saved.setApiKey(null);
```

---

## 🚀 使用场景

### 场景1：企业用户登录（Web/移动端）

使用**密码登录**方式：

```bash
# 1. 登录获取令牌
POST /api/auth/login
{
  "address": "0x123...",
  "password": "user_password"
}

# 2. 使用令牌访问API
POST /api/bill
Authorization: Bearer {token}
{...}
```

### 场景2：系统间调用（后端服务）

使用**API密钥**方式：

```bash
# 1. 使用API密钥获取令牌
POST /api/auth/api-key
{
  "apiKey": "a1b2c3d4..."
}

# 2. 使用令牌进行后续调用
```

### 场景3：管理员初始化账户

```java
// 注册企业时设置初始密码
enterpriseService.registerEnterprise(enterprise, "InitialPassword123!");

// 或后续设置密码
enterpriseService.setEnterprisePassword(address, "NewPassword456!");
```

---

## 📝 数据库迁移

需要添加两个字段到`enterprise`表：

```sql
-- 添加密码字段
ALTER TABLE enterprise
ADD COLUMN password VARCHAR(255) COMMENT '登录密码（BCrypt加密）';

-- 添加API密钥字段
ALTER TABLE enterprise
ADD COLUMN api_key VARCHAR(64) UNIQUE COMMENT 'API密钥（用于程序化访问）';

-- 为现有企业生成密码和API密钥
UPDATE enterprise
SET password = '$2a$12$...'  -- 使用BCrypt加密的密码
WHERE password IS NULL;

UPDATE enterprise
SET api_key = '...'  -- 随机生成的64位密钥
WHERE api_key IS NULL;
```

---

## 🛠️ API密钥管理

### 生成新API密钥

```java
// 通过服务方法
String newApiKey = enterpriseService.resetApiKey(address);
```

### 查看API密钥（仅首次）

企业注册时会返回API密钥，请妥善保存：

```json
{
  "id": 1,
  "address": "0x123...",
  "name": "供应商A",
  "apiKey": "a1b2c3d4e5f6...",  // 仅在注册时返回
  "status": "PENDING"
}
```

**提示：**
- API密钥只显示一次，请立即保存
- 如丢失，需联系管理员重置
- 建议定期轮换API密钥

---

## ⚠️ 安全最佳实践

### 1. 密码要求

- 最少8个字符
- 包含大小写字母、数字
- 建议使用特殊字符
- 不要使用常见密码

### 2. API密钥使用

- 仅用于后端服务间调用
- 不要存储在前端代码中
- 通过环境变量或配置管理工具存储
- 定期轮换（建议每季度）

### 3. 日志安全

```java
// 不要在日志中记录敏感信息
log.info("User login: address={}", address);  // ✅ 正确
log.info("User login: password={}", password); // ❌ 错误

// API密钥掩码显示
log.info("API key: {}", maskApiKey(apiKey));  // a1b2****c3d4
```

### 4. 令牌使用

- JWT令牌有效期24小时
- 过期后需要重新登录
- 令牌存储在安全位置（HttpOnly Cookie或内存）
- 使用HTTPS传输

---

## 📊 对比总结

| 特性 | 旧版本 | 新版本 |
|------|--------|--------|
| 身份验证 | ❌ 无 | ✅ 密码验证 |
| 程序化访问 | ❌ 无 | ✅ API密钥 |
| 密码加密 | N/A | ✅ BCrypt |
| 安全性 | 🔴 极低 | 🟢 高 |
| 适用场景 | ❌ 不可用 | ✅ 生产环境 |

---

## 🔧 故障排查

### 问题1：登录提示"账户未设置密码"

**原因**：企业账户是在旧版本创建的，没有密码

**解决方法**：
```bash
# 管理员设置初始密码
POST /api/admin/set-password
{
  "address": "0x123...",
  "password": "NewPassword123!"
}
```

### 问题2：API密钥丢失

**原因**：API密钥只在注册时显示一次

**解决方法**：
```bash
# 重置API密钥
POST /api/admin/reset-api-key
{
  "address": "0x123..."
}

# 返回新的API密钥
{
  "apiKey": "new_api_key_here"  # 请立即保存
}
```

### 问题3：密码验证失败

**原因**：
- 密码错误
- 企业账户未激活
- 数据库中password字段为NULL

**解决方法**：
1. 确认密码正确
2. 联系管理员激活账户
3. 管理员设置初始密码

---

## 📚 相关文档

- [JWT使用指南](./JWT_AUTH_GUIDE.md) - JWT配置和使用
- [数据库设计](./docs/DATABASE_SCHEMA.md) - 数据库表结构
- [API文档](./swagger-ui.html) - 完整API文档

---

## ✅ 总结

改进后的认证机制：

1. ✅ **安全** - 使用密码和API密钥双重验证
2. ✅ **灵活** - 支持多种认证方式
3. ✅ **生产就绪** - 符合行业安全标准
4. ✅ **易于管理** - 提供完整的密钥管理功能

**建议立即升级到新版本！**
