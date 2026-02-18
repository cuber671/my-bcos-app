package com.fisco.app.controller.blockchain;

import com.fisco.app.util.ContractDeployer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 智能合约部署Controller
 * 提供业务智能合约的部署功能
 *
 * @author FISCO BCOS
 * @since 2025-01-22
 */
@Api(tags = "BlockchainManagement")
@ConditionalOnProperty(name = "fisco.enabled", havingValue = "true", matchIfMissing = true)
@RestController
@RequestMapping("/api/admin/blockchain/deploy")
@RequiredArgsConstructor
public class DeployController {

    private final ContractDeployer contractDeployer;

    /**
     * 部署Bill合约
     */
    @ApiOperation(value = "部署Bill合约", notes = "部署Bill票据管理合约到区块链")
    @ApiResponses({
        @ApiResponse(code = 200, message = "合约部署成功"),
        @ApiResponse(code = 500, message = "合约部署失败")
    })
    @PostMapping("/bill")
    public ResponseEntity<Map<String, Object>> deployBill() {
        ContractDeployer.DeployResult result = contractDeployer.deployBill();
        return ResponseEntity.ok(toMap("Bill", result));
    }

    /**
     * 部署Receivable合约
     */
    @ApiOperation(value = "部署Receivable合约", notes = "部署Receivable应收账款管理合约到区块链")
    @ApiResponses({
        @ApiResponse(code = 200, message = "合约部署成功"),
        @ApiResponse(code = 500, message = "合约部署失败")
    })
    @PostMapping("/receivable")
    public ResponseEntity<Map<String, Object>> deployReceivable() {
        ContractDeployer.DeployResult result = contractDeployer.deployReceivable();
        return ResponseEntity.ok(toMap("Receivable", result));
    }

    /**
     * 部署WarehouseReceipt合约
     */
    @ApiOperation(value = "部署WarehouseReceipt合约", notes = "部署WarehouseReceipt仓单管理合约到区块链")
    @ApiResponses({
        @ApiResponse(code = 200, message = "合约部署成功"),
        @ApiResponse(code = 500, message = "合约部署失败")
    })
    @PostMapping("/warehouse-receipt")
    public ResponseEntity<Map<String, Object>> deployWarehouseReceipt() {
        ContractDeployer.DeployResult result = contractDeployer.deployWarehouseReceipt();
        return ResponseEntity.ok(toMap("WarehouseReceipt", result));
    }

    /**
     * 部署EnterpriseRegistry合约
     */
    @ApiOperation(value = "部署EnterpriseRegistry合约", notes = "部署EnterpriseRegistry企业注册合约到区块链")
    @ApiResponses({
        @ApiResponse(code = 200, message = "合约部署成功"),
        @ApiResponse(code = 500, message = "合约部署失败")
    })
    @PostMapping("/enterprise-registry")
    public ResponseEntity<Map<String, Object>> deployEnterpriseRegistry() {
        ContractDeployer.DeployResult result = contractDeployer.deployEnterpriseRegistry();
        return ResponseEntity.ok(toMap("EnterpriseRegistry", result));
    }

    /**
     * 部署所有合约
     */
    @ApiOperation(value = "部署所有合约", notes = "一次性部署所有业务合约到区块链")
    @ApiResponses({
        @ApiResponse(code = 200, message = "所有合约部署成功"),
        @ApiResponse(code = 500, message = "合约部署失败")
    })
    @PostMapping("/all")
    public ResponseEntity<Map<String, String>> deployAll() {
        try {
            contractDeployer.deployAll();
            return ResponseEntity.ok(Map.of(
                "message", "所有合约部署完成！请查看日志获取合约地址。"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "message", "部署失败: " + e.getMessage()
            ));
        }
    }

    private Map<String, Object> toMap(String contractName, ContractDeployer.DeployResult result) {
        Map<String, Object> map = new HashMap<>();
        map.put("contract", contractName);
        map.put("contractAddress", result.contractAddress);
        map.put("transactionHash", result.transactionHash);
        map.put("gasUsed", result.gasUsed);
        map.put("success", true);
        return map;
    }
}
