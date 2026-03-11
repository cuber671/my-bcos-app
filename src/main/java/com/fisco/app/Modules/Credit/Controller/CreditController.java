package com.fisco.app.Modules.Credit.Controller;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fisco.app.Common.Annotation.RequireRole;
import com.fisco.app.Common.Utils.CurrentUser;
import com.fisco.app.Common.Utils.Result;
import com.fisco.app.Modules.Credit.Entity.CreditEvent;
import com.fisco.app.Modules.Credit.Service.CreditService;
import com.fisco.app.Modules.Credit.Service.CreditService.CreditPortrait;
import com.fisco.app.Modules.Credit.Service.CreditService.CreditScoreResult;
import com.fisco.app.Modules.Credit.Service.CreditService.LimitCheckResult;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * 信用管理 Controller
 *
 * 提供企业信用档案管理、信用事件上报、信用评分计算等 API
 *
 * 权限控制说明：
 * - 企业用户可查看自身信用画像、查询信用事件
 * - 管理员可设置授信额度、重算信用等级、查看所有企业信用
 * - 金融/风控模块通过内部调用触发信用事件上报
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Api(tags = "信用管理")
@RestController
@RequestMapping("/api/v1/credit")
public class CreditController {

    private static final Logger logger = LoggerFactory.getLogger(CreditController.class);

    @Autowired
    private CreditService creditService;

    // ==================== 信用画像查询 ====================

    /**
     * 获取企业信用画像
     *
     * 查看企业的信用分、等级、授信额度使用情况
     *
     * 权限：JWT认证，企业用户可查看自身，管理员可查看所有
     */
    @ApiOperation("获取企业信用画像")
    @GetMapping("/profile")
    @RequireRole(value = {"ADMIN"}, adminBypass = true)
    public Result<CreditPortraitResponse> getCreditProfile(HttpServletRequest request) {
        try {
            // 仅从JWT获取企业信息，防止越权
            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                return Result.error(401, "无法获取当前企业信息，请先登录");
            }

            CreditPortrait portrait = creditService.getCreditPortrait(entId);
            return Result.success(convertToPortraitResponse(portrait));

        } catch (Exception e) {
            logger.error("获取信用画像异常: ", e);
            return Result.error(500, "获取信用画像失败");
        }
    }

    /**
     * 获取当前企业信用画像
     *
     * 使用JWT自动获取当前登录用户所在企业的信用画像
     */
    @ApiOperation("获取当前企业信用画像")
    @GetMapping("/profile/me")
    public Result<CreditPortraitResponse> getMyCreditProfile(HttpServletRequest request) {
        try {
            Long entId = getCurrentEntId(request);
            if (entId == null) {
                return Result.error(401, "未登录或Token无效");
            }

            CreditPortrait portrait = creditService.getCreditPortrait(entId);
            return Result.success(convertToPortraitResponse(portrait));

        } catch (Exception e) {
            logger.error("获取当前企业信用画像异常", e);
            return Result.error(500, "获取信用画像异常: " + e.getMessage());
        }
    }

    /**
     * 获取信用评分
     * 权限：JWT认证，企业用户可查看自身，管理员可查看所有
     */
    @ApiOperation("获取信用评分")
    @GetMapping("/score")
    @RequireRole(value = {"ADMIN"}, adminBypass = true)
    public Result<CreditScoreResultResponse> getCreditScore(HttpServletRequest request) {
        try {
            // 仅从JWT获取企业信息，防止越权
            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                return Result.error(401, "无法获取当前企业信息，请先登录");
            }

            CreditScoreResult scoreResult = creditService.getCreditScore(entId);
            return Result.success(convertToScoreResponse(scoreResult));

        } catch (Exception e) {
            logger.error("获取信用评分异常: ", e);
            return Result.error(500, "获取信用评分失败");
        }
    }

    // ==================== 信用事件管理 ====================

    /**
     * 上报信用事件
     *
     * 金融模块发现逾期，或风控发现偏航时自动触发
     * 系统根据事件等级扣分
     * 权限：仅管理员可调用
     */
    @ApiOperation("上报信用事件")
    @PostMapping("/event/report")
    @RequireRole(value = {"ADMIN", "RISK"}, adminBypass = true)
    public Result<Map<String, Object>> reportCreditEvent(@RequestBody CreditEventReportRequest request) {
        try {
            // 参数校验
            if (request.getEntId() == null) {
                return Result.error(400, "企业ID不能为空");
            }
            if (request.getEventType() == null || request.getEventType().isEmpty()) {
                return Result.error(400, "事件类型不能为空");
            }
            if (request.getEventLevel() == null || request.getEventLevel().isEmpty()) {
                return Result.error(400, "事件等级不能为空");
            }

            Long eventId = creditService.reportCreditEvent(
                    request.getEntId(),
                    request.getEventType(),
                    request.getEventLevel(),
                    request.getEventDesc(),
                    request.getScoreChange(),
                    request.getRelatedModule(),
                    request.getRelatedId()
            );

            Map<String, Object> result = new HashMap<>();
            result.put("eventId", eventId);
            result.put("message", "信用事件上报成功");

            logger.info("信用事件上报成功: entId={}, eventId={}, type={}",
                    request.getEntId(), eventId, request.getEventType());

            return Result.success(result);

        } catch (Exception e) {
            logger.error("上报信用事件异常: entId={}", request.getEntId(), e);
            return Result.error(500, "上报信用事件异常: " + e.getMessage());
        }
    }

    /**
     * 物流偏航触发信用扣分
     * 当风控检测到物流路径严重偏移时，自动触发信用扣分
     * 权限：仅管理员或风控模块可调用
     */
    @ApiOperation("物流偏航触发信用扣分")
    @PostMapping("/event/logistics-deviation")
    @RequireRole(value = {"ADMIN", "RISK", "LOGISTICS"}, adminBypass = true)
    public Result<Map<String, Object>> reportLogisticsDeviation(
            @RequestBody LogisticsDeviationRequest request) {
        try {
            // 参数校验
            if (request.getEntId() == null) {
                return Result.error(400, "企业ID不能为空");
            }
            if (request.getLogisticsOrderId() == null || request.getLogisticsOrderId().isEmpty()) {
                return Result.error(400, "物流订单ID不能为空");
            }

            // 偏航级别对应的扣分数
            Integer scoreChange;
            String eventLevel;
            String eventDesc;

            if (request.getDeviationLevel() == null || request.getDeviationLevel() <= 1) {
                // 轻度偏航
                scoreChange = -10;
                eventLevel = "LOW";
                eventDesc = "物流路径轻度偏移，偏离预定路线";
            } else if (request.getDeviationLevel() <= 2) {
                // 中度偏航
                scoreChange = -15;
                eventLevel = "MEDIUM";
                eventDesc = "物流路径中度偏移，存在绕路嫌疑";
            } else {
                // 严重偏航
                scoreChange = -25;
                eventLevel = "HIGH";
                eventDesc = "物流路径严重偏移，可能存在异常";
            }

            // 添加自定义描述
            if (request.getDeviationDesc() != null && !request.getDeviationDesc().isEmpty()) {
                eventDesc = request.getDeviationDesc();
            }

            Long eventId = creditService.reportCreditEvent(
                    request.getEntId(),
                    CreditEvent.EVENT_TYPE_LOGISTICS_DEVIATION,
                    eventLevel,
                    eventDesc,
                    scoreChange,
                    "LOGISTICS",
                    request.getLogisticsOrderId()
            );

            Map<String, Object> result = new HashMap<>();
            result.put("eventId", eventId);
            result.put("scoreChange", scoreChange);
            result.put("eventLevel", eventLevel);
            result.put("message", "物流偏航扣分上报成功");

            logger.info("物流偏航触发信用扣分: entId={}, orderId={}, deviationLevel={}, scoreChange={}",
                    request.getEntId(), request.getLogisticsOrderId(), request.getDeviationLevel(), scoreChange);

            return Result.success(result);

        } catch (Exception e) {
            logger.error("物流偏航扣分异常: entId={}", request.getEntId(), e);
            return Result.error(500, "物流偏航扣分异常: " + e.getMessage());
        }
    }

    /**
     * 查询企业信用事件列表
     */
    @ApiOperation("查询企业信用事件列表")
    @GetMapping("/events")
    public Result<List<CreditEventResponse>> listCreditEvents(
            @ApiParam(value = "企业ID，不传则查询当前企业")
            @RequestParam(required = false) Long entId,
            @ApiParam(value = "事件类型")
            @RequestParam(required = false) String eventType,
            HttpServletRequest request) {
        try {
            // 如果未指定entId，从当前登录用户获取
            if (entId == null) {
                entId = getCurrentEntId(request);
            }
            if (entId == null) {
                return Result.error(401, "未登录或Token无效");
            }

            List<CreditEvent> events;
            if (eventType != null && !eventType.isEmpty()) {
                events = creditService.listCreditEventsByType(entId, eventType);
            } else {
                events = creditService.listCreditEvents(entId);
            }

            return Result.success(convertToEventList(events));

        } catch (Exception e) {
            logger.error("查询信用事件列表异常: ", e);
            return Result.error(500, "查询信用事件列表异常: " + e.getMessage());
        }
    }

    // ==================== 信用额度管理 ====================

    /**
     * 设置授信额度
     *
     * 管理员设置企业授信额度
     */
    @ApiOperation("设置授信额度")
    @PutMapping("/limit")
    @RequireRole(value = {"ADMIN"}, adminBypass = true)
    public Result<Map<String, Object>> setCreditLimit(
            @ApiParam(value = "授信额度", required = true)
            @RequestParam BigDecimal availableLimit) {
        try {
            // 仅从JWT获取企业信息，防止越权
            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                return Result.error(401, "无法获取当前企业信息，请先登录");
            }
            if (availableLimit == null || availableLimit.compareTo(BigDecimal.ZERO) <= 0) {
                return Result.error(400, "授信额度必须大于0");
            }

            boolean success = creditService.setCreditLimit(entId, availableLimit);

            Map<String, Object> result = new HashMap<>();
            result.put("entId", entId);
            result.put("availableLimit", availableLimit);
            result.put("success", success);

            logger.info("设置授信额度: entId={}, limit={}", entId, availableLimit);
            return Result.success(result);

        } catch (IllegalArgumentException e) {
            logger.warn("设置授信额度参数异常: ", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("设置授信额度异常: ", e);
            return Result.error(500, "设置授信额度失败，请稍后重试");
        }
    }

    /**
     * 额度校验
     *
     * 物流委派单生成前调用，检查额度是否充足
     */
    @ApiOperation("额度校验")
    @PostMapping("/limit/check")
    public Result<LimitCheckResultResponse> checkCreditLimit(
            @RequestBody CreditLimitCheckRequest request,
            HttpServletRequest httpRequest) {
        try {
            // 获取企业ID
            Long entId = request.getEntId();
            if (entId == null) {
                entId = getCurrentEntId(httpRequest);
            }
            if (entId == null) {
                return Result.error(401, "未登录或Token无效");
            }
            if (request.getRequiredAmount() == null || request.getRequiredAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return Result.error(400, "需求金额必须大于0");
            }

            LimitCheckResult checkResult = creditService.checkCreditLimit(entId, request.getRequiredAmount());
            return Result.success(convertToLimitCheckResponse(checkResult));

        } catch (Exception e) {
            logger.error("额度校验异常", e);
            return Result.error(500, "额度校验异常: " + e.getMessage());
        }
    }

    /**
     * 额度实时锁死
     *
     * 当企业已用额度超过授信总额时，自动拦截物流委派单的生成
     * 权限：仅管理员可调用
     */
    @ApiOperation("额度实时锁死")
    @PostMapping("/limit/lock")
    @RequireRole(value = {"ADMIN"}, adminBypass = true)
    public Result<Map<String, Object>> lockCreditLimit(HttpServletRequest request) {
        try {
            // 仅从JWT获取企业信息，防止越权
            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                return Result.error(401, "无法获取当前企业信息，请先登录");
            }

            boolean success = creditService.lockCreditLimit(entId);

            Map<String, Object> result = new HashMap<>();
            result.put("entId", entId);
            result.put("success", success);
            result.put("message", success ? "额度已锁死" : "锁死失败");

            logger.warn("额度实时锁死: entId={}, success={}", entId, success);
            return Result.success(result);

        } catch (Exception e) {
            logger.error("额度锁死异常: ", e);
            return Result.error(500, "额度锁死异常: " + e.getMessage());
        }
    }

    /**
     * 获取可用信用额度
     * 权限：JWT认证，企业用户可查看自身，管理员可查看所有
     */
    @ApiOperation("获取可用信用额度")
    @GetMapping("/limit/available")
    @RequireRole(value = {"ADMIN"}, adminBypass = true)
    public Result<Map<String, Object>> getAvailableCreditLimit(HttpServletRequest request) {
        try {
            // 仅从JWT获取企业信息，防止越权
            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                return Result.error(401, "无法获取当前企业信息，请先登录");
            }

            BigDecimal available = creditService.getAvailableCreditLimit(entId);

            Map<String, Object> result = new HashMap<>();
            result.put("entId", entId);
            result.put("availableLimit", available);

            return Result.success(result);

        } catch (Exception e) {
            logger.error("获取可用额度异常: ", e);
            return Result.error(500, "获取可用额度异常: " + e.getMessage());
        }
    }

    // ==================== 信用评分计算 ====================

    /**
     * 信用等级重算
     *
     * 系统每月根据企业的交易频次、履约率自动更新分数和等级
     * 管理员也可手动触发
     */
    @ApiOperation("信用等级重算")
    @PatchMapping("/reevaluate")
    @RequireRole(value = {"ADMIN"}, adminBypass = true)
    public Result<Map<String, Object>> recalculateCreditLevel(HttpServletRequest request) {
        try {
            // 仅从JWT获取企业信息，防止越权
            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                return Result.error(401, "无法获取当前企业信息，请先登录");
            }

            String newLevel = creditService.recalculateCreditLevel(entId);
            CreditScoreResult scoreResult = creditService.getCreditScore(entId);

            Map<String, Object> result = new HashMap<>();
            result.put("entId", entId);
            result.put("creditScore", scoreResult.getCreditScore());
            result.put("creditLevel", newLevel);
            result.put("message", "信用等级重算完成");

            logger.info("信用等级重算: entId={}, score={}, level={}",
                    entId, scoreResult.getCreditScore(), newLevel);

            return Result.success(result);

        } catch (Exception e) {
            logger.error("信用等级重算异常: ", e);
            return Result.error(500, "信用等级重算异常: " + e.getMessage());
        }
    }

    /**
     * 批量信用等级重算
     */
    @ApiOperation("批量信用等级重算")
    @PatchMapping("/reevaluate/batch")
    @RequireRole(value = {"ADMIN"}, adminBypass = true)
    public Result<Map<String, Object>> batchRecalculateCreditLevel(
            @RequestBody BatchRecalculateRequest request) {
        try {
            if (request.getEntIds() == null || request.getEntIds().isEmpty()) {
                return Result.error(400, "企业ID列表不能为空");
            }

            int successCount = 0;
            int failCount = 0;

            for (Long entId : request.getEntIds()) {
                try {
                    creditService.recalculateCreditLevel(entId);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    logger.error("批量重算失败: ", e);
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("total", request.getEntIds().size());
            result.put("success", successCount);
            result.put("failed", failCount);
            result.put("message", "批量重算完成");

            logger.info("批量信用等级重算: total={}, success={}, fail={}",
                    request.getEntIds().size(), successCount, failCount);

            return Result.success(result);

        } catch (Exception e) {
            logger.error("批量信用等级重算异常", e);
            return Result.error(500, "批量重算异常: " + e.getMessage());
        }
    }

    // ==================== 信用黑名单 ====================

    /**
     * 检查是否触发信用黑名单
     */
    @ApiOperation("检查信用黑名单")
    @GetMapping("/blacklist/check")
    @RequireRole(value = {"ADMIN"}, adminBypass = true)
    public Result<Map<String, Object>> checkBlacklist(HttpServletRequest request) {
        try {
            // 仅从JWT获取企业信息，防止越权
            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                return Result.error(401, "无法获取当前企业信息，请先登录");
            }

            boolean isBlacklisted = creditService.checkBlacklist(entId);

            Map<String, Object> result = new HashMap<>();
            result.put("entId", entId);
            result.put("isBlacklisted", isBlacklisted);

            return Result.success(result);

        } catch (Exception e) {
            logger.error("检查黑名单异常: ", e);
            return Result.error(500, "检查黑名单异常: " + e.getMessage());
        }
    }

    /**
     * 触发信用黑名单
     */
    @ApiOperation("触发信用黑名单")
    @PostMapping("/blacklist/trigger")
    @RequireRole(value = {"ADMIN"}, adminBypass = true)
    public Result<Map<String, Object>> triggerBlacklist(HttpServletRequest request) {
        try {
            // 仅从JWT获取企业信息，防止越权
            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                return Result.error(401, "无法获取当前企业信息，请先登录");
            }

            boolean success = creditService.triggerBlacklist(entId);

            Map<String, Object> result = new HashMap<>();
            result.put("entId", entId);
            result.put("success", success);
            result.put("message", success ? "已触发信用黑名单" : "触发失败");

            logger.warn("触发信用黑名单: entId={}, success={}", entId, success);

            return Result.success(result);

        } catch (Exception e) {
            logger.error("触发黑名单异常: ", e);
            return Result.error(500, "触发黑名单异常: " + e.getMessage());
        }
    }

    /**
     * 移除信用黑名单
     */
    @ApiOperation("移除信用黑名单")
    @DeleteMapping("/blacklist/remove")
    @RequireRole(value = {"ADMIN"}, adminBypass = true)
    public Result<Map<String, Object>> removeBlacklist(HttpServletRequest request) {
        try {
            // 仅从JWT获取企业信息，防止越权
            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                return Result.error(401, "无法获取当前企业信息，请先登录");
            }

            boolean success = creditService.removeBlacklist(entId);

            Map<String, Object> result = new HashMap<>();
            result.put("entId", entId);
            result.put("success", success);
            result.put("message", success ? "已移除信用黑名单" : "移除失败，信用分仍低于阈值");

            logger.info("移除信用黑名单: entId={}, success={}", entId, success);

            return Result.success(result);

        } catch (Exception e) {
            logger.error("移除黑名单异常: ", e);
            return Result.error(500, "移除黑名单异常: " + e.getMessage());
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 从请求中获取当前登录用户的企业ID
     */
    private Long getCurrentEntId(HttpServletRequest request) {
        Object entIdAttr = request.getAttribute("ent_id");
        if (entIdAttr != null) {
            try {
                return Long.parseLong(entIdAttr.toString());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 转换为信用画像响应
     */
    private CreditPortraitResponse convertToPortraitResponse(CreditPortrait portrait) {
        if (portrait == null) {
            return null;
        }
        CreditPortraitResponse response = new CreditPortraitResponse();
        response.setEntId(portrait.getEntId());
        response.setEnterpriseName(portrait.getEnterpriseName());
        response.setCreditScore(portrait.getCreditScore());
        response.setCreditLevel(portrait.getCreditLevel());
        response.setAvailableLimit(portrait.getAvailableLimit());
        response.setUsedLimit(portrait.getUsedLimit());
        response.setAvailableBalance(portrait.getAvailableBalance());
        response.setOverdueCount(portrait.getOverdueCount());
        response.setLastEvalTime(portrait.getLastEvalTime());
        response.setIsBlacklisted(portrait.getIsBlacklisted());
        return response;
    }

    /**
     * 转换为信用评分响应
     */
    private CreditScoreResultResponse convertToScoreResponse(CreditScoreResult scoreResult) {
        if (scoreResult == null) {
            return null;
        }
        CreditScoreResultResponse response = new CreditScoreResultResponse();
        response.setEntId(scoreResult.getEntId());
        response.setCreditScore(scoreResult.getCreditScore());
        response.setCreditLevel(scoreResult.getCreditLevel());
        response.setLastEvalTime(scoreResult.getLastEvalTime());
        response.setAvailableLimit(scoreResult.getAvailableLimit());
        response.setUsedLimit(scoreResult.getUsedLimit());
        response.setOverdueCount(scoreResult.getOverdueCount());
        return response;
    }

    /**
     * 转换为额度校验响应
     */
    private LimitCheckResultResponse convertToLimitCheckResponse(LimitCheckResult checkResult) {
        if (checkResult == null) {
            return null;
        }
        LimitCheckResultResponse response = new LimitCheckResultResponse();
        response.setPassed(checkResult.isPassed());
        response.setMessage(checkResult.getMessage());
        response.setAvailableLimit(checkResult.getAvailableLimit());
        response.setRequiredAmount(checkResult.getRequiredAmount());
        response.setEntId(checkResult.getEntId());
        return response;
    }

    /**
     * 转换为事件列表响应
     */
    private List<CreditEventResponse> convertToEventList(List<CreditEvent> events) {
        if (events == null) {
            return null;
        }
        return events.stream().map(this::convertToEventResponse).collect(Collectors.toList());
    }

    /**
     * 转换为事件响应
     */
    private CreditEventResponse convertToEventResponse(CreditEvent event) {
        if (event == null) {
            return null;
        }
        CreditEventResponse response = new CreditEventResponse();
        response.setId(event.getId());
        response.setEntId(event.getEntId());
        response.setEventType(event.getEventType());
        response.setEventLevel(event.getEventLevel());
        response.setEventDesc(event.getEventDesc());
        response.setScoreChange(event.getScoreChange());
        response.setRelatedModule(event.getRelatedModule());
        response.setRelatedId(event.getRelatedId());
        response.setChainTxHash(event.getChainTxHash());
        response.setReportTime(event.getReportTime() != null ? event.getReportTime().toString() : null);
        return response;
    }

    // ==================== 请求/响应类 ====================

    /**
     * 企业注册请求
     */
    public static class EnterpriseRegisterRequest {
        private String username;
        private String password;
        private String payPassword;
        private String enterpriseName;
        private String orgCode;
        private Integer entRole;
        private String localAddress;
        private String contactPhone;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getPayPassword() { return payPassword; }
        public void setPayPassword(String payPassword) { this.payPassword = payPassword; }
        public String getEnterpriseName() { return enterpriseName; }
        public void setEnterpriseName(String enterpriseName) { this.enterpriseName = enterpriseName; }
        public String getOrgCode() { return orgCode; }
        public void setOrgCode(String orgCode) { this.orgCode = orgCode; }
        public Integer getEntRole() { return entRole; }
        public void setEntRole(Integer entRole) { this.entRole = entRole; }
        public String getLocalAddress() { return localAddress; }
        public void setLocalAddress(String localAddress) { this.localAddress = localAddress; }
        public String getContactPhone() { return contactPhone; }
        public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    }

    /**
     * 信用事件上报请求
     */
    public static class CreditEventReportRequest {
        private Long entId;
        private String eventType;
        private String eventLevel;
        private String eventDesc;
        private Integer scoreChange;
        private String relatedModule;
        private String relatedId;

        public Long getEntId() { return entId; }
        public void setEntId(Long entId) { this.entId = entId; }
        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }
        public String getEventLevel() { return eventLevel; }
        public void setEventLevel(String eventLevel) { this.eventLevel = eventLevel; }
        public String getEventDesc() { return eventDesc; }
        public void setEventDesc(String eventDesc) { this.eventDesc = eventDesc; }
        public Integer getScoreChange() { return scoreChange; }
        public void setScoreChange(Integer scoreChange) { this.scoreChange = scoreChange; }
        public String getRelatedModule() { return relatedModule; }
        public void setRelatedModule(String relatedModule) { this.relatedModule = relatedModule; }
        public String getRelatedId() { return relatedId; }
        public void setRelatedId(String relatedId) { this.relatedId = relatedId; }
    }

    /**
     * 物流偏航扣分请求
     */
    public static class LogisticsDeviationRequest {
        private Long entId;
        private String logisticsOrderId;
        private Integer deviationLevel;  // 偏航级别: 1=轻度, 2=中度, 3=严重
        private String deviationDesc;    // 偏航描述

        public Long getEntId() { return entId; }
        public void setEntId(Long entId) { this.entId = entId; }
        public String getLogisticsOrderId() { return logisticsOrderId; }
        public void setLogisticsOrderId(String logisticsOrderId) { this.logisticsOrderId = logisticsOrderId; }
        public Integer getDeviationLevel() { return deviationLevel; }
        public void setDeviationLevel(Integer deviationLevel) { this.deviationLevel = deviationLevel; }
        public String getDeviationDesc() { return deviationDesc; }
        public void setDeviationDesc(String deviationDesc) { this.deviationDesc = deviationDesc; }
    }

    /**
     * 额度校验请求
     */
    public static class CreditLimitCheckRequest {
        private Long entId;
        private BigDecimal requiredAmount;

        public Long getEntId() { return entId; }
        public void setEntId(Long entId) { this.entId = entId; }
        public BigDecimal getRequiredAmount() { return requiredAmount; }
        public void setRequiredAmount(BigDecimal requiredAmount) { this.requiredAmount = requiredAmount; }
    }

    /**
     * 批量重算请求
     */
    public static class BatchRecalculateRequest {
        private List<Long> entIds;

        public List<Long> getEntIds() { return entIds; }
        public void setEntIds(List<Long> entIds) { this.entIds = entIds; }
    }

    // ==================== 响应类 ====================

    /**
     * 信用画像响应
     */
    public static class CreditPortraitResponse {
        private Long entId;
        private String enterpriseName;
        private Integer creditScore;
        private String creditLevel;
        private java.math.BigDecimal availableLimit;
        private java.math.BigDecimal usedLimit;
        private java.math.BigDecimal availableBalance;
        private Integer overdueCount;
        private String lastEvalTime;
        private Boolean isBlacklisted;

        public Long getEntId() { return entId; }
        public void setEntId(Long entId) { this.entId = entId; }
        public String getEnterpriseName() { return enterpriseName; }
        public void setEnterpriseName(String enterpriseName) { this.enterpriseName = enterpriseName; }
        public Integer getCreditScore() { return creditScore; }
        public void setCreditScore(Integer creditScore) { this.creditScore = creditScore; }
        public String getCreditLevel() { return creditLevel; }
        public void setCreditLevel(String creditLevel) { this.creditLevel = creditLevel; }
        public java.math.BigDecimal getAvailableLimit() { return availableLimit; }
        public void setAvailableLimit(java.math.BigDecimal availableLimit) { this.availableLimit = availableLimit; }
        public java.math.BigDecimal getUsedLimit() { return usedLimit; }
        public void setUsedLimit(java.math.BigDecimal usedLimit) { this.usedLimit = usedLimit; }
        public java.math.BigDecimal getAvailableBalance() { return availableBalance; }
        public void setAvailableBalance(java.math.BigDecimal availableBalance) { this.availableBalance = availableBalance; }
        public Integer getOverdueCount() { return overdueCount; }
        public void setOverdueCount(Integer overdueCount) { this.overdueCount = overdueCount; }
        public String getLastEvalTime() { return lastEvalTime; }
        public void setLastEvalTime(String lastEvalTime) { this.lastEvalTime = lastEvalTime; }
        public Boolean getIsBlacklisted() { return isBlacklisted; }
        public void setIsBlacklisted(Boolean isBlacklisted) { this.isBlacklisted = isBlacklisted; }
    }

    /**
     * 信用评分响应
     */
    public static class CreditScoreResultResponse {
        private Long entId;
        private Integer creditScore;
        private String creditLevel;
        private String lastEvalTime;
        private java.math.BigDecimal availableLimit;
        private java.math.BigDecimal usedLimit;
        private Integer overdueCount;

        public Long getEntId() { return entId; }
        public void setEntId(Long entId) { this.entId = entId; }
        public Integer getCreditScore() { return creditScore; }
        public void setCreditScore(Integer creditScore) { this.creditScore = creditScore; }
        public String getCreditLevel() { return creditLevel; }
        public void setCreditLevel(String creditLevel) { this.creditLevel = creditLevel; }
        public String getLastEvalTime() { return lastEvalTime; }
        public void setLastEvalTime(String lastEvalTime) { this.lastEvalTime = lastEvalTime; }
        public java.math.BigDecimal getAvailableLimit() { return availableLimit; }
        public void setAvailableLimit(java.math.BigDecimal availableLimit) { this.availableLimit = availableLimit; }
        public java.math.BigDecimal getUsedLimit() { return usedLimit; }
        public void setUsedLimit(java.math.BigDecimal usedLimit) { this.usedLimit = usedLimit; }
        public Integer getOverdueCount() { return overdueCount; }
        public void setOverdueCount(Integer overdueCount) { this.overdueCount = overdueCount; }
    }

    /**
     * 额度校验响应
     */
    public static class LimitCheckResultResponse {
        private boolean passed;
        private String message;
        private java.math.BigDecimal availableLimit;
        private java.math.BigDecimal requiredAmount;
        private Long entId;

        public boolean isPassed() { return passed; }
        public void setPassed(boolean passed) { this.passed = passed; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public java.math.BigDecimal getAvailableLimit() { return availableLimit; }
        public void setAvailableLimit(java.math.BigDecimal availableLimit) { this.availableLimit = availableLimit; }
        public java.math.BigDecimal getRequiredAmount() { return requiredAmount; }
        public void setRequiredAmount(java.math.BigDecimal requiredAmount) { this.requiredAmount = requiredAmount; }
        public Long getEntId() { return entId; }
        public void setEntId(Long entId) { this.entId = entId; }
    }

    /**
     * 信用事件响应
     */
    public static class CreditEventResponse {
        private Long id;
        private Long entId;
        private String eventType;
        private String eventLevel;
        private String eventDesc;
        private Integer scoreChange;
        private String relatedModule;
        private String relatedId;
        private String chainTxHash;
        private String reportTime;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getEntId() { return entId; }
        public void setEntId(Long entId) { this.entId = entId; }
        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }
        public String getEventLevel() { return eventLevel; }
        public void setEventLevel(String eventLevel) { this.eventLevel = eventLevel; }
        public String getEventDesc() { return eventDesc; }
        public void setEventDesc(String eventDesc) { this.eventDesc = eventDesc; }
        public Integer getScoreChange() { return scoreChange; }
        public void setScoreChange(Integer scoreChange) { this.scoreChange = scoreChange; }
        public String getRelatedModule() { return relatedModule; }
        public void setRelatedModule(String relatedModule) { this.relatedModule = relatedModule; }
        public String getRelatedId() { return relatedId; }
        public void setRelatedId(String relatedId) { this.relatedId = relatedId; }
        public String getChainTxHash() { return chainTxHash; }
        public void setChainTxHash(String chainTxHash) { this.chainTxHash = chainTxHash; }
        public String getReportTime() { return reportTime; }
        public void setReportTime(String reportTime) { this.reportTime = reportTime; }
    }

    // ==================== 企业信息响应类 ====================

    public static class EnterpriseInfoResponse {
        private Long entId;
        private String enterpriseName;
        private String orgCode;
        private Integer status;
        private String blockchainAddress;

        public Long getEntId() { return entId; }
        public void setEntId(Long entId) { this.entId = entId; }
        public String getEnterpriseName() { return enterpriseName; }
        public void setEnterpriseName(String enterpriseName) { this.enterpriseName = enterpriseName; }
        public String getOrgCode() { return orgCode; }
        public void setOrgCode(String orgCode) { this.orgCode = orgCode; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
        public String getBlockchainAddress() { return blockchainAddress; }
        public void setBlockchainAddress(String blockchainAddress) { this.blockchainAddress = blockchainAddress; }
    }

    public static class EnterpriseDetailResponse {
        private Long entId;
        private String enterpriseName;
        private String orgCode;
        private String localAddress;
        private String contactPhone;
        private Integer entRole;
        private Integer status;
        private String blockchainAddress;
        private Integer userCount;

        public Long getEntId() { return entId; }
        public void setEntId(Long entId) { this.entId = entId; }
        public String getEnterpriseName() { return enterpriseName; }
        public void setEnterpriseName(String enterpriseName) { this.enterpriseName = enterpriseName; }
        public String getOrgCode() { return orgCode; }
        public void setOrgCode(String orgCode) { this.orgCode = orgCode; }
        public String getLocalAddress() { return localAddress; }
        public void setLocalAddress(String localAddress) { this.localAddress = localAddress; }
        public String getContactPhone() { return contactPhone; }
        public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
        public Integer getEntRole() { return entRole; }
        public void setEntRole(Integer entRole) { this.entRole = entRole; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
        public String getBlockchainAddress() { return blockchainAddress; }
        public void setBlockchainAddress(String blockchainAddress) { this.blockchainAddress = blockchainAddress; }
        public Integer getUserCount() { return userCount; }
        public void setUserCount(Integer userCount) { this.userCount = userCount; }
    }

    public static class InvitationCodeResponse {
        private String code;
        private Integer maxUses;
        private Integer usedCount;
        private String expireTime;
        private String remark;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public Integer getMaxUses() { return maxUses; }
        public void setMaxUses(Integer maxUses) { this.maxUses = maxUses; }
        public Integer getUsedCount() { return usedCount; }
        public void setUsedCount(Integer usedCount) { this.usedCount = usedCount; }
        public String getExpireTime() { return expireTime; }
        public void setExpireTime(String expireTime) { this.expireTime = expireTime; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
    }
}
