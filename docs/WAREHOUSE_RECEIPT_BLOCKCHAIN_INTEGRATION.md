# 仓单模块区块链集成接口清单

生成时间：2026-02-02
系统：FISCO BCOS 供应链金融平台

---

## 📊 总体统计

| 类别 | 数量 | 说明 |
|------|------|------|
| **区块链接口** | 13个 | ContractService中的上链方法 |
| **涉及业务接口** | 10个 | 调用区块链的API接口 |
| **智能合约** | 1个 | WarehouseReceipt.sol |
| **状态检查** | 1个 | @RequireOnChain AOP切面 |

---

## 🔗 区块链集成方法（ContractService）

### 1️⃣ 仓单基础操作（3个）

| 方法 | 功能 | 上链合约方法 | 使用场景 |
|------|------|------------|---------|
| `createReceiptOnChain()` | 创建仓单上链 | `createReceipt()` | 仓单审核通过后创建 |
| `verifyReceiptOnChain()` | 验证仓单上链 | `verifyReceipt()` | 验证仓单有效性 |
| `getBlockNumber()` | 获取区块号 | - | 获取交易所在区块 |

**调用流程：**
```
审核通过 → createReceiptOnChain（创建）→ verifyReceiptOnChain（验证）→ getBlockNumber（获取区块号）
```

### 2️⃣ 仓单转让操作（1个）

| 方法 | 功能 | 上链合约方法 | 使用场景 |
|------|------|------------|---------|
| `transferReceiptOnChain()` | 背书转让上链 | `transferReceipt()` | 背书转让确认后 |

**参数：**
- receiptId - 仓单ID
- newOwner - 新持单人地址
- endorsementType - 背书类型
- endorsementReason - 背书原因

### 3️⃣ 仓单融资操作（2个）

| 方法 | 功能 | 上链合约方法 | 使用场景 |
|------|------|------------|---------|
| `pledgeReceiptOnChain()` | 质押上链 | `pledgeReceipt()` | 质押确认 |
| `releaseReceiptOnChain()` | 释放上链 | `releaseReceipt()` | 还款释放 |

**pledgeReceiptOnChain参数：**
- receiptId - 仓单ID
- financialInstitutionAddress - 金融机构地址
- pledgeAmount - 质押金额
- financeRate - 融资利率
- pledgeContractNo - 质押合同号

### 4️⃣ 仓单冻结操作（2个）

| 方法 | 功能 | 上链合约方法 | 使用场景 |
|------|------|------------|---------|
| `freezeReceiptOnChain()` | 冻结上链 | `freezeReceipt()` | 管理员审核通过冻结 |
| `unfreezeReceiptOnChain()` | 解冻上链 | `unfreezeReceipt()` | 管理员解冻 |

**freezeReceiptOnChain参数：**
- receiptId - 仓单ID
- freezeReason - 冻结原因
- referenceNo - 参考编号（如法院文书号）

---

## 📡 涉及区块链的API接口

### 1️⃣ 仓单审核接口（1个）

**接口：** `POST /api/ewr/approve`

**区块链调用：**
```java
// 步骤1: 创建仓单上链
String txHash1 = contractService.createReceiptOnChain(receipt);

// 步骤2: 验证仓单上链
String txHash2 = contractService.verifyReceiptOnChain(receiptId);

// 步骤3: 获取区块号
Long blockNumber = contractService.getBlockNumber(txHash2);
```

**状态流转：**
```
DRAFT → PENDING_ONCHAIN →（上链中）→ NORMAL（成功）/ ONCHAIN_FAILED（失败）
```

**更新字段：**
- receiptStatus: NORMAL / ONCHAIN_FAILED
- txHash: 交易哈希
- blockNumber: 区块号
- blockchainStatus: SYNCED / FAILED
- blockchainTimestamp: 上链时间

---

### 2️⃣ 上链重试接口（1个）

**接口：** `POST /api/ewr/retry-blockchain/{id}`

**区块链调用：**
```java
// 步骤1: 创建仓单上链
String txHash1 = contractService.createReceiptOnChain(receipt);

// 步骤2: 验证仓单上链
String txHash2 = contractService.verifyReceiptOnChain(receiptId);

// 步骤3: 获取区块号
Long blockNumber = contractService.getBlockNumber(txHash2);
```

**状态流转：**
```
ONCHAIN_FAILED →（重试）→ NORMAL（成功）/ ONCHAIN_FAILED（失败）
```

**权限要求：**
- 仓储方
- 系统管理员

---

### 3️⃣ 背书转让接口（1个）

**接口：** `POST /api/endorsement/confirm`

**区块链调用：**
```java
// 背书转让上链
String txHash = contractService.transferReceiptOnChain(
    receiptId,
    newOwner,           // 新持单人地址
    endorsementType,    // 背书类型
    endorsementReason   // 背书原因
);

// 获取区块号
Long blockNumber = contractService.getBlockNumber(txHash);
```

**状态流转：**
```
PENDING → CONFIRMED → COMPLETED
receipt.holderAddress 更新为新持单人地址
```

**更新字段：**
- endorsement.status: CONFIRMED / COMPLETED
- endorsement.txHash: 交易哈希
- endorsement.blockNumber: 区块号
- receipt.holderAddress: 新持单人地址
- receipt.currentHolder: 新持单人名称
- receipt.endorsementCount: 背书次数+1
- receipt.lastEndorsementDate: 最后背书时间

---

### 4️⃣ 质押申请接口（1个）

**接口：** `POST /api/pledge/initiate` 或 `POST /api/pledge/apply`

**区块链调用：**
```java
// 质押上链
String txHash = contractService.pledgeReceiptOnChain(
    receiptId,
    financialInstitutionAddress,
    pledgeAmount,
    financeRate,
    pledgeContractNo
);

// 获取区块号
Long blockNumber = contractService.getBlockNumber(txHash);
```

**状态流转：**
```
NORMAL → PLEDGE_PENDING → PLEDGED
```

**更新字段：**
- pledge.pledgeStatus: PLEDGED
- pledge.txHash: 交易哈希
- pledge.blockNumber: 区块号
- receipt.isFinanced: true
- receipt.financeAmount: 融资金额
- receipt.financeRate: 融资利率
- receipt.financeDate: 融资日期
- receipt.financierAddress: 资金方地址
- receipt.pledgeContractNo: 质押合同号
- receipt.receiptStatus: PLEDGED

---

### 5️⃣ 还款释放接口（1个）

**接口：** `POST /api/pledge/release`

**区块链调用：**
```java
// 释放上链
String releaseTxHash = contractService.releaseReceiptOnChain(receiptId);

// 获取区块号
Long releaseBlockNumber = contractService.getBlockNumber(releaseTxHash);
```

**状态流转：**
```
PLEDGED → NORMAL
```

**更新字段：**
- pledge.releaseStatus: RELEASED
- pledge.releaseTxHash: 释放交易哈希
- pledge.releaseBlockNumber: 释放区块号
- pledge.releaseDate: 释放时间
- receipt.receiptStatus: NORMAL
- receipt.isFinanced: false

---

### 6️⃣ 冻结申请审核接口（1个）

**接口：** `POST /api/ewr/freeze-application/review`

**区块链调用：**
```java
// 冻结上链
String txHash = contractService.freezeReceiptOnChain(
    receiptId,
    freezeReason,
    referenceNo
);

// 获取区块号
Long blockNumber = contractService.getBlockNumber(txHash);
```

**状态流转：**
```
NORMAL → FROZEN
```

**更新字段：**
- application.requestStatus: APPROVED
- application.freezeTxHash: 冻结交易哈希
- application.blockNumber: 区块号
- receipt.receiptStatus: FROZEN
- receipt.blockchainTimestamp: 冻结时间

---

### 7️⃣ 解冻接口（1个）

**接口：** `POST /api/ewr/unfreeze`

**区块链调用：**
```java
// 解冻上链
String txHash = contractService.unfreezeReceiptOnChain(
    receiptId,
    "NORMAL"  // 目标状态
);

// 获取区块号
Long blockNumber = contractService.getBlockNumber(txHash);
```

**状态流转：**
```
FROZEN → NORMAL
```

**更新字段：**
- receipt.receiptStatus: NORMAL
- receipt.unfreezeReason: 解冻原因
- receipt.unfreezeTime: 解冻时间
- receipt.txHash: 解冻交易哈希
- receipt.blockNumber: 区块号

---

### 8️⃣ 提货接口（1个）

**接口：** `PUT /api/ewr/delivery/{id}`

**AOP状态检查：**
```java
@RequireOnChain(value = "提货", allowFailed = false)
```

**功能：**
- ✅ 检查仓单blockchainStatus是否为SYNCED
- ✅ 如果未上链或上链失败，抛出异常
- ✅ 只有已上链的仓单才能提货

**状态流转：**
```
NORMAL → DELIVERED
```

---

## 🔐 区块链状态验证（AOP）

### @RequireOnChain 注解

**位置：** `src/main/java/com/fisco/app/aspect/RequireOnChainAspect.java`

**功能：**
- 自动拦截未上链的关键操作
- 验证仓单的blockchainStatus
- allowFailed=false时，上链失败也无法操作

**使用示例：**
```java
@RequireOnChain(value = "提货", allowFailed = false)
public ElectronicWarehouseReceiptResponse updateActualDeliveryDate(String id, ...) {
    // 只有已上链的仓单才能提货
}
```

**检查逻辑：**
```java
if (blockchainStatus != BlockchainStatus.SYNCED) {
    if (blockchainStatus == BlockchainStatus.PENDING) {
        throw new RuntimeException("仓单正在上链中，请稍后再试");
    } else if (blockchainStatus == BlockchainStatus.FAILED) {
        throw new RuntimeException("仓单上链失败，无法提货");
    } else {
        throw new RuntimeException("仓单未上链，无法提货");
    }
}
```

---

## 📊 区块链状态字段

### BlockchainStatus 枚举

| 状态 | 说明 | 可执行操作 |
|------|------|-----------|
| **PENDING** | 待上链 | ⏳ 等待上链完成 |
| **SYNCED** | 已同步 | ✅ 可执行所有操作 |
| **FAILED** | 上链失败 | ⚠️ 需要重试或回滚 |

### 相关数据库字段

| 字段 | 类型 | 说明 |
|------|------|------|
| blockchain_status | VARCHAR(20) | 区块链状态枚举 |
| tx_hash | VARCHAR(128) | 最新交易哈希 |
| block_number | BIGINT | 区块号 |
| blockchain_timestamp | TIMESTAMP | 上链时间 |

---

## 🔄 完整的区块链上链流程

### 1. 仓单创建流程

```
货主创建仓单（DRAFT）
    ↓
仓储方审核
    ↓
状态变为 PENDING_ONCHAIN
    ↓
【区块链调用1】createReceiptOnChain（创建）
    ↓
【区块链调用2】verifyReceiptOnChain（验证）
    ↓
【区块链调用3】getBlockNumber（获取区块号）
    ↓
更新状态：blockchainStatus=SYNCED, receiptStatus=NORMAL
    ↓
保存：txHash, blockNumber, blockchainTimestamp
```

### 2. 背书转让流程

```
持单人发起背书
    ↓
被背书方确认
    ↓
【区块链调用】transferReceiptOnChain
    ↓
【区块链调用】getBlockNumber
    ↓
更新：holderAddress, currentHolder, endorsementCount++
    ↓
保存：txHash, blockNumber
```

### 3. 质押融资流程

```
持单人申请质押
    ↓
金融机构确认
    ↓
【区块链调用】pledgeReceiptOnChain
    ↓
【区块链调用】getBlockNumber
    ↓
更新：isFinanced=true, financeAmount, financierAddress
    ↓
状态变为：PLEDGED
    ↓
保存：txHash, blockNumber
```

### 4. 冻结流程

```
方式1：管理员直接冻结
    ↓
【区块链调用】freezeReceiptOnChain
    ↓
【区块链调用】getBlockNumber
    ↓
状态变为：FROZEN
    ↓
保存：txHash, blockNumber

方式2：仓储方申请冻结
    ↓
提交冻结申请
    ↓
管理员审核批准
    ↓
【区块链调用】freezeReceiptOnChain
    ↓
【区块链调用】getBlockNumber
    ↓
状态变为：FROZEN
    ↓
保存：txHash, blockNumber
```

---

## 🎯 区块链集成统计

### 按业务模块分类

| 业务模块 | 区块链方法数 | API接口数 | 上链场景 |
|---------|------------|----------|---------|
| **仓单基础管理** | 3 | 2 | 创建、验证、重试 |
| **背书转让** | 1 | 1 | 转让确认 |
| **质押融资** | 2 | 2 | 质押、释放 |
| **冻结解冻** | 2 | 2 | 冻结、解冻 |
| **提货管理** | 0 | 1 | 状态检查（AOP） |
| **总计** | **8** | **8** | - |

### 区块链调用分布

| 区块链方法 | 调用位置 | 调用次数 |
|-----------|---------|---------|
| createReceiptOnChain | 审核接口 | 创建时 |
| verifyReceiptOnChain | 审核接口 | 验证时 |
| getBlockNumber | 所有接口 | 每次上链后 |
| transferReceiptOnChain | 背书确认 | 背书时 |
| pledgeReceiptOnChain | 质押确认 | 质押时 |
| releaseReceiptOnChain | 还款释放 | 释放时 |
| freezeReceiptOnChain | 冻结审核 | 冻结时 |
| unfreezeReceiptOnChain | 解冻操作 | 解冻时 |

---

## ⚠️ 注意事项

### 1. 上链失败处理

**自动重试：**
- 只有上链失败的仓单可以重试
- 重试会清除之前的失败记录
- 重试失败会记录在备注中

**回滚草稿：**
- 放弃重试可以回滚到草稿状态
- 回滚后可以修改仓单重新提交
- 已质押或已转让的仓单不能回滚

### 2. 区块链状态检查

**@RequireOnChain使用场景：**
- 提货操作（不允许上链失败）
- 背书转让（建议已上链）
- 质押融资（建议已上链）

### 3. 交易哈希管理

- 每次成功的区块链操作都保存txHash
- 主仓单保存最新的txHash
- 背书、质押等子表保存各自的txHash
- 可通过txHash查询区块链浏览器

### 4. 区块号记录

- 记录交易所在的区块号
- 用于追踪区块链确认状态
- 可用于数据一致性验证

---

## 📈 智能合约

**合约名称：** WarehouseReceipt.sol
**合约地址：** 配置在 `contracts.warehouse-receipt.address`
**合约方法：**

```solidity
// 创建仓单
function createReceipt(...) returns (bool)

// 验证仓单
function verifyReceipt(string memory receiptId) returns (bool)

// 转让仓单
function transferReceipt(string memory receiptId, address newOwner, ...) returns (bool)

// 质押仓单
function pledgeReceipt(string memory receiptId, address financialInstitution, ...) returns (bool)

// 释放仓单
function releaseReceipt(string memory receiptId) returns (bool)

// 冻结仓单
function freezeReceipt(string memory receiptId, ...) returns (bool)

// 解冻仓单
function unfreezeReceipt(string memory receiptId, ...) returns (bool)
```

---

**文档版本：** v1.0
**最后更新：** 2026-02-02
**系统状态：** 运行中 ✅
**区块链网络：** FISCO BCOS
