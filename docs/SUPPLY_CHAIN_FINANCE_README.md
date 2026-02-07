# 供应链金融系统 - 基于FISCO BCOS

一个完整的区块链供应链金融系统，支持应收账款管理、票据融资、仓单质押等功能。

## 系统架构

### 技术栈

**区块链层**
- FISCO BCOS 3.x
- Solidity 0.8.x智能合约

**后端层**
- Spring Boot 2.7.18
- Spring Data JPA
- MySQL 8.0
- Druid连接池

**前端层**（待开发）
- Vue.js 3.x
- Element Plus

## 核心功能模块

### 1. 企业征信管理
- 企业注册与认证
- 信用评级管理
- 授信额度设置
- 信用历史记录

### 2. 应收账款管理
- 应收账款创建
- 核心企业确认
- 应收账款融资
- 应收账款转让
- 还款管理
- 违约处理

### 3. 票据/信用证管理
- 票据开具
- 票据承兑
- 票据背书转让
- 票据贴现
- 票据付款/拒付

### 4. 仓单融资管理
- 仓单创建
- 仓库验证
- 仓单质押
- 仓单融资
- 仓单释放/清算

## 快速开始

### 前置要求

- Java 11+
- Maven 3.6+
- MySQL 8.0+
- FISCO BCOS 3.x节点

### 1. 数据库初始化

```bash
# 创建数据库
mysql -u root -p
CREATE DATABASE supply_chain_finance DEFAULT CHARSET utf8mb4;

# 导入表结构
mysql -u root -p supply_chain_finance < src/main/resources/db/migration/V1__init_schema.sql
```

### 2. 配置数据库

编辑 `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/supply_chain_finance
spring.datasource.username=root
spring.datasource.password=your_password
```

### 3. 编译智能合约

**方案A：使用FISCO BCOS控制台**

```bash
# 1. 将合约文件复制到控制台目录
cp src/main/resources/contracts/*.sol ~/fisco/console/contracts/solidity/

# 2. 进入控制台目录
cd ~/fisco/console

# 3. 编译合约
./sol2java.sh org.fisco.bcos.sdk.demo.contract

# 4. 将生成的Java合约类复制到项目中
cp sdk/src/main/java/org/fisco/bcos/sdk/demo/contract/*.java \
   my-bcos-app/src/main/java/com/fisco/app/contract/
```

**方案B：手动编译（如果控制台不可用）**

使用Solidity编译器：
```bash
solc --bin --abi --overwrite -o build/ src/main/resources/contracts/*.sol
```

然后根据ABI和BIN手动创建Java合约包装类（参考HelloWorld.java）。

### 4. 部署智能合约

启动应用后，通过API或控制台部署合约：

```bash
# 部署企业注册合约
curl -X POST http://localhost:8080/api/contract/deploy/EnterpriseRegistry

# 部署应收账款合约
curl -X POST http://localhost:8080/api/contract/deploy/Receivable

# 部署票据合约
curl -X POST http://localhost:8080/api/contract/deploy/Bill

# 部署仓单合约
curl -X POST http://localhost:8080/api/contract/deploy/WarehouseReceipt
```

### 5. 启动应用

```bash
cd my-bcos-app

# 使用Maven启动
mvn spring-boot:run

# 或打包后启动
mvn clean package
java -jar target/my-bcos-app-1.0-SNAPSHOT.jar
```

### 6. 验证安装

```bash
# 检查应用健康状态
curl http://localhost:8080/api/health

# 检查区块链连接
curl http://localhost:8080/api/block/latest
```

## API接口文档

### 基础接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/health | 健康检查 |
| GET | /api/block/latest | 获取最新区块号 |

### 企业管理接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/enterprise/register | 注册企业 |
| PUT | /api/enterprise/{address}/approve | 审核企业 |
| PUT | /api/enterprise/{address}/status | 更新企业状态 |
| PUT | /api/enterprise/{address}/credit-rating | 更新信用评级 |
| PUT | /api/enterprise/{address}/credit-limit | 设置授信额度 |
| GET | /api/enterprise/{address} | 获取企业信息 |
| GET | /api/enterprise/active | 获取所有活跃企业 |
| GET | /api/enterprise/role/{role} | 按角色查询企业 |
| GET | /api/enterprise/status/{status} | 按状态查询企业 |
| GET | /api/enterprise/rating?min=60&max=80 | 按评级范围查询 |

### 应收账款接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/receivable | 创建应收账款 |
| PUT | /api/receivable/{id}/confirm | 确认应收账款 |
| PUT | /api/receivable/{id}/finance | 应收账款融资 |
| PUT | /api/receivable/{id}/repay | 应收账款还款 |
| PUT | /api/receivable/{id}/transfer | 转让应收账款 |
| GET | /api/receivable/{id} | 获取应收账款详情 |
| GET | /api/receivable/supplier/{address} | 获取供应商的应收账款 |
| GET | /api/receivable/core-enterprise/{address} | 获取核心企业的应付账款 |
| GET | /api/receivable/financier/{address} | 获取资金方的融资账款 |
| GET | /api/receivable/holder/{address} | 获取持票人的应收账款 |
| GET | /api/receivable/status/{status} | 按状态查询应收账款 |

## 使用示例

### 1. 注册企业

```bash
curl -X POST http://localhost:8080/api/enterprise/register \
  -H "Content-Type: application/json" \
  -d '{
    "address": "0x1234567890abcdef",
    "name": "供应商A",
    "creditCode": "91110000MA001234XY",
    "enterpriseAddress": "北京市朝阳区",
    "role": "SUPPLIER",
    "creditRating": 75,
    "creditLimit": 1000000.00
  }'
```

### 2. 创建应收账款

```bash
curl -X POST http://localhost:8080/api/receivable \
  -H "Content-Type: application/json" \
  -d '{
    "receivableId": "REC20240113001",
    "supplierAddress": "0x1234567890abcdef",
    "coreEnterpriseAddress": "0xabcdef1234567890",
    "amount": 500000.00,
    "currency": "CNY",
    "issueDate": "2024-01-13T10:00:00",
    "dueDate": "2024-04-13T10:00:00",
    "description": "原材料采购款"
  }'
```

### 3. 核心企业确认应收账款

```bash
curl -X PUT http://localhost:8080/api/receivable/REC20240113001/confirm
```

### 4. 金融机构融资

```bash
curl -X PUT http://localhost:8080/api/receivable/REC20240113001/finance \
  -H "Content-Type: application/json" \
  -d '{
    "financierAddress": "0x567890abcdef1234",
    "financeAmount": 450000.00,
    "financeRate": 500
  }'
```

## 数据库表结构

### 核心表

- **enterprise** - 企业基本信息
- **credit_history** - 信用历史记录
- **receivable** - 应收账款
- **receivable_transfer** - 应收账款转让历史
- **bill** - 票据
- **bill_endorsement** - 票据背书历史
- **warehouse_receipt** - 仓单
- **warehouse_pledge** - 仓单质押历史
- **sys_user** - 系统用户
- **audit_log** - 操作日志

## 智能合约说明

### EnterpriseRegistry.sol
企业注册与征信管理合约，负责：
- 企业注册
- 企业审核
- 信用评级管理
- 授信额度管理

### Receivable.sol
应收账款管理合约，负责：
- 应收账款创建
- 应收账款确认
- 应收账款融资
- 应收账款转让
- 还款管理

### Bill.sol
票据管理合约，负责：
- 票据开具
- 票据承兑
- 票据背书
- 票据贴现
- 付款管理

### WarehouseReceipt.sol
仓单管理合约，负责：
- 仓单创建
- 仓单验证
- 仓单质押
- 仓单融资
- 仓单清算

## 系统特性

### 数据存储策略
- **关键数据上链**：交易记录、状态变更、所有权转移
- **详细数据入库**：企业详情、货物描述、操作日志
- **混合存储**：区块链保证不可篡改，数据库提供高效查询

### 安全性
- JWT令牌认证
- 基于角色的权限控制
- 区块链签名验证
- 操作日志审计

### 可扩展性
- 模块化设计
- RESTful API
- 智能合约可升级
- 支持多链部署

## 开发计划

- [x] 智能合约开发
- [x] 数据库设计
- [x] 后端API开发
- [ ] 用户认证和权限管理
- [ ] 前端管理后台
- [ ] 合约Java代码生成
- [ ] 区块链集成测试
- [ ] 性能优化
- [ ] 文档完善

## 常见问题

### Q: 如何重置数据库？
```bash
mysql -u root -p -e "DROP DATABASE supply_chain_finance;"
mysql -u root -p -e "CREATE DATABASE supply_chain_finance DEFAULT CHARSET utf8mb4;"
mysql -u root -p supply_chain_finance < src/main/resources/db/migration/V1__init_schema.sql
```

### Q: 如何查看日志？
```bash
tail -f logs/application.log
```

### Q: 如何修改端口？
编辑 `application.properties`:
```properties
server.port=8081
```

### Q: 区块链连接失败怎么办？
1. 检查FISCO BCOS节点是否运行
2. 检查config.toml配置
3. 查看应用日志中的错误信息

## 许可证

本项目仅供学习和参考使用。

## 联系方式

如有问题，请提交Issue或联系开发团队。
