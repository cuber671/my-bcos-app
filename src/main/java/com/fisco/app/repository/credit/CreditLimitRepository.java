package com.fisco.app.repository.credit;

import com.fisco.app.entity.credit.CreditLimit;
import com.fisco.app.enums.CreditLimitStatus;
import com.fisco.app.enums.CreditLimitType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 信用额度Repository
 */
@Repository
public interface CreditLimitRepository extends JpaRepository<CreditLimit, String>, JpaSpecificationExecutor<CreditLimit> {

    @Override
    @NonNull
    Optional<CreditLimit> findById(@NonNull String id);

    /**
     * 根据企业地址查找所有额度
     */
    List<CreditLimit> findByEnterpriseAddress(String enterpriseAddress);

    /**
     * 根据企业地址和额度类型查找额度
     */
    Optional<CreditLimit> findByEnterpriseAddressAndLimitType(String enterpriseAddress, CreditLimitType limitType);

    /**
     * 根据企业地址、额度类型和状态查找额度
     */
    Optional<CreditLimit> findByEnterpriseAddressAndLimitTypeAndStatus(
            String enterpriseAddress, CreditLimitType limitType, CreditLimitStatus status);

    /**
     * 根据企业地址和状态查找额度
     */
    List<CreditLimit> findByEnterpriseAddressAndStatus(String enterpriseAddress, CreditLimitStatus status);

    /**
     * 根据状态查找额度（分页）
     */
    Page<CreditLimit> findByStatus(CreditLimitStatus status, Pageable pageable);

    /**
     * 根据额度类型查找额度（分页）
     */
    Page<CreditLimit> findByLimitType(CreditLimitType limitType, Pageable pageable);

    /**
     * 根据风险等级查找额度
     */
    List<CreditLimit> findByRiskLevel(CreditLimit.RiskLevel riskLevel);

    /**
     * 根据风险等级查找额度（分页）
     */
    Page<CreditLimit> findByRiskLevel(CreditLimit.RiskLevel riskLevel, Pageable pageable);

    /**
     * 查找所有即将到期的额度（30天内到期）
     */
    @Query("SELECT cl FROM CreditLimit cl WHERE cl.status = 'ACTIVE' " +
           "AND cl.expiryDate BETWEEN :startDate AND :endDate " +
           "ORDER BY cl.expiryDate ASC")
    List<CreditLimit> findExpiringLimits(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    /**
     * 查找使用率超过阈值的额度
     */
    @Query("SELECT cl FROM CreditLimit cl WHERE cl.status = 'ACTIVE' " +
           "AND (cl.usedLimit * 100.0 / cl.totalLimit) >= :threshold")
    List<CreditLimit> findByUsageRateAboveThreshold(@Param("threshold") Double threshold);

    /**
     * 查找需要预警的额度
     */
    @Query("SELECT cl FROM CreditLimit cl WHERE cl.status = 'ACTIVE' " +
           "AND (cl.usedLimit * 100.0 / cl.totalLimit) >= cl.warningThreshold")
    List<CreditLimit> findLimitsNeedingWarning();

    /**
     * 统计企业的额度数量
     */
    long countByEnterpriseAddress(String enterpriseAddress);

    /**
     * 统计指定状态的额度数量
     */
    long countByStatus(CreditLimitStatus status);

    /**
     * 统计指定风险等级的额度数量
     */
    long countByRiskLevel(CreditLimit.RiskLevel riskLevel);

    /**
     * 计算企业的总可用额度
     */
    @Query("SELECT COALESCE(SUM(cl.totalLimit - cl.usedLimit - cl.frozenLimit), 0) " +
           "FROM CreditLimit cl WHERE cl.enterpriseAddress = :enterpriseAddress " +
           "AND cl.status = 'ACTIVE'")
    Long getTotalAvailableLimitByEnterprise(@Param("enterpriseAddress") String enterpriseAddress);

    /**
     * 计算企业的总已使用额度
     */
    @Query("SELECT COALESCE(SUM(cl.usedLimit), 0) FROM CreditLimit cl " +
           "WHERE cl.enterpriseAddress = :enterpriseAddress AND cl.status = 'ACTIVE'")
    Long getTotalUsedLimitByEnterprise(@Param("enterpriseAddress") String enterpriseAddress);

    /**
     * 计算企业的总冻结额度
     */
    @Query("SELECT COALESCE(SUM(cl.frozenLimit), 0) FROM CreditLimit cl " +
           "WHERE cl.enterpriseAddress = :enterpriseAddress AND cl.status = 'ACTIVE'")
    Long getTotalFrozenLimitByEnterprise(@Param("enterpriseAddress") String enterpriseAddress);

    /**
     * 查找所有活跃的融资额度
     */
    @Query("SELECT cl FROM CreditLimit cl WHERE cl.limitType = 'FINANCING' " +
           "AND cl.status = 'ACTIVE' ORDER BY cl.totalLimit DESC")
    List<CreditLimit> findAllActiveFinancingLimits();

    /**
     * 查找所有活跃的担保额度
     */
    @Query("SELECT cl FROM CreditLimit cl WHERE cl.limitType = 'GUARANTEE' " +
           "AND cl.status = 'ACTIVE' ORDER BY cl.totalLimit DESC")
    List<CreditLimit> findAllActiveGuaranteeLimits();

    /**
     * 查找所有活跃的赊账额度
     */
    @Query("SELECT cl FROM CreditLimit cl WHERE cl.limitType = 'CREDIT' " +
           "AND cl.status = 'ACTIVE' ORDER BY cl.totalLimit DESC")
    List<CreditLimit> findAllActiveCreditLimits();

    /**
     * 查找逾期次数超过阈值的企业额度
     */
    @Query("SELECT cl FROM CreditLimit cl WHERE cl.overdueCount >= :threshold " +
           "AND cl.status = 'ACTIVE'")
    List<CreditLimit> findByOverdueCountAboveThreshold(@Param("threshold") Integer threshold);

    /**
     * 查找坏账次数超过阈值的企业额度
     */
    @Query("SELECT cl FROM CreditLimit cl WHERE cl.badDebtCount >= :threshold " +
           "AND cl.status = 'ACTIVE'")
    List<CreditLimit> findByBadDebtCountAboveThreshold(@Param("threshold") Integer threshold);

    /**
     * 检查企业是否存在指定类型的活跃额度
     */
    @Query("SELECT CASE WHEN COUNT(cl) > 0 THEN true ELSE false END FROM CreditLimit cl " +
           "WHERE cl.enterpriseAddress = :enterpriseAddress AND cl.limitType = :limitType " +
           "AND cl.status = 'ACTIVE'")
    boolean existsActiveLimitByEnterpriseAndType(@Param("enterpriseAddress") String enterpriseAddress,
                                                 @Param("limitType") CreditLimitType limitType);
}
