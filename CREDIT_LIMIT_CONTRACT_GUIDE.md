# 信用额度智能合约部署和使用指南

## 1. 合约概述

`CreditLimit.sol` 是信用额度管理的智能合约，提供了完整的额度管理功能，包括：
- 创建信用额度
- 使用/释放额度
- 调整信用额度
- 冻结/解冻额度
- 风险等级管理

## 2. 部署步骤

### 2.1 编译合约

```bash
cd /home/llm_rca/fisco/my-bcos-app

# 编译Solidity合约
solc/src/main/resources/contracts/CreditLimit.sol --bin --abi --optimize -o build/contracts/
```

### 2.2 部署合约

使用FISCO BCOS控制台或SDK部署合约：

```bash
# 方法1: 使用控制台部署
cd ~/fisco/bcos-executor
./sol2java.sh org.fisco.bcos.sdk.demo.contract.CreditLimit

# 方法2: 使用Java SDK部署（DeployController）
# 发送POST请求到部署接口
```

### 2.3 配置合约地址

部署成功后，将合约地址配置到 `application.properties`:

```properties
# 信用额度合约地址
contracts.credit-limit.address=0x[部署后的合约地址]
```

## 3. 合约功能说明

### 3.1 创建信用额度

```solidity
function createCreditLimit(
    string memory limitId,
    address enterprise,
    string memory enterpriseName,
    LimitType limitType,
    uint256 totalLimit,
    uint256 warningThreshold,
    uint256 effectiveDate,
    uint256 expiryDate,
    bytes32 dataHash
) public onlyAdmin
```

**参数说明：**
- `limitId`: 额度ID（唯一标识）
- `enterprise`: 企业地址（区块链地址）
- `enterpriseName`: 企业名称
- `limitType`: 额度类型（0=融资额度, 1=担保额度, 2=赊账额度）
- `totalLimit`: 总额度（单位：分）
- `warningThreshold`: 预警阈值（百分比，1-100）
- `effectiveDate`: 生效日期（Unix时间戳）
- `expiryDate`: 失效日期（Unix时间戳，0表示永久有效）
- `dataHash`: 数据哈希（链下数据的SHA256哈希值）

**事件：**
```solidity
event CreditLimitCreated(
    string indexed limitId,
    address indexed enterprise,
    LimitType limitType,
    uint256 totalLimit,
    bytes32 dataHash
);
```

### 3.2 使用额度

```solidity
function useCredit(
    string memory limitId,
    uint256 amount,
    string memory businessType,
    string memory businessId,
    bytes32 dataHash
) public onlyExistingLimit(limitId)
```

**参数说明：**
- `limitId`: 额度ID
- `amount`: 使用金额（分）
- `businessType`: 业务类型（如：FINANCING_APPLICATION）
- `businessId`: 业务ID（关联业务表的主键）
- `dataHash`: 数据哈希

**前置条件：**
- 额度必须存在
- 额度状态必须为Active（生效中）
- 可用额度充足

**事件：**
```solidity
event CreditLimitUsed(
    string indexed limitId,
    UsageType usageType,
    uint256 amount,
    uint256 remainingLimit,
    bytes32 dataHash
);
```

### 3.3 释放额度

```solidity
function releaseCredit(
    string memory limitId,
    uint256 amount,
    string memory businessType,
    string memory businessId,
    bytes32 dataHash
) public onlyExistingLimit(limitId)
```

**前置条件：**
- 额度必须存在
- 已使用额度 >= 释放金额

### 3.4 调整信用额度

```solidity
function adjustCreditLimit(
    string memory limitId,
    AdjustType adjustType,
    uint256 newLimit,
    string memory reason,
    bytes32 dataHash
) public onlyAdmin onlyExistingLimit(limitId)
```

**参数说明：**
- `adjustType`: 调整类型（0=增加, 1=减少, 2=重置）
- `newLimit`: 新额度（分）
- `reason`: 调整原因

**调整规则：**
- `Increase`: 新额度必须大于当前额度
- `Decrease`: 新额度不能小于已使用额度
- `Reset`: 直接设置为新额度

### 3.5 冻结/解冻额度

```solidity
function freezeCreditLimit(string memory limitId, string memory reason)
    public onlyAdmin onlyExistingLimit(limitId)

function unfreezeCreditLimit(string memory limitId, string memory reason)
    public onlyAdmin onlyExistingLimit(limitId)
```

### 3.6 更新风险等级

```solidity
function updateRiskLevel(
    string memory limitId,
    RiskLevel newRiskLevel,
    string memory reason
) public onlyAdmin onlyExistingLimit(limitId)
```

**风险等级：**
- 0: Low（低风险）
- 1: Medium（中风险）
- 2: High（高风险）

## 4. 查询方法

### 4.1 查询额度详情

```solidity
function getCreditLimit(string memory limitId) public view returns (
    address enterprise,
    string memory enterpriseName,
    LimitType limitType,
    uint256 totalLimit,
    uint256 usedLimit,
    uint256 frozenLimit,
    uint256 availableLimit,
    LimitStatus status,
    RiskLevel riskLevel,
    uint256 overdueCount,
    uint256 badDebtCount
)
```

### 4.2 查询企业的额度列表

```solidity
function getEnterpriseLimits(address enterprise)
    public view returns (string[] memory)
```

### 4.3 查询使用记录数量

```solidity
function getUsageRecordCount(string memory limitId)
    public view returns (uint256)
```

### 4.4 查询调整记录数量

```solidity
function getAdjustRecordCount(string memory limitId)
    public view returns (uint256)
```

## 5. Java集成

### 5.1 合约加载

合约在应用启动时自动加载（见 `ContractService.init()`）：

```java
@Value("${contracts.credit-limit.address:}")
private String creditLimitContractAddress;

@PostConstruct
public void init() {
    if (creditLimitContractAddress != null && !creditLimitContractAddress.isEmpty()) {
        creditLimitContract = CreditLimit.load(
            creditLimitContractAddress, client, cryptoKeyPair);
        log.info("CreditLimit contract loaded successfully");
    }
}
```

### 5.2 上链方法调用

ContractService提供了以下上链方法：

```java
// 创建额度上链
String txHash = contractService.recordCreditLimitOnChain(creditLimit);

// 使用额度上链
String txHash = contractService.recordCreditUsageOnChain(usage);

// 调整额度上链
String txHash = contractService.recordCreditAdjustOnChain(adjustRequest);

// 冻结额度上链
String txHash = contractService.freezeCreditLimitOnChain(limitId, reason);

// 解冻额度上链
String txHash = contractService.unfreezeCreditLimitOnChain(limitId, reason);

// 更新风险等级上链
String txHash = contractService.updateRiskLevelOnChain(limitId, riskLevel, reason);
```

### 5.3 Service层集成

在CreditLimitService中，以下操作会自动上链：

1. **创建额度** - `createCreditLimit()` 方法
2. **使用额度** - `useCredit()` 方法
3. **释放额度** - `releaseCredit()` 方法
4. **冻结额度** - `freezeCreditLimit()` 方法
5. **解冻额度** - `unfreezeCreditLimit()` 方法
6. **审批调整** - `approveAdjust()` 方法（仅审批通过时上链）

**注意：** 上链失败不会影响数据库操作，系统会记录警告日志。

## 6. 事件监听

可以监听以下事件来实现业务逻辑：

### 6.1 额度创建事件

```solidity
event CreditLimitCreated(
    string indexed limitId,
    address indexed enterprise,
    LimitType limitType,
    uint256 totalLimit,
    bytes32 dataHash
);
```

**用途：** 通知企业额度已创建

### 6.2 额度使用事件

```solidity
event CreditLimitUsed(
    string indexed limitId,
    UsageType usageType,
    uint256 amount,
    uint256 remainingLimit,
    bytes32 dataHash
);
```

**用途：** 实时监控额度使用情况

### 6.3 额度调整事件

```solidity
event CreditLimitAdjusted(
    string indexed limitId,
    AdjustType adjustType,
    uint256 beforeLimit,
    uint256 afterLimit,
    bytes32 dataHash
);
```

**用途：** 通知额度变更

### 6.4 风险等级更新事件

```solidity
event RiskLevelUpdated(
    string indexed limitId,
    RiskLevel oldLevel,
    RiskLevel newLevel,
    string reason
);
```

**用途：** 触发预警或风控流程

## 7. 数据转换

### 7.1 金额单位

- **数据库存储：** 分（整数，Long类型）
- **合约存储：** 分（uint256）
- **前端显示：** 元（BigDecimal，保留2位小数）

**转换公式：**
```java
// 元转分
Long fen = yuan.multiply(new BigDecimal("100")).longValue();

// 分转元
BigDecimal yuan = new BigDecimal(fen).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
```

### 7.2 时间戳

```java
// LocalDateTime转Unix时间戳
BigInteger timestamp = BigInteger.valueOf(
    localDateTime.atZone(ZoneId.systemDefault()).toEpochSecond()
);

// Unix时间戳转LocalDateTime
LocalDateTime localDateTime = LocalDateTime.ofEpochSecond(
    timestamp.longValue(),
    0,
    ZoneId.systemDefault()
);
```

### 7.3 枚举类型

```java
// Java枚举转Solidity枚举
CreditLimit.CreditLimitType convert(CreditLimitType type) {
    switch (type) {
        case FINANCING: return CreditLimit.CreditLimitType.Financing;
        case GUARANTEE: return CreditLimit.CreditLimitType.Guarantee;
        case CREDIT: return CreditLimit.CreditLimitType.Credit;
    }
}
```

## 8. 测试建议

### 8.1 单元测试

```java
@Test
public void testCreateCreditLimit() {
    // 准备测试数据
    CreditLimitCreateRequest request = new CreditLimitCreateRequest();
    request.setEnterpriseAddress("0x123...");
    request.setLimitType(CreditLimitType.FINANCING);
    request.setTotalLimit(new BigDecimal("1000000.00"));

    // 调用服务
    CreditLimitDTO result = creditLimitService.createCreditLimit(request, "admin");

    // 验证结果
    assertNotNull(result.getId());
    assertNotNull(result.getTxHash()); // 验证上链成功
}
```

### 8.2 集成测试

```java
@Test
public void testCreditLimitWorkflow() {
    // 1. 创建额度
    CreditLimitDTO limit = createLimit();

    // 2. 使用额度
    useCredit(limit.getId(), new BigDecimal("50000.00"));

    // 3. 查询额度
    CreditLimitDTO updated = getCreditLimit(limit.getId());
    assertEquals(new BigDecimal("950000.00"), updated.getAvailableLimit());

    // 4. 释放额度
    releaseCredit(limit.getId(), new BigDecimal("50000.00"));

    // 5. 调整额度
    adjustCreditLimit(limit.getId(), new BigDecimal("1500000.00"));
}
```

## 9. 故障排查

### 9.1 合约未加载

**现象：** 日志显示 "CreditLimit合约未加载，跳过上链操作"

**解决方案：**
1. 检查 `application.properties` 中的合约地址配置
2. 确认合约已成功部署
3. 验证合约地址格式正确（0x开头的40位十六进制）

### 9.2 上链失败

**现象：** 日志显示 "上链失败，但数据库操作成功"

**可能原因：**
1. 区块链节点未启动
2. 网络连接问题
3. 合约地址错误
4. 账户权限不足

**排查步骤：**
1. 检查区块链节点状态
2. 验证网络连接
3. 检查账户私钥配置
4. 查看完整错误日志

### 9.3 数据不一致

**现象：** 数据库数据与链上数据不一致

**解决方案：**
1. 检查事务是否正确提交
2. 验证上链调用是否成功
3. 查看区块链交易回执
4. 必要时使用数据修复工具

## 10. 安全建议

1. **权限控制：**
   - 只有管理员可以创建和调整额度
   - 使用 `onlyAdmin` 修饰器保护敏感操作

2. **数据验证：**
   - 验证所有输入参数
   - 检查业务规则（如可用额度充足性）

3. **异常处理：**
   - 上链失败不应影响数据库操作
   - 记录详细日志便于排查

4. **数据隐私：**
   - 链上只存储核心数据
   - 详细数据存储在链下，使用哈希值验证

5. **Gas优化：**
   - 使用事件记录历史
   - 合理设置数据有效期
   - 避免存储大文本

## 11. 后续扩展

### 11.1 定时任务
- 自动检查额度到期情况
- 自动评估风险等级
- 自动生成预警

### 11.2 数据分析
- 额度使用趋势分析
- 风险等级分布统计
- 逾期率与额度关系分析

### 11.3 通知服务
- 额度即将到期通知
- 使用率超标预警
- 额度调整通知

---

**版本：** v1.0
**最后更新：** 2026-02-03
**维护者：** FISCO BCOS开发团队
