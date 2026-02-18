package com.fisco.app.repository.system;

import com.fisco.app.entity.system.PermissionAuditLog;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

import java.util.List;

import java.time.LocalDateTime;

/**
 * 权限审计日志Repository
 */
@Repository
public interface PermissionAuditLogRepository extends JpaRepository<PermissionAuditLog, String> {

    /**
     * 查询指定用户的权限审计日志
     */
    List<PermissionAuditLog> findByUsername(String username);

    /**
     * 查询指定企业的权限审计日志
     */
    List<PermissionAuditLog> findByEnterpriseId(String enterpriseId);

    /**
     * 查询指定权限类型的日志
     */
    List<PermissionAuditLog> findByPermissionType(String permissionType);

    /**
     * 查询被拒绝的访问日志
     */
    List<PermissionAuditLog> findByAccessGrantedFalse();

    /**
     * 查询指定时间范围的日志
     */
    List<PermissionAuditLog> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * 查询指定用户在指定时间范围内的日志
     */
    List<PermissionAuditLog> findByUsernameAndCreatedAtBetween(String username, LocalDateTime start, LocalDateTime end);
}
