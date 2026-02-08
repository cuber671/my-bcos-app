package com.fisco.app.repository.risk;

import com.fisco.app.entity.risk.OverdueRemindRecord;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;

import java.util.List;

import java.time.LocalDateTime;

/**
 * 逾期催收记录Repository
 */
@Repository
public interface OverdueRemindRecordRepository extends JpaRepository<OverdueRemindRecord, String> {

    /**
     * 根据应收账款ID查询催收记录
     */
    List<OverdueRemindRecord> findByReceivableIdOrderByRemindDateDesc(String receivableId);

    /**
     * 根据催收类型查询记录
     */
    List<OverdueRemindRecord> findByRemindTypeOrderByRemindDateDesc(OverdueRemindRecord.RemindType remindType);

    /**
     * 根据操作人地址查询记录
     */
    List<OverdueRemindRecord> findByOperatorAddressOrderByRemindDateDesc(String operatorAddress);

    /**
     * 查询指定日期范围内的催收记录
     */
    List<OverdueRemindRecord> findByRemindDateBetweenOrderByRemindDateDesc(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 统计某个应收账款的催收次数
     */
    @Query("SELECT COUNT(r) FROM OverdueRemindRecord r WHERE r.receivableId = :receivableId")
    long countByReceivableId(@Param("receivableId") String receivableId);

    /**
     * 查询某个应收账款的最后一次催收记录
     */
    @Query("SELECT r FROM OverdueRemindRecord r WHERE r.receivableId = :receivableId ORDER BY r.remindDate DESC LIMIT 1")
    OverdueRemindRecord findLatestByReceivableId(@Param("receivableId") String receivableId);
}
