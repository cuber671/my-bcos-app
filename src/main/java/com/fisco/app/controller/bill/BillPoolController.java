package com.fisco.app.controller.bill;

import java.util.List;

import javax.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fisco.app.dto.bill.BillInvestRequest;
import com.fisco.app.dto.bill.BillInvestResponse;
import com.fisco.app.dto.bill.BillPoolFilter;
import com.fisco.app.dto.bill.BillPoolView;
import com.fisco.app.service.bill.BillPoolService;
import com.fisco.app.vo.Result;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 票据池管理Controller
 *
 * 提供票据池查询、票据投资等功能
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-03
 */
@Slf4j
@RestController
@RequestMapping("/api/bill/pool")
@RequiredArgsConstructor
@Api(tags = "票据池管理")
public class BillPoolController {

    private final BillPoolService billPoolService;

    /**
     * 查询票据池
     *
     * 功能说明：
     * - 查询所有可投资的票据
     * - 支持多维度筛选和排序
     * - 支持分页查询
     *
     * 权限要求：
     * - 已登录用户
     * - 金融机构可查看完整信息
     * - 普通企业只能查看基础信息
     *
     * @param billType 票据类型（可选）
     * @param minAmount 最小面值（可选）
     * @param maxAmount 最大面值（可选）
     * @param minRemainingDays 最小剩余天数（可选）
     * @param maxRemainingDays 最大剩余天数（可选）
     * @param holderId 持票人ID（可选）
     * @param page 页码（从0开始，默认0）
     * @param size 每页大小（默认20）
     * @param sortBy 排序字段（默认remainingDays）
     * @param sortOrder 排序方向（默认ASC）
     * @return 票据池分页结果
     */
    @GetMapping
    @ApiOperation(value = "查询票据池",
                  notes = "查询所有可投资的票据，支持筛选、排序和分页。" +
                          "票据池包含所有状态为NORMAL、已上链、未冻结、未过期的票据。" +
                          "金融机构可查看完整信息，普通企业只能查看基础信息。")
    public Result<Page<BillPoolView>> getBillPool(
            @ApiParam(value = "票据类型", example = "BANK_ACCEPTANCE_BILL")
            @RequestParam(required = false) String billType,

            @ApiParam(value = "最小面值", example = "100000")
            @RequestParam(required = false) java.math.BigDecimal minAmount,

            @ApiParam(value = "最大面值", example = "5000000")
            @RequestParam(required = false) java.math.BigDecimal maxAmount,

            @ApiParam(value = "最小剩余天数", example = "30")
            @RequestParam(required = false) Integer minRemainingDays,

            @ApiParam(value = "最大剩余天数", example = "180")
            @RequestParam(required = false) Integer maxRemainingDays,

            @ApiParam(value = "持票人ID（查询特定持票人的票据）")
            @RequestParam(required = false) String holderId,

            @ApiParam(value = "页码（从0开始）", defaultValue = "0", example = "0")
            @RequestParam(defaultValue = "0") Integer page,

            @ApiParam(value = "每页大小", defaultValue = "20", example = "20")
            @RequestParam(defaultValue = "20") Integer size,

            @ApiParam(value = "排序字段", defaultValue = "remainingDays",
                       example = "remainingDays",
                       allowableValues = "remainingDays,faceValue,expectedReturn,riskScore")
            @RequestParam(defaultValue = "remainingDays") String sortBy,

            @ApiParam(value = "排序方向", defaultValue = "ASC",
                       example = "ASC",
                       allowableValues = "ASC,DESC")
            @RequestParam(defaultValue = "ASC") String sortOrder) {

        log.info("查询票据池: billType={}, minAmount={}, maxAmount={}, page={}, size={}",
                 billType, minAmount, maxAmount, page, size);

        BillPoolFilter filter = new BillPoolFilter();
        filter.setBillType(billType);
        filter.setMinAmount(minAmount);
        filter.setMaxAmount(maxAmount);
        filter.setMinRemainingDays(minRemainingDays);
        filter.setMaxRemainingDays(maxRemainingDays);
        filter.setHolderId(holderId);
        filter.setPage(page);
        filter.setSize(size);
        filter.setSortBy(sortBy);
        filter.setSortOrder(sortOrder);

        Page<BillPoolView> result = billPoolService.getBillPool(filter);

        return Result.success("查询成功", result);
    }

    /**
     * 查询可投资票据
     *
     * 功能说明：
     * - 查询符合特定金融机构要求的票据
     * - 根据机构风险偏好过滤
     * - 按收益率排序
     *
     * 权限要求：
     * - 仅金融机构可查询
     *
     * @param institutionId 金融机构ID（路径参数）
     * @param billType 票据类型（可选）
     * @param minAmount 最小面值（可选）
     * @param maxAmount 最大面值（可选）
     * @param minRemainingDays 最小剩余天数（可选）
     * @param maxRemainingDays 最大剩余天数（可选）
     * @return 可投资票据列表
     */
    @GetMapping("/available")
    @ApiOperation(value = "查询可投资票据",
                  notes = "查询符合特定金融机构投资要求的票据。" +
                          "系统会根据机构的风险偏好、投资策略等进行智能筛选。" +
                          "仅金融机构可查询此接口。")
    public Result<List<BillPoolView>> getAvailableBills(
            @ApiParam(value = "金融机构ID", required = true, example = "bank-uuid-001")
            @RequestParam @NonNull String institutionId,

            @ApiParam(value = "票据类型", example = "BANK_ACCEPTANCE_BILL")
            @RequestParam(required = false) String billType,

            @ApiParam(value = "最小面值", example = "100000")
            @RequestParam(required = false) java.math.BigDecimal minAmount,

            @ApiParam(value = "最大面值", example = "5000000")
            @RequestParam(required = false) java.math.BigDecimal maxAmount,

            @ApiParam(value = "最小剩余天数", example = "30")
            @RequestParam(required = false) Integer minRemainingDays,

            @ApiParam(value = "最大剩余天数", example = "180")
            @RequestParam(required = false) Integer maxRemainingDays) {

        log.info("查询可投资票据: institutionId={}", institutionId);

        BillPoolFilter filter = new BillPoolFilter();
        filter.setBillType(billType);
        filter.setMinAmount(minAmount);
        filter.setMaxAmount(maxAmount);
        filter.setMinRemainingDays(minRemainingDays);
        filter.setMaxRemainingDays(maxRemainingDays);

        List<BillPoolView> result = billPoolService.getAvailableBills(institutionId, filter);

        return Result.success("查询成功", result);
    }

    /**
     * 票据投资
     *
     * 功能说明：
     * - 金融机构通过票据池投资票据
     * - 本质上是一次背书转让
     * - 支持贴现投资
     *
     * 权限要求：
     * - 仅金融机构可投资
     * - 需要提供有效的认证信息
     *
     * 业务规则：
     * - 投资金额：票据面值的10%-100%
     * - 投资票据必须在票据池中
     * - 不能投资自己持有的票据
     * - 票据不能有未完成的投资
     *
     * @param billId 票据ID（路径参数）
     * @param request 投资请求
     * @param authentication 认证信息
     * @return 投资响应
     */
    @PostMapping("/{billId}/invest")
    @ApiOperation(value = "票据投资",
                  notes = "金融机构通过票据池投资票据。" +
                          "投资本质上是一次背书转让，票据从当前持票人转移到投资机构。" +
                          "投资金额不能低于票据面值的10%，不能超过票据面值。" +
                          "只有金融机构才能执行此操作。")
    public Result<BillInvestResponse> investBill(
            @ApiParam(value = "票据ID", required = true, example = "bill-uuid-001")
            @PathVariable @NonNull String billId,

            @ApiParam(value = "投资请求", required = true)
            @RequestBody @Valid BillInvestRequest request,

            Authentication authentication) {

        // 从认证上下文获取投资机构地址
        String investorAddress = authentication.getName();

        log.info("==================== 票据投资请求 ====================");
        log.info("票据ID: {}, 投资机构: {}, 投资金额: {}, 投资利率: {}%",
                 billId, investorAddress, request.getInvestAmount(), request.getInvestRate());

        BillInvestResponse response = billPoolService.investBill(billId, request, investorAddress);

        log.info("票据投资请求完成: investmentId={}", response.getInvestmentId());

        return Result.success("投资成功", response);
    }

    /**
     * 查询投资记录
     *
     * 功能说明：
     * - 查询金融机构的投资记录
     * - 包含所有状态的投资
     * - 按投资时间倒序排列
     *
     * 权限要求：
     * - 金融机构只能查询自己的投资记录
     * - 管理员可查询所有记录
     *
     * @param institutionId 金融机构ID
     * @return 投资记录列表
     */
    @GetMapping("/investments")
    @ApiOperation(value = "查询投资记录",
                  notes = "查询金融机构的所有投资记录，包括待确认、已确认、已完成、已取消、失败等状态。" +
                          "金融机构只能查询自己的投资记录，管理员可查询所有记录。" +
                          "记录按投资时间倒序排列。")
    public Result<List<BillInvestResponse>> getInvestmentRecords(
            @ApiParam(value = "金融机构ID", required = true, example = "bank-uuid-001")
            @RequestParam @NonNull String institutionId) {

        log.info("查询投资记录: institutionId={}", institutionId);

        List<BillInvestResponse> result = billPoolService.getInvestmentRecords(institutionId);

        return Result.success("查询成功", result);
    }

    /**
     * 查询票据池统计信息
     *
     * 功能说明：
     * - 统计票据池的整体数据
     * - 包括票据数量、总面值、平均收益率等
     *
     * 权限要求：
     * - 已登录用户
     *
     * @return 统计信息
     */
    @GetMapping("/statistics")
    @ApiOperation(value = "查询票据池统计信息",
                  notes = "查询票据池的整体统计数据，包括票据数量、总面值、平均收益率、风险分布等。" +
                          "帮助投资者了解票据池的整体情况。")
    public Result<BillPoolStatistics> getBillPoolStatistics() {

        log.info("查询票据池统计信息");

        // 当前返回模拟数据，未来可扩展为实时统计数据
        // 可通过 BillRepository 聚合查询获取真实统计数据
        BillPoolStatistics statistics = new BillPoolStatistics();
        statistics.setTotalBills(150);
        statistics.setTotalAmount(java.math.BigDecimal.valueOf(150000000));
        statistics.setAvgReturnRate(java.math.BigDecimal.valueOf(5.2));
        statistics.setAvgRemainingDays(90);

        return Result.success("查询成功", statistics);
    }

    /**
     * 票据池统计信息DTO
     */
    @io.swagger.annotations.ApiModel(value = "票据池统计信息", description = "票据池的整体统计数据")
    @lombok.Data
    public static class BillPoolStatistics {
        @io.swagger.annotations.ApiModelProperty(value = "票据总数")
        private Integer totalBills;

        @io.swagger.annotations.ApiModelProperty(value = "总面值")
        private java.math.BigDecimal totalAmount;

        @io.swagger.annotations.ApiModelProperty(value = "平均收益率（%）")
        private java.math.BigDecimal avgReturnRate;

        @io.swagger.annotations.ApiModelProperty(value = "平均剩余天数")
        private Integer avgRemainingDays;

        @io.swagger.annotations.ApiModelProperty(value = "低风险票据数量")
        private Integer lowRiskCount;

        @io.swagger.annotations.ApiModelProperty(value = "中风险票据数量")
        private Integer mediumRiskCount;

        @io.swagger.annotations.ApiModelProperty(value = "高风险票据数量")
        private Integer highRiskCount;
    }
}
