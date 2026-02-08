package com.fisco.app.repository.bill;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fisco.app.entity.bill.BillInvestment;

/**
 * 票据投资Repository
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-03
 */
@Repository
public interface BillInvestmentRepository extends JpaRepository<BillInvestment, String> {

    /**
     * 根据票据ID查询投资记录
     */
    List<BillInvestment> findByBillId(String billId);

    /**
     * 根据票据ID和状态查询投资记录
     */
    List<BillInvestment> findByBillIdAndStatus(String billId, String status);

    /**
     * 查询特定票据的待确认投资
     */
    @Query("SELECT i FROM BillInvestment i WHERE i.billId = :billId AND i.status = 'PENDING'")
    List<BillInvestment> findPendingInvestmentsByBillId(@Param("billId") String billId);

    /**
     * 根据投资机构ID查询投资记录
     */
    List<BillInvestment> findByInvestorId(String investorId);

    /**
     * 查询特定投资机构的投资记录（按投资时间倒序）
     */
    @Query("SELECT i FROM BillInvestment i WHERE i.investorId = :investorId ORDER BY i.investmentDate DESC")
    List<BillInvestment> findByInvestorIdOrderByDateDesc(@Param("investorId") String investorId);

    /**
     * 查询特定投资机构的待确认投资
     */
    @Query("SELECT i FROM BillInvestment i WHERE i.investorId = :investorId AND i.status = 'PENDING' ORDER BY i.investmentDate DESC")
    List<BillInvestment> findPendingInvestmentsByInvestorId(@Param("investorId") String investorId);

    /**
     * 查询所有待确认的投资
     */
    @Query("SELECT i FROM BillInvestment i WHERE i.status = 'PENDING' ORDER BY i.investmentDate ASC")
    List<BillInvestment> findAllPendingInvestments();

    /**
     * 查询已确认但未完成的投资
     */
    @Query("SELECT i FROM BillInvestment i WHERE i.status = 'CONFIRMED' ORDER BY i.confirmationDate ASC")
    List<BillInvestment> findConfirmedInvestments();

    /**
     * 查询已完成的投资
     */
    @Query("SELECT i FROM BillInvestment i WHERE i.status = 'COMPLETED' ORDER BY i.completionDate DESC")
    List<BillInvestment> findCompletedInvestments();

    /**
     * 统计投资机构的投资数量
     */
    @Query("SELECT COUNT(i) FROM BillInvestment i WHERE i.investorId = :investorId")
    Long countByInvestorId(@Param("investorId") String investorId);

    /**
     * 统计特定票据的投资数量
     */
    @Query("SELECT COUNT(i) FROM BillInvestment i WHERE i.billId = :billId")
    Long countByBillId(@Param("billId") String billId);

    /**
     * 查询特定时间范围内的投资
     */
    @Query("SELECT i FROM BillInvestment i WHERE i.investmentDate BETWEEN :startDate AND :endDate ORDER BY i.investmentDate DESC")
    List<BillInvestment> findByInvestmentDateBetween(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * 查询即将到期的投资（30天内）
     */
    @Query("SELECT i FROM BillInvestment i WHERE i.status = 'CONFIRMED' AND i.maturityAmount IS NOT NULL AND FUNCTION('DATEDIFF', DAY, :currentDate, i.completionDate) <= 30 ORDER BY i.completionDate ASC")
    List<BillInvestment> findUpcomingMaturityInvestments(@Param("currentDate") LocalDateTime currentDate);

    /**
     * 查询已到期的投资（未结算）
     */
    @Query("SELECT i FROM BillInvestment i WHERE i.status = 'CONFIRMED' AND i.completionDate <= :currentDate AND i.settlementDate IS NULL ORDER BY i.completionDate ASC")
    List<BillInvestment> findMaturedUnsettledInvestments(@Param("currentDate") LocalDateTime currentDate);

    /**
     * 计算投资机构的总投资收益
     */
    @Query("SELECT SUM(i.actualReturn) FROM BillInvestment i WHERE i.investorId = :investorId AND i.status = 'COMPLETED' AND i.actualReturn IS NOT NULL")
    BigDecimal getTotalReturnByInvestor(@Param("investorId") String investorId);

    /**
     * 查询票据的所有投资记录（包括已取消的）
     */
    @Query("SELECT i FROM BillInvestment i WHERE i.billId = :billId ORDER BY i.investmentDate DESC")
    List<BillInvestment> findAllByBillIdOrderByDateDesc(@Param("billId") String billId);

    /**
     * 检查票据是否有待确认的投资
     */
    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM BillInvestment i WHERE i.billId = :billId AND i.status = 'PENDING'")
    boolean existsPendingInvestmentByBillId(@Param("billId") String billId);

    /**
     * 查询原持票人的票据转让记录
     */
    @Query("SELECT i FROM BillInvestment i WHERE i.originalHolderId = :holderId ORDER BY i.investmentDate DESC")
    List<BillInvestment> findByOriginalHolderId(@Param("holderId") String holderId);
}
