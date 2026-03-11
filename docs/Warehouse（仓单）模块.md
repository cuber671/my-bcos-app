### 1. 模块定位

Warehouse 模块负责物流金融中“物”的数字化转换。通过将物理库存映射为链上 **电子仓单** ，实现货权的 **确权、质押、拆分与背书流转** ，为融资模块提供底层资产支撑。

---

### 2. 核心业务逻辑

模块围绕电子仓单的“生命周期”展开，重点处理以下核心操作：

* **签发 (Mint)** ：仓储方根据入库实物信息，在链上铸造唯一数字凭证。
* **背书转让 (Endorsement)** ：货权人通过“发起背书+对方签收”的闭环流程，法律有效地转移货权，并记录转让过程，便于溯源。
* **拆分与合并 (Split & Merge)** ：支持资产的灵活重组，拆分需满足“重量守恒”原则。
* **质押与解押 (Lock & Unlock)** ：由金融机构等发起，冻结资产流转权限，仓储方只能接收，无需确认（因为没有权限）。
* **核销 (Burn)** ：提货出库后，仓单的当前持有者发起，仓储方审核。资产在链上注销，生命周期结束。

---

### 3. 操作权限矩阵 (RBAC + ABAC)

| **业务动作**          | **平台管理员** | **仓储方 (Warehouse)** | **企业 (Owner/Supplier)** | **金融机构 (Bank)** | **核心约束条件 (ABAC & 状态机)**                  |
| --------------------------- | -------------------- | ---------------------------- | ------------------------------- | ------------------------- | ------------------------------------------------------- |
| **仓单签发 (Mint)**   | ❌                   | **✅ 发起**            | ❌                              | ❌                        | 必须基于真实入库单据；产生初始 `root_id`              |
| **仓单拆分/合并**     | ❌                   | ✅**操作**             | ✅**发起**                | ❌                        | 1. 仅限当前持有人``2.`is_locked`必须为**false** |
| **发起背书转让**      | ❌                   | ❌                           | **✅ 发起**               | ❌                        | 1. 仅限当前持有人``2. 仓单状态必须为 `ACTIVE`         |
| **背书签收确认**      | ❌                   | ❌                           | **✅ 接收**               | ❌                        | 仅限被背书的目标企业操作                                |
| **质押锁定 (Lock)**   | ❌                   | ❌                           | ❌                              | **✅ 操作**         | 1. 基于有效的融资申请``2. 仓单必须处于未锁定状态        |
| **还款解押 (Unlock)** | ❌                   | ❌                           | ❌                              | **✅ 操作**         | 1. 仅限原质押权的银行操作``2. 需核验还款流水            |
| **申请核销出库**      | ❌                   | ❌                           | **✅ 发起**               | ❌                        | 1. 仅限当前持有人``2.`is_locked`必须为**false** |
| **出库终审执行**      | ❌                   | **✅ 审核**            | ❌                              | ❌                        | 1. 校验提货指令的数字签名``2. 链上执行 `Burn`逻辑     |
| **资产全路径溯源**    | ✅                   | ✅                           | ⚠️                            | ⚠️                      | 企业仅限查看名下及历史资产；银行仅限查看质押资产        |

---

### 4. 数据字典

#### 1. 电子仓单

| **字段名**                | **类型**    | **约束**                  | **说明**                                                                   |
| ------------------------------- | ----------------- | ------------------------------- | -------------------------------------------------------------------------------- |
| **`id`**                | `Long`          | `Primary Key`,`Auto`        | 仓单唯一主键 ID,雪花算法生成。                                                   |
| **`warehouse_id`**       | `Long`          | `NotNull`                     | **物理仓库 ID** ，关联具体地理位置与仓库属性。                             |
| **`on_chain_id`**       | `String`        | `Indexed`                     | 链上资产唯一标识（FISCO BCOS TokenID）                                           |
| **`owner_ent_id`**      | `Long`          | `NotNull`                     | **当前货权人** （企业 ID），用于 ABAC 权限校验                             |
| **`owner_user_id`**     | `Long`          | `NotNull`                     | 当前操作人ID，用于溯源记录。                                                     |
| **`warehouse_ent_id`**  | `Long`          | `NotNull`                     | **监管方** （仓储企业 ID），仅该 ID 有权核销出库                           |
| **`warehouse_user_id`** | `Long`          | `NotNull`                     | **监管方操作人ID**，用于溯源记录。                                         |
| **`goods_name`**        | `String`        | `NotNull`                     | 货物名称（如：螺纹钢、大豆）                                                     |
| **`weight`**            | `BigDecimal`    | `NotNull`,`Precision(18,4)` | 货物重量/数量，拆分合并的计算基准                                                |
| **`unit`**              | `String`        | `Default: '吨'`               | 计量单位                                                                         |
| **`parent_id`**         | `Long`          | `Default: 0`                  | 父节点 ID，用于记录拆分来源，追踪资产前身                                        |
| **`root_id`**           | `Long`          | `Default: 0`                  | 原始节点 ID，用于全路径追溯初始入库源头                                          |
| **`is_locked`**         | `Boolean`       | `Default: false`              | **质押锁定状态** 。`true`时禁止拆分、转让和核销                          |
| **`status`**            | `Integer`       | `NotNull`                     | 业务状态（1-在库, 2-待转让, 3-已拆分/合并, 4-已核销，5-物流转运中） |
| **`create_time`**       | `LocalDateTime` | `NotNull`                     | 仓单签发入库时间                                                                 |
| **`update_time`**       | `LocalDateTime` | `Nullable`                    | 最后一次状态变更时间                                                             |

#### 2. 背书记录表

| **字段名**                 | **类型**    | **约束**          | **说明**                                                |
| -------------------------------- | ----------------- | ----------------------- | ------------------------------------------------------------- |
| **`id`**                 | `Long`          | `PK`, 雪花算法        | 背书流水唯一主键 ID                                           |
| **`receipt_id`**         | `Long`          | `NotNull`,`Indexed` | 关联的原始仓单主键 ID                                         |
| **`transferor_ent_id`**  | `Long`          | `NotNull`             | **背书企业ID** （转出方企业，即原持有人）               |
| **`transferor_user_id`** | `Long`          | `NotNull`             | **背书操作人ID** （具体执行转让申请的员工）             |
| **`transferee_ent_id`**  | `Long`          | `NotNull`             | **被背书企业ID** （接收方企业，即新持有人）             |
| **`transferee_user_id`** | `Long`          | `Nullable`            | **接收操作人ID** （具体点击签收确认的员工）             |
| **`signature_hash`**     | `String`        | `NotNull`             | **数字签名哈希** 。由转出方私钥生成，证明转让真实意愿   |
| **`tx_hash`**            | `String`        | `Nullable`            | **区块链交易哈希** 。对应链上货权转移的存证记录         |
| **`status`**             | `Integer`       | `Default: 1`          | **记录状态** （1:待签收, 2:已签收, 3:已拒绝, 4:已撤回） |
| **`remark`**             | `String`        | `Size(255)`           | **转让备注** 。如关联的贸易合同号或转让原因             |
| **`create_time`**        | `LocalDateTime` | `NotNull`             | **发起时间** 。背书转让指令创建的时间                   |
| **`finish_time`**        | `LocalDateTime` | `Nullable`            | **确权时间** 。接收方完成签收动作的时间                 |

#### 3. 仓单拆分/合并记录表

| **字段名**                 | **类型**    | **约束**         | **说明**                                                  |
| -------------------------------- | ----------------- | ---------------------- | --------------------------------------------------------------- |
| **`id`**                 | `Long`          | `PK`, 雪花算法       | 操作记录唯一主键 ID                                             |
| **`op_type`**            | `Integer`       | `NotNull`            | **操作类型** （1: 拆分 Split, 2: 合并 Merge）             |
| **`source_receipt_ids`** | `String`        | `NotNull`            | **来源单据集** ：原仓单 ID 列表，多个 ID 以逗号分隔       |
| **`target_receipt_ids`** | `String`        | `NotNull`            | **生成单据集** ：新产生的仓单 ID 列表，多个 ID 以逗号分隔 |
| **`total_weight`**       | `BigDecimal`    | `NotNull`,`(18,4)` | **操作总重量** ：操作前后的总重量，用于校验资产平衡       |
| **`apply_ent_id`**       | `Long`          | `NotNull`            | **申请企业 ID** ：货主企业（发起方）                      |
| **`apply_user_id`**      | `Long`          | `NotNull`            | **申请操作人 ID** ：货主企业中发起申请的员工              |
| **`execute_ent_id`**     | `Long`          | `NotNull`            | **执行企业 ID** ：仓储方企业（监管人）                    |
| **`execute_user_id`**    | `Long`          | `NotNull`            | **执行操作人 ID** ：仓储方具体操作执行的员工              |
| **`tx_hash`**            | `String`        | `Nullable`           | **区块链交易哈希** ：链上销毁与重铸的最终确权凭证         |
| **`status`**             | `Integer`       | `Default: 1`         | **记录状态** ：1-待操作, 2-已完成, 3-已驳回               |
| **`remark`**             | `String`        | `Size(255)`          | **操作备注** ：拆分/合并原因（如：应融资拆分）            |
| **`create_time`**        | `LocalDateTime` | `NotNull`            | **申请发起时间**                                          |
| **`finish_time`**        | `LocalDateTime` | `Nullable`           | **执行完成时间**                                          |

#### 4. 仓库信息表

| **字段名**            | **类型** | **约束**  | **说明**                                   |
| --------------------------- | -------------- | --------------- | ------------------------------------------------ |
| **`id`**            | `Long`       | `PK`,`Auto` | 物理仓库唯一主键 ID。                            |
| **`ent_id`**        | `Long`       | `NotNull`     | 所属监管方企业 ID（关联 `warehouse_ent_id`）。 |
| **`name`**          | `String`     | `NotNull`     | 仓库名称（如：宝山 1 号金属监管仓）。            |
| **`address`**       | `String`     | `NotNull`     | 详细地址（实物存放的具体位置）。                 |
| **`contact_user`**  | `String`     | `Nullable`    | 现场负责人姓名。                                 |
| **`contact_phone`** | `String`     | `Nullable`    | 现场联系电话。                                   |
| **`status`**        | `Integer`    | `Default: 1`  | 仓库状态（1:正常营业, 2:暂停接单, 3:已关闭）。   |

---

#### 5. 仓单模块 API 接口设计表

| **接口名称**          | **路径 (Method)**        | **必填字段**                                         | **后台逻辑说明**                                                                                                                 |
| --------------------------- | ------------------------------ | ---------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------- |
| **入库单申请**        | `/stock/in/apply`(POST)      | `warehouseId`,`goodsName`,`weight`,`attachmentUrl` | **企业方发起** 。创建 `StockOrder`记录。此时仅为意向，尚未产生电子仓单。                                                       |
| **仓单签发 (Mint)**   | `/receipt/mint`(POST)        | `stockOrderId`,`onChainId`                             | **仓储方操作** 。基于已确认的入库单，向 FISCO BCOS 铸造 Token，并在数据库生成 `WarehouseReceipt`，设置 `root_id=0`。         |
| **发起背书转让**      | `/endorse/launch`(POST)      | `receiptId`,`transfereeEntId`,`signatureHash`        | **持有人发起** 。校验 `is_locked=false`。在 `ReceiptEndorsement`插入状态为“待签收”的记录。**不改变**仓单主表 owner。 |
| **背书签收确认**      | `/endorse/confirm`(PUT)      | `endorseId`,`action`(Accept/Reject)                    | **接收方操作** 。若接受：1. 链上转移货权；2. 更新仓单主表 `owner_ent_id`；3. 流水状态更新。                                    |
| **发起拆分申请**      | `/receipt/split/apply`(POST) | `receiptId`,`targetWeights[]`(数组)                    | **持有人发起** 。校验 `is_locked`及总重量平衡。在 `ReceiptOperationLog`插入待处理记录。                                      |
| **执行拆分/合并**     | `/receipt/op/execute`(POST)  | `opLogId`,`action`(Execute/Reject)                     | **仓储方操作** 。1. 链上销毁旧 Token，铸造新 Token；2. 旧单主表状态设为 3（失效）；3. 插入新单记录并维护 `parent_id`。         |
| **质押锁定 (Lock)**   | `/receipt/lock`(PATCH)       | `receiptId`,`loanId`(融资单号)                         | **金融机构操作** 。校验融资申请有效性。更新主表 `is_locked=true`，禁止除查看外的所有流转接口。                                 |
| **还款解押 (Unlock)** | `/receipt/unlock`(PATCH)     | `receiptId`                                              | **金融机构操作** 。校验还款状态。恢复 `is_locked=false`，释放资产流动性。                                                      |
| **申请核销出库**      | `/receipt/burn/apply`(POST)  | `receiptId`,`signatureHash`                            | **持有人发起** 。校验非锁定状态。创建出库类 `StockOrder`，仓单主表状态转为“出库中”。                                         |
| **核销终审 (Burn)**   | `/receipt/burn/confirm`(PUT) | `stockOrderId`                                           | **仓储方操作** 。1. 校验提货签名；2. 链上 `Burn`逻辑；3. 仓单主表状态设为 4（已核销）。                                        |
| **全路径溯源查询**    | `/trace/receipt/{id}`(GET)   | `id`(Path Variable)                                      | **全角色（按权）** 。递归查询 `parent_id`链条，并组合 `ReceiptEndorsement`流水，还原资产完整演化图谱。                       |

---

### 💡 关键后台设计要点

#### 1. 事务一致性（TCC/本地事务 + 链状态）

在执行“拆分/合并”或“背书”时，必须保证 **数据库事务** 与 **区块链交互** 的最终一致性。

* **推荐做法** ：先在数据库开启事务 **$\rightarrow$** 锁定记录 **$\rightarrow$** 调用 FISCO BCOS 合约 **$\rightarrow$** 收到回执后更新 `tx_hash` **$\rightarrow$** 提交事务。

#### 2. ABAC 权限拦截器设计

在 Service 层或 Controller 层，通过拦截器实现动态校验：

* **权属校验** ：`check(receipt.owner_ent_id == current_user.ent_id)`
* **状态校验** ：`check(receipt.is_locked == false && receipt.status == 1)`
* **监管校验** ：`check(receipt.warehouse_ent_id == current_user.ent_id)`

#### 3. 溯源逻辑实现

对于  **全路径溯源** ，建议利用 `root_id` 快速定位源头，或利用 `parent_id` 进行递归查询：

$$
TracePath = \{ R_{current} \rightarrow R_{parent} \rightarrow \dots \rightarrow R_{root} \}
$$

同时关联 `ReceiptOperationLog` 解释每一次 `parent_id` 变更的原因。
