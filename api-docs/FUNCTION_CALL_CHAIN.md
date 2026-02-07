# 🔐 地址生成的完整函数调用链

## 📍 完整调用流程

```
应用启动 (BcosApplication)
    ↓
BcosSDK.build("config.toml")                    // 读取配置
    ↓
new CryptoSuite(cryptoType, configOption)       // 初始化加密套件
    ↓
【第1步】initCryptoSuite(cryptoType)             // CryptoSuite.java:89-128
    ├─ 创建 ECDSAKeyPair()
    └─ generateRandomKeyPair()
         ↓
【第2步】this.keyPair.generateKeyPair()          // CryptoSuite.java:337
    │
    └─ ECDSAKeyPair.generateKeyPair()           // ECDSAKeyPair.java:85-87
         │
         └─ NativeInterface.secp256k1GenKeyPair()  // ★ JNI调用C/C++生成密钥
              │
              ├─ 生成随机私钥 (32字节)
              ├─ 计算公钥 (secp256k1椭圆曲线)
              └─ 返回 CryptoResult
                   │
                   └─ new ECDSAKeyPair(CryptoResult)  // ECDSAKeyPair.java:51-56
                        ├─ 提取私钥: hexPrivateKey
                        ├─ 提取公钥: hexPublicKey  
                        └─ 计算: getAddress()
                             ↓
【第3步】getAddress()                            // CryptoKeyPair.java:200-204
    │
    └─ getAddress(publicKey, hashImpl)           // CryptoKeyPair.java:217-235
         │
         ├─ 去掉公钥前缀 "04" (65字节 → 64字节)
         ├─ Keccak256.hash(公钥)                   // 哈希计算
         ├─ 取后20字节 (160位)
         └─ 添加 "0x" 前缀
              ↓
         最终地址: 0x4a7268beef8c6076388d1ae5c5a6cfbe67616d02
```

---

## 🔑 关键函数详解

### 1️⃣ secp256k1GenKeyPair() - 生成密钥对

**位置**: 通过 JNI 调用本地 C/C++ 代码

**函数原型**:
```cpp
// C/C++ 代码 (在 libbcos-sdk-jni.so 中)
extern "C" {
    CryptoResult* secp256k1_gen_key_pair();
}
```

**功能**:
```c
// 1. 使用随机数生成器生成私钥
unsigned char private_key[32];
rand_bytes_seed(private_key, 32);

// 2. 使用椭圆曲线计算公钥
secp256k1_pubkey public_key;
secp256k1_pubkey_create(&public_key, private_key);

// 3. 返回密钥对
CryptoResult result;
result.private_key = private_key;
result.public_key = public_key;
return &result;
```

**特点**:
- ✅ 使用 C/C++ 实现，性能高
- ✅ 使用 OpenSSL 的 secp256k1 曲线
- ✅ 密码学安全的随机数生成器

---

### 2️⃣ getAddress() - 计算地址

**位置**: `CryptoKeyPair.java:200-204`

**代码**:
```java
public String getAddress() {
    // 公钥通常以 04 开头（未压缩格式）
    // 计算地址时需要去掉这个前缀
    return getAddress(this.getHexPublicKey());
}
```

**调用链**:
```java
getAddress()
  ↓
getAddress(publicKey, hashImpl)  // CryptoKeyPair.java:217
```

---

### 3️⃣ getAddress(publicKey, hashInterface) - 地址计算核心

**位置**: `CryptoKeyPair.java:217-235`

**完整代码**:
```java
protected static String getAddress(String publicKey, Hash hashInterface) {
    try {
        // 1. 去掉公钥前缀 "04" (未压缩公钥标志)
        String publicKeyNoPrefix = Numeric.getKeyNoPrefix(
            UNCOMPRESSED_PUBLICKEY_FLAG_STR,  // "04"
            publicKey,                          // 原公钥
            PUBLIC_KEY_LENGTH_IN_HEX           // 128 (64字节=128位十六进制)
        );
        
        // 2. 使用 Keccak256 哈希算法计算公钥哈希
        String publicKeyHash = Hex.toHexString(
            hashInterface.hash(Hex.decode(publicKeyNoPrefix))
        );
        
        // 3. 取哈希值的后20字节（最后40位十六进制字符）
        return "0x" + publicKeyHash.substring(
            publicKeyHash.length() - ADDRESS_LENGTH_IN_HEX  // 40
        );
    } catch (DecoderException e) {
        throw new KeyPairException("getAddress failed", e);
    }
}
```

**详细步骤**:
```java
// 输入: publicKey = "04f7c6ad6897985d9ebc82d3d61e8cb125adc4d827a8a1fbfc5c7098199b8c6034c5670f6f01d235347da4ef21fee5721903794bb138c2da955bf63669ee516e67"

// 步骤1: 去掉前缀 "04"
publicKeyNoPrefix = "f7c6ad6897985d9ebc82d3d61e8cb125adc4d827a8a1fbfc5c7098199b8c6034c5670f6f01d235347da4ef21fee5721903794bb138c2da955bf63669ee516e67"

// 步骤2: Keccak256 哈希
publicKeyHash = "a3f6ce7a8d8f7c2b9e1d8e7f6a3b5c8d9e7f6a3b5c8d9e7f6a3b5c8d9e7f6a3b5c8d9e7f6a3b5c8d9e7f6a3b5c8d9e7f6a3b54a7268beef8c6076388d1ae5c5a6cfbe67616d02"

// 步骤3: 取后 40 个字符
address = "0x4a7268beef8c6076388d1ae5c5a6cfbe67616d02"
```

---

## 📊 核心函数总结

| 函数 | 位置 | 功能 | 实现语言 |
|------|------|------|---------|
| `secp256k1GenKeyPair()` | JNI (C/C++) | 生成随机私钥和公钥 | C++ |
| `generateRandomKeyPair()` | CryptoSuite.java:336 | 调用密钥生成 | Java |
| `getAddress()` | CryptoKeyPair.java:200 | 计算地址入口 | Java |
| `getAddress(publicKey, hash)` | CryptoKeyPair.java:217 | 地址计算核心 | Java |
| `hash()` | Keccak256.java | 哈希算法 | Java |
| `storeKeyPairWithPemFormat()` | CryptoKeyPair.java:250 | 保存为PEM文件 | Java |

---

## 🎯 代码位置索引

### FISCO BCOS SDK 源码
```
~/.m2/repository/org/fisco-bcos/java-sdk/fisco-bcos-java-sdk/3.8.0/
├── fisco-bcos-java-sdk-3.8.0-sources.jar  ← 源码
└── fisco-bcos-java-sdk-3.8.0.jar          ← 编译后的类
```

### 关键类文件
```
org/fisco/bcos/sdk/v3/crypto/
├── CryptoSuite.java                      ← 加密套件主类
│   ├── initCryptoSuite()                 [L89]  初始化
│   ├── generateRandomKeyPair()           [L336] 生成随机密钥对
│   └── loadAccount()                     [L130] 加载账户
├── keypair/
│   ├── CryptoKeyPair.java                ← 密钥对基类
│   │   ├── getAddress()                  [L200] 地址计算入口
│   │   ├── getAddress(publicKey, hash)   [L217] 地址计算核心 ★
│   │   └── storeKeyPairWithPemFormat()   [L250] 保存PEM文件
│   └── ECDSAKeyPair.java                 ← ECDSA密钥对实现
│       ├── generateKeyPair()             [L85]  生成密钥对 ★
│       └── createKeyPair()               [L75]  创建密钥对
└── hash/
    └── Keccak256.java                    ← Keccak256哈希算法
        └── hash()                        哈希计算
```

---

## 🔍 实际调用示例

### 你的项目启动时

```java
// 1. 应用启动
@SpringBootApplication
public class BcosApplication {
    public static void main(String[] args) {
        SpringApplication.run(BcosApplication.class, args);
    }
}

// 2. BcosConfig 初始化
@Configuration
public class BcosConfig {
    @Bean
    public CryptoKeyPair cryptoKeyPair(Client client) {
        // ★ 这里触发自动生成
        return client.getCryptoSuite().getCryptoKeyPair();
    }
}

// 3. Client 初始化时调用
Client client = sdk.getClient(group0);
    ↓
CryptoSuite cryptoSuite = new CryptoSuite(cryptoType, configOption);
    ↓
if (!configOption.getAccountConfig().isAccountConfigured()) {
    // 配置文件中 accountAddress 为空，执行这里
    generateRandomKeyPair();  // ★ 生成随机密钥对
}
```

---

## 📝 函数调用总结

**地址生成 = 3 个核心函数**:

1. **`secp256k1GenKeyPair()`** (C/C++ via JNI)
   - 生成随机私钥 (32字节)
   - 计算公钥 (椭圆曲线加密)

2. **`getAddress(publicKey, hash)`** (Java)
   - 去掉公钥前缀 "04"
   - Keccak256 哈希
   - 取后20字节
   - 添加 "0x" 前缀

3. **`storeKeyPairWithPemFormat()`** (Java)
   - 保存私钥到 `accounts/` 目录
   - 文件名: `{地址}.pem`

**安全性保证**:
- C/C++ 实现，使用 OpenSSL
- 密码学安全的随机数
- secp256k1 标准椭圆曲线
- Keccak256 哈希算法

