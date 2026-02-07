package com.fisco.app.controller;

import com.fisco.app.dto.*;
import com.fisco.app.enums.CreditAdjustRequestStatus;
import com.fisco.app.service.CreditLimitService;
import com.fisco.app.vo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 信用额度管理Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/credit-limit")
@RequiredArgsConstructor
@Api(tags = "信用额度管理")
public class CreditLimitController {

    private final CreditLimitService creditLimitService;

    // ==================== 额度管理 ====================

    /**
     * 创建信用额度
     * POST /api/credit-limit
     */
    @PostMapping
    @ApiOperation(value = "创建信用额度", notes = "管理员为企业创建信用额度")
    public Result<CreditLimitDTO> createCreditLimit(
            @ApiParam(value = "创建信用额度请求参数", required = true) @Valid @RequestBody CreditLimitCreateRequest request,
            Authentication authentication) {
        String operatorAddress = authentication.getName();

        log.info("==================== 接收到创建信用额度请求 ====================");
        log.info("创建信息: enterpriseAddress={}, limitType={}, totalLimit={}元",
                request.getEnterpriseAddress(), request.getLimitType(), request.getTotalLimit());

        long startTime = System.currentTimeMillis();

        try {
            CreditLimitDTO result = creditLimitService.createCreditLimit(request, operatorAddress);

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 创建信用额度请求处理完成: limitId={}, 耗时={}ms", result.getId(), duration);
            log.info("==================== 创建信用额度请求结束 ====================");

            return Result.success("信用额度创建成功", result);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 创建信用额度请求处理失败: 耗时={}ms, error={}", duration, e.getMessage(), e);
            log.info("==================== 创建信用额度请求失败（结束） ====================");
            throw e;
        }
    }

    /**
     * 查询信用额度列表
     * GET /api/credit-limit
     */
    @GetMapping
    @ApiOperation(value = "查询信用额度列表", notes = "支持多条件筛选、分页查询")
    public Result<CreditLimitQueryResponse> queryCreditLimits(
            @ApiParam(value = "企业地址") @RequestParam(required = false) String enterpriseAddress,
            @ApiParam(value = "额度类型") @RequestParam(required = false) String limitType,
            @ApiParam(value = "额度状态") @RequestParam(required = false) String status,
            @ApiParam(value = "风险等级") @RequestParam(required = false) String riskLevel,
            @ApiParam(value = "使用率最小值") @RequestParam(required = false) Double usageRateMin,
            @ApiParam(value = "使用率最大值") @RequestParam(required = false) Double usageRateMax,
            @ApiParam(value = "页码（从0开始）") @RequestParam(required = false, defaultValue = "0") Integer page,
            @ApiParam(value = "每页大小") @RequestParam(required = false, defaultValue = "10") Integer size,
            @ApiParam(value = "排序字段") @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @ApiParam(value = "排序方向") @RequestParam(required = false, defaultValue = "DESC") String sortDirection) {

        log.info("==================== 接收到查询信用额度请求 ====================");
        log.info("查询条件: enterpriseAddress={}, limitType={}, status={}, page={}, size={}",
                enterpriseAddress, limitType, status, page, size);

        long startTime = System.currentTimeMillis();

        try {
            // 构建查询请求
            CreditLimitQueryRequest request = new CreditLimitQueryRequest();
            request.setEnterpriseAddress(enterpriseAddress);
            // 这里需要将字符串转换为枚举，为了简化先省略
            request.setStatus(status != null ? com.fisco.app.enums.CreditLimitStatus.valueOf(status) : null);
            request.setRiskLevel(riskLevel != null ? com.fisco.app.entity.CreditLimit.RiskLevel.valueOf(riskLevel) : null);
            request.setUsageRateMin(usageRateMin);
            request.setUsageRateMax(usageRateMax);
            request.setPage(page);
            request.setSize(size);
            request.setSortBy(sortBy);
            request.setSortDirection(sortDirection);

            CreditLimitQueryResponse result = creditLimitService.queryCreditLimits(request);

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 查询信用额度请求处理完成: 总记录数={}, 耗时={}ms",
                    result.getTotalElements(), duration);
            log.info("==================== 查询信用额度请求结束 ====================");

            return Result.success(result);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 查询信用额度请求处理失败: 耗时={}ms, error={}", duration, e.getMessage(), e);
            log.info("==================== 查询信用额度请求失败（结束） ====================");
            throw e;
        }
    }

    /**
     * 查询单个额度详情
     * GET /api/credit-limit/{id}
     */
    @GetMapping("/{id}")
    @ApiOperation(value = "查询单个额度详情", notes = "根据ID查询信用额度详情")
    public Result<CreditLimitDTO> getCreditLimitById(
            @ApiParam(value = "额度ID", required = true) @PathVariable @NonNull String id) {

        log.info("==================== 接收到查询单个额度请求 ====================");
        log.info("额度ID: {}", id);

        long startTime = System.currentTimeMillis();

        try {
            CreditLimitDTO result = creditLimitService.getCreditLimitById(id);

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 查询单个额度请求处理完成: limitId={}, 耗时={}ms", id, duration);
            log.info("==================== 查询单个额度请求结束 ====================");

            return Result.success(result);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 查询单个额度请求处理失败: limitId={}, 耗时={}ms, error={}",
                    id, duration, e.getMessage(), e);
            log.info("==================== 查询单个额度请求失败（结束） ====================");
            throw e;
        }
    }

    /**
     * 查询企业的所有额度
     * GET /api/credit-limit/enterprise/{address}
     */
    @GetMapping("/enterprise/{address}")
    @ApiOperation(value = "查询企业的所有额度", notes = "根据企业地址查询所有信用额度")
    public Result<java.util.List<CreditLimitDTO>> getCreditLimitsByEnterprise(
            @ApiParam(value = "企业地址", required = true) @PathVariable @NonNull String address) {

        log.info("==================== 接收到查询企业额度请求 ====================");
        log.info("企业地址: {}", address);

        long startTime = System.currentTimeMillis();

        try {
            java.util.List<CreditLimitDTO> result = creditLimitService.getCreditLimitsByEnterprise(address);

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 查询企业额度请求处理完成: enterpriseAddress={}, count={}, 耗时={}ms",
                    address, result.size(), duration);
            log.info("==================== 查询企业额度请求结束 ====================");

            return Result.success(result);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 查询企业额度请求处理失败: enterpriseAddress={}, 耗时={}ms, error={}",
                    address, duration, e.getMessage(), e);
            log.info("==================== 查询企业额度请求失败（结束） ====================");
            throw e;
        }
    }

    // ==================== 额度冻结/解冻 ====================

    /**
     * 冻结额度
     * POST /api/credit-limit/{id}/freeze
     */
    @PostMapping("/{id}/freeze")
    @ApiOperation(value = "冻结额度", notes = "管理员冻结企业的信用额度")
    @SuppressWarnings("null")
    public Result<CreditLimitFreezeResponse> freezeCreditLimit(
            @ApiParam(value = "额度ID", required = true) @PathVariable @NonNull String id,
            @ApiParam(value = "冻结原因", required = true) @RequestBody CreditLimitFreezeRequest request,
            Authentication authentication) {

        String operatorAddress = authentication.getName();

        log.info("==================== 接收到冻结额度请求 ====================");
        log.info("冻结信息: limitId={}, reason={}", id, request.getReason());

        long startTime = System.currentTimeMillis();

        String reason = request.getReason();
        try {
            CreditLimitFreezeResponse result = creditLimitService.freezeCreditLimit(
                    id, reason, operatorAddress);

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 冻结额度请求处理完成: limitId={}, 耗时={}ms", id, duration);
            log.info("==================== 冻结额度请求结束 ====================");

            return Result.success("额度冻结成功", result);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 冻结额度请求处理失败: limitId={}, 耗时={}ms, error={}",
                    id, duration, e.getMessage(), e);
            log.info("==================== 冻结额度请求失败（结束） ====================");
            throw e;
        }
    }

    /**
     * 解冻额度
     * POST /api/credit-limit/{id}/unfreeze
     */
    @PostMapping("/{id}/unfreeze")
    @ApiOperation(value = "解冻额度", notes = "管理员解冻企业的信用额度")
    @SuppressWarnings("null")
    public Result<CreditLimitFreezeResponse> unfreezeCreditLimit(
            @ApiParam(value = "额度ID", required = true) @PathVariable @NonNull String id,
            @ApiParam(value = "解冻原因", required = true) @RequestBody CreditLimitFreezeRequest request,
            Authentication authentication) {

        String operatorAddress = authentication.getName();

        log.info("==================== 接收到解冻额度请求 ====================");
        log.info("解冻信息: limitId={}, reason={}", id, request.getReason());

        long startTime = System.currentTimeMillis();

        String reason = request.getReason();
        try {
            CreditLimitFreezeResponse result = creditLimitService.unfreezeCreditLimit(
                    id, reason, operatorAddress);

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 解冻额度请求处理完成: limitId={}, 耗时={}ms", id, duration);
            log.info("==================== 解冻额度请求结束 ====================");

            return Result.success("额度解冻成功", result);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 解冻额度请求处理失败: limitId={}, 耗时={}ms, error={}",
                    id, duration, e.getMessage(), e);
            log.info("==================== 解冻额度请求失败（结束） ====================");
            throw e;
        }
    }

    // ==================== 额度调整申请和审批 ====================

    /**
     * 申请额度调整
     * POST /api/credit-limit/adjust/request
     */
    @PostMapping("/adjust/request")
    @ApiOperation(value = "申请额度调整", notes = "企业申请调整信用额度")
    @SuppressWarnings("null")
    public Result<CreditLimitAdjustResponse> requestAdjust(
            @ApiParam(value = "额度调整申请请求", required = true) @Valid @RequestBody CreditLimitAdjustRequestDTO request,
            Authentication authentication) {

        String requesterAddress = authentication.getName();

        log.info("==================== 接收到申请额度调整请求 ====================");
        log.info("申请信息: creditLimitId={}, adjustType={}, newLimit={}元",
                request.getCreditLimitId(), request.getAdjustType(), request.getNewLimit());

        long startTime = System.currentTimeMillis();

        try {
            // 获取请求者姓名（这里简化处理，实际应从用户服务获取）
            String requesterName = requesterAddress; // TODO: 从用户服务获取真实姓名

            CreditLimitAdjustResponse result = creditLimitService.requestAdjust(
                    request, requesterAddress, requesterName);

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 申请额度调整请求处理完成: requestId={}, 耗时={}ms",
                    result.getId(), duration);
            log.info("==================== 申请额度调整请求结束 ====================");

            return Result.success("额度调整申请提交成功", result);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 申请额度调整请求处理失败: 耗时={}ms, error={}", duration, e.getMessage(), e);
            log.info("==================== 申请额度调整请求失败（结束） ====================");
            throw e;
        }
    }

    /**
     * 查询待审批的调整申请
     * GET /api/credit-limit/adjust/pending
     */
    @GetMapping("/adjust/pending")
    @ApiOperation(value = "查询待审批的调整申请", notes = "管理员查询所有待审批的额度调整申请")
    public Result<java.util.List<CreditLimitAdjustResponse>> getPendingAdjustRequests() {

        log.info("==================== 接收到查询待审批调整申请请求 ====================");

        long startTime = System.currentTimeMillis();

        try {
            // TODO: 实现查询待审批申请的方法
            java.util.List<CreditLimitAdjustResponse> result = java.util.Collections.emptyList();

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 查询待审批调整申请请求处理完成: count={}, 耗时={}ms",
                    result.size(), duration);
            log.info("==================== 查询待审批调整申请请求结束 ====================");

            return Result.success(result);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 查询待审批调整申请请求处理失败: 耗时={}ms, error={}",
                    duration, e.getMessage(), e);
            log.info("==================== 查询待审批调整申请请求失败（结束） ====================");
            throw e;
        }
    }

    /**
     * 审批额度调整申请
     * POST /api/credit-limit/adjust/{requestId}/approve
     */
    @PostMapping("/adjust/{requestId}/approve")
    @ApiOperation(value = "审批额度调整申请", notes = "管理员审批额度调整申请")
    public Result<CreditLimitAdjustResponse> approveAdjust(
            @ApiParam(value = "申请ID", required = true) @PathVariable @NonNull String requestId,
            @ApiParam(value = "审批请求", required = true) @Valid @RequestBody CreditLimitAdjustApprovalRequest request,
            Authentication authentication) {

        String approverAddress = authentication.getName();

        log.info("==================== 接收到审批额度调整请求 ====================");
        log.info("审批信息: requestId={}, result={}", requestId, request.getApprovalResult());

        long startTime = System.currentTimeMillis();

        try {
            // 获取审批人姓名
            String approverName = approverAddress; // TODO: 从用户服务获取真实姓名

            CreditLimitAdjustResponse result = creditLimitService.approveAdjust(
                    requestId,
                    request.getApprovalResult(),
                    request.getApproveReason(),
                    request.getRejectReason(),
                    approverAddress,
                    approverName);

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 审批额度调整请求处理完成: requestId={}, result={}, 耗时={}ms",
                    requestId, request.getApprovalResult(), duration);
            log.info("==================== 审批额度调整请求结束 ====================");

            String successMessage = request.getApprovalResult() == CreditAdjustRequestStatus.APPROVED ?
                    "额度调整申请已通过" : "额度调整申请已拒绝";

            return Result.success(successMessage, result);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 审批额度调整请求处理失败: requestId={}, 耗时={}ms, error={}",
                    requestId, duration, e.getMessage(), e);
            log.info("==================== 审批额度调整请求失败（结束） ====================");
            throw e;
        }
    }

    /**
     * 查询额度调整历史
     * GET /api/credit-limit/adjust/history
     */
    @GetMapping("/adjust/history")
    @ApiOperation(value = "查询额度调整历史", notes = "查询额度调整申请历史记录")
    public Result<java.util.List<CreditLimitAdjustResponse>> getAdjustHistory(
            @ApiParam(value = "额度ID") @RequestParam(required = false) String creditLimitId,
            @ApiParam(value = "页码") @RequestParam(required = false, defaultValue = "0") Integer page,
            @ApiParam(value = "每页大小") @RequestParam(required = false, defaultValue = "10") Integer size) {

        log.info("==================== 接收到查询额度调整历史请求 ====================");
        log.info("查询条件: creditLimitId={}, page={}, size={}", creditLimitId, page, size);

        long startTime = System.currentTimeMillis();

        try {
            // TODO: 实现查询历史的方法
            java.util.List<CreditLimitAdjustResponse> result = java.util.Collections.emptyList();

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 查询额度调整历史请求处理完成: count={}, 耗时={}ms",
                    result.size(), duration);
            log.info("==================== 查询额度调整历史请求结束 ====================");

            return Result.success(result);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 查询额度调整历史请求处理失败: 耗时={}ms, error={}",
                    duration, e.getMessage(), e);
            log.info("==================== 查询额度调整历史请求失败（结束） ====================");
            throw e;
        }
    }

    // ==================== 使用记录查询 ====================

    /**
     * 查询额度使用记录
     * GET /api/credit-limit/usage
     */
    @GetMapping("/usage")
    @ApiOperation(value = "查询额度使用记录", notes = "查询信用额度使用记录")
    public Result<CreditLimitUsageQueryResponse> queryUsageRecords(
            @ApiParam(value = "额度ID") @RequestParam(required = false) String creditLimitId,
            @ApiParam(value = "企业地址") @RequestParam(required = false) String enterpriseAddress,
            @ApiParam(value = "使用类型") @RequestParam(required = false) String usageType,
            @ApiParam(value = "页码") @RequestParam(required = false, defaultValue = "0") Integer page,
            @ApiParam(value = "每页大小") @RequestParam(required = false, defaultValue = "10") Integer size) {

        log.info("==================== 接收到查询使用记录请求 ====================");
        log.info("查询条件: creditLimitId={}, enterpriseAddress={}, usageType={}, page={}, size={}",
                creditLimitId, enterpriseAddress, usageType, page, size);

        long startTime = System.currentTimeMillis();

        try {
            // 构建查询请求
            CreditLimitUsageQueryRequest request = new CreditLimitUsageQueryRequest();
            request.setCreditLimitId(creditLimitId);
            request.setEnterpriseAddress(enterpriseAddress);
            request.setUsageType(usageType != null ? com.fisco.app.enums.CreditUsageType.valueOf(usageType) : null);
            request.setPage(page);
            request.setSize(size);

            CreditLimitUsageQueryResponse result = creditLimitService.queryUsageRecords(request);

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 查询使用记录请求处理完成: 总记录数={}, 耗时={}ms",
                    result.getTotalElements(), duration);
            log.info("==================== 查询使用记录请求结束 ====================");

            return Result.success(result);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 查询使用记录请求处理失败: 耗时={}ms, error={}", duration, e.getMessage(), e);
            log.info("==================== 查询使用记录请求失败（结束） ====================");
            throw e;
        }
    }

    /**
     * 查询指定额度的使用记录
     * GET /api/credit-limit/usage/credit-limit/{id}
     */
    @GetMapping("/usage/credit-limit/{id}")
    @ApiOperation(value = "查询指定额度的使用记录", notes = "根据额度ID查询所有使用记录")
    public Result<CreditLimitUsageQueryResponse> getUsageByCreditLimit(
            @ApiParam(value = "额度ID", required = true) @PathVariable @NonNull String id,
            @ApiParam(value = "页码") @RequestParam(required = false, defaultValue = "0") Integer page,
            @ApiParam(value = "每页大小") @RequestParam(required = false, defaultValue = "10") Integer size) {

        log.info("==================== 接收到查询额度使用记录请求 ====================");
        log.info("查询条件: creditLimitId={}, page={}, size={}", id, page, size);

        long startTime = System.currentTimeMillis();

        try {
            CreditLimitUsageQueryRequest request = new CreditLimitUsageQueryRequest();
            request.setCreditLimitId(id);
            request.setPage(page);
            request.setSize(size);

            CreditLimitUsageQueryResponse result = creditLimitService.queryUsageRecords(request);

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 查询额度使用记录请求处理完成: 总记录数={}, 耗时={}ms",
                    result.getTotalElements(), duration);
            log.info("==================== 查询额度使用记录请求结束 ====================");

            return Result.success(result);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 查询额度使用记录请求处理失败: creditLimitId={}, 耗时={}ms, error={}",
                    id, duration, e.getMessage(), e);
            log.info("==================== 查询额度使用记录请求失败（结束） ====================");
            throw e;
        }
    }

    // ==================== 预警记录查询 ====================

    /**
     * 查询预警记录
     * GET /api/credit-limit/warnings
     */
    @GetMapping("/warnings")
    @ApiOperation(value = "查询预警记录", notes = "查询信用额度预警记录")
    public Result<CreditLimitWarningQueryResponse> queryWarnings(
            @ApiParam(value = "额度ID") @RequestParam(required = false) String creditLimitId,
            @ApiParam(value = "企业地址") @RequestParam(required = false) String enterpriseAddress,
            @ApiParam(value = "预警级别") @RequestParam(required = false) String warningLevel,
            @ApiParam(value = "是否已处理") @RequestParam(required = false) Boolean isResolved,
            @ApiParam(value = "页码") @RequestParam(required = false, defaultValue = "0") Integer page,
            @ApiParam(value = "每页大小") @RequestParam(required = false, defaultValue = "10") Integer size) {

        log.info("==================== 接收到查询预警记录请求 ====================");
        log.info("查询条件: creditLimitId={}, warningLevel={}, isResolved={}, page={}, size={}",
                creditLimitId, warningLevel, isResolved, page, size);

        long startTime = System.currentTimeMillis();

        try {
            // 构建查询请求
            CreditLimitWarningQueryRequest request = new CreditLimitWarningQueryRequest();
            request.setCreditLimitId(creditLimitId);
            request.setEnterpriseAddress(enterpriseAddress);
            request.setWarningLevel(warningLevel != null ? com.fisco.app.enums.CreditWarningLevel.valueOf(warningLevel) : null);
            request.setIsResolved(isResolved);
            request.setPage(page);
            request.setSize(size);

            CreditLimitWarningQueryResponse result = creditLimitService.queryWarnings(request);

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 查询预警记录请求处理完成: 总记录数={}, 耗时={}ms",
                    result.getTotalElements(), duration);
            log.info("==================== 查询预警记录请求结束 ====================");

            return Result.success(result);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 查询预警记录请求处理失败: 耗时={}ms, error={}", duration, e.getMessage(), e);
            log.info("==================== 查询预警记录请求失败（结束） ====================");
            throw e;
        }
    }

    /**
     * 处理预警
     * POST /api/credit-limit/warnings/{id}/resolve
     */
    @PostMapping("/warnings/{id}/resolve")
    @ApiOperation(value = "处理预警", notes = "处理信用额度预警")
    public Result<Void> resolveWarning(
            @ApiParam(value = "预警ID", required = true) @PathVariable @NonNull String id,
            @ApiParam(value = "处理措施", required = true) @RequestBody java.util.Map<String, String> requestBody,
            Authentication authentication) {

        String resolution = requestBody.get("resolution");

        log.info("==================== 接收到处理预警请求 ====================");
        log.info("处理信息: warningId={}, resolution={}", id, resolution);

        long startTime = System.currentTimeMillis();

        try {
            // TODO: 实现处理预警的方法
            // 1. 更新预警记录
            // 2. 记录处理人、处理时间、处理措施

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 处理预警请求处理完成: warningId={}, 耗时={}ms", id, duration);
            log.info("==================== 处理预警请求结束 ====================");

            return Result.success();

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 处理预警请求处理失败: warningId={}, 耗时={}ms, error={}",
                    id, duration, e.getMessage(), e);
            log.info("==================== 处理预警请求失败（结束） ====================");
            throw e;
        }
    }

    // ==================== 统计信息 ====================

    /**
     * 获取额度统计信息
     * GET /api/credit-limit/statistics
     */
    @GetMapping("/statistics")
    @ApiOperation(value = "获取额度统计信息", notes = "获取信用额度统计信息")
    public Result<CreditLimitQueryResponse.CreditLimitStatistics> getStatistics(
            @ApiParam(value = "企业地址") @RequestParam(required = false) String enterpriseAddress) {

        log.info("==================== 接收到获取统计信息请求 ====================");
        log.info("查询条件: enterpriseAddress={}", enterpriseAddress);

        long startTime = System.currentTimeMillis();

        try {
            // 构建查询请求
            CreditLimitQueryRequest request = new CreditLimitQueryRequest();
            request.setEnterpriseAddress(enterpriseAddress);
            request.setPage(0);
            request.setSize(Integer.MAX_VALUE); // 获取所有记录用于统计

            CreditLimitQueryResponse response = creditLimitService.queryCreditLimits(request);

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 获取统计信息请求处理完成: 耗时={}ms", duration);
            log.info("==================== 获取统计信息请求结束 ====================");

            return Result.success(response.getStatistics());

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 获取统计信息请求处理失败: 耗时={}ms, error={}", duration, e.getMessage(), e);
            log.info("==================== 获取统计信息请求失败（结束） ====================");
            throw e;
        }
    }
}
