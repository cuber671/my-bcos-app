# 票据背书转让功能使用指南

生成时间：2026-02-02
功能状态：✅ 已完成

---

## 📋 功能概述

票据背书转让是指票据持票人（背书人）将票据权利转让给他人（被背书人）的法律行为。本系统支持完整的背书转让流程，包括：
- 普通背书转让
- 贴现背书
- 质押背书
- 背书历史查询
- 区块链验证

---

## ✅ 已实现的功能接口

### 1. 票据背书转让

**接口地址：** `POST /api/bill/{billId}/endorse`

**功能说明：** 当前持票人将票据背书转让给被背书人

**请求参数：**
```json
{
  "endorseeAddress": "0xabcdef1234567890abcdef1234567890abcdef12",
  "endorsementType": "NORMAL",
  "endorsementAmount": null,
  "remark": "转让给供应商A用于货款结算"
}
```

**参数说明：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| endorseeAddress | String | ✅ | 被背书人地址（新持票人） |
| endorsementType | Enum | ✅ | 背书类型：NORMAL-普通, DISCOUNT-贴现, PLEDGE-质押 |
| endorsementAmount | Long | ❌ | 背书金额（分），null表示全额背书 |
| remark | String | ❌ | 背书备注说明 |

**背书类型说明：**

1. **NORMAL（普通背书）** - 标准的票据权利转让
2. **DISCOUNT（贴现背书）** - 贴现给金融机构
3. **PLEDGE（质押背书）** - 作为质押物

**响应示例：**
```json
{
  "code": 200,
  "message": "票据背书成功",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "billId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "endorserAddress": "0x1234567890abcdef1234567890abcdef12345678",
    "endorseeAddress": "0xabcdef1234567890abcdef1234567890abcdef12",
    "endorsementType": "NORMAL",
    "endorsementAmount": null,
    "endorsementDate": "2026-02-02T10:30:00",
    "endorsementSequence": 1,
    "txHash": "0x9876543210987654321098765432109876543210987654321098765432109876",
    "remark": "转让给供应商A用于货款结算"
  }
}
```

**业务规则：**

1. ✅ 只能背书状态为 `ISSUED`、`NORMAL` 或 `ENDORSED` 的票据
2. ✅ 背书人必须是票据的当前持票人
3. ✅ 被背书人必须是已注册并激活的企业
4. ✅ 不能背书给自己
5. ✅ 背书成功后，票据状态变更为 `ENDORSED`
6. ✅ 自动更新票据的当前持票人
7. ✅ 记录背书序号（第几次背书）
8. ✅ 所有操作上链存证

---

### 2. 查询票据背书历史

**接口地址：** `GET /api/bill/{billId}/endorsements`

**功能说明：** 查询票据的所有背书记录，按时间正序排列

**请求示例：**
```bash
GET /api/bill/a1b2c3d4-e5f6-7890-abcd-ef1234567890/endorsements
```

**响应示例：**
```json
{
  "code": 200,
  "message": "查询成功",
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "billId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "endorserAddress": "0x1234567890abcdef1234567890abcdef12345678",
      "endorseeAddress": "0xabcdef1234567890abcdef1234567890abcdef12",
      "endorsementType": "NORMAL",
      "endorsementAmount": null,
      "endorsementDate": "2026-02-02T10:30:00",
      "endorsementSequence": 1,
      "txHash": "0x9876543210987654321098765432109876543210987654321098765432109876",
      "remark": "第一次转让"
    },
    {
      "id": "660e8400-e29b-41d4-a716-446655440001",
      "billId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "endorserAddress": "0xabcdef1234567890abcdef1234567890abcdef12",
      "endorseeAddress": "0xfedcba0987654321fedcba0987654321fedcba09",
      "endorsementType": "NORMAL",
      "endorsementAmount": null,
      "endorsementDate": "2026-02-15T14:20:00",
      "endorsementSequence": 2,
      "txHash": "0x8765432109876543210987654321098765432109876543210987654321098765",
      "remark": "第二次转让"
    }
  ]
}
```

---

### 3. 从区块链查询背书历史

**接口地址：** `GET /api/bill/{billId}/endorsements/chain`

**功能说明：** 从区块链查询票据的所有背书记录，用于验证数据完整性

**请求示例：**
```bash
GET /api/bill/a1b2c3d4-e5f6-7890-abcd-ef1234567890/endorsements/chain
```

**响应示例：**
```json
{
  "code": 200,
  "message": "查询成功",
  "data": [
    {
      "index": 0,
      "endorser": "0x1234567890abcdef1234567890abcdef12345678",
      "endorsee": "0xabcdef1234567890abcdef1234567890abcdef12",
      "timestamp": "1738467100",
      "endorsementType": "NORMAL"
    },
    {
      "index": 1,
      "endorser": "0xabcdef1234567890abcdef1234567890abcdef12",
      "endorsee": "0xfedcba0987654321fedcba0987654321fedcba09",
      "timestamp": "1739607600",
      "endorsementType": "NORMAL"
    }
  ]
}
```

---

### 4. 验证背书历史完整性

**接口地址：** `GET /api/bill/{billId}/endorsements/validate`

**功能说明：** 对比数据库和区块链上的背书记录，验证数据完整性

**请求示例：**
```bash
GET /api/bill/a1b2c3d4-e5f6-7890-abcd-ef1234567890/endorsements/validate
```

**响应示例（验证通过）：**
```json
{
  "code": 200,
  "message": "验证完成",
  "data": {
    "billId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "dbCount": 2,
    "chainCount": 2,
    "isValid": true,
    "message": "背书记录数量一致，所有背书记录验证通过"
  }
}
```

**响应示例（验证失败）：**
```json
{
  "code": 200,
  "message": "验证完成",
  "data": {
    "billId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "dbCount": 2,
    "chainCount": 1,
    "isValid": false,
    "message": "背书记录数量不一致",
    "mismatches": [
      {
        "sequence": "2",
        "dbRecord": "Endorsement{id='660e8400...', endorser='0xabc...', endorsee='0xfed...'}",
        "chainRecord": "{index=1, endorser=0xabc..., endorsee=0xDIFFERENT...}"
      }
    ]
  }
}
```

---

## 🔧 技术实现细节

### 数据库设计

**endorsement 表结构：**

| 字段 | 类型 | 说明 | 索引 |
|------|------|------|------|
| id | VARCHAR(36) | 背书记录ID（主键） | PRIMARY |
| bill_id | VARCHAR(36) | 票据ID | idx_endorsement_bill_id |
| endorser_address | VARCHAR(42) | 背书人地址 | idx_endorsement_from |
| endorsee_address | VARCHAR(42) | 被背书人地址 | idx_endorsement_to |
| endorsement_type | VARCHAR(20) | 背书类型 | - |
| endorsement_amount | DECIMAL(20,2) | 背书金额（分） | - |
| endorsement_date | DATETIME(6) | 背书日期 | idx_endorsement_date |
| remark | TEXT | 背书备注 | - |
| tx_hash | VARCHAR(66) | 区块链交易哈希 | - |
| endorsement_sequence | INT | 背书序号 | - |
| created_at | DATETIME(6) | 创建时间 | - |

### 核心业务逻辑

```
背书转让流程：
┌─────────────────────────────────────────────────────────────┐
│ 1. 验证票据                                                │
│    - 票据是否存在                                           │
│    - 票据状态是否允许背书（ISSUED/NORMAL/ENDORSED）          │
│    - 背书人是否为当前持票人                                 │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 2. 验证被背书人                                            │
│    - 被背书人是否已注册                                     │
│    - 被背书人是否已激活                                     │
│    - 不能背书给自己                                         │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 3. 调用区块链合约                                          │
│    - endorseBillOnChain(billId, endorsee, type)            │
│    - 获取交易哈希                                           │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 4. 创建背书记录                                            │
│    - 自动生成ID（UUID）                                      │
│    - 计算背书序号（自动递增）                                 │
│    - 保存到数据库                                           │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 5. 更新票据状态                                            │
│    - 更新当前持票人                                         │
│    - 更新票据状态为ENDORSED                                 │
│    - 保存区块链交易哈希                                     │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 6. 构建响应并返回                                          │
└─────────────────────────────────────────────────────────────┘
```

### 事务管理

```java
@Transactional(rollbackFor = Exception.class)
public EndorsementResponse endorseBill(...) {
    // 所有操作在同一事务中
    // 如果区块链调用失败，自动回滚数据库操作
}
```

**事务保证：**
- ✅ 数据库和区块链一致性
- ✅ 任何步骤失败自动回滚
- ✅ 背书记录和票据状态同步更新

---

## 📝 使用示例

### 示例1：普通背书转让

**场景：** A公司将票据转让给B公司用于支付货款

```bash
# 1. A公司登录系统，获取JWT Token
TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "company_a",
    "password": "password123"
  }' | jq -r '.data.token')

# 2. 执行背书转让
curl -X POST http://localhost:8080/api/bill/a1b2c3d4-e5f6-7890-abcd-ef1234567890/endorse \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "endorseeAddress": "0xabcdef1234567890abcdef1234567890abcdef12",
    "endorsementType": "NORMAL",
    "remark": "转让给B公司用于货款结算"
  }'
```

**响应：**
```json
{
  "code": 200,
  "message": "票据背书成功",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "billId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "endorsementSequence": 1,
    "txHash": "0x9876543210987654321098765432109876543210987654321098765432109876"
  }
}
```

---

### 示例2：查询背书历史

**场景：** 查看票据的完整转让链条

```bash
curl -X GET http://localhost:8080/api/bill/a1b2c3d4-e5f6-7890-abcd-ef1234567890/endorsements \
  -H "Authorization: Bearer $TOKEN"
```

**响应：**
```json
{
  "code": 200,
  "message": "查询成功",
  "data": [
    {
      "endorsementSequence": 1,
      "endorserAddress": "0x1234...5678",  // 出票人
      "endorseeAddress": "0xabcd...ef12",   // A公司
      "endorsementDate": "2026-02-01T10:00:00",
      "remark": "初始转让"
    },
    {
      "endorsementSequence": 2,
      "endorserAddress": "0xabcd...ef12",   // A公司
      "endorseeAddress": "0xfedc...ba09",   // B公司
      "endorsementDate": "2026-02-10T14:30:00",
      "remark": "货款结算"
    },
    {
      "endorsementSequence": 3,
      "endorserAddress": "0xfedc...ba09",   // B公司
      "endorseeAddress": "0x9876...5432",   // C公司
      "endorsementDate": "2026-02-20T09:15:00",
      "remark": "二次转让"
    }
  ]
}
```

**转让链条：**
```
出票人 → A公司 → B公司 → C公司
```

---

### 示例3：验证数据完整性

**场景：** 验证数据库和区块链数据是否一致

```bash
curl -X GET http://localhost:8080/api/bill/a1b2c3d4-e5f6-7890-abcd-ef1234567890/endorsements/validate \
  -H "Authorization: Bearer $TOKEN"
```

**响应（验证通过）：**
```json
{
  "code": 200,
  "message": "验证完成",
  "data": {
    "billId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "dbCount": 3,
    "chainCount": 3,
    "isValid": true,
    "message": "背书记录数量一致，所有背书记录验证通过"
  }
}
```

---

## ⚠️ 注意事项

### 权限要求

1. **背书操作权限**
   - ✅ 必须是票据的当前持票人
   - ✅ 必须已登录（JWT认证）
   - ✅ 被背书人必须是已注册企业

2. **查询权限**
   - 背书历史：所有人可查询
   - 区块链数据：所有人可查询
   - 数据验证：所有人可验证

### 业务限制

1. **票据状态限制**
   ```
   可背书的票据状态：
   ✅ ISSUED    - 已开立
   ✅ NORMAL    - 正常（已承兑）
   ✅ ENDORSED  - 已背书（可再次背书）

   不可背书的票据状态：
   ❌ DRAFT           - 草稿
   ❌ PENDING_ISSUANCE - 待开立
   ❌ DISCOUNTED      - 已贴现
   ❌ PLEDGED         - 已质押
   ❌ FROZEN          - 已冻结
   ❌ CANCELLED       - 已作废
   ❌ PAID            - 已付款
   ❌ SETTLED         - 已结算
   ```

2. **背书限制**
   - ❌ 不能背书给自己
   - ❌ 不能背书给未注册企业
   - ❌ 已贴现/质押/冻结的票据不能背书

### 错误处理

**常见错误：**

| 错误码 | 错误信息 | 原因 | 解决方案 |
|--------|----------|------|----------|
| 400 | 票据不存在 | billId错误 | 检查票据ID |
| 403 | 只有当前持票人可以背书转让 | 权限不足 | 使用当前持票人账户 |
| 400 | 被背书人不存在或未激活 | 被背书人未注册 | 先注册被背书人企业 |
| 400 | 不能背书给自己 | 背书人和被背书人相同 | 使用不同的地址 |
| 400 | 只能背书已开立、已承兑或已背书的票据 | 票据状态不允许 | 检查票据状态 |
| 500 | 区块链操作失败 | 区块链连接问题 | 检查区块链服务 |

---

## 🔍 背书类型详解

### 1. NORMAL（普通背书）

**用途：** 标准的票据权利转让

**适用场景：**
- 支付货款
- 偿还债务
- 资金调拨
- 一般性票据转让

**示例：**
```json
{
  "endorsementType": "NORMAL",
  "remark": "用于支付XX公司货款"
}
```

---

### 2. DISCOUNT（贴现背书）

**用途：** 将票据贴现给金融机构

**适用场景：**
- 企业需要提前变现
- 银行贴现业务
- 融资需求

**注意：** 贴现背书应使用专门的贴现接口 `/api/bill/{billId}/discount`

---

### 3. PLEDGE（质押背书）

**用途：** 将票据作为质押物

**适用场景：**
- 获取贷款
- 担保业务
- 融资质押

**注意：** 质押背书应使用专门的质押接口（待实现）

---

## 📊 背书统计与分析

### 背书次数查询

```sql
-- 查询某个票据的背书次数
SELECT COUNT(*) FROM endorsement WHERE bill_id = 'bill-uuid';

-- 查询某个企业背书出去的次数
SELECT COUNT(*) FROM endorsement WHERE endorser_address = '0x123...';

-- 查询某个企业被背书的次数
SELECT COUNT(*) FROM endorsement WHERE endorsee_address = '0xabc...';
```

### Repository提供的查询方法

```java
// 根据票据ID查找所有背书记录
List<Endorsement> findByBillIdOrderByEndorsementDateAsc(String billId);

// 查找作为背书人的所有记录
List<Endorsement> findByEndorserAddressOrderByEndorsementDateDesc(String endorserAddress);

// 查找作为被背书人的所有记录
List<Endorsement> findByEndorseeAddressOrderByEndorsementDateDesc(String endorseeAddress);

// 根据背书类型查找
List<Endorsement> findByEndorsementType(Endorsement.EndorsementType endorsementType);

// 统计票据的背书次数
Long countByBillId(String billId);
```

---

## 🧪 测试用例

### 测试用例1：正常背书流程

```bash
# 前置条件：
# 1. 已开立票据，billId = "test-bill-001"
# 2. 当前持票人 = A公司地址
# 3. 被背书人 = B公司地址（已注册）

# 执行背书
curl -X POST http://localhost:8080/api/bill/test-bill-001/endorse \
  -H "Authorization: Bearer $COMPANY_A_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "endorseeAddress": "0xbcd...",
    "endorsementType": "NORMAL",
    "remark": "正常背书测试"
  }'

# 预期结果：
# - 返回 200
# - 创建背书记录
# - 票据状态变更为 ENDORSED
# - 当前持票人变更为 B公司
```

### 测试用例2：验证数据完整性

```bash
# 1. 执行背书操作
# 2. 查询数据库背书历史
curl -X GET http://localhost:8080/api/bill/test-bill-001/endorsements

# 3. 查询区块链背书历史
curl -X GET http://localhost:8080/api/bill/test-bill-001/endorsements/chain

# 4. 验证数据一致性
curl -X GET http://localhost:8080/api/bill/test-bill-001/endorsements/validate

# 预期结果：
# - dbCount == chainCount
# - isValid == true
```

### 测试用例3：重复背书

```bash
# 1. 第一次背书：A -> B
# 2. 第二次背书：B -> C
# 3. 第三次背书：C -> D

# 查询背书历史
curl -X GET http://localhost:8080/api/bill/test-bill-001/endorsements

# 预期结果：
# - endorsementSequence = 1, 2, 3
# - 按时间正序排列
# - 形成完整转让链：A -> B -> C -> D
```

---

## 🎯 功能完成清单

- ✅ 票据背书接口
- ✅ 背书历史查询（数据库）
- ✅ 背书历史查询（区块链）
- ✅ 数据完整性验证
- ✅ 背书序号自动递增
- ✅ 事务管理
- ✅ 区块链集成
- ✅ 权限验证
- ✅ 错误处理
- ✅ 日志记录

---

## 📚 相关文档

- [票据模块API文档](../src/main/java/com/fisco/app/controller/BillController.java)
- [业务逻辑实现](../src/main/java/com/fisco/app/service/BillService.java)
- [实体定义](../src/main/java/com/fisco/app/entity/Endorsement.java)
- [待开发功能清单](PENDING_FEATURES_LIST.md)

---

**文档版本：** v1.0
**创建时间：** 2026-02-02
**更新时间：** 2026-02-02
**维护人员：** FISCO BCOS供应链金融团队
