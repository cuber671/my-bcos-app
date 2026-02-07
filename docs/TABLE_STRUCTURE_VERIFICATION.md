# 电子仓单表结构验证报告

生成时间：2026-02-01
数据库：bcos_supply_chain
表名：electronic_warehouse_receipt

---

## ✅ 验证结果：完全一致

### 总体统计

| 项目 | 数据库 | 实体类 | 状态 |
|------|--------|--------|------|
| 字段总数 | 52 | 52 | ✅ 完全一致 |
| 枚举状态数 | 10 | 10 | ✅ 完全一致 |
| 索引数量 | 16 | 13 | ✅ 数据库更完善 |
| 外键约束 | 3 | 3 | ✅ 完全一致 |
| 检查约束 | 6 | - | ✅ 数据库增强 |

---

## ✅ 枚举状态验证（关键）

### ReceiptStatus（仓单状态）

| 状态 | 实体类 | 数据库约束 | 状态 |
|------|--------|-----------|------|
| DRAFT | ✅ | ✅ | ✅ 一致 |
| **PENDING_ONCHAIN** | ✅ | ✅ | ✅ **已添加** |
| NORMAL | ✅ | ✅ | ✅ 一致 |
| **ONCHAIN_FAILED** | ✅ | ✅ | ✅ **已添加** |
| PLEDGED | ✅ | ✅ | ✅ 一致 |
| TRANSFERRED | ✅ | ✅ | ✅ 一致 |
| FROZEN | ✅ | ✅ | ✅ 一致 |
| EXPIRED | ✅ | ✅ | ✅ 一致 |
| DELIVERED | ✅ | ✅ | ✅ 一致 |
| CANCELLED | ✅ | ✅ | ✅ 一致 |

**数据库约束：**
```sql
CONSTRAINT `chk_ewr_receipt_status` CHECK (
    `receipt_status` IN (
        'DRAFT',             -- 草稿
        'PENDING_ONCHAIN',   -- 待上链 ✅
        'NORMAL',            -- 正常
        'ONCHAIN_FAILED',    -- 上链失败 ✅
        'PLEDGED',           -- 已质押
        'TRANSFERRED',       -- 已转让
        'FROZEN',            -- 已冻结
        'EXPIRED',           -- 已过期
        'DELIVERED',         -- 已提货
        'CANCELLED'          -- 已取消
    )
)
```

---

## ✅ 新增字段验证（之前缺失的7个字段）

| 字段名 | 数据类型 | 允许NULL | 默认值 | 状态 |
|--------|---------|---------|--------|------|
| **unit** | VARCHAR(20) | NO | '吨' | ✅ 已添加 |
| **market_price** | DECIMAL(20,2) | YES | NULL | ✅ 已添加 |
| **actual_delivery_date** | DATETIME(6) | YES | NULL | ✅ 已添加 |
| **delivery_person_name** | VARCHAR(100) | YES | NULL | ✅ 已添加 |
| **delivery_person_contact** | VARCHAR(50) | YES | NULL | ✅ 已添加 |
| **delivery_no** | VARCHAR(64) | YES | NULL | ✅ 已添加 |
| **vehicle_plate** | VARCHAR(20) | YES | NULL | ✅ 已添加 |
| **driver_name** | VARCHAR(100) | YES | NULL | ✅ 已添加 |

---

## 📋 完整字段对比

### 基础信息（8个字段）

| 字段 | 实体类 | 数据库 | 状态 |
|------|--------|--------|------|
| id | ✅ String(36) | ✅ VARCHAR(36) | ✅ |
| receipt_no | ✅ String(64) | ✅ VARCHAR(64) | ✅ |
| warehouse_id | ✅ String(36) | ✅ VARCHAR(36) | ✅ |
| warehouse_address | ✅ String(42) | ✅ VARCHAR(42) | ✅ |
| warehouse_name | ✅ String(255) | ✅ VARCHAR(255) | ✅ |
| owner_id | ✅ String(36) | ✅ VARCHAR(36) | ✅ |
| owner_address | ✅ String(42) | ✅ VARCHAR(42) | ✅ |
| holder_address | ✅ String(42) | ✅ VARCHAR(42) | ✅ |

### 货物信息（6个字段）

| 字段 | 实体类 | 数据库 | 状态 |
|------|--------|--------|------|
| goods_name | ✅ String | ✅ VARCHAR(255) | ✅ |
| **unit** | ✅ String(20) | ✅ VARCHAR(20) | ✅ **已添加** |
| quantity | ✅ BigDecimal(20,2) | ✅ DECIMAL(20,2) | ✅ |
| unit_price | ✅ BigDecimal(20,2) | ✅ DECIMAL(20,2) | ✅ |
| total_value | ✅ BigDecimal(20,2) | ✅ DECIMAL(20,2) | ✅ |
| **market_price** | ✅ BigDecimal(20,2) | ✅ DECIMAL(20,2) | ✅ **已添加** |

### 仓储信息（6个字段）

| 字段 | 实体类 | 数据库 | 状态 |
|------|--------|--------|------|
| warehouse_location | ✅ String(500) | ✅ VARCHAR(500) | ✅ |
| storage_location | ✅ String(200) | ✅ VARCHAR(200) | ✅ |
| storage_date | ✅ LocalDateTime | ✅ DATETIME(6) | ✅ |
| expiry_date | ✅ LocalDateTime | ✅ DATETIME(6) | ✅ |

### 提货信息（6个字段）

| 字段 | 实体类 | 数据库 | 状态 |
|------|--------|--------|------|
| **actual_delivery_date** | ✅ LocalDateTime | ✅ DATETIME(6) | ✅ **已添加** |
| **delivery_person_name** | ✅ String(100) | ✅ VARCHAR(100) | ✅ **已添加** |
| **delivery_person_contact** | ✅ String(50) | ✅ VARCHAR(50) | ✅ **已添加** |
| **delivery_no** | ✅ String(64) | ✅ VARCHAR(64) | ✅ **已添加** |
| **vehicle_plate** | ✅ String(20) | ✅ VARCHAR(20) | ✅ **已添加** |
| **driver_name** | ✅ String(100) | ✅ VARCHAR(100) | ✅ **已添加** |

### 状态管理（3个字段）

| 字段 | 实体类 | 数据库 | 状态 |
|------|--------|--------|------|
| receipt_status | ✅ Enum | ✅ VARCHAR(20) | ✅ **枚举已更新** |
| parent_receipt_id | ✅ String(36) | ✅ VARCHAR(36) | ✅ |
| batch_no | ✅ String(64) | ✅ VARCHAR(64) | ✅ |

### 企业和操作人（5个字段）

| 字段 | 实体类 | 数据库 | 状态 |
|------|--------|--------|------|
| owner_name | ✅ String(255) | ✅ VARCHAR(255) | ✅ |
| owner_operator_id | ✅ String(36) | ✅ VARCHAR(36) | ✅ |
| owner_operator_name | ✅ String(100) | ✅ VARCHAR(100) | ✅ |
| warehouse_operator_id | ✅ String(36) | ✅ VARCHAR(36) | ✅ |
| warehouse_operator_name | ✅ String(100) | ✅ VARCHAR(100) | ✅ |

### 融资信息（6个字段）

| 字段 | 实体类 | 数据库 | 状态 |
|------|--------|--------|------|
| is_financed | ✅ Boolean | ✅ TINYINT(1) | ✅ |
| finance_amount | ✅ BigDecimal(20,2) | ✅ DECIMAL(20,2) | ✅ |
| finance_rate | ✅ Integer | ✅ INT | ✅ |
| finance_date | ✅ LocalDateTime | ✅ DATETIME(6) | ✅ |
| financier_address | ✅ String(42) | ✅ VARCHAR(42) | ✅ |
| pledge_contract_no | ✅ String(64) | ✅ VARCHAR(64) | ✅ |

### 背书统计（3个字段）

| 字段 | 实体类 | 数据库 | 状态 |
|------|--------|--------|------|
| endorsement_count | ✅ Integer | ✅ INT | ✅ 默认值: 0 |
| last_endorsement_date | ✅ LocalDateTime | ✅ DATETIME(6) | ✅ |
| current_holder | ✅ String(42) | ✅ VARCHAR(42) | ✅ |

### 区块链（4个字段）

| 字段 | 实体类 | 数据库 | 状态 |
|------|--------|--------|------|
| tx_hash | ✅ String(66) | ✅ VARCHAR(66) | ✅ |
| blockchain_status | ✅ Enum | ✅ VARCHAR(20) | ✅ |
| block_number | ✅ Long | ✅ BIGINT | ✅ |
| blockchain_timestamp | ✅ LocalDateTime | ✅ DATETIME(6) | ✅ |

### 其他和审计（7个字段）

| 字段 | 实体类 | 数据库 | 状态 |
|------|--------|--------|------|
| remarks | ✅ String (TEXT) | ✅ TEXT | ✅ |
| created_at | ✅ LocalDateTime | ✅ DATETIME(6) | ✅ |
| updated_at | ✅ LocalDateTime | ✅ DATETIME(6) | ✅ |
| created_by | ✅ String(50) | ✅ VARCHAR(50) | ✅ |
| updated_by | ✅ String(50) | ✅ VARCHAR(50) | ✅ |
| deleted_at | ✅ LocalDateTime | ✅ DATETIME(6) | ✅ |
| deleted_by | ✅ String(50) | ✅ VARCHAR(50) | ✅ |

---

## 🎯 额外的数据库增强

### 检查约束（数据完整性保护）

```sql
CONSTRAINT `chk_ewr_quantity_positive` CHECK (`quantity` > 0)
CONSTRAINT `chk_ewr_unit_price_positive` CHECK (`unit_price` > 0)
CONSTRAINT `chk_ewr_total_value_positive` CHECK (`total_value` >= 0)
CONSTRAINT `chk_ewr_endorsement_count_positive` CHECK (`endorsement_count` >= 0)
CONSTRAINT `chk_ewr_market_price_positive` CHECK (`market_price` IS NULL OR `market_price` >= 0)
CONSTRAINT `chk_ewr_blockchain_status` CHECK (`blockchain_status` IN ('PENDING', 'SYNCED', 'FAILED', 'VERIFIED'))
```

### 索引（查询性能优化）

16 个索引，包括：
- 主键索引：`id`
- 唯一索引：`receipt_no`
- 业务查询索引：`idx_warehouse`, `idx_owner`, `idx_holder`
- 状态查询索引：`idx_status`, `idx_blockchain_status`
- 时间范围索引：`idx_expiry_date`, `idx_storage_date`, `idx_created_at`
- 软删除索引：`idx_deleted_at`
- 新增提货索引：`idx_delivery_no`

### 外键约束（引用完整性）

```sql
CONSTRAINT `fk_ewr_owner` FOREIGN KEY (`owner_id`) REFERENCES `enterprise` (`id`) ON DELETE RESTRICT
CONSTRAINT `fk_ewr_warehouse` FOREIGN KEY (`warehouse_id`) REFERENCES `enterprise` (`id`) ON DELETE RESTRICT
CONSTRAINT `fk_ewr_parent` FOREIGN KEY (`parent_receipt_id`) REFERENCES `electronic_warehouse_receipt` (`id`) ON DELETE SET NULL
```

---

## 🚀 下一步：重启系统

表结构已完全准备就绪，现在可以重启系统：

```bash
# 启动应用
mvn spring-boot:run
```

**预期结果：**
- ✅ 应用正常启动
- ✅ JPA/Hibernate 识别表结构
- ✅ 所有字段正确映射
- ✅ 枚举状态流转功能正常工作
- ✅ @RequireOnChain 切面正常工作

---

## ✅ 验证清单

重启后请验证以下功能：

### 1. 创建仓单
```bash
curl -X POST "http://localhost:8080/api/ewr/create" \
  -H "Content-Type: application/json" \
  -d '{
    "receiptNo": "EWR20260201000001",
    "warehouseId": "warehouse-001",
    "warehouseAddress": "0x1234567890abcdef1234567890abcdef12345678",
    "ownerId": "owner-001",
    "ownerAddress": "0xabcdefabcdefabcdefabcdefabcdefabcdefabcd",
    "goodsName": "螺纹钢",
    "unit": "吨",  // ✅ 测试新字段
    "quantity": 1000.00,
    "unitPrice": 4500.00,
    "totalValue": 4500000.00
  }'
```

### 2. 审核通过（测试新状态流转）
```bash
curl -X POST "http://localhost:8080/api/ewr/approve" \
  -H "Content-Type: application/json" \
  -d '{
    "receiptId": "...",
    "warehouseId": "warehouse-001",
    "approvalResult": "APPROVED"
  }'
```

**期望结果：**
- 状态变更为 `PENDING_ONCHAIN` ✅
- 上链成功 → `NORMAL` ✅
- 上链失败 → `ONCHAIN_FAILED` ✅

### 3. 测试提货功能（验证新增字段）
```bash
curl -X PUT "http://localhost:8080/api/ewr/delivery/{id}" \
  -H "Content-Type: application/json" \
  -d '{
    "actualDeliveryDate": "2026-06-15T14:30:00",
    "deliveryPersonName": "张三",  // ✅ 测试新字段
    "deliveryPersonContact": "13800138000",  // ✅ 测试新字段
    "deliveryNo": "DEL202606150001",  // ✅ 测试新字段
    "vehiclePlate": "沪A12345",  // ✅ 测试新字段
    "driverName": "李四"  // ✅ 测试新字段
  }'
```

---

## 📊 总结

| 验证项 | 状态 |
|--------|------|
| 字段完整性 | ✅ 52/52 完全一致 |
| 枚举状态 | ✅ 10/10 完全一致（包含新增状态） |
| 数据类型 | ✅ 完全匹配 |
| 约束完整性 | ✅ 6个检查约束 |
| 外键关系 | ✅ 3个外键约束 |
| 索引优化 | ✅ 16个索引 |
| **总体验证结果** | ✅ **完全一致，可以重启** |

---

**验证完成时间：** 2026-02-01
**验证结果：** ✅ 通过
**建议：** 立即重启系统
