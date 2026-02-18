package com.fisco.app.repository.enterprise;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fisco.app.entity.enterprise.CreditRatingHistory;

/**
 * 信用评级历史Repository
 */
@Repository
public interface CreditRatingHistoryRepository extends JpaRepository<CreditRatingHistory, Long> {

    /**
     * 查询企业的评级历史（按时间倒序）
     * @param enterpriseAddress 企业区块链地址
     * @return 评级历史列表
     */
    List<CreditRatingHistory> findByEnterpriseAddressOrderByChangedAtDesc(String enterpriseAddress);

    /**
     * 分页查询企业的评级历史
     * @param enterpriseAddress 企业区块链地址
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<CreditRatingHistory> findByEnterpriseAddress(String enterpriseAddress, Pageable pageable);

    /**
     * 统计企业的评级变更次数
     * @param enterpriseAddress 企业区块链地址
     * @return 变更次数
     */
    Long countByEnterpriseAddress(String enterpriseAddress);

    /**
     * 查询操作人的评级变更历史
     * @param changedBy 操作人用户名
     * @return 评级历史列表
     */
    List<CreditRatingHistory> findByChangedByOrderByChangedAtDesc(String changedBy);

    /**
     * 查询指定时间范围内的评级变更
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @return 评级历史列表
     */
    @Query("SELECT h FROM CreditRatingHistory h WHERE h.changedAt BETWEEN :startDate AND :endDate ORDER BY h.changedAt DESC")
    List<CreditRatingHistory> findByChangedAtBetween(@Param("startDate") java.time.LocalDateTime startDate,
                                                      @Param("endDate") java.time.LocalDateTime endDate);

    /**
     * 查询最近的评级变更记录
     * @param pageable 分页参数
     * @return 分页结果
     */
    @Query("SELECT h FROM CreditRatingHistory h ORDER BY h.changedAt DESC")
    Page<CreditRatingHistory> findRecentChanges(Pageable pageable);
}
