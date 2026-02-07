# 票据池管理功能 - 完整业务逻辑设计

版本：v1.0
创建时间：2026-02-02
设计状态：待评审

---

## 📋 目录

1. [业务需求分析](#1-业务需求分析)
2. [核心概念定义](#2-核心概念定义)
3. [业务流程设计](#3-业务流程设计)
4. [数据模型设计](#4-数据模型设计)
5. [接口设计](#5-接口设计)
6. [业务规则](#6-业务规则)
7. [异常处理](#7-异常处理)
8. [权限控制](#8-权限控制)
9. [与其他模块集成](#9-与其他模块集成)
10. [业务场景示例](#10-业务场景示例)

---

## 1. 业务需求分析

### 1.1 业务背景

**票据池**是指将所有可投资的票据聚合在一起，供金融机构筛选和投资的平台。

**业务痛点：**
- 金融机构有闲置资金，希望投资优质票据获取收益
- 票据持票人希望快速转让票据获得资金
- 传统票据投资渠道有限，信息不透明
- 缺乏统一的票据投资市场

**业务价值：**
- 为金融机构提供新的投资渠道
- 提高票据流动性和资金使用效率
- 增加平台交易活跃度和用户粘性
- 创造平台收益（交易手续费）

### 1.2 用户角色

| 角色 | 描述 | 权限 |
|------|------|------|
| **金融机构** | 银行、证券公司等持牌金融机构 | 查询票据池、筛选票据、投资票据 |
| **票据持票人** | 持有票据的企业 | 将票据放入票据池、查看投资意向 |
| **平台管理员** | 系统管理员 | 管理票据池规则、查看统计数据、处理异常 |
| **普通企业** | 非金融机构企业 | 仅查看票据池（只读） |

### 1.3 业务目标

**短期目标：**
- 实现票据池基础查询功能
- 实现票据投资功能
- 集成背书转让流程

**长期目标：**
- 智能推荐票据
- 风险评估模型
- 收益率预测
- 票据拍卖机制

---

## 2. 核心概念定义

### 2.1 票据池（Bill Pool）

**定义：** 所有可投资票据的集合。

**特征：**
- 包含所有 NORMAL（正常）状态的票据
- 排除已冻结、已过期、已拒付的票据
- 实时更新票据状态
- 支持多维度筛选和排序

### 2.2 可投资票据（Investable Bill）

**定义：** 符合特定投资条件的票据。

**判断标准：**
- 票据状态 = NORMAL
- 未被冻结
- 未过期
- 已承兑
- 剩余期限 ≥ 30天
- 承兑人信用评级 ≥ 投资机构要求

### 2.3 票据投资（Bill Investment）

**定义：** 金融机构购买票据的行为，本质上是一次背书转让。

**特点：**
- 是票据背书的一种特殊形式
- 从持票人转移到金融机构
- 金融机构支付对价
- 记录投资关系和收益

### 2.4 投资状态（Investment Status）

| 状态 | 说明 | 可转状态 |
|------|------|---------|
| PENDING | 待确认 | CONFIRMED, CANCELLED |
| CONFIRMED | 已确认 | COMPLETED, FAILED |
| COMPLETED | 完成 | - |
| CANCELLED | 已取消 | - |
| FAILED | 失败 | - |

---

## 3. 业务流程设计

### 3.1 查询票据池流程

```
┌─────────────┐
│ 金融机构     │
└──────┬──────┘
       │
       │ 1. 查询票据池（带筛选条件）
       ▼
┌─────────────┐
│ 票据池服务   │
└──────┬──────┘
       │
       │ 2. 验证权限
       ▼
┌─────────────┐
│ 权限验证     │ ← 失败 → 返回403
└──────┬──────┘
       │
       │ 3. 筛选票据
       ▼
┌─────────────┐
│ 票据筛选器   │
│ - 状态过滤   │
│ - 期限过滤   │
│ - 金额过滤   │
│ - 评级过滤   │
└──────┬──────┘
       │
       │ 4. 排序和分页
       ▼
┌─────────────┐
│ 返回票据列表 │
└─────────────┘
```

### 3.2 票据投资流程

```
┌─────────────┐
│ 金融机构     │
└──────┬──────┘
       │
       │ 1. 发起投资请求
       ▼
┌─────────────┐
│ 投资服务     │
└──────┬──────┘
       │
       │ 2. 验证投资条件
       ▼
┌─────────────┐
│ 验证模块     │
│ - 金融机构  │
│ - 票据状态   │
│ - 投资金额   │
│ - 投资资格   │
└──────┬──────┘
       │
       │ 3. 计算投资价格
       ▼
┌─────────────┐
│ 定价模块     │
│ - 票据面值   │
│ - 贴现率     │
│ - 剩余天数   │
└──────┬──────┘
       │
       │ 4. 执行背书转让
       ▼
┌─────────────┐
│ 背书服务     │
│ - 更新持票人 │
│ - 记录背书   │
│ - 上链       │
└──────┬──────┘
       │
       │ 5. 创建投资记录
       ▼
┌─────────────┐
│ 投资记录     │
└──────┬──────┘
       │
       │ 6. 返回投资结果
       ▼
┌─────────────┐
│ 投资完成     │
└─────────────┘
```

### 3.3 投资撤销流程

```
┌─────────────┐
│ 金融机构     │
└──────┬──────┘
       │
       │ 1. 请求撤销投资
       ▼
┌─────────────┐
│ 检查状态     │
│ (只允许      │
│  PENDING)   │
└──────┬──────┘
       │
       │ 2. 执行撤销
       ▼
┌─────────────┐
│ 恢复票据     │
│ 状态         │
└──────┬──────┘
       │
       │ 3. 更新投资记录
       ▼
┌─────────────┐
│ 撤销完成     │
└─────────────┘
```

---

## 4. 数据模型设计

### 4.1 票据投资实体（BillInvestment）

```java
@Entity
@Table(name = "bill_investment")
public class BillInvestment {

    // ========== 主键 ==========
    @Id
    private String id;                          // 投资记录ID

    // ========== 关联票据信息 ==========
    @Column(name = "bill_id", nullable = false)
    private String billId;                      // 票据ID

    @Column(name = "bill_no", nullable = false)
    private String billNo;                      // 票据编号

    @Column(name = "bill_face_value", nullable = false)
    private BigDecimal billFaceValue;          // 票据面值

    // ========== 投资方信息 ==========
    @Column(name = "investor_id", nullable = false)
    private String investorId;                  // 投资机构ID

    @Column(name = "investor_name", nullable = false)
    private String investorName;                // 投资机构名称

    @Column(name = "investor_address")
    private String investorAddress;             // 投资机构区块链地址

    // ========== 原持票人信息 ==========
    @Column(name = "original_holder_id")
    private String originalHolderId;            // 原持票人ID

    @Column(name = "original_holder_name")
    private String originalHolderName;          // 原持票人名称

    @Column(name = "original_holder_address")
    private String originalHolderAddress;       // 原持票人地址

    // ========== 投资详情 ==========
    @Column(name = "invest_amount", nullable = false)
    private BigDecimal investAmount;            // 投资金额（实际支付金额）

    @Column(name = "invest_rate", nullable = false)
    private BigDecimal investRate;              // 投资利率（贴现率）

    @Column(name = "expected_return")
    private BigDecimal expectedReturn;          // 预期收益

    @Column(name = "investment_days")
    private Integer investmentDays;             // 投资天数（票据剩余天数）

    // ========== 投资状态 ==========
    @Column(name = "status", nullable = false)
    private String status;                      // 投资状态

    @Column(name = "investment_date")
    private LocalDateTime investmentDate;        // 投资日期

    @Column(name = "confirmation_date")
    private LocalDateTime confirmationDate;      // 确认日期

    @Column(name = "completion_date")
    private LocalDateTime completionDate;        // 完成日期

    @Column(name = "cancellation_date")
    private LocalDateTime cancellationDate;      // 撤销日期

    // ========== 收益结算 ==========
    @Column(name = "maturity_amount")
    private BigDecimal maturityAmount;          // 到期金额（面值）

    @Column(name = "actual_return")
    private BigDecimal actualReturn;            // 实际收益

    @Column(name = "settlement_date")
    private LocalDateTime settlementDate;        // 结算日期

    // ========== 备注信息 ==========
    @Column(name = "investment_notes", columnDefinition = "TEXT")
    private String investmentNotes;             // 投资备注

    @Column(name = "rejection_reason")
    private String rejectionReason;             // 拒绝原因（如果被拒绝）

    // ========== 区块链信息 ==========
    @Column(name = "endorsement_id")
    private String endorsementId;               // 关联的背书ID

    @Column(name = "tx_hash")
    private String txHash;                      // 区块链交易哈希

    @Column(name = "blockchain_time")
    private LocalDateTime blockchainTime;        // 上链时间

    // ========== 审计字段 ==========
    @Column(name = "created_by")
    private String createdBy;                   // 创建人

    @Column(name = "updated_by")
    private String updatedBy;                   // 更新人

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;            // 创建时间

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;            // 更新时间

    // ========== 生命周期回调 ==========
    @PrePersist
    protected void onCreate() {
        if (id == null || id.isEmpty()) {
            id = UUID.randomUUID().toString();
        }
        if (status == null) {
            status = "PENDING";
        }
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        investmentDate = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

### 4.2 票据池视图（BillPoolView）

**说明：** 不需要实体，使用查询组装DTO。

```java
@Data
public class BillPoolView {
    // 票据基础信息
    private String billId;
    private String billNo;
    private String billType;
    private BigDecimal faceValue;

    // 时间信息
    private Integer remainingDays;      // 剩余天数
    private LocalDateTime maturityDate;  // 到期日期

    // 承兑人信息
    private String acceptorName;
    private String acceptorRating;      // 承兑人评级
    private String acceptorType;        // 承兑人类型（银行/企业）

    // 当前持票人
    private String currentHolderName;
    private String currentHolderType;   // 企业/金融机构

    // 投资指标
    private BigDecimal expectedReturn;  // 预期收益率
    private BigDecimal riskScore;       // 风险评分
    private Boolean canInvest;          // 是否可投资

    // 投资建议
    private String investmentAdvice;    // 投资建议（推荐/谨慎/不推荐）

    // 统计信息
    private Integer viewCount;          // 浏览次数
    private Integer inquiryCount;       // 询价次数
}
```

### 4.3 筛选条件DTO（BillPoolFilter）

```java
@Data
public class BillPoolFilter {
    // 基础筛选
    private String billType;            // 票据类型
    private BigDecimal minAmount;       // 最小面值
    private BigDecimal maxAmount;       // 最大面值

    // 期限筛选
    private Integer minRemainingDays;  // 最小剩余天数
    private Integer maxRemainingDays;  // 最大剩余天数

    // 承兑人筛选
    private String acceptorType;       // 承兑人类型
    private String minRating;          // 最低评级

    // 风险筛选
    private String riskLevel;          // 风险等级（LOW/MEDIUM/HIGH）
    private Integer maxRiskScore;      // 最大风险评分

    // 收益筛选
    private BigDecimal minReturnRate;   // 最低收益率

    // 分页
    private Integer page = 0;
    private Integer size = 20;
    private String sortBy = "remainingDays";  // 排序字段
    private String sortOrder = "ASC";         // 排序方向
}
```

### 4.4 投资请求DTO（BillInvestRequest）

```java
@Data
public class BillInvestRequest {
    @NotBlank(message = "票据ID不能为空")
    private String billId;

    @NotNull(message = "投资金额不能为空")
    @DecimalMin(value = "0.01", message = "投资金额必须大于0")
    private BigDecimal investAmount;

    @NotNull(message = "投资利率不能为空")
    @DecimalMin(value = "0.01", message = "投资利率必须大于0")
    private BigDecimal investRate;

    @Future(message = "投资日期必须是未来时间")
    private LocalDateTime investDate;

    private String investmentNotes;    // 投资备注
}
```

### 4.5 投资响应DTO（BillInvestResponse）

```java
@Data
public class BillInvestResponse {
    // 投资记录信息
    private String investmentId;
    private String billId;
    private String billNo;

    // 投资详情
    private BigDecimal investAmount;
    private BigDecimal investRate;
    private BigDecimal expectedReturn;
    private Integer investmentDays;

    // 状态信息
    private String status;
    private LocalDateTime investmentDate;

    // 转让信息
    private String originalHolderName;
    private String investorName;

    // 区块链信息
    private String endorsementId;
    private String txHash;
}
```

---

## 5. 接口设计

### 5.1 查询票据池

#### 接口定义
```java
GET /api/bill/pool
```

#### 请求参数
```java
@GetMapping("/pool")
public Result<Page<BillPoolView>> getBillPool(
    @RequestParam(required = false) String billType,
    @RequestParam(required = false) BigDecimal minAmount,
    @RequestParam(required = false) BigDecimal maxAmount,
    @RequestParam(required = false) Integer minRemainingDays,
    @RequestParam(required = false) Integer maxRemainingDays,
    @RequestParam(required = false) String acceptorType,
    @RequestParam(required = false) String minRating,
    @RequestParam(required = false) String riskLevel,
    @RequestParam(required = false) BigDecimal minReturnRate,
    @RequestParam(defaultValue = "0") Integer page,
    @RequestParam(defaultValue = "20") Integer size,
    @RequestParam(defaultValue = "remainingDays") String sortBy,
    @RequestParam(defaultValue = "ASC") String sortOrder
)
```

#### 业务逻辑

```java
@Transactional(readOnly = true)
public Page<BillPoolView> getBillPool(BillPoolFilter filter) {

    // 1. 构建查询条件
    Specification<Bill> spec = (root, query, cb) -> {
        List<Predicate> predicates = new ArrayList<>();

        // 基础条件：只查询可投资的票据
        predicates.add(cb.equal(root.get("billStatus"), Bill.BillStatus.NORMAL));
        predicates.add(cb.equal(root.get("blockchainStatus"), "ONCHAIN"));

        // 排除条件
        predicates.add(cb.notEqual(root.get("frozen"), true));
        predicates.add(cb.greaterThan(root.get("dueDate"), LocalDateTime.now()));

        // 用户自定义筛选条件
        if (filter.getBillType() != null) {
            predicates.add(cb.equal(root.get("billType"), filter.getBillType()));
        }

        if (filter.getMinAmount() != null) {
            predicates.add(cb.greaterThanOrEqualTo(
                root.get("faceValue"), filter.getMinAmount()));
        }

        if (filter.getMaxAmount() != null) {
            predicates.add(cb.lessThanOrEqualTo(
                root.get("faceValue"), filter.getMaxAmount()));
        }

        // ... 更多筛选条件

        return cb.and(predicates.toArray(new Predicate[0]));
    };

    // 2. 执行查询
    Page<Bill> bills = billRepository.findAll(spec,
        PageRequest.of(filter.getPage(), filter.getSize()));

    // 3. 组装视图对象
    List<BillPoolView> views = bills.stream()
        .map(this::buildBillPoolView)
        .collect(Collectors.toList());

    // 4. 计算投资指标
    views.forEach(this::calculateInvestmentMetrics);

    return new PageImpl<>(views,
        PageRequest.of(filter.getPage(), filter.getSize()),
        bills.getTotalElements());
}

private BillPoolView buildBillPoolView(Bill bill) {
    BillPoolView view = new BillPoolView();
    view.setBillId(bill.getBillId());
    view.setBillNo(bill.getBillNo());
    view.setBillType(bill.getBillType());
    view.setFaceValue(bill.getFaceValue());

    // 计算剩余天数
    long remainingDays = ChronoUnit.DAYS.between(
        LocalDateTime.now(), bill.getDueDate());
    view.setRemainingDays((int) remainingDays);
    view.setMaturityDate(bill.getDueDate());

    // 承兑人信息
    view.setAcceptorName(bill.getDraweeName());
    // ... 设置其他字段

    return view;
}

private void calculateInvestmentMetrics(BillPoolView view) {
    // 计算预期收益率
    BigDecimal returnRate = calculateReturnRate(view);
    view.setExpectedReturn(returnRate);

    // 计算风险评分
    Integer riskScore = calculateRiskScore(view);
    view.setRiskScore(riskScore);

    // 判断是否可投资
    view.setCanInvest(determineInvestable(view));

    // 生成投资建议
    view.setInvestmentAdvice(generateInvestmentAdvice(view));
}
```

#### 响应示例
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
        "currentHolderType": "ENTERPRISE",
        "expectedReturn": 5.5,
        "riskScore": 15,
        "canInvest": true,
        "investmentAdvice": "推荐"
      }
    ],
    "pageable": {
      "page": 0,
      "size": 20
    },
    "totalPages": 8,
    "totalElements": 150
  }
}
```

### 5.2 查询可投资票据

#### 接口定义
```java
GET /api/bill/pool/available
```

#### 业务逻辑

```java
@Transactional(readOnly = true)
public List<BillPoolView> getAvailableBills(String institutionId,
                                            BillPoolFilter filter) {

    // 1. 查询金融机构信息
    Enterprise institution = enterpriseRepository.findById(institutionId)
        .orElseThrow(() -> new BusinessException("金融机构不存在"));

    // 2. 根据金融机构风险偏好调整筛选条件
    if (institution.getRiskLevel() != null) {
        filter.setMaxRiskScore(institution.getMaxAcceptableRisk());
    }

    // 3. 查询符合条件的票据
    List<Bill> bills = billRepository.findAvailableBills(filter);

    // 4. 过滤不符合机构要求的票据
    List<BillPoolView> views = bills.stream()
        .filter(bill -> isInstitutionAllowed(institution, bill))
        .map(this::buildBillPoolView)
        .collect(Collectors.toList());

    // 5. 按收益率排序
    views.sort((v1, v2) ->
        v2.getExpectedReturn().compareTo(v1.getExpectedReturn()));

    return views;
}
```

### 5.3 票据投资

#### 接口定义
```java
POST /api/bill/pool/{billId}/invest
```

#### 完整业务逻辑

```java
@Transactional(rollbackFor = Exception.class)
public BillInvestResponse investBill(String billId,
                                     BillInvestRequest request,
                                     String investorAddress) {

    log.info("========== 票据投资开始 ==========");
    log.info("票据ID: {}, 投资机构: {}, 投资金额: {}",
             billId, investorAddress, request.getInvestAmount());

    // ========== 步骤1: 验证票据 ==========
    log.debug("步骤1: 验证票据");
    Bill bill = billRepository.findById(billId)
        .orElseThrow(() -> new BusinessException("票据不存在"));

    // 验证票据状态
    if (bill.getBillStatus() != Bill.BillStatus.NORMAL) {
        throw new BusinessException("票据状态不正常，当前状态: " +
            bill.getBillStatus());
    }

    // 验证票据是否冻结
    if (Boolean.TRUE.equals(bill.getFrozen())) {
        throw new BusinessException("票据已冻结，无法投资");
    }

    // 验证票据是否已过期
    if (bill.getDueDate().isBefore(LocalDateTime.now())) {
        throw new BusinessException("票据已过期，无法投资");
    }

    // 验证票据是否已上链
    if (!"ONCHAIN".equals(bill.getBlockchainStatus())) {
        throw new BusinessException("票据未上链，无法投资");
    }

    // ========== 步骤2: 验证投资机构 ==========
    log.debug("步骤2: 验证投资机构");
    Enterprise investor = enterpriseRepository.findByAddress(investorAddress)
        .orElseThrow(() -> new BusinessException("投资机构不存在"));

    // 验证是否为金融机构
    if (investor.getEnterpriseType() == null ||
        !investor.getEnterpriseType().toString().startsWith("FINANCIAL")) {
        throw new BusinessException("只有金融机构才能投资票据");
    }

    // 验证金融机构状态
    if ("FROZEN".equals(investor.getStatus()) ||
        "CANCELLED".equals(investor.getStatus())) {
        throw new BusinessException("金融机构状态异常，无法投资");
    }

    // ========== 步骤3: 验证投资金额 ==========
    log.debug("步骤3: 验证投资金额");
    BigDecimal investAmount = request.getInvestAmount();
    BigDecimal faceValue = bill.getFaceValue();

    if (investAmount.compareTo(faceValue) > 0) {
        throw new BusinessException("投资金额不能超过票据面值");
    }

    if (investAmount.compareTo(faceValue.multiply(new BigDecimal("0.1"))) < 0) {
        throw new BusinessException("投资金额不能低于票据面值的10%");
    }

    // ========== 步骤4: 验证当前持票人 ==========
    log.debug("步骤4: 验证当前持票人");
    String currentHolderId = bill.getCurrentHolderId();
    String originalHolderId = currentHolderId;

    if (currentHolderId.equals(investor.getEnterpriseId())) {
        throw new BusinessException("不能投资自己持有的票据");
    }

    // ========== 步骤5: 计算投资价格 ==========
    log.debug("步骤5: 计算投资价格");
    int remainingDays = (int) ChronoUnit.DAYS.between(
        LocalDateTime.now(), bill.getDueDate());

    // 贴现计算：实际支付金额 = 面值 - (面值 × 利率 × 天数 / 360)
    BigDecimal discount = faceValue
        .multiply(request.getInvestRate())
        .multiply(BigDecimal.valueOf(remainingDays))
        .divide(BigDecimal.valueOf(36000), 2, RoundingMode.HALF_UP);

    BigDecimal actualPayAmount = faceValue.subtract(discount);

    log.info("贴现计算: 面值={}, 利率={}%, 天数={}, 贴现={}, 实付={}",
             faceValue, request.getInvestRate(), remainingDays,
             discount, actualPayAmount);

    // ========== 步骤6: 检查是否有未完成的投资 ==========
    log.debug("步骤6: 检查未完成投资");
    List<BillInvestment> pendingInvestments =
        investmentRepository.findByBillIdAndStatus(billId, "PENDING");

    if (!pendingInvestments.isEmpty()) {
        throw new BusinessException("票据有未完成的投资，请稍后再试");
    }

    // ========== 步骤7: 创建投资记录 ==========
    log.debug("步骤7: 创建投资记录");
    BillInvestment investment = new BillInvestment();
    investment.setBillId(billId);
    investment.setBillNo(bill.getBillNo());
    investment.setBillFaceValue(faceValue);
    investment.setInvestorId(investor.getEnterpriseId());
    investment.setInvestorName(investor.getEnterpriseName());
    investment.setInvestorAddress(investorAddress);
    investment.setOriginalHolderId(originalHolderId);
    investment.setOriginalHolderName(bill.getCurrentHolderName());
    investment.setOriginalHolderAddress(bill.getCurrentHolderAddress());
    investment.setInvestAmount(investAmount);
    investment.setInvestRate(request.getInvestRate());
    investment.setExpectedReturn(faceValue.subtract(investAmount));
    investment.setInvestmentDays(remainingDays);
    investment.setStatus("PENDING");
    investment.setMaturityAmount(faceValue);
    investment.setInvestmentNotes(request.getInvestmentNotes());
    investment.setCreatedBy(investorAddress);

    investmentRepository.save(investment);

    // ========== 步骤8: 执行背书转让 ==========
    log.debug("步骤8: 执行背书转让");

    // 创建背书请求
    EndorseBillRequest endorseRequest = new EndorseBillRequest();
    endorseRequest.setEndorseeAddress(investorAddress);
    endorseRequest.setEndorseeName(investor.getEnterpriseName());
    endorseRequest.setEndorsementType(Endorsement.EndorsementType.DISCOUNT);
    endorseRequest.setEndorseNotes("票据池投资");
    endorseRequest.setEndorsePrice(investAmount);

    // 执行背书
    EndorsementResponse endorseResponse = endorseBill(billId, endorseRequest,
        bill.getCurrentHolderAddress());

    // 更新投资记录
    investment.setEndorsementId(endorseResponse.getId());
    investment.setTxHash(endorseResponse.getTxHash());
    investment.setStatus("CONFIRMED");
    investment.setConfirmationDate(LocalDateTime.now());
    investment.setCompletionDate(LocalDateTime.now());
    investment.setBlockchainTime(endorseResponse.getBlockchainTime());
    investment.setUpdatedBy(investorAddress);

    investmentRepository.save(investment);

    // ========== 步骤9: 发送通知 ==========
    log.debug("步骤9: 发送通知");
    // TODO: 发送通知给原持票人和投资机构

    // ========== 步骤10: 构建响应 ==========
    log.debug("步骤10: 构建响应");
    BillInvestResponse response = new BillInvestResponse();
    response.setInvestmentId(investment.getId());
    response.setBillId(billId);
    response.setBillNo(bill.getBillNo());
    response.setInvestAmount(investAmount);
    response.setInvestRate(request.getInvestRate());
    response.setExpectedReturn(investment.getExpectedReturn());
    response.setInvestmentDays(remainingDays);
    response.setStatus("CONFIRMED");
    response.setInvestmentDate(investment.getInvestmentDate());
    response.setOriginalHolderName(bill.getCurrentHolderName());
    response.setInvestorName(investor.getEnterpriseName());
    response.setEndorsementId(endorseResponse.getId());
    response.setTxHash(endorseResponse.getTxHash());

    log.info("✓ 票据投资完成: investmentId={}, 票据={}, 投资金额={}",
             investment.getId(), billNo, investAmount);
    log.info("========== 票据投资结束 ==========");

    return response;
}
```

---

## 6. 业务规则

### 6.1 票据池准入规则

**必须同时满足：**
1. 票据状态 = NORMAL（正常）
2. 区块链状态 = ONCHAIN（已上链）
3. 未被冻结（frozen != true）
4. 未过期（dueDate > now）
5. 已承兑（acceptanceDate != null）
6. 剩余天数 ≥ 30天

**任一条件不满足则不在票据池中**

### 6.2 投资资格规则

**金融机构：**
1. 企业类型必须为 FINANCIAL 开头
2. 企业状态必须为 ACTIVE
3. 不能投资自己持有的票据
4. 符合监管要求（资本充足率等）

**票据：**
1. 必须在票据池中
2. 当前没有PENDING状态的投资
3. 符合机构风险偏好

### 6.3 投资金额规则

```java
最小投资金额 = 票据面值 × 10%
最大投资金额 = 票据面值

实际支付金额 = 面值 - 贴现
贴现 = 面值 × 投资利率 × 剩余天数 / 36000
```

**示例：**
```
面值：1,000,000元
投资利率：5.5%
剩余天数：90天

贴现 = 1,000,000 × 5.5 × 90 / 36000 = 13,750元
实付 = 1,000,000 - 13,750 = 986,250元
收益 = 1,000,000 - 986,250 = 13,750元
收益率 = 13,750 / 986,250 × 365 / 90 × 100% = 5.66%
```

### 6.4 投资状态转换规则

```
PENDING → CONFIRMED → COMPLETED
  ↓          ↓
CANCELLED  FAILED
```

**转换条件：**
- PENDING → CONFIRMED: 背书成功
- PENDING → CANCELLED: 用户主动撤销或超时
- CONFIRMED → COMPLETED: 区块链确认
- CONFIRMED → FAILED: 区块链失败或票据状态变更

---

## 7. 异常处理

### 7.1 业务异常

| 异常码 | 异常信息 | 处理方式 |
|--------|---------|---------|
| BILL_NOT_FOUND | 票据不存在 | 返回404 |
| BILL_NOT_IN_POOL | 票据不在票据池中 | 返回400，说明原因 |
| BILL_ALREADY_FROZEN | 票据已冻结 | 返回400 |
| BILL_EXPIRED | 票据已过期 | 返回400 |
| BILL_NOT_ONCHAIN | 票据未上链 | 返回400 |
| INVESTOR_NOT_FOUND | 投资机构不存在 | 返回404 |
| NOT_FINANCIAL_INSTITUTION | 非金融机构 | 返回403 |
| INVESTOR_STATUS_ABNORMAL | 机构状态异常 | 返回403 |
| INVALID_INVEST_AMOUNT | 投资金额无效 | 返回400 |
| INVEST_SELF_OWNED | 不能投资自己的票据 | 返回400 |
| BILL_HAS_PENDING_INVESTMENT | 有未完成的投资 | 返回409 |

### 7.2 系统异常

| 异常 | 处理方式 |
|------|---------|
| 数据库连接失败 | 返回500，记录日志 |
| 区块链调用失败 | 返回500，记录日志，重试3次 |
| 网络超时 | 返回504，记录日志 |

### 7.3 异常处理流程

```java
try {
    // 业务逻辑
} catch (BusinessException e) {
    // 业务异常，返回具体错误信息
    log.warn("业务异常: {}", e.getMessage());
    return Result.error(e.getMessage());
} catch (Exception e) {
    // 系统异常，返回通用错误信息
    log.error("系统异常: {}", e.getMessage(), e);
    return Result.error("系统错误，请稍后重试");
}
```

---

## 8. 权限控制

### 8.1 角色权限矩阵

| 功能 | 金融机构 | 普通企业 | 平台管理员 | 游客 |
|------|---------|---------|-----------|------|
| 查询票据池 | ✅ 只读 | ✅ 只读 | ✅ 读写 | ❌ |
| 查询可投资票据 | ✅ | ❌ | ✅ | ❌ |
| 投资票据 | ✅ | ❌ | ✅ | ❌ |
| 撤销投资 | ✅ 自己的 | ❌ | ✅ 全部 | ❌ |
| 查看投资记录 | ✅ 自己的 | ✅ 自己的 | ✅ 全部 | ❌ |
| 管理票据池规则 | ❌ | ❌ | ✅ | ❌ |

### 8.2 权限验证逻辑

```java
// 在Controller层验证
@GetMapping("/pool")
public Result<Page<BillPoolView>> getBillPool(
    @RequestParam(required = false) String institutionId,
    Authentication authentication) {

    String userAddress = authentication.getName();
    Enterprise user = enterpriseRepository.findByAddress(userAddress)
        .orElseThrow(() -> new BusinessException("用户不存在"));

    // 检查是否有查询权限
    if (!hasPermission(user, "BILL_POOL_VIEW")) {
        return Result.error("无权限查询票据池");
    }

    // 如果是金融机构查询可投资票据，需要额外验证
    if (institutionId != null) {
        if (!user.getEnterpriseId().equals(institutionId)) {
            return Result.error("只能查询自己的可投资票据");
        }
    }

    Page<BillPoolView> result = billService.getBillPool(filter);
    return Result.success(result);
}

@PostMapping("/pool/{billId}/invest")
public Result<BillInvestResponse> investBill(
    @PathVariable String billId,
    @RequestBody @Valid BillInvestRequest request,
    Authentication authentication) {

    String userAddress = authentication.getName();
    Enterprise user = enterpriseRepository.findByAddress(userAddress)
        .orElseThrow(() -> new BusinessException("用户不存在"));

    // 验证是否为金融机构
    if (!isFinancialInstitution(user)) {
        return Result.error("只有金融机构才能投资票据");
    }

    BillInvestResponse result = billService.investBill(
        billId, request, userAddress);

    return Result.success(result);
}

private boolean isFinancialInstitution(Enterprise enterprise) {
    return enterprise.getEnterpriseType() != null &&
           enterprise.getEnterpriseType().toString().startsWith("FINANCIAL");
}

private boolean hasPermission(Enterprise user, String permission) {
    // 实现权限检查逻辑
    return true; // 简化示例
}
```

---

## 9. 与其他模块集成

### 9.1 与背书模块集成

**集成方式：** 票据投资本质上是一次背书转让。

```java
// 在investBill方法中调用背书服务
EndorseBillRequest endorseRequest = new EndorseBillRequest();
endorseRequest.setEndorseeAddress(investorAddress);
endorseRequest.setEndorsementType(Endorsement.EndorsementType.DISCOUNT);
// ... 设置其他字段

EndorsementResponse endorseResponse =
    billService.endorseBill(billId, endorseRequest, holderAddress);

// 关联背书记录
investment.setEndorsementId(endorseResponse.getId());
investment.setTxHash(endorseResponse.getTxHash());
```

### 9.2 与区块链模块集成

**集成方式：** 背书操作上链。

```java
// 在背书服务中处理上链
String txHash = contractService.endorseBill(
    billId,
    endorserAddress,
    endorseeAddress,
    endorsementType.toString()
);

// 记录交易哈希
endorsement.setTxHash(txHash);
investment.setTxHash(txHash);
```

### 9.3 与企业模块集成

**集成方式：** 验证金融机构资质。

```java
Enterprise institution = enterpriseRepository.findById(institutionId)
    .orElseThrow(() -> new BusinessException("金融机构不存在"));

// 验证机构类型
if (!institution.getEnterpriseType().toString().startsWith("FINANCIAL")) {
    throw new BusinessException("非金融机构");
}

// 验证机构状态
if (!"ACTIVE".equals(institution.getStatus())) {
    throw new BusinessException("机构状态异常");
}

// 检查风险等级
if (filter.getMaxRiskScore() != null) {
    Integer riskScore = riskService.calculateRisk(institution);
    if (riskScore > filter.getMaxRiskScore()) {
        // 过滤高风险票据
    }
}
```

---

## 10. 业务场景示例

### 10.1 场景1：金融机构查询票据池

**场景描述：** 某银行希望查看所有可投资的银行承兑汇票。

**操作流程：**
```
1. 银行登录系统
2. 进入"票据池"页面
3. 设置筛选条件：
   - 票据类型：银行承兑汇票
   - 面值范围：50万-200万
   - 剩余天数：30-180天
   - 承兑人评级：AA及以上
4. 点击"查询"
5. 系统返回符合条件的票据列表
6. 银行查看票据详情，评估投资价值
```

**系统操作：**
```java
GET /api/bill/pool?billType=BANK_ACCEPTANCE_BILL
                  &minAmount=500000
                  &maxAmount=2000000
                  &minRemainingDays=30
                  &maxRemainingDays=180
                  &minRating=AA
                  &page=0
                  &size=20
```

**返回结果：**
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
        "maturityDate": "2026-05-03",
        "acceptorName": "中国工商银行",
        "acceptorRating": "AAA",
        "currentHolderName": "供应商A",
        "expectedReturn": 5.5,
        "riskScore": 15,
        "canInvest": true,
        "investmentAdvice": "推荐"
      }
    ],
    "totalElements": 50
  }
}
```

### 10.2 场景2：票据投资

**场景描述：** 银行决定投资一张票据。

**操作流程：**
```
1. 银行选择票据 bill-001
2. 查看票据详情
3. 输入投资参数：
   - 投资金额：100万
   - 投资利率：5.5%
4. 系统计算：
   - 剩余天数：90天
   - 贴现：13,750元
   - 实付：986,250元
   - 预期收益：13,750元
5. 银行确认投资
6. 系统执行背书转让
7. 投资完成
```

**系统操作：**
```java
POST /api/bill/pool/bill-001/invest
Header: X-User-Address: 0x... (银行地址)

Body:
{
  "investAmount": 1000000.00,
  "investRate": 5.5,
  "investDate": "2026-02-03T10:30:00",
  "investmentNotes": "看好该票据"
}
```

**返回结果：**
```json
{
  "code": 200,
  "message": "投资成功",
  "data": {
    "investmentId": "invest-001",
    "billId": "bill-001",
    "billNo": "BILL20260101001",
    "investAmount": 1000000.00,
    "investRate": 5.5,
    "expectedReturn": 13750.00,
    "investmentDays": 90,
    "status": "CONFIRMED",
    "investmentDate": "2026-02-03T10:30:00",
    "originalHolderName": "供应商A",
    "investorName": "XX银行",
    "endorsementId": "endorse-001",
    "txHash": "0xabcdef..."
  }
}
```

### 10.3 场景3：票据到期结算

**场景描述：** 投资的票据到期，银行获得本金和收益。

**操作流程：**
```
1. 票据到期日（2026-05-03）
2. 系统自动检测到期票据
3. 向承兑人发起收款请求
4. 承兑人支付票据面值（100万）
5. 系统记录：
   - 收到金额：1,000,000元
   - 投资成本：986,250元
   - 投资收益：13,750元
   - 收益率：5.66%（年化）
6. 更新投资记录状态为COMPLETED
7. 生成收益报表
```

---

## 11. 后续扩展功能

### 11.1 智能推荐
- 基于历史投资行为推荐票据
- 基于风险偏好推荐票据
- 票据组合优化建议

### 11.2 收益预测
- 收益率曲线图
- 历史收益分析
- 未来收益预测

### 11.3 风险评估
- 票据风险评分
- 承兑人信用评估
- 行业风险分析

### 11.4 票据拍卖
- 票据竞价机制
- 拍卖流程管理
- 竞价记录查询

---

## 12. 技术实现要点

### 12.1 性能优化

**查询优化：**
- 创建复合索引：(bill_status, frozen, due_date)
- 使用分页查询
- 缓存热点数据
- 使用视图对象（View Object）

**计算优化：**
- 预计算收益率
- 异步更新风险评分
- 批量处理投资记录

### 12.2 数据一致性

**事务管理：**
```java
@Transactional(rollbackFor = Exception.class)
public BillInvestResponse investBill(...) {
    // 所有数据库操作在同一事务中
    // 保证数据一致性
}
```

**并发控制：**
```java
// 使用乐观锁
@Version
private Long version;

// 或使用悲观锁
Bill bill = billRepository.findByIdWithLock(billId);
```

### 12.3 可扩展性

**设计原则：**
- 接口与实现分离
- 业务逻辑与数据访问分离
- 支持多种投资方式
- 易于添加新的筛选条件

---

## 13. 测试要点

### 13.1 功能测试

**查询票据池：**
- ✅ 正常查询
- ✅ 带筛选条件查询
- ✅ 分页查询
- ✅ 排序查询
- ✅ 空结果查询

**票据投资：**
- ✅ 正常投资
- ✅ 投资金额边界值
- ✅ 重复投资
- ✅ 投资自己的票据
- ✅ 投资冻结票据

**权限控制：**
- ✅ 金融机构投资
- ❌ 普通企业投资
- ❌ 未登录访问

### 13.2 性能测试

**查询性能：**
- 票据池1000条记录，查询时间 < 1秒
- 带筛选条件查询，查询时间 < 2秒

**并发测试：**
- 支持100个并发查询
- 支持10个并发投资

### 13.3 集成测试

**与背书模块集成：**
- 投资后背书记录正确
- 票据持票人正确更新

**与区块链集成：**
- 交易哈希正确记录
- 上链时间正确记录

---

## 14. 总结

### 14.1 核心价值

1. **完善业务闭环**
   - 票据开立 → 背书转让 → 贴现/融资 → **票据投资** ✨

2. **提升平台价值**
   - 为金融机构提供投资渠道
   - 提高票据流动性
   - 增加交易活跃度

3. **技术实现简单**
   - 复用背书转让逻辑
   - 复用现有计算逻辑
   - 预计工作量：2天

### 14.2 实施计划

**Day 1: 基础功能**
- 创建实体和Repository
- 实现查询票据池接口
- 实现筛选逻辑
- 单元测试

**Day 2: 投资功能**
- 实现票据投资接口
- 集成背书转让流程
- 集成区块链上链
- 集成测试
- 文档编写

### 14.3 风险评估

**技术风险：** 🟢 低
- 功能简单，技术成熟
- 可复用现有代码

**业务风险：** 🟡 中
- 需要明确监管要求
- 需要完善的权限控制

**时间风险：** 🟢 低
- 预计2天完成
- 有充足缓冲时间

---

## 15. 附录

### 15.1 数据库表结构

**bill_investment 表：**
```sql
CREATE TABLE bill_investment (
    id VARCHAR(36) PRIMARY KEY,
    bill_id VARCHAR(36) NOT NULL,
    bill_no VARCHAR(50) NOT NULL,
    bill_face_value DECIMAL(20,2) NOT NULL,
    investor_id VARCHAR(36) NOT NULL,
    investor_name VARCHAR(200) NOT NULL,
    investor_address VARCHAR(42),
    original_holder_id VARCHAR(36),
    original_holder_name VARCHAR(200),
    original_holder_address VARCHAR(42),
    invest_amount DECIMAL(20,2) NOT NULL,
    invest_rate DECIMAL(10,4) NOT NULL,
    expected_return DECIMAL(20,2),
    investment_days INT,
    status VARCHAR(50) NOT NULL,
    investment_date DATETIME(6),
    confirmation_date DATETIME(6),
    completion_date DATETIME(6),
    cancellation_date DATETIME(6),
    maturity_amount DECIMAL(20,2),
    actual_return DECIMAL(20,2),
    settlement_date DATETIME(6),
    investment_notes TEXT,
    rejection_reason VARCHAR(500),
    endorsement_id VARCHAR(36),
    tx_hash VARCHAR(100),
    blockchain_time DATETIME(6),
    created_by VARCHAR(36),
    updated_by VARCHAR(36),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    INDEX idx_bill_id (bill_id),
    INDEX idx_investor_id (investor_id),
    INDEX idx_status (status),
    INDEX idx_investment_date (investment_date),

    CONSTRAINT fk_invest_bill FOREIGN KEY (bill_id) REFERENCES bill(bill_id),
    CONSTRAINT fk_invest_investor FOREIGN KEY (investor_id) REFERENCES enterprise(enterprise_id),

    UNIQUE KEY uk_bill_pending (bill_id, status)
);
```

**唯一约束说明：**
- `uk_bill_pending`: 确保同一票据同时只有一个PENDING状态的投资

### 15.2 API文档

完整的Swagger API文档将在实现后生成。

### 15.3 配置文件

```yaml
# application.yml
bill:
  pool:
    enabled: true
    min-remaining-days: 30
    min-invest-ratio: 0.1  # 最小投资比例（面值的10%）
    max-pending-time: 3600  # 最大pending时间（秒）
```

---

**文档版本：** v1.0
**创建时间：** 2026-02-02
**状态：** 待评审
**下一步：** 评审通过后开始实现
