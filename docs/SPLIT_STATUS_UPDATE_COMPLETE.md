# 仓单拆分状态枚举更新完成

生成时间：2026-02-02
状态：✅ 完成

---

## 📋 更新内容

### 1. 数据库迁移脚本 ✅

**文件：** `src/main/resources/db/migration/V11__add_split_status.sql`

**更新内容：**

#### 修改receipt_status枚举约束（添加2个新状态）

```sql
ALTER TABLE `electronic_warehouse_receipt` ADD CONSTRAINT `chk_ewr_receipt_status` CHECK (
    `receipt_status` IN (
        'DRAFT',             -- 草稿
        'PENDING_ONCHAIN',   -- 待上链
        'NORMAL',            -- 正常
        'ONCHAIN_FAILED',    -- 上链失败
        'PLEDGED',           -- 已质押
        'TRANSFERRED',       -- 已转让
        'FROZEN',            -- 已冻结
        'SPLITTING',         -- 拆分中 ✨ 新增
        'SPLIT',             -- 已拆分 ✨ 新增
        'EXPIRED',           -- 已过期
        'DELIVERED',         -- 已提货
        'CANCELLED'          -- 已取消
    )
);
```

#### 添加拆分相关字段（2个新字段）

```sql
-- 拆分时间
ALTER TABLE `electronic_warehouse_receipt`
ADD COLUMN `split_time` DATETIME(6) DEFAULT NULL
COMMENT '拆分时间|仓单被拆分的时间|状态变为SPLIT时记录' AFTER `remarks`;

-- 子仓单数量
ALTER TABLE `electronic_warehouse_receipt`
ADD COLUMN `split_count` INT DEFAULT NULL
COMMENT '子仓单数量|拆分后的子仓单数量|只有已拆分的仓单有值' AFTER `split_time`;
```

#### 添加索引（2个）

```sql
CREATE INDEX `idx_split_time` ON `electronic_warehouse_receipt` (`split_time`);
CREATE INDEX `idx_parent_receipt` ON `electronic_warehouse_receipt` (`parent_receipt_id`);
```

#### 添加检查约束

```sql
ALTER TABLE `electronic_warehouse_receipt`
ADD CONSTRAINT `chk_ewr_split_count_positive` CHECK (
    `split_count` IS NULL OR `split_count` >= 2
);
```

---

### 2. Java实体类更新 ✅

**文件：** `src/main/java/com/fisco/app/entity/ElectronicWarehouseReceipt.java`

#### 更新ReceiptStatus枚举（添加2个新状态）

```java
public enum ReceiptStatus {
    DRAFT,            // 草稿
    PENDING_ONCHAIN,  // 待上链（审核通过，正在上链中）
    NORMAL,           // 正常（已审核且已上链）
    ONCHAIN_FAILED,   // 上链失败（审核通过但上链失败，可重试）
    PLEDGED,          // 已质押
    TRANSFERRED,      // 已转让
    FROZEN,           // 已冻结
    SPLITTING,        // 拆分中 ✨ 新增（拆分申请已提交，正在审核）
    SPLIT,            // 已拆分 ✨ 新增（拆分完成，父仓单状态）
    EXPIRED,          // 已过期
    DELIVERED,        // 已提货
    CANCELLED         // 已取消
}
```

#### 添加拆分相关字段（2个字段）

```java
// 在remarks字段后添加

@Column(name = "split_time", columnDefinition = "DATETIME(6)")
@ApiModelProperty(value = "拆分时间", example = "2026-02-02T10:30:00")
private LocalDateTime splitTime;

@ApiModelProperty(value = "子仓单数量", example = "2")
private Integer splitCount;
```

---

## 🔄 状态流转说明

### 拆分流程状态变化

```
1. 提交拆分申请
   父仓单：NORMAL → SPLITTING

2. 审核通过
   父仓单：SPLITTING → SPLIT（已拆分）
   子仓单：新创建，状态为NORMAL

3. 审核拒绝
   父仓单：SPLITTING → NORMAL（恢复原状态）
```

### 完整的状态枚举（12个）

| 序号 | 状态 | 说明 | 是否终态 |
|------|------|------|---------|
| 1 | DRAFT | 草稿 | 否 |
| 2 | PENDING_ONCHAIN | 待上链 | 否 |
| 3 | NORMAL | 正常 | 否 |
| 4 | ONCHAIN_FAILED | 上链失败 | 否 |
| 5 | PLEDGED | 已质押 | 否 |
| 6 | TRANSFERRED | 已转让 | 否 |
| 7 | FROZEN | 已冻结 | 否 |
| 8 | **SPLITTING** | **拆分中** ✨ | **否** |
| 9 | **SPLIT** | **已拆分** ✨ | **是（父仓单）** |
| 10 | EXPIRED | 已过期 | 是 |
| 11 | DELIVERED | 已提货 | 是 |
| 12 | CANCELLED | 已取消 | 是 |

---

## 📊 数据库表结构变化

### electronic_warehouse_receipt 表

| 变化类型 | 字段/约束 | 说明 |
|---------|----------|------|
| **枚举扩展** | receipt_status | 从10个状态扩展到12个状态 |
| **新增字段** | split_time | 记录拆分时间 |
| **新增字段** | split_count | 记录子仓单数量 |
| **新增索引** | idx_split_time | 按拆分时间查询优化 |
| **新增索引** | idx_parent_receipt | 父子关系查询（已有）|
| **新增约束** | chk_ewr_split_count_positive | split_count必须>=2 |

---

## 🎯 字段说明

### split_time（拆分时间）

- **类型：** DATETIME(6)
- **可空：** YES
- **说明：** 记录仓单被拆分的时间
- **何时有值：** receipt_status = SPLIT 时
- **示例：** 2026-02-02T10:30:00

### split_count（子仓单数量）

- **类型：** INT
- **可空：** YES
- **约束：** 如果不为NULL，必须 >= 2
- **说明：** 记录拆分后生成的子仓单数量
- **何时有值：** receipt_status = SPLIT 时
- **示例：** 2（表示拆分成2个子仓单）

---

## 🔍 验证方法

### 1. 验证数据库约束

```sql
-- 查看receipt_status约束
SELECT CONSTRAINT_NAME, CHECK_CLAUSE
FROM INFORMATION_SCHEMA.CHECK_CONSTRAINTS
WHERE TABLE_NAME = 'electronic_warehouse_receipt'
  AND CONSTRAINT_NAME = 'chk_ewr_receipt_status';
```

**预期结果：** 应包含SPLITTING和SPLIT

### 2. 验证新字段

```sql
DESCRIBE electronic_warehouse_receipt;
```

**预期结果：** 应该看到split_time和split_count字段

### 3. 验证Java实体

```java
// 测试新状态
ElectronicWarehouseReceipt.ReceiptStatus.SPLITTING
ElectronicWarehouseReceipt.ReceiptStatus.SPLIT

// 测试新字段
LocalDateTime splitTime = receipt.getSplitTime();
Integer splitCount = receipt.getSplitCount();
```

---

## 📝 使用示例

### 1. 查询已拆分的仓单

```java
// Service层
public List<ElectronicWarehouseReceipt> getSplitReceipts() {
    return repository.findByReceiptStatus(
        ElectronicWarehouseReceipt.ReceiptStatus.SPLIT
    );
}
```

### 2. 查询某个父仓单的所有子仓单

```java
// Service层
public List<ElectronicWarehouseReceipt> getChildReceipts(String parentReceiptId) {
    return repository.findByParentReceiptId(parentReceiptId);
}
```

### 3. 更新拆分状态

```java
// Service层
@Transactional
public void markAsSplit(String receiptId, Integer splitCount) {
    ElectronicWarehouseReceipt receipt = repository.findById(receiptId)
        .orElseThrow(() -> new RuntimeException("仓单不存在"));

    receipt.setReceiptStatus(ElectronicWarehouseReceipt.ReceiptStatus.SPLIT);
    receipt.setSplitTime(LocalDateTime.now());
    receipt.setSplitCount(splitCount);

    repository.save(receipt);
}
```

---

## ✅ 完成清单

- [x] 数据库迁移脚本创建（V11__add_split_status.sql）
- [x] 修改ReceiptStatus枚举（添加SPLITTING和SPLIT）
- [x] 添加split_time字段
- [x] 添加split_count字段
- [x] 添加split_time索引
- [x] 添加split_count检查约束
- [x] Java实体类更新
- [x] 编译验证通过

---

## 🚀 下一步

数据库和实体类已经准备好，现在可以开始实现仓单拆分功能：

1. 创建DTO类（5个）
2. 创建拆分申请实体类
3. 创建Repository
4. 实现Service层逻辑
5. 实现Controller接口
6. 编写单元测试

**预计工作量：** 2个工作日

---

**文档版本：** v1.0
**更新时间：** 2026-02-02
**状态：** ✅ 完成
