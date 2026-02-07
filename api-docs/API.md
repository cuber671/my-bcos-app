# 供应链金融系统 API 文档
**生成时间**: 2026-01-18 21:55:40
**版本**: 1.0.0
**描述**: 基于FISCO BCOS区块链的供应链金融系统接口文档

---
## 目录
- [bcos-controller](#bcos-controller) - Bcos Controller
- [仓单管理](#仓单管理) - Warehouse Receipt Controller
- [企业和用户关联查询](#企业和用户关联查询) - Enterprise User Relation Controller
- [企业管理](#企业管理) - Enterprise Controller
- [合约状态查询](#合约状态查询) - Contract Status Controller
- [审计日志管理](#审计日志管理) - Audit Log Controller
- [应收账款管理](#应收账款管理) - Receivable Controller
- [智能合约部署](#智能合约部署) - Deploy Controller
- [用户管理](#用户管理) - User Controller
- [票据管理](#票据管理) - Bill Controller
- [认证管理](#认证管理) - Auth Controller
- [邀请码管理](#邀请码管理) - Invitation Code Controller

---
## bcos-controller
Bcos Controller

### GET /api/
**index**

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/account
**getAccountInfo**

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/block
**getBlock**

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/block/latest
**getLatestBlockNumber**

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/block/{number}
**getBlockByNumber**

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| number | path | string | ✓ | number |

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### POST /api/contract/deploy
**deployHelloWorld**

#### 响应
- **200**: OK
- **201**: Created
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/contract/{address}/get
**callHelloWorldGet**

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| address | path | string | ✓ | address |

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### POST /api/contract/{address}/set
**callHelloWorldSet**

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| address | path | string | ✓ | address |

#### 请求体
**Content-Type**: `application/json`

#### 响应
- **200**: OK
- **201**: Created
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/health
**health**

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/nodes
**getPeers**

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

## 仓单管理
Warehouse Receipt Controller

### POST /api/warehouse-receipt
**创建仓单**

货主创建仓单，使用CreateReceiptParams结构体封装参数，避免Solidity 16变量限制

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| authenticated | query | boolean |  |  |
| authorities[0].authority | query | string |  |  |
| credentials | query | object |  |  |
| details | query | object |  |  |
| principal | query | object |  |  |

#### 请求体
**Content-Type**: `application/json`

**必填字段**: expiryDate, goods, receiptId, storageDate, warehouseAddress, warehouseLocation

| 字段 | 类型 | 必填 | 描述 | 验证规则 |
|------|------|------|------|----------|
| expiryDate | string | ✓ | 过期日期 |  |
| goods | object | ✓ | 货物信息 |  |
| receiptId | string | ✓ | 仓单ID |  |
| storageDate | string | ✓ | 入库日期 |  |
| warehouseAddress | string | ✓ | 仓库地址 |  |
| warehouseLocation | string | ✓ | 仓库物理位置 |  |

#### 响应
- **200**: OK
- **201**: Created
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/warehouse-receipt/{receiptId}
**获取仓单详情**

根据仓单ID查询详细信息

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| receiptId | path | string | ✓ | 仓单ID |

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### POST /api/warehouse-receipt/{receiptId}/release
**释放仓单**

释放已质押的仓单

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| receiptId | path | string | ✓ | 仓单ID |

#### 请求体
**Content-Type**: `application/json`

**必填字段**: releaseType

| 字段 | 类型 | 必填 | 描述 | 验证规则 |
|------|------|------|------|----------|
| interestAmount | number |  | 利息金额 |  |
| releaseType | string | ✓ | 释放类型 |  |
| remark | string |  | 释放备注 |  |
| repaymentAmount | number |  | 还款金额（全额还款时可选） |  |

#### 响应
- **200**: OK
- **201**: Created
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/warehouse-receipt/{receiptId}/releases
**获取仓单释放历史**

查询仓单的所有释放记录

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| receiptId | path | string | ✓ | 仓单ID |

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

## 企业和用户关联查询
Enterprise User Relation Controller

### GET /api/relations/enterprise/address/{address}
**根据地址获取企业及其所有用户**

根据区块链地址查询企业详情和所有用户列表

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| address | path | string | ✓ | 企业地址 |

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/relations/enterprise/{enterpriseId}
**获取企业及其所有用户**

根据企业ID查询企业详情和所有用户列表

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| enterpriseId | path | integer | ✓ | 企业ID |

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/relations/enterprise/{enterpriseId}/active-users
**获取企业活跃用户**

获取指定企业的所有活跃用户

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| enterpriseId | path | integer | ✓ | 企业ID |

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/relations/enterprise/{enterpriseId}/user-count
**统计企业用户数量**

统计指定企业的用户总数

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| enterpriseId | path | integer | ✓ | 企业ID |

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/relations/me
**获取当前用户及其企业信息**

获取当前登录用户的详情和所属企业信息

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| authenticated | query | boolean |  |  |
| authorities[0].authority | query | string |  |  |
| credentials | query | object |  |  |
| details | query | object |  |  |
| principal | query | object |  |  |

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/relations/user/username/{username}
**根据用户名获取用户及其企业信息**

根据用户名查询用户详情和所属企业信息

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| username | path | string | ✓ | 用户名 |

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/relations/user/{userId}
**获取用户及其企业信息**

根据用户ID查询用户详情和所属企业信息

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| userId | path | integer | ✓ | 用户ID |

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/relations/user/{userId}/enterprise/{enterpriseId}/verify
**验证用户归属**

验证用户是否属于指定企业

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| userId | path | integer | ✓ | 用户ID |
| enterpriseId | path | integer | ✓ | 企业ID |

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

## 企业管理
Enterprise Controller

### GET /api/enterprise/active
**获取所有活跃企业**

查询所有状态为ACTIVE的企业列表

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/enterprise/audit-logs
**获取所有审核日志**

分页查询所有企业的审核操作日志

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| page | query | integer |  | 页码（从0开始） |
| size | query | integer |  | 每页大小 |
| sortField | query | string |  | 排序字段 |
| sortDirection | query | string |  | 排序方向（ASC, DESC） |

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/enterprise/audit-logs/action/{action}
**按审核动作查询日志**

根据审核动作（APPROVE/REJECT等）查询审核日志

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| action | path | string | ✓ | 审核动作 |

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/enterprise/audit-statistics
**获取审核统计信息**

统计审核人的审核次数

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### POST /api/enterprise/batch-approve
**批量审核企业**

批量审核通过多个企业注册申请

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| authenticated | query | boolean |  |  |
| authorities[0].authority | query | string |  |  |
| credentials | query | object |  |  |
| details | query | object |  |  |
| principal | query | object |  |  |

#### 请求体
**Content-Type**: `application/json`

#### 响应
- **200**: OK
- **201**: Created
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### POST /api/enterprise/batch-reject
**批量拒绝企业**

批量拒绝多个企业注册申请

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| authenticated | query | boolean |  |  |
| authorities[0].authority | query | string |  |  |
| credentials | query | object |  |  |
| details | query | object |  |  |
| principal | query | object |  |  |

#### 请求体
**Content-Type**: `application/json`

#### 响应
- **200**: OK
- **201**: Created
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/enterprise/pending
**获取待审核企业列表**

查询所有状态为PENDING的待审核企业（支持分页）

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| page | query | integer |  | 页码（从0开始） |
| size | query | integer |  | 每页大小 |
| sortField | query | string |  | 排序字段 |
| sortDirection | query | string |  | 排序方向（ASC, DESC） |

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/enterprise/rating
**按评级范围查询企业**

查询信用评级在指定范围内的企业

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| min | query | integer | ✓ | 最低评级 |
| max | query | integer | ✓ | 最高评级 |

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### POST /api/enterprise/register
**注册企业**

注册新的企业，需要提供企业基本信息

#### 请求体
**Content-Type**: `application/json`

**必填字段**: address, creditCode, name, role

| 字段 | 类型 | 必填 | 描述 | 验证规则 |
|------|------|------|------|----------|
| address | string | ✓ | 区块链地址 |  |
| creditCode | string | ✓ | 统一社会信用代码 |  |
| creditLimit | number |  | 授信额度 |  |
| creditRating | integer |  | 信用评级(0-100) |  |
| enterpriseAddress | string |  | 企业地址 |  |
| name | string | ✓ | 企业名称 |  |
| remarks | string |  | 备注信息 |  |
| role | string | ✓ | 企业角色 |  |
| status | string |  | 企业状态 |  |

#### 响应
- **200**: OK
- **201**: Created
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/enterprise/role/{role}
**按角色查询企业**

根据企业角色查询企业列表

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| role | path | string | ✓ | 企业角色 |

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/enterprise/status/{status}
**按状态查询企业**

根据企业状态查询企业列表

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| status | path | string | ✓ | 企业状态 |

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/enterprise/{address}
**获取企业信息**

根据区块链地址查询企业详细信息

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| address | path | string | ✓ | 企业区块链地址 |

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### PUT /api/enterprise/{address}/approve
**审核企业**

管理员审核通过企业注册

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| address | path | string | ✓ | 企业区块链地址 |
| authenticated | query | boolean |  |  |
| authorities[0].authority | query | string |  |  |
| credentials | query | object |  |  |
| details | query | object |  |  |
| principal | query | object |  |  |

#### 响应
- **200**: OK
- **201**: Created
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/enterprise/{address}/audit-history
**获取企业审核历史**

查询指定企业的审核操作历史

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| address | path | string | ✓ | 企业区块链地址 |

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### PUT /api/enterprise/{address}/credit-limit
**设置授信额度**

为金融机构设置企业的授信额度

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| address | path | string | ✓ | 企业区块链地址 |

#### 请求体
**Content-Type**: `application/json`

#### 响应
- **200**: OK
- **201**: Created
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### PUT /api/enterprise/{address}/credit-rating
**更新信用评级**

更新企业信用评级，范围0-100

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| address | path | string | ✓ | 企业区块链地址 |

#### 请求体
**Content-Type**: `application/json`

#### 响应
- **200**: OK
- **201**: Created
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### PUT /api/enterprise/{address}/reject
**拒绝企业注册**

管理员拒绝企业注册申请

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| address | path | string | ✓ | 企业区块链地址 |
| authenticated | query | boolean |  |  |
| authorities[0].authority | query | string |  |  |
| credentials | query | object |  |  |
| details | query | object |  |  |
| principal | query | object |  |  |

#### 请求体
**Content-Type**: `application/json`

#### 响应
- **200**: OK
- **201**: Created
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### PUT /api/enterprise/{address}/status
**更新企业状态**

更新企业状态（激活、暂停、拉黑等）

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| address | path | string | ✓ | 企业区块链地址 |

#### 请求体
**Content-Type**: `application/json`

#### 响应
- **200**: OK
- **201**: Created
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

## 合约状态查询
Contract Status Controller

### GET /api/contract-status/bill
**查询Bill合约状态**

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/contract-status/code/{address}
**查询合约代码**

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| address | path | string | ✓ | address |

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/contract-status/overview
**查询所有合约状态概览**

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/contract-status/receivable
**查询Receivable合约状态**

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/contract-status/warehouse-receipt
**查询WarehouseReceipt合约状态**

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

## 审计日志管理
Audit Log Controller

### GET /api/audit/entity/{entityType}/{entityId}
**查询实体的操作历史**

查询特定实体的所有操作记录

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| entityType | path | string | ✓ | 实体类型 |
| entityId | path | string | ✓ | 实体ID |

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/audit/logs
**分页查询审计日志**

支持多条件组合查询审计日志

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| actionType | query | string |  | 操作类型（CREATE, UPDATE, DELETE等） |
| endDate | query | string |  | 结束时间 |
| entityId | query | string |  | 实体ID |
| entityType | query | string |  | 实体类型 |
| isSuccess | query | boolean |  | 是否成功 |
| module | query | string |  | 操作模块（BILL, RECEIVABLE, WAREHOUSE_RECEIPT等） |
| page | query | integer |  | 页码（从0开始） |
| size | query | integer |  | 每页大小 |
| sortDirection | query | string |  | 排序方向（ASC, DESC） |
| sortField | query | string |  | 排序字段 |
| startDate | query | string |  | 开始时间 |
| userAddress | query | string |  | 操作人地址 |

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/audit/logs/recent
**获取最近的审计日志**

获取最近的操作记录

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| limit | query | integer |  | 返回数量限制 |

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/audit/logs/{id}
**获取审计日志详情**

根据ID查询审计日志的详细信息

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| id | path | integer | ✓ | 审计日志ID |

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/audit/statistics
**统计审计日志**

获取操作统计数据

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| startDate | query | string |  | 开始时间 |
| endDate | query | string |  | 结束时间 |

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/audit/tx/{txHash}
**根据交易哈希查询**

根据区块链交易哈希查询相关的审计日志

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| txHash | path | string | ✓ | 交易哈希 |

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/audit/user/{userAddress}
**查询用户的操作历史**

查询特定用户的所有操作记录

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| userAddress | path | string | ✓ | 用户地址 |

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

## 应收账款管理
Receivable Controller

### POST /api/receivable
**创建应收账款**

供应商创建新的应收账款，等待核心企业确认。使用CreateReceivableParams结构体封装参数，避免Solidity 16变量限制

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| authenticated | query | boolean |  |  |
| authorities[0].authority | query | string |  |  |
| credentials | query | object |  |  |
| details | query | object |  |  |
| principal | query | object |  |  |

#### 请求体
**Content-Type**: `application/json`

**必填字段**: amount, coreEnterpriseAddress, dueDate, issueDate, receivableId

| 字段 | 类型 | 必填 | 描述 | 验证规则 |
|------|------|------|------|----------|
| amount | number | ✓ | 应收金额 |  |
| coreEnterpriseAddress | string | ✓ | 核心企业地址 |  |
| currency | string |  | 币种 |  |
| description | string |  | 描述 |  |
| dueDate | string | ✓ | 到期日期 |  |
| issueDate | string | ✓ | 出票日期 |  |
| receivableId | string | ✓ | 应收账款ID |  |

#### 响应
- **200**: OK
- **201**: Created
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/receivable/core-enterprise/{address}
**获取核心企业的应付账款**

查询指定核心企业的所有应付账款

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| address | path | string | ✓ | 核心企业地址 |

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/receivable/financier/{address}
**获取资金方的融资账款**

查询指定金融机构的所有融资账款

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| address | path | string | ✓ | 金融机构地址 |

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/receivable/holder/{address}
**获取持票人的应收账款**

查询当前持票人持有的所有应收账款

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| address | path | string | ✓ | 持票人地址 |

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/receivable/status/{status}
**按状态查询应收账款**

根据状态查询应收账款列表

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| status | path | string | ✓ | 应收账款状态 |

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/receivable/supplier/{address}
**获取供应商的应收账款**

查询指定供应商的所有应收账款

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| address | path | string | ✓ | 供应商地址 |

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/receivable/{receivableId}
**获取应收账款详情**

根据应收账款ID查询详细信息

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| receivableId | path | string | ✓ | 应收账款ID |

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### PUT /api/receivable/{receivableId}/confirm
**确认应收账款**

核心企业确认应收账款，确认后可进行融资

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| receivableId | path | string | ✓ | 应收账款ID |

#### 响应
- **200**: OK
- **201**: Created
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### PUT /api/receivable/{receivableId}/finance
**应收账款融资**

金融机构为已确认的应收账款提供融资

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| receivableId | path | string | ✓ | 应收账款ID |

#### 请求体
**Content-Type**: `application/json`

**必填字段**: 

| 字段 | 类型 | 必填 | 描述 | 验证规则 |
|------|------|------|------|----------|
| financeAmount | number |  |  |  |
| financeRate | integer |  |  |  |
| financierAddress | string |  |  |  |

#### 响应
- **200**: OK
- **201**: Created
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### PUT /api/receivable/{receivableId}/repay
**应收账款还款**

核心企业或金融机构进行还款

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| receivableId | path | string | ✓ | 应收账款ID |

#### 请求体
**Content-Type**: `application/json`

#### 响应
- **200**: OK
- **201**: Created
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### PUT /api/receivable/{receivableId}/transfer
**转让应收账款**

将应收账款转让给新的持有人

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| receivableId | path | string | ✓ | 应收账款ID |

#### 请求体
**Content-Type**: `application/json`

#### 响应
- **200**: OK
- **201**: Created
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

## 智能合约部署
Deploy Controller

### POST /api/deploy/all
**部署所有合约**

#### 响应
- **200**: OK
- **201**: Created
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### POST /api/deploy/bill
**部署Bill合约**

#### 响应
- **200**: OK
- **201**: Created
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### POST /api/deploy/enterprise-registry
**部署EnterpriseRegistry合约**

#### 响应
- **200**: OK
- **201**: Created
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### POST /api/deploy/receivable
**部署Receivable合约**

#### 响应
- **200**: OK
- **201**: Created
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### POST /api/deploy/warehouse-receipt
**部署WarehouseReceipt合约**

#### 响应
- **200**: OK
- **201**: Created
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

## 用户管理
User Controller

### GET /api/users
**查询用户列表**

分页查询用户列表

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| page | query | integer |  | 页码 |
| size | query | integer |  | 每页大小 |
| sortBy | query | string |  | 排序字段 |
| sortDir | query | string |  | 排序方向 |

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### POST /api/users
**创建用户**

创建新用户账户

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| authenticated | query | boolean |  |  |
| authorities[0].authority | query | string |  |  |
| credentials | query | object |  |  |
| details | query | object |  |  |
| principal | query | object |  |  |

#### 请求体
**Content-Type**: `application/json`

**必填字段**: password, realName, username

| 字段 | 类型 | 必填 | 描述 | 验证规则 |
|------|------|------|------|----------|
| department | string |  | 部门 |  |
| email | string |  | 电子邮箱 |  |
| enterpriseId | integer |  | 所属企业ID |  |
| password | string | ✓ | 密码 |  |
| phone | string |  | 手机号码 |  |
| position | string |  | 职位 |  |
| realName | string | ✓ | 真实姓名 |  |
| userType | string |  | 用户类型 |  |
| username | string | ✓ | 用户名 |  |

#### 响应
- **200**: OK
- **201**: Created
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/users/enterprise/{enterpriseId}
**获取企业用户**

查询指定企业的所有用户

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| enterpriseId | path | integer | ✓ | 企业ID |

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/users/enterprise/{enterpriseId}/pending
**获取待审核用户**

查询指定企业的所有待审核用户

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| enterpriseId | path | integer | ✓ | 企业ID |

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/users/me
**获取当前用户信息**

获取当前登录用户的详细信息

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| authenticated | query | boolean |  |  |
| authorities[0].authority | query | string |  |  |
| credentials | query | object |  |  |
| details | query | object |  |  |
| principal | query | object |  |  |

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/users/search
**搜索用户**

按真实姓名搜索用户

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| keyword | query | string | ✓ | 搜索关键词 |

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/users/{userId}
**获取用户信息**

根据ID查询用户详细信息

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| userId | path | integer | ✓ | 用户ID |

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### PUT /api/users/{userId}
**更新用户信息**

更新指定用户的信息

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| authenticated | query | boolean |  |  |
| authorities[0].authority | query | string |  |  |
| credentials | query | object |  |  |
| details | query | object |  |  |
| principal | query | object |  |  |
| userId | path | integer | ✓ | 用户ID |

#### 请求体
**Content-Type**: `application/json`

**必填字段**: username

| 字段 | 类型 | 必填 | 描述 | 验证规则 |
|------|------|------|------|----------|
| avatarUrl | string |  | 头像URL |  |
| department | string |  | 部门 |  |
| email | string |  | 电子邮箱 |  |
| phone | string |  | 手机号码 |  |
| position | string |  | 职位 |  |
| realName | string |  | 真实姓名 |  |
| username | string | ✓ | 用户名 |  |

#### 响应
- **200**: OK
- **201**: Created
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### DELETE /api/users/{userId}
**删除用户**

删除指定用户

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| authenticated | query | boolean |  |  |
| authorities[0].authority | query | string |  |  |
| credentials | query | object |  |  |
| details | query | object |  |  |
| principal | query | object |  |  |
| userId | path | integer | ✓ | 用户ID |

#### 响应
- **200**: OK
- **204**: No Content
- **401**: Unauthorized
- **403**: Forbidden

---

### PUT /api/users/{userId}/approve
**审核通过用户**

企业审核通过用户注册申请

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| authenticated | query | boolean |  |  |
| authorities[0].authority | query | string |  |  |
| credentials | query | object |  |  |
| details | query | object |  |  |
| principal | query | object |  |  |
| userId | path | integer | ✓ | 用户ID |

#### 响应
- **200**: OK
- **201**: Created
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### PUT /api/users/{userId}/password
**修改密码**

用户修改自己的密码

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| userId | path | integer | ✓ | 用户ID |

#### 请求体
**Content-Type**: `application/json`

**必填字段**: newPassword, oldPassword

| 字段 | 类型 | 必填 | 描述 | 验证规则 |
|------|------|------|------|----------|
| newPassword | string | ✓ | 新密码 |  |
| oldPassword | string | ✓ | 原密码 |  |

#### 响应
- **200**: OK
- **201**: Created
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### PUT /api/users/{userId}/reject
**拒绝用户注册**

企业拒绝用户注册申请

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| authenticated | query | boolean |  |  |
| authorities[0].authority | query | string |  |  |
| credentials | query | object |  |  |
| details | query | object |  |  |
| principal | query | object |  |  |
| userId | path | integer | ✓ | 用户ID |

#### 请求体
**Content-Type**: `application/json`

#### 响应
- **200**: OK
- **201**: Created
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### PUT /api/users/{userId}/reset-password
**重置密码**

管理员重置指定用户的密码

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| authenticated | query | boolean |  |  |
| authorities[0].authority | query | string |  |  |
| credentials | query | object |  |  |
| details | query | object |  |  |
| principal | query | object |  |  |
| userId | path | integer | ✓ | 用户ID |

#### 请求体
**Content-Type**: `application/json`

**必填字段**: newPassword

| 字段 | 类型 | 必填 | 描述 | 验证规则 |
|------|------|------|------|----------|
| newPassword | string | ✓ | 新密码 |  |

#### 响应
- **200**: OK
- **201**: Created
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### PUT /api/users/{userId}/status
**设置用户状态**

启用/禁用/锁定用户

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| authenticated | query | boolean |  |  |
| authorities[0].authority | query | string |  |  |
| credentials | query | object |  |  |
| details | query | object |  |  |
| principal | query | object |  |  |
| userId | path | integer | ✓ | 用户ID |

#### 请求体
**Content-Type**: `application/json`

**必填字段**: status

| 字段 | 类型 | 必填 | 描述 | 验证规则 |
|------|------|------|------|----------|
| status | string | ✓ | 用户状态 |  |

#### 响应
- **200**: OK
- **201**: Created
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

## 票据管理
Bill Controller

### POST /api/bill
**开票**

出票人开立票据，使用IssueBillParams结构体封装参数，避免Solidity 16变量限制

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| authenticated | query | boolean |  |  |
| authorities[0].authority | query | string |  |  |
| credentials | query | object |  |  |
| details | query | object |  |  |
| principal | query | object |  |  |

#### 请求体
**Content-Type**: `application/json`

**必填字段**: acceptorAddress, amount, beneficiaryAddress, billId, billType, dueDate, issueDate

| 字段 | 类型 | 必填 | 描述 | 验证规则 |
|------|------|------|------|----------|
| acceptorAddress | string | ✓ | 承兑人地址 |  |
| amount | number | ✓ | 票面金额 |  |
| beneficiaryAddress | string | ✓ | 受益人地址 |  |
| billId | string | ✓ | 票据ID |  |
| billType | string | ✓ | 票据类型 |  |
| currency | string |  | 币种 |  |
| description | string |  | 描述 |  |
| dueDate | string | ✓ | 到期日期 |  |
| issueDate | string | ✓ | 出票日期 |  |

#### 响应
- **200**: OK
- **201**: Created
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/bill/{billId}
**获取票据详情**

根据票据ID查询详细信息

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| billId | path | string | ✓ | 票据ID |

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### POST /api/bill/{billId}/discount
**票据贴现**

当前持票人将票据贴现给金融机构

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| authenticated | query | boolean |  |  |
| authorities[0].authority | query | string |  |  |
| billId | path | string | ✓ | 票据ID |
| credentials | query | object |  |  |
| details | query | object |  |  |
| principal | query | object |  |  |

#### 请求体
**Content-Type**: `application/json`

**必填字段**: discountAmount, discountRate, financialInstitutionAddress

| 字段 | 类型 | 必填 | 描述 | 验证规则 |
|------|------|------|------|----------|
| discountAmount | number | ✓ | 贴现金额（元） | 范围: 0.01-? |
| discountRate | number | ✓ | 贴现率（百分比，如 5.5 表示 5.5%） | 范围: 0.0001-? |
| financialInstitutionAddress | string | ✓ | 金融机构地址 |  |
| remark | string |  | 贴现备注 |  |

#### 响应
- **200**: OK
- **201**: Created
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/bill/{billId}/discounts
**获取票据贴现历史**

查询票据的所有贴现记录

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| billId | path | string | ✓ | 票据ID |

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### POST /api/bill/{billId}/endorse
**票据背书**

当前持票人将票据背书转让给被背书人

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| authenticated | query | boolean |  |  |
| authorities[0].authority | query | string |  |  |
| billId | path | string | ✓ | 票据ID |
| credentials | query | object |  |  |
| details | query | object |  |  |
| principal | query | object |  |  |

#### 请求体
**Content-Type**: `application/json`

**必填字段**: endorseeAddress, endorsementType

| 字段 | 类型 | 必填 | 描述 | 验证规则 |
|------|------|------|------|----------|
| endorseeAddress | string | ✓ | 被背书人地址（新持票人） |  |
| endorsementAmount | integer |  | 背书金额（分，部分背书时使用，null表示全额背书） |  |
| endorsementType | string | ✓ | 背书类型 |  |
| remark | string |  | 背书备注 |  |

#### 响应
- **200**: OK
- **201**: Created
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/bill/{billId}/endorsements
**获取票据背书历史**

查询票据的所有背书记录，按时间顺序排列

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| billId | path | string | ✓ | 票据ID |

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/bill/{billId}/endorsements/chain
**从区块链获取背书历史**

从区块链查询票据的所有背书记录，用于验证数据完整性

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| billId | path | string | ✓ | 票据ID |

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/bill/{billId}/endorsements/validate
**验证背书历史完整性**

对比数据库和区块链上的背书记录，验证数据完整性

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| billId | path | string | ✓ | 票据ID |

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### POST /api/bill/{billId}/maturity
**票据到期处理**

处理已贴现票据的到期，自动计算利息并更新状态

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| billId | path | string | ✓ | 票据ID |

#### 响应
- **200**: OK
- **201**: Created
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### POST /api/bill/{billId}/repay
**票据还款**

承兑人主动还款，支持提前还款或逾期还款

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| authenticated | query | boolean |  |  |
| authorities[0].authority | query | string |  |  |
| billId | path | string | ✓ | 票据ID |
| credentials | query | object |  |  |
| details | query | object |  |  |
| principal | query | object |  |  |

#### 请求体
**Content-Type**: `application/json`

**必填字段**: paymentAmount, paymentType

| 字段 | 类型 | 必填 | 描述 | 验证规则 |
|------|------|------|------|----------|
| interestAmount | number |  | 利息金额 |  |
| paymentAmount | number | ✓ | 还款金额 |  |
| paymentType | string | ✓ | 还款类型 |  |
| penaltyInterestAmount | number |  | 逾期利息金额 |  |
| remark | string |  | 还款备注 |  |

#### 响应
- **200**: OK
- **201**: Created
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/bill/{billId}/repayments
**获取票据还款历史**

查询票据的所有还款记录

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| billId | path | string | ✓ | 票据ID |

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

## 认证管理
Auth Controller

### POST /api/auth/api-key
**API密钥认证**

使用API密钥获取JWT令牌（适用于系统间调用）

#### 请求体
**Content-Type**: `application/json`

**必填字段**: apiKey

| 字段 | 类型 | 必填 | 描述 | 验证规则 |
|------|------|------|------|----------|
| apiKey | string | ✓ | API密钥 |  |

#### 响应
- **200**: OK
- **201**: Created
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### POST /api/auth/enterprise-login
**企业登录**

使用区块链地址和密码登录（向后兼容）

#### 请求体
**Content-Type**: `application/json`

**必填字段**: address, password

| 字段 | 类型 | 必填 | 描述 | 验证规则 |
|------|------|------|------|----------|
| address | string | ✓ | 企业区块链地址 |  |
| password | string | ✓ | 登录密码 |  |

#### 响应
- **200**: OK
- **201**: Created
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### POST /api/auth/login
**用户登录**

使用用户名和密码登录（推荐方式）

#### 请求体
**Content-Type**: `application/json`

**必填字段**: password, username

| 字段 | 类型 | 必填 | 描述 | 验证规则 |
|------|------|------|------|----------|
| password | string | ✓ | 登录密码 |  |
| username | string | ✓ | 用户名 |  |

#### 响应
- **200**: OK
- **201**: Created
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### POST /api/auth/register
**用户注册**

用户使用企业邀请码进行注册，注册后需要企业审核

#### 请求体
**Content-Type**: `application/json`

**必填字段**: invitationCode, password, realName, username

| 字段 | 类型 | 必填 | 描述 | 验证规则 |
|------|------|------|------|----------|
| department | string |  | 部门 | 长度: 0-100 |
| email | string |  | 电子邮箱 |  |
| invitationCode | string | ✓ | 企业邀请码 |  |
| password | string | ✓ | 登录密码 | 长度: 6-100 |
| phone | string |  | 手机号码 | 正则: `^1[3-9]\d{9}$` |
| position | string |  | 职位 | 长度: 0-100 |
| realName | string | ✓ | 真实姓名 | 长度: 0-100 |
| remarks | string |  | 注册备注信息 | 长度: 0-500 |
| username | string | ✓ | 用户名（登录账号） | 长度: 3-50 |

#### 响应
- **200**: OK
- **201**: Created
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### POST /api/auth/validate
**验证令牌**

验证JWT令牌是否有效

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| Authorization | header | string | ✓ | Authorization |

#### 响应
- **200**: OK
- **201**: Created
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

## 邀请码管理
Invitation Code Controller

### GET /api/invitation-codes/enterprise/{enterpriseId}
**获取企业邀请码**

查询指定企业的所有邀请码

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| enterpriseId | path | integer | ✓ | 企业ID |

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/invitation-codes/enterprise/{enterpriseId}/active
**获取有效邀请码**

查询指定企业的所有有效邀请码

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| enterpriseId | path | integer | ✓ | 企业ID |

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### POST /api/invitation-codes/generate
**生成邀请码**

为企业生成新的邀请码

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| authenticated | query | boolean |  |  |
| authorities[0].authority | query | string |  |  |
| credentials | query | object |  |  |
| details | query | object |  |  |
| principal | query | object |  |  |

#### 请求体
**Content-Type**: `application/json`

**必填字段**: enterpriseId

| 字段 | 类型 | 必填 | 描述 | 验证规则 |
|------|------|------|------|----------|
| daysValid | integer |  | 有效期（天） |  |
| enterpriseId | integer | ✓ | 企业ID |  |
| maxUses | integer |  | 最大使用次数 |  |
| remarks | string |  | 备注信息 |  |

#### 响应
- **200**: OK
- **201**: Created
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/invitation-codes/validate/{code}
**验证邀请码**

验证邀请码是否有效

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| code | path | string | ✓ | 邀请码 |

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### GET /api/invitation-codes/{codeId}
**获取邀请码详情**

查询邀请码详细信息

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| codeId | path | integer | ✓ | 邀请码ID |

#### 响应
- **200**: OK
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### DELETE /api/invitation-codes/{codeId}
**删除邀请码**

删除指定的邀请码

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| codeId | path | integer | ✓ | 邀请码ID |

#### 响应
- **200**: OK
- **204**: No Content
- **401**: Unauthorized
- **403**: Forbidden

---

### PUT /api/invitation-codes/{codeId}/disable
**禁用邀请码**

禁用指定的邀请码

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| codeId | path | integer | ✓ | 邀请码ID |

#### 响应
- **200**: OK
- **201**: Created
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

### PUT /api/invitation-codes/{codeId}/enable
**启用邀请码**

启用指定的邀请码

#### 参数
| 名称 | 位置 | 类型 | 必填 | 描述 |
|------|------|------|------|------|
| codeId | path | integer | ✓ | 邀请码ID |

#### 响应
- **200**: OK
- **201**: Created
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found

---

