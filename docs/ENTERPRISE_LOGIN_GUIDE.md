# 企业登录使用指南

## ✅ 是的！系统完全支持企业身份登录

系统提供**企业级认证功能**，支持企业使用区块链地址和密码登录。

---

## 🎯 支持的企业角色

系统支持以下4种企业角色登录：

| 企业角色 | 英文标识 | 说明 | 业务权限 |
|---------|---------|------|---------|
| **供应商** | `SUPPLIER` | 提供商品/服务的企业 | 创建应收账款、转让账款 |
| **核心企业** | `CORE_ENTERPRISE` | 大型采购商 | 确认应收账款、确认付款 |
| **金融机构** | `FINANCIAL_INSTITUTION` | 银行、保理公司 | 提供融资、管理授信 |
| **监管机构** | `REGULATOR` | 政府监管部门 | 查看所有交易、监控风险 |

---

## 🔐 企业登录方式

### 方式1：区块链地址 + 密码登录

**端点：** `POST /api/auth/enterprise-login`

**请求示例：**
```bash
curl -X POST http://localhost:8080/api/auth/enterprise-login \
  -H "Content-Type: application/json" \
  -d '{
    "address": "0x1234567890abcdef1234567890abcdef12345678",
    "password": "EnterprisePassword123!"
  }'
```

**请求体格式：**
```json
{
  "address": "区块链地址（42字符，0x开头）",
  "password": "企业密码"
}
```

**成功响应示例：**
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIweDEyMzQ1Njc4OTAiLCJpYXQiOjE2...",
    "type": "Bearer",
    "address": "0x1234567890abcdef1234567890abcdef12345678",
    "enterpriseName": "供应商A",
    "role": "SUPPLIER",
    "loginType": "ENTERPRISE"
  }
}
```

**失败响应示例：**

1. **企业未激活：**
```json
{
  "code": 500,
  "message": "企业账户未激活，请联系管理员"
}
```

2. **密码错误：**
```json
{
  "code": 500,
  "message": "密码错误"
}
```

3. **企业不存在：**
```json
{
  "code": 500,
  "message": "企业账户不存在，请先注册"
}
```

4. **未设置密码：**
```json
{
  "code": 500,
  "message": "账户未设置密码，请联系管理员初始化"
}
```

---

### 方式2：API密钥认证（程序化访问）

**端点：** `POST /api/auth/api-key`

**适用场景：**
- 系统间API调用
- 自动化脚本
- 第三方集成

**请求示例：**
```bash
curl -X POST http://localhost:8080/api/auth/api-key \
  -H "Content-Type: application/json" \
  -d '{
    "apiKey": "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6a7b8c9d0e1f2"
  }'
```

**请求体格式：**
```json
{
  "apiKey": "64位十六进制API密钥"
}
```

**成功响应示例：**
```json
{
  "code": 200,
  "message": "认证成功",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIweDEyMzQ1Njc4OTAiLCJpYXQiOjE2...",
    "type": "Bearer",
    "address": "0x1234567890abcdef1234567890abcdef12345678",
    "enterpriseName": "供应商A",
    "role": "SUPPLIER",
    "loginType": "API_KEY"
  }
}
```

---

## 📋 使用JWT令牌访问API

登录成功后，使用返回的`token`访问受保护的API：

### 设置Authorization头

```bash
# 使用企业token访问API
curl -X GET http://localhost:8080/api/enterprise/0x1234...abcd \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."
```

### 示例：企业查看自己的信息

```bash
# 供应商查看应收账款列表
curl -X GET http://localhost:8080/api/receivables/supplier/0x1234...abcd \
  -H "Authorization: Bearer {token}"
```

---

## 🎭 不同角色企业登录示例

### 示例1：供应商登录

```json
// POST /api/auth/enterprise-login
{
  "address": "0x1234567890abcdef1234567890abcdef12345678",
  "password": "SupplierPass123!"
}

// 响应
{
  "address": "0x1234567890abcdef1234567890abcdef12345678",
  "enterpriseName": "供应商A",
  "role": "SUPPLIER"
}
```

**供应商权限：**
- ✅ 创建应收账款
- ✅ 查看自己的应收账款
- ✅ 转让应收账款
- ✅ 申请融资

### 示例2：核心企业登录

```json
// POST /api/auth/enterprise-login
{
  "address": "0xabcd1234567890abcdef1234567890abcdef12",
  "password": "CorePass123!"
}

// 响应
{
  "address": "0xabcd1234567890abcdef1234567890abcdef12",
  "enterpriseName": "核心企业集团",
  "role": "CORE_ENTERPRISE"
}
```

**核心企业权限：**
- ✅ 确认应收账款
- ✅ 查看应付账款
- ✅ 确认付款
- ✅ 查看供应商列表

### 示例3：金融机构登录

```json
// POST /api/auth/enterprise-login
{
  "address": "0x567890abcdef1234567890abcdef1234567890ab",
  "password": "BankPass123!"
}

// 响应
{
  "address": "0x567890abcdef1234567890abcdef1234567890ab",
  "enterpriseName": "XX银行",
  "role": "FINANCIAL_INSTITUTION"
}
```

**金融机构权限：**
- ✅ 查看融资申请
- ✅ 提供融资
- ✅ 查看融资记录
- ✅ 管理授信额度

### 示例4：监管机构登录

```json
// POST /api/auth/enterprise-login
{
  "address": "0x9abcdef0123456789012345678901234567890",
  "password": "RegulatorPass123!"
}

// 响应
{
  "address": "0x9abcdef0123456789012345678901234567890",
  "enterpriseName": "金融监管局",
  "role": "REGULATOR"
}
```

**监管机构权限：**
- ✅ 查看所有交易
- ✅ 监控风险
- ✅ 查看企业信息
- ✅ 生成监管报告

---

## 🔍 企业登录验证流程

系统会执行以下验证步骤：

```
1. 企业提交地址和密码
   ↓
2. EnterpriseService.getEnterprise(address)
   ↓
3. 根据地址查找企业
   - 不存在 → "企业账户不存在，请先注册"
   ↓
4. 检查企业状态
   - PENDING → "企业账户未激活，请联系管理员"
   - SUSPENDED → "企业账户未激活，请联系管理员"
   - BLACKLISTED → "企业账户未激活，请联系管理员"
   - ACTIVE → 继续
   ↓
5. 检查是否设置密码
   - 未设置 → "账户未设置密码，请联系管理员初始化"
   - 已设置 → 继续
   ↓
6. BCrypt验证密码
   - 密码错误 → "密码错误"
   - 密码正确 → 继续
   ↓
7. 生成JWT令牌
   - subject = address (区块链地址)
   - 有效期 = 24小时
   ↓
8. 返回令牌和企业信息
```

---

## 🚫 企业状态限制

### 企业状态说明

| 企业状态 | 是否可登录 | 提示信息 |
|---------|-----------|---------|
| `ACTIVE` | ✅ 可登录 | - |
| `PENDING` | ❌ 不可登录 | "企业账户未激活，请联系管理员" |
| `SUSPENDED` | ❌ 不可登录 | "企业账户未激活，请联系管理员" |
| `BLACKLISTED` | ❌ 不可登录 | "企业账户未激活，请联系管理员" |

### 企业激活流程

```
1. 企业注册（PENDING状态）
   ↓
2. 管理员审核
   ↓
3. 激活企业（变为ACTIVE状态）
   ↓
4. 设置密码
   ↓
5. 企业可以登录
```

---

## 📊 完整使用场景示例

### 场景：供应链金融中的三个参与方

#### 1. 供应商登录并创建应收账款

```bash
# 1. 供应商登录
curl -X POST http://localhost:8080/api/auth/enterprise-login \
  -H "Content-Type: application/json" \
  -d '{
    "address": "0x1234567890abcdef1234567890abcdef12345678",
    "password": "SupplierPass123!"
  }'

# 2. 获取token
TOKEN="eyJhbGciOiJIUzUxMiJ9..."

# 3. 创建应收账款
curl -X POST http://localhost:8080/api/receivables \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "receivableId": "REC20260116001",
    "coreEnterpriseAddress": "0xabcd1234567890abcdef1234567890abcdef12",
    "amount": 100000.00,
    "currency": "CNY",
    "issueDate": "2026-01-16T10:00:00",
    "dueDate": "2026-04-16T10:00:00",
    "description": "1月份货物供应款"
  }'

# 4. 查看创建的应收账款
curl -X GET http://localhost:8080/api/receivables/supplier/0x1234...abcd \
  -H "Authorization: Bearer $TOKEN"
```

#### 2. 核心企业登录并确认应收账款

```bash
# 1. 核心企业登录
curl -X POST http://localhost:8080/api/auth/enterprise-login \
  -H "Content-Type: application/json" \
  -d '{
    "address": "0xabcd1234567890abcdef1234567890abcdef12",
    "password": "CorePass123!"
  }'

# 2. 确认应收账款
curl -X POST http://localhost:8080/api/receivables/REC20260116001/confirm \
  -H "Authorization: Bearer {core_enterprise_token}"
```

#### 3. 金融机构登录并提供融资

```bash
# 1. 金融机构登录（使用API密钥）
curl -X POST http://localhost:8080/api/auth/api-key \
  -H "Content-Type: application/json" \
  -d '{
    "apiKey": "a1b2c3d4e5f6...64字符密钥"
  }'

# 2. 提供融资
curl -X POST http://localhost:8080/api/receivables/REC20260116001/finance \
  -H "Authorization: Bearer {bank_token}" \
  -H "Content-Type: application/json" \
  -d '{
    "financierAddress": "0x567890abcdef1234567890abcdef1234567890ab",
    "financeAmount": 90000.00,
    "financeRate": 5
  }'
```

---

## 🆚 企业登录 vs 用户登录

### 企业登录特点

**优势：**
- ✅ 企业级认证
- ✅ 基于区块链地址
- ✅ 适合系统间API调用
- ✅ 简单直接（一个企业一个账户）
- ✅ 与区块链系统集成

**局限：**
- ⚠️ 一个企业只能有一个账户
- ⚠️ 无法区分企业内部员工
- ⚠️ 缺少细粒度权限控制

### 用户登录特点

**优势：**
- ✅ 支持多用户
- ✅ 细粒度权限控制
- ✅ 用户级别管理
- ✅ 部门和职位信息
- ✅ 完整的审计日志

**局限：**
- ⚠️ 需要先创建用户
- ⚠️ 需要分配企业关联

---

## 🛠️ 企业密码管理

### 设置企业密码

管理员为企业设置初始密码：

```bash
PUT /api/enterprise/{address}/password
Authorization: Bearer {admin_token}
{
  "newPassword": "EnterprisePassword123!"
}
```

### 修改企业密码

企业修改自己的密码：

```bash
PUT /api/enterprise/{address}/change-password
Authorization: Bearer {enterprise_token}
{
  "oldPassword": "OldPassword123!",
  "newPassword": "NewPassword123!"
}
```

### 重置企业密码

管理员重置企业密码：

```bash
PUT /api/enterprise/{address}/reset-password
Authorization: Bearer {admin_token}
{
  "newPassword": "NewPassword123!"
}
```

---

## 🔐 API密钥管理

### 生成API密钥

管理员为企业生成API密钥：

```bash
POST /api/enterprise/{address}/generate-api-key
Authorization: Bearer {admin_token}
```

**响应：**
```json
{
  "code": 200,
  "message": "API密钥生成成功",
  "data": {
    "apiKey": "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6a7b8c9d0e1f2"
  }
}
```

### 重置API密钥

企业重置自己的API密钥：

```bash
POST /api/enterprise/{address}/reset-api-key
Authorization: Bearer {enterprise_token}
```

### 使用API密钥

```bash
# 使用API密钥获取JWT令牌
curl -X POST http://localhost:8080/api/auth/api-key \
  -H "Content-Type: application/json" \
  -d '{"apiKey": "a1b2c3d4..."}'

# 使用返回的JWT令牌调用API
curl -X GET http://localhost:8080/api/receivables/... \
  -H "Authorization: Bearer {token}"
```

---

## 💡 使用建议

### 何时使用企业登录

✅ **推荐使用企业登录的场景：**
1. **企业级应用** - 整个企业作为一个实体
2. **系统间调用** - 后台系统之间的API调用
3. **简单场景** - 企业不需要多用户管理
4. **区块链集成** - 与区块链地址紧密集成
5. **API集成** - 第三方系统集成

❌ **不推荐使用企业登录的场景：**
1. **多员工企业** - 需要区分不同员工
2. **细粒度权限** - 需要角色权限控制
3. **审计要求高** - 需要详细的操作日志
4. **部门管理** - 需要按部门组织用户

### 何时使用用户登录

✅ **推荐使用用户登录的场景：**
1. **企业内部多用户** - 一个企业多个员工
2. **权限管理** - 需要不同角色权限
3. **审计需求** - 需要记录每个员工的操作
4. **部门组织** - 需要按部门管理

---

## 🔒 安全特性

### 已实现的安全措施

1. **密码加密**
   - ✅ BCrypt算法加密（strength=12）
   - ✅ 密码不以明文存储
   - ✅ 密码不在响应中返回

2. **令牌安全**
   - ✅ HS512强加密算法
   - ✅ 24小时有效期
   - ✅ 签名验证防篡改

3. **API密钥安全**
   - ✅ 64位十六进制随机密钥
   - ✅ 可随时重置
   - ✅ 密钥掩码显示（日志中）

4. **企业状态管理**
   - ✅ 激活状态验证
   - ✅ 暂停/拉黑控制
   - ✅ 待审核状态

---

## 📝 总结

✅ **系统完全支持企业身份登录**
- 4种企业角色：SUPPLIER、CORE_ENTERPRISE、FINANCIAL_INSTITUTION、REGULATOR
- 两种登录方式：地址+密码、API密钥
- JWT令牌管理会话
- 企业状态验证
- 密码BCrypt加密

✅ **企业登录适用场景**
- 企业级应用
- 系统间API调用
- 区块链集成
- 简单权限模型

✅ **推荐使用用户登录**
- 支持多用户管理
- 细粒度权限控制
- 完整审计功能

---

**文档版本：** v1.0
**最后更新：** 2026-01-16
**相关文档：**
- [USER_LOGIN_GUIDE.md](USER_LOGIN_GUIDE.md) - 用户登录指南
- [PERMISSION_AND_AUTH_SYSTEM.md](PERMISSION_AND_AUTH_SYSTEM.md) - 权限体系说明
