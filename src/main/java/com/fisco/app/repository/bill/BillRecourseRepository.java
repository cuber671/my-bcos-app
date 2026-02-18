package com.fisco.app.repository.bill;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fisco.app.entity.bill.BillRecourse;

/**
 * 票据追索记录Repository
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-02
 */
@Repository
public interface BillRecourseRepository extends JpaRepository<BillRecourse, String> {

    /**
     * 根据票据ID查询追索记录
     */
    List<BillRecourse> findByBillId(String billId);

    /**
     * 根据票据编号查询追索记录
     */
    List<BillRecourse> findByBillNo(String billNo);

    /**
     * 根据追索状态查询
     */
    List<BillRecourse> findByRecourseStatus(BillRecourse.RecourseStatus status);

    /**
     * 根据发起人ID查询
     */
    List<BillRecourse> findByInitiatorId(String initiatorId);

    /**
     * 查询进行中的追索
     */
    @Query("SELECT r FROM BillRecourse r WHERE r.recourseStatus IN ('INITIATED', 'IN_PROGRESS') ORDER BY r.createdAt DESC")
    List<BillRecourse> findActiveRecourses();

    /**
     * 查询已完成的追索
     */
    @Query("SELECT r FROM BillRecourse r WHERE r.recourseStatus = 'COMPLETED' ORDER BY r.completedAt DESC")
    List<BillRecourse> findCompletedRecourses();

    /**
     * 查询涉及法律诉讼的追索
     */
    @Query("SELECT r FROM BillRecourse r WHERE r.legalAction = true ORDER BY r.createdAt DESC")
    List<BillRecourse> findLegalActionRecourses();

    /**
     * 查询日期范围内的追索记录
     */
    @Query("SELECT r FROM BillRecourse r WHERE r.dishonoredDate BETWEEN :startDate AND :endDate ORDER BY r.dishonoredDate DESC")
    List<BillRecourse> findByDishonoredDateRange(@Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate);

    /**
     * 统计发起人的追索次数
     */
    @Query("SELECT COUNT(r) FROM BillRecourse r WHERE r.initiatorId = :initiatorId")
    Long countByInitiatorId(@Param("initiatorId") String initiatorId);

    /**
     * 统计票据的追索次数
     */
    @Query("SELECT COUNT(r) FROM BillRecourse r WHERE r.billId = :billId")
    Long countByBillId(@Param("billId") String billId);

    /**
     * 计算发起人的追回总金额
     */
    @Query("SELECT COALESCE(SUM(r.settledAmount), 0) FROM BillRecourse r WHERE r.initiatorId = :initiatorId AND r.recourseStatus = 'COMPLETED'")
    BigDecimal sumSettledAmountByInitiator(@Param("initiatorId") String initiatorId);

    /**
     * 查询部分追回的追索记录
     */
    @Query("SELECT r FROM BillRecourse r WHERE r.recourseStatus = 'PARTIAL' ORDER BY r.settlementDate DESC")
    List<BillRecourse> findPartialRecourses();

    /**
     * 查询失败的追索记录
     */
    @Query("SELECT r FROM BillRecourse r WHERE r.recourseStatus = 'FAILED' ORDER BY r.createdAt DESC")
    List<BillRecourse> findFailedRecourses();

    /**
     * 根据票据ID和状态查询
     */
    List<BillRecourse> findByBillIdAndRecourseStatus(String billId, BillRecourse.RecourseStatus status);

    /**
     * 查询未通知前手的追索记录
     */
    @Query("SELECT r FROM BillRecourse r WHERE r.notificationDate IS NULL AND r.recourseStatus IN ('INITIATED', 'IN_PROGRESS')")
    List<BillRecourse> findPendingNotificationRecourses();

    /**
     * 统计追索成功率
     */
    @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN " +
           "CAST(SUM(CASE WHEN recourse_status = 'COMPLETED' THEN 1 ELSE 0 END) AS FLOAT) / COUNT(*) * 100 " +
           "ELSE 0 END " +
           "FROM bill_recourse " +
           "WHERE initiator_id = :initiatorId",
           nativeQuery = true)
    Double calculateRecourseSuccessRate(@Param("initiatorId") String initiatorId);
}
