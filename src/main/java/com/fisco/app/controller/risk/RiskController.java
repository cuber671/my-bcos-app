package com.fisco.app.controller.risk;

import com.fisco.app.dto.risk.RiskAssessmentRequest;
import com.fisco.app.dto.risk.RiskAssessmentResponse;
import com.fisco.app.entity.risk.RiskAssessment;
import com.fisco.app.service.risk.RiskService;
import com.fisco.app.vo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 风险监测Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/risk")
@RequiredArgsConstructor
@Api(tags = "风险监测")
public class RiskController {

    private final RiskService riskService;

    /**
     * POST /api/risk/assess
     * 评估企业风险
     */
    @PostMapping("/assess")
    @ApiOperation(value = "评估企业风险", notes = "对指定企业进行全面风险评估，包括信用风险、逾期风险等")
    public Result<RiskAssessmentResponse> assessRisk(
            @Valid @RequestBody RiskAssessmentRequest request,
            Authentication authentication) {
        log.info("评估企业风险: enterpriseAddress={}", request.getEnterpriseAddress());

        RiskAssessmentResponse response = riskService.assessRisk(request);

        return Result.success("风险评估完成", response);
    }

    /**
     * GET /api/risk/assess/{enterpriseAddress}
     * 快速评估企业风险
     */
    @GetMapping("/assess/{enterpriseAddress}")
    @ApiOperation(value = "快速评估企业风险", notes = "使用默认参数快速评估企业风险")
    public Result<RiskAssessmentResponse> quickAssessRisk(
            @ApiParam(value = "企业地址", required = true, example = "0x1234567890abcdef")
            @PathVariable String enterpriseAddress,
            Authentication authentication) {
        log.info("快速评估企业风险: enterpriseAddress={}", enterpriseAddress);

        RiskAssessmentRequest request = new RiskAssessmentRequest();
        request.setEnterpriseAddress(enterpriseAddress);
        request.setAssessmentType(RiskAssessmentRequest.RiskAssessmentType.COMPREHENSIVE);
        request.setAssessmentTime(LocalDateTime.now());
        request.setAssessmentPeriod(12);

        RiskAssessmentResponse response = riskService.assessRisk(request);

        return Result.success("风险评估完成", response);
    }

    /**
     * GET /api/risk/history/{enterpriseAddress}
     * 查询企业风险历史
     */
    @GetMapping("/history/{enterpriseAddress}")
    @ApiOperation(value = "查询企业风险历史", notes = "获取企业的历史风险评估记录")
    public Result<List<RiskAssessment>> getRiskHistory(
            @ApiParam(value = "企业地址", required = true, example = "0x1234567890abcdef")
            @PathVariable String enterpriseAddress,
            @ApiParam(value = "查询数量限制", example = "10")
            @RequestParam(defaultValue = "10") Integer limit,
            Authentication authentication) {
        log.info("查询企业风险历史: enterpriseAddress={}, limit={}", enterpriseAddress, limit);

        List<RiskAssessment> history = riskService.getEnterpriseRiskHistory(enterpriseAddress, limit);

        return Result.success("查询成功", history);
    }

    /**
     * GET /api/risk/alerts
     * 查询活跃风险预警
     */
    @GetMapping("/alerts")
    @ApiOperation(value = "查询活跃风险预警", notes = "获取所有活跃的风险预警信息")
    public Result<List<Map<String, Object>>> getActiveAlerts(
            @ApiParam(value = "企业地址（可选，用于筛选特定企业的预警）", example = "0x1234567890abcdef")
            @RequestParam(required = false) String enterpriseAddress,
            Authentication authentication) {
        log.info("查询活跃风险预警: enterpriseAddress={}", enterpriseAddress);

        List<Map<String, Object>> alerts = riskService.getActiveAlerts(enterpriseAddress);

        return Result.success("查询成功", alerts);
    }

    /**
     * GET /api/risk/alerts/{enterpriseAddress}
     * 查询企业风险预警
     */
    @GetMapping("/alerts/{enterpriseAddress}")
    @ApiOperation(value = "查询企业风险预警", notes = "获取指定企业的风险预警信息")
    public Result<List<Map<String, Object>>> getEnterpriseAlerts(
            @ApiParam(value = "企业地址", required = true, example = "0x1234567890abcdef")
            @PathVariable String enterpriseAddress,
            Authentication authentication) {
        log.info("查询企业风险预警: enterpriseAddress={}", enterpriseAddress);

        List<Map<String, Object>> alerts = riskService.getActiveAlerts(enterpriseAddress);

        return Result.success("查询成功", alerts);
    }

    /**
     * GET /api/risk/report
     * 生成风险报告
     */
    @GetMapping("/report")
    @ApiOperation(value = "生成风险报告", notes = "生成指定时间段的风险分析报告")
    public Result<Map<String, Object>> generateRiskReport(
            @ApiParam(value = "报告开始时间", required = true, example = "2026-01-01T00:00:00")
            @RequestParam @Valid @DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @ApiParam(value = "报告结束时间", required = true, example = "2026-12-31T23:59:59")
            @RequestParam @Valid @DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Authentication authentication) {
        log.info("生成风险报告: startDate={}, endDate={}", startDate, endDate);

        Map<String, Object> report = riskService.generateRiskReport(startDate, endDate);

        return Result.success("报告生成成功", report);
    }

    /**
     * GET /api/risk/metrics
     * 查询风险指标
     */
    @GetMapping("/metrics")
    @ApiOperation(value = "查询风险指标", notes = "获取平台整体风险指标统计")
    public Result<RiskMetricsSummary> getRiskMetrics(
            @ApiParam(value = "企业地址（可选）", example = "0x1234567890abcdef")
            @RequestParam(required = false) String enterpriseAddress,
            Authentication authentication) {
        log.info("查询风险指标: enterpriseAddress={}", enterpriseAddress);

        // 简化实现
        RiskMetricsSummary metrics = new RiskMetricsSummary();
        metrics.setTotalEnterprisesAssessed(0L);
        metrics.setHighRiskEnterprises(0L);
        metrics.setMediumRiskEnterprises(0L);
        metrics.setLowRiskEnterprises(0L);
        metrics.setActiveAlerts(0L);

        return Result.success("查询成功", metrics);
    }

    /**
     * 风险指标汇总
     */
    @Data
    @io.swagger.annotations.ApiModel(value = "风险指标汇总")
    public static class RiskMetricsSummary {
        @io.swagger.annotations.ApiModelProperty(value = "已评估企业总数")
        private Long totalEnterprisesAssessed;

        @io.swagger.annotations.ApiModelProperty(value = "高风险企业数量")
        private Long highRiskEnterprises;

        @io.swagger.annotations.ApiModelProperty(value = "中风险企业数量")
        private Long mediumRiskEnterprises;

        @io.swagger.annotations.ApiModelProperty(value = "低风险企业数量")
        private Long lowRiskEnterprises;

        @io.swagger.annotations.ApiModelProperty(value = "活跃预警数量")
        private Long activeAlerts;

        @io.swagger.annotations.ApiModelProperty(value = "平均风险评分")
        private Double averageRiskScore;

        @io.swagger.annotations.ApiModelProperty(value = "平均信用评分")
        private Double averageCreditScore;
    }
}
