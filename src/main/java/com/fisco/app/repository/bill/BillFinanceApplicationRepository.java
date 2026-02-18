package com.fisco.app.repository.bill;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fisco.app.entity.bill.BillFinanceApplication;

/**
 * 票据融资申请Repository
 */
@Repository
public interface BillFinanceApplicationRepository extends JpaRepository<BillFinanceApplication, String> {

    /**
     * 根据票据ID查找融资申请
     */
    List<BillFinanceApplication> findByBillId(String billId);

    /**
     * 根据申请人ID查找融资申请
     */
    List<BillFinanceApplication> findByApplicantId(String applicantId);

    /**
     * 根据金融机构ID查找融资申请
     */
    List<BillFinanceApplication> findByFinancialInstitutionId(String financialInstitutionId);

    /**
     * 查找待审核的融资申请
     */
    @Query("SELECT a FROM BillFinanceApplication a WHERE a.status = 'PENDING' ORDER BY a.applyDate ASC")
    List<BillFinanceApplication> findPendingApplications();

    /**
     * 查找特定金融机构的待审核申请
     */
    @Query("SELECT a FROM BillFinanceApplication a WHERE a.financialInstitutionId = :institutionId AND a.status = 'PENDING' ORDER BY a.applyDate ASC")
    List<BillFinanceApplication> findPendingApplicationsByInstitution(@Param("institutionId") String institutionId);

    /**
     * 查找活跃的融资申请（已放款未还款）
     */
    @Query("SELECT a FROM BillFinanceApplication a WHERE a.status = 'ACTIVE' ORDER BY a.disbursementDate DESC")
    List<BillFinanceApplication> findActiveApplications();

    /**
     * 统计企业的融资申请数量
     */
    @Query("SELECT COUNT(a) FROM BillFinanceApplication a WHERE a.applicantId = :applicantId")
    Long countByApplicant(@Param("applicantId") String applicantId);

    /**
     * 查询特定时间范围内的融资申请
     */
    @Query("SELECT a FROM BillFinanceApplication a WHERE a.applyDate BETWEEN :startDate AND :endDate ORDER BY a.applyDate DESC")
    List<BillFinanceApplication> findByApplyDateBetween(
        @Param("startDate") java.time.LocalDateTime startDate,
        @Param("endDate") java.time.LocalDateTime endDate
    );
}
