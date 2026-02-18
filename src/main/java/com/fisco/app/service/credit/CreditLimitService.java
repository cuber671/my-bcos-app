package com.fisco.app.service.credit;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Subquery;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fisco.app.dto.credit.CreditLimitAdjustRequestDTO;
import com.fisco.app.dto.credit.CreditLimitAdjustResponse;
import com.fisco.app.dto.credit.CreditLimitAvailableResponse;
import com.fisco.app.dto.credit.CreditLimitCreateRequest;
import com.fisco.app.dto.credit.CreditLimitDTO;
import com.fisco.app.dto.credit.CreditLimitFreezeResponse;
import com.fisco.app.dto.credit.CreditLimitQueryRequest;
import com.fisco.app.dto.credit.CreditLimitQueryResponse;
import com.fisco.app.dto.credit.CreditLimitUsageDTO;
import com.fisco.app.dto.credit.CreditLimitUsageQueryRequest;
import com.fisco.app.dto.credit.CreditLimitUsageQueryResponse;
import com.fisco.app.dto.credit.CreditLimitWarningDTO;
import com.fisco.app.dto.credit.CreditLimitWarningQueryRequest;
import com.fisco.app.dto.credit.CreditLimitWarningQueryResponse;
import com.fisco.app.entity.credit.CreditLimit;
import com.fisco.app.entity.credit.CreditLimitAdjustRequest;
import com.fisco.app.entity.credit.CreditLimitUsage;
import com.fisco.app.entity.credit.CreditLimitWarning;
import com.fisco.app.entity.enterprise.Enterprise;
import com.fisco.app.enums.CreditAdjustRequestStatus;
import com.fisco.app.enums.CreditLimitStatus;
import com.fisco.app.enums.CreditLimitType;
import com.fisco.app.enums.CreditUsageType;
import com.fisco.app.enums.CreditWarningLevel;
import com.fisco.app.exception.BusinessException;
import com.fisco.app.repository.credit.CreditLimitAdjustRequestRepository;
import com.fisco.app.repository.credit.CreditLimitRepository;
import com.fisco.app.repository.credit.CreditLimitUsageRepository;
import com.fisco.app.repository.credit.CreditLimitWarningRepository;
import com.fisco.app.repository.enterprise.EnterpriseRepository;
import com.fisco.app.service.blockchain.ContractService;

import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 信用额度Service
 */
@Slf4j
@ConditionalOnProperty(name = "fisco.enabled", havingValue = "true", matchIfMissing = true)
@Service
@Api(tags = "信用额度服务")
@RequiredArgsConstructor
public class CreditLimitService {

    private final CreditLimitRepository creditLimitRepository;
    private final CreditLimitUsageRepository usageRepository;
    private final CreditLimitAdjustRequestRepository adjustRequestRepository;
    private final CreditLimitWarningRepository warningRepository;
    private final EnterpriseRepository enterpriseRepository;
    private final ContractService contractService;

    // ==================== 额度管理 ====================

    /**
     * 创建信用额度
     */
    @Transactional(rollbackFor = Exception.class)
    public CreditLimitDTO createCreditLimit(CreditLimitCreateRequest request, String operatorAddress) {
        log.info("==================== 创建信用额度开始 ====================");
        log.info("创建信息: enterpriseAddress={}, limitType={}, totalLimit={}元",
                request.getEnterpriseAddress(), request.getLimitType(), request.getTotalLimit());

        long startTime = System.currentTimeMillis();

        try {
            // 1. 验证企业是否存在
            log.debug("验证企业: enterpriseAddress={}", request.getEnterpriseAddress());
            Enterprise enterprise = enterpriseRepository.findByAddress(request.getEnterpriseAddress())
                    .orElseThrow(() -> new BusinessException("企业不存在: " + request.getEnterpriseAddress()));
            log.debug("✓ 企业验证通过: enterpriseName={}", enterprise.getName());

            // 2. 检查是否已存在同类型的活跃额度
            log.debug("检查是否已存在同类型的活跃额度");
            Optional<CreditLimit> existingLimit = creditLimitRepository
                    .findByEnterpriseAddressAndLimitTypeAndStatus(
                            request.getEnterpriseAddress(),
                            request.getLimitType(),
                            CreditLimitStatus.ACTIVE);
            if (existingLimit.isPresent()) {
                log.warn("企业已存在同类型的活跃额度: limitId={}", existingLimit.get().getId());
                throw new BusinessException("企业已存在同类型的活跃额度，请先调整或失效现有额度");
            }
            log.debug("✓ 无重复额度");

            // 3. 验证日期
            log.debug("验证日期合理性");
            if (request.getExpiryDate() != null && request.getExpiryDate().isBefore(request.getEffectiveDate())) {
                log.error("失效日期必须晚于生效日期");
                throw new BusinessException("失效日期必须晚于生效日期");
            }
            log.debug("✓ 日期验证通过");

            // 4. 创建额度实体
            log.debug("构建额度实体");
            CreditLimit creditLimit = new CreditLimit();
            creditLimit.setEnterpriseAddress(request.getEnterpriseAddress());
            creditLimit.setEnterpriseName(enterprise.getName());
            creditLimit.setLimitType(request.getLimitType());
            // 将元转换为分存储
            creditLimit.setTotalLimit(request.getTotalLimit().multiply(new BigDecimal("100")).longValue());
            creditLimit.setUsedLimit(0L);
            creditLimit.setFrozenLimit(0L);
            creditLimit.setWarningThreshold(request.getWarningThreshold() != null ?
                    request.getWarningThreshold() : 80);
            creditLimit.setEffectiveDate(request.getEffectiveDate());
            creditLimit.setExpiryDate(request.getExpiryDate());
            creditLimit.setStatus(CreditLimitStatus.ACTIVE);
            creditLimit.setApproverAddress(request.getApproverAddress());
            creditLimit.setApproveReason(request.getApproveReason());
            creditLimit.setApproveTime(LocalDateTime.now());
            creditLimit.setOverdueCount(0);
            creditLimit.setBadDebtCount(0);
            creditLimit.setRiskLevel(CreditLimit.RiskLevel.LOW);

            // 5. 保存到数据库
            log.debug("保存额度到数据库");
            CreditLimit saved = creditLimitRepository.save(creditLimit);
            log.info("✓ 数据库保存成功: limitId={}", saved.getId());

            // 6. 上链记录（可选）
            String txHash = null;
            try {
                if (contractService != null) {
                    txHash = contractService.recordCreditLimitOnChain(saved);
                    log.info("✓ 额度已上链: txHash={}", txHash);
                    saved.setTxHash(txHash);
                    saved = creditLimitRepository.save(saved);
                }
            } catch (Exception e) {
                log.warn("上链失败，但数据库保存成功: {}", e.getMessage());
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 信用额度创建完成: limitId={}, 耗时={}ms", saved.getId(), duration);
            log.info("==================== 创建信用额度结束 ====================");

            return convertToDTO(saved);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 创建信用额度失败: 耗时={}ms, error={}", duration, e.getMessage(), e);
            log.info("==================== 创建信用额度失败（结束） ====================");
            throw e;
        }
    }

    /**
     * 查询信用额度列表（分页、多条件筛选）
     */
    public CreditLimitQueryResponse queryCreditLimits(CreditLimitQueryRequest request) {
        log.info("==================== 查询信用额度开始 ====================");
        log.info("查询条件: enterpriseAddress={}, limitType={}, status={}, page={}, size={}",
                request.getEnterpriseAddress(), request.getLimitType(), request.getStatus(),
                request.getPage(), request.getSize());

        long startTime = System.currentTimeMillis();

        try {
            // 1. 构建查询条件
            Specification<CreditLimit> spec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();

                if (request.getEnterpriseAddress() != null) {
                    predicates.add(cb.equal(root.get("enterpriseAddress"), request.getEnterpriseAddress()));
                }
                if (request.getLimitType() != null) {
                    predicates.add(cb.equal(root.get("limitType"), request.getLimitType()));
                }
                if (request.getStatus() != null) {
                    predicates.add(cb.equal(root.get("status"), request.getStatus()));
                }
                if (request.getRiskLevel() != null) {
                    predicates.add(cb.equal(root.get("riskLevel"), request.getRiskLevel()));
                }
                if (request.getEffectiveDateStart() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("effectiveDate"), request.getEffectiveDateStart()));
                }
                if (request.getEffectiveDateEnd() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("effectiveDate"), request.getEffectiveDateEnd()));
                }
                if (request.getExpiryDateStart() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("expiryDate"), request.getExpiryDateStart()));
                }
                if (request.getExpiryDateEnd() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("expiryDate"), request.getExpiryDateEnd()));
                }
                if (request.getTotalLimitMin() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("totalLimit"), request.getTotalLimitMin()));
                }
                if (request.getTotalLimitMax() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("totalLimit"), request.getTotalLimitMax()));
                }
                if (request.getOverdueCountMin() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("overdueCount"), request.getOverdueCountMin()));
                }
                if (request.getOverdueCountMax() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("overdueCount"), request.getOverdueCountMax()));
                }
                if (request.getNeedsWarning() != null && request.getNeedsWarning()) {
                    // 使用率 >= 预警阈值
                    predicates.add(cb.greaterThanOrEqualTo(
                            cb.prod(root.get("usedLimit"), 100.0),
                            cb.prod(root.get("totalLimit"), root.get("warningThreshold").as(Double.class))
                    ));
                }

                return cb.and(predicates.toArray(new Predicate[0]));
            };

            // 2. 构建排序
            Sort sort = buildSort(request.getSortBy(), request.getSortDirection());
            Pageable pageable = PageRequest.of(
                    request.getPage() != null ? request.getPage() : 0,
                    request.getSize() != null ? request.getSize() : 10,
                    sort);

            // 3. 执行查询
            Page<CreditLimit> page = creditLimitRepository.findAll(spec, pageable);

            // 4. 转换为DTO
            List<CreditLimitDTO> content = page.getContent().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            // 5. 构建响应
            CreditLimitQueryResponse response = new CreditLimitQueryResponse();
            response.setContent(content);
            response.setPageNumber(page.getNumber());
            response.setPageSize(page.getSize());
            response.setTotalPages(page.getTotalPages());
            response.setTotalElements(page.getTotalElements());
            response.setFirst(page.isFirst());
            response.setLast(page.isLast());

            // 6. 构建统计信息
            response.setStatistics(buildStatistics(content));

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 查询完成: 总记录数={}, 耗时={}ms", page.getTotalElements(), duration);
            log.info("==================== 查询信用额度结束 ====================");

            return response;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 查询信用额度失败: 耗时={}ms, error={}", duration, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 查询单个额度详情
     */
    @SuppressWarnings("null")
    public CreditLimitDTO getCreditLimitById(@NonNull String id) {
        log.debug("查询额度详情: limitId={}", id);
        CreditLimit creditLimit = creditLimitRepository.findById(id)
                .orElseThrow(() -> new BusinessException("额度不存在: " + id));
        return convertToDTO(creditLimit);
    }

    /**
     * 查询企业的所有额度
     */
    public List<CreditLimitDTO> getCreditLimitsByEnterprise(@NonNull String enterpriseAddress) {
        log.debug("查询企业的所有额度: enterpriseAddress={}", enterpriseAddress);
        List<CreditLimit> limits = creditLimitRepository.findByEnterpriseAddress(enterpriseAddress);
        return limits.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 检查额度是否充足
     */
    public boolean isLimitSufficient(String enterpriseAddress, CreditLimitType limitType, Long amountInFen) {
        log.debug("检查额度是否充足: enterpriseAddress={}, limitType={}, amount={}分",
                enterpriseAddress, limitType, amountInFen);

        Optional<CreditLimit> limitOpt = creditLimitRepository
                .findByEnterpriseAddressAndLimitTypeAndStatus(
                        enterpriseAddress, limitType, CreditLimitStatus.ACTIVE);

        if (!limitOpt.isPresent()) {
            log.warn("企业无此类型的活跃额度");
            return false;
        }

        CreditLimit limit = limitOpt.get();
        Long availableLimit = limit.getAvailableLimit();
        boolean sufficient = availableLimit >= amountInFen;

        log.debug("额度检查结果: totalLimit={}分, usedLimit={}分, frozenLimit={}分, availableLimit={}分, required={}分, sufficient={}",
                limit.getTotalLimit(), limit.getUsedLimit(), limit.getFrozenLimit(), availableLimit, amountInFen, sufficient);

        return sufficient;
    }

    /**
     * 使用额度
     */
    @Transactional(rollbackFor = Exception.class)
    public CreditLimitUsageDTO useCredit(@NonNull String creditLimitId, Long amountInFen,
                                         @NonNull String businessType, @NonNull String businessId,
                                         @NonNull String operatorAddress, @NonNull String operatorName, String remark) {
        log.info("==================== 使用额度开始 ====================");
        log.info("使用信息: creditLimitId={}, amount={}分, businessType={}, businessId={}",
                creditLimitId, amountInFen, businessType, businessId);

        long startTime = System.currentTimeMillis();

        try {
            // 1. 获取额度
            CreditLimit creditLimit = creditLimitRepository.findById(creditLimitId)
                    .orElseThrow(() -> new BusinessException("额度不存在: " + creditLimitId));

            // 2. 检查额度状态
            if (creditLimit.getStatus() != CreditLimitStatus.ACTIVE) {
                throw new BusinessException("额度状态不是生效中，无法使用: " + creditLimit.getStatus());
            }

            // 3. 检查可用额度是否充足
            Long beforeAvailable = creditLimit.getAvailableLimit();
            if (beforeAvailable < amountInFen) {
                throw new BusinessException(String.format(
                        "可用额度不足: 可用=%d分, 需要=%d分",
                        beforeAvailable, amountInFen));
            }

            // 4. 更新额度
            Long beforeUsed = creditLimit.getUsedLimit();
            creditLimit.setUsedLimit(beforeUsed + amountInFen);
            CreditLimit updatedLimit = creditLimitRepository.save(creditLimit);

            // 5. 创建使用记录
            CreditLimitUsage usage = new CreditLimitUsage();
            usage.setCreditLimitId(creditLimitId);
            usage.setUsageType(CreditUsageType.USE);
            usage.setBusinessType(businessType);
            usage.setBusinessId(businessId);
            usage.setAmount(amountInFen);
            usage.setBeforeAvailable(beforeAvailable);
            usage.setAfterAvailable(updatedLimit.getAvailableLimit());
            usage.setBeforeUsed(beforeUsed);
            usage.setAfterUsed(updatedLimit.getUsedLimit());
            usage.setBeforeFrozen(creditLimit.getFrozenLimit());
            usage.setAfterFrozen(creditLimit.getFrozenLimit());
            usage.setOperatorAddress(operatorAddress);
            usage.setOperatorName(operatorName);
            usage.setUsageDate(LocalDateTime.now());
            usage.setRemark(remark);
            CreditLimitUsage savedUsage = usageRepository.save(usage);

            // 6. 检查是否需要预警
            checkAndCreateWarning(updatedLimit);

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 额度使用完成: usageId={}, 耗时={}ms", savedUsage.getId(), duration);
            log.info("==================== 使用额度结束 ====================");

            return convertToUsageDTO(savedUsage);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 使用额度失败: 耗时={}ms, error={}", duration, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 释放额度
     */
    @Transactional(rollbackFor = Exception.class)
    public CreditLimitUsageDTO releaseCredit(@NonNull String creditLimitId, Long amountInFen,
                                            @NonNull String businessType, @NonNull String businessId,
                                            @NonNull String operatorAddress, @NonNull String operatorName, String remark) {
        log.info("==================== 释放额度开始 ====================");
        log.info("释放信息: creditLimitId={}, amount={}分, businessType={}, businessId={}",
                creditLimitId, amountInFen, businessType, businessId);

        long startTime = System.currentTimeMillis();

        try {
            // 1. 获取额度
            CreditLimit creditLimit = creditLimitRepository.findById(creditLimitId)
                    .orElseThrow(() -> new BusinessException("额度不存在: " + creditLimitId));

            // 2. 检查已使用额度是否充足
            Long beforeUsed = creditLimit.getUsedLimit();
            if (beforeUsed < amountInFen) {
                throw new BusinessException(String.format(
                        "已使用额度不足，无法释放: 已使用=%d分, 尝试释放=%d分",
                        beforeUsed, amountInFen));
            }

            // 3. 更新额度
            creditLimit.setUsedLimit(beforeUsed - amountInFen);
            CreditLimit updatedLimit = creditLimitRepository.save(creditLimit);

            // 4. 创建使用记录
            CreditLimitUsage usage = new CreditLimitUsage();
            usage.setCreditLimitId(creditLimitId);
            usage.setUsageType(CreditUsageType.RELEASE);
            usage.setBusinessType(businessType);
            usage.setBusinessId(businessId);
            usage.setAmount(-amountInFen); // 负数表示释放
            usage.setBeforeAvailable(creditLimit.getAvailableLimit() + amountInFen);
            usage.setAfterAvailable(updatedLimit.getAvailableLimit());
            usage.setBeforeUsed(beforeUsed);
            usage.setAfterUsed(updatedLimit.getUsedLimit());
            usage.setBeforeFrozen(creditLimit.getFrozenLimit());
            usage.setAfterFrozen(creditLimit.getFrozenLimit());
            usage.setOperatorAddress(operatorAddress);
            usage.setOperatorName(operatorName);
            usage.setUsageDate(LocalDateTime.now());
            usage.setRemark(remark);
            CreditLimitUsage savedUsage = usageRepository.save(usage);

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 额度释放完成: usageId={}, 耗时={}ms", savedUsage.getId(), duration);
            log.info("==================== 释放额度结束 ====================");

            return convertToUsageDTO(savedUsage);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 释放额度失败: 耗时={}ms, error={}", duration, e.getMessage(), e);
            throw e;
        }
    }

    // ==================== 额度冻结/解冻 ====================

    /**
     * 冻结额度
     */
    @Transactional(rollbackFor = Exception.class)
    public CreditLimitFreezeResponse freezeCreditLimit(@NonNull String creditLimitId, @NonNull String reason, @NonNull String operatorAddress) {
        log.info("==================== 冻结额度开始 ====================");
        log.info("冻结信息: creditLimitId={}, reason={}", creditLimitId, reason);

        long startTime = System.currentTimeMillis();

        try {
            CreditLimit creditLimit = creditLimitRepository.findById(creditLimitId)
                    .orElseThrow(() -> new BusinessException("额度不存在: " + creditLimitId));

            CreditLimitStatus previousStatus = creditLimit.getStatus();

            if (previousStatus == CreditLimitStatus.FROZEN) {
                throw new BusinessException("额度已经是冻结状态");
            }

            // 更新状态为冻结
            creditLimit.setStatus(CreditLimitStatus.FROZEN);
            CreditLimit saved = creditLimitRepository.save(creditLimit);

            // 上链记录
            String txHash = null;
            try {
                if (contractService != null) {
                    txHash = contractService.freezeCreditLimitOnChain(creditLimitId, reason);
                    if (txHash != null) {
                        saved.setTxHash(txHash);
                        creditLimitRepository.save(saved);
                        log.info("✓ 额度冻结已上链: txHash={}", txHash);
                    }
                }
            } catch (Exception e) {
                log.warn("上链失败，但数据库操作成功: {}", e.getMessage());
            }

            // 构建响应
            CreditLimitFreezeResponse response = new CreditLimitFreezeResponse();
            response.setId(saved.getId());
            response.setEnterpriseAddress(saved.getEnterpriseAddress());
            response.setEnterpriseName(saved.getEnterpriseName());
            response.setLimitType(saved.getLimitType());
            response.setPreviousStatus(previousStatus);
            response.setCurrentStatus(saved.getStatus());
            response.setTotalLimit(convertFenToYuan(saved.getTotalLimit()));
            response.setUsedLimit(convertFenToYuan(saved.getUsedLimit()));
            response.setFrozenLimit(convertFenToYuan(saved.getFrozenLimit()));
            response.setAvailableLimit(convertFenToYuan(saved.getAvailableLimit()));
            response.setReason(reason);
            response.setOperatorAddress(operatorAddress);
            response.setOperationTime(LocalDateTime.now());

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 额度冻结完成: limitId={}, 耗时={}ms", creditLimitId, duration);
            log.info("==================== 冻结额度结束 ====================");

            return response;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 冻结额度失败: 耗时={}ms, error={}", duration, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 解冻额度
     */
    @Transactional(rollbackFor = Exception.class)
    public CreditLimitFreezeResponse unfreezeCreditLimit(@NonNull String creditLimitId, @NonNull String reason, @NonNull String operatorAddress) {
        log.info("==================== 解冻额度开始 ====================");
        log.info("解冻信息: creditLimitId={}, reason={}", creditLimitId, reason);

        long startTime = System.currentTimeMillis();

        try {
            CreditLimit creditLimit = creditLimitRepository.findById(creditLimitId)
                    .orElseThrow(() -> new BusinessException("额度不存在: " + creditLimitId));

            CreditLimitStatus previousStatus = creditLimit.getStatus();

            if (previousStatus != CreditLimitStatus.FROZEN) {
                throw new BusinessException("额度不是冻结状态，无法解冻");
            }

            // 检查是否到期
            if (creditLimit.getExpiryDate() != null && creditLimit.getExpiryDate().isBefore(LocalDateTime.now())) {
                creditLimit.setStatus(CreditLimitStatus.EXPIRED);
            } else {
                creditLimit.setStatus(CreditLimitStatus.ACTIVE);
            }

            CreditLimit saved = creditLimitRepository.save(creditLimit);

            // 上链记录
            String txHash = null;
            try {
                if (contractService != null) {
                    txHash = contractService.unfreezeCreditLimitOnChain(creditLimitId, reason);
                    if (txHash != null) {
                        saved.setTxHash(txHash);
                        creditLimitRepository.save(saved);
                        log.info("✓ 额度解冻已上链: txHash={}", txHash);
                    }
                }
            } catch (Exception e) {
                log.warn("上链失败，但数据库操作成功: {}", e.getMessage());
            }

            // 构建响应
            CreditLimitFreezeResponse response = new CreditLimitFreezeResponse();
            response.setId(saved.getId());
            response.setEnterpriseAddress(saved.getEnterpriseAddress());
            response.setEnterpriseName(saved.getEnterpriseName());
            response.setLimitType(saved.getLimitType());
            response.setPreviousStatus(previousStatus);
            response.setCurrentStatus(saved.getStatus());
            response.setTotalLimit(convertFenToYuan(saved.getTotalLimit()));
            response.setUsedLimit(convertFenToYuan(saved.getUsedLimit()));
            response.setFrozenLimit(convertFenToYuan(saved.getFrozenLimit()));
            response.setAvailableLimit(convertFenToYuan(saved.getAvailableLimit()));
            response.setReason(reason);
            response.setOperatorAddress(operatorAddress);
            response.setOperationTime(LocalDateTime.now());

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 额度解冻完成: limitId={}, 耗时={}ms", creditLimitId, duration);
            log.info("==================== 解冻额度结束 ====================");

            return response;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 解冻额度失败: 耗时={}ms, error={}", duration, e.getMessage(), e);
            throw e;
        }
    }

    // ==================== 额度调整申请和审批 ====================

    /**
     * 申请额度调整
     */
    @Transactional(rollbackFor = Exception.class)
    public CreditLimitAdjustResponse requestAdjust(@NonNull CreditLimitAdjustRequestDTO request,
                                                    @NonNull String requesterAddress, @NonNull String requesterName) {
        log.info("==================== 申请额度调整开始 ====================");
        log.info("申请信息: creditLimitId={}, adjustType={}, newLimit={}元",
                request.getCreditLimitId(), request.getAdjustType(), request.getNewLimit());

        long startTime = System.currentTimeMillis();

        try {
            // 1. 获取额度
            CreditLimit creditLimit = creditLimitRepository.findById(request.getCreditLimitId())
                    .orElseThrow(() -> new BusinessException("额度不存在: " + request.getCreditLimitId()));

            // 2. 计算调整金额
            Long currentLimitInFen = creditLimit.getTotalLimit();
            Long newLimitInFen = request.getNewLimit().multiply(new BigDecimal("100")).longValue();
            Long adjustAmountInFen = newLimitInFen - currentLimitInFen;

            // 3. 创建调整申请
            CreditLimitAdjustRequest adjustRequest = new CreditLimitAdjustRequest();
            adjustRequest.setCreditLimitId(request.getCreditLimitId());
            adjustRequest.setAdjustType(request.getAdjustType());
            adjustRequest.setCurrentLimit(currentLimitInFen);
            adjustRequest.setNewLimit(newLimitInFen);
            adjustRequest.setAdjustAmount(adjustAmountInFen);
            adjustRequest.setRequestReason(request.getRequestReason());
            adjustRequest.setRequesterAddress(requesterAddress);
            adjustRequest.setRequesterName(requesterName);
            adjustRequest.setRequestDate(LocalDateTime.now());
            adjustRequest.setRequestStatus(CreditAdjustRequestStatus.PENDING);

            CreditLimitAdjustRequest saved = adjustRequestRepository.save(adjustRequest);

            // 4. 构建响应
            CreditLimitAdjustResponse response = new CreditLimitAdjustResponse();
            response.setId(saved.getId());
            response.setCreditLimitId(saved.getCreditLimitId());
            response.setAdjustType(saved.getAdjustType().toString());
            response.setCurrentLimit(convertFenToYuan(saved.getCurrentLimit()));
            response.setNewLimit(convertFenToYuan(saved.getNewLimit()));
            response.setAdjustAmount(convertFenToYuan(saved.getAdjustAmount()));
            response.setRequestReason(saved.getRequestReason());
            response.setRequesterAddress(saved.getRequesterAddress());
            response.setRequesterName(saved.getRequesterName());
            response.setRequestDate(saved.getRequestDate());
            response.setRequestStatus(saved.getRequestStatus().toString());

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 额度调整申请完成: requestId={}, 耗时={}ms", saved.getId(), duration);
            log.info("==================== 申请额度调整结束 ====================");

            return response;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 申请额度调整失败: 耗时={}ms, error={}", duration, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 审批额度调整申请
     */
    @Transactional(rollbackFor = Exception.class)
    public CreditLimitAdjustResponse approveAdjust(@NonNull String requestId,
                                                    @NonNull CreditAdjustRequestStatus approvalResult,
                                                    String approveReason, String rejectReason,
                                                    String approverAddress, String approverName) {
        log.info("==================== 审批额度调整开始 ====================");
        log.info("审批信息: requestId={}, result={}, approver={}",
                requestId, approvalResult, approverAddress);

        long startTime = System.currentTimeMillis();

        try {
            // 1. 获取申请
            CreditLimitAdjustRequest request = adjustRequestRepository.findById(requestId)
                    .orElseThrow(() -> new BusinessException("调整申请不存在: " + requestId));

            // 2. 检查状态
            if (request.getRequestStatus() != CreditAdjustRequestStatus.PENDING) {
                throw new BusinessException("申请已被处理，状态: " + request.getRequestStatus());
            }

            // 3. 更新申请状态
            request.setRequestStatus(approvalResult);
            request.setApproverAddress(approverAddress);
            request.setApproverName(approverName);
            request.setApproveDate(LocalDateTime.now());
            request.setApproveReason(approveReason);
            request.setRejectReason(rejectReason);
            CreditLimitAdjustRequest updatedRequest = adjustRequestRepository.save(request);

            // 4. 如果审批通过，更新额度
            if (approvalResult == CreditAdjustRequestStatus.APPROVED) {
                CreditLimit creditLimit = creditLimitRepository.findById(request.getCreditLimitId())
                        .orElseThrow(() -> new BusinessException("额度不存在: " + request.getCreditLimitId()));

                creditLimit.setTotalLimit(request.getNewLimit());
                creditLimit.setApproverAddress(approverAddress);
                creditLimit.setApproveReason(approveReason);
                creditLimit.setApproveTime(LocalDateTime.now());
                CreditLimit savedCreditLimit = creditLimitRepository.save(creditLimit);

                // 上链记录额度调整
                try {
                    if (contractService != null) {
                        String txHash = contractService.recordCreditAdjustOnChain(updatedRequest);
                        if (txHash != null) {
                            savedCreditLimit.setTxHash(txHash);
                            creditLimitRepository.save(savedCreditLimit);
                            log.info("✓ 额度调整已上链: txHash={}", txHash);
                        }
                    }
                } catch (Exception e) {
                    log.warn("上链失败，但数据库操作成功: {}", e.getMessage());
                }

                log.info("✓ 额度已更新: limitId={}, newLimit={}分",
                        creditLimit.getId(), request.getNewLimit());
            }

            // 5. 构建响应
            CreditLimitAdjustResponse response = new CreditLimitAdjustResponse();
            response.setId(updatedRequest.getId());
            response.setCreditLimitId(updatedRequest.getCreditLimitId());
            response.setAdjustType(updatedRequest.getAdjustType().toString());
            response.setCurrentLimit(convertFenToYuan(updatedRequest.getCurrentLimit()));
            response.setNewLimit(convertFenToYuan(updatedRequest.getNewLimit()));
            response.setAdjustAmount(convertFenToYuan(updatedRequest.getAdjustAmount()));
            response.setRequestReason(updatedRequest.getRequestReason());
            response.setRequesterAddress(updatedRequest.getRequesterAddress());
            response.setRequesterName(updatedRequest.getRequesterName());
            response.setRequestDate(updatedRequest.getRequestDate());
            response.setRequestStatus(updatedRequest.getRequestStatus().toString());
            response.setApproverAddress(updatedRequest.getApproverAddress());
            response.setApproverName(updatedRequest.getApproverName());
            response.setApproveDate(updatedRequest.getApproveDate());
            response.setApproveReason(updatedRequest.getApproveReason());
            response.setRejectReason(updatedRequest.getRejectReason());

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 额度调整审批完成: requestId={}, result={}, 耗时={}ms",
                    requestId, approvalResult, duration);
            log.info("==================== 审批额度调整结束 ====================");

            return response;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 审批额度调整失败: 耗时={}ms, error={}", duration, e.getMessage(), e);
            throw e;
        }
    }

    // ==================== 查询使用记录和预警 ====================

    /**
     * 查询额度使用记录
     */
    public CreditLimitUsageQueryResponse queryUsageRecords(CreditLimitUsageQueryRequest request) {
        log.info("==================== 查询使用记录开始 ====================");

        long startTime = System.currentTimeMillis();

        try {
            // 构建查询条件
            Specification<CreditLimitUsage> spec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();

                if (request.getCreditLimitId() != null) {
                    predicates.add(cb.equal(root.get("creditLimitId"), request.getCreditLimitId()));
                }
                if (request.getEnterpriseAddress() != null) {
                    // 子查询：通过企业地址查找额度ID
                    Subquery<CreditLimit> subquery = query.subquery(CreditLimit.class);
                    javax.persistence.criteria.Root<CreditLimit> limitRoot = subquery.from(CreditLimit.class);
                    subquery.select(limitRoot.get("id"));
                    subquery.where(cb.equal(limitRoot.get("enterpriseAddress"), request.getEnterpriseAddress()));
                    predicates.add(cb.in(root.get("creditLimitId")).value(subquery));
                }
                if (request.getUsageType() != null) {
                    predicates.add(cb.equal(root.get("usageType"), request.getUsageType()));
                }
                if (request.getBusinessType() != null) {
                    predicates.add(cb.equal(root.get("businessType"), request.getBusinessType()));
                }
                if (request.getBusinessId() != null) {
                    predicates.add(cb.equal(root.get("businessId"), request.getBusinessId()));
                }
                if (request.getOperatorAddress() != null) {
                    predicates.add(cb.equal(root.get("operatorAddress"), request.getOperatorAddress()));
                }
                if (request.getUsageDateStart() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("usageDate"), request.getUsageDateStart()));
                }
                if (request.getUsageDateEnd() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("usageDate"), request.getUsageDateEnd()));
                }
                if (request.getAmountMin() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), request.getAmountMin()));
                }
                if (request.getAmountMax() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("amount"), request.getAmountMax()));
                }

                return cb.and(predicates.toArray(new Predicate[0]));
            };

            // 构建分页和排序
            Sort sort = buildSort(request.getSortBy(), request.getSortDirection());
            Pageable pageable = PageRequest.of(
                    request.getPage() != null ? request.getPage() : 0,
                    request.getSize() != null ? request.getSize() : 10,
                    sort);

            // 执行查询
            Page<CreditLimitUsage> page = usageRepository.findAll(spec, pageable);

            // 转换为DTO
            List<CreditLimitUsageDTO> content = page.getContent().stream()
                    .map(this::convertToUsageDTO)
                    .collect(Collectors.toList());

            // 构建响应
            CreditLimitUsageQueryResponse response = new CreditLimitUsageQueryResponse();
            response.setContent(content);
            response.setPageNumber(page.getNumber());
            response.setPageSize(page.getSize());
            response.setTotalPages(page.getTotalPages());
            response.setTotalElements(page.getTotalElements());
            response.setFirst(page.isFirst());
            response.setLast(page.isLast());

            // 构建统计信息
            response.setStatistics(buildUsageStatistics(content));

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 查询完成: 总记录数={}, 耗时={}ms", page.getTotalElements(), duration);
            log.info("==================== 查询使用记录结束 ====================");

            return response;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 查询使用记录失败: 耗时={}ms, error={}", duration, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 查询预警记录
     */
    public CreditLimitWarningQueryResponse queryWarnings(CreditLimitWarningQueryRequest request) {
        log.info("==================== 查询预警记录开始 ====================");

        long startTime = System.currentTimeMillis();

        try {
            // 构建查询条件
            Specification<CreditLimitWarning> spec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();

                if (request.getCreditLimitId() != null) {
                    predicates.add(cb.equal(root.get("creditLimitId"), request.getCreditLimitId()));
                }
                if (request.getEnterpriseAddress() != null) {
                    Subquery<CreditLimit> subquery = query.subquery(CreditLimit.class);
                    javax.persistence.criteria.Root<CreditLimit> limitRoot = subquery.from(CreditLimit.class);
                    subquery.select(limitRoot.get("id"));
                    subquery.where(cb.equal(limitRoot.get("enterpriseAddress"), request.getEnterpriseAddress()));
                    predicates.add(cb.in(root.get("creditLimitId")).value(subquery));
                }
                if (request.getWarningLevel() != null) {
                    predicates.add(cb.equal(root.get("warningLevel"), request.getWarningLevel()));
                }
                if (request.getWarningType() != null) {
                    predicates.add(cb.equal(root.get("warningType"), request.getWarningType()));
                }
                if (request.getIsResolved() != null) {
                    predicates.add(cb.equal(root.get("isResolved"), request.getIsResolved()));
                }
                if (request.getWarningDateStart() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("warningDate"), request.getWarningDateStart()));
                }
                if (request.getWarningDateEnd() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("warningDate"), request.getWarningDateEnd()));
                }

                return cb.and(predicates.toArray(new Predicate[0]));
            };

            // 构建分页和排序
            Sort sort = buildSort(request.getSortBy(), request.getSortDirection());
            Pageable pageable = PageRequest.of(
                    request.getPage() != null ? request.getPage() : 0,
                    request.getSize() != null ? request.getSize() : 10,
                    sort);

            // 执行查询
            Page<CreditLimitWarning> page = warningRepository.findAll(spec, pageable);

            // 转换为DTO
            List<CreditLimitWarningDTO> content = page.getContent().stream()
                    .map(this::convertToWarningDTO)
                    .collect(Collectors.toList());

            // 构建响应
            CreditLimitWarningQueryResponse response = new CreditLimitWarningQueryResponse();
            response.setContent(content);
            response.setPageNumber(page.getNumber());
            response.setPageSize(page.getSize());
            response.setTotalPages(page.getTotalPages());
            response.setTotalElements(page.getTotalElements());
            response.setFirst(page.isFirst());
            response.setLast(page.isLast());

            // 构建统计信息
            response.setStatistics(buildWarningStatistics(content));

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 查询完成: 总记录数={}, 耗时={}ms", page.getTotalElements(), duration);
            log.info("==================== 查询预警记录结束 ====================");

            return response;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 查询预警记录失败: 耗时={}ms, error={}", duration, e.getMessage(), e);
            throw e;
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 检查并创建预警
     */
    private void checkAndCreateWarning(CreditLimit creditLimit) {
        if (!creditLimit.needsWarning()) {
            return;
        }

        // 检查是否已存在未处理的相同预警
        List<CreditLimitWarning> existingWarnings = warningRepository
                .findByCreditLimitIdAndIsResolved(creditLimit.getId(), false);

        boolean hasSimilarWarning = existingWarnings.stream()
                .anyMatch(w -> w.getWarningType().equals("USAGE_HIGH"));

        if (hasSimilarWarning) {
            return; // 已存在未处理的预警，不重复创建
        }

        // 创建预警
        CreditLimitWarning warning = new CreditLimitWarning();
        warning.setCreditLimitId(creditLimit.getId());
        warning.setWarningLevel(determineWarningLevel(creditLimit));
        warning.setWarningType("USAGE_HIGH");
        warning.setCurrentUsageRate(creditLimit.getUsageRate());
        warning.setWarningThreshold(creditLimit.getWarningThreshold().doubleValue());
        warning.setWarningTitle(String.format("%s使用率超过%d%%",
                creditLimit.getLimitType().getDescription(),
                creditLimit.getWarningThreshold()));
        warning.setWarningContent(String.format(
                "企业的%s使用率已达到%.1f%%，超过预警阈值%d%%，请注意控制额度使用",
                creditLimit.getLimitType().getDescription(),
                creditLimit.getUsageRate(),
                creditLimit.getWarningThreshold()));
        warning.setWarningDate(LocalDateTime.now());
        warning.setIsResolved(false);

        warningRepository.save(warning);
        log.info("✓ 创建预警: warningLevel={}, usageRate={}%",
                warning.getWarningLevel(), creditLimit.getUsageRate());
    }

    /**
     * 确定预警级别
     */
    private CreditWarningLevel determineWarningLevel(CreditLimit creditLimit) {
        Double usageRate = creditLimit.getUsageRate();

        if (usageRate >= 95) {
            return CreditWarningLevel.CRITICAL;
        } else if (usageRate >= 90) {
            return CreditWarningLevel.HIGH;
        } else if (usageRate >= 80) {
            return CreditWarningLevel.MEDIUM;
        } else {
            return CreditWarningLevel.LOW;
        }
    }

    /**
     * 构建排序
     */
    @NonNull
    private Sort buildSort(String sortBy, String sortDirection) {
        Sort.Direction direction = "DESC".equalsIgnoreCase(sortDirection) ?
                Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(direction, sortBy != null ? sortBy : "createdAt");
    }

    /**
     * 构建统计信息
     */
    private CreditLimitQueryResponse.CreditLimitStatistics buildStatistics(List<CreditLimitDTO> limits) {
        CreditLimitQueryResponse.CreditLimitStatistics stats =
                new CreditLimitQueryResponse.CreditLimitStatistics();

        stats.setTotalCount(limits.size());

        BigDecimal totalLimit = limits.stream()
                .map(CreditLimitDTO::getTotalLimit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setTotalLimit(totalLimit);

        BigDecimal totalUsedLimit = limits.stream()
                .map(CreditLimitDTO::getUsedLimit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setTotalUsedLimit(totalUsedLimit);

        BigDecimal totalFrozenLimit = limits.stream()
                .map(CreditLimitDTO::getFrozenLimit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setTotalFrozenLimit(totalFrozenLimit);

        stats.setTotalAvailableLimit(totalLimit.subtract(totalUsedLimit).subtract(totalFrozenLimit));

        double avgUsageRate = limits.isEmpty() ? 0.0 :
                limits.stream()
                        .mapToDouble(dto -> dto.getUsageRate() != null ? dto.getUsageRate() : 0.0)
                        .average()
                        .orElse(0.0);
        stats.setAverageUsageRate(avgUsageRate);

        long needsWarningCount = limits.stream()
                .filter(dto -> dto.getNeedsWarning() != null && dto.getNeedsWarning())
                .count();
        stats.setNeedsWarningCount(needsWarningCount);

        // 按类型统计
        stats.setFinancingCount((long) limits.stream()
                .filter(dto -> dto.getLimitType() == CreditLimitType.FINANCING)
                .count());
        stats.setGuaranteeCount((long) limits.stream()
                .filter(dto -> dto.getLimitType() == CreditLimitType.GUARANTEE)
                .count());
        stats.setCreditCount((long) limits.stream()
                .filter(dto -> dto.getLimitType() == CreditLimitType.CREDIT)
                .count());

        // 按状态统计
        stats.setActiveCount((long) limits.stream()
                .filter(dto -> dto.getStatus() == CreditLimitStatus.ACTIVE)
                .count());
        stats.setFrozenCount((long) limits.stream()
                .filter(dto -> dto.getStatus() == CreditLimitStatus.FROZEN)
                .count());
        stats.setExpiredCount((long) limits.stream()
                .filter(dto -> dto.getStatus() == CreditLimitStatus.EXPIRED)
                .count());

        return stats;
    }

    /**
     * 构建使用记录统计信息
     */
    private CreditLimitUsageQueryResponse.CreditLimitUsageStatistics buildUsageStatistics(
            List<CreditLimitUsageDTO> records) {
        CreditLimitUsageQueryResponse.CreditLimitUsageStatistics stats =
                new CreditLimitUsageQueryResponse.CreditLimitUsageStatistics();

        stats.setTotalCount(records.size());

        BigDecimal totalUsageAmount = records.stream()
                .filter(dto -> dto.getUsageType() == CreditUsageType.USE)
                .map(CreditLimitUsageDTO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setTotalUsageAmount(totalUsageAmount);

        BigDecimal totalReleaseAmount = records.stream()
                .filter(dto -> dto.getUsageType() == CreditUsageType.RELEASE)
                .map(CreditLimitUsageDTO::getAmount)
                .map(amount -> amount.abs()) // 取绝对值
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setTotalReleaseAmount(totalReleaseAmount);

        stats.setNetUsageAmount(totalUsageAmount.subtract(totalReleaseAmount));

        BigDecimal totalFreezeAmount = records.stream()
                .filter(dto -> dto.getUsageType() == CreditUsageType.FREEZE)
                .map(CreditLimitUsageDTO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setTotalFreezeAmount(totalFreezeAmount);

        BigDecimal totalUnfreezeAmount = records.stream()
                .filter(dto -> dto.getUsageType() == CreditUsageType.UNFREEZE)
                .map(CreditLimitUsageDTO::getAmount)
                .map(amount -> amount.abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setTotalUnfreezeAmount(totalUnfreezeAmount);

        stats.setCurrentFrozenAmount(totalFreezeAmount.subtract(totalUnfreezeAmount));

        return stats;
    }

    /**
     * 构建预警统计信息
     */
    private CreditLimitWarningQueryResponse.CreditLimitWarningStatistics buildWarningStatistics(
            List<CreditLimitWarningDTO> warnings) {
        CreditLimitWarningQueryResponse.CreditLimitWarningStatistics stats =
                new CreditLimitWarningQueryResponse.CreditLimitWarningStatistics();

        stats.setTotalCount(warnings.size());

        long unresolvedCount = warnings.stream()
                .filter(dto -> dto.getIsResolved() != null && !dto.getIsResolved())
                .count();
        stats.setUnresolvedCount(unresolvedCount);
        stats.setResolvedCount(warnings.size() - unresolvedCount);

        // 按级别统计
        stats.setLowCount(warnings.stream()
                .filter(dto -> dto.getWarningLevel() == CreditWarningLevel.LOW)
                .count());
        stats.setMediumCount(warnings.stream()
                .filter(dto -> dto.getWarningLevel() == CreditWarningLevel.MEDIUM)
                .count());
        stats.setHighCount(warnings.stream()
                .filter(dto -> dto.getWarningLevel() == CreditWarningLevel.HIGH)
                .count());
        stats.setCriticalCount(warnings.stream()
                .filter(dto -> dto.getWarningLevel() == CreditWarningLevel.CRITICAL)
                .count());

        // 按类型统计
        stats.setUsageHighCount(warnings.stream()
                .filter(dto -> "USAGE_HIGH".equals(dto.getWarningType()))
                .count());
        stats.setExpirySoonCount(warnings.stream()
                .filter(dto -> "EXPIRY_SOON".equals(dto.getWarningType()))
                .count());
        stats.setRiskUpCount(warnings.stream()
                .filter(dto -> "RISK_UP".equals(dto.getWarningType()))
                .count());
        stats.setOverdueCount(warnings.stream()
                .filter(dto -> "OVERDUE".equals(dto.getWarningType()))
                .count());

        return stats;
    }

    /**
     * 转换为DTO
     */
    private CreditLimitDTO convertToDTO(@NonNull CreditLimit entity) {
        CreditLimitDTO dto = new CreditLimitDTO();
        dto.setId(entity.getId());
        dto.setEnterpriseAddress(entity.getEnterpriseAddress());
        dto.setEnterpriseName(entity.getEnterpriseName());
        dto.setLimitType(entity.getLimitType());
        dto.setTotalLimit(convertFenToYuan(entity.getTotalLimit()));
        dto.setUsedLimit(convertFenToYuan(entity.getUsedLimit()));
        dto.setFrozenLimit(convertFenToYuan(entity.getFrozenLimit()));
        dto.setAvailableLimit(convertFenToYuan(entity.getAvailableLimit()));
        dto.setUsageRate(entity.getUsageRate());
        dto.setWarningThreshold(entity.getWarningThreshold());
        dto.setNeedsWarning(entity.needsWarning());
        dto.setEffectiveDate(entity.getEffectiveDate());
        dto.setExpiryDate(entity.getExpiryDate());
        dto.setStatus(entity.getStatus());
        dto.setApproverAddress(entity.getApproverAddress());
        dto.setApproveReason(entity.getApproveReason());
        dto.setApproveTime(entity.getApproveTime());
        dto.setOverdueCount(entity.getOverdueCount());
        dto.setBadDebtCount(entity.getBadDebtCount());
        dto.setRiskLevel(entity.getRiskLevel());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setTxHash(entity.getTxHash());
        return dto;
    }

    /**
     * 转换使用记录为DTO
     */
    private CreditLimitUsageDTO convertToUsageDTO(CreditLimitUsage entity) {
        CreditLimitUsageDTO dto = new CreditLimitUsageDTO();
        dto.setId(entity.getId());
        dto.setCreditLimitId(entity.getCreditLimitId());
        dto.setUsageType(entity.getUsageType());
        dto.setBusinessType(entity.getBusinessType());
        dto.setBusinessId(entity.getBusinessId());
        dto.setAmount(entity.getAmountInYuan());
        dto.setBeforeAvailable(convertFenToYuan(entity.getBeforeAvailable()));
        dto.setAfterAvailable(convertFenToYuan(entity.getAfterAvailable()));
        dto.setBeforeUsed(convertFenToYuan(entity.getBeforeUsed()));
        dto.setAfterUsed(convertFenToYuan(entity.getAfterUsed()));
        dto.setBeforeFrozen(convertFenToYuan(entity.getBeforeFrozen()));
        dto.setAfterFrozen(convertFenToYuan(entity.getAfterFrozen()));
        dto.setOperatorAddress(entity.getOperatorAddress());
        dto.setOperatorName(entity.getOperatorName());
        dto.setUsageDate(entity.getUsageDate());
        dto.setRemark(entity.getRemark());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setTxHash(entity.getTxHash());
        return dto;
    }

    /**
     * 转换预警为DTO
     */
    @SuppressWarnings("null")
    private CreditLimitWarningDTO convertToWarningDTO(CreditLimitWarning entity) {
        CreditLimitWarningDTO dto = new CreditLimitWarningDTO();
        dto.setId(entity.getId());
        dto.setCreditLimitId(entity.getCreditLimitId());

        // 获取企业信息
        String creditLimitId = entity.getCreditLimitId();
        creditLimitRepository.findById(creditLimitId)
                .ifPresent(limit -> {
                    dto.setEnterpriseAddress(limit.getEnterpriseAddress());
                    dto.setEnterpriseName(limit.getEnterpriseName());
                });

        dto.setWarningLevel(entity.getWarningLevel());
        dto.setWarningType(entity.getWarningType());
        dto.setCurrentUsageRate(entity.getCurrentUsageRate());
        dto.setWarningThreshold(entity.getWarningThreshold());
        dto.setWarningTitle(entity.getWarningTitle());
        dto.setWarningContent(entity.getWarningContent());
        dto.setWarningDate(entity.getWarningDate());
        dto.setIsResolved(entity.getIsResolved());
        dto.setResolvedByAddress(entity.getResolvedByAddress());
        dto.setResolvedByName(entity.getResolvedByName());
        dto.setResolvedDate(entity.getResolvedDate());
        dto.setResolution(entity.getResolution());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setTxHash(entity.getTxHash());
        return dto;
    }

    /**
     * 分转换为元
     */
    private BigDecimal convertFenToYuan(Long fen) {
        if (fen == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(fen).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    // ==================== 额度可用余额查询 ====================

    /**
     * 查询额度可用余额
     * @param id 额度ID
     * @return 可用余额响应
     */
    public CreditLimitAvailableResponse getCreditLimitAvailable(@NonNull String id) {
        log.debug("查询额度可用余额: limitId={}", id);

        // 1. 查询额度实体
        CreditLimit creditLimit = Objects.requireNonNull(
            creditLimitRepository.findById(id)
                .orElseThrow(() -> new BusinessException("额度不存在: " + id)),
            "Credit limit should not be null after orElseThrow"
        );

        // 2. 转换为响应DTO
        return convertToAvailableResponse(creditLimit);
    }

    /**
     * 转换为可用余额响应
     */
    private CreditLimitAvailableResponse convertToAvailableResponse(@NonNull CreditLimit entity) {
        CreditLimitAvailableResponse response = new CreditLimitAvailableResponse();

        // 基本信息
        response.setId(entity.getId());
        response.setEnterpriseAddress(entity.getEnterpriseAddress());
        response.setEnterpriseName(entity.getEnterpriseName());
        response.setLimitType(entity.getLimitType().getCode());
        response.setLimitTypeName(entity.getLimitType().getDescription());
        response.setStatus(entity.getStatus().getCode());
        response.setStatusName(entity.getStatus().getDescription());

        // 金额信息（分转元）
        response.setTotalLimit(convertFenToYuan(entity.getTotalLimit()));
        response.setUsedLimit(convertFenToYuan(entity.getUsedLimit()));
        response.setFrozenLimit(convertFenToYuan(entity.getFrozenLimit()));
        response.setAvailableLimit(convertFenToYuan(entity.getAvailableLimit()));

        // 使用率和预警
        response.setUsageRate(entity.getUsageRate());
        response.setWarningThreshold(entity.getWarningThreshold());
        response.setNeedsWarning(entity.needsWarning());

        // 时间信息
        response.setEffectiveDate(entity.getEffectiveDate());
        response.setExpiryDate(entity.getExpiryDate());
        response.setDaysUntilExpiry(calculateDaysUntilExpiry(entity.getExpiryDate()));

        // 查询时间
        response.setQueriedAt(LocalDateTime.now());

        return response;
    }

    /**
     * 计算距离失效天数
     */
    private Integer calculateDaysUntilExpiry(LocalDateTime expiryDate) {
        if (expiryDate == null) {
            return null; // 永久有效
        }
        LocalDateTime now = LocalDateTime.now();
        long days = java.time.temporal.ChronoUnit.DAYS.between(now, expiryDate);
        return days >= 0 ? (int) days : 0; // 已过期返回0
    }
}
