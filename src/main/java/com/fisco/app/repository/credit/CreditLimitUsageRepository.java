package com.fisco.app.repository.credit;

import com.fisco.app.entity.credit.CreditLimitUsage;
import com.fisco.app.enums.CreditUsageType;

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
 * 信用额度使用记录Repository
 */
@Repository
public interface CreditLimitUsageRepository extends JpaRepository<CreditLimitUsage, String>, JpaSpecificationExecutor<CreditLimitUsage> {

    /**
     * 根据额度ID查找使用记录
     */
    List<CreditLimitUsage> findByCreditLimitId(String creditLimitId);

    /**
     * 根据额度ID查找使用记录（分页）
     */
    Page<CreditLimitUsage> findByCreditLimitId(String creditLimitId, Pageable pageable);

    /**
     * 根据额度ID和使用类型查找使用记录
     */
    List<CreditLimitUsage> findByCreditLimitIdAndUsageType(String creditLimitId, CreditUsageType usageType);

    /**
     * 根据业务类型和业务ID查找使用记录
     */
    List<CreditLimitUsage> findByBusinessTypeAndBusinessId(String businessType, String businessId);

    /**
     * 根据使用类型查找使用记录（分页）
     */
    Page<CreditLimitUsage> findByUsageType(CreditUsageType usageType, Pageable pageable);

    /**
     * 根据操作人地址查找使用记录（分页）
     */
    Page<CreditLimitUsage> findByOperatorAddress(String operatorAddress, Pageable pageable);

    /**
     * 根据时间范围查找使用记录
     */
    @Query("SELECT u FROM CreditLimitUsage u WHERE u.usageDate BETWEEN :startDate AND :endDate " +
           "ORDER BY u.usageDate DESC")
    List<CreditLimitUsage> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    /**
     * 根据额度ID和时间范围查找使用记录
     */
    @Query("SELECT u FROM CreditLimitUsage u WHERE u.creditLimitId = :creditLimitId " +
           "AND u.usageDate BETWEEN :startDate AND :endDate ORDER BY u.usageDate DESC")
    List<CreditLimitUsage> findByCreditLimitIdAndDateRange(@Param("creditLimitId") String creditLimitId,
                                                            @Param("startDate") LocalDateTime startDate,
                                                            @Param("endDate") LocalDateTime endDate);

    /**
     * 查找最近的使用记录
     */
    @Query("SELECT u FROM CreditLimitUsage u WHERE u.creditLimitId = :creditLimitId " +
           "ORDER BY u.usageDate DESC")
    List<CreditLimitUsage> findRecentUsageByCreditLimitId(@Param("creditLimitId") String creditLimitId,
                                                           Pageable pageable);

    /**
     * 统计额度使用次数
     */
    long countByCreditLimitId(String creditLimitId);

    /**
     * 统计指定类型的使用次数
     */
    long countByCreditLimitIdAndUsageType(String creditLimitId, CreditUsageType usageType);

    /**
     * 计算总额度使用量（按使用类型）
     */
    @Query("SELECT COALESCE(SUM(u.amount), 0) FROM CreditLimitUsage u " +
           "WHERE u.creditLimitId = :creditLimitId AND u.usageType = :usageType")
    Long sumAmountByCreditLimitIdAndUsageType(@Param("creditLimitId") String creditLimitId,
                                               @Param("usageType") CreditUsageType usageType);

    /**
     * 计算净使用量（使用量 - 释放量）
     */
    @Query("SELECT COALESCE(SUM(CASE WHEN u.usageType = 'USE' THEN u.amount " +
           "WHEN u.usageType = 'RELEASE' THEN -u.amount ELSE 0 END), 0) " +
           "FROM CreditLimitUsage u WHERE u.creditLimitId = :creditLimitId")
    Long calculateNetUsageByCreditLimitId(@Param("creditLimitId") String creditLimitId);

    /**
     * 查找指定业务的所有使用记录
     */
    @Query("SELECT u FROM CreditLimitUsage u WHERE u.businessId = :businessId " +
           "ORDER BY u.usageDate ASC")
    List<CreditLimitUsage> findByBusinessIdOrderByDate(@Param("businessId") String businessId);

    /**
     * 统计企业的时间范围内的使用次数
     */
    @Query("SELECT COUNT(u) FROM CreditLimitUsage u WHERE u.creditLimitId IN " +
           "(SELECT cl.id FROM CreditLimit cl WHERE cl.enterpriseAddress = :enterpriseAddress) " +
           "AND u.usageDate BETWEEN :startDate AND :endDate")
    long countByEnterpriseAddressAndDateRange(@Param("enterpriseAddress") String enterpriseAddress,
                                              @Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);
}
