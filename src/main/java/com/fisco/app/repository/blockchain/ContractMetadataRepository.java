package com.fisco.app.repository.blockchain;

import com.fisco.app.entity.blockchain.ContractMetadata;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 合约元数据访问接口
 *
 * @author FISCO BCOS Team
 * @since 2026-02-10
 */
@Repository
public interface ContractMetadataRepository extends JpaRepository<ContractMetadata, String> {

    /**
     * 根据合约地址查询
     */
    Optional<ContractMetadata> findByContractAddress(String address);

    /**
     * 检查合约地址是否存在
     */
    boolean existsByContractAddress(String address);

    /**
     * 根据合约类型和状态查询
     */
    Page<ContractMetadata> findByContractTypeAndStatus(
        String contractType, String status, Pageable pageable);

    /**
     * 根据状态查询
     */
    Page<ContractMetadata> findByStatus(String status, Pageable pageable);

    /**
     * 根据合约类型查询
     */
    List<ContractMetadata> findByContractType(String contractType);

    /**
     * 根据多条件过滤查询
     */
    @Query("SELECT c FROM ContractMetadata c WHERE " +
           "(:contractType IS NULL OR c.contractType = :contractType) AND " +
           "(:status IS NULL OR c.status = :status)")
    Page<ContractMetadata> findByFilters(
        @Param("contractType") String contractType,
        @Param("status") String status,
        Pageable pageable
    );

    /**
     * 统计指定类型的合约数量
     */
    Long countByContractType(String contractType);

    /**
     * 统计指定状态的合约数量
     */
    Long countByStatus(String status);
}
