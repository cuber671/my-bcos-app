package com.fisco.app.service.bill;

import java.time.LocalDateTime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fisco.app.dto.bill.BillStatisticsDTO;
import com.fisco.app.dto.bill.DiscountBillRequest;
import com.fisco.app.dto.bill.DiscountBillResponse;
import com.fisco.app.dto.bill.IssueBillRequest;
import com.fisco.app.dto.bill.RepayBillRequest;
import com.fisco.app.dto.bill.RepayBillResponse;
import com.fisco.app.dto.endorsement.EndorseBillRequest;
import com.fisco.app.dto.endorsement.EndorsementResponse;
import com.fisco.app.entity.bill.Bill;
import com.fisco.app.entity.bill.DiscountRecord;
import com.fisco.app.entity.bill.Endorsement;
import com.fisco.app.entity.bill.RepaymentRecord;
import com.fisco.app.exception.BlockchainIntegrationException;
import com.fisco.app.repository.bill.BillRepository;
import com.fisco.app.repository.bill.DiscountRecordRepository;
import com.fisco.app.repository.bill.EndorsementRepository;
import com.fisco.app.repository.bill.RepaymentRecordRepository;
import com.fisco.app.service.blockchain.ContractService;
import com.fisco.app.service.enterprise.EnterpriseService;

import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 票据Service
 */
@Slf4j
@ConditionalOnProperty(name = "fisco.enabled", havingValue = "true", matchIfMissing = true)
@Service
@Api(tags = "票据服务")
@RequiredArgsConstructor
public class BillService {

    private final BillRepository billRepository;
    private final EnterpriseService enterpriseService;
    private final ContractService contractService;
    private final EndorsementRepository endorsementRepository;
    private final DiscountRecordRepository discountRecordRepository;
    private final RepaymentRecordRepository repaymentRecordRepository;

    /**
     * 开票
     */
    @Transactional(rollbackFor = Exception.class)
    public Bill issueBill(IssueBillRequest request, String issuerAddress) {
        log.info("==================== 票据开立开始 ====================");
        log.info("票据基本信息: billId={}, type={}, amount={}, currency={}",
                 request.getId(), request.getBillType(), request.getAmount(), request.getCurrency());
        log.info("参与方: issuer={}, acceptor={}, beneficiary={}",
                 issuerAddress, request.getAcceptorAddress(), request.getBeneficiaryAddress());
        log.info("日期信息: issueDate={}, dueDate={}",
                 request.getIssueDate(), request.getDueDate());

        long startTime = System.currentTimeMillis();

        try {
            // 检查票据ID是否已存在
            log.debug("检查票据ID唯一性: billId={}", request.getId());
            if (billRepository.findById(request.getId()).isPresent()) {
                log.error("票据ID已存在: billId={}", request.getId());
                throw new com.fisco.app.exception.BusinessException("票据ID已存在");
            }
            log.debug("✓ 票据ID唯一性检查通过");

            // 验证出票人是否存在
            log.debug("验证出票人: issuerAddress={}", issuerAddress);
            if (!enterpriseService.isEnterpriseValid(issuerAddress)) {
                log.error("出票人不存在或未激活: issuerAddress={}", issuerAddress);
                throw new com.fisco.app.exception.BusinessException("出票人不存在或未激活");
            }
            log.debug("✓ 出票人验证通过");

            // 验证承兑人是否存在
            log.debug("验证承兑人: acceptorAddress={}", request.getAcceptorAddress());
            if (!enterpriseService.isEnterpriseValid(request.getAcceptorAddress())) {
                log.error("承兑人不存在或未激活: acceptorAddress={}", request.getAcceptorAddress());
                throw new com.fisco.app.exception.BusinessException("承兑人不存在或未激活");
            }
            log.debug("✓ 承兑人验证通过");

            // 验证受益人是否存在
            log.debug("验证受益人: beneficiaryAddress={}", request.getBeneficiaryAddress());
            if (!enterpriseService.isEnterpriseValid(request.getBeneficiaryAddress())) {
                log.error("受益人不存在或未激活: beneficiaryAddress={}", request.getBeneficiaryAddress());
                throw new com.fisco.app.exception.BusinessException("受益人不存在或未激活");
            }
            log.debug("✓ 受益人验证通过");

            // 验证日期
            log.debug("验证日期合理性");
            if (request.getDueDate().isBefore(request.getIssueDate())) {
                log.error("到期日期必须晚于出票日期: issueDate={}, dueDate={}",
                         request.getIssueDate(), request.getDueDate());
                throw new com.fisco.app.exception.BusinessException("到期日期必须晚于出票日期");
            }
            log.debug("✓ 日期验证通过");

            // 从请求构建 Bill 实体
            log.debug("构建票据实体");
            Bill bill = new Bill();
            bill.setBillId(request.getId());
            bill.setBillType(request.getBillType());
            bill.setDrawerAddress(issuerAddress);
            bill.setDraweeAddress(request.getAcceptorAddress());
            bill.setPayeeAddress(request.getBeneficiaryAddress());
            bill.setCurrentHolderAddress(request.getBeneficiaryAddress());
            bill.setFaceValue(request.getAmount());
            bill.setCurrency(request.getCurrency());
            bill.setIssueDate(request.getIssueDate());
            bill.setDueDate(request.getDueDate());
            bill.setGoodsDescription(request.getDescription());
            bill.setBillStatus(Bill.BillStatus.ISSUED);

            // 步骤1: 保存到数据库（触发 @PrePersist 设置 createdAt/updatedAt）
            log.debug("保存票据到数据库");
            Bill saved = billRepository.save(bill);
            log.info("✓ 数据库保存成功: billId={}", saved.getBillId());

            // 步骤2: 调用区块链合约
            log.debug("准备上链开立票据");
            try {
                String txHash = contractService.issueBillOnChain(saved);
                log.info("✓ 票据已上链: billId={}, txHash={}", saved.getBillId(), txHash);

                // 步骤3: 更新 txHash
                log.debug("更新交易哈希到数据库");
                saved.setBlockchainTxHash(txHash);
                Bill finalSaved = billRepository.save(saved);
                log.info("✓ 交易哈希已保存");

                long duration = System.currentTimeMillis() - startTime;
                log.info("✓✓✓ 票据开立完成: billId={}, txHash={}, 耗时={}ms",
                         finalSaved.getBillId(), txHash, duration);
                log.info("==================== 票据开立结束 ====================");
                return finalSaved;

            } catch (BlockchainIntegrationException e) {
                long duration = System.currentTimeMillis() - startTime;
                log.error("✗✗✗ 区块链调用失败，回滚数据库事务: billId={}, 耗时={}ms, error={}",
                         saved.getBillId(), duration, e.getMessage(), e);
                log.info("==================== 票据开立失败（结束） ====================");
                // 抛出业务异常，触发 @Transactional 回滚
                throw new com.fisco.app.exception.BusinessException(
                    500, "区块链操作失败: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 票据开立失败: billId={}, 耗时={}ms, error={}",
                     request.getId(), duration, e.getMessage(), e);
            log.info("==================== 票据开立失败（结束） ====================");
            throw e;
        }
    }

    /**
     * 承兑票据
     */
    @Transactional
    public void acceptBill(@NonNull String billId) {
        log.info("==================== 票据承兑开始 ====================");
        log.info("票据ID: {}", billId);

        long startTime = System.currentTimeMillis();

        try {
            log.debug("查询票据信息: billId={}", billId);
            Bill bill = billRepository.findById(billId)
                    .orElseThrow(() -> new com.fisco.app.exception.BusinessException("票据不存在: " + billId));
            log.debug("✓ 票据查询成功: currentStatus={}", bill.getBillStatus());

            // 验证状态
            log.debug("验证票据状态");
            if (bill.getBillStatus() != Bill.BillStatus.ISSUED) {
                log.error("票据状态错误，只能承兑已开立的票据: currentStatus={}", bill.getBillStatus());
                throw new com.fisco.app.exception.BusinessException("只能承兑已开立的票据");
            }
            log.debug("✓ 状态验证通过");

            // 步骤1: 调用区块链合约
            log.debug("准备上链承兑票据");
            try {
                String txHash = contractService.acceptBillOnChain(billId);
                log.info("✓ 票据已上链承兑: billId={}, txHash={}", billId, txHash);

                // 步骤2: 更新数据库状态和交易哈希
                log.debug("更新数据库状态: {} -> NORMAL", bill.getBillStatus());
                bill.setBillStatus(Bill.BillStatus.ISSUED);
                bill.setBlockchainTxHash(txHash);
                billRepository.save(bill);
                log.info("✓ 数据库更新成功");

                long duration = System.currentTimeMillis() - startTime;
                log.info("✓✓✓ 票据承兑完成: billId={}, txHash={}, 耗时={}ms", billId, txHash, duration);
                log.info("==================== 票据承兑结束 ====================");

            } catch (BlockchainIntegrationException e) {
                long duration = System.currentTimeMillis() - startTime;
                log.error("✗✗✗ 区块链调用失败，回滚数据库事务: billId={}, 耗时={}ms, error={}",
                         billId, duration, e.getMessage(), e);
                log.info("==================== 票据承兑失败（结束） ====================");
                throw new com.fisco.app.exception.BusinessException(
                    500, "区块链操作失败: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 票据承兑失败: billId={}, 耗时={}ms, error={}", billId, duration, e.getMessage(), e);
            log.info("==================== 票据承兑失败（结束） ====================");
            throw e;
        }
    }

    /**
     * 支付票据
     */
    @Transactional
    public void payBill(@NonNull String billId) {
        log.info("支付票据: id={}", billId);

        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new com.fisco.app.exception.BusinessException("票据不存在: " + billId));

        if (bill.getBillStatus() != Bill.BillStatus.ISSUED) {
            throw new com.fisco.app.exception.BusinessException("只能支付已承兑的票据");
        }

        // 步骤1: 调用区块链合约
        try {
            String txHash = contractService.payBillOnChain(billId);

            // 步骤2: 更新数据库状态和交易哈希
            bill.setBillStatus(Bill.BillStatus.PAID);
            bill.setBlockchainTxHash(txHash);
            billRepository.save(bill);

            log.info("票据支付成功: id={}, txHash={}", billId, txHash);

        } catch (BlockchainIntegrationException e) {
            log.error("区块链调用失败，回滚数据库事务: billId={}, error={}",
                billId, e.getMessage(), e);
            // 抛出业务异常，触发 @Transactional 回滚
            throw new com.fisco.app.exception.BusinessException(
                500, "区块链操作失败: " + e.getMessage(), e);
        }
    }

    /**
     * 票据背书
     */
    @Transactional
    public EndorsementResponse endorseBill(@NonNull String billId, EndorseBillRequest request, String endorserAddress) {
        log.info("票据背书: billId={}, endorser={}, endorsee={}, type={}",
            billId, endorserAddress, request.getEndorseeAddress(), request.getEndorsementType());

        // 步骤1: 验证票据
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new com.fisco.app.exception.BusinessException("票据不存在: " + billId));

        // 验证票据状态 - 只有已开立、已承兑或已背书的票据可以再次背书
        if (bill.getBillStatus() != Bill.BillStatus.ISSUED &&
            bill.getBillStatus() != Bill.BillStatus.ISSUED &&
            bill.getBillStatus() != Bill.BillStatus.ENDORSED) {
            throw new com.fisco.app.exception.BusinessException.InvalidStatusException(
                "只能背书已开立、已承兑或已背书的票据");
        }

        // 验证背书人是否为当前持票人
        if (!bill.getCurrentHolderAddress().equals(endorserAddress)) {
            throw new com.fisco.app.exception.BusinessException("只有当前持票人可以背书转让");
        }

        // 验证被背书人是否存在
        if (!enterpriseService.isEnterpriseValid(request.getEndorseeAddress())) {
            throw new com.fisco.app.exception.BusinessException("被背书人不存在或未激活");
        }

        // 不能背书给自己
        if (endorserAddress.equals(request.getEndorseeAddress())) {
            throw new com.fisco.app.exception.BusinessException("不能背书给自己");
        }

        // 步骤2: 调用区块链合约
        try {
            String txHash = contractService.endorseBillOnChain(billId, request.getEndorseeAddress());

            // 步骤3: 创建背书记录
            Endorsement endorsement = new Endorsement();
            endorsement.setBillId(billId);
            endorsement.setEndorserAddress(endorserAddress);
            endorsement.setEndorseeAddress(request.getEndorseeAddress());
            endorsement.setEndorsementType(request.getEndorsementType());
            endorsement.setEndorsementAmount(request.getEndorsementAmount());
            endorsement.setRemark(request.getRemark());
            endorsement.setTxHash(txHash);

            // 获取下一个背书序号
            Integer nextSequence = endorsementRepository.getNextEndorsementSequence(billId);
            endorsement.setEndorsementSequence(nextSequence != null ? nextSequence : 1);

            Endorsement savedEndorsement = endorsementRepository.save(endorsement);

            // 步骤4: 更新票据状态和持票人
            bill.setCurrentHolderAddress(request.getEndorseeAddress());
            bill.setBillStatus(Bill.BillStatus.ENDORSED);
            bill.setBlockchainTxHash(txHash);
            billRepository.save(bill);

            // 步骤5: 构建响应
            EndorsementResponse response = new EndorsementResponse();
            response.setId(savedEndorsement.getId());
            response.setBillId(billId);
            response.setEndorserAddress(savedEndorsement.getEndorserAddress());
            response.setEndorseeAddress(savedEndorsement.getEndorseeAddress());
            response.setEndorsementType(savedEndorsement.getEndorsementType());
            response.setEndorsementAmount(savedEndorsement.getEndorsementAmount());
            response.setEndorsementDate(savedEndorsement.getEndorsementDate());
            response.setEndorsementSequence(savedEndorsement.getEndorsementSequence());
            response.setTxHash(savedEndorsement.getTxHash());
            response.setRemark(savedEndorsement.getRemark());

            log.info("票据背书成功: billId={}, endorser={}, endorsee={}, sequence={}",
                billId, endorserAddress, request.getEndorseeAddress(), savedEndorsement.getEndorsementSequence());

            return response;

        } catch (BlockchainIntegrationException e) {
            log.error("区块链调用失败，回滚数据库事务: billId={}, error={}",
                billId, e.getMessage(), e);
            // 抛出业务异常，触发 @Transactional 回滚
            throw new com.fisco.app.exception.BusinessException(
                500, "区块链操作失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取票据的背书历史
     */
    public java.util.List<Endorsement> getEndorsementHistory(@NonNull String billId) {
        log.info("查询票据背书历史: billId={}", billId);

        // 验证票据是否存在
        if (!billRepository.findById(billId).isPresent()) {
            throw new com.fisco.app.exception.BusinessException("票据不存在: " + billId);
        }

        return endorsementRepository.findByBillIdOrderByEndorsementDateAsc(billId);
    }

    /**
     * 票据贴现
     */
    @Transactional
    public DiscountBillResponse discountBill(@NonNull String billId, DiscountBillRequest request, String holderAddress) {
        log.info("票据贴现: billId={}, holder={}, institution={}, amount={}, rate={}",
            billId, holderAddress, request.getFinancialInstitutionAddress(),
            request.getDiscountAmount(), request.getDiscountRate());

        // 步骤1: 验证票据
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new com.fisco.app.exception.BusinessException("票据不存在: " + billId));

        // 验证票据状态 - 只有已开立、已承兑或已背书的票据可以贴现
        if (bill.getBillStatus() != Bill.BillStatus.ISSUED &&
            bill.getBillStatus() != Bill.BillStatus.ISSUED &&
            bill.getBillStatus() != Bill.BillStatus.ENDORSED) {
            throw new com.fisco.app.exception.BusinessException.InvalidStatusException(
                "只能贴现已开立、已承兑或已背书的票据");
        }

        // 验证持票人是否为当前持票人
        if (!bill.getCurrentHolderAddress().equals(holderAddress)) {
            throw new com.fisco.app.exception.BusinessException("只有当前持票人可以申请贴现");
        }

        // 验证金融机构是否存在
        if (!enterpriseService.isEnterpriseValid(request.getFinancialInstitutionAddress())) {
            throw new com.fisco.app.exception.BusinessException("金融机构不存在或未激活");
        }

        // 不能自己贴现给自己
        if (holderAddress.equals(request.getFinancialInstitutionAddress())) {
            throw new com.fisco.app.exception.BusinessException("不能向自己贴现");
        }

        // 验证贴现金额
        if (request.getDiscountAmount().compareTo(bill.getFaceValue()) > 0) {
            throw new com.fisco.app.exception.BusinessException("贴现金额不能超过票面金额");
        }

        // 步骤2: 调用区块链合约
        try {
            String txHash = contractService.discountBillOnChain(
                billId,
                request.getFinancialInstitutionAddress(),
                request.getDiscountAmount(),
                request.getDiscountRate()
            );

            // 步骤3: 创建贴现记录
            DiscountRecord discountRecord = new DiscountRecord();
            discountRecord.setId(billId);
            discountRecord.setHolderAddress(holderAddress);
            discountRecord.setFinancialInstitutionAddress(request.getFinancialInstitutionAddress());
            discountRecord.setBillAmount(bill.getFaceValue());
            discountRecord.setDiscountAmount(request.getDiscountAmount());
            discountRecord.setDiscountRate(request.getDiscountRate());
            discountRecord.setMaturityDate(bill.getDueDate());
            discountRecord.setRemark(request.getRemark());
            discountRecord.setTxHash(txHash);
            discountRecord.setStatus(DiscountRecord.DiscountStatus.ACTIVE);

            DiscountRecord savedRecord = discountRecordRepository.save(discountRecord);

            // 步骤4: 更新票据状态和持票人
            bill.setCurrentHolderAddress(request.getFinancialInstitutionAddress());
            bill.setBillStatus(Bill.BillStatus.DISCOUNTED);
            bill.setDiscountInstitutionId(request.getFinancialInstitutionAddress());
            bill.setDiscountAmount(request.getDiscountAmount());
            bill.setDiscountDate(java.time.LocalDateTime.now());
            bill.setBlockchainTxHash(txHash);
            billRepository.save(bill);

            // 步骤5: 构建响应
            DiscountBillResponse response = new DiscountBillResponse();
            response.setId(savedRecord.getId());
            response.setId(savedRecord.getId());
            response.setHolderAddress(savedRecord.getHolderAddress());
            response.setFinancialInstitutionAddress(savedRecord.getFinancialInstitutionAddress());
            response.setBillAmount(savedRecord.getBillAmount());
            response.setDiscountAmount(savedRecord.getDiscountAmount());
            response.setDiscountRate(savedRecord.getDiscountRate());
            response.setDiscountInterest(savedRecord.getDiscountInterest());
            response.setDiscountDate(savedRecord.getDiscountDate());
            response.setMaturityDate(savedRecord.getMaturityDate());
            response.setDiscountDays(savedRecord.getDiscountDays());
            response.setTxHash(savedRecord.getTxHash());
            response.setStatus(savedRecord.getStatus());
            response.setRemark(savedRecord.getRemark());

            log.info("票据贴现成功: billId={}, holder={}, institution={}, amount={}",
                billId, holderAddress, request.getFinancialInstitutionAddress(), request.getDiscountAmount());

            return response;

        } catch (BlockchainIntegrationException e) {
            log.error("区块链调用失败，回滚数据库事务: billId={}, error={}",
                billId, e.getMessage(), e);
            // 抛出业务异常，触发 @Transactional 回滚
            throw new com.fisco.app.exception.BusinessException(
                500, "区块链操作失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取票据的贴现记录
     */
    public java.util.List<DiscountRecord> getDiscountHistory(@NonNull String billId) {
        log.info("查询票据贴现历史: billId={}", billId);

        // 验证票据是否存在
        if (!billRepository.findById(billId).isPresent()) {
            throw new com.fisco.app.exception.BusinessException("票据不存在: " + billId);
        }

        return discountRecordRepository.findByBillIdOrderByDiscountDateDesc(billId);
    }

    /**
     * 从区块链获取票据背书历史
     *
     * @param billId 票据ID
     * @return 区块链上的背书历史记录
     */
    public java.util.List<java.util.Map<String, Object>> getEndorsementHistoryFromChain(@NonNull String billId) {
        log.info("从区块链获取背书历史: billId={}", billId);

        // 验证票据是否存在
        if (!billRepository.findById(billId).isPresent()) {
            throw new com.fisco.app.exception.BusinessException("票据不存在: " + billId);
        }

        return contractService.getEndorsementHistoryFromChain(billId);
    }

    /**
     * 验证票据背书历史的数据完整性
     * 对比数据库和区块链上的背书记录
     *
     * @param billId 票据ID
     * @return 验证结果，包含是否匹配以及不匹配的详细信息
     */
    public java.util.Map<String, Object> validateEndorsementHistory(@NonNull String billId) {
        log.info("验证背书历史完整性: billId={}", billId);

        // 获取数据库中的背书历史
        java.util.List<Endorsement> dbEndorsements = endorsementRepository.findByBillIdOrderByEndorsementDateAsc(billId);

        // 获取区块链上的背书历史
        java.util.List<java.util.Map<String, Object>> chainEndorsements = contractService.getEndorsementHistoryFromChain(billId);

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("billId", billId);
        result.put("dbCount", dbEndorsements.size());
        result.put("chainCount", chainEndorsements.size());
        result.put("isValid", dbEndorsements.size() == chainEndorsements.size());

        if (dbEndorsements.size() != chainEndorsements.size()) {
            result.put("message", "背书记录数量不一致");
        } else {
            result.put("message", "背书记录数量一致");

            // 验证每条记录
            java.util.List<java.util.Map<String, String>> mismatches = new java.util.ArrayList<>();
            for (int i = 0; i < dbEndorsements.size(); i++) {
                Endorsement dbRecord = dbEndorsements.get(i);
                java.util.Map<String, Object> chainRecord = chainEndorsements.get(i);

                // 验证关键字段
                if (!dbRecord.getEndorserAddress().equals(chainRecord.get("endorser")) ||
                    !dbRecord.getEndorseeAddress().equals(chainRecord.get("endorsee")) ||
                    !dbRecord.getEndorsementType().name().equals(chainRecord.get("endorsementType"))) {

                    java.util.Map<String, String> mismatch = new java.util.HashMap<>();
                    mismatch.put("sequence", String.valueOf(i + 1));
                    mismatch.put("dbRecord", dbRecord.toString());
                    mismatch.put("chainRecord", chainRecord.toString());
                    mismatches.add(mismatch);
                }
            }

            if (mismatches.isEmpty()) {
                result.put("isValid", true);
                result.put("message", "所有背书记录验证通过");
            } else {
                result.put("isValid", false);
                result.put("message", "发现 " + mismatches.size() + " 条不匹配记录");
                result.put("mismatches", mismatches);
            }
        }

        return result;
    }

    /**
     * 票据到期处理
     * 自动处理到期票据，计算利息并更新状态
     */
    @Transactional(rollbackFor = Exception.class)
    public RepayBillResponse handleBillMaturity(@NonNull String billId) {
        log.info("处理票据到期: billId={}", billId);

        // 步骤1: 验证票据
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new com.fisco.app.exception.BusinessException("票据不存在: " + billId));

        // 验证票据状态 - 只有已贴现的票据需要到期处理
        if (bill.getBillStatus() != Bill.BillStatus.DISCOUNTED) {
            throw new com.fisco.app.exception.BusinessException.InvalidStatusException(
                "只能处理已贴现票据的到期");
        }

        // 获取贴现记录
        java.util.Optional<DiscountRecord> discountRecordOpt =
            discountRecordRepository.findFirstByBillIdAndStatusOrderByDiscountDateDesc(
                billId, DiscountRecord.DiscountStatus.ACTIVE);

        if (!discountRecordOpt.isPresent()) {
            throw new com.fisco.app.exception.BusinessException("未找到有效的贴现记录");
        }

        DiscountRecord discountRecord = discountRecordOpt.get();

        // 步骤2: 调用区块链合约支付票据
        try {
            String txHash = contractService.payBillOnChain(billId);

            // 步骤3: 计算利息
            java.math.BigDecimal interestAmount = calculateInterest(
                discountRecord.getDiscountAmount(),
                discountRecord.getDiscountRate(),
                discountRecord.getDiscountDate(),
                bill.getDueDate()
            );

            // 步骤4: 创建还款记录
            RepaymentRecord repaymentRecord = new RepaymentRecord();
            repaymentRecord.setId(billId);
            repaymentRecord.setPayerAddress(bill.getDraweeAddress()); // 承兑人付款
            repaymentRecord.setFinancialInstitutionAddress(discountRecord.getFinancialInstitutionAddress());
            repaymentRecord.setBillAmount(bill.getFaceValue());
            repaymentRecord.setDiscountAmount(discountRecord.getDiscountAmount());
            repaymentRecord.setPaymentAmount(bill.getFaceValue()); // 按票面金额还款
            repaymentRecord.setPaymentType(RepaymentRecord.PaymentType.MATURITY_PAYMENT);
            repaymentRecord.setPrincipalAmount(discountRecord.getDiscountAmount());
            repaymentRecord.setInterestAmount(interestAmount);
            repaymentRecord.setDueDate(bill.getDueDate());
            repaymentRecord.setPaymentDate(java.time.LocalDateTime.now());
            repaymentRecord.setTxHash(txHash);
            repaymentRecord.setStatus(RepaymentRecord.PaymentStatus.COMPLETED);

            RepaymentRecord savedRecord = repaymentRecordRepository.save(repaymentRecord);

            // 步骤5: 更新票据状态
            bill.setBillStatus(Bill.BillStatus.PAID);
            bill.setBlockchainTxHash(txHash);
            billRepository.save(bill);

            // 步骤6: 更新贴现记录状态
            discountRecord.setStatus(DiscountRecord.DiscountStatus.MATURED);
            discountRecordRepository.save(discountRecord);

            // 步骤7: 构建响应
            RepayBillResponse response = new RepayBillResponse();
            response.setId(savedRecord.getId());
            response.setId(savedRecord.getId());
            response.setPayerAddress(savedRecord.getPayerAddress());
            response.setFinancialInstitutionAddress(savedRecord.getFinancialInstitutionAddress());
            response.setBillAmount(savedRecord.getBillAmount());
            response.setDiscountAmount(savedRecord.getDiscountAmount());
            response.setPaymentAmount(savedRecord.getPaymentAmount());
            response.setPaymentType(savedRecord.getPaymentType());
            response.setPrincipalAmount(savedRecord.getPrincipalAmount());
            response.setInterestAmount(savedRecord.getInterestAmount());
            response.setPenaltyInterestAmount(savedRecord.getPenaltyInterestAmount());
            response.setOverdueDays(savedRecord.getOverdueDays());
            response.setPaymentDate(savedRecord.getPaymentDate());
            response.setDueDate(savedRecord.getDueDate());
            response.setStatus(savedRecord.getStatus());
            response.setTxHash(savedRecord.getTxHash());
            response.setRemark("票据到期自动还款");

            log.info("票据到期处理成功: billId={}, paymentAmount={}, interest={}",
                billId, bill.getFaceValue(), interestAmount);

            return response;

        } catch (BlockchainIntegrationException e) {
            log.error("区块链调用失败，回滚数据库事务: billId={}, error={}",
                billId, e.getMessage(), e);
            throw new com.fisco.app.exception.BusinessException(
                500, "区块链操作失败: " + e.getMessage(), e);
        }
    }

    /**
     * 票据还款
     * 支持主动还款（提前或逾期）
     */
    @Transactional(rollbackFor = Exception.class)
    public RepayBillResponse repayBill(@NonNull String billId, RepayBillRequest request, String payerAddress) {
        log.info("票据还款: billId={}, payer={}, amount={}, type={}",
            billId, payerAddress, request.getPaymentAmount(), request.getPaymentType());

        // 步骤1: 验证票据
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new com.fisco.app.exception.BusinessException("票据不存在: " + billId));

        // 验证票据状态 - 只有已贴现的票据可以还款
        if (bill.getBillStatus() != Bill.BillStatus.DISCOUNTED) {
            throw new com.fisco.app.exception.BusinessException.InvalidStatusException(
                "只能对已贴现的票据进行还款");
        }

        // 验证还款人是否为承兑人
        if (!bill.getDraweeAddress().equals(payerAddress)) {
            throw new com.fisco.app.exception.BusinessException("只有票据承兑人可以还款");
        }

        // 获取贴现记录
        java.util.Optional<DiscountRecord> discountRecordOpt =
            discountRecordRepository.findFirstByBillIdAndStatusOrderByDiscountDateDesc(
                billId, DiscountRecord.DiscountStatus.ACTIVE);

        if (!discountRecordOpt.isPresent()) {
            throw new com.fisco.app.exception.BusinessException("未找到有效的贴现记录");
        }

        DiscountRecord discountRecord = discountRecordOpt.get();

        // 步骤2: 调用区块链合约支付票据
        try {
            String txHash = contractService.payBillOnChain(billId);

            // 步骤3: 计算逾期天数和利息
            java.math.BigDecimal calculatedInterest = request.getInterestAmount();
            Integer overdueDays = 0;
            java.math.BigDecimal penaltyInterest = request.getPenaltyInterestAmount();

            LocalDateTime now = java.time.LocalDateTime.now();
            if (now.isAfter(bill.getDueDate())) {
                overdueDays = (int) java.time.temporal.ChronoUnit.DAYS.between(bill.getDueDate(), now);

                // 如果未提供逾期利息，自动计算
                if (penaltyInterest == null && overdueDays > 0) {
                    // 逾期利息 = 逾期天数 × 日罚息率 × 贴现金额
                    // 假设日罚息率为0.05%
                    penaltyInterest = discountRecord.getDiscountAmount()
                        .multiply(java.math.BigDecimal.valueOf(overdueDays))
                        .multiply(java.math.BigDecimal.valueOf(0.0005))
                        .setScale(2, java.math.RoundingMode.HALF_UP);
                }
            }

            // 步骤4: 创建还款记录
            RepaymentRecord repaymentRecord = new RepaymentRecord();
            repaymentRecord.setId(billId);
            repaymentRecord.setPayerAddress(payerAddress);
            repaymentRecord.setFinancialInstitutionAddress(discountRecord.getFinancialInstitutionAddress());
            repaymentRecord.setBillAmount(bill.getFaceValue());
            repaymentRecord.setDiscountAmount(discountRecord.getDiscountAmount());
            repaymentRecord.setPaymentAmount(request.getPaymentAmount());
            repaymentRecord.setPaymentType(request.getPaymentType());
            repaymentRecord.setPrincipalAmount(discountRecord.getDiscountAmount());
            repaymentRecord.setInterestAmount(calculatedInterest);
            repaymentRecord.setPenaltyInterestAmount(penaltyInterest);
            repaymentRecord.setOverdueDays(overdueDays);
            repaymentRecord.setDueDate(bill.getDueDate());
            repaymentRecord.setPaymentDate(now);
            repaymentRecord.setTxHash(txHash);
            repaymentRecord.setStatus(RepaymentRecord.PaymentStatus.COMPLETED);
            repaymentRecord.setRemark(request.getRemark());

            RepaymentRecord savedRecord = repaymentRecordRepository.save(repaymentRecord);

            // 步骤5: 更新票据状态
            bill.setBillStatus(Bill.BillStatus.PAID);
            bill.setBlockchainTxHash(txHash);
            billRepository.save(bill);

            // 步骤6: 更新贴现记录状态
            if (request.getPaymentType() == RepaymentRecord.PaymentType.FULL_PAYMENT) {
                discountRecord.setStatus(DiscountRecord.DiscountStatus.REPAID);
            }
            discountRecordRepository.save(discountRecord);

            // 步骤7: 构建响应
            RepayBillResponse response = new RepayBillResponse();
            response.setId(savedRecord.getId());
            response.setId(savedRecord.getId());
            response.setPayerAddress(savedRecord.getPayerAddress());
            response.setFinancialInstitutionAddress(savedRecord.getFinancialInstitutionAddress());
            response.setBillAmount(savedRecord.getBillAmount());
            response.setDiscountAmount(savedRecord.getDiscountAmount());
            response.setPaymentAmount(savedRecord.getPaymentAmount());
            response.setPaymentType(savedRecord.getPaymentType());
            response.setPrincipalAmount(savedRecord.getPrincipalAmount());
            response.setInterestAmount(savedRecord.getInterestAmount());
            response.setPenaltyInterestAmount(savedRecord.getPenaltyInterestAmount());
            response.setOverdueDays(savedRecord.getOverdueDays());
            response.setPaymentDate(savedRecord.getPaymentDate());
            response.setDueDate(savedRecord.getDueDate());
            response.setStatus(savedRecord.getStatus());
            response.setTxHash(savedRecord.getTxHash());
            response.setRemark(savedRecord.getRemark());

            log.info("票据还款成功: billId={}, payer={}, amount={}, type={}",
                billId, payerAddress, request.getPaymentAmount(), request.getPaymentType());

            return response;

        } catch (BlockchainIntegrationException e) {
            log.error("区块链调用失败，回滚数据库事务: billId={}, error={}",
                billId, e.getMessage(), e);
            throw new com.fisco.app.exception.BusinessException(
                500, "区块链操作失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取票据的还款记录
     */
    public java.util.List<RepaymentRecord> getRepaymentHistory(@NonNull String billId) {
        log.info("查询票据还款历史: billId={}", billId);

        // 验证票据是否存在
        if (!billRepository.findById(billId).isPresent()) {
            throw new com.fisco.app.exception.BusinessException("票据不存在: " + billId);
        }

        return repaymentRecordRepository.findByBillIdOrderByPaymentDateDesc(billId);
    }

    /**
     * 计算利息
     * @param principal 本金金额
     * @param rate 年化利率（百分比，如 5.5 表示 5.5%）
     * @param startDate 起始日期
     * @param endDate 结束日期
     * @return 利息金额
     */
    private java.math.BigDecimal calculateInterest(java.math.BigDecimal principal,
                                                   java.math.BigDecimal rate,
                                                   java.time.LocalDateTime startDate,
                                                   java.time.LocalDateTime endDate) {
        // 计算天数
        long days = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        if (days <= 0) {
            return java.math.BigDecimal.ZERO;
        }

        // 利息 = 本金 × 年利率 × 天数 / 365
        return principal
            .multiply(rate)
            .multiply(java.math.BigDecimal.valueOf(days))
            .divide(java.math.BigDecimal.valueOf(36500), 2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * 作废票据
     */
    @Transactional(rollbackFor = Exception.class)
    public Bill cancelBill(@NonNull String billId, com.fisco.app.dto.bill.CancelBillRequest request, String operatorAddress) {
        log.info("==================== 票据作废开始 ====================");
        log.info("票据ID: {}, 操作人: {}", billId, operatorAddress);
        log.info("作废原因: {}, 类型: {}", request.getCancelReason(), request.getCancelType());

        long startTime = System.currentTimeMillis();

        try {
            log.debug("查询票据信息: billId={}", billId);
            Bill bill = billRepository.findById(billId)
                    .orElseThrow(() -> new com.fisco.app.exception.BusinessException("票据不存在: " + billId));
            log.debug("✓ 票据查询成功: currentStatus={}", bill.getBillStatus());

            // 验证票据状态
            log.debug("验证票据状态");
            Bill.BillStatus currentStatus = bill.getBillStatus();
            if (currentStatus == Bill.BillStatus.PAID ||
                currentStatus == Bill.BillStatus.SETTLED ||
                currentStatus == Bill.BillStatus.CANCELLED) {
                log.error("票据状态错误，已结算或已作废的票据不能作废: currentStatus={}", currentStatus);
                throw new com.fisco.app.exception.BusinessException("已结算或已作废的票据不能再次作废");
            }
            log.debug("✓ 状态验证通过");

            // 验证操作权限（票据持有人或管理员）
            log.debug("验证操作权限");
            if (!bill.getCurrentHolderAddress().equals(operatorAddress)) {
                log.error("权限不足，只有当前持票人可以作废票据");
                throw new com.fisco.app.exception.BusinessException("只有当前持票人可以作废票据");
            }
            log.debug("✓ 权限验证通过");

            // 更新票据状态
            log.debug("更新票据状态: {} -> CANCELLED", currentStatus);
            bill.setBillStatus(Bill.BillStatus.CANCELLED);
            bill.setRemarks(request.getCancelReason() + " | " + request.getCancelType() +
                          (request.getReferenceNo() != null ? " | " + request.getReferenceNo() : ""));
            bill.setUpdatedBy(operatorAddress);
            billRepository.save(bill);
            log.info("✓ 票据已作废");

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 票据作废完成: billId={}, 耗时={}ms", billId, duration);
            log.info("==================== 票据作废结束 ====================");
            return bill;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 票据作废失败: billId={}, 耗时={}ms, error={}", billId, duration, e.getMessage(), e);
            log.info("==================== 票据作废失败（结束） ====================");
            throw e;
        }
    }

    /**
     * 冻结票据
     */
    @Transactional(rollbackFor = Exception.class)
    public Bill freezeBill(@NonNull String billId, com.fisco.app.dto.bill.FreezeBillRequest request, String operatorAddress) {
        log.info("==================== 票据冻结开始 ====================");
        log.info("票据ID: {}, 操作人: {}", billId, operatorAddress);
        log.info("冻结原因: {}", request.getFreezeReason());

        long startTime = System.currentTimeMillis();

        try {
            log.debug("查询票据信息: billId={}", billId);
            Bill bill = billRepository.findById(billId)
                    .orElseThrow(() -> new com.fisco.app.exception.BusinessException("票据不存在: " + billId));

            // 验证票据状态
            log.debug("验证票据状态");
            if (bill.getBillStatus() == Bill.BillStatus.FROZEN) {
                throw new com.fisco.app.exception.BusinessException("票据已被冻结");
            }
            if (bill.getBillStatus() == Bill.BillStatus.CANCELLED ||
                bill.getBillStatus() == Bill.BillStatus.PAID ||
                bill.getBillStatus() == Bill.BillStatus.SETTLED) {
                throw new com.fisco.app.exception.BusinessException("已结算或已作废的票据不能冻结");
            }
            log.debug("✓ 状态验证通过");

            // 验证操作权限
            log.debug("验证操作权限");
            if (!bill.getCurrentHolderAddress().equals(operatorAddress)) {
                throw new com.fisco.app.exception.BusinessException("只有当前持票人可以冻结票据");
            }
            log.debug("✓ 权限验证通过");

            // 更新票据状态为FROZEN
            log.debug("更新票据状态: {} -> FROZEN", bill.getBillStatus());
            Bill.BillStatus previousStatus = bill.getBillStatus();
            bill.setBillStatus(Bill.BillStatus.FROZEN);
            bill.setRemarks("冻结原因: " + request.getFreezeReason() +
                          (request.getReferenceNo() != null ? " | 凭证号: " + request.getReferenceNo() : "") +
                          (request.getEvidence() != null ? " | 证据: " + request.getEvidence() : ""));
            bill.setUpdatedBy(operatorAddress);
            billRepository.save(bill);
            log.info("✓ 票据已冻结: previousStatus={}", previousStatus);

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 票据冻结完成: billId={}, 耗时={}ms", billId, duration);
            log.info("==================== 票据冻结结束 ====================");
            return bill;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 票据冻结失败: billId={}, 耗时={}ms, error={}", billId, duration, e.getMessage(), e);
            log.info("==================== 票据冻结失败（结束） ====================");
            throw e;
        }
    }

    /**
     * 解冻票据
     */
    @Transactional(rollbackFor = Exception.class)
    public Bill unfreezeBill(@NonNull String billId, com.fisco.app.dto.bill.UnfreezeBillRequest request, String operatorAddress) {
        log.info("==================== 票据解冻开始 ====================");
        log.info("票据ID: {}, 操作人: {}", billId, operatorAddress);
        log.info("解冻原因: {}", request.getUnfreezeReason());

        long startTime = System.currentTimeMillis();

        try {
            log.debug("查询票据信息: billId={}", billId);
            Bill bill = billRepository.findById(billId)
                    .orElseThrow(() -> new com.fisco.app.exception.BusinessException("票据不存在: " + billId));

            // 验证票据状态
            log.debug("验证票据状态");
            if (bill.getBillStatus() != Bill.BillStatus.FROZEN) {
                throw new com.fisco.app.exception.BusinessException("只能解冻已冻结的票据");
            }
            log.debug("✓ 状态验证通过");

            // 验证操作权限
            log.debug("验证操作权限");
            if (!bill.getCurrentHolderAddress().equals(operatorAddress)) {
                throw new com.fisco.app.exception.BusinessException("只有当前持票人可以解冻票据");
            }
            log.debug("✓ 权限验证通过");

            // 恢复票据状态（根据备注信息推断之前的状态，或者默认为NORMAL）
            log.debug("恢复票据状态: FROZEN -> NORMAL");
            bill.setBillStatus(Bill.BillStatus.ISSUED);
            bill.setRemarks(bill.getRemarks() + "\n解冻原因: " + request.getUnfreezeReason() +
                          (request.getReferenceNo() != null ? " | 凭证号: " + request.getReferenceNo() : ""));
            bill.setUpdatedBy(operatorAddress);
            billRepository.save(bill);
            log.info("✓ 票据已解冻");

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 票据解冻完成: billId={}, 耗时={}ms", billId, duration);
            log.info("==================== 票据解冻结束 ====================");
            return bill;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 票据解冻失败: billId={}, 耗时={}ms, error={}", billId, duration, e.getMessage(), e);
            log.info("==================== 票据解冻失败（结束） ====================");
            throw e;
        }
    }

    /**
     * 查询已过期票据
     */
    public java.util.List<Bill> getExpiredBills(String enterpriseId) {
        log.info("查询过期票据: enterpriseId={}", enterpriseId);

        // 查询条件：dueDate < 当前日期 且 status != PAID 且 status != SETTLED 且 status != CANCELLED
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.util.List<Bill> expiredBills;

        if (enterpriseId != null && !enterpriseId.isEmpty()) {
            // 查询指定企业的过期票据
            expiredBills = billRepository.findExpiredBillsByEnterprise(enterpriseId, now);
        } else {
            // 查询所有过期票据
            expiredBills = billRepository.findAll().stream()
                    .filter(bill -> bill.getDueDate().isBefore(now) &&
                                  bill.getBillStatus() != Bill.BillStatus.PAID &&
                                  bill.getBillStatus() != Bill.BillStatus.SETTLED &&
                                  bill.getBillStatus() != Bill.BillStatus.CANCELLED)
                    .collect(java.util.stream.Collectors.toList());
        }

        log.info("查询到 {} 张过期票据", expiredBills.size());
        return expiredBills;
    }

    /**
     * 查询拒付票据
     */
    public java.util.List<Bill> getDishonoredBills(String acceptorAddress, java.time.LocalDateTime startDate, java.time.LocalDateTime endDate) {
        log.info("查询拒付票据: acceptorAddress={}, startDate={}, endDate={}", acceptorAddress, startDate, endDate);

        java.util.List<Bill> dishonoredBills = billRepository.findAll().stream()
                .filter(bill -> bill.getDishonored() != null && bill.getDishonored())
                .filter(bill -> {
                    // 按承兑人筛选
                    if (acceptorAddress != null && !acceptorAddress.isEmpty()) {
                        return bill.getDraweeAddress().equals(acceptorAddress);
                    }
                    return true;
                })
                .filter(bill -> {
                    // 按时间范围筛选
                    if (startDate != null) {
                        return bill.getDishonoredDate() != null &&
                               !bill.getDishonoredDate().isBefore(startDate);
                    }
                    if (endDate != null) {
                        return bill.getDishonoredDate() != null &&
                               !bill.getDishonoredDate().isAfter(endDate);
                    }
                    return true;
                })
                .collect(java.util.stream.Collectors.toList());

        log.info("查询到 {} 张拒付票据", dishonoredBills.size());
        return dishonoredBills;
    }

    // ==================== 票据融资管理 ====================

    private final com.fisco.app.repository.bill.BillFinanceApplicationRepository financeApplicationRepository;

    /**
     * 票据融资申请
     */
    @Transactional(rollbackFor = Exception.class)
    public com.fisco.app.dto.bill.FinanceApplicationResponse applyFinance(@NonNull String billId,
                                                                       com.fisco.app.dto.bill.FinanceBillRequest request,
                                                                       @org.springframework.lang.Nullable String applicantAddress) {
        log.info("==================== 票据融资申请开始 ====================");
        log.info("票据ID: {}, 申请人: {}, 金融机构: {}, 金额: {}",
                 billId, applicantAddress, request.getFinancialInstitutionId(), request.getFinanceAmount());

        // 验证必要参数
        if (applicantAddress == null || applicantAddress.trim().isEmpty()) {
            throw new com.fisco.app.exception.BusinessException("申请人地址不能为空");
        }

        long startTime = System.currentTimeMillis();

        try {
            // 步骤1: 验证票据
            log.debug("验证票据信息: billId={}", billId);
            Bill bill = billRepository.findById(billId)
                    .orElseThrow(() -> new com.fisco.app.exception.BusinessException("票据不存在: " + billId));
            log.debug("✓ 票据查询成功: currentStatus={}", bill.getBillStatus());

            // 验证票据状态 - 只能融资正常或已背书的票据
            log.debug("验证票据状态");
            if (bill.getBillStatus() != Bill.BillStatus.ISSUED &&
                bill.getBillStatus() != Bill.BillStatus.ENDORSED &&
                bill.getBillStatus() != Bill.BillStatus.ISSUED) {
                log.error("票据状态错误，只能融资正常或已背书的票据: currentStatus={}", bill.getBillStatus());
                throw new com.fisco.app.exception.BusinessException("只能融资正常或已背书的票据");
            }
            log.debug("✓ 状态验证通过");

            // 验证申请人是否为当前持票人
            if (!bill.getCurrentHolderAddress().equals(applicantAddress)) {
                log.error("权限不足，只有当前持票人可以申请融资");
                throw new com.fisco.app.exception.BusinessException("只有当前持票人可以申请融资");
            }
            log.debug("✓ 持票人验证通过");

            // 验证金融机构是否存在
            if (!enterpriseService.isEnterpriseValid(request.getFinancialInstitutionId())) {
                log.error("金融机构不存在或未激活");
                throw new com.fisco.app.exception.BusinessException("金融机构不存在或未激活");
            }
            log.debug("✓ 金融机构验证通过");

            // 验证融资金额
            if (request.getFinanceAmount().compareTo(bill.getFaceValue()) > 0) {
                log.error("融资金额不能超过票面金额");
                throw new com.fisco.app.exception.BusinessException("融资金额不能超过票面金额");
            }
            log.debug("✓ 金额验证通过");

            // 步骤2: 创建融资申请
            log.debug("创建融资申请");
            com.fisco.app.entity.bill.BillFinanceApplication application = new com.fisco.app.entity.bill.BillFinanceApplication();
            application.setBillId(billId);
            application.setApplicantId(applicantAddress);
            application.setFinancialInstitutionId(request.getFinancialInstitutionId());
            application.setFinanceAmount(request.getFinanceAmount());
            application.setFinanceRate(request.getFinanceRate());
            application.setFinancePeriod(request.getFinancePeriod());
            application.setPledgeAgreement(request.getPledgeAgreement());
            application.setStatus("PENDING");
            application.setApplyDate(LocalDateTime.now());

            com.fisco.app.entity.bill.BillFinanceApplication savedApplication = financeApplicationRepository.save(application);
            log.info("✓ 融资申请已创建: applicationId={}", savedApplication.getId());

            // 步骤3: 构建响应
            com.fisco.app.dto.bill.FinanceApplicationResponse response = buildFinanceResponse(savedApplication, bill);
            log.info("✓✓✓ 票据融资申请创建成功");

            long duration = System.currentTimeMillis() - startTime;
            log.info("==================== 票据融资申请结束 ====================, 耗时={}ms", duration);
            return response;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 票据融资申请失败: billId={}, 耗时={}ms, error={}",
                     billId, duration, e.getMessage(), e);
            log.info("==================== 票据融资申请失败（结束） ====================", duration);
            throw e;
        }
    }

    /**
     * 审核票据融资申请
     */
    @Transactional(rollbackFor = Exception.class)
    @SuppressWarnings("null")
    public com.fisco.app.dto.bill.FinanceApplicationResponse approveFinance(com.fisco.app.dto.bill.ApproveFinanceRequest request,
                                                                           String reviewerAddress) {
        log.info("==================== 审核票据融资开始 ====================");
        log.info("申请ID: {}, 审核人: {}, 结果: {}",
                 request.getApplicationId(), reviewerAddress, request.getApprovalResult());

        // 验证必要参数
        if (reviewerAddress == null || reviewerAddress.trim().isEmpty()) {
            throw new com.fisco.app.exception.BusinessException("审核人地址不能为空");
        }

        long startTime = System.currentTimeMillis();

        try {
            // 步骤1: 查询融资申请
            String applicationId = request.getApplicationId();
            assert applicationId != null : "申请ID不能为空";
            log.debug("查询融资申请: applicationId={}", applicationId);
            com.fisco.app.entity.bill.BillFinanceApplication application = financeApplicationRepository.findById(applicationId)
                    .orElseThrow(() -> new com.fisco.app.exception.BusinessException("融资申请不存在: " + applicationId));

            // 验证申请状态
            if (!"PENDING".equals(application.getStatus())) {
                throw new com.fisco.app.exception.BusinessException("只能审核待审核状态的申请");
            }

            // 步骤2: 处理审核结果
            log.debug("处理审核结果: {}", request.getApprovalResult());
            if (request.getApprovalResult() == com.fisco.app.dto.bill.ApproveFinanceRequest.ApprovalResult.APPROVED) {
                // 审核通过
                log.debug("审核通过");
                application.setStatus("APPROVED");
                application.setApprovedAmount(request.getApprovedAmount() != null ? request.getApprovedAmount() : application.getFinanceAmount());
                application.setApprovedRate(request.getApprovedRate() != null ? request.getApprovedRate() : application.getFinanceRate());
                application.setApprovalComments(request.getApprovalComments());
                application.setApproveDate(LocalDateTime.now());

                // 注：实际放款会在后续的单独接口中完成
                log.info("✓ 融资申请审核通过: applicationId={}, 批准金额={}",
                        application.getId(), application.getApprovedAmount());

            } else {
                // 审核拒绝
                log.debug("审核拒绝");
                application.setStatus("REJECTED");
                application.setRejectionReason(request.getApprovalComments() != null ? request.getApprovalComments() : "未提供原因");
                application.setApproveDate(LocalDateTime.now());
                application.setApprovalComments(request.getApprovalComments());

                log.info("✓ 融资申请已拒绝: applicationId={}", application.getId());
            }

            financeApplicationRepository.save(application);

            // 步骤3: 构建响应
            String billId = application.getBillId();
            assert billId != null : "票据ID不能为空";
            Bill bill = billRepository.findById(billId)
                    .orElseThrow(() -> new com.fisco.app.exception.BusinessException("票据不存在"));
            com.fisco.app.dto.bill.FinanceApplicationResponse response = buildFinanceResponse(application, bill);

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 审核票据融资完成: applicationId={}, 耗时={}ms", request.getApplicationId(), duration);
            log.info("==================== 审核票据融资结束 ====================, 耗时={}ms", duration);
            return response;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 审核票据融资失败: applicationId={}, 耗时={}ms, error={}",
                     request.getApplicationId(), duration, e.getMessage(), e);
            log.info("==================== 审核票据融资失败（结束） ====================", duration);
            throw e;
        }
    }

    /**
     * 查询待审核融资申请
     */
    public java.util.List<com.fisco.app.dto.bill.FinanceApplicationResponse> getPendingFinanceApplications(String institutionId) {
        log.info("查询待审核融资申请: institutionId={}", institutionId);

        java.util.List<com.fisco.app.entity.bill.BillFinanceApplication> applications;

        if (institutionId != null && !institutionId.isEmpty()) {
            // 查询特定金融机构的待审核申请
            applications = financeApplicationRepository.findPendingApplicationsByInstitution(institutionId);
        } else {
            // 查询所有待审核申请
            applications = financeApplicationRepository.findPendingApplications();
        }

        log.info("查询到 {} 条待审核申请", applications.size());

        // 转换为响应对象
        return applications.stream()
                .map(app -> {
                    String billId = app.getBillId();
                    Bill bill = billId != null ? billRepository.findById(billId).orElse(null) : null;
                    return buildFinanceResponse(app, bill);
                })
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 票据融资还款
     */
    @Transactional(rollbackFor = Exception.class)
    @SuppressWarnings("null")
    public com.fisco.app.dto.bill.FinanceApplicationResponse repayFinance(@NonNull String applicationId,
                                                                     com.fisco.app.dto.bill.RepayFinanceRequest request,
                                                                     @org.springframework.lang.Nullable String payerAddress) {
        log.info("==================== 票据融资还款开始 ====================");
        log.info("申请ID: {}, 还款人: {}, 金额: {}", applicationId, payerAddress, request.getRepayAmount());

        // 验证必要参数
        if (payerAddress == null || payerAddress.trim().isEmpty()) {
            throw new com.fisco.app.exception.BusinessException("还款人地址不能为空");
        }

        long startTime = System.currentTimeMillis();

        try {
            // 步骤1: 查询融资申请
            log.debug("查询融资申请: applicationId={}", applicationId);
            com.fisco.app.entity.bill.BillFinanceApplication application = financeApplicationRepository.findById(applicationId)
                    .orElseThrow(() -> new com.fisco.app.exception.BusinessException("融资申请不存在: " + applicationId));

            // 验证申请状态
            if (!"ACTIVE".equals(application.getStatus()) && !"APPROVED".equals(application.getStatus())) {
                throw new com.fisco.app.exception.BusinessException("只能还款已批准或已放款的融资申请");
            }

            // 步骤2: 验证还款人（应该是申请人或承兑人）
            String billId = application.getBillId();
            assert billId != null : "票据ID不能为空";
            Bill bill = billRepository.findById(billId)
                    .orElseThrow(() -> new com.fisco.app.exception.BusinessException("票据不存在"));

            // 简化验证：允许申请人或票据持票人还款
            if (!application.getApplicantId().equals(payerAddress) &&
                !bill.getCurrentHolderAddress().equals(payerAddress)) {
                log.error("还款人权限不足");
                throw new com.fisco.app.exception.BusinessException("只有申请人或票据持票人可以还款");
            }

            // 步骤3: 处理还款
            log.debug("处理还款: amount={}, type={}", request.getRepayAmount(), request.getRepayType());

            // 计算应付金额（本金 + 利息）
            java.math.BigDecimal repayAmount = request.getRepayAmount();
            if (request.getRepayType() == com.fisco.app.dto.bill.RepayFinanceRequest.RepayType.FULL) {
                // 全额还款：本金 + 全部利息
                long days = java.time.temporal.ChronoUnit.DAYS.between(
                    application.getDisbursementDate() != null ? application.getDisbursementDate() : application.getApplyDate(),
                    LocalDateTime.now()
                );
                if (days > 0) {
                    java.math.BigDecimal interest = application.getFinanceAmount()
                            .multiply(application.getFinanceRate())
                            .multiply(java.math.BigDecimal.valueOf(days))
                            .divide(java.math.BigDecimal.valueOf(36500), 2, java.math.RoundingMode.HALF_UP);
                    repayAmount = application.getFinanceAmount().add(interest);
                    log.info("全额还款计算：本金={}, 利息={}, 天数={}, 总还款额={}",
                            application.getFinanceAmount(), interest, days, repayAmount);
                }
            }

            // 更新申请状态
            application.setStatus("REPAID");
            application.setActualRepaymentAmount(repayAmount);
            application.setRepaymentDate(LocalDateTime.now());
            application.setUpdatedBy(payerAddress);

            // 更新票据状态
            bill.setBillStatus(Bill.BillStatus.PAID);

            financeApplicationRepository.save(application);
            billRepository.save(bill);

            log.info("✓ 票据融资还款完成: applicationId={}, 还款金额={}", applicationId, request.getRepayAmount());

            // 构建响应
            com.fisco.app.dto.bill.FinanceApplicationResponse response = buildFinanceResponse(application, bill);

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 票据融资还款完成: applicationId={}, 耗时={}ms", applicationId, duration);
            log.info("==================== 票据融资还款结束 ====================, 耗时={}ms", duration);
            return response;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 票据融资还款失败: applicationId={}, 耗时={}ms, error={}",
                     applicationId, duration, e.getMessage(), e);
            log.info("==================== 票据融资还款失败（结束） ====================", duration);
            throw e;
        }
    }

    /**
     * 构建融资申请响应对象
     */
    private com.fisco.app.dto.bill.FinanceApplicationResponse buildFinanceResponse(
            com.fisco.app.entity.bill.BillFinanceApplication application,
            @org.springframework.lang.Nullable Bill bill) {
        com.fisco.app.dto.bill.FinanceApplicationResponse response = new com.fisco.app.dto.bill.FinanceApplicationResponse();
        response.setId(application.getId());
        response.setBillId(application.getBillId());

        if (bill != null) {
            response.setBillNo(bill.getBillNo());
            response.setBillFaceValue(bill.getFaceValue());
        }

        response.setApplicantId(application.getApplicantId());
        response.setFinancialInstitutionId(application.getFinancialInstitutionId());
        response.setFinanceAmount(application.getFinanceAmount());
        response.setFinanceRate(application.getFinanceRate());
        response.setFinancePeriod(application.getFinancePeriod());
        response.setApprovedAmount(application.getApprovedAmount());
        response.setApprovedRate(application.getApprovedRate());
        response.setActualAmount(application.getActualAmount());
        response.setStatus(application.getStatus());
        response.setApplyDate(application.getApplyDate());
        response.setApproveDate(application.getApproveDate());
        response.setApprovalComments(application.getApprovalComments());
        response.setRejectionReason(application.getRejectionReason());
        response.setDisbursementDate(application.getDisbursementDate());
        response.setRepaymentDate(application.getRepaymentDate());
        response.setTxHash(application.getTxHash());

        return response;
    }

    /**
     * 获取票据信息
     */
    public Bill getBill(@NonNull String billId) {
        return billRepository.findById(billId)
                .orElseThrow(() -> new com.fisco.app.exception.BusinessException("票据不存在: " + billId));
    }

    // ==================== 新增功能方法 ====================

    /**
     * 获取票据统计数据
     */
    public BillStatisticsDTO getBillStatistics(LocalDateTime startTime, LocalDateTime endTime,
                                               String enterpriseAddress, String dimension) {
        log.info("获取票据统计数据: startTime={}, endTime={}, enterprise={}, dimension={}",
                 startTime, endTime, enterpriseAddress, dimension);

        BillStatisticsDTO statistics = new BillStatisticsDTO();
        statistics.setStartTime(startTime);
        statistics.setEndTime(endTime);
        statistics.setGeneratedAt(LocalDateTime.now());

        // 查询符合条件的票据列表
        java.util.List<Bill> bills = queryBillsByCondition(startTime, endTime, enterpriseAddress);
        log.debug("查询到票据数量: {}", bills.size());

        // 计算基础统计
        calculateBasicStatistics(statistics, bills);

        // 计算状态分布
        calculateStatusDistribution(statistics, bills);

        // 计算类型分布
        calculateTypeDistribution(statistics, bills);

        // 计算融资统计
        calculateFinancingStatistics(statistics, bills);

        // 计算风险统计
        calculateRiskStatistics(statistics, bills);

        // 计算持票人统计
        calculateHolderStatistics(statistics, bills);

        return statistics;
    }

    /**
     * 按条件查询票据
     */
    private java.util.List<Bill> queryBillsByCondition(LocalDateTime startTime, LocalDateTime endTime,
                                                        String enterpriseAddress) {
        if (enterpriseAddress != null && !enterpriseAddress.isEmpty()) {
            // 按持票人查询
            if (startTime != null && endTime != null) {
                return billRepository.findByCurrentHolderAddressAndCreatedAtBetween(
                    enterpriseAddress, startTime, endTime);
            } else {
                return billRepository.findByCurrentHolderAddress(enterpriseAddress);
            }
        } else {
            // 查询全部
            if (startTime != null && endTime != null) {
                return billRepository.findByCreatedAtBetween(startTime, endTime);
            } else {
                return billRepository.findAll();
            }
        }
    }

    /**
     * 计算基础统计
     */
    private void calculateBasicStatistics(BillStatisticsDTO statistics, java.util.List<Bill> bills) {
        long totalBills = bills.size();
        long totalAmount = bills.stream()
            .mapToLong(b -> b.getFaceValue().multiply(new java.math.BigDecimal("100")).longValue())
            .sum();
        long averageAmount = totalBills > 0 ? totalAmount / totalBills : 0;
        long minAmount = bills.stream()
            .mapToLong(b -> b.getFaceValue().multiply(new java.math.BigDecimal("100")).longValue())
            .min()
            .orElse(0);
        long maxAmount = bills.stream()
            .mapToLong(b -> b.getFaceValue().multiply(new java.math.BigDecimal("100")).longValue())
            .max()
            .orElse(0);

        statistics.setTotalBills(totalBills);
        statistics.setTotalAmount(totalAmount);
        statistics.setAverageAmount(averageAmount);
        statistics.setMinAmount(minAmount);
        statistics.setMaxAmount(maxAmount);
    }

    /**
     * 计算状态分布
     */
    private void calculateStatusDistribution(BillStatisticsDTO statistics, java.util.List<Bill> bills) {
        java.util.Map<String, Long> statusCounts = bills.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                b -> b.getBillStatus().name(),
                java.util.stream.Collectors.counting()
            ));

        java.util.List<BillStatisticsDTO.StatusDistribution> distribution = statusCounts.entrySet().stream()
            .map(entry -> {
                BillStatisticsDTO.StatusDistribution sd = new BillStatisticsDTO.StatusDistribution();
                sd.setStatus(entry.getKey());
                sd.setStatusName(getStatusDisplayName(entry.getKey()));
                sd.setCount(entry.getValue());
                sd.setPercentage(bills.size() > 0 ? (entry.getValue() * 100.0 / bills.size()) : 0);
                return sd;
            })
            .collect(java.util.stream.Collectors.toList());

        statistics.setStatusDistribution(distribution);
        statistics.setStatusCounts(statusCounts);
    }

    /**
     * 计算类型分布
     */
    private void calculateTypeDistribution(BillStatisticsDTO statistics, java.util.List<Bill> bills) {
        java.util.Map<String, Long> typeCounts = bills.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                b -> b.getBillType().name(),
                java.util.stream.Collectors.counting()
            ));

        java.util.List<BillStatisticsDTO.TypeDistribution> distribution = typeCounts.entrySet().stream()
            .map(entry -> {
                BillStatisticsDTO.TypeDistribution td = new BillStatisticsDTO.TypeDistribution();
                td.setType(entry.getKey());
                td.setTypeName(getTypeDisplayName(entry.getKey()));
                td.setCount(entry.getValue());
                td.setPercentage(bills.size() > 0 ? (entry.getValue() * 100.0 / bills.size()) : 0);
                return td;
            })
            .collect(java.util.stream.Collectors.toList());

        statistics.setTypeDistribution(distribution);
        statistics.setTypeCounts(typeCounts);
    }

    /**
     * 计算融资统计
     */
    private void calculateFinancingStatistics(BillStatisticsDTO statistics, java.util.List<Bill> bills) {
        long discountedCount = bills.stream()
            .filter(b -> b.getBillStatus() == Bill.BillStatus.DISCOUNTED)
            .count();

        long discountAmount = bills.stream()
            .filter(b -> b.getBillStatus() == Bill.BillStatus.DISCOUNTED)
            .mapToLong(b -> b.getFaceValue().multiply(new java.math.BigDecimal("100")).longValue())
            .sum();

        long pledgedCount = bills.stream()
            .filter(b -> b.getBillStatus() == Bill.BillStatus.PLEDGED)
            .count();

        long pledgeAmount = bills.stream()
            .filter(b -> b.getBillStatus() == Bill.BillStatus.PLEDGED)
            .mapToLong(b -> b.getFaceValue().multiply(new java.math.BigDecimal("100")).longValue())
            .sum();

        BillStatisticsDTO.FinancingStatistics financing = new BillStatisticsDTO.FinancingStatistics();
        financing.setDiscountedCount(discountedCount);
        financing.setDiscountAmount(discountAmount);
        financing.setPledgedCount(pledgedCount);
        financing.setPledgeAmount(pledgeAmount);
        financing.setFinancingRate(bills.size() > 0 ? ((discountedCount + pledgedCount) * 100.0 / bills.size()) : 0);

        statistics.setFinancing(financing);
    }

    /**
     * 计算风险统计
     */
    private void calculateRiskStatistics(BillStatisticsDTO statistics, java.util.List<Bill> bills) {
        long frozenCount = bills.stream()
            .filter(b -> b.getBillStatus() == Bill.BillStatus.FROZEN)
            .count();

            long cancelledCount = bills.stream()
            .filter(b -> b.getBillStatus() == Bill.BillStatus.CANCELLED)
            .count();

        long totalRiskCount = frozenCount + cancelledCount;

        BillStatisticsDTO.RiskStatistics risk = new BillStatisticsDTO.RiskStatistics();
        risk.setFrozenCount(frozenCount);
        risk.setCancelledCount(cancelledCount);
        risk.setTotalRiskCount(totalRiskCount);
        risk.setRiskRate(bills.size() > 0 ? (totalRiskCount * 100.0 / bills.size()) : 0);

        statistics.setRisk(risk);
    }

    /**
     * 计算持票人统计
     */
    private void calculateHolderStatistics(BillStatisticsDTO statistics, java.util.List<Bill> bills) {
        java.util.Map<String, java.util.List<Bill>> groupedByHolder = bills.stream()
            .collect(java.util.stream.Collectors.groupingBy(Bill::getCurrentHolderAddress));

        java.util.List<BillStatisticsDTO.HolderStatistics> topHolders = groupedByHolder.entrySet().stream()
            .map(entry -> {
                BillStatisticsDTO.HolderStatistics hs = new BillStatisticsDTO.HolderStatistics();
                hs.setHolderId(entry.getValue().get(0).getCurrentHolderId());
                hs.setHolderName(entry.getValue().get(0).getCurrentHolderName());
                hs.setBillCount((long) entry.getValue().size());
                hs.setTotalAmount(entry.getValue().stream()
                    .mapToLong(b -> b.getFaceValue().multiply(new java.math.BigDecimal("100")).longValue())
                    .sum());
                return hs;
            })
            .sorted((a, b) -> Long.compare(b.getTotalAmount(), a.getTotalAmount()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());

        statistics.setTopHolders(topHolders);
    }

    /**
     * 获取状态显示名称
     */
    private String getStatusDisplayName(String status) {
        switch (status) {
            case "DRAFT": return "草稿";
            case "PENDING_ISSUANCE": return "待开票";
            case "ISSUED": return "已开票";
            case "NORMAL": return "正常";
            case "ENDORSED": return "已背书";
            case "PLEDGED": return "已质押";
            case "DISCOUNTED": return "已贴现";
            case "FINANCED": return "已融资";
            case "FROZEN": return "已冻结";
            case "EXPIRED": return "已过期";
            case "DISHONORED": return "已拒付";
            case "CANCELLED": return "已作废";
            case "PAID": return "已付款";
            case "SETTLED": return "已结算";
            default: return status;
        }
    }

    /**
     * 获取类型显示名称
     */
    private String getTypeDisplayName(String type) {
        switch (type) {
            case "BANK_ACCEPTANCE_BILL": return "银行承兑汇票";
            case "COMMERCIAL_ACCEPTANCE_BILL": return "商业承兑汇票";
            case "BANK_NOTE": return "银行本票";
            default: return type;
        }
    }
}
