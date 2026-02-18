package com.fisco.app.repository.bill;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fisco.app.entity.bill.BillSettlement;

/**
 * 票据结算记录Repository
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-02
 */
@Repository
public interface BillSettlementRepository extends JpaRepository<BillSettlement, String> {

    /**
     * 根据票据ID查询结算记录
     */
    List<BillSettlement> findByBillId(String billId);

    /**
     * 根据票据编号查询结算记录
     */
    List<BillSettlement> findByBillNo(String billNo);

    /**
     * 根据结算类型查询
     */
    List<BillSettlement> findBySettlementType(BillSettlement.SettlementType settlementType);

    /**
     * 根据结算状态查询
     */
    List<BillSettlement> findBySettlementStatus(BillSettlement.SettlementStatus settlementStatus);

    /**
     * 根据发起人ID查询
     */
    List<BillSettlement> findByInitiatorId(String initiatorId);

    /**
     * 查询待结算的记录
     */
    @Query("SELECT s FROM BillSettlement s WHERE s.settlementStatus = 'PENDING' ORDER BY s.settlementDate DESC")
    List<BillSettlement> findPendingSettlements();

    /**
     * 查询已完成的结算
     */
    @Query("SELECT s FROM BillSettlement s WHERE s.settlementStatus = 'COMPLETED' ORDER BY s.completionDate DESC")
    List<BillSettlement> findCompletedSettlements();

    /**
     * 查询三角债结算
     */
    @Query("SELECT s FROM BillSettlement s WHERE s.settlementType = 'TRIANGULAR' ORDER BY s.settlementDate DESC")
    List<BillSettlement> findTriangularSettlements();

    /**
     * 查询涉及仓单转让的结算
     */
    @Query("SELECT s FROM BillSettlement s WHERE s.receiptTransfer = true ORDER BY s.settlementDate DESC")
    List<BillSettlement> findReceiptTransferSettlements();

    /**
     * 查询日期范围内的结算记录
     */
    @Query("SELECT s FROM BillSettlement s WHERE s.settlementDate BETWEEN :startDate AND :endDate ORDER BY s.settlementDate DESC")
    List<BillSettlement> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    /**
     * 统计发起人的结算次数
     */
    @Query("SELECT COUNT(s) FROM BillSettlement s WHERE s.initiatorId = :initiatorId")
    Long countByInitiatorId(@Param("initiatorId") String initiatorId);

    /**
     * 统计票据的结算次数
     */
    @Query("SELECT COUNT(s) FROM BillSettlement s WHERE s.billId = :billId")
    Long countByBillId(@Param("billId") String billId);

    /**
     * 计算发起人的结算总额
     */
    @Query("SELECT COALESCE(SUM(s.settlementAmount), 0) FROM BillSettlement s WHERE s.initiatorId = :initiatorId AND s.settlementStatus = 'COMPLETED'")
    BigDecimal sumSettlementAmountByInitiator(@Param("initiatorId") String initiatorId);

    /**
     * 查询部分结算的记录
     */
    @Query("SELECT s FROM BillSettlement s WHERE s.settlementStatus = 'PARTIAL' ORDER BY s.settlementDate DESC")
    List<BillSettlement> findPartialSettlements();

    /**
     * 查询失败的结算记录
     */
    @Query("SELECT s FROM BillSettlement s WHERE s.settlementStatus = 'FAILED' ORDER BY s.settlementDate DESC")
    List<BillSettlement> findFailedSettlements();

    /**
     * 根据票据ID和状态查询
     */
    List<BillSettlement> findByBillIdAndSettlementStatus(String billId, BillSettlement.SettlementStatus status);

    /**
     * 统计结算成功率
     */
    @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN " +
           "CAST(SUM(CASE WHEN settlement_status = 'COMPLETED' THEN 1 ELSE 0 END) AS FLOAT) / COUNT(*) * 100 " +
           "ELSE 0 END " +
           "FROM bill_settlement " +
           "WHERE initiator_id = :initiatorId",
           nativeQuery = true)
    Double calculateSettlementSuccessRate(@Param("initiatorId") String initiatorId);

    /**
     * 查询多方结算记录
     */
    @Query("SELECT s FROM BillSettlement s WHERE s.settlementType = 'MULTILATERAL' ORDER BY s.settlementDate DESC")
    List<BillSettlement> findMultilateralSettlements();

    /**
     * 根据票据ID和结算类型查询
     */
    List<BillSettlement> findByBillIdAndSettlementType(String billId, BillSettlement.SettlementType settlementType);

    /**
     * 查询结算金额超过指定金额的记录
     */
    @Query("SELECT s FROM BillSettlement s WHERE s.settlementAmount >= :amount ORDER BY s.settlementAmount DESC")
    List<BillSettlement> findLargeAmountSettlements(@Param("amount") BigDecimal amount);
}
