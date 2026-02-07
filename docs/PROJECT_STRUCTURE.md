# 供应链金融系统 - 项目结构说明

## 项目概述

这是一个基于FISCO BCOS区块链的完整供应链金融系统，实现了应收账款管理、票据融资、仓单质押等核心功能。

## 项目目录结构

```
my-bcos-app/
├── src/main/
│   ├── java/com/fisco/app/
│   │   ├── BcosApplication.java              # 主应用类
│   │   ├── config/                           # 配置类
│   │   │   └── BcosConfig.java              # FISCO BCOS配置
│   │   │   └── GlobalExceptionHandler.java  # 全局异常处理
│   │   ├── controller/                       # 控制器层
│   │   │   ├── BcosController.java          # 区块链基础接口
│   │   │   ├── EnterpriseController.java    # 企业管理API
│   │   │   └── ReceivableController.java    # 应收账款API
│   │   ├── entity/                           # 实体类
│   │   │   ├── Enterprise.java              # 企业实体
│   │   │   ├── Receivable.java              # 应收账款实体
│   │   │   ├── Bill.java                    # 票据实体
│   │   │   └── WarehouseReceipt.java        # 仓单实体
│   │   ├── repository/                       # 数据访问层
│   │   │   ├── EnterpriseRepository.java    # 企业Repository
│   │   │   ├── ReceivableRepository.java    # 应收账款Repository
│   │   │   ├── BillRepository.java          # 票据Repository
│   │   │   └── WarehouseReceiptRepository.java # 仓单Repository
│   │   ├── vo/                              # 视图对象
│   │   │   └── Result.java                  # 统一响应结果
│   │   ├── exception/                        # 异常类
│   │   │   └── BusinessException.java      # 业务异常
│   │   └── contract/                         # 智能合约Java类
│   │       └── HelloWorld.java              # 示例合约
│   └── resources/
│       ├── config.toml                       # FISCO BCOS配置
│       ├── application.properties            # 应用配置
│       ├── contracts/                        # Solidity合约源码
│       │   ├── EnterpriseRegistry.sol       # 企业注册合约
│       │   ├── Receivable.sol               # 应收账款合约
│       │   ├── Bill.sol                     # 票据合约
│       │   └── WarehouseReceipt.sol         # 仓单合约
│       ├── db/migration/                     # 数据库迁移脚本
│       │   └── V1__init_schema.sql          # 数据库初始化
│       └── conf/                            # 证书目录
├── pom.xml                                  # Maven配置
├── README.md                                # 原始项目说明
├── SUPPLY_CHAIN_FINANCE_README.md          # 供应链金融系统说明
└── PROJECT_STRUCTURE.md                    # 本文件
```

## 核心模块说明

### 1. 智能合约层（Solidity）

#### EnterpriseRegistry.sol
**功能**：企业注册与征信管理
- 企业注册
- 企业审核
- 信用评级更新
- 授信额度管理

**主要方法**：
- `registerEnterprise()` - 注册企业
- `approveEnterprise()` - 审核企业
- `updateCreditRating()` - 更新信用评级
- `setCreditLimit()` - 设置授信额度

#### Receivable.sol
**功能**：应收账款管理
- 创建应收账款
- 核心企业确认
- 应收账款融资
- 应收账款转让
- 还款管理

**主要方法**：
- `createReceivable()` - 创建应收账款
- `confirmReceivable()` - 确认应收账款
- `financeReceivable()` - 应收账款融资
- `transferReceivable()` - 转让应收账款
- `repayReceivable()` - 还款

#### Bill.sol
**功能**：票据/信用证管理
- 票据开具
- 票据承兑
- 票据背书
- 票据贴现
- 付款/拒付

**主要方法**：
- `issueBill()` - 开具票据
- `acceptBill()` - 承兑票据
- `endorseBill()` - 背书转让
- `discountBill()` - 票据贴现
- `payBill()` - 付款

#### WarehouseReceipt.sol
**功能**：仓单融资管理
- 仓单创建
- 仓库验证
- 仓单质押
- 仓单融资
- 仓单清算

**主要方法**：
- `createReceipt()` - 创建仓单
- `verifyReceipt()` - 验证仓单
- `pledgeReceipt()` - 质押仓单
- `financeReceipt()` - 仓单融资
- `releaseReceipt()` - 释放仓单

### 2. 数据库层（MySQL）

#### 核心表
- **enterprise** - 企业基本信息表
- **credit_history** - 信用历史记录表
- **receivable** - 应收账款表
- **receivable_transfer** - 应收账款转让历史表
- **bill** - 票据表
- **bill_endorsement** - 票据背书历史表
- **warehouse_receipt** - 仓单表
- **warehouse_pledge** - 仓单质押历史表
- **sys_user** - 系统用户表
- **audit_log** - 操作日志表

#### 索引设计
- 所有业务ID字段建立唯一索引
- 地址字段建立普通索引（便于按地址查询）
- 状态字段建立索引（便于状态筛选）
- 日期字段建立索引（便于时间范围查询）

### 3. 后端应用层（Spring Boot）

#### Controller层
**职责**：接收HTTP请求，参数验证，调用Service，返回响应

- `EnterpriseController` - 企业管理接口
- `ReceivableController` - 应收账款接口
- `BillController` - 票据管理接口（待开发）
- `WarehouseReceiptController` - 仓单管理接口（待开发）

#### Repository层
**职责**：数据库访问，提供CRUD操作

- 继承`JpaRepository`获得基础CRUD功能
- 自定义查询方法（按字段查询、统计等）
- 使用`@Query`注解编写复杂查询

#### Entity层
**职责**：数据模型定义，与数据库表映射

- 使用JPA注解（@Entity, @Table, @Column等）
- 定义枚举类型
- 自动时间戳管理（@PrePersist, @PreUpdate）

### 4. 数据存储策略

#### 区块链存储
**存储内容**：
- 企业注册记录
- 应收账款创建/确认/融资记录
- 票据开具/承兑/背书记录
- 仓单创建/质押/融资记录
- 所有权转移记录

**特点**：
- 不可篡改
- 永久存储
- 可追溯
- 公开透明

#### 数据库存储
**存储内容**：
- 企业详细信息
- 业务详细数据
- 查询索引
- 操作日志
- 用户权限数据

**特点**：
- 高效查询
- 灵活检索
- 支持复杂业务逻辑
- 易于扩展

## 系统特性

### 1. 混合存储架构
- 关键数据上链保证不可篡改
- 详细数据入库提供高效查询
- 双重存储确保数据一致性和可靠性

### 2. 模块化设计
- 各业务模块独立开发
- 统一的接口规范
- 易于维护和扩展

### 3. 完整的业务流程
- 企业注册→征信管理
- 应收账款创建→确认→融资→还款
- 票据开具→承兑→背书→贴现
- 仓单创建→验证→质押→融资

### 4. 安全性保障
- JWT令牌认证（待实现）
- 基于角色的权限控制（待实现）
- 区块链签名验证
- 操作日志审计

## 后续开发计划

### 短期目标
1. 完成剩余Controller开发
   - BillController
   - WarehouseReceiptController

2. 实现用户认证
   - JWT集成
   - 登录/登出接口
   - 权限控制

3. 集成区块链合约
   - 编译Solidity合约
   - 生成Java合约类
   - 在Controller中调用合约

### 中期目标
1. 开发Service业务层
   - 封装业务逻辑
   - 区块链+数据库混合操作
   - 事务管理

2. 开发前端管理后台
   - Vue.js 3.x
   - Element Plus UI
   - 数据可视化

3. 完善异常处理
   - 详细的错误信息
   - 友好的用户提示
   - 完善的日志记录

### 长期目标
1. 性能优化
   - 数据库查询优化
   - 缓存机制（Redis）
   - 批量操作支持

2. 高可用部署
   - 集群部署
   - 负载均衡
   - 容灾备份

3. 高级功能
   - 数据分析与报表
   - 风险评估模型
   - 自动化审批流程
   - 移动端应用

## 技术亮点

1. **区块链+数据库混合架构**
   - 结合区块链的不可篡改特性
   - 利用数据库的高效查询能力

2. **完整的业务流程**
   - 涵盖供应链金融核心场景
   - 支持多角色协作

3. **标准化接口设计**
   - RESTful API
   - 统一的响应格式
   - 完善的错误处理

4. **可扩展性强**
   - 模块化设计
   - 智能合约可升级
   - 支持多链部署

## 开发建议

1. **数据库优先**：先设计好数据库表结构，确保数据模型合理

2. **API优先**：设计好接口规范，前后端可以并行开发

3. **测试驱动**：为关键业务逻辑编写单元测试和集成测试

4. **文档先行**：保持文档的及时更新，便于团队协作

5. **安全第一**：注意数据验证、权限控制、敏感信息加密

## 许可证

本项目仅供学习和参考使用。
