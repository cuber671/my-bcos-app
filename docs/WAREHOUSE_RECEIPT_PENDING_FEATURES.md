# 仓单模块待实现功能清单

生成时间：2026-02-02
系统：FISCO BCOS 供应链金融平台

---

## 📊 当前实现状态

| 功能模块 | 已实现接口 | 完成度 | 状态 |
|---------|----------|--------|------|
| 电子仓单基础管理 | 22个 | 100% | ✅ 完成 |
| 背书转让 | 10个 | 100% | ✅ 完成 |
| 质押融资 | 10个 | 100% | ✅ 完成 |
| 冻结/解冻 | 7个 | 100% | ✅ 完成 |
| 提货管理 | 1个 | 100% | ✅ 完成 |
| 统计查询 | 2个 | 100% | ✅ 完成 |
| 区块链集成 | 8个方法 | 100% | ✅ 完成 |
| **总计** | **60个** | **100%** | **核心功能完成** |

---

## ⏭️ 待实现功能

### 1️⃣ 仓单拆分功能 🎯 优先级：高

#### 业务需求

**场景描述：**
货主持有一张大额仓单（如1000吨钢材），需要将其中一部分（如300吨）转让给第三方，或者将一部分用于质押融资。此时需要对仓单进行拆分。

**业务价值：**
- ✅ 提高仓单流动性
- ✅ 支持灵活的货物管理
- ✅ 便于部分质押或部分转让
- ✅ 降低交易成本

#### 功能设计

**1. 拆分申请接口**

```java
POST /api/ewr/split/apply
```

**请求参数：**
```json
{
  "parentReceiptId": "父仓单ID",
  "splitReason": "拆分原因",
  "splits": [
    {
      "goodsName": "货物名称",
      "quantity": 300.00,
      "unitPrice": 4500.00,
      "totalValue": 1350000.00,
      "storageLocation": "存储位置",
      "remarks": "备注"
    },
    {
      "goodsName": "货物名称",
      "quantity": 700.00,
      "unitPrice": 4500.00,
      "totalValue": 3150000.00,
      "storageLocation": "存储位置",
      "remarks": "备注"
    }
  ]
}
```

**业务规则：**
- ✅ 只有当前持单人可以申请拆分
- ✅ 拆分后的数量总和必须等于原仓单数量
- ✅ 拆分后的总价值必须等于原仓单价值
- ✅ 只能拆分NORMAL状态的仓单
- ✅ 已冻结、已质押、已过期的仓单不能拆分
- ✅ 至少拆分成2个子仓单
- ✅ 验证：`receipt.holderAddress == 当前用户的企业地址`

**状态流转：**
```
父仓单：NORMAL → SPLITTING（拆分中）
创建拆分申请记录
```

---

**2. 拆分审核接口**

```java
POST /api/ewr/split/approve
```

**请求参数：**
```json
{
  "applicationId": "拆分申请ID",
  "approvalResult": "APPROVED/REJECTED",
  "approvalComments": "审核意见"
}
```

**权限要求：**
- ✅ 管理员审核
- ✅ 仓储方可以审核（关联的仓单）

**审核通过后执行：**
1. 验证拆分规则
2. 生成子仓单（2个或更多）
3. 更新父仓单状态为SPLIT
4. 所有子仓单上链
5. 更新区块链状态

---

**3. 拆分执行逻辑**

**数据模型：**

**父仓单更新：**
```java
parentReceipt.setReceiptStatus(ReceiptStatus.SPLIT);
parentReceipt.setSplitTime(LocalDateTime.now());
parentReceipt.setSplitCount(splits.size()); // 子仓单数量
parentReceipt.setBlockchainStatus(BlockchainStatus.SYNCED);
```

**子仓单创建：**
```java
for (SplitRequest split : splits) {
    ElectronicWarehouseReceipt childReceipt = new ElectronicWarehouseReceipt();

    // 继承父仓单的基础信息
    childReceipt.setParentReceiptId(parentReceipt.getId());
    childReceipt.setWarehouseId(parentReceipt.getWarehouseId());
    childReceipt.setWarehouseAddress(parentReceipt.getWarehouseAddress());
    childReceipt.setWarehouseName(parentReceipt.getWarehouseName());
    childReceipt.setOwnerId(parentReceipt.getOwnerId());
    childReceipt.setOwnerAddress(parentReceipt.getOwnerAddress());
    childReceipt.setOwnerName(parentReceipt.getOwnerName());
    childReceipt.setHolderAddress(parentReceipt.getHolderAddress());
    childReceipt.setCurrentHolder(parentReceipt.getCurrentHolder());

    // 使用拆分请求的货物信息
    childReceipt.setGoodsName(split.getGoodsName());
    childReceipt.setQuantity(split.getQuantity());
    childReceipt.setUnitPrice(split.getUnitPrice());
    childReceipt.setTotalValue(split.getTotalValue());
    childReceipt.setStorageLocation(split.getStorageLocation());

    // 生成新的仓单编号
    childReceipt.setReceiptNo(generateChildReceiptNo(parentReceipt.getReceiptNo(), index));

    // 状态为NORMAL
    childReceipt.setReceiptStatus(ReceiptStatus.NORMAL);

    // 保存子仓单
    repository.save(childReceipt);

    // 上链
    String txHash = contractService.createReceiptOnChain(childReceipt);
    contractService.verifyReceiptOnChain(childReceipt.getId());
    Long blockNumber = contractService.getBlockNumber(txHash);

    childReceipt.setTxHash(txHash);
    childReceipt.setBlockNumber(blockNumber);
    childReceipt.setBlockchainStatus(BlockchainStatus.SYNCED);
    childReceipt.setBlockchainTimestamp(LocalDateTime.now());
    repository.save(childReceipt);
}
```

**子仓单编号规则：**
```
父仓单编号：EWR20260126000001
子仓单编号：EWR20260126000001-1, EWR20260126000001-2, ...
```

---

**4. 查询接口**

```java
// 查询子仓单列表
GET /api/ewr/children/{parentReceiptId}

// 查询父仓单
GET /api/ewr/parent/{childReceiptId}
```

---

#### 需要创建的组件

**1. DTO类**
- `SplitApplicationRequest.java` - 拆分申请请求
- `SplitApplicationResponse.java` - 拆分申请响应
- `SplitApprovalRequest.java` - 拆分审核请求
- `SplitApprovalResponse.java` - 拆分审核响应
- `ChildReceiptResponse.java` - 子仓单响应

**2. 实体类**
- `ReceiptSplitApplication.java` - 拆分申请记录
  - applicationId
  - parentReceiptId
  - splitReason
  - splitCount
  - requestStatus (PENDING, APPROVED, REJECTED)
  - applicantId
  - applicantName
  - reviewerId
  - reviewerName
  - reviewTime
  - reviewComments
  - createdAt
  - updatedAt

**3. Repository**
- `ReceiptSplitApplicationRepository.java`

**4. Service方法**
- `submitSplitApplication()` - 提交拆分申请
- `approveSplitApplication()` - 审核拆分申请
- `executeSplit()` - 执行拆分逻辑
- `getChildReceipts()` - 查询子仓单
- `getParentReceipt()` - 查询父仓单

**5. Controller接口**
- `POST /api/ewr/split/apply` - 提交拆分申请
- `POST /api/ewr/split/approve` - 审核拆分申请
- `GET /api/ewr/split/pending` - 查询待审核的拆分申请
- `GET /api/ewr/children/{parentReceiptId}` - 查询子仓单
- `GET /api/ewr/parent/{childReceiptId}` - 查询父仓单

**6. 智能合约方法**（可选）
- `splitReceipt(string parentId, string[] childIds)` - 拆分仓单上链

---

#### 数据库表设计

**表名：** `receipt_split_application`

```sql
CREATE TABLE receipt_split_application (
    id VARCHAR(36) PRIMARY KEY,
    parent_receipt_id VARCHAR(36) NOT NULL,
    split_reason TEXT,
    split_count INT NOT NULL,
    request_status VARCHAR(20) NOT NULL,
    applicant_id VARCHAR(36),
    applicant_name VARCHAR(100),
    reviewer_id VARCHAR(36),
    reviewer_name VARCHAR(100),
    review_time DATETIME,
    review_comments TEXT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_parent_receipt (parent_receipt_id),
    INDEX idx_status (request_status),
    INDEX idx_applicant (applicant_id)
);
```

---

#### 权限验证

**提交申请：**
```java
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
UserAuthentication userAuth = (UserAuthentication) auth;

// 验证持单人权限
permissionChecker.checkHolderPermission(
    auth,
    receipt.getHolderAddress(),
    "拆分仓单"
);
```

**审核申请：**
```java
// 管理员可直接审核
if (userAuth.isSystemAdmin()) {
    // 允许审核
}

// 仓储方可以审核关联的仓单
if (receipt.getWarehouseId().equals(userAuth.getEnterpriseId())) {
    // 允许审核
}
```

---

#### 预期工作量

| 任务 | 工作量 | 说明 |
|------|--------|------|
| 创建DTO类 | 5个 | 约2小时 |
| 创建实体类 | 1个 | 约1小时 |
| 创建Repository | 1个 | 约0.5小时 |
| 实现Service方法 | 5个 | 约4小时 |
| 实现Controller接口 | 5个 | 约2小时 |
| 权限验证 | - | 约1小时 |
| 单元测试 | - | 约2小时 |
| 集成测试 | - | 约2小时 |
| **总计** | - | **约14.5小时（2个工作日）** |

---

### 2️⃣ 仓单作废功能 🎯 优先级：中

#### 业务需求

**场景描述：**
1. 货物因质量问题、损坏等原因无法继续使用
2. 仓单信息错误且无法修正
3. 法律纠纷需要作废仓单
4. 货主主动申请作废

**业务价值：**
- ✅ 提供仓单退出机制
- ✅ 处理异常情况
- ✅ 保持数据完整性
- ✅ 满足合规要求

#### 功能设计

**1. 作废申请接口**

```java
POST /api/ewr/cancel/apply
```

**请求参数：**
```json
{
  "receiptId": "仓单ID",
  "cancelReason": "作废原因",
  "cancelType": "QUALITY_ISSUE/DAMAGED/WRONG_INFO/LEGAL_DISPUTE/VOLUNTARY",
  "evidence": "证明材料（可选）",
  "referenceNo": "参考编号（如法律文书号）"
}
```

**业务规则：**
- ✅ 只有当前持单人可以申请作废
- ✅ NORMAL状态和ONCHAIN_FAILED状态可以申请作废
- ✅ 已提货（DELIVERED）的仓单不能作废
- ✅ 已质押（PLEDGED）的仓单需要先释放
- ✅ 已冻结（FROZEN）的仓单需要先解冻
- ✅ 作废后的仓单无法恢复

**状态流转：**
```
NORMAL → CANCELLING（作废中）
创建作废申请记录
```

---

**2. 作废审核接口**

```java
POST /api/ewr/cancel/approve
```

**请求参数：**
```json
{
  "applicationId": "作废申请ID",
  "approvalResult": "APPROVED/REJECTED",
  "approvalComments": "审核意见"
}
```

**权限要求：**
- ✅ 管理员审核
- ✅ 仓储方可以审核（关联的仓单）

**审核通过后执行：**
1. 更新仓单状态为CANCELLED
2. 记录作废原因和作废时间
3. 上链记录（可选）
4. 发送通知

---

**3. 作废执行逻辑**

```java
// 更新仓单状态
receipt.setReceiptStatus(ReceiptStatus.CANCELLED);
receipt.setCancelReason(application.getCancelReason());
receipt.setCancelType(application.getCancelType());
receipt.setCancelTime(LocalDateTime.now());
receipt.setCancelledBy(application.getReviewerId());
receipt.setReferenceNo(application.getReferenceNo());

// 可选：上链记录
String txHash = contractService.cancelReceiptOnChain(
    receipt.getId(),
    application.getCancelReason()
);
receipt.setTxHash(txHash);

// 保存
repository.save(receipt);
```

---

**4. 查询接口**

```java
// 查询作废申请
GET /api/ewr/cancel/application/{applicationId}

// 查询待审核的作废申请
GET /api/ewr/cancel/pending

// 查询已作废的仓单
GET /api/ewr/cancelled
```

---

#### 需要创建的组件

**1. DTO类**
- `CancelApplicationRequest.java` - 作废申请请求
- `CancelApplicationResponse.java` - 作废申请响应
- `CancelApprovalRequest.java` - 作废审核请求
- `CancelApprovalResponse.java` - 作废审核响应

**2. 实体类**
- `ReceiptCancelApplication.java` - 作废申请记录
  - applicationId
  - receiptId
  - cancelReason
  - cancelType
  - evidence
  - referenceNo
  - requestStatus (PENDING, APPROVED, REJECTED)
  - applicantId
  - applicantName
  - reviewerId
  - reviewerName
  - reviewTime
  - reviewComments
  - createdAt
  - updatedAt

**3. Repository**
- `ReceiptCancelApplicationRepository.java`

**4. Service方法**
- `submitCancelApplication()` - 提交作废申请
- `approveCancelApplication()` - 审核作废申请
- `getCancelledReceipts()` - 查询已作废的仓单
- `getPendingCancelApplications()` - 查询待审核的作废申请

**5. Controller接口**
- `POST /api/ewr/cancel/apply` - 提交作废申请
- `POST /api/ewr/cancel/approve` - 审核作废申请
- `GET /api/ewr/cancel/pending` - 查询待审核的作废申请
- `GET /api/ewr/cancelled` - 查询已作废的仓单

**6. 智能合约方法**（可选）
- `cancelReceipt(string receiptId, string reason)` - 作废仓单上链

---

#### 数据库表设计

**表名：** `receipt_cancel_application`

```sql
CREATE TABLE receipt_cancel_application (
    id VARCHAR(36) PRIMARY KEY,
    receipt_id VARCHAR(36) NOT NULL,
    cancel_reason TEXT NOT NULL,
    cancel_type VARCHAR(50) NOT NULL,
    evidence TEXT,
    reference_no VARCHAR(100),
    request_status VARCHAR(20) NOT NULL,
    applicant_id VARCHAR(36),
    applicant_name VARCHAR(100),
    reviewer_id VARCHAR(36),
    reviewer_name VARCHAR(100),
    review_time DATETIME,
    review_comments TEXT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_receipt (receipt_id),
    INDEX idx_status (request_status),
    INDEX idx_applicant (applicant_id)
);
```

---

#### 权限验证

**提交申请：**
```java
// 验证持单人权限
permissionChecker.checkHolderPermission(
    auth,
    receipt.getHolderAddress(),
    "作废仓单"
);
```

**审核申请：**
```java
// 管理员可直接审核
if (userAuth.isSystemAdmin()) {
    // 允许审核
}

// 仓储方可以审核关联的仓单
if (receipt.getWarehouseId().equals(userAuth.getEnterpriseId())) {
    // 允许审核
}
```

---

#### 预期工作量

| 任务 | 工作量 | 说明 |
|------|--------|------|
| 创建DTO类 | 4个 | 约1.5小时 |
| 创建实体类 | 1个 | 约1小时 |
| 创建Repository | 1个 | 约0.5小时 |
| 实现Service方法 | 4个 | 约3小时 |
| 实现Controller接口 | 4个 | 约1.5小时 |
| 权限验证 | - | 约1小时 |
| 单元测试 | - | 约1.5小时 |
| 集成测试 | - | 约1.5小时 |
| **总计** | - | **约11.5小时（1.5个工作日）** |

---

## 📊 实现优先级

### 高优先级（1-2周）

1. **仓单拆分功能** ⭐⭐⭐
   - 业务价值高
   - 用户需求强烈
   - 提高仓单流动性
   - **预计工作量：2个工作日**

### 中优先级（2-4周）

2. **仓单作废功能** ⭐⭐
   - 完善仓单生命周期
   - 处理异常情况
   - 合规要求
   - **预计工作量：1.5个工作日**

---

## 🎯 总体工作量评估

| 功能 | 优先级 | 工作量 | 完成时间 |
|------|--------|--------|---------|
| 仓单拆分 | 高 | 2个工作日 | 1-2周 |
| 仓单作废 | 中 | 1.5个工作日 | 2-4周 |
| **总计** | - | **3.5个工作日** | **2-4周** |

---

## 📋 实施建议

### 第一阶段：仓单拆分（1-2周）

**Week 1:**
- Day 1-2: 数据库表设计、实体类、DTO类创建
- Day 3-4: Service层实现
- Day 5: Controller接口实现

**Week 2:**
- Day 1: 单元测试
- Day 2: 集成测试
- Day 3: 文档编写
- Day 4-5: Code Review和修复

### 第二阶段：仓单作废（1周）

**Day 1-2:** 基础组件开发
**Day 3:** Service和Controller实现
**Day 4:** 测试
**Day 5:** 文档和Code Review

---

## 🔍 风险评估

### 技术风险

1. **拆分逻辑复杂性**
   - ⚠️ 需要确保数量和价值平衡
   - ⚠️ 子仓单生成逻辑需要严格验证
   - **缓解措施：** 充分的单元测试和集成测试

2. **区块链性能**
   - ⚠️ 拆分可能产生多个上链交易
   - ⚠️ 需要考虑交易失败的处理
   - **缓解措施：** 使用事务机制，确保原子性

3. **权限控制**
   - ⚠️ 拆分和作废都需要严格的权限验证
   - ⚠️ 需要防止越权操作
   - **缓解措施：** 使用JWT地址认证 + PermissionChecker

### 业务风险

1. **拆分后的追溯**
   - ⚠️ 需要维护父子关系
   - ⚠️ 需要支持完整的审计追踪
   - **缓解措施：** 设计完善的数据库索引和查询接口

2. **作废的合规性**
   - ⚠️ 作废需要有充分的理由
   - ⚠️ 可能需要法律依据
   - **缓解措施：** 要求上传证明材料，记录审核日志

---

## 📄 相关文档

- 仓单功能完整清单：`docs/WAREHOUSE_RECEIPT_FEATURES_COMPLETE.md`
- 区块链集成文档：`docs/WAREHOUSE_RECEIPT_BLOCKCHAIN_INTEGRATION.md`
- 背书转让设计：`docs/ENDORSEMENT_TRANSFER_DESIGN.md`

---

**文档版本：** v1.0
**创建时间：** 2026-02-02
**预计完成时间：** 2026-02-16
**状态：** 待实施
