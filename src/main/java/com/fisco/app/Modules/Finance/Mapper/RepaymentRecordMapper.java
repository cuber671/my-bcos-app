package com.fisco.app.Modules.Finance.Mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fisco.app.Modules.Finance.Entity.RepaymentRecord;

/**
 * 还款记录 Mapper接口
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Mapper
public interface RepaymentRecordMapper extends BaseMapper<RepaymentRecord> {

    /**
     * 根据还款编号查询还款记录
     *
     * @param repaymentNo 还款编号
     * @return 还款记录
     */
    @Select("SELECT * FROM t_repayment_record WHERE repayment_no = #{repaymentNo}")
    RepaymentRecord selectByRepaymentNo(@Param("repaymentNo") String repaymentNo);

    /**
     * 根据应收款ID查询还款记录列表
     *
     * @param receivableId 应收款ID
     * @return 还款记录列表
     */
    @Select("SELECT * FROM t_repayment_record WHERE receivable_id = #{receivableId} ORDER BY repayment_time DESC")
    List<RepaymentRecord> selectByReceivableId(@Param("receivableId") Long receivableId);

    /**
     * 根据还款类型查询还款记录列表
     *
     * @param repaymentType 还款类型：1-现金还款；2-仓单抵债
     * @return 还款记录列表
     */
    @Select("SELECT * FROM t_repayment_record WHERE repayment_type = #{repaymentType} ORDER BY repayment_time DESC")
    List<RepaymentRecord> selectByRepaymentType(@Param("repaymentType") Integer repaymentType);

    /**
     * 根据仓单ID查询还款记录列表（用于仓单抵债查询）
     *
     * @param receiptId 仓单ID
     * @return 还款记录列表
     */
    @Select("SELECT * FROM t_repayment_record WHERE receipt_id = #{receiptId} ORDER BY repayment_time DESC")
    List<RepaymentRecord> selectByReceiptId(@Param("receiptId") Long receiptId);
}
