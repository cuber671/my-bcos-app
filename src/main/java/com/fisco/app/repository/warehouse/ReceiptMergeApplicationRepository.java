package com.fisco.app.repository.warehouse;

import com.fisco.app.entity.warehouse.ReceiptMergeApplication;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 仓单合并申请Repository
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-09
 * @version 1.0
 */
@Repository
public interface ReceiptMergeApplicationRepository extends JpaRepository<ReceiptMergeApplication, String> {

    /**
     * 根据申请状态查询
     */
    List<ReceiptMergeApplication> findByRequestStatus(String status);

    /**
     * 根据申请人ID查询（按创建时间倒序）
     */
    List<ReceiptMergeApplication> findByApplicantIdOrderByCreatedAtDesc(String applicantId);

    /**
     * 根据合并后的仓单ID查询
     */
    Optional<ReceiptMergeApplication> findByMergedReceiptId(String mergedReceiptId);

    /**
     * 检查仓单是否有待审核的合并申请
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM ReceiptMergeApplication r WHERE r.requestStatus = :status AND r.sourceReceiptIds LIKE %:receiptId%")
    boolean existsByRequestStatusAndSourceReceiptIdsContaining(@Param("status") String status, @Param("receiptId") String receiptId);

    /**
     * 查询待审核的合并申请
     */
    @Query("SELECT r FROM ReceiptMergeApplication r WHERE r.requestStatus = 'PENDING' ORDER BY r.createdAt DESC")
    List<ReceiptMergeApplication> findPendingApplications();
}
