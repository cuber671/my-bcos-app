package com.fisco.app.repository.warehouse;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fisco.app.entity.warehouse.WarehouseReceipt;

/**
 * 仓单Repository
 */
@Repository
public interface WarehouseReceiptRepository extends JpaRepository<WarehouseReceipt, String> {

    /**
     * 查找货主的所有仓单
    */
    List<WarehouseReceipt> findByOwnerAddress(String ownerAddress);

    /**
     * 查找仓库的所有仓单
     */
    List<WarehouseReceipt> findByWarehouseAddress(String warehouseAddress);

    /**
     * 查找金融机构的仓单
     */
    List<WarehouseReceipt> findByFinancialInstitution(String financialInstitution);

    /**
     * 根据状态查找
     */
    List<WarehouseReceipt> findByStatus(WarehouseReceipt.ReceiptStatus status);

    /**
     * 查找即将过期的仓单
     */
    @Query("SELECT wr FROM WarehouseReceipt wr WHERE wr.expiryDate BETWEEN :startDate AND :endDate AND wr.status IN ('VERIFIED', 'PLEDGED', 'FINANCED')")
    List<WarehouseReceipt> findExpiringSoon(@Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);

    /**
     * 查找已过期但未处理的仓单
     */
    @Query("SELECT wr FROM WarehouseReceipt wr WHERE wr.expiryDate < :currentDate AND wr.status NOT IN ('RELEASED', 'LIQUIDATED', 'EXPIRED')")
    List<WarehouseReceipt> findExpiredUnprocessed(@Param("currentDate") LocalDateTime currentDate);

    /**
     * 统计货主的质押总金额
     */
    @Query("SELECT SUM(wr.pledgeAmount) FROM WarehouseReceipt wr WHERE wr.ownerAddress = :ownerAddress AND wr.status IN ('PLEDGED', 'FINANCED')")
    java.math.BigDecimal totalPledgeAmountByOwner(@Param("ownerAddress") String ownerAddress);

    /**
     * 统计金融机构的融资总金额
     */
    @Query("SELECT SUM(wr.financeAmount) FROM WarehouseReceipt wr WHERE wr.financialInstitution = :financialInstitution AND wr.status = 'FINANCED'")
    java.math.BigDecimal totalFinanceAmountByFinancier(@Param("financialInstitution") String financialInstitution);
}
