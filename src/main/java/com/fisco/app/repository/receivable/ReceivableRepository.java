package com.fisco.app.repository.receivable;

import com.fisco.app.entity.receivable.Receivable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;

import java.util.List;

import java.time.LocalDateTime;

/**
 * 应收账款Repository
 */
@Repository
public interface ReceivableRepository extends JpaRepository<Receivable, String> {

    /**
     * 查找供应商的所有应收账款
     */
    List<Receivable> findBySupplierAddress(String supplierAddress);

    /**
     * 查找核心企业的所有应付账款
     */
    List<Receivable> findByCoreEnterpriseAddress(String coreEnterpriseAddress);

    /**
     * 查找当前持有人持有的应收账款
     */
    List<Receivable> findByCurrentHolder(String currentHolder);

    /**
     * 查找资金方的融资账款
     */
    List<Receivable> findByFinancierAddress(String financierAddress);

    /**
     * 根据状态查找应收账款
     */
    List<Receivable> findByStatus(Receivable.ReceivableStatus status);

    /**
     * 查找即将到期的应收账款
     */
    @Query("SELECT r FROM Receivable r WHERE r.dueDate BETWEEN :startDate AND :endDate AND r.status IN ('CONFIRMED', 'FINANCED')")
    List<Receivable> findDueSoonReceivables(@Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);

    /**
     * 查找已违约的应收账款
     */
    @Query("SELECT r FROM Receivable r WHERE r.status = 'DEFAULTED' OR (r.dueDate < :currentDate AND r.status IN ('CONFIRMED', 'FINANCED'))")
    List<Receivable> findDefaultedReceivables(@Param("currentDate") LocalDateTime currentDate);

    /**
     * 统计供应商的总应收金额
     */
    @Query("SELECT SUM(r.amount) FROM Receivable r WHERE r.supplierAddress = :supplierAddress AND r.status IN ('CONFIRMED', 'FINANCED')")
    java.math.BigDecimal totalAmountBySupplier(@Param("supplierAddress") String supplierAddress);

    /**
     * 统计资金方的总融资金额
     */
    @Query("SELECT SUM(r.financeAmount) FROM Receivable r WHERE r.financierAddress = :financierAddress AND r.status = 'FINANCED'")
    java.math.BigDecimal totalFinanceAmountByFinancier(@Param("financierAddress") String financierAddress);

    /**
     * 查找父应收账款的所有子应收账款
     */
    List<Receivable> findByParentReceivableId(String parentReceivableId);

    /**
     * 根据id查找应收账款（使用业务ID查询）
     */
    @Query("SELECT r FROM Receivable r WHERE r.id = :receivableId")
    java.util.Optional<Receivable> findByReceivableId(@Param("receivableId") String receivableId);
}
