package com.fisco.app.controller.blockchain;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fisco.app.dto.blockchain.*;
import com.fisco.app.service.blockchain.BlockService;
import com.fisco.app.vo.Result;
import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.crypto.keypair.CryptoKeyPair;
import org.fisco.bcos.sdk.v3.model.TransactionReceipt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// import com.fisco.app.contract.HelloWorld; // HelloWorld contract not available in V2

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

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

    // 错误消息常量
    private static final String ERROR_INVALID_BLOCK_FORMAT = "区块号格式错误";
    private static final String ERROR_BLOCK_NUMBER_OUT_OF_RANGE = "区块号无效或超出范围";
    private static final String ERROR_BLOCKCHAIN_QUERY_FAILED = "区块链查询失败";

    @Autowired
    private Client client;

    @Autowired
    private CryptoKeyPair cryptoKeyPair;

    @Autowired
    private BlockService blockService;

    /**
     * 验证区块号是否有效
     * @param blockNumber 区块号
     * @return true 如果区块号有效，否则 false
     */
    private boolean isValidBlockNumber(BigInteger blockNumber) {
        // 检查是否为负数
        if (blockNumber.compareTo(BigInteger.ZERO) < 0) {
            return false;
        }

        try {
            // 检查是否超出当前最新区块号
            BigInteger latestBlock = blockService.getLatestBlockNumber();
            return blockNumber.compareTo(latestBlock) <= 0;
        } catch (Exception e) {
            logger.warn("Failed to validate block number range: {}", blockNumber, e);
            return false;
        }
    }

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
            // 移除敏感信息：账户地址和公钥
            result.put("message", "区块链连接正常");

            logger.info("Health check: blockNumber={}", blockNumber);
        } catch (Exception e) {
            result.put("status", "error");
            result.put("connected", false);
            result.put("message", "区块链连接失败: " + e.getMessage());
            logger.error("Health check failed", e);
        }
        return result;
    }

    /**
     * 获取区块信息（增强版）
     * GET /api/blockchain/block/{number}
     *
     * 注意：此为公开接口，无需Token即可访问
     */
    @GetMapping("/block/{number}")
    @ApiOperation(value = "获取指定区块信息（公开接口）",
                  notes = "【公开接口】根据区块号获取区块详细信息，包含交易列表、状态根、交易根等完整信息")
    public Result<BlockDTO> getBlockByNumber(
            @ApiParam(value = "区块号", required = true, example = "100")
            @PathVariable String number) {
        try {
            BigInteger blockNumber = new BigInteger(number);

            // 验证区块号范围
            if (!isValidBlockNumber(blockNumber)) {
                logger.warn("Block number out of range: {}", number);
                return Result.error(ERROR_BLOCK_NUMBER_OUT_OF_RANGE);
            }

            BlockDTO block = blockService.getBlockByNumber(blockNumber);

            logger.info("Get block: number={}, txCount={}", number, block.getTransactionCount());
            return Result.success("区块查询成功", block);
        } catch (NumberFormatException e) {
            logger.error("Invalid block number format: {}", number);
            return Result.paramError(ERROR_INVALID_BLOCK_FORMAT);
        } catch (Exception e) {
            logger.error("Get block failed: number={}", number, e);
            return Result.error(ERROR_BLOCKCHAIN_QUERY_FAILED);
        }
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
     * 获取区块中的所有交易详情
     * GET /api/blockchain/block/{blockNumber}/transactions
     *
     * 注意：此为公开接口，无需Token即可访问
     */
    @GetMapping("/block/{blockNumber}/transactions")
    @ApiOperation(value = "获取区块交易列表（公开接口）",
                  notes = "【公开接口】获取指定区块中的所有交易详情")
    public Result<List<TransactionDTO>> getBlockTransactions(
            @ApiParam(value = "区块号", required = true, example = "100")
            @PathVariable String blockNumber) {
        try {
            BigInteger number = new BigInteger(blockNumber);

            // 验证区块号范围
            if (!isValidBlockNumber(number)) {
                logger.warn("Block number out of range: {}", blockNumber);
                return Result.error(ERROR_BLOCK_NUMBER_OUT_OF_RANGE);
            }

            List<TransactionDTO> transactions = blockService.getBlockTransactions(number);

            logger.info("Get block transactions: blockNumber={}, count={}", blockNumber, transactions.size());
            return Result.success("区块交易列表查询成功", transactions);
        } catch (NumberFormatException e) {
            logger.error("Invalid block number format: {}", blockNumber);
            return Result.paramError(ERROR_INVALID_BLOCK_FORMAT);
        } catch (Exception e) {
            logger.error("Get block transactions failed: blockNumber={}", blockNumber, e);
            return Result.error(ERROR_BLOCKCHAIN_QUERY_FAILED);
        }
    }

    /**
     * 验证区块完整性
     * GET /api/blockchain/block/validate/{blockNumber}
     *
     * 注意：此为公开接口，无需Token即可访问
     */
    @GetMapping("/block/validate/{blockNumber}")
    @ApiOperation(value = "验证区块完整性（公开接口）",
                  notes = "【公开接口】验证区块的哈希链完整性、交易根、状态根等")
    public Result<BlockValidationResponse> validateBlock(
            @ApiParam(value = "区块号", required = true, example = "100")
            @PathVariable String blockNumber) {
        try {
            BigInteger number = new BigInteger(blockNumber);

            // 验证区块号范围
            if (!isValidBlockNumber(number)) {
                logger.warn("Block number out of range: {}", blockNumber);
                return Result.error(ERROR_BLOCK_NUMBER_OUT_OF_RANGE);
            }

            BlockValidationResponse response = blockService.validateBlock(number);

            logger.info("Validate block: blockNumber={}, isValid={}", blockNumber, response.getIsValid());
            return Result.success("区块验证完成", response);
        } catch (NumberFormatException e) {
            logger.error("Invalid block number format: {}", blockNumber);
            return Result.paramError(ERROR_INVALID_BLOCK_FORMAT);
        } catch (Exception e) {
            logger.error("Validate block failed: blockNumber={}", blockNumber, e);
            return Result.error(ERROR_BLOCKCHAIN_QUERY_FAILED);
        }
    }

    /**
     * 获取区块链统计信息
     * GET /api/blockchain/block/statistics
     *
     * 注意：此为公开接口，无需Token即可访问
     */
    @GetMapping("/block/statistics")
    @ApiOperation(value = "获取区块链统计信息（公开接口）",
                  notes = "【公开接口】获取区块链网络的统计信息，包含TPS、出块时间、Gas使用率等（缓存30秒）")
    public Result<BlockStatisticsDTO> getBlockchainStatistics() {
        try {
            BlockStatisticsDTO stats = blockService.getBlockchainStatistics();

            logger.info("Get blockchain statistics: latestBlock={}, tps={}",
                stats.getLatestBlockNumber(), stats.getTransactionsPerSecond());
            return Result.success("区块链统计信息查询成功", stats);
        } catch (Exception e) {
            logger.error("Get blockchain statistics failed", e);
            return Result.error("获取区块链统计信息失败: " + e.getMessage());
        }
    }

    /**
     * 部署 HelloWorld 合约
     * POST /api/blockchain/contract/deploy
     * NOTE: HelloWorld contract not available in V2, temporarily disabled
     */
    /*@PostMapping("/contract/deploy")*/
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
    //@GetMapping("/contract/{address}/get")
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
    //@PostMapping("/contract/{address}/set")
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
     * 获取账户信息（仅地址，已移除敏感信息）
     * GET /api/blockchain/account
     *
     * 注意：此为公开接口，无需Token即可访问
     * 安全警告：已移除公钥和加密类型信息，防止信息泄露
     */
    @GetMapping("/account")
    @ApiOperation(value = "获取账户信息（公开接口）",
                  notes = "【公开接口】获取当前区块链账户地址（已移除敏感信息）")
    public Map<String, Object> getAccountInfo() {
        Map<String, Object> result = new HashMap<>();
        try {
            result.put("status", "success");
            // 仅返回地址，移除公钥和加密类型等敏感信息
            result.put("address", cryptoKeyPair.getAddress());

            logger.info("Get account info: address={}", cryptoKeyPair.getAddress());
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "获取账户信息失败: " + e.getMessage());
            logger.error("Get account info failed", e);
        }
        return result;
    }

    /**
     * 获取节点列表（已过滤敏感信息）
     * GET /api/blockchain/nodes
     *
     * 注意：此为公开接口，无需Token即可访问
     * 安全警告：已移除节点 IP 和端口信息，防止信息泄露
     */
    @GetMapping("/nodes")
    @ApiOperation(value = "获取节点列表（公开接口）",
                  notes = "【公开接口】获取区块链网络中的节点数量（已移除 IP 和端口等敏感信息）")
    public Map<String, Object> getPeers() {
        Map<String, Object> result = new HashMap<>();
        try {
            org.fisco.bcos.sdk.v3.client.protocol.response.Peers peers = client.getPeers();
            result.put("status", "success");

            // 仅返回节点数量，不返回 IP 和端口等敏感信息
            int nodeCount = 0;
            if (peers != null && peers.getPeers() != null) {
                java.util.List<org.fisco.bcos.sdk.v3.client.protocol.response.Peers.PeerInfo> peerList =
                    peers.getPeers().getPeers();
                nodeCount = peerList != null ? peerList.size() : 0;
            }

            result.put("nodeCount", nodeCount);
            logger.info("Get peers info: nodeCount={}", nodeCount);
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "获取节点列表失败");
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
        endpoints.put("GET /api/blockchain/block/{number}", "获取指定区块信息（增强版）");
        endpoints.put("GET /api/blockchain/block/{blockNumber}/transactions", "获取区块交易列表");
        endpoints.put("GET /api/blockchain/block/validate/{blockNumber}", "验证区块完整性");
        endpoints.put("GET /api/blockchain/block/statistics", "获取区块链统计信息");
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
