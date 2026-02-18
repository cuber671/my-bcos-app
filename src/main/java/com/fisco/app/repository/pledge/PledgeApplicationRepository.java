package com.fisco.app.repository.pledge;

import com.fisco.app.entity.pledge.PledgeApplication;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import java.time.LocalDateTime;

/**
 * 仓单质押申请Repository
 */
@Repository
public interface PledgeApplicationRepository extends JpaRepository<PledgeApplication, Long> {

    /**
     * 根据仓单ID查询质押申请
     */
    List<PledgeApplication> findByReceiptId(String receiptId);

    /**
     * 根据仓单ID查询最新的质押申请
     */
    Optional<PledgeApplication> findFirstByReceiptIdOrderByApplyTimeDesc(String receiptId);

    /**
     * 根据申请编号查询
     */
    Optional<PledgeApplication> findByApplicationNo(String applicationNo);

    /**
     * 根据货主ID分页查询质押申请
     */
    Page<PledgeApplication> findByOwnerId(String ownerId, Pageable pageable);

    /**
     * 根据金融机构ID分页查询质押申请
     */
    Page<PledgeApplication> findByFinancialInstitutionId(String financialInstitutionId, Pageable pageable);

    /**
     * 根据状态分页查询质押申请
     */
    Page<PledgeApplication> findByStatus(PledgeApplication.ApplicationStatus status, Pageable pageable);

    /**
     * 根据仓单ID和状态查询
     */
    List<PledgeApplication> findByReceiptIdAndStatus(String receiptId, PledgeApplication.ApplicationStatus status);

    /**
     * 查询待审核的质押申请（按申请时间排序）
     */
    List<PledgeApplication> findByStatusOrderByApplyTimeAsc(PledgeApplication.ApplicationStatus status);

    /**
     * 根据金融机构ID和状态分页查询
     */
    Page<PledgeApplication> findByFinancialInstitutionIdAndStatus(
            String financialInstitutionId,
            PledgeApplication.ApplicationStatus status,
            Pageable pageable
    );

    /**
     * 查询指定时间范围内的质押申请
     */
    List<PledgeApplication> findByApplyTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计货主的质押申请数量
     */
    long countByOwnerId(String ownerId);

    /**
     * 统计金融机构的质押申请数量
     */
    long countByFinancialInstitutionId(String financialInstitutionId);

    /**
     * 统计指定状态的质押申请数量
     */
    long countByStatus(PledgeApplication.ApplicationStatus status);

    /**
     * 检查仓单是否存在有效的质押申请
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM PledgeApplication p " +
           "WHERE p.receiptId = :receiptId AND p.status IN ('APPROVED', 'PENDING') AND p.deleted = false")
    boolean hasValidPledgeApplication(@Param("receiptId") String receiptId);

    /**
     * 查询仓单的当前有效质押申请
     */
    @Query("SELECT p FROM PledgeApplication p WHERE p.receiptId = :receiptId " +
           "AND p.status = 'APPROVED' AND p.deleted = false ORDER BY p.applyTime DESC")
    Optional<PledgeApplication> findActivePledgeByReceiptId(@Param("receiptId") String receiptId);

    /**
     * 分页查询质押申请（支持多条件）
     */
    @Query("SELECT p FROM PledgeApplication p WHERE " +
           "(:receiptId IS NULL OR p.receiptId = :receiptId) AND " +
           "(:ownerId IS NULL OR p.ownerId = :ownerId) AND " +
           "(:financialInstitutionId IS NULL OR p.financialInstitutionId = :financialInstitutionId) AND " +
           "(:status IS NULL OR p.status = :status) AND " +
           "p.deleted = false " +
           "ORDER BY p.applyTime DESC")
    Page<PledgeApplication> findByConditions(
            @Param("receiptId") String receiptId,
            @Param("ownerId") String ownerId,
            @Param("financialInstitutionId") String financialInstitutionId,
            @Param("status") PledgeApplication.ApplicationStatus status,
            Pageable pageable
    );
}
