# 权限和登录体系说明

## 📋 目录

1. [角色体系](#角色体系)
2. [登录方式](#登录方式)
3. [权限控制](#权限控制)
4. [认证流程](#认证流程)
5. [使用场景](#使用场景)

---

## 1. 角色体系

### 🔷 企业角色 (EnterpriseRole)

企业实体的角色定义在业务层面，用于区分企业在供应链金融中的角色类型：

| 角色               | 英文标识                  | 说明                     | 业务权限                                                            |
| ------------------ | ------------------------- | ------------------------ | ------------------------------------------------------------------- |
| **供应商**   | `SUPPLIER`              | 提供商品/服务的企业      | • 创建应收账款`<br>`• 查看自己的应收账款`<br>`• 转让应收账款 |
| **核心企业** | `CORE_ENTERPRISE`       | 大型采购商，承担付款责任 | • 确认应收账款`<br>`• 查看应付账款`<br>`• 确认付款           |
| **金融机构** | `FINANCIAL_INSTITUTION` | 银行、保理公司等         | • 提供融资`<br>`• 查看融资记录`<br>`• 管理授信额度           |
| **监管机构** | `REGULATOR`             | 政府监管部门             | • 查看所有交易`<br>`• 监控风险`<br>`• 审计报告               |

**数据库字段：**

```java
@Column(name = "role", nullable = false, length = 20)
private EnterpriseRole role;
```

---

### 👤 用户角色 (UserType)

用户实体的角色定义在系统层面，用于区分用户在管理系统中的权限：

| 角色                 | 英文标识            | 说明         | 系统权限                                                                         |
| -------------------- | ------------------- | ------------ | -------------------------------------------------------------------------------- |
| **系统管理员** | `ADMIN`           | 平台管理员   | • 管理所有企业`<br>`• 审核企业注册`<br>`• 管理所有用户`<br>`• 系统配置 |
| **企业用户**   | `ENTERPRISE_USER` | 企业员工     | • 操作所属企业业务`<br>`• 查看企业数据`<br>`• 提交业务申请                |
| **审计员**     | `AUDITOR`         | 内部审计人员 | • 查看交易记录`<br>`• 审计合规性`<br>`• 生成审计报告                      |
| **操作员**     | `OPERATOR`        | 系统操作人员 | • 日常业务操作`<br>`• 数据录入`<br>`• 基础查询                            |

**数据库字段：**

```java
@Column(name = "user_type", nullable = false, length = 20)
private UserType userType = UserType.ENTERPRISE_USER;
```

---

## 2. 登录方式

系统支持**三种登录方式**，满足不同场景需求：

### ✅ 方式1：用户名密码登录（推荐）

**适用场景：** 企业内部多用户使用

**端点：** `POST /api/auth/login`

**请求示例：**

```json
{
  "username": "zhangsan",
  "password": "UserPassword123!"
}
```

**响应示例：**

```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ6aGFuZ3NhbiIsImlhdCI6MTY0...",
    "type": "Bearer",
    "userId": 5,
    "username": "zhangsan",
    "realName": "张三",
    "userType": "ENTERPRISE_USER",
    "enterpriseId": 1,
    "department": "财务部",
    "position": "财务经理",
    "loginType": "USER"
  }
}
```

**特点：**

- ✅ 支持一个企业多个用户
- ✅ 细粒度权限控制（用户类型）
- ✅ 用户级别管理（启用/禁用/锁定）
- ✅ 完整的审计日志（登录时间、IP、次数）

**验证流程：**

1. 根据用户名查找用户
2. 检查用户状态（是否被锁定/禁用）
3. 使用BCrypt验证密码
4. 更新最后登录信息
5. 生成JWT令牌（令牌中的subject是username）

---

### ✅ 方式2：企业地址+密码登录（向后兼容）

**适用场景：** 企业级应用、系统间调用

**端点：** `POST /api/auth/enterprise-login`

**请求示例：**

```json
{
  "address": "0x1234567890abcdef1234567890abcdef12345678",
  "password": "EnterprisePassword123!"
}
```

**响应示例：**

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

**特点：**

- ✅ 企业级认证（一个企业一个账户）
- ✅ 基于区块链地址（与区块链系统集成）
- ✅ 适合系统间API调用
- ⚠️ 一个企业只能有一个账户（不够灵活）

**验证流程：**

1. 根据区块链地址查找企业
2. 检查企业状态（是否ACTIVE）
3. 使用BCrypt验证密码
4. 生成JWT令牌（令牌中的subject是address）

---

### ✅ 方式3：API密钥认证（程序化访问）

**适用场景：** 系统间调用、自动化脚本

**端点：** `POST /api/auth/api-key`

**请求示例：**

```json
{
  "apiKey": "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6a7b8c9d0e1f2"
}
```

**响应示例：**

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

**特点：**

- ✅ 无需暴露密码
- ✅ 可随时撤销（重置API密钥）
- ✅ 适合程序化调用
- ⚠️ 需要妥善保管API密钥

**验证流程：**

1. 根据API密钥查找企业
2. 检查企业状态
3. 生成JWT令牌

---

## 3. 权限控制

### 🔐 认证层 (Authentication)

使用 **JWT (JSON Web Token)** 进行无状态认证：

**JWT配置：**

```java
// 算法：HS512
// 密钥长度：至少256位（从环境变量读取）
// 过期时间：24小时
// Claims包含：subject（用户名或企业地址）、iat（签发时间）、exp（过期时间）
```

**JWT过滤器链：**

```
HTTP Request
    ↓
JwtAuthenticationFilter (提取并验证JWT)
    ↓
SecurityContextHolder (设置认证信息)
    ↓
Controller (从SecurityContext获取用户信息)
```

**请求头格式：**

```
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

---

### 🛡️ 授权规则 (Authorization)

**SecurityConfig配置：**

```java
// 1. 公开端点（无需认证）
- /api/auth/**              // 登录、注册
- /api/enterprise/register  // 企业注册
- /swagger-ui/**            // API文档
- 静态资源

// 2. 受保护端点（需要认证）
- /api/**                   // 所有其他API

// 3. 管理员端点（需要ADMIN角色）
- /api/admin/**             // （已预留，待扩展）
```

**当前状态：**

- ✅ 所有 `/api/**` 端点都需要JWT认证
- ⚠️ 暂未实现基于角色的细粒度权限控制（RBAC）
- ⚠️ 企业用户可以访问所有业务端点（未按企业角色区分）

---

### 👥 用户状态控制

**用户状态 (UserStatus)：**

| 状态         | 说明   | 是否可登录                        |
| ------------ | ------ | --------------------------------- |
| `ACTIVE`   | 正常   | ✅ 可登录                         |
| `DISABLED` | 禁用   | ❌ 不可登录（提示"账户已被禁用"） |
| `LOCKED`   | 锁定   | ❌ 不可登录（提示"账户已被锁定"） |
| `PENDING`  | 待审核 | ❌ 不可登录                       |

**企业状态 (EnterpriseStatus)：**

| 状态            | 说明   | 是否可登录  |
| --------------- | ------ | ----------- |
| `ACTIVE`      | 已激活 | ✅ 可登录   |
| `PENDING`     | 待审核 | ❌ 不可登录 |
| `SUSPENDED`   | 已暂停 | ❌ 不可登录 |
| `BLACKLISTED` | 已拉黑 | ❌ 不可登录 |

---

## 4. 认证流程

### 用户登录流程

```
1. 用户提交用户名和密码
   ↓
2. UserController → UserService.validateLogin()
   ↓
3. 检查用户状态（是否锁定/禁用）
   ↓
4. BCrypt验证密码
   ↓
5. 更新最后登录信息（时间、IP、次数）
   ↓
6. JwtTokenProvider.generateToken(username)
   - 使用HS512算法
   - subject = username
   - 有效期24小时
   ↓
7. 返回JWT令牌给客户端
   ↓
8. 客户端后续请求携带 JWT
   - Header: Authorization: Bearer {token}
   ↓
9. JwtAuthenticationFilter拦截请求
   - 提取JWT令牌
   - 验证令牌有效性
   - 从令牌中提取username
   - 创建UserAuthentication对象
   - 设置到SecurityContextHolder
   ↓
10. Controller处理业务逻辑
    - 从SecurityContext获取用户信息
    - authentication.getName() 获取用户名
```

### 企业登录流程

```
1. 企业提交地址和密码
   ↓
2. AuthController → EnterpriseService.getEnterprise()
   ↓
3. 检查企业状态（是否ACTIVE）
   ↓
4. BCrypt验证密码
   ↓
5. JwtTokenProvider.generateToken(address)
   - subject = address (区块链地址)
   ↓
6. 返回JWT令牌
```

---

## 5. 使用场景

### 场景1：供应商企业有3个员工

**企业信息：**

- 企业名称：供应商A
- 企业角色：SUPPLIER
- 企业状态：ACTIVE

**员工列表：**

| 用户名   | 真实姓名 | 用户类型        | 部门   | 职位     | 权限范围                     |
| -------- | -------- | --------------- | ------ | -------- | ---------------------------- |
| zhangsan | 张三     | ENTERPRISE_USER | 财务部 | 财务经理 | 创建应收账款、查看本企业数据 |
| lisi     | 李四     | ENTERPRISE_USER | 业务部 | 业务员   | 提交业务申请、查看自己的数据 |
| wangwu   | 王五     | ENTERPRISE_USER | 财务部 | 出纳     | 查看应收账款、确认收款       |

**登录方式：**

```bash
# 张三登录
POST /api/auth/login
{
  "username": "zhangsan",
  "password": "ZhangSan123!"
}

# 返回的JWT令牌中包含：
# - username: "zhangsan"
# - userType: "ENTERPRISE_USER"
# - enterpriseId: 1
```

**操作权限：**

- 张三可以创建应收账款（因为他是供应商A的员工）
- 张三只能查看供应商A的数据（通过enterpriseId关联）
- 张三不能查看其他企业的数据

---

### 场景2：系统管理员管理企业和用户

**管理员登录：**

```bash
POST /api/auth/login
{
  "username": "admin",
  "password": "Admin123!@#"
}
```

**管理操作：**

```bash
# 1. 审核企业注册
PUT /api/enterprise/0x1234.../approve

# 2. 创建企业用户
POST /api/users
{
  "username": "zhangsan",
  "password": "ZhangSan123!",
  "realName": "张三",
  "enterpriseId": 1,
  "userType": "ENTERPRISE_USER",
  "department": "财务部",
  "position": "财务经理"
}

# 3. 禁用用户
PUT /api/users/5/status
{
  "status": "DISABLED"
}

# 4. 重置用户密码
PUT /api/users/5/reset-password
{
  "newPassword": "NewPassword123!"
}
```

---

### 场景3：金融机构通过API访问系统

**设置API密钥：**

```bash
# 管理员为金融机构设置API密钥
PUT /api/enterprise/0xabcd.../reset-api-key
# 返回新的API密钥
```

**程序化调用：**

```python
import requests

# 使用API密钥获取JWT令牌
response = requests.post('http://localhost:8080/api/auth/api-key', json={
    'apiKey': 'a1b2c3d4e5f6...64字符密钥'
})
token = response.json()['data']['token']

# 使用JWT令牌调用业务API
headers = {'Authorization': f'Bearer {token}'}
response = requests.get(
    'http://localhost:8080/api/receivables/financer/0xabcd...',
    headers=headers
)
```

---

## 6. 数据模型关系

### 企业和用户的关系

```
Enterprise (企业)
├── id: 1
├── name: "供应商A"
├── address: "0x1234..."
├── role: SUPPLIER
└── User (用户列表)
    ├── id: 5, username: "zhangsan", enterpriseId: 1
    ├── id: 6, username: "lisi", enterpriseId: 1
    └── id: 7, username: "wangwu", enterpriseId: 1
```

**关联方式：**

- `User.enterpriseId` → `Enterprise.id` (外键关联)
- 一个企业可以有多个用户
- 一个用户只能属于一个企业

---

## 7. 安全特性

### 密码安全

- ✅ 使用BCrypt算法加密（strength=12）
- ✅ 密码不在日志中输出
- ✅ 密码不在响应中返回
- ✅ 密码修改时间记录

### JWT令牌安全

- ✅ 使用HS512强加密算法
- ✅ 密钥从环境变量读取（不硬编码）
- ✅ 令牌有效期24小时
- ✅ 签名验证防止篡改

### 账户安全

- ✅ 登录失败记录
- ✅ 账户锁定功能
- ✅ 最后登录IP记录
- ✅ 登录次数统计

### API安全

- ✅ 所有业务端点需要认证
- ✅ JWT令牌验证
- ✅ 异常处理（401未授权）
- ✅ API密钥可撤销

---

## 8. 待完善功能

### ⚠️ 当前限制

1. **细粒度权限控制未实现**

   - 所有认证用户可以访问所有端点
   - 未按用户类型限制访问
   - 未按企业角色限制业务操作
2. **企业角色权限未落实**

   - 供应商、核心企业、金融机构角色未强制执行
   - 需要添加业务层权限检查
3. **企业数据隔离未完全实现**

   - 用户应该只能看到所属企业的数据
   - 需要在Service层添加数据过滤

### ✅ 建议改进

```java
// 示例：在Controller层添加权限检查
@PostMapping("/receivables")
public Result<Receivable> createReceivable(@RequestBody CreateReceivableRequest request,
                                           Authentication authentication) {
    String username = authentication.getName();
    User user = userService.getUserByUsername(username);

    // 1. 检查用户类型
    if (user.getUserType() != UserType.ENTERPRISE_USER) {
        return Result.error("只有企业用户可以创建应收账款");
    }

    // 2. 检查企业角色
    Enterprise enterprise = enterpriseService.getEnterpriseById(user.getEnterpriseId());
    if (enterprise.getRole() != EnterpriseRole.SUPPLIER) {
        return Result.error("只有供应商可以创建应收账款");
    }

    // 3. 业务逻辑...
}
```

---

## 9. 总结

### 登录方式选择建议

| 场景             | 推荐登录方式  | 原因                     |
| ---------------- | ------------- | ------------------------ |
| 企业员工日常操作 | 用户名密码    | 支持多用户、细粒度权限   |
| 系统间API调用    | API密钥       | 安全、可撤销、无密码暴露 |
| 企业级集成应用   | 企业地址+密码 | 与区块链系统集成         |
| 管理员后台管理   | 用户名密码    | 完整的用户管理功能       |

### 权限体系特点

✅ **已实现：**

- 双层角色体系（企业角色 + 用户角色）
- 三种登录方式
- JWT无状态认证
- 用户状态管理
- 密码加密存储
- API密钥管理

⚠️ **待完善：**

- 细粒度权限控制（RBAC）
- 业务层数据隔离
- 企业角色权限强制执行
- 操作日志审计

---

**文档版本：** v1.0
**最后更新：** 2026-01-16
