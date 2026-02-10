package com.fisco.app.controller.receivable;

import java.math.BigDecimal;
import java.util.List;

import javax.validation.Valid;

import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fisco.app.dto.receivable.AgedAnalysisResponse;
import com.fisco.app.dto.receivable.BadDebtQueryRequest;
import com.fisco.app.dto.receivable.BadDebtQueryResponse;
import com.fisco.app.dto.receivable.CreateReceivableRequest;
import com.fisco.app.dto.receivable.FinanceRecordResponse;
import com.fisco.app.dto.receivable.OverdueQueryRequest;
import com.fisco.app.dto.receivable.OverdueQueryResponse;
import com.fisco.app.dto.receivable.PenaltyCalculateRequest;
import com.fisco.app.dto.receivable.PenaltyCalculateResponse;
import com.fisco.app.dto.receivable.ReceivableMergeRequest;
import com.fisco.app.dto.receivable.ReceivableMergeResponse;
import com.fisco.app.dto.receivable.ReceivableSplitRequest;
import com.fisco.app.dto.receivable.ReceivableSplitResponse;
import com.fisco.app.dto.receivable.RemindRequest;
import com.fisco.app.dto.receivable.RemindResponse;
import com.fisco.app.dto.receivable.RepayDetailRequest;
import com.fisco.app.dto.receivable.RepaymentRecordResponse;
import com.fisco.app.dto.receivable.ReceivableStatisticsResponse;
import com.fisco.app.dto.receivable.TransferHistoryResponse;
import com.fisco.app.dto.receivable.WriteOffRequest;
import com.fisco.app.entity.receivable.Receivable;
import com.fisco.app.service.receivable.ReceivableOverdueService;
import com.fisco.app.service.receivable.ReceivableQueryService;
import com.fisco.app.service.receivable.ReceivableRepaymentService;
import com.fisco.app.service.receivable.ReceivableService;
import com.fisco.app.vo.Result;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 应收账款管理Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/receivable")
@RequiredArgsConstructor
@Api(tags = "应收账款管理")
public class ReceivableController {

    private final ReceivableService receivableService;
    private final ReceivableOverdueService receivableOverdueService;
    private final ReceivableRepaymentService receivableRepaymentService;
    private final ReceivableQueryService receivableQueryService;

    /**
     * 创建应收账款
     * POST /api/receivable
     */
    @PostMapping
    @ApiOperation(value = "创建应收账款", notes = "供应商创建新的应收账款，等待核心企业确认。使用CreateReceivableParams结构体封装参数，避免Solidity 16变量限制")
    public Result<Receivable> createReceivable(
            @ApiParam(value = "创建应收账款请求参数（结构体封装）", required = true) @Valid @RequestBody CreateReceivableRequest request,
            Authentication authentication) {
        // 从认证上下文获取用户地址（由JWT过滤器设置）
        String supplierAddress = authentication.getName();

        log.info("==================== 接收到应收账款创建请求 ====================");
        log.info("应收账款基本信息: receivableId={}, amount={}, currency={}",
                 request.getId(), request.getAmount(), request.getCurrency());
        log.info("参与方: supplier={}, coreEnterprise={}",
                 supplierAddress, request.getCoreEnterpriseAddress());
        log.info("日期信息: issueDate={}, dueDate={}",
                 request.getIssueDate(), request.getDueDate());

        long startTime = System.currentTimeMillis();

        try {
            log.debug("调用ReceivableService创建应收账款");
            Receivable saved = receivableService.createReceivable(request, supplierAddress);

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 应收账款创建请求处理完成: receivableId={}, txHash={}, 耗时={}ms",
                     saved.getId(), saved.getTxHash(), duration);
            log.info("==================== 应收账款创建请求结束 ====================");

            return Result.success("应收账款创建成功", saved);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 应收账款创建请求处理失败: receivableId={}, 耗时={}ms, error={}",
                     request.getId(), duration, e.getMessage(), e);
            log.info("==================== 应收账款创建请求失败（结束） ====================");
            throw e;
        }
    }

    /**
     * 核心企业确认应收账款
     * PUT /api/receivable/{receivableId}/confirm
     */
    @PutMapping("/{receivableId}/confirm")
    @ApiOperation(value = "确认应收账款", notes = "核心企业确认应收账款，确认后可进行融资")
    public Result<Void> confirmReceivable(
            @ApiParam(value = "应收账款ID", required = true, example = "REC20240113001") @PathVariable @NonNull String receivableId) {
        log.info("==================== 接收到应收账款确认请求 ====================");
        log.info("应收账款ID: {}", receivableId);

        long startTime = System.currentTimeMillis();

        try {
            log.debug("调用ReceivableService确认应收账款");
            receivableService.confirmReceivable(receivableId);

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 应收账款确认请求处理完成: receivableId={}, 耗时={}ms", receivableId, duration);
            log.info("==================== 应收账款确认请求结束 ====================");

            return Result.success();

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 应收账款确认请求处理失败: receivableId={}, 耗时={}ms, error={}",
                     receivableId, duration, e.getMessage(), e);
            log.info("==================== 应收账款确认请求失败（结束） ====================");
            throw e;
        }
    }

    /**
     * 应收账款融资
     * PUT /api/receivable/{receivableId}/finance
     */
    @PutMapping("/{receivableId}/finance")
    @ApiOperation(value = "应收账款融资", notes = "金融机构为已确认的应收账款提供融资")
    public Result<Void> financeReceivable(
            @ApiParam(value = "应收账款ID", required = true) @PathVariable @NonNull String receivableId,
            @ApiParam(value = "融资请求信息", required = true) @RequestBody FinanceRequest request) {
        log.info("==================== 接收到应收账款融资请求 ====================");
        log.info("融资信息: receivableId={}, financier={}, amount={}, rate={}",
                 receivableId, request.getFinancierAddress(),
                 request.getFinanceAmount(), request.getFinanceRate());

        long startTime = System.currentTimeMillis();

        try {
            log.debug("调用ReceivableService融资应收账款");
            receivableService.financeReceivable(
                receivableId,
                request.getFinancierAddress(),
                request.getFinanceAmount(),
                request.getFinanceRate()
            );

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 应收账款融资请求处理完成: receivableId={}, 耗时={}ms", receivableId, duration);
            log.info("==================== 应收账款融资请求结束 ====================");

            return Result.success();

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 应收账款融资请求处理失败: receivableId={}, 耗时={}ms, error={}",
                     receivableId, duration, e.getMessage(), e);
            log.info("==================== 应收账款融资请求失败（结束） ====================");
            throw e;
        }
    }

    /**
     * 应收账款还款
     * PUT /api/receivable/{receivableId}/repay
     */
    @PutMapping("/{receivableId}/repay")
    @ApiOperation(value = "应收账款还款", notes = "核心企业或金融机构进行还款")
    public Result<Void> repayReceivable(
            @ApiParam(value = "应收账款ID", required = true) @PathVariable @NonNull String receivableId,
            @ApiParam(value = "还款金额", required = true, example = "500000.00") @RequestBody BigDecimal amount) {
        receivableService.repayReceivable(receivableId, amount);
        return Result.success();
    }

    // ==================== 还款详情接口（增强版）====================

    /**
     * 应收账款还款（详细版）
     * PUT /api/receivable/{receivableId}/repay-detail
     */
    @PutMapping("/{receivableId}/repay-detail")
    @ApiOperation(value = "应收账款还款（详细版）", notes = "支持部分还款、提前还款、逾期还款，记录完整的还款信息")
    public Result<RepaymentRecordResponse> repayDetail(
            @ApiParam(value = "应收账款ID", required = true) @PathVariable @NonNull String receivableId,
            @ApiParam(value = "还款详情", required = true) @Valid @RequestBody RepayDetailRequest request,
            Authentication authentication) {

        log.info("==================== 接收到应收账款还款详情请求 ====================");
        log.info("还款详情: receivableId={}, type={}, amount={}",
                receivableId, request.getRepaymentType(), request.getRepaymentAmount());

        long startTime = System.currentTimeMillis();

        try {
            // 设置应收账款ID
            request.setReceivableId(receivableId);

            String payerAddress = authentication.getName();
            if (payerAddress == null) {
                throw new IllegalStateException("Authentication principal is null");
            }
            RepaymentRecordResponse response = receivableRepaymentService.repayDetail(request, payerAddress);

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 应收账款还款详情请求处理完成: receivableId={}, 耗时={}ms",
                    receivableId, duration);
            log.info("==================== 应收账款还款详情请求结束 ====================");

            return Result.success("还款成功", response);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 应收账款还款详情请求处理失败: receivableId={}, 耗时={}ms, error={}",
                    receivableId, duration, e.getMessage(), e);
            log.info("==================== 应收账款还款详情请求失败（结束） ====================");
            throw e;
        }
    }

    /**
     * 查询应收账款的还款记录
     * GET /api/receivable/{receivableId}/repayment-records
     */
    @GetMapping("/{receivableId}/repayment-records")
    @ApiOperation(value = "查询应收账款的还款记录", notes = "查询指定应收账款的所有还款历史记录")
    public Result<List<RepaymentRecordResponse>> getRepaymentRecords(
            @ApiParam(value = "应收账款ID", required = true) @PathVariable @NonNull String receivableId,
            Authentication authentication) {

        log.info("查询还款记录: receivableId={}", receivableId);

        String userAddress = authentication.getName();
        if (userAddress == null) {
            throw new IllegalStateException("Authentication principal is null");
        }
        List<RepaymentRecordResponse> records =
                receivableRepaymentService.getRepaymentRecords(receivableId, userAddress);

        log.info("查询到{}条还款记录", records.size());
        return Result.success(records);
    }

    /**
     * 转让应收账款
     * PUT /api/receivable/{receivableId}/transfer
     */
    @PutMapping("/{receivableId}/transfer")
    @ApiOperation(value = "转让应收账款", notes = "将应收账款转让给新的持有人")
    public Result<Void> transferReceivable(
            @ApiParam(value = "应收账款ID", required = true) @PathVariable @NonNull String receivableId,
            @ApiParam(value = "新持有人地址", required = true, example = "0x567890abcdef1234") @RequestBody String newHolder) {
        receivableService.transferReceivable(receivableId, newHolder);
        return Result.success();
    }

    /**
     * 获取应收账款信息
     * GET /api/receivable/{receivableId}
     */
    @GetMapping("/{receivableId}")
    @ApiOperation(value = "获取应收账款详情", notes = "根据应收账款ID查询详细信息")
    public Result<Receivable> getReceivable(
            @ApiParam(value = "应收账款ID", required = true) @PathVariable @NonNull String receivableId) {
        Receivable receivable = receivableService.getReceivable(receivableId);
        return Result.success(receivable);
    }

    /**
     * 获取供应商的所有应收账款
     * GET /api/receivable/supplier/{address}
     */
    @GetMapping("/supplier/{address}")
    @ApiOperation(value = "获取供应商的应收账款", notes = "查询指定供应商的所有应收账款")
    public Result<List<Receivable>> getSupplierReceivables(
            @ApiParam(value = "供应商地址", required = true) @PathVariable String address) {
        List<Receivable> receivables = receivableService.getSupplierReceivables(address);
        return Result.success(receivables);
    }

    /**
     * 获取核心企业的所有应付账款
     * GET /api/receivable/core-enterprise/{address}
     */
    @GetMapping("/core-enterprise/{address}")
    @ApiOperation(value = "获取核心企业的应付账款", notes = "查询指定核心企业的所有应付账款")
    public Result<List<Receivable>> getCoreEnterpriseReceivables(
            @ApiParam(value = "核心企业地址", required = true) @PathVariable String address) {
        List<Receivable> receivables = receivableService.getCoreEnterpriseReceivables(address);
        return Result.success(receivables);
    }

    /**
     * 获取资金方的所有融资账款
     * GET /api/receivable/financier/{address}
     */
    @GetMapping("/financier/{address}")
    @ApiOperation(value = "获取资金方的融资账款", notes = "查询指定金融机构的所有融资账款")
    public Result<List<Receivable>> getFinancierReceivables(
            @ApiParam(value = "金融机构地址", required = true) @PathVariable String address) {
        List<Receivable> receivables = receivableService.getFinancierReceivables(address);
        return Result.success(receivables);
    }

    /**
     * 获取持票人的所有应收账款
     * GET /api/receivable/holder/{address}
     */
    @GetMapping("/holder/{address}")
    @ApiOperation(value = "获取持票人的应收账款", notes = "查询当前持票人持有的所有应收账款")
    public Result<List<Receivable>> getHolderReceivables(
            @ApiParam(value = "持票人地址", required = true) @PathVariable String address) {
        List<Receivable> receivables = receivableService.getHolderReceivables(address);
        return Result.success(receivables);
    }

    /**
     * 根据状态查询应收账款
     * GET /api/receivable/status/{status}
     */
    @GetMapping("/status/{status}")
    @ApiOperation(value = "按状态查询应收账款", notes = "根据状态查询应收账款列表")
    public Result<List<Receivable>> getReceivablesByStatus(
            @ApiParam(value = "应收账款状态", required = true, example = "FINANCED") @PathVariable Receivable.ReceivableStatus status) {
        List<Receivable> receivables = receivableService.getReceivablesByStatus(status);
        return Result.success(receivables);
    }

    // ==================== 查询历史接口 ====================

    /**
     * 查询应收账款的转让历史
     * GET /api/receivable/{receivableId}/transfer-history
     */
    @GetMapping("/{receivableId}/transfer-history")
    @ApiOperation(value = "查询应收账款的转让历史", notes = "查询指定应收账款的所有转让记录")
    public Result<List<TransferHistoryResponse>> getTransferHistory(
            @ApiParam(value = "应收账款ID", required = true) @PathVariable @NonNull String receivableId,
            Authentication authentication) {

        log.info("查询转让历史: receivableId={}", receivableId);

        String userAddress = authentication.getName();
        if (userAddress == null) {
            throw new IllegalStateException("Authentication principal is null");
        }
        List<TransferHistoryResponse> history =
                receivableQueryService.getTransferHistory(receivableId, userAddress);

        log.info("查询到{}条转让记录", history.size());
        return Result.success(history);
    }

    /**
     * 查询应收账款的融资记录
     * GET /api/receivable/{receivableId}/finance-records
     */
    @GetMapping("/{receivableId}/finance-records")
    @ApiOperation(value = "查询应收账款的融资记录", notes = "查询指定应收账款的融资历史记录")
    public Result<List<FinanceRecordResponse>> getFinanceRecords(
            @ApiParam(value = "应收账款ID", required = true) @PathVariable @NonNull String receivableId,
            Authentication authentication) {

        log.info("查询融资记录: receivableId={}", receivableId);

        String userAddress = authentication.getName();
        if (userAddress == null) {
            throw new IllegalStateException("Authentication principal is null");
        }
        List<FinanceRecordResponse> records =
                receivableQueryService.getFinanceRecords(receivableId, userAddress);

        log.info("查询到{}条融资记录", records.size());
        return Result.success(records);
    }

    // ==================== 统计分析接口 ====================

    /**
     * 查询应收账款统计
     * GET /api/receivable/statistics
     */
    @GetMapping("/statistics")
    @ApiOperation(value = "查询应收账款统计", notes = "查询应收账款的多维度统计数据（只能查询自己相关的数据）")
    public Result<ReceivableStatisticsResponse> getStatistics(
            @ApiParam(value = "供应商地址（可选）") @RequestParam(required = false) String supplierAddress,
            @ApiParam(value = "核心企业地址（可选）") @RequestParam(required = false) String coreEnterpriseAddress,
            @ApiParam(value = "资金方地址（可选）") @RequestParam(required = false) String financierAddress,
            Authentication authentication) {

        log.info("查询应收账款统计: supplier={}, core={}, financier={}",
                supplierAddress, coreEnterpriseAddress, financierAddress);

        // 权限验证：用户只能查询自己相关的统计数据
        String userAddress = authentication.getName();

        // 验证用户不能查询其他用户的数据
        if (supplierAddress != null && !supplierAddress.equals(userAddress)) {
            log.warn("用户{}试图查询供应商{}的统计数据，拒绝访问", userAddress, supplierAddress);
            throw new com.fisco.app.exception.BusinessException("无权限查询其他供应商的统计数据");
        }
        if (coreEnterpriseAddress != null && !coreEnterpriseAddress.equals(userAddress)) {
            log.warn("用户{}试图查询核心企业{}的统计数据，拒绝访问", userAddress, coreEnterpriseAddress);
            throw new com.fisco.app.exception.BusinessException("无权限查询其他核心企业的统计数据");
        }
        if (financierAddress != null && !financierAddress.equals(userAddress)) {
            log.warn("用户{}试图查询资金方{}的统计数据，拒绝访问", userAddress, financierAddress);
            throw new com.fisco.app.exception.BusinessException("无权限查询其他资金方的统计数据");
        }

        // 如果没有指定过滤条件，默认查询当前用户相关的所有数据
        // 注意：这里假设用户有权限查看自己的数据，实际业务可能需要更复杂的权限控制
        ReceivableStatisticsResponse statistics =
                receivableQueryService.getStatistics(supplierAddress, coreEnterpriseAddress, financierAddress);

        log.info("统计完成: totalCount={}, totalAmount={}",
                statistics.getTotalCount(), statistics.getTotalAmount());
        return Result.success(statistics);
    }

    /**
     * 查询应收账款账龄分析
     * GET /api/receivable/aged
     */
    @GetMapping("/aged")
    @ApiOperation(value = "查询应收账款账龄分析", notes = "按账龄段统计应收账款（0-30天、31-60天、61-90天、90天以上）（只能查询自己相关的数据）")
    public Result<AgedAnalysisResponse> getAgedAnalysis(
            @ApiParam(value = "供应商地址（可选）") @RequestParam(required = false) String supplierAddress,
            @ApiParam(value = "核心企业地址（可选）") @RequestParam(required = false) String coreEnterpriseAddress,
            @ApiParam(value = "资金方地址（可选）") @RequestParam(required = false) String financierAddress,
            Authentication authentication) {

        log.info("查询账龄分析: supplier={}, core={}, financier={}",
                supplierAddress, coreEnterpriseAddress, financierAddress);

        // 权限验证：用户只能查询自己相关的账龄分析数据
        String userAddress = authentication.getName();

        // 验证用户不能查询其他用户的数据
        if (supplierAddress != null && !supplierAddress.equals(userAddress)) {
            log.warn("用户{}试图查询供应商{}的账龄分析，拒绝访问", userAddress, supplierAddress);
            throw new com.fisco.app.exception.BusinessException("无权限查询其他供应商的账龄分析");
        }
        if (coreEnterpriseAddress != null && !coreEnterpriseAddress.equals(userAddress)) {
            log.warn("用户{}试图查询核心企业{}的账龄分析，拒绝访问", userAddress, coreEnterpriseAddress);
            throw new com.fisco.app.exception.BusinessException("无权限查询其他核心企业的账龄分析");
        }
        if (financierAddress != null && !financierAddress.equals(userAddress)) {
            log.warn("用户{}试图查询资金方{}的账龄分析，拒绝访问", userAddress, financierAddress);
            throw new com.fisco.app.exception.BusinessException("无权限查询其他资金方的账龄分析");
        }

        AgedAnalysisResponse analysis =
                receivableQueryService.getAgedAnalysis(supplierAddress, coreEnterpriseAddress, financierAddress);

        log.info("账龄分析完成");
        return Result.success(analysis);
    }

    // ==================== 核销管理接口 ====================

    /**
     * 核销坏账
     * POST /api/receivable/{receivableId}/write-off
     */
    @PostMapping("/{receivableId}/write-off")
    @ApiOperation(value = "核销坏账", notes = "对无法回收的坏账进行财务核销处理")
    public Result<Void> writeOffBadDebt(
            @ApiParam(value = "应收账款ID", required = true) @PathVariable @NonNull String receivableId,
            @ApiParam(value = "核销信息", required = true) @Valid @RequestBody WriteOffRequest request,
            Authentication authentication) {

        log.info("==================== 接收到坏账核销请求 ====================");
        log.info("核销信息: receivableId={}, reason={}", receivableId, request.getReason());

        long startTime = System.currentTimeMillis();

        try {
            String operatorAddress = authentication.getName();
            if (operatorAddress == null) {
                throw new IllegalStateException("Authentication principal is null");
            }
            receivableQueryService.writeOffBadDebt(receivableId, request, operatorAddress);

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 坏账核销请求处理完成: receivableId={}, 耗时={}ms", receivableId, duration);
            log.info("==================== 坏账核销请求结束 ====================");

            return Result.success();

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 坏账核销请求处理失败: receivableId={}, 耗时={}ms, error={}",
                    receivableId, duration, e.getMessage(), e);
            log.info("==================== 坏账核销请求失败（结束） ====================");
            throw e;
        }
    }

    // ==================== 逾期管理接口 ====================

    /**
     * 查询逾期应收账款
     * GET /api/receivable/overdue
     */
    @GetMapping("/overdue")
    @ApiOperation(value = "查询逾期应收账款", notes = "查询所有已逾期账款，支持多维度筛选")
    public Result<OverdueQueryResponse> queryOverdueReceivables(
            @ApiParam(value = "逾期查询请求参数") @Valid OverdueQueryRequest request,
            Authentication authentication) {
        String userAddress = authentication.getName();
        if (userAddress == null) {
            throw new IllegalStateException("Authentication principal is null");
        }
        OverdueQueryResponse response = receivableOverdueService.queryOverdueReceivables(request, userAddress);
        return Result.success(response);
    }

    /**
     * 逾期催收
     * POST /api/receivable/{id}/remind
     */
    @PostMapping("/{id}/remind")
    @ApiOperation(value = "逾期催收", notes = "对逾期账款执行催收操作（邮件/短信/电话/函件/法律）")
    public Result<RemindResponse> createRemindRecord(
            @ApiParam(value = "应收账款ID", required = true) @PathVariable @NonNull String id,
            @ApiParam(value = "催收请求信息", required = true) @RequestBody @Valid RemindRequest request,
            Authentication authentication) {
        String operatorAddress = authentication.getName();
        if (operatorAddress == null) {
            throw new IllegalStateException("Authentication principal is null");
        }
        RemindResponse response = receivableOverdueService.createRemindRecord(id, request, operatorAddress);
        return Result.success("催收记录创建成功", response);
    }

    /**
     * 逾期罚息
     * POST /api/receivable/{id}/penalty
     */
    @PostMapping("/{id}/penalty")
    @ApiOperation(value = "逾期罚息计算", notes = "计算和记录逾期罚息，支持自动和手动计算")
    public Result<PenaltyCalculateResponse> calculatePenalty(
            @ApiParam(value = "应收账款ID", required = true) @PathVariable @NonNull String id,
            @ApiParam(value = "罚息计算请求", required = true) @RequestBody @Valid PenaltyCalculateRequest request) {
        PenaltyCalculateResponse response = receivableOverdueService.calculatePenalty(id, request);
        return Result.success("罚息计算成功", response);
    }

    /**
     * 查询坏账
     * GET /api/receivable/bad-debt
     */
    @GetMapping("/bad-debt")
    @ApiOperation(value = "查询坏账", notes = "查询已认定的坏账记录及统计信息")
    public Result<BadDebtQueryResponse> queryBadDebts(
            @ApiParam(value = "坏账查询请求参数") @Valid BadDebtQueryRequest request,
            Authentication authentication) {
        String userAddress = authentication.getName();
        if (userAddress == null) {
            throw new IllegalStateException("Authentication principal is null");
        }
        BadDebtQueryResponse response = receivableOverdueService.queryBadDebts(request, userAddress);
        return Result.success(response);
    }

    /**
     * 认定坏账
     * POST /api/receivable/{id}/bad-debt
     */
    @PostMapping("/{id}/bad-debt")
    @ApiOperation(value = "认定坏账", notes = "将逾期应收账款认定为坏账")
    public Result<com.fisco.app.entity.risk.BadDebtRecord> createBadDebt(
            @ApiParam(value = "应收账款ID", required = true) @PathVariable @NonNull String id,
            @RequestBody BadDebtRequest request) {
        com.fisco.app.entity.risk.BadDebtRecord record = receivableOverdueService.createBadDebt(
            id,
            request.getBadDebtType(),
            request.getBadDebtReason()
        );
        return Result.success("坏账认定成功", record);
    }

    /**
     * 坏账回收
     * POST /api/receivable/{id}/bad-debt/recover
     */
    @PostMapping("/{id}/bad-debt/recover")
    @ApiOperation(value = "坏账回收", notes = "记录坏账回收信息")
    public Result<com.fisco.app.entity.risk.BadDebtRecord> recoverBadDebt(
            @ApiParam(value = "应收账款ID", required = true) @PathVariable @NonNull String id,
            @RequestBody BadDebtRecoveryRequest request) {
        com.fisco.app.entity.risk.BadDebtRecord record = receivableOverdueService.recoverBadDebt(
            id,
            request.getRecoveredAmount(),
            request.getRecoveryStatus()
        );
        return Result.success("坏账回收记录成功", record);
    }

    /**
     * 融资请求DTO
     */
    @Schema(name = "融资请求")
    public static class FinanceRequest {
        private String financierAddress;
        private BigDecimal financeAmount;
        private Integer financeRate;

        public String getFinancierAddress() {
            return financierAddress;
        }

        public void setFinancierAddress(String financierAddress) {
            this.financierAddress = financierAddress;
        }

        public BigDecimal getFinanceAmount() {
            return financeAmount;
        }

        public void setFinanceAmount(BigDecimal financeAmount) {
            this.financeAmount = financeAmount;
        }

        public Integer getFinanceRate() {
            return financeRate;
        }

        public void setFinanceRate(Integer financeRate) {
            this.financeRate = financeRate;
        }
    }

    /**
     * 坏账认定请求DTO
     */
    @Schema(name = "坏账认定请求")
    public static class BadDebtRequest {
        @io.swagger.annotations.ApiModelProperty(value = "坏账类型", required = true, example = "OVERDUE_180")
        private com.fisco.app.entity.risk.BadDebtRecord.BadDebtType badDebtType;

        @io.swagger.annotations.ApiModelProperty(value = "坏账原因", example = "逾期180天以上，债务人失联")
        private String badDebtReason;

        public com.fisco.app.entity.risk.BadDebtRecord.BadDebtType getBadDebtType() {
            return badDebtType;
        }

        public void setBadDebtType(com.fisco.app.entity.risk.BadDebtRecord.BadDebtType badDebtType) {
            this.badDebtType = badDebtType;
        }

        public String getBadDebtReason() {
            return badDebtReason;
        }

        public void setBadDebtReason(String badDebtReason) {
            this.badDebtReason = badDebtReason;
        }
    }

    /**
     * 坏账回收请求DTO
     */
    @Schema(name = "坏账回收请求")
    public static class BadDebtRecoveryRequest {
        @io.swagger.annotations.ApiModelProperty(value = "已回收金额", required = true, example = "100000.00")
        private BigDecimal recoveredAmount;

        @io.swagger.annotations.ApiModelProperty(value = "回收状态", required = true, example = "PARTIAL_RECOVERED")
        private com.fisco.app.entity.risk.BadDebtRecord.RecoveryStatus recoveryStatus;

        public BigDecimal getRecoveredAmount() {
            return recoveredAmount;
        }

        public void setRecoveredAmount(BigDecimal recoveredAmount) {
            this.recoveredAmount = recoveredAmount;
        }

        public com.fisco.app.entity.risk.BadDebtRecord.RecoveryStatus getRecoveryStatus() {
            return recoveryStatus;
        }

        public void setRecoveryStatus(com.fisco.app.entity.risk.BadDebtRecord.RecoveryStatus recoveryStatus) {
            this.recoveryStatus = recoveryStatus;
        }
    }

    /**
     * POST /api/receivable/{receivableId}/split
     * 拆分应收账款
     */
    @PostMapping("/{receivableId}/split")
    @ApiOperation(value = "拆分应收账款", notes = "将一笔应收账款拆分为多笔")
    @SuppressWarnings("null")
    public Result<ReceivableSplitResponse> splitReceivable(
            @ApiParam(value = "应收账款ID", required = true) @PathVariable @NonNull String receivableId,
            @Valid @RequestBody ReceivableSplitRequest request,
            Authentication authentication) {
        log.info("==================== 接收到应收账款拆分请求 ====================");
        log.info("应收账款ID: {}, 拆分数量: {}", receivableId, request.getSplitCount());

        long startTime = System.currentTimeMillis();

        try {
            // 设置应收账款ID
            request.setReceivableId(receivableId);

            if (authentication == null || authentication.getName() == null) {
                throw new IllegalStateException("用户未认证");
            }
            String applicantId = authentication.getName();
            ReceivableSplitResponse response = receivableService.splitReceivable(request, applicantId);

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 应收账款拆分请求处理完成: receivableId={}, splitCount={}, 耗时={}ms",
                    receivableId, response.getSplitCount(), duration);
            log.info("==================== 应收账款拆分请求结束 ====================");

            return Result.success("应收账款拆分申请提交成功", response);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 应收账款拆分请求处理失败: receivableId={}, 耗时={}ms, error={}",
                    receivableId, duration, e.getMessage(), e);
            log.info("==================== 应收账款拆分请求失败（结束） ====================");
            throw e;
        }
    }

    /**
     * POST /api/receivable/merge
     * 合并应收账款
     */
    @PostMapping("/merge")
    @ApiOperation(value = "合并应收账款", notes = "将多笔应收账款合并为一笔")
    public Result<ReceivableMergeResponse> mergeReceivables(
            @Valid @RequestBody ReceivableMergeRequest request,
            Authentication authentication) {
        log.info("==================== 接收到应收账款合并请求 ====================");
        log.info("应收账款数量: {}", request.getReceivableIds().size());

        long startTime = System.currentTimeMillis();

        try {
            ReceivableMergeResponse response = receivableService.mergeReceivables(request, authentication.getName());

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 应收账款合并请求处理完成: mergeCount={}, totalAmount={}, 耗时={}ms",
                    response.getMergeCount(), response.getTotalAmount(), duration);
            log.info("==================== 应收账款合并请求结束 ====================");

            return Result.success("应收账款合并申请提交成功", response);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 应收账款合并请求处理失败: 耗时={}ms, error={}", duration, e.getMessage(), e);
            log.info("==================== 应收账款合并请求失败（结束） ====================");
            throw e;
        }
    }

    /**
     * PUT /api/receivable/split/{receivableId}/approve
     * 审批应收账款拆分
     */
    @PutMapping("/split/{receivableId}/approve")
    @ApiOperation(value = "审批应收账款拆分", notes = "审批通过或拒绝应收账款拆分申请")
    @SuppressWarnings("null")
    public Result<Void> approveSplit(
            @ApiParam(value = "应收账款ID", required = true) @PathVariable @NonNull String receivableId,
            @RequestBody SplitApprovalRequest request,
            Authentication authentication) {
        log.info("==================== 接收到应收账款拆分审批请求 ====================");
        log.info("应收账款ID: {}, approved={}", receivableId, request.isApproved());

        long startTime = System.currentTimeMillis();

        try {
            if (authentication == null || authentication.getName() == null) {
                throw new IllegalStateException("用户未认证");
            }
            String approverId = authentication.getName();
            String reason = request.getReason() != null ? request.getReason() : "";
            receivableService.approveSplit(receivableId, request.isApproved(), approverId, reason);

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 应收账款拆分审批请求处理完成: receivableId={}, approved={}, 耗时={}ms",
                    receivableId, request.isApproved(), duration);
            log.info("==================== 应收账款拆分审批请求结束 ====================");

            return Result.success();

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 应收账款拆分审批请求处理失败: receivableId={}, 耗时={}ms, error={}",
                    receivableId, duration, e.getMessage(), e);
            log.info("==================== 应收账款拆分审批请求失败（结束） ====================");
            throw e;
        }
    }

    /**
     * PUT /api/receivable/merge/{mergedReceivableId}/approve
     * 审批应收账款合并
     */
    @PutMapping("/merge/{mergedReceivableId}/approve")
    @ApiOperation(value = "审批应收账款合并", notes = "审批通过或拒绝应收账款合并申请")
    @SuppressWarnings("null")
    public Result<Void> approveMerge(
            @ApiParam(value = "合并后的应收账款ID", required = true) @PathVariable @NonNull String mergedReceivableId,
            @RequestBody MergeApprovalRequest request,
            Authentication authentication) {
        log.info("==================== 接收到应收账款合并审批请求 ====================");
        log.info("合并后的应收账款ID: {}, approved={}", mergedReceivableId, request.isApproved());

        long startTime = System.currentTimeMillis();

        try {
            if (authentication == null || authentication.getName() == null) {
                throw new IllegalStateException("用户未认证");
            }
            String approverId = authentication.getName();
            String reason = request.getReason() != null ? request.getReason() : "";
            receivableService.approveMerge(mergedReceivableId, request.isApproved(), approverId, reason);

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 应收账款合并审批请求处理完成: mergedReceivableId={}, approved={}, 耗时={}ms",
                    mergedReceivableId, request.isApproved(), duration);
            log.info("==================== 应收账款合并审批请求结束 ====================");

            return Result.success();

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 应收账款合并审批请求处理失败: mergedReceivableId={}, 耗时={}ms, error={}",
                    mergedReceivableId, duration, e.getMessage(), e);
            log.info("==================== 应收账款合并审批请求失败（结束） ====================");
            throw e;
        }
    }

    /**
     * 拆分审批请求DTO
     */
    @Data
    @Schema(name = "拆分审批请求")
    public static class SplitApprovalRequest {
        @io.swagger.annotations.ApiModelProperty(value = "是否通过", required = true, example = "true")
        private boolean approved;

        @io.swagger.annotations.ApiModelProperty(value = "审批意见/拒绝原因", example = "拆分合理")
        private String reason;
    }

    /**
     * 合并审批请求DTO
     */
    @Data
    @Schema(name = "合并审批请求")
    public static class MergeApprovalRequest {
        @io.swagger.annotations.ApiModelProperty(value = "是否通过", required = true, example = "true")
        private boolean approved;

        @io.swagger.annotations.ApiModelProperty(value = "审批意见/拒绝原因", example = "合并符合规范")
        private String reason;
    }
}
