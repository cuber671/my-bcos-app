# JWT区块链地址认证功能实现说明

## 修改概述

已将企业区块链地址集成到JWT Token中，实现基于地址的权限验证。

## 修改的文件

### 1. JwtTokenProvider.java
- **新增方法** `generateTokenWithAddress()`: 生成包含区块链地址的JWT Token
- **新增方法** `getEnterpriseAddressFromToken()`: 从Token中提取企业区块链地址
- **保留方法** `generateEnhancedToken()`: 向后兼容，不包含地址

```java
// Token中包含的Claims:
{
  "sub": "用户名",
  "enterpriseId": "企业ID（UUID）",
  "role": "角色",
  "loginType": "登录类型",
  "enterpriseAddress": "0x1234...5678"  // 新增
}
```

### 2. UserAuthentication.java
- **新增字段** `enterpriseAddress`: 存储用户的企业区块链地址
- **新增方法** `isHolder(String address)`: 检查用户是否是指定地址的持单人

### 3. JwtAuthenticationFilter.java
- 从Token中提取 `enterpriseAddress` claim
- 创建包含地址的 `UserAuthentication` 对象
- 设置到 Spring Security Context

### 4. AuthController.java
- **userLogin()**: 查询企业地址，生成带地址的Token，响应中包含 `enterpriseAddress`
- **enterpriseLogin()**: 使用企业地址生成带地址的Token
- **authenticateWithApiKey()**: API密钥认证时也包含地址

### 5. PermissionChecker.java
- **新增方法** `checkHolderPermission()`: 验证用户是否是仓单持单人（基于地址）
- **新增方法** `getCurrentUserAddress()`: 获取当前用户的区块链地址

## 使用方法

### 基本用法：获取当前用户地址

```java
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.fisco.app.security.UserAuthentication;
import com.fisco.app.security.PermissionChecker;

@Autowired
private PermissionChecker permissionChecker;

public void someMethod() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    // 方式1: 使用 PermissionChecker（推荐）
    String address = permissionChecker.getCurrentUserAddress(auth);

    // 方式2: 直接转换
    if (auth instanceof UserAuthentication) {
        UserAuthentication userAuth = (UserAuthentication) auth;
        String address = userAuth.getEnterpriseAddress();
    }
}
```

### 权限验证：检查持单人权限

```java
import com.fisco.app.entity.ElectronicWarehouseReceipt;

public void transferEndorsement(String receiptId, String endorseTo) {
    // 1. 获取当前认证信息
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    // 2. 查询仓单
    ElectronicWarehouseReceipt receipt = receiptRepository.findById(receiptId)
        .orElseThrow(() -> new RuntimeException("仓单不存在"));

    // 3. 验证持单人权限
    permissionChecker.checkHolderPermission(
        auth,
        receipt.getHolderAddress(),  // 仓单的持单人地址
        "背书转让"                   // 操作名称
    );

    // 4. 执行业务逻辑...
}
```

### 冻结/解冻仓单权限验证

```java
public ReceiptFreezeResponse freezeReceipt(ReceiptFreezeRequest request) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    ElectronicWarehouseReceipt receipt = repository.findById(request.getReceiptId())
        .orElseThrow(() -> new RuntimeException("仓单不存在"));

    // 系统管理员可以直接冻结
    if (((UserAuthentication) auth).isSystemAdmin()) {
        // 管理员逻辑
    } else {
        // 检查是否是持单人
        permissionChecker.checkHolderPermission(
            auth,
            receipt.getHolderAddress(),
            "冻结仓单"
        );
    }

    // 执行冻结逻辑...
}
```

### 仓单拆分权限验证

```java
public void splitReceipt(String receiptId, SplitRequest request) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    ElectronicWarehouseReceipt receipt = repository.findById(receiptId)
        .orElseThrow(() -> new RuntimeException("仓单不存在"));

    // 只有当前持单人可以拆分仓单
    permissionChecker.checkHolderPermission(
        auth,
        receipt.getHolderAddress(),
        "拆分仓单"
    );

    // 执行拆分逻辑...
}
```

## 兼容性说明

- **向后兼容**: 旧版本的 `generateEnhancedToken()` 方法仍然可用
- **新Token**: 所有登录方法现在都会生成包含地址的Token
- **旧Token**: 如果Token中没有地址，`getEnterpriseAddress()` 返回 null

## 测试示例

### 1. 测试企业登录

```bash
# 企业登录请求
curl -X POST http://localhost:8080/api/auth/enterprise-login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "enterprise_001",
    "password": "Pass123456"
  }'

# 响应包含:
{
  "code": 0,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "address": "0x1234567890abcdef1234567890abcdef12345678",
    "enterpriseName": "供应商A",
    "role": "SUPPLIER",
    "loginType": "ENTERPRISE"
  }
}
```

### 2. 使用Token进行操作

```bash
# 使用Token进行背书转让
curl -X POST http://localhost:8080/api/ewr/endorsement/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..." \
  -d '{
    "receiptId": "xxx",
    "endorseTo": "0xabcdabcdabcdabcdabcdabcdabcdabcdabcdabcd",
    "endorseToName": "新持单人",
    "endorsementType": "TRANSFER",
    "endorsementReason": "贸易融资"
  }'

# 系统会自动验证:
# 1. Token中的 enterpriseAddress
# 2. 仓单的 holderAddress
# 3. 两者必须一致（或用户是系统管理员）
```

## 注意事项

1. **地址匹配**: 地址比较使用 `equalsIgnoreCase()`，不区分大小写
2. **null处理**: 如果用户没有关联企业或地址不存在，`getEnterpriseAddress()` 返回 null
3. **系统管理员**: 系统管理员（`SUPER_ADMIN`）可以绕过地址验证
4. **审计日志**: 所有权限验证都会自动记录审计日志

## 相关实体字段

| 实体 | 字段 | 说明 |
|-----|------|------|
| Enterprise | address | 企业区块链地址（唯一，42字符） |
| ElectronicWarehouseReceipt | holderAddress | 持单人地址（可背书转让） |
| ElectronicWarehouseReceipt | ownerAddress | 货主地址（不可变） |
| ElectronicWarehouseReceipt | warehouseAddress | 仓储方地址（不可变） |

## 后续优化建议

1. 添加地址格式验证（42字符，0x开头）
2. 考虑在仓单表中添加 `holder_id` 字段，实现ID和地址双重验证
3. 对于频繁的地址查询，可以考虑添加缓存
