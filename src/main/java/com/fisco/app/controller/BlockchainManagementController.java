package com.fisco.app.controller;

import com.fisco.app.entity.Enterprise;
import com.fisco.app.service.ContractService;
import com.fisco.app.service.EnterpriseService;
import com.fisco.app.vo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 区块链管理相关接口
 * 提供区块链数据查询、验证等功能
 *
 * @author FISCO BCOS
 * @since 2025-01-22
 */
@Slf4j
@ConditionalOnProperty(name = "fisco.enabled", havingValue = "true", matchIfMissing = true)
@RestController
@RequestMapping("/api/admin/blockchain")
@Api(tags = "BlockchainManagement")
@Tag(name = "BlockchainManagement", description = "区块链管理相关接口")
public class BlockchainManagementController {

    @Autowired
    private ContractService contractService;

    @Autowired
    private EnterpriseService enterpriseService;

    /**
     * 查询所有已上链的企业
     * 从数据库查询状态为ACTIVE的企业，并验证其在区块链上的存在性
     *
     * GET /api/admin/blockchain/enterprises
     *
     * @return 已上链企业列表（含区块链验证状态）
     */
    @GetMapping("/enterprises")
    @ApiOperation(value = "查询已上链企业", notes = "从数据库查询已激活企业，并验证区块链状态")
    @ApiResponses({
        @ApiResponse(code = 200, message = "查询成功"),
        @ApiResponse(code = 500, message = "查询失败")
    })
    public Result<List<Map<String, Object>>> getOnChainEnterprises() {
        log.info("==================== 查询已上链企业开始 ====================");

        try {
            // 从数据库查询所有ACTIVE状态的企业
            List<Enterprise> activeEnterprises = enterpriseService.getAllEnterprisesWithoutPagination();
            log.info("数据库中ACTIVE状态企业数量: {}", activeEnterprises.size());

            List<Map<String, Object>> result = new ArrayList<>();

            // 验证每个企业在区块链上的状态
            for (Enterprise enterprise : activeEnterprises) {
                Map<String, Object> enterpriseInfo = new HashMap<>();
                enterpriseInfo.put("id", enterprise.getId());
                enterpriseInfo.put("address", enterprise.getAddress());
                enterpriseInfo.put("name", enterprise.getName());
                enterpriseInfo.put("creditCode", enterprise.getCreditCode());
                enterpriseInfo.put("role", enterprise.getRole());
                enterpriseInfo.put("status", enterprise.getStatus());
                enterpriseInfo.put("registeredAt", enterprise.getRegisteredAt());

                // 查询区块链验证企业是否存在
                try {
                    var chainEnterprise = contractService.getEnterpriseFromChain(enterprise.getAddress());
                    boolean existsOnChain = chainEnterprise.exists;
                    enterpriseInfo.put("onChain", existsOnChain);
                    enterpriseInfo.put("verified", true);

                    if (existsOnChain) {
                        enterpriseInfo.put("chainName", chainEnterprise.name);
                        enterpriseInfo.put("chainCreditCode", chainEnterprise.creditCode);
                        enterpriseInfo.put("chainRole", chainEnterprise.role);
                        enterpriseInfo.put("chainStatus", chainEnterprise.status);
                        log.info("✓ 企业已上链: address={}, name={}", enterprise.getAddress(), enterprise.getName());
                    } else {
                        log.warn("✗ 企业未上链: address={}, name={}", enterprise.getAddress(), enterprise.getName());
                    }

                } catch (Exception e) {
                    enterpriseInfo.put("onChain", false);
                    enterpriseInfo.put("verified", false);
                    enterpriseInfo.put("error", e.getMessage());
                    log.warn("区块链查询失败: address={}, error={}", enterprise.getAddress(), e.getMessage());
                }

                result.add(enterpriseInfo);
            }

            log.info("==================== 查询已上链企业完成 ====================");
            return Result.success("查询成功", result);

        } catch (Exception e) {
            log.error("查询已上链企业失败", e);
            return Result.error(500, "查询失败: " + e.getMessage());
        }
    }

    /**
     * 分页查询已上链企业
     *
     * GET /api/admin/blockchain/enterprises/page
     *
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 分页结果
     */
    @GetMapping("/enterprises/page")
    @ApiOperation(value = "分页查询已上链企业", notes = "分页查询已激活企业并验证区块链状态")
    @ApiResponses({
        @ApiResponse(code = 200, message = "查询成功"),
        @ApiResponse(code = 500, message = "查询失败")
    })
    public Result<Page<Map<String, Object>>> getOnChainEnterprisesPage(
            @ApiParam(value = "页码", example = "0") @RequestParam(defaultValue = "0") int page,
            @ApiParam(value = "每页大小", example = "10") @RequestParam(defaultValue = "10") int size) {

        log.info("分页查询已上链企业: page={}, size={}", page, size);

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Enterprise> activeEnterprises = enterpriseService.getEnterprisesByStatus(
                Enterprise.EnterpriseStatus.ACTIVE, pageable
            );

            log.info("数据库中ACTIVE状态企业数量: {}", activeEnterprises.getTotalElements());

            // 转换为带区块链验证状态的Map
            Page<Map<String, Object>> result = activeEnterprises.map(enterprise -> {
                Map<String, Object> enterpriseInfo = new HashMap<>();
                enterpriseInfo.put("id", enterprise.getId());
                enterpriseInfo.put("address", enterprise.getAddress());
                enterpriseInfo.put("name", enterprise.getName());
                enterpriseInfo.put("creditCode", enterprise.getCreditCode());
                enterpriseInfo.put("role", enterprise.getRole());
                enterpriseInfo.put("status", enterprise.getStatus());
                enterpriseInfo.put("registeredAt", enterprise.getRegisteredAt());

                // 验证区块链状态
                try {
                    var chainEnterprise = contractService.getEnterpriseFromChain(enterprise.getAddress());
                    enterpriseInfo.put("onChain", chainEnterprise.exists);
                    enterpriseInfo.put("verified", true);

                    if (chainEnterprise.exists) {
                        enterpriseInfo.put("chainName", chainEnterprise.name);
                        enterpriseInfo.put("chainStatus", chainEnterprise.status);
                    }

                } catch (Exception e) {
                    enterpriseInfo.put("onChain", false);
                    enterpriseInfo.put("verified", false);
                    enterpriseInfo.put("error", e.getMessage());
                }

                return enterpriseInfo;
            });

            return Result.success("查询成功", result);

        } catch (Exception e) {
            log.error("分页查询已上链企业失败", e);
            return Result.error(500, "查询失败: " + e.getMessage());
        }
    }

    /**
     * 查询区块链上的企业总数
     *
     * GET /api/admin/blockchain/enterprises/count
     *
     * @return 区块链上的企业总数
     */
    @GetMapping("/enterprises/count")
    @ApiOperation(value = "查询区块链企业总数", notes = "从智能合约获取已注册企业总数")
    @ApiResponses({
        @ApiResponse(code = 200, message = "查询成功"),
        @ApiResponse(code = 500, message = "查询失败")
    })
    public Result<Map<String, Object>> getBlockchainEnterpriseCount() {
        log.info("查询区块链上的企业总数");

        try {
            long chainCount = contractService.getActiveEnterpriseCountFromChain();

            Map<String, Object> result = new HashMap<>();
            result.put("blockchainCount", chainCount);

            // 同时返回数据库中ACTIVE企业的数量用于对比
            long dbCount = enterpriseService.countByStatus(Enterprise.EnterpriseStatus.ACTIVE);
            result.put("databaseActiveCount", dbCount);

            log.info("区块链企业总数: {}, 数据库ACTIVE企业数: {}", chainCount, dbCount);

            return Result.success("查询成功", result);

        } catch (Exception e) {
            log.error("查询区块链企业总数失败", e);
            return Result.error(500, "查询失败: " + e.getMessage());
        }
    }

    /**
     * 验证特定企业是否已上链
     *
     * GET /api/admin/blockchain/enterprises/verify/{address}
     *
     * @param address 企业区块链地址
     * @return 验证结果
     */
    @GetMapping("/enterprises/verify/{address}")
    @ApiOperation(value = "验证企业上链状态", notes = "验证指定地址的企业是否已注册到区块链")
    @ApiResponses({
        @ApiResponse(code = 200, message = "验证完成"),
        @ApiResponse(code = 500, message = "验证失败")
    })
    public Result<Map<String, Object>> verifyEnterpriseOnChain(
            @ApiParam(value = "企业区块链地址", required = true, example = "0x1234567890abcdef1234567890abcdef12345678")
            @PathVariable String address) {

        log.info("验证企业上链状态: address={}", address);

        try {
            // 从数据库查询企业
            Enterprise dbEnterprise = enterpriseService.getEnterpriseByAddress(address);

            // 从区块链查询企业
            var chainEnterprise = contractService.getEnterpriseFromChain(address);

            Map<String, Object> result = new HashMap<>();
            result.put("address", address);
            result.put("existsOnChain", chainEnterprise.exists);
            result.put("databaseStatus", dbEnterprise.getStatus());
            result.put("databaseName", dbEnterprise.getName());

            if (chainEnterprise.exists) {
                result.put("chainName", chainEnterprise.name);
                result.put("chainCreditCode", chainEnterprise.creditCode);
                result.put("chainRole", chainEnterprise.role);
                result.put("chainStatus", chainEnterprise.status);
                result.put("chainCreditRating", chainEnterprise.creditRating);
                result.put("chainCreditLimit", chainEnterprise.creditLimit);
                result.put("registeredAt", chainEnterprise.registeredAt);
                result.put("updatedAt", chainEnterprise.updatedAt);

                log.info("✓ 企业已上链: address={}, name={}", address, chainEnterprise.name);
            } else {
                log.warn("✗ 企业未上链: address={}", address);
            }

            return Result.success("验证完成", result);

        } catch (Exception e) {
            log.error("验证企业上链状态失败: address={}", address, e);
            return Result.error(500, "验证失败: " + e.getMessage());
        }
    }

    /**
     * 获取区块链统计信息
     *
     * GET /api/admin/blockchain/statistics
     *
     * @return 统计信息
     */
    @GetMapping("/statistics")
    @ApiOperation(value = "获取区块链统计信息", notes = "获取区块链和数据库的企业统计对比")
    @ApiResponses({
        @ApiResponse(code = 200, message = "统计成功"),
        @ApiResponse(code = 500, message = "统计失败")
    })
    public Result<Map<String, Object>> getBlockchainStatistics() {
        log.info("获取区块链统计信息");

        try {
            Map<String, Object> stats = new HashMap<>();

            // 区块链统计
            long chainCount = contractService.getActiveEnterpriseCountFromChain();
            stats.put("blockchainTotalCount", chainCount);

            // 数据库统计
            long dbActiveCount = enterpriseService.countByStatus(Enterprise.EnterpriseStatus.ACTIVE);
            long dbPendingCount = enterpriseService.countByStatus(Enterprise.EnterpriseStatus.PENDING);
            long dbTotalCount = enterpriseService.countTotal();

            stats.put("databaseActiveCount", dbActiveCount);
            stats.put("databasePendingCount", dbPendingCount);
            stats.put("databaseTotalCount", dbTotalCount);

            // 一致性检查
            boolean consistent = (chainCount == dbActiveCount);
            stats.put("consistent", consistent);

            if (!consistent) {
                stats.put("difference", Math.abs(chainCount - dbActiveCount));
                log.warn("⚠️ 数据不一致: 区块链={}, 数据库ACTIVE={}, 差值={}",
                         chainCount, dbActiveCount, Math.abs(chainCount - dbActiveCount));
            } else {
                log.info("✓ 数据一致: 区块链={}, 数据库ACTIVE={}", chainCount, dbActiveCount);
            }

            return Result.success("统计完成", stats);

        } catch (Exception e) {
            log.error("获取区块链统计信息失败", e);
            return Result.error(500, "统计失败: " + e.getMessage());
        }
    }

    /**
     * 批量验证企业上链状态
     *
     * POST /api/admin/blockchain/enterprises/verify-batch
     *
     * @param addresses 企业区块链地址列表
     * @return 验证结果列表
     */
    @PostMapping("/enterprises/verify-batch")
    @ApiOperation(value = "批量验证企业上链状态", notes = "批量验证多个企业的区块链注册状态")
    @ApiResponses({
        @ApiResponse(code = 200, message = "批量验证完成"),
        @ApiResponse(code = 500, message = "验证失败")
    })
    public Result<List<Map<String, Object>>> batchVerifyEnterprisesOnChain(
            @ApiParam(value = "企业区块链地址列表", required = true)
            @RequestBody List<String> addresses) {

        log.info("批量验证企业上链状态: 数量={}", addresses.size());

        List<Map<String, Object>> results = new ArrayList<>();

        for (String address : addresses) {
            Map<String, Object> result = new HashMap<>();
            result.put("address", address);

            try {
                var chainEnterprise = contractService.getEnterpriseFromChain(address);
                result.put("onChain", chainEnterprise.exists);
                result.put("verified", true);

                if (chainEnterprise.exists) {
                    result.put("name", chainEnterprise.name);
                    result.put("creditCode", chainEnterprise.creditCode);
                }

                log.info("✓ 验证完成: address={}, onChain={}", address, chainEnterprise.exists);

            } catch (Exception e) {
                result.put("onChain", false);
                result.put("verified", false);
                result.put("error", e.getMessage());
                log.warn("✗ 验证失败: address={}, error={}", address, e.getMessage());
            }

            results.add(result);
        }

        log.info("批量验证完成: 总数={}, 成功={}", addresses.size(), results.size());

        return Result.success("批量验证完成", results);
    }
}
