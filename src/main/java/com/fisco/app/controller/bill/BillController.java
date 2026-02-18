package com.fisco.app.controller.bill;

import java.util.List;

import javax.validation.Valid;

import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fisco.app.dto.bill.AcceptBillRequest;
import com.fisco.app.dto.bill.ApproveFinanceRequest;
import com.fisco.app.dto.bill.BillAcceptanceResponse;
import com.fisco.app.dto.bill.BillGuaranteeResponse;
import com.fisco.app.dto.bill.BillMergeResponse;
import com.fisco.app.dto.bill.BillSplitResponse;
import com.fisco.app.dto.bill.BillStatisticsDTO;
import com.fisco.app.dto.bill.CancelBillRequest;
import com.fisco.app.dto.bill.DiscountBillRequest;
import com.fisco.app.dto.bill.DiscountBillResponse;
import com.fisco.app.dto.bill.FinanceApplicationResponse;
import com.fisco.app.dto.bill.FinanceBillRequest;
import com.fisco.app.dto.bill.FreezeBillRequest;
import com.fisco.app.dto.bill.GuaranteeBillRequest;
import com.fisco.app.dto.bill.IssueBillRequest;
import com.fisco.app.dto.bill.MergeBillsRequest;
import com.fisco.app.dto.bill.RepayBillRequest;
import com.fisco.app.dto.bill.RepayBillResponse;
import com.fisco.app.dto.bill.RepayFinanceRequest;
import com.fisco.app.dto.bill.SplitBillRequest;
import com.fisco.app.dto.bill.UnfreezeBillRequest;
import com.fisco.app.dto.endorsement.EndorseBillRequest;
import com.fisco.app.dto.endorsement.EndorsementResponse;
import com.fisco.app.entity.bill.Bill;
import com.fisco.app.entity.bill.DiscountRecord;
import com.fisco.app.entity.bill.Endorsement;
import com.fisco.app.entity.bill.RepaymentRecord;
import com.fisco.app.security.RequireEnterprise;
import com.fisco.app.service.bill.BillService;
import com.fisco.app.vo.Result;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 票据管理Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/bill")
@RequiredArgsConstructor
@Api(tags = "票据管理")
public class BillController {

    private final BillService billService;

    /**
     * 开票
     * POST /api/bill
     */
    @PostMapping
    @ApiOperation(value = "开票", notes = "出票人开立票据，使用IssueBillParams结构体封装参数，避免Solidity 16变量限制")
    public Result<Bill> issueBill(
            @ApiParam(value = "开票请求参数（结构体封装）", required = true) @Valid @RequestBody IssueBillRequest request,
            Authentication authentication) {
        // 从认证上下文获取用户地址（由JWT过滤器设置）
        String issuerAddress = authentication.getName();

        log.info("==================== 接收到开票请求 ====================");
        log.info("票据基本信息: billId={}, type={}, amount={}, currency={}",
                 request.getId(), request.getBillType(), request.getAmount(), request.getCurrency());
        log.info("参与方: issuer={}, acceptor={}, beneficiary={}",
                 issuerAddress, request.getAcceptorAddress(), request.getBeneficiaryAddress());
        log.info("日期信息: issueDate={}, dueDate={}",
                 request.getIssueDate(), request.getDueDate());

        long startTime = System.currentTimeMillis();

        try {
            log.debug("调用BillService开票");
            Bill saved = billService.issueBill(request, issuerAddress);

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 开票请求处理完成: billId={}, txHash={}, 耗时={}ms",
                     saved.getBillId(), saved.getBlockchainTxHash(), duration);
            log.info("==================== 开票请求结束 ====================");

            return Result.success("票据开立成功", saved);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 开票请求处理失败: billId={}, 耗时={}ms, error={}",
                     request.getId(), duration, e.getMessage(), e);
            log.info("==================== 开票请求失败（结束） ====================");
            throw e;
        }
    }

    /**
     * 获取票据信息
     * GET /api/bill/{billId}
     */
    @GetMapping("/{billId}")
    @ApiOperation(value = "获取票据详情", notes = "根据票据ID查询详细信息")
    public Result<Bill> getBill(
            @ApiParam(value = "票据ID", required = true) @PathVariable @NonNull String billId) {

        log.debug("查询票据信息请求: billId={}", billId);
        long startTime = System.currentTimeMillis();

        try {
            Bill bill = billService.getBill(billId);

            long duration = System.currentTimeMillis() - startTime;
            log.debug("✓ 票据信息查询完成: billId={}, status={}, 耗时={}ms",
                     billId, bill.getBillStatus(), duration);

            return Result.success(bill);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗ 票据信息查询失败: billId={}, 耗时={}ms, error={}",
                     billId, duration, e.getMessage());
            throw e;
        }
    }

    /**
     * 票据背书
     * POST /api/bill/{billId}/endorse
     */
    @PostMapping("/{billId}/endorse")
    @ApiOperation(value = "票据背书", notes = "当前持票人将票据背书转让给被背书人")
    public Result<EndorsementResponse> endorseBill(
            @ApiParam(value = "票据ID", required = true) @PathVariable @NonNull String billId,
            @ApiParam(value = "背书请求参数", required = true) @Valid @RequestBody EndorseBillRequest request,
            Authentication authentication) {
        // 从认证上下文获取背书人地址（当前持票人）
        String endorserAddress = authentication.getName();

        log.info("==================== 接收到票据背书请求 ====================");
        log.info("背书信息: billId={}, endorser={}, endorsee={}, type={}",
                 billId, endorserAddress, request.getEndorseeAddress(), request.getEndorsementType());

        long startTime = System.currentTimeMillis();

        try {
            log.debug("调用BillService背书票据");
            EndorsementResponse response = billService.endorseBill(billId, request, endorserAddress);

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 票据背书请求处理完成: billId={}, endorsementId={}, 耗时={}ms",
                     billId, response.getId(), duration);
            log.info("==================== 票据背书请求结束 ====================");

            return Result.success("票据背书成功", response);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 票据背书请求处理失败: billId={}, 耗时={}ms, error={}",
                     billId, duration, e.getMessage(), e);
            log.info("==================== 票据背书请求失败（结束） ====================");
            throw e;
        }
    }

    /**
     * 获取票据背书历史
     * GET /api/bill/{billId}/endorsements
     */
    @GetMapping("/{billId}/endorsements")
    @ApiOperation(value = "获取票据背书历史", notes = "查询票据的所有背书记录，按时间顺序排列")
    public Result<List<Endorsement>> getEndorsementHistory(
            @ApiParam(value = "票据ID", required = true) @PathVariable @NonNull String billId) {
        log.info("查询背书历史: billId={}", billId);

        List<Endorsement> history = billService.getEndorsementHistory(billId);
        return Result.success("查询成功", history);
    }

    /**
     * 从区块链获取票据背书历史
     * GET /api/bill/{billId}/endorsements/chain
     */
    @GetMapping("/{billId}/endorsements/chain")
    @ApiOperation(value = "从区块链获取背书历史", notes = "从区块链查询票据的所有背书记录，用于验证数据完整性")
    public Result<List<java.util.Map<String, Object>>> getEndorsementHistoryFromChain(
            @ApiParam(value = "票据ID", required = true) @PathVariable @NonNull String billId) {
        log.info("从区块链查询背书历史: billId={}", billId);

        List<java.util.Map<String, Object>> history = billService.getEndorsementHistoryFromChain(billId);
        return Result.success("查询成功", history);
    }

    /**
     * 验证票据背书历史的数据完整性
     * GET /api/bill/{billId}/endorsements/validate
     */
    @GetMapping("/{billId}/endorsements/validate")
    @ApiOperation(value = "验证背书历史完整性", notes = "对比数据库和区块链上的背书记录，验证数据完整性")
    public Result<java.util.Map<String, Object>> validateEndorsementHistory(
            @ApiParam(value = "票据ID", required = true) @PathVariable @NonNull String billId) {
        log.info("验证背书历史完整性: billId={}", billId);

        java.util.Map<String, Object> result = billService.validateEndorsementHistory(billId);
        return Result.success("验证完成", result);
    }

    /**
     * 票据贴现
     * POST /api/bill/{billId}/discount
     */
    @PostMapping("/{billId}/discount")
    @ApiOperation(value = "票据贴现", notes = "当前持票人将票据贴现给金融机构")
    public Result<DiscountBillResponse> discountBill(
            @ApiParam(value = "票据ID", required = true) @PathVariable @NonNull String billId,
            @ApiParam(value = "贴现请求参数", required = true) @Valid @RequestBody DiscountBillRequest request,
            Authentication authentication) {
        // 从认证上下文获取持票人地址
        String holderAddress = authentication.getName();

        log.info("==================== 接收到票据贴现请求 ====================");
        log.info("贴现信息: billId={}, holder={}, institution={}, amount={}, rate={}",
                 billId, holderAddress, request.getFinancialInstitutionAddress(),
                 request.getDiscountAmount(), request.getDiscountRate());

        long startTime = System.currentTimeMillis();

        try {
            log.debug("调用BillService贴现票据");
            DiscountBillResponse response = billService.discountBill(billId, request, holderAddress);

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 票据贴现请求处理完成: billId={}, discountId={}, 耗时={}ms",
                     billId, response.getId(), duration);
            log.info("==================== 票据贴现请求结束 ====================");

            return Result.success("票据贴现成功", response);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 票据贴现请求处理失败: billId={}, 耗时={}ms, error={}",
                     billId, duration, e.getMessage(), e);
            log.info("==================== 票据贴现请求失败（结束） ====================");
            throw e;
        }
    }

    /**
     * 获取票据贴现历史
     * GET /api/bill/{billId}/discounts
     */
    @GetMapping("/{billId}/discounts")
    @ApiOperation(value = "获取票据贴现历史", notes = "查询票据的所有贴现记录")
    public Result<List<DiscountRecord>> getDiscountHistory(
            @ApiParam(value = "票据ID", required = true) @PathVariable @NonNull String billId) {
        log.info("查询贴现历史: billId={}", billId);

        List<DiscountRecord> history = billService.getDiscountHistory(billId);
        return Result.success("查询成功", history);
    }

    /**
     * 票据到期处理
     * POST /api/bill/{billId}/maturity
     */
    @PostMapping("/{billId}/maturity")
    @ApiOperation(value = "票据到期处理", notes = "处理已贴现票据的到期，自动计算利息并更新状态")
    public Result<RepayBillResponse> handleBillMaturity(
            @ApiParam(value = "票据ID", required = true) @PathVariable @NonNull String billId) {
        log.info("票据到期处理请求: billId={}", billId);

        RepayBillResponse response = billService.handleBillMaturity(billId);
        return Result.success("票据到期处理成功", response);
    }

    /**
     * 票据还款
     * POST /api/bill/{billId}/repay
     */
    @PostMapping("/{billId}/repay")
    @ApiOperation(value = "票据还款", notes = "承兑人主动还款，支持提前还款或逾期还款")
    public Result<RepayBillResponse> repayBill(
            @ApiParam(value = "票据ID", required = true) @PathVariable @NonNull String billId,
            @ApiParam(value = "还款请求参数", required = true) @Valid @RequestBody RepayBillRequest request,
            Authentication authentication) {
        // 从认证上下文获取承兑人地址
        String payerAddress = authentication.getName();

        log.info("==================== 接收到票据还款请求 ====================");
        log.info("还款信息: billId={}, payer={}, amount={}, type={}",
                 billId, payerAddress, request.getPaymentAmount(), request.getPaymentType());

        long startTime = System.currentTimeMillis();

        try {
            log.debug("调用BillService还款");
            RepayBillResponse response = billService.repayBill(billId, request, payerAddress);

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 票据还款请求处理完成: billId={}, paymentId={}, 耗时={}ms",
                     billId, response.getId(), duration);
            log.info("==================== 票据还款请求结束 ====================");

            return Result.success("票据还款成功", response);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 票据还款请求处理失败: billId={}, 耗时={}ms, error={}",
                     billId, duration, e.getMessage(), e);
            log.info("==================== 票据还款请求失败（结束） ====================");
            throw e;
        }
    }

    /**
     * 获取票据还款历史
     * GET /api/bill/{billId}/repayments
     */
    @GetMapping("/{billId}/repayments")
    @ApiOperation(value = "获取票据还款历史", notes = "查询票据的所有还款记录")
    public Result<List<RepaymentRecord>> getRepaymentHistory(
            @ApiParam(value = "票据ID", required = true) @PathVariable @NonNull String billId) {
        log.info("查询还款历史: billId={}", billId);

        List<RepaymentRecord> history = billService.getRepaymentHistory(billId);
        return Result.success("查询成功", history);
    }

    // ==================== 票据生命周期管理接口 ====================

    /**
     * 作废票据
     * POST /api/bill/{billId}/cancel
     */
    @PostMapping("/{billId}/cancel")
    @ApiOperation(value = "作废票据", notes = "当前持票人作废票据（丢失、错误等原因）")
    public Result<Bill> cancelBill(
            @ApiParam(value = "票据ID", required = true) @PathVariable @NonNull String billId,
            @ApiParam(value = "作废请求参数", required = true) @Valid @RequestBody CancelBillRequest request,
            Authentication authentication) {
        String operatorAddress = authentication.getName();

        log.info("==================== 接收到票据作废请求 ====================");
        log.info("票据ID: {}, 操作人: {}, 原因: {}", billId, operatorAddress, request.getCancelReason());

        long startTime = System.currentTimeMillis();

        try {
            Bill bill = billService.cancelBill(billId, request, operatorAddress);

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 票据作废请求处理完成: billId={}, 耗时={}ms", billId, duration);
            log.info("==================== 票据作废请求结束 ====================");

            return Result.success("票据作废成功", bill);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 票据作废请求处理失败: billId={}, 耗时={}ms, error={}", billId, duration, e.getMessage(), e);
            log.info("==================== 票据作废请求失败（结束） ====================");
            throw e;
        }
    }

    /**
     * 冻结票据
     * POST /api/bill/{billId}/freeze
     */
    @PostMapping("/{billId}/freeze")
    @ApiOperation(value = "冻结票据", notes = "冻结票据（法律纠纷等场景），冻结后禁止所有操作")
    public Result<Bill> freezeBill(
            @ApiParam(value = "票据ID", required = true) @PathVariable @NonNull String billId,
            @ApiParam(value = "冻结请求参数", required = true) @Valid @RequestBody FreezeBillRequest request,
            Authentication authentication) {
        String operatorAddress = authentication.getName();

        log.info("==================== 接收到票据冻结请求 ====================");
        log.info("票据ID: {}, 操作人: {}, 原因: {}", billId, operatorAddress, request.getFreezeReason());

        long startTime = System.currentTimeMillis();

        try {
            Bill bill = billService.freezeBill(billId, request, operatorAddress);

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 票据冻结请求处理完成: billId={}, 耗时={}ms", billId, duration);
            log.info("==================== 票据冻结请求结束 ====================");

            return Result.success("票据冻结成功", bill);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 票据冻结请求处理失败: billId={}, 耗时={}ms, error={}", billId, duration, e.getMessage(), e);
            log.info("==================== 票据冻结请求失败（结束） ====================");
            throw e;
        }
    }

    /**
     * 解冻票据
     * POST /api/bill/{billId}/unfreeze
     */
    @PostMapping("/{billId}/unfreeze")
    @ApiOperation(value = "解冻票据", notes = "解冻已冻结的票据，恢复正常状态")
    public Result<Bill> unfreezeBill(
            @ApiParam(value = "票据ID", required = true) @PathVariable @NonNull String billId,
            @ApiParam(value = "解冻请求参数", required = true) @Valid @RequestBody UnfreezeBillRequest request,
            Authentication authentication) {
        String operatorAddress = authentication.getName();

        log.info("==================== 接收到票据解冻请求 ====================");
        log.info("票据ID: {}, 操作人: {}, 原因: {}", billId, operatorAddress, request.getUnfreezeReason());

        long startTime = System.currentTimeMillis();

        try {
            Bill bill = billService.unfreezeBill(billId, request, operatorAddress);

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 票据解冻请求处理完成: billId={}, 耗时={}ms", billId, duration);
            log.info("==================== 票据解冻请求结束 ====================");

            return Result.success("票据解冻成功", bill);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 票据解冻请求处理失败: billId={}, 耗时={}ms, error={}", billId, duration, e.getMessage(), e);
            log.info("==================== 票据解冻请求失败（结束） ====================");
            throw e;
        }
    }

    /**
     * 查询已过期票据
     * GET /api/bill/expired
     */
    @GetMapping("/expired")
    @ApiOperation(value = "查询已过期票据", notes = "查询所有已过期但未付款的票据")
    public Result<java.util.List<Bill>> getExpiredBills(
            @ApiParam(value = "企业ID（可选，不传则查询所有）") @RequestParam(required = false) String enterpriseId) {
        log.info("查询过期票据: enterpriseId={}", enterpriseId);

        java.util.List<Bill> expiredBills = billService.getExpiredBills(enterpriseId);
        return Result.success("查询成功", expiredBills);
    }

    /**
     * 查询拒付票据
     * GET /api/bill/dishonored
     */
    @GetMapping("/dishonored")
    @ApiOperation(value = "查询拒付票据", notes = "查询所有拒付的票据")
    public Result<java.util.List<Bill>> getDishonoredBills(
            @ApiParam(value = "承兑人地址（可选）") @RequestParam(required = false) String acceptorAddress,
            @ApiParam(value = "开始日期（可选）") @RequestParam(required = false) String startDate,
            @ApiParam(value = "结束日期（可选）") @RequestParam(required = false) String endDate) {
        log.info("查询拒付票据: acceptorAddress={}, startDate={}, endDate={}", acceptorAddress, startDate, endDate);

        java.time.LocalDateTime start = startDate != null ? java.time.LocalDateTime.parse(startDate) : null;
        java.time.LocalDateTime end = endDate != null ? java.time.LocalDateTime.parse(endDate) : null;

        java.util.List<Bill> dishonoredBills = billService.getDishonoredBills(acceptorAddress, start, end);
        return Result.success("查询成功", dishonoredBills);
    }

    /**
     * 申请票据融资
     */
    @PostMapping("/{billId}/finance")
    @ApiOperation(value = "申请票据融资", notes = "企业使用票据申请融资")
    @SuppressWarnings("null")
    public Result<FinanceApplicationResponse> applyFinance(
            @ApiParam(value = "票据ID", required = true) @PathVariable String billId,
            @ApiParam(value = "融资申请信息", required = true) @RequestBody @Valid FinanceBillRequest request,
            @RequestHeader(value = "X-User-Address", required = false) String applicantAddress) {

        log.info("申请票据融资: billId={}, applicantAddress={}, financeAmount={}",
                billId, applicantAddress, request.getFinanceAmount());

        FinanceApplicationResponse response = billService.applyFinance(billId, request, applicantAddress);
        return Result.success("融资申请提交成功", response);
    }

    /**
     * 审核票据融资申请
     */
    @PostMapping("/finance/approve")
    @ApiOperation(value = "审核票据融资申请", notes = "金融机构审核融资申请")
    public Result<FinanceApplicationResponse> approveFinance(
            @ApiParam(value = "审核信息", required = true) @RequestBody @Valid ApproveFinanceRequest request,
            @RequestHeader("X-User-Address") String reviewerAddress) {

        log.info("审核票据融资申请: applicationId={}, approvalResult={}, reviewerAddress={}",
                request.getApplicationId(), request.getApprovalResult(), reviewerAddress);

        FinanceApplicationResponse response = billService.approveFinance(request, reviewerAddress);
        return Result.success("融资申请审核完成", response);
    }

    /**
     * 查询待审核的融资申请
     */
    @GetMapping("/finance/pending")
    @ApiOperation(value = "查询待审核的融资申请", notes = "查询所有或特定金融机构的待审核申请")
    public Result<java.util.List<FinanceApplicationResponse>> getPendingFinanceApplications(
            @ApiParam(value = "金融机构ID（可选，为空则查询所有）") @RequestParam(required = false) String institutionId) {

        log.info("查询待审核的融资申请: institutionId={}", institutionId);

        java.util.List<FinanceApplicationResponse> responses =
                billService.getPendingFinanceApplications(institutionId);
        return Result.success("查询成功", responses);
    }

    /**
     * 票据融资还款
     */
    @PostMapping("/finance/{applicationId}/repay")
    @ApiOperation(value = "票据融资还款", notes = "企业归还融资款项")
    @SuppressWarnings("null")
    public Result<FinanceApplicationResponse> repayFinance(
            @ApiParam(value = "融资申请ID", required = true) @PathVariable String applicationId,
            @ApiParam(value = "还款信息", required = true) @RequestBody @Valid RepayFinanceRequest request,
            @RequestHeader(value = "X-User-Address", required = false) String payerAddress) {

        log.info("票据融资还款: applicationId={}, repayAmount={}, repayType={}, payerAddress={}",
                applicationId, request.getRepayAmount(), request.getRepayType(), payerAddress);

        FinanceApplicationResponse response = billService.repayFinance(applicationId, request, payerAddress);
        return Result.success("还款成功", response);
    }

    // ==================== 新增功能接口 ====================

    /**
     * 获取票据统计数据
     * GET /api/bill/statistics
     *
     * 权限说明：
     * - 企业用户只能查询自己的统计数据
     * - 如果传入enterpriseAddress参数，必须是当前登录用户自己的地址
     */
    @GetMapping("/statistics")
    @RequireEnterprise
    @ApiOperation(value = "票据统计分析", notes = "查询票据的整体统计数据，支持多维度聚合分析")
    public Result<BillStatisticsDTO> getBillStatistics(
            @ApiParam(value = "统计开始时间") @RequestParam(required = false) java.time.LocalDateTime startTime,
            @ApiParam(value = "统计结束时间") @RequestParam(required = false) java.time.LocalDateTime endTime,
            @ApiParam(value = "企业地址（可选，不传则查询当前用户数据）") @RequestParam(required = false) String enterpriseAddress,
            @ApiParam(value = "统计维度") @RequestParam(required = false) String dimension,
            Authentication authentication) {

        String currentUserAddress = authentication.getName();

        // 安全检查：如果传入了enterpriseAddress，必须是当前用户自己的地址
        if (enterpriseAddress != null && !enterpriseAddress.isEmpty()
            && !enterpriseAddress.equals(currentUserAddress)) {
            log.warn("用户尝试查询其他企业的统计数据: currentUser={}, requestedEnterprise={}",
                     currentUserAddress, enterpriseAddress);
            return Result.error("无权查询其他企业的统计数据");
        }

        // 如果没有传入enterpriseAddress，使用当前登录用户的地址
        if (enterpriseAddress == null || enterpriseAddress.isEmpty()) {
            enterpriseAddress = currentUserAddress;
        }

        log.info("查询票据统计数据: startTime={}, endTime={}, enterprise={}, dimension={}",
                 startTime, endTime, enterpriseAddress, dimension);

        BillStatisticsDTO statistics = billService.getBillStatistics(startTime, endTime, enterpriseAddress, dimension);
        return Result.success("统计查询成功", statistics);
    }

    /**
     * 票据承兑
     * POST /api/bill/{billId}/acceptance
     *
     * 权限说明：
     * - 只有企业用户可以访问
     * - TODO: 业务层需要验证当前用户是否是该票据的承兑人
     */
    @PostMapping("/{billId}/acceptance")
    @RequireEnterprise
    @ApiOperation(value = "票据承兑", notes = "承兑人确认承兑票据")
    public Result<BillAcceptanceResponse> acceptBill(
            @ApiParam(value = "票据ID", required = true) @PathVariable String billId,
            @ApiParam(value = "承兑请求参数", required = true) @RequestBody @Valid AcceptBillRequest request,
            Authentication authentication) {

        String acceptorAddress = authentication.getName();
        log.info("票据承兑请求: billId={}, acceptorAddress={}, acceptanceType={}",
                 billId, acceptorAddress, request.getAcceptanceType());

        // TODO: Implement acceptance logic in BillService
        // TODO: Add business logic check: verify current user is the acceptor of the bill
        BillAcceptanceResponse response = new BillAcceptanceResponse();
        response.setBillId(billId);
        response.setAcceptanceType(request.getAcceptanceType());
        response.setAcceptanceRemarks(request.getAcceptanceRemarks());
        response.setAcceptorAddress(acceptorAddress);
        response.setResult("success");
        response.setMessage("票据承兑功能待实现");

        return Result.success("票据承兑请求已接收", response);
    }

    /**
     * 票据拆分
     * POST /api/bill/{billId}/split
     *
     * 权限说明：
     * - 只有企业用户可以访问
     * - TODO: 业务层需要验证当前用户是否是该票据的当前持票人
     */
    @PostMapping("/{billId}/split")
    @RequireEnterprise
    @ApiOperation(value = "票据拆分", notes = "将一笔票据拆分为多笔小额票据")
    public Result<BillSplitResponse> splitBill(
            @ApiParam(value = "父票据ID", required = true) @PathVariable String billId,
            @ApiParam(value = "拆分请求参数", required = true) @RequestBody @Valid SplitBillRequest request,
            Authentication authentication) {

        String applicantAddress = authentication.getName();
        log.info("票据拆分请求: billId={}, applicantAddress={}, splitScheme={}, splitCount={}",
                 billId, applicantAddress, request.getSplitScheme(), request.getSplitCount());

        // TODO: Implement split logic in BillService
        // TODO: Add business logic check: verify current user is the current holder of the bill
        BillSplitResponse response = new BillSplitResponse();
        response.setParentBillId(billId);
        response.setSplitScheme(request.getSplitScheme());
        response.setSplitCount(request.getSplitCount());
        response.setResult("pending");
        response.setMessage("票据拆分功能待实现");

        return Result.success("票据拆分请求已接收", response);
    }

    /**
     * 票据合并
     * POST /api/bill/merge
     *
     * 权限说明：
     * - 只有企业用户可以访问
     * - TODO: 业务层需要验证当前用户是否是所有待合并票据的当前持票人
     */
    @PostMapping("/merge")
    @RequireEnterprise
    @ApiOperation(value = "票据合并", notes = "将多笔票据合并为一笔大额票据")
    public Result<BillMergeResponse> mergeBills(
            @ApiParam(value = "合并请求参数", required = true) @RequestBody @Valid MergeBillsRequest request,
            Authentication authentication) {

        String applicantAddress = authentication.getName();
        log.info("票据合并请求: applicantAddress={}, mergeType={}, billCount={}",
                 applicantAddress, request.getMergeType(), request.getBillIds().size());

        // TODO: Implement merge logic in BillService
        // TODO: Add business logic check: verify current user is the holder of all bills to be merged
        BillMergeResponse response = new BillMergeResponse();
        response.setMergeType(request.getMergeType());
        response.setResult("pending");
        response.setMessage("票据合并功能待实现");

        return Result.success("票据合并请求已接收", response);
    }

    /**
     * 票据担保
     * POST /api/bill/{billId}/guarantee
     *
     * 权限说明：
     * - 只有企业用户可以访问
     * - 任何企业都可以为票据提供担保（第三方担保）
     */
    @PostMapping("/{billId}/guarantee")
    @RequireEnterprise
    @ApiOperation(value = "票据担保", notes = "第三方为票据提供担保")
    public Result<BillGuaranteeResponse> guaranteeBill(
            @ApiParam(value = "票据ID", required = true) @PathVariable String billId,
            @ApiParam(value = "担保请求参数", required = true) @RequestBody @Valid GuaranteeBillRequest request,
            Authentication authentication) {

        String guarantorAddress = authentication.getName();
        log.info("票据担保请求: billId={}, guarantorAddress={}, guaranteeType={}, guaranteeAmount={}",
                 billId, guarantorAddress, request.getGuaranteeType(), request.getGuaranteeAmount());

        // TODO: Implement guarantee logic in BillService
        BillGuaranteeResponse response = new BillGuaranteeResponse();
        response.setBillId(billId);
        response.setGuarantorAddress(guarantorAddress);
        response.setGuaranteeType(request.getGuaranteeType());
        response.setGuaranteeAmount(request.getGuaranteeAmount());
        response.setResult("pending");
        response.setMessage("票据担保功能待实现");

        return Result.success("票据担保请求已接收", response);
    }
}
