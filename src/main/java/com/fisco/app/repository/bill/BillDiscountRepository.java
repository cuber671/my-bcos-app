package com.fisco.app.repository.bill;

import com.fisco.app.entity.bill.BillDiscount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;

import java.util.List;


import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 票据贴现记录Repository
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-02
 */
@Repository
public interface BillDiscountRepository extends JpaRepository<BillDiscount, String> {

    /**
     * 根据票据ID查询贴现记录
     */
    List<BillDiscount> findByBillId(String billId);

    /**
     * 根据票据编号查询贴现记录
     */
    List<BillDiscount> findByBillNo(String billNo);

    /**
     * 根据申请状态查询
     */
    List<BillDiscount> findByApplicationStatus(BillDiscount.ApplicationStatus status);

    /**
     * 根据贴现机构ID查询
     */
    List<BillDiscount> findByDiscountInstitutionId(String discountInstitutionId);

    /**
     * 根据申请人ID查询
     */
    List<BillDiscount> findByApplicantId(String applicantId);

    /**
     * 查询待审核的贴现申请
     */
    @Query("SELECT d FROM BillDiscount d WHERE d.applicationStatus = 'PENDING' ORDER BY d.applicationDate DESC")
    List<BillDiscount> findPendingApplications();

    /**
     * 查询已付款的贴现记录
     */
    @Query("SELECT d FROM BillDiscount d WHERE d.applicationStatus = 'PAID' ORDER BY d.paymentDate DESC")
    List<BillDiscount> findPaidDiscounts();

    /**
     * 查询贴现机构的待审核申请
     */
    @Query("SELECT d FROM BillDiscount d WHERE d.discountInstitutionId = :institutionId AND d.applicationStatus = 'PENDING' ORDER BY d.applicationDate DESC")
    List<BillDiscount> findPendingApplicationsByInstitution(@Param("institutionId") String institutionId);

    /**
     * 统计申请人的贴现申请数量
     */
    @Query("SELECT COUNT(d) FROM BillDiscount d WHERE d.applicantId = :applicantId")
    Long countByApplicantId(@Param("applicantId") String applicantId);

    /**
     * 统计票据的贴现申请数量
     */
    @Query("SELECT COUNT(d) FROM BillDiscount d WHERE d.billId = :billId")
    Long countByBillId(@Param("billId") String billId);

    /**
     * 检查票据是否有待审核的贴现申请
     */
    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END FROM BillDiscount d WHERE d.billId = :billId AND d.applicationStatus = 'PENDING'")
    boolean existsPendingApplicationByBillId(@Param("billId") String billId);

    /**
     * 查询日期范围内的贴现申请
     */
    @Query("SELECT d FROM BillDiscount d WHERE d.applicationDate BETWEEN :startDate AND :endDate ORDER BY d.applicationDate DESC")
    List<BillDiscount> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    /**
     * 计算贴现机构的贴现总额
     */
    @Query("SELECT COALESCE(SUM(d.netAmount), 0) FROM BillDiscount d WHERE d.discountInstitutionId = :institutionId AND d.applicationStatus = 'PAID'")
    BigDecimal sumDiscountAmountByInstitution(@Param("institutionId") String institutionId);

    /**
     * 计算申请人的贴现总额
     */
    @Query("SELECT COALESCE(SUM(d.netAmount), 0) FROM BillDiscount d WHERE d.applicantId = :applicantId AND d.applicationStatus = 'PAID'")
    BigDecimal sumDiscountAmountByApplicant(@Param("applicantId") String applicantId);

    /**
     * 查询已批准但未付款的贴现记录
     */
    @Query("SELECT d FROM BillDiscount d WHERE d.applicationStatus = 'APPROVED' AND d.paymentDate IS NULL ORDER BY d.approvalDate DESC")
    List<BillDiscount> findApprovedButNotPaid();

    /**
     * 根据票据ID和状态查询
     */
    List<BillDiscount> findByBillIdAndApplicationStatus(String billId, BillDiscount.ApplicationStatus status);

    /**
     * 查询高贴现率的贴现记录
     */
    @Query("SELECT d FROM BillDiscount d WHERE d.discountRate > :rate ORDER BY d.discountRate DESC")
    List<BillDiscount> findHighRateDiscounts(@Param("rate") BigDecimal rate);

    /**
     * 统计贴机构的平均贴现率
     */
    @Query("SELECT AVG(d.discountRate) FROM BillDiscount d WHERE d.discountInstitutionId = :institutionId AND d.applicationStatus = 'PAID'")
    Double calculateAverageDiscountRate(@Param("institutionId") String institutionId);
}
