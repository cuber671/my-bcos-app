package com.fisco.app.repository.blockchain;

import com.fisco.app.entity.blockchain.TransactionPool;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 交易池数据访问接口
 *
 * @author FISCO BCOS Team
 * @since 2026-02-10
 */
@Repository
public interface TransactionPoolRepository extends JpaRepository<TransactionPool, String> {

    /**
     * 查询账户在交易池中的交易
     */
    List<TransactionPool> findByFromAddressOrderBySubmittedAtDesc(String fromAddress);

    /**
     * 查询待处理的交易池记录
     */
    List<TransactionPool> findByStatusOrderBySubmittedAtAsc(TransactionPool.PoolStatus status);

    /**
     * 统计账户待处理交易数
     */
    @Query("SELECT COUNT(tp) FROM TransactionPool tp WHERE tp.fromAddress = :address AND tp.status = 'PENDING'")
    Long countPendingByAddress(@Param("address") String address);

    /**
     * 删除已确认的交易池记录
     */
    void deleteByTransactionHashAndStatus(String transactionHash, TransactionPool.PoolStatus status);

    /**
     * 删除超时的待处理交易
     */
    @Query("DELETE FROM TransactionPool tp WHERE tp.status = 'PENDING' AND tp.submittedAt < :expiryTime")
    void deleteExpiredPendingTransactions(@Param("expiryTime") LocalDateTime expiryTime);
}
