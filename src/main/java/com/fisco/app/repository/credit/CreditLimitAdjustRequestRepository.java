package com.fisco.app.repository.credit;

import com.fisco.app.entity.credit.CreditLimitAdjustRequest;
import com.fisco.app.enums.CreditAdjustRequestStatus;
import com.fisco.app.enums.CreditAdjustType;

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
 * 信用额度调整申请Repository
 */
@Repository
public interface CreditLimitAdjustRequestRepository extends JpaRepository<CreditLimitAdjustRequest, String>, JpaSpecificationExecutor<CreditLimitAdjustRequest> {

    @Override
    @NonNull
    Optional<CreditLimitAdjustRequest> findById(@NonNull String id);

    /**
     * 根据额度ID查找调整申请
     */
    List<CreditLimitAdjustRequest> findByCreditLimitId(String creditLimitId);

    /**
     * 根据额度ID查找调整申请（分页）
     */
    Page<CreditLimitAdjustRequest> findByCreditLimitId(String creditLimitId, Pageable pageable);

    /**
     * 根据申请状态查找调整申请（分页）
     */
    Page<CreditLimitAdjustRequest> findByRequestStatus(CreditAdjustRequestStatus status, Pageable pageable);

    /**
     * 根据额度ID和申请状态查找调整申请
     */
    List<CreditLimitAdjustRequest> findByCreditLimitIdAndRequestStatus(String creditLimitId,
                                                                        CreditAdjustRequestStatus status);

    /**
     * 根据调整类型查找调整申请（分页）
     */
    Page<CreditLimitAdjustRequest> findByAdjustType(CreditAdjustType adjustType, Pageable pageable);

    /**
     * 根据申请人地址查找调整申请（分页）
     */
    Page<CreditLimitAdjustRequest> findByRequesterAddress(String requesterAddress, Pageable pageable);

    /**
     * 根据审批人地址查找调整申请（分页）
     */
    Page<CreditLimitAdjustRequest> findByApproverAddress(String approverAddress, Pageable pageable);

    /**
     * 查找所有待审批的申请
     */
    @Query("SELECT ar FROM CreditLimitAdjustRequest ar WHERE ar.requestStatus = 'PENDING' " +
           "ORDER BY ar.requestDate ASC")
    List<CreditLimitAdjustRequest> findAllPendingRequests();

    /**
     * 根据时间范围查找调整申请
     */
    @Query("SELECT ar FROM CreditLimitAdjustRequest ar WHERE ar.requestDate BETWEEN :startDate AND :endDate " +
           "ORDER BY ar.requestDate DESC")
    List<CreditLimitAdjustRequest> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                                    @Param("endDate") LocalDateTime endDate);

    /**
     * 根据额度ID和时间范围查找调整申请
     */
    @Query("SELECT ar FROM CreditLimitAdjustRequest ar WHERE ar.creditLimitId = :creditLimitId " +
           "AND ar.requestDate BETWEEN :startDate AND :endDate ORDER BY ar.requestDate DESC")
    List<CreditLimitAdjustRequest> findByCreditLimitIdAndDateRange(@Param("creditLimitId") String creditLimitId,
                                                                     @Param("startDate") LocalDateTime startDate,
                                                                     @Param("endDate") LocalDateTime endDate);

    /**
     * 统计额度调整申请次数
     */
    long countByCreditLimitId(String creditLimitId);

    /**
     * 统计指定状态的申请次数
     */
    long countByRequestStatus(CreditAdjustRequestStatus status);

    /**
     * 统计企业待审批的申请次数
     */
    @Query("SELECT COUNT(ar) FROM CreditLimitAdjustRequest ar WHERE ar.creditLimitId IN " +
           "(SELECT cl.id FROM CreditLimit cl WHERE cl.enterpriseAddress = :enterpriseAddress) " +
           "AND ar.requestStatus = 'PENDING'")
    long countPendingRequestsByEnterpriseAddress(@Param("enterpriseAddress") String enterpriseAddress);

    /**
     * 查找企业最近的调整申请
     */
    @Query("SELECT ar FROM CreditLimitAdjustRequest ar WHERE ar.creditLimitId IN " +
           "(SELECT cl.id FROM CreditLimit cl WHERE cl.enterpriseAddress = :enterpriseAddress) " +
           "ORDER BY ar.requestDate DESC")
    List<CreditLimitAdjustRequest> findRecentRequestsByEnterpriseAddress(@Param("enterpriseAddress") String enterpriseAddress,
                                                                           Pageable pageable);

    /**
     * 查找指定额度的最近调整申请
     */
    @Query("SELECT ar FROM CreditLimitAdjustRequest ar WHERE ar.creditLimitId = :creditLimitId " +
           "ORDER BY ar.requestDate DESC")
    List<CreditLimitAdjustRequest> findRecentRequestsByCreditLimitId(@Param("creditLimitId") String creditLimitId,
                                                                      Pageable pageable);

    /**
     * 统计指定时间范围内的申请次数
     */
    @Query("SELECT COUNT(ar) FROM CreditLimitAdjustRequest ar WHERE ar.creditLimitId = :creditLimitId " +
           "AND ar.requestDate BETWEEN :startDate AND :endDate")
    long countByCreditLimitIdAndDateRange(@Param("creditLimitId") String creditLimitId,
                                          @Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    /**
     * 计算指定时间范围内的额度增加总额
     */
    @Query("SELECT COALESCE(SUM(ar.adjustAmount), 0) FROM CreditLimitAdjustRequest ar " +
           "WHERE ar.creditLimitId = :creditLimitId AND ar.adjustType = 'INCREASE' " +
           "AND ar.requestStatus = 'APPROVED' AND ar.approveDate BETWEEN :startDate AND :endDate")
    Long sumIncreasedAmountByDateRange(@Param("creditLimitId") String creditLimitId,
                                        @Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    /**
     * 计算指定时间范围内的额度减少总额
     */
    @Query("SELECT COALESCE(SUM(ABS(ar.adjustAmount)), 0) FROM CreditLimitAdjustRequest ar " +
           "WHERE ar.creditLimitId = :creditLimitId AND ar.adjustType = 'DECREASE' " +
           "AND ar.requestStatus = 'APPROVED' AND ar.approveDate BETWEEN :startDate AND :endDate")
    Long sumDecreasedAmountByDateRange(@Param("creditLimitId") String creditLimitId,
                                        @Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);
}
