package com.fisco.app.controller.system;

import com.fisco.app.service.blockchain.ContractService;
import com.fisco.app.service.system.DataMigrationService;
import com.fisco.app.vo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据迁移Controller
 * 提供数据库到区块链的数据迁移功能
 *
 * @author FISCO BCOS
 * @since 2025-01-22
 */
@Slf4j
@ConditionalOnProperty(name = "fisco.enabled", havingValue = "true", matchIfMissing = true)
@RestController
@RequestMapping("/api/admin/blockchain/migration")
@RequiredArgsConstructor
@Api(tags = "BlockchainManagement")
public class DataMigrationController {

    private final DataMigrationService dataMigrationService;
    private final ContractService contractService;

    /**
     * 获取待迁移的企业数量
     * GET /api/admin/blockchain/migration/enterprises/count
     */
    @GetMapping("/enterprises/count")
    @ApiOperation(value = "获取待迁移企业数量", notes = "统计需要迁移到区块链的企业数量")
    @ApiResponses({
        @ApiResponse(code = 200, message = "查询成功"),
        @ApiResponse(code = 500, message = "查询失败")
    })
    public Result<Long> getPendingMigrationCount() {
        long count = dataMigrationService.getPendingMigrationCount();
        return Result.success(count);
    }

    /**
     * 获取迁移进度
     * GET /api/admin/blockchain/migration/progress
     */
    @GetMapping("/progress")
    @ApiOperation(value = "获取迁移进度", notes = "获取当前数据迁移的实时进度信息")
    @ApiResponses({
        @ApiResponse(code = 200, message = "查询成功"),
        @ApiResponse(code = 500, message = "查询失败")
    })
    public Result<DataMigrationService.MigrationProgress> getMigrationProgress() {
        DataMigrationService.MigrationProgress progress = dataMigrationService.getMigrationProgress();
        return Result.success(progress);
    }

    /**
     * 执行数据迁移
     * POST /api/admin/blockchain/migration/execute
     */
    @PostMapping("/execute")
    @ApiOperation(value = "执行数据迁移", notes = "开始将企业数据从数据库迁移到区块链（异步执行）")
    @ApiResponses({
        @ApiResponse(code = 200, message = "迁移任务已启动"),
        @ApiResponse(code = 500, message = "启动失败或任务已在运行")
    })
    public Result<String> executeMigration() {
        log.info("收到数据迁移请求");

        if (dataMigrationService.getMigrationProgress().isMigrating()) {
            return Result.error("迁移任务已在运行中");
        }

        // 异步执行迁移
        dataMigrationService.migrateEnterprises();

        return Result.success("数据迁移任务已启动，请使用 /api/admin/blockchain/migration/progress 查看进度");
    }

    /**
     * 验证迁移结果
     * GET /api/admin/blockchain/migration/verify
     */
    @GetMapping("/verify")
    @ApiOperation(value = "验证迁移结果", notes = "验证数据库和区块链的企业数据是否一致")
    @ApiResponses({
        @ApiResponse(code = 200, message = "验证完成"),
        @ApiResponse(code = 500, message = "验证失败")
    })
    public Result<DataMigrationService.MigrationResult> verifyMigration() {
        DataMigrationService.MigrationResult result = dataMigrationService.verifyMigration();

        if (result.isConsistent()) {
            return Result.success("迁移验证通过，数据一致", result);
        } else {
            return Result.error("迁移验证失败，数据不一致: 数据库=" +
                result.getDatabaseCount() + ", 区块链=" + result.getBlockchainCount());
        }
    }

    /**
     * 获取迁移状态
     * GET /api/admin/blockchain/migration/status
     */
    @GetMapping("/status")
    @ApiOperation(value = "获取迁移状态", notes = "获取数据迁移任务的当前详细状态")
    @ApiResponses({
        @ApiResponse(code = 200, message = "查询成功"),
        @ApiResponse(code = 500, message = "查询失败")
    })
    public Result<Object> getMigrationStatus() {
        DataMigrationService.MigrationProgress progress = dataMigrationService.getMigrationProgress();

        java.util.Map<String, Object> status = new java.util.HashMap<>();
        status.put("isMigrating", progress.isMigrating());
        status.put("status", progress.getStatus());
        status.put("migratedCount", progress.getMigratedCount());
        status.put("totalCount", progress.getTotalCount());
        status.put("progress", String.format("%.2f%%", progress.getProgress()));
        status.put("lastError", progress.getLastError());

        return Result.success(status);
    }

    /**
     * 获取企业数量统计
     * GET /api/admin/blockchain/migration/statistics
     */
    @GetMapping("/statistics")
    @ApiOperation(value = "获取企业数量统计", notes = "对比数据库和区块链中的企业数量统计信息")
    @ApiResponses({
        @ApiResponse(code = 200, message = "统计成功"),
        @ApiResponse(code = 500, message = "统计失败")
    })
    public Result<Map<String, Object>> getEnterpriseStatistics() {
        log.info("获取企业数量统计");

        Map<String, Object> stats = new HashMap<>();

        // 从数据库查询企业数量
        long dbActiveCount = dataMigrationService.getPendingMigrationCount();
        stats.put("databaseActiveCount", dbActiveCount);

        // 从区块链查询企业数量
        try {
            long blockchainActiveCount = contractService.getActiveEnterpriseCountFromChain();
            stats.put("blockchainActiveCount", blockchainActiveCount);

            long blockchainTotalCount = contractService.getTotalEnterpriseCountFromChain();
            stats.put("blockchainTotalCount", blockchainTotalCount);

            // 数据一致性检查
            boolean consistent = (dbActiveCount == blockchainActiveCount);
            stats.put("consistent", consistent);

            if (consistent) {
                stats.put("message", "数据库与区块链数据一致");
            } else {
                stats.put("message", String.format("数据不一致 - 数据库: %d, 区块链活跃: %d, 区块链总数: %d",
                    dbActiveCount, blockchainActiveCount, blockchainTotalCount));
            }

        } catch (Exception e) {
            log.error("从区块链查询企业数量失败", e);
            stats.put("blockchainActiveCount", -1);
            stats.put("blockchainTotalCount", -1);
            stats.put("consistent", false);
            stats.put("message", "区块链查询失败: " + e.getMessage());
        }

        stats.put("timestamp", java.time.LocalDateTime.now());

        return Result.success(stats);
    }
}
