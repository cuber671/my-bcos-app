package com.fisco.app.Modules.Blockchain.Service.impl;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.crypto.keypair.CryptoKeyPair;
import org.fisco.bcos.sdk.v3.model.TransactionReceipt;
import org.fisco.bcos.sdk.v3.transaction.model.dto.CallResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fisco.app.Common.Config.BlockchainConfig;
import com.fisco.app.Modules.Blockchain.Service.BlockchainService;

/**
 * 区块链基础服务实现类
 *
 * 实现 BlockchainService 接口，提供统一的区块链操作能力
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Service
public class BlockchainServiceImpl implements BlockchainService {

    private static final Logger logger = LoggerFactory.getLogger(BlockchainServiceImpl.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired(required = false)
    private Client client;

    @Autowired(required = false)
    private CryptoKeyPair cryptoKeyPair;

    @Autowired
    private BlockchainConfig blockchainConfig;

    @Value("${fisco.enabled:true}")
    private boolean fiscoEnabled;

    // ==================== 基础查询 ====================

    @Override
    public BigInteger getBlockNumber() {
        checkEnabled();
        checkClient();
        try {
            Object result = client.getBlockNumber().getBlockNumber();
            if (result instanceof BigInteger) {
                return (BigInteger) result;
            } else if (result instanceof Number) {
                return BigInteger.valueOf(((Number) result).longValue());
            }
            return BigInteger.valueOf(Long.parseLong(result.toString()));
        } catch (Exception e) {
            logger.error("获取块高失败", e);
            throw new RuntimeException("获取块高失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String getBlockByNumber(BigInteger blockNumber) {
        checkEnabled();
        checkClient();

        // 验证块号
        if (blockNumber == null || blockNumber.compareTo(BigInteger.ZERO) < 0) {
            throw new IllegalArgumentException("块号不能为负数");
        }

        try {
            // 先获取当前块高，判断区块是否存在
            BigInteger currentBlockNumber = getBlockNumber();
            if (blockNumber.compareTo(currentBlockNumber) > 0) {
                return null;  // 区块不存在
            }

            var response = client.getBlockByNumber(blockNumber, true, false);
            return objectMapper.writeValueAsString(response.getResult());
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            // SDK 可能返回 "Block not found" 或类似错误
            if (errorMsg != null && (errorMsg.contains("not found") || errorMsg.contains("Not Found"))) {
                return null;
            }
            logger.error("获取区块信息失败, blockNumber={}", blockNumber, e);
            throw new RuntimeException("获取区块信息失败: " + errorMsg, e);
        }
    }

    @Override
    public String getBlockHashByNumber(BigInteger blockNumber) {
        checkEnabled();
        checkClient();
        try {
            var response = client.getBlockHashByNumber(blockNumber);
            return response.getResult();
        } catch (Exception e) {
            logger.error("获取区块哈希失败, blockNumber={}", blockNumber, e);
            throw new RuntimeException("获取区块哈希失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String getChainId() {
        checkEnabled();
        checkClient();
        try {
            // SDK 3.x: getChainId() 直接返回 String
            return client.getChainId();
        } catch (Exception e) {
            logger.error("获取链ID失败", e);
            throw new RuntimeException("获取链ID失败: " + e.getMessage(), e);
        }
    }

    // ==================== 交易操作 ====================

    @Override
    public TransactionReceipt getTransactionReceipt(String txHash) {
        checkEnabled();
        checkClient();

        // 验证 txHash 格式
        if (txHash == null || txHash.isEmpty()) {
            throw new IllegalArgumentException("交易哈希不能为空");
        }
        // 统一添加 0x 前缀
        if (!txHash.startsWith("0x")) {
            txHash = "0x" + txHash;
        }

        try {
            // SDK 3.x: getTransactionReceipt(String txHash, boolean bool)
            // 返回 BcosTransactionReceipt，转换为 TransactionReceipt 接口
            Object receipt = client.getTransactionReceipt(txHash, false);

            // 处理交易不存在的情况
            if (receipt == null) {
                return null;
            }

            return (TransactionReceipt) receipt;
        } catch (Exception e) {
            // SDK 3.x 在交易不存在时可能返回多种错误
            // 包括：callRemoteMethod failed, decode the message exception 等
            String errorMsg = e.getMessage();
            if (errorMsg != null && (
                errorMsg.contains("not found") ||
                errorMsg.contains("Not Found") ||
                errorMsg.contains("callRemoteMethod") ||
                errorMsg.contains("decode")
            )) {
                logger.warn("交易收据不存在或未确认, txHash={}, error={}", txHash, errorMsg);
                return null;  // 返回 null，让 Controller 返回 404
            }
            logger.error("获取交易收据失败, txHash={}", txHash, e);
            throw new RuntimeException("获取交易收据失败: " + errorMsg, e);
        }
    }

    @Override
    public TransactionReceipt sendRawTransaction(String to, String data) {
        checkEnabled();
        checkClient();
        checkCryptoKeyPair();
        try {
            // SDK 3.x: 使用 client.sendTransaction(String encodedData, boolean signed)
            // data 应该是已编码的交易数据
            Object receipt = client.sendTransaction(data, false);
            return (TransactionReceipt) receipt;
        } catch (Exception e) {
            logger.error("发送原始交易失败, to={}", to, e);
            throw new RuntimeException("发送原始交易失败: " + e.getMessage(), e);
        }
    }

    // ==================== 账户操作 ====================

    @Override
    public String getCurrentAccountAddress() {
        checkCryptoKeyPair();
        return cryptoKeyPair.getAddress();
    }

    @Override
    public String getBalance(String address) {
        checkEnabled();
        checkClient();

        // 验证地址格式
        if (address == null || address.isEmpty()) {
            throw new IllegalArgumentException("地址不能为空");
        }

        // 统一添加 0x 前缀
        if (!address.startsWith("0x")) {
            address = "0x" + address;
        }

        try {
            // SDK 3.x 不直接支持 getBalance
            // 可以通过调用预编译合约或返回说明
            logger.warn("SDK 3.x 不支持 getBalance 方法，请通过合约调用实现");
            return null;
        } catch (Exception e) {
            logger.error("查询余额失败, address={}", address, e);
            throw new RuntimeException("查询余额失败: " + e.getMessage(), e);
        }
    }

    // ==================== 合约操作 ====================

    @Override
    public CallResponse callContract(String contractAddress, String abi, String method, List<Object> params) {
        checkEnabled();
        checkClient();
        try {
            // SDK 3.x 不支持动态合约调用
            // 请使用生成的合约类（如 EnterpriseRegistryAuth）进行调用
            throw new UnsupportedOperationException(
                    "动态合约调用不支持，请使用生成的合约类（如 EnterpriseRegistryAuth）进行调用");
        } catch (UnsupportedOperationException e) {
            throw e;
        } catch (Exception e) {
            logger.error("合约调用失败, contract={}, method={}", contractAddress, method, e);
            throw new RuntimeException("合约调用失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Object sendContractTransaction(String contractAddress, String abi, String method, List<Object> params) {
        checkEnabled();
        checkClient();
        checkCryptoKeyPair();
        try {
            // SDK 3.x 不支持动态合约调用
            // 请使用生成的合约类（如 EnterpriseRegistryAuth）进行调用
            throw new UnsupportedOperationException(
                    "动态合约调用不支持，请使用生成的合约类（如 EnterpriseRegistryAuth）进行调用");
        } catch (UnsupportedOperationException e) {
            throw e;
        } catch (Exception e) {
            logger.error("合约交易失败, contract={}, method={}", contractAddress, method, e);
            throw new RuntimeException("合约交易失败: " + e.getMessage(), e);
        }
    }

    // ==================== 群组操作 ====================

    @Override
    public List<String> getGroupList() {
        checkEnabled();
        checkClient();
        try {
            // 获取群组列表需要通过 SDK 访问
            // 这里返回当前配置的群组
            List<String> groups = new ArrayList<>();
            groups.add(blockchainConfig.getGroup());
            return groups;
        } catch (Exception e) {
            logger.error("获取群组列表失败", e);
            throw new RuntimeException("获取群组列表失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String getGroupInfo() {
        checkEnabled();
        checkClient();
        try {
            var response = client.getGroupInfo();
            return objectMapper.writeValueAsString(response.getResult());
        } catch (Exception e) {
            logger.error("获取群组信息失败", e);
            throw new RuntimeException("获取群组信息失败: " + e.getMessage(), e);
        }
    }

    // ==================== 状态检查 ====================

    @Override
    public boolean isConnected() {
        if (!fiscoEnabled) {
            return false;
        }
        if (client == null) {
            return false;
        }
        try {
            client.getBlockNumber().getBlockNumber();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== 私有方法 ====================

    private void checkEnabled() {
        if (!fiscoEnabled) {
            throw new RuntimeException("FISCO BCOS 功能已禁用");
        }
    }

    private void checkClient() {
        if (client == null) {
            throw new RuntimeException("区块链 Client 未初始化，请检查 SDK 配置");
        }
    }

    private void checkCryptoKeyPair() {
        if (cryptoKeyPair == null) {
            throw new RuntimeException("加密密钥对未初始化，请检查 SDK 配置");
        }
    }

}
