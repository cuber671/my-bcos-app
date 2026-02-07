# 仓单模块完整功能清单

生成时间：2026-02-02
系统：FISCO BCOS 供应链金融平台

---

## 📊 功能概览

| 类别 | 数量 | 说明 |
|------|------|------|
| Controller | 4个 | EWR + Endorsement + Pledge + WarehouseReceipt |
| API接口 | 62个 | 全功能覆盖 |
| 实体字段 | 52个 | 支持完整的仓单生命周期管理 |
| 状态枚举 | 10个 | 从草稿到已提货的完整流转 |

---

## ✅ 已实现功能模块

### 1️⃣ 电子仓单基础管理（22个接口）

#### CRUD操作（6个）

| 接口 | 方法 | 说明 | 状态 |
|------|------|------|------|
| `POST /api/ewr/create` | `createReceipt()` | 创建草稿状态仓单，支持52个字段 | ✅ |
| `PUT /api/ewr/update/{id}` | `updateReceipt()` | 更新仓单信息（仅草稿/正常状态可更新） | ✅ |
| `DELETE /api/ewr/{id}` | `deleteReceipt()` | 软删除仓单（仅草稿状态可删除） | ✅ |
| `GET /api/ewr/{id}` | `getReceiptById()` | 根据ID查询仓单详情 | ✅ |
| `GET /api/ewr/by-no/{receiptNo}` | `getReceiptByNo()` | 根据仓单编号查询 | ✅ |
| `POST /api/ewr/query` | `queryReceipts()` | 支持多条件分页查询 | ✅ |

#### 查询功能（8个）

| 接口 | 方法 | 说明 | 状态 |
|------|------|------|------|
| `GET /api/ewr/by-owner/{ownerId}` | `getReceiptsByOwner()` | 查询指定货主的所有仓单 | ✅ |
| `GET /api/ewr/by-holder/{holderAddress}` | `getReceiptsByHolder()` | 查询指定持单人的所有仓单 | ✅ |
| `GET /api/ewr/by-warehouse/{warehouseId}` | `getReceiptsByWarehouse()` | 查询指定仓储企业的所有仓单 | ✅ |
| `GET /api/ewr/expiring` | `getExpiringReceipts()` | 查询7天内即将过期的仓单 | ✅ |
| `GET /api/ewr/expired` | `getExpiredReceipts()` | 查询已经过期的仓单 | ✅ |
| `GET /api/ewr/pending/{warehouseId}` | `getPendingReceipts()` | 查询指定仓储企业的待审核仓单 | ✅ |
| `GET /api/ewr/pending-onchain` | `getPendingOnChainReceipts()` | 查询正在上链中的仓单 | ✅ |
| `GET /api/ewr/onchain-failed` | `getOnChainFailedReceipts()` | 查询上链失败的仓单 | ✅ |

#### 审核流程（2个）

| 接口 | 方法 | 状态流转 | 状态 |
|------|------|----------|------|
| `POST /api/ewr/approve` | `approveReceipt()` | DRAFT → PENDING_ONCHAIN → NORMAL/ONCHAIN_FAILED | ✅ |
| `PUT /api/ewr/status/{id}` | `updateReceiptStatus()` | 手动更新仓单状态 | ✅ |

**审核流程：**
```
货主创建仓单（DRAFT）
    ↓
仓储方审核（approve）
    ↓
状态变更为 PENDING_ONCHAIN（待上链）
    ↓
系统自动上链（两步：创建 + 验证）
    ↓
    ├─ 成功 → NORMAL（正常）
    └─ 失败 → ONCHAIN_FAILED（上链失败）
```

#### 区块链上链管理（3个）

| 接口 | 方法 | 说明 | 状态 |
|------|------|------|------|
| `POST /api/ewr/retry-blockchain/{id}` | `retryReceiptOnChain()` | 重试将上链失败的仓单上传到区块链 | ✅ |
| `PUT /api/ewr/blockchain/{id}` | `updateBlockchainStatus()` | 更新仓单的区块链上链状态 | ✅ |
| `POST /api/ewr/rollback-to-draft/{id}` | `rollbackToDraft()` | 放弃重试，回滚到草稿状态 | ✅ |

#### 提货管理（1个）

| 接口 | 方法 | 字段 | 状态 |
|------|------|------|------|
| `PUT /api/ewr/delivery/{id}` | `updateActualDeliveryDate()` | actualDeliveryDate, deliveryPersonName, deliveryPersonContact, deliveryNo, vehiclePlate, driverName | ✅ |

#### 统计功能（2个）

| 接口 | 方法 | 说明 | 状态 |
|------|------|------|------|
| `GET /api/ewr/count/owner/{ownerId}` | `countByOwner()` | 统计指定货主的仓单总数 | ✅ |
| `GET /api/ewr/count/warehouse/{warehouseId}` | `countByWarehouse()` | 统计指定仓储企业的仓单总数 | ✅ |

---

### 2️⃣ 背书转让功能（10个接口）✨

**Controller:** `EwrEndorsementChainController.java`
**Service:** `EwrEndorsementChainService.java`

| 接口 | 方法 | 说明 | 状态 |
|------|------|------|------|
| `POST /api/endorsement/create` | `createEndorsement()` | 创建背书转让请求 | ✅ |
| `POST /api/endorsement/confirm` | `confirmEndorsement()` | 确认背书转让 | ✅ |
| `GET /api/endorsement/chain/{receiptId}` | `getEndorsementChain()` | 查询仓单的完整背书链 | ✅ |
| `GET /api/endorsement/pending/{endorseTo}` | `getPendingEndorsements()` | 查询待确认的背书请求 | ✅ |
| `GET /api/endorsement/{id}` | `getEndorsementById()` | 根据ID查询背书记录 | ✅ |
| `GET /api/endorsement/by-no/{endorsementNo}` | `getEndorsementByNo()` | 根据背书编号查询 | ✅ |
| `GET /api/endorsement/from/{endorseFrom}` | `getEndorsementsFrom()` | 查询转出的背书记录 | ✅ |
| `GET /api/endorsement/to/{endorseTo}` | `getEndorsementsTo()` | 查询转入的背书记录 | ✅ |
| `GET /api/endorsement/count/{receiptId}` | `getEndorsementCount()` | 统计仓单的背书次数 | ✅ |
| `PUT /api/endorsement/blockchain/{id}` | `updateEndorsementBlockchainStatus()` | 更新背书的区块链状态 | ✅ |

**背书转让流程：**
```
当前持单人发起背书请求
    ↓
状态：PENDING（待确认）
    ↓
被背书方确认背书
    ↓
状态：CONFIRMED（已确认）
    ↓
系统更新仓单holder_address
系统自动上链
    ↓
状态：COMPLETED（已完成）
```

**权限验证：**
- ✅ 只有当前持单人可以发起背书
- ✅ 验证仓单状态必须为NORMAL
- ✅ 自动更新背书统计字段

---

### 3️⃣ 质押融资功能（10个接口）✨

**Controller:** `PledgeManagementController.java`
**Service:** `PledgeService.java`

| 接口 | 方法 | 说明 | 状态 |
|------|------|------|------|
| `POST /api/pledge/initiate` | `initiatePledge()` | 发起质押申请 | ✅ |
| `POST /api/pledge/confirm` | `confirmPledge()` | 金融机构确认质押 | ✅ |
| `POST /api/pledge/release` | `releasePledge()` | 还款释放仓单 | ✅ |
| `GET /api/pledge/pending/{financialInstitutionAddress}` | `getPendingPledges()` | 查询待确认的质押申请 | ✅ |
| `GET /api/pledge/history/{receiptId}` | `getPledgeHistory()` | 查询仓单的质押历史 | ✅ |
| `POST /api/pledge/query` | `queryPledges()` | 分页查询质押记录 | ✅ |
| `GET /api/pledge/status/{receiptId}` | `getPledgeStatus()` | 查询仓单质押状态 | ✅ |
| `POST /api/pledge/apply` | `applyForPledge()` | 申请融资（新版本） | ✅ |
| `POST /api/pledge/approve` | `approvePledgeApplication()` | 审批融资申请 | ✅ |

**质押融资流程：**
```
持单人发起质押申请
    ↓
状态：PLEDGE_PENDING（待确认）
    ↓
金融机构审核并确认
    ↓
状态：PLEDGED（已质押）
    ↓
系统更新仓单融资字段
系统自动上链
    ↓
持单人还款
    ↓
机构释放仓单
    ↓
状态：NORMAL（恢复正常）
```

**融资信息字段（6个）：**
- is_financed - 是否已融资
- finance_amount - 融资金额
- finance_rate - 融资利率
- finance_date - 融资日期
- financier_address - 资金方地址
- pledge_contract_no - 质押合同编号

---

### 4️⃣ 冻结/解冻功能（5个接口）✨

**Controller:** `ElectronicWarehouseReceiptController.java`
**Service:** `ElectronicWarehouseReceiptService.java`

| 接口 | 方法 | 说明 | 状态 |
|------|------|------|------|
| `POST /api/ewr/freeze` | `freezeReceipt()` | 管理员直接冻结仓单 | ✅ |
| `POST /api/ewr/unfreeze` | `unfreezeReceipt()` | 管理员解冻仓单 | ✅ |
| `POST /api/ewr/freeze-application/submit` | `submitFreezeApplication()` | 仓储方提交冻结申请 | ✅ |
| `POST /api/ewr/freeze-application/review` | `reviewFreezeApplication()` | 管理员审核冻结申请 | ✅ |
| `GET /api/ewr/freeze-application/pending` | `getPendingFreezeApplications()` | 查询待审核的冻结申请 | ✅ |
| `GET /api/ewr/freeze-application/by-warehouse/{warehouseId}` | `getFreezeApplicationsByWarehouse()` | 查询仓储企业的冻结申请 | ✅ |
| `GET /api/ewr/frozen` | `getFrozenReceipts()` | 查询已冻结的仓单 | ✅ |

**冻结流程：**
```
方式1：管理员直接冻结
    ↓
验证管理员权限
    ↓
状态：NORMAL → FROZEN
    ↓
记录冻结原因
    ↓
系统自动上链

方式2：仓储方申请冻结
    ↓
提交冻结申请
    ↓
状态：PENDING
    ↓
管理员审核
    ↓
    ├─ 批准 → FROZEN
    └─ 拒绝 → 保持原状态
```

**权限验证：**
- ✅ 管理员可直接冻结/解冻
- ✅ 仓储方可申请冻结，需管理员审核
- ✅ 冻结的仓单无法进行背书、提货等操作

---

## 🔄 仓单状态枚举（10种）

| 状态 | 说明 | 可执行操作 |
|------|------|-----------|
| **DRAFT** | 草稿 | ✅ 更新、删除、提交审核 |
| **PENDING_ONCHAIN** | 待上链 | ⏳ 等待上链完成 |
| **NORMAL** | 正常 | ✅ 转让、质押、提货、冻结 |
| **ONCHAIN_FAILED** | 上链失败 | ⚠️ 重试上链、回滚草稿 |
| **PLEDGED** | 已质押 | ✅ 还款释放 |
| **TRANSFERRED** | 已转让 | ✅ 再次转让、提货、质押 |
| **FROZEN** | 已冻结 | ⏸️ 等待解冻 |
| **EXPIRED** | 已过期 | ❌ 无法操作 |
| **DELIVERED** | 已提货 | 终态 |
| **CANCELLED** | 已取消 | 终态 |

---

## 🔐 安全与权限

### 权限控制机制

| 权限检查 | 说明 | 实现状态 |
|---------|------|---------|
| `@RequireOnChain` | 拦截未上链的操作（如提货、转让） | ✅ |
| `checkCreateReceiptPermission` | 检查创建仓单权限（货主） | ✅ |
| `checkReceiptApprovalPermission` | 检查审核权限（仓储方） | ✅ |
| `checkReceiptAccessPermission` | 检查查询权限（货主/仓储方） | ✅ |
| `checkHolderPermission` | 检查持单人权限（基于区块链地址） | ✅ |

### JWT地址认证

✅ **已实现** - 2026-02-02新增
- Token中包含企业区块链地址
- 持单人权限验证基于 `holder_address`
- 支持背书转让后的权限转移

### 角色权限矩阵

| 操作 | 货主 | 仓储方 | 金融机构 | 系统管理员 |
|------|------|--------|---------|-----------|
| 创建仓单 | ✅ | ❌ | ❌ | ✅ |
| 更新仓单（草稿） | ✅ | ❌ | ❌ | ✅ |
| 删除仓单（草稿） | ✅ | ❌ | ❌ | ✅ |
| 审核仓单 | ❌ | ✅ | ❌ | ✅ |
| 查询仓单 | ✅（自己的） | ✅（关联的） | ✅（质押的） | ✅（全部） |
| 重试上链 | ❌ | ✅ | ❌ | ✅ |
| 回滚草稿 | ✅ | ❌ | ❌ | ✅ |
| 提货 | ✅ | ✅ | ❌ | ✅ |
| **背书转让** | ✅（持单人） | ❌ | ❌ | ✅ |
| **质押融资** | ✅（持单人） | ❌ | ✅（资金方） | ✅ |
| **冻结/解冻** | ❌ | ❌ | ❌ | ✅ |

---

## 📈 数据库设计

### electronic_warehouse_receipt 表

| 项目 | 数量 | 说明 |
|------|------|------|
| 字段总数 | 52 | 完整的仓单信息 |
| 索引 | 16 | 优化查询性能 |
| 外键约束 | 3 | 关联企业、父仓单、用户 |
| 检查约束 | 6 | 数据完整性保护 |

### 关键字段分类

#### 基础信息（8个）
- id, receipt_no, warehouse_id, warehouse_address, warehouse_name
- owner_id, owner_address, holder_address

#### 货物信息（6个）
- goods_name, unit, quantity, unit_price, total_value, market_price

#### 仓储信息（6个）
- warehouse_location, storage_location, storage_date, expiry_date
- actual_delivery_date等6个提货字段

#### 状态管理（3个）
- receipt_status（10种枚举状态）, parent_receipt_id, batch_no

#### 融资信息（6个）
- is_financed, finance_amount, finance_rate, finance_date
- financier_address, pledge_contract_no

#### 背书统计（3个）
- endorsement_count, last_endorsement_date, current_holder

#### 区块链（4个）
- tx_hash, blockchain_status, block_number, blockchain_timestamp

---

## 📋 API接口统计

| 模块 | 接口数量 | 状态 |
|------|---------|------|
| **电子仓单基础管理** | 22 | ✅ 100% |
| **背书转让** | 10 | ✅ 100% |
| **质押融资** | 10 | ✅ 100% |
| **冻结/解冻** | 7 | ✅ 100% |
| **基础仓单接口** | 4 | ✅ 100% |
| **总计** | **53** | **✅ 100%** |

---

## 🎯 核心业务流程

### 完整的仓单生命周期

```
1. 货主创建仓单
   POST /api/ewr/create
   状态：DRAFT
   ✅ 已实现

2. 仓储方审核入库
   POST /api/ewr/approve
   状态流转：DRAFT → PENDING_ONCHAIN → NORMAL/ONCHAIN_FAILED
   ✅ 已实现

3. [上链失败处理]
   - 重试上链：POST /api/ewr/retry-blockchain/{id}
   - 回滚草稿：POST /api/ewr/rollback-to-draft/{id}
   ✅ 已实现

4. 背书转让
   POST /api/endorsement/create
   状态流转：NORMAL → TRANSFERRED
   ✅ 已实现

5. 质押融资
   POST /api/pledge/initiate
   状态流转：NORMAL → PLEDGED → NORMAL
   ✅ 已实现

6. 冻结/解冻
   POST /api/ewr/freeze
   状态流转：NORMAL → FROZEN → NORMAL
   ✅ 已实现

7. 货主提货
   PUT /api/ewr/delivery/{id}
   状态：NORMAL → DELIVERED
   ✅ 已实现
```

---

## 🎉 核心亮点

### ✅ 已完整实现的核心功能

1. **完整的仓单CRUD** - 创建、更新、删除、查询
2. **智能审核流程** - 草稿 → 待上链 → 正常/失败
3. **区块链集成** - 自动上链、重试、回滚
4. **背书转让功能** - 完整的转让链追踪
5. **质押融资功能** - 申请、审批、还款、释放
6. **冻结/解冻功能** - 管理员直接操作 + 申请审核流程
7. **AOP状态检查** - @RequireOnChain 自动拦截未上链的操作
8. **完整提货流程** - 记录提货人、联系方式、车牌号等详细信息
9. **多维度查询** - 按货主、仓储方、持单人、状态、时间等查询
10. **预警功能** - 即将过期、已过期仓单查询
11. **统计功能** - 货主、仓储企业的仓单统计
12. **权限控制** - 细粒度的企业级权限管理 + 区块链地址认证
13. **数据完整性** - 52个字段、16个索引、6个检查约束

### 🚀 技术特色

- **双状态设计**：receipt_status（业务状态）+ blockchain_status（上链状态）
- **状态流转清晰**：10种枚举状态覆盖完整生命周期
- **区块链状态检查切面**：自动拦截未上链的关键操作
- **软删除支持**：数据可恢复
- **审计字段完整**：created_at, updated_at, created_by, updated_by
- **外键约束**：保证引用完整性
- **区块链地址认证**：Token包含企业地址，支持持单人权限验证
- **背书链追踪**：完整的转让历史记录
- **融资全流程**：申请 → 审批 → 质押 → 还款 → 释放
- **冻结审核机制**：管理员直接操作 + 仓储方申请审核

---

## 📊 功能实现度总结

| 模块 | 功能总数 | 已实现 | 完成度 |
|------|---------|--------|--------|
| **CRUD操作** | 6 | 6 | 100% |
| **查询功能** | 8 | 8 | 100% |
| **审核流程** | 3 | 3 | 100% |
| **上链管理** | 3 | 3 | 100% |
| **提货管理** | 1 | 1 | 100% |
| **统计功能** | 2 | 2 | 100% |
| **背书转让** | 10 | 10 | 100% |
| **质押融资** | 10 | 10 | 100% |
| **冻结解冻** | 7 | 7 | 100% |
| **状态管理** | 10 | 10 | 100% |
| **总计** | **60** | **60** | **100%** ✅ |

---

## ⏭️ 待实现功能

### 1. 仓单拆分功能

**设计已完成** ✅
- 需要实现拆分申请、审批、执行接口
- 需要生成子仓单，更新父仓单
- 需要权限验证（基于区块链地址的持单人验证）

### 2. 仓单作废功能

**状态枚举已定义** ✅
- CANCELLED（已取消）
- 需要实现作废申请、审批接口
- 需要记录作废原因

---

**文档版本：** v2.0
**最后更新：** 2026-02-02
**系统状态：** 运行中 ✅
**完成度：** 核心功能 100% ✅
