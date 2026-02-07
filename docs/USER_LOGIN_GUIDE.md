# 用户登录使用指南

## ✅ 是的！系统完全支持用户角色身份登录

该系统提供**完整的用户认证功能**，支持不同角色的用户登录系统。

---

## 🎯 支持的用户角色

系统支持以下4种用户类型登录：

| 用户角色 | 英文标识 | 说明 | 权限范围 |
|---------|---------|------|---------|
| **系统管理员** | `ADMIN` | 平台管理员 | 管理所有企业、所有用户、系统配置 |
| **企业用户** | `ENTERPRISE_USER` | 企业员工 | 操作所属企业的业务 |
| **审计员** | `AUDITOR` | 内部审计人员 | 查看交易记录、审计合规 |
| **操作员** | `OPERATOR` | 系统操作人员 | 日常业务操作、数据录入 |

---

## 🔐 用户登录方式

### 方式：用户名 + 密码登录（推荐）

**端点：** `POST /api/auth/login`

**请求示例：**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "zhangsan",
    "password": "UserPassword123!"
  }'
```

**请求体格式：**
```json
{
  "username": "用户名",
  "password": "密码"
}
```

**成功响应示例：**
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
    "userType": "ENTERPRISE_USER",     // 用户角色
    "enterpriseId": 1,                  // 所属企业ID
    "department": "财务部",
    "position": "财务经理",
    "loginType": "USER"
  }
}
```

**失败响应示例：**
```json
{
  "code": 500,
  "message": "用户名或密码错误"
}
```

或

```json
{
  "code": 500,
  "message": "账户已被锁定，请联系管理员"
}
```

---

## 📋 使用JWT令牌访问API

登录成功后，使用返回的`token`访问受保护的API：

### 设置Authorization头

```bash
# 方式1：使用curl
curl -X GET http://localhost:8080/api/users/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."

# 方式2：使用Postman
# Headers:
#   Authorization: Bearer {token}
```

### 示例：获取当前用户信息

```bash
curl -X GET http://localhost:8080/api/users/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."
```

**响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 5,
    "username": "zhangsan",
    "realName": "张三",
    "email": "zhangsan@example.com",
    "phone": "13800138000",
    "enterpriseId": 1,
    "userType": "ENTERPRISE_USER",
    "status": "ACTIVE",
    "department": "财务部",
    "position": "财务经理",
    "loginCount": 15
  }
}
```

---

## 🎭 不同角色用户登录示例

### 示例1：系统管理员登录

```json
// POST /api/auth/login
{
  "username": "admin",
  "password": "Admin123!@#"
}

// 响应
{
  "userType": "ADMIN",
  "realName": "系统管理员",
  "enterpriseId": null
}
```

**管理员权限：**
- ✅ 查看所有企业
- ✅ 创建/审核企业
- ✅ 管理所有用户
- ✅ 重置用户密码
- ✅ 启用/禁用用户

### 示例2：企业用户（财务经理）登录

```json
// POST /api/auth/login
{
  "username": "zhangsan",
  "password": "ZhangSan123!"
}

// 响应
{
  "userType": "ENTERPRISE_USER",
  "realName": "张三",
  "enterpriseId": 1,
  "department": "财务部",
  "position": "财务经理"
}
```

**企业用户权限：**
- ✅ 创建应收账款
- ✅ 查看所属企业数据
- ✅ 提交融资申请
- ✅ 查看自己的操作记录

### 示例3：审计员登录

```json
// POST /api/auth/login
{
  "username": "auditor1",
  "password": "Auditor123!"
}

// 响应
{
  "userType": "AUDITOR",
  "realName": "李审计",
  "enterpriseId": null
}
```

**审计员权限：**
- ✅ 查看所有交易记录
- ✅ 审计合规性检查
- ✅ 生成审计报告
- ✅ 查看操作日志

### 示例4：操作员登录

```json
// POST /api/auth/login
{
  "username": "operator1",
  "password": "Operator123!"
}

// 响应
{
  "userType": "OPERATOR",
  "realName": "王操作",
  "department": "业务部"
}
```

**操作员权限：**
- ✅ 日常业务操作
- ✅ 数据录入
- ✅ 基础查询
- ❌ 无管理权限

---

## 🔍 登录验证流程

系统会执行以下验证步骤：

```
1. 用户提交用户名和密码
   ↓
2. UserService.validateLogin()
   ↓
3. 根据用户名查找用户
   - 不存在 → "用户不存在"
   ↓
4. 检查用户状态
   - LOCKED → "账户已被锁定，请联系管理员"
   - DISABLED → "账户已被禁用，请联系管理员"
   - ACTIVE → 继续
   ↓
5. BCrypt验证密码
   - 密码错误 → "用户名或密码错误"
   - 密码正确 → 继续
   ↓
6. 更新最后登录信息
   - 记录登录时间
   - 记录登录IP
   - 增加登录次数
   ↓
7. 生成JWT令牌
   - subject = username
   - 有效期 = 24小时
   ↓
8. 返回令牌和用户信息
```

---

## 🚫 登录限制

### 账户状态限制

| 用户状态 | 是否可登录 | 提示信息 |
|---------|-----------|---------|
| `ACTIVE` | ✅ 可登录 | - |
| `DISABLED` | ❌ 不可登录 | "账户已被禁用，请联系管理员" |
| `LOCKED` | ❌ 不可登录 | "账户已被锁定，请联系管理员" |
| `PENDING` | ❌ 不可登录 | "账户待审核" |

### 密码安全要求

- 密码必须BCrypt加密存储
- 密码长度：最少6位
- 建议使用强密码（包含大小写字母、数字、特殊字符）

### 登录失败处理

- ✅ 记录失败日志
- ✅ 返回通用错误信息（不泄露用户是否存在）
- ⚠️ 暂未实现登录次数限制（待完善）

---

## 📊 完整使用场景示例

### 场景：供应商A的3个员工分别登录

**企业信息：**
- 企业名称：供应商A
- 企业ID：1
- 企业角色：SUPPLIER

**员工1：张三（财务经理）**
```bash
# 登录
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "zhangsan",
    "password": "ZhangSan123!"
  }'

# 响应包含：
# - userType: "ENTERPRISE_USER"
# - enterpriseId: 1
# - department: "财务部"
# - position: "财务经理"

# 使用token创建应收账款
curl -X POST http://localhost:8080/api/receivables \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "receivableId": "REC001",
    "coreEnterpriseAddress": "0xabcd...",
    "amount": 100000,
    "currency": "CNY",
    "issueDate": "2026-01-16T10:00:00",
    "dueDate": "2026-04-16T10:00:00",
    "description": "货物供应款"
  }'
```

**员工2：李四（业务员）**
```bash
# 登录
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "lisi",
    "password": "LiSi123!"
  }'

# 响应包含：
# - userType: "ENTERPRISE_USER"
# - enterpriseId: 1
# - department: "业务部"
# - position": "业务员"

# 使用token查看应收账款
curl -X GET http://localhost:8080/api/receivables/supplier/0x1234... \
  -H "Authorization: Bearer {token}"
```

**员工3：王五（出纳）**
```bash
# 登录
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "wangwu",
    "password": "WangWu123!"
  }'

# 响应包含：
# - userType: "ENTERPRISE_USER"
# - enterpriseId: 1
# - department: "财务部"
# - position: "出纳"
```

---

## 🛠️ 后端实现详解

### 1. 登录验证方法

**文件：** `UserService.java`

```java
/**
 * 验证用户登录
 */
public User validateLogin(String username, String password) {
    log.info("用户登录验证: username={}", username);

    // 1. 根据用户名查找用户
    User user = getUserByUsername(username);

    // 2. 检查账户状态
    if (user.isLocked()) {
        throw new BusinessException("账户已被锁定，请联系管理员");
    }

    if (user.isDisabled()) {
        throw new BusinessException("账户已被禁用，请联系管理员");
    }

    // 3. 验证密码
    if (!PasswordUtil.matches(password, user.getPassword())) {
        throw new BusinessException("用户名或密码错误");
    }

    log.info("用户登录验证成功: username={}", username);
    return user;
}
```

### 2. JWT令牌生成

**文件：** `JwtTokenProvider.java`

```java
/**
 * 生成JWT令牌
 */
public String generateToken(String username) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

    return Jwts.builder()
        .setSubject(username)           // 令牌主题：用户名
        .setIssuedAt(now)               // 签发时间
        .setExpiration(expiryDate)      // 过期时间
        .signWith(SignatureAlgorithm.HS512, jwtSecret)  // 签名算法
        .compact();
}
```

### 3. 认证过滤器

**文件：** `JwtAuthenticationFilter.java`

```java
@Override
protected void doFilterInternal(HttpServletRequest request,
                                HttpServletResponse response,
                                FilterChain filterChain) {
    // 1. 从请求头获取Authorization
    String authHeader = request.getHeader("Authorization");

    // 2. 提取JWT令牌
    String token = jwtTokenProvider.extractTokenFromHeader(authHeader);

    // 3. 验证令牌并设置认证信息
    if (token != null && jwtTokenProvider.validateToken(token)) {
        String username = jwtTokenProvider.getUserAddressFromToken(token);
        UserAuthentication authentication = new UserAuthentication(username);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    // 4. 继续过滤器链
    filterChain.doFilter(request, response);
}
```

---

## 💡 与企业登录的区别

系统支持两种登录方式：

### 用户登录（推荐）
```json
POST /api/auth/login
{
  "username": "zhangsan",
  "password": "Password123!"
}

// 返回用户信息
{
  "userType": "ENTERPRISE_USER",
  "enterpriseId": 1,
  "department": "财务部"
}
```

**优势：**
- ✅ 支持多用户
- ✅ 细粒度权限控制
- ✅ 用户级别管理
- ✅ 部门和职位信息

### 企业登录（向后兼容）
```json
POST /api/auth/enterprise-login
{
  "address": "0x1234...abcd",
  "password": "Password123!"
}

// 返回企业信息
{
  "role": "SUPPLIER",
  "enterpriseName": "供应商A"
}
```

**特点：**
- ✅ 企业级认证
- ✅ 基于区块链地址
- ⚠️ 一个企业一个账户

---

## 🔐 安全特性

### 已实现的安全措施

1. **密码加密**
   - ✅ BCrypt算法加密（strength=12）
   - ✅ 密码不在日志中输出
   - ✅ 密码不在响应中返回

2. **令牌安全**
   - ✅ HS512强加密算法
   - ✅ 24小时有效期
   - ✅ 签名验证防篡改

3. **账户保护**
   - ✅ 登录失败记录
   - ✅ 账户锁定功能
   - ✅ 账户禁用功能
   - ✅ 最后登录IP记录

4. **审计日志**
   - ✅ 记录登录时间
   - ✅ 记录登录IP
   - ✅ 统计登录次数

---

## ❓ 常见问题

### Q1: 忘记密码怎么办？

**A:** 联系管理员重置密码
```bash
# 管理员重置用户密码
PUT /api/users/{userId}/reset-password
Authorization: Bearer {admin_token}
{
  "newPassword": "NewPassword123!"
}
```

### Q2: 账户被锁定怎么办？

**A:** 联系管理员解锁
```bash
# 管理员解锁用户
PUT /api/users/{userId}/status
Authorization: Bearer {admin_token}
{
  "status": "ACTIVE"
}
```

### Q3: JWT令牌过期怎么办？

**A:** 重新登录获取新令牌
```bash
# 令牌有效期24小时
# 过期后重新登录
POST /api/auth/login
```

### Q4: 如何修改自己的密码？

**A:** 使用修改密码接口
```bash
# 修改密码
PUT /api/users/{userId}/password
Authorization: Bearer {token}
{
  "oldPassword": "OldPassword123!",
  "newPassword": "NewPassword123!"
}
```

---

## 📝 总结

✅ **系统完全支持用户角色登录**
- 4种用户类型：ADMIN、ENTERPRISE_USER、AUDITOR、OPERATOR
- 用户名密码认证方式
- JWT令牌管理会话
- 完整的用户状态管理
- 详细的登录审计日志

✅ **推荐使用用户登录**
- 相比企业登录更加灵活
- 支持多用户管理
- 细粒度权限控制
- 完整的审计功能

---

**文档版本：** v1.0
**最后更新：** 2026-01-16
**相关文档：** [PERMISSION_AND_AUTH_SYSTEM.md](PERMISSION_AND_AUTH_SYSTEM.md)
