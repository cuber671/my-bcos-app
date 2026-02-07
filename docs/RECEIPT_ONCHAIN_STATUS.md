# 仓单区块链状态检查和新状态流转说明文档

## 📋 概述

本文档说明了电子仓单模块中新增的区块链状态检查机制和状态流转逻辑。

---

## 🔄 新的状态流转

### 状态枚举变更

```java
public enum ReceiptStatus {
    DRAFT,            // 草稿（货主创建）
    PENDING_ONCHAIN,  // 待上链（审核通过，正在上链中）⭐ 新增
    NORMAL,           // 正常（已审核且已上链）
    ONCHAIN_FAILED,   // 上链失败（审核通过但上链失败）⭐ 新增
    PLEDGED,          // 已质押
    TRANSFERRED,      // 已转让
    FROZEN,           // 已冻结
    EXPIRED,          // 已过期
    DELIVERED,        // 已提货
    CANCELLED         // 已取消
}
```

### 完整状态流转图

```
┌──────────────────────────────────────────────────────────┐
│  1. DRAFT（草稿）                                         │
│     ├─ 货主：创建、修改、删除                              │
│     └─ 仓储方：查询待审核列表                              │
└──────────────────────────────────────────────────────────┘
                    │ 审核通过
                    ▼
┌──────────────────────────────────────────────────────────┐
│  2. PENDING_ONCHAIN（待上链）⭐ 新增状态                  │
│     └─ 系统自动：执行上链操作（创建 + 验证）               │
└──────────────────────────────────────────────────────────┘
                    │
        ┌───────────┴───────────┐
        │                       │
    上链成功                  上链失败
        │                       │
        ▼                       ▼
┌──────────────────┐   ┌────────────────────────────────┐
│ 3. NORMAL        │   │ 4. ONCHAIN_FAILED ⭐ 新增状态   │
│    (正常)         │   │    (上链失败)                   │
│    ✅ 已上链      │   │    ⚠️  功能受限                 │
│                  │   │    ├─ 只能：重试上链            │
│    ├─ 可转让      │   │    ├─ 只能：回滚到草稿          │
│    ├─ 可质押      │   │    └─ 禁止：其他操作            │
│    └─ 可提货      │   └────────────────────────────────┘
└──────────────────┘                │
                            ┌───────┴────────┐
                            │                │
                        重试成功            放弃重试
                            │                │
                            ▼                ▼
                    ┌──────────────────┐  ┌──────────┐
                    │ 3. NORMAL        │  │ 1. DRAFT │
                    │    (正常)         │  │  (草稿)  │
                    └──────────────────┘  └──────────┘
                            ↑              │
                            │              └─ 可修改后重新提交
                            │
                    ┌───────┴────────┐
                    │  转让、质押、提货  │
                    └──────────────────┘
                            │
                            ▼
                    ┌──────────────────┐
                    │ 5. PLEDGED /     │
                    │    TRANSFERRED / │
                    │    DELIVERED     │
                    └──────────────────┘
```

---

## 🛡️ AOP 切面：区块链状态检查

### @RequireOnChain 注解

用于标记需要区块链上链的操作，自动拦截并检查仓单的区块链状态。

```java
@RequireOnChain(value = "操作名称", allowFailed = false)
```

**参数说明：**
- `value`: 操作名称，用于异常提示（如"转让"、"质押"、"提货"）
- `allowFailed`: 是否允许 ONCHAIN_FAILED 状态的操作（默认 false）

### 使用示例

#### 1. 在 Service 方法上添加注解

```java
@RequireOnChain(value = "提货", allowFailed = false)
@Transactional
public ElectronicWarehouseReceiptResponse updateActualDeliveryDate(
        @NonNull String id,
        DeliveryUpdateRequest request) {
    // 业务逻辑...
}
```

#### 2. 转让背书方法

```java
@RequireOnChain(value = "背书转让", allowFailed = false)
@Transactional
public void endorseReceipt(String receiptId, String newHolderAddress, String newHolderName) {
    // 只有已上链的仓单才能转让
}
```

#### 3. 质押融资方法

```java
@RequireOnChain(value = "质押融资", allowFailed = false)
@Transactional
public void pledgeReceipt(String receiptId, String financierAddress, BigDecimal amount) {
    // 金融机构只接受已上链的仓单
}
```

#### 4. 允许失败状态的操作（特殊场景）

```java
@RequireOnChain(value = "查询详情", allowFailed = true)
public ElectronicWarehouseReceiptResponse getReceiptById(String id) {
    // 查询操作可以查看失败状态的仓单
}
```

---

## 📡 API 接口

### 1. 重试上链

**接口：** `POST /api/ewr/retry-blockchain/{id}`

**说明：** 重试将上链失败的仓单上传到区块链

**请求示例：**
```bash
curl -X POST "http://localhost:8080/api/ewr/retry-blockchain/a1b2c3d4-e5f6-7890-abcd-ef1234567890" \
  -H "Authorization: Bearer <token>"
```

**响应示例：**
```json
{
  "success": true,
  "message": "仓单上链成功，状态已变更为NORMAL",
  "txHash": "0x1234567890abcdef...",
  "receiptId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

### 2. 回滚到草稿

**接口：** `POST /api/ewr/rollback-to-draft/{id}`

**说明：** 放弃重试上链，将上链失败的仓单回滚到草稿状态

**请求示例：**
```bash
curl -X POST "http://localhost:8080/api/ewr/rollback-to-draft/a1b2c3d4-e5f6-7890-abcd-ef1234567890" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"reason": "区块链节点故障，暂时无法上链，先回滚修改后重新提交"}'
```

**响应示例：**
```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "receiptNo": "EWR20260126000001",
  "receiptStatus": "DRAFT",
  "blockchainStatus": "PENDING"
}
```

### 3. 查询上链失败的仓单

**接口：** `GET /api/ewr/onchain-failed?ownerId={ownerId}`

**说明：** 查询所有上链失败的仓单列表

**请求示例：**
```bash
curl -X GET "http://localhost:8080/api/ewr/onchain-failed?ownerId=enterprise-001" \
  -H "Authorization: Bearer <token>"
```

**响应示例：**
```json
[
  {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "receiptNo": "EWR20260126000001",
    "receiptStatus": "ONCHAIN_FAILED",
    "blockchainStatus": "FAILED",
    "goodsName": "螺纹钢",
    "quantity": 1000.00,
    "totalValue": 4500000.00,
    "remarks": "[审核通过] 货物质量合格\n[上链失败] Connection timeout"
  }
]
```

### 4. 查询待上链的仓单

**接口：** `GET /api/ewr/pending-onchain`

**说明：** 查询正在上链中的仓单列表（PENDING_ONCHAIN状态）

**请求示例：**
```bash
curl -X GET "http://localhost:8080/api/ewr/pending-onchain" \
  -H "Authorization: Bearer <token>"
```

---

## 📊 状态与权限矩阵

| 仓单状态 | 区块链状态 | 货主操作 | 仓储方操作 | 金融机构操作 | 系统管理员 |
|---------|-----------|---------|-----------|-------------|-----------|
| **DRAFT** | PENDING | ✅ 创建<br>✅ 修改<br>✅ 删除 | ✅ 审核通过<br>✅ 审核拒绝 | ❌ | ✅ 全部 |
| **PENDING_ONCHAIN** | - | ⏳ 等待上链完成 | ⏳ 等待上链完成 | ❌ | ✅ 全部 |
| **NORMAL** | SYNCED | ✅ 转让<br>✅ 质押申请<br>✅ 提货 | ✅ 查询<br>✅ 协助提货 | ✅ 质押放款 | ✅ 全部 |
| **ONCHAIN_FAILED** | FAILED | ⚠️ 重试上链<br>⚠️ 回滚到草稿 | ✅ 查询 | ❌ | ✅ 全部 |
| **PLEDGED** | SYNCED | ✅ 还款<br>❌ 转让<br>❌ 提货 | ✅ 查询 | ✅ 查询<br>✅ 逾期处理 | ✅ 全部 |

---

## ⚠️ 错误处理

### 1. 上链失败时的错误提示

```java
{
  "timestamp": "2026-01-26T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "仓单无法进行质押融资操作。仓单上链失败，请先重试上链操作。（仓单编号：EWR20260126000001）",
  "path": "/api/ewr/pledge"
}
```

### 2. 状态不允许操作的错误提示

```java
{
  "timestamp": "2026-01-26T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "仓单正在上链中，请稍后再试。",
  "path": "/api/ewr/transfer"
}
```

### 3. 回滚失败时的错误提示

```java
{
  "timestamp": "2026-01-26T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "该仓单已进行融资，无法回滚到草稿",
  "path": "/api/ewr/rollback-to-draft/a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

## 🔧 前端集成建议

### 1. 状态显示组件

```javascript
// 仓单状态标签组件
function ReceiptStatusBadge({ receipt }) {
  const statusConfig = {
    'DRAFT': { color: 'gray', text: '草稿' },
    'PENDING_ONCHAIN': { color: 'blue', text: '待上链', icon: 'sync', spinning: true },
    'NORMAL': { color: 'green', text: '正常', verified: true },
    'ONCHAIN_FAILED': { color: 'red', text: '上链失败', icon: 'warning' },
    'PLEDGED': { color: 'orange', text: '已质押' },
    'TRANSFERRED': { color: 'purple', text: '已转让' },
    'FROZEN': { color: 'yellow', text: '已冻结' },
    'EXPIRED': { color: 'gray', text: '已过期' },
    'DELIVERED': { color: 'green', text: '已提货' },
    'CANCELLED': { color: 'red', text: '已取消' }
  };

  const config = statusConfig[receipt.receiptStatus];
  const blockchainWarning = receipt.blockchainStatus !== 'SYNCED'
    ? '(未上链)'
    : '';

  return (
    <Badge color={config.color} icon={config.icon}>
      {config.text} {blockchainWarning}
    </Badge>
  );
}
```

### 2. 操作按钮组件

```javascript
// 根据状态显示可用操作
function ReceiptActions({ receipt }) {
  const actions = [];

  if (receipt.receiptStatus === 'NORMAL' && receipt.blockchainStatus === 'SYNCED') {
    actions.push(
      <Button key="transfer">转让</Button>,
      <Button key="pledge">质押</Button>,
      <Button key="deliver">提货</Button>
    );
  }

  if (receipt.receiptStatus === 'ONCHAIN_FAILED') {
    actions.push(
      <Button key="retry" type="primary">重试上链</Button>,
      <Button key="rollback" type="danger">回滚到草稿</Button>
    );
  }

  if (receipt.receiptStatus === 'PENDING_ONCHAIN') {
    actions.push(
      <Button key="loading" disabled loading>上链中...</Button>
    );
  }

  return <div>{actions}</div>;
}
```

### 3. 错误提示处理

```javascript
// API 错误处理
async function handleReceiptOperation(receiptId, operation) {
  try {
    const response = await api.post(`/api/ewr/${operation}/${receiptId}`);
    message.success('操作成功');
  } catch (error) {
    if (error.response?.data?.message?.includes('上链失败')) {
      notification.warning({
        message: '仓单未上链',
        description: '请先完成上链操作后再试',
        btn: (
          <Button type="primary" onClick={() => retryOnChain(receiptId)}>
            重试上链
          </Button>
        )
      });
    } else {
      message.error(error.response?.data?.message || '操作失败');
    }
  }
}
```

---

## 📝 数据库迁移

### Flyway 脚本

文件：`V11__add_onchain_failed_status.sql`

```sql
-- 添加新的仓单状态：PENDING_ONCHAIN 和 ONCHAIN_FAILED
ALTER TYPE receipt_status ADD VALUE 'PENDING_ONCHAIN' AFTER 'DRAFT';
ALTER TYPE receipt_status ADD VALUE 'ONCHAIN_FAILED' AFTER 'NORMAL';

-- 更新现有上链失败的仓单状态
UPDATE electronic_warehouse_receipt
SET receipt_status = 'ONCHAIN_FAILED'
WHERE blockchain_status = 'FAILED' AND receipt_status = 'NORMAL';

-- 创建索引以提高查询性能
CREATE INDEX idx_ewr_receipt_status_new
ON electronic_warehouse_receipt(receipt_status);
```

---

## ✅ 测试清单

### 单元测试

- [ ] 测试审核通过后状态变更为 PENDING_ONCHAIN
- [ ] 测试上链成功后状态变更为 NORMAL
- [ ] 测试上链失败后状态变更为 ONCHAIN_FAILED
- [ ] 测试重试上链成功后状态变更为 NORMAL
- [ ] 测试回滚到草稿后状态变更为 DRAFT
- [ ] 测试 @RequireOnChain 注解拦截未上链的仓单

### 集成测试

- [ ] 测试完整的审核流程（DRAFT → PENDING_ONCHAIN → NORMAL）
- [ ] 测试上链失败的处理流程（ONCHAIN_FAILED → 重试 / 回滚）
- [ ] 测试质押融资时的区块链状态检查
- [ ] 测试提货时的区块链状态检查
- [ ] 测试并发场景下的状态一致性

### API 测试

```bash
# 1. 创建仓单
curl -X POST "http://localhost:8080/api/ewr/create" \
  -H "Content-Type: application/json" \
  -d '{"receiptNo": "EWR20260126000001", ...}'

# 2. 审核通过（状态变为 PENDING_ONCHAIN）
curl -X POST "http://localhost:8080/api/ewr/approve" \
  -H "Content-Type: application/json" \
  -d '{"receiptId": "...", "approvalResult": "APPROVED"}'

# 3. 查询状态
curl -X GET "http://localhost:8080/api/ewr/pending-onchain"

# 4. 模拟上链失败（手动更新数据库）
UPDATE electronic_warehouse_receipt
SET receipt_status = 'ONCHAIN_FAILED', blockchain_status = 'FAILED'
WHERE id = '...';

# 5. 重试上链
curl -X POST "http://localhost:8080/api/ewr/retry-blockchain/{id}"

# 6. 或回滚到草稿
curl -X POST "http://localhost:8080/api/ewr/rollback-to-draft/{id}" \
  -d '{"reason": "测试回滚"}'
```

---

## 📚 相关文件清单

### 新增文件

1. `src/main/java/com/fisco/app/aspect/RequireOnChain.java` - 自定义注解
2. `src/main/java/com/fisco/app/aspect/OnChainStatusAspect.java` - AOP切面
3. `src/main/resources/db/migration/V11__add_onchain_failed_status.sql` - 数据库迁移脚本
4. `docs/RECEIPT_ONCHAIN_STATUS.md` - 本文档

### 修改文件

1. `src/main/java/com/fisco/app/entity/ElectronicWarehouseReceipt.java`
   - 新增状态：`PENDING_ONCHAIN`, `ONCHAIN_FAILED`

2. `src/main/java/com/fisco/app/service/ElectronicWarehouseReceiptService.java`
   - 修改 `approveReceipt()` 方法
   - 修改 `retryReceiptOnChain()` 方法
   - 新增 `rollbackToDraft()` 方法
   - 新增 `getOnChainFailedReceipts()` 方法
   - 在 `updateActualDeliveryDate()` 上添加 `@RequireOnChain` 注解

3. `src/main/java/com/fisco/app/controller/ElectronicWarehouseReceiptController.java`
   - 新增 `POST /api/ewr/rollback-to-draft/{id}` 接口
   - 新增 `GET /api/ewr/onchain-failed` 接口
   - 新增 `GET /api/ewr/pending-onchain` 接口

4. `src/main/java/com/fisco/app/repository/ElectronicWarehouseReceiptRepository.java`
   - 新增 `findByOwnerIdAndReceiptStatus()` 方法

---

## 🎯 最佳实践

1. **所有需要区块链可信度的操作都应添加 `@RequireOnChain` 注解**
   - 转让背书
   - 质押融资
   - 提货
   - 冻结/解冻

2. **查询类操作可以允许 FAILED 状态**
   ```java
   @RequireOnChain(value = "查询", allowFailed = true)
   public ElectronicWarehouseReceiptResponse getReceipt(String id) { ... }
   ```

3. **前端应明确区分不同状态的仓单**
   - NORMAL + SYNCED = 可用
   - ONCHAIN_FAILED = 需要处理（重试或回滚）
   - PENDING_ONCHAIN = 等待中

4. **日志记录完整的状态变更**
   - 审核通过：DRAFT → PENDING_ONCHAIN
   - 上链成功：PENDING_ONCHAIN → NORMAL
   - 上链失败：PENDING_ONCHAIN → ONCHAIN_FAILED
   - 重试成功：ONCHAIN_FAILED → NORMAL
   - 回滚草稿：ONCHAIN_FAILED → DRAFT

---

## 📞 技术支持

如有问题，请联系：
- 开发团队：dev@fisco-app.com
- 技术文档：https://docs.fisco-app.com
