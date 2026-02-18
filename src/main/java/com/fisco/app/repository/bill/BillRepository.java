package com.fisco.app.repository.bill;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fisco.app.entity.bill.Bill;

// import com.fisco.app.entity.bill.BillPool; 

/**
 * 票据Repository（完整版）
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-02
 * @version 2.0
 */
@Repository
public interface BillRepository extends JpaRepository<Bill, String> {

    // ==================== 基础查询 ====================

    /**
     * 根据票据编号查询
     */
    Optional<Bill> findByBillNo(String billNo);

    /**
     * 根据票据类型查询
     */
    List<Bill> findByBillType(Bill.BillType billType);

    /**
     * 根据票据状态查询
     */
    List<Bill> findByBillStatus(Bill.BillStatus billStatus);

    // ==================== 参与方查询 ====================

    /**
     * 查找出票人的所有票据
     */
    List<Bill> findByDrawerId(String drawerId);

    /**
     * 查询承兑人的所有票据
     */
    List<Bill> findByDraweeId(String draweeId);

    /**
     * 查询收款人的所有票据
     */
    List<Bill> findByPayeeId(String payeeId);

    /**
     * 查询当前持票人的所有票据
     */
    List<Bill> findByCurrentHolderId(String currentHolderId);

    /**
     * 查询贴现机构的票据
     */
    List<Bill> findByDiscountInstitutionId(String discountInstitutionId);

    /**
     * 查询质押机构的票据
     */
    List<Bill> findByPledgeInstitutionId(String pledgeInstitutionId);

    // ==================== 状态组合查询 ====================

    /**
     * 查询持票人的正常状态票据
     */
    @Query("SELECT b FROM Bill b WHERE b.currentHolderId = :holderId AND b.billStatus IN ('NORMAL', 'ISSUED')")
    List<Bill> findNormalBillsByHolder(@Param("holderId") String holderId);

    /**
     * 查询持票人的可流通票据
     */
    @Query("SELECT b FROM Bill b WHERE b.currentHolderId = :holderId AND b.billStatus IN ('NORMAL', 'ISSUED', 'ENDORSED')")
    List<Bill> findTransferableBillsByHolder(@Param("holderId") String holderId);

    /**
     * 查询即将到期的票据
     */
    @Query("SELECT b FROM Bill b WHERE b.dueDate BETWEEN :startDate AND :endDate AND b.billStatus IN ('NORMAL', 'ISSUED', 'ENDORSED')")
    List<Bill> findDueSoonBills(@Param("startDate") LocalDateTime startDate,
                                 @Param("endDate") LocalDateTime endDate);

    /**
     * 查询已到期未付款的票据
     */
    @Query("SELECT b FROM Bill b WHERE b.dueDate < :currentDate AND b.billStatus NOT IN ('PAID', 'SETTLED', 'CANCELLED')")
    List<Bill> findOverdueBills(@Param("currentDate") LocalDateTime currentDate);

    /**
     * 查询企业的已过期票据
     */
    @Query("SELECT b FROM Bill b WHERE b.currentHolderId = :enterpriseId AND b.dueDate < :currentDate AND b.billStatus NOT IN ('PAID', 'SETTLED', 'CANCELLED')")
    List<Bill> findExpiredBillsByEnterprise(@Param("enterpriseId") String enterpriseId,
                                           @Param("currentDate") LocalDateTime currentDate);

    // ==================== 仓单联动查询 ====================

    /**
     * 根据担保仓单ID查询票据
     */
    List<Bill> findByBackedReceiptId(String backedReceiptId);

    /**
     * 根据仓单质押ID查询票据
     */
    List<Bill> findByReceiptPledgeId(String receiptPledgeId);

    /**
     * 查询所有有仓单担保的票据
     */
    @Query("SELECT b FROM Bill b WHERE b.backedReceiptId IS NOT NULL ORDER BY b.createdAt DESC")
    List<Bill> findReceiptBackedBills();

    // ==================== 统计查询 ====================

    /**
     * 统计持票人的票据总金额
     */
    @Query("SELECT SUM(b.faceValue) FROM Bill b WHERE b.currentHolderId = :holderId AND b.billStatus IN ('NORMAL', 'ISSUED', 'ENDORSED')")
    BigDecimal totalAmountByHolder(@Param("holderId") String holderId);

    /**
     * 统计出票人的开票总金额
     */
    @Query("SELECT SUM(b.faceValue) FROM Bill b WHERE b.drawerId = :drawerId AND b.billStatus != 'CANCELLED'")
    BigDecimal totalIssuedAmountByDrawer(@Param("drawerId") String drawerId);

    /**
     * 统计承兑人的承兑总金额
     */
    @Query("SELECT SUM(b.faceValue) FROM Bill b WHERE b.draweeId = :draweeId AND b.billStatus != 'CANCELLED'")
    BigDecimal totalAcceptedAmountByDrawee(@Param("draweeId") String draweeId);

    /**
     * 统计持票人的票据数量
     */
    @Query("SELECT COUNT(b) FROM Bill b WHERE b.currentHolderId = :holderId AND b.billStatus IN ('NORMAL', 'ISSUED', 'ENDORSED')")
    Long countBillsByHolder(@Param("holderId") String holderId);

    // ==================== 融资相关查询 ====================

    /**
     * 查询已贴现的票据
     */
    @Query("SELECT b FROM Bill b WHERE b.billStatus = 'DISCOUNTED' ORDER BY b.discountDate DESC")
    List<Bill> findDiscountedBills();

    /**
     * 查询已质押的票据
     */
    @Query("SELECT b FROM Bill b WHERE b.billStatus = 'PLEDGED' ORDER BY b.pledgeDate DESC")
    List<Bill> findPledgedBills();

    /**
     * 查询被拒付的票据
     */
    @Query("SELECT b FROM Bill b WHERE b.dishonored = true ORDER BY b.dishonoredDate DESC")
    List<Bill> findDishonoredBills();

    // ==================== 区块链状态查询 ====================

    /**
     * 查询未上链的票据
     */
    @Query("SELECT b FROM Bill b WHERE b.blockchainStatus = 'NOT_ONCHAIN' AND b.billStatus NOT IN ('DRAFT', 'PENDING_ISSUANCE')")
    List<Bill> findNotOnChainBills();

    /**
     * 查询待上链的票据
     */
    @Query("SELECT b FROM Bill b WHERE b.blockchainStatus = 'PENDING' ORDER BY b.createdAt DESC")
    List<Bill> findPendingOnChainBills();

    /**
     * 查询上链失败的票据
     */
    @Query("SELECT b FROM Bill b WHERE b.blockchainStatus = 'FAILED' ORDER BY b.createdAt DESC")
    List<Bill> findFailedOnChainBills();

    // ==================== 日期范围查询 ====================

    /**
     * 查询日期范围内创建的票据
     */
    @Query("SELECT b FROM Bill b WHERE b.createdAt BETWEEN :startDate AND :endDate ORDER BY b.createdAt DESC")
    List<Bill> findByCreatedDateRange(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);

    /**
     * 查询日期范围内到期的票据
     */
    @Query("SELECT b FROM Bill b WHERE b.dueDate BETWEEN :startDate AND :endDate ORDER BY b.dueDate ASC")
    List<Bill> findByDueDateRange(@Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);

    // ==================== 高级查询 ====================

    /**
     * 根据票据编号或状态查询
     */
    @Query("SELECT b FROM Bill b WHERE b.billNo LIKE %:keyword% OR b.billStatus = :status")
    List<Bill> findByBillNoContainingOrBillStatus(@Param("keyword") String keyword,
                                                   @Param("status") Bill.BillStatus status);

    /**
     * 查询涉及特定交易合同的票据
     */
    List<Bill> findByTradeContractId(String tradeContractId);

    /**
     * 检查票据编号是否存在
     */
    boolean existsByBillNo(String billNo);

    // ==================== 票据池查询 ====================

    /**
     * 查询票据池中的所有票据
     * 条件：状态为NORMAL，已上链，未冻结，未过期
     */
    @Query("SELECT b FROM Bill b WHERE b.billStatus = 'NORMAL' " +
           "AND b.blockchainStatus = 'ONCHAIN' " +
           "AND b.dueDate > :currentDate " +
           "ORDER BY b.dueDate ASC")
    List<Bill> findBillPoolBills(@Param("currentDate") LocalDateTime currentDate);

    /**
     * 根据票据类型查询票据池
     */
    @Query("SELECT b FROM Bill b WHERE b.billStatus = 'NORMAL' " +
           "AND b.blockchainStatus = 'ONCHAIN' " +
           "AND b.dueDate > :currentDate " +
           "AND b.billType = :billType " +
           "ORDER BY b.dueDate ASC")
    List<Bill> findBillPoolByBillType(@Param("currentDate") LocalDateTime currentDate,
                                       @Param("billType") Bill.BillType billType);

    /**
     * 查询票据池中剩余天数在指定范围内的票据
     */
    @Query("SELECT b FROM Bill b WHERE b.billStatus = 'NORMAL' " +
           "AND b.blockchainStatus = 'ONCHAIN' " +
           "AND b.dueDate > :currentDate " +
           "AND FUNCTION('DATEDIFF', DAY, :currentDate, b.dueDate) BETWEEN :minDays AND :maxDays " +
           "ORDER BY b.dueDate ASC")
    List<Bill> findBillPoolByRemainingDays(@Param("currentDate") LocalDateTime currentDate,
                                            @Param("minDays") Integer minDays,
                                            @Param("maxDays") Integer maxDays);

    /**
     * 查询票据池中面值在指定范围内的票据
     */
    @Query("SELECT b FROM Bill b WHERE b.billStatus = 'NORMAL' " +
           "AND b.blockchainStatus = 'ONCHAIN' " +
           "AND b.dueDate > :currentDate " +
           "AND b.faceValue BETWEEN :minAmount AND :maxAmount " +
           "ORDER BY b.faceValue DESC")
    List<Bill> findBillPoolByFaceValueRange(@Param("currentDate") LocalDateTime currentDate,
                                             @Param("minAmount") BigDecimal minAmount,
                                             @Param("maxAmount") BigDecimal maxAmount);

    /**
     * 查询票据池中特定承兑人的票据
     */
    @Query("SELECT b FROM Bill b WHERE b.billStatus = 'NORMAL' " +
           "AND b.blockchainStatus = 'ONCHAIN' " +
           "AND b.dueDate > :currentDate " +
           "AND b.draweeId = :draweeId " +
           "ORDER BY b.dueDate ASC")
    List<Bill> findBillPoolByDrawee(@Param("currentDate") LocalDateTime currentDate,
                                    @Param("draweeId") String draweeId);

    /**
     * 查询票据池中特定持票人的票据
     */
    @Query("SELECT b FROM Bill b WHERE b.billStatus = 'NORMAL' " +
           "AND b.blockchainStatus = 'ONCHAIN' " +
           "AND b.dueDate > :currentDate " +
           "AND b.currentHolderId = :holderId " +
           "ORDER BY b.dueDate ASC")
    List<Bill> findBillPoolByHolder(@Param("currentDate") LocalDateTime currentDate,
                                    @Param("holderId") String holderId);

    /**
     * 统计票据池中的票据数量
     */
    @Query("SELECT COUNT(b) FROM Bill b WHERE b.billStatus = 'NORMAL' " +
           "AND b.blockchainStatus = 'ONCHAIN' " +
           "AND b.dueDate > :currentDate")
    Long countBillPoolBills(@Param("currentDate") LocalDateTime currentDate);

    /**
     * 统计票据池中票据的总面值
     */
    @Query("SELECT SUM(b.faceValue) FROM Bill b WHERE b.billStatus = 'NORMAL' " +
           "AND b.blockchainStatus = 'ONCHAIN' " +
           "AND b.dueDate > :currentDate")
    BigDecimal sumBillPoolFaceValue(@Param("currentDate") LocalDateTime currentDate);

    // ==================== 新增查询方法 ====================

    /**
     * 根据创建时间范围查询票据
     */
    List<Bill> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 根据父票据ID查询子票据
     */
    List<Bill> findByParentBillId(String parentBillId);

    /**
     * 根据担保ID查询票据
     */
    List<Bill> findByGuaranteeId(String guaranteeId);

    /**
     * 查询有担保的票据
     */
    List<Bill> findByHasGuaranteeTrue();

    // ==================== 持票人地址查询 ====================

    /**
     * 根据当前持票人区块链地址查询票据
     */
    List<Bill> findByCurrentHolderAddress(String currentHolderAddress);

    /**
     * 根据当前持票人区块链地址和创建时间范围查询票据
     */
    List<Bill> findByCurrentHolderAddressAndCreatedAtBetween(
        String currentHolderAddress,
        LocalDateTime startDate,
        LocalDateTime endDate
    );
}
