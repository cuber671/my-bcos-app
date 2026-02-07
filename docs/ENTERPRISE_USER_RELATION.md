# 企业与用户关联关系说明

## 📊 关联关系总览

系统中有**企业和用户的关联关系**，通过数据库外键实现。

---

## 🗃️ 相关数据表

### 1. enterprise表（企业表）

**表名：** `enterprise`

**主要字段：**
```sql
CREATE TABLE enterprise (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,          -- 企业ID（主键）
    address VARCHAR(42) NOT NULL UNIQUE,           -- 区块链地址
    name VARCHAR(255) NOT NULL,                    -- 企业名称
    credit_code VARCHAR(50) NOT NULL UNIQUE,       -- 统一社会信用代码
    role VARCHAR(20) NOT NULL,                     -- 企业角色
    status VARCHAR(20) NOT NULL,                   -- 企业状态
    credit_rating INT,                             -- 信用评级
    credit_limit DECIMAL(20,2),                    -- 授信额度
    password VARCHAR(255),                         -- ✅ 登录密码（需添加）
    api_key VARCHAR(64) UNIQUE,                    -- ✅ API密钥（需添加）
    registered_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50)
);
```

### 2. user表（用户表）- **需要创建**

**表名：** `user`

**主要字段：**
```sql
CREATE TABLE user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,          -- 用户ID（主键）
    username VARCHAR(50) NOT NULL UNIQUE,           -- 用户名
    password VARCHAR(255) NOT NULL,                -- 登录密码
    real_name VARCHAR(100),                        -- 真实姓名
    email VARCHAR(100),                            -- 邮箱
    phone VARCHAR(20),                             -- 手机号
    enterprise_id BIGINT,                          -- 🔑 所属企业ID（外键）
    user_type VARCHAR(20) NOT NULL,                -- 用户类型
    status VARCHAR(20) NOT NULL,                   -- 用户状态
    department VARCHAR(100),                       -- 部门
    position VARCHAR(100),                         -- 职位
    avatar_url VARCHAR(500),                       -- 头像
    last_login_time TIMESTAMP,                     -- 最后登录时间
    last_login_ip VARCHAR(50),                     -- 最后登录IP
    login_count INT DEFAULT 0,                     -- 登录次数
    password_changed_at TIMESTAMP,                 -- 密码修改时间
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),

    -- 🔑 外键约束：关联到enterprise表
    FOREIGN KEY (enterprise_id) REFERENCES enterprise(id) ON DELETE SET NULL
);
```

---

## 🔗 关联关系详解

### 一对多关系

```
enterprise (企业表)
    ↓ 1
    ↓
    ↓ N
    ↓
user (用户表)
```

**关系说明：**
- **一个企业** 可以有 **多个用户**
- **一个用户** 只能属于 **一个企业**
- 通过 `user.enterprise_id` 外键关联到 `enterprise.id`

### ER图（实体关系图）

```
┌─────────────────────┐
│    enterprise       │
├─────────────────────┤
│ id (PK)            │┐
│ address            ││
│ name               ││ 1
│ role               ││
│ status             ││
│ password           ││
│ api_key            ││
└─────────────────────┘│
                      │
                      │
                      │ N
                      │
                      ↓
              ┌─────────────────────┐
              │       user           │
              ├─────────────────────┤
              │ id (PK)            │
              │ username           │
              │ password           │
              │ enterprise_id (FK) │◄── 外键指向enterprise.id
              │ real_name          │
              │ user_type          │
              │ status             │
              │ department         │
              │ position           │
              └─────────────────────┘
```

---

## 📋 关联查询示例

### SQL查询

#### 1. 查询企业的所有用户

```sql
-- 查询企业ID=1的所有用户
SELECT
    u.id,
    u.username,
    u.real_name,
    u.user_type,
    u.department,
    u.position,
    e.name AS enterprise_name,
    e.role AS enterprise_role
FROM user u
LEFT JOIN enterprise e ON u.enterprise_id = e.id
WHERE u.enterprise_id = 1;
```

#### 2. 查询用户所属的企业信息

```sql
-- 查询用户名='zhangsan'的用户及其企业信息
SELECT
    u.username,
    u.real_name,
    u.user_type,
    e.id AS enterprise_id,
    e.name AS enterprise_name,
    e.address AS enterprise_address,
    e.role AS enterprise_role
FROM user u
LEFT JOIN enterprise e ON u.enterprise_id = e.id
WHERE u.username = 'zhangsan';
```

#### 3. 统计每个企业的用户数量

```sql
SELECT
    e.id,
    e.name,
    e.role,
    COUNT(u.id) AS user_count
FROM enterprise e
LEFT JOIN user u ON e.id = u.enterprise_id
GROUP BY e.id
ORDER BY user_count DESC;
```

#### 4. 查询企业及其所有管理员用户

```sql
SELECT
    e.name AS enterprise_name,
    e.role AS enterprise_role,
    u.username,
    u.real_name,
    u.user_type
FROM enterprise e
LEFT JOIN user u ON e.id = u.enterprise_id
WHERE u.user_type = 'ADMIN'
   OR (e.role = 'REGULATOR' AND u.enterprise_id = e.id);
```

---

## 💻 JPA实体类关联

### Enterprise实体

```java
@Entity
@Table(name = "enterprise")
public class Enterprise {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String address;
    private String name;
    private String creditCode;
    private EnterpriseRole role;
    private EnterpriseStatus status;

    // ❌ 未定义与User的关联关系
    // 可以添加：
    // @OneToMany(mappedBy = "enterprise")
    // private List<User> users;
}
```

### User实体

```java
@Entity
@Table(name = "user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String password;
    private String realName;

    // ✅ 已定义外键关联
    @Column(name = "enterprise_id")
    private Long enterpriseId;  // 外键，指向enterprise.id

    // ❌ 未定义@ManyToOne关联对象
    // 可以添加：
    // @ManyToOne
    // @JoinColumn(name = "enterprise_id", insertable = false, updatable = false)
    // private Enterprise enterprise;

    private UserType userType;
    private UserStatus status;
    private String department;
    private String position;
}
```

---

## 🚀 改进建议

### 建议添加JPA关联关系

#### 1. 在User实体中添加Enterprise对象

```java
@Entity
@Table(name = "user")
public class User {

    // 保留原有的enterprise_id字段
    @Column(name = "enterprise_id")
    private Long enterpriseId;

    // ✅ 添加关联对象（便于查询）
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enterprise_id", insertable = false, updatable = false)
    private Enterprise enterprise;

    // 使用示例：
    // User user = userRepository.findById(userId);
    // Enterprise enterprise = user.getEnterprise();
    // String enterpriseName = enterprise.getName();
}
```

#### 2. 在Enterprise实体中添加User列表

```java
@Entity
@Table(name = "enterprise")
public class Enterprise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ✅ 添加一对多关联
    @OneToMany(mappedBy = "enterprise", fetch = FetchType.LAZY)
    private List<User> users;

    // 使用示例：
    // Enterprise enterprise = enterpriseRepository.findById(enterpriseId);
    // List<User> users = enterprise.getUsers();
    // long userCount = users.size();
}
```

#### 3. 添加Repository查询方法

```java
public interface UserRepository extends JpaRepository<User, Long> {

    // ✅ 已存在的方法
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    List<User> findByEnterpriseId(Long enterpriseId);
    Long countByEnterpriseId(Long enterpriseId);

    // ✅ 建议添加的方法（带JOIN FETCH）
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.enterprise WHERE u.id = :userId")
    Optional<User> findByIdWithEnterprise(@Param("userId") Long userId);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.enterprise WHERE u.enterpriseId = :enterpriseId")
    List<User> findByEnterpriseIdWithEnterprise(@Param("enterpriseId") Long enterpriseId);
}

public interface EnterpriseRepository extends JpaRepository<Enterprise, Long> {

    // ✅ 已存在的方法
    Optional<Enterprise> findByAddress(String address);
    Optional<Enterprise> findById(Long id);

    // ✅ 建议添加的方法（带JOIN FETCH）
    @Query("SELECT e FROM Enterprise e LEFT JOIN FETCH e.users WHERE e.id = :enterpriseId")
    Optional<Enterprise> findByIdWithUsers(@Param("enterpriseId") Long enterpriseId);

    @Query("SELECT e FROM Enterprise e LEFT JOIN FETCH e.users WHERE e.address = :address")
    Optional<Enterprise> findByAddressWithUsers(@Param("address") String address);
}
```

---

## 📊 使用场景示例

### 场景1：查询企业及其所有员工

```java
@Service
@RequiredArgsConstructor
public class EnterpriseUserService {

    private final EnterpriseRepository enterpriseRepository;
    private final UserRepository userRepository;

    // 获取企业及其所有用户
    public EnterpriseWithUsersDTO getEnterpriseWithUsers(Long enterpriseId) {
        // 方式1: 两次查询
        Enterprise enterprise = enterpriseRepository.findById(enterpriseId)
            .orElseThrow(() -> new EnterpriseNotFoundException(enterpriseId));
        List<User> users = userRepository.findByEnterpriseId(enterpriseId);

        // 方式2: 使用JOIN FETCH（一次查询）
        Enterprise enterprise = enterpriseRepository
            .findByIdWithUsers(enterpriseId)
            .orElseThrow(() -> new EnterpriseNotFoundException(enterpriseId));

        return new EnterpriseWithUsersDTO(enterprise, enterprise.getUsers());
    }
}
```

### 场景2：查询用户及其企业信息

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // 获取用户及其企业信息
    public UserWithEnterpriseDTO getUserWithEnterprise(Long userId) {
        // 方式1: 两次查询
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
        Enterprise enterprise = enterpriseRepository
            .findById(user.getEnterpriseId())
            .orElse(null);

        // 方式2: 使用JOIN FETCH（一次查询）
        User user = userRepository
            .findByIdWithEnterprise(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));

        return new UserWithEnterpriseDTO(user, user.getEnterprise());
    }
}
```

### 场景3：验证用户是否属于指定企业

```java
@Service
@RequiredArgsConstructor
public class SecurityService {

    private final UserRepository userRepository;

    // 验证用户是否属于指定企业
    public boolean isUserBelongsToEnterprise(Long userId, Long enterpriseId) {
        return userRepository.existsByIdAndEnterpriseId(userId, enterpriseId);
    }

    // 获取用户可访问的企业数据
    public UserEnterpriseInfo getUserEnterpriseInfo(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UserNotFoundException(username));

        if (user.getEnterpriseId() == null) {
            // 管理员用户，可以访问所有企业
            return UserEnterpriseInfo.adminUser();
        }

        // 企业用户，只能访问所属企业的数据
        Enterprise enterprise = enterpriseRepository
            .findById(user.getEnterpriseId())
            .orElseThrow(() -> new EnterpriseNotFoundException(user.getEnterpriseId()));

        return UserEnterpriseInfo.enterpriseUser(enterprise);
    }
}
```

---

## ⚠️ 当前状态

### 数据库表状态

| 表名 | 是否存在 | 关联字段 | 外键约束 |
|------|---------|---------|---------|
| `enterprise` | ✅ 存在 | `id` (主键) | - |
| `user` | ⚠️ 需要创建 | `enterprise_id` (外键) | ❌ 需要添加 |
| `sys_user` | ✅ 存在（旧表） | `enterprise_address` (简单字段) | ❌ 无外键约束 |

### 当前问题

1. **❌ 新的user表尚未创建**
   - 迁移脚本V3已创建，等待执行
   - 包含完整的外键约束

2. **❌ enterprise表缺少登录字段**
   - 缺少 `password` 字段
   - 缺少 `api_key` 字段
   - 迁移脚本V2已创建，等待执行

3. **❌ JPA实体关联关系未定义**
   - User实体只有 `enterpriseId` 字段
   - 未定义 `@ManyToOne` 关联对象
   - Enterprise实体未定义 `@OneToMany` 关联

---

## 🔧 完整解决方案

### 步骤1：执行数据库迁移

```bash
# 启动应用，Flyway自动执行迁移
mvn spring-boot:run
```

**迁移脚本会：**
1. 为 `enterprise` 表添加 `password` 和 `api_key` 字段
2. 创建新的 `user` 表，包含外键约束
3. 插入测试数据（admin、zhangsan等）

### 步骤2：验证外键约束

```sql
-- 查看user表的外键约束
SHOW CREATE TABLE user;

-- 应该看到：
-- CONSTRAINT ... FOREIGN KEY (enterprise_id)
-- REFERENCES enterprise (id) ON DELETE SET NULL
```

### 步骤3：测试关联查询

```java
// 测试查询企业的所有用户
Enterprise enterprise = enterpriseRepository.findById(1L);
List<User> users = userRepository.findByEnterpriseId(enterprise.getId());
System.out.println("企业 " + enterprise.getName() + " 有 " + users.size() + " 个用户");

// 测试查询用户的企业
User user = userRepository.findByUsername("zhangsan");
Enterprise enterprise = enterpriseRepository.findById(user.getEnterpriseId());
System.out.println("用户 " + user.getRealName() + " 属于企业 " + enterprise.getName());
```

---

## 📝 总结

### 关联关系存在吗？

**✅ 是的，企业和用户的关联关系存在：**

1. **数据库层面**
   - ✅ SQL脚本已定义外键约束
   - ⚠️ 等待迁移执行后生效
   - `user.enterprise_id` → `enterprise.id`

2. **实体类层面**
   - ✅ User实体有 `enterpriseId` 字段
   - ⚠️ 未定义JPA关联关系（建议添加）

3. **业务逻辑层面**
   - ✅ UserService通过enterpriseId查询企业
   - ✅ 创建用户时需要提供enterpriseId

### 关联类型

**一对多关系：**
- 1个企业 → N个用户
- 通过 `user.enterprise_id` 外键关联

### 下一步

1. ✅ 执行数据库迁移V2和V3
2. ✅ 验证外键约束已创建
3. ✅ 测试企业和用户的关联查询
4. ⚠️ （可选）添加JPA @ManyToOne/@OneToMany关联

---

**文档版本：** v1.0
**最后更新：** 2026-01-16
**相关文档：**
- [DATABASE_TABLES_GUIDE.md](DATABASE_TABLES_GUIDE.md) - 数据库表完整说明
- [DATABASE_MIGRATION_README.md](DATABASE_MIGRATION_README.md) - 迁移执行指南
- [PERMISSION_AND_AUTH_SYSTEM.md](PERMISSION_AND_AUTH_SYSTEM.md) - 权限体系说明
