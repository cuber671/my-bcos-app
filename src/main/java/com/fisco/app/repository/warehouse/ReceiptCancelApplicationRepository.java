package com.fisco.app.repository.warehouse;

import com.fisco.app.entity.warehouse.ReceiptCancelApplication;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 仓单作废申请Repository
 */
@Repository
public interface ReceiptCancelApplicationRepository extends JpaRepository<ReceiptCancelApplication, String> {

    /**
     * 根据仓单ID查询所有作废申请
     */
    List<ReceiptCancelApplication> findByReceiptId(String receiptId);

    /**
     * 根据申请状态查询
     */
    List<ReceiptCancelApplication> findByRequestStatus(String requestStatus);

    /**
     * 查询待审核的作废申请
     */
    @Query("SELECT a FROM ReceiptCancelApplication a WHERE a.requestStatus = 'PENDING' ORDER BY a.createdAt DESC")
    List<ReceiptCancelApplication> findPendingApplications();

    /**
     * 检查是否存在待审核的作废申请
     */
    @Query("SELECT COUNT(a) > 0 FROM ReceiptCancelApplication a WHERE a.receiptId = :receiptId AND a.requestStatus = 'PENDING'")
    boolean existsPendingCancelApplication(@Param("receiptId") String receiptId);

    /**
     * 根据申请人ID查询
     */
    List<ReceiptCancelApplication> findByApplicantId(String applicantId);

    /**
     * 根据审核人ID查询
     */
    List<ReceiptCancelApplication> findByReviewerId(String reviewerId);

    /**
     * 统计仓储企业的待审核申请数量
     */
    @Query(value = "SELECT COUNT(*) FROM receipt_cancel_application a " +
           "JOIN electronic_warehouse_receipt r ON a.receipt_id = r.id " +
           "WHERE r.warehouse_id = :warehouseId AND a.request_status = 'PENDING'",
           nativeQuery = true)
    Long countPendingByWarehouse(@Param("warehouseId") String warehouseId);

    /**
     * 根据仓储企业ID查询待审核的作废申请
     */
    @Query(value = "SELECT a.* FROM receipt_cancel_application a " +
           "JOIN electronic_warehouse_receipt r ON a.receipt_id = r.id " +
           "WHERE r.warehouse_id = :warehouseId AND a.request_status = 'PENDING' " +
           "ORDER BY a.created_at DESC",
           nativeQuery = true)
    List<ReceiptCancelApplication> findPendingApplicationsByWarehouse(
        @Param("warehouseId") String warehouseId);
}
