package com.fisco.app.Modules.Logistics.Mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fisco.app.Modules.Logistics.Entity.LogisticsTrack;

/**
 * 物流轨迹记录 Mapper接口
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Mapper
public interface LogisticsTrackMapper extends BaseMapper<LogisticsTrack> {

    /**
     * 根据委派单编号查询轨迹列表
     *
     * @param voucherNo 委派单编号
     * @return 轨迹列表
     */
    @Select("SELECT * FROM t_logistics_track WHERE voucher_no = #{voucherNo} ORDER BY event_time ASC")
    List<LogisticsTrack> selectByVoucherNo(@Param("voucherNo") String voucherNo);

    /**
     * 查询最新的轨迹记录
     *
     * @param voucherNo 委派单编号
     * @return 最新轨迹记录
     */
    @Select("SELECT * FROM t_logistics_track WHERE voucher_no = #{voucherNo} ORDER BY event_time DESC LIMIT 1")
    LogisticsTrack selectLatestByVoucherNo(@Param("voucherNo") String voucherNo);

    /**
     * 查询偏航记录
     *
     * @param voucherNo 委派单编号
     * @return 偏航记录列表
     */
    @Select("SELECT * FROM t_logistics_track WHERE voucher_no = #{voucherNo} AND is_deviation = 1 ORDER BY event_time DESC")
    List<LogisticsTrack> selectDeviationByVoucherNo(@Param("voucherNo") String voucherNo);
}
