package com.fisco.app.Modules.Finance.Service.blockchain;

import java.math.BigInteger;
import java.util.List;

import org.fisco.bcos.sdk.v3.model.TransactionReceipt;
import org.fisco.bcos.sdk.v3.transaction.model.dto.TransactionResponse;
import org.fisco.bcos.sdk.v3.transaction.model.exception.ContractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fisco.app.Common.Service.BaseContractService;
import com.fisco.app.contract.receivable.ReceivableCore;
import com.fisco.app.contract.receivable.ReceivableCore.ReceivableInput;
import com.fisco.app.contract.receivable.ReceivableCore.ReceivableInfo;
import com.fisco.app.contract.receivable.ReceivableRepayment;

import io.swagger.annotations.ApiOperation;

/**
 * 应收款上链服务
 *
 * 提供应收款创建、确认、调整、融资、结算等区块链操作
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@ApiOperation("应收款上链服务")
@Service
public class ReceivableContractService extends BaseContractService {

    private static final Logger logger = LoggerFactory.getLogger(ReceivableContractService.class);

    /**
     * 应收款核心合约地址
     */
    @Value("${contract.receivable-core:}")
    private String receivableCoreAddress;

    /**
     * 应收款还款合约地址
     */
    @Value("${contract.receivable-repayment:}")
    private String receivableRepaymentAddress;

    /**
     * 应收款核心合约实例
     */
    private ReceivableCore receivableCoreContract;

    /**
     * 应收款还款合约实例
     */
    private ReceivableRepayment receivableRepaymentContract;

    /**
     * 初始化应收款合约
     */
    @javax.annotation.PostConstruct
    public void init() {
        if (!fiscoEnabled) {
            logger.warn("FISCO BCOS 功能已禁用，应收款合约服务不可用");
            return;
        }
        if (client == null || cryptoKeyPair == null) {
            logger.error("区块链客户端未初始化，无法加载应收款合约");
            return;
        }

        logger.info("使用 SDK 密钥对，地址: {}", cryptoKeyPair.getAddress());

        // 加载应收款核心合约
        if (receivableCoreAddress != null && !receivableCoreAddress.isBlank()) {
            this.receivableCoreContract = ReceivableCore.load(
                    receivableCoreAddress,
                    client,
                    cryptoKeyPair
            );
            logger.info("应收款核心合约加载成功，地址: {}", receivableCoreAddress);
        } else {
            logger.warn("应收款核心合约地址未配置");
        }

        // 加载应收款还款合约
        if (receivableRepaymentAddress != null && !receivableRepaymentAddress.isBlank()) {
            this.receivableRepaymentContract = ReceivableRepayment.load(
                    receivableRepaymentAddress,
                    client,
                    cryptoKeyPair
            );
            logger.info("应收款还款合约加载成功，地址: {}", receivableRepaymentAddress);
        } else {
            logger.warn("应收款还款合约地址未配置");
        }
    }

    /**
     * 检查核心合约是否已加载
     */
    private void checkCoreContract() {
        if (receivableCoreContract == null) {
            throw new RuntimeException("应收款核心合约未初始化，请检查区块链连接");
        }
    }

    /**
     * 检查还款合约是否已加载
     */
    private void checkRepaymentContract() {
        if (receivableRepaymentContract == null) {
            throw new RuntimeException("应收款还款合约未初始化，请检查区块链连接");
        }
    }

    /**
     * 实现抽象方法 - 加载应收款合约
     */
    @Override
    @SuppressWarnings("unchecked")
    protected org.fisco.bcos.sdk.v3.contract.Contract loadContract(String contractAddress) {
        // 根据地址判断加载哪个合约
        if (contractAddress != null && contractAddress.equals(receivableCoreAddress)) {
            return (org.fisco.bcos.sdk.v3.contract.Contract)
                    ReceivableCore.load(contractAddress, client, cryptoKeyPair);
        } else if (contractAddress != null && contractAddress.equals(receivableRepaymentAddress)) {
            return (org.fisco.bcos.sdk.v3.contract.Contract)
                    ReceivableRepayment.load(contractAddress, client, cryptoKeyPair);
        }
        return null;
    }

    // ==================== 应收款核心操作 ====================

    /**
     * 创建应收款上链
     *
     * @param receivableId 应收款ID
     * @param initialAmount 初始金额
     * @param dueDate 到期日期（时间戳）
     * @param buyerSellerPairHash 买卖方对哈希
     * @param invoiceHash 发票哈希
     * @param contractHash 合同哈希
     * @param goodsDetailHash 货物详情哈希
     * @param businessScene 业务场景 (1-入库生成, 2-转让配送签收生成)
     * @return 交易收据
     */
    public TransactionReceipt createReceivable(
            String receivableId,
            BigInteger initialAmount,
            BigInteger dueDate,
            byte[] buyerSellerPairHash,
            byte[] invoiceHash,
            byte[] contractHash,
            byte[] goodsDetailHash,
            BigInteger businessScene) {

        checkCoreContract();

        // 参数校验
        if (receivableId == null || receivableId.isBlank()) {
            throw new IllegalArgumentException("应收款ID不能为空");
        }
        if (initialAmount == null || initialAmount.compareTo(BigInteger.ZERO) <= 0) {
            throw new IllegalArgumentException("初始金额必须大于0");
        }
        if (dueDate == null) {
            throw new IllegalArgumentException("到期日期不能为空");
        }

        // 构建输入参数
        ReceivableInput input = new ReceivableInput(
                new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(receivableId),
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256(initialAmount),
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256(dueDate),
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(buyerSellerPairHash != null ? buyerSellerPairHash : new byte[32]),
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(invoiceHash != null ? invoiceHash : new byte[32]),
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(contractHash != null ? contractHash : new byte[32]),
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(goodsDetailHash != null ? goodsDetailHash : new byte[32]),
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint8(businessScene != null ? businessScene : BigInteger.ONE)
        );

        logger.info("创建应收款上链: receivableId={}, initialAmount={}, businessScene={}",
                receivableId, initialAmount, businessScene);

        TransactionResponse response = sendTransactionWithAudit(
                receivableCoreContract,
                "createReceivable",
                new Object[]{input},
                "RECEIVABLE_CREATE"
        );

        // 校验交易回执状态
        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("创建应收款上链失败: {}", errorMsg);
            throw new RuntimeException("操作失败，请稍后重试");
        }
        return receipt;
    }

    /**
     * 确认应收款（债务人签名确认）
     *
     * @param receivableId 应收款ID
     * @param signature 债务人签名
     * @return 交易收据
     */
    public TransactionReceipt confirmReceivable(String receivableId, byte[] signature) {
        checkCoreContract();

        if (receivableId == null || receivableId.isBlank()) {
            throw new IllegalArgumentException("应收款ID不能为空");
        }

        logger.info("确认应收款上链: receivableId={}", receivableId);

        TransactionResponse response = sendTransactionWithAudit(
                receivableCoreContract,
                "confirmReceivable",
                new Object[]{receivableId, signature != null ? signature : new byte[0]},
                "RECEIVABLE_CONFIRM"
        );

        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("确认应收款上链失败: {}", errorMsg);
            throw new RuntimeException("操作失败，请稍后重试");
        }
        return receipt;
    }

    /**
     * 调整应收款金额
     *
     * @param receivableId 应收款ID
     * @param adjustedAmount 调整后的金额
     * @param adjustType 调整类型 (1-物流损耗扣减, 2-仓单拆分同步)
     * @return 交易收据
     */
    public TransactionReceipt adjustReceivable(String receivableId, BigInteger adjustedAmount, BigInteger adjustType) {
        checkCoreContract();

        if (receivableId == null || receivableId.isBlank()) {
            throw new IllegalArgumentException("应收款ID不能为空");
        }
        if (adjustedAmount == null || adjustedAmount.compareTo(BigInteger.ZERO) < 0) {
            throw new IllegalArgumentException("调整金额不能为负数");
        }

        logger.info("调整应收款上链: receivableId={}, adjustedAmount={}, adjustType={}",
                receivableId, adjustedAmount, adjustType);

        TransactionResponse response = sendTransactionWithAudit(
                receivableCoreContract,
                "adjustReceivable",
                new Object[]{receivableId, adjustedAmount, adjustType},
                "RECEIVABLE_ADJUST"
        );

        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("调整应收款上链失败: {}", errorMsg);
            throw new RuntimeException("操作失败，请稍后重试");
        }
        return receipt;
    }

    /**
     * 应收款融资
     *
     * @param receivableId 应收款ID
     * @param financeAmount 融资金额
     * @param financeEntity 融资方地址
     * @return 交易收据
     */
    public TransactionReceipt financeReceivable(String receivableId, BigInteger financeAmount, String financeEntity) {
        checkCoreContract();

        if (receivableId == null || receivableId.isBlank()) {
            throw new IllegalArgumentException("应收款ID不能为空");
        }
        if (financeAmount == null || financeAmount.compareTo(BigInteger.ZERO) <= 0) {
            throw new IllegalArgumentException("融资金额必须大于0");
        }

        logger.info("应收款融资上链: receivableId={}, financeAmount={}, financeEntity={}",
                receivableId, financeAmount, financeEntity);

        TransactionResponse response = sendTransactionWithAudit(
                receivableCoreContract,
                "financeReceivable",
                new Object[]{receivableId, financeAmount, financeEntity},
                "RECEIVABLE_FINANCE"
        );

        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("应收款融资上链失败: {}", errorMsg);
            throw new RuntimeException("操作失败，请稍后重试");
        }
        return receipt;
    }

    /**
     * 应收款结算
     *
     * @param receivableId 应收款ID
     * @return 交易收据
     */
    public TransactionReceipt settleReceivable(String receivableId) {
        checkCoreContract();

        if (receivableId == null || receivableId.isBlank()) {
            throw new IllegalArgumentException("应收款ID不能为空");
        }

        logger.info("应收款结算上链: receivableId={}", receivableId);

        TransactionResponse response = sendTransactionWithAudit(
                receivableCoreContract,
                "settleReceivable",
                new Object[]{receivableId},
                "RECEIVABLE_SETTLE"
        );

        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("应收款结算上链失败: {}", errorMsg);
            throw new RuntimeException("操作失败，请稍后重试");
        }
        return receipt;
    }

    // ==================== 应收款查询操作 ====================

    /**
     * 查询应收款信息
     *
     * @param receivableId 应收款ID
     * @return 应收款信息
     */
    public ReceivableInfo getReceivable(String receivableId) {
        checkCoreContract();

        logger.debug("查询应收款: receivableId={}", receivableId);

        try {
            return receivableCoreContract.getReceivable(receivableId);
        } catch (ContractException e) {
            logger.warn("应收款不存在: receivableId={}", receivableId);
            return null;
        }
    }

    /**
     * 根据买卖方对查询应收款列表
     *
     * @param buyerSellerPairHash 买卖方对哈希
     * @return 应收款ID列表
     */
    @SuppressWarnings("unchecked")
    public List<String> getReceivablesByPair(byte[] buyerSellerPairHash) {
        checkCoreContract();

        logger.debug("根据买卖方对查询应收款");

        try {
            return (List<String>) receivableCoreContract.getReceivablesByPair(buyerSellerPairHash);
        } catch (ContractException e) {
            logger.error("查询应收款列表失败", e);
            return List.of();
        }
    }

    /**
     * 查询应收款状态
     *
     * @param receivableId 应收款ID
     * @return 应收款状态 (0-不存在, 1-待确认, 2-生效中, 3-部分还款, 4-已结清, 5-逾期)
     */
    public BigInteger getReceivableStatus(String receivableId) {
        checkCoreContract();

        logger.debug("查询应收款状态: receivableId={}", receivableId);

        try {
            return receivableCoreContract.getReceivableStatus(receivableId);
        } catch (ContractException e) {
            logger.warn("查询应收款状态失败: receivableId={}", receivableId);
            return BigInteger.ZERO;
        }
    }

    // ==================== 还款记录操作 ====================

    /**
     * 记录还款
     *
     * @param receivableId 应收款ID
     * @param repaymentAmount 还款金额
     * @param repaymentType 还款类型 (1-现金还款, 2-仓单抵债)
     * @return 交易收据
     */
    public TransactionReceipt recordRepayment(String receivableId, BigInteger repaymentAmount, BigInteger repaymentType) {
        checkRepaymentContract();

        if (receivableId == null || receivableId.isBlank()) {
            throw new IllegalArgumentException("应收款ID不能为空");
        }
        if (repaymentAmount == null || repaymentAmount.compareTo(BigInteger.ZERO) <= 0) {
            throw new IllegalArgumentException("还款金额必须大于0");
        }

        logger.info("记录还款上链: receivableId={}, repaymentAmount={}, repaymentType={}",
                receivableId, repaymentAmount, repaymentType);

        TransactionResponse response = sendTransactionWithAudit(
                receivableRepaymentContract,
                "recordRepayment",
                new Object[]{receivableId, repaymentAmount, repaymentType},
                "REPAYMENT_RECORD"
        );

        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("记录还款上链失败: {}", errorMsg);
            throw new RuntimeException("操作失败，请稍后重试");
        }
        return receipt;
    }

    /**
     * 记录全额还款
     *
     * @param receivableId 应收款ID
     * @return 交易收据
     */
    public TransactionReceipt recordFullRepayment(String receivableId) {
        checkRepaymentContract();

        if (receivableId == null || receivableId.isBlank()) {
            throw new IllegalArgumentException("应收款ID不能为空");
        }

        logger.info("记录全额还款上链: receivableId={}", receivableId);

        TransactionResponse response = sendTransactionWithAudit(
                receivableRepaymentContract,
                "recordFullRepayment",
                new Object[]{receivableId},
                "REPAYMENT_FULL"
        );

        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("记录全额还款上链失败: {}", errorMsg);
            throw new RuntimeException("操作失败，请稍后重试");
        }
        return receipt;
    }

    /**
     * 以物抵债
     *
     * @param receivableId 应收款ID
     * @param receiptId 仓单ID
     * @param offsetAmount 抵债金额
     * @param signatureHash 签名哈希
     * @return 交易收据
     */
    public TransactionReceipt offsetDebtWithCollateral(
            String receivableId,
            String receiptId,
            BigInteger offsetAmount,
            byte[] signatureHash) {

        checkRepaymentContract();

        if (receivableId == null || receivableId.isBlank()) {
            throw new IllegalArgumentException("应收款ID不能为空");
        }
        if (receiptId == null || receiptId.isBlank()) {
            throw new IllegalArgumentException("仓单ID不能为空");
        }

        logger.info("以物抵债上链: receivableId={}, receiptId={}, offsetAmount={}",
                receivableId, receiptId, offsetAmount);

        TransactionResponse response = sendTransactionWithAudit(
                receivableRepaymentContract,
                "offsetDebtWithCollateral",
                new Object[]{receivableId, receiptId, offsetAmount, signatureHash != null ? signatureHash : new byte[0]},
                "OFFSET_DEBT_COLLATERAL"
        );

        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("以物抵债上链失败: {}", errorMsg);
            throw new RuntimeException("操作失败，请稍后重试");
        }
        return receipt;
    }
}
