package com.fisco.app.Modules.Test.Controller;

import java.util.HashMap;
import java.util.Map;

import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.crypto.keypair.CryptoKeyPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fisco.app.Common.Config.BlockchainConfig;
import com.fisco.app.Common.Utils.Result;

/**
 * 区块链连接测试 Controller
 * 提供 /api/v1/test/blockchain 接口用于验证 FISCO BCOS 连接
 */
@RestController
@RequestMapping("/api/v1/test")
public class BlockchainTestController {

    private static final Logger logger = LoggerFactory.getLogger(BlockchainTestController.class);

    @Autowired(required = false)
    private Client client;

    @Autowired(required = false)
    private CryptoKeyPair cryptoKeyPair;

    @Autowired
    private BlockchainConfig blockchainConfig;

    @Value("${fisco.enabled:true}")
    private boolean fiscoEnabled;

    /**
     * 区块链连接状态测试接口
     *
     * 测试步骤：
     * 1. 检查 fisco.enabled 配置
     * 2. 检查 Client 是否为空
     * 3. 测试获取块高
     * 4. 检查账户地址
     *
     * 调用示例：
     * curl http://localhost:8080/api/v1/test/blockchain
     */
    @GetMapping("/blockchain")
    public Result<Map<String, Object>> testBlockchain() {
        Map<String, Object> result = new HashMap<>();

        // 1. 检查功能开关
        result.put("enabled", fiscoEnabled);
        result.put("configEnabled", blockchainConfig.isEnabled());

        if (!fiscoEnabled) {
            result.put("status", "DISABLED");
            result.put("message", "FISCO BCOS 功能已禁用");
            return Result.success(result);
        }

        // 2. 检查 Client
        if (client == null) {
            result.put("status", "ERROR");
            result.put("client", "null");
            result.put("message", "Client bean 未初始化，请检查 SDK 配置");
            return Result.success(result);
        }
        result.put("client", "initialized");

        // 3. 测试连接 - 获取块高
        try {
            // SDK 3.x 使用 getBlockNumber().getBlockNumber()
            Object blockNumberObj = client.getBlockNumber().getBlockNumber();
            result.put("blockNumber", blockNumberObj);
            result.put("connected", true);
            result.put("status", "CONNECTED");
            result.put("message", "区块链连接成功");
        } catch (Exception e) {
            logger.error("获取块高失败", e);
            result.put("connected", false);
            result.put("status", "ERROR");
            result.put("blockNumber", null);
            result.put("error", e.getMessage());
            result.put("message", "区块链连接失败: " + e.getMessage());
        }

        // 4. 检查账户地址
        if (cryptoKeyPair != null) {
            result.put("accountAddress", cryptoKeyPair.getAddress());
            result.put("cryptoKeyPair", "initialized");
        } else {
            result.put("accountAddress", null);
            result.put("cryptoKeyPair", "null");
        }

        // 5. 群组信息
        result.put("group", blockchainConfig.getGroup());

        return Result.success(result);
    }

    /**
     * 简单健康检查 - 只检查基本连接
     */
    @GetMapping("/blockchain/health")
    public Result<Map<String, Object>> blockchainHealth() {
        Map<String, Object> result = new HashMap<>();

        if (!fiscoEnabled || client == null) {
            result.put("status", "DOWN");
            return Result.success(result);
        }

        try {
            // SDK API 已变更，暂不支持块高查询
            result.put("status", "DOWN");
            result.put("message", "SDK 未初始化");
        } catch (Exception e) {
            result.put("status", "DOWN");
            result.put("error", e.getMessage());
        }

        return Result.success(result);
    }
}
