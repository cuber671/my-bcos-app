package com.fisco.app.Modules.Finance.Service.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fisco.app.Modules.Credit.Service.CreditService;
import com.fisco.app.Modules.Finance.Entity.Receivable;
import com.fisco.app.Modules.Finance.Entity.RepaymentRecord;
import com.fisco.app.Modules.Finance.Mapper.ReceivableMapper;
import com.fisco.app.Modules.Finance.Mapper.RepaymentRecordMapper;
import com.fisco.app.Modules.Finance.Service.FinanceService;
import com.fisco.app.Modules.Finance.Service.blockchain.ReceivableContractService;
import com.fisco.app.Modules.Logistics.Entity.LogisticsDelegate;
import com.fisco.app.Modules.Logistics.Mapper.LogisticsDelegateMapper;
import com.fisco.app.Modules.Warehouse.Entity.WarehouseReceipt;
import com.fisco.app.Modules.Warehouse.Mapper.WarehouseReceiptMapper;

/**
 * 金融服务实现类
 *
 * 实现应收款生成、确认、调整、还款等业务逻辑
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Service
public class FinanceServiceImpl implements FinanceService {

    private static final Logger logger = LoggerFactory.getLogger(FinanceServiceImpl.class);

    @Autowired
    private ReceivableMapper receivableMapper;

    @Autowired
    private RepaymentRecordMapper repaymentRecordMapper;

    @Autowired
    private LogisticsDelegateMapper logisticsDelegateMapper;

    @Autowired
    private ReceivableContractService receivableContractService;

    @Autowired
    private CreditService creditService;

    @Autowired
    private WarehouseReceiptMapper warehouseReceiptMapper;

    // ==================== 应收款操作 ====================

    /**
     * 预生成应收款（物流送达时自动调用）
     *
     * 根据物流单信息生成应收款，初始金额 = 实际重量 × 单价
     * 状态设为"待确认"
     */
    @Override
    @Transactional
    public Receivable generateReceivable(Long voucherId, BigDecimal unitPrice) {
        // 参数校验
        if (voucherId == null) {
            throw new IllegalArgumentException("物流单ID不能为空");
        }
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("单价必须大于0");
        }

        // 查询物流单信息
        LogisticsDelegate delegate = logisticsDelegateMapper.selectById(voucherId);
        if (delegate == null) {
            throw new IllegalArgumentException("物流单不存在: " + voucherId);
        }

        // 检查物流单状态是否为"已交付"（状态4）
        if (delegate.getStatus() == null || delegate.getStatus() != 4) {
            throw new IllegalStateException("物流单状态必须为已交付(4)才能生成应收款，当前状态: " + delegate.getStatus());
        }

        // 计算初始金额 = 实际运输数量 × 单价
        BigDecimal initialAmount = delegate.getTransportQuantity().multiply(unitPrice);

        // 生成应收款编号
        String receivableNo = "AR" + System.currentTimeMillis();

        // 创建应收款记录
        Receivable receivable = new Receivable();
        receivable.setReceivableNo(receivableNo);
        receivable.setBusinessScene(delegate.getBusinessScene()); // 1-入库生成, 2-转让配送签收生成
        receivable.setSourceVoucherId(voucherId);
        receivable.setCreditorEntId(delegate.getOwnerEntId()); // 债权人 = 货主
        receivable.setDebtorEntId(delegate.getCarrierEntId()); // 债务人 = 承运企业（买方）
        receivable.setInitialAmount(initialAmount);
        receivable.setAdjustedAmount(initialAmount);
        receivable.setCollectedAmount(BigDecimal.ZERO);
        receivable.setBalanceUnpaid(initialAmount);
        receivable.setCurrency("CNY");
        receivable.setDueDate(LocalDateTime.now().plusDays(30)); // 默认30天还款期限
        receivable.setStatus(Receivable.STATUS_PENDING); // 待确认
        receivable.setIsFinanced(0); // 未融资

        // 保存到数据库
        receivableMapper.insert(receivable);

        logger.info("生成应收款: receivableNo={}, initialAmount={}, voucherId={}",
                receivableNo, initialAmount, voucherId);

        // 尝试上链（失败则抛出异常回滚事务）
        if (receivableContractService != null) {
            try {
                String receivableId = String.valueOf(receivable.getId());
                // 转换为时间戳
                long dueDateTimestamp = receivable.getDueDate()
                        .atZone(ZoneId.systemDefault()).toEpochSecond();

                // 计算买卖方对哈希（简化处理，使用企业ID拼接）
                String buyerSellerPair = receivable.getCreditorEntId() + "_" + receivable.getDebtorEntId();
                byte[] buyerSellerPairHash = hashToBytes32(buyerSellerPair);

                receivableContractService.createReceivable(
                        receivableId,
                        initialAmount.multiply(new BigDecimal("100")).toBigInteger(), // 转换为分
                        BigInteger.valueOf(dueDateTimestamp),
                        buyerSellerPairHash,
                        new byte[32], // invoiceHash
                        new byte[32], // contractHash
                        new byte[32], // goodsDetailHash
                        BigInteger.valueOf(delegate.getBusinessScene())
                );

                logger.info("应收款上链成功: receivableNo={}", receivableNo);
            } catch (Exception e) {
                logger.error("应收款上链失败，事务将回滚: receivableNo={}", receivableNo, e);
                throw new RuntimeException("操作失败，请稍后重试");
            }
        }

        return receivable;
    }

    /**
     * 根据ID查询应收款
     */
    @Override
    public Receivable getReceivableById(Long id) {
        if (id == null) {
            return null;
        }
        return receivableMapper.selectById(id);
    }

    /**
     * 根据应收款编号查询应收款
     */
    @Override
    public Receivable getReceivableByNo(String receivableNo) {
        if (receivableNo == null || receivableNo.isBlank()) {
            return null;
        }
        return receivableMapper.selectByReceivableNo(receivableNo);
    }

    /**
     * 查询债权人的应收款列表
     */
    @Override
    public List<Receivable> listByCreditor(Long creditorEntId) {
        if (creditorEntId == null) {
            return List.of();
        }
        return receivableMapper.selectByCreditorEntId(creditorEntId);
    }

    /**
     * 查询债务人的应收款列表
     */
    @Override
    public List<Receivable> listByDebtor(Long debtorEntId) {
        if (debtorEntId == null) {
            return List.of();
        }
        return receivableMapper.selectByDebtorEntId(debtorEntId);
    }

    /**
     * 账单确权确认（债务人数字签名确认）
     *
     * 确认后账单正式生效，具备法律效力
     * 状态从"待确认"变为"生效中"
     */
    @Override
    @Transactional
    public Receivable confirmReceivable(Long receivableId, String signature) {
        // 查询应收款
        Receivable receivable = receivableMapper.selectById(receivableId);
        if (receivable == null) {
            throw new IllegalArgumentException("应收款不存在: " + receivableId);
        }

        // 状态校验：只有待确认(1)才能确认
        if (receivable.getStatus() != Receivable.STATUS_PENDING) {
            throw new IllegalStateException("应收款状态不是待确认，无法确认，当前状态: " + receivable.getStatus());
        }

        // 更新状态为生效中
        receivable.setStatus(Receivable.STATUS_ACTIVE);
        receivableMapper.updateById(receivable);

        logger.info("确认应收款: receivableId={}, receivableNo={}", receivableId, receivable.getReceivableNo());

        // 尝试上链
        try {
            if (receivableContractService != null) {
                String receivableIdStr = String.valueOf(receivable.getId());
                byte[] signatureBytes = signature != null ? signature.getBytes() : new byte[0];
                receivableContractService.confirmReceivable(receivableIdStr, signatureBytes);
                logger.info("应收款确认上链成功: receivableNo={}", receivable.getReceivableNo());
            }
        } catch (Exception e) {
            logger.warn("应收款确认上链失败: receivableNo={}, error={}", receivable.getReceivableNo(), e.getMessage());
        }

        return receivable;
    }

    /**
     * 金额动态修正
     *
     * 处理损耗或拆分：
     * - adjustType=1: 物流损耗扣减
     * - adjustType=2: 仓单拆分同步
     */
    @Override
    @Transactional
    public Receivable adjustReceivable(Long receivableId, Integer adjustType, BigDecimal amount) {
        // 查询应收款
        Receivable receivable = receivableMapper.selectById(receivableId);
        if (receivable == null) {
            throw new IllegalArgumentException("应收款不存在: " + receivableId);
        }

        // 状态校验：只有生效中(2)才能调整
        if (receivable.getStatus() != Receivable.STATUS_ACTIVE) {
            throw new IllegalStateException("应收款状态不是生效中，无法调整，当前状态: " + receivable.getStatus());
        }

        // 调整类型校验
        if (adjustType == null || (adjustType != 1 && adjustType != 2)) {
            throw new IllegalArgumentException("调整类型必须是1(物流损耗扣减)或2(仓单拆分同步)");
        }

        // 计算新的结算金额
        BigDecimal newAdjustedAmount = receivable.getAdjustedAmount().add(amount);

        // 校验：调整后金额不能为负
        if (newAdjustedAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("调整后金额不能为负数");
        }

        // 更新结算金额和待还余额
        BigDecimal balanceDiff = receivable.getAdjustedAmount().subtract(newAdjustedAmount);
        receivable.setAdjustedAmount(newAdjustedAmount);
        receivable.setBalanceUnpaid(receivable.getBalanceUnpaid().add(balanceDiff));
        receivableMapper.updateById(receivable);

        logger.info("调整应收款: receivableId={}, adjustType={}, oldAmount={}, newAmount={}",
                receivableId, adjustType, receivable.getAdjustedAmount(), newAdjustedAmount);

        // 尝试上链
        try {
            if (receivableContractService != null) {
                String receivableIdStr = String.valueOf(receivable.getId());
                receivableContractService.adjustReceivable(
                        receivableIdStr,
                        newAdjustedAmount.multiply(new BigDecimal("100")).toBigInteger(),
                        BigInteger.valueOf(adjustType)
                );
                logger.info("应收款调整上链成功: receivableNo={}", receivable.getReceivableNo());
            }
        } catch (Exception e) {
            logger.warn("应收款调整上链失败: receivableNo={}, error={}", receivable.getReceivableNo(), e.getMessage());
        }

        return receivable;
    }

    // ==================== 还款操作 ====================

    /**
     * 现金分批还款
     *
     * 记录本次还款额，增加 collected_amount
     * 校验还款金额 ≤ 待还余额
     */
    @Override
    @Transactional
    public RepaymentRecord cashRepayment(Long receivableId, BigDecimal amount, String paymentVoucher) {
        // 查询应收款
        Receivable receivable = receivableMapper.selectById(receivableId);
        if (receivable == null) {
            throw new IllegalArgumentException("应收款不存在: " + receivableId);
        }

        // 状态校验：只有生效中(2)或部分还款(3)才能还款
        if (receivable.getStatus() != Receivable.STATUS_ACTIVE && receivable.getStatus() != Receivable.STATUS_PARTIAL_REPAYMENT) {
            throw new IllegalStateException("应收款状态不是生效中或部分还款，无法还款，当前状态: " + receivable.getStatus());
        }

        // 校验还款金额
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("还款金额必须大于0");
        }
        if (amount.compareTo(receivable.getBalanceUnpaid()) > 0) {
            throw new IllegalArgumentException("还款金额不能大于待还余额，当前待还: " + receivable.getBalanceUnpaid());
        }

        // 生成还款编号
        String repaymentNo = "REP" + System.currentTimeMillis();

        // 创建还款记录
        RepaymentRecord record = new RepaymentRecord();
        record.setReceivableId(receivableId);
        record.setRepaymentNo(repaymentNo);
        record.setRepaymentType(1); // 现金还款
        record.setAmount(amount);
        record.setCurrency("CNY");
        record.setPaymentVoucher(paymentVoucher);
        record.setRepaymentTime(LocalDateTime.now());
        repaymentRecordMapper.insert(record);

        // 更新应收款
        BigDecimal newCollectedAmount = receivable.getCollectedAmount().add(amount);
        BigDecimal newBalanceUnpaid = receivable.getBalanceUnpaid().subtract(amount);
        receivable.setCollectedAmount(newCollectedAmount);
        receivable.setBalanceUnpaid(newBalanceUnpaid);

        // 更新状态
        if (newBalanceUnpaid.compareTo(BigDecimal.ZERO) == 0) {
            receivable.setStatus(Receivable.STATUS_SETTLED); // 已结清
        } else {
            receivable.setStatus(Receivable.STATUS_PARTIAL_REPAYMENT); // 部分还款
        }
        receivableMapper.updateById(receivable);

        logger.info("现金还款: receivableId={}, amount={}, newBalance={}",
                receivableId, amount, newBalanceUnpaid);

        // 尝试上链
        try {
            if (receivableContractService != null) {
                String receivableIdStr = String.valueOf(receivable.getId());
                receivableContractService.recordRepayment(
                        receivableIdStr,
                        amount.multiply(new BigDecimal("100")).toBigInteger(),
                        BigInteger.ONE // 现金还款
                );
                logger.info("还款记录上链成功: repaymentNo={}", repaymentNo);
            }
        } catch (Exception e) {
            logger.warn("还款记录上链失败: repaymentNo={}, error={}", repaymentNo, e.getMessage());
        }

        // 如果已结清，尝试上链结算
        if (receivable.getStatus() == Receivable.STATUS_SETTLED) {
            try {
                if (receivableContractService != null) {
                    receivableContractService.settleReceivable(String.valueOf(receivableId));
                    logger.info("应收款结算上链成功: receivableNo={}", receivable.getReceivableNo());
                }
            } catch (Exception e) {
                logger.warn("应收款结算上链失败: receivableNo={}, error={}", receivable.getReceivableNo(), e.getMessage());
            }
        }

        // FIN_023: 信用扣分触发 - 检查是否逾期还款
        try {
            triggerCreditEventForOverdueRepayment(receivable, amount);
        } catch (Exception e) {
            logger.warn("信用扣分触发失败: receivableNo={}, error={}", receivable.getReceivableNo(), e.getMessage());
        }

        return record;
    }

    /**
     * 仓单抵债核销
     *
     * 验证仓单所有权，确认后仓单 owner_id 变更为债权人
     * 同时按抵债价值冲减账单余额
     */
    @Override
    @Transactional
    public RepaymentRecord offsetWithCollateral(Long receivableId, Long receiptId,
                                                BigDecimal offsetPrice, String signatureHash) {
        // 查询应收款
        Receivable receivable = receivableMapper.selectById(receivableId);
        if (receivable == null) {
            throw new IllegalArgumentException("应收款不存在: " + receivableId);
        }

        // 状态校验
        if (receivable.getStatus() != Receivable.STATUS_ACTIVE && receivable.getStatus() != Receivable.STATUS_PARTIAL_REPAYMENT) {
            throw new IllegalStateException("应收款状态不是生效中或部分还款，无法抵债，当前状态: " + receivable.getStatus());
        }

        // 校验抵债金额
        if (offsetPrice == null || offsetPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("抵债价格必须大于0");
        }

        // 生成还款编号
        String repaymentNo = "REP" + System.currentTimeMillis();

        // 创建还款记录
        RepaymentRecord record = new RepaymentRecord();
        record.setReceivableId(receivableId);
        record.setRepaymentNo(repaymentNo);
        record.setRepaymentType(2); // 仓单抵债
        record.setAmount(offsetPrice);
        record.setCurrency("CNY");
        record.setReceiptId(receiptId);
        record.setOffsetPrice(offsetPrice);
        record.setSignatureHash(signatureHash);
        record.setRepaymentTime(LocalDateTime.now());
        repaymentRecordMapper.insert(record);

        // 更新应收款
        BigDecimal newCollectedAmount = receivable.getCollectedAmount().add(offsetPrice);
        BigDecimal newBalanceUnpaid = receivable.getBalanceUnpaid().subtract(offsetPrice);
        receivable.setCollectedAmount(newCollectedAmount);
        receivable.setBalanceUnpaid(newBalanceUnpaid);

        if (newBalanceUnpaid.compareTo(BigDecimal.ZERO) <= 0) {
            receivable.setStatus(Receivable.STATUS_SETTLED); // 已结清
        } else {
            receivable.setStatus(Receivable.STATUS_PARTIAL_REPAYMENT); // 部分还款
        }
        receivableMapper.updateById(receivable);

        logger.info("仓单抵债: receivableId={}, receiptId={}, offsetPrice={}, newBalance={}",
                receivableId, receiptId, offsetPrice, newBalanceUnpaid);

        // 尝试上链（以物抵债）
        try {
            if (receivableContractService != null) {
                String receivableIdStr = String.valueOf(receivable.getId());
                String receiptIdStr = String.valueOf(receiptId);
                byte[] signatureBytes = signatureHash != null ? signatureHash.getBytes() : new byte[0];

                receivableContractService.offsetDebtWithCollateral(
                        receivableIdStr,
                        receiptIdStr,
                        offsetPrice.multiply(new BigDecimal("100")).toBigInteger(),
                        signatureBytes
                );
                logger.info("以物抵债上链成功: repaymentNo={}", repaymentNo);
            }
        } catch (Exception e) {
            logger.warn("以物抵债上链失败: repaymentNo={}, error={}", repaymentNo, e.getMessage());
        }

        return record;
    }

    /**
     * 根据应收款ID查询还款记录列表
     */
    @Override
    public List<RepaymentRecord> listRepayments(Long receivableId) {
        if (receivableId == null) {
            return List.of();
        }
        return repaymentRecordMapper.selectByReceivableId(receivableId);
    }

    // ==================== 仓单拆分联动 ====================

    // ==================== 融资结算操作 ====================

    /**
     * 应收款融资
     *
     * 债权人用应收款向金融机构申请融资
     * 融资后应收款状态变为"已融资"
     */
    @Override
    @Transactional
    public Receivable financeReceivable(Long receivableId, BigDecimal financeAmount, Long financeEntId) {
        // 参数校验
        if (receivableId == null) {
            throw new IllegalArgumentException("应收款ID不能为空");
        }
        if (financeAmount == null || financeAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("融资金额必须大于0");
        }
        if (financeEntId == null) {
            throw new IllegalArgumentException("金融机构ID不能为空");
        }

        // 查询应收款
        Receivable receivable = receivableMapper.selectById(receivableId);
        if (receivable == null) {
            throw new IllegalArgumentException("应收款不存在: " + receivableId);
        }

        // 状态校验：只有生效中(2)才能融资
        if (receivable.getStatus() != Receivable.STATUS_ACTIVE) {
            throw new IllegalStateException("应收款状态不是生效中，无法融资，当前状态: " + receivable.getStatus());
        }

        // 校验融资金额不能超过应收款金额
        if (financeAmount.compareTo(receivable.getAdjustedAmount()) > 0) {
            throw new IllegalArgumentException("融资金额不能超过应收款金额，当前应收款: " + receivable.getAdjustedAmount());
        }

        // 检查是否已融资
        if (receivable.getIsFinanced() != null && receivable.getIsFinanced() == 1) {
            throw new IllegalStateException("该应收款已融资，不能重复融资");
        }

        // 更新应收款状态为已融资
        receivable.setIsFinanced(1);
        receivableMapper.updateById(receivable);

        logger.info("应收款融资: receivableId={}, receivableNo={}, financeAmount={}, financeEntId={}",
                receivableId, receivable.getReceivableNo(), financeAmount, financeEntId);

        // 尝试上链
        try {
            if (receivableContractService != null) {
                String receivableIdStr = String.valueOf(receivable.getId());
                String financeEntity = String.valueOf(financeEntId);

                receivableContractService.financeReceivable(
                        receivableIdStr,
                        financeAmount.multiply(new BigDecimal("100")).toBigInteger(),
                        financeEntity
                );
                logger.info("应收款融资上链成功: receivableNo={}", receivable.getReceivableNo());
            }
        } catch (Exception e) {
            logger.warn("应收款融资上链失败: receivableNo={}, error={}", receivable.getReceivableNo(), e.getMessage());
        }

        return receivable;
    }

    /**
     * 应收款结算
     *
     * 融资还款后结算应收款
     * 结算后应收款状态变为"已结清"
     */
    @Override
    @Transactional
    public Receivable settleReceivable(Long receivableId) {
        // 参数校验
        if (receivableId == null) {
            throw new IllegalArgumentException("应收款ID不能为空");
        }

        // 查询应收款
        Receivable receivable = receivableMapper.selectById(receivableId);
        if (receivable == null) {
            throw new IllegalArgumentException("应收款不存在: " + receivableId);
        }

        // 状态校验：只有已融资(状态2或3)才能结算
        // 已融资的应收款状态仍为2-生效中或3-部分还款
        if (receivable.getStatus() != Receivable.STATUS_ACTIVE && receivable.getStatus() != Receivable.STATUS_PARTIAL_REPAYMENT) {
            throw new IllegalStateException("应收款状态不是生效中或部分还款，无法结算，当前状态: " + receivable.getStatus());
        }

        // 校验是否已全额还款
        if (receivable.getBalanceUnpaid() == null || receivable.getBalanceUnpaid().compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException("应收款尚未全额还款，无法结算，待还余额: " + receivable.getBalanceUnpaid());
        }

        // 更新状态为已结清
        receivable.setStatus(Receivable.STATUS_SETTLED); // 已结清
        receivableMapper.updateById(receivable);

        logger.info("应收款结算: receivableId={}, receivableNo={}", receivableId, receivable.getReceivableNo());

        // 尝试上链
        try {
            if (receivableContractService != null) {
                receivableContractService.settleReceivable(String.valueOf(receivableId));
                logger.info("应收款结算上链成功: receivableNo={}", receivable.getReceivableNo());
            }
        } catch (Exception e) {
            logger.warn("应收款结算上链失败: receivableNo={}, error={}", receivable.getReceivableNo(), e.getMessage());
        }

        return receivable;
    }

    // ==================== 辅助方法 ====================

    /**
     * FIN_022: 仓单拆分联动 - 当仓单拆分时自动调用
     *
     * 当仓单拆分时，同步调整应收款金额
     * 1. 查询原应收款（关联原仓单）
     * 2. 根据拆分后的仓单金额比例，调整应收款金额
     * 3. 返回拆分后的应收款列表
     */
    @Override
    @Transactional
    public List<Receivable> syncReceiptSplit(Long parentReceiptId, List<Long> childReceiptIds) {
        // 参数校验
        if (parentReceiptId == null) {
            throw new IllegalArgumentException("原仓单ID不能为空");
        }
        if (childReceiptIds == null || childReceiptIds.isEmpty()) {
            throw new IllegalArgumentException("拆分后的仓单ID列表不能为空");
        }

        // 查询原仓单
        WarehouseReceipt parentReceipt = warehouseReceiptMapper.selectById(parentReceiptId);
        if (parentReceipt == null) {
            throw new IllegalArgumentException("原仓单不存在: " + parentReceiptId);
        }

        // 查询原应收款（根据source_voucher_id关联）
        Receivable parentReceivable = receivableMapper.selectById(parentReceipt.getId());
        if (parentReceivable == null) {
            logger.warn("原仓单未关联应收款: parentReceiptId={}", parentReceiptId);
            return List.of();
        }

        // 查询拆分后的仓单列表
        List<WarehouseReceipt> childReceipts = warehouseReceiptMapper.selectBatchIds(childReceiptIds);
        if (childReceipts.size() != childReceiptIds.size()) {
            throw new IllegalArgumentException("部分子仓单不存在");
        }

        // 计算原仓单总价值（使用重量作为价值参考）
        BigDecimal totalParentValue = parentReceipt.getWeight() != null ?
                parentReceipt.getWeight() : BigDecimal.ZERO;

        if (totalParentValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("原仓单价值为0，无法拆分");
        }

        // 计算拆分比例并调整应收款金额
        BigDecimal parentReceivableAmount = parentReceivable.getAdjustedAmount();

        // 简单处理：按拆分后仓单数量平均分配金额
        // 实际业务中应根据每个仓单的实际价值比例计算
        BigDecimal averageAmount = parentReceivableAmount.divide(
                BigDecimal.valueOf(childReceipts.size()),
                new MathContext(2, RoundingMode.HALF_UP));

        // 更新原应收款为拆分后的第一个子仓单金额
        parentReceivable.setAdjustedAmount(averageAmount);
        parentReceivable.setBalanceUnpaid(averageAmount.subtract(parentReceivable.getCollectedAmount()));
        receivableMapper.updateById(parentReceivable);

        logger.info("仓单拆分联动: parentReceiptId={}, originalAmount={}, newAmount={}",
                parentReceiptId, parentReceivableAmount, averageAmount);

        // 尝试上链调整金额
        try {
            if (receivableContractService != null) {
                receivableContractService.adjustReceivable(
                        String.valueOf(parentReceivable.getId()),
                        averageAmount.multiply(new BigDecimal("100")).toBigInteger(),
                        BigInteger.valueOf(2) // adjustType=2 表示仓单拆分同步
                );
                logger.info("仓单拆分联动上链成功: receivableId={}", parentReceivable.getId());
            }
        } catch (Exception e) {
            logger.warn("仓单拆分联动上链失败: receivableId={}, error={}",
                    parentReceivable.getId(), e.getMessage());
        }

        // 返回更新后的应收款列表（包含原应收款）
        return List.of(parentReceivable);
    }

    /**
     * FIN_023: 信用扣分触发 - 检查是否逾期还款
     *
     * 当应收款已逾期且用户进行还款时，触发信用扣分事件
     */
    private void triggerCreditEventForOverdueRepayment(Receivable receivable, BigDecimal repaymentAmount) {
        if (receivable == null || receivable.getDueDate() == null) {
            return;
        }

        // 检查是否已逾期（到期日早于当前时间）
        LocalDateTime now = LocalDateTime.now();
        if (receivable.getDueDate().isAfter(now)) {
            // 未逾期，不触发信用事件
            return;
        }

        Long debtorEntId = receivable.getDebtorEntId();
        if (debtorEntId == null) {
            logger.warn("应收款缺少债务人信息，无法触发信用事件");
            return;
        }

        // 计算逾期天数
        long overdueDays = java.time.Duration.between(receivable.getDueDate(), now).toDays();

        // 根据逾期天数计算扣分
        int scoreChange;
        String eventLevel;
        if (overdueDays <= 7) {
            scoreChange = -5; // 轻微逾期
            eventLevel = "LOW";
        } else if (overdueDays <= 30) {
            scoreChange = -10; // 中度逾期
            eventLevel = "MEDIUM";
        } else {
            scoreChange = -20; // 严重逾期
            eventLevel = "HIGH";
        }

        // 触发信用事件
        try {
            Long eventId = creditService.reportCreditEvent(
                    debtorEntId,
                    "OVERDUE_REPAYMENT", // 逾期还款
                    eventLevel,
                    "应收款编号:" + receivable.getReceivableNo() + "，逾期" + overdueDays + "天，本次还款" + repaymentAmount,
                    scoreChange,
                    "FINANCE",
                    String.valueOf(receivable.getId())
            );

            logger.info("信用扣分触发成功: debtorEntId={}, overdueDays={}, scoreChange={}, eventId={}",
                    debtorEntId, overdueDays, scoreChange, eventId);

        } catch (Exception e) {
            logger.error("信用扣分触发失败: debtorEntId={}, error={}", debtorEntId, e.getMessage());
        }
    }

    /**
     * 将字符串转换为32字节哈希
     */
    private byte[] hashToBytes32(String input) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            byte[] result = new byte[32];
            System.arraycopy(hash, 0, result, 0, 32);
            return result;
        } catch (Exception e) {
            logger.error("计算哈希失败: {}", e.getMessage());
            return new byte[32];
        }
    }
}
