package com.fisco.app.controller;

import com.fisco.app.contract.HelloWorld;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.crypto.keypair.CryptoKeyPair;
import org.fisco.bcos.sdk.v3.model.TransactionReceipt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * FISCO BCOS 基础交互控制器
 * 提供区块链基础操作接口，包括健康检查、区块查询、合约交互等
 *
 * @author FISCO BCOS
 * @since 2025-01-22
 */
@RestController
@RequestMapping("/api/blockchain")
@Api(tags = "BlockchainManagement")
public class BcosController {

    private static final Logger logger = LoggerFactory.getLogger(BcosController.class);

    @Autowired
    private Client client;

    @Autowired
    private CryptoKeyPair cryptoKeyPair;

    /**
     * 健康检查 - 检查区块链连接状态
     * GET /api/blockchain/health
     *
     * 注意：此为公开接口，无需Token即可访问
     */
    @GetMapping("/health")
    @ApiOperation(value = "区块链健康检查（公开接口）",
                  notes = "【公开接口】检查区块链连接状态和基本信息")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        try {
            Object blockNumber = client.getBlockNumber();

            result.put("status", "success");
            result.put("connected", true);
            result.put("blockNumber", blockNumber.toString());
            result.put("accountAddress", cryptoKeyPair.getAddress());
            result.put("message", "区块链连接正常");

            logger.info("Health check: blockNumber={}, account={}", blockNumber, cryptoKeyPair.getAddress());
        } catch (Exception e) {
            result.put("status", "error");
            result.put("connected", false);
            result.put("message", "区块链连接失败: " + e.getMessage());
            logger.error("Health check failed", e);
        }
        return result;
    }

    /**
     * 获取区块信息
     * GET /api/blockchain/block/{number}
     *
     * 注意：此为公开接口，无需Token即可访问
     */
    @GetMapping("/block/{number}")
    @ApiOperation(value = "获取指定区块信息（公开接口）",
                  notes = "【公开接口】根据区块号获取区块详细信息")
    public Map<String, Object> getBlockByNumber(
            @ApiParam(value = "区块号", required = true, example = "100")
            @PathVariable String number) {
        Map<String, Object> result = new HashMap<>();
        try {
            BigInteger blockNumber = new BigInteger(number);
            client.getBlockByNumber(blockNumber, false, false);

            // 简化实现，直接返回区块号
            result.put("status", "success");
            result.put("blockNumber", number);
            result.put("message", "区块查询功能");
            logger.info("Get block: number={}", number);
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "获取区块异常: " + e.getMessage());
            logger.error("Get block failed", e);
        }
        return result;
    }

    /**
     * 获取最新区块号
     * GET /api/blockchain/block/latest
     *
     * 注意：此为公开接口，无需Token即可访问
     */
    @GetMapping("/block/latest")
    @ApiOperation(value = "获取最新区块号（公开接口）",
                  notes = "【公开接口】获取区块链当前最新区块号")
    public Map<String, Object> getLatestBlockNumber() {
        Map<String, Object> result = new HashMap<>();
        try {
            Object blockNumber = client.getBlockNumber();
            result.put("status", "success");
            result.put("blockNumber", blockNumber.toString());
            logger.info("Get latest block number: {}", blockNumber);
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "获取区块号失败: " + e.getMessage());
            logger.error("Get latest block number failed", e);
        }
        return result;
    }

    /**
     * 部署 HelloWorld 合约
     * POST /api/blockchain/contract/deploy
     */
    @PostMapping("/contract/deploy")
    @ApiOperation(value = "部署HelloWorld合约", notes = "部署一个HelloWorld测试合约到区块链")
    public Map<String, Object> deployHelloWorld() {
        Map<String, Object> result = new HashMap<>();
        try {
            logger.info("Deploying HelloWorld contract...");
            HelloWorld helloWorld = HelloWorld.deploy(client, cryptoKeyPair);

            String contractAddress = helloWorld.getContractAddress();
            result.put("status", "success");
            result.put("contractAddress", contractAddress);
            result.put("message", "合约部署成功");

            logger.info("Contract deployed successfully: address={}", contractAddress);
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "合约部署失败: " + e.getMessage());
            logger.error("Deploy contract failed", e);
        }
        return result;
    }

    /**
     * 调用 HelloWorld 合约的 get 方法（读取数据）
     * GET /api/blockchain/contract/{address}/get
     */
    @GetMapping("/contract/{address}/get")
    @ApiOperation(value = "读取合约数据", notes = "调用HelloWorld合约的get方法读取数据")
    public Map<String, Object> callHelloWorldGet(
            @ApiParam(value = "合约地址", required = true, example = "0x1234567890abcdef1234567890abcdef12345678")
            @PathVariable String address) {
        Map<String, Object> result = new HashMap<>();
        try {
            HelloWorld helloWorld = HelloWorld.load(address, client, cryptoKeyPair);
            String value = helloWorld.get();

            result.put("status", "success");
            result.put("value", value);
            result.put("contractAddress", address);
            result.put("message", "读取成功");

            logger.info("Called contract get: address={}, value={}", address, value);
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "调用合约失败: " + e.getMessage());
            logger.error("Call contract get failed", e);
        }
        return result;
    }

    /**
     * 调用 HelloWorld 合约的 set 方法（写入数据）
     * POST /api/blockchain/contract/{address}/set
     * Body: {"value": "新值"}
     */
    @PostMapping("/contract/{address}/set")
    @ApiOperation(value = "写入合约数据", notes = "调用HelloWorld合约的set方法写入数据")
    public Map<String, Object> callHelloWorldSet(
            @ApiParam(value = "合约地址", required = true, example = "0x1234567890abcdef1234567890abcdef12345678")
            @PathVariable String address,
            @RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();
        try {
            String value = request.get("value");
            if (value == null || value.isEmpty()) {
                result.put("status", "error");
                result.put("message", "value 参数不能为空");
                return result;
            }

            HelloWorld helloWorld = HelloWorld.load(address, client, cryptoKeyPair);
            TransactionReceipt receipt = helloWorld.set(value);

            result.put("status", "success");
            result.put("contractAddress", address);
            result.put("newValue", value);
            result.put("message", "设置成功");
            result.put("transactionHash", receipt.getTransactionHash());
            result.put("blockNumber", receipt.getBlockNumber());
            result.put("receiptStatus", receipt.getStatus());

            logger.info("Called contract set: address={}, value={}, txHash={}",
                address, value, receipt.getTransactionHash());
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "调用合约失败: " + e.getMessage());
            logger.error("Call contract set failed", e);
        }
        return result;
    }

    /**
     * 获取账户信息
     * GET /api/blockchain/account
     *
     * 注意：此为公开接口，无需Token即可访问
     */
    @GetMapping("/account")
    @ApiOperation(value = "获取账户信息（公开接口）",
                  notes = "【公开接口】获取当前区块链账户的地址和公钥信息")
    public Map<String, Object> getAccountInfo() {
        Map<String, Object> result = new HashMap<>();
        try {
            result.put("status", "success");
            result.put("address", cryptoKeyPair.getAddress());
            result.put("publicKey", cryptoKeyPair.getHexPublicKey());
            result.put("cryptoType", client.getCryptoSuite().getCryptoTypeConfig());

            logger.info("Get account info: address={}", cryptoKeyPair.getAddress());
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "获取账户信息失败: " + e.getMessage());
            logger.error("Get account info failed", e);
        }
        return result;
    }

    /**
     * 获取节点列表
     * GET /api/blockchain/nodes
     *
     * 注意：此为公开接口，无需Token即可访问
     */
    @GetMapping("/nodes")
    @ApiOperation(value = "获取节点列表（公开接口）",
                  notes = "【公开接口】获取区块链网络中的节点信息")
    public Map<String, Object> getPeers() {
        Map<String, Object> result = new HashMap<>();
        try {
            Object peers = client.getPeers();
            result.put("status", "success");
            result.put("peers", peers.toString());
            logger.info("Get peers info");
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "获取节点列表失败: " + e.getMessage());
            logger.error("Get peers failed", e);
        }
        return result;
    }

    /**
     * 根路径 - API 欢迎页面
     * GET /api/blockchain/
     */
    @GetMapping("/")
    @ApiOperation(value = "区块链API首页", notes = "显示所有可用的区块链API端点")
    public Map<String, Object> index() {
        Map<String, Object> result = new HashMap<>();
        result.put("name", "FISCO BCOS REST API");
        result.put("version", "1.0");
        result.put("description", "FISCO BCOS 区块链交互接口");

        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("GET /api/blockchain/health", "健康检查");
        endpoints.put("GET /api/blockchain/block/latest", "获取最新区块号");
        endpoints.put("GET /api/blockchain/block/{number}", "获取指定区块信息");
        endpoints.put("GET /api/blockchain/account", "获取账户信息");
        endpoints.put("GET /api/blockchain/nodes", "获取节点列表");
        endpoints.put("POST /api/blockchain/contract/deploy", "部署 HelloWorld 合约");
        endpoints.put("GET /api/blockchain/contract/{address}/get", "调用合约 get 方法");
        endpoints.put("POST /api/blockchain/contract/{address}/set", "调用合约 set 方法");

        result.put("endpoints", endpoints);
        return result;
    }

    @GetMapping("/block")
    @ApiOperation(value = "获取当前块高", notes = "快速获取区块链当前高度")
    public String getBlock() {
        return "当前最新块高: " + client.getBlockNumber().getBlockNumber();
    }
}
