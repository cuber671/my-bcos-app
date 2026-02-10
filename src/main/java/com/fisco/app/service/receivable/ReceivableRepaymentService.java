package com.fisco.app.service.receivable;

import com.fisco.app.dto.receivable.RepaymentRecordResponse;
import com.fisco.app.dto.receivable.RepayDetailRequest;
import com.fisco.app.entity.receivable.Receivable;
import com.fisco.app.entity.receivable.ReceivableRepaymentRecord;
import com.fisco.app.exception.BlockchainIntegrationException;
import com.fisco.app.exception.BusinessException;
import com.fisco.app.repository.receivable.ReceivableRepaymentRecordRepository;
import com.fisco.app.repository.receivable.ReceivableRepository;
import com.fisco.app.service.blockchain.ContractService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 应收账款还款Service
 *
 * 提供应收账款的还款详情记录功能，支持部分还款、提前还款、逾期还款等场景
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-09
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReceivableRepaymentService {

    private final ReceivableRepaymentRecordRepository repaymentRecordRepository;
    private final ReceivableRepository receivableRepository;
    private final ContractService contractService;

    /**
     * 还款详情记录（支持部分还款、提前还款、逾期还款）
     *
     * @param request 还款详情请求
     * @param payerAddress 还款人地址（核心企业）
     * @return 还款记录响应
     */
    @Transactional(rollbackFor = Exception.class)
    public RepaymentRecordResponse repayDetail(@NonNull RepayDetailRequest request, @NonNull String payerAddress) {
        log.info("==================== 应收账款还款详情开始 ====================");
        log.info("还款基本信息: receivableId={}, type={}, amount={}",
                request.getReceivableId(), request.getRepaymentType(), request.getRepaymentAmount());

        long startTime = System.currentTimeMillis();

        try {
            // 1. 查询应收账款（使用receivable_id字段查询）
            Receivable receivable = receivableRepository.findByReceivableId(request.getReceivableId())
                    .orElseThrow(() -> new BusinessException("应收账款不存在: " + request.getReceivableId()));

            log.debug("✓ 应收账款查询成功: status={}, currentHolder={}",
                    receivable.getStatus(), receivable.getCurrentHolder());

            // 2. 验证还款人权限（必须是核心企业）
            if (!receivable.getCoreEnterpriseAddress().equals(payerAddress)) {
                log.error("权限验证失败: 核心企业地址={}, 还款人地址={}",
                        receivable.getCoreEnterpriseAddress(), payerAddress);
                throw new BusinessException("只有核心企业可以进行还款");
            }
            log.debug("✓ 权限验证通过");

            // 3. 确定收款人（关键业务逻辑）
            String receiverAddress;
            if (receivable.getStatus() == Receivable.ReceivableStatus.FINANCED) {
                // 已融资：还给当前持有人（金融机构）
                receiverAddress = receivable.getCurrentHolder();
                log.info("已融资状态，还款给金融机构: receiver={}", receiverAddress);
            } else {
                // 未融资：还给供应商
                receiverAddress = receivable.getSupplierAddress();
                log.info("未融资状态，还款给供应商: receiver={}", receiverAddress);
            }

            // 4. 计算剩余应还金额
            BigDecimal totalRepaid = repaymentRecordRepository.totalRepaidAmountByReceivable(
                    request.getReceivableId());
            BigDecimal remainingAmount = receivable.getAmount().subtract(totalRepaid);

            log.debug("还款金额计算: 总金额={}, 已还金额={}, 剩余金额={}",
                    receivable.getAmount(), totalRepaid, remainingAmount);

            if (request.getRepaymentAmount().compareTo(remainingAmount) > 0) {
                log.error("还款金额超过剩余应还金额: 还款金额={}, 剩余金额={}",
                        request.getRepaymentAmount(), remainingAmount);
                throw new BusinessException("还款金额超过剩余应还金额: 剩余=" + remainingAmount);
            }

            // 4.5. 验证还款金额组成（本金+利息+罚息必须等于还款总金额）
            BigDecimal interestAmount = request.getInterestAmount() != null ?
                    request.getInterestAmount() : BigDecimal.ZERO;
            BigDecimal penaltyAmount = request.getPenaltyAmount() != null ?
                    request.getPenaltyAmount() : BigDecimal.ZERO;
            BigDecimal totalComponents = request.getPrincipalAmount()
                    .add(interestAmount)
                    .add(penaltyAmount);

            if (totalComponents.compareTo(request.getRepaymentAmount()) != 0) {
                log.error("还款金额组成不一致: 总金额={}, 本金={}, 利息={}, 罚息={}, 组合总和={}",
                        request.getRepaymentAmount(), request.getPrincipalAmount(),
                        interestAmount, penaltyAmount, totalComponents);
                throw new BusinessException(String.format(
                        "还款金额组成不一致: 总金额=%s, 本金+利息+罚息=%s (差额=%s)",
                        request.getRepaymentAmount(), totalComponents,
                        request.getRepaymentAmount().subtract(totalComponents).abs()));
            }
            log.debug("✓ 还款金额组成验证通过");

            // 5. 计算提前/逾期天数
            Integer earlyDays = null;
            Integer overdueDays = null;
            LocalDate paymentDate = request.getPaymentDate();
            LocalDate dueDate = receivable.getDueDate().toLocalDate();

            if (paymentDate.isBefore(dueDate)) {
                earlyDays = (int) ChronoUnit.DAYS.between(paymentDate, dueDate);
                log.debug("提前还款: 提前天数={}", earlyDays);
            } else if (paymentDate.isAfter(dueDate)) {
                overdueDays = (int) ChronoUnit.DAYS.between(dueDate, paymentDate);
                log.debug("逾期还款: 逾期天数={}", overdueDays);
            }

            // 6. 创建还款记录
            ReceivableRepaymentRecord record = new ReceivableRepaymentRecord();
            record.setReceivableId(request.getReceivableId());
            record.setRepaymentType(request.getRepaymentType());
            record.setRepaymentAmount(request.getRepaymentAmount());
            record.setPrincipalAmount(request.getPrincipalAmount());
            record.setInterestAmount(request.getInterestAmount());
            record.setPenaltyAmount(request.getPenaltyAmount());
            record.setPayerAddress(payerAddress);
            record.setReceiverAddress(receiverAddress);
            record.setPaymentDate(paymentDate);
            record.setActualPaymentTime(LocalDateTime.now());
            record.setPaymentMethod(request.getPaymentMethod());
            record.setPaymentAccount(request.getPaymentAccount());
            record.setTransactionNo(request.getTransactionNo());
            record.setVoucherUrl(request.getVoucherUrl());
            record.setEarlyPaymentDays(earlyDays);
            record.setOverdueDays(overdueDays);
            record.setRemark(request.getRemark());
            record.setCreatedBy(payerAddress);
            record.setStatus(ReceivableRepaymentRecord.RepaymentStatus.PENDING);

            // 7. 调用区块链合约
            try {
                String txHash = contractService.repayReceivableOnChain(
                        request.getReceivableId(), request.getRepaymentAmount());
                record.setTxHash(txHash);
                record.setStatus(ReceivableRepaymentRecord.RepaymentStatus.CONFIRMED);
                log.info("✓ 还款已上链: txHash={}", txHash);
            } catch (BlockchainIntegrationException e) {
                record.setStatus(ReceivableRepaymentRecord.RepaymentStatus.FAILED);
                log.error("✗ 区块链上链失败: {}", e.getMessage());
                // 即使上链失败，也保存还款记录，状态为FAILED
            }

            // 8. 保存还款记录
            ReceivableRepaymentRecord saved = repaymentRecordRepository.save(record);
            log.info("✓ 还款记录已保存: recordId={}", saved.getId());

            // 9. 检查是否全额还款
            BigDecimal newTotalRepaid = totalRepaid.add(request.getRepaymentAmount());
            if (newTotalRepaid.compareTo(receivable.getAmount()) >= 0) {
                // 全额还款完成，更新状态
                receivable.setStatus(Receivable.ReceivableStatus.REPAID);
                receivableRepository.save(receivable);
                log.info("✓✓✓ 全额还款完成，更新状态为REPAID");
            } else {
                log.info("✓ 部分还款完成: 已还金额={}, 剩余金额={}",
                        newTotalRepaid, receivable.getAmount().subtract(newTotalRepaid));
            }

            // 10. 返回响应
            RepaymentRecordResponse response = convertToResponse(saved);

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 应收账款还款完成: receivableId={}, 耗时={}ms",
                    request.getReceivableId(), duration);
            log.info("==================== 应收账款还款结束 ====================");

            return response;

        } catch (BusinessException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 应收账款还款失败: receivableId={}, 耗时={}ms, error={}",
                    request.getReceivableId(), duration, e.getMessage());
            log.info("==================== 应收账款还款失败（结束） ====================");
            throw e;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 应收账款还款失败: receivableId={}, 耗时={}ms, error={}",
                    request.getReceivableId(), duration, e.getMessage(), e);
            log.info("==================== 应收账款还款失败（结束） ====================");
            throw new BusinessException(500, "还款失败: " + e.getMessage(), e);
        }
    }

    /**
     * 查询应收账款的所有还款记录
     *
     * @param receivableId 应收账款ID
     * @param userAddress 用户地址
     * @return 还款记录列表
     */
    public List<RepaymentRecordResponse> getRepaymentRecords(
            @NonNull String receivableId,
            @NonNull String userAddress) {

        log.info("查询还款记录: receivableId={}, userAddress={}", receivableId, userAddress);

        // 权限验证：只有参与方可以查询
        Receivable receivable = receivableRepository.findByReceivableId(receivableId)
                .orElseThrow(() -> new BusinessException("应收账款不存在: " + receivableId));

        if (!isParticipant(receivable, userAddress)) {
            log.error("权限验证失败: 用户无权限查询此应收账款的还款记录");
            throw new BusinessException("无权限查询此应收账款的还款记录");
        }

        List<ReceivableRepaymentRecord> records =
                repaymentRecordRepository.findByReceivableIdOrderByActualPaymentTimeDesc(receivableId);

        log.info("查询到{}条还款记录", records.size());

        return records.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 判断用户是否是应收账款的参与方
     *
     * @param receivable 应收账款
     * @param userAddress 用户地址
     * @return 是否是参与方
     */
    private boolean isParticipant(Receivable receivable, String userAddress) {
        return userAddress.equals(receivable.getSupplierAddress()) ||
                userAddress.equals(receivable.getCoreEnterpriseAddress()) ||
                userAddress.equals(receivable.getCurrentHolder()) ||
                (receivable.getFinancierAddress() != null &&
                 userAddress.equals(receivable.getFinancierAddress()));
    }

    /**
     * 将实体转换为响应DTO
     *
     * @param record 还款记录实体
     * @return 还款记录响应
     */
    private RepaymentRecordResponse convertToResponse(ReceivableRepaymentRecord record) {
        RepaymentRecordResponse response = new RepaymentRecordResponse();

        response.setId(record.getId());
        response.setReceivableId(record.getReceivableId());
        response.setRepaymentType(record.getRepaymentType().name());
        response.setRepaymentTypeName(record.getRepaymentType().getDescription());
        response.setRepaymentAmount(record.getRepaymentAmount());
        response.setPrincipalAmount(record.getPrincipalAmount());
        response.setInterestAmount(record.getInterestAmount());
        response.setPenaltyAmount(record.getPenaltyAmount());
        response.setPayerAddress(record.getPayerAddress());
        response.setReceiverAddress(record.getReceiverAddress());
        response.setPaymentDate(record.getPaymentDate());
        response.setActualPaymentTime(record.getActualPaymentTime());
        response.setPaymentMethod(record.getPaymentMethod());
        response.setPaymentMethodName(getPaymentMethodName(record.getPaymentMethod()));
        response.setPaymentAccount(record.getPaymentAccount());
        response.setTransactionNo(record.getTransactionNo());
        response.setVoucherUrl(record.getVoucherUrl());
        response.setEarlyPaymentDays(record.getEarlyPaymentDays());
        response.setOverdueDays(record.getOverdueDays());
        response.setRemark(record.getRemark());
        response.setStatus(record.getStatus().name());
        response.setStatusName(record.getStatus().getDescription());
        response.setTxHash(record.getTxHash());
        response.setCreatedAt(record.getCreatedAt());
        response.setCreatedBy(record.getCreatedBy());

        return response;
    }

    /**
     * 获取支付方式名称
     *
     * @param paymentMethod 支付方式代码
     * @return 支付方式名称
     */
    private String getPaymentMethodName(String paymentMethod) {
        if (paymentMethod == null) {
            return null;
        }
        switch (paymentMethod) {
            case "BANK":
                return "银行转账";
            case "ALIPAY":
                return "支付宝";
            case "WECHAT":
                return "微信";
            case "OTHER":
                return "其他";
            default:
                return paymentMethod;
        }
    }
}
