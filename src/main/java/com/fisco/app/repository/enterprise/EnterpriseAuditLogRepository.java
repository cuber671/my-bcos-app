package com.fisco.app.repository.enterprise;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fisco.app.entity.enterprise.EnterpriseAuditLog;

/**
 * 企业审核日志Repository
 */
@Repository
public interface EnterpriseAuditLogRepository extends JpaRepository<EnterpriseAuditLog, Long> {

    /**
     * 根据企业地址查询审核日志
     */
    List<EnterpriseAuditLog> findByEnterpriseAddressOrderByAuditTimeDesc(String enterpriseAddress);

    /**
     * 根据审核人查询审核日志
     */
    List<EnterpriseAuditLog> findByAuditorOrderByAuditTimeDesc(String auditor);

    /**
     * 根据审核动作查询日志
     */
    List<EnterpriseAuditLog> findByActionOrderByAuditTimeDesc(EnterpriseAuditLog.AuditAction action);

    /**
     * 查询指定时间范围内的审核日志
     */
    List<EnterpriseAuditLog> findByAuditTimeBetweenOrderByAuditTimeDesc(
            LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查询企业最新的审核日志
     */
    @Query("SELECT e FROM EnterpriseAuditLog e WHERE e.enterpriseAddress = :address ORDER BY e.auditTime DESC")
    List<EnterpriseAuditLog> findLatestAuditLogsByAddress(@Param("address") String address);

    /**
     * 统计审核人的审核次数
     */
    @Query("SELECT e.auditor, COUNT(e) FROM EnterpriseAuditLog e GROUP BY e.auditor")
    List<Object[]> countByAuditor();

    /**
     * 统计审核动作的数量
     */
    @Query("SELECT e.action, COUNT(e) FROM EnterpriseAuditLog e GROUP BY e.action")
    List<Object[]> countByAction();
}
