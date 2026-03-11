package com.fisco.app.Modules.Logistics.Mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fisco.app.Modules.Logistics.Entity.LogisticsDelegate;

/**
 * 电子物流委派单 Mapper接口
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Mapper
public interface LogisticsDelegateMapper extends BaseMapper<LogisticsDelegate> {

    /**
     * 根据 voucherNo 查询委派单
     *
     * @param voucherNo 委派单编号
     * @return 委派单
     */
    @Select("SELECT * FROM t_logistics_delegate WHERE voucher_no = #{voucherNo}")
    LogisticsDelegate selectByVoucherNo(@Param("voucherNo") String voucherNo);

    /**
     * 更新委派单状态
     *
     * @param voucherNo 委派单编号
     * @param status 新状态
     * @return 影响行数
     */
    @Update("UPDATE t_logistics_delegate SET status = #{status}, update_time = NOW() WHERE voucher_no = #{voucherNo}")
    int updateStatusByVoucherNo(@Param("voucherNo") String voucherNo, @Param("status") Integer status);

    /**
     * 根据企业ID查询委派单列表
     *
     * @param ownerEntId 企业ID
     * @return 委派单列表
     */
    @Select("SELECT * FROM t_logistics_delegate WHERE owner_ent_id = #{ownerEntId} ORDER BY create_time DESC")
    java.util.List<LogisticsDelegate> selectByOwnerEntId(@Param("ownerEntId") Long ownerEntId);

    /**
     * 根据承运企业ID查询委派单列表
     *
     * @param carrierEntId 承运企业ID
     * @return 委派单列表
     */
    @Select("SELECT * FROM t_logistics_delegate WHERE carrier_ent_id = #{carrierEntId} ORDER BY create_time DESC")
    java.util.List<LogisticsDelegate> selectByCarrierEntId(@Param("carrierEntId") Long carrierEntId);
}
