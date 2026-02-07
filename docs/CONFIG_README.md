# FISCO BCOS 配置说明

## 配置文件位置

项目中的 FISCO BCOS SDK 配置文件位于：

```
src/main/resources/
├── config.toml          # FISCO BCOS SDK 配置文件
├── application.properties  # Spring Boot 应用配置
└── conf/                # 证书目录
    ├── ca.crt          # CA 证书
    ├── sdk.crt         # SDK 客户端证书
    └── sdk.key         # SDK 客户端私钥
```

## 配置文件说明

### 1. config.toml - SDK 连接配置

```toml
[cryptoMaterial]
certPath = "conf"              # 证书路径
disableSsl = "true"            # 禁用 SSL（根据节点配置）
useSMCrypto = "false"          # 使用非国密加密

[network]
messageTimeout = "10000"       # 消息超时时间（毫秒）
defaultGroup = "group0"        # 默认连接的群组
peers = ["127.0.0.1:20200", "127.0.0.1:20201"]  # 节点地址列表

[account]
keyStoreDir = "account"        # 账户文件存储目录
accountFileFormat = "pem"      # 账户文件格式
```

### 2. application.properties - 应用配置

```properties
# FISCO BCOS 配置
fisco.config-file=config.toml  # SDK 配置文件路径
fisco.group=group0              # 连接的群组

# 日志配置
logging.level.com.fisco.app=DEBUG
logging.level.org.fisco.bcos.sdk=INFO
```

## 配置类说明

### BcosConfig 类

位置：`src/main/java/com/fisco/app/config/BcosConfig.java`

功能：
- 初始化 FISCO BCOS SDK
- 创建并管理 Client Bean
- 配置区块链连接参数
- 提供优雅的资源清理

主要 Bean：
1. **BcosSDK**: SDK 实例，管理底层连接
2. **Client**: 客户端实例，用于与区块链交互

## 使用示例

### 在其他组件中注入 Client

```java
import org.fisco.bcos.sdk.v3.client.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MyService {

    @Autowired
    private Client client;

    public void someMethod() {
        // 使用 client 与区块链交互
        int blockNumber = client.getBlockNumber();
        System.out.println("Current block number: " + blockNumber);
    }
}
```

### 部署和调用合约

```java
import com.fisco.app.contract.HelloWorld;
import org.fisco.bcos.sdk.v3.client.Client;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class ContractService {

    @Autowired
    private Client client;

    // 部署合约
    public String deployHelloWorld() {
        HelloWorld helloWorld = HelloWorld.deploy(client, client.getCryptoKeyPair());
        return helloWorld.getContractAddress();
    }

    // 加载已部署的合约
    public HelloWorld loadHelloWorld(String address) {
        return HelloWorld.load(address, client, client.getCryptoKeyPair());
    }

    // 调用合约方法
    public void callContract(HelloWorld helloWorld) {
        // 读取数据（不发送交易）
        String value = helloWorld.get();

        // 写入数据（发送交易）
        TransactionReceipt receipt = helloWorld.set("New Value");
    }
}
```

## 连接状态检查

配置完成后，可以通过启动应用检查连接状态：

```bash
mvn spring-boot:run
```

查看日志输出：
- ✓ `FISCO BCOS SDK initialized successfully` - SDK 初始化成功
- ✓ `Connected to group: group0` - 成功连接到群组

## 常见问题

### 1. 连接失败
- 检查节点是否运行：`ps aux | grep fisco-bcos`
- 检查端口是否正确：默认 20200, 20201
- 检查 config.toml 中的 peers 配置

### 2. 证书错误
- 确认 `conf/` 目录下有正确的证书文件
- 检查证书是否与节点匹配
- disableSsl 设为 true 可跳过 SSL 验证

### 3. 群组不匹配
- 确认节点配置中的 group_id
- 修改 application.properties 中的 fisco.group

## 配置调整

### 切换到国密模式

```toml
[cryptoMaterial]
useSMCrypto = "true"
```

### 启用 SSL 连接

```toml
[cryptoMaterial]
disableSsl = "false"
```

### 添加更多节点

```toml
[network]
peers = [
    "127.0.0.1:20200",
    "127.0.0.1:20201",
    "127.0.0.1:20202"
]
```
