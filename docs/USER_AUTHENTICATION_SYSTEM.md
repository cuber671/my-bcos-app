# 用户认证体系完整说明

## 📋 问题回答

**用户问题**：是否有对应的用户名和密码对应数据库及其实体类和服务类？

**答案**：✅ **现在有了！**

之前系统**没有**独立的User实体，只有Enterprise实体。我已经为你创建了完整的用户认证体系。

---

## ✅ 已创建的组件

### 1. 数据库层

#### User实体类
**文件**：`src/main/java/com/fisco/app/entity/User.java`

**数据库表**：`user`

**字段**：
```java
@Entity
@Table(name = "user")
public class User {
    private Long id;                    // 用户ID
    private String username;             // 用户名（唯一）
    private String password;             // 密码（BCrypt加密）
    private String realName;             // 真实姓名
    private String email;                // 电子邮箱
    private String phone;                // 手机号码
    private Long enterpriseId;           // 所属企业ID
    private UserType userType;           // 用户类型
    private UserStatus status;           // 用户状态
    private String department;           // 部门
    private String position;             // 职位
    private String avatarUrl;            // 头像URL
    private LocalDateTime lastLoginTime; // 最后登录时间
    private String lastLoginIp;          // 最后登录IP
    private Integer loginCount;          // 登录次数
    private LocalDateTime passwordChangedAt; // 密码修改时间
    // ...
}
```

**用户类型**：
- `ADMIN` - 系统管理员
- `ENTERPRISE_USER` - 企业用户
- `AUDITOR` - 审计员
- `OPERATOR` - 操作员

**用户状态**：
- `ACTIVE` - 正常
- `DISABLED` - 禁用
- `LOCKED` - 锁定
- `PENDING` - 待审核

---

### 2. Repository层

#### UserRepository
**文件**：`src/main/java/com/fisco/app/repository/UserRepository.java`

**主要方法**：
```java
public interface UserRepository extends JpaRepository<User, Long> {
    // 根据用户名查找
    Optional<User> findByUsername(String username);

    // 根据邮箱查找
    Optional<User> findByEmail(String email);

    // 根据企业ID查找用户
    List<User> findByEnterpriseId(Long enterpriseId);

    // 检查用户名是否存在
    boolean existsByUsername(String username);

    // 统计企业用户数量
    Long countByEnterpriseId(Long enterpriseId);

    // 更多方法...
}
```

---

### 3. Service层

#### UserService
**文件**：`src/main/java/com/fisco/app/service/UserService.java`

**核心功能**：

```java
public class UserService {
    // 创建用户
    User createUser(User user, String createdBy);

    // 更新用户信息
    User updateUser(Long userId, User updatedUser, String updatedBy);

    // 修改密码
    void changePassword(Long userId, String oldPassword, String newPassword);

    // 重置密码（管理员）
    void resetPassword(Long userId, String newPassword, String operator);

    // 设置用户状态
    void setUserStatus(Long userId, User.UserStatus status, String operator);

    // 锁定/解锁用户
    void lockUser(Long userId, String operator);
    void unlockUser(Long userId, String operator);

    // 删除用户
    void deleteUser(Long userId);

    // 验证登录
    User validateLogin(String username, String password);

    // 更新最后登录信息
    void updateLastLogin(Long userId, String ip);

    // 获取企业用户列表
    List<User> getUsersByEnterpriseId(Long enterpriseId);

    // 分页查询
    Page<User> getUsers(Pageable pageable);

    // 搜索用户
    List<User> searchUsersByRealName(String keyword);
}
```

---

### 4. Controller层

#### UserController
**文件**：`src/main/java/com/fisco/app/controller/UserController.java`

**API端点**：

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/users` | 创建用户 |
| PUT | `/api/users/{userId}` | 更新用户信息 |
| GET | `/api/users/{userId}` | 获取用户详情 |
| GET | `/api/users/me` | 获取当前用户信息 |
| PUT | `/api/users/{userId}/password` | 修改密码 |
| PUT | `/api/users/{userId}/reset-password` | 重置密码（管理员） |
| PUT | `/api/users/{userId}/status` | 设置用户状态 |
| DELETE | `/api/users/{userId}` | 删除用户 |
| GET | `/api/users` | 分页查询用户列表 |
| GET | `/api/users/enterprise/{enterpriseId}` | 获取企业用户 |
| GET | `/api/users/search` | 搜索用户 |

---

## 🔐 认证方式对比

### 方式1：用户名密码登录（推荐）✅

**适用场景**：企业内部多用户使用

```bash
POST /api/auth/login
{
  "username": "zhangsan",
  "password": "UserPassword123!"
}
```

**响应**：
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "type": "Bearer",
    "userId": 1,
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

**优势**：
- ✅ 支持一个企业多个用户
- ✅ 细粒度权限控制
- ✅ 用户级别管理
- ✅ 完整的审计日志

---

### 方式2：企业地址+密码登录（向后兼容）

**适用场景**：企业级应用、系统间调用

```bash
POST /api/auth/enterprise-login
{
  "address": "0x1234567890abcdef1234567890abcdef12345678",
  "password": "EnterprisePassword123!"
}
```

**响应**：
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "type": "Bearer",
    "address": "0x1234567890abcdef1234567890abcdef12345678",
    "enterpriseName": "供应商A",
    "role": "SUPPLIER",
    "loginType": "ENTERPRISE"
  }
}
```

---

### 方式3：API密钥认证（系统间调用）

```bash
POST /api/auth/api-key
{
  "apiKey": "a1b2c3d4e5f6..."
}
```

---

## 📊 数据库表结构

### user表

```sql
CREATE TABLE `user` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名（登录账号）',
  `password` VARCHAR(255) NOT NULL COMMENT '登录密码（BCrypt加密）',
  `real_name` VARCHAR(100) COMMENT '真实姓名',
  `email` VARCHAR(100) COMMENT '电子邮箱',
  `phone` VARCHAR(20) COMMENT '手机号码',
  `enterprise_id` BIGINT COMMENT '所属企业ID',
  `user_type` VARCHAR(20) NOT NULL COMMENT '用户类型（ADMIN/ENTERPRISE_USER/AUDITOR/OPERATOR）',
  `status` VARCHAR(20) NOT NULL COMMENT '用户状态（ACTIVE/DISABLED/LOCKED/PENDING）',
  `department` VARCHAR(100) COMMENT '部门',
  `position` VARCHAR(100) COMMENT '职位',
  `avatar_url` VARCHAR(500) COMMENT '头像URL',
  `last_login_time` DATETIME COMMENT '最后登录时间',
  `last_login_ip` VARCHAR(50) COMMENT '最后登录IP',
  `login_count` INT DEFAULT 0 COMMENT '登录次数',
  `password_changed_at` DATETIME COMMENT '密码修改时间',
  `created_at` DATETIME NOT NULL COMMENT '创建时间',
  `updated_at` DATETIME COMMENT '更新时间',
  `created_by` VARCHAR(50) COMMENT '创建人',
  `updated_by` VARCHAR(50) COMMENT '更新人',

  INDEX `idx_username` (`username`),
  INDEX `idx_email` (`email`),
  INDEX `idx_enterprise_id` (`enterprise_id`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
```

---

## 🎯 使用场景示例

### 场景1：企业A有3个员工

```
企业A (ID: 1)
├── 张三（财务部经理）
├── 李四（业务员）
└── 王五（出纳）
```

**创建用户**：
```bash
# 创建张三
POST /api/users
{
  "username": "zhangsan",
  "password": "ZhangSan123!",
  "realName": "张三",
  "email": "zhangsan@companyA.com",
  "phone": "13800138001",
  "enterpriseId": 1,
  "userType": "ENTERPRISE_USER",
  "department": "财务部",
  "position": "财务经理"
}
```

**登录**：
```bash
# 张三登录
POST /api/auth/login
{
  "username": "zhangsan",
  "password": "ZhangSan123!"
}
```

---

### 场景2：管理员管理用户

```bash
# 管理员禁用用户
PUT /api/users/5/status
Authorization: Bearer {admin_token}
{
  "status": "DISABLED"
}

# 管理员重置用户密码
PUT /api/users/5/reset-password
Authorization: Bearer {admin_token}
{
  "newPassword": "NewPassword123!"
}
```

---

## 📁 文件清单

### 新增文件

1. ✅ `User.java` - 用户实体类
2. ✅ `UserRepository.java` - 用户Repository
3. ✅ `UserService.java` - 用户Service
4. ✅ `UserController.java` - 用户Controller
5. ✅ `PasswordUtil.java` - 密码加密工具

### 更新文件

1. ✅ `Enterprise.java` - 添加password和apiKey字段
2. ✅ `EnterpriseRepository.java` - 添加findByApiKey方法
3. ✅ `EnterpriseService.java` - 添加密码和API密钥管理方法
4. ✅ `AuthController.java` - 支持三种认证方式
5. ✅ `BusinessException.java` - 添加UserNotFoundException

---

## 🚀 快速开始

### 1. 数据库迁移

```sql
-- 创建user表
CREATE TABLE `user` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL UNIQUE,
  `password` VARCHAR(255) NOT NULL,
  `real_name` VARCHAR(100),
  `email` VARCHAR(100),
  `phone` VARCHAR(20),
  `enterprise_id` BIGINT,
  `user_type` VARCHAR(20) NOT NULL DEFAULT 'ENTERPRISE_USER',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  `department` VARCHAR(100),
  `position` VARCHAR(100),
  `avatar_url` VARCHAR(500),
  `last_login_time` DATETIME,
  `last_login_ip` VARCHAR(50),
  `login_count` INT DEFAULT 0,
  `password_changed_at` DATETIME,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME,
  `created_by` VARCHAR(50),
  `updated_by` VARCHAR(50),
  INDEX `idx_username` (`username`),
  INDEX `idx_email` (`email`),
  INDEX `idx_enterprise_id` (`enterprise_id`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 2. 创建管理员账户

```bash
# 创建管理员用户
POST /api/users
{
  "username": "admin",
  "password": "Admin123!@#",
  "realName": "系统管理员",
  "email": "admin@example.com",
  "userType": "ADMIN"
}
```

### 3. 登录测试

```bash
# 使用管理员账户登录
POST /api/auth/login
{
  "username": "admin",
  "password": "Admin123!@#"
}
```

---

## 🎉 总结

### 现在拥有的功能

✅ **完整的用户认证体系**
- User实体、Repository、Service、Controller齐全
- 支持用户名密码登录
- 密码BCrypt加密存储
- 用户状态管理
- 登录审计日志

✅ **多种认证方式**
1. 用户名密码（推荐）
2. 企业地址+密码（向后兼容）
3. API密钥（系统间调用）

✅ **细粒度权限控制**
- 用户类型：管理员、企业用户、审计员、操作员
- 用户状态：正常、禁用、锁定、待审核
- 企业级用户管理

✅ **完整的管理功能**
- 创建/更新/删除用户
- 修改/重置密码
- 启用/禁用/锁定用户
- 查询/搜索用户

---

**项目编译成功！所有组件已就绪！** 🎉
