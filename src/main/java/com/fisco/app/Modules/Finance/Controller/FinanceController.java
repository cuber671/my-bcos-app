package com.fisco.app.Modules.Finance.Controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fisco.app.Common.Annotation.RequireRole;
import com.fisco.app.Common.Utils.CurrentUser;
import com.fisco.app.Common.Utils.Result;
import com.fisco.app.Modules.Finance.Entity.Receivable;
import com.fisco.app.Modules.Finance.Entity.RepaymentRecord;
import com.fisco.app.Modules.Finance.Service.FinanceService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * 金融管理 Controller
 *
 * 提供应收款生成、确认、调整、还款等 API
 *
 * 权限控制说明：
 * - 应收款生成：仅管理员可调用（或由系统自动触发，如物流送达）
 * - 应收款确认：债务人企业确认，需要企业用户权限
 * - 应收款调整：财务人员，需要FINANCE角色
 * - 现金还款/仓单抵债：财务人员，需要FINANCE角色
 * - 查询类接口：登录用户可查看自身相关的应收款
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Api(tags = "金融管理")
@RestController
@RequestMapping("/api/v1/finance")
public class FinanceController {

    private static final Logger logger = LoggerFactory.getLogger(FinanceController.class);

    @Autowired
    private FinanceService financeService;

    // ==================== 应收款生成 ====================

    /**
     * 生成应收款
     *
     * 根据物流单信息生成应收款，初始金额 = 实际运输数量 × 单价
     * 状态设为"待确认"
     *
     * 权限：仅管理员可调用
     */
    @ApiOperation("生成应收款")
    @PostMapping("/receivable/generate")
    @RequireRole(value = {"ADMIN"}, adminBypass = true)
    public Result<ReceivableResponse> generateReceivable(@RequestBody GenerateReceivableRequest request) {
        try {
            // 参数校验
            if (request.getVoucherId() == null) {
                return Result.error(400, "物流单ID不能为空");
            }
            if (request.getUnitPrice() == null || request.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                return Result.error(400, "单价必须大于0");
            }

            // 调用Service生成应收款
            Receivable receivable = financeService.generateReceivable(
                    request.getVoucherId(),
                    request.getUnitPrice()
            );

            logger.info("生成应收款成功: receivableNo={}, voucherId={}",
                    receivable.getReceivableNo(), request.getVoucherId());

            return Result.success(convertToReceivableResponse(receivable));

        } catch (IllegalArgumentException e) {
            logger.warn("生成应收款参数错误: {}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (IllegalStateException e) {
            logger.warn("生成应收款状态错误: {}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("生成应收款异常: ", e);
            return Result.error(500, "操作失败，请稍后重试");
        }
    }

    // ==================== 应收款确认 ====================

    /**
     * 确认应收款
     *
     * 债务人数字签名确认，确认后账单正式生效
     * 状态从"待确认"变为"生效中"
     *
     * 权限：债务人企业（当前登录用户的所属企业）
     */
    @ApiOperation("确认应收款")
    @PostMapping("/receivable/confirm")
    public Result<ReceivableResponse> confirmReceivable(@RequestBody ConfirmReceivableRequest request) {
        try {
            // 参数校验
            if (request.getReceivableId() == null) {
                return Result.error(400, "应收款ID不能为空");
            }

            // 获取当前登录用户信息（债务人企业）
            Long currentEntId = CurrentUser.getEntId();
            if (currentEntId == null) {
                return Result.error(401, "未登录或Token无效");
            }

            // 查询应收款，验证是否为当前企业的债务
            Receivable receivable = financeService.getReceivableById(request.getReceivableId());
            if (receivable == null) {
                return Result.error(404, "应收款不存在");
            }

            // 验证当前企业是否为债务人
            if (!currentEntId.equals(receivable.getDebtorEntId())) {
                return Result.error(403, "只有债务人才能确认此应收款");
            }

            // 调用Service确认应收款
            Receivable confirmed = financeService.confirmReceivable(
                    request.getReceivableId(),
                    request.getSignature()
            );

            logger.info("确认应收款成功: receivableId={}, receivableNo={}",
                    request.getReceivableId(), confirmed.getReceivableNo());

            return Result.success(convertToReceivableResponse(confirmed));

        } catch (IllegalArgumentException e) {
            logger.warn("确认应收款参数错误: {}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (IllegalStateException e) {
            logger.warn("确认应收款状态错误: {}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("确认应收款异常: ", e);
            return Result.error(500, "操作失败，请稍后重试");
        }
    }

    // ==================== 应收款调整 ====================

    /**
     * 调整应收款金额
     *
     * 处理损耗或拆分：
     * - adjustType=1: 物流损耗扣减
     * - adjustType=2: 仓单拆分同步
     *
     * 权限：财务人员（FINANCE角色）
     */
    @ApiOperation("调整应收款金额")
    @PatchMapping("/receivable/adjust")
    @RequireRole(value = {"ADMIN", "FINANCE"}, adminBypass = true)
    public Result<ReceivableResponse> adjustReceivable(@RequestBody AdjustReceivableRequest request) {
        try {
            // 参数校验
            if (request.getReceivableId() == null) {
                return Result.error(400, "应收款ID不能为空");
            }
            if (request.getAdjustType() == null || (request.getAdjustType() != 1 && request.getAdjustType() != 2)) {
                return Result.error(400, "调整类型必须是1(物流损耗扣减)或2(仓单拆分同步)");
            }
            if (request.getAmount() == null) {
                return Result.error(400, "调整金额不能为空");
            }

            // 获取当前登录用户信息
            Long currentEntId = CurrentUser.getEntId();
            if (currentEntId == null) {
                return Result.error(401, "未登录或Token无效");
            }

            // 查询应收款，验证权限
            Receivable receivable = financeService.getReceivableById(request.getReceivableId());
            if (receivable == null) {
                return Result.error(404, "应收款不存在");
            }

            // 验证当前企业是否为债权人或债务人（双方均可发起调整）
            boolean isParty = currentEntId.equals(receivable.getCreditorEntId())
                    || currentEntId.equals(receivable.getDebtorEntId());
            if (!CurrentUser.isAdmin() && !isParty) {
                return Result.error(403, "无权调整此应收款");
            }

            // 调用Service调整应收款
            Receivable adjusted = financeService.adjustReceivable(
                    request.getReceivableId(),
                    request.getAdjustType(),
                    request.getAmount()
            );

            logger.info("调整应收款成功: receivableId={}, adjustType={}, amount={}",
                    request.getReceivableId(), request.getAdjustType(), request.getAmount());

            return Result.success(convertToReceivableResponse(adjusted));

        } catch (IllegalArgumentException e) {
            logger.warn("调整应收款参数错误: {}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (IllegalStateException e) {
            logger.warn("调整应收款状态错误: {}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("调整应收款异常: ", e);
            return Result.error(500, "操作失败，请稍后重试");
        }
    }

    // ==================== 现金还款 ====================

    /**
     * 现金还款
     *
     * 记录本次还款额，增加 collected_amount
     * 校验还款金额 ≤ 待还余额
     *
     * 权限：财务人员（FINANCE角色）
     */
    @ApiOperation("现金还款")
    @PostMapping("/repayment/cash")
    @RequireRole(value = {"ADMIN", "FINANCE"}, adminBypass = true)
    public Result<RepaymentRecordResponse> cashRepayment(@RequestBody CashRepaymentRequest request) {
        try {
            // 参数校验
            if (request.getReceivableId() == null) {
                return Result.error(400, "应收款ID不能为空");
            }
            if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return Result.error(400, "还款金额必须大于0");
            }

            // 获取当前登录用户信息
            Long currentEntId = CurrentUser.getEntId();
            if (currentEntId == null) {
                return Result.error(401, "未登录或Token无效");
            }

            // 查询应收款，验证是否为债务人
            Receivable receivable = financeService.getReceivableById(request.getReceivableId());
            if (receivable == null) {
                return Result.error(404, "应收款不存在");
            }

            // 验证当前企业是否为债务人
            if (!currentEntId.equals(receivable.getDebtorEntId())) {
                return Result.error(403, "只有债务人才能还款");
            }

            // 调用Service执行现金还款
            RepaymentRecord record = financeService.cashRepayment(
                    request.getReceivableId(),
                    request.getAmount(),
                    request.getPaymentVoucher()
            );

            logger.info("现金还款成功: receivableId={}, amount={}, repaymentNo={}",
                    request.getReceivableId(), request.getAmount(), record.getRepaymentNo());

            return Result.success(convertToRepaymentRecordResponse(record));

        } catch (IllegalArgumentException e) {
            logger.warn("现金还款参数错误: {}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (IllegalStateException e) {
            logger.warn("现金还款状态错误: {}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("现金还款异常: ", e);
            return Result.error(500, "操作失败，请稍后重试");
        }
    }

    // ==================== 仓单抵债 ====================

    /**
     * 仓单抵债核销
     *
     * 验证仓单所有权，确认后仓单 owner_id 变更为债权人
     * 同时按抵债价值冲减账单余额
     *
     * 权限：财务人员（FINANCE角色）
     */
    @ApiOperation("仓单抵债核销")
    @PostMapping("/repayment/offset")
    @RequireRole(value = {"ADMIN", "FINANCE"}, adminBypass = true)
    public Result<RepaymentRecordResponse> offsetWithCollateral(@RequestBody OffsetWithCollateralRequest request) {
        try {
            // 参数校验
            if (request.getReceivableId() == null) {
                return Result.error(400, "应收款ID不能为空");
            }
            if (request.getReceiptId() == null) {
                return Result.error(400, "仓单ID不能为空");
            }
            if (request.getOffsetPrice() == null || request.getOffsetPrice().compareTo(BigDecimal.ZERO) <= 0) {
                return Result.error(400, "抵债价格必须大于0");
            }

            // 获取当前登录用户信息
            Long currentEntId = CurrentUser.getEntId();
            if (currentEntId == null) {
                return Result.error(401, "未登录或Token无效");
            }

            // 查询应收款，验证是否为债务人
            Receivable receivable = financeService.getReceivableById(request.getReceivableId());
            if (receivable == null) {
                return Result.error(404, "应收款不存在");
            }

            // 验证当前企业是否为债务人
            if (!currentEntId.equals(receivable.getDebtorEntId())) {
                return Result.error(403, "只有债务人才能执行抵债操作");
            }

            // 调用Service执行仓单抵债
            RepaymentRecord record = financeService.offsetWithCollateral(
                    request.getReceivableId(),
                    request.getReceiptId(),
                    request.getOffsetPrice(),
                    request.getSignatureHash()
            );

            logger.info("仓单抵债成功: receivableId={}, receiptId={}, offsetPrice={}, repaymentNo={}",
                    request.getReceivableId(), request.getReceiptId(),
                    request.getOffsetPrice(), record.getRepaymentNo());

            return Result.success(convertToRepaymentRecordResponse(record));

        } catch (IllegalArgumentException e) {
            logger.warn("仓单抵债参数错误: {}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (IllegalStateException e) {
            logger.warn("仓单抵债状态错误: {}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("仓单抵债异常: ", e);
            return Result.error(500, "操作失败，请稍后重试");
        }
    }

    // ==================== 应收款融资 ====================

    /**
     * 应收款融资
     *
     * 债权人用应收款向金融机构申请融资
     * 融资后应收款状态变为"已融资"
     *
     * 权限：债权人企业（FINANCE角色）
     */
    @ApiOperation("应收款融资")
    @PostMapping("/receivable/finance")
    @RequireRole(value = {"ADMIN", "FINANCE"}, adminBypass = true)
    public Result<ReceivableResponse> financeReceivable(@RequestBody FinanceReceivableRequest request) {
        try {
            // 参数校验
            if (request.getReceivableId() == null) {
                return Result.error(400, "应收款ID不能为空");
            }
            if (request.getFinanceAmount() == null || request.getFinanceAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return Result.error(400, "融资金额必须大于0");
            }
            if (request.getFinanceEntId() == null) {
                return Result.error(400, "金融机构ID不能为空");
            }

            // 获取当前登录用户信息（债权人）
            Long currentEntId = CurrentUser.getEntId();
            if (currentEntId == null) {
                return Result.error(401, "未登录或Token无效");
            }

            // 查询应收款，验证是否为当前企业的债权
            Receivable receivable = financeService.getReceivableById(request.getReceivableId());
            if (receivable == null) {
                return Result.error(404, "应收款不存在");
            }

            // 验证当前企业是否为债权人
            if (!currentEntId.equals(receivable.getCreditorEntId())) {
                return Result.error(403, "只有债权人才能对此应收款进行融资");
            }

            // 调用Service执行融资
            Receivable financed = financeService.financeReceivable(
                    request.getReceivableId(),
                    request.getFinanceAmount(),
                    request.getFinanceEntId()
            );

            logger.info("应收款融资成功: receivableId={}, financeAmount={}, financeEntId={}",
                    request.getReceivableId(), request.getFinanceAmount(), request.getFinanceEntId());

            return Result.success(convertToReceivableResponse(financed));

        } catch (IllegalArgumentException e) {
            logger.warn("应收款融资参数错误: {}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (IllegalStateException e) {
            logger.warn("应收款融资状态错误: {}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("应收款融资异常: ", e);
            return Result.error(500, "操作失败，请稍后重试");
        }
    }

    /**
     * 应收款结算
     *
     * 融资还款后结算应收款
     * 结算后应收款状态变为"已结清"
     *
     * 权限：债权人企业（FINANCE角色）
     */
    @ApiOperation("应收款结算")
    @PostMapping("/receivable/settle")
    @RequireRole(value = {"ADMIN", "FINANCE"}, adminBypass = true)
    public Result<ReceivableResponse> settleReceivable(@RequestBody SettleReceivableRequest request) {
        try {
            // 参数校验
            if (request.getReceivableId() == null) {
                return Result.error(400, "应收款ID不能为空");
            }

            // 获取当前登录用户信息
            Long currentEntId = CurrentUser.getEntId();
            if (currentEntId == null) {
                return Result.error(401, "未登录或Token无效");
            }

            // 查询应收款
            Receivable receivable = financeService.getReceivableById(request.getReceivableId());
            if (receivable == null) {
                return Result.error(404, "应收款不存在");
            }

            // 验证当前企业是否为债权人或债务人
            boolean isParty = currentEntId.equals(receivable.getCreditorEntId())
                    || currentEntId.equals(receivable.getDebtorEntId());
            if (!CurrentUser.isAdmin() && !isParty) {
                return Result.error(403, "无权结算此应收款");
            }

            // 调用Service执行结算
            Receivable settled = financeService.settleReceivable(request.getReceivableId());

            logger.info("应收款结算成功: receivableId={}, receivableNo={}",
                    request.getReceivableId(), settled.getReceivableNo());

            return Result.success(convertToReceivableResponse(settled));

        } catch (IllegalArgumentException e) {
            logger.warn("应收款结算参数错误: {}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (IllegalStateException e) {
            logger.warn("应收款结算状态错误: {}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("应收款结算异常: ", e);
            return Result.error(500, "操作失败，请稍后重试");
        }
    }

    // ==================== 应收款查询 ====================

    /**
     * 根据ID查询应收款
     */
    @ApiOperation("根据ID查询应收款")
    @GetMapping("/receivable/{id}")
    public Result<ReceivableResponse> getReceivableById(
            @ApiParam("应收款ID") @PathVariable("id") Long id) {
        try {
            if (id == null) {
                return Result.error(400, "应收款ID不能为空");
            }

            // 获取当前登录用户信息
            Long currentEntId = CurrentUser.getEntId();
            if (currentEntId == null) {
                return Result.error(401, "未登录或Token无效");
            }

            Receivable receivable = financeService.getReceivableById(id);
            if (receivable == null) {
                return Result.error(404, "应收款不存在");
            }

            // 验证当前企业是否为当事人
            boolean isParty = currentEntId.equals(receivable.getCreditorEntId())
                    || currentEntId.equals(receivable.getDebtorEntId());
            if (!CurrentUser.isAdmin() && !isParty) {
                return Result.error(403, "无权查看此应收款");
            }

            return Result.success(convertToReceivableResponse(receivable));

        } catch (Exception e) {
            logger.error("查询应收款异常: ", e);
            return Result.error(500, "查询失败，请稍后重试");
        }
    }

    /**
     * 根据应收款编号查询应收款
     */
    @ApiOperation("根据应收款编号查询应收款")
    @GetMapping("/receivable/no/{receivableNo}")
    public Result<ReceivableResponse> getReceivableByNo(
            @ApiParam("应收款编号") @PathVariable("receivableNo") String receivableNo) {
        try {
            if (receivableNo == null || receivableNo.isBlank()) {
                return Result.error(400, "应收款编号不能为空");
            }

            // 获取当前登录用户信息
            Long currentEntId = CurrentUser.getEntId();
            if (currentEntId == null) {
                return Result.error(401, "未登录或Token无效");
            }

            Receivable receivable = financeService.getReceivableByNo(receivableNo);
            if (receivable == null) {
                return Result.error(404, "应收款不存在");
            }

            // 验证当前企业是否为当事人
            boolean isParty = currentEntId.equals(receivable.getCreditorEntId())
                    || currentEntId.equals(receivable.getDebtorEntId());
            if (!CurrentUser.isAdmin() && !isParty) {
                return Result.error(403, "无权查看此应收款");
            }

            return Result.success(convertToReceivableResponse(receivable));

        } catch (Exception e) {
            logger.error("查询应收款异常: ", e);
            return Result.error(500, "查询失败，请稍后重试");
        }
    }

    /**
     * 查询当前企业的应收款列表（作为债权人）
     */
    @ApiOperation("查询债权人应收款列表")
    @GetMapping("/receivable/creditor/list")
    public Result<List<ReceivableResponse>> listByCreditor(HttpServletRequest request) {
        try {
            Long currentEntId = CurrentUser.getEntId();
            if (currentEntId == null) {
                return Result.error(401, "未登录或Token无效");
            }

            List<Receivable> receivables = financeService.listByCreditor(currentEntId);
            return Result.success(convertToReceivableResponseList(receivables));

        } catch (Exception e) {
            logger.error("查询债权人应收款列表异常: ", e);
            return Result.error(500, "查询失败，请稍后重试");
        }
    }

    /**
     * 查询当前企业的应收款列表（作为债务人）
     */
    @ApiOperation("查询债务人应收款列表")
    @GetMapping("/receivable/debtor/list")
    public Result<List<ReceivableResponse>> listByDebtor(HttpServletRequest request) {
        try {
            Long currentEntId = CurrentUser.getEntId();
            if (currentEntId == null) {
                return Result.error(401, "未登录或Token无效");
            }

            List<Receivable> receivables = financeService.listByDebtor(currentEntId);
            return Result.success(convertToReceivableResponseList(receivables));

        } catch (Exception e) {
            logger.error("查询债务人应收款列表异常: ", e);
            return Result.error(500, "查询失败，请稍后重试");
        }
    }

    /**
     * 查询指定企业的应收款列表（管理员）
     */
    @ApiOperation("查询指定企业的应收款列表（管理员）")
    @GetMapping("/receivable/ent/{entId}")
    @RequireRole(value = {"ADMIN"}, adminBypass = true)
    public Result<Map<String, Object>> listByEnterprise(
            @ApiParam("企业ID") @PathVariable("entId") Long entId,
            @ApiParam("角色类型: creditor-债权人, debtor-债务人") @RequestParam(defaultValue = "creditor") String roleType) {
        try {
            if (entId == null) {
                return Result.error(400, "企业ID不能为空");
            }

            List<Receivable> receivables;
            if ("debtor".equalsIgnoreCase(roleType)) {
                receivables = financeService.listByDebtor(entId);
            } else {
                receivables = financeService.listByCreditor(entId);
            }

            return Result.success(Map.of(
                    "entId", entId,
                    "roleType", roleType,
                    "total", receivables.size(),
                    "list", convertToReceivableResponseList(receivables)
            ));

        } catch (Exception e) {
            logger.error("查询企业应收款列表异常: ", e);
            return Result.error(500, "查询失败，请稍后重试");
        }
    }

    // ==================== 还款记录查询 ====================

    /**
     * 查询应收款的还款记录列表
     */
    @ApiOperation("查询应收款还款记录列表")
    @GetMapping("/repayment/list/{receivableId}")
    public Result<List<RepaymentRecordResponse>> listRepayments(
            @ApiParam("应收款ID") @PathVariable("receivableId") Long receivableId) {
        try {
            if (receivableId == null) {
                return Result.error(400, "应收款ID不能为空");
            }

            // 获取当前登录用户信息
            Long currentEntId = CurrentUser.getEntId();
            if (currentEntId == null) {
                return Result.error(401, "未登录或Token无效");
            }

            // 验证应收款权限
            Receivable receivable = financeService.getReceivableById(receivableId);
            if (receivable == null) {
                return Result.error(404, "应收款不存在");
            }

            // 验证当前企业是否为当事人
            boolean isParty = currentEntId.equals(receivable.getCreditorEntId())
                    || currentEntId.equals(receivable.getDebtorEntId());
            if (!CurrentUser.isAdmin() && !isParty) {
                return Result.error(403, "无权查看此应收款的还款记录");
            }

            List<RepaymentRecord> records = financeService.listRepayments(receivableId);
            return Result.success(convertToRepaymentRecordResponseList(records));

        } catch (Exception e) {
            logger.error("查询还款记录列表异常: ", e);
            return Result.error(500, "查询失败，请稍后重试");
        }
    }

    // ==================== 内部类 - 请求/响应对象 ====================

    /**
     * 生成应收款请求
     */
    public static class GenerateReceivableRequest {
        private Long voucherId;
        private BigDecimal unitPrice;

        public Long getVoucherId() { return voucherId; }
        public void setVoucherId(Long voucherId) { this.voucherId = voucherId; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    }

    /**
     * 确认应收款请求
     */
    public static class ConfirmReceivableRequest {
        private Long receivableId;
        private String signature;

        public Long getReceivableId() { return receivableId; }
        public void setReceivableId(Long receivableId) { this.receivableId = receivableId; }
        public String getSignature() { return signature; }
        public void setSignature(String signature) { this.signature = signature; }
    }

    /**
     * 调整应收款请求
     */
    public static class AdjustReceivableRequest {
        private Long receivableId;
        private Integer adjustType;
        private BigDecimal amount;

        public Long getReceivableId() { return receivableId; }
        public void setReceivableId(Long receivableId) { this.receivableId = receivableId; }
        public Integer getAdjustType() { return adjustType; }
        public void setAdjustType(Integer adjustType) { this.adjustType = adjustType; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
    }

    /**
     * 现金还款请求
     */
    public static class CashRepaymentRequest {
        private Long receivableId;
        private BigDecimal amount;
        private String paymentVoucher;

        public Long getReceivableId() { return receivableId; }
        public void setReceivableId(Long receivableId) { this.receivableId = receivableId; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getPaymentVoucher() { return paymentVoucher; }
        public void setPaymentVoucher(String paymentVoucher) { this.paymentVoucher = paymentVoucher; }
    }

    /**
     * 仓单抵债请求
     */
    public static class OffsetWithCollateralRequest {
        private Long receivableId;
        private Long receiptId;
        private BigDecimal offsetPrice;
        private String signatureHash;

        public Long getReceivableId() { return receivableId; }
        public void setReceivableId(Long receivableId) { this.receivableId = receivableId; }
        public Long getReceiptId() { return receiptId; }
        public void setReceiptId(Long receiptId) { this.receiptId = receiptId; }
        public BigDecimal getOffsetPrice() { return offsetPrice; }
        public void setOffsetPrice(BigDecimal offsetPrice) { this.offsetPrice = offsetPrice; }
        public String getSignatureHash() { return signatureHash; }
        public void setSignatureHash(String signatureHash) { this.signatureHash = signatureHash; }
    }

    /**
     * 应收款融资请求
     */
    public static class FinanceReceivableRequest {
        private Long receivableId;
        private BigDecimal financeAmount;
        private Long financeEntId;

        public Long getReceivableId() { return receivableId; }
        public void setReceivableId(Long receivableId) { this.receivableId = receivableId; }
        public BigDecimal getFinanceAmount() { return financeAmount; }
        public void setFinanceAmount(BigDecimal financeAmount) { this.financeAmount = financeAmount; }
        public Long getFinanceEntId() { return financeEntId; }
        public void setFinanceEntId(Long financeEntId) { this.financeEntId = financeEntId; }
    }

    /**
     * 应收款结算请求
     */
    public static class SettleReceivableRequest {
        private Long receivableId;

        public Long getReceivableId() { return receivableId; }
        public void setReceivableId(Long receivableId) { this.receivableId = receivableId; }
    }

    /**
     * 应收款响应
     */
    public static class ReceivableResponse {
        private Long id;
        private String receivableNo;
        private Integer businessScene;
        private Long sourceVoucherId;
        private Long creditorEntId;
        private Long debtorEntId;
        private BigDecimal initialAmount;
        private BigDecimal adjustedAmount;
        private BigDecimal collectedAmount;
        private BigDecimal balanceUnpaid;
        private String currency;
        private java.time.LocalDateTime dueDate;
        private Integer status;
        private Integer isFinanced;
        private java.time.LocalDateTime createTime;
        private java.time.LocalDateTime updateTime;

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getReceivableNo() { return receivableNo; }
        public void setReceivableNo(String receivableNo) { this.receivableNo = receivableNo; }
        public Integer getBusinessScene() { return businessScene; }
        public void setBusinessScene(Integer businessScene) { this.businessScene = businessScene; }
        public Long getSourceVoucherId() { return sourceVoucherId; }
        public void setSourceVoucherId(Long sourceVoucherId) { this.sourceVoucherId = sourceVoucherId; }
        public Long getCreditorEntId() { return creditorEntId; }
        public void setCreditorEntId(Long creditorEntId) { this.creditorEntId = creditorEntId; }
        public Long getDebtorEntId() { return debtorEntId; }
        public void setDebtorEntId(Long debtorEntId) { this.debtorEntId = debtorEntId; }
        public BigDecimal getInitialAmount() { return initialAmount; }
        public void setInitialAmount(BigDecimal initialAmount) { this.initialAmount = initialAmount; }
        public BigDecimal getAdjustedAmount() { return adjustedAmount; }
        public void setAdjustedAmount(BigDecimal adjustedAmount) { this.adjustedAmount = adjustedAmount; }
        public BigDecimal getCollectedAmount() { return collectedAmount; }
        public void setCollectedAmount(BigDecimal collectedAmount) { this.collectedAmount = collectedAmount; }
        public BigDecimal getBalanceUnpaid() { return balanceUnpaid; }
        public void setBalanceUnpaid(BigDecimal balanceUnpaid) { this.balanceUnpaid = balanceUnpaid; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public java.time.LocalDateTime getDueDate() { return dueDate; }
        public void setDueDate(java.time.LocalDateTime dueDate) { this.dueDate = dueDate; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
        public Integer getIsFinanced() { return isFinanced; }
        public void setIsFinanced(Integer isFinanced) { this.isFinanced = isFinanced; }
        public java.time.LocalDateTime getCreateTime() { return createTime; }
        public void setCreateTime(java.time.LocalDateTime createTime) { this.createTime = createTime; }
        public java.time.LocalDateTime getUpdateTime() { return updateTime; }
        public void setUpdateTime(java.time.LocalDateTime updateTime) { this.updateTime = updateTime; }

        public String getStatusName() {
            if (status == null) return "未知";
            switch (status) {
                case 1: return "待确认";
                case 2: return "生效中";
                case 3: return "部分还款";
                case 4: return "已结清";
                case 5: return "逾期";
                default: return "未知";
            }
        }

        public String getBusinessSceneName() {
            if (businessScene == null) return "未知";
            switch (businessScene) {
                case 1: return "入库生成";
                case 2: return "转让配送签收生成";
                default: return "未知";
            }
        }
    }

    /**
     * 还款记录响应
     */
    public static class RepaymentRecordResponse {
        private Long id;
        private Long receivableId;
        private String repaymentNo;
        private Integer repaymentType;
        private BigDecimal amount;
        private String currency;
        private Long receiptId;
        private BigDecimal offsetPrice;
        private String signatureHash;
        private String paymentVoucher;
        private java.time.LocalDateTime repaymentTime;
        private java.time.LocalDateTime createTime;

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getReceivableId() { return receivableId; }
        public void setReceivableId(Long receivableId) { this.receivableId = receivableId; }
        public String getRepaymentNo() { return repaymentNo; }
        public void setRepaymentNo(String repaymentNo) { this.repaymentNo = repaymentNo; }
        public Integer getRepaymentType() { return repaymentType; }
        public void setRepaymentType(Integer repaymentType) { this.repaymentType = repaymentType; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public Long getReceiptId() { return receiptId; }
        public void setReceiptId(Long receiptId) { this.receiptId = receiptId; }
        public BigDecimal getOffsetPrice() { return offsetPrice; }
        public void setOffsetPrice(BigDecimal offsetPrice) { this.offsetPrice = offsetPrice; }
        public String getSignatureHash() { return signatureHash; }
        public void setSignatureHash(String signatureHash) { this.signatureHash = signatureHash; }
        public String getPaymentVoucher() { return paymentVoucher; }
        public void setPaymentVoucher(String paymentVoucher) { this.paymentVoucher = paymentVoucher; }
        public java.time.LocalDateTime getRepaymentTime() { return repaymentTime; }
        public void setRepaymentTime(java.time.LocalDateTime repaymentTime) { this.repaymentTime = repaymentTime; }
        public java.time.LocalDateTime getCreateTime() { return createTime; }
        public void setCreateTime(java.time.LocalDateTime createTime) { this.createTime = createTime; }

        public String getRepaymentTypeName() {
            if (repaymentType == null) return "未知";
            switch (repaymentType) {
                case 1: return "现金还款";
                case 2: return "仓单抵债";
                default: return "未知";
            }
        }
    }

    // ==================== 内部方法 - 类型转换 ====================

    private ReceivableResponse convertToReceivableResponse(Receivable receivable) {
        if (receivable == null) return null;

        ReceivableResponse response = new ReceivableResponse();
        response.setId(receivable.getId());
        response.setReceivableNo(receivable.getReceivableNo());
        response.setBusinessScene(receivable.getBusinessScene());
        response.setSourceVoucherId(receivable.getSourceVoucherId());
        response.setCreditorEntId(receivable.getCreditorEntId());
        response.setDebtorEntId(receivable.getDebtorEntId());
        response.setInitialAmount(receivable.getInitialAmount());
        response.setAdjustedAmount(receivable.getAdjustedAmount());
        response.setCollectedAmount(receivable.getCollectedAmount());
        response.setBalanceUnpaid(receivable.getBalanceUnpaid());
        response.setCurrency(receivable.getCurrency());
        response.setDueDate(receivable.getDueDate());
        response.setStatus(receivable.getStatus());
        response.setIsFinanced(receivable.getIsFinanced());
        response.setCreateTime(receivable.getCreateTime());
        response.setUpdateTime(receivable.getUpdateTime());
        return response;
    }

    private List<ReceivableResponse> convertToReceivableResponseList(List<Receivable> receivables) {
        if (receivables == null) return List.of();
        return receivables.stream()
                .map(this::convertToReceivableResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    private RepaymentRecordResponse convertToRepaymentRecordResponse(RepaymentRecord record) {
        if (record == null) return null;

        RepaymentRecordResponse response = new RepaymentRecordResponse();
        response.setId(record.getId());
        response.setReceivableId(record.getReceivableId());
        response.setRepaymentNo(record.getRepaymentNo());
        response.setRepaymentType(record.getRepaymentType());
        response.setAmount(record.getAmount());
        response.setCurrency(record.getCurrency());
        response.setReceiptId(record.getReceiptId());
        response.setOffsetPrice(record.getOffsetPrice());
        response.setSignatureHash(record.getSignatureHash());
        response.setPaymentVoucher(record.getPaymentVoucher());
        response.setRepaymentTime(record.getRepaymentTime());
        response.setCreateTime(record.getCreateTime());
        return response;
    }

    private List<RepaymentRecordResponse> convertToRepaymentRecordResponseList(List<RepaymentRecord> records) {
        if (records == null) return List.of();
        return records.stream()
                .map(this::convertToRepaymentRecordResponse)
                .collect(java.util.stream.Collectors.toList());
    }
}
