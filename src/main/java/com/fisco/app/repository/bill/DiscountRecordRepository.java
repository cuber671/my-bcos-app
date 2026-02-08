package com.fisco.app.repository.bill;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fisco.app.entity.bill.DiscountRecord;

/**
 * 票据贴现记录Repository
 */
@Repository
public interface DiscountRecordRepository extends JpaRepository<DiscountRecord, String> {

    /**
     * 根据票据ID查找贴现记录
     */
    List<DiscountRecord> findByBillIdOrderByDiscountDateDesc(String billId);

    /**
     * 根据票据ID查找最新的有效贴现记录
     */
    Optional<DiscountRecord> findFirstByBillIdAndStatusOrderByDiscountDateDesc(
        String billId,
        DiscountRecord.DiscountStatus status
    );

    /**
     * 查找持票人的所有贴现记录
     */
    List<DiscountRecord> findByHolderAddressOrderByDiscountDateDesc(String holderAddress);

    /**
     * 查找金融机构的所有贴现记录
     */
    List<DiscountRecord> findByFinancialInstitutionAddressOrderByDiscountDateDesc(String institutionAddress);

    /**
     * 根据贴现状态查找
     */
    List<DiscountRecord> findByStatus(DiscountRecord.DiscountStatus status);

    /**
     * 查询即将到期的贴现记录
     */
    @Query("SELECT d FROM DiscountRecord d WHERE d.maturityDate BETWEEN :startDate AND :endDate AND d.status = 'ACTIVE'")
    List<DiscountRecord> findMaturingSoon(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * 查询已到期但未还款的贴现记录
     */
    @Query("SELECT d FROM DiscountRecord d WHERE d.maturityDate < :now AND d.status = 'ACTIVE'")
    List<DiscountRecord> findOverdueDiscounts(@Param("now") LocalDateTime now);

    /**
     * 统计金融机构的贴现总金额
     */
    @Query("SELECT SUM(d.discountAmount) FROM DiscountRecord d WHERE d.financialInstitutionAddress = :institutionAddress AND d.status = 'ACTIVE'")
    java.math.BigDecimal totalActiveDiscountAmountByInstitution(@Param("institutionAddress") String institutionAddress);

    /**
     * 统计金融机构的贴现总利息收入
     */
    @Query("SELECT SUM(d.discountInterest) FROM DiscountRecord d WHERE d.financialInstitutionAddress = :institutionAddress")
    java.math.BigDecimal totalDiscountInterestByInstitution(@Param("institutionAddress") String institutionAddress);
}
