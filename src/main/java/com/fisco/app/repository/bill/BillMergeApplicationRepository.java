package com.fisco.app.repository.bill;

import com.fisco.app.entity.bill.BillMergeApplication;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 票据合并申请Repository
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-09
 * @version 1.0
 */
@Repository
public interface BillMergeApplicationRepository extends JpaRepository<BillMergeApplication, String> {

    /**
     * 根据申请人ID查询合并申请
     */
    List<BillMergeApplication> findByApplicantId(String applicantId);

    /**
     * 根据合并后的票据ID查询
     */
    List<BillMergeApplication> findByMergedBillId(String mergedBillId);

    /**
     * 根据状态查询合并申请
     */
    List<BillMergeApplication> findByStatus(String status);

    /**
     * 查询待处理的合并申请
     */
    @Query("SELECT a FROM BillMergeApplication a WHERE a.status = 'PENDING' ORDER BY a.createdAt DESC")
    List<BillMergeApplication> findPendingApplications();

    /**
     * 根据处理人ID查询
     */
    List<BillMergeApplication> findByProcessorId(String processorId);

    /**
     * 统计企业的待处理申请数量
     */
    @Query("SELECT COUNT(a) FROM BillMergeApplication a WHERE a.applicantId = :applicantId AND a.status = 'PENDING'")
    Long countPendingByApplicant(@Param("applicantId") String applicantId);
}
