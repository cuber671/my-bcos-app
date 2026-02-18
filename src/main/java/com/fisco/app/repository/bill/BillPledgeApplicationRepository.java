package com.fisco.app.repository.bill;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fisco.app.entity.bill.BillPledgeApplication;

/**
 * 票据质押融资申请Repository
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-02
 */
@Repository
public interface BillPledgeApplicationRepository extends JpaRepository<BillPledgeApplication, String> {

    /**
     * 根据票据ID查询质押申请
     */
    List<BillPledgeApplication> findByBillId(String billId);

    /**
     * 根据票据编号查询质押申请
     */
    List<BillPledgeApplication> findByBillNo(String billNo);

    /**
     * 根据申请状态查询
     */
    List<BillPledgeApplication> findByApplicationStatus(BillPledgeApplication.ApplicationStatus status);

    /**
     * 根据金融机构ID查询
     */
    List<BillPledgeApplication> findByFinancialInstitutionId(String financialInstitutionId);

    /**
     * 根据申请人ID查询
     */
    List<BillPledgeApplication> findByApplicantId(String applicantId);

    /**
     * 查询待审核的质押申请
     */
    @Query("SELECT a FROM BillPledgeApplication a WHERE a.applicationStatus = 'PENDING' ORDER BY a.createdAt DESC")
    List<BillPledgeApplication> findPendingApplications();

    /**
     * 查询金融机构的待审核申请
     */
    @Query("SELECT a FROM BillPledgeApplication a WHERE a.financialInstitutionId = :institutionId AND a.applicationStatus = 'PENDING' ORDER BY a.createdAt DESC")
    List<BillPledgeApplication> findPendingApplicationsByInstitution(@Param("institutionId") String institutionId);

    /**
     * 统计申请人的质押申请数量
     */
    @Query("SELECT COUNT(a) FROM BillPledgeApplication a WHERE a.applicantId = :applicantId")
    Long countByApplicantId(@Param("applicantId") String applicantId);

    /**
     * 统计票据的质押申请数量
     */
    @Query("SELECT COUNT(a) FROM BillPledgeApplication a WHERE a.billId = :billId")
    Long countByBillId(@Param("billId") String billId);

    /**
     * 检查票据是否有待审核的质押申请
     */
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM BillPledgeApplication a WHERE a.billId = :billId AND a.applicationStatus = 'PENDING'")
    boolean existsPendingApplicationByBillId(@Param("billId") String billId);

    /**
     * 查询日期范围内的质押申请
     */
    @Query("SELECT a FROM BillPledgeApplication a WHERE a.createdAt BETWEEN :startDate AND :endDate ORDER BY a.createdAt DESC")
    List<BillPledgeApplication> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);

    /**
     * 查询高风险质押申请
     */
    @Query("SELECT a FROM BillPledgeApplication a WHERE a.riskLevel = 'CRITICAL' AND a.applicationStatus = 'PENDING'")
    List<BillPledgeApplication> findHighRiskApplications();

    /**
     * 根据票据ID和状态查询
     */
    List<BillPledgeApplication> findByBillIdAndApplicationStatus(String billId,
                                                                 BillPledgeApplication.ApplicationStatus status);
}
