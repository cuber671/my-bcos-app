package com.fisco.app.controller.system;

import java.time.LocalDateTime;

import javax.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fisco.app.dto.risk.RiskStatisticsDTO;
import com.fisco.app.dto.statistics.BusinessStatisticsDTO;
import com.fisco.app.dto.statistics.ComprehensiveReportDTO;
import com.fisco.app.dto.statistics.FinancingStatisticsDTO;
import com.fisco.app.dto.statistics.StatisticsQueryRequest;
import com.fisco.app.service.system.StatisticsService;
import com.fisco.app.vo.Result;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 统计分析Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@Api(tags = "统计分析")
public class StatisticsController {

    private final StatisticsService statisticsService;

    /**
     * GET /api/statistics/business
     * 查询业务统计
     */
    @GetMapping("/business")
    @ApiOperation(value = "查询业务统计", notes = "获取平台业务活动统计信息，包括票据、应收账款、仓单等业务的统计数据")
    public Result<BusinessStatisticsDTO> getBusinessStatistics(
            @ApiParam(value = "统计开始时间", required = true, example = "2026-01-01T00:00:00")
            @RequestParam @Valid @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @ApiParam(value = "统计结束时间", required = true, example = "2026-12-31T23:59:59")
            @RequestParam @Valid @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @ApiParam(value = "企业地址（可选）", example = "0x1234567890abcdef")
            @RequestParam(required = false) String enterpriseAddress,
            @ApiParam(value = "统计粒度", example = "MONTH")
            @RequestParam(required = false) StatisticsQueryRequest.StatisticsGranularity granularity,
            Authentication authentication) {
        log.info("查询业务统计: startTime={}, endTime={}", startTime, endTime);

        StatisticsQueryRequest request = new StatisticsQueryRequest();
        request.setStartTime(startTime);
        request.setEndTime(endTime);
        request.setEnterpriseAddress(enterpriseAddress);
        request.setGranularity(granularity);

        BusinessStatisticsDTO statistics = statisticsService.getBusinessStatistics(request);

        return Result.success("查询成功", statistics);
    }

    /**
     * GET /api/statistics/financing
     * 查询融资统计
     */
    @GetMapping("/financing")
    @ApiOperation(value = "查询融资统计", notes = "获取平台融资活动统计信息，包括融资金额、利率、期限、还款等数据")
    public Result<FinancingStatisticsDTO> getFinancingStatistics(
            @ApiParam(value = "统计开始时间", required = true, example = "2026-01-01T00:00:00")
            @RequestParam @Valid @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @ApiParam(value = "统计结束时间", required = true, example = "2026-12-31T23:59:59")
            @RequestParam @Valid @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @ApiParam(value = "企业地址（可选）", example = "0x1234567890abcdef")
            @RequestParam(required = false) String enterpriseAddress,
            @ApiParam(value = "统计粒度", example = "MONTH")
            @RequestParam(required = false) StatisticsQueryRequest.StatisticsGranularity granularity,
            Authentication authentication) {
        log.info("查询融资统计: startTime={}, endTime={}", startTime, endTime);

        StatisticsQueryRequest request = new StatisticsQueryRequest();
        request.setStartTime(startTime);
        request.setEndTime(endTime);
        request.setEnterpriseAddress(enterpriseAddress);
        request.setGranularity(granularity);

        FinancingStatisticsDTO statistics = statisticsService.getFinancingStatistics(request);

        return Result.success("查询成功", statistics);
    }

    /**
     * GET /api/statistics/risk
     * 查询风险统计
     */
    @GetMapping("/risk")
    @ApiOperation(value = "查询风险统计", notes = "获取平台风险监控统计信息，包括逾期、罚息、坏账、预警等数据")
    public Result<RiskStatisticsDTO> getRiskStatistics(
            @ApiParam(value = "统计开始时间", required = true, example = "2026-01-01T00:00:00")
            @RequestParam @Valid @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @ApiParam(value = "统计结束时间", required = true, example = "2026-12-31T23:59:59")
            @RequestParam @Valid @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @ApiParam(value = "企业地址（可选）", example = "0x1234567890abcdef")
            @RequestParam(required = false) String enterpriseAddress,
            @ApiParam(value = "统计粒度", example = "MONTH")
            @RequestParam(required = false) StatisticsQueryRequest.StatisticsGranularity granularity,
            Authentication authentication) {
        log.info("查询风险统计: startTime={}, endTime={}", startTime, endTime);

        StatisticsQueryRequest request = new StatisticsQueryRequest();
        request.setStartTime(startTime);
        request.setEndTime(endTime);
        request.setEnterpriseAddress(enterpriseAddress);
        request.setGranularity(granularity);

        RiskStatisticsDTO statistics = statisticsService.getRiskStatistics(request);

        return Result.success("查询成功", statistics);
    }

    /**
     * GET /api/statistics/comprehensive
     * 查询综合报表
     */
    @GetMapping("/comprehensive")
    @ApiOperation(value = "查询综合报表", notes = "获取平台综合运营报表，包含业务、融资、风险等多维度数据")
    public Result<ComprehensiveReportDTO> getComprehensiveReport(
            @ApiParam(value = "统计开始时间", required = true, example = "2026-01-01T00:00:00")
            @RequestParam @Valid @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @ApiParam(value = "统计结束时间", required = true, example = "2026-12-31T23:59:59")
            @RequestParam @Valid @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @ApiParam(value = "企业地址（可选）", example = "0x1234567890abcdef")
            @RequestParam(required = false) String enterpriseAddress,
            @ApiParam(value = "统计粒度", example = "MONTH")
            @RequestParam(required = false) StatisticsQueryRequest.StatisticsGranularity granularity,
            Authentication authentication) {
        log.info("查询综合报表: startTime={}, endTime={}", startTime, endTime);

        StatisticsQueryRequest request = new StatisticsQueryRequest();
        request.setStartTime(startTime);
        request.setEndTime(endTime);
        request.setEnterpriseAddress(enterpriseAddress);
        request.setGranularity(granularity);

        ComprehensiveReportDTO report = statisticsService.getComprehensiveReport(request);

        return Result.success("查询成功", report);
    }

    /**
     * GET /api/statistics/dashboard
     * 查询仪表盘数据（简化版综合统计）
     */
    @GetMapping("/dashboard")
    @ApiOperation(value = "查询仪表盘数据", notes = "获取仪表盘所需的简化版综合统计数据")
    public Result<DashboardStatisticsDTO> getDashboardStatistics(
            @ApiParam(value = "统计开始时间", required = true, example = "2026-01-01T00:00:00")
            @RequestParam @Valid @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @ApiParam(value = "统计结束时间", required = true, example = "2026-12-31T23:59:59")
            @RequestParam @Valid @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            Authentication authentication) {
        log.info("查询仪表盘统计: startTime={}, endTime={}", startTime, endTime);

        StatisticsQueryRequest request = new StatisticsQueryRequest();
        request.setStartTime(startTime);
        request.setEndTime(endTime);

        // 获取各模块统计数据
        BusinessStatisticsDTO businessStats = statisticsService.getBusinessStatistics(request);
        FinancingStatisticsDTO financingStats = statisticsService.getFinancingStatistics(request);
        RiskStatisticsDTO riskStats = statisticsService.getRiskStatistics(request);

        // 组装仪表盘数据
        DashboardStatisticsDTO dashboard = new DashboardStatisticsDTO();
        dashboard.setTotalTransactionAmount(businessStats.getTotalBillAmount() + businessStats.getTotalReceivableAmount());
        dashboard.setTotalFinancingAmount(financingStats.getTotalFinancingAmount());
        dashboard.setOverdueAmount(riskStats.getOverdueAmount());
        dashboard.setTotalBills(businessStats.getTotalBills());
        dashboard.setTotalReceivables(businessStats.getTotalReceivables());
        dashboard.setFinancingCount(financingStats.getTotalFinancingCount());
        dashboard.setOverdueRate(riskStats.getOverdueRate());

        return Result.success("查询成功", dashboard);
    }

    /**
     * 仪表盘统计DTO
     */
    @lombok.Data
    @io.swagger.annotations.ApiModel(value = "仪表盘统计")
    public static class DashboardStatisticsDTO {
        @io.swagger.annotations.ApiModelProperty(value = "总交易额（分）")
        private Long totalTransactionAmount;

        @io.swagger.annotations.ApiModelProperty(value = "总融资金额（分）")
        private Long totalFinancingAmount;

        @io.swagger.annotations.ApiModelProperty(value = "逾期金额（分）")
        private Long overdueAmount;

        @io.swagger.annotations.ApiModelProperty(value = "票据总数")
        private Long totalBills;

        @io.swagger.annotations.ApiModelProperty(value = "应收账款总数")
        private Long totalReceivables;

        @io.swagger.annotations.ApiModelProperty(value = "融资笔数")
        private Long financingCount;

        @io.swagger.annotations.ApiModelProperty(value = "逾期率")
        private Double overdueRate;
    }
}
