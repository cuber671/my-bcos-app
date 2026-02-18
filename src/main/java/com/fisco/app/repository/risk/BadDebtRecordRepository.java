package com.fisco.app.repository.risk;

import com.fisco.app.entity.risk.BadDebtRecord;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;

import java.util.List;

import java.math.BigDecimal;

/**
 * 坏账记录Repository
 */
@Repository
public interface BadDebtRecordRepository extends JpaRepository<BadDebtRecord, String> {

    /**
     * 根据应收账款ID查询坏账记录
     */
    BadDebtRecord findByReceivableId(String receivableId);

    /**
     * 根据坏账类型查询记录
     */
    List<BadDebtRecord> findByBadDebtTypeOrderByCreatedAtDesc(BadDebtRecord.BadDebtType badDebtType);

    /**
     * 根据回收状态查询记录
     */
    List<BadDebtRecord> findByRecoveryStatusOrderByCreatedAtDesc(BadDebtRecord.RecoveryStatus recoveryStatus);

    /**
     * 查询所有未回收的坏账
     */
    @Query("SELECT b FROM BadDebtRecord b WHERE b.recoveryStatus = 'NOT_RECOVERED' OR b.recoveryStatus = 'PARTIAL_RECOVERED' ORDER BY b.createdAt DESC")
    List<BadDebtRecord> findUnrecoveredBadDebts();

    /**
     * 统计坏账总本金
     */
    @Query("SELECT COALESCE(SUM(b.principalAmount), 0) FROM BadDebtRecord b")
    BigDecimal sumPrincipalAmount();

    /**
     * 统计坏账总损失金额
     */
    @Query("SELECT COALESCE(SUM(b.totalLossAmount), 0) FROM BadDebtRecord b")
    BigDecimal sumTotalLossAmount();

    /**
     * 统计已回收总金额
     */
    @Query("SELECT COALESCE(SUM(b.recoveredAmount), 0) FROM BadDebtRecord b WHERE b.recoveryStatus IN ('PARTIAL_RECOVERED', 'FULL_RECOVERED')")
    BigDecimal sumRecoveredAmount();

    /**
     * 统计各回收状态的坏账数量
     */
    @Query("SELECT b.recoveryStatus, COUNT(b) FROM BadDebtRecord b GROUP BY b.recoveryStatus")
    List<Object[]> countByRecoveryStatus();

    /**
     * 根据条件分页查询坏账记录
     *
     * @param badDebtType 坏账类型（可选）
     * @param recoveryStatus 回收状态（可选）
     * @param overdueDaysMin 最小逾期天数（可选）
     * @param createdDateStart 创建日期起始（可选）
     * @param createdDateEnd 创建日期结束（可选）
     * @param pageable 分页参数
     * @return 分页结果
     */
    @Query("SELECT b FROM BadDebtRecord b WHERE " +
           "(:badDebtType IS NULL OR b.badDebtType = :badDebtType) AND " +
           "(:recoveryStatus IS NULL OR b.recoveryStatus = :recoveryStatus) AND " +
           "(:overdueDaysMin IS NULL OR b.overdueDays >= :overdueDaysMin) AND " +
           "(:createdDateStart IS NULL OR b.createdAt >= :createdDateStart) AND " +
           "(:createdDateEnd IS NULL OR b.createdAt <= :createdDateEnd)")
    Page<BadDebtRecord> findBadDebtsByConditions(
            @Param("badDebtType") BadDebtRecord.BadDebtType badDebtType,
            @Param("recoveryStatus") BadDebtRecord.RecoveryStatus recoveryStatus,
            @Param("overdueDaysMin") Integer overdueDaysMin,
            @Param("createdDateStart") java.time.LocalDateTime createdDateStart,
            @Param("createdDateEnd") java.time.LocalDateTime createdDateEnd,
            Pageable pageable);
}
