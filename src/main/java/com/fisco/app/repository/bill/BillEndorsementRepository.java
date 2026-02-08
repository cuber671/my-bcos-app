package com.fisco.app.repository.bill;

import com.fisco.app.entity.bill.BillEndorsement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 票据背书记录Repository
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-02
 */
@Repository
public interface BillEndorsementRepository extends JpaRepository<BillEndorsement, String> {

    /**
     * 根据票据ID查询背书记录
     */
    List<BillEndorsement> findByBillId(String billId);

    /**
     * 根据票据编号查询背书记录，按时间排序
     */
    List<BillEndorsement> findByBillNoOrderByEndorsementDateAsc(String billNo);

    /**
     * 根据背书人ID查询
     */
    List<BillEndorsement> findByEndorserId(String endorserId);

    /**
     * 根据被背书人ID查询
     */
    List<BillEndorsement> findByEndorseeId(String endorseeId);

    /**
     * 根据背书类型查询
     */
    List<BillEndorsement> findByEndorsementType(BillEndorsement.EndorsementType endorsementType);

    /**
     * 查询票据的最新背书记录
     */
    @Query("SELECT e FROM BillEndorsement e WHERE e.billId = :billId ORDER BY e.endorsementDate DESC")
    List<BillEndorsement> findLatestEndorsementByBillId(@Param("billId") String billId);

    /**
     * 统计票据的背书次数
     */
    @Query("SELECT COUNT(e) FROM BillEndorsement e WHERE e.billId = :billId")
    Long countByBillId(@Param("billId") String billId);

    /**
     * 查询企业的背书历史（作为背书人）
     */
    @Query("SELECT e FROM BillEndorsement e WHERE e.endorserId = :endorserId ORDER BY e.endorsementDate DESC")
    List<BillEndorsement> findEndorserHistory(@Param("endorserId") String endorserId);

    /**
     * 查询企业的背书历史（作为被背书人）
     */
    @Query("SELECT e FROM BillEndorsement e WHERE e.endorseeId = :endorseeId ORDER BY e.endorsementDate DESC")
    List<BillEndorsement> findEndorseeHistory(@Param("endorseeId") String endorseeId);

    /**
     * 查询涉及仓单交付的背书记录
     */
    @Query("SELECT e FROM BillEndorsement e WHERE e.receiptDelivery = true ORDER BY e.endorsementDate DESC")
    List<BillEndorsement> findReceiptDeliveryEndorsements();

    /**
     * 查询关联特定仓单的背书记录
     */
    List<BillEndorsement> findByRelatedReceiptId(String receiptId);

    /**
     * 查询日期范围内的背书记录
     */
    @Query("SELECT e FROM BillEndorsement e WHERE e.endorsementDate BETWEEN :startDate AND :endDate ORDER BY e.endorsementDate DESC")
    List<BillEndorsement> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    /**
     * 检查背书连续性（按时间顺序）
     */
    @Query("SELECT e FROM BillEndorsement e WHERE e.billId = :billId ORDER BY e.endorsementDate ASC")
    List<BillEndorsement> findEndorsementChain(@Param("billId") String billId);

    /**
     * 根据区块链状态查询
     */
    List<BillEndorsement> findByBlockchainStatus(BillEndorsement.BlockchainStatus status);

    /**
     * 查询未上链的背书记录
     */
    @Query("SELECT e FROM BillEndorsement e WHERE e.blockchainStatus = 'NOT_ONCHAIN' ORDER BY e.createdAt DESC")
    List<BillEndorsement> findNotOnChainEndorsements();
}
