package com.fisco.app.repository.blockchain;

import com.fisco.app.entity.blockchain.TransactionReceiptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 交易回执数据访问接口
 *
 * @author FISCO BCOS Team
 * @since 2026-02-10
 */
@Repository
public interface TransactionReceiptRepository extends JpaRepository<TransactionReceiptEntity, String> {

    /**
     * 根据交易哈希查询回执
     */
    Optional<TransactionReceiptEntity> findByTransactionHash(String transactionHash);
}
