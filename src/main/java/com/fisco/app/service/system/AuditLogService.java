package com.fisco.app.service.system;

import com.fisco.app.repository.system.AuditLogRepository;
import com.fisco.app.dto.audit.AuditLogStatistics;
import com.fisco.app.dto.audit.AuditLogQueryRequest;
import com.fisco.app.entity.system.AuditLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import io.swagger.annotations.Api;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fisco.app.exception.BusinessException;

import java.time.LocalDateTime;
import java.util.Arrays;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * 审计日志Service
 */
@Slf4j
@Service
@Api(tags = "审计日志服务")
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * 分页查询审计日志
     */
    @SuppressWarnings("null")
    public Page<AuditLog> queryAuditLogs(AuditLogQueryRequest request) {
        log.info("查询审计日志: userAddress={}, module={}, actionType={}, entityType={}, isSuccess={}",
            request.getUserAddress(), request.getModule(), request.getActionType(),
            request.getEntityType(), request.getIsSuccess());

        // 创建排序
        Sort sort = Sort.by(
            "DESC".equalsIgnoreCase(request.getSortDirection()) ? Sort.Direction.DESC : Sort.Direction.ASC,
            request.getSortField()
        );

        // 创建分页
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

        // 根据条件查询
        List<AuditLog> allLogs = auditLogRepository.findByConditions(
            request.getUserAddress(),
            request.getModule(),
            request.getActionType(),
            request.getEntityType(),
            request.getIsSuccess(),
            request.getStartDate(),
            request.getEndDate()
        );

        // 手动分页
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allLogs.size());

        List<AuditLog> pageContent = start < allLogs.size()
            ? allLogs.subList(start, end)
            : List.of();

        return new org.springframework.data.domain.PageImpl<>(pageContent, pageable, allLogs.size());
    }

    /**
     * 获取审计日志详情
     */
    @SuppressWarnings("null")
    public AuditLog getAuditLogById(Long id) {
        return auditLogRepository.findById(id)
                .orElseThrow(() -> new BusinessException("审计日志不存在: " + id));
    }

    /**
     * 查询实体的操作历史
     */
    public List<AuditLog> getEntityHistory(String entityType, String entityId) {
        log.info("查询实体操作历史: entityType={}, entityId={}", entityType, entityId);
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId);
    }

    /**
     * 查询用户的操作历史
     */
    public List<AuditLog> getUserHistory(String userAddress) {
        log.info("查询用户操作历史: userAddress={}", userAddress);
        return auditLogRepository.findByUserAddressOrderByCreatedAtDesc(userAddress);
    }

    /**
     * 统计审计日志
     */
    public AuditLogStatistics getStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("统计审计日志: startDate={}, endDate={}", startDate, endDate);

        AuditLogStatistics stats = new AuditLogStatistics();
        stats.setStartTime(startDate);
        stats.setEndTime(endDate);

        // 总操作次数
        Long totalCount = auditLogRepository.countByDateRange(startDate, endDate);
        stats.setTotalOperations(totalCount != null ? totalCount : 0L);

        // 失败操作次数
        Long failureCount = auditLogRepository.countFailedOperations();
        stats.setFailureCount(failureCount != null ? failureCount : 0L);

        // 成功操作次数
        stats.setSuccessCount(stats.getTotalOperations() - stats.getFailureCount());

        // 成功率
        if (stats.getTotalOperations() > 0) {
            stats.setSuccessRate(
                (stats.getSuccessCount().doubleValue() / stats.getTotalOperations().doubleValue()) * 100
            );
        } else {
            stats.setSuccessRate(0.0);
        }

        // 按模块统计
        Map<String, Long> moduleStats = new HashMap<>();
        for (String module : Arrays.asList("BILL", "RECEIVABLE", "WAREHOUSE_RECEIPT", "ENTERPRISE", "USER")) {
            List<AuditLog> moduleLogs = auditLogRepository.findByModuleOrderByCreatedAtDesc(module);
            if (startDate != null && endDate != null) {
                moduleLogs = moduleLogs.stream()
                    .filter(log -> log.getCreatedAt().isAfter(startDate) && log.getCreatedAt().isBefore(endDate))
                    .collect(Collectors.toList());
            }
            moduleStats.put(module, (long) moduleLogs.size());
        }
        stats.setModuleStats(moduleStats);

        // 按操作类型统计
        Map<String, Long> actionTypeStats = new HashMap<>();
        for (String actionType : Arrays.asList("CREATE", "UPDATE", "DELETE", "QUERY")) {
            List<AuditLog> actionLogs = auditLogRepository.findByActionTypeOrderByCreatedAtDesc(actionType);
            if (startDate != null && endDate != null) {
                actionLogs = actionLogs.stream()
                    .filter(log -> log.getCreatedAt().isAfter(startDate) && log.getCreatedAt().isBefore(endDate))
                    .collect(Collectors.toList());
            }
            actionTypeStats.put(actionType, (long) actionLogs.size());
        }
        stats.setActionTypeStats(actionTypeStats);

        // 平均操作时长
        List<AuditLog> allLogs = auditLogRepository.findByConditions(
            null, null, null, null, null, startDate, endDate
        );
        if (!allLogs.isEmpty()) {
            long totalDuration = allLogs.stream()
                .filter(log -> log.getDuration() != null)
                .mapToLong(AuditLog::getDuration)
                .sum();
            stats.setAverageDuration(totalDuration / allLogs.size());
        }

        return stats;
    }

    /**
     * 获取最近的审计日志
     */
    public List<AuditLog> getRecentLogs(int limit) {
        log.info("获取最近的审计日志: limit={}", limit);
        List<AuditLog> allLogs = auditLogRepository.findRecentLogs();
        return allLogs.stream()
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * 根据区块链交易哈希查询
     */
    public List<AuditLog> getLogsByTxHash(String txHash) {
        log.info("根据交易哈希查询审计日志: txHash={}", txHash);
        return auditLogRepository.findByTxHash(txHash);
    }
}
