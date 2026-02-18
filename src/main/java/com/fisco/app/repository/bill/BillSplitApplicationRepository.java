package com.fisco.app.repository.bill;

import com.fisco.app.entity.bill.BillSplitApplication;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 票据拆分申请Repository
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-09
 * @version 1.0
 */
@Repository
public interface BillSplitApplicationRepository extends JpaRepository<BillSplitApplication, String> {

    /**
     * 根据父票据ID查询所有拆分申请
     */
    List<BillSplitApplication> findByParentBillId(String parentBillId);

    /**
     * 根据申请人ID查询拆分申请
     */
    List<BillSplitApplication> findByApplicantId(String applicantId);

    /**
     * 根据状态查询拆分申请
     */
    List<BillSplitApplication> findByStatus(String status);

    /**
     * 查询待处理的拆分申请
     */
    @Query("SELECT a FROM BillSplitApplication a WHERE a.status = 'PENDING' ORDER BY a.createdAt DESC")
    List<BillSplitApplication> findPendingApplications();

    /**
     * 检查是否存在待处理的拆分申请
     */
    @Query("SELECT COUNT(a) > 0 FROM BillSplitApplication a WHERE a.parentBillId = :parentBillId AND a.status = 'PENDING'")
    boolean existsPendingSplitApplication(@Param("parentBillId") String parentBillId);

    /**
     * 根据处理人ID查询
     */
    List<BillSplitApplication> findByProcessorId(String processorId);

    /**
     * 统计企业的待处理申请数量
     */
    @Query("SELECT COUNT(a) FROM BillSplitApplication a WHERE a.applicantId = :applicantId AND a.status = 'PENDING'")
    Long countPendingByApplicant(@Param("applicantId") String applicantId);
}
