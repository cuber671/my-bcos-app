package com.fisco.app.repository.risk;

import com.fisco.app.entity.risk.FinancingRecord;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 仓单融资记录Repository
 */
@Repository
public interface FinancingRecordRepository extends JpaRepository<FinancingRecord, Long> {

    /**
     * 根据仓单ID查询融资记录
     */
    List<FinancingRecord> findByReceiptIdOrderByFinancingTimeDesc(String receiptId);

    /**
     * 根据融资编号查询
     */
    Optional<FinancingRecord> findByFinancingNo(String financingNo);

    /**
     * 根据背书ID查询融资记录
     */
    Optional<FinancingRecord> findByEndorsementId(String endorsementId);

    /**
     * 根据货主ID分页查询融资记录
     */
    Page<FinancingRecord> findByOwnerId(String ownerId, Pageable pageable);

    /**
     * 根据金融机构ID分页查询融资记录
     */
    Page<FinancingRecord> findByFinancialInstitutionId(String financialInstitutionId, Pageable pageable);

    /**
     * 根据状态查询融资记录
     */
    List<FinancingRecord> findByStatus(FinancingRecord.FinancingStatus status);

    /**
     * 根据状态分页查询融资记录
     */
    Page<FinancingRecord> findByStatus(FinancingRecord.FinancingStatus status, Pageable pageable);

    /**
     * 查询仓单的当前有效融资记录
     */
    @Query("SELECT f FROM FinancingRecord f WHERE f.receiptId = :receiptId " +
           "AND f.status = 'ACTIVE' AND f.deleted = false ORDER BY f.financingTime DESC")
    Optional<FinancingRecord> findActiveFinancingByReceiptId(@Param("receiptId") String receiptId);

    /**
     * 检查仓单是否存在有效的融资记录
     */
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM FinancingRecord f " +
           "WHERE f.receiptId = :receiptId AND f.status = 'ACTIVE' AND f.deleted = false")
    boolean hasActiveFinancing(@Param("receiptId") String receiptId);

    /**
     * 统计货主的融资记录数量
     */
    long countByOwnerId(String ownerId);

    /**
     * 统计金融机构的融资记录数量
     */
    long countByFinancialInstitutionId(String financialInstitutionId);

    /**
     * 统计指定状态的融资记录数量
     */
    long countByStatus(FinancingRecord.FinancingStatus status);

    /**
     * 分页查询融资记录（支持多条件）
     */
    @Query("SELECT f FROM FinancingRecord f WHERE " +
           "(:receiptId IS NULL OR f.receiptId = :receiptId) AND " +
           "(:ownerId IS NULL OR f.ownerId = :ownerId) AND " +
           "(:financialInstitutionId IS NULL OR f.financialInstitutionId = :financialInstitutionId) AND " +
           "(:status IS NULL OR f.status = :status) AND " +
           "f.deleted = false " +
           "ORDER BY f.financingTime DESC")
    Page<FinancingRecord> findByConditions(
            @Param("receiptId") String receiptId,
            @Param("ownerId") String ownerId,
            @Param("financialInstitutionId") String financialInstitutionId,
            @Param("status") FinancingRecord.FinancingStatus status,
            Pageable pageable
    );

    /**
     * 查询逾期的融资记录
     */
    @Query("SELECT f FROM FinancingRecord f WHERE f.dueDate < CURRENT_DATE " +
           "AND f.status = 'ACTIVE' AND f.deleted = false")
    List<FinancingRecord> findOverdueFinancingRecords();
}
