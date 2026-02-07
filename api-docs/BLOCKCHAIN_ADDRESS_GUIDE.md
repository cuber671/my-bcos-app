# 🔑 FISCO BCOS 区块链地址获取指南

## 📍 地址是什么？

在 FISCO BCOS 中，区块链地址是：
- **格式**：0x 开头的 40 位十六进制字符（共 42 位）
- **示例**：`0x4a7268beef8c6076388d1ae5c5a6cfbe67616d02`
- **作用**：标识企业或用户在区块链上的身份
- **唯一性**：每个地址对应唯一的私钥/公钥对

---

## 🎯 获取地址的 4 种方式

### 方式 1: 使用控制台工具生成（推荐）

#### 使用 get_account.sh 脚本
```bash
cd /home/llm_rca/fisco/console
./tools/get_account.sh
```

**输出示例**：
```
[INFO] Account privateHex: 0x9e158b742dcd4792f8e67ff1408999e8df8de47d2bd33e771dcdf34da53d2785
[INFO] Account publicHex : 0xf7c6ad6897985d9ebc82d3d61e8cb125adc4d827a8a1fbfc5c7098199b8c6034c5670f6f01d235347da4ef21fee5721903794bb138c2da955bf63669ee516e67
[INFO] Account Address   : 0x4a7268beef8c6076388d1ae5c5a6cfbe67616d02
[INFO] Private Key (pem) : accounts/0x4a7268beef8c6076388d1ae5c5a6cfbe67616d02.pem
[INFO] Public  Key (pem) : accounts/0x4a7268beef8c6076388d1ae5c5a6cfbe67616d02.public.pem
```

**特点**：
✅ 自动生成私钥/公钥对  
✅ 保存为 PEM 文件（方便后续使用）  
✅ 安全可靠（由官方工具生成）  

---

### 方式 2: 通过 API 接口查看应用账户

如果你的应用已启动，可以通过 API 查看应用当前的区块链账户地址：

```bash
# 方法 1: 使用 curl（如果无需认证）
curl http://localhost:8080/api/account

# 方法 2: 如果需要认证，先获取 Token
# 登录获取 Token
curl -X POST http://localhost:8080/api/auth/login-enterprise \
  -H "Content-Type: application/json" \
  -d '{
    "address": "已注册的企业地址",
    "password": "密码"
  }'

# 使用 Token 访问账户信息
curl http://localhost:8080/api/account \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**返回示例**：
```json
{
  "status": "success",
  "address": "0x4a7268beef8c6076388d1ae5c5a6cfbe67616d02",
  "publicKey": "0xf7c6ad6897985d9ebc82d3d61e8cb125adc4d827a8a1fbfc5c7098199b8c6034c5670f6f01d235347da4ef21fee5721903794bb138c2da955bf63669ee516e67",
  "cryptoType": "ECDSA"
}
```

---

### 方式 3: 使用控制台命令

#### 启动控制台
```bash
cd /home/llm_rca/fisco/console
bash start.sh
```

#### 在控制台中执行命令
```bash
# 创建新账户
newAccount

# 查看当前账户
getAccount

# 查看账户余额
getBalance 地址
```

---

### 方式 4: 代码生成（开发中使用）

如果你是开发者，可以在代码中动态生成地址：

```java
import org.fisco.bcos.sdk.v3.crypto.keypair.CryptoKeyPair;
import org.fisco.bcos.sdk.v3.crypto.CryptoSuite;

// 创建密钥对
CryptoSuite cryptoSuite = new CryptoSuite(CryptoType.ECDSA_TYPE);
CryptoKeyPair keyPair = cryptoSuite.createKeyPair();

// 获取地址
String address = keyPair.getAddress();
System.out.println("地址: " + address);
```

---

## 🔐 地址与私钥的关系

### 重要概念
- **一个地址** = **一对密钥**（私钥 + 公钥）
- **私钥**：必须保密，用于签名交易
- **公钥**：可以公开，用于验证签名
- **地址**：由公钥生成，对外展示

### 安全建议
⚠️ **私钥安全**：
- 私钥一旦丢失，无法找回地址控制权
- 不要将私钥提交到代码仓库
- 生产环境使用加密存储
- 定期备份私钥文件

---

## 📝 企业注册流程中的地址使用

### 步骤 1: 生成区块链地址
```bash
cd /home/llm_rca/fisco/console
./tools/get_account.sh
```

### 步骤 2: 记录地址信息
脚本会输出：
- **Account Address**：`0x4a7268beef8c6076388d1ae5c5a6cfbe67616d02` ← 这个用于注册
- **Private Key (pem)**：保存位置 ← 后续签名交易需要

### 步骤 3: 使用地址注册企业
```bash
curl -X POST http://localhost:8080/api/enterprise/register \
  -H "Content-Type: application/json" \
  -d '{
    "address": "0x4a7268beef8c6076388d1ae5c5a6cfbe67616d02",
    "name": "测试企业",
    "creditCode": "91110000MA001234XY",
    "role": "SUPPLIER",
    "initialPassword": "Test@123456"
  }'
```

### 步骤 4: 妥善保管私钥文件
```bash
# 私钥文件位置
/home/llm_rca/fisco/console/accounts/0x4a7268beef8c6076388d1ae5c5a6cfbe67616d02.pem

# 复制到安全位置
cp /home/llm_rca/fisco/console/accounts/*.pem /your/secure/location/
```

---

## 🛠️ 常见问题

### Q1: 地址可以重复使用吗？
**A**: 
- ✅ 可以：一个企业/用户使用固定地址
- ❌ 不建议：为了安全，建议每个实体独立地址

### Q2: 地址生成后可以修改吗？
**A**: 
- ❌ 不能：地址由私钥决定，一旦生成不可更改
- ✅ 可以：创建新地址并迁移数据

### Q3: 如何验证地址格式是否正确？
**A**: 
```bash
# 正则表达式验证
^0x[a-fA-F0-9]{40}$

# 示例检查
echo "0x4a7268beef8c6076388d1ae5c5a6cfbe67616d02" | grep -E "^0x[a-fA-F0-9]{40}$"
```

### Q4: 私钥丢失了怎么办？
**A**: 
- ⚠️ 无法找回：私钥无法恢复
- 🔄 需要重新注册：使用新生成的地址重新注册企业

### Q5: 应用启动时如何配置地址？
**A**: 
在 `config.toml` 中配置：
```toml
[account]
keyStoreDir = "account"           # 账户文件目录
accountFileFormat = "pem"         # 文件格式
# accountAddress = ""             # 留空则自动生成随机账户
```

---

## 📊 地址格式对照表

| 项目 | 格式 | 示例 |
|------|------|------|
| **区块链地址** | 0x + 40位十六进制 | `0x4a7268beef8c6076388d1ae5c5a6cfbe67616d02` |
| **私钥（Hex）** | 0x + 64位十六进制 | `0x9e158b742dcd4792f8e67ff1408999e8df8de47d2bd33e771dcdf34da53d2785` |
| **公钥（Hex）** | 0x + 128位十六进制 | `0xf7c6ad6897985d9ebc82d3d61e8cb125adc4d827a8a1fbfc5c7098199b8c6034...` |
| **交易哈希** | 0x + 64位十六进制 | `0xabc123...` |
| **合约地址** | 0x + 40位十六进制 | `0xd24180cc0fef2f3e545de4f9aafc09345cd08903` |

---

## 🚀 快速生成地址的命令

### 一键生成（推荐）
```bash
cd /home/llm_rca/fisco/console && ./tools/get_account.sh
```

### 生成并保存到指定目录
```bash
cd /home/llm_rca/fisco/console
./tools/get_account.sh | tee generated_account.txt
```

### 批量生成多个地址
```bash
for i in {1..5}; do
  echo "=== 生成第 $i 个地址 ==="
  ./tools/get_account.sh
  echo ""
done
```

---

**建议**：对于企业注册，建议使用控制台工具生成地址，这样私钥文件会自动保存，方便后续管理。

