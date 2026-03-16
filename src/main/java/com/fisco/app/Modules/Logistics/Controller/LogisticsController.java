package com.fisco.app.Modules.Logistics.Controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fisco.app.Common.Utils.CurrentUser;
import com.fisco.app.Common.Utils.Result;
import com.fisco.app.Modules.Logistics.Entity.LogisticsDelegate;
import com.fisco.app.Modules.Logistics.Entity.LogisticsTrack;
import com.fisco.app.Modules.Logistics.Service.LogisticsService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * 物流控制器
 *
 * 提供物流模块的REST API接口，包括：
 * - 物流委派单创建与管理
 * - 物流指派任务
 * - 仓库提货确认
 * - 到货入库申请
 * - 物流状态追踪
 *
 * 权限控制：
 * - 大多数接口需要JWT认证，通过CurrentUser获取企业ID
 * - 校验企业是否拥有该委派单的操作权限
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Api(tags = "物流管理")
@RestController
@RequestMapping("/api/v1/logistics")
public class LogisticsController {

    private static final Logger logger = LoggerFactory.getLogger(LogisticsController.class);

    @Autowired
    private LogisticsService logisticsService;

    // ==================== 委派单管理 ====================

    /**
     * 创建物流委派单
     *
     * 权限：企业用户（货主/买方）
     * 业务逻辑：校验仓单余额，锁定对应数量
     */
    @ApiOperation("创建物流委派单")
    @PostMapping("/create")
    public Result<LogisticsDelegate> createDelegate(
            @ApiParam(value = "委派单信息", required = true) @RequestBody LogisticsDelegate delegate,
            HttpServletRequest request) {
        try {
            // 参数校验
            if (delegate.getBusinessScene() == null) {
                return Result.error(400, "业务场景不能为空（1-直接移库，2-转让后移库，3-发货入库）");
            }
            if (delegate.getTransportQuantity() == null) {
                return Result.error(400, "运输数量不能为空");
            }
            if (delegate.getUnit() == null || delegate.getUnit().isEmpty()) {
                return Result.error(400, "计量单位不能为空");
            }
            if (delegate.getCarrierEntId() == null) {
                return Result.error(400, "承运企业ID不能为空");
            }
            if (delegate.getSourceWhId() == null) {
                return Result.error(400, "起运地仓库ID不能为空");
            }
            if (delegate.getActionOnArrival() == null) {
                delegate.setActionOnArrival(1); // 默认生成新仓单
            }

            // 获取当前企业ID
            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                return Result.error(401, "未登录或Token无效");
            }

            // 设置货主企业ID
            delegate.setOwnerEntId(entId);

            // 创建委派单
            LogisticsDelegate result = logisticsService.createDelegate(delegate);

            logger.info("创建物流委派单成功: voucherNo={}, entId={}", result.getVoucherNo(), entId);
            return Result.success(result);

        } catch (IllegalArgumentException e) {
            logger.warn("创建物流委派单参数错误: ", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("创建物流委派单异常: ", e);
            return Result.error(500, "创建物流委派单失败");
        }
    }

    /**
     * 查询委派单详情
     *
     * 权限：委派单所属企业或承运企业
     */
    @ApiOperation("查询委派单详情")
    @GetMapping("/delegate/{voucherNo}")
    public Result<LogisticsDelegate> getDelegate(
            @ApiParam(value = "委派单编号", required = true) @PathVariable String voucherNo,
            HttpServletRequest request) {
        try {
            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                return Result.error(401, "未登录或Token无效");
            }

            LogisticsDelegate delegate = logisticsService.getDelegateByVoucherNo(voucherNo);
            if (delegate == null) {
                return Result.error(404, "委派单不存在");
            }

            // 校验权限：只能是货主企业或承运企业查询
            if (!entId.equals(delegate.getOwnerEntId()) && !entId.equals(delegate.getCarrierEntId())) {
                return Result.error(403, "无权限查询该委派单");
            }

            return Result.success(delegate);

        } catch (Exception e) {
            logger.error("查询委派单异常: ", e);
            return Result.error(500, "查询失败");
        }
    }

    /**
     * 查询企业所有的委派单列表
     *
     * 权限：当前登录企业
     */
    @ApiOperation("查询企业委派单列表")
    @GetMapping("/delegate/list")
    public Result<List<LogisticsDelegate>> listDelegates(
            @ApiParam("业务场景筛选") @RequestParam(required = false) Integer businessScene,
            @ApiParam("状态筛选") @RequestParam(required = false) Integer status,
            HttpServletRequest request) {
        try {
            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                return Result.error(401, "未登录或Token无效");
            }

            // 查询作为货主的委派单
            List<LogisticsDelegate> ownerList = logisticsService.listByOwnerEntId(entId);

            // 查询作为承运企业的委派单
            List<LogisticsDelegate> carrierList = logisticsService.listByCarrierEntId(entId);

            // 合并结果并去重
            ownerList.addAll(carrierList);

            // 如果有筛选条件，在内存中过滤
            if (businessScene != null) {
                ownerList.removeIf(d -> !businessScene.equals(d.getBusinessScene()));
            }
            if (status != null) {
                ownerList.removeIf(d -> !status.equals(d.getStatus()));
            }

            return Result.success(ownerList);

        } catch (Exception e) {
            logger.error("查询委派单列表异常: ", e);
            return Result.error(500, "查询失败");
        }
    }

    // ==================== 物流指派 ====================

    /**
     * 物流指派任务
     *
     * 权限：承运企业（物流公司）
     * 业务逻辑：绑定司机身份与车牌，生成动态加密二维码
     */
    @ApiOperation("物流指派任务")
    @PostMapping("/assign")
    public Result<LogisticsDelegate> assignDriver(
            @ApiParam(value = "指派信息", required = true) @RequestBody Map<String, Object> params,
            HttpServletRequest request) {
        try {
            String voucherNo = params.get("voucherNo") != null ? params.get("voucherNo").toString() : null;
            String driverId = params.get("driverId") != null ? params.get("driverId").toString() : null;
            String driverName = params.get("driverName") != null ? params.get("driverName").toString() : null;
            String vehicleNo = params.get("vehicleNo") != null ? params.get("vehicleNo").toString() : null;

            if (voucherNo == null || voucherNo.isEmpty()) {
                return Result.error(400, "委派单编号不能为空");
            }
            if (driverId == null || driverId.isEmpty()) {
                return Result.error(400, "司机ID不能为空");
            }
            if (driverName == null || driverName.isEmpty()) {
                return Result.error(400, "司机姓名不能为空");
            }
            if (vehicleNo == null || vehicleNo.isEmpty()) {
                return Result.error(400, "车牌号不能为空");
            }

            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                return Result.error(401, "未登录或Token无效");
            }

            // 校验权限：只能是承运企业操作
            LogisticsDelegate delegate = logisticsService.getDelegateByVoucherNo(voucherNo);
            if (delegate == null) {
                return Result.error(404, "委派单不存在");
            }

            if (!entId.equals(delegate.getCarrierEntId())) {
                return Result.error(403, "无权限操作该委派单，只有承运企业才能指派司机");
            }

            // 执行指派
            LogisticsDelegate result = logisticsService.assignDriver(voucherNo, driverId, driverName, vehicleNo);

            logger.info("物流指派任务完成: voucherNo={}, entId={}", voucherNo, entId);
            return Result.success(result);

        } catch (IllegalArgumentException e) {
            logger.warn("物流指派参数错误: ", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("物流指派异常: ", e);
            return Result.error(500, "指派失败");
        }
    }

    // ==================== 提货确认 ====================

    /**
     * 仓库提货确认
     *
     * 权限：仓库管理员
     * 业务逻辑：校验地理围栏和司机实名信息，扣减原仓单重量
     */
    @ApiOperation("仓库提货确认")
    @PostMapping("/pickup")
    public Result<LogisticsDelegate> confirmPickup(
            @ApiParam(value = "提货确认信息", required = true) @RequestBody Map<String, Object> params,
            HttpServletRequest request) {
        try {
            String voucherNo = params.get("voucherNo") != null ? params.get("voucherNo").toString() : null;
            String authCode = params.get("authCode") != null ? params.get("authCode").toString() : null;
            BigDecimal driverLatitude = params.get("driverLatitude") != null ? new BigDecimal(params.get("driverLatitude").toString()) : null;
            BigDecimal driverLongitude = params.get("driverLongitude") != null ? new BigDecimal(params.get("driverLongitude").toString()) : null;

            if (voucherNo == null || voucherNo.isEmpty()) {
                return Result.error(400, "委派单编号不能为空");
            }

            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                return Result.error(401, "未登录或Token无效");
            }

            // 执行提货确认（带地理围栏校验）
            LogisticsDelegate result;
            if (driverLatitude != null && driverLongitude != null) {
                result = logisticsService.confirmPickup(voucherNo, authCode, driverLatitude, driverLongitude);
            } else {
                result = logisticsService.confirmPickup(voucherNo, authCode);
            }

            logger.info("仓库提货确认成功: voucherNo={}, entId={}", voucherNo, entId);
            return Result.success(result);

        } catch (IllegalArgumentException e) {
            logger.warn("提货确认参数错误: ", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("提货确认异常: ", e);
            return Result.error(500, "提货确认失败");
        }
    }

    // ==================== 到货入库 ====================

    /**
     * 到货入库申请
     *
     * 权限：目的地仓库管理员
     * 业务逻辑：根据action_type决定生成新仓单或增量入库
     */
    @ApiOperation("到货入库申请")
    @PostMapping("/arrive")
    public Result<LogisticsDelegate> arrive(
            @ApiParam(value = "到货入库信息", required = true) @RequestBody Map<String, Object> params,
            HttpServletRequest request) {
        try {
            String voucherNoStr = params.get("voucherNo") != null ? params.get("voucherNo").toString() : null;
            Object actionTypeObj = params.get("actionType");
            Object targetReceiptIdObj = params.get("targetReceiptId");

            if (voucherNoStr == null || voucherNoStr.isEmpty()) {
                return Result.error(400, "委派单编号不能为空");
            }
            Integer actionType = actionTypeObj != null ? Integer.parseInt(actionTypeObj.toString()) : null;
            Long targetReceiptId = targetReceiptIdObj != null ? Long.parseLong(targetReceiptIdObj.toString()) : null;

            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                return Result.error(401, "未登录或Token无效");
            }

            // 校验所有权：承运企业才能执行到货入库
            LogisticsDelegate delegate = logisticsService.getDelegateByVoucherNo(voucherNoStr);
            if (delegate == null) {
                return Result.error(404, "委派单不存在");
            }
            if (!entId.equals(delegate.getCarrierEntId())) {
                return Result.error(403, "无权限操作该委派单");
            }

            // 校验参数
            if (actionType == null || (actionType != 1 && actionType != 2)) {
                return Result.error(400, "无效的到货处理动作，请使用1或2");
            }

            if (actionType == 2 && targetReceiptId == null) {
                return Result.error(400, "增量入库时必须指定目标仓单ID");
            }

            // 执行到货入库
            LogisticsDelegate result = logisticsService.arrive(voucherNoStr, actionType, targetReceiptId);

            logger.info("到货入库申请成功: voucherNo={}, actionType={}, entId={}", voucherNoStr, actionType, entId);
            return Result.success(result);

        } catch (IllegalArgumentException e) {
            logger.warn("到货入库参数错误: ", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("到货入库异常: ", e);
            return Result.error(500, "到货入库失败");
        }
    }

    // ==================== 物流追踪 ====================

    /**
     * 物流状态追踪
     *
     * 权限：委派单相关企业
     * 全流程监控，包含实时地理位置校验
     */
    @ApiOperation("物流状态追踪")
    @GetMapping("/track")
    public Result<Map<String, Object>> trackLogistics(
            @ApiParam("委派单编号") @RequestParam String voucherNo,
            HttpServletRequest request) {
        try {
            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                return Result.error(401, "未登录或Token无效");
            }

            // 校验权限
            LogisticsDelegate delegate = logisticsService.getDelegateByVoucherNo(voucherNo);
            if (delegate == null) {
                return Result.error(404, "委派单不存在");
            }

            if (!entId.equals(delegate.getOwnerEntId()) && !entId.equals(delegate.getCarrierEntId())) {
                return Result.error(403, "无权限查询该委派单");
            }

            // 获取追踪信息
            Map<String, Object> result = logisticsService.trackLogistics(voucherNo);

            return Result.success(result);

        } catch (Exception e) {
            logger.error("物流追踪异常: ", e);
            return Result.error(500, "查询失败");
        }
    }

    /**
     * 查询物流轨迹列表
     */
    @ApiOperation("查询物流轨迹列表")
    @GetMapping("/track/list")
    public Result<List<LogisticsTrack>> listTracks(
            @ApiParam("委派单编号") @RequestParam String voucherNo,
            HttpServletRequest request) {
        try {
            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                return Result.error(401, "未登录或Token无效");
            }

            // 校验所有权
            LogisticsDelegate delegate = logisticsService.getDelegateByVoucherNo(voucherNo);
            if (delegate == null) {
                return Result.error(404, "委派单不存在");
            }
            if (!entId.equals(delegate.getOwnerEntId()) && !entId.equals(delegate.getCarrierEntId())) {
                return Result.error(403, "无权限查询该委派单的轨迹");
            }

            List<LogisticsTrack> tracks = logisticsService.listTracks(voucherNo);
            return Result.success(tracks);

        } catch (Exception e) {
            logger.error("查询轨迹列表异常: ", e);
            return Result.error(500, "查询失败");
        }
    }

    /**
     * 获取最新轨迹
     */
    @ApiOperation("获取最新轨迹")
    @GetMapping("/track/latest")
    public Result<LogisticsTrack> getLatestTrack(
            @ApiParam("委派单编号") @RequestParam String voucherNo,
            HttpServletRequest request) {
        try {
            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                return Result.error(401, "未登录或Token无效");
            }

            // 校验所有权
            LogisticsDelegate delegate = logisticsService.getDelegateByVoucherNo(voucherNo);
            if (delegate == null) {
                return Result.error(404, "委派单不存在");
            }
            if (!entId.equals(delegate.getOwnerEntId()) && !entId.equals(delegate.getCarrierEntId())) {
                return Result.error(403, "无权限查询该委派单的轨迹");
            }

            LogisticsTrack track = logisticsService.getLatestTrack(voucherNo);
            return Result.success(track);

        } catch (Exception e) {
            logger.error("获取最新轨迹异常: ", e);
            return Result.error(500, "查询失败");
        }
    }

    /**
     * 查询偏航记录
     */
    @ApiOperation("查询偏航记录")
    @GetMapping("/track/deviations")
    public Result<List<LogisticsTrack>> listDeviations(
            @ApiParam("委派单编号") @RequestParam String voucherNo,
            HttpServletRequest request) {
        try {
            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                return Result.error(401, "未登录或Token无效");
            }

            // 校验所有权
            LogisticsDelegate delegate = logisticsService.getDelegateByVoucherNo(voucherNo);
            if (delegate == null) {
                return Result.error(404, "委派单不存在");
            }
            if (!entId.equals(delegate.getOwnerEntId()) && !entId.equals(delegate.getCarrierEntId())) {
                return Result.error(403, "无权限查询该委派单的轨迹");
            }

            List<LogisticsTrack> deviations = logisticsService.listDeviations(voucherNo);
            return Result.success(deviations);

        } catch (Exception e) {
            logger.error("查询偏航记录异常: ", e);
            return Result.error(500, "查询失败");
        }
    }

    // ==================== 轨迹上报 ====================

    /**
     * 上报物流轨迹
     *
     * 权限：承运企业（物流公司）或系统自动上报
     */
    @ApiOperation("上报物流轨迹")
    @PostMapping("/track/report")
    public Result<LogisticsTrack> reportTrack(
            @ApiParam(value = "轨迹信息", required = true) @RequestBody LogisticsTrack track,
            HttpServletRequest request) {
        try {
            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                return Result.error(401, "未登录或Token无效");
            }

            // 校验权限
            LogisticsDelegate delegate = logisticsService.getDelegateByVoucherNo(track.getVoucherNo());
            if (delegate == null) {
                return Result.error(404, "委派单不存在");
            }

            // 只能由承运企业上报轨迹
            if (!entId.equals(delegate.getCarrierEntId())) {
                return Result.error(403, "无权限上报该委派单的轨迹");
            }

            LogisticsTrack result = logisticsService.reportTrack(track);
            return Result.success(result);

        } catch (IllegalArgumentException e) {
            logger.warn("上报轨迹参数错误: ", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("上报轨迹异常: ", e);
            return Result.error(500, "上报失败");
        }
    }

    // ==================== 状态更新 ====================

    /**
     * 更新物流状态
     */
    @ApiOperation("更新物流状态")
    @PutMapping("/status")
    public Result<LogisticsDelegate> updateStatus(
            @ApiParam(value = "状态更新信息", required = true) @RequestBody Map<String, Object> params,
            HttpServletRequest request) {
        try {
            String voucherNo = params.get("voucherNo") != null ? params.get("voucherNo").toString() : null;
            Object statusObj = params.get("status");

            if (voucherNo == null || voucherNo.isEmpty()) {
                return Result.error(400, "委派单编号不能为空");
            }
            if (statusObj == null) {
                return Result.error(400, "状态不能为空");
            }
            Integer status = Integer.parseInt(statusObj.toString());

            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                return Result.error(401, "未登录或Token无效");
            }

            // 校验状态值
            if (status < 1 || status > 5) {
                return Result.error(400, "无效的状态值");
            }

            // 校验所有权：货主或承运企业才能更新状态
            LogisticsDelegate delegate = logisticsService.getDelegateByVoucherNo(voucherNo);
            if (delegate == null) {
                return Result.error(404, "委派单不存在");
            }
            if (!entId.equals(delegate.getOwnerEntId()) && !entId.equals(delegate.getCarrierEntId())) {
                return Result.error(403, "无权限操作该委派单");
            }

            LogisticsDelegate result = logisticsService.updateStatus(voucherNo, status);
            return Result.success(result);

        } catch (IllegalArgumentException e) {
            logger.warn("更新状态参数错误: ", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("更新状态异常: ", e);
            return Result.error(500, "更新失败");
        }
    }

    /**
     * 确认交付
     */
    @ApiOperation("确认交付")
    @PostMapping("/delivery/confirm")
    public Result<LogisticsDelegate> confirmDelivery(
            @ApiParam(value = "交付确认信息", required = true) @RequestBody Map<String, Object> params,
            HttpServletRequest request) {
        try {
            String voucherNo = params.get("voucherNo") != null ? params.get("voucherNo").toString() : null;
            Object actionObj = params.get("action");
            Object targetReceiptIdObj = params.get("targetReceiptId");

            if (voucherNo == null || voucherNo.isEmpty()) {
                return Result.error(400, "委派单编号不能为空");
            }

            Integer action = actionObj != null ? Integer.parseInt(actionObj.toString()) : 1;
            String targetReceiptId = targetReceiptIdObj != null ? targetReceiptIdObj.toString() : null;

            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                return Result.error(401, "未登录或Token无效");
            }

            // 校验所有权：货主才能确认交付
            LogisticsDelegate delegate = logisticsService.getDelegateByVoucherNo(voucherNo);
            if (delegate == null) {
                return Result.error(404, "委派单不存在");
            }
            if (!entId.equals(delegate.getOwnerEntId())) {
                return Result.error(403, "只有货主才能确认交付");
            }

            LogisticsDelegate result = logisticsService.confirmDelivery(voucherNo, action, targetReceiptId);
            return Result.success(result);

        } catch (IllegalArgumentException e) {
            logger.warn("确认交付参数错误: ", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("确认交付异常: ", e);
            return Result.error(500, "确认交付失败");
        }
    }

    /**
     * 使委派单失效
     */
    @ApiOperation("使委派单失效")
    @PostMapping("/invalidate")
    public Result<LogisticsDelegate> invalidate(
            @ApiParam(value = "失效操作信息", required = true) @RequestBody Map<String, Object> params,
            HttpServletRequest request) {
        try {
            String voucherNo = params.get("voucherNo") != null ? params.get("voucherNo").toString() : null;

            if (voucherNo == null || voucherNo.isEmpty()) {
                return Result.error(400, "委派单编号不能为空");
            }

            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                return Result.error(401, "未登录或Token无效");
            }

            // 校验权限：只能是货主企业操作
            LogisticsDelegate delegate = logisticsService.getDelegateByVoucherNo(voucherNo);
            if (delegate == null) {
                return Result.error(404, "委派单不存在");
            }

            if (!entId.equals(delegate.getOwnerEntId())) {
                return Result.error(403, "无权限操作该委派单");
            }

            LogisticsDelegate result = logisticsService.invalidate(voucherNo);
            return Result.success(result);

        } catch (Exception e) {
            logger.error("使委派单失效异常: ", e);
            return Result.error(500, "操作失败");
        }
    }

    /**
     * 验证物流委派单
     */
    @ApiOperation("验证物流委派单")
    @GetMapping("/validate")
    public Result<Boolean> validateDelegate(
            @ApiParam("委派单编号") @RequestParam String voucherNo,
            HttpServletRequest request) {
        try {
            boolean isValid = logisticsService.validateDelegate(voucherNo);
            return Result.success(isValid);

        } catch (Exception e) {
            logger.error("验证委派单异常: ", e);
            return Result.error(500, "验证失败");
        }
    }
}
