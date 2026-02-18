package com.fisco.app.service.receivable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import javax.validation.Valid;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fisco.app.dto.receivable.CreateReceivableRequest;
import com.fisco.app.entity.receivable.Receivable;
import com.fisco.app.exception.BlockchainIntegrationException;
import com.fisco.app.repository.receivable.ReceivableRepository;
import com.fisco.app.service.blockchain.ContractService;
import com.fisco.app.service.enterprise.EnterpriseService;

import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 应收账款Service
 */
@Slf4j
@ConditionalOnProperty(name = "fisco.enabled", havingValue = "true", matchIfMissing = true)
@Service
@Api(tags = "应收账款服务")
@RequiredArgsConstructor
public class ReceivableService {

    private final ReceivableRepository receivableRepository;
    private final EnterpriseService enterpriseService;
    private final ContractService contractService;

    // ========== 敏感数据脱敏辅助方法 ==========

    /**
     * 脱敏区块链地址（只显示前6位和后4位）
     */
    private String maskAddress(String address) {
        if (address == null || address.length() < 10) return "***";
        return address.substring(0, 6) + "..." + address.substring(address.length() - 4);
    }

    /**
     * 脱敏金额（只显示整数部分和2位小数，隐藏大额）
     */
    private String maskAmount(Object amount) {
        if (amount == null) return "***";
        String str = amount.toString();
        // 对于BigDecimal或大额金额，只显示范围
        try {
            double value = Double.parseDouble(str);
            if (value > 1000000) {
                return ">1M";
            } else if (value > 10000) {
                return String.format("%.1fK", value / 1000);
            }
            return String.format("%.2f", value);
        } catch (NumberFormatException e) {
            return "***";
        }
    }

    /**
     * 创建应收账款
     */
    @Transactional(rollbackFor = Exception.class)
    public Receivable createReceivable(CreateReceivableRequest request, String supplierAddress) {
        log.info("==================== 应收账款创建开始 ====================");
        // 敏感信息脱敏后记录到INFO级别
        log.info("应收账款基本信息: receivableId={}, amount={}, currency={}",
                 request.getId(), maskAmount(request.getAmount()), request.getCurrency());
        log.info("参与方: supplier={}, coreEnterprise={}",
                 maskAddress(supplierAddress), maskAddress(request.getCoreEnterpriseAddress()));
        log.info("日期信息: issueDate={}, dueDate={}",
                 request.getIssueDate(), request.getDueDate());

        long startTime = System.currentTimeMillis();

        try {
            // 幂等性检查：如果应收账款已存在，返回现有记录
            log.debug("检查应收账款幂等性: receivableId={}", request.getId());
            if (receivableRepository.existsById(request.getId())) {
                log.info("应收账款已存在，返回现有记录（幂等）: receivableId={}", request.getId());
                return receivableRepository.findById(request.getId())
                        .orElseThrow(() -> new com.fisco.app.exception.BusinessException("应收账款不存在"));
            }
            log.debug("✓ 应收账款ID唯一性检查通过");

            // 验证供应商是否存在
            log.debug("验证供应商: supplierAddress={}", supplierAddress);
            if (!enterpriseService.isEnterpriseValid(supplierAddress)) {
                log.error("供应商不存在或未激活: supplierAddress={}", supplierAddress);
                throw new com.fisco.app.exception.BusinessException("供应商不存在或未激活");
            }
            log.debug("✓ 供应商验证通过");

            // 验证核心企业是否存在
            log.debug("验证核心企业: coreEnterpriseAddress={}", request.getCoreEnterpriseAddress());
            if (!enterpriseService.isEnterpriseValid(request.getCoreEnterpriseAddress())) {
                log.error("核心企业不存在或未激活: coreEnterpriseAddress={}", request.getCoreEnterpriseAddress());
                throw new com.fisco.app.exception.BusinessException("核心企业不存在或未激活");
            }
            log.debug("✓ 核心企业验证通过");

            // 验证日期
            log.debug("验证日期合理性");
            if (request.getDueDate().isBefore(request.getIssueDate())) {
                log.error("到期日期必须晚于出票日期: issueDate={}, dueDate={}",
                         request.getIssueDate(), request.getDueDate());
                throw new com.fisco.app.exception.BusinessException("到期日期必须晚于出票日期");
            }
            log.debug("✓ 日期验证通过");

            // 从请求构建 Receivable 实体
            log.debug("构建应收账款实体");
            Receivable receivable = new Receivable();
            receivable.setId(request.getId());
            receivable.setSupplierAddress(supplierAddress);
            receivable.setCoreEnterpriseAddress(request.getCoreEnterpriseAddress());
            receivable.setAmount(request.getAmount());
            receivable.setCurrency(request.getCurrency());
            receivable.setIssueDate(request.getIssueDate());
            receivable.setDueDate(request.getDueDate());
            receivable.setDescription(request.getDescription());
            receivable.setStatus(Receivable.ReceivableStatus.CREATED);
            receivable.setCurrentHolder(supplierAddress);

            // 步骤1: 保存到数据库
            log.debug("保存应收账款到数据库");
            Receivable saved = receivableRepository.save(receivable);
            log.info("✓ 数据库保存成功: receivableId={}", saved.getId());

            // 步骤2: 调用区块链合约
            log.debug("准备上链创建应收账款");
            try {
                String txHash = contractService.createReceivableOnChain(saved);
                log.info("✓ 应收账款已上链: receivableId={}, txHash={}", saved.getId(), txHash);

                // 步骤3: 更新 txHash
                log.debug("更新交易哈希到数据库");
                saved.setTxHash(txHash);
                Receivable finalSaved = receivableRepository.save(saved);
                log.info("✓ 交易哈希已保存");

                long duration = System.currentTimeMillis() - startTime;
                log.info("✓✓✓ 应收账款创建完成: receivableId={}, txHash={}, 耗时={}ms",
                         finalSaved.getId(), txHash, duration);
                log.info("==================== 应收账款创建结束 ====================");
                return finalSaved;

            } catch (BlockchainIntegrationException e) {
                long duration = System.currentTimeMillis() - startTime;
                log.error("✗✗✗ 区块链调用失败，回滚数据库事务: receivableId={}, 耗时={}ms, error={}",
                         saved.getId(), duration, e.getMessage(), e);
                log.info("==================== 应收账款创建失败（结束） ====================");
                throw new com.fisco.app.exception.BusinessException(
                    500, "区块链操作失败: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 应收账款创建失败: receivableId={}, 耗时={}ms, error={}",
                     request.getId(), duration, e.getMessage(), e);
            log.info("==================== 应收账款创建失败（结束） ====================");
            throw e;
        }
    }

    /**
     * 核心企业确认应收账款
     */
    @Transactional
    public void confirmReceivable(@NonNull String receivableId) {
        log.info("==================== 应收账款确认开始 ====================");
        log.info("应收账款ID: {}", receivableId);

        long startTime = System.currentTimeMillis();

        try {
            log.debug("查询应收账款信息: receivableId={}", receivableId);
            Receivable receivable = receivableRepository.findById(receivableId)
                    .orElseThrow(() -> new com.fisco.app.exception.BusinessException.ReceivableNotFoundException(receivableId));
            log.debug("✓ 应收账款查询成功: currentStatus={}", receivable.getStatus());

            // 验证状态
            log.debug("验证应收账款状态");
            if (receivable.getStatus() != Receivable.ReceivableStatus.CREATED) {
                log.error("状态错误，只能确认已创建的应收账款: currentStatus={}", receivable.getStatus());
                throw new com.fisco.app.exception.BusinessException.InvalidStatusException("只能确认已创建的应收账款");
            }
            log.debug("✓ 状态验证通过");

            // 步骤1: 调用区块链合约
            log.debug("准备上链确认应收账款");
            try {
                String txHash = contractService.confirmReceivableOnChain(receivableId);
                log.info("✓ 应收账款已上链确认: receivableId={}, txHash={}", receivableId, txHash);

                // 步骤2: 更新数据库状态和交易哈希
                log.debug("更新数据库状态: {} -> CONFIRMED", receivable.getStatus());
                receivable.setStatus(Receivable.ReceivableStatus.CONFIRMED);
                receivable.setTxHash(txHash);
                receivableRepository.save(receivable);
                log.info("✓ 数据库更新成功");

                long duration = System.currentTimeMillis() - startTime;
                log.info("✓✓✓ 应收账款确认完成: receivableId={}, txHash={}, 耗时={}ms", receivableId, txHash, duration);
                log.info("==================== 应收账款确认结束 ====================");

            } catch (BlockchainIntegrationException e) {
                long duration = System.currentTimeMillis() - startTime;
                log.error("✗✗✗ 区块链调用失败，回滚数据库事务: receivableId={}, 耗时={}ms, error={}",
                         receivableId, duration, e.getMessage(), e);
                log.info("==================== 应收账款确认失败（结束） ====================");
                throw new com.fisco.app.exception.BusinessException(
                    500, "区块链操作失败: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 应收账款确认失败: receivableId={}, 耗时={}ms, error={}", receivableId, duration, e.getMessage(), e);
            log.info("==================== 应收账款确认失败（结束） ====================");
            throw e;
        }
    }

    /**
     * 应收账款融资
     */
    @Transactional
    public void financeReceivable(@NonNull String receivableId, String financierAddress,
                                   BigDecimal financeAmount, Integer financeRate) {
        log.info("应收账款融资: id={}, financier={}, amount={}, rate={}",
                 receivableId, financierAddress, financeAmount, financeRate);

        Receivable receivable = receivableRepository.findById(receivableId)
                .orElseThrow(() -> new com.fisco.app.exception.BusinessException.ReceivableNotFoundException(receivableId));

        if (receivable.getStatus() != Receivable.ReceivableStatus.CONFIRMED) {
            throw new com.fisco.app.exception.BusinessException.InvalidStatusException("只能对已确认的应收账款进行融资");
        }

        if (financeAmount.compareTo(receivable.getAmount()) > 0) {
            throw new com.fisco.app.exception.BusinessException("融资金额不能超过应收账款金额");
        }

        // 验证金融机构是否存在
        if (!enterpriseService.isEnterpriseValid(financierAddress)) {
            throw new com.fisco.app.exception.BusinessException("金融机构不存在或未激活");
        }

        // 步骤1: 调用区块链合约
        try {
            String txHash = contractService.financeReceivableOnChain(
                receivableId,
                financierAddress,
                financeAmount,
                financeRate
            );

            // 步骤2: 更新数据库状态和交易哈希
            receivable.setStatus(Receivable.ReceivableStatus.FINANCED);
            receivable.setFinancierAddress(financierAddress);
            receivable.setFinanceAmount(financeAmount);
            receivable.setFinanceRate(financeRate);
            receivable.setFinanceDate(LocalDateTime.now());
            receivable.setCurrentHolder(financierAddress);
            receivable.setTxHash(txHash);
            receivableRepository.save(receivable);

            log.info("应收账款融资成功: id={}, txHash={}", receivableId, txHash);

        } catch (BlockchainIntegrationException e) {
            log.error("区块链调用失败，回滚数据库事务: receivableId={}, error={}",
                receivableId, e.getMessage(), e);
            // 抛出业务异常，触发 @Transactional 回滚
            throw new com.fisco.app.exception.BusinessException(
                500, "区块链操作失败: " + e.getMessage(), e);
        }
    }

    /**
     * 应收账款还款
     */
    @Transactional
    public void repayReceivable(@NonNull String receivableId, BigDecimal amount) {
        log.info("应收账款还款: id={}, amount={}", receivableId, amount);

        Receivable receivable = receivableRepository.findById(receivableId)
                .orElseThrow(() -> new com.fisco.app.exception.BusinessException.ReceivableNotFoundException(receivableId));

        if (receivable.getStatus() != Receivable.ReceivableStatus.FINANCED) {
            throw new com.fisco.app.exception.BusinessException.InvalidStatusException("只能对已融资的应收账款进行还款");
        }

        // 步骤1: 调用区块链合约
        try {
            String txHash = contractService.repayReceivableOnChain(receivableId, amount);

            // 步骤2: 更新数据库状态和交易哈希
            receivable.setStatus(Receivable.ReceivableStatus.REPAID);
            receivable.setTxHash(txHash);
            receivableRepository.save(receivable);

            log.info("应收账款还款成功: id={}, txHash={}", receivableId, txHash);

        } catch (BlockchainIntegrationException e) {
            log.error("区块链调用失败，回滚数据库事务: receivableId={}, error={}",
                receivableId, e.getMessage(), e);
            // 抛出业务异常，触发 @Transactional 回滚
            throw new com.fisco.app.exception.BusinessException(
                500, "区块链操作失败: " + e.getMessage(), e);
        }
    }

    /**
     * 转让应收账款
     */
    @Transactional
    public void transferReceivable(@NonNull String receivableId, String newHolder) {
        log.info("转让应收账款: id={}, newHolder={}", receivableId, newHolder);

        Receivable receivable = receivableRepository.findById(receivableId)
                .orElseThrow(() -> new com.fisco.app.exception.BusinessException.ReceivableNotFoundException(receivableId));

        // 验证新持有人是否存在
        if (!enterpriseService.isEnterpriseValid(newHolder)) {
            throw new com.fisco.app.exception.BusinessException("新持有人不存在或未激活");
        }

        String oldHolder = receivable.getCurrentHolder();

        // 步骤1: 调用区块链合约
        try {
            String txHash = contractService.transferReceivableOnChain(receivableId, newHolder);

            // 步骤2: 更新数据库状态和交易哈希
            receivable.setCurrentHolder(newHolder);
            receivable.setTxHash(txHash);
            receivableRepository.save(receivable);

            log.info("应收账款转让成功: id={}, from={}, to={}, txHash={}",
                     receivableId, oldHolder, newHolder, txHash);

        } catch (BlockchainIntegrationException e) {
            log.error("区块链调用失败，回滚数据库事务: receivableId={}, error={}",
                receivableId, e.getMessage(), e);
            // 抛出业务异常，触发 @Transactional 回滚
            throw new com.fisco.app.exception.BusinessException(
                500, "区块链操作失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取应收账款信息
     */
    public Receivable getReceivable(@NonNull String receivableId) {
        return receivableRepository.findById(receivableId)
                .orElseThrow(() -> new com.fisco.app.exception.BusinessException.ReceivableNotFoundException(receivableId));
    }

    /**
     * 获取供应商的所有应收账款
     */
    public List<Receivable> getSupplierReceivables(String address) {
        return receivableRepository.findBySupplierAddress(address);
    }

    /**
     * 获取核心企业的所有应付账款
     */
    public List<Receivable> getCoreEnterpriseReceivables(String address) {
        return receivableRepository.findByCoreEnterpriseAddress(address);
    }

    /**
     * 获取资金方的所有融资账款
     */
    public List<Receivable> getFinancierReceivables(String address) {
        return receivableRepository.findByFinancierAddress(address);
    }

    /**
     * 获取持票人的所有应收账款
     */
    public List<Receivable> getHolderReceivables(String address) {
        return receivableRepository.findByCurrentHolder(address);
    }

    /**
     * 根据状态查询应收账款
     */
    public List<Receivable> getReceivablesByStatus(Receivable.ReceivableStatus status) {
        return receivableRepository.findByStatus(status);
    }

    /**
     * 获取即将到期的应收账款
     */
    public List<Receivable> getDueSoonReceivables(LocalDateTime startDate, LocalDateTime endDate) {
        return receivableRepository.findDueSoonReceivables(startDate, endDate);
    }

    /**
     * 统计供应商的总应收金额
     */
    public BigDecimal getTotalAmountBySupplier(String supplierAddress) {
        return receivableRepository.totalAmountBySupplier(supplierAddress);
    }

    /**
     * 统计资金方的总融资金额
     */
    public BigDecimal getTotalFinanceAmountByFinancier(String financierAddress) {
        return receivableRepository.totalFinanceAmountByFinancier(financierAddress);
    }

    /**
     * 拆分应收账款
     */
    @Transactional
    public com.fisco.app.dto.receivable.ReceivableSplitResponse splitReceivable(
            @NonNull @Valid com.fisco.app.dto.receivable.ReceivableSplitRequest request, @NonNull String applicantId) {
        log.info("==================== 应收账款拆分开始 ====================");
        log.info("应收账款ID: {}, 拆分数量: {}, 拆分方案: {}",
                request.getReceivableId(), request.getSplitCount(), request.getSplitScheme());

        long startTime = System.currentTimeMillis();

        try {
            // 1. 验证原应收账款是否存在
            String receivableId = request.getReceivableId();
            if (receivableId == null) {
                throw new IllegalArgumentException("应收账款ID不能为空");
            }
            Receivable original = receivableRepository.findById(receivableId)
                    .orElseThrow(() -> new com.fisco.app.exception.BusinessException("应收账款不存在"));

            // 2. 验证状态
            if (original.getStatus() != Receivable.ReceivableStatus.CONFIRMED) {
                throw new com.fisco.app.exception.BusinessException("只能拆分已确认的应收账款");
            }

            // 3. 验证申请人是否为供应商
            if (!original.getSupplierAddress().equals(applicantId)) {
                throw new com.fisco.app.exception.BusinessException("只有供应商可以拆分应收账款");
            }

            // 4. 计算拆分金额
            java.math.BigDecimal totalAmount = original.getAmount();
            java.math.BigDecimal amountPerSplit = totalAmount.divide(
                    java.math.BigDecimal.valueOf(request.getSplitCount()), 2, java.math.RoundingMode.HALF_UP);

            log.info("拆分计算: 总金额={}, 拆分数={}, 每份金额={}", totalAmount, request.getSplitCount(), amountPerSplit);

            // 5. 创建拆分明细
            java.util.List<java.util.Map<String, Object>> splitDetailsList = new java.util.ArrayList<>();
            if (request.getSplitScheme() == com.fisco.app.dto.receivable.ReceivableSplitRequest.SplitScheme.EQUAL) {
                // 等额拆分
                for (int i = 0; i < request.getSplitCount(); i++) {
                    java.util.Map<String, Object> detail = new java.util.HashMap<>();
                    detail.put("amount", amountPerSplit.multiply(java.math.BigDecimal.valueOf(100)).longValue());
                    detail.put("ratio", java.math.BigDecimal.ONE.divide(java.math.BigDecimal.valueOf(request.getSplitCount()), 4, java.math.RoundingMode.HALF_UP));
                    detail.put("remark", "拆分-" + (i + 1));
                    splitDetailsList.add(detail);
                }
            } else {
                // 自定义拆分
                if (request.getSplitDetails() == null || request.getSplitDetails().size() != request.getSplitCount()) {
                    throw new com.fisco.app.exception.BusinessException("自定义拆分必须提供完整的拆分明细");
                }
                for (com.fisco.app.dto.receivable.ReceivableSplitRequest.SplitDetail detail : request.getSplitDetails()) {
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    map.put("amount", detail.getAmount());
                    map.put("ratio", detail.getRatio());
                    map.put("remark", detail.getRemark());
                    splitDetailsList.add(map);
                }
            }

            // 6. 更新原应收账款状态为拆分中
            original.setStatus(Receivable.ReceivableStatus.SPLITTING);
            receivableRepository.save(original);

            // 7. 创建子应收账款
            java.util.List<Receivable> splitReceivables = new java.util.ArrayList<>();
            for (int i = 0; i < request.getSplitCount(); i++) {
                Receivable split = new Receivable();
                split.setId(request.getReceivableId() + "-SPLIT-" + String.format("%03d", i + 1));
                split.setSupplierAddress(original.getSupplierAddress());
                split.setCoreEnterpriseAddress(original.getCoreEnterpriseAddress());
                split.setParentReceivableId(request.getReceivableId());

                java.util.Map<String, Object> detail = splitDetailsList.get(i);
                split.setAmount(java.math.BigDecimal.valueOf((Long) detail.get("amount")).divide(java.math.BigDecimal.valueOf(100)));
                split.setCurrency(original.getCurrency());
                split.setIssueDate(original.getIssueDate());
                split.setDueDate(original.getDueDate());
                split.setDescription(original.getDescription() + " - 拆分" + (i + 1));
                split.setStatus(Receivable.ReceivableStatus.SPLITTING);
                split.setCurrentHolder(original.getCurrentHolder());

                splitReceivables.add(split);
            }

            receivableRepository.saveAll(splitReceivables);

            // 8. 创建响应
            com.fisco.app.dto.receivable.ReceivableSplitResponse response = new com.fisco.app.dto.receivable.ReceivableSplitResponse();
            response.setOriginalReceivableId(request.getReceivableId());
            response.setApplicationId(java.util.UUID.randomUUID().toString());
            response.setStatus("PENDING");
            response.setSplitCount(request.getSplitCount());
            response.setSplitReceivables(splitReceivables);
            response.setApplicantId(applicantId);
            response.setApplicationTime(LocalDateTime.now());

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 应收账款拆分完成: receivableId={}, splitCount={}, 耗时={}ms",
                    request.getReceivableId(), request.getSplitCount(), duration);

            return response;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 应收账款拆分失败: receivableId={}, 耗时={}ms, error={}",
                    request.getReceivableId(), duration, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 合并应收账款
     */
    @SuppressWarnings("null")
    @Transactional
    public com.fisco.app.dto.receivable.ReceivableMergeResponse mergeReceivables(
            com.fisco.app.dto.receivable.ReceivableMergeRequest request, String applicantId) {
        log.info("==================== 应收账款合并开始 ====================");
        log.info("应收账款数量: {}, 合并类型: {}", request.getReceivableIds().size(), request.getMergeType());

        long startTime = System.currentTimeMillis();

        try {
            // 1. 批量查询验证所有应收账款是否存在（修复N+1查询问题）
            java.util.List<Receivable> receivables = new java.util.ArrayList<>();

            // 使用批量查询代替循环查询，避免N+1问题
            java.util.List<String> ids = request.getReceivableIds().stream()
                    .filter(id -> id != null)
                    .collect(java.util.stream.Collectors.toList());

            java.util.List<Receivable> foundReceivables = receivableRepository.findAllById(ids);

            // 验证是否所有ID都找到了
            if (foundReceivables.size() != ids.size()) {
                java.util.Set<String> foundIds = foundReceivables.stream()
                        .map(Receivable::getId)
                        .collect(java.util.stream.Collectors.toSet());
                String missingIds = ids.stream()
                        .filter(id -> !foundIds.contains(id))
                        .collect(java.util.stream.Collectors.joining(", "));
                throw new com.fisco.app.exception.BusinessException("应收账款不存在: " + missingIds);
            }

            receivables.addAll(foundReceivables);

            // 2. 验证所有应收账款的状态
            for (Receivable r : receivables) {
                if (r.getStatus() != Receivable.ReceivableStatus.CONFIRMED) {
                    throw new com.fisco.app.exception.BusinessException("只能合并已确认的应收账款");
                }
                if (!r.getSupplierAddress().equals(applicantId)) {
                    throw new com.fisco.app.exception.BusinessException("只能合并自己的应收账款");
                }
            }

            // 3. 验证供应商和核心企业是否一致
            String supplier = receivables.get(0).getSupplierAddress();
            String coreEnterprise = receivables.get(0).getCoreEnterpriseAddress();
            for (Receivable r : receivables) {
                if (!r.getSupplierAddress().equals(supplier) || !r.getCoreEnterpriseAddress().equals(coreEnterprise)) {
                    throw new com.fisco.app.exception.BusinessException("只能合并同一供应商和核心企业的应收账款");
                }
            }

            // 4. 计算合并后的金额和期限
            java.math.BigDecimal totalAmount = java.math.BigDecimal.ZERO;
            LocalDateTime maxDueDate = null;

            for (Receivable r : receivables) {
                totalAmount = totalAmount.add(r.getAmount());
                if (maxDueDate == null || r.getDueDate().isAfter(maxDueDate)) {
                    maxDueDate = r.getDueDate();
                }
            }

            LocalDateTime avgDueDate = maxDueDate; // 默认使用最长期限

            // 5. 更新原应收账款状态为合并中
            for (Receivable r : receivables) {
                r.setStatus(Receivable.ReceivableStatus.MERGING);
            }
            receivableRepository.saveAll(receivables);

            // 6. 创建合并后的应收账款
            Receivable merged = new Receivable();
            merged.setId("MERGED-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            merged.setSupplierAddress(supplier);
            merged.setCoreEnterpriseAddress(coreEnterprise);
            merged.setAmount(totalAmount);
            merged.setCurrency(receivables.get(0).getCurrency());
            merged.setIssueDate(receivables.get(0).getIssueDate());
            merged.setDueDate(avgDueDate);
            merged.setDescription("合并" + receivables.size() + "笔应收账款");
            merged.setStatus(Receivable.ReceivableStatus.MERGING);
            merged.setCurrentHolder(supplier);
            merged.setMergeCount(receivables.size());

            receivableRepository.save(merged);

            // 7. 创建响应
            com.fisco.app.dto.receivable.ReceivableMergeResponse response = new com.fisco.app.dto.receivable.ReceivableMergeResponse();
            response.setApplicationId(java.util.UUID.randomUUID().toString());
            response.setStatus("PENDING");
            response.setSourceReceivables(receivables);
            response.setMergedReceivable(merged);
            response.setMergeCount(receivables.size());
            response.setTotalAmount(totalAmount.multiply(java.math.BigDecimal.valueOf(100)).longValue());
            response.setApplicantId(applicantId);
            response.setApplicationTime(LocalDateTime.now());

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 应收账款合并完成: mergeCount={}, totalAmount={}, 耗时={}ms",
                    receivables.size(), totalAmount, duration);

            return response;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 应收账款合并失败: 耗时={}ms, error={}", duration, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 审批应收账款拆分
     */
    @Transactional
    @SuppressWarnings("null")
    public void approveSplit(@NonNull String receivableId, boolean approved, @NonNull String approverId, @NonNull String reason) {
        log.info("审批应收账款拆分: receivableId={}, approved={}", receivableId, approved);

        Receivable original = receivableRepository.findById(receivableId)
                .orElseThrow(() -> new com.fisco.app.exception.BusinessException("应收账款不存在"));

        if (original.getStatus() != Receivable.ReceivableStatus.SPLITTING) {
            throw new com.fisco.app.exception.BusinessException("应收账款状态不是拆分中");
        }

        java.util.List<Receivable> children = receivableRepository.findByParentReceivableId(receivableId);

        if (approved) {
            // 审批通过
            original.setStatus(Receivable.ReceivableStatus.SPLIT);
            original.setSplitCount(children.size());
            original.setSplitTime(LocalDateTime.now());

            for (Receivable child : children) {
                child.setStatus(Receivable.ReceivableStatus.CONFIRMED);
            }
            receivableRepository.saveAll(children);
        } else {
            // 审批拒绝
            original.setStatus(Receivable.ReceivableStatus.CONFIRMED);

            for (Receivable child : children) {
                child.setStatus(Receivable.ReceivableStatus.CANCELLED);
            }
            Iterable<Receivable> childrenToSave = children;
            receivableRepository.saveAll(childrenToSave);
        }

        receivableRepository.save(original);
        log.info("应收账款拆分审批完成: receivableId={}, approved={}", receivableId, approved);
    }

    /**
     * 审批应收账款合并
     */
    @Transactional
    public void approveMerge(@NonNull String mergedReceivableId, boolean approved, @NonNull String approverId, @NonNull String reason) {
        log.info("审批应收账款合并: mergedReceivableId={}, approved={}", mergedReceivableId, approved);

        Receivable merged = receivableRepository.findById(mergedReceivableId)
                .orElseThrow(() -> new com.fisco.app.exception.BusinessException("合并后的应收账款不存在"));

        if (merged.getStatus() != Receivable.ReceivableStatus.MERGING) {
            throw new com.fisco.app.exception.BusinessException("应收账款状态不是合并中");
        }

        // 查找所有源应收账款
        java.util.List<Receivable> sources = receivableRepository.findAll().stream()
                .filter(r -> mergedReceivableId.equals(r.getId()) || r.getStatus() == Receivable.ReceivableStatus.MERGING)
                .collect(java.util.stream.Collectors.toList());

        if (approved) {
            // 审批通过
            merged.setStatus(Receivable.ReceivableStatus.CONFIRMED);
            merged.setMergeTime(LocalDateTime.now());

            // 标记源应收账款为已合并
            for (Receivable source : sources) {
                if (source.getStatus() == Receivable.ReceivableStatus.MERGING && !source.getId().equals(mergedReceivableId)) {
                    source.setStatus(Receivable.ReceivableStatus.MERGED);
                    source.setParentReceivableId(mergedReceivableId);
                    receivableRepository.save(source);
                }
            }
        } else {
            // 审批拒绝
            merged.setStatus(Receivable.ReceivableStatus.CANCELLED);

            // 恢复源应收账款状态
            for (Receivable source : sources) {
                if (source.getStatus() == Receivable.ReceivableStatus.MERGING && !source.getId().equals(mergedReceivableId)) {
                    source.setStatus(Receivable.ReceivableStatus.CONFIRMED);
                    receivableRepository.save(source);
                }
            }
        }

        receivableRepository.save(merged);
        log.info("应收账款合并审批完成: mergedReceivableId={}, approved={}", mergedReceivableId, approved);
    }
}
