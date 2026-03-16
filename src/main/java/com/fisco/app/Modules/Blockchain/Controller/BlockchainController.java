package com.fisco.app.Modules.Blockchain.Controller;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fisco.bcos.sdk.v3.model.TransactionReceipt;
import org.fisco.bcos.sdk.v3.transaction.model.dto.CallResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fisco.app.Common.Annotation.RequireRole;
import com.fisco.app.Common.Utils.Result;
import com.fisco.app.Modules.Blockchain.Service.BlockchainService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * 区块链基础操作 Controller
 *
 * 提供区块链网络的基础操作 API，包括：
 * - 区块链状态查询
 * - 区块/交易信息查询
 * - 账户余额查询
 * - 合约调用
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Api(tags = "区块链基础服务")
@RestController
@RequestMapping("/api/v1/blockchain")
public class BlockchainController {

    private static final Logger logger = LoggerFactory.getLogger(BlockchainController.class);

    @Autowired
    private BlockchainService blockchainService;

    @Value("${fisco.enabled:true}")
    private boolean fiscoEnabled;

    // ==================== 状态查询 ====================

    @ApiOperation("获取区块链状态")
    @GetMapping("/status")
    public Result<Map<String, Object>> getStatus() {
        Map<String, Object> result = new HashMap<>();

        result.put("enabled", fiscoEnabled);
        result.put("connected", blockchainService.isConnected());

        if (blockchainService.isConnected()) {
            try {
                result.put("blockNumber", blockchainService.getBlockNumber());
                result.put("chainId", blockchainService.getChainId());
                result.put("group", blockchainService.getGroupList());
                result.put("accountAddress", blockchainService.getCurrentAccountAddress());
            } catch (Exception e) {
                logger.error("获取区块链状态失败", e);
                result.put("error", e.getMessage());
            }
        }

        return Result.success(result);
    }

    @ApiOperation("健康检查")
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", blockchainService.isConnected() ? "UP" : "DOWN");
        return Result.success(result);
    }

    // ==================== 区块查询 ====================

    @ApiOperation("获取当前块高")
    @GetMapping("/blockNumber")
    public Result<BigInteger> getBlockNumber() {
        try {
            BigInteger blockNumber = blockchainService.getBlockNumber();
            return Result.success(blockNumber);
        } catch (Exception e) {
            logger.error("获取块高失败", e);
            return Result.error(500, "操作失败，请稍后重试");
        }
    }

    @ApiOperation("根据块号获取区块信息")
    @GetMapping("/block/{blockNumber}")
    public Result<String> getBlock(
            @ApiParam(value = "块号", required = true)
            @PathVariable BigInteger blockNumber) {
        try {
            String blockInfo = blockchainService.getBlockByNumber(blockNumber);
            if (blockInfo == null) {
                return Result.error(404, "区块不存在: " + blockNumber);
            }
            return Result.success(blockInfo);
        } catch (IllegalArgumentException e) {
            return Result.error(400, "请求参数无效");
        } catch (Exception e) {
            logger.error("获取区块信息失败, blockNumber={}", blockNumber, e);
            return Result.error(500, "操作失败，请稍后重试");
        }
    }

    @ApiOperation("根据块号获取区块哈希")
    @GetMapping("/blockHash/{blockNumber}")
    public Result<String> getBlockHash(
            @ApiParam(value = "块号", required = true)
            @PathVariable BigInteger blockNumber) {
        try {
            String blockHash = blockchainService.getBlockHashByNumber(blockNumber);
            return Result.success(blockHash);
        } catch (Exception e) {
            logger.error("获取区块哈希失败, blockNumber={}", blockNumber, e);
            return Result.error(500, "操作失败，请稍后重试");
        }
    }

    // ==================== 交易查询 ====================

    @ApiOperation("根据交易哈希获取交易收据")
    @GetMapping("/receipt/{txHash}")
    public Result<TransactionReceipt> getTransactionReceipt(
            @ApiParam(value = "交易哈希", required = true)
            @PathVariable String txHash) {
        try {
            TransactionReceipt receipt = blockchainService.getTransactionReceipt(txHash);
            if (receipt == null) {
                return Result.error(404, "交易收据不存在或交易未确认: " + txHash);
            }
            return Result.success(receipt);
        } catch (IllegalArgumentException e) {
            return Result.error(400, "请求参数无效");
        } catch (Exception e) {
            logger.error("获取交易收据失败, txHash={}", txHash, e);
            return Result.error(500, "操作失败，请稍后重试");
        }
    }

    // ==================== 账户操作 ====================

    @ApiOperation("获取当前系统账户地址")
    @GetMapping("/account")
    public Result<String> getCurrentAccount() {
        try {
            String address = blockchainService.getCurrentAccountAddress();
            return Result.success(address);
        } catch (Exception e) {
            logger.error("获取当前账户地址失败", e);
            return Result.error(500, "操作失败，请稍后重试");
        }
    }

    @ApiOperation("查询账户余额")
    @GetMapping("/balance/{address}")
    public Result<String> getBalance(
            @ApiParam(value = "账户地址", required = true)
            @PathVariable String address) {
        try {
            // 统一处理地址格式
            if (!address.startsWith("0x")) {
                address = "0x" + address;
            }
            String balance = blockchainService.getBalance(address);
            if (balance == null) {
                // SDK 3.x 不支持，返回友好提示
                return Result.success("SDK 3.x 不支持余额查询，请使用合约调用");
            }
            return Result.success(balance);
        } catch (IllegalArgumentException e) {
            return Result.error(400, "请求参数无效");
        } catch (Exception e) {
            logger.error("查询余额失败, address={}", address, e);
            return Result.error(500, "操作失败，请稍后重试");
        }
    }

    // ==================== 合约操作 ====================

    @ApiOperation("调用合约只读方法")
    @PostMapping("/call")
    public Result<CallResponse> callContract(
            @ApiParam(value = "合约调用信息", required = true) @RequestBody ContractCallRequest request) {
        try {
            if (request.getContractAddress() == null || request.getAbi() == null
                    || request.getMethod() == null) {
                return Result.error(400, "缺少必要参数: contractAddress, abi, method");
            }

            List<Object> params = request.getParams();
            if (params == null) {
                params = List.of();
            }

            CallResponse response = blockchainService.callContract(
                    request.getContractAddress(),
                    request.getAbi(),
                    request.getMethod(),
                    params);

            return Result.success(response);
        } catch (Exception e) {
            logger.error("合约调用失败", e);
            return Result.error(500, "操作失败，请稍后重试");
        }
    }

    @ApiOperation("发送合约交易")
    @RequireRole(value = {"ADMIN"}, adminBypass = true)
    @PostMapping("/transaction")
    public Result<Object> sendTransaction(
            @ApiParam(value = "合约交易信息", required = true) @RequestBody ContractCallRequest request) {
        try {
            if (request.getContractAddress() == null || request.getAbi() == null
                    || request.getMethod() == null) {
                return Result.error(400, "缺少必要参数: contractAddress, abi, method");
            }

            List<Object> params = request.getParams();
            if (params == null) {
                params = List.of();
            }

            Object response = blockchainService.sendContractTransaction(
                    request.getContractAddress(),
                    request.getAbi(),
                    request.getMethod(),
                    params);

            return Result.success(response);
        } catch (Exception e) {
            logger.error("发送合约交易失败", e);
            return Result.error(500, "操作失败，请稍后重试");
        }
    }

    // ==================== 群组操作 ====================

    @ApiOperation("获取群组信息")
    @GetMapping("/group")
    public Result<String> getGroupInfo() {
        try {
            String groupInfo = blockchainService.getGroupInfo();
            return Result.success(groupInfo);
        } catch (Exception e) {
            logger.error("获取群组信息失败", e);
            return Result.error(500, "操作失败，请稍后重试");
        }
    }

    @ApiOperation("获取群组列表")
    @GetMapping("/groups")
    public Result<List<String>> getGroupList() {
        try {
            List<String> groups = blockchainService.getGroupList();
            return Result.success(groups);
        } catch (Exception e) {
            logger.error("获取群组列表失败", e);
            return Result.error(500, "操作失败，请稍后重试");
        }
    }

    // ==================== 请求对象 ====================

    /**
     * 合约调用请求对象
     */
    public static class ContractCallRequest {
        private String contractAddress;
        private String abi;
        private String method;
        private List<Object> params;

        public String getContractAddress() {
            return contractAddress;
        }

        public void setContractAddress(String contractAddress) {
            this.contractAddress = contractAddress;
        }

        public String getAbi() {
            return abi;
        }

        public void setAbi(String abi) {
            this.abi = abi;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public List<Object> getParams() {
            return params;
        }

        public void setParams(List<Object> params) {
            this.params = params;
        }
    }
}
