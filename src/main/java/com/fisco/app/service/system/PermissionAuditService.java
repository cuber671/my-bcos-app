package com.fisco.app.service.system;

import com.fisco.app.entity.system.PermissionAuditLog;
import com.fisco.app.repository.system.PermissionAuditLogRepository;
import com.fisco.app.security.UserAuthentication;
import org.springframework.security.core.Authentication;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

import javax.servlet.http.HttpServletRequest;

import org.springframework.transaction.annotation.Transactional;

/**
 * 权限审计日志服务
 * 异步记录权限检查和访问控制事件
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionAuditService {

    private final PermissionAuditLogRepository auditLogRepository;

    /**
     * 异步记录权限审计日志
     */
    @Async
    @Transactional
    public void logPermissionCheck(@NonNull PermissionAuditLog auditLog) {
        try {
            auditLogRepository.save(auditLog);
            log.debug("权限审计日志已保存: id={}, username={}, operation={}, accessGranted={}",
                     auditLog.getId(), auditLog.getUsername(), auditLog.getOperation(), auditLog.getAccessGranted());
        } catch (Exception e) {
            log.error("保存权限审计日志失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 记录权限检查成功
     */
    public void logAccessGranted(Authentication authentication, String permissionType,
                                String targetResource, String operation,
                                HttpServletRequest request) {
        if (!(authentication instanceof UserAuthentication)) {
            return;
        }

        UserAuthentication userAuth = (UserAuthentication) authentication;

        PermissionAuditLog auditLog = new PermissionAuditLog();
        auditLog.setUsername(userAuth.getName());
        auditLog.setEnterpriseId(userAuth.getEnterpriseId());
        auditLog.setUserRole(userAuth.getRole());
        auditLog.setLoginType(userAuth.getLoginType());
        auditLog.setPermissionType(permissionType);
        auditLog.setTargetResource(targetResource);
        auditLog.setOperation(operation);
        auditLog.setAccessGranted(true);
        auditLog.setIpAddress(getClientIp(request));
        auditLog.setUserAgent(request != null ? request.getHeader("User-Agent") : null);
        auditLog.setRequestMethod(request != null ? request.getMethod() : null);
        auditLog.setRequestUri(request != null ? request.getRequestURI() : null);

        logPermissionCheck(auditLog);
    }

    /**
     * 记录权限检查失败
     */
    public void logAccessDenied(Authentication authentication, String permissionType,
                               String targetResource, String operation,
                               String denialReason, HttpServletRequest request) {
        if (!(authentication instanceof UserAuthentication)) {
            return;
        }

        UserAuthentication userAuth = (UserAuthentication) authentication;

        PermissionAuditLog auditLog = new PermissionAuditLog();
        auditLog.setUsername(userAuth.getName());
        auditLog.setEnterpriseId(userAuth.getEnterpriseId());
        auditLog.setUserRole(userAuth.getRole());
        auditLog.setLoginType(userAuth.getLoginType());
        auditLog.setPermissionType(permissionType);
        auditLog.setTargetResource(targetResource);
        auditLog.setOperation(operation);
        auditLog.setAccessGranted(false);
        auditLog.setDenialReason(denialReason);
        auditLog.setIpAddress(getClientIp(request));
        auditLog.setUserAgent(request != null ? request.getHeader("User-Agent") : null);
        auditLog.setRequestMethod(request != null ? request.getMethod() : null);
        auditLog.setRequestUri(request != null ? request.getRequestURI() : null);

        // 构建详细信息
        Map<String, Object> details = new HashMap<>();
        details.put("userEnterpriseId", userAuth.getEnterpriseId());
        details.put("targetEnterpriseId", targetResource);
        details.put("denialReason", denialReason);
        auditLog.setDetails(details.toString());

        logPermissionCheck(auditLog);

        // 记录警告日志
        log.warn("权限访问被拒绝: username={}, operation={}, targetResource={}, reason={}",
                userAuth.getName(), operation, targetResource, denialReason);
    }

    /**
     * 获取指定用户的审计日志
     */
    public List<PermissionAuditLog> getUserAuditLogs(String username) {
        return auditLogRepository.findByUsername(username);
    }

    /**
     * 获取指定企业的审计日志
     */
    public List<PermissionAuditLog> getEnterpriseAuditLogs(String enterpriseId) {
        return auditLogRepository.findByEnterpriseId(enterpriseId);
    }

    /**
     * 获取所有被拒绝的访问日志
     */
    public List<PermissionAuditLog> getDeniedAccessLogs() {
        return auditLogRepository.findByAccessGrantedFalse();
    }

    /**
     * 获取指定时间范围的审计日志
     */
    public List<PermissionAuditLog> getAuditLogsByTimeRange(LocalDateTime start, LocalDateTime end) {
        return auditLogRepository.findByCreatedAtBetween(start, end);
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 处理多个IP的情况（X-Forwarded-For可能包含多个IP）
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip != null ? ip : "unknown";
    }
}
