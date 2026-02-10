package com.fisco.app.repository.bill;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fisco.app.entity.bill.BillGuarantee;

/**
 * 票据担保Repository
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-09
 * @version 1.0
 */
@Repository
public interface BillGuaranteeRepository extends JpaRepository<BillGuarantee, String> {

    // ==================== 基础查询 ====================

    /**
     * 根据票据ID查询所有担保记录
     */
    List<BillGuarantee> findByBillId(String billId);

    /**
     * 根据担保方ID查询所有担保记录
     */
    List<BillGuarantee> findByGuarantorId(String guarantorId);

    /**
     * 根据票据ID和担保方ID查询担保记录
     */
    List<BillGuarantee> findByBillIdAndGuarantorId(String billId, String guarantorId);

    /**
     * 根据状态查询担保记录
     */
    List<BillGuarantee> findByStatus(String status);

    /**
     * 根据票据ID和状态查询担保记录
     */
    List<BillGuarantee> findByBillIdAndStatus(String billId, String status);

    // ==================== 统计查询 ====================

    /**
     * 统计票据的有效担保金额总和
     */
    @Query("SELECT COALESCE(SUM(bg.guaranteeAmount), 0) FROM BillGuarantee bg " +
           "WHERE bg.billId = :billId AND bg.status = 'ACTIVE'")
    Long sumGuaranteeAmountByBillId(@Param("billId") String billId);

    /**
     * 统计担保方的担保数量
     */
    @Query("SELECT COUNT(bg) FROM BillGuarantee bg WHERE bg.guarantorId = :guarantorId")
    Long countByGuarantorId(@Param("guarantorId") String guarantorId);

    /**
     * 统计票据的担保数量
     */
    @Query("SELECT COUNT(bg) FROM BillGuarantee bg WHERE bg.billId = :billId")
    Long countByBillId(@Param("billId") String billId);
}
