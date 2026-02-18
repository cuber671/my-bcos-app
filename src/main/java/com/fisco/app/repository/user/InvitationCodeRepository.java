package com.fisco.app.repository.user;

import com.fisco.app.entity.user.InvitationCode;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import java.time.LocalDateTime;

/**
 * 邀请码Repository
 */
@Repository
public interface InvitationCodeRepository extends JpaRepository<InvitationCode, Long> {

    /**
     * 根据邀请码查找
     */
    Optional<InvitationCode> findByCode(String code);

    /**
     * 根据企业ID查找所有邀请码
     */
    List<InvitationCode> findByEnterpriseIdOrderByCreatedAtDesc(String enterpriseId);

    /**
     * 根据企业ID和状态查找邀请码
     */
    List<InvitationCode> findByEnterpriseIdAndStatusOrderByCreatedAtDesc(
            String enterpriseId, InvitationCode.InvitationCodeStatus status);

    /**
     * 查找所有有效的邀请码（按企业分组）
     */
    @Query("SELECT ic FROM InvitationCode ic WHERE ic.status = 'ACTIVE' AND " +
           "(ic.expiresAt IS NULL OR ic.expiresAt > :now) AND " +
           "(ic.maxUses IS NULL OR ic.usedCount < ic.maxUses)")
    List<InvitationCode> findAllActiveCodes(@Param("now") LocalDateTime now);

    /**
     * 查找某个企业的所有有效邀请码
     */
    @Query("SELECT ic FROM InvitationCode ic WHERE ic.enterpriseId = :enterpriseId AND " +
           "ic.status = 'ACTIVE' AND " +
           "(ic.expiresAt IS NULL OR ic.expiresAt > :now) AND " +
           "(ic.maxUses IS NULL OR ic.usedCount < ic.maxUses)")
    List<InvitationCode> findActiveCodesByEnterprise(
            @Param("enterpriseId") String enterpriseId,
            @Param("now") LocalDateTime now);

    /**
     * 统计某个企业的邀请码使用情况
     */
    @Query("SELECT COUNT(ic) FROM InvitationCode ic WHERE ic.enterpriseId = :enterpriseId")
    long countByEnterpriseId(@Param("enterpriseId") String enterpriseId);

    /**
     * 统计某个企业的有效邀请码数量
     */
    @Query("SELECT COUNT(ic) FROM InvitationCode ic WHERE ic.enterpriseId = :enterpriseId AND " +
           "ic.status = 'ACTIVE' AND " +
           "(ic.expiresAt IS NULL OR ic.expiresAt > :now)")
    long countActiveByEnterpriseId(
            @Param("enterpriseId") String enterpriseId,
            @Param("now") LocalDateTime now);

    /**
     * 查找企业的有效邀请码（单个，用于"一个企业一个邀请码"业务规则）
     * 返回第一个有效的邀请码，按创建时间排序
     */
    @Query("SELECT ic FROM InvitationCode ic WHERE ic.enterpriseId = :enterpriseId AND " +
           "ic.status = 'ACTIVE' AND " +
           "(ic.expiresAt IS NULL OR ic.expiresAt > :now) " +
           "ORDER BY ic.createdAt DESC")
    Optional<InvitationCode> findActiveCodeByEnterprise(
            @Param("enterpriseId") String enterpriseId,
            @Param("now") LocalDateTime now);

    /**
     * 检查邀请码是否存在
     */
    boolean existsByCode(String code);

    /**
     * 删除过期的邀请码
     */
    @Query("DELETE FROM InvitationCode ic WHERE ic.status = 'EXPIRED' AND ic.expiresAt < :expireBefore")
    void deleteExpiredCodes(@Param("expireBefore") LocalDateTime expireBefore);
}
