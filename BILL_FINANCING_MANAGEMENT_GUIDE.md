# 票据融资管理功能使用指南

## 📋 功能概述

票据融资管理功能为企业提供了使用票据进行融资的完整流程，包括融资申请、审核、放款和还款等功能。

### 核心功能

1. **融资申请** - 企业使用票据向金融机构申请融资
2. **融资审核** - 金融机构审核融资申请（批准/拒绝）
3. **查询待审核申请** - 查看待审核的融资申请列表
4. **融资还款** - 企业归还融资本息

---

## 🔧 技术实现

### 数据库表

**bill_finance_application** - 票据融资申请表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | VARCHAR(36) | 融资申请ID（主键） |
| bill_id | VARCHAR(36) | 票据ID |
| bill_no | VARCHAR(50) | 票据编号 |
| bill_face_value | DECIMAL(20,2) | 票据面值 |
| applicant_id | VARCHAR(36) | 申请人ID（企业） |
| applicant_name | VARCHAR(200) | 申请人名称 |
| financial_institution_id | VARCHAR(36) | 金融机构ID |
| financial_institution_name | VARCHAR(200) | 金融机构名称 |
| finance_amount | DECIMAL(20,2) | 申请融资金额 |
| finance_rate | DECIMAL(10,4) | 申请融资利率（%） |
| finance_period | INT | 融资期限（天） |
| pledge_agreement | TEXT | 质押协议内容 |
| status | VARCHAR(50) | 申请状态 |
| approved_amount | DECIMAL(20,2) | 批准金额 |
| approved_rate | DECIMAL(10,4) | 批准利率（%） |
| actual_amount | DECIMAL(20,2) | 实际放款金额 |
| apply_date | DATETIME(6) | 申请日期 |
| approve_date | DATETIME(6) | 审核日期 |
| approval_comments | TEXT | 审核意见 |
| rejection_reason | VARCHAR(500) | 拒绝原因 |
| disbursement_date | DATETIME(6) | 放款日期 |
| repayment_date | DATETIME(6) | 还款日期 |
| actual_repayment_amount | DECIMAL(20,2) | 实际还款金额 |
| tx_hash | VARCHAR(100) | 区块链交易哈希 |
| created_by | VARCHAR(36) | 创建人ID |
| updated_by | VARCHAR(36) | 更新人ID |
| created_at | DATETIME(6) | 创建时间 |
| updated_at | DATETIME(6) | 更新时间 |

### 申请状态流转

```
PENDING（待审核）
    ↓
    ├── APPROVED（已批准） → ACTIVE（已放款） → REPAID（已还款）
    │
    └── REJECTED（已拒绝）
```

---

## 📡 API 接口说明

### 1. 申请票据融资

**接口地址：** `POST /api/bill/{billId}/finance`

**请求头：**
```
Content-Type: application/json
X-User-Address: 0x1234567890abcdef  // 申请人区块链地址
Authorization: Bearer <token>
```

**请求体：**
```json
{
  "financialInstitutionId": "inst-uuid-001",
  "financeAmount": 950000.00,
  "financeRate": 5.5,
  "financePeriod": 90,
  "pledgeAgreement": "质押协议条款内容..."
}
```

**请求参数说明：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| billId | String | 是 | 票据ID（路径参数） |
| financialInstitutionId | String | 是 | 金融机构ID |
| financeAmount | BigDecimal | 是 | 申请融资金额（≤ 票据面值） |
| financeRate | BigDecimal | 是 | 申请融资利率（%） |
| financePeriod | Integer | 是 | 融资期限（天） |
| pledgeAgreement | String | 否 | 质押协议内容 |

**业务规则：**
- 票据状态必须为：NORMAL（正常）、ENDORSED（已背书）或 ISSUED（已开票）
- 申请人必须是票据的当前持票人
- 融资金额不能超过票据面值
- 申请创建后状态为 PENDING

**响应示例：**
```json
{
  "code": 200,
  "message": "融资申请提交成功",
  "data": {
    "id": "app-uuid-001",
    "billId": "bill-uuid-001",
    "billNo": "BILL20260101001",
    "billFaceValue": 1000000.00,
    "applicantId": "ent-uuid-001",
    "applicantName": "供应商A",
    "financialInstitutionId": "inst-uuid-001",
    "financialInstitutionName": "银行B",
    "financeAmount": 950000.00,
    "financeRate": 5.5,
    "financePeriod": 90,
    "status": "PENDING",
    "applyDate": "2026-02-02T10:30:00"
  }
}
```

---

### 2. 审核票据融资申请

**接口地址：** `POST /api/bill/finance/approve`

**请求头：**
```
Content-Type: application/json
X-User-Address: 0x1234567890abcdef  // 审核人地址
Authorization: Bearer <token>
```

**请求体（批准）：**
```json
{
  "applicationId": "app-uuid-001",
  "approvalResult": "APPROVED",
  "approvedAmount": 950000.00,
  "approvedRate": 5.5,
  "approvalComments": "审核通过，信用良好"
}
```

**请求体（拒绝）：**
```json
{
  "applicationId": "app-uuid-001",
  "approvalResult": "REJECTED",
  "approvalComments": "票据信用等级不足"
}
```

**请求参数说明：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| applicationId | String | 是 | 融资申请ID |
| approvalResult | String | 是 | 审核结果：APPROVED（批准）/ REJECTED（拒绝） |
| approvedAmount | BigDecimal | 条件必填 | 批准时必填，批准金额 |
| approvedRate | BigDecimal | 条件必填 | 批准时必填，批准利率 |
| approvalComments | String | 否 | 审核意见或拒绝原因 |

**业务规则：**
- 只能审核状态为 PENDING 的申请
- 批准时必须提供批准金额和批准利率
- 拒绝时只需提供拒绝原因

**响应示例（批准）：**
```json
{
  "code": 200,
  "message": "融资申请审核完成",
  "data": {
    "id": "app-uuid-001",
    "status": "APPROVED",
    "approvedAmount": 950000.00,
    "approvedRate": 5.5,
    "approveDate": "2026-02-02T14:30:00",
    "approvalComments": "审核通过，信用良好"
  }
}
```

---

### 3. 查询待审核的融资申请

**接口地址：** `GET /api/bill/finance/pending`

**请求头：**
```
Authorization: Bearer <token>
```

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| institutionId | String | 否 | 金融机构ID（为空则查询所有待审核申请） |

**请求示例：**
```
GET /api/bill/finance/pending
GET /api/bill/finance/pending?institutionId=inst-uuid-001
```

**响应示例：**
```json
{
  "code": 200,
  "message": "查询成功",
  "data": [
    {
      "id": "app-uuid-001",
      "billId": "bill-uuid-001",
      "billNo": "BILL20260101001",
      "billFaceValue": 1000000.00,
      "applicantId": "ent-uuid-001",
      "applicantName": "供应商A",
      "financialInstitutionId": "inst-uuid-001",
      "financialInstitutionName": "银行B",
      "financeAmount": 950000.00,
      "financeRate": 5.5,
      "financePeriod": 90,
      "status": "PENDING",
      "applyDate": "2026-02-02T10:30:00"
    }
  ]
}
```

---

### 4. 票据融资还款

**接口地址：** `POST /api/bill/finance/{applicationId}/repay`

**请求头：**
```
Content-Type: application/json
X-User-Address: 0x1234567890abcdef  // 还款人地址
Authorization: Bearer <token>
```

**请求体（全额还款）：**
```json
{
  "repayAmount": 962500.00,
  "repayType": "FULL",
  "repaymentProof": "转账凭证号TXN001"
}
```

**请求体（部分还款）：**
```json
{
  "repayAmount": 500000.00,
  "repayType": "PARTIAL",
  "repaymentProof": "转账凭证号TXN002"
}
```

**请求参数说明：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| applicationId | String | 是 | 融资申请ID（路径参数） |
| repayAmount | BigDecimal | 是 | 还款金额 |
| repayType | String | 否 | 还款类型：FULL（全额，默认）/ PARTIAL（部分） |
| repaymentProof | String | 否 | 还款凭证 |

**业务规则：**
- 只能对状态为 ACTIVE 的融资申请进行还款
- FULL 类型：系统自动计算利息 = 本金 × 利率 × 天数 / 365
- 还款后申请状态更新为 REPAID
- 票据状态更新为 PAID

**利息计算示例：**
```
利息 = 本金 × 年利率 × 融资天数 / 365
例如：950,000 × 5.5% × 90 / 365 = 12,876.71元
还款总额 = 950,000 + 12,876.71 = 962,876.71元
```

**响应示例：**
```json
{
  "code": 200,
  "message": "还款成功",
  "data": {
    "id": "app-uuid-001",
    "status": "REPAID",
    "actualRepaymentAmount": 962876.71,
    "repaymentDate": "2026-05-03T10:30:00",
    "txHash": "0xabcdef..."
  }
}
```

---

## 🔐 权限控制

### 申请人（企业）
- ✅ 创建融资申请
- ✅ 查看自己的融资申请
- ✅ 执行还款操作

### 金融机构
- ✅ 查看待审核的融资申请
- ✅ 审核融资申请（批准/拒绝）
- ✅ 查看已批准的融资申请

### 其他用户
- ❌ 无权限操作

---

## 📊 业务场景示例

### 场景1：企业融资流程

1. **企业A持有一张面值100万的票据**
   - 票据状态：NORMAL
   - 当前持票人：企业A

2. **企业A向银行申请融资**
   ```bash
   POST /api/bill/bill-001/finance
   {
     "financialInstitutionId": "bank-001",
     "financeAmount": 950000,
     "financeRate": 5.5,
     "financePeriod": 90
   }
   ```
   - 创建融资申请，状态：PENDING

3. **银行审核申请**
   ```bash
   POST /api/bill/finance/approve
   {
     "applicationId": "app-001",
     "approvalResult": "APPROVED",
     "approvedAmount": 950000,
     "approvedRate": 5.5,
     "approvalComments": "审核通过"
   }
   ```
   - 申请状态更新：APPROVED

4. **银行放款**
   - 实际放款金额：950,000元
   - 申请状态更新：ACTIVE
   - 票据状态更新：FINANCED

5. **企业A到期还款**
   ```bash
   POST /api/bill/finance/app-001/repay
   {
     "repayAmount": 962876.71,
     "repayType": "FULL"
   }
   ```
   - 计算利息：950,000 × 5.5% × 90 / 365 = 12,876.71元
   - 还款总额：962,876.71元
   - 申请状态更新：REPAID
   - 票据状态更新：PAID

---

## 🧪 测试要点

### 1. 融资申请测试
- ✅ 正常状态票据可申请
- ✅ 持票人可申请
- ✅ 融资金额不超过面值
- ❌ 已冻结票据不能申请
- ❌ 非持票人不能申请
- ❌ 融资金额超过面值应失败

### 2. 融资审核测试
- ✅ 批准申请更新状态
- ✅ 拒绝申请记录原因
- ❌ 不能重复审核
- ❌ 不能审核非PENDING状态

### 3. 融资还款测试
- ✅ 全额还款计算利息
- ✅ 还款后状态更新
- ❌ 不能重复还款
- ❌ 不能还款非ACTIVE状态

---

## 📝 注意事项

1. **票据状态限制**
   - 只有 NORMAL、ENDORSED、ISSUED 状态的票据可以申请融资
   - 融资后票据状态变为 FINANCED
   - 还款后票据状态变为 PAID

2. **利息计算**
   - FULL 类型：系统自动计算利息
   - PARTIAL 类型：按实际金额还款，不计利息

3. **区块链集成**
   - 融资申请、审核、还款操作都应上链记录
   - tx_hash 记录区块链交易哈希

4. **数据一致性**
   - 使用 @Transactional 保证数据库操作原子性
   - 票据状态和融资申请状态同步更新

---

## 🔗 相关文档

- [票据生命周期管理指南](BILL_LIFECYCLE_MANAGEMENT_GUIDE.md)
- [票据背书转让指南](BILL_ENDORSEMENT_GUIDE.md)
- [数据库表设计](DATABASE_SCHEMA.md)
- [API完整文档](SWAGGER_UI_URL)

---

## 📞 技术支持

如有问题，请联系技术团队或查看项目文档。
