package com.fisco.app.service.warehouse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fisco.app.dto.warehouse.WarehouseReceiptStatisticsDTO;
import com.fisco.app.dto.warehouse.WarehouseReceiptStatisticsDTO.EnterpriseStatistics;
import com.fisco.app.dto.warehouse.WarehouseReceiptStatisticsDTO.GoodsTypeStatistics;
import com.fisco.app.dto.warehouse.WarehouseReceiptStatisticsDTO.StatusStatistics;
import com.fisco.app.entity.warehouse.ElectronicWarehouseReceipt;
import com.fisco.app.entity.warehouse.ElectronicWarehouseReceipt.ReceiptStatus;
import com.fisco.app.repository.warehouse.ElectronicWarehouseReceiptRepository;
import com.fisco.app.repository.warehouse.ReceiptMergeApplicationRepository;
import com.fisco.app.repository.warehouse.ReceiptSplitApplicationRepository;
import com.fisco.app.repository.warehouse.ReceiptCancelApplicationRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * 仓单统计Service
 *
 * 提供仓单的多维度统计分析功能
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-09
 * @version 1.0
 */
@Slf4j
@Service
public class WarehouseReceiptStatisticsService {

    @Autowired
    private ElectronicWarehouseReceiptRepository repository;

    @Autowired
    private ReceiptSplitApplicationRepository splitApplicationRepository;

    @Autowired
    private ReceiptMergeApplicationRepository mergeApplicationRepository;

    @Autowired
    private ReceiptCancelApplicationRepository cancelApplicationRepository;

    /**
     * 查询仓单统计
     *
     * @param startTime 统计开始时间（可选）
     * @param endTime 统计结束时间（可选）
     * @return 仓单统计数据
     */
    public WarehouseReceiptStatisticsDTO getReceiptStatistics(
            LocalDateTime startTime,
            LocalDateTime endTime) {

        log.info("查询仓单统计: startTime={}, endTime={}", startTime, endTime);

        WarehouseReceiptStatisticsDTO statistics = new WarehouseReceiptStatisticsDTO();

        // 设置统计时间范围
        statistics.setStartTime(startTime);
        statistics.setEndTime(endTime);
        statistics.setGeneratedAt(LocalDateTime.now());

        // ==================== 基础统计 ====================
        calculateBasicStatistics(statistics, startTime, endTime);

        // ==================== 状态分布统计 ====================
        calculateStatusDistribution(statistics, startTime, endTime);

        // ==================== 货物类型分布统计 ====================
        calculateGoodsTypeDistribution(statistics, startTime, endTime);

        // ==================== 企业分布统计 ====================
        calculateEnterpriseDistribution(statistics, startTime, endTime);

        // ==================== 风险统计 ====================
        calculateRiskStatistics(statistics);

        // ==================== 操作统计 ====================
        calculateOperationStatistics(statistics, startTime, endTime);

        log.info("仓单统计计算完成: totalReceipts={}, totalValue={}",
            statistics.getTotalReceipts(), statistics.getTotalValue());

        return statistics;
    }

    /**
     * 计算基础统计
     */
    private void calculateBasicStatistics(
            WarehouseReceiptStatisticsDTO statistics,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        List<ElectronicWarehouseReceipt> receipts = getReceiptsInTimeRange(startTime, endTime);

        statistics.setTotalReceipts((long) receipts.size());

        BigDecimal totalValue = receipts.stream()
            .map(ElectronicWarehouseReceipt::getTotalValue)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        statistics.setTotalValue(totalValue);

        BigDecimal totalQuantity = receipts.stream()
            .map(ElectronicWarehouseReceipt::getQuantity)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        statistics.setTotalQuantity(totalQuantity);
    }

    /**
     * 计算状态分布统计
     */
    private void calculateStatusDistribution(
            WarehouseReceiptStatisticsDTO statistics,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        List<ElectronicWarehouseReceipt> receipts = getReceiptsInTimeRange(startTime, endTime);

        Map<ReceiptStatus, List<ElectronicWarehouseReceipt>> groupedByStatus = receipts.stream()
            .collect(Collectors.groupingBy(ElectronicWarehouseReceipt::getReceiptStatus));

        List<StatusStatistics> statusDistribution = groupedByStatus.entrySet().stream()
            .map(entry -> {
                StatusStatistics stats = new StatusStatistics();
                stats.setStatus(entry.getKey().name());
                stats.setStatusName(getStatusDisplayName(entry.getKey()));
                stats.setCount((long) entry.getValue().size());

                BigDecimal totalValue = entry.getValue().stream()
                    .map(ElectronicWarehouseReceipt::getTotalValue)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                stats.setTotalValue(totalValue);

                // 计算占比
                if (statistics.getTotalReceipts() > 0) {
                    double percentage = (double) entry.getValue().size() / statistics.getTotalReceipts() * 100;
                    stats.setPercentage(Math.round(percentage * 100.0) / 100.0);
                } else {
                    stats.setPercentage(0.0);
                }

                return stats;
            })
            .collect(Collectors.toList());

        statistics.setStatusDistribution(statusDistribution);
    }

    /**
     * 计算货物类型分布统计
     */
    private void calculateGoodsTypeDistribution(
            WarehouseReceiptStatisticsDTO statistics,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        List<ElectronicWarehouseReceipt> receipts = getReceiptsInTimeRange(startTime, endTime);

        Map<String, List<ElectronicWarehouseReceipt>> groupedByGoods = receipts.stream()
            .collect(Collectors.groupingBy(r ->
                r.getGoodsName() != null ? r.getGoodsName() : "未知货物"));

        List<GoodsTypeStatistics> goodsTypeDistribution = groupedByGoods.entrySet().stream()
            .map(entry -> {
                GoodsTypeStatistics stats = new GoodsTypeStatistics();
                stats.setGoodsName(entry.getKey());
                stats.setCount((long) entry.getValue().size());

                BigDecimal totalQuantity = entry.getValue().stream()
                    .map(ElectronicWarehouseReceipt::getQuantity)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                stats.setTotalQuantity(totalQuantity);

                BigDecimal totalValue = entry.getValue().stream()
                    .map(ElectronicWarehouseReceipt::getTotalValue)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                stats.setTotalValue(totalValue);

                // 计算平均单价
                if (totalQuantity.compareTo(BigDecimal.ZERO) > 0) {
                    stats.setAvgUnitPrice(totalValue.divide(totalQuantity, 2, RoundingMode.HALF_UP));
                } else {
                    stats.setAvgUnitPrice(BigDecimal.ZERO);
                }

                return stats;
            })
            .sorted((a, b) -> b.getTotalValue().compareTo(a.getTotalValue())) // 按总价值降序
            .collect(Collectors.toList());

        statistics.setGoodsTypeDistribution(goodsTypeDistribution);
    }

    /**
     * 计算企业分布统计（Top 10）
     */
    private void calculateEnterpriseDistribution(
            WarehouseReceiptStatisticsDTO statistics,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        List<ElectronicWarehouseReceipt> receipts = getReceiptsInTimeRange(startTime, endTime);

        Map<String, List<ElectronicWarehouseReceipt>> groupedByOwner = receipts.stream()
            .collect(Collectors.groupingBy(ElectronicWarehouseReceipt::getOwnerId));

        List<EnterpriseStatistics> enterpriseDistribution = groupedByOwner.entrySet().stream()
            .map(entry -> {
                EnterpriseStatistics stats = new EnterpriseStatistics();
                stats.setEnterpriseId(entry.getKey());

                // 获取企业名称（从第一个仓单中获取）
                stats.setEnterpriseName(entry.getValue().get(0).getOwnerName());
                stats.setReceiptCount((long) entry.getValue().size());

                BigDecimal totalValue = entry.getValue().stream()
                    .map(ElectronicWarehouseReceipt::getTotalValue)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                stats.setTotalValue(totalValue);

                return stats;
            })
            .sorted((a, b) -> b.getTotalValue().compareTo(a.getTotalValue())) // 按总价值降序
            .limit(10) // 只取Top 10
            .collect(Collectors.toList());

        statistics.setOwnerDistribution(enterpriseDistribution);
    }

    /**
     * 计算风险统计
     */
    private void calculateRiskStatistics(WarehouseReceiptStatisticsDTO statistics) {
        LocalDateTime now = LocalDateTime.now();

        // 查询所有正常状态的仓单
        List<ElectronicWarehouseReceipt> normalReceipts = repository.findByReceiptStatus(ReceiptStatus.NORMAL);

        // 即将过期（7天内）
        List<ElectronicWarehouseReceipt> expiringSoon = normalReceipts.stream()
            .filter(r -> r.getExpiryDate() != null)
            .filter(r -> {
                long daysUntilExpiry = ChronoUnit.DAYS.between(now, r.getExpiryDate());
                return daysUntilExpiry >= 0 && daysUntilExpiry <= 7;
            })
            .collect(Collectors.toList());

        statistics.setExpiringSoonCount((long) expiringSoon.size());
        statistics.setExpiringSoonValue(expiringSoon.stream()
            .map(ElectronicWarehouseReceipt::getTotalValue)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add));

        // 已过期
        List<ElectronicWarehouseReceipt> expired = normalReceipts.stream()
            .filter(r -> r.getExpiryDate() != null)
            .filter(r -> r.getExpiryDate().isBefore(now))
            .collect(Collectors.toList());

        statistics.setExpiredCount((long) expired.size());
        statistics.setExpiredValue(expired.stream()
            .map(ElectronicWarehouseReceipt::getTotalValue)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add));

        // 已冻结
        List<ElectronicWarehouseReceipt> frozen = repository.findByReceiptStatus(ReceiptStatus.FROZEN);

        statistics.setFrozenCount((long) frozen.size());
        statistics.setFrozenValue(frozen.stream()
            .map(ElectronicWarehouseReceipt::getTotalValue)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /**
     * 计算操作统计
     */
    private void calculateOperationStatistics(
            WarehouseReceiptStatisticsDTO statistics,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        // 统计待处理的申请数量
        if (startTime != null && endTime != null) {
            // 统计指定时间范围内创建的申请数量
            statistics.setSplitApplicationCount(splitApplicationRepository.findAll().stream()
                .filter(a -> a.getCreatedAt() != null)
                .filter(a -> !a.getCreatedAt().isBefore(startTime) && !a.getCreatedAt().isAfter(endTime))
                .count());

            statistics.setMergeApplicationCount(mergeApplicationRepository.findAll().stream()
                .filter(a -> a.getCreatedAt() != null)
                .filter(a -> !a.getCreatedAt().isBefore(startTime) && !a.getCreatedAt().isAfter(endTime))
                .count());

            statistics.setCancelApplicationCount(cancelApplicationRepository.findAll().stream()
                .filter(a -> a.getCreatedAt() != null)
                .filter(a -> !a.getCreatedAt().isBefore(startTime) && !a.getCreatedAt().isAfter(endTime))
                .count());
        } else {
            // 如果没有指定时间范围，统计所有待处理的申请
            statistics.setSplitApplicationCount((long) splitApplicationRepository.findByRequestStatus("PENDING").size());
            statistics.setMergeApplicationCount((long) mergeApplicationRepository.findByRequestStatus("PENDING").size());
            statistics.setCancelApplicationCount((long) cancelApplicationRepository.findByRequestStatus("PENDING").size());
        }
    }

    /**
     * 获取指定时间范围内的仓单
     */
    private List<ElectronicWarehouseReceipt> getReceiptsInTimeRange(
            LocalDateTime startTime,
            LocalDateTime endTime) {

        if (startTime != null && endTime != null) {
            // 使用findAll()然后在内存中过滤（因为findByCreatedAtBetween方法不存在）
            return repository.findAll().stream()
                .filter(r -> r.getCreatedAt() != null)
                .filter(r -> !r.getCreatedAt().isBefore(startTime) && !r.getCreatedAt().isAfter(endTime))
                .collect(Collectors.toList());
        } else {
            // 如果没有指定时间范围，返回所有仓单
            return repository.findAll();
        }
    }

    /**
     * 获取状态显示名称
     */
    private String getStatusDisplayName(ReceiptStatus status) {
        switch (status) {
            case DRAFT:
                return "草稿";
            case PENDING_ONCHAIN:
                return "待上链";
            case NORMAL:
                return "正常";
            case ONCHAIN_FAILED:
                return "上链失败";
            case PLEDGED:
                return "已质押";
            case TRANSFERRED:
                return "已转让";
            case FROZEN:
                return "已冻结";
            case SPLITTING:
                return "拆分中";
            case SPLIT:
                return "已拆分";
            case MERGING:
                return "合并中";
            case MERGED:
                return "已合并";
            case CANCELLING:
                return "作废中";
            case CANCELLED:
                return "已作废";
            case EXPIRED:
                return "已过期";
            case DELIVERED:
                return "已提货";
            default:
                return status.name();
        }
    }
}
