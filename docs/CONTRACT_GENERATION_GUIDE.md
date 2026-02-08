# 信用额度智能合约Java包装类生成指南

## 问题说明

当前创建的 `CreditLimit.java` 是简化版实现，用于临时编译通过。

**完整版本应该使用sol2java工具从Solidity合约自动生成。**

## 使用sol2java工具生成完整Java包装类

### 1. 准备工作

确保已安装FISCO BCOS控制台工具：

```bash
cd ~/fisco/bcos-executor
```

### 2. 编译Solidity合约

```bash
# 进入控制台目录
cd ~/fisco/bcos-executor

# 编译CreditLimit.sol合约
solc/src/main/resources/contracts/CreditLimit.sol --bin --abi --optimize -o build/contracts/
```

### 3. 生成Java包装类

使用sol2java工具：

```bash
# 方法1: 使用sol2java.sh脚本
./sol2java.sh org.fisco.bcos.sdk.demo.contract.CreditLimit

# 方法2: 如果使用完整包名
./sol2java.sh com.fisco.app.contract.CreditLimit
```

### 4. 替换简化版文件

生成的Java文件会保存在：
```
~/fisco/bcos-execulator/src/main/java/org/fisco/bcos/sdk/demo/contract/CreditLimit.java
```

或使用完整包名生成时：
```
~/fisco/bcos-execulator/src/main/java/com/fisco/app/contract/CreditLimit.java
```

**替换步骤：**

```bash
# 1. 备份当前简化版文件
mv /home/llm_rca/fisco/my-bcos-app/src/main/java/com/fisco/app/contract/CreditLimit.java \
   /home/llm_rca/fisco/my-bcos-app/src/main/java/com/fisco/app/contract/CreditLimit.java.backup

# 2. 复制生成的完整版文件
cp ~/fisco/bcos-executor/src/main/java/com/fisco/app/contract/CreditLimit.java \
   /home/llm_rca/fisco/my-bcos-app/src/main/java/com/fisco/app/contract/CreditLimit.java

# 3. 重新编译验证
cd /home/llm_rca/fisco/my-bcos-app
mvn compile -DskipTests
```

## 部署智能合约

### 方法1: 使用DeployController部署

```bash
# 发送部署请求
curl -X POST "http://localhost:8080/api/deploy/credit-limit" \
  -H "Content-Type: application/json"
```

### 方法2: 使用控制台部署

```bash
cd ~/fisco/bcos-executor

# 启动控制台
./scripts/start.sh

# 在控制台中部署
deploy CreditLimit
```

部署成功后会返回合约地址，例如：
```
0x1234567890abcdef1234567890abcdef12345678
```

### 配置合约地址

将合约地址配置到 `application.properties`:

```properties
# 信用额度合约地址
contracts.credit-limit.address=0x1234567890abcdef1234567890abcdef12345678
```

## 验证部署

### 1. 检查合约是否加载

启动应用后查看日志：

```
✓ CreditLimit contract loaded successfully at: 0x...
```

### 2. 测试合约调用

创建信用额度测试：

```bash
curl -X POST "http://localhost:8080/api/credit-limit" \
  -H "Content-Type: application/json" \
  -d '{
    "enterpriseAddress": "0x...",
    "limitType": "FINANCING",
    "totalLimit": 1000000.00,
    "warningThreshold": 80,
    "effectiveDate": "2026-02-03T00:00:00"
  }'
```

### 3. 查看区块链交易

使用控制台查看链上数据：

```bash
# 在控制台中调用查询方法
call CreditLimit 0x... getCreditLimit("limit-id-here")
```

## 完整版与简化版的主要区别

### 简化版（当前实现）
- ✅ 基本结构完整
- ✅ 包含所有枚举类型
- ✅ 包含核心调用方法
- ⚠️  BINARY和ABI可能不完整
- ⚠️  缺少一些辅助方法

### 完整版（自动生成）
- ✅ 完整的合约字节码（BINARY）
- ✅ 完整的ABI定义
- ✅ 所有查询方法
- ✅ 所有事件监听方法
- ✅ 类型安全的枚举转换

## 常见问题

### Q1: sol2java工具找不到

```bash
# 检查控制台是否正确安装
ls -la ~/fisco/bcos-executor/sol2java.sh

# 如果不存在，重新下载控制台
cd ~/fisco
curl -LO https://github.com/FISCO-BCOS/FISCO-BCOS/releases/download/v3.9.0/bcos-executor.tar.gz
tar -zxf bcos-executor.tar.gz
```

### Q2: 编译合约失败

```bash
# 检查Solidity编译器版本
solc --version

# 应该显示 0.8.0 或更高版本
```

### Q3: 生成Java文件失败

```bash
# 确保Solidity文件存在
ls -la /home/llm_rca/fisco/my-bcos-app/src/main/resources/contracts/CreditLimit.sol

# 检查语法是否正确
solc --check /home/llm_rca/fisco/my-bcos-app/src/main/resources/contracts/CreditLimit.sol
```

### Q4: 合约部署后无法调用

```bash
# 检查合约地址配置
cat /home/llm_rca/fisco/my-bcos-app/src/main/resources/application.properties | grep credit-limit

# 检查应用日志
tail -f /home/llm_rca/fisco/my-bcos-app/logs/application.log | grep CreditLimit
```

## 下一步

1. ✅ 使用简化版Java包装类（当前已创建）
2. ⏭️ 使用sol2java生成完整版
3. ⏭️ 部署CreditLimit.sol合约到FISCO BCOS
4. ⏭️ 配置合约地址
5. ⏭️ 测试完整功能

---

**注意：** 当前简化版已满足基本编译和开发需求，可以正常使用。完整版建议在生产环境部署前生成。

**相关文档：**
- [CREDIT_LIMIT_CONTRACT_GUIDE.md](./CREDIT_LIMIT_CONTRACT_GUIDE.md) - 完整的合约使用指南
- FISCO BCOS官方文档：https://fisco-bcos-documentation.readthedocs.io/
