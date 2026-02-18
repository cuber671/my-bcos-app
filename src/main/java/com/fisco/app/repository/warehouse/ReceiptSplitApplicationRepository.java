package com.fisco.app.repository.warehouse;

import com.fisco.app.entity.warehouse.ReceiptSplitApplication;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 仓单拆分申请Repository
 */
@Repository
public interface ReceiptSplitApplicationRepository extends JpaRepository<ReceiptSplitApplication, String> {

    /**
     * 根据父仓单ID查询所有拆分申请
     */
    List<ReceiptSplitApplication> findByParentReceiptId(String parentReceiptId);

    /**
     * 根据申请状态查询
     */
    List<ReceiptSplitApplication> findByRequestStatus(String requestStatus);

    /**
     * 查询待审核的拆分申请
     */
    @Query("SELECT a FROM ReceiptSplitApplication a WHERE a.requestStatus = 'PENDING' ORDER BY a.createdAt DESC")
    List<ReceiptSplitApplication> findPendingApplications();

    /**
     * 检查是否存在待审核的拆分申请
     */
    boolean existsByParentReceiptIdAndRequestStatus(String parentReceiptId, String requestStatus);

    /**
     * 根据申请人ID查询
     */
    List<ReceiptSplitApplication> findByApplicantId(String applicantId);

    /**
     * 根据审核人ID查询
     */
    List<ReceiptSplitApplication> findByReviewerId(String reviewerId);

    /**
     * 统计仓储企业的待审核申请数量
     */
    @Query(value = "SELECT COUNT(*) FROM receipt_split_application a " +
           "JOIN electronic_warehouse_receipt r ON a.parent_receipt_id = r.id " +
           "WHERE r.warehouse_id = :warehouseId AND a.request_status = 'PENDING'",
           nativeQuery = true)
    Long countPendingByWarehouse(@Param("warehouseId") String warehouseId);
}
