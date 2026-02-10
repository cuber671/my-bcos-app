package com.fisco.app.controller.blockchain;

import com.fisco.app.dto.blockchain.*;
import com.fisco.app.service.blockchain.TransactionService;
import com.fisco.app.vo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 交易管理控制器
 * 提供区块链交易相关接口
 *
 * @author FISCO BCOS Team
 * @since 2026-02-10
 */
@Slf4j
@RestController
@RequestMapping("/api/blockchain/transaction")
@RequiredArgsConstructor
@Api(tags = "TransactionManagement")
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * 查询交易详情
     * GET /api/blockchain/transaction/{hash}
     */
    @GetMapping("/{hash}")
    @ApiOperation(value = "查询交易详情",
        notes = "根据交易哈希查询交易详细信息。" +
                "参数说明：" +
                "- hash: 交易哈希，66位十六进制字符串（0x开头）；" +
                "返回：交易详情，包括发送者、接收者、值、Gas、区块号、状态等。" +
                "【公开接口】无需认证即可访问。")
    @ApiResponses({
        @ApiResponse(code = 200, message = "查询成功", response = TransactionDetailDTO.class),
        @ApiResponse(code = 400, message = "交易哈希格式错误"),
        @ApiResponse(code = 404, message = "交易不存在"),
        @ApiResponse(code = 500, message = "区块链查询失败")
    })
    public Result<TransactionDetailDTO> getTransaction(
            @ApiParam(value = "交易哈希", required = true, example = "0xabc123...")
            @PathVariable String hash) {
        try {
            TransactionDetailDTO detail = transactionService.getTransactionByHash(hash);
            return Result.success("查询成功", detail);
        } catch (Exception e) {
            log.error("Failed to get transaction: hash={}", hash, e);
            return Result.error("查询交易失败: " + e.getMessage());
        }
    }

    /**
     * 查询交易池
     * GET /api/blockchain/transaction/pool
     */
    @GetMapping("/pool")
    @ApiOperation(value = "查询交易池",
        notes = "获取本地待处理的交易列表。" +
                "注意事项：" +
                "- FISCO BCOS不提供直接访问链上交易池的RPC接口；" +
                "- 本接口返回的是本地数据库中记录的待处理交易；" +
                "- 仅包含通过本系统提交的交易。" +
                "【公开接口】无需认证即可访问。")
    @ApiResponses({
        @ApiResponse(code = 200, message = "查询成功", response = TransactionDTO.class, responseContainer = "List"),
        @ApiResponse(code = 500, message = "服务器内部错误")
    })
    public Result<List<TransactionDTO>> getTransactionPool() {
        try {
            List<TransactionDTO> pool = transactionService.getTransactionPool();
            return Result.success("查询成功", pool);
        } catch (Exception e) {
            log.error("Failed to get transaction pool", e);
            return Result.error("查询交易池失败: " + e.getMessage());
        }
    }

    /**
     * 查询账户待处理交易
     * GET /api/blockchain/transaction/pending/{address}
     */
    @GetMapping("/pending/{address}")
    @ApiOperation(value = "查询账户待处理交易",
        notes = "查询指定地址的待处理交易列表。" +
                "参数说明：" +
                "- address: 账户地址，42位十六进制字符串（0x开头）；" +
                "返回：该地址待处理的交易列表（仅包含通过本系统提交的交易）。" +
                "【公开接口】无需认证即可访问。")
    @ApiResponses({
        @ApiResponse(code = 200, message = "查询成功", response = TransactionDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "地址格式错误"),
        @ApiResponse(code = 500, message = "服务器内部错误")
    })
    public Result<List<TransactionDTO>> getPendingTransactions(
            @ApiParam(value = "账户地址", required = true, example = "0x123...")
            @PathVariable String address) {
        try {
            List<TransactionDTO> transactions =
                transactionService.getPendingTransactionsByAddress(address);
            return Result.success("查询成功", transactions);
        } catch (Exception e) {
            log.error("Failed to get pending transactions for address: {}", address, e);
            return Result.error("查询待处理交易失败: " + e.getMessage());
        }
    }

    /**
     * 取消交易
     * POST /api/blockchain/transaction/cancel
     */
    @PostMapping("/cancel")
    @ApiOperation(value = "取消交易",
        notes = "取消待处理的交易。" +
                "业务规则：" +
                "- 只能取消状态为PENDING（待处理）的交易；" +
                "- 已确认或已打包的交易不能取消；" +
                "- 取消操作仅从本地数据库中移除交易记录，不会从链上交易池移除；" +
                "- 如果交易已被矿工打包，取消操作不影响链上执行。" +
                "【公开接口】无需认证即可访问。")
    @ApiResponses({
        @ApiResponse(code = 200, message = "取消成功"),
        @ApiResponse(code = 400, message = "请求参数错误或交易状态不允许取消"),
        @ApiResponse(code = 404, message = "交易不存在"),
        @ApiResponse(code = 409, message = "交易已确认，无法取消"),
        @ApiResponse(code = 500, message = "服务器内部错误")
    })
    public Result<Void> cancelTransaction(
            @Valid @RequestBody CancelTransactionRequestDTO request) {
        try {
            transactionService.cancelTransaction(request);
            return Result.success();
        } catch (Exception e) {
            log.error("Failed to cancel transaction: hash={}",
                     request.getTransactionHash(), e);
            return Result.error("取消交易失败: " + e.getMessage());
        }
    }

    /**
     * 查询交易回执
     * GET /api/blockchain/transaction/receipt/{hash}
     */
    @GetMapping("/receipt/{hash}")
    @ApiOperation(value = "查询交易回执",
        notes = "根据交易哈希查询交易回执信息。" +
                "参数说明：" +
                "- hash: 交易哈希，66位十六进制字符串（0x开头）；" +
                "返回：交易回执，包含状态、Gas使用量、日志、事件等。" +
                "注意事项：" +
                "- 只有已打包的交易才有回执；" +
                "- 待处理的交易查询回执会返回404。" +
                "【公开接口】无需认证即可访问。")
    @ApiResponses({
        @ApiResponse(code = 200, message = "查询成功", response = TransactionReceiptDTO.class),
        @ApiResponse(code = 400, message = "交易哈希格式错误"),
        @ApiResponse(code = 404, message = "交易回执不存在（交易未打包）"),
        @ApiResponse(code = 500, message = "区块链查询失败")
    })
    public Result<TransactionReceiptDTO> getTransactionReceipt(
            @ApiParam(value = "交易哈希", required = true, example = "0xabc123...")
            @PathVariable String hash) {
        try {
            TransactionReceiptDTO receipt = transactionService.getTransactionReceipt(hash);
            return Result.success("查询成功", receipt);
        } catch (Exception e) {
            log.error("Failed to get transaction receipt: hash={}", hash, e);
            return Result.error("查询交易回执失败: " + e.getMessage());
        }
    }

    /**
     * 估算Gas
     * POST /api/blockchain/transaction/estimate-gas
     */
    @PostMapping("/estimate-gas")
    @ApiOperation(value = "估算Gas消耗",
        notes = "估算交易的Gas消耗量。" +
                "参数说明：" +
                "- toAddress: 接收地址（合约地址或账户地址）；" +
                "- value: 交易值（单位：wei），默认为0；" +
                "- data: 交易输入数据（合约调用参数或附加数据）。" +
                "返回：估算的Gas限制值。" +
                "注意事项：" +
                "- 本接口使用简化算法，实际Gas消耗可能不同；" +
                "- 建议在估算值基础上增加10%-20%作为实际Gas限制；" +
                "- 仅用于估算，不执行交易。" +
                "【公开接口】无需认证即可访问。")
    @ApiResponses({
        @ApiResponse(code = 200, message = "Gas估算成功", response = Long.class),
        @ApiResponse(code = 400, message = "请求参数错误（地址格式不正确）"),
        @ApiResponse(code = 500, message = "Gas估算失败")
    })
    public Result<Long> estimateGas(
            @Valid @RequestBody EstimateGasRequestDTO request) {
        try {
            Long gasLimit = transactionService.estimateGas(request);
            return Result.success("Gas估算成功", gasLimit);
        } catch (Exception e) {
            log.error("Failed to estimate gas", e);
            return Result.error("Gas估算失败: " + e.getMessage());
        }
    }
}
