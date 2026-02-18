package com.fisco.app.repository.warehouse;

import com.fisco.app.entity.warehouse.EwrEndorsementChain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import java.time.LocalDateTime;

/**
 * 背书链Repository
 */
@Repository
public interface EwrEndorsementChainRepository extends JpaRepository<EwrEndorsementChain, String> {

    /**
     * 根据背书编号查询
     */
    Optional<EwrEndorsementChain> findByEndorsementNo(String endorsementNo);

    /**
     * 根据仓单ID查询所有背书记录
     */
    List<EwrEndorsementChain> findByReceiptId(String receiptId);

    /**
     * 根据仓单编号查询所有背书记录
     */
    List<EwrEndorsementChain> findByReceiptNo(String receiptNo);

    /**
     * 根据转出方地址查询
     */
    List<EwrEndorsementChain> findByEndorseFrom(String endorseFrom);

    /**
     * 根据转入方地址查询
     */
    List<EwrEndorsementChain> findByEndorseTo(String endorseTo);

    /**
     * 根据背书状态查询
     */
    List<EwrEndorsementChain> findByEndorsementStatus(EwrEndorsementChain.EndorsementStatus status);

    /**
     * 根据背书类型查询
     */
    List<EwrEndorsementChain> findByEndorsementType(EwrEndorsementChain.EndorsementType type);

    /**
     * 查询待确认的背书（转入方视角）
     */
    @Query("SELECT e FROM EwrEndorsementChain e WHERE e.endorseTo = :endorseTo AND e.endorsementStatus = 'PENDING' ORDER BY e.endorsementTime DESC")
    List<EwrEndorsementChain> findPendingEndorsementsByEndorseTo(@Param("endorseTo") String endorseTo);

    /**
     * 查询待确认的背书（仓单视角）
     */
    @Query("SELECT e FROM EwrEndorsementChain e WHERE e.receiptId = :receiptId AND e.endorsementStatus = 'PENDING' ORDER BY e.endorsementTime DESC")
    List<EwrEndorsementChain> findPendingEndorsementsByReceiptId(@Param("receiptId") String receiptId);

    /**
     * 查询仓单的完整背书链（按时间正序）
     */
    @Query("SELECT e FROM EwrEndorsementChain e WHERE e.receiptId = :receiptId AND e.endorsementStatus = 'CONFIRMED' ORDER BY e.endorsementTime ASC")
    List<EwrEndorsementChain> findConfirmedEndorsementChainByReceiptId(@Param("receiptId") String receiptId);

    /**
     * 统计仓单的背书次数
     */
    @Query("SELECT COUNT(e) FROM EwrEndorsementChain e WHERE e.receiptId = :receiptId AND e.endorsementStatus = 'CONFIRMED'")
    Long countConfirmedEndorsementsByReceiptId(@Param("receiptId") String receiptId);

    /**
     * 查询指定地址作为转出方的已确认背书
     */
    @Query("SELECT e FROM EwrEndorsementChain e WHERE e.endorseFrom = :endorseFrom AND e.endorsementStatus = 'CONFIRMED' ORDER BY e.endorsementTime DESC")
    List<EwrEndorsementChain> findConfirmedEndorsementsByEndorseFrom(@Param("endorseFrom") String endorseFrom);

    /**
     * 查询指定地址作为转入方的已确认背书
     */
    @Query("SELECT e FROM EwrEndorsementChain e WHERE e.endorseTo = :endorseTo AND e.endorsementStatus = 'CONFIRMED' ORDER BY e.endorsementTime DESC")
    List<EwrEndorsementChain> findConfirmedEndorsementsByEndorseTo(@Param("endorseTo") String endorseTo);

    /**
     * 查询指定时间范围内的背书
     */
    @Query("SELECT e FROM EwrEndorsementChain e WHERE e.endorsementTime BETWEEN :startTime AND :endTime ORDER BY e.endorsementTime DESC")
    List<EwrEndorsementChain> findByEndorsementTimeBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 查询指定经手人的背书记录
     */
    @Query("SELECT e FROM EwrEndorsementChain e WHERE (e.operatorFromId = :operatorId OR e.operatorToId = :operatorId) ORDER BY e.endorsementTime DESC")
    List<EwrEndorsementChain> findByOperatorId(@Param("operatorId") String operatorId);

    /**
     * 检查是否存在待确认的背书
     */
    @Query("SELECT COUNT(e) > 0 FROM EwrEndorsementChain e WHERE e.receiptId = :receiptId AND e.endorsementStatus = 'PENDING'")
    boolean existsPendingEndorsement(@Param("receiptId") String receiptId);

    /**
     * 根据交易哈希查询
     */
    Optional<EwrEndorsementChain> findByTxHash(String txHash);

    /**
     * 查询未上链的背书
     */
    @Query("SELECT e FROM EwrEndorsementChain e WHERE e.txHash IS NULL AND e.endorsementStatus = 'CONFIRMED' ORDER BY e.endorsementTime ASC")
    List<EwrEndorsementChain> findEndorsementsWithoutTxHash();

    /**
     * 查询最新的已确认背书
     */
    @Query("SELECT e FROM EwrEndorsementChain e WHERE e.receiptId = :receiptId AND e.endorsementStatus = 'CONFIRMED' ORDER BY e.confirmedTime DESC")
    List<EwrEndorsementChain> findLatestConfirmedEndorsement(@Param("receiptId") String receiptId);
}
