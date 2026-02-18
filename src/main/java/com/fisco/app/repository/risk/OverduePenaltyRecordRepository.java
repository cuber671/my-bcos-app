package com.fisco.app.repository.risk;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional; // 建议使用 Optional 包装单条记录

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fisco.app.entity.risk.OverduePenaltyRecord;

/**
 * 逾期罚息记录Repository
 */
@Repository
public interface OverduePenaltyRecordRepository extends JpaRepository<OverduePenaltyRecord, String> {

    /**
     * 根据应收账款ID查询罚息记录
     */
    List<OverduePenaltyRecord> findByReceivableIdOrderByCalculateDateDesc(String receivableId);

    /**
     * 根据罚息类型查询记录
     */
    List<OverduePenaltyRecord> findByPenaltyTypeOrderByCalculateDateDesc(OverduePenaltyRecord.PenaltyType penaltyType);

    /**
     * 查询指定日期范围内的罚息记录
     */
    List<OverduePenaltyRecord> findByCalculateDateBetweenOrderByCalculateDateDesc(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 统计某个应收账款的罚息总额
     */
    @Query("SELECT COALESCE(SUM(r.penaltyAmount), 0) FROM OverduePenaltyRecord r WHERE r.receivableId = :receivableId")
    BigDecimal sumPenaltyByReceivableId(@Param("receivableId") String receivableId);

    /**
     * 查询某个应收账款的最后一次罚息记录
     * 使用 Spring Data JPA 的命名规范自动实现 LIMIT 1
     */
    Optional<OverduePenaltyRecord> findFirstByReceivableIdOrderByCalculateDateDesc(String receivableId);
}