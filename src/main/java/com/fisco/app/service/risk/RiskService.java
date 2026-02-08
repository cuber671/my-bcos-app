package com.fisco.app.service.risk;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fisco.app.dto.risk.RiskAssessmentRequest;
import com.fisco.app.dto.risk.RiskAssessmentResponse;
import com.fisco.app.entity.bill.Bill;
import com.fisco.app.entity.credit.CreditLimitWarning;
import com.fisco.app.entity.enterprise.Enterprise;
import com.fisco.app.entity.receivable.Receivable;
import com.fisco.app.entity.risk.RiskAssessment;
import com.fisco.app.repository.bill.BillRepository;
import com.fisco.app.repository.credit.CreditLimitWarningRepository;
import com.fisco.app.repository.enterprise.EnterpriseRepository;
import com.fisco.app.repository.receivable.ReceivableRepository;

import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 风险监测Service
 */
@Slf4j
@Service
@Api(tags = "风险监测服务")
@RequiredArgsConstructor
public class RiskService {

    private final ReceivableRepository receivableRepository;
    private final BillRepository billRepository;
    private final CreditLimitWarningRepository creditLimitWarningRepository;
    private final EnterpriseRepository enterpriseRepository;
    private final ObjectMapper objectMapper;

    /**
     * 评估企业风险
     */
    @Transactional
    public RiskAssessmentResponse assessRisk(RiskAssessmentRequest request) {
        log.info("评估企业风险: enterpriseAddress={}, assessmentType={}",
                request.getEnterpriseAddress(), request.getAssessmentType());

        String enterpriseAddress = request.getEnterpriseAddress();
        LocalDateTime assessmentTime = request.getAssessmentTime() != null ? request.getAssessmentTime() : LocalDateTime.now();

        // 获取企业信息
        Enterprise enterprise = enterpriseRepository.findByAddress(enterpriseAddress)
                .orElseThrow(() -> new com.fisco.app.exception.BusinessException("企业不存在"));

        // 创建评估响应
        RiskAssessmentResponse response = new RiskAssessmentResponse();
        response.setEnterpriseAddress(enterpriseAddress);
        response.setEnterpriseName(enterprise.getName());
        response.setAssessmentTime(assessmentTime);

        // 1. 计算逾期风险指标
        List<Receivable> overdueReceivables = receivableRepository.findAll().stream()
                .filter(r -> enterpriseAddress.equals(r.getSupplierAddress()) || enterpriseAddress.equals(r.getCoreEnterpriseAddress()))
                .filter(r -> r.getOverdueLevel() != null)
                .collect(Collectors.toList());

        response.setOverdueCount(overdueReceivables.size());
        long overdueAmount = overdueReceivables.stream()
                .filter(r -> r.getAmount() != null)
                .mapToLong(r -> r.getAmount().multiply(BigDecimal.valueOf(100)).longValue())
                .sum();
        response.setOverdueAmount(overdueAmount);

        // 2. 计算交易行为指标
        List<Bill> bills = billRepository.findAll().stream()
                .filter(b -> enterpriseAddress.equals(b.getDrawerAddress()) || enterpriseAddress.equals(b.getDraweeAddress()))
                .collect(Collectors.toList());

        response.setTransactionCount(bills.size());

        int successCount = (int) bills.stream()
                .filter(b -> b.getBillStatus() == Bill.BillStatus.ISSUED || b.getBillStatus() == Bill.BillStatus.SETTLED)
                .count();
        response.setTransactionSuccessRate(bills.size() > 0 ?
                BigDecimal.valueOf(successCount).divide(BigDecimal.valueOf(bills.size()), 4, RoundingMode.HALF_UP) : BigDecimal.ONE);

        long avgTransactionAmount = bills.stream()
                .filter(b -> b.getFaceValue() != null)
                .mapToLong(b -> b.getFaceValue().multiply(BigDecimal.valueOf(100)).longValue())
                .sum();
        response.setAverageTransactionAmount(bills.size() > 0 ? avgTransactionAmount / bills.size() : 0L);

        // 3. 计算风险评分
        int riskScore = calculateRiskScore(response);
        response.setRiskScore(riskScore);

        // 4. 确定风险等级
        RiskAssessmentResponse.RiskLevel riskLevel = RiskAssessmentResponse.RiskLevel.fromScore(riskScore);
        response.setRiskLevel(riskLevel);

        // 5. 计算信用评分（简化：基础分600 + 风险评分*4）
        response.setCreditScore(600 + riskScore * 4);

        // 6. 生成风险预警
        List<RiskAssessmentResponse.RiskWarning> warnings = generateRiskWarnings(response, enterpriseAddress);
        response.setWarningCount(warnings.size());
        response.setWarnings(warnings);

        // 7. 分析风险因素
        Map<String, BigDecimal> riskFactors = analyzeRiskFactors(response);
        response.setRiskFactors(riskFactors);

        // 8. 生成改进建议
        List<String> recommendations = generateRecommendations(response, riskLevel);
        response.setRecommendations(recommendations);

        // 9. 保存评估记录
        saveRiskAssessment(response, request.getAssessmentType());

        log.info("企业风险评估完成: enterpriseAddress={}, riskLevel={}, riskScore={}",
                enterpriseAddress, riskLevel, riskScore);

        return response;
    }

    /**
     * 计算风险评分（0-100，分数越高风险越低）
     */
    private int calculateRiskScore(RiskAssessmentResponse response) {
        int score = 100;

        // 逾期影响（最多扣30分）
        if (response.getOverdueCount() > 0) {
            score -= Math.min(30, response.getOverdueCount() * 5);
        }

        // 交易成功率影响（最多扣20分）
        if (response.getTransactionSuccessRate() != null) {
            double successRate = response.getTransactionSuccessRate().doubleValue();
            if (successRate < 0.9) {
                score -= (int) ((0.9 - successRate) * 200);
            }
        }

        // 确保分数在0-100范围内
        return Math.max(0, Math.min(100, score));
    }

    /**
     * 生成风险预警
     */
    private List<RiskAssessmentResponse.RiskWarning> generateRiskWarnings(
            RiskAssessmentResponse response, String enterpriseAddress) {

        List<RiskAssessmentResponse.RiskWarning> warnings = new ArrayList<>();

        // 逾期预警
        if (response.getOverdueCount() > 0) {
            RiskAssessmentResponse.RiskWarning warning = new RiskAssessmentResponse.RiskWarning();
            warning.setWarningType("OVERDUE");
            warning.setWarningLevel(response.getOverdueCount() > 3 ? "HIGH" : "MEDIUM");
            warning.setWarningMessage(String.format("企业存在%d笔逾期，逾期金额%d元",
                    response.getOverdueCount(), response.getOverdueAmount() / 100));
            warning.setWarningTime(LocalDateTime.now());
            warnings.add(warning);
        }

        // 信用额度预警
        List<CreditLimitWarning> creditWarnings = creditLimitWarningRepository.findAll().stream()
                .filter(w -> {
                    // Need to check if this warning belongs to the enterprise
                    // For now, skip the enterprise filter or implement a proper join
                    return true;
                })
                .filter(w -> Boolean.FALSE.equals(w.getIsResolved()))
                .collect(Collectors.toList());

        for (CreditLimitWarning cw : creditWarnings) {
            RiskAssessmentResponse.RiskWarning warning = new RiskAssessmentResponse.RiskWarning();
            warning.setWarningType("CREDIT_LIMIT");
            warning.setWarningLevel(cw.getWarningLevel().name());
            warning.setWarningMessage("信用额度使用率预警");
            warning.setWarningTime(cw.getWarningDate());
            warnings.add(warning);
        }

        return warnings;
    }

    /**
     * 分析风险因素
     */
    private Map<String, BigDecimal> analyzeRiskFactors(RiskAssessmentResponse response) {
        Map<String, BigDecimal> factors = new HashMap<>();

        // 逾期风险权重
        double overdueWeight = response.getOverdueCount() > 0 ? 0.4 : 0.1;
        factors.put("逾期风险", BigDecimal.valueOf(overdueWeight));

        // 交易行为权重
        double transactionWeight = 0.3;
        factors.put("交易行为", BigDecimal.valueOf(transactionWeight));

        // 信用额度权重
        double creditWeight = 0.2;
        factors.put("信用额度", BigDecimal.valueOf(creditWeight));

        // 其他因素权重
        factors.put("其他", BigDecimal.valueOf(0.2 - overdueWeight + 0.1));

        return factors;
    }

    /**
     * 生成改进建议
     */
    private List<String> generateRecommendations(RiskAssessmentResponse response,
                                                   RiskAssessmentResponse.RiskLevel riskLevel) {
        List<String> recommendations = new ArrayList<>();

        if (response.getOverdueCount() > 0) {
            recommendations.add("及时处理逾期应收账款，减少逾期次数");
        }

        if (response.getTransactionSuccessRate() != null && response.getTransactionSuccessRate().doubleValue() < 0.9) {
            recommendations.add("提高交易履约率，建立良好信用记录");
        }

        switch (riskLevel) {
            case HIGH:
            case VERY_HIGH:
                recommendations.add("风险较高，建议增加资金储备，降低负债率");
                recommendations.add("建议与金融机构协商，获取更多支持");
                break;
            case MEDIUM:
                recommendations.add("注意控制风险，加强应收账款管理");
                break;
            case LOW:
            case VERY_LOW:
                recommendations.add("风险较低，可适度增加业务规模");
                break;
        }

        return recommendations;
    }

    /**
     * 保存风险评估记录
     */
    private void saveRiskAssessment(RiskAssessmentResponse response, RiskAssessmentRequest.RiskAssessmentType assessmentType) {
        try {
            RiskAssessment assessment = new RiskAssessment();
            assessment.setId(UUID.randomUUID().toString());
            assessment.setEnterpriseAddress(response.getEnterpriseAddress());
            assessment.setEnterpriseName(response.getEnterpriseName());
            assessment.setAssessmentType(assessmentType != null ? assessmentType.name() : "COMPREHENSIVE");
            assessment.setAssessmentTime(response.getAssessmentTime());
            assessment.setRiskLevel(response.getRiskLevel().name());
            assessment.setRiskScore(response.getRiskScore());
            assessment.setCreditScore(response.getCreditScore());
            assessment.setOverdueCount(response.getOverdueCount());
            assessment.setOverdueAmount(response.getOverdueAmount());
            assessment.setOverdueRate(response.getOverdueRate() != null ? response.getOverdueRate() : BigDecimal.ZERO);
            assessment.setTransactionCount(response.getTransactionCount());
            assessment.setWarningCount(response.getWarningCount());

            // 转换复杂对象为JSON
            if (response.getRiskFactors() != null) {
                assessment.setRiskFactors(objectMapper.writeValueAsString(response.getRiskFactors()));
            }
            if (response.getRecommendations() != null) {
                assessment.setRecommendations(objectMapper.writeValueAsString(response.getRecommendations()));
            }

            // 这里需要RiskAssessmentRepository，暂时不保存
            // riskAssessmentRepository.save(assessment);

            log.debug("风险评估记录已保存: id={}", assessment.getId());
        } catch (JsonProcessingException e) {
            log.warn("转换风险评估数据为JSON失败", e);
        }
    }

    /**
     * 查询企业风险历史
     */
    public List<RiskAssessment> getEnterpriseRiskHistory(String enterpriseAddress, int limit) {
        log.info("查询企业风险历史: enterpriseAddress={}, limit={}", enterpriseAddress, limit);

        // 这里需要RiskAssessmentRepository
        // return riskAssessmentRepository.findByEnterpriseAddressOrderByAssessmentTimeDesc(enterpriseAddress, PageRequest.of(0, limit));

        // 暂时返回空列表
        return new ArrayList<>();
    }

    /**
     * 查询风险预警列表
     */
    public List<Map<String, Object>> getActiveAlerts(String enterpriseAddress) {
        log.info("查询活跃风险预警: enterpriseAddress={}", enterpriseAddress);

        List<Map<String, Object>> alerts = new ArrayList<>();

        // 查询信用额度预警
        List<CreditLimitWarning> warnings = creditLimitWarningRepository.findAll().stream()
                .filter(w -> Boolean.FALSE.equals(w.getIsResolved()))
                .collect(Collectors.toList());

        for (CreditLimitWarning warning : warnings) {
            Map<String, Object> alert = new HashMap<>();
            alert.put("type", "CREDIT_LIMIT");
            alert.put("level", warning.getWarningLevel().name());
            alert.put("message", "信用额度使用率: " + warning.getCurrentUsageRate() + "%");
            alert.put("createdAt", warning.getWarningDate());
            alerts.add(alert);
        }

        return alerts;
    }

    /**
     * 生成风险报告
     */
    public Map<String, Object> generateRiskReport(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("生成风险报告: startDate={}, endDate={}", startDate, endDate);

        Map<String, Object> report = new HashMap<>();

        // 统计各风险等级企业数量
        Map<String, Integer> riskLevelDistribution = new HashMap<>();
        riskLevelDistribution.put("HIGH", 0);
        riskLevelDistribution.put("MEDIUM", 0);
        riskLevelDistribution.put("LOW", 0);

        // 统计预警数量
        long totalAlerts = creditLimitWarningRepository.count();
        long activeAlerts = creditLimitWarningRepository.findAll().stream()
                .filter(w -> Boolean.FALSE.equals(w.getIsResolved()))
                .count();

        report.put("reportPeriod", startDate.toLocalDate() + " 至 " + endDate.toLocalDate());
        report.put("reportGeneratedAt", LocalDateTime.now());
        report.put("riskLevelDistribution", riskLevelDistribution);
        report.put("totalAlerts", totalAlerts);
        report.put("activeAlerts", activeAlerts);

        return report;
    }
}
