package com.fisco.app.Modules.Logistics.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.fisco.app.Modules.Logistics.Entity.LogisticsDelegate;
import com.fisco.app.Modules.Logistics.Entity.LogisticsTrack;

/**
 * 物流服务接口
 *
 * 定义物流模块的核心业务方法
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
public interface LogisticsService {

    // ==================== 委派单操作 ====================

    /**
     * 创建物流委派单
     *
     * @param delegate 委派单信息
     * @return 创建结果
     */
    LogisticsDelegate createDelegate(LogisticsDelegate delegate);

    /**
     * 根据ID查询委派单
     *
     * @param id 委派单ID
     * @return 委派单
     */
    LogisticsDelegate getDelegateById(Long id);

    /**
     * 根据 voucherNo 查询委派单
     *
     * @param voucherNo 委派单编号
     * @return 委派单
     */
    LogisticsDelegate getDelegateByVoucherNo(String voucherNo);

    /**
     * 查询企业所有的委派单
     *
     * @param ownerEntId 企业ID
     * @return 委派单列表
     */
    List<LogisticsDelegate> listByOwnerEntId(Long ownerEntId);

    /**
     * 查询承运企业的委派单
     *
     * @param carrierEntId 承运企业ID
     * @return 委派单列表
     */
    List<LogisticsDelegate> listByCarrierEntId(Long carrierEntId);

    /**
     * 物流指派任务（派单给司机）
     *
     * @param voucherNo 委派单编号
     * @param driverId 司机ID
     * @param driverName 司机姓名
     * @param vehicleNo 车牌号
     * @return 更新结果
     */
    LogisticsDelegate assignDriver(String voucherNo, String driverId, String driverName, String vehicleNo);

    /**
     * 仓库提货确认（出库核销）
     *
     * @param voucherNo 委派单编号
     * @param authCode 授权码
     * @return 更新结果
     */
    LogisticsDelegate confirmPickup(String voucherNo, String authCode);

    /**
     * 仓库提货确认（带地理围栏校验）
     *
     * @param voucherNo 委派单编号
     * @param authCode 授权码
     * @param driverLatitude 司机当前位置纬度
     * @param driverLongitude 司机当前位置经度
     * @return 更新结果
     */
    LogisticsDelegate confirmPickup(String voucherNo, String authCode,
                                   BigDecimal driverLatitude, BigDecimal driverLongitude);

    /**
     * 到货入库申请
     *
     * @param voucherNo 委派单编号
     * @param actionType 到货处理动作
     * @param targetReceiptId 目标仓单ID（增量入库时必填）
     * @return 更新结果
     */
    LogisticsDelegate arrive(String voucherNo, Integer actionType, Long targetReceiptId);

    /**
     * 更新物流状态
     *
     * @param voucherNo 委派单编号
     * @param status 新状态
     * @return 更新结果
     */
    LogisticsDelegate updateStatus(String voucherNo, Integer status);

    // ==================== 轨迹操作 ====================

    /**
     * 上报物流轨迹
     *
     * @param track 轨迹信息
     * @return 上报结果
     */
    LogisticsTrack reportTrack(LogisticsTrack track);

    /**
     * 查询物流轨迹列表
     *
     * @param voucherNo 委派单编号
     * @return 轨迹列表
     */
    List<LogisticsTrack> listTracks(String voucherNo);

    /**
     * 获取最新轨迹
     *
     * @param voucherNo 委派单编号
     * @return 最新轨迹
     */
    LogisticsTrack getLatestTrack(String voucherNo);

    /**
     * 查询偏航记录
     *
     * @param voucherNo 委派单编号
     * @return 偏航记录列表
     */
    List<LogisticsTrack> listDeviations(String voucherNo);

    // ==================== 物流追踪 ====================

    /**
     * 物流状态追踪
     *
     * @param voucherNo 委派单编号
     * @return 物流追踪信息
     */
    Map<String, Object> trackLogistics(String voucherNo);

    /**
     * 确认交付
     *
     * @param voucherNo 委派单编号
     * @param action 交付动作
     * @param targetReceiptId 目标仓单ID
     * @return 更新结果
     */
    LogisticsDelegate confirmDelivery(String voucherNo, Integer action, String targetReceiptId);

    /**
     * 使委派单失效
     *
     * @param voucherNo 委派单编号
     * @return 更新结果
     */
    LogisticsDelegate invalidate(String voucherNo);

    /**
     * 验证物流委派单
     *
     * @param voucherNo 委派单编号
     * @return 是否有效
     */
    boolean validateDelegate(String voucherNo);
}
