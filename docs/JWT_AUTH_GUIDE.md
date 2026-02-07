# JWT 认证机制使用说明

## 📋 概述

系统已实现基于JWT（JSON Web Token）的认证机制，所有API端点（除了登录和注册）都需要提供有效的JWT令牌才能访问。

## 🔐 安全特性

1. **JWT令牌认证** - 替代了不安全的请求头认证
2. **环境变量配置** - 敏感信息不再硬编码
3. **细粒度访问控制** - 公开端点与认证端点分离
4. **无状态会话** - 不依赖服务器会话，易于扩展

## 🚀 快速开始

### 1. 配置环境变量

创建 `.env` 文件（参考 `.env.example`）：

```bash
# 复制示例文件
cp .env.example .env

# 编辑 .env 文件，填写实际值
```

**重要**：
- `DB_PASSWORD`: 设置数据库密码
- `JWT_SECRET`: 生成强随机密钥（必须至少256位）

```bash
# 生成JWT密钥的命令
openssl rand -base64 64
```

### 2. 配置应用启动方式

#### 方式1: 使用IDE启动（推荐开发环境）

在IDE中设置环境变量：
- `DB_USERNAME=fisco_admin`
- `DB_PASSWORD=your_password`
- `JWT_SECRET=your_jwt_secret`

#### 方式2: 使用Maven启动（开发环境）

```bash
export DB_USERNAME=fisco_admin
export DB_PASSWORD=your_password
export JWT_SECRET=$(openssl rand -base64 64)

mvn spring-boot:run
```

#### 方式3: 使用java -jar启动（生产环境）

```bash
export DB_USERNAME=fisco_admin
export DB_PASSWORD=your_password
export JWT_SECRET=$(openssl rand -base64 64)

java -jar target/my-bcos-app-1.0-SNAPSHOT.jar
```

#### 方式4: 使用Docker启动（生产环境推荐）

```dockerfile
# 在Dockerfile或docker-compose.yml中设置环境变量
environment:
  - DB_USERNAME=fisco_admin
  - DB_PASSWORD=your_password
  - JWT_SECRET=your_jwt_secret
```

### 3. 登录获取JWT令牌

**请求**：
```bash
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "address": "0x1234567890abcdef1234567890abcdef12345678"
}
```

**响应**：
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIweDEyMzQ1Njc4OTBhYmNkZWYxMjM0NTY3ODkwYWJjZGVmIiwiaWF0IjoxNjQyMDAwMDAwLCJleHAiOjE2NDI4NjQwMDB9...",
    "type": "Bearer",
    "address": "0x1234567890abcdef1234567890abcdef12345678",
    "enterpriseName": "示例企业"
  },
  "timestamp": 1642000000000
}
```

### 4. 使用JWT令牌访问API

在后续所有API请求的请求头中添加：

```
Authorization: Bearer {your_jwt_token}
```

**示例**：
```bash
POST http://localhost:8080/api/bill
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
Content-Type: application/json

{
  "billId": "BILL20240116001",
  "billType": "COMMERCIAL_BILL",
  "acceptorAddress": "0xabcdef1234567890abcdef1234567890abcdef",
  "beneficiaryAddress": "0x1234567890abcdef1234567890abcdef12345678",
  "amount": 1000000.00,
  "currency": "CNY",
  "issueDate": "2024-01-16T10:00:00",
  "dueDate": "2024-07-16T10:00:00",
  "description": "货物采购款"
}
```

## 📡 API端点说明

### 公开端点（无需认证）

- `POST /api/auth/login` - 用户登录获取令牌
- `POST /api/enterprise/register` - 企业注册
- `GET /swagger-ui/**` - API文档
- `GET /v3/api-docs/**` - API规范

### 认证端点（需要JWT令牌）

所有 `/api/**` 下的端点都需要认证，例如：

- `POST /api/bill` - 开票
- `POST /api/receivable` - 创建应收账款
- `POST /api/warehouse-receipt` - 创建仓单
- `GET /api/bill/{billId}` - 查询票据
- `PUT /api/receivable/{id}/confirm` - 确认应收账款

## ⚠️ 错误处理

### 401 Unauthorized（未授权）

**原因**：
- 未提供JWT令牌
- JWT令牌无效
- JWT令牌已过期

**响应示例**：
```json
{
  "code": 401,
  "error": "未授权访问",
  "message": "请提供有效的JWT令牌",
  "timestamp": 1642000000000
}
```

**解决方法**：
1. 先调用 `/api/auth/login` 获取JWT令牌
2. 在请求头中添加 `Authorization: Bearer {token}`
3. 如果令牌过期，重新登录获取新令牌

### 403 Forbidden（禁止访问）

**原因**：
- 企业账户未激活
- 权限不足

**解决方法**：
- 联系管理员激活企业账户
- 确认账户具有相应权限

## 🔒 安全最佳实践

1. **保护JWT密钥**
   - 永远不要将 `JWT_SECRET` 提交到Git
   - 使用强随机密钥（至少256位）
   - 定期轮换密钥

2. **保护JWT令牌**
   - 令牌存储在安全的地方（如HttpOnly Cookie或内存）
   - 使用HTTPS传输令牌
   - 令牌有过期时间（默认24小时）

3. **环境隔离**
   - 开发、测试、生产环境使用不同的密钥
   - 使用配置管理工具（如Spring Cloud Config、Vault）

4. **定期审计**
   - 监控异常登录行为
   - 记录所有认证失败的尝试

## 🛠️ 故障排查

### 问题1: 启动时报错 "JWT_SECRET不能为空"

**解决方法**：
```bash
# 设置环境变量
export JWT_SECRET=$(openssl rand -base64 64)

# 或在IDE中配置环境变量
# IDEA: Run -> Edit Configurations -> Environment variables
# Eclipse: Run -> Run Configurations -> Environment
```

### 问题2: 数据库连接失败

**解决方法**：
```bash
# 检查环境变量是否设置
echo $DB_USERNAME
echo $DB_PASSWORD

# 确认数据库服务运行中
mysql -u fisco_admin -p
```

### 问题3: 令牌验证失败

**解决方法**：
- 确认令牌格式：`Authorization: Bearer {token}`
- 确认令牌未过期
- 确认 `JWT_SECRET` 与登录时一致

## 📚 更多信息

- [JWT规范](https://jwt.io/)
- [Spring Security文档](https://docs.spring.io/spring-security/reference/)
- [FISCO BCOS文档](https://fisco-bcos-documentation.readthedocs.io/)
