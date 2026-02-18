package com.fisco.app.repository.credit;

import com.fisco.app.entity.credit.CreditLimitWarning;
import com.fisco.app.enums.CreditWarningLevel;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 信用额度预警记录Repository
 */
@Repository
public interface CreditLimitWarningRepository extends JpaRepository<CreditLimitWarning, String>, JpaSpecificationExecutor<CreditLimitWarning> {

    /**
     * 根据额度ID查找预警记录
     */
    List<CreditLimitWarning> findByCreditLimitId(String creditLimitId);

    /**
     * 根据额度ID查找预警记录（分页）
     */
    Page<CreditLimitWarning> findByCreditLimitId(String creditLimitId, Pageable pageable);

    /**
     * 根据预警级别查找预警记录（分页）
     */
    Page<CreditLimitWarning> findByWarningLevel(CreditWarningLevel warningLevel, Pageable pageable);

    /**
     * 根据额度ID和预警级别查找预警记录
     */
    List<CreditLimitWarning> findByCreditLimitIdAndWarningLevel(String creditLimitId, CreditWarningLevel warningLevel);

    /**
     * 根据预警类型查找预警记录
     */
    List<CreditLimitWarning> findByWarningType(String warningType);

    /**
     * 根据预警类型查找预警记录（分页）
     */
    Page<CreditLimitWarning> findByWarningType(String warningType, Pageable pageable);

    /**
     * 根据是否已处理查找预警记录（分页）
     */
    Page<CreditLimitWarning> findByIsResolved(Boolean isResolved, Pageable pageable);

    /**
     * 根据额度ID和是否已处理查找预警记录
     */
    List<CreditLimitWarning> findByCreditLimitIdAndIsResolved(String creditLimitId, Boolean isResolved);

    /**
     * 查找所有未处理的预警记录
     */
    @Query("SELECT w FROM CreditLimitWarning w WHERE w.isResolved = false " +
           "ORDER BY w.warningLevel DESC, w.warningDate DESC")
    List<CreditLimitWarning> findAllUnresolvedWarnings();

    /**
     * 查找所有未处理的预警记录（分页）
     */
    Page<CreditLimitWarning> findByIsResolvedFalse(Pageable pageable);

    /**
     * 根据时间范围查找预警记录
     */
    @Query("SELECT w FROM CreditLimitWarning w WHERE w.warningDate BETWEEN :startDate AND :endDate " +
           "ORDER BY w.warningDate DESC")
    List<CreditLimitWarning> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);

    /**
     * 根据额度ID和时间范围查找预警记录
     */
    @Query("SELECT w FROM CreditLimitWarning w WHERE w.creditLimitId = :creditLimitId " +
           "AND w.warningDate BETWEEN :startDate AND :endDate ORDER BY w.warningDate DESC")
    List<CreditLimitWarning> findByCreditLimitIdAndDateRange(@Param("creditLimitId") String creditLimitId,
                                                               @Param("startDate") LocalDateTime startDate,
                                                               @Param("endDate") LocalDateTime endDate);

    /**
     * 查找最近的预警记录
     */
    @Query("SELECT w FROM CreditLimitWarning w WHERE w.creditLimitId = :creditLimitId " +
           "ORDER BY w.warningDate DESC")
    List<CreditLimitWarning> findRecentWarningsByCreditLimitId(@Param("creditLimitId") String creditLimitId,
                                                                 Pageable pageable);

    /**
     * 统计额度预警次数
     */
    long countByCreditLimitId(String creditLimitId);

    /**
     * 统计指定级别的预警次数
     */
    long countByWarningLevel(CreditWarningLevel warningLevel);

    /**
     * 统计额度未处理的预警次数
     */
    long countByCreditLimitIdAndIsResolved(String creditLimitId, Boolean isResolved);

    /**
     * 统计所有未处理的预警次数
     */
    long countByIsResolvedFalse();

    /**
     * 统计指定时间范围内的预警次数
     */
    @Query("SELECT COUNT(w) FROM CreditLimitWarning w WHERE w.creditLimitId = :creditLimitId " +
           "AND w.warningDate BETWEEN :startDate AND :endDate")
    long countByCreditLimitIdAndDateRange(@Param("creditLimitId") String creditLimitId,
                                          @Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    /**
     * 查找企业的未处理预警
     */
    @Query("SELECT w FROM CreditLimitWarning w WHERE w.creditLimitId IN " +
           "(SELECT cl.id FROM CreditLimit cl WHERE cl.enterpriseAddress = :enterpriseAddress) " +
           "AND w.isResolved = false ORDER BY w.warningLevel DESC, w.warningDate DESC")
    List<CreditLimitWarning> findUnresolvedWarningsByEnterpriseAddress(@Param("enterpriseAddress") String enterpriseAddress);

    /**
     * 查找指定时间范围内未处理的预警
     */
    @Query("SELECT w FROM CreditLimitWarning w WHERE w.isResolved = false " +
           "AND w.warningDate BETWEEN :startDate AND :endDate " +
           "ORDER BY w.warningLevel DESC, w.warningDate DESC")
    List<CreditLimitWarning> findUnresolvedWarningsByDateRange(@Param("startDate") LocalDateTime startDate,
                                                                 @Param("endDate") LocalDateTime endDate);

    /**
     * 统计企业未处理的预警次数
     */
    @Query("SELECT COUNT(w) FROM CreditLimitWarning w WHERE w.creditLimitId IN " +
           "(SELECT cl.id FROM CreditLimit cl WHERE cl.enterpriseAddress = :enterpriseAddress) " +
           "AND w.isResolved = false")
    long countUnresolvedWarningsByEnterpriseAddress(@Param("enterpriseAddress") String enterpriseAddress);

    /**
     * 查找最近的高级别预警（HIGH或CRITICAL）
     */
    @Query("SELECT w FROM CreditLimitWarning w WHERE w.warningLevel IN ('HIGH', 'CRITICAL') " +
           "AND w.isResolved = false ORDER BY w.warningLevel DESC, w.warningDate DESC")
    List<CreditLimitWarning> findRecentHighPriorityWarnings(Pageable pageable);
}
