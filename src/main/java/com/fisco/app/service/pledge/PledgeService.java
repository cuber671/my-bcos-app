package com.fisco.app.service.pledge;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fisco.app.dto.pledge.PledgeConfirmRequest;
import com.fisco.app.dto.pledge.PledgeConfirmResponse;
import com.fisco.app.dto.pledge.PledgeInitiateRequest;
import com.fisco.app.dto.pledge.PledgeInitiateResponse;
import com.fisco.app.dto.pledge.PledgeRecordResponse;
import com.fisco.app.dto.pledge.PledgeReleaseRequest;
import com.fisco.app.entity.enterprise.Enterprise;
import com.fisco.app.entity.pledge.PledgeRecord;
import com.fisco.app.entity.risk.FinancingRecord;
import com.fisco.app.entity.warehouse.ElectronicWarehouseReceipt;
import com.fisco.app.entity.warehouse.EwrEndorsementChain;
import com.fisco.app.exception.BusinessException;
import com.fisco.app.repository.pledge.PledgeRecordRepository;
import com.fisco.app.repository.risk.FinancingRecordRepository;
import com.fisco.app.repository.warehouse.ElectronicWarehouseReceiptRepository;
import com.fisco.app.repository.warehouse.EwrEndorsementChainRepository;
import com.fisco.app.service.blockchain.ContractService;
import com.fisco.app.service.enterprise.EnterpriseService;

import lombok.extern.slf4j.Slf4j;

/**
 * 仓单质押融资Service（简化版）
 * 直接使用背书系统管理质押流程，移除质押申请层
 */
@Slf4j
@Service
@SuppressWarnings("null")
public class PledgeService {

    @Autowired
    private PledgeRecordRepository pledgeRecordRepository;

    @Autowired
    private FinancingRecordRepository financingRecordRepository;

    @Autowired
    private ElectronicWarehouseReceiptRepository receiptRepository;

    @Autowired
    private EwrEndorsementChainRepository endorsementRepository;

    @Autowired
    private ContractService contractService;

    @Autowired
    private EnterpriseService enterpriseService;

    /**
     * 发起质押（货主）
     * 创建PLEDGE类型背书，仓单状态变为FROZEN
     */
    @Transactional(rollbackFor = Exception.class)
    public PledgeInitiateResponse initiatePledge(
            @NonNull PledgeInitiateRequest request,
            String ownerId,
            String ownerName) {

        log.info("发起质押, 仓单ID: {}, 金融机构ID: {}, 质押金额: {}",
                request.getReceiptId(), request.getFinancialInstitutionId(), request.getPledgeAmount());

        // 1. 验证仓单状态（必须是 NORMAL）
        ElectronicWarehouseReceipt receipt = receiptRepository.findById(request.getReceiptId())
                .orElseThrow(() -> new BusinessException("仓单不存在"));

        if (receipt.getReceiptStatus() != ElectronicWarehouseReceipt.ReceiptStatus.NORMAL) {
            throw new BusinessException("只有正常状态的仓单可以质押，当前状态：" + receipt.getReceiptStatus());
        }

        // 2. 验证货主权限
        if (!receipt.getOwnerId().equals(ownerId)) {
            throw new BusinessException("您不是该仓单的货主");
        }

        // 3. 检查是否已有待确认背书
        if (endorsementRepository.existsPendingEndorsement(request.getReceiptId())) {
            throw new BusinessException("该仓单存在待确认的背书");
        }

        // 4. 验证质押金额（不超过仓单价值 × 质押率）
        BigDecimal maxAmount = receipt.getTotalValue().multiply(request.getPledgeRatio())
                .setScale(2, RoundingMode.HALF_UP);
        if (request.getPledgeAmount().compareTo(maxAmount) > 0) {
            throw new BusinessException(String.format(
                    "质押金额超限，最大可质押金额：%s元（仓单价值×%s%%）",
                    maxAmount, request.getPledgeRatio().multiply(new BigDecimal("100")).intValue()));
        }

        // 5. 查询金融机构
        Enterprise fi = enterpriseService.getEnterpriseById(request.getFinancialInstitutionId());
        if (fi == null) {
            throw new BusinessException("金融机构不存在");
        }

        if (fi.getRole() != Enterprise.EnterpriseRole.FINANCIAL_INSTITUTION) {
            throw new BusinessException("指定的企业不是金融机构");
        }

        // 6. 验证是否已存在待处理或已生效的质押
        List<PledgeRecord> existingPledges = pledgeRecordRepository
                .findByReceiptIdAndStatusIn(request.getReceiptId(),
                        java.util.Arrays.asList(
                                PledgeRecord.PledgeStatus.ACTIVE));

        if (!existingPledges.isEmpty()) {
            throw new BusinessException("该仓单已存在质押记录");
        }

        log.info("质押业务规则验证通过: 仓单状态={}, 货主权限={}, 质押金额={}",
                receipt.getReceiptStatus(), ownerId, request.getPledgeAmount());

        // 6. 创建 PLEDGE 类型背书（PENDING 状态）
        String endorsementNo = generateEndorsementNo();

        EwrEndorsementChain endorsement = new EwrEndorsementChain();
        endorsement.setId(UUID.randomUUID().toString());
        endorsement.setReceiptId(receipt.getId());
        endorsement.setReceiptNo(receipt.getReceiptNo());
        endorsement.setEndorsementNo(endorsementNo);
        endorsement.setEndorseFrom(receipt.getHolderAddress());
        endorsement.setEndorseFromName(receipt.getCurrentHolder());
        endorsement.setEndorseTo(fi.getAddress());
        endorsement.setEndorseToName(fi.getName());
        endorsement.setEndorsementType(EwrEndorsementChain.EndorsementType.PLEDGE);
        endorsement.setEndorsementStatus(EwrEndorsementChain.EndorsementStatus.PENDING);
        endorsement.setEndorsementTime(LocalDateTime.now());
        endorsement.setRemarks(String.format("期望质押：%s元，期限：%s至%s",
                        request.getPledgeAmount(),
                        request.getPledgeStartDate(),
                        request.getPledgeEndDate()));

        endorsementRepository.save(endorsement);

        // 7. 修改仓单状态为 FROZEN
        receipt.setReceiptStatus(ElectronicWarehouseReceipt.ReceiptStatus.FROZEN);
        receiptRepository.save(receipt);

        log.info("质押发起成功，背书ID: {}，仓单状态已更新为FROZEN", endorsement.getId());

        return PledgeInitiateResponse.builder()
                .endorsementId(endorsement.getId())
                .endorsementNo(endorsementNo)
                .receiptId(receipt.getId())
                .receiptNo(receipt.getReceiptNo())
                .receiptStatus("FROZEN")
                .pledgeAmount(request.getPledgeAmount())
                .financialInstitutionId(fi.getId())
                .financialInstitutionName(fi.getName())
                .endorsementStatus("PENDING")
                .initiateTime(LocalDateTime.now())
                .build();
    }

    /**
     * 确认质押（金融机构）
     * 批准：FROZEN → PLEDGED，创建融资记录
     * 拒绝：FROZEN → NORMAL
     */
    @Transactional(rollbackFor = Exception.class)
    public PledgeConfirmResponse confirmPledge(
            @NonNull PledgeConfirmRequest request,
            String confirmerId,
            String confirmerName) {

        log.info("确认质押, 背书ID: {}, 确认结果: {}", request.getEndorsementId(), request.getConfirmResult());

        // 1. 查询背书
        EwrEndorsementChain endorsement = endorsementRepository.findById(request.getEndorsementId())
                .orElseThrow(() -> new BusinessException("背书不存在"));

        if (endorsement.getEndorsementType() != EwrEndorsementChain.EndorsementType.PLEDGE) {
            throw new BusinessException("该背书不是质押类型");
        }

        if (endorsement.getEndorsementStatus() != EwrEndorsementChain.EndorsementStatus.PENDING) {
            throw new BusinessException("只有待确认状态的背书可以操作，当前状态：" + endorsement.getEndorsementStatus());
        }

        // 2. 查询仓单
        ElectronicWarehouseReceipt receipt = receiptRepository.findById(endorsement.getReceiptId())
                .orElseThrow(() -> new BusinessException("仓单不存在"));

        if (receipt.getReceiptStatus() != ElectronicWarehouseReceipt.ReceiptStatus.FROZEN) {
            throw new BusinessException("仓单状态不是FROZEN，当前状态：" + receipt.getReceiptStatus());
        }

        // 3. 根据确认结果处理
        if ("CONFIRMED".equals(request.getConfirmResult())) {
            return handlePledgeConfirmation(endorsement, receipt, request, confirmerId, confirmerName);
        } else {
            return handlePledgeCancellation(endorsement, receipt, request, confirmerId, confirmerName);
        }
    }

    /**
     * 处理质押确认（批准）
     */
    private PledgeConfirmResponse handlePledgeConfirmation(
            EwrEndorsementChain endorsement,
            ElectronicWarehouseReceipt receipt,
            PledgeConfirmRequest request,
            String confirmerId,
            String confirmerName) {

        try {
            log.info("开始处理质押批准, 背书ID: {}, 仓单ID: {}", endorsement.getId(), receipt.getId());

            // 验证批准参数
            if (request.getApprovedAmount() == null) {
                throw new BusinessException("批准金额不能为空");
            }
            if (request.getInterestRate() == null) {
                throw new BusinessException("利率不能为空");
            }

            // 1. 更新背书状态为 CONFIRMED
            endorsement.setEndorsementStatus(EwrEndorsementChain.EndorsementStatus.CONFIRMED);
            endorsement.setConfirmedTime(LocalDateTime.now());
            endorsementRepository.save(endorsement);

            // 2. 计算融资信息
            BigDecimal financingAmount = request.getApprovedAmount();
            BigDecimal interestRate = request.getInterestRate();

            // 从备注中解析期限，默认90天
            long days = 90;
            String remarks = endorsement.getRemarks();
            if (remarks != null && remarks.contains("至")) {
                try {
                    String[] parts = remarks.split("至");
                    if (parts.length == 2) {
                        String endDateStr = parts[1].split(" ")[0]; // 提取日期部分
                        LocalDate endDate = LocalDate.parse(endDateStr);
                        days = ChronoUnit.DAYS.between(LocalDate.now(), endDate);
                        if (days <= 0) days = 90;
                    }
                } catch (Exception e) {
                    log.warn("解析质押期限失败，使用默认90天: {}", e.getMessage());
                }
            }

            BigDecimal totalInterest = financingAmount
                    .multiply(interestRate)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal(days))
                    .divide(new BigDecimal("365"), 2, RoundingMode.HALF_UP);
            BigDecimal repaymentAmount = financingAmount.add(totalInterest);

            // 3. 上链质押
            log.info("开始质押上链, 仓单ID: {}", receipt.getId());
            String txHash = contractService.pledgeReceiptOnChain(
                    receipt.getId(),
                    endorsement.getEndorseTo(),
                    financingAmount
            );

            Long blockNumber = contractService.getBlockNumber(txHash);

            log.info("质押上链成功, 仓单ID: {}, txHash: {}, blockNumber: {}",
                    receipt.getId(), txHash, blockNumber);

            // 4. 更新背书区块链信息
            endorsement.setTxHash(txHash);
            endorsement.setBlockNumber(blockNumber);
            endorsementRepository.save(endorsement);

            // 5. 更新仓单状态为 PLEDGED，持有人改为金融机构
            String previousHolder = receipt.getHolderAddress();

            receipt.setReceiptStatus(ElectronicWarehouseReceipt.ReceiptStatus.PLEDGED);
            receipt.setHolderAddress(endorsement.getEndorseTo());
            receipt.setCurrentHolder(endorsement.getEndorseToName());
            receipt.setIsFinanced(true);
            receipt.setFinanceAmount(financingAmount);
            receipt.setFinanceRate(interestRate.multiply(new BigDecimal("100")).intValue());
            receipt.setFinanceDate(LocalDateTime.now());
            receipt.setFinancierAddress(endorsement.getEndorseTo());
            receipt.setEndorsementCount((receipt.getEndorsementCount() != null ?
                    receipt.getEndorsementCount() : 0) + 1);
            receipt.setLastEndorsementDate(LocalDateTime.now());
            receiptRepository.save(receipt);

            log.info("仓单状态已更新为PLEDGED, 持有人已更新为金融机构");

            // 6. 创建质押记录（链接到 endorsementId）
            PledgeRecord pledgeRecord = PledgeRecord.builder()
                    .receiptId(receipt.getId())
                    .receiptNo(receipt.getReceiptNo())
                    .endorsementId(endorsement.getId())
                    .endorsementNo(endorsement.getEndorsementNo())
                    .ownerId(receipt.getOwnerId())
                    .ownerName(receipt.getOwnerName())
                    .financialInstitutionId(confirmerId)
                    .financialInstitutionName(endorsement.getEndorseToName())
                    .financialInstitutionAddress(endorsement.getEndorseTo())
                    .previousHolderAddress(previousHolder)
                    .pledgeAmount(financingAmount)
                    .interestRate(interestRate)
                    .pledgeStartDate(LocalDate.now())
                    .pledgeEndDate(LocalDate.now().plusDays(days))
                    .status(PledgeRecord.PledgeStatus.ACTIVE)
                    .pledgeTime(LocalDateTime.now())
                    .txHash(txHash)
                    .blockNumber(blockNumber)
                    .remark("质押确认，所有权已转让给金融机构")
                    .build();

            PledgeRecord savedPledgeRecord = pledgeRecordRepository.save(pledgeRecord);

            // 7. 创建融资记录（链接到 endorsementId）
            String financingNo = generateFinancingNo();
            FinancingRecord financingRecord = FinancingRecord.builder()
                    .financingNo(financingNo)
                    .receiptId(receipt.getId())
                    .receiptNo(receipt.getReceiptNo())
                    .endorsementId(endorsement.getId())
                    .endorsementNo(endorsement.getEndorsementNo())
                    .ownerId(receipt.getOwnerId())
                    .ownerName(receipt.getOwnerName())
                    .financialInstitutionId(confirmerId)
                    .financialInstitutionName(endorsement.getEndorseToName())
                    .financingAmount(financingAmount)
                    .principalAmount(financingAmount)
                    .interestRate(interestRate)
                    .totalInterest(totalInterest)
                    .repaymentAmount(repaymentAmount)
                    .financingDate(LocalDate.now())
                    .dueDate(LocalDate.now().plusDays(days))
                    .status(FinancingRecord.FinancingStatus.ACTIVE)
                    .financingTime(LocalDateTime.now())
                    .remark(request.getRemark())
                    .build();

            FinancingRecord savedFinancingRecord = financingRecordRepository.save(financingRecord);

            log.info("质押确认完成，背书ID: {}，融资编号: {}", endorsement.getId(), financingNo);

            // 8. 构建响应
            return PledgeConfirmResponse.builder()
                    .endorsementId(endorsement.getId())
                    .confirmResult("CONFIRMED")
                    .confirmResultDesc("确认质押")
                    .receiptId(receipt.getId())
                    .receiptStatus("PLEDGED")
                    .pledgeRecordId(savedPledgeRecord.getId())
                    .financingRecordId(savedFinancingRecord.getId())
                    .financingAmount(financingAmount)
                    .interestRate(interestRate)
                    .repaymentAmount(repaymentAmount)
                    .txHash(txHash)
                    .blockNumber(blockNumber)
                    .confirmTime(LocalDateTime.now())
                    .previousHolderAddress(previousHolder)
                    .currentHolderAddress(endorsement.getEndorseTo())
                    .message("质押确认成功，仓单所有权已转让给金融机构")
                    .build();

        } catch (BusinessException e) {
            log.error("质押批准失败, 背书ID: {}, 错误: {}", endorsement.getId(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("质押批准失败, 背书ID: {}, 未知错误", endorsement.getId(), e);
            throw new BusinessException("质押批准失败：" + e.getMessage());
        }
    }

    /**
     * 处理质押取消（拒绝）
     */
    private PledgeConfirmResponse handlePledgeCancellation(
            EwrEndorsementChain endorsement,
            ElectronicWarehouseReceipt receipt,
            PledgeConfirmRequest request,
            String confirmerId,
            String confirmerName) {

        // 1. 更新背书状态为 CANCELLED
        endorsement.setEndorsementStatus(EwrEndorsementChain.EndorsementStatus.CANCELLED);
        String rejectionReason = request.getRejectionReason() != null ? request.getRejectionReason() : "未说明原因";
        endorsement.setRemarks(endorsement.getRemarks() +
                "\n拒绝原因：" + rejectionReason);
        endorsementRepository.save(endorsement);

        // 2. 恢复仓单状态为 NORMAL
        receipt.setReceiptStatus(ElectronicWarehouseReceipt.ReceiptStatus.NORMAL);
        receiptRepository.save(receipt);

        log.info("质押已拒绝，背书ID: {}，仓单状态已恢复为NORMAL", endorsement.getId());

        return PledgeConfirmResponse.builder()
                .endorsementId(endorsement.getId())
                .confirmResult("CANCELLED")
                .confirmResultDesc("拒绝质押")
                .receiptId(receipt.getId())
                .receiptStatus("NORMAL")
                .message("质押已拒绝，仓单状态已恢复正常")
                .build();
    }

    /**
     * 释放质押（货主还款）
     * 创建RELEASE背书，仓单状态恢复为NORMAL
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> releasePledge(
            @NonNull PledgeReleaseRequest request,
            String ownerId) {

        log.info("开始处理质押释放, 仓单ID: {}, 背书ID: {}, 还款金额: {}",
                request.getReceiptId(), request.getEndorsementId(), request.getRepayAmount());

        // 1. 查询质押背书
        EwrEndorsementChain pledgeEndorsement = endorsementRepository.findById(request.getEndorsementId())
                .orElseThrow(() -> new BusinessException("质押背书不存在"));

        if (pledgeEndorsement.getEndorsementType() != EwrEndorsementChain.EndorsementType.PLEDGE ||
                pledgeEndorsement.getEndorsementStatus() != EwrEndorsementChain.EndorsementStatus.CONFIRMED) {
            throw new BusinessException("该背书不是已确认的质押背书");
        }

        // 2. 查询质押记录（通过 endorsementId）
        PledgeRecord pledgeRecord = pledgeRecordRepository.findByEndorsementId(request.getEndorsementId())
                .orElseThrow(() -> new BusinessException("质押记录不存在"));

        if (pledgeRecord.getStatus() != PledgeRecord.PledgeStatus.ACTIVE) {
            throw new BusinessException("质押记录不是活跃状态，当前状态：" + pledgeRecord.getStatus());
        }

        // 3. 验证货主
        if (!pledgeRecord.getOwnerId().equals(ownerId)) {
            throw new BusinessException("您不是该质押的货主");
        }

        // 4. 查询融资记录（通过 endorsementId）
        FinancingRecord financingRecord = financingRecordRepository
                .findByEndorsementId(request.getEndorsementId())
                .orElseThrow(() -> new BusinessException("融资记录不存在"));

        // 5. 验证还款金额
        if (request.getRepayAmount().compareTo(financingRecord.getRepaymentAmount()) < 0) {
            throw new BusinessException(String.format(
                    "还款金额不足，应还金额：%s元，实还金额：%s元",
                    financingRecord.getRepaymentAmount(), request.getRepayAmount()));
        }

        // 6. 查询仓单
        ElectronicWarehouseReceipt receipt = receiptRepository.findById(request.getReceiptId())
                .orElseThrow(() -> new BusinessException("仓单不存在"));

        try {
            // 7. 上链释放
            log.info("开始质押释放上链, 仓单ID: {}", receipt.getId());
            String releaseTxHash = contractService.releaseReceiptOnChain(receipt.getId());

            Long releaseBlockNumber = contractService.getBlockNumber(releaseTxHash);

            log.info("质押释放上链成功, 仓单ID: {}, txHash: {}, blockNumber: {}",
                    receipt.getId(), releaseTxHash, releaseBlockNumber);

            // 8. 创建 RELEASE 类型背书（立即确认）
            String releaseEndorsementNo = generateEndorsementNo();

            EwrEndorsementChain releaseEndorsement = new EwrEndorsementChain();
            releaseEndorsement.setId(UUID.randomUUID().toString());
            releaseEndorsement.setReceiptId(receipt.getId());
            releaseEndorsement.setReceiptNo(receipt.getReceiptNo());
            releaseEndorsement.setEndorsementNo(releaseEndorsementNo);
            releaseEndorsement.setEndorseFrom(receipt.getHolderAddress()); // 当前持有人（金融机构）
            releaseEndorsement.setEndorseFromName(receipt.getCurrentHolder());
            releaseEndorsement.setEndorseTo(pledgeRecord.getPreviousHolderAddress()); // 原货主
            releaseEndorsement.setEndorseToName(pledgeRecord.getOwnerName());
            releaseEndorsement.setEndorsementType(EwrEndorsementChain.EndorsementType.RELEASE);
            releaseEndorsement.setEndorsementReason("还款后释放质押");
            releaseEndorsement.setEndorsementStatus(EwrEndorsementChain.EndorsementStatus.CONFIRMED);
            releaseEndorsement.setEndorsementTime(LocalDateTime.now());
            releaseEndorsement.setConfirmedTime(LocalDateTime.now());
            releaseEndorsement.setTxHash(releaseTxHash);
            releaseEndorsement.setBlockNumber(releaseBlockNumber);
            releaseEndorsement.setRemarks(String.format("还款金额：%s元", request.getRepayAmount()));

            endorsementRepository.save(releaseEndorsement);

            // 9. 更新仓单状态为 NORMAL，持有人归还给货主
            receipt.setReceiptStatus(ElectronicWarehouseReceipt.ReceiptStatus.NORMAL);
            receipt.setHolderAddress(pledgeRecord.getPreviousHolderAddress());
            receipt.setCurrentHolder(pledgeRecord.getOwnerName());
            receipt.setIsFinanced(false);
            receipt.setEndorsementCount((receipt.getEndorsementCount() != null ?
                    receipt.getEndorsementCount() : 0) + 1);
            receipt.setLastEndorsementDate(LocalDateTime.now());
            receiptRepository.save(receipt);

            log.info("仓单所有权已归还给货主");

            // 10. 更新质押记录状态
            pledgeRecord.setStatus(PledgeRecord.PledgeStatus.RELEASED);
            pledgeRecord.setReleaseTime(LocalDateTime.now());
            pledgeRecord.setReleaseTxHash(releaseTxHash);
            pledgeRecord.setReleaseBlockNumber(releaseBlockNumber);
            pledgeRecord.setRemark(pledgeRecord.getRemark() +
                    "\n质押已释放，所有权归还给货主");
            pledgeRecordRepository.save(pledgeRecord);

            // 11. 更新融资记录状态
            financingRecord.setStatus(FinancingRecord.FinancingStatus.PAID_OFF);
            financingRecord.setActualRepaymentDate(LocalDate.now());
            financingRecord.setRepaymentTime(LocalDateTime.now());
            financingRecordRepository.save(financingRecord);

            log.info("质押释放完成，仓单ID: {}，所有权已归还给货主", receipt.getId());

            // 12. 构建响应
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("message", "质押释放成功，所有权已归还给货主");
            response.put("receiptId", request.getReceiptId());
            response.put("endorsementId", request.getEndorsementId());
            response.put("releaseEndorsementId", releaseEndorsement.getId());
            response.put("txHash", releaseTxHash);
            response.put("blockNumber", releaseBlockNumber);
            response.put("receiptStatus", "NORMAL");
            response.put("repaymentAmount", request.getRepayAmount());
            response.put("releaseTime", LocalDateTime.now());
            response.put("previousHolder", pledgeRecord.getFinancialInstitutionAddress());
            response.put("currentHolder", pledgeRecord.getPreviousHolderAddress());

            return response;

        } catch (BusinessException e) {
            log.error("质押释放失败, 仓单ID: {}, 错误: {}", receipt.getId(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("质押释放失败, 仓单ID: {}, 未知错误", receipt.getId(), e);
            throw new BusinessException("质押释放失败：" + e.getMessage());
        }
    }

    /**
     * 查询待确认的质押背书
     */
    public List<PledgeInitiateResponse> getPendingPledges(String financialInstitutionAddress) {
        log.info("查询待确认质押, 金融机构地址: {}", financialInstitutionAddress);

        // 查询待确认的质押背书
        List<EwrEndorsementChain> pending = endorsementRepository
                .findPendingEndorsementsByEndorseTo(financialInstitutionAddress)
                .stream()
                .filter(e -> e.getEndorsementType() == EwrEndorsementChain.EndorsementType.PLEDGE)
                .collect(Collectors.toList());

        return pending.stream()
                .map(e -> PledgeInitiateResponse.builder()
                        .endorsementId(e.getId())
                        .endorsementNo(e.getEndorsementNo())
                        .receiptId(e.getReceiptId())
                        .receiptNo(e.getReceiptNo())
                        .receiptStatus("FROZEN")
                        .financialInstitutionName(e.getEndorseToName())
                        .endorsementStatus("PENDING")
                        .initiateTime(e.getEndorsementTime())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 查询质押历史记录
     */
    public List<PledgeRecordResponse> getPledgeHistory(@NonNull String receiptId) {
        List<PledgeRecord> records = pledgeRecordRepository.findByReceiptIdOrderByPledgeTimeDesc(receiptId);
        return records.stream()
                .map(PledgeRecordResponse::fromEntity)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 分页查询质押记录
     */
    public Page<PledgeRecordResponse> queryPledgeRecords(
            String receiptId,
            String ownerId,
            String financialInstitutionId,
            String status,
            Pageable pageable) {

        PledgeRecord.PledgeStatus pledgeStatus = null;
        if (status != null && !status.isEmpty()) {
            pledgeStatus = PledgeRecord.PledgeStatus.valueOf(status);
        }

        Page<PledgeRecord> page = pledgeRecordRepository.findByConditions(
                receiptId, ownerId, financialInstitutionId, pledgeStatus, pageable);

        return page.map(PledgeRecordResponse::fromEntity);
    }

    /**
     * 生成背书编号
     */
    private String generateEndorsementNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String sequence = String.format("%06d", System.currentTimeMillis() % 1000000);
        return "PLG" + dateStr + sequence;
    }

    /**
     * 生成融资编号
     */
    private String generateFinancingNo() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "FIN" + timestamp + uuid;
    }
}
