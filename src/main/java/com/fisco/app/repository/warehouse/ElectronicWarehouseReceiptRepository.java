package com.fisco.app.repository.warehouse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fisco.app.entity.warehouse.ElectronicWarehouseReceipt;

/**
 * 电子仓单Repository
 */
@Repository
public interface ElectronicWarehouseReceiptRepository extends JpaRepository<ElectronicWarehouseReceipt, String> {

    /**
     * 根据仓单编号查询
     */
    Optional<ElectronicWarehouseReceipt> findByReceiptNo(String receiptNo);

    /**
     * 根据仓储企业ID查询
     */
    List<ElectronicWarehouseReceipt> findByWarehouseId(String warehouseId);

    /**
     * 根据货主企业ID查询
     */
    List<ElectronicWarehouseReceipt> findByOwnerId(String ownerId);

    /**
     * 根据货主企业ID和状态查询
     */
    @Query("SELECT e FROM ElectronicWarehouseReceipt e WHERE e.ownerId = :ownerId AND e.receiptStatus = :status AND e.deletedAt IS NULL")
    List<ElectronicWarehouseReceipt> findByOwnerIdAndReceiptStatus(
            @Param("ownerId") String ownerId,
            @Param("status") ElectronicWarehouseReceipt.ReceiptStatus status
    );

    /**
     * 根据持单人地址查询
     */
    List<ElectronicWarehouseReceipt> findByHolderAddress(String holderAddress);

    /**
     * 根据仓单状态查询
     */
    List<ElectronicWarehouseReceipt> findByReceiptStatus(ElectronicWarehouseReceipt.ReceiptStatus status);

    /**
     * 根据仓储企业ID和状态查询
     */
    @Query("SELECT e FROM ElectronicWarehouseReceipt e WHERE e.warehouseId = :warehouseId AND e.receiptStatus = :status")
    List<ElectronicWarehouseReceipt> findByWarehouseIdAndReceiptStatus(
            @Param("warehouseId") String warehouseId,
            @Param("status") ElectronicWarehouseReceipt.ReceiptStatus status
    );

    /**
     * 根据区块链状态查询
     */
    List<ElectronicWarehouseReceipt> findByBlockchainStatus(ElectronicWarehouseReceipt.BlockchainStatus status);

    /**
     * 查询即将过期的仓单（7天内过期）
     */
    @Query("SELECT e FROM ElectronicWarehouseReceipt e WHERE e.expiryDate BETWEEN :now AND :expiryThreshold AND e.receiptStatus NOT IN ('DELIVERED', 'CANCELLED')")
    List<ElectronicWarehouseReceipt> findExpiringReceipts(@Param("now") LocalDateTime now, @Param("expiryThreshold") LocalDateTime expiryThreshold);

    /**
     * 查询已过期的仓单
     */
    @Query("SELECT e FROM ElectronicWarehouseReceipt e WHERE e.expiryDate < :now AND e.receiptStatus NOT IN ('DELIVERED', 'CANCELLED', 'EXPIRED')")
    List<ElectronicWarehouseReceipt> findExpiredReceipts(@Param("now") LocalDateTime now);

    /**
     * 查询已融资的仓单
     */
    List<ElectronicWarehouseReceipt> findByIsFinancedTrue();

    /**
     * 根据质押合同编号查询
     */
    Optional<ElectronicWarehouseReceipt> findByPledgeContractNo(String pledgeContractNo);

    /**
     * 根据批次号查询
     */
    List<ElectronicWarehouseReceipt> findByBatchNo(String batchNo);

    /**
     * 查询未删除的仓单
     */
    @Query("SELECT e FROM ElectronicWarehouseReceipt e WHERE e.deletedAt IS NULL ORDER BY e.createdAt DESC")
    List<ElectronicWarehouseReceipt> findAllActiveReceipts();

    /**
     * 根据货物名称查询（模糊匹配）
     */
    @Query("SELECT e FROM ElectronicWarehouseReceipt e WHERE e.goodsName LIKE %:goodsName% AND e.deletedAt IS NULL")
    List<ElectronicWarehouseReceipt> findByGoodsNameContaining(@Param("goodsName") String goodsName);

    /**
     * 统计货主的仓单数量
     */
    @Query("SELECT COUNT(e) FROM ElectronicWarehouseReceipt e WHERE e.ownerId = :ownerId AND e.deletedAt IS NULL")
    Long countByOwnerId(@Param("ownerId") String ownerId);

    /**
     * 统计仓储企业的仓单数量
     */
    @Query("SELECT COUNT(e) FROM ElectronicWarehouseReceipt e WHERE e.warehouseId = :warehouseId AND e.deletedAt IS NULL")
    Long countByWarehouseId(@Param("warehouseId") String warehouseId);

    /**
     * 查询指定持单人的正常状态仓单
     */
    @Query("SELECT e FROM ElectronicWarehouseReceipt e WHERE e.holderAddress = :holderAddress AND e.receiptStatus = 'NORMAL' AND e.deletedAt IS NULL")
    List<ElectronicWarehouseReceipt> findNormalReceiptsByHolder(@Param("holderAddress") String holderAddress);

    /**
     * 根据父仓单ID查询所有子仓单
     */
    List<ElectronicWarehouseReceipt> findByParentReceiptId(String parentReceiptId);
}
