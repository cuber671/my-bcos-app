# 票据业务逻辑设计文档（含仓单联动）

生成时间：2026-02-02
文档版本：v1.0
设计原则：货权与金钱债权联动，实现供应链金融闭环

---

## 📋 目录

1. [票据核心功能设计](#1-票据核心功能设计)
2. [票据与仓单联动业务](#2-票据与仓单联动业务)
3. [票据状态机设计](#3-票据状态机设计)
4. [票据业务流程设计](#4-票据业务流程设计)
5. [风险控制设计](#5-风险控制设计)
6. [API接口设计](#6-api接口设计)
7. [数据模型设计](#7-数据模型设计)

---

## 1. 票据核心功能设计

### 1.1 票据类型定义

根据中国票据法，系统支持以下票据类型：

#### 1.1.1 银行承兑汇票（Bank Acceptance Bill）
- **定义：** 银行承兑的远期汇票
- **特点：** 银行信用+商业信用，风险低
- **用途：** 跨区域支付、融资工具
- **承兑人：** 银行
- **付款责任人：** 承兑银行

#### 1.1.2 商业承兑汇票（Commercial Acceptance Bill）
- **定义：** 企业承兑的远期汇票
- **特点：** 纯商业信用，风险较高
- **用途：** 企业间支付、融资
- **承兑人：** 企业
- **付款责任人：** 承兑企业

#### 1.1.3 银行本票（Bank Note）
- **定义：** 银行签发的承诺付款票据
- **特点：** 银行信用，见票即付
- **用途：** 即期支付、现金替代
- **付款责任人：** 签发银行

---

### 1.2 票据七大功能设计

#### 1.2.1 支付功能

**场景1：票据替代现金支付**
```
买方 ──票据(100万)──> 卖方
     │
     └─> 背书转让抵消债务
```

**核心逻辑：**
1. 出票人开具票据，承诺到期付款
2. 持票人通过背书转让支付货款
3. 收款人持有票据，到期提示付款
4. 票据作为即时/远期债权债务清算载体

**业务规则：**
- ✅ 票据金额必须等于交易金额
- ✅ 票据到期日必须晚于交易完成日
- ✅ 背书必须连续
- ✅ 不得签发无真实交易背景的票据

---

#### 1.2.2 融资功能

**场景：票据贴现融资**
```
持票人 ──贴现──> 银行
   │      │
   │      └> 获得资金（面值 - 贴现利息）
   │
   └──> 背书转让给银行
```

**贴现计算逻辑：**
```
贴现利息 = 票据面值 × 贴现率 × 剩余天数 / 360
实付金额 = 票据面值 - 贴现利息

示例：
面值：100万
贴现率：4.5%
剩余天数：90天
贴现利息 = 100万 × 4.5% × 90 / 360 = 11,250元
实付金额 = 100万 - 11,250 = 988,750元
```

**质押融资逻辑：**
```
持票人 ──质押票据──> 银行
   │      │
   │      └> 获得贷款（票据面值的70-90%）
   │
   └──> 票据质押登记，到期还款赎回
```

**业务规则：**
- ✅ 贴现率：根据持票人信用等级动态调整
- ✅ 质押率：票据面值的70%-90%
- ✅ 票据必须处于NORMAL状态
- ✅ 已质押、已冻结、已转让的票据不能融资
- ✅ 贴现后票据权利转移给银行

---

#### 1.2.3 信用担保功能

**担保逻辑：**
1. **出票人担保**：第一付款责任人
2. **承兑人担保**：银行承兑则银行担保
3. **背书人担保**：背书人对后手承担担保责任
4. **保证人担保**（可选）：第三方担保

**追索逻辑：**
```
票据到期被拒付
    ↓
持票人可向前手追索
    ↓
追索顺序：持票人 → 背书人 → 出票人 → 承兑人
    ↓
所有前手承担连带责任
```

**业务规则：**
- ✅ 追索期限：票据到期日起2年
- ✅ 追索金额：票据金额 + 罚息 + 费用
- ✅ 拒票证明：需要银行拒付证明
- ✅ 通知前手：必须在法定期限内通知

---

#### 1.2.4 结算功能

**场景：多主体债权债务抵消**
```
A欠B 100万 ──────────┐
B欠C 100万 ───┐       │
C欠A 100万 ───┼───────┘
│         │
└─────────┘
三方通过票据背书转让抵消债务
```

**结算流程：**
1. A向B开具票据100万，抵消A欠B债务
2. B持有票据，背书转让给C，抵消B欠C债务
3. C持有票据，到期提示付款，完成三角债闭环

**业务规则：**
- ✅ 票据金额必须匹配债务金额
- ✅ 背书记载：债权债务抵消说明
- ✅ 多方协议：三角债清算协议
- ✅ 降低实际资金占用

---

#### 1.2.5 汇兑功能

**跨区域汇兑场景：**
```
北京企业 ──100万票据──> 上海供应商
  │                       │
  │                       └─> 背书转让给上海银行
  │
  └──> 北京银行承兑汇票
```

**汇兑优势：**
- ✅ 无需现金跨区域调拨
- ✅ 降低汇款费用
- ✅ 即时到账（票据背书转让）
- ✅ 延期付款（票据期限）

**业务规则：**
- ✅ 票据必须经过银行承兑
- ✅ 跨区域背书需要银行确认
- ✅ 汇兑手续费由出票人或持票人承担
- ✅ 到期自动兑付，无需持票人到场

---

#### 1.2.6 权利证明功能

**权利证明链：**
```
1. 合同 ──> 证明债权债务关系
2. 票据 ──> 证明票据权利
3. 背书 ──> 证明权利转让
4. 持有 ───> 证明当前权利人
```

**维权核心凭证：**
- ✅ 票据原件：物权凭证
- ✅ 背书连续：权利链完整
- ✅ 拒付证明：追索依据
- ✅ 公证：法律效力加强

**业务规则：**
- ✅ 合法持票人享有票据权利
- ✅ 恶意持票人不享有票据权利
- ✅ 票据丢失需要公示催告
- ✅ 票据伪造需追究法律责任

---

#### 1.2.7 风险管理功能

**锁定机制：**
1. **金额锁定**：票面金额固定
2. **到期日锁定**：还款期限固定
3. **付款主体锁定**：承兑人/出票人
4. **质押物锁定**：票据质押期间不得转让

**风险防控：**
- ✅ 信用风险：承兑人信用评级
- �票据风险评估模型
- ✅ 票据保险机制
- ✅ 拒备预警机制

---

## 2. 票据与仓单联动业务

### 2.1 核心场景：仓单质押 + 票据融资

#### 场景描述
```
企业（货主）拥有：
- 仓单：钢材100吨，价值450万
- 需要资金：400万

操作流程：
1. 企业以仓单为质押物
2. 银行开具以仓单质押为基础的银行承兑汇票400万
3. 企业将票据贴现，获得资金395万
4. 到期还款赎回票据和仓单
```

#### 业务流程设计

**步骤1：仓单质押审核**
```java
POST /api/ewr/{receiptId}/pledge/bill
```
**功能：** 仓单质押申请为票据融资做准备
**请求参数：**
```json
{
  "receiptId": "receipt-uuid-001",
  "pledgeType": "BILL_FINANCE",
  "requestedAmount": 4000000.00,
  "financialInstitutionId": "bank-uuid-001"
}
```

**业务规则：**
- ✅ 仓单状态必须为NORMAL
- ✅ 仓单未质押、未冻结、未拆分
- ✅ 仓单评估价值 ≥ 融资金额的120%
- ✅ 货主企业信用评级 ≥ A

**质押办理：**
1. 仓单质押登记（区块链上链）
2. 质押权设立（金融机构成为质押权人）
3. 质押物保管协议签署
4. 质押物权证生成

---

**步骤2：银行开具承兑汇票**
```java
POST /api/bill/issue/receipt-backed
```
**功能：** 银行开具以仓单为担保的承兑汇票
**请求参数：**
```json
{
  "billId": "bill-uuid-001",
  "billType": "BANK_ACCEPTANCE_BILL",
  "faceValue": 4000000.00,
  "issueDate": "2026-02-02",
  "dueDate": "2026-05-02",
  "drawer": "enterprise-uuid-001",
  "drawee": "bank-uuid-001",
  "payee": "supplier-uuid-001",
  "receiptPledgeId": "pledge-uuid-001",
  "receiptId": "receipt-uuid-001",
  "pledgeValue": 4500000.00
}
```

**业务规则：**
- ✅ 必须先完成仓单质押
- ✅ 票据金额 ≤ 仓单评估价值的90%
- ✅ 银行承兑（银行成为主债务人）
- ✅ 票据与仓单关联（无法单独转让）

**票据记载：**
```
票据备注：
- 质押仓单编号：EWR20260126000001
- 质押价值：450万
- 质押合同编号：PLEDGE-2026-0202-001
- 仓储企业：XX仓储公司
- 货物位置：A区03栋
```

---

**步骤3：票据贴现融资**
```java
POST /api/bill/{billId}/discount
```
**功能：** 持票人贴现票据获得资金
**请求参数：**
```json
{
  "billId": "bill-uuid-001",
  "discountRate": 4.5,
  "discountPeriod": 90,
  "financialInstitutionId": "bank-uuid-001"
}
```

**贴现计算：**
```
票面金额：400万
贴现率：4.5%
贴现期：90天
贴现息 = 400万 × 4.5% × 90 / 360 = 45,000元
实付金额 = 400万 - 45,000 = 3,955,000元
```

**权利转移：**
- ✅ 票据权利转移给贴现银行
- ✅ 背书：持票人 → 银行
- ✅ 质押物变更：银行成为第二质押权人

---

**步骤4：到期还款赎回**
```java
POST /api/bill/pledge/redeem
```
**功能：** 到期还款，赎回票据和仓单
**请求参数：**
```json
{
  "pledgeId": "pledge-uuid-001",
  "billId": "bill-uuid-001",
  "repayAmount": 4000000.00,
  "repaymentProof": "转账凭证"
}
```

**赎回流程：**
1. 支付票据本金 + 利息
2. 银行归还票据
3. 解除仓单质押
4. 质押权消灭

**违约处理：**
```
如果到期未还款：
1. 银行持有票据向承兑银行提示付款
2. 承兑银行付款
3. 银行行使仓单质押权
4. 拍卖质押仓单，优先受偿
5. 不足部分向企业追索
```

**受偿顺序：**
```
1. 仓单拍卖款（质押权优先）
2. 质权人保证金
3. 票据款项（已付给银行）
4. 企业补充资金
```

---

### 2.2 次要场景：票据支付 + 仓单交付

#### 场景描述
```
交易场景：
- 买方：北京A公司（需要钢材）
- 卖方：上海B公司（持有仓单）
- 交易金额：100万

支付方式：
- 银行承兑汇票90万
- 现金10万
```

#### 业务流程

**步骤1：仓单货权确认**
```java
POST /api/ewr/{receiptId}/delivery-reserve
```
**功能：** 预留仓单货权，配合票据支付
**请求参数：**
```json
{
  "receiptId": "receipt-uuid-001",
  "paymentMethod": "BILL_PAYMENT",
  "paymentBillId": "bill-uuid-001",
  "buyerId": "enterprise-uuid-001",
  "sellerId": "enterprise-uuid-002",
  "cashAmount": 100000.00,
  "billAmount": 900000.00
}
```

**货权确认：**
- ✅ 仓单状态变更为RESERVED（预留）
- ✅ 仓单与票据绑定
- ✅ 不得单独转让或质押
- ✅ 交付时自动解除预留

---

**步骤2：票据背书转让**
```java
POST /api/bill/{billId}/endorse/payment
```
**功能：** 票据背书转让给卖方
**请求参数：**
```json
{
  "billId": "bill-uuid-001",
  "newHolder": "enterprise-uuid-002",
  "endorsementType": "PAYMENT",
  "receiptId": "receipt-uuid-001",
  "transactionId": "txn-uuid-001"
}
```

**背书记录：**
```
背书类型：支付转让
背书人：买方
被背书人：卖方
交易编号：TXN-20260202-001
关联仓单：EWR20260126000001
背书时间：2026-02-02T10:00:00
```

---

**步骤3：货款确认与交付**
```java
POST /api/ewr/{receiptId}/delivery-confirm
```
**功能：** 确认收到票据，交付仓单
**请求参数：**
```json
{
  "receiptId": "receipt-uuid-001",
  "billId": "bill-uuid-001",
  "buyerConfirm": true,
  "sellerConfirm": true,
  "deliveryType": "PLEDGED"
}
```

**交付逻辑：**
1. 双方确认交易完成
2. 仓单状态：RESERVED → DELIVERED
3. 保管人：卖方 → 买方
4. 仓单绑定票据自动解除

---

**步骤4：票据到期付款**
```java
POST /api/bill/{billId}/payment
```
**功能：** 票据到期自动付款
**业务规则：**
- ✅ 承兑银行自动付款
- ✅ 无需持票人提示
- ✀ 资金自动到账
- ✀ 生成付款凭证

---

### 2.3 延伸场景：仓单转让 + 票据结算

#### 场景描述：三角债票据结算
```
债务关系：
- A欠B 100万
- B欠C 100万
- C欠A 100万

解决方案：
1. C将仓单EWR001（价值100万）转让给A
2. A以仓单为质押，向银行开票100万给C
3. C背书转让票据给B，抵消C欠B债务
4. B背书转让票据给A，抵消A欠B债务
5. 银行到期付款，完成三角债闭环
```

#### 业务流程

**步骤1：仓单转让给债权人**
```java
POST /api/ewr/{receiptId}/transfer/to-creditor
```
**功能：** C将仓单转让给A，抵销C欠A的债务
**请求参数：**
```json
{
  "receiptId": "receipt-uuid-001",
  "fromOwner": "enterprise-uuid-003",
  "toOwner": "enterprise-uuid-001",
  "transferReason": "债务抵销",
  "debtAmount": 1000000.00,
  "debtProof": "合同编号20260126001"
}
```

**转让规则：**
- ✅ 仓单价值 ≈ 债务金额
- ✅ 债权确认书或协议
- ✅ 债务关系真实有效
- ✅ 背书转让手续完整

---

**步骤2：仓单质押开票**
```java
POST /api/bill/issue/receipt-backed-triangle
```
**功能：** A以仓单为质押，开票给C
**业务规则：** 同核心场景

---

**步骤3：票据背书抵债**
```java
POST /api/bill/{billId}/endorse/debt-settlement
```
**功能：** C背书票据给B，抵销债务
**请求参数：**
```json
{
  "billId": "bill-uuid-001",
  "creditor": "enterprise-uuid-002",
  "debtor": "enterprise-uuid-003",
  "debtAmount": 1000000.00
}
```

---

**步骤4：最终结算**
```
票据流转：
C → B → A → 银行 → C
（循环完成）

仓单状态：
EWR001: NORMAL → DELIVERED（交付给A）
```

---

## 3. 票据状态机设计

### 3.1 票据状态定义

```java
public enum BillStatus {
    // 开票阶段
    DRAFT,                    // 草稿 - 未提交
    PENDING_ISSUANCE,         // 待开票 - 已提交等待审核
    ISSUED,                  // 已开票 - 票据已生成

    // 流通阶段
    NORMAL,                  // 正常 - 可流通
    ENDORSED,                // 已背书 - 已背书转让
    PLEDGED,                 // 已质押 - 已质押融资

    // 融资阶段
    DISCOUNTED,               // 已贴现 - 已贴现给银行
    FINANCED,                 // 已融资 - 已质押融资

    // 异常状态
    FROZEN,                  // 已冻结 - 法律纠纷
    EXPIRED,                 // 已过期 - 已到期未付款
    DISHONORED,              // 已拒付 - 承兑人拒付
    CANCELLED,                // 已作废 - 票据作废

    // 结算状态
    PAID,                    // 已付款 - 已完成付款
    SETTLED                  // 已结算 - 已完成债权债务清算
}
```

### 3.2 状态流转图

```
                    开票
    ┌─────────────────────┐
    ↓                     │
   DRAFT ─────────────> PENDING_ISSUANCE
    │                     │
    │                     ↓
    │                   ISSUED
    │                     │
    │                     ↓
    │                   NORMAL
    │                     │
    │           ┌─────┼─────┐
    │           │     │     │
    │           │     │     ↓
    │           │     │  ENDORSED
    │           │     │     │
    │           │     │     ↓
    │           │     │  PLEDGED ─>─┐
    │           │     │                 │
    │           │     └────> FINANCED        │
    │           │                           │
    │           └───────────────────────┘
    │
    └──────> EXPIRED ──> PAID
                │
                ├──────> DISHONORED
                │
                └──────> CANCELLED

异常状态：
    FROZEN：FROZEN → 原状态（解冻后）
```

---

## 4. 票据业务流程设计

### 4.1 票据全生命周期流程

#### 流程图
```
1. 开票阶段
   出票人 ──┐
   ─────> 票据开立申请
   承兑人 ──┘
   ↓
2. 审核阶段
   银行审核 ──> 承兑/拒付
   ↓
3. 流通阶段
   正常流通
   ├─> 背书转让（支付）
   ├─> 贴押融资
   └─> 贴现贴现
   ↓
4. 到期阶段
   提示付款 ──> 承兑付款
   ↓
5. 异常处理
   拒付 ──> 追索
   逾期 ──> 罚息
```

---

### 4.2 票据开票流程

```java
POST /api/bill/issue
```

**步骤1：填写开票申请**
- 出票人信息
- 收款人信息
- 票据金额
- 到期日
- 票据类型

**步骤2：承兑人审核**
- 银行承兑：银行审核企业信用
- 商业承兑：企业审核

**步骤3：票据生成**
- 票据编号生成
- 区块链上链
- 票据签发

**步骤4：票据交付**
- 交付给收款人
- 背书转让登记

---

### 4.3 票据背书流程

```java
POST /api/bill/{id}/endorse
```

**背书类型：**
1. **转让背书**：票据权利转让
2. **质押背书**：票据质押融资
3. **委托收款背书**：委托银行收款

**背书要求：**
- ✅ 背书连续
- ✅ 记载完整
- ✅ 签章真实
- ✅ 符合票据法规定

**背书链：**
```
出票人 ──> A ──> B ──> C
        │     │     │
        └─> 背书 ──> 背书 ──> 背书
```

---

### 4.4 票据贴现流程

```java
POST /api/bill/{id}/discount
```

**步骤1：贴现申请**
- 持票人提交贴现申请
- 银行审核票据和持票人信用
- 确定贴现率和期限

**步骤2：贴现计算**
```
贴现利息 = 面值 × 贴现率 × 天数 / 360
实付金额 = 面值 - 贴现利息
```

**步骤3：权利转移**
- 票据背书给银行
- 质押登记（如需）
- 资金到账

**步骤4：票据到期处理**
- 银行提示付款
- 承兑人付款
- 资金清算

---

### 4.5 票据追索流程

```java
POST /api/bill/{id}/recourse
```

**追索触发条件：**
1. 票据到期被拒付
2. 承兑人破产
3. 票据伪造
4. 其他法律原因

**追索流程：**
1. 持票人取得拒付证明
2. 通知前手（背书人）
3. 行使追索权
4. 前手付款后再向其前手追索

**追索顺序：**
```
持票人 → 最后背书人 → ... → 第一背书人 → 出票人 → 承兑人
```

**追索时效：**
- 票据到期日起2年
- 需在法定期限内行使

---

## 5. 风险控制设计

### 5.1 风险类型

#### 5.1.1 票据风险
```
1. 信用风险
   - 承兑人无力兑付
   - 出票人破产
   - 背书人无力偿付

2. 操作风险
   - 票据伪造
   - 票据变造
   - 假背书

3. 法律风险
   - 票据要素不全
   - 背书不连续
   - 超过时效

4. 市场风险
   - 利率波动
   - 汇率风险（涉外票据）
   - 流动性风险
```

#### 5.1.2 仓单风险
```
1. 货物风险
   - 货物灭失
   - 货物贬值
   - 质量问题

2. 保管风险
   - 保管人违约
   - 仓单伪造
   - 重复质押

3. 权属风险
   - 权属争议
   - 质押无效
   - 查封冻结
```

---

### 5.2 风险防控措施

#### 5.2.1 票据风险防控

**开票阶段：**
1. ✅ 企业信用评级
   - 查询企业征信
   - 评估偿债能力
   - 设定开票额度

2. ✅ 票据真实性验证
   - 交易背景审查
   - 合同验证
   - 资金流向核实

3. ✅ 要式性审查
   - 票据要素齐全
   - 符合票据法
   - 签章真实有效

**流通阶段：**
1. ✅ 背书连续性检查
   - 背书链完整
   - 签章连续
   - 记载清晰

2. ✅ 持票人验证
   - 身份认证
   - 权利确认
   - 反洗钱检查

3. ✅ 票据查询
   - 挂失查询
   - 伪造查询
   - 冻结查询

**融资阶段：**
1. ✅ 贴现率动态调整
   - 基于信用评级
   - 基于票据类型
   - 基于市场利率

2. ✅ 质押率控制
   - 票据面值的70%-90%
   - 企业综合评估
   - 风险敞口控制

3. ✅ 担保措施
   - 保证人担保
   - 抵押物补充
   - 保险机制

---

#### 5.2.2 仓单风险防控

**仓单管理：**
1. ✅ 仓单真实性验证
   - 区块链查询
   - 仓储企业确认
   - 货物核查

2. ✅ 货权确认
   - 权属清晰
   - 无争议
   - 可合法质押

3. ✅ 货物价值评估
   - 市场价值评估
   - 折旧率计算
   - 价值动态监控

**保管风险：**
1. ✅ 仓储企业资质审查
   - 资质认证
   - 保险覆盖
   - 风险保证金

2. ✅ 仓单绑定货物
   - 定期盘点
   - 数量监控
   - 质量检查

3. ✅ 仓单状态监控
   - 状态变更通知
   - 异常预警
   - 风险提示

---

#### 5.2.3 联动业务风险防控

**仓单质押 + 票据融资：**
1. ✅ 价值匹配原则
   - 票据金额 ≤ 仓单评估价值 × 90%
   - 仓单价值动态重评

2. ✅ 双重保障机制
   - 票据权利（票据）
   - 质押权利（仓单）

3. **违约处理优先级：**
   ```
   优先级1：仓单拍卖款
   优先级2：保证金
   优先级3：票据款项
   优先级4：企业补足
   ```

4. ✅ 风险预警
   - 仓单贬值预警
   - 到期提醒
   - 逾期预警

---

### 5.3 风险评估模型

#### 5.3.1 票据风险评分
```java
@ApiModel("票据风险评估模型")
public class BillRiskAssessment {

    // 风险维度
    @ApiModelProperty("出票人信用评分 (0-100)")
    private Integer drawerCreditScore;

    @ApiModelProperty("承兑人信用评分 (0-100)")
    private Integer acceptorCreditScore;

    @ApiModelProperty("票据类型风险系数")
    private Double billTypeRiskFactor;

    @ApiModelProperty("票据期限风险 (期限越长风险越高)")
    private Double termRiskFactor;

    @ApiModelProperty("背书连续性评分")
    private Integer endorsementScore;

    @ApiModelProperty("逾期记录")
    private Integer overdueRecordCount;

    // 风险等级
    @ApiModelProperty("综合风险等级")
    private String riskLevel;  // LOW, MEDIUM, HIGH, CRITICAL

    // 贴现/质押建议
    @ApiModelProperty("建议贴现率 (%)")
    private Double suggestedDiscountRate;

    @ApiModelProperty("建议质押率 (%)")
    private Double suggestedPledgeRatio;
}
```

#### 5.3.2 仓单风险评分
```java
@ApiModel("仓单风险评估模型")
public class ReceiptRiskAssessment {

    // 风险维度
    @ApiModelProperty("货物类型风险系数")
    private Double goodsTypeRiskFactor;

    @ApiModelProperty("货物价值波动率")
    private Double valueVolatility;

    @ApiModelProperty("仓储企业信用评分")
    private Integer warehouseCreditScore;

    @ApiModelProperty("仓单状态")
    private String receiptStatus;

    @ApiModelProperty("仓单龄期")
    private Integer receiptAge;

    @ApiModelProperty("质押率")
    private Double pledgeRatio;

    // 风险等级
    @ApiModelProperty("综合风险等级")
    private String riskLevel;

    // 价值评估
    @ApiModelProperty("当前估值")
    private BigDecimal currentValue;

    @ApiModelProperty("估值折算率")
    private Double valuationRatio;
}
```

---

## 6. API接口设计

### 6.1 票据基础管理接口

#### 6.1.1 票据开立
```java
POST /api/bill/issue
```
**功能说明：** 开立票据（汇票/本票）
**权限要求：** 出票人
**请求参数：**
```json
{
  "billType": "BANK_ACCEPTANCE_BILL",
  "faceValue": 1000000.00,
  "currency": "CNY",
  "issueDate": "2026-02-02",
  "dueDate": "2026-05-02",
  "drawer": {
    "enterpriseId": "enterprise-uuid-001",
    "enterpriseName": "XX贸易公司",
    "accountNumber": "123456789",
    "bankAddress": "0x..."
  },
  "drawee": {
    "enterpriseId": "bank-uuid-001",
    "enterpriseName": "XX银行",
    "bankAddress": "0x..."
  },
  "payee": {
    "enterpriseId": "enterprise-uuid-002",
    "enterpriseName": "YY供应商",
    "accountNumber": "987654321"
  },
  "tradeInfo": {
    "tradeContractId": "contract-uuid-001",
    "tradeAmount": 1000000.00,
    "goodsDescription": "钢材采购",
    "tradeDate": "2026-02-02"
  }
}
```

---

#### 6.1.2 票据承兑
```java
POST /api/bill/{id}/accept
```
**功能说明：** 承兑人承兑票据
**权限要求：** 承兑人（银行或企业）
**请求参数：**
```json
{
  "acceptanceType": "ACCEPT",
  "acceptanceComments": "同意承兑",
  "creditLimitAssigned": 5000000.00
}
```

---

#### 6.1.3 票据付款
```java
POST /api/bill/{id}/payment
```
**功能说明：** 承兑人到期付款
**权限要求：** 承兑人
**触发时机：** 系统自动触发或手动付款
**响应：**
```json
{
  "billId": "bill-uuid-001",
  "faceValue": 1000000.00,
  "paidAmount": 1000000.00,
  "paymentTime": "2026-05-02T10:00:00",
  "paymentProof": "tx-hash-xxx",
  "status": "PAID"
}
```

---

### 6.2 票据背书接口

#### 6.2.1 背书转让
```java
POST /api/bill/{id}/endorse
```
**功能说明：** 票据背书转让
**权限要求：** 当前持票人
**请求参数：**
```json
{
  "newHolder": "enterprise-uuid-003",
  "endorsementType": "TRANSFER",
  "endorsementReason": "货款支付",
  "relatedContract": "contract-uuid-001"
}
```

---

#### 6.2.2 背书查询
```java
GET /api/bill/{id}/endorsements
```
**功能说明：** 查询票据背书历史
**响应：**
```json
{
  "billId": "bill-uuid-001",
  "endorsements": [
    {
      "endorser": "企业A",
      "endorsee": "企业B",
      "type": "TRANSFER",
      "time": "2026-02-02T10:00:00",
      "reason": "货款支付"
    }
  ]
}
```

---

### 6.3 票据融资接口

#### 6.3.1 票据贴现
```java
POST /api/bill/{id}/discount
```
**功能说明：** 票据贴现融资
**权限要求：** 持票人
**请求参数：**
```json
{
  "discountRate": 4.5,
  "discountPeriod": 90,
  "financialInstitutionId": "bank-uuid-001",
  "applicationReason": "流动资金需求"
}
```

---

#### 6.3.2 票据质押融资
```java
POST /api/bill/{id}/pledge
```
**功能说明：** 票据质押融资
**权限要求：** 持票人
**请求参数：**
```json
{
  "pledgeAmount": 900000.00,
  "pledgePeriod": 180,
  "financialInstitutionId": "bank-uuid-001",
  "pledgePurpose": "流动资金贷款",
  "collateralInfo": {
    "additionalCollateral": "房产证",
    "guarantor": "担保公司"
  }
}
```

---

### 6.4 仓单票据联动接口

#### 6.4.1 仓单质押开票申请
```java
POST /api/bill/issue/receipt-backed
```
**功能说明：** 以仓单质押为基础开立票据
**权限要求：** 货主企业
**核心逻辑：**
1. 验证仓单状态
2. 办理仓单质押
3. 银行审核授信
4. 开具承兑汇票
5. 票据与仓单绑定

---

#### 6.4.2 票据支付仓单交付
```java
POST /api/bill/{billId}/pay-with-receipt
```
**功能说明：** 票据支付 + 仓单货权转移
**权限要求：** 买卖双方
**核心逻辑：**
1. 票据背书转让
2. 仓单货权确认
3. 票据到期付款
4. 仓单货权交付

---

#### 6.4.3 票据结算仓单抵债
```java
POST /api/bill/settle/debt-with-receipt
```
**功能说明：** 票据结算 + 仓单抵债
**核心逻辑：**
1. 仓单转让给债权人
2. 票据背书抵债
3. 完成三角债闭环

---

## 7. 数据模型设计

### 7.1 票据实体扩展

```java
@Entity
@Table(name = "bill")
public class Bill {

    // === 基础信息 ===
    @Id
    private String billId;

    @Column(name = "bill_type")
    private String billType;  // BANK_ACCEPTANCE_BILL, COMMERCIAL_BILL, BANK_NOTE

    @Column(name = "face_value")
    private BigDecimal faceValue;

    @Column(name = "currency")
    private String currency;  // CNY, USD

    @Column(name = "issue_date")
    private LocalDateTime issueDate;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    // === 参与方 ===
    @Column(name = "drawer_id")
    private String drawerId;  // 出票人

    @Column(name = "drawer_name")
    private String drawerName;

    @Column(name = "drawer_address")
    private String drawerAddress;

    @Column(name = "drawee_id")
    private String draweeId;  // 承兑人

    @Column(name = "drawee_name")
    private String draweeName;

    @Column(name = "drawee_address")
    private String draweeAddress;

    @Column(name = "payee_id")
    private String payeeId;  // 收款人

    @Column(name = "payee_name")
    private String payeeName;

    @Column(name = "payee_address")
    private String payeeAddress;

    // === 状态信息 ===
    @Column(name = "bill_status")
    @Enumerated(EnumType.STRING)
    private BillStatus billStatus;

    @Column(name = "blockchain_status")
    @Enumerated(EnumType.STRING)
    private BlockchainStatus blockchainStatus;

    // === 融资信息 ===
    @Column(name = "discount_rate")
    private BigDecimal discountRate;  // 贴现率

    @Column(name = "discount_amount")
    private BigDecimal discountAmount;  // 贴现金额

    @Column(name = "discount_date")
    private LocalDateTime discountDate;  // 贴现日期

    @Column(name = "pledge_amount")
    private BigDecimal pledgeAmount;  // 质押金额

    @Column(name = "pledge_institution")
    private String pledgeInstitution;  // 质押机构

    // === 仓单联动 ===
    @Column(name = "receipt_pledge_id")
    private String receiptPledgeId;  // 关联的仓单质押ID

    @Column(name = "backed_receipt_id")
    private String backedReceiptId;  // 担保仓单ID

    @Column(name = "receipt_pledge_value")
    private BigDecimal receiptPledgeValue;  // 仓单担保价值

    // === 追索信息 ===
    @Column(name = "dishonored")
    private Boolean dishonored;  // 是否拒付

    @Column(name = "dishonored_date")
    private LocalDateTime dishonoredDate;  // 拒付日期

    @Column(name = "recourse_status")
    private String recourseStatus;  // 追索状态

    // === 结算信息 ===
    @Column(name = "settlement_id")
    private String settlementId;  // 结算编号

    @Column(name = "related_debts")
    private String relatedDebts;  // 关联债务（JSON）

    // === 审计信息 ===
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

---

### 7.2 票据质押融资申请实体

```java
@Entity
@Table(name = "bill_pledge_application")
public class BillPledgeApplication {

    @Id
    private String applicationId;

    @Column(name = "bill_id")
    private String billId;

    @Column(name = "pledge_amount")
    private BigDecimal pledgeAmount;

    @Column(name="pledge_period")
    private Integer pledgePeriod;

    @Column(name = "financial_institution_id")
    private String financialInstitutionId;

    @Column(name = "application_status")
    private String applicationStatus;  // PENDING, APPROVED, REJECTED

    @Column(name = "collateral_info")
    private String collateralInfo;  // 额外担保物（JSON）

    @Column(name = "risk_assessment")
    private String riskAssessment;  // 风险评估结果（JSON）

    @Column(name = "approval_comments")
    private String approvalComments;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
```

---

### 7.3 票据背书记录实体

```java
@Entity
@Table(name = "bill_endorsement")
public class BillEndorsement {

    @Id
    private String endorsementId;

    @Column(name = "bill_id")
    private String billId;

    @Column(name = "endorser_id")
    private String endorserId;

    @Column(name = "endorser_name")
    private String endorserName;

    @Column(name = "endorsee_id")
    private String endorseeId;

    @Column(name = "endorsee_name")
    private String endorseeName;

    @Column(name = "endorsement_type")
    private String endorsementType;  // TRANSFER, PLEDGE, COLLECTION

    @Column(name = "endorsement_reason")
    private String endorsementReason;

    @Column(name = "related_contract")
    private String relatedContract;

    @Column(name = "endorsement_date")
    private LocalDateTime endorsementDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
```

---

### 7.4 票据追索记录实体

```java
@Entity
@Table(name = "bill_recourse")
public class BillRecourse {

    @Id
    private String recourseId;

    @Column(name = "bill_id")
    private String billId;

    @Column(name = "dishonored_date")
    private LocalDateTime dishonoredDate;

    @Column(name = "recourse_amount")
    private BigDecimal recourseAmount;

    @Column(name = "recourse_status")
    private String recourseStatus;  // INITIATED, IN_PROGRESS, COMPLETED, FAILED

    @Column(name = "notified_parties")
    private String notifiedParties;  // 已通知的前手

    @Column(name = "recourse_results")
    private String recourseResults;  // 追索结果（JSON）

    @Column(name = "settled_amount")
    private BigDecimal settledAmount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
```

---

## 8. 实施建议

### 8.1 待审核确认的业务设计清单

#### 核心业务设计
- ✅ 票据七大功能设计
- ✅ 票据与仓单三大联动场景
- ✅ 票据状态机设计
- ✅ 完整业务流程设计
- ✅ 风险控制体系

#### 接口设计
- ✅ 票据基础管理接口（6个）
- ✅ 票据背书接口（3个）
- ✅ 票据融资接口（5个）
- ✅ 票据支付结算接口（4个）
- ✅ 票据追索接口（4个）
- ✅ 仓单票据联动接口（3个）

#### 数据模型
- ✅ Bill实体扩展字段
- ✅ BillPledgeApplication实体
- ✅ BillEndorsement实体
- ✅ BillRecourse实体

---

### 8.2 待确认的关键设计决策

#### 决策1：票据贴现计算模型
```
方案A：简单利息计算
贴现息 = 面值 × 贴现率 × 天数 / 360

方案B：复合利息计算
贴现息 = 面值 × (1 + 贴现率)^天数 - 面值

建议：方案A（行业标准，简单透明）
```

#### 决策2：仓单质押率设定
```
方案A：固定质押率
仓单质押率 = 90%

方案B：动态质押率
仓单质押率 = 90% - 风险系数 × 贬值波动率

建议：方案B（更科学，风险敏感）
```

#### 决策3：票据追索顺序
```
标准追索顺序：
持票人 → 最后背书人 → ... → 出票人 → 承兑人

例外情况：
- 承兑银行：主债务人，可以跳过中间直接追索
- 保证人：可以并行追索
- 票据伪造：直接向出票人追索
```

#### 决策4：仓单票据违约受偿顺序
```
优先级1：仓单拍卖款（质押权优先）
优先级2：保证金
优先级3：票据款项
优先级4：企业补足资金
```

---

### 8.3 需要新增的Repository方法

```java
// BillRepository
Optional<Bill> findByBillNo(String billNo);
List<Bill> findByDrawerId(String drawerId);
List<Bill> findByDraweeId(String draweeId);
List<Bill> findByPayeeId(String payeeId);
List<Bill> findByBillStatusAndDueDateBefore(
    BillStatus status,
    LocalDateTime date
);

// BillEndorsementRepository
List<BillEndorsement> findByBillIdOrderByDate(String billId);
List<BillEndorsement> findByEndorserId(String endorserId);
Optional<BillEndorsement> findLatestEndorsement(String billId);

// BillPledgeApplicationRepository
List<BillPledgeApplication> findByBillId(String billId);
List<BillPledgeApplication> findByStatus(String status);
```

---

## 9. 总结

### 9.1 核心价值

**票据功能：**
- ✅ 支付功能：替代现金，跨区域清算
- ✅ 融资功能：贴现、质押、转贴现
- ✅ 信用担保：无条件付款承诺
- ✅ 结算功能：背书转让抵消债务
- ✅ 汇兑功能：无现金资金划转
- ✅ 权利证明：债权凭证
- ✅ 风险管理：锁定金额、到期日、主体

**仓单票据联动：**
- ✅ 核心场景：仓单质押 + 票据融资
- ✅ 次要场景：票据支付 + 仓单交付
- ✅ 延伸场景：仓单转让 + 票据结算

---

### 9.2 实施优先级

**第一阶段：核心票据功能（P0）**
1. 票据开立、承兑、付款
2. 票据背书转让
3. 票据贴现融资
4. 基础追索功能

**第二阶段：仓单票据联动（P1）**
1. 仓单质押开票
2. 票据支付仓单交付
3. 票据结算仓单抵债
4. 风险评估系统

**第三阶段：高级功能（P2）**
1. 票据池管理
2. 多票据清算
3. 票据证券化
4. AI风险评估

---

## 10. 下一步

请您审核此业务逻辑设计：

1. ✅ 票据七大功能设计是否合理？
2. ✅ 票据与仓单联动场景是否完整？
3. ✅ 风险控制措施是否充分？
4. ✅ API接口设计是否符合需求？
5. ✅ 数据模型设计是否完善？
6. ✅ 是否需要调整业务流程？
7. ✅ 是否有遗漏的关键功能？

**审核通过后，我将开始进行代码开发。**

---

**文档版本：** v1.0
**设计时间：** 2026-02-02
**审核状态：** ⏳ 等待审核
