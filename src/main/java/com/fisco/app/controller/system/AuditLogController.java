package com.fisco.app.controller.system;

import java.time.LocalDateTime;
import java.util.List;

import javax.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fisco.app.dto.audit.AuditLogQueryRequest;
import com.fisco.app.dto.audit.AuditLogStatistics;
import com.fisco.app.entity.system.AuditLog;
import com.fisco.app.security.RequireAdmin;
import com.fisco.app.service.system.AuditLogService;
import com.fisco.app.vo.Result;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 审计日志Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@Api(tags = "审计日志管理")
public class AuditLogController {

    private final AuditLogService auditLogService;

    /**
     * 分页查询审计日志
     * GET /api/audit/logs
     */
    @GetMapping("/logs")
    @ApiOperation(value = "分页查询审计日志", notes = "支持多条件组合查询审计日志")
    @RequireAdmin(RequireAdmin.AdminRole.AUDITOR)
    public Result<Page<AuditLog>> queryAuditLogs(
            @Valid AuditLogQueryRequest request) {
        log.info("查询审计日志: {}", request);

        Page<AuditLog> page = auditLogService.queryAuditLogs(request);
        return Result.success("查询成功", page);
    }

    /**
     * 获取审计日志详情
     * GET /api/audit/logs/{id}
     */
    @GetMapping("/logs/{id}")
    @ApiOperation(value = "获取审计日志详情", notes = "根据ID查询审计日志的详细信息")
    @RequireAdmin(RequireAdmin.AdminRole.AUDITOR)
    public Result<AuditLog> getAuditLogById(
            @ApiParam(value = "审计日志ID", required = true) @PathVariable Long id) {
        AuditLog auditLog = auditLogService.getAuditLogById(id);
        return Result.success(auditLog);
    }

    /**
     * 查询实体的操作历史
     * GET /api/audit/entity/{entityType}/{entityId}
     */
    @GetMapping("/entity/{entityType}/{entityId}")
    @ApiOperation(value = "查询实体的操作历史", notes = "查询特定实体的所有操作记录")
    @RequireAdmin(RequireAdmin.AdminRole.AUDITOR)
    public Result<List<AuditLog>> getEntityHistory(
            @ApiParam(value = "实体类型", required = true) @PathVariable String entityType,
            @ApiParam(value = "实体ID", required = true) @PathVariable String entityId) {
        log.info("查询实体操作历史: entityType={}, entityId={}", entityType, entityId);

        List<AuditLog> history = auditLogService.getEntityHistory(entityType, entityId);
        return Result.success("查询成功", history);
    }

    /**
     * 查询用户的操作历史
     * GET /api/audit/user/{userAddress}
     */
    @GetMapping("/user/{userAddress}")
    @ApiOperation(value = "查询用户的操作历史", notes = "查询特定用户的所有操作记录")
    @RequireAdmin(RequireAdmin.AdminRole.AUDITOR)
    public Result<List<AuditLog>> getUserHistory(
            @ApiParam(value = "用户地址", required = true) @PathVariable String userAddress) {
        log.info("查询用户操作历史: userAddress={}", userAddress);

        List<AuditLog> history = auditLogService.getUserHistory(userAddress);
        return Result.success("查询成功", history);
    }

    /**
     * 获取最近的审计日志
     * GET /api/audit/logs/recent
     */
    @GetMapping("/logs/recent")
    @ApiOperation(value = "获取最近的审计日志", notes = "获取最近的操作记录")
    @RequireAdmin(RequireAdmin.AdminRole.AUDITOR)
    public Result<List<AuditLog>> getRecentLogs(
            @ApiParam(value = "返回数量限制", example = "20") @RequestParam(defaultValue = "20") int limit) {
        log.info("获取最近的审计日志: limit={}", limit);

        List<AuditLog> logs = auditLogService.getRecentLogs(limit);
        return Result.success("查询成功", logs);
    }

    /**
     * 统计审计日志
     * GET /api/audit/statistics
     */
    @GetMapping("/statistics")
    @ApiOperation(value = "统计审计日志", notes = "获取操作统计数据")
    @RequireAdmin(RequireAdmin.AdminRole.AUDITOR)
    public Result<AuditLogStatistics> getStatistics(
            @ApiParam(value = "开始时间") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @ApiParam(value = "结束时间") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        log.info("统计审计日志: startDate={}, endDate={}", startDate, endDate);

        // 默认统计最近30天
        if (startDate == null) {
            startDate = LocalDateTime.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }

        AuditLogStatistics stats = auditLogService.getStatistics(startDate, endDate);
        return Result.success("统计成功", stats);
    }

    /**
     * 根据区块链交易哈希查询
     * GET /api/audit/tx/{txHash}
     */
    @GetMapping("/tx/{txHash}")
    @ApiOperation(value = "根据交易哈希查询", notes = "根据区块链交易哈希查询相关的审计日志")
    @RequireAdmin(RequireAdmin.AdminRole.AUDITOR)
    public Result<List<AuditLog>> getLogsByTxHash(
            @ApiParam(value = "交易哈希", required = true) @PathVariable String txHash) {
        log.info("根据交易哈希查询审计日志: txHash={}", txHash);

        List<AuditLog> logs = auditLogService.getLogsByTxHash(txHash);
        return Result.success("查询成功", logs);
    }
}
