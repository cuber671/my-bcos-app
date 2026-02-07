# Service层架构说明

## 概述

Service层是系统的业务逻辑层，负责处理复杂的业务逻辑、数据库操作和区块链交互。Controller层通过调用Service层来实现业务功能。

## 架构设计

```
Controller → Service → Repository + Blockchain Contracts
              ↓
         Business Logic
              ↓
         Database + Blockchain
```

## 核心Service

### 1. EnterpriseService

**职责**：
- 企业注册与管理
- 信用评级管理
- 授信额度管理
- 企业信息查询

**主要方法**：
- `registerEnterprise(Enterprise)` - 注册企业
- `approveEnterprise(String address)` - 审核企业
- `updateCreditRating(String address, Integer rating)` - 更新信用评级
- `setCreditLimit(String address, BigDecimal limit)` - 设置授信额度
- `getEnterprise(String address)` - 获取企业信息
- `isEnterpriseValid(String address)` - 检查企业是否有效

**业务规则**：
- 信用代码必须唯一
- 区块链地址必须唯一
- 信用评级范围：0-100
- 只有ACTIVE状态的企业才能参与业务

### 2. ReceivableService

**职责**：
- 应收账款创建与确认
- 应收账款融资
- 应收账款转让
- 还款管理

**主要方法**：
- `createReceivable(Receivable)` - 创建应收账款
- `confirmReceivable(String receivableId)` - 确认应收账款
- `financeReceivable(String id, String financier, BigDecimal amount, Integer rate)` - 融资
- `repayReceivable(String id, BigDecimal amount)` - 还款
- `transferReceivable(String id, String newHolder)` - 转让
- `getReceivable(String id)` - 获取应收账款详情
- `getTotalAmountBySupplier(String address)` - 统计供应商总应收金额

**业务规则**：
- 应收账款ID必须唯一
- 供应商和核心企业必须存在且已激活
- 只有已确认的应收账款才能融资
- 融资金额不能超过应收账款金额
- 只有已融资的应收账款才能还款

## 区块链集成

### 集成方式

Service层通过注释的TODO标记了需要调用区块链合约的位置。当智能合约编译并部署后，取消这些注释即可启用区块链功能。

### 示例代码（企业注册）

```java
// 当前代码（仅数据库）
Enterprise saved = enterpriseRepository.save(enterprise);

// TODO: 调用区块链合约注册企业
// EnterpriseRegistry contract = loadEnterpriseRegistryContract();
// TransactionReceipt receipt = contract.registerEnterprise(
//     enterprise.getName(),
//     enterprise.getCreditCode(),
//     enterprise.getAddress(),
//     enterprise.getRole()
// );
// saved.setTxHash(receipt.getTransactionHash());
```

### 启用区块链集成的步骤

1. **编译智能合约**
   ```bash
   cd ~/fisco/console
   ./sol2java.sh org.fisco.bcos.sdk.demo.contract
   ```

2. **复制合约Java类到项目**
   ```bash
   cp sdk/src/main/java/org/fisco/bcos/sdk/demo/contract/*.java \
      my-bcos-app/src/main/java/com/fisco/app/contract/
   ```

3. **在Service中加载合约**
   ```java
   @Autowired
   private Client client;
   
   @Autowired
   private CryptoKeyPair cryptoKeyPair;
   
   private EnterpriseRegistry loadEnterpriseRegistryContract() {
       String contractAddress = getContractAddressFromConfig("EnterpriseRegistry");
       return EnterpriseRegistry.load(contractAddress, client, cryptoKeyPair);
   }
   ```

4. **取消TODO注释，启用区块链调用**

## 事务管理

Service层使用`@Transactional`注解确保数据一致性：

```java
@Transactional
public void financeReceivable(String receivableId, String financierAddress,
                               BigDecimal financeAmount, Integer financeRate) {
    // 1. 查询应收账款
    // 2. 验证状态
    // 3. 更新数据库
    // 4. 调用区块链合约
    // 所有操作在一个事务中，确保数据一致性
}
```

## 异常处理

Service层通过抛出`BusinessException`来处理业务异常：

```java
// 检查企业是否存在
if (!enterpriseRepository.existsByAddress(address)) {
    throw new EnterpriseNotFoundException(address);
}

// 检查信用评级范围
if (creditRating < 0 || creditRating > 100) {
    throw new BusinessException("信用评级必须在0-100之间");
}
```

Controller层的全局异常处理器会捕获这些异常并返回友好的错误信息。

## 日志记录

Service层使用Slf4j记录关键操作：

```java
log.info("创建应收账款: id={}, amount={}", receivableId, amount);
log.info("应收账款融资成功: id={}, txHash={}", receivableId, txHash);
log.error("应收账款融资失败: id={}, error={}", receivableId, error);
```

## 数据验证

Service层在执行业务逻辑前进行数据验证：

```java
// 验证企业状态
if (!enterpriseService.isEnterpriseValid(supplierAddress)) {
    throw new BusinessException("供应商不存在或未激活");
}

// 验证金额范围
if (financeAmount.compareTo(receivable.getAmount()) > 0) {
    throw new BusinessException("融资金额不能超过应收账款金额");
}

// 验证日期
if (dueDate.isBefore(issueDate)) {
    throw new BusinessException("到期日期必须晚于出票日期");
}
```

## 查询功能

Service层提供丰富的查询功能：

```java
// 根据状态查询
List<Receivable> receivables = receivableService.getReceivablesByStatus(
    Receivable.ReceivableStatus.FINANCED
);

// 根据持有人查询
List<Receivable> holderReceivables = receivableService.getHolderReceivables(address);

// 统计查询
BigDecimal totalAmount = receivableService.getTotalAmountBySupplier(supplierAddress);

// 时间范围查询
List<Receivable> dueSoon = receivableService.getDueSoonReceivables(
    LocalDateTime.now(),
    LocalDateTime.now().plusDays(30)
);
```

## 扩展指南

### 添加新的Service方法

1. 在Service接口中定义方法
2. 在ServiceImpl中实现业务逻辑
3. 添加事务控制（`@Transactional`）
4. 添加日志记录
5. 添加异常处理
6. 在Controller中调用

### 示例：添加批量融资功能

```java
// Service方法
@Transactional
public List<Receivable> batchFinance(List<String> receivableIds, 
                                     String financierAddress) {
    List<Receivable> results = new ArrayList<>();
    
    for (String id : receivableIds) {
        try {
            Receivable r = getReceivable(id);
            financeReceivable(id, financierAddress, 
                             r.getAmount(), 500);
            results.add(r);
        } catch (Exception e) {
            log.error("批量融资失败: id={}, error={}", id, e.getMessage());
        }
    }
    
    return results;
}

// Controller方法
@PostMapping("/batch-finance")
public Result<List<Receivable>> batchFinance(
        @RequestBody BatchFinanceRequest request) {
    List<Receivable> results = receivableService.batchFinance(
        request.getReceivableIds(),
        request.getFinancierAddress()
    );
    return Result.success(results);
}
```

## 性能优化建议

1. **批量操作**：对大量数据使用批量查询/更新
2. **延迟加载**：使用@EntityGraph优化关联查询
3. **缓存**：对频繁查询的数据添加缓存
4. **异步处理**：对耗时操作使用@Async注解
5. **分页查询**：使用Pageable进行分页

## 测试建议

### 单元测试示例

```java
@SpringBootTest
@Transactional
class ReceivableServiceTest {
    
    @Autowired
    private ReceivableService receivableService;
    
    @Test
    void testCreateReceivable() {
        Receivable receivable = new Receivable();
        receivable.setReceivableId("TEST001");
        // ... 设置其他属性
        
        Receivable saved = receivableService.createReceivable(receivable);
        
        assertNotNull(saved.getId());
        assertEquals("TEST001", saved.getReceivableId());
    }
    
    @Test
    void testFinanceReceivable_InvalidStatus() {
        assertThrows(BusinessException.class, () -> {
            receivableService.financeReceivable("INVALID_ID", 
                "0x123", new BigDecimal("1000"), 500);
        });
    }
}
```

## 总结

Service层是系统的核心，它：
- ✅ 封装业务逻辑
- ✅ 协调数据库和区块链操作
- ✅ 提供事务管理
- ✅ 处理异常和验证
- ✅ 记录操作日志
- ✅ 支持复杂查询

通过良好的分层设计，系统具有清晰的职责划分和良好的可维护性。
