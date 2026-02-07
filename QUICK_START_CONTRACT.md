# 智能合约快速部署指南

## 🚀 快速开始（5分钟完成）

### 前置条件
- ✅ FISCO BCOS 区块链已运行
- ✅ Java 开发环境已配置
- ✅ 项目已编译成功

---

## 📝 步骤 1: 编译合约（1分钟）

```bash
cd /home/llm_rca/fisco/my-bcos-app

# 创建编译输出目录
mkdir -p src/main/resources/contracts/build

# 编译合约
solc --bin --abi --optimize -o src/main/resources/contracts/build \
  src/main/resources/contracts/ReceivableWithOverdue.sol
```

**预期输出:**
```
Compiler run successful. ABI and bytecode generated.
```

---

## 📝 步骤 2: 部署合约（2分钟）

### 方式 A: 使用 FISCO 控制台（推荐）

```bash
# 启动控制台
cd /fisco/bcos-console
python3 console.py

# 在控制台中执行
deploy ReceivableWithOverdue
```

**保存返回的合约地址，例如:**
```
contract address: 0x1234567890123456789012345678901234567890
```

### 方式 B: 使用 Java 测试类

```bash
cd /home/llm_rca/fisco/my-bcos-app

# 运行部署测试
mvn exec:java -Dexec.mainClass="com.fisco.app.contract.DeployReceivableWithOverdueTest" \
  -Dexec.classpathScope=compile
```

---

## 📝 步骤 3: 更新配置（1分钟）

编辑 `src/main/resources/application.yml`，添加：

```yaml
# 智能合约配置
contracts:
  receivable-with-overdue: "0xYourDeployedContractAddressHere"
```

---

## 📝 步骤 4: 取消注释合约调用代码（1分钟）

编辑 `ContractService.java`，找到以下方法并取消注释：

### recordRemindOnChain (约 1707 行)
```java
// 取消注释:
BigInteger remindTypeValue = convertRemindTypeToBigInteger(remindType);
// ... 其他变量

// 取消注释并替换临时代码:
TransactionReceipt txReceipt = receivableWithOverdueContract.recordRemind(
    receivableId,
    remindTypeValue,
    operatorAddress,
    remindTimestamp,
    remindHash
);
validateTransactionReceipt(txReceipt, receivableWithOverdueContractAddress, "recordRemind");
String txHash = txReceipt.getTransactionHash();

// 删除或注释临时代码:
// log.warn("recordRemind方法需要在Receivable合约中实现，当前使用模拟交易哈希");
// String txHash = "0x" + java.util.UUID.randomUUID().toString().replace("-", "");
```

### recordPenaltyOnChain (约 1792 行)
```java
// 取消注释变量:
BigInteger penaltyTypeValue = convertPenaltyTypeToBigInteger(penaltyType);
BigInteger dailyRateValue = dailyRate.multiply(new BigDecimal("10000")).toBigInteger();
BigInteger totalPenaltyAmountInFen = convertAmountToFen(totalPenaltyAmount);

// 取消注释并替换:
TransactionReceipt txReceipt = receivableWithOverdueContract.recordPenalty(
    receivableId,
    penaltyTypeValue,
    principalAmountInFen,
    overdueDaysValue,
    dailyRateValue,
    penaltyAmountInFen,
    totalPenaltyAmountInFen,
    calculateStartDate,
    calculateEndDate,
    penaltyHash
);
validateTransactionReceipt(txReceipt, receivableWithOverdueContractAddress, "recordPenalty");
String txHash = txReceipt.getTransactionHash();
```

### recordBadDebtOnChain (约 1883 行)
```java
// 取消注释变量:
BigInteger badDebtTypeValue = convertBadDebtTypeToBigInteger(badDebtType);

// 取消注释并替换:
TransactionReceipt txReceipt = receivableWithOverdueContract.recordBadDebt(
    receivableId,
    badDebtTypeValue,
    principalAmountInFen,
    overdueDaysValue,
    totalPenaltyAmountInFen,
    totalLossAmountInFen,
    badDebtReason != null ? badDebtReason : "",
    badDebtTimestamp,
    badDebtHash
);
validateTransactionReceipt(txReceipt, receivableWithOverdueContractAddress, "recordBadDebt");
String txHash = txReceipt.getTransactionHash();
```

### updateOverdueStatusOnChain (约 1950 行)
```java
// 取消注释变量:
BigInteger overdueLevelValue = convertOverdueLevelToBigInteger(overdueLevel);
BigInteger overdueDaysValue = BigInteger.valueOf(overdueDays);
BigInteger updateTimestamp = convertDateTimeToTimestamp(LocalDateTime.now());

// 取消注释并替换:
TransactionReceipt txReceipt = receivableWithOverdueContract.updateOverdueStatus(
    receivableId,
    overdueLevelValue,
    overdueDaysValue,
    updateTimestamp
);
validateTransactionReceipt(txReceipt, receivableWithOverdueContractAddress, "updateOverdueStatus");
String txHash = txReceipt.getTransactionHash();
```

---

## 📝 步骤 5: 重启应用（30秒）

```bash
cd /home/llm_rca/fisco/my-bcos-app

# 重新编译
mvn clean compile

# 重启应用
mvn spring-boot:run
```

---

## ✅ 步骤 6: 测试验证

### 测试催收记录上链

```bash
curl -X POST "http://localhost:8080/api/receivable/{id}/remind" \
  -H "Content-Type: application/json" \
  -d '{
    "remindType": "EMAIL",
    "remindLevel": "NORMAL",
    "remindContent": "催收提醒测试"
  }'
```

**预期响应:**
```json
{
  "code": 200,
  "message": "催收记录创建成功",
  "data": {
    "id": "...",
    "txHash": "0x..."
  }
}
```

### 测试罚息计算上链

```bash
curl -X POST "http://localhost:8080/api/receivable/{id}/penalty" \
  -H "Content-Type: application/json" \
  -d '{
    "penaltyType": "AUTO"
  }'
```

### 测试坏账认定上链

```bash
curl -X POST "http://localhost:8080/api/receivable/{id}/bad-debt" \
  -H "Content-Type: application/json" \
  -d '{
    "badDebtType": "OVERDUE_180",
    "badDebtReason": "测试坏账认定"
  }'
```

---

## 🔍 验证上链成功

### 方法 1: 查看应用日志

```bash
tail -f logs/spring.log | grep "上链成功"
```

**预期输出:**
```
✓ 催收记录已上链: remindRecordId=..., txHash=0x...
✓ 罚息记录已上链: penaltyRecordId=..., txHash=0x...
✓ 坏账记录已上链: badDebtRecordId=..., txHash=0x...
```

### 方法 2: 使用区块链浏览器

访问 FISCO BCOS 浏览器，输入交易哈希查询交易详情。

---

## ❗ 常见问题

### Q1: 编译失败 - solc 命令未找到

**解决:**
```bash
# 安装 Solidity 编译器
sudo apt-get install solc

# 或使用 Docker
docker pull ethereum/solc:stable
docker run -v $(pwd):/work ethereum/solc:stable --bin solc /work/contract.sol
```

### Q2: 部署失败 - 账户余额不足

**解决:**
```bash
# 查询账户余额
getAccountBalance 0xYourAddress

# 如需测试币，使用控制台转账
sendAccountBalance 0xDeployerAddress 0xYourAddress 100000000
```

### Q3: 合约调用失败 - 合约地址错误

**解决:**
- 检查 application.yml 中的合约地址是否正确
- 确认合约已成功部署
- 重新启动应用

### Q4: 交易回滚 - Gas 不足

**解决:**
```bash
# 增加 Gas 限制
# 在 application.yml 中添加:
fisco:
  gas-limit: 30000000
```

---

## 📚 更多信息

详细文档请参阅:
- 📄 [CONTRACT_DEPLOYMENT.md](./CONTRACT_DEPLOYMENT.md) - 完整部署文档
- 📘 [FISCO BCOS 官方文档](https://fisco-bcos-documentation.readthedocs.io/zh_CN/latest/)

---

**状态**: 🟢 可用
**更新时间**: 2026-02-03
**维护者**: FISCO BCOS 开发团队
