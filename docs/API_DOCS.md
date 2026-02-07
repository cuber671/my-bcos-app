# FISCO BCOS REST API 文档

## 概述

本项目提供了一个 RESTful API 接口，通过浏览器或 HTTP 客户端与 FISCO BCOS 区块链进行交互。

**服务器地址**: `http://localhost:8080`

## 启动应用

```bash
mvn spring-boot:run
```

或编译后运行：

```bash
mvn clean package
java -jar target/my-bcos-app-1.0-SNAPSHOT.jar
```

## API 接口列表

### 1. API 首页

**接口**: `GET /api/`

**描述**: 获取所有可用的 API 端点列表

**示例**:
```bash
curl http://localhost:8080/api/
```

**响应**:
```json
{
  "name": "FISCO BCOS REST API",
  "version": "1.0",
  "description": "FISCO BCOS 区块链交互接口",
  "endpoints": {
    "GET /api/health": "健康检查",
    "GET /api/block/latest": "获取最新区块号",
    ...
  }
}
```

---

### 2. 健康检查

**接口**: `GET /api/health`

**描述**: 检查区块链连接状态和基本信息

**示例**:
```bash
curl http://localhost:8080/api/health
```

**响应**:
```json
{
  "status": "success",
  "connected": true,
  "blockNumber": "100",
  "accountAddress": "0x...",
  "message": "区块链连接正常"
}
```

---

### 3. 获取最新区块号

**接口**: `GET /api/block/latest`

**描述**: 获取链上最新的区块号

**示例**:
```bash
curl http://localhost:8080/api/block/latest
```

**响应**:
```json
{
  "status": "success",
  "blockNumber": "100"
}
```

---

### 4. 获取指定区块信息

**接口**: `GET /api/block/{number}`

**描述**: 获取指定区块的详细信息

**参数**:
- `number`: 区块号（路径参数）

**示例**:
```bash
curl http://localhost:8080/api/block/100
```

**响应**:
```json
{
  "status": "success",
  "blockNumber": "100",
  "message": "区块查询功能"
}
```

---

### 5. 获取账户信息

**接口**: `GET /api/account`

**描述**: 获取当前账户的地址和公钥信息

**示例**:
```bash
curl http://localhost:8080/api/account
```

**响应**:
```json
{
  "status": "success",
  "address": "0x123...",
  "publicKey": "0xabc...",
  "cryptoType": 0
}
```

---

### 6. 获取节点列表

**接口**: `GET /api/nodes`

**描述**: 获取已连接的区块链节点列表

**示例**:
```bash
curl http://localhost:8080/api/nodes
```

**响应**:
```json
{
  "status": "success",
  "peers": "节点信息..."
}
```

---

### 7. 部署 HelloWorld 合约

**接口**: `POST /api/contract/deploy`

**描述**: 部署一个新的 HelloWorld 智能合约到区块链

**示例**:
```bash
curl -X POST http://localhost:8080/api/contract/deploy
```

**响应**:
```json
{
  "status": "success",
  "contractAddress": "0xxyz...",
  "message": "合约部署成功"
}
```

**注意**: 保存返回的 `contractAddress`，后续调用合约时需要使用。

---

### 8. 调用 HelloWorld 合约的 get 方法

**接口**: `GET /api/contract/{address}/get`

**描述**: 读取 HelloWorld 合约中的 name 值（不发送交易）

**参数**:
- `address`: 合约地址（路径参数）

**示例**:
```bash
curl http://localhost:8080/api/contract/0xxyz.../get
```

**响应**:
```json
{
  "status": "success",
  "value": "Hello, World!",
  "contractAddress": "0xxyz...",
  "message": "读取成功"
}
```

---

### 9. 调用 HelloWorld 合约的 set 方法

**接口**: `POST /api/contract/{address}/set`

**描述**: 设置 HelloWorld 合约中的 name 值（发送交易）

**参数**:
- `address`: 合约地址（路径参数）
- Body: `{"value": "新值"}`

**示例**:
```bash
curl -X POST http://localhost:8080/api/contract/0xxyz.../set \
  -H "Content-Type: application/json" \
  -d '{"value": "Hello FISCO!"}'
```

**响应**:
```json
{
  "status": "success",
  "contractAddress": "0xxyz...",
  "newValue": "Hello FISCO!",
  "message": "设置成功",
  "transactionHash": "0xtxn...",
  "blockNumber": "101",
  "receiptStatus": "0x0"
}
```

---

## 完整工作流示例

### 1. 启动应用并检查连接

```bash
# 检查健康状态
curl http://localhost:8080/api/health

# 获取账户信息
curl http://localhost:8080/api/account
```

### 2. 部署合约

```bash
# 部署 HelloWorld 合约
curl -X POST http://localhost:8080/api/contract/deploy
```

保存返回的合约地址，例如：`0x1234567890abcdef...`

### 3. 读取合约数据

```bash
# 读取 name 值（初始值应该是 "Hello, World!"）
curl http://localhost:8080/api/contract/0x1234567890abcdef.../get
```

### 4. 修改合约数据

```bash
# 设置新的 name 值
curl -X POST http://localhost:8080/api/contract/0x1234567890abcdef.../set \
  -H "Content-Type: application/json" \
  -d '{"value": "My New Value"}'
```

### 5. 验证修改

```bash
# 再次读取 name 值，应该返回 "My New Value"
curl http://localhost:8080/api/contract/0x1234567890abcdef.../get
```

---

## 使用 Postman 或其他 API 测试工具

### 导入以下接口到 Postman：

1. **Health Check**
   - Method: GET
   - URL: `http://localhost:8080/api/health`

2. **Deploy Contract**
   - Method: POST
   - URL: `http://localhost:8080/api/contract/deploy`

3. **Get Contract Value**
   - Method: GET
   - URL: `http://localhost:8080/api/contract/{{address}}/get`
   - Variables: `address` - 合约地址

4. **Set Contract Value**
   - Method: POST
   - URL: `http://localhost:8080/api/contract/{{address}}/set`
   - Headers: `Content-Type: application/json`
   - Body (raw JSON):
     ```json
     {
       "value": "your value here"
     }
     ```

---

## 错误处理

所有接口返回格式：

```json
{
  "status": "error",
  "message": "错误描述..."
}
```

常见错误：
- **连接失败**: 检查节点是否运行
- **合约地址无效**: 确保合约已部署
- **参数缺失**: 检查请求参数是否完整

---

## 浏览器直接访问

除了 `POST` 请求外，所有 `GET` 请求都可以直接在浏览器中访问：

- 访问: `http://localhost:8080/api/health`
- 访问: `http://localhost:8080/api/block/latest`
- 访问: `http://localhost:8080/api/contract/<合约地址>/get`

---

## 注意事项

1. **端口配置**: 默认端口为 8080，可在 `application.properties` 中修改
2. **跨域问题**: 如果需要从前端应用调用，可能需要添加 CORS 支持
3. **交易确认**: set 方法会发送交易，需要等待区块打包
4. **Gas 费用**: 每次写入操作（set、deploy）都会消耗 gas

---

## 项目结构

```
src/main/java/com/fisco/app/
├── controller/
│   └── BcosController.java    # REST API 控制器
├── config/
│   └── BcosConfig.java        # SDK 配置类
├── contract/
│   └── HelloWorld.java        # 智能合约 Java 类
└── BcosApplication.java       # 主应用类
```
