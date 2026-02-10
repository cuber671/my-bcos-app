package com.fisco.app.repository.blockchain;

import com.fisco.app.entity.blockchain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 交易数据访问接口
 *
 * @author FISCO BCOS Team
 * @since 2026-02-10
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    /**
     * 根据交易哈希查询
     */
    Optional<Transaction> findByTransactionHash(String transactionHash);

    /**
     * 查询账户的交易记录
     */
    List<Transaction> findByFromAddressOrToAddressOrderByCreatedAtDesc(
        String fromAddress, String toAddress);

    /**
     * 查询指定区块的交易
     */
    List<Transaction> findByBlockNumberOrderByTransactionIndexAsc(Long blockNumber);

    /**
     * 查询待处理的交易
     */
    List<Transaction> findByStatusOrderByCreatedAtAsc(Integer status);

    /**
     * 查询时间范围内的交易
     */
    List<Transaction> findByCreatedAtBetweenOrderByCreatedAtDesc(
        LocalDateTime start, LocalDateTime end);

    /**
     * 统计账户交易数量
     */
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.fromAddress = :address OR t.toAddress = :address")
    Long countByAddress(@Param("address") String address);

    /**
     * 查询最新的交易
     */
    @Query(value = "SELECT * FROM blockchain_transaction ORDER BY created_at DESC LIMIT :limit", nativeQuery = true)
    List<Transaction> findLatestTransactions(@Param("limit") int limit);
}
