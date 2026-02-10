package com.fisco.app.repository.warehouse;

import com.fisco.app.entity.warehouse.ReceiptChangeHistory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 仓单变更历史Repository
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-09
 * @version 1.0
 */
@Repository
public interface ReceiptChangeHistoryRepository extends JpaRepository<ReceiptChangeHistory, String> {

    /**
     * 根据仓单ID查询变更历史
     */
    List<ReceiptChangeHistory> findByReceiptIdOrderByChangeTimeDesc(String receiptId);

    /**
     * 根据仓单编号查询变更历史
     */
    List<ReceiptChangeHistory> findByReceiptNoOrderByChangeTimeDesc(String receiptNo);

    /**
     * 根据操作人ID查询变更历史
     */
    List<ReceiptChangeHistory> findByOperatorIdOrderByChangeTimeDesc(String operatorId);

    /**
     * 查询指定时间范围内的变更历史
     */
    List<ReceiptChangeHistory> findByChangeTimeBetweenOrderByChangeTimeDesc(
        LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据变更类型查询
     */
    List<ReceiptChangeHistory> findByChangeTypeOrderByChangeTimeDesc(String changeType);
}
