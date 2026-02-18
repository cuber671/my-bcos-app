package com.fisco.app.repository.pledge;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fisco.app.entity.pledge.ReleaseRecord;

/**
 * 仓单释放记录Repository
 */
@Repository
public interface ReleaseRecordRepository extends JpaRepository<ReleaseRecord, String> {

    /**
     * 根据仓单ID查找释放记录
     */
    List<ReleaseRecord> findByReceiptIdOrderByReleaseDateDesc(String receiptId);

    /**
     * 查找所有者的所有释放记录
     */
    List<ReleaseRecord> findByOwnerAddressOrderByReleaseDateDesc(String ownerAddress);

    /**
     * 查找金融机构的所有释放记录
     */
    List<ReleaseRecord> findByFinancialInstitutionAddressOrderByReleaseDateDesc(String institutionAddress);

    /**
     * 根据释放类型查找
     */
    List<ReleaseRecord> findByReleaseType(ReleaseRecord.ReleaseType releaseType);

    /**
     * 查询指定时间范围内的释放记录
     */
    @Query("SELECT r FROM ReleaseRecord r WHERE r.releaseDate BETWEEN :startDate AND :endDate")
    List<ReleaseRecord> findByReleaseDateBetween(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * 统计金融机构的释放总金额
     */
    @Query("SELECT SUM(r.repaymentAmount) FROM ReleaseRecord r WHERE r.financialInstitutionAddress = :institutionAddress")
    java.math.BigDecimal totalRepaymentByInstitution(@Param("institutionAddress") String institutionAddress);

    /**
     * 统计金融机构的利息总收入
     */
    @Query("SELECT SUM(r.interestAmount) FROM ReleaseRecord r WHERE r.financialInstitutionAddress = :institutionAddress")
    java.math.BigDecimal totalInterestByInstitution(@Param("institutionAddress") String institutionAddress);
}
