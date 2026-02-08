package com.fisco.app.service.system;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.fisco.app.dto.risk.RiskStatisticsDTO;
import com.fisco.app.dto.statistics.BusinessStatisticsDTO;
import com.fisco.app.dto.statistics.ComprehensiveReportDTO;
import com.fisco.app.dto.statistics.FinancingStatisticsDTO;
import com.fisco.app.dto.statistics.StatisticsQueryRequest;
import com.fisco.app.entity.bill.Bill;
import com.fisco.app.entity.credit.CreditLimitWarning;
import com.fisco.app.entity.enterprise.Enterprise;
import com.fisco.app.entity.pledge.PledgeApplication;
import com.fisco.app.entity.receivable.Receivable;
import com.fisco.app.entity.risk.BadDebtRecord;
import com.fisco.app.entity.risk.OverduePenaltyRecord;
import com.fisco.app.entity.warehouse.ElectronicWarehouseReceipt;
import com.fisco.app.repository.bill.BillRepository;
import com.fisco.app.repository.credit.CreditLimitWarningRepository;
import com.fisco.app.repository.enterprise.EnterpriseRepository;
import com.fisco.app.repository.pledge.PledgeApplicationRepository;
import com.fisco.app.repository.receivable.ReceivableRepository;
import com.fisco.app.repository.risk.BadDebtRecordRepository;
import com.fisco.app.repository.risk.OverduePenaltyRecordRepository;
import com.fisco.app.repository.warehouse.ElectronicWarehouseReceiptRepository;

import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 统计分析Service
 */
@Slf4j
@Service
@Api(tags = "统计分析服务")
@RequiredArgsConstructor
public class StatisticsService {

    private final BillRepository billRepository;
    private final ReceivableRepository receivableRepository;
    private final ElectronicWarehouseReceiptRepository warehouseReceiptRepository;
    private final PledgeApplicationRepository pledgeApplicationRepository;
    private final CreditLimitWarningRepository creditLimitWarningRepository;
    private final OverduePenaltyRecordRepository overduePenaltyRecordRepository;
    private final BadDebtRecordRepository badDebtRecordRepository;
    private final EnterpriseRepository enterpriseRepository;

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy年MM月");

    /**
     * 查询业务统计
     */
    public BusinessStatisticsDTO getBusinessStatistics(StatisticsQueryRequest request) {
        log.info("查询业务统计: startTime={}, endTime={}", request.getStartTime(), request.getEndTime());

        BusinessStatisticsDTO statistics = new BusinessStatisticsDTO();

        // 设置统计周期
        String period = formatPeriod(request.getStartTime(), request.getEndTime());
        statistics.setPeriod(period);

        // 票据统计
        List<Bill> bills = billRepository.findAll().stream()
                .filter(b -> !b.getCreatedAt().isBefore(request.getStartTime()) && !b.getCreatedAt().isAfter(request.getEndTime()))
                .collect(Collectors.toList());

        statistics.setTotalBills((long) bills.size());
        statistics.setAcceptedBills(bills.stream().filter(b -> b.getBillStatus() == Bill.BillStatus.ISSUED).count());
        statistics.setDiscountedBills(bills.stream().filter(b -> b.getBillStatus() == Bill.BillStatus.DISCOUNTED).count());
        statistics.setInvestedBills(bills.stream().filter(b -> b.getBillStatus() == Bill.BillStatus.FINANCED).count());

        long totalBillAmount = bills.stream()
                .filter(b -> b.getFaceValue() != null)
                .mapToLong(b -> b.getFaceValue().multiply(BigDecimal.valueOf(100)).longValue())
                .sum();
        statistics.setTotalBillAmount(totalBillAmount);

        // 应收账款统计
        List<Receivable> receivables = receivableRepository.findAll().stream()
                .filter(r -> !r.getCreatedAt().isBefore(request.getStartTime()) && !r.getCreatedAt().isAfter(request.getEndTime()))
                .collect(Collectors.toList());

        statistics.setTotalReceivables((long) receivables.size());
        statistics.setConfirmedReceivables(receivables.stream().filter(r -> r.getStatus() == Receivable.ReceivableStatus.CONFIRMED).count());
        statistics.setFinancedReceivables(receivables.stream().filter(r -> r.getStatus() == Receivable.ReceivableStatus.FINANCED).count());

        long totalReceivableAmount = receivables.stream()
                .filter(r -> r.getAmount() != null)
                .mapToLong(r -> r.getAmount().multiply(BigDecimal.valueOf(100)).longValue())
                .sum();
        statistics.setTotalReceivableAmount(totalReceivableAmount);

        // 仓单统计
        List<ElectronicWarehouseReceipt> receipts = warehouseReceiptRepository.findAll().stream()
                .filter(e -> !e.getCreatedAt().isBefore(request.getStartTime()) && !e.getCreatedAt().isAfter(request.getEndTime()))
                .collect(Collectors.toList());

        statistics.setTotalWarehouseReceipts((long) receipts.size());
        statistics.setPledgedReceipts(receipts.stream().filter(e -> e.getReceiptStatus() == ElectronicWarehouseReceipt.ReceiptStatus.PLEDGED).count());

        long totalReceiptValue = receipts.stream()
                .filter(e -> e.getTotalValue() != null)
                .mapToLong(e -> e.getTotalValue().multiply(BigDecimal.valueOf(100)).longValue())
                .sum();
        statistics.setTotalReceiptValue(totalReceiptValue);

        // 质押统计
        List<PledgeApplication> pledges = pledgeApplicationRepository.findAll().stream()
                .filter(p -> !p.getCreatedAt().isBefore(request.getStartTime()) && !p.getCreatedAt().isAfter(request.getEndTime()))
                .collect(Collectors.toList());

        statistics.setTotalPledgeApplications((long) pledges.size());
        statistics.setApprovedPledgeApplications(pledges.stream().filter(p -> p.getStatus() == PledgeApplication.ApplicationStatus.APPROVED).count());

        long totalPledgeAmount = pledges.stream()
                .filter(p -> p.getPledgeAmount() != null)
                .mapToLong(p -> p.getPledgeAmount().multiply(BigDecimal.valueOf(100)).longValue())
                .sum();
        statistics.setTotalPledgeAmount(totalPledgeAmount);

        // 业务类型分布
        Map<String, Long> businessTypeDistribution = new HashMap<>();
        businessTypeDistribution.put("BILL", statistics.getTotalBills());
        businessTypeDistribution.put("RECEIVABLE", statistics.getTotalReceivables());
        businessTypeDistribution.put("EWR", statistics.getTotalWarehouseReceipts());
        statistics.setBusinessTypeDistribution(businessTypeDistribution);

        // 生成趋势数据（简化版）
        statistics.setBillAmountTrend(generateTrendData(bills, request, "bill"));
        statistics.setReceivableAmountTrend(generateTrendData(receivables, request, "receivable"));

        return statistics;
    }

    /**
     * 查询融资统计
     */
    public FinancingStatisticsDTO getFinancingStatistics(StatisticsQueryRequest request) {
        log.info("查询融资统计: startTime={}, endTime={}", request.getStartTime(), request.getEndTime());

        FinancingStatisticsDTO statistics = new FinancingStatisticsDTO();

        // 设置统计周期
        String period = formatPeriod(request.getStartTime(), request.getEndTime());
        statistics.setPeriod(period);

        // 票据融资统计（使用贴现数据）
        List<Bill> financedBills = billRepository.findAll().stream()
                .filter(b -> b.getBillStatus() == Bill.BillStatus.DISCOUNTED)
                .filter(b -> b.getDiscountDate() != null)
                .filter(b -> !b.getDiscountDate().isBefore(request.getStartTime()) && !b.getDiscountDate().isAfter(request.getEndTime()))
                .collect(Collectors.toList());

        long billFinancingAmount = financedBills.stream()
                .filter(b -> b.getDiscountAmount() != null)
                .mapToLong(b -> b.getDiscountAmount().multiply(BigDecimal.valueOf(100)).longValue())
                .sum();
        statistics.setBillFinancingAmount(billFinancingAmount);
        statistics.setBillFinancingCount((long) financedBills.size());

        // 应收账款融资统计
        List<Receivable> financedReceivables = receivableRepository.findAll().stream()
                .filter(r -> r.getStatus() == Receivable.ReceivableStatus.FINANCED)
                .filter(r -> r.getFinanceDate() != null)
                .filter(r -> !r.getFinanceDate().isBefore(request.getStartTime()) && !r.getFinanceDate().isAfter(request.getEndTime()))
                .collect(Collectors.toList());

        long receivableFinancingAmount = financedReceivables.stream()
                .filter(r -> r.getFinanceAmount() != null)
                .mapToLong(r -> r.getFinanceAmount().multiply(BigDecimal.valueOf(100)).longValue())
                .sum();
        statistics.setReceivableFinancingAmount(receivableFinancingAmount);
        statistics.setReceivableFinancingCount((long) financedReceivables.size());

        // 仓单质押融资统计
        List<PledgeApplication> approvedPledges = pledgeApplicationRepository.findAll().stream()
                .filter(p -> p.getStatus() == PledgeApplication.ApplicationStatus.APPROVED)
                .filter(p -> !p.getCreatedAt().isBefore(request.getStartTime()) && !p.getCreatedAt().isAfter(request.getEndTime()))
                .collect(Collectors.toList());

        long pledgeFinancingAmount = approvedPledges.stream()
                .filter(p -> p.getPledgeAmount() != null)
                .mapToLong(p -> p.getPledgeAmount().multiply(BigDecimal.valueOf(100)).longValue())
                .sum();
        statistics.setPledgeFinancingAmount(pledgeFinancingAmount);
        statistics.setPledgeFinancingCount((long) approvedPledges.size());

        // 总融资统计
        statistics.setTotalFinancingAmount(billFinancingAmount + receivableFinancingAmount + pledgeFinancingAmount);
        statistics.setTotalFinancingCount(Long.valueOf(financedBills.size() + financedReceivables.size() + approvedPledges.size()));

        // 平均融资利率（使用贴现利率）
        OptionalDouble avgBillRate = financedBills.stream()
                .filter(b -> b.getDiscountRate() != null)
                .mapToDouble(b -> b.getDiscountRate().doubleValue())
                .average();
        statistics.setBillAverageRate(avgBillRate.isPresent() ? (int) (avgBillRate.getAsDouble() * 100) : 0);

        OptionalDouble avgReceivableRate = financedReceivables.stream()
                .filter(r -> r.getFinanceRate() != null)
                .mapToInt(Receivable::getFinanceRate)
                .average();
        statistics.setReceivableAverageRate(avgReceivableRate.isPresent() ? (int) avgReceivableRate.getAsDouble() : 0);

        OptionalDouble avgRate = Stream.concat(
                financedBills.stream().filter(b -> b.getDiscountRate() != null).mapToDouble(b -> b.getDiscountRate().doubleValue() * 100).boxed(),
                financedReceivables.stream().filter(r -> r.getFinanceRate() != null).mapToInt(Receivable::getFinanceRate).boxed().map(Integer::doubleValue)
        ).mapToDouble(Double::doubleValue).average();
        statistics.setAverageFinancingRate(avgRate.isPresent() ? (int) avgRate.getAsDouble() : 0);

        // 还款统计
        long billRepaid = financedBills.stream()
                .filter(b -> b.getBillStatus() == Bill.BillStatus.SETTLED)
                .filter(b -> b.getDiscountAmount() != null)
                .mapToLong(b -> b.getDiscountAmount().multiply(BigDecimal.valueOf(100)).longValue())
                .sum();

        long receivableRepaid = financedReceivables.stream()
                .filter(r -> r.getStatus() == Receivable.ReceivableStatus.REPAID)
                .filter(r -> r.getFinanceAmount() != null)
                .mapToLong(r -> r.getFinanceAmount().multiply(BigDecimal.valueOf(100)).longValue())
                .sum();

        long repaidAmount = billRepaid + receivableRepaid;
        statistics.setRepaidAmount(repaidAmount);
        statistics.setOutstandingAmount(statistics.getTotalFinancingAmount() - repaidAmount);

        if (statistics.getTotalFinancingAmount() > 0) {
            statistics.setRepaymentRate(BigDecimal.valueOf(repaidAmount)
                    .divide(BigDecimal.valueOf(statistics.getTotalFinancingAmount()), 4, RoundingMode.HALF_UP));
        }

        // 生成趋势数据
        statistics.setFinancingAmountTrend(generateFinancingTrendData(financedBills, financedReceivables, approvedPledges, request));
        statistics.setRateTrend(generateRateTrendData(financedBills, financedReceivables, request));

        return statistics;
    }

    /**
     * 查询风险统计
     */
    public RiskStatisticsDTO getRiskStatistics(StatisticsQueryRequest request) {
        log.info("查询风险统计: startTime={}, endTime={}", request.getStartTime(), request.getEndTime());

        RiskStatisticsDTO statistics = new RiskStatisticsDTO();

        // 设置统计周期
        String period = formatPeriod(request.getStartTime(), request.getEndTime());
        statistics.setPeriod(period);

        // 逾期统计
        List<Receivable> overdueReceivables = receivableRepository.findAll().stream()
                .filter(r -> r.getOverdueLevel() != null)
                .filter(r -> r.getOverdueCalculatedDate() != null)
                .filter(r -> !r.getOverdueCalculatedDate().isBefore(request.getStartTime()) && !r.getOverdueCalculatedDate().isAfter(request.getEndTime()))
                .collect(Collectors.toList());

        statistics.setOverdueReceivablesCount((long) overdueReceivables.size());

        long overdueAmount = overdueReceivables.stream()
                .filter(r -> r.getAmount() != null)
                .mapToLong(r -> r.getAmount().multiply(BigDecimal.valueOf(100)).longValue())
                .sum();
        statistics.setOverdueAmount(overdueAmount);

        // 逾期等级统计
        statistics.setMildOverdueCount(overdueReceivables.stream()
                .filter(r -> "MILD".equals(r.getOverdueLevel())).count());
        statistics.setModerateOverdueCount(overdueReceivables.stream()
                .filter(r -> "MODERATE".equals(r.getOverdueLevel())).count());
        statistics.setSevereOverdueCount(overdueReceivables.stream()
                .filter(r -> "SEVERE".equals(r.getOverdueLevel())).count());
        statistics.setBadDebtCount(overdueReceivables.stream()
                .filter(r -> "BAD_DEBT".equals(r.getOverdueLevel())).count());

        // 罚息统计
        List<OverduePenaltyRecord> penalties = overduePenaltyRecordRepository.findAll().stream()
                .filter(p -> !p.getCreatedAt().isBefore(request.getStartTime()) && !p.getCreatedAt().isAfter(request.getEndTime()))
                .collect(Collectors.toList());

        long totalPenaltyAmount = penalties.stream()
                .filter(p -> p.getPenaltyAmount() != null)
                .mapToLong(p -> p.getPenaltyAmount().multiply(BigDecimal.valueOf(100)).longValue())
                .sum();
        statistics.setTotalPenaltyAmount(totalPenaltyAmount);
        statistics.setMonthlyPenaltyAmount(totalPenaltyAmount); // 简化处理

        // 坏账统计
        List<BadDebtRecord> badDebts = badDebtRecordRepository.findAll().stream()
                .filter(b -> !b.getCreatedAt().isBefore(request.getStartTime()) && !b.getCreatedAt().isAfter(request.getEndTime()))
                .collect(Collectors.toList());

        long badDebtAmount = badDebts.stream()
                .filter(b -> b.getTotalLossAmount() != null)
                .mapToLong(b -> b.getTotalLossAmount().multiply(BigDecimal.valueOf(100)).longValue())
                .sum();
        statistics.setBadDebtAmount(badDebtAmount);

        long recoveredAmount = badDebts.stream()
                .filter(b -> b.getRecoveredAmount() != null)
                .mapToLong(b -> b.getRecoveredAmount().multiply(BigDecimal.valueOf(100)).longValue())
                .sum();
        statistics.setRecoveredBadDebtAmount(recoveredAmount);

        if (badDebtAmount > 0) {
            statistics.setBadDebtRecoveryRate(Double.valueOf(recoveredAmount) / Double.valueOf(badDebtAmount));
        }

        // 信用额度预警统计
        List<CreditLimitWarning> warnings = creditLimitWarningRepository.findAll().stream()
                .filter(w -> !w.getCreatedAt().isBefore(request.getStartTime()) && !w.getCreatedAt().isAfter(request.getEndTime()))
                .collect(Collectors.toList());

        statistics.setCreditLimitWarningCount((long) warnings.size());
        statistics.setLowWarningCount(warnings.stream().filter(w -> w.getWarningLevel() == com.fisco.app.enums.CreditWarningLevel.LOW).count());
        statistics.setMediumWarningCount(warnings.stream().filter(w -> w.getWarningLevel() == com.fisco.app.enums.CreditWarningLevel.MEDIUM).count());
        statistics.setHighWarningCount(warnings.stream().filter(w -> w.getWarningLevel() == com.fisco.app.enums.CreditWarningLevel.HIGH).count());

        // 生成趋势数据
        statistics.setOverdueAmountTrend(generateOverdueTrendData(overdueReceivables, request));
        statistics.setOverdueRateTrend(generateOverdueRateTrendData(overdueReceivables, request));

        return statistics;
    }

    /**
     * 查询综合报表
     */
    public ComprehensiveReportDTO getComprehensiveReport(StatisticsQueryRequest request) {
        log.info("查询综合报表: startTime={}, endTime={}", request.getStartTime(), request.getEndTime());

        ComprehensiveReportDTO report = new ComprehensiveReportDTO();

        // 设置报表周期
        String period = formatPeriod(request.getStartTime(), request.getEndTime());
        report.setPeriod(period);
        report.setReportGeneratedTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        // 获取业务统计
        BusinessStatisticsDTO businessStats = getBusinessStatistics(request);
        FinancingStatisticsDTO financingStats = getFinancingStatistics(request);
        RiskStatisticsDTO riskStats = getRiskStatistics(request);

        // 核心指标
        report.setTotalTransactionAmount(businessStats.getTotalBillAmount() + businessStats.getTotalReceivableAmount() + businessStats.getTotalReceiptValue());
        report.setMonthlyTransactionAmount(report.getTotalTransactionAmount()); // 简化处理

        // 企业统计
        List<Enterprise> allEnterprises = enterpriseRepository.findAll();
        report.setTotalEnterprises((long) allEnterprises.size());
        report.setActiveEnterprises(allEnterprises.stream().filter(e -> e.getStatus() == Enterprise.EnterpriseStatus.ACTIVE).count());

        // 业务量统计
        report.setBillBusinessVolume(businessStats.getTotalBills());
        report.setReceivableBusinessVolume(businessStats.getTotalReceivables());
        report.setWarehouseReceiptBusinessVolume(businessStats.getTotalWarehouseReceipts());
        report.setTotalBusinessVolume(report.getBillBusinessVolume() + report.getReceivableBusinessVolume() + report.getWarehouseReceiptBusinessVolume());

        // 融资统计
        report.setTotalFinancingAmount(financingStats.getTotalFinancingAmount());
        report.setFinancingCount(financingStats.getTotalFinancingCount());
        if (report.getFinancingCount() > 0) {
            report.setAverageFinancingAmount(report.getTotalFinancingAmount() / report.getFinancingCount());
        }

        // 风险指标
        report.setOverdueAmount(riskStats.getOverdueAmount());
        report.setBadDebtAmount(riskStats.getBadDebtAmount());
        report.setRiskReserve(report.getOverdueAmount() + report.getBadDebtAmount()); // 简化处理

        // 收入统计（简化）
        report.setFeeIncome(report.getTotalFinancingAmount() / 100); // 假设手续费率1%
        report.setInterestIncome(report.getTotalFinancingAmount() / 20); // 假设利率5%
        report.setPenaltyIncome(riskStats.getTotalPenaltyAmount());
        report.setTotalIncome(report.getFeeIncome() + report.getInterestIncome() + report.getPenaltyIncome());

        return report;
    }

    // ========== 辅助方法 ==========

    private String formatPeriod(LocalDateTime start, LocalDateTime end) {
        return start.format(MONTH_FORMATTER);
    }

    private List<BusinessStatisticsDTO.TrendData> generateTrendData(List<?> items, StatisticsQueryRequest request, String type) {
        // 简化实现，返回空列表
        return new ArrayList<>();
    }

    private List<FinancingStatisticsDTO.TrendData> generateFinancingTrendData(
            List<Bill> bills, List<Receivable> receivables, List<PledgeApplication> pledges, StatisticsQueryRequest request) {
        // 简化实现，返回空列表
        return new ArrayList<>();
    }

    private List<FinancingStatisticsDTO.RateTrendData> generateRateTrendData(
            List<Bill> bills, List<Receivable> receivables, StatisticsQueryRequest request) {
        // 简化实现，返回空列表
        return new ArrayList<>();
    }

    private List<RiskStatisticsDTO.TrendData> generateOverdueTrendData(
            List<Receivable> overdueReceivables, StatisticsQueryRequest request) {
        // 简化实现，返回空列表
        return new ArrayList<>();
    }

    private List<RiskStatisticsDTO.RateTrendData> generateOverdueRateTrendData(
            List<Receivable> overdueReceivables, StatisticsQueryRequest request) {
        // 简化实现，返回空列表
        return new ArrayList<>();
    }
}
