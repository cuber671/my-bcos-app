# 票据池管理功能 - 快速开始指南

版本：v1.0
创建时间：2026-02-02

---

## 🎯 功能概述

票据池管理功能允许金融机构通过统一的平台查看和投资符合条件的票据，为票据持票人提供快速融资渠道。

### 核心价值
- ✅ **提高票据流动性**：票据持票人可以快速将票据变现
- ✅ **增加投资渠道**：金融机构获得新的投资产品
- ✅ **信息透明**：统一的票据池展示，信息透明公开
- ✅ **业务闭环**：完善票据全生命周期管理

---

## 🚀 快速开始

### 1. 查询票据池

**接口：**
```bash
GET /api/bill/pool
```

**请求示例：**
```bash
# 基础查询
curl -X GET "http://localhost:8080/api/bill/pool?page=0&size=10" \
  -H "Authorization: Bearer <your-token>"

# 带筛选条件查询
curl -X GET "http://localhost:8080/api/bill/pool?" \
  "billType=BANK_ACCEPTANCE_BILL" \
  "&minAmount=500000" \
  "&maxAmount=2000000" \
  "&minRemainingDays=30" \
  "&maxRemainingDays=180" \
  "&page=0&size=20" \
  -H "Authorization: Bearer <your-token>"
```

**响应示例：**
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "content": [
      {
        "billId": "bill-001",
        "billNo": "BILL20260101001",
        "billType": "BANK_ACCEPTANCE_BILL",
        "faceValue": 1000000.00,
        "remainingDays": 90,
        "maturityDate": "2026-05-03T00:00:00",
        "acceptorName": "中国工商银行",
        "acceptorRating": "AAA",
        "acceptorType": "BANK",
        "currentHolderName": "供应商A",
        "expectedReturn": 5.5,
        "riskScore": 15,
        "riskLevel": "LOW",
        "canInvest": true,
        "investmentAdvice": "RECOMMENDED"
      }
    ],
    "totalElements": 150
  }
}
```

---

### 2. 票据投资

**接口：**
```bash
POST /api/bill/pool/{billId}/invest
```

**请求示例：**
```bash
curl -X POST "http://localhost:8080/api/bill/pool/bill-uuid-001/invest" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-token>" \
  -H "X-User-Address: 0x... (投资机构地址)" \
  -d '{
    "investAmount": 950000.00,
    "investRate": 5.5,
    "investDate": "2026-02-03T14:00:00",
    "investmentNotes": "看好该票据，决定投资"
  }'
```

**响应示例：**
```json
{
  "code": 200,
  "message": "投资成功",
  "data": {
    "investmentId": "invest-uuid-001",
    "billId": "bill-uuid-001",
    "billNo": "BILL20260101001",
    "investAmount": 950000.00,
    "investRate": 5.5,
    "expectedReturn": 50000.00,
    "investmentDays": 90,
    "maturityAmount": 1000000.00,
    "status": "CONFIRMED",
    "investmentDate": "2026-02-03T14:30:00",
    "originalHolderName": "供应商A",
    "investorName": "XX银行",
    "txHash": "0xabcdef..."
  }
}
```

**投资计算说明：**
```
票据面值：1,000,000元
投资利率：5.5%
投资天数：90天

贴现 = 1,000,000 × 5.5 × 90 / 36000 = 13,750元
实付 = 1,000,000 - 13,750 = 986,250元（如果按面值购买）
收益 = 1,000,000 - 986,250 = 13,750元
年化收益率 = 13,750 / 986,250 × 365 / 90 × 100% = 5.66%
```

---

### 3. 查询投资记录

**接口：**
```bash
GET /api/bill/pool/investments?institutionId={institutionId}
```

**请求示例：**
```bash
curl -X GET "http://localhost:8080/api/bill/pool/investments?institutionId=bank-uuid-001" \
  -H "Authorization: Bearer <your-token>"
```

**响应示例：**
```json
{
  "code": 200,
  "message": "查询成功",
  "data": [
    {
      "investmentId": "invest-001",
      "billNo": "BILL20260101001",
      "investAmount": 950000.00,
      "expectedReturn": 50000.00,
      "status": "CONFIRMED",
      "investmentDate": "2026-02-03T14:30:00"
    }
  ]
}
```

---

## 📊 票据池准入条件

票据必须同时满足以下条件才能进入票据池：

1. ✅ **票据状态** = NORMAL（正常）
2. ✅ **已上链**（区块链状态 = ONCHAIN）
3. ✅ **未过期**（到期日期 > 当前时间）
4. ✅ **已承兑**（有承兑记录）
5. ✅ **剩余天数 ≥ 30天**

**排除条件：**
- ❌ 票据状态不是NORMAL（如已贴现、已质押、已融资等）
- ❌ 未上链或上链失败
- ❌ 已过期
- ❌ 剩余天数少于30天

---

## 🔑 投资规则

### 投资金额规则
```
最小投资金额 = 票据面值 × 10%
最大投资金额 = 票据面值
投资金额必须在此范围内
```

**示例：**
```
票据面值：1,000,000元
最小投资：100,000元
最大投资：1,000,000元
```

### 投资资格
- ✅ 只有金融机构可以投资
- ✅ 金融机构状态必须正常
- ✅ 不能投资自己持有的票据
- ✅ 票据不能有未完成的投资

### 投资流程
```
1. 查询票据池，找到合适的票据
2. 核对票据信息（面值、期限、承兑人等）
3. 提交投资申请
4. 系统执行背书转让
5. 投资完成，票据转移给投资机构
6. 到期后获得票据面值，实现收益
```

---

## 💡 使用建议

### 对于金融机构

1. **投资策略**
   - 优先选择银行承兑汇票（风险较低）
   - 关注剩余天数和收益率
   - 分散投资，降低集中度

2. **风险控制**
   - 查看票据风险评分
   - 了解承兑人信用评级
   - 注意票据到期时间分布

3. **投资组合**
   - 不同期限组合
   - 不同类型组合
   - 不同承兑人组合

### 对于票据持票人

1. **提高投资成功率**
   - 确保票据已上链
   - 剩余天数要充足（>30天）
   - 票据状态必须为NORMAL

2. **定价策略**
   - 参考市场利率
   - 考虑承兑人信用
   - 考虑剩余天数

---

## 🧪 测试方法

### 1. 启动应用
```bash
cd /home/llm_rca/fisco/my-bcos-app
mvn spring-boot:run
```

### 2. 访问Swagger UI
```
http://localhost:8080/swagger-ui.html
```

### 3. 测试票据池查询
```
1. 找到"票据池管理"标签
2. 展开 GET /api/bill/pool 接口
3. 点击 "Try it out"
4. 设置筛选条件（可选）
5. 点击 "Execute"
6. 查看返回结果
```

### 4. 运行测试脚本
```bash
chmod +x scripts/test_bill_pool.sh
./scripts/test_bill_pool.sh http://localhost:8080 <token> <institution-id>
```

---

## 📈 业务价值

### 1. 完善票据业务闭环
```
票据开立 → 背书转让 → 贴现 → 融资 → 投资 ✨
```

### 2. 提高资金效率
- 票据持票人快速变现
- 金融机构资金有效利用
- 降低融资成本

### 3. 增加交易活跃度
- 提供统一的投资平台
- 信息透明公开
- 促进票据流转

---

## ⚠️ 注意事项

1. **权限控制**
   - 只有金融机构可以投资
   - 普通企业只能查看
   - 管理员可以查看所有

2. **投资风险**
   - 银行承兑汇票风险较低
   - 商业承兑汇票风险较高
   - 需要评估承兑人信用

3. **流动性风险**
   - 投资后票据被锁定
   - 到期前不能转让
   - 需要考虑资金安排

4. **操作提示**
   - 确认票据信息后再投资
   - 注意投资金额范围
   - 保留好投资记录

---

## 📞 技术支持

如有问题，请参考：
- [完整实现文档](BILL_POOL_IMPLEMENTATION_SUMMARY.md)
- [业务逻辑设计](BILL_POOL_BUSINESS_LOGIC_DESIGN.md)
- [API文档](http://localhost:8080/swagger-ui.html)
- [测试脚本](scripts/test_bill_pool.sh)

---

**快速开始！** 🎉

**票据池管理功能已就绪，立即体验！** ✨
