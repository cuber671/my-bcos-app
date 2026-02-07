# 仓单背书转让功能设计文档

生成时间：2026-02-01
系统：FISCO BCOS 供应链金融平台
状态：✅ **已完整实现**

---

## 📋 功能概述

仓单背书转让是电子仓单的核心功能之一，支持仓单持单人将仓单所有权转让给其他企业。系统采用**双重确认机制**，确保转让过程的安全性和可追溯性。

### 核心特性

- ✅ **双重确认机制**：发起背书 → 确认背书
- ✅ **权限验证**：只有当前持单人可以发起背书
- ✅ **状态流转**：NORMAL → TRANSFERRED
- ✅ **货物快照**：自动保存背书时的货物信息
- ✅ **背书链追溯**：完整记录仓单的所有转让历史
- ✅ **区块链集成**：支持背书记录上链存储
- ✅ **多维度查询**：按转出方、转入方、仓单等查询
- ✅ **背书统计**：统计仓单的背书次数

---

## 🎯 业务架构

### 功能模块划分

| 模块 | 文件 | 说明 | 状态 |
|------|------|------|------|
| **实体类** | `EwrEndorsementChain.java` | 28个字段，支持完整的背书信息记录 | ✅ 已实现 |
| **Repository** | `EwrEndorsementChainRepository.java` | 15个查询方法 | ✅ 已实现 |
| **Service** | `EwrEndorsementChainService.java` | 核心业务逻辑（327行） | ✅ 已实现 |
| **Controller** | `EwrEndorsementChainController.java` | 11个REST API接口 | ✅ 已实现 |
| **DTO** | `EwrEndorsementCreateRequest.java` | 创建背书请求DTO（12个字段） | ✅ 已实现 |
| **DTO** | `EwrEndorsementConfirmRequest.java` | 确认背书请求DTO（4个字段） | ✅ 已实现 |
| **DTO** | `EwrEndorsementChainResponse.java` | 背书响应DTO | ✅ 已实现 |

---

## 🔄 业务流程设计

### 1️⃣ 背书转让完整流程

```
┌─────────────────────────────────────────────────────────────────┐
│                    背书转让完整业务流程                            │
└─────────────────────────────────────────────────────────────────┘

步骤1: 转出方发起背书
  ├─ 接口: POST /api/ewr/endorsement/create
  ├─ 权限检查: 验证转出方是否为当前持单人
  ├─ 状态验证: 仓单状态必须为 NORMAL
  ├─ 冲突检查: 检查是否存在待确认的背书
  ├─ 生成背书编号: END+yyyyMMdd+6位流水号
  ├─ 创建背书记录: 状态 = PENDING
  ├─ 保存货物快照: JSON格式记录货物信息
  └─ 返回背书详情

步骤2: 转入方确认背书
  ├─ 接口: POST /api/ewr/endorsement/confirm
  ├─ 状态验证: 背书状态必须为 PENDING
  ├─
  ├─ 确认路径 (CONFIRMED):
  │   ├─ 更新背书状态: PENDING → CONFIRMED
  │   ├─ 记录确认时间
  │   ├─ 更新仓单持单人: holder_address → endorseTo
  │   ├─ 更新仓单当前持单人: current_holder → endorseToName
  │   ├─ 增加背书次数: endorsement_count++
  │   ├─ 更新最后背书时间: last_endorsement_date
  │   ├─ 更新仓单状态: NORMAL → TRANSFERRED
  │   └─ 保存更新
  │
  └─ 取消路径 (CANCELLED):
      ├─ 更新背书状态: PENDING → CANCELLED
      └─ 记录取消原因

步骤3: 查询背书链（可选）
  ├─ 接口: GET /api/ewr/endorsement/chain/{receiptId}
  ├─ 查询所有已确认的背书记录
  ├─ 按时间正序排列
  └─ 返回完整转让历史

步骤4: 上链存储（异步）
  ├─ 接口: PUT /api/ewr/endorsement/blockchain/{id}
  ├─ 更新交易哈希: tx_hash
  ├─ 更新区块高度: block_number
  └─ 更新上链时间: blockchain_timestamp
```

### 2️⃣ 状态流转图

```
仓单状态流转:
  NORMAL(正常) ──发起背书──> PENDING(待确认)
                            │
                            ├─ 确认 ──> TRANSFERRED(已转让)
                            │
                            └─ 取消 ──> NORMAL(正常)

背书状态流转:
  PENDING(待确认) ──┬──> CONFIRMED(已确认)
                    └──> CANCELLED(已取消)
```

### 3️⃣ 数据流转图

```
┌──────────────┐
│ 电子仓单表    │
│ (receipt)    │
│              │
│ - holder_address  ───────────┐
│ - current_holder            │  更新
│ - endorsement_count  <──────┘
│ - last_endorsement_date
│ - receipt_status: NORMAL → TRANSFERRED
└──────────────┘
       │
       │  创建背书
       ↓
┌──────────────┐
│ 背书链表      │
│ (endorsement)│
│              │
│ - endorse_from (转出方)
│ - endorse_to   (转入方)
│ - status: PENDING → CONFIRMED/CANCELLED
│ - goods_snapshot (货物快照)
│ - tx_hash (上链哈希)
└──────────────┘
```

---

## 🔐 业务规则与验证

### 创建背书时的验证

| 验证项 | 规则 | 错误提示 |
|--------|------|----------|
| **仓单存在性** | 仓单ID必须存在于数据库 | "仓单不存在: {receiptId}" |
| **仓单状态** | 必须为 NORMAL 状态 | "只有正常状态的仓单可以背书, 当前状态: {status}" |
| **持单人权限** | endorseFrom 必须等于 receipt.holderAddress | "只有当前持单人可以发起背书" |
| **待确认冲突** | 仓单不能有状态为 PENDING 的背书 | "该仓单存在待确认的背书,请先处理" |
| **地址格式** | 地址必须是42位以太坊地址格式 | "背书企业地址格式不正确" |
| **背书类型** | 必须为 TRANSFER/PLEDGE/RELEASE/CANCEL | "背书类型不正确" |

### 确认背书时的验证

| 验证项 | 规则 | 错误提示 |
|--------|------|----------|
| **背书存在性** | 背书ID必须存在 | "背书记录不存在: {id}" |
| **背书状态** | 必须为 PENDING 状态 | "只有待确认状态的背书可以确认, 当前状态: {status}" |
| **确认状态** | 必须为 CONFIRMED 或 CANCELLED | "无效的确认状态: {status}" |

---

## 📊 数据结构设计

### 背书链实体（EwrEndorsementChain）

#### 字段分类（28个字段）

**基础信息（4个字段）**
- `id` - 背书ID（UUID）
- `receipt_id` - 仓单ID
- `receipt_no` - 仓单编号（冗余）
- `endorsement_no` - 背书编号（格式: END+yyyyMMdd+6位流水号）

**背书企业信息（4个字段）**
- `endorse_from` - 背书企业地址（转出方，42位）
- `endorse_from_name` - 背书企业名称
- `endorse_to` - 被背书企业地址（转入方，42位）
- `endorse_to_name` - 被背书企业名称

**经手人信息（4个字段）**
- `operator_from_id` - 转出方经手人ID
- `operator_from_name` - 转出方经手人姓名
- `operator_to_id` - 转入方经手人ID
- `operator_to_name` - 转入方经手人姓名

**背书类型和原因（2个字段）**
- `endorsement_type` - 背书类型（TRANSFER/PLEDGE/RELEASE/CANCEL）
- `endorsement_reason` - 背书原因说明

**货物信息快照（1个字段）**
- `goods_snapshot` - 背书时的货物信息（JSON格式）
  ```json
  {
    "goods_name": "螺纹钢",
    "quantity": 1000.00,
    "unit_price": 4500.00,
    "total_value": 4500000.00,
    "storage_date": "2026-01-26T10:00:00",
    "warehouse_location": "上海市宝山区仓库A区"
  }
  ```

**价格和金额（2个字段）**
- `transfer_price` - 转让价格（元）
- `transfer_amount` - 转让金额

**区块链信息（3个字段）**
- `tx_hash` - 背书交易哈希（66位）
- `block_number` - 区块高度
- `blockchain_timestamp` - 区块链时间戳

**状态信息（2个字段）**
- `endorsement_status` - 背书状态（PENDING/CONFIRMED/CANCELLED）
- `remarks` - 备注信息

**时间戳（4个字段）**
- `endorsement_time` - 背书发起时间
- `confirmed_time` - 确认时间
- `created_at` - 创建时间
- `updated_at` - 更新时间

**操作人信息（2个字段）**
- `created_by` - 创建人
- `updated_by` - 更新人

### 数据库索引（10个）

```sql
INDEX idx_receipt_id         -- 仓单ID查询
INDEX idx_receipt_no         -- 仓单编号查询
INDEX idx_endorse_from       -- 转出方查询
INDEX idx_endorse_to         -- 转入方查询
INDEX idx_operator_from      -- 转出方经手人查询
INDEX idx_operator_to        -- 转入方经手人查询
INDEX idx_endorsement_time   -- 时间范围查询
INDEX idx_endorsement_status -- 状态查询
INDEX idx_endorsement_type   -- 类型查询
INDEX idx_tx_hash            -- 交易哈希查询
```

---

## 🌐 REST API 接口设计

### API接口清单（11个）

#### 1. 创建背书请求
```
POST /api/ewr/endorsement/create

Request Body:
{
  "receiptId": "ewr-uuid-001",
  "endorseFrom": "0xabcdabcdabcdabcdabcdabcdabcdabcdabcdabcd",
  "endorseTo": "0x1234567890abcdef1234567890abcdef12345678",
  "endorseToName": "YY物流有限公司",
  "endorsementType": "TRANSFER",
  "endorsementReason": "货物所有权转让",
  "transferPrice": 4600.00,
  "transferAmount": 4600000.00,
  "operatorToId": "user-uuid-002",
  "operatorToName": "赵六",
  "remarks": "背书协议已签署"
}

Response:
{
  "id": "endorsement-uuid-001",
  "endorsementNo": "END20260126000001",
  "endorsementStatus": "PENDING",
  "endorsementTime": "2026-01-26T14:30:00"
}
```

#### 2. 确认背书
```
POST /api/ewr/endorsement/confirm

Request Body:
{
  "id": "endorsement-uuid-001",
  "endorsementNo": "END20260126000001",
  "confirmStatus": "CONFIRMED",  // CONFIRMED 或 CANCELLED
  "remarks": "已核实背书信息"
}

Response:
{
  "id": "endorsement-uuid-001",
  "endorsementStatus": "CONFIRMED",
  "confirmedTime": "2026-01-26T15:00:00"
}
```

#### 3. 查询背书链
```
GET /api/ewr/endorsement/chain/{receiptId}

Response: [
  {
    "id": "endorsement-uuid-001",
    "endorsementNo": "END20260126000001",
    "endorseFrom": "0xabcd...",
    "endorseTo": "0x1234...",
    "endorsementStatus": "CONFIRMED",
    "endorsementTime": "2026-01-26T14:30:00"
  }
]
```

#### 4. 查询待确认背书
```
GET /api/ewr/endorsement/pending/{endorseTo}

Response: [
  {
    "id": "endorsement-uuid-001",
    "endorsementNo": "END20260126000001",
    "endorseFrom": "0xabcd...",
    "endorseTo": "0x1234...",
    "endorsementStatus": "PENDING"
  }
]
```

#### 5-11. 其他查询接口
```
GET /api/ewr/endorsement/{id}                           - 根据ID查询
GET /api/ewr/endorsement/by-no/{endorsementNo}          - 根据编号查询
GET /api/ewr/endorsement/from/{endorseFrom}            - 查询发起的背书
GET /api/ewr/endorsement/to/{endorseTo}                - 查询接收的背书
GET /api/ewr/endorsement/by-operator/{operatorId}      - 查询经手人的背书
GET /api/ewr/endorsement/count/{receiptId}             - 统计背书次数
PUT /api/ewr/endorsement/blockchain/{id}               - 更新区块链信息
```

---

## 🔄 仓单状态同步

### 背书确认后自动更新仓单字段

| 仓单字段 | 更新逻辑 | 说明 |
|---------|----------|------|
| `holder_address` | = `endorseTo` | 更新为转入方地址 |
| `current_holder` | = `endorseToName` | 更新为转入方名称 |
| `endorsement_count` | +1 | 背书次数自增 |
| `last_endorsement_date` | = 当前时间 | 最后背书时间 |
| `receipt_status` | NORMAL → TRANSFERRED | 仓单状态流转 |

**代码实现位置**：`EwrEndorsementChainService.java:116-132`

```java
// 4. 更新仓单的持单人
ElectronicWarehouseReceipt receipt = receiptRepository.findById(endorsement.getReceiptId())
        .orElseThrow(() -> new RuntimeException("仓单不存在"));

receipt.setHolderAddress(endorsement.getEndorseTo());
receipt.setCurrentHolder(endorsement.getEndorseToName());

// 增加背书次数
if (receipt.getEndorsementCount() == null) {
    receipt.setEndorsementCount(0);
}
receipt.setEndorsementCount(receipt.getEndorsementCount() + 1);
receipt.setLastEndorsementDate(LocalDateTime.now());

// 5. 更新仓单状态为已转让
receipt.setReceiptStatus(ElectronicWarehouseReceipt.ReceiptStatus.TRANSFERRED);

receiptRepository.save(receipt);
```

---

## 🔍 查询功能详解

### Repository查询方法（15个）

| 方法 | 说明 | 用途 |
|------|------|------|
| `findByEndorsementNo()` | 根据编号查询 | 获取单条背书记录 |
| `findByReceiptId()` | 根据仓单ID查询 | 查询仓单的所有背书 |
| `findByReceiptNo()` | 根据仓单编号查询 | 通过编号查询背书列表 |
| `findByEndorseFrom()` | 根据转出方查询 | 查询企业发起的背书 |
| `findByEndorseTo()` | 根据转入方查询 | 查询企业接收的背书 |
| `findPendingEndorsementsByEndorseTo()` | 查询待确认背书（转入方） | 获取待处理的背书请求 |
| `findPendingEndorsementsByReceiptId()` | 查询待确认背书（仓单） | 检查仓单是否有待确认背书 |
| `findConfirmedEndorsementChainByReceiptId()` | 查询完整背书链 | 获取仓单转让历史 |
| `countConfirmedEndorsementsByReceiptId()` | 统计背书次数 | 获取仓单背书总数 |
| `findConfirmedEndorsementsByEndorseFrom()` | 查询已确认背书（转出方） | 查询企业已完成的转出 |
| `findConfirmedEndorsementsByEndorseTo()` | 查询已确认背书（转入方） | 查询企业已完成的转入 |
| `findByEndorsementTimeBetween()` | 时间范围查询 | 查询指定时间段内的背书 |
| `findByOperatorId()` | 根据经手人查询 | 查询经手人的操作记录 |
| `existsPendingEndorsement()` | 检查待确认背书是否存在 | 防止重复发起 |
| `findEndorsementsWithoutTxHash()` | 查询未上链背书 | 批量上链任务 |

---

## 🎯 业务场景示例

### 场景1：正常转让流程

```bash
# 1. A公司（持单人）发起背书给B公司
curl -X POST "http://localhost:8080/api/ewr/endorsement/create" \
  -H "Content-Type: application/json" \
  -d '{
    "receiptId": "ewr-001",
    "endorseFrom": "0xAAA...",  # A公司地址
    "endorseTo": "0xBBB...",    # B公司地址
    "endorseToName": "B物流公司",
    "endorsementType": "TRANSFER",
    "endorsementReason": "货物所有权转让",
    "transferPrice": 4600.00,
    "transferAmount": 4600000.00
  }'

# 返回: 背书编号 END20260126000001, 状态 PENDING

# 2. B公司确认背书
curl -X POST "http://localhost:8080/api/ewr/endorsement/confirm" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "endorsement-001",
    "endorsementNo": "END20260126000001",
    "confirmStatus": "CONFIRMED"
  }'

# 返回: 状态变更为 CONFIRMED, 仓单持单人已更新为B公司
```

### 场景2：取消背书

```bash
# B公司拒绝背书
curl -X POST "http://localhost:8080/api/ewr/endorsement/confirm" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "endorsement-001",
    "endorsementNo": "END20260126000001",
    "confirmStatus": "CANCELLED",
    "remarks": "货物规格不符合要求"
  }'

# 返回: 状态变更为 CANCELLED, 仓单持单人不变（仍为A公司）
```

### 场景3：查询背书链

```bash
# 查询仓单的完整转让历史
curl -X GET "http://localhost:8080/api/ewr/endorsement/chain/ewr-001"

# 返回:
[
  {
    "endorsementNo": "END20260101000001",
    "endorseFrom": "0xOOO...",  # 仓储方
    "endorseTo": "0xAAA...",    # A公司（第一次转让）
    "endorsementTime": "2026-01-01T10:00:00"
  },
  {
    "endorsementNo": "END20260126000001",
    "endorseFrom": "0xAAA...",  # A公司
    "endorseTo": "0xBBB...",    # B公司（第二次转让）
    "endorsementTime": "2026-01-26T14:30:00"
  }
]
```

---

## 🔐 权限控制矩阵

| 操作 | 货主（持单人） | 仓储方 | 金融机构 | 系统管理员 |
|------|--------------|--------|---------|-----------|
| 发起背书 | ✅（自己的仓单） | ❌ | ❌ | ✅ |
| 确认背书 | ✅（作为转入方） | ❌ | ❌ | ✅ |
| 取消背书 | ✅（作为转入方） | ❌ | ❌ | ✅ |
| 查询背书链 | ✅（自己的仓单） | ✅（关联的仓单） | ✅（质押的仓单） | ✅（全部） |
| 查询待确认背书 | ✅（自己的） | ✅（自己的） | ✅（自己的） | ✅（全部） |
| 统计背书次数 | ✅（自己的仓单） | ✅（关联的仓单） | ✅（质押的仓单） | ✅（全部） |
| 更新区块链信息 | ❌ | ❌ | ❌ | ✅ |

---

## 📈 性能优化设计

### 1. 索引优化

```sql
-- 高频查询字段已建立索引
CREATE INDEX idx_receipt_id ON ewr_endorsement_chain(receipt_id);
CREATE INDEX idx_endorse_from ON ewr_endorsement_chain(endorse_from);
CREATE INDEX idx_endorse_to ON ewr_endorsement_chain(endorse_to);
CREATE INDEX idx_endorsement_status ON ewr_endorsement_chain(endorsement_status);
```

### 2. 冗余字段设计

| 冗余字段 | 目的 | 性能提升 |
|---------|------|----------|
| `receipt_no` | 避免关联查询仓单表 | ⚡️ 减少JOIN |
| `endorse_from_name` | 避免关联查询企业表 | ⚡️ 减少JOIN |
| `endorse_to_name` | 避免关联查询企业表 | ⚡️ 减少JOIN |
| `goods_snapshot` | 记录历史货物信息 | ⚡️ 避免查询历史表 |

### 3. 快照机制

背书时自动保存货物信息快照（JSON格式），确保即使仓单后续被修改，背书记录仍保留历史状态。

**代码位置**：`EwrEndorsementChainService.java:261-272`

---

## ⚠️ 异常处理

### 业务异常

| 异常场景 | 抛出异常 | HTTP状态码 |
|---------|---------|-----------|
| 仓单不存在 | `RuntimeException("仓单不存在")` | 404 |
| 仓单状态不正确 | `RuntimeException("只有正常状态的仓单可以背书")` | 400 |
| 无权限 | `RuntimeException("只有当前持单人可以发起背书")` | 403 |
| 待确认冲突 | `RuntimeException("该仓单存在待确认的背书")` | 409 |
| 背书状态不正确 | `RuntimeException("只有待确认状态的背书可以确认")` | 400 |

### 事务回滚

所有背书操作都标注了 `@Transactional` 注解，确保：
- 创建背书失败 → 自动回滚，不影响仓单表
- 确认背书失败 → 自动回滚，仓单状态保持不变

---

## 🚀 扩展功能预留

### 已预留的扩展点

1. **质押背书（PLEDGE）**
   - `endorsementType` 枚举已包含 `PLEDGE`
   - 可用于仓单质押融资场景

2. **解押背书（RELEASE）**
   - `endorsementType` 枚举已包含 `RELEASE`
   - 可用于质押到期释放场景

3. **撤销背书（CANCEL）**
   - `endorsementType` 枚举已包含 `CANCEL`
   - 可用于背书撤销场景

4. **区块链上链**
   - 预留了 `tx_hash`、`block_number`、`blockchain_timestamp` 字段
   - 提供了 `updateBlockchainInfo()` 接口
   - 支持异步批量上链

---

## 📊 数据统计维度

### 可统计的指标

1. **仓单维度**
   - 背书总次数
   - 最后背书时间
   - 当前持单人

2. **企业维度**
   - 发起的背书数（转出）
   - 接收的背书数（转入）
   - 待确认背书数

3. **时间维度**
   - 指定时间范围内的背书数量
   - 背书频率统计

4. **经手人维度**
   - 经手人处理的背书数量
   - 经手人操作记录

---

## ✅ 实现完成度

| 功能模块 | 功能点 | 实现状态 | 完成度 |
|---------|--------|---------|--------|
| **实体类** | 28个字段 | ✅ 已实现 | 100% |
| **Repository** | 15个查询方法 | ✅ 已实现 | 100% |
| **Service** | 核心业务逻辑 | ✅ 已实现 | 100% |
| **Controller** | 11个API接口 | ✅ 已实现 | 100% |
| **DTO** | 3个请求/响应对象 | ✅ 已实现 | 100% |
| **验证机制** | 6项业务验证 | ✅ 已实现 | 100% |
| **状态流转** | NORMAL → TRANSFERRED | ✅ 已实现 | 100% |
| **快照机制** | 货物信息快照 | ✅ 已实现 | 100% |
| **查询功能** | 多维度查询 | ✅ 已实现 | 100% |
| **统计功能** | 背书次数统计 | ✅ 已实现 | 100% |
| **权限控制** | 持单人验证 | ✅ 已实现 | 100% |
| **异常处理** | 事务回滚 | ✅ 已实现 | 100% |
| **区块链集成** | 上链字段预留 | ✅ 已实现 | 100% |
| **总完成度** | - | - | **100%** |

---

## 🎉 核心亮点

### 1. **双重确认机制**
- 转让方发起 → 转入方确认
- 防止误操作和恶意转让

### 2. **完整的权限验证**
- 只有当前持单人可以发起背书
- 防止非法转让

### 3. **货物信息快照**
- 记录背书时的货物状态
- 避免后续修改影响历史追溯

### 4. **完整背书链**
- 记录仓单的所有转让历史
- 支持追溯仓单流向

### 5. **多维度查询**
- 按仓单、企业、经手人、时间等维度查询
- 满足不同业务场景需求

### 6. **状态同步**
- 背书确认后自动更新仓单状态
- 确保数据一致性

### 7. **事务安全**
- 所有操作都在事务中执行
- 失败自动回滚

---

## 📝 后续优化建议

### 可选增强功能

1. **区块链自动上链**
   - 当前需要手动调用 `updateBlockchainInfo()`
   - 可优化为确认后自动上链

2. **背书通知机制**
   - 转入方收到背书请求后发送通知
   - 可集成邮件、短信、站内信

3. **背书审批流**
   - 对于大额背书，可增加多级审批
   - 预留 `endorsementReason` 字段

4. **背书撤销时限**
   - 待确认背书可设置有效期
   - 超时自动取消

5. **批量背书**
   - 支持一次发起多个仓单的背书
   - 提高操作效率

---

## 📋 测试建议

### 功能测试用例

| 用例 | 操作 | 预期结果 |
|------|------|----------|
| **正常转让** | 发起背书 → 确认背书 | 仓单持单人更新，状态变更为TRANSFERRED |
| **取消背书** | 发起背书 → 取消背书 | 仓单持单人不变，状态仍为NORMAL |
| **权限验证** | 非持单人发起背书 | 抛出异常："只有当前持单人可以发起背书" |
| **状态验证** | 非NORMAL状态仓单发起背书 | 抛出异常："只有正常状态的仓单可以背书" |
| **冲突检查** | 重复发起背书 | 抛出异常："该仓单存在待确认的背书" |
| **背书链查询** | 查询仓单背书链 | 返回按时间正序的所有已确认背书 |

---

**文档版本：** v1.0
**最后更新：** 2026-02-01
**实现状态：** ✅ 已完整实现
**下一步建议：** 测试背书功能 → 实现质押融资功能
