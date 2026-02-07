package com.fisco.app.controller;

import com.fisco.app.dto.CreateReceivableRequest;
import com.fisco.app.dto.*;
import com.fisco.app.entity.Receivable;
import com.fisco.app.service.ReceivableService;
import com.fisco.app.service.ReceivableOverdueService;
import com.fisco.app.vo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.lang.NonNull;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.List;

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
    public Result<com.fisco.app.entity.BadDebtRecord> createBadDebt(
            @ApiParam(value = "应收账款ID", required = true) @PathVariable @NonNull String id,
            @RequestBody BadDebtRequest request) {
        com.fisco.app.entity.BadDebtRecord record = receivableOverdueService.createBadDebt(
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
    public Result<com.fisco.app.entity.BadDebtRecord> recoverBadDebt(
            @ApiParam(value = "应收账款ID", required = true) @PathVariable @NonNull String id,
            @RequestBody BadDebtRecoveryRequest request) {
        com.fisco.app.entity.BadDebtRecord record = receivableOverdueService.recoverBadDebt(
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
        private com.fisco.app.entity.BadDebtRecord.BadDebtType badDebtType;

        @io.swagger.annotations.ApiModelProperty(value = "坏账原因", example = "逾期180天以上，债务人失联")
        private String badDebtReason;

        public com.fisco.app.entity.BadDebtRecord.BadDebtType getBadDebtType() {
            return badDebtType;
        }

        public void setBadDebtType(com.fisco.app.entity.BadDebtRecord.BadDebtType badDebtType) {
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
        private com.fisco.app.entity.BadDebtRecord.RecoveryStatus recoveryStatus;

        public BigDecimal getRecoveredAmount() {
            return recoveredAmount;
        }

        public void setRecoveredAmount(BigDecimal recoveredAmount) {
            this.recoveredAmount = recoveredAmount;
        }

        public com.fisco.app.entity.BadDebtRecord.RecoveryStatus getRecoveryStatus() {
            return recoveryStatus;
        }

        public void setRecoveryStatus(com.fisco.app.entity.BadDebtRecord.RecoveryStatus recoveryStatus) {
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
