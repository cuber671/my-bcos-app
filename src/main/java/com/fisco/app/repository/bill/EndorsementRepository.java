package com.fisco.app.repository.bill;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fisco.app.entity.bill.Endorsement;

/**
 * 票据背书Repository
 */
@Repository
public interface EndorsementRepository extends JpaRepository<Endorsement, String> {

    /**
     * 根据票据ID查找所有背书记录
     */
    List<Endorsement> findByBillIdOrderByEndorsementDateAsc(String billId);

    /**
     * 根据票据ID查找最新的背书记录
     * 使用 First instead of LIMIT in JPQL
     */
    @Query("SELECT e FROM Endorsement e WHERE e.billId = :billId ORDER BY e.endorsementSequence DESC")
    List<Endorsement> findEndorsementsByBillIdOrderBySequenceDesc(@Param("billId") String billId);

    /**
     * 查找作为背书人的所有背书记录
     */
    List<Endorsement> findByEndorserAddressOrderByEndorsementDateDesc(String endorserAddress);

    /**
     * 查找作为被背书人的所有背书记录
     */
    List<Endorsement> findByEndorseeAddressOrderByEndorsementDateDesc(String endorseeAddress);

    /**
     * 根据背书类型查找
     */
    List<Endorsement> findByEndorsementType(Endorsement.EndorsementType endorsementType);

    /**
     * 统计票据的背书次数
     */
    @Query("SELECT COUNT(e) FROM Endorsement e WHERE e.billId = :billId")
    Long countByBillId(@Param("billId") String billId);

    /**
     * 查询指定时间范围内的背书记录
     */
    @Query("SELECT e FROM Endorsement e WHERE e.endorsementDate BETWEEN :startDate AND :endDate")
    List<Endorsement> findByEndorsementDateBetween(
        @Param("startDate") java.time.LocalDateTime startDate,
        @Param("endDate") java.time.LocalDateTime endDate
    );

    /**
     * 获取票据的下一个背书序号
     */
    @Query("SELECT COALESCE(MAX(e.endorsementSequence), 0) + 1 FROM Endorsement e WHERE e.billId = :billId")
    Integer getNextEndorsementSequence(@Param("billId") String billId);
}
