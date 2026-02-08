package com.fisco.app.repository.risk;

import com.fisco.app.entity.risk.BadDebtRecord;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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
}
