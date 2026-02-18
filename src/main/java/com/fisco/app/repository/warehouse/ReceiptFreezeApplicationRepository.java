package com.fisco.app.repository.warehouse;

import com.fisco.app.entity.warehouse.ReceiptFreezeApplication;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 仓单冻结申请Repository
 */
@Repository
public interface ReceiptFreezeApplicationRepository extends JpaRepository<ReceiptFreezeApplication, String> {

    /**
     * 根据仓单ID查询所有申请
     */
    List<ReceiptFreezeApplication> findByReceiptId(String receiptId);

    /**
     * 根据申请状态查询
     */
    List<ReceiptFreezeApplication> findByRequestStatus(String requestStatus);

    /**
     * 根据仓储企业ID和状态查询
     */
    @Query("SELECT a FROM ReceiptFreezeApplication a WHERE a.warehouseId = :warehouseId AND a.requestStatus = :status ORDER BY a.createdAt DESC")
    List<ReceiptFreezeApplication> findByWarehouseIdAndStatus(
            @Param("warehouseId") String warehouseId,
            @Param("status") String status
    );

    /**
     * 查询待审核的冻结申请
     */
    @Query("SELECT a FROM ReceiptFreezeApplication a WHERE a.requestStatus = 'PENDING' ORDER BY a.createdAt DESC")
    List<ReceiptFreezeApplication> findPendingApplications();

    /**
     * 统计仓储企业的待审核申请数量
     */
    @Query("SELECT COUNT(a) FROM ReceiptFreezeApplication a WHERE a.warehouseId = :warehouseId AND a.requestStatus = 'PENDING'")
    Long countPendingByWarehouse(@Param("warehouseId") String warehouseId);

    /**
     * 根据申请人ID查询
     */
    List<ReceiptFreezeApplication> findByApplicantId(String applicantId);
}
