package com.fisco.app.service.receivable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fisco.app.dto.receivable.BadDebtQueryRequest;
import com.fisco.app.dto.receivable.BadDebtQueryResponse;
import com.fisco.app.dto.receivable.OverdueQueryRequest;
import com.fisco.app.dto.receivable.OverdueQueryResponse;
import com.fisco.app.dto.receivable.OverdueReceivableDTO;
import com.fisco.app.dto.receivable.PenaltyCalculateRequest;
import com.fisco.app.dto.receivable.PenaltyCalculateResponse;
import com.fisco.app.dto.receivable.RemindRequest;
import com.fisco.app.dto.receivable.RemindResponse;
import com.fisco.app.entity.receivable.Receivable;
import com.fisco.app.entity.risk.BadDebtRecord;
import com.fisco.app.entity.risk.OverduePenaltyRecord;
import com.fisco.app.entity.risk.OverdueRemindRecord;
import com.fisco.app.exception.BlockchainIntegrationException;
import com.fisco.app.exception.BusinessException;
import com.fisco.app.repository.receivable.ReceivableRepository;
import com.fisco.app.repository.risk.BadDebtRecordRepository;
import com.fisco.app.repository.risk.OverduePenaltyRecordRepository;
import com.fisco.app.repository.risk.OverdueRemindRecordRepository;
import com.fisco.app.service.blockchain.ContractService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 应收账款逾期管理Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReceivableOverdueService {

    private final ReceivableRepository receivableRepository;
    private final OverdueRemindRecordRepository remindRecordRepository;
    private final OverduePenaltyRecordRepository penaltyRecordRepository;
    private final BadDebtRecordRepository badDebtRecordRepository;
    private final ContractService contractService;

    // 逾期等级常量
    public static final String OVERDUE_LEVEL_MILD = "MILD";
    public static final String OVERDUE_LEVEL_MODERATE = "MODERATE";
    public static final String OVERDUE_LEVEL_SEVERE = "SEVERE";
    public static final String OVERDUE_LEVEL_BAD_DEBT = "BAD_DEBT";

    // 日利率常量
    private static final BigDecimal DAILY_RATE_MILD = new BigDecimal("0.0005");      // 0.05%
    private static final BigDecimal DAILY_RATE_MODERATE = new BigDecimal("0.0008");  // 0.08%
    private static final BigDecimal DAILY_RATE_SEVERE = new BigDecimal("0.0012");    // 0.12%

    /**
     * 查询逾期应收账款
     */
    public OverdueQueryResponse queryOverdueReceivables(OverdueQueryRequest request, @org.springframework.lang.NonNull String userAddress) {
        log.info("查询逾期应收账款: request={}, userAddress={}", request, userAddress);

        // 构建分页和排序
        Sort sort = Sort.by(
            "DESC".equalsIgnoreCase(request.getSortDirection()) ? Sort.Direction.DESC : Sort.Direction.ASC,
            request.getSortBy()
        );
        Pageable pageable = org.springframework.data.domain.PageRequest.of(request.getPage(), request.getSize(), sort);

        // 查询逾期账款
        List<Receivable> overdueReceivables = findOverdueReceivables(request, userAddress);

        // 转换为DTO
        List<OverdueReceivableDTO> dtoList = overdueReceivables.stream()
            .map(this::convertToOverdueReceivableDTO)
            .collect(Collectors.toList());

        // 分页处理
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), dtoList.size());
        List<OverdueReceivableDTO> pageContent = dtoList.subList(start, end);

        // 构建响应
        OverdueQueryResponse response = new OverdueQueryResponse();
        response.setContent(pageContent);
        response.setPageNumber(request.getPage());
        response.setPageSize(request.getSize());
        response.setTotalElements(dtoList.size());
        response.setTotalPages((int) Math.ceil((double) dtoList.size() / request.getSize()));
        response.setFirst(request.getPage() == 0);
        response.setLast(end >= dtoList.size());

        // 构建统计信息
        response.setStatistics(buildOverdueStatistics(dtoList));

        log.info("查询逾期应收账款完成: totalCount={}", dtoList.size());
        return response;
    }

    /**
     * 创建催收记录
     */
    @Transactional
    public RemindResponse createRemindRecord(@org.springframework.lang.NonNull String receivableId, RemindRequest request, @org.springframework.lang.NonNull String operatorAddress) {
        log.info("创建催收记录: receivableId={}, request={}, operator={}", receivableId, request, operatorAddress);

        // 验证应收账款是否存在
        Receivable receivable = receivableRepository.findById(receivableId)
            .orElseThrow(() -> new BusinessException.ReceivableNotFoundException(receivableId));

        // 更新逾期信息
        updateOverdueInfo(receivable);

        // 创建催收记录
        OverdueRemindRecord record = new OverdueRemindRecord();
        record.setReceivableId(receivableId);
        record.setRemindType(request.getRemindType());
        record.setRemindLevel(request.getRemindLevel());
        record.setRemindDate(LocalDateTime.now());
        record.setOperatorAddress(operatorAddress);
        record.setRemindContent(request.getRemindContent());
        record.setRemindResult(request.getRemindResult());
        record.setNextRemindDate(request.getNextRemindDate());
        record.setRemark(request.getRemark());

        OverdueRemindRecord savedRecord = remindRecordRepository.save(record);

        // 上链记录催收
        try {
            String txHash = contractService.recordRemindOnChain(
                receivableId,
                request.getRemindType().name(),
                operatorAddress,
                request.getRemindContent()
            );
            if (txHash != null) {
                savedRecord.setTxHash(txHash);
                remindRecordRepository.save(savedRecord);
                log.info("催收记录已上链: remindRecordId={}, txHash={}", savedRecord.getId(), txHash);
            }
        } catch (BlockchainIntegrationException e) {
            log.error("催收记录上链失败: receivableId={}, error={}", receivableId, e.getMessage());
            // 不影响业务流程，继续执行
        }

        // 更新应收账款的催收信息
        receivable.setLastRemindDate(LocalDateTime.now());
        receivable.setRemindCount((receivable.getRemindCount() != null ? receivable.getRemindCount() : 0) + 1);
        receivableRepository.save(receivable);

        // 构建响应
        RemindResponse response = new RemindResponse();
        response.setId(savedRecord.getId());
        response.setReceivableId(receivableId);
        response.setRemindType(savedRecord.getRemindType());
        response.setRemindLevel(savedRecord.getRemindLevel());
        response.setRemindDate(savedRecord.getRemindDate());
        response.setRemindContent(savedRecord.getRemindContent());
        response.setRemindResult(savedRecord.getRemindResult());
        response.setNextRemindDate(savedRecord.getNextRemindDate());
        response.setUpdatedRemindCount(receivable.getRemindCount());

        log.info("催收记录创建成功: id={}, remindCount={}", savedRecord.getId(), receivable.getRemindCount());
        return response;
    }

    /**
     * 计算罚息
     */
    @Transactional
    public PenaltyCalculateResponse calculatePenalty(@org.springframework.lang.NonNull String receivableId, PenaltyCalculateRequest request) {
        log.info("计算罚息: receivableId={}, request={}", receivableId, request);

        // 验证应收账款是否存在
        Receivable receivable = receivableRepository.findById(receivableId)
            .orElseThrow(() -> new BusinessException.ReceivableNotFoundException(receivableId));

        // 更新逾期信息
        updateOverdueInfo(receivable);

        // 计算罚息
        OverduePenaltyRecord record = new OverduePenaltyRecord();
        record.setReceivableId(receivableId);
        record.setPenaltyType(request.getPenaltyType());
        record.setPrincipalAmount(receivable.getAmount());

        if (request.getPenaltyType() == OverduePenaltyRecord.PenaltyType.AUTO) {
            // 自动计算
            int overdueDays = calculateOverdueDays(receivable.getDueDate(), LocalDateTime.now());
            record.setOverdueDays(overdueDays);
            record.setDailyRate(calculateDailyRate(overdueDays));
            record.setCalculateStartDate(receivable.getDueDate());
            record.setCalculateEndDate(LocalDateTime.now());
        } else {
            // 手动计算
            record.setOverdueDays(request.getOverdueDays());
            record.setDailyRate(request.getDailyRate());
            record.setCalculateStartDate(request.getCalculateStartDate());
            record.setCalculateEndDate(request.getCalculateEndDate());
        }

        // 计算罚息金额
        BigDecimal penaltyAmount = calculatePenaltyAmount(
            record.getPrincipalAmount(),
            record.getDailyRate(),
            record.getOverdueDays()
        );
        record.setPenaltyAmount(penaltyAmount);

        // 获取之前的累计罚息
        BigDecimal previousPenalty = penaltyRecordRepository.sumPenaltyByReceivableId(receivableId);
        if (previousPenalty == null) {
            previousPenalty = BigDecimal.ZERO;
        }
        BigDecimal totalPenalty = previousPenalty.add(penaltyAmount);
        record.setTotalPenaltyAmount(totalPenalty);
        record.setCalculateDate(LocalDateTime.now());

        // 保存罚息记录
        OverduePenaltyRecord savedRecord = penaltyRecordRepository.save(record);

        // 上链记录罚息
        try {
            String txHash = contractService.recordPenaltyOnChain(
                receivableId,
                request.getPenaltyType().name(),
                record.getPrincipalAmount(),
                record.getOverdueDays(),
                record.getDailyRate(),
                penaltyAmount,
                totalPenalty
            );
            if (txHash != null) {
                log.info("罚息记录已上链: penaltyRecordId={}, txHash={}", savedRecord.getId(), txHash);
            }
        } catch (BlockchainIntegrationException e) {
            log.error("罚息记录上链失败: receivableId={}, error={}", receivableId, e.getMessage());
            // 不影响业务流程，继续执行
        }

        // 更新应收账款的罚息金额
        receivable.setPenaltyAmount(totalPenalty);
        receivableRepository.save(receivable);

        // 构建响应
        PenaltyCalculateResponse response = new PenaltyCalculateResponse();
        response.setId(savedRecord.getId());
        response.setReceivableId(receivableId);
        response.setPenaltyType(savedRecord.getPenaltyType());
        response.setPrincipalAmount(savedRecord.getPrincipalAmount());
        response.setOverdueDays(savedRecord.getOverdueDays());
        response.setDailyRate(savedRecord.getDailyRate());
        response.setPenaltyAmount(savedRecord.getPenaltyAmount());
        response.setTotalPenaltyAmount(savedRecord.getTotalPenaltyAmount());
        response.setCalculateStartDate(savedRecord.getCalculateStartDate());
        response.setCalculateEndDate(savedRecord.getCalculateEndDate());
        response.setCalculateDate(savedRecord.getCalculateDate());
        response.setUpdatedReceivablePenaltyAmount(totalPenalty);

        log.info("罚息计算成功: penaltyAmount={}, totalPenalty={}", penaltyAmount, totalPenalty);
        return response;
    }

    /**
     * 认定坏账
     */
    @Transactional
    public BadDebtRecord createBadDebt(@org.springframework.lang.NonNull String receivableId,
                                       BadDebtRecord.BadDebtType badDebtType,
                                       String badDebtReason) {
        log.info("认定坏账: receivableId={}, type={}, reason={}", receivableId, badDebtType, badDebtReason);

        // 验证应收账款是否存在
        Receivable receivable = receivableRepository.findById(receivableId)
            .orElseThrow(() -> new BusinessException.ReceivableNotFoundException(receivableId));

        // 检查是否已经认定为坏账
        BadDebtRecord existingRecord = badDebtRecordRepository.findByReceivableId(receivableId);
        if (existingRecord != null) {
            throw new BusinessException("该应收账款已被认定为坏账");
        }

        // 更新逾期信息
        updateOverdueInfo(receivable);

        // 获取累计罚息
        BigDecimal totalPenalty = penaltyRecordRepository.sumPenaltyByReceivableId(receivableId);
        if (totalPenalty == null) {
            totalPenalty = BigDecimal.ZERO;
        }

        // 计算总损失金额
        BigDecimal totalLoss = receivable.getAmount().add(totalPenalty);

        // 创建坏账记录
        BadDebtRecord record = new BadDebtRecord();
        record.setReceivableId(receivableId);
        record.setBadDebtType(badDebtType);
        record.setPrincipalAmount(receivable.getAmount());
        record.setOverdueDays(receivable.getOverdueDays() != null ? receivable.getOverdueDays() : 0);
        record.setTotalPenaltyAmount(totalPenalty);
        record.setTotalLossAmount(totalLoss);
        record.setBadDebtReason(badDebtReason);
        record.setRecoveryStatus(BadDebtRecord.RecoveryStatus.NOT_RECOVERED);
        record.setRecoveredAmount(BigDecimal.ZERO);

        BadDebtRecord savedRecord = badDebtRecordRepository.save(record);

        // 上链记录坏账
        try {
            String txHash = contractService.recordBadDebtOnChain(
                receivableId,
                badDebtType.name(),
                record.getPrincipalAmount(),
                record.getOverdueDays(),
                record.getTotalPenaltyAmount(),
                record.getTotalLossAmount(),
                badDebtReason
            );
            if (txHash != null) {
                log.info("坏账记录已上链: badDebtRecordId={}, txHash={}", savedRecord.getId(), txHash);
            }
        } catch (BlockchainIntegrationException e) {
            log.error("坏账记录上链失败: receivableId={}, error={}", receivableId, e.getMessage());
            // 不影响业务流程，继续执行
        }

        // 更新应收账款的坏账信息
        receivable.setOverdueLevel(OVERDUE_LEVEL_BAD_DEBT);
        receivable.setBadDebtDate(LocalDateTime.now());
        receivable.setBadDebtReason(badDebtReason);
        receivableRepository.save(receivable);

        // 上链更新逾期状态
        try {
            String txHash = contractService.updateOverdueStatusOnChain(
                receivableId,
                OVERDUE_LEVEL_BAD_DEBT,
                receivable.getOverdueDays()
            );
            if (txHash != null) {
                log.info("逾期状态已更新上链: receivableId={}, txHash={}", receivableId, txHash);
            }
        } catch (BlockchainIntegrationException e) {
            log.error("逾期状态更新上链失败: receivableId={}, error={}", receivableId, e.getMessage());
            // 不影响业务流程，继续执行
        }

        log.info("坏账认定成功: id={}, totalLoss={}", savedRecord.getId(), totalLoss);
        return savedRecord;
    }

    /**
     * 坏账回收
     */
    @Transactional
    public BadDebtRecord recoverBadDebt(@org.springframework.lang.NonNull String receivableId,
                                        BigDecimal recoveredAmount,
                                        BadDebtRecord.RecoveryStatus recoveryStatus) {
        log.info("坏账回收: receivableId={}, amount={}, status={}", receivableId, recoveredAmount, recoveryStatus);

        // 获取坏账记录
        BadDebtRecord record = badDebtRecordRepository.findByReceivableId(receivableId);
        if (record == null) {
            throw new BusinessException("坏账记录不存在");
        }

        // 更新回收信息
        record.setRecoveredAmount(recoveredAmount);
        record.setRecoveryStatus(recoveryStatus);
        if (recoveryStatus != BadDebtRecord.RecoveryStatus.NOT_RECOVERED) {
            record.setRecoveryDate(LocalDateTime.now());
        }

        BadDebtRecord savedRecord = badDebtRecordRepository.save(record);

        log.info("坏账回收成功: id={}, recoveredAmount={}", savedRecord.getId(), recoveredAmount);
        return savedRecord;
    }

    /**
     * 查询坏账（数据库分页优化版）
     */
    @SuppressWarnings("nullness")
    public BadDebtQueryResponse queryBadDebts(BadDebtQueryRequest request, @org.springframework.lang.NonNull String userAddress) {
        log.info("查询坏账（数据库分页）: request={}, userAddress={}", request, userAddress);

        // 1. 构建分页请求
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(),
            Sort.by(Sort.Direction.DESC, "createdAt"));

        // 2. 数据库分页查询（条件过滤在SQL中完成）
        Page<BadDebtRecord> badDebtPage = badDebtRecordRepository.findBadDebtsByConditions(
            request.getBadDebtType(),
            request.getRecoveryStatus(),
            request.getOverdueDaysMin(),
            request.getCreatedDateStart(),
            request.getCreatedDateEnd(),
            pageable
        );

        // 3. 提取所有应收账款ID（批量查询）
        List<String> receivableIds = badDebtPage.getContent().stream()
            .map(BadDebtRecord::getReceivableId)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());

        // 4. 批量查询应收账款信息（避免N+1查询）
        Map<String, Receivable> receivableMap;
        if (!receivableIds.isEmpty()) {
            List<Receivable> receivables = receivableRepository.findAllById(receivableIds);
            receivableMap = receivables.stream()
                .collect(Collectors.toMap(Receivable::getId, Function.identity()));
        } else {
            receivableMap = new HashMap<>();
        }

        // 5. 构建DTO，同时进行权限过滤
        List<BadDebtQueryResponse.BadDebtDTO> dtoList = badDebtPage.getContent().stream()
            .filter(record -> {
                // 权限过滤
                if (userAddress != null && record.getReceivableId() != null) {
                    Receivable receivable = receivableMap.get(record.getReceivableId());
                    if (receivable != null) {
                        boolean isRelated = userAddress.equals(receivable.getSupplierAddress()) ||
                            userAddress.equals(receivable.getFinancierAddress()) ||
                            userAddress.equals(receivable.getCurrentHolder());
                        return isRelated;
                    }
                    return false;
                }
                return true;
            })
            .map(record -> {
                BadDebtQueryResponse.BadDebtDTO dto = new BadDebtQueryResponse.BadDebtDTO();
                dto.setId(record.getId());
                dto.setReceivableId(record.getReceivableId());
                dto.setBadDebtType(record.getBadDebtType());
                dto.setPrincipalAmount(record.getPrincipalAmount());
                dto.setOverdueDays(record.getOverdueDays());
                dto.setTotalPenaltyAmount(record.getTotalPenaltyAmount());
                dto.setTotalLossAmount(record.getTotalLossAmount());
                dto.setBadDebtReason(record.getBadDebtReason());
                dto.setRecoveryStatus(record.getRecoveryStatus());
                dto.setRecoveredAmount(record.getRecoveredAmount());
                dto.setRecoveryDate(record.getRecoveryDate());
                dto.setCreatedAt(record.getCreatedAt());

                // 从Map中获取应收账款信息（避免再次查询）
                Receivable receivable = receivableMap.get(record.getReceivableId());
                if (receivable != null) {
                    dto.setSupplierAddress(receivable.getSupplierAddress());
                    dto.setFinancierAddress(receivable.getFinancierAddress());
                }
                return dto;
            })
            .collect(Collectors.toList());

        // 6. 构建响应（使用分页信息）
        BadDebtQueryResponse response = new BadDebtQueryResponse();
        response.setContent(dtoList);
        response.setPageNumber(badDebtPage.getNumber());
        response.setPageSize(badDebtPage.getSize());
        response.setTotalElements(badDebtPage.getTotalElements());
        response.setTotalPages(badDebtPage.getTotalPages());
        response.setFirst(badDebtPage.isFirst());
        response.setLast(badDebtPage.isLast());

        // 7. 构建统计信息
        response.setStatistics(buildBadDebtStatistics(dtoList));

        log.info("查询坏账完成: totalCount={}, totalPages={}",
            badDebtPage.getTotalElements(), badDebtPage.getTotalPages());
        return response;
    }

    // ==================== 辅助方法 ====================

    /**
     * 查询逾期应收账款（带过滤条件）
     */
    private List<Receivable> findOverdueReceivables(OverdueQueryRequest request, String userAddress) {
        // 获取所有应收账款
        List<Receivable> allReceivables = receivableRepository.findAll();

        // 过滤出逾期账款
        List<Receivable> overdueReceivables = allReceivables.stream()
            .filter(receivable -> {
                // 计算逾期天数
                int overdueDays = calculateOverdueDays(receivable.getDueDate(), LocalDateTime.now());
                if (overdueDays <= 0) {
                    return false; // 未逾期
                }

                // 应用过滤条件
                if (request.getOverdueLevel() != null) {
                    String level = determineOverdueLevel(overdueDays);
                    if (!level.equals(request.getOverdueLevel())) {
                        return false;
                    }
                }

                if (request.getSupplierAddress() != null &&
                    !receivable.getSupplierAddress().equals(request.getSupplierAddress())) {
                    return false;
                }

                if (request.getCoreEnterpriseAddress() != null &&
                    !receivable.getCoreEnterpriseAddress().equals(request.getCoreEnterpriseAddress())) {
                    return false;
                }

                if (request.getFinancierAddress() != null &&
                    !request.getFinancierAddress().equals(receivable.getFinancierAddress())) {
                    return false;
                }

                if (request.getStatus() != null && receivable.getStatus() != request.getStatus()) {
                    return false;
                }

                if (request.getOverdueDaysMin() != null && overdueDays < request.getOverdueDaysMin()) {
                    return false;
                }

                if (request.getOverdueDaysMax() != null && overdueDays > request.getOverdueDaysMax()) {
                    return false;
                }

                if (request.getDueDateStart() != null && receivable.getDueDate().isBefore(request.getDueDateStart())) {
                    return false;
                }

                if (request.getDueDateEnd() != null && receivable.getDueDate().isAfter(request.getDueDateEnd())) {
                    return false;
                }

                if (request.getReminded() != null) {
                    boolean hasReminded = receivable.getRemindCount() != null && receivable.getRemindCount() > 0;
                    if (request.getReminded() && !hasReminded) {
                        return false;
                    }
                    if (!request.getReminded() && hasReminded) {
                        return false;
                    }
                }

                // 权限过滤：只能查看自己相关的账款
                if (userAddress != null) {
                    boolean isRelated = userAddress.equals(receivable.getSupplierAddress()) ||
                        userAddress.equals(receivable.getFinancierAddress()) ||
                        userAddress.equals(receivable.getCurrentHolder());
                    if (!isRelated) {
                        return false;
                    }
                }

                return true;
            })
            .collect(Collectors.toList());

        // 更新逾期信息
        overdueReceivables.forEach(this::updateOverdueInfo);

        return overdueReceivables;
    }

    /**
     * 计算逾期天数
     */
    public int calculateOverdueDays(LocalDateTime dueDate, LocalDateTime currentDate) {
        if (dueDate == null || currentDate == null) {
            return 0;
        }
        return (int) ChronoUnit.DAYS.between(dueDate, currentDate);
    }

    /**
     * 判断逾期等级
     */
    public String determineOverdueLevel(int overdueDays) {
        if (overdueDays <= 30) {
            return OVERDUE_LEVEL_MILD;
        } else if (overdueDays <= 90) {
            return OVERDUE_LEVEL_MODERATE;
        } else if (overdueDays <= 179) {
            return OVERDUE_LEVEL_SEVERE;
        } else {
            return OVERDUE_LEVEL_BAD_DEBT;
        }
    }

    /**
     * 计算日利率
     */
    public BigDecimal calculateDailyRate(int overdueDays) {
        String level = determineOverdueLevel(overdueDays);
        switch (level) {
            case OVERDUE_LEVEL_MILD:
                return DAILY_RATE_MILD;
            case OVERDUE_LEVEL_MODERATE:
                return DAILY_RATE_MODERATE;
            case OVERDUE_LEVEL_SEVERE:
            case OVERDUE_LEVEL_BAD_DEBT:
                return DAILY_RATE_SEVERE;
            default:
                return DAILY_RATE_MILD;
        }
    }

    /**
     * 计算罚息金额
     * 公式：罚息 = 本金 × 日利率 × 逾期天数
     */
    public BigDecimal calculatePenaltyAmount(BigDecimal principal, BigDecimal dailyRate, int overdueDays) {
        return principal.multiply(dailyRate).multiply(new BigDecimal(overdueDays))
            .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 更新应收账款的逾期信息
     */
    private void updateOverdueInfo(Receivable receivable) {
        int overdueDays = calculateOverdueDays(receivable.getDueDate(), LocalDateTime.now());
        if (overdueDays > 0) {
            receivable.setOverdueDays(overdueDays);
            receivable.setOverdueLevel(determineOverdueLevel(overdueDays));
            receivable.setOverdueCalculatedDate(LocalDateTime.now());
        }
    }

    /**
     * 转换为逾期账款DTO
     */
    private OverdueReceivableDTO convertToOverdueReceivableDTO(Receivable receivable) {
        OverdueReceivableDTO dto = new OverdueReceivableDTO();
        dto.setId(receivable.getId());
        dto.setSupplierAddress(receivable.getSupplierAddress());
        dto.setCoreEnterpriseAddress(receivable.getCoreEnterpriseAddress());
        dto.setAmount(receivable.getAmount());
        dto.setCurrency(receivable.getCurrency());
        dto.setIssueDate(receivable.getIssueDate());
        dto.setDueDate(receivable.getDueDate());
        dto.setDescription(receivable.getDescription());
        dto.setStatus(receivable.getStatus());
        dto.setCurrentHolder(receivable.getCurrentHolder());
        dto.setFinancierAddress(receivable.getFinancierAddress());
        dto.setFinanceAmount(receivable.getFinanceAmount());
        dto.setFinanceRate(receivable.getFinanceRate());
        dto.setFinanceDate(receivable.getFinanceDate());
        dto.setOverdueLevel(receivable.getOverdueLevel());
        dto.setOverdueDays(receivable.getOverdueDays());
        dto.setPenaltyAmount(receivable.getPenaltyAmount() != null ? receivable.getPenaltyAmount() : BigDecimal.ZERO);
        dto.setLastRemindDate(receivable.getLastRemindDate());
        dto.setRemindCount(receivable.getRemindCount());
        dto.setBadDebtDate(receivable.getBadDebtDate());
        dto.setBadDebtReason(receivable.getBadDebtReason());
        dto.setCreatedAt(receivable.getCreatedAt());
        dto.setUpdatedAt(receivable.getUpdatedAt());
        return dto;
    }

    /**
     * 构建逾期统计信息
     */
    private OverdueQueryResponse.OverdueStatistics buildOverdueStatistics(List<OverdueReceivableDTO> dtoList) {
        OverdueQueryResponse.OverdueStatistics statistics = new OverdueQueryResponse.OverdueStatistics();
        statistics.setTotalCount(dtoList.size());
        statistics.setTotalAmount(dtoList.stream()
            .map(OverdueReceivableDTO::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add));
        statistics.setMildCount(dtoList.stream()
            .filter(dto -> OVERDUE_LEVEL_MILD.equals(dto.getOverdueLevel()))
            .count());
        statistics.setModerateCount(dtoList.stream()
            .filter(dto -> OVERDUE_LEVEL_MODERATE.equals(dto.getOverdueLevel()))
            .count());
        statistics.setSevereCount(dtoList.stream()
            .filter(dto -> OVERDUE_LEVEL_SEVERE.equals(dto.getOverdueLevel()))
            .count());
        statistics.setBadDebtCount(dtoList.stream()
            .filter(dto -> OVERDUE_LEVEL_BAD_DEBT.equals(dto.getOverdueLevel()))
            .count());
        statistics.setTotalPenaltyAmount(dtoList.stream()
            .map(OverdueReceivableDTO::getPenaltyAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add));
        return statistics;
    }

    /**
     * 构建坏账统计信息
     */
    private BadDebtQueryResponse.BadDebtStatistics buildBadDebtStatistics(List<BadDebtQueryResponse.BadDebtDTO> dtoList) {
        BadDebtQueryResponse.BadDebtStatistics statistics = new BadDebtQueryResponse.BadDebtStatistics();
        statistics.setTotalCount(dtoList.size());
        statistics.setTotalPrincipalAmount(dtoList.stream()
            .map(BadDebtQueryResponse.BadDebtDTO::getPrincipalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add));
        statistics.setTotalLossAmount(dtoList.stream()
            .map(BadDebtQueryResponse.BadDebtDTO::getTotalLossAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add));
        statistics.setTotalRecoveredAmount(dtoList.stream()
            .map(BadDebtQueryResponse.BadDebtDTO::getRecoveredAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add));

        // 计算回收率
        if (statistics.getTotalLossAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal recoveryRate = statistics.getTotalRecoveredAmount()
                .divide(statistics.getTotalLossAmount(), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
            statistics.setRecoveryRate(recoveryRate);
        } else {
            statistics.setRecoveryRate(BigDecimal.ZERO);
        }

        statistics.setNotRecoveredCount(dtoList.stream()
            .filter(dto -> dto.getRecoveryStatus() == BadDebtRecord.RecoveryStatus.NOT_RECOVERED)
            .count());
        statistics.setPartialRecoveredCount(dtoList.stream()
            .filter(dto -> dto.getRecoveryStatus() == BadDebtRecord.RecoveryStatus.PARTIAL_RECOVERED)
            .count());
        statistics.setFullRecoveredCount(dtoList.stream()
            .filter(dto -> dto.getRecoveryStatus() == BadDebtRecord.RecoveryStatus.FULL_RECOVERED)
            .count());

        return statistics;
    }
}
