# ReceivableWithOverdue 智能合约部署指南

## 📋 概述

本文档详细说明如何部署 `ReceivableWithOverdue` 智能合约到 FISCO BCOS 区块链，并集成到现有的应收账款逾期管理系统中。

---

## 🏗️ 合约架构

### 合约文件位置
```
/home/llm_rca/fisco/my-bcos-app/src/main/resources/contracts/ReceivableWithOverdue.sol
```

### 合约功能

#### 原有功能（继承自 Receivable.sol）
- ✅ 创建应收账款 (`createReceivable`)
- ✅ 确认应收账款 (`confirmReceivable`)
- ✅ 应收账款融资 (`financeReceivable`)
- ✅ 应收账款还款 (`repayReceivable`)

#### 新增逾期管理功能
- ✅ 更新逾期状态 (`updateOverdueStatus`)
- ✅ 记录催收 (`recordRemind`)
- ✅ 记录罚息 (`recordPenalty`)
- ✅ 记录坏账 (`recordBadDebt`)
- ✅ 更新坏账回收状态 (`updateBadDebtRecovery`)

---

## 📦 部署步骤

### 方式一：使用 FISCO 控制台部署（推荐）

#### 1. 启动 FISCO 控制台

```bash
cd /fisco/bcos-console
python3 console.py
```

#### 2. 编译合约

在控制台中执行：
```bash
# 进入合约目录
cd /home/llm_rca/fisco/my-bcos-app/src/main/resources/contracts

# 编译合约
solc --bin --abi --optimize -o ./build ReceivableWithOverdue.sol
```

#### 3. 部署合约

在控制台中执行：
```bash
# 部署合约
deploy ReceivableWithOverdue

# 输出示例：
# contract address: 0x1234567890123456789012345678901234567890
```

**保存返回的合约地址！** 后续配置需要使用。

---

### 方式二：使用 Java SDK 部署

#### 1. 创建部署测试类

创建 `src/test/java/com/fisco/app/contract/DeployReceivableWithOverdueTest.java`：

```java
package com.fisco.app.contract;

import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.crypto.keypair.CryptoKeyPair;
import org.fisco.bcos.sdk.v3.model.TransactionReceipt;
import org.fisco.bcos.sdk.v3.transaction.codec.ContractCodec;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@SpringBootTest
public class DeployReceivableWithOverdueTest {

    @Test
    public void deployContract() throws Exception {
        // 1. 读取合约 ABI 和 Bin
        String abi = new String(Files.readAllBytes(
            Paths.get("src/main/resources/contracts/build/ReceivableWithOverdue.abi")));
        String bin = new String(Files.readAllBytes(
            Paths.get("src/main/resources/contracts/build/ReceivableWithOverdue.bin")));

        // 2. 初始化客户端（使用现有配置）
        Client client = Client.build("src/main/resources/config.toml");
        CryptoKeyPair keyPair = client.getCryptoSuite().createKeyPair();

        // 3. 部署合约
        TransactionReceipt receipt = client.deploy(
            "ReceivableWithOverdue",
            abi,
            bin,
            keyPair
        ).send();

        // 4. 输出部署结果
        System.out.println("========================================");
        System.out.println("合约部署成功！");
        System.out.println("========================================");
        System.out.println("合约地址: " + receipt.getContractAddress());
        System.out.println("交易哈希: " + receipt.getTransactionHash());
        System.out.println("区块号: " + receipt.getBlockNumber());
        System.out.println("Gas使用: " + receipt.getGasUsed());
        System.out.println("========================================");

        // 5. 保存合约地址到配置文件
        System.out.println("\n请将以下配置添加到 application.yml:");
        System.out.println("\ncontracts:");
        System.out.println("  receivable-with-overdue: \"" + receipt.getContractAddress() + "\"");
    }
}
```

#### 2. 运行部署

```bash
cd /home/llm_rca/fisco/my-bcos-app
mvn test -Dtest=DeployReceivableWithOverdueTest
```

---

### 方式三：使用 Web3JS 部署

#### 1. 安装依赖

```bash
npm install --save web3
```

#### 2. 创建部署脚本 `deploy.js`

```javascript
const Web3 = require('web3');
const fs = require('fs');

// 配置
const web3 = new Web3('http://127.0.0.1:8545'); // FISCO BCOS 节点地址
const privateKey = 'YOUR_PRIVATE_KEY'; // 替换为实际私钥

async function deploy() {
    // 读取合约 ABI 和 Bin
    const abi = JSON.parse(fs.readFileSync('src/main/resources/contracts/build/ReceivableWithOverdue.abi'));
    const bytecode = '0x' + fs.readFileSync('src/main/resources/contracts/build/ReceivableWithOverdue.bin');

    // 创建合约对象
    const contract = new web3.eth.Contract(abi);

    // 获取账户
    const account = web3.eth.accounts.privateKeyToAccount(privateKey);
    web3.eth.accounts.wallet.add(account);

    // 估算 Gas
    const gas = await contract.deploy({
        data: bytecode,
        arguments: []
    }).estimateGas();

    // 部署合约
    const deployedContract = contract.deploy({
        data: bytecode,
        arguments: []
    });

    const tx = await deployedContract.send({
        from: account.address,
        gas: gas
    });

    console.log('========================================');
    console.log('合约部署成功！');
    console.log('========================================');
    console.log('合约地址:', tx.options.address);
    console.log('交易哈希:', tx.transactionHash);
    console.log('========================================');

    return tx.options.address;
}

deploy().catch(console.error);
```

#### 3. 运行部署

```bash
node deploy.js
```

---

## ⚙️ 配置集成

### 1. 更新 application.yml

在 `src/main/resources/application.yml` 中添加：

```yaml
# FISCO BCOS 配置
fisco:
  enabled: true
  # 其他现有配置...

# 智能合约配置
contracts:
  receivable-with-overdue: "0xYourDeployedContractAddressHere"  # 替换为实际部署的合约地址

# 或者保留原有配置，添加新配置
# contracts:
#   bill: "0x..."
#   receivable: "0x..."
#   receivable-with-overdue: "0x..."  # 新增
```

### 2. 更新 ContractService 配置

在 `ContractService.java` 中添加新合约的加载：

```java
@Value("${contracts.receivable-with-overdue:}")
private String receivableWithOverdueContractAddress;

// 合约实例
private ReceivableWithOverdue receivableWithOverdueContract;

@PostConstruct
public void init() {
    // ... 现有代码 ...

    // 加载 ReceivableWithOverdue 合约
    if (receivableWithOverdueContractAddress != null && !receivableWithOverdueContractAddress.isEmpty()) {
        receivableWithOverdueContract = ReceivableWithOverdue.load(
            receivableWithOverdueContractAddress,
            client,
            cryptoKeyPair
        );
        log.info("ReceivableWithOverdue contract loaded successfully at: {}", receivableWithOverdueContractAddress);
    } else {
        log.warn("ReceivableWithOverdue contract address not configured");
    }
}
```

### 3. 取消注释合约调用代码

在 `ContractService.java` 中，找到以下注释的代码块并取消注释：

#### recordRemindOnChain 方法（约 1707 行）
```java
// 取消注释以下代码：
TransactionReceipt txReceipt = receivableWithOverdueContract.recordRemind(
    receivableId,
    remindTypeValue,
    operatorAddress,
    remindTimestamp,
    remindHash
);
validateTransactionReceipt(txReceipt, receivableWithOverdueContractAddress, "recordRemind");
String txHash = txReceipt.getTransactionHash();
```

#### recordPenaltyOnChain 方法（约 1792 行）
```java
// 取消注释以下代码：
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

#### recordBadDebtOnChain 方法（约 1883 行）
```java
// 取消注释以下代码：
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

#### updateOverdueStatusOnChain 方法（约 1950 行）
```java
// 取消注释以下代码：
TransactionReceipt txReceipt = receivableWithOverdueContract.updateOverdueStatus(
    receivableId,
    overdueLevelValue,
    overdueDaysValue,
    updateTimestamp
);
validateTransactionReceipt(txReceipt, receivableWithOverdueContractAddress, "updateOverdueStatus");
String txHash = txReceipt.getTransactionHash();
```

同时需要取消注释变量定义：
```java
BigInteger remindTypeValue = convertRemindTypeToBigInteger(remindType);
BigInteger penaltyTypeValue = convertPenaltyTypeToBigInteger(penaltyType);
BigInteger dailyRateValue = dailyRate.multiply(new BigDecimal("10000")).toBigInteger();
BigInteger totalPenaltyAmountInFen = convertAmountToFen(totalPenaltyAmount);
BigInteger badDebtTypeValue = convertBadDebtTypeToBigInteger(badDebtType);
BigInteger overdueLevelValue = convertOverdueLevelToBigInteger(overdueLevel);
BigInteger overdueDaysValue = BigInteger.valueOf(overdueDays);
BigInteger updateTimestamp = convertDateTimeToTimestamp(LocalDateTime.now());
```

---

## 🧪 测试合约

### 1. 单元测试

创建 `src/test/java/com/fisco/app/contract/ReceivableWithOverdueTest.java`:

```java
package com.fisco.app.contract;

import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.crypto.keypair.CryptoKeyPair;
import org.fisco.bcos.sdk.v3.model.TransactionReceipt;
import org.fisco.bcos.sdk.v3.transaction.model.exceptions.ContractException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.PerClass)
public class ReceivableWithOverdueTest {

    private static Client client;
    private static CryptoKeyPair keyPair;
    private static String contractAddress;
    private static ReceivableWithOverdue contract;
    private static String adminAddress;
    private static String supplierAddress;
    private static String coreEnterpriseAddress;

    @BeforeAll
    public static void setUp() throws Exception {
        // 初始化客户端和密钥对
        client = Client.build("src/main/resources/config.toml");
        keyPair = client.getCryptoSuite().createKeyPair();
        adminAddress = keyPair.getAddress();
        supplierAddress = "0x1234567890123456789012345678901234567890"; // 测试地址
        coreEnterpriseAddress = "0xabcdefabcdefabcdefabcdefabcdefabcd"; // 测试地址

        // 加载已部署的合约
        contractAddress = System.getProperty("contract.address");
        assertNotNull(contractAddress, "请设置合约地址: -Dcontract.address=0x...");

        contract = ReceivableWithOverdue.load(contractAddress, client, keyPair);
    }

    @Test
    public void testCreateReceivable() throws Exception {
        String receivableId = "REC_TEST_001";
        BigInteger amount = BigInteger.valueOf(100000000L); // 1,000,000.00元（分）
        BigInteger issueDate = BigInteger.valueOf(System.currentTimeMillis() / 1000);
        BigInteger dueDate = issueDate.add(BigInteger.valueOf(90 * 24 * 60 * 60)); // 90天后

        TransactionReceipt receipt = contract.createReceivable(
            receivableId,
            coreEnterpriseAddress,
            amount,
            issueDate,
            dueDate,
            getRandomHash()
        ).send();

        assertEquals(0, receipt.getStatus());
        assertTrue(receipt.getTransactionReceipt().isStatusOK());
        System.out.println("✓ 创建应收账款成功: " + receivableId);
    }

    @Test
    public void testUpdateOverdueStatus() throws Exception {
        String receivableId = "REC_TEST_001";
        BigInteger overdueDays = BigInteger.valueOf(45);

        TransactionReceipt receipt = contract.updateOverdueStatus(
            receivableId,
            ReceivableWithOverdue.OverdueLevel.Mild,
            overdueDays
        ).send();

        assertEquals(0, receipt.getStatus());
        System.out.println("✓ 更新逾期状态成功");
    }

    @Test
    public void testRecordRemind() throws Exception {
        String receivableId = "REC_TEST_001";
        BigInteger remindDate = BigInteger.valueOf(System.currentTimeMillis() / 1000);

        TransactionReceipt receipt = contract.recordRemind(
            receivableId,
            ReceivableWithOverdue.RemindType.Email,
            adminAddress,
            remindDate,
            "您的应收账款已逾期，请尽快处理",
            getRandomHash()
        ).send();

        assertEquals(0, receipt.getStatus());
        System.out.println("✓ 记录催收成功");
    }

    @Test
    public void testRecordPenalty() throws Exception {
        String receivableId = "REC_TEST_001";
        BigInteger principalAmount = BigInteger.valueOf(100000000L);
        BigInteger overdueDays = BigInteger.valueOf(45);
        BigInteger dailyRate = BigInteger.valueOf(5); // 0.05% (×10000)
        BigInteger penaltyAmount = BigInteger.valueOf(22500000L); // 计算得出的罚息
        BigInteger totalPenaltyAmount = BigInteger.valueOf(22500000L);
        BigInteger calculateStartDate = BigInteger.valueOf(System.currentTimeMillis() / 1000 - 45 * 24 * 60 * 60);
        BigInteger calculateEndDate = BigInteger.valueOf(System.currentTimeMillis() / 1000);

        TransactionReceipt receipt = contract.recordPenalty(
            receivableId,
            ReceivableWithOverdue.PenaltyType.Auto,
            principalAmount,
            overdueDays,
            dailyRate,
            penaltyAmount,
            totalPenaltyAmount,
            calculateStartDate,
            calculateEndDate,
            getRandomHash()
        ).send();

        assertEquals(0, receipt.getStatus());
        System.out.println("✓ 记录罚息成功");
    }

    @Test
    public void testRecordBadDebt() throws Exception {
        String receivableId = "REC_TEST_001";
        BigInteger principalAmount = BigInteger.valueOf(100000000L);
        BigInteger overdueDays = BigInteger.valueOf(200);
        BigInteger totalPenaltyAmount = BigInteger.valueOf(50000000L);
        BigInteger totalLossAmount = principalAmount.add(totalPenaltyAmount);

        TransactionReceipt receipt = contract.recordBadDebt(
            receivableId,
            ReceivableWithOverdue.BadDebtType.Overdue180,
            principalAmount,
            overdueDays,
            totalPenaltyAmount,
            totalLossAmount,
            "逾期超过180天",
            getRandomHash()
        ).send();

        assertEquals(0, receipt.getStatus());
        System.out.println("✓ 记录坏账成功");
    }

    @Test
    public void testGetOverdueInfo() throws Exception {
        String receivableId = "REC_TEST_001";
        ReceivableWithOverdue.OverdueInfo memory info = contract.getOverdueInfo(receivableId);

        assertNotNull(info);
        assertNotNull(info.level);
        assertNotNull(info.overdueDays);
        System.out.println("逾期等级: " + info.level);
        System.out.println("逾期天数: " + info.overdueDays);
    }

    private byte[] getRandomHash() {
        return java.security.MessageDigest.getInstance("SHA-256")
            .digest(java.util.UUID.randomUUID().toString().getBytes());
    }
}
```

### 2. 运行测试

```bash
cd /home/llm_rca/fisco/my-bcos-app
mvn test -Dtest=ReceivableWithOverdueTest
```

---

## ✅ 验证部署

### 1. 检查合约地址

```bash
# 在控制台中查询
call ReceivableWithOverdue 0xYourContractAddress admin
```

### 2. 测试基本功能

使用 Postman 或 curl 测试 API：

```bash
# 1. 创建应收账款
curl -X POST "http://localhost:8080/api/receivable" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "id": "REC_TEST_001",
    "coreEnterpriseAddress": "0xabcdef...",
    "amount": 100000.00,
    "currency": "CNY",
    "issueDate": "2024-01-01T00:00:00",
    "dueDate": "2024-04-01T00:00:00",
    "description": "测试应收账款"
  }'

# 2. 催收记录上链
curl -X POST "http://localhost:8080/api/receivable/REC_TEST_001/remind" \
  -H "Content-Type: application/json" \
  -d '{
    "remindType": "EMAIL",
    "remindContent": "催收提醒"
  }'

# 3. 计算罚息
curl -X POST "http://localhost:8080/api/receivable/REC_TEST_001/penalty" \
  -H "Content-Type: application/json" \
  -d '{
    "penaltyType": "AUTO"
  }'

# 4. 认定坏账
curl -X POST "http://localhost:8080/api/receivable/REC_TEST_001/bad-debt" \
  -H "Content-Type: application/json" \
  -d '{
    "badDebtType": "OVERDUE_180",
    "badDebtReason": "逾期180天以上"
  }'
```

---

## 🔧 故障排查

### 问题 1: 合约部署失败

**原因**: 账户余额不足
**解决**: 确保部署账户有足够的 GAS

```bash
# 查询账户余额
getAccountBalance 0xYourAddress
```

### 问题 2: 合约调用失败

**原因**: 合约地址配置错误
**解决**: 检查 application.yml 中的合约地址是否正确

### 问题 3: 交易回滚

**原因**: 权限不足或参数错误
**解决**: 检查调用者是否为管理员、供应商或资金方

---

## 📊 合约 Gas 消耗估算

| 方法 | Gas 消耗 | 说明 |
|------|---------|------|
| createReceivable | ~200,000 | 创建应收账款 |
| confirmReceivable | ~50,000 | 确认应收账款 |
| updateOverdueStatus | ~40,000 | 更新逾期状态 |
| recordRemind | ~80,000 | 记录催收 |
| recordPenalty | ~100,000 | 记录罚息 |
| recordBadDebt | ~120,000 | 记录坏账 |

---

## 📝 部署检查清单

- [ ] 编译合约成功
- [ ] 部署合约到区块链
- [ ] 保存合约地址
- [ ] 更新 application.yml 配置
- [ ] 取消注释 ContractService 中的合约调用代码
- [ ] 重启应用程序
- [ ] 测试基本功能
- [ ] 测试上链功能
- [ ] 查看区块链浏览器确认交易

---

**状态**: 🟢 就绪
**版本**: v1.0
**日期**: 2026-02-03
