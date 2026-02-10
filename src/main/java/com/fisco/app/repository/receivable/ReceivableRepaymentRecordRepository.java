package com.fisco.app.repository.receivable;

import com.fisco.app.entity.receivable.ReceivableRepaymentRecord;
import com.fisco.app.entity.receivable.ReceivableRepaymentRecord.RepaymentStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 应收账款还款记录Repository接口
 *
 * 提供还款记录的查询和统计方法
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-09
 * @version 1.0
 */
public interface ReceivableRepaymentRecordRepository extends JpaRepository<ReceivableRepaymentRecord, String> {

    /**
     * 查询应收账款的所有还款记录（按时间倒序）
     *
     * @param receivableId 应收账款ID
     * @return 还款记录列表
     */
    List<ReceivableRepaymentRecord> findByReceivableIdOrderByActualPaymentTimeDesc(String receivableId);

    /**
     * 计算应收账款的已还总金额（只统计已确认的还款）
     *
     * @param receivableId 应收账款ID
     * @return 已还总金额
     */
    @Query("SELECT COALESCE(SUM(r.repaymentAmount), 0) FROM ReceivableRepaymentRecord r WHERE r.receivableId = :receivableId AND r.status = 'CONFIRMED'")
    BigDecimal totalRepaidAmountByReceivable(@Param("receivableId") String receivableId);

    /**
     * 统计应收账款的还款次数
     *
     * @param receivableId 应收账款ID
     * @param status 还款状态
     * @return 还款次数
     */
    long countByReceivableIdAndStatus(String receivableId, RepaymentStatus status);

    /**
     * 查询指定日期范围的还款记录（按日期倒序）
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 还款记录列表
     */
    List<ReceivableRepaymentRecord> findByPaymentDateBetweenOrderByPaymentDateDesc(
        LocalDate startDate, LocalDate endDate);

    /**
     * 查询还款人的还款记录（按时间倒序）
     *
     * @param payerAddress 还款人地址
     * @return 还款记录列表
     */
    List<ReceivableRepaymentRecord> findByPayerAddressOrderByActualPaymentTimeDesc(String payerAddress);

    /**
     * 查询收款人的收款记录（按时间倒序）
     *
     * @param receiverAddress 收款人地址
     * @return 还款记录列表
     */
    List<ReceivableRepaymentRecord> findByReceiverAddressOrderByActualPaymentTimeDesc(String receiverAddress);
}
