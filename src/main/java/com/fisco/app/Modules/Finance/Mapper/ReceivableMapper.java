package com.fisco.app.Modules.Finance.Mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fisco.app.Modules.Finance.Entity.Receivable;

/**
 * 电子应收款项 Mapper接口
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Mapper
public interface ReceivableMapper extends BaseMapper<Receivable> {

    /**
     * 根据 receivableNo 查询应收款
     *
     * @param receivableNo 应收款编号
     * @return 应收款
     */
    @Select("SELECT * FROM t_receivable WHERE receivable_no = #{receivableNo}")
    Receivable selectByReceivableNo(@Param("receivableNo") String receivableNo);

    /**
     * 根据关联物流单ID查询应收款列表
     *
     * @param sourceVoucherId 物流单ID
     * @return 应收款列表
     */
    @Select("SELECT * FROM t_receivable WHERE source_voucher_id = #{sourceVoucherId} ORDER BY create_time DESC")
    List<Receivable> selectBySourceVoucherId(@Param("sourceVoucherId") Long sourceVoucherId);

    /**
     * 根据债权人ID查询应收款列表
     *
     * @param creditorEntId 债权人ID
     * @return 应收款列表
     */
    @Select("SELECT * FROM t_receivable WHERE creditor_ent_id = #{creditorEntId} ORDER BY create_time DESC")
    List<Receivable> selectByCreditorEntId(@Param("creditorEntId") Long creditorEntId);

    /**
     * 根据债务人ID查询应收款列表
     *
     * @param debtorEntId 债务人ID
     * @return 应收款列表
     */
    @Select("SELECT * FROM t_receivable WHERE debtor_ent_id = #{debtorEntId} ORDER BY create_time DESC")
    List<Receivable> selectByDebtorEntId(@Param("debtorEntId") Long debtorEntId);

    /**
     * 更新应收款状态
     *
     * @param receivableNo 应收款编号
     * @param status 新状态
     * @return 影响行数
     */
    @Update("UPDATE t_receivable SET status = #{status}, update_time = NOW() WHERE receivable_no = #{receivableNo}")
    int updateStatusByReceivableNo(@Param("receivableNo") String receivableNo, @Param("status") Integer status);

    /**
     * 更新应收款金额信息
     *
     * @param receivableNo 应收款编号
     * @param adjustedAmount 结算金额
     * @param balanceUnpaid 待还余额
     * @return 影响行数
     */
    @Update("UPDATE t_receivable SET adjusted_amount = #{adjustedAmount}, balance_unpaid = #{balanceUnpaid}, update_time = NOW() WHERE receivable_no = #{receivableNo}")
    int updateAmountByReceivableNo(@Param("receivableNo") String receivableNo,
                                    @Param("adjustedAmount") java.math.BigDecimal adjustedAmount,
                                    @Param("balanceUnpaid") java.math.BigDecimal balanceUnpaid);

    /**
     * 更新已回收金额和待还余额
     *
     * @param receivableNo 应收款编号
     * @param collectedAmount 新已回收金额
     * @param balanceUnpaid 新待还余额
     * @return 影响行数
     */
    @Update("UPDATE t_receivable SET collected_amount = #{collectedAmount}, balance_unpaid = #{balanceUnpaid}, update_time = NOW() WHERE receivable_no = #{receivableNo}")
    int updateCollectedAmount(@Param("receivableNo") String receivableNo,
                              @Param("collectedAmount") java.math.BigDecimal collectedAmount,
                              @Param("balanceUnpaid") java.math.BigDecimal balanceUnpaid);
}
