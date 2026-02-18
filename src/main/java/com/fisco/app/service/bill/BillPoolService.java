package com.fisco.app.service.bill;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fisco.app.dto.bill.BillInvestRequest;
import com.fisco.app.dto.bill.BillInvestResponse;
import com.fisco.app.dto.bill.BillPoolFilter;
import com.fisco.app.dto.bill.BillPoolView;
import com.fisco.app.dto.endorsement.EndorseBillRequest;
import com.fisco.app.dto.endorsement.EndorsementResponse;
import com.fisco.app.entity.bill.Bill;
import com.fisco.app.entity.bill.BillInvestment;
import com.fisco.app.entity.bill.Endorsement;
import com.fisco.app.entity.enterprise.Enterprise;
import com.fisco.app.exception.BusinessException;
import com.fisco.app.repository.bill.BillInvestmentRepository;
import com.fisco.app.repository.bill.BillRepository;
import com.fisco.app.repository.enterprise.EnterpriseRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 票据池服务
 * 提供票据池查询、票据投资等功能
 *
 * @author FISCO BCOS Supply Chain Finance
 * @since 2026-02-03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BillPoolService {

    private final BillRepository billRepository;
    private final BillInvestmentRepository investmentRepository;
    private final BillService billService;
    private final EnterpriseRepository enterpriseRepository;

    // ==================== 票据池查询 ====================

    /**
     * 查询票据池
     */
    @Transactional(readOnly = true)
    @SuppressWarnings("nullness")
    public Page<BillPoolView> getBillPool(BillPoolFilter filter) {
        log.info("查询票据池: filter={}", filter);

        LocalDateTime now = LocalDateTime.now();

        // 构建基础查询条件
        List<Bill> bills = billRepository.findBillPoolBills(now);

        log.debug("查询到 {} 条票据池基础数据", bills.size());

        // 应用筛选条件
        List<Bill> filteredBills = bills.stream()
                .filter(bill -> applyFilter(bill, filter))
                .collect(Collectors.toList());

        log.debug("筛选后剩余 {} 条票据", filteredBills.size());

        // 转换为视图对象
        List<BillPoolView> views = filteredBills.stream()
                .map(this::buildBillPoolView)
                .collect(Collectors.toList());

        // 计算投资指标
        views.forEach(this::calculateInvestmentMetrics);

        // 排序
        sortViews(views, filter.getSortBy(), filter.getSortOrder());

        // 分页
        int start = filter.getPage() * filter.getSize();
        int end = Math.min(start + filter.getSize(), views.size());

        // 分页并确保非空
        int safeStart = Math.min(start, views.size());
        int safeEnd = Math.min(end, views.size());

        List<BillPoolView> pagedViews;
        if (safeStart < safeEnd) {
            pagedViews = views.subList(safeStart, safeEnd);
        } else {
            pagedViews = java.util.Collections.emptyList();
        }

        // 确保非空以满足IDE的null检查
        Objects.requireNonNull(pagedViews);

        return new PageImpl<>(pagedViews,
            PageRequest.of(filter.getPage(), filter.getSize()),
            views.size());
    }

    /**
     * 查询可投资票据
     */
    @Transactional(readOnly = true)
    public List<BillPoolView> getAvailableBills(String institutionId, BillPoolFilter filter) {
        log.info("查询可投资票据: institutionId={}", institutionId);

        // 获取票据池
        filter.setPage(0);
        filter.setSize(Integer.MAX_VALUE);
        Page<BillPoolView> poolPage = getBillPool(filter);

        // 按收益率排序
        List<BillPoolView> availableBills = poolPage.stream()
                .sorted((v1, v2) -> v2.getExpectedReturn().compareTo(v1.getExpectedReturn()))
                .collect(Collectors.toList());

        log.info("找到 {} 条可投资票据", availableBills.size());

        return availableBills;
    }

    // ==================== 票据投资 ====================

    /**
     * 票据投资
     */
    @Transactional(rollbackFor = Exception.class)
    @SuppressWarnings("null")
    public BillInvestResponse investBill(String billId,
                                        BillInvestRequest request,
                                        String investorAddress) {
        log.info("==================== 票据投资开始 ====================");
        log.info("票据ID: {}, 投资机构: {}, 投资金额: {}",
                 billId, investorAddress, request.getInvestAmount());

        LocalDateTime now = LocalDateTime.now();
        long startTime = System.currentTimeMillis();

        try {
            // ========== 步骤1: 验证票据 ==========
            log.debug("步骤1: 验证票据");
            Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new BusinessException("票据不存在"));

            validateBillForInvestment(bill);

            // ========== 步骤2: 验证投资机构 ==========
            log.debug("步骤2: 验证投资机构");
            Enterprise investor = enterpriseRepository.findByAddress(investorAddress)
                .orElseThrow(() -> new BusinessException("投资机构不存在"));

            validateInvestor(investor);

            // ========== 步骤3: 验证投资金额 ==========
            log.debug("步骤3: 验证投资金额");
            BigDecimal investAmount = request.getInvestAmount();
            BigDecimal faceValue = bill.getFaceValue();

            if (investAmount.compareTo(faceValue) > 0) {
                throw new BusinessException("投资金额不能超过票据面值");
            }

            if (investAmount.compareTo(faceValue.multiply(new BigDecimal("0.1"))) < 0) {
                throw new BusinessException("投资金额不能低于票据面值的10%");
            }

            // ========== 步骤4: 验证当前持票人 ==========
            log.debug("步骤4: 验证当前持票人");
            String currentHolderId = bill.getCurrentHolderId();

            if (currentHolderId.equals(investor.getId())) {
                throw new BusinessException("不能投资自己持有的票据");
            }

            // ========== 步骤5: 计算投资价格 ==========
            log.debug("步骤5: 计算投资价格");
            int remainingDays = (int) ChronoUnit.DAYS.between(now, bill.getDueDate());

            // 贴现计算
            BigDecimal discount = faceValue
                .multiply(request.getInvestRate())
                .multiply(BigDecimal.valueOf(remainingDays))
                .divide(BigDecimal.valueOf(36000), 2, RoundingMode.HALF_UP);

            BigDecimal expectedReturn = faceValue.subtract(investAmount);

            log.info("贴现计算: 面值={}, 利率={}%, 天数={}, 贴现={}, 预期收益={}",
                     faceValue, request.getInvestRate(), remainingDays,
                     discount, expectedReturn);

            // ========== 步骤6: 检查是否有未完成的投资 ==========
            log.debug("步骤6: 检查未完成投资");
            boolean hasPendingInvestment = investmentRepository.existsPendingInvestmentByBillId(billId);

            if (hasPendingInvestment) {
                throw new BusinessException("票据有未完成的投资，请稍后再试");
            }

            // ========== 步骤7: 创建投资记录 ==========
            log.debug("步骤7: 创建投资记录");
            BillInvestment investment = new BillInvestment();
            investment.setBillId(billId);
            investment.setBillNo(bill.getBillNo());
            investment.setBillFaceValue(faceValue);
            investment.setInvestorId(investor.getId());
            investment.setInvestorName(investor.getName());
            investment.setInvestorAddress(investor.getAddress());
            investment.setOriginalHolderId(currentHolderId);
            investment.setOriginalHolderName(bill.getCurrentHolderName());
            investment.setOriginalHolderAddress(bill.getCurrentHolderAddress());
            investment.setInvestAmount(investAmount);
            investment.setInvestRate(request.getInvestRate());
            investment.setExpectedReturn(expectedReturn);
            investment.setInvestmentDays(remainingDays);
            investment.setMaturityAmount(faceValue);
            investment.setInvestmentNotes(request.getInvestmentNotes());
            investment.setCreatedBy(investor.getId());

            investmentRepository.save(investment);

            // ========== 步骤8: 执行背书转让 ==========
            log.debug("步骤8: 执行背书转让");
            EndorseBillRequest endorseRequest = new EndorseBillRequest();
            endorseRequest.setEndorseeAddress(investorAddress);
            endorseRequest.setEndorsementType(Endorsement.EndorsementType.DISCOUNT);
            endorseRequest.setRemark("票据池投资");

            // 执行背书
            EndorsementResponse endorseResponse = billService.endorseBill(
                billId, endorseRequest, bill.getCurrentHolderAddress());

            // 更新投资记录
            investment.setEndorsementId(endorseResponse.getId());
            investment.setTxHash(endorseResponse.getTxHash());
            investment.setStatus(BillInvestment.InvestmentStatus.CONFIRMED.toString());
            investment.setConfirmationDate(now);
            investment.setCompletionDate(now);
            investment.setBlockchainTime(now);
            investment.setUpdatedBy(investor.getId());

            investmentRepository.save(investment);

            // ========== 步骤9: 构建响应 ==========
            log.debug("步骤9: 构建响应");
            BillInvestResponse response = buildInvestResponse(investment, bill);

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓✓✓ 票据投资完成: investmentId={}, 票据={}, 投资金额={}, 耗时={}ms",
                     investment.getId(), bill.getBillNo(), investAmount, duration);
            log.info("==================== 票据投资结束 ====================");

            return response;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ 票据投资失败: billId={}, 耗时={}ms, error={}",
                     billId, duration, e.getMessage(), e);
            log.info("==================== 票据投资失败（结束） ====================");
            throw e;
        }
    }

    /**
     * 查询投资记录
     */
    @Transactional(readOnly = true)
    public List<BillInvestResponse> getInvestmentRecords(@NonNull String institutionId) {
        log.info("查询投资记录: institutionId={}", institutionId);

        // 参数非空验证
        Objects.requireNonNull(institutionId, "机构ID不能为空");

        Enterprise investor = enterpriseRepository.findById(institutionId)
            .orElseThrow(() -> new BusinessException("投资机构不存在"));

        String investorId = investor.getId();
        if (investorId == null) {
            throw new BusinessException("投资机构ID为空");
        }

        // 确保非空以满足IDE的null检查
        Objects.requireNonNull(investorId);

        List<BillInvestment> investments = investmentRepository
            .findByInvestorIdOrderByDateDesc(investorId);

        return investments.stream()
                .map(inv -> {
                    String billId = inv.getBillId();
                    if (billId == null) {
                        return buildInvestResponse(inv, null);
                    }
                    Bill bill = billRepository.findById(billId).orElse(null);
                    return buildInvestResponse(inv, bill);
                })
                .collect(Collectors.toList());
    }

    /**
     * 票据拆分
     * @param billId 原票据ID
     * @param splitAmounts 拆分后的金额列表（总和必须等于原票据面值）
     */
    @Transactional(rollbackFor = Exception.class)
    public List<Bill> splitBill(@NonNull String billId, List<BigDecimal> splitAmounts, String operatorAddress) {
        log.info("开始拆分票据: billId={}, 拆分金额={}", billId, splitAmounts);

        // 1. 验证原票据
        Bill originalBill = billRepository.findById(billId)
                .orElseThrow(() -> new BusinessException("原票据不存在"));

        // 只有状态为 ISSUED 的票据可以拆分
        if (originalBill.getBillStatus() != Bill.BillStatus.ISSUED) {
            throw new BusinessException("当前状态不允许拆分");
        }

        // 2. 验证金额总和
        BigDecimal totalSplit = splitAmounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalSplit.compareTo(originalBill.getFaceValue()) != 0) {
            throw new BusinessException("拆分金额总和必须等于原票据面值");
        }

        // 3. 调用区块链合约：在链上销毁原票，生成新票
        // String txHash = contractService.splitBillOnChain(billId, splitAmounts);

        // 4. 数据库处理：作废原票据
        originalBill.setBillStatus(Bill.BillStatus.CANCELLED);
        originalBill.setRemarks("票据已拆分，原票据作废");
        billRepository.save(originalBill);

        // 5. 循环创建新子票据
        List<Bill> subBills = new ArrayList<>();
        for (int i = 0; i < splitAmounts.size(); i++) {
            Bill subBill = new Bill();
            BeanUtils.copyProperties(originalBill, subBill, "id", "faceValue", "billNo");
            
            subBill.setBillId(UUID.randomUUID().toString());
            subBill.setBillNo(originalBill.getBillNo() + "-" + String.format("%03d", i + 1));
            subBill.setFaceValue(splitAmounts.get(i));
            subBill.setBillStatus(Bill.BillStatus.ISSUED);
            subBill.setParentBillId(billId); // 建立溯源关系
            
            subBills.add(billRepository.save(subBill));
        }

        return subBills;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 验证票据是否可以投资
     */
    private void validateBillForInvestment(Bill bill) {
        if (bill.getBillStatus() != Bill.BillStatus.ISSUED) {
            throw new BusinessException("票据状态不正常，当前状态: " + bill.getBillStatus());
        }

        if (bill.getDueDate().isBefore(LocalDateTime.now())) {
            throw new BusinessException("票据已过期，无法投资");
        }

        if (bill.getBlockchainStatus() != Bill.BlockchainStatus.ONCHAIN) {
            throw new BusinessException("票据未上链，无法投资");
        }

        // 检查剩余天数
        long remainingDays = ChronoUnit.DAYS.between(LocalDateTime.now(), bill.getDueDate());
        if (remainingDays < 30) {
            throw new BusinessException("票据剩余天数少于30天，无法投资");
        }
    }

    /**
     * 验证投资机构资格
     *
     * @param investor 投资机构
     */
    private void validateInvestor(Enterprise investor) {
        // 验证投资机构状态
        // 未来可扩展：验证金融机构资质、风险等级、投资限额等
        log.debug("验证投资机构: {}", investor.getName());
    }

    /**
     * 应用筛选条件
     */
    private boolean applyFilter(Bill bill, BillPoolFilter filter) {
        // 票据类型筛选
        if (filter.getBillType() != null &&
            !bill.getBillType().toString().equals(filter.getBillType())) {
            return false;
        }

        // 金额筛选
        if (filter.getMinAmount() != null &&
            bill.getFaceValue().compareTo(filter.getMinAmount()) < 0) {
            return false;
        }

        if (filter.getMaxAmount() != null &&
            bill.getFaceValue().compareTo(filter.getMaxAmount()) > 0) {
            return false;
        }

        // 剩余天数筛选
        long remainingDays = ChronoUnit.DAYS.between(LocalDateTime.now(), bill.getDueDate());

        if (filter.getMinRemainingDays() != null &&
            remainingDays < filter.getMinRemainingDays()) {
            return false;
        }

        if (filter.getMaxRemainingDays() != null &&
            remainingDays > filter.getMaxRemainingDays()) {
            return false;
        }

        // 持票人筛选
        if (filter.getHolderId() != null &&
            !bill.getCurrentHolderId().equals(filter.getHolderId())) {
            return false;
        }

        return true;
    }

    /**
     * 构建票据池视图对象
     */
    private BillPoolView buildBillPoolView(Bill bill) {
        BillPoolView view = new BillPoolView();
        view.setBillId(bill.getBillId());
        view.setBillNo(bill.getBillNo());
        view.setBillType(bill.getBillType().toString());
        view.setFaceValue(bill.getFaceValue());
        view.setCurrency(bill.getCurrency());
        view.setMaturityDate(bill.getDueDate());
        view.setIssueDate(bill.getIssueDate());
        view.setAcceptorName(bill.getDraweeName());
        view.setCurrentHolderName(bill.getCurrentHolderName());
        view.setOnChain(bill.getBlockchainStatus() == Bill.BlockchainStatus.ONCHAIN);
        view.setTxHash(bill.getBlockchainTxHash());

        // 计算剩余天数
        long remainingDays = ChronoUnit.DAYS.between(LocalDateTime.now(), bill.getDueDate());
        view.setRemainingDays((int) remainingDays);

        return view;
    }

    /**
     * 计算投资指标
     */
    private void calculateInvestmentMetrics(BillPoolView view) {
        // 计算预期收益率
        long remainingDays = view.getRemainingDays() != null ? view.getRemainingDays() : 90;

        // 假设年化收益率为5.5%
        BigDecimal annualRate = new BigDecimal("5.5");
        BigDecimal expectedReturn = annualRate
            .multiply(BigDecimal.valueOf(remainingDays))
            .divide(BigDecimal.valueOf(365), 4, RoundingMode.HALF_UP);

        view.setExpectedReturn(expectedReturn);

        // 计算风险评分
        Integer riskScore;
        String riskLevel;
        String advice;

        if ("BANK_ACCEPTANCE_BILL".equals(view.getBillType())) {
            riskScore = 15;
            riskLevel = "LOW";
            advice = "RECOMMENDED";
        } else {
            riskScore = 35;
            riskLevel = "MEDIUM";
            advice = "CAUTION";
        }

        view.setRiskScore(riskScore);
        view.setRiskLevel(riskLevel);
        view.setCanInvest(true);
        view.setInvestmentAdvice(advice);

        // 初始化统计数据
        view.setViewCount(0);
        view.setInquiryCount(0);
        view.setInvestmentCount(0);
    }

    /**
     * 排序
     */
    private void sortViews(List<BillPoolView> views, String sortBy, String sortOrder) {
        Comparator<BillPoolView> comparator;

        switch (sortBy) {
            case "remainingDays":
                comparator = Comparator.comparing(BillPoolView::getRemainingDays);
                break;
            case "faceValue":
                comparator = Comparator.comparing(BillPoolView::getFaceValue);
                break;
            case "expectedReturn":
                comparator = Comparator.comparing(BillPoolView::getExpectedReturn);
                break;
            case "riskScore":
                comparator = Comparator.comparing(BillPoolView::getRiskScore);
                break;
            default:
                comparator = Comparator.comparing(BillPoolView::getRemainingDays);
        }

        if ("DESC".equals(sortOrder)) {
            comparator = comparator.reversed();
        }

        views.sort(comparator);
    }

    /**
     * 构建投资响应
     */
    private BillInvestResponse buildInvestResponse(BillInvestment investment, Bill bill) {
        BillInvestResponse response = new BillInvestResponse();
        response.setInvestmentId(investment.getId());
        response.setBillId(investment.getBillId());
        response.setBillNo(investment.getBillNo());
        response.setInvestAmount(investment.getInvestAmount());
        response.setInvestRate(investment.getInvestRate());
        response.setExpectedReturn(investment.getExpectedReturn());
        response.setInvestmentDays(investment.getInvestmentDays());
        response.setMaturityAmount(investment.getMaturityAmount());
        response.setStatus(investment.getStatus());
        response.setInvestmentDate(investment.getInvestmentDate());
        response.setConfirmationDate(investment.getConfirmationDate());
        response.setCompletionDate(investment.getCompletionDate());
        response.setOriginalHolderName(investment.getOriginalHolderName());
        response.setInvestorName(investment.getInvestorName());
        response.setEndorsementId(investment.getEndorsementId());
        response.setTxHash(investment.getTxHash());
        response.setBlockchainTime(investment.getBlockchainTime());
        response.setInvestmentNotes(investment.getInvestmentNotes());

        if (bill != null) {
            response.setActualReturn(investment.getActualReturn());
            response.setSettlementDate(investment.getSettlementDate());
        }

        return response;
    }
}
