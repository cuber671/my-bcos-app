package com.fisco.app.controller.blockchain;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.client.protocol.response.Code;
import org.fisco.bcos.sdk.v3.crypto.keypair.CryptoKeyPair;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fisco.app.contract.bill.BillV2;
import com.fisco.app.contract.receivable.ReceivableV2;
import com.fisco.app.contract.warehouse.WarehouseReceiptV2;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;


/**
 * 合约状态查询Controller
 * 提供区块链上各类合约的状态查询功能
 *
 * @author FISCO BCOS
 * @since 2025-01-22
 */
@Api(tags = "BlockchainManagement")
@RestController
@RequestMapping("/api/admin/blockchain/contracts")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "fisco.enabled", havingValue = "true", matchIfMissing = true)
public class ContractStatusController {

    private final Client client;
    private final CryptoKeyPair cryptoKeyPair;

    // 合约地址
    private static final String BILL_ADDRESS = "0xd24180cc0fef2f3e545de4f9aafc09345cd08903";
    private static final String RECEIVABLE_ADDRESS = "0x37a44585bf1e9618fdb4c62c4c96189a07dd4b48";
    private static final String WAREHOUSE_RECEIPT_ADDRESS = "0x31ed5233b81c79d5adddeef991f531a9bbc2ad01";

    /**
     * 查询所有合约状态概览
     */
    @ApiOperation(value = "查询所有合约状态概览", notes = "获取区块链和所有业务合约的概览信息")
    @GetMapping("/overview")
    public Map<String, Object> getContractOverview() {
        Map<String, Object> result = new HashMap<>();

        try {
            // 获取区块链信息
            BigInteger blockNumber = client.getBlockNumber().getBlockNumber();
            result.put("blockNumber", blockNumber.toString());
            result.put("accountAddress", cryptoKeyPair.getAddress());
            result.put("chainId", client.getChainId());
            result.put("group", "group0");

            // 合约地址
            Map<String, String> contracts = new HashMap<>();
            contracts.put("Bill", BILL_ADDRESS);
            contracts.put("Receivable", RECEIVABLE_ADDRESS);
            contracts.put("WarehouseReceipt", WAREHOUSE_RECEIPT_ADDRESS);
            result.put("contracts", contracts);

            result.put("status", "success");
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "查询失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 查询Bill合约状态
     */
    @ApiOperation(value = "查询Bill合约状态", notes = "查询区块链上Bill合约的详细状态信息")
    @GetMapping("/bill")
    public Map<String, Object> getBillContractStatus() {
        Map<String, Object> result = new HashMap<>();

        try {
            BillV2.load(BILL_ADDRESS, client, cryptoKeyPair);

            // V2合约没有admin和billCount方法，仅返回基本信息
            result.put("contractAddress", BILL_ADDRESS);
            result.put("status", "success");
            result.put("note", "V2 contract - use BillService to query detailed information");

        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "查询Bill合约失败: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
        }

        return result;
    }

    /**
     * 查询Receivable合约状态
     */
    @ApiOperation(value = "查询Receivable合约状态", notes = "查询区块链上Receivable合约的详细状态信息")
    @GetMapping("/receivable")
    public Map<String, Object> getReceivableContractStatus() {
        Map<String, Object> result = new HashMap<>();

        try {
            ReceivableV2.load(RECEIVABLE_ADDRESS, client, cryptoKeyPair);

            // V2合约没有admin和receivableCount方法，仅返回基本信息
            result.put("contractAddress", RECEIVABLE_ADDRESS);
            result.put("status", "success");
            result.put("note", "V2 contract - use ReceivableService to query detailed information");

        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "查询Receivable合约失败: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
        }

        return result;
    }

    /**
     * 查询WarehouseReceipt合约状态
     */
    @ApiOperation(value = "查询WarehouseReceipt合约状态", notes = "查询区块链上WarehouseReceipt合约的详细状态信息")
    @GetMapping("/warehouse-receipt")
    public Map<String, Object> getWarehouseReceiptContractStatus() {
        Map<String, Object> result = new HashMap<>();

        try {
            WarehouseReceiptV2.load(WAREHOUSE_RECEIPT_ADDRESS, client, cryptoKeyPair);

            // V2合约没有admin和receiptCount方法，仅返回基本信息
            result.put("contractAddress", WAREHOUSE_RECEIPT_ADDRESS);
            result.put("status", "success");
            result.put("note", "V2 contract - use WarehouseReceiptService to query detailed information");

        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "查询WarehouseReceipt合约失败: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
        }

        return result;
    }

    /**
     * 查询合约代码
     */
    @ApiOperation(value = "查询合约代码", notes = "根据合约地址查询合约的部署代码")
    @GetMapping("/code/{address}")
    public Map<String, Object> getContractCode(
            @io.swagger.annotations.ApiParam(value = "合约地址", required = true, example = "0x1234567890abcdef1234567890abcdef12345678")
            @PathVariable String address) {
        Map<String, Object> result = new HashMap<>();

        try {
            Code codeResponse = client.getCode(address);
            String code = codeResponse.getCode();
            result.put("code", code);
            result.put("codeLength", code != null ? code.length() : 0);
            result.put("status", "success");
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "查询合约代码失败: " + e.getMessage());
        }

        return result;
    }
}
