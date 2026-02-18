package com.fisco.app.repository.pledge;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fisco.app.entity.pledge.PledgeRecord;

/**
 * 仓单质押记录Repository
 */
@Repository
public interface PledgeRecordRepository extends JpaRepository<PledgeRecord, Long> {

    /**
     * 根据仓单ID查询质押记录
     */
    List<PledgeRecord> findByReceiptIdOrderByPledgeTimeDesc(String receiptId);

    /**
     * 根据背书ID查询质押记录
     */
    Optional<PledgeRecord> findByEndorsementId(String endorsementId);

    /**
     * 根据货主ID分页查询质押记录
     */
    Page<PledgeRecord> findByOwnerId(String ownerId, Pageable pageable);

    /**
     * 根据金融机构ID分页查询质押记录
     */
    Page<PledgeRecord> findByFinancialInstitutionId(String financialInstitutionId, Pageable pageable);

    /**
     * 根据状态查询质押记录
     */
    List<PledgeRecord> findByStatus(PledgeRecord.PledgeStatus status);

    /**
     * 根据状态分页查询质押记录
     */
    Page<PledgeRecord> findByStatus(PledgeRecord.PledgeStatus status, Pageable pageable);

    /**
     * 查询仓单的当前有效质押记录
     */
    @Query("SELECT p FROM PledgeRecord p WHERE p.receiptId = :receiptId " +
           "AND p.status = 'ACTIVE' AND p.deleted = false ORDER BY p.pledgeTime DESC")
    Optional<PledgeRecord> findActivePledgeByReceiptId(@Param("receiptId") String receiptId);

    /**
     * 检查仓单是否存在有效的质押记录
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM PledgeRecord p " +
           "WHERE p.receiptId = :receiptId AND p.status = 'ACTIVE' AND p.deleted = false")
    boolean hasActivePledge(@Param("receiptId") String receiptId);

    /**
     * 根据仓单ID和状态列表查询质押记录
     */
    List<PledgeRecord> findByReceiptIdAndStatusIn(String receiptId, List<PledgeRecord.PledgeStatus> statuses);

    /**
     * 统计货主的质押记录数量
     */
    long countByOwnerId(String ownerId);

    /**
     * 统计金融机构的质押记录数量
     */
    long countByFinancialInstitutionId(String financialInstitutionId);

    /**
     * 统计指定状态的质押记录数量
     */
    long countByStatus(PledgeRecord.PledgeStatus status);

    /**
     * 分页查询质押记录（支持多条件）
     */
    @Query("SELECT p FROM PledgeRecord p WHERE " +
           "(:receiptId IS NULL OR p.receiptId = :receiptId) AND " +
           "(:ownerId IS NULL OR p.ownerId = :ownerId) AND " +
           "(:financialInstitutionId IS NULL OR p.financialInstitutionId = :financialInstitutionId) AND " +
           "(:status IS NULL OR p.status = :status) AND " +
           "p.deleted = false " +
           "ORDER BY p.pledgeTime DESC")
    Page<PledgeRecord> findByConditions(
            @Param("receiptId") String receiptId,
            @Param("ownerId") String ownerId,
            @Param("financialInstitutionId") String financialInstitutionId,
            @Param("status") PledgeRecord.PledgeStatus status,
            Pageable pageable
    );
}
