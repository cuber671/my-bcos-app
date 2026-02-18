package com.fisco.app.repository.blockchain;

import com.fisco.app.entity.blockchain.ContractEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 合约事件数据访问接口
 *
 * @author FISCO BCOS Team
 * @since 2026-02-10
 */
@Repository
public interface ContractEventRepository extends JpaRepository<ContractEvent, String> {

    /**
     * 根据合约地址和事件名称查询
     */
    Page<ContractEvent> findByContractAddressAndEventName(
        String contractAddress, String eventName, Pageable pageable);

    /**
     * 根据合约地址和区块范围查询
     */
    Page<ContractEvent> findByContractAddressAndBlockNumberBetween(
        String contractAddress, Long fromBlock, Long toBlock, Pageable pageable);

    /**
     * 根据合约地址和时间范围查询
     */
    Page<ContractEvent> findByContractAddressAndEventTimestampBetween(
        String contractAddress, LocalDateTime from, LocalDateTime to, Pageable pageable);

    /**
     * 根据交易哈希查询
     */
    List<ContractEvent> findByTransactionHash(String transactionHash);

    /**
     * 统计合约的事件数量
     */
    Long countByContractAddress(String contractAddress);

    /**
     * 根据多条件过滤查询
     */
    @Query("SELECT e FROM ContractEvent e WHERE e.contractAddress = :address AND " +
           "(:eventName IS NULL OR e.eventName = :eventName) AND " +
           "(:fromBlock IS NULL OR e.blockNumber >= :fromBlock) AND " +
           "(:toBlock IS NULL OR e.blockNumber <= :toBlock)")
    Page<ContractEvent> findByFilters(
        @Param("address") String address,
        @Param("eventName") String eventName,
        @Param("fromBlock") Long fromBlock,
        @Param("toBlock") Long toBlock,
        Pageable pageable
    );

    /**
     * 查询合约的所有事件名称
     */
    @Query("SELECT DISTINCT e.eventName FROM ContractEvent e WHERE e.contractAddress = :address")
    List<String> findDistinctEventNamesByContractAddress(@Param("address") String address);

    /**
     * 删除指定合约地址的所有事件
     */
    void deleteByContractAddress(String contractAddress);
}
