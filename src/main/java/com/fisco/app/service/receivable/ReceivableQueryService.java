package com.fisco.app.service.receivable;

import com.fisco.app.dto.receivable.*;
import com.fisco.app.entity.receivable.Receivable;
import com.fisco.app.entity.receivable.ReceivableTransfer;
import com.fisco.app.exception.BusinessException;
import com.fisco.app.repository.receivable.ReceivableRepository;
import com.fisco.app.repository.receivable.ReceivableTransferRepository;
import com.fisco.app.repository.receivable.ReceivableRepaymentRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 应收账款查询统计Service
 *
 * 提供应收账款的查询历史、统计、账龄分析和核销功能
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-09
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReceivableQueryService {

    private final ReceivableRepository receivableRepository;
    private final ReceivableTransferRepository transferRepository;
    private final ReceivableRepaymentRecordRepository repaymentRecordRepository;

    // ==================== 查询历史功能 ====================

    /**
     * 查询应收账款的转让历史
     *
     * @param receivableId 应收账款ID
     * @param userAddress 用户地址
     * @return 转让历史列表
     */
    public List<TransferHistoryResponse> getTransferHistory(
            @NonNull String receivableId,
            @NonNull String userAddress) {

        log.info("查询转让历史: receivableId={}, userAddress={}", receivableId, userAddress);

        // 权限验证：只有参与方可以查询
        Receivable receivable = receivableRepository.findByReceivableId(receivableId)
                .orElseThrow(() -> new BusinessException("应收账款不存在: " + receivableId));

        if (!isParticipant(receivable, userAddress)) {
            throw new BusinessException("无权限查询此应收账款的转让历史");
        }

        List<ReceivableTransfer> transfers =
                transferRepository.findByReceivableIdOrderByTimestampDesc(receivableId);

        log.info("查询到{}条转让记录", transfers.size());

        return transfers.stream()
                .map(this::convertToTransferHistoryResponse)
                .collect(Collectors.toList());
    }

    /**
     * 查询应收账款的融资记录
     *
     * @param receivableId 应收账款ID
     * @param userAddress 用户地址
     * @return 融资记录列表
     */
    public List<FinanceRecordResponse> getFinanceRecords(
            @NonNull String receivableId,
            @NonNull String userAddress) {

        log.info("查询融资记录: receivableId={}, userAddress={}", receivableId, userAddress);

        // 权限验证
        Receivable receivable = receivableRepository.findByReceivableId(receivableId)
                .orElseThrow(() -> new BusinessException("应收账款不存在: " + receivableId));

        if (!isParticipant(receivable, userAddress)) {
            throw new BusinessException("无权限查询此应收账款的融资记录");
        }

        // 只有已融资或已还款的应收账款才有融资记录
        if (receivable.getStatus() != Receivable.ReceivableStatus.FINANCED &&
            receivable.getStatus() != Receivable.ReceivableStatus.REPAID) {
            log.info("应收账款未融资，返回空列表");
            return Collections.emptyList();
        }

        // 检查是否有融资信息
        if (receivable.getFinanceAmount() == null) {
            log.info("应收账款无融资信息");
            return Collections.emptyList();
        }

        FinanceRecordResponse response = new FinanceRecordResponse();
        response.setReceivableId(receivableId);
        response.setFinanceAmount(receivable.getFinanceAmount());
        response.setFinanceRate(receivable.getFinanceRate());
        response.setFinanceDate(receivable.getFinanceDate());
        response.setFinancierAddress(receivable.getFinancierAddress());
        response.setPrincipalAmount(receivable.getAmount());
        response.setTxHash(receivable.getTxHash());
        response.setStatus(receivable.getStatus().name());
        response.setStatusName(getStatusDisplayName(receivable.getStatus()));

        // 计算融资比例
        if (receivable.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal ratio = receivable.getFinanceAmount()
                    .divide(receivable.getAmount(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            response.setFinanceRatio(ratio);
        }

        log.info("查询到融资记录: 融资金额={}, 融资利率={}%",
                response.getFinanceAmount(), response.getFinanceRate());

        return Collections.singletonList(response);
    }

    // ==================== 统计分析功能 ====================

    /**
     * 查询应收账款统计
     *
     * @param supplierAddress 供应商地址（可选）
     * @param coreEnterpriseAddress 核心企业地址（可选）
     * @param financierAddress 资金方地址（可选）
     * @return 统计数据
     */
    public ReceivableStatisticsResponse getStatistics(
            String supplierAddress,
            String coreEnterpriseAddress,
            String financierAddress) {

        log.info("查询应收账款统计: supplier={}, core={}, financier={}",
                supplierAddress, coreEnterpriseAddress, financierAddress);

        ReceivableStatisticsResponse statistics = new ReceivableStatisticsResponse();
        statistics.setGeneratedAt(LocalDateTime.now());

        // 1. 查询符合条件的应收账款
        List<Receivable> receivables = queryReceivables(
                supplierAddress, coreEnterpriseAddress, financierAddress);

        // 2. 基础统计
        calculateBasicStatistics(statistics, receivables);

        // 3. 状态分布统计
        calculateStatusDistribution(statistics, receivables);

        // 4. 融资统计
        calculateFinanceStatistics(statistics, receivables);

        // 5. 逾期统计
        calculateOverdueStatistics(statistics, receivables);

        log.info("统计计算完成: totalCount={}, totalAmount={}, financedCount={}, overdueCount={}",
                statistics.getTotalCount(), statistics.getTotalAmount(),
                statistics.getFinancedCount(), statistics.getOverdueCount());

        return statistics;
    }

    // ==================== 账龄分析功能 ====================

    /**
     * 账龄分析
     *
     * @param supplierAddress 供应商地址（可选）
     * @param coreEnterpriseAddress 核心企业地址（可选）
     * @param financierAddress 资金方地址（可选）
     * @return 账龄分析数据
     */
    public AgedAnalysisResponse getAgedAnalysis(
            String supplierAddress,
            String coreEnterpriseAddress,
            String financierAddress) {

        log.info("查询账龄分析: supplier={}, core={}, financier={}",
                supplierAddress, coreEnterpriseAddress, financierAddress);

        AgedAnalysisResponse analysis = new AgedAnalysisResponse();
        analysis.setAnalysisTime(LocalDateTime.now());

        List<Receivable> receivables = queryReceivables(
                supplierAddress, coreEnterpriseAddress, financierAddress);

        LocalDateTime now = LocalDateTime.now();
        BigDecimal totalAmount = receivables.stream()
                .map(Receivable::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 初始化账龄段
        AgedAnalysisResponse.AgedBucket current = new AgedAnalysisResponse.AgedBucket();
        AgedAnalysisResponse.AgedBucket overdue30 = new AgedAnalysisResponse.AgedBucket();
        AgedAnalysisResponse.AgedBucket overdue60 = new AgedAnalysisResponse.AgedBucket();
        AgedAnalysisResponse.AgedBucket overdue90 = new AgedAnalysisResponse.AgedBucket();
        AgedAnalysisResponse.AgedBucket overdue90Plus = new AgedAnalysisResponse.AgedBucket();

        // 分类统计
        for (Receivable r : receivables) {
            // 已还款或已取消的不参与账龄分析
            if (r.getStatus() == Receivable.ReceivableStatus.REPAID ||
                r.getStatus() == Receivable.ReceivableStatus.CANCELLED) {
                continue;
            }

            long daysOverdue = ChronoUnit.DAYS.between(r.getDueDate(), now);

            if (daysOverdue <= 0) {
                // 未到期
                incrementBucket(current, r);
            } else if (daysOverdue <= 30) {
                incrementBucket(overdue30, r);
            } else if (daysOverdue <= 60) {
                incrementBucket(overdue60, r);
            } else if (daysOverdue <= 90) {
                incrementBucket(overdue90, r);
            } else {
                incrementBucket(overdue90Plus, r);
            }
        }

        // 计算占比
        if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
            calculatePercentage(current, totalAmount);
            calculatePercentage(overdue30, totalAmount);
            calculatePercentage(overdue60, totalAmount);
            calculatePercentage(overdue90, totalAmount);
            calculatePercentage(overdue90Plus, totalAmount);
        }

        analysis.setCurrent(current);
        analysis.setOverdue30(overdue30);
        analysis.setOverdue60(overdue60);
        analysis.setOverdue90(overdue90);
        analysis.setOverdue90Plus(overdue90Plus);

        log.info("账龄分析完成: 总金额={}, 未到期={}, 逾期1-30天={}",
                totalAmount, current.getAmount(), overdue30.getAmount());

        return analysis;
    }

    // ==================== 坏账核销功能 ====================

    /**
     * 核销坏账
     *
     * @param receivableId 应收账款ID
     * @param request 核销请求
     * @param operatorAddress 操作人地址
     */
    @Transactional(rollbackFor = Exception.class)
    public void writeOffBadDebt(
            @NonNull String receivableId,
            @NonNull WriteOffRequest request,
            @NonNull String operatorAddress) {

        log.info("==================== 坏账核销开始 ====================");
        log.info("核销信息: receivableId={}, reason={}", receivableId, request.getReason());

        long startTime = System.currentTimeMillis();

        try {
            // 1. 查询应收账款
            Receivable receivable = receivableRepository.findByReceivableId(receivableId)
                    .orElseThrow(() -> new BusinessException("应收账款不存在: " + receivableId));

            log.debug("✓ 应收账款查询成功: status={}, overdueDays={}",
                    receivable.getStatus(), receivable.getOverdueDays());

            // 2. 验证状态
            boolean canWriteOff = receivable.getStatus() == Receivable.ReceivableStatus.DEFAULTED;
            if (!canWriteOff && receivable.getOverdueDays() != null &&
                receivable.getOverdueDays() >= 180) {
                canWriteOff = true;
            }

            if (!canWriteOff) {
                log.error("状态验证失败: 只有已违约或逾期超过180天的应收账款可以核销");
                throw new BusinessException("只有已违约或逾期超过180天的应收账款可以核销");
            }
            log.debug("✓ 状态验证通过");

            // 3. 更新状态和核销信息
            receivable.setStatus(Receivable.ReceivableStatus.CANCELLED);
            receivable.setBadDebtDate(LocalDateTime.now());
            receivable.setBadDebtReason(request.getReason());
            receivableRepository.save(receivable);

            log.info("✓✓✓ 坏账核销完成: receivableId={}, reason={}",
                    receivableId, request.getReason());

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 坏账核销完成，耗时={}ms", duration);
            log.info("==================== 坏账核销结束 ====================");

        } catch (BusinessException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 坏账核销失败: receivableId={}, 耗时={}ms, error={}",
                    receivableId, duration, e.getMessage());
            log.info("==================== 坏账核销失败（结束） ====================");
            throw e;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 坏账核销失败: receivableId={}, 耗时={}ms, error={}",
                    receivableId, duration, e.getMessage(), e);
            log.info("==================== 坏账核销失败（结束） ====================");
            throw new BusinessException(500, "核销失败: " + e.getMessage(), e);
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 判断用户是否是应收账款的参与方
     */
    private boolean isParticipant(Receivable receivable, String userAddress) {
        return userAddress.equals(receivable.getSupplierAddress()) ||
                userAddress.equals(receivable.getCoreEnterpriseAddress()) ||
                userAddress.equals(receivable.getCurrentHolder()) ||
                (receivable.getFinancierAddress() != null &&
                 userAddress.equals(receivable.getFinancierAddress()));
    }

    /**
     * 查询应收账款（根据条件过滤）
     */
    private List<Receivable> queryReceivables(
            String supplierAddress,
            String coreEnterpriseAddress,
            String financierAddress) {

        if (supplierAddress != null) {
            return receivableRepository.findBySupplierAddress(supplierAddress);
        } else if (coreEnterpriseAddress != null) {
            return receivableRepository.findByCoreEnterpriseAddress(coreEnterpriseAddress);
        } else if (financierAddress != null) {
            return receivableRepository.findByFinancierAddress(financierAddress);
        } else {
            return receivableRepository.findAll();
        }
    }

    /**
     * 计算基础统计
     */
    private void calculateBasicStatistics(
            ReceivableStatisticsResponse statistics,
            List<Receivable> receivables) {

        statistics.setTotalCount((long) receivables.size());

        BigDecimal totalAmount = receivables.stream()
                .map(Receivable::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        statistics.setTotalAmount(totalAmount);

        // 计算已还金额（从还款记录表中聚合）
        BigDecimal totalRepaid = BigDecimal.ZERO;
        for (Receivable r : receivables) {
            BigDecimal repaid = repaymentRecordRepository.totalRepaidAmountByReceivable(r.getId());
            totalRepaid = totalRepaid.add(repaid);
        }
        statistics.setTotalRepaidAmount(totalRepaid);
        statistics.setTotalOutstandingAmount(totalAmount.subtract(totalRepaid));

        // 平均融资金额
        BigDecimal avgFinance = receivables.stream()
                .filter(r -> r.getFinanceAmount() != null)
                .map(Receivable::getFinanceAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long financedCount = receivables.stream()
                .filter(r -> r.getFinanceAmount() != null)
                .count();
        if (financedCount > 0) {
            statistics.setAvgFinanceAmount(avgFinance.divide(
                    BigDecimal.valueOf(financedCount), 2, RoundingMode.HALF_UP));
        }
    }

    /**
     * 计算状态分布统计
     */
    private void calculateStatusDistribution(
            ReceivableStatisticsResponse statistics,
            List<Receivable> receivables) {

        Map<Receivable.ReceivableStatus, List<Receivable>> groupedByStatus =
                receivables.stream()
                        .collect(Collectors.groupingBy(Receivable::getStatus));

        List<ReceivableStatisticsResponse.StatusStatistics> statusDistribution =
                groupedByStatus.entrySet().stream()
                        .map(entry -> {
                            ReceivableStatisticsResponse.StatusStatistics stats =
                                    new ReceivableStatisticsResponse.StatusStatistics();
                            stats.setStatus(entry.getKey().name());
                            stats.setStatusName(getStatusDisplayName(entry.getKey()));
                            stats.setCount((long) entry.getValue().size());

                            BigDecimal totalAmount = entry.getValue().stream()
                                    .map(Receivable::getAmount)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                            stats.setTotalAmount(totalAmount);

                            if (statistics.getTotalCount() > 0) {
                                double percentage = (double) entry.getValue().size() /
                                        statistics.getTotalCount() * 100;
                                stats.setPercentage(Math.round(percentage * 100.0) / 100.0);
                            }

                            return stats;
                        })
                        .collect(Collectors.toList());

        statistics.setStatusDistribution(statusDistribution);
    }

    /**
     * 计算融资统计
     */
    private void calculateFinanceStatistics(
            ReceivableStatisticsResponse statistics,
            List<Receivable> receivables) {

        List<Receivable> financed = receivables.stream()
                .filter(r -> r.getStatus() == Receivable.ReceivableStatus.FINANCED ||
                           r.getStatus() == Receivable.ReceivableStatus.REPAID)
                .collect(Collectors.toList());

        statistics.setFinancedCount((long) financed.size());

        BigDecimal financedAmount = financed.stream()
                .map(Receivable::getFinanceAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        statistics.setFinancedAmount(financedAmount);

        statistics.setUnfinancedCount(
                statistics.getTotalCount() - statistics.getFinancedCount());
        statistics.setUnfinancedAmount(
                statistics.getTotalAmount().subtract(financedAmount));

        // 融资率
        if (statistics.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal rate = financedAmount
                    .divide(statistics.getTotalAmount(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            statistics.setFinanceRate(rate);
        }
    }

    /**
     * 计算逾期统计
     */
    private void calculateOverdueStatistics(
            ReceivableStatisticsResponse statistics,
            List<Receivable> receivables) {

        LocalDateTime now = LocalDateTime.now();

        List<Receivable> overdue = receivables.stream()
                .filter(r -> r.getDueDate().isBefore(now) &&
                           r.getStatus() != Receivable.ReceivableStatus.REPAID &&
                           r.getStatus() != Receivable.ReceivableStatus.CANCELLED)
                .collect(Collectors.toList());

        statistics.setOverdueCount((long) overdue.size());

        BigDecimal overdueAmount = overdue.stream()
                .map(Receivable::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        statistics.setOverdueAmount(overdueAmount);

        // 逾期率
        if (statistics.getTotalCount() > 0) {
            BigDecimal rate = BigDecimal.valueOf(statistics.getOverdueCount())
                    .divide(BigDecimal.valueOf(statistics.getTotalCount()), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            statistics.setOverdueRate(rate);
        }
    }

    /**
     * 增加账龄段计数
     */
    private void incrementBucket(AgedAnalysisResponse.AgedBucket bucket, Receivable receivable) {
        bucket.setCount(bucket.getCount() == null ? 1 : bucket.getCount() + 1);
        bucket.setAmount(bucket.getAmount() == null ?
                receivable.getAmount() :
                bucket.getAmount().add(receivable.getAmount()));
    }

    /**
     * 计算占比
     */
    private void calculatePercentage(AgedAnalysisResponse.AgedBucket bucket, BigDecimal total) {
        if (bucket.getAmount() != null) {
            double percentage = bucket.getAmount()
                    .divide(total, 4, RoundingMode.HALF_UP)
                    .doubleValue() * 100;
            bucket.setPercentage(Math.round(percentage * 100.0) / 100.0);
        }
    }

    /**
     * 转换为转让历史响应
     */
    private TransferHistoryResponse convertToTransferHistoryResponse(ReceivableTransfer transfer) {
        TransferHistoryResponse response = new TransferHistoryResponse();
        response.setId(transfer.getId());
        response.setReceivableId(transfer.getReceivableId());
        response.setFromAddress(transfer.getFromAddress());
        response.setToAddress(transfer.getToAddress());
        response.setAmount(transfer.getAmount());
        response.setTransferType(transfer.getTransferType());
        response.setTransferTypeName(getTransferTypeName(transfer.getTransferType()));
        response.setTimestamp(transfer.getTimestamp());
        response.setTxHash(transfer.getTxHash());
        return response;
    }

    /**
     * 获取状态显示名称
     */
    private String getStatusDisplayName(Receivable.ReceivableStatus status) {
        switch (status) {
            case CREATED: return "已创建";
            case CONFIRMED: return "已确认";
            case FINANCED: return "已融资";
            case REPAID: return "已还款";
            case DEFAULTED: return "已违约";
            case CANCELLED: return "已取消";
            case SPLITTING: return "拆分中";
            case SPLIT: return "已拆分";
            case MERGING: return "合并中";
            case MERGED: return "已合并";
            default: return status.name();
        }
    }

    /**
     * 获取转让类型名称
     */
    private String getTransferTypeName(String transferType) {
        if (transferType == null) {
            return null;
        }
        switch (transferType) {
            case "financing": return "融资";
            case "transfer": return "转让";
            case "repayment": return "还款";
            default: return transferType;
        }
    }
}
