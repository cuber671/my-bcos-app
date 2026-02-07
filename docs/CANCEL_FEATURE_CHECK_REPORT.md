# 仓单作废功能检查报告

生成时间：2026-02-02
检查范围：角色权限验证、Swagger注解完整性

---

## ✅ 检查通过的项目

### 1. DTO类的Swagger注解
- ✅ `CancelApplicationRequest` - 注解完整
- ✅ `CancelApplicationResponse` - 注解完整
- ✅ `CancelApprovalRequest` - 注解完整
- ✅ `CancelApprovalResponse` - 注解完整

### 2. 提交作废申请权限验证
```java
// submitCancelApplication() - ✅ 完整的4层权限验证
1. ✅ 身份验证：检查是否为UserAuthentication
2. ✅ 持有人权限：基于区块链地址验证
3. ✅ 企业角色验证：只有货主企业可以申请
4. ✅ 业务规则验证：状态、质押、冻结等检查
```

### 3. 审核作废申请权限验证
```java
// approveCancelApplication() - ✅ 正确的权限验证
1. ✅ 身份验证：检查是否为UserAuthentication
2. ✅ 系统管理员：可以审核所有作废申请
3. ✅ 仓储企业：可以审核关联的仓单
```

---

## ⚠️ 发现的问题

### 问题1：查询接口缺少权限验证

**严重程度：** 🔴 高

**1.1 查询待审核的作废申请**
```java
@GetMapping("/cancel/pending")
public ResponseEntity<List<ReceiptCancelApplication>> getPendingCancelApplications()
```
- ❌ **问题：** 任何用户都可以查询所有待审核的作废申请
- ❌ **风险：** 信息泄露，用户可以看到不相关的作废申请
- ✅ **建议：**
  - 只允许管理员查询所有待审核申请
  - 仓储企业只能查询自己仓单的待审核申请

**1.2 查询已作废的仓单**
```java
@GetMapping("/cancelled")
public ResponseEntity<List<ElectronicWarehouseReceiptResponse>> getCancelledReceipts(
    @RequestParam(required = false) String enterpriseId)
```
- ❌ **问题：** 任何用户都可以查询所有已作废的仓单
- ❌ **风险：** 数据泄露，跨企业数据访问
- ✅ **建议：**
  - 如果提供了enterpriseId，验证当前用户是否有权查询该企业
  - 如果未提供，只返回当前用户企业的已作废仓单

---

### 问题2：Swagger注解缺少权限说明

**严重程度：** 🟡 中

**2.1 查询接口缺少权限说明**
```java
@ApiOperation(value = "查询待审核的作废申请", notes = "查询所有待审核的作废申请列表。" +
        "管理员和仓储企业使用，用于查看需要审核的作废申请。")
```
- ⚠️ **问题：** notes中提到了"管理员和仓储企业使用"，但没有明确说明权限要求
- ✅ **建议：** 添加明确的权限说明
  - "权限要求：系统管理员或仓储企业"
  - "管理员可查询所有，仓储企业只能查询自己仓单的申请"

**2.2 提交申请接口缺少权限说明**
```java
@ApiOperation(value = "提交仓单作废申请", notes = "货主企业提交仓单作废申请。")
```
- ⚠️ **问题：** 没有明确说明只有当前持单人可以申请
- ✅ **建议：** 添加"权限要求：当前持单人（货主企业）"

---

## 📋 修复方案

### 修复1：增强查询接口的权限验证

#### 1.1 修改 Service 层方法签名

**修改前：**
```java
public List<ReceiptCancelApplication> getPendingCancelApplications()
```

**修改后：**
```java
public List<ReceiptCancelApplication> getPendingCancelApplications(
    Authentication auth,
    String enterpriseId)
```

**添加权限验证逻辑：**
```java
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
if (!(auth instanceof UserAuthentication)) {
    throw new BusinessException("无效的认证信息");
}
UserAuthentication userAuth = (UserAuthentication) auth;

// 管理员可以查询所有待审核申请
if (userAuth.isSystemAdmin()) {
    return cancelApplicationRepository.findPendingApplications();
}

// 仓储企业只能查询自己仓单的待审核申请
if (userAuth.getEnterpriseId() != null) {
    // 需要添加Repository方法：findPendingApplicationsByWarehouse
    return cancelApplicationRepository.findPendingApplicationsByWarehouse(
        userAuth.getEnterpriseId());
}

throw new BusinessException("无权限查询待审核的作废申请");
```

#### 1.2 查询已作废仓单添加权限验证

**修改前：**
```java
public List<ElectronicWarehouseReceiptResponse> getCancelledReceipts(String enterpriseId)
```

**修改后：**
```java
public List<ElectronicWarehouseReceiptResponse> getCancelledReceipts(
    String enterpriseId,
    Authentication auth)
```

**添加权限验证逻辑：**
```java
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
if (!(auth instanceof UserAuthentication)) {
    throw new BusinessException("无效的认证信息");
}
UserAuthentication userAuth = (UserAuthentication) auth;

// 如果提供了enterpriseId，验证权限
if (enterpriseId != null && !enterpriseId.isEmpty()) {
    // 管理员可以查询任何企业
    if (!userAuth.isSystemAdmin() &&
        !userAuth.getEnterpriseId().equals(enterpriseId)) {
        throw new BusinessException("无权限查询该企业的已作废仓单");
    }
} else {
    // 未提供enterpriseId，默认查询当前用户企业的
    enterpriseId = userAuth.getEnterpriseId();
}

// 继续原有逻辑...
```

---

### 修复2：完善Swagger注解

#### 2.1 提交作废申请接口

```java
@PostMapping("/cancel/apply")
@ApiOperation(value = "提交仓单作废申请",
    notes = "货主企业提交仓单作废申请。" +
            "权限要求：当前持单人（货主企业）。" +
            "只有正常状态(NORMAL)或上链失败状态(ONCHAIN_FAILED)的仓单可以申请作废。" +
            "已提货、已质押、已冻结、已拆分的仓单不能作废。" +
            "作废后的仓单无法恢复。")
@ApiResponses({
    @ApiResponse(code = 200, message = "作废申请提交成功", response = CancelApplicationResponse.class),
    @ApiResponse(code = 400, message = "请求参数错误或仓单状态不允许作废"),
    @ApiResponse(code = 403, message = "无权限操作（只有当前持单人/货主企业可以申请作废）"),
    @ApiResponse(code = 404, message = "仓单不存在"),
    @ApiResponse(code = 409, message = "该仓单已有待审核的作废申请")
})
```

#### 2.2 审核作废申请接口

```java
@PostMapping("/cancel/approve")
@ApiOperation(value = "审核仓单作废申请",
    notes = "管理员或仓储企业审核作废申请。" +
            "权限要求：系统管理员或关联的仓储企业。" +
            "审核通过后仓单状态变为CANCELLED（已作废），无法恢复。" +
            "审核拒绝后仓单状态恢复为NORMAL。")
@ApiResponses({
    @ApiResponse(code = 200, message = "审核完成", response = CancelApprovalResponse.class),
    @ApiResponse(code = 400, message = "申请状态不允许审核或仓单状态已变更"),
    @ApiResponse(code = 403, message = "无权限审核（只有系统管理员或关联的仓储企业可以审核）"),
    @ApiResponse(code = 404, message = "作废申请不存在")
})
```

#### 2.3 查询待审核申请接口

```java
@GetMapping("/cancel/pending")
@ApiOperation(value = "查询待审核的作废申请",
    notes = "查询待审核的作废申请列表。" +
            "权限要求：系统管理员可查询所有待审核申请；" +
            "仓储企业只能查询自己仓单的待审核申请。" +
            "用于管理员和仓储企业查看需要审核的作废申请。")
@ApiResponses({
    @ApiResponse(code = 200, message = "查询成功", response = ReceiptCancelApplication.class, responseContainer = "List"),
    @ApiResponse(code = 403, message = "无权限查询")
})
```

#### 2.4 查询已作废仓单接口

```java
@GetMapping("/cancelled")
@ApiOperation(value = "查询已作废的仓单",
    notes = "查询已作废状态的仓单列表。" +
            "权限要求：默认只返回当前用户企业的已作废仓单；" +
            "系统管理员可以通过enterpriseId参数查询指定企业的已作废仓单。" +
            "可以按企业ID筛选，不传则查询当前企业的已作废仓单。")
@ApiResponses({
    @ApiResponse(code = 200, message = "查询成功", response = ElectronicWarehouseReceiptResponse.class, responseContainer = "List"),
    @ApiResponse(code = 403, message = "无权限查询该企业的已作废仓单")
})
```

---

### 修复3：添加Repository方法

需要在 `ReceiptCancelApplicationRepository` 中添加：

```java
/**
 * 根据仓储企业ID查询待审核的作废申请
 */
@Query("SELECT a FROM ReceiptCancelApplication a " +
       "JOIN electronic_warehouse_receipt r ON a.receipt_id = r.id " +
       "WHERE r.warehouse_id = :warehouseId AND a.requestStatus = 'PENDING' " +
       "ORDER BY a.createdAt DESC")
List<ReceiptCancelApplication> findPendingApplicationsByWarehouse(
    @Param("warehouseId") String warehouseId);
```

---

## 📊 优先级建议

| 优先级 | 问题 | 影响 | 建议修复时间 |
|--------|------|------|-------------|
| 🔴 P0 | 查询接口缺少权限验证 | 数据泄露 | 立即修复 |
| 🟡 P1 | Swagger注解缺少权限说明 | API文档不清晰 | 尽快修复 |
| 🟢 P2 | 添加Repository方法 | 功能完整性 | 尽快修复 |

---

## 🎯 总结

### 优点
- ✅ DTO类Swagger注解完整
- ✅ 提交申请权限验证严格（4层验证）
- ✅ 审核申请权限验证正确
- ✅ 业务规则验证完善

### 需要改进
- ❌ 查询接口缺少权限验证（P0 - 严重）
- ⚠️ Swagger注解需要补充权限说明（P1）
- ⚠️ 需要添加新的Repository方法（P2）

### 建议行动
1. **立即**：修复查询接口的权限验证问题
2. **尽快**：完善Swagger注解的权限说明
3. **尽快**：添加Repository的查询方法

---

**报告生成时间：** 2026-02-02
**检查人员：** Claude Code
**下一步：** 实施修复方案
