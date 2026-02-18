package com.fisco.app.repository.system;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fisco.app.entity.system.AuditLog;

/**
 * 审计日志Repository
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * 根据用户地址查询审计日志
     */
    List<AuditLog> findByUserAddressOrderByCreatedAtDesc(String userAddress);

    /**
     * 根据实体类型和实体ID查询
     */
    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
        String entityType,
        String entityId
    );

    /**
     * 根据模块查询审计日志
     */
    List<AuditLog> findByModuleOrderByCreatedAtDesc(String module);

    /**
     * 根据操作类型查询
     */
    List<AuditLog> findByActionTypeOrderByCreatedAtDesc(String actionType);

    /**
     * 查询指定时间范围内的审计日志
     */
    @Query("SELECT a FROM AuditLog a WHERE a.createdAt BETWEEN :startDate AND :endDate")
    List<AuditLog> findByCreatedAtBetween(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * 多条件查询审计日志
     */
    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:userAddress IS NULL OR a.userAddress = :userAddress) AND " +
           "(:module IS NULL OR a.module = :module) AND " +
           "(:actionType IS NULL OR a.actionType = :actionType) AND " +
           "(:entityType IS NULL OR a.entityType = :entityType) AND " +
           "(:isSuccess IS NULL OR a.isSuccess = :isSuccess) AND " +
           "(:startDate IS NULL OR a.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR a.createdAt <= :endDate) " +
           "ORDER BY a.createdAt DESC")
    List<AuditLog> findByConditions(
        @Param("userAddress") String userAddress,
        @Param("module") String module,
        @Param("actionType") String actionType,
        @Param("entityType") String entityType,
        @Param("isSuccess") Boolean isSuccess,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * 统计用户操作次数
     */
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.userAddress = :userAddress")
    Long countByUserAddress(@Param("userAddress") String userAddress);

    /**
     * 统计失败的操作次数
     */
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.isSuccess = false")
    Long countFailedOperations();

    /**
     * 统计指定时间范围内的操作次数
     */
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.createdAt BETWEEN :startDate AND :endDate")
    Long countByDateRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * 查询最近的审计日志
     */
    @Query("SELECT a FROM AuditLog a ORDER BY a.createdAt DESC")
    List<AuditLog> findRecentLogs();

    /**
     * 根据区块链交易哈希查询
     */
    List<AuditLog> findByTxHash(String txHash);
}
