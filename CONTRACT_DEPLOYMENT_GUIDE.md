# ReceivableWithOverdue 智能合约部署指南

## 📋 已部署合约信息

**合约名称**: ReceivableWithOverdue
**合约地址**: `0xc3eead5e05854e87d9e2952516ceafc70adf015b`
**交易哈希**: `0xea423d4b3d5e2db84c3125eeaf9bd5b3db3794fec18915d9ab04c726a62ec84b`
**部署时间**: 2026-02-03

---

## 🚀 如何部署新合约（将来参考）

### 方式 1: 使用 FISCO 控制台（推荐）

```bash
# 1. 复制合约到控制台目录
cp src/main/resources/contracts/ReceivableWithOverdue.sol \
   /home/llm_rca/fisco/console/dist/contracts/solidity/

# 2. 启动控制台并部署
cd /home/llm_rca/fisco/console/dist
bash console.sh deploy ReceivableWithOverdue

# 3. 保存返回的合约地址
```

### 方式 2: 使用控制台脚本

```bash
# 创建部署脚本
cat > deploy_commands.txt << EOF
deploy ReceivableWithOverdue
exit
EOF

# 执行部署
bash console.sh deploy_commands.txt
```

### 方式 3: 使用 contract2java 生成 Java 包装类

```bash
cd /home/llm_rca/fisco/console/dist

# 生成 Java 包装类
bash contract2java.sh solidity \
  -p com.fisco.app.contract \
  -s contracts/solidity/ReceivableWithOverdue.sol \
  -o /home/llm_rca/fisco/my-bcos-app/src/main/java/com/fisco/app/contract
```

---

## 📝 部署后配置步骤

### 1. 更新应用配置

编辑 `src/main/resources/application.properties`，添加：

```properties
# ReceivableWithOverdue 合约地址
contracts.receivable-with-overdue.address=0x<新部署的合约地址>
```

### 2. 生成 Java 包装类

如果重新部署了合约，需要重新生成 Java 包装类：

```bash
cd /home/llm_rca/fisco/console/dist
bash contract2java.sh solidity \
  -p com.fisco.app.contract \
  -s contracts/solidity/ReceivableWithOverdue.sol \
  -o /home/llm_rca/fisco/my-bcos-app/src/main/java/com/fisco/app/contract
```

### 3. 更新代码中的警告注解

生成的 `ReceivableWithOverdue.java` 可能需要添加：

```java
@SuppressWarnings("rawtypes")
public class ReceivableWithOverdue extends Contract {
    // ...
}
```

### 4. 重启应用

```bash
cd /home/llm_rca/fisco/my-bcos-app
mvn clean package
mvn spring-boot:run
```

---

## ✅ 验证部署

### 1. 检查合约地址

```bash
# 在控制台中查询
bash console.sh << EOF
getDeployLog ReceivableWithOverdue
exit
EOF
```

### 2. 测试合约调用

```bash
# 测试催收记录上链
curl -X POST "http://localhost:8080/api/receivable/{id}/remind" \
  -H "Content-Type: application/json" \
  -d '{"remindType": "EMAIL", "remindContent": "测试"}'

# 测试罚息计算上链
curl -X POST "http://localhost:8080/api/receivable/{id}/penalty" \
  -H "Content-Type: application/json" \
  -d '{"penaltyType": "AUTO"}'

# 测试坏账认定上链
curl -X POST "http://localhost:8080/api/receivable/{id}/bad-debt" \
  -H "Content-Type: application/json" \
  -d '{"badDebtType": "OVERDUE_180", "badDebtReason": "测试"}'
```

### 3. 查看应用日志

```bash
tail -f logs/spring.log | grep "ReceivableWithOverdue"
```

预期输出：
```
ReceivableWithOverdue contract loaded successfully at: 0xc3eead5e05854e87d9e2952516ceafc70adf015b
```

---

## 🔧 常见问题

### Q1: 合约部署失败 - 账户余额不足

**解决:**
```bash
# 查询账户余额
getAccountBalance 0xYourAddress

# 如需测试币，使用控制台转账
sendAccountBalance 0xDeployerAddress 0xYourAddress 100000000
```

### Q2: Java 包装类编译错误

**解决:** 添加 `@SuppressWarnings("rawtypes")` 注解到类声明

### Q3: 合约调用失败

**检查:**
1. 确认合约地址配置正确
2. 确认区块链节点正在运行
3. 检查应用日志中的错误信息

---

## 📚 相关文档

- `INTEGRATION_GUIDE.md` - 集成指南
- `QUICK_START_CONTRACT.md` - 快速开始指南
- `CONTRACT_DEPLOYMENT.md` - 完整部署文档
- `DEPLOYMENT_COMPLETE.md` - 部署完成报告

---

**最后更新**: 2026-02-03
**维护状态**: ✅ 已部署并运行
