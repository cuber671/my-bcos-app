package com.fisco.app.repository.bill;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fisco.app.entity.bill.RepaymentRecord;

/**
 * 票据还款记录Repository
 */
@Repository
public interface RepaymentRecordRepository extends JpaRepository<RepaymentRecord, String> {

    /**
     * 根据票据ID查找还款记录
     */
    List<RepaymentRecord> findByBillIdOrderByPaymentDateDesc(String billId);

    /**
     * 查找票据的最新还款记录
     */
    Optional<RepaymentRecord> findFirstByBillIdOrderByPaymentDateDesc(String billId);

    /**
     * 查找还款人的所有还款记录
     */
    List<RepaymentRecord> findByPayerAddressOrderByPaymentDateDesc(String payerAddress);

    /**
     * 查找金融机构的所有还款记录
     */
    List<RepaymentRecord> findByFinancialInstitutionAddressOrderByPaymentDateDesc(String institutionAddress);

    /**
     * 根据还款类型查找
     */
    List<RepaymentRecord> findByPaymentType(RepaymentRecord.PaymentType paymentType);

    /**
     * 根据还款状态查找
     */
    List<RepaymentRecord> findByStatus(RepaymentRecord.PaymentStatus status);

    /**
     * 查询指定时间范围内的还款记录
     */
    @Query("SELECT r FROM RepaymentRecord r WHERE r.paymentDate BETWEEN :startDate AND :endDate")
    List<RepaymentRecord> findByPaymentDateBetween(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * 查询逾期还款记录
     */
    @Query("SELECT r FROM RepaymentRecord r WHERE r.overdueDays > 0")
    List<RepaymentRecord> findOverduePayments();

    /**
     * 统计票据的已还总金额
     */
    @Query("SELECT SUM(r.paymentAmount) FROM RepaymentRecord r WHERE r.billId = :billId AND r.status = 'COMPLETED'")
    java.math.BigDecimal totalPaymentAmountByBillId(@Param("billId") String billId);

    /**
     * 统计金融机构的还款总金额
     */
    @Query("SELECT SUM(r.paymentAmount) FROM RepaymentRecord r WHERE r.financialInstitutionAddress = :institutionAddress AND r.status = 'COMPLETED'")
    java.math.BigDecimal totalPaymentAmountByInstitution(@Param("institutionAddress") String institutionAddress);

    /**
     * 统计金融机构的利息总收入
     */
    @Query("SELECT SUM(r.interestAmount) FROM RepaymentRecord r WHERE r.financialInstitutionAddress = :institutionAddress AND r.status = 'COMPLETED'")
    java.math.BigDecimal totalInterestByInstitution(@Param("institutionAddress") String institutionAddress);

    /**
     * 统计逾期利息总收入
     */
    @Query("SELECT SUM(r.penaltyInterestAmount) FROM RepaymentRecord r WHERE r.financialInstitutionAddress = :institutionAddress AND r.status = 'COMPLETED'")
    java.math.BigDecimal totalPenaltyInterestByInstitution(@Param("institutionAddress") String institutionAddress);
}
