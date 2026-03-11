package com.fisco.app.Modules.Credit.Service;

import java.math.BigInteger;
import java.util.List;

import org.fisco.bcos.sdk.v3.codec.datatypes.generated.tuples.generated.Tuple3;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.tuples.generated.Tuple5;
import org.fisco.bcos.sdk.v3.model.TransactionReceipt;
import org.fisco.bcos.sdk.v3.transaction.model.dto.TransactionResponse;
import org.fisco.bcos.sdk.v3.transaction.model.exception.ContractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fisco.app.Common.Service.BaseContractService;
import com.fisco.app.contract.credit.CreditLimitCore;
import com.fisco.app.contract.credit.CreditLimitCore.CreditEvent;
import com.fisco.app.contract.credit.CreditLimitScore;
import com.fisco.app.contract.credit.CreditLimitScore.ScoreRecord;

import io.swagger.annotations.ApiOperation;

/**
 * 信用合约上链服务
 *
 * 提供信用额度管理、信用评分计算等区块链操作
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@ApiOperation("信用合约上链服务")
@Service
public class CreditContractService extends BaseContractService {

    private static final Logger logger = LoggerFactory.getLogger(CreditContractService.class);

    /**
     * 信用额度核心合约地址
     */
    @Value("${contract.credit-core}")
    private String creditCoreContractAddress;

    /**
     * 信用评分合约地址
     */
    @Value("${contract.credit-score}")
    private String creditScoreContractAddress;

    /**
     * 信用额度核心合约实例
     */
    private CreditLimitCore creditCoreContract;

    /**
     * 信用评分合约实例
     */
    private CreditLimitScore creditScoreContract;

    /**
     * 初始化信用合约
     */
    @javax.annotation.PostConstruct
    public void init() {
        if (!fiscoEnabled) {
            logger.warn("FISCO BCOS 功能已禁用，信用合约服务不可用");
            return;
        }
        if (client == null || cryptoKeyPair == null) {
            logger.error("区块链客户端未初始化，无法加载信用合约");
            return;
        }

        logger.info("使用 SDK 密钥对，地址: {}", cryptoKeyPair.getAddress());

        // 加载信用额度核心合约
        this.creditCoreContract = CreditLimitCore.load(
                creditCoreContractAddress,
                client,
                cryptoKeyPair
        );
        logger.info("信用额度核心合约加载成功，地址: {}", creditCoreContractAddress);

        // 加载信用评分合约
        this.creditScoreContract = CreditLimitScore.load(
                creditScoreContractAddress,
                client,
                cryptoKeyPair
        );
        logger.info("信用评分合约加载成功，地址: {}", creditScoreContractAddress);
    }

    /**
     * 检查合约是否已加载
     */
    private void checkCoreContract() {
        if (creditCoreContract == null) {
            throw new RuntimeException("信用额度核心合约未初始化，请检查区块链连接");
        }
    }

    private void checkScoreContract() {
        if (creditScoreContract == null) {
            throw new RuntimeException("信用评分合约未初始化，请检查区块链连接");
        }
    }

    /**
     * 实现抽象方法 - 加载信用额度核心合约
     */
    @Override
    @SuppressWarnings("unchecked")
    protected org.fisco.bcos.sdk.v3.contract.Contract loadContract(String contractAddress) {
        return (org.fisco.bcos.sdk.v3.contract.Contract)
                CreditLimitCore.load(contractAddress, client, cryptoKeyPair);
    }

    // ==================== 信用额度管理 ====================

    /**
     * 设置授信额度
     *
     * @param enterpriseAddress 企业区块链地址
     * @param newLimit 新授信额度
     * @return 交易收据
     */
    public TransactionReceipt setCreditLimit(String enterpriseAddress, BigInteger newLimit) {
        checkCoreContract();

        // 参数校验
        if (enterpriseAddress == null || enterpriseAddress.isBlank()) {
            throw new IllegalArgumentException("企业地址不能为空");
        }
        if (newLimit == null || newLimit.compareTo(BigInteger.ZERO) < 0) {
            throw new IllegalArgumentException("授信额度不能为负数");
        }

        logger.info("设置授信额度: address={}, limit={}", enterpriseAddress, newLimit);

        TransactionResponse response = sendTransactionWithAudit(
                creditCoreContract,
                "setCreditLimit",
                new Object[]{enterpriseAddress, newLimit},
                "CREDIT_SET_LIMIT"
        );

        // 校验交易回执状态
        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("设置授信额度失败: {}", errorMsg);
            throw new RuntimeException("操作失败，请稍后重试");
        }
        return receipt;
    }

    /**
     * 使用信用额度
     *
     * @param enterpriseAddress 企业区块链地址
     * @param amount 使用金额
     * @param operationType 操作类型
     * @return 交易收据
     */
    public TransactionReceipt useCredit(String enterpriseAddress, BigInteger amount, String operationType) {
        checkCoreContract();

        // 参数校验
        if (enterpriseAddress == null || enterpriseAddress.isBlank()) {
            throw new IllegalArgumentException("企业地址不能为空");
        }
        if (amount == null || amount.compareTo(BigInteger.ZERO) <= 0) {
            throw new IllegalArgumentException("使用金额必须大于0");
        }

        logger.info("使用信用额度: address={}, amount={}, type={}", enterpriseAddress, amount, operationType);

        TransactionResponse response = sendTransactionWithAudit(
                creditCoreContract,
                "useCredit",
                new Object[]{enterpriseAddress, amount, operationType},
                "CREDIT_USE"
        );

        // 校验交易回执状态
        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("使用信用额度失败: {}", errorMsg);
            throw new RuntimeException("操作失败，请稍后重试");
        }
        return receipt;
    }

    /**
     * 释放信用额度
     *
     * @param enterpriseAddress 企业区块链地址
     * @param amount 释放金额
     * @param operationType 操作类型
     * @return 交易收据
     */
    public TransactionReceipt releaseCredit(String enterpriseAddress, BigInteger amount, String operationType) {
        checkCoreContract();

        // 参数校验
        if (enterpriseAddress == null || enterpriseAddress.isBlank()) {
            throw new IllegalArgumentException("企业地址不能为空");
        }
        if (amount == null || amount.compareTo(BigInteger.ZERO) <= 0) {
            throw new IllegalArgumentException("释放金额必须大于0");
        }

        logger.info("释放信用额度: address={}, amount={}, type={}", enterpriseAddress, amount, operationType);

        TransactionResponse response = sendTransactionWithAudit(
                creditCoreContract,
                "releaseCredit",
                new Object[]{enterpriseAddress, amount, operationType},
                "CREDIT_RELEASE"
        );

        // 校验交易回执状态
        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("释放信用额度失败: {}", errorMsg);
            throw new RuntimeException("操作失败，请稍后重试");
        }
        return receipt;
    }

    /**
     * 调整已用额度
     *
     * @param enterpriseAddress 企业区块链地址
     * @param adjustment 调整金额（正负）
     * @return 交易收据
     */
    public TransactionReceipt adjustUsedCredit(String enterpriseAddress, BigInteger adjustment) {
        checkCoreContract();

        // 参数校验
        if (enterpriseAddress == null || enterpriseAddress.isBlank()) {
            throw new IllegalArgumentException("企业地址不能为空");
        }
        if (adjustment == null) {
            throw new IllegalArgumentException("调整金额不能为空");
        }

        logger.info("调整已用额度: address={}, adjustment={}", enterpriseAddress, adjustment);

        TransactionResponse response = sendTransactionWithAudit(
                creditCoreContract,
                "adjustUsedCredit",
                new Object[]{enterpriseAddress, adjustment},
                "CREDIT_ADJUST"
        );

        // 校验交易回执状态
        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("调整已用额度失败: {}", errorMsg);
            throw new RuntimeException("操作失败，请稍后重试");
        }
        return receipt;
    }

    // ==================== 信用事件上报 ====================

    /**
     * 上报信用事件
     *
     * @param enterpriseAddress 企业区块链地址
     * @param eventType 事件类型 (0-8)
     * @param impact 影响值（正负）
     * @param eventDataHash 事件数据哈希
     * @return 交易收据
     */
    public TransactionReceipt reportCreditEvent(String enterpriseAddress, BigInteger eventType,
            BigInteger impact, byte[] eventDataHash) {
        checkCoreContract();

        // 参数校验
        if (enterpriseAddress == null || enterpriseAddress.isBlank()) {
            throw new IllegalArgumentException("企业地址不能为空");
        }
        if (eventType == null) {
            throw new IllegalArgumentException("事件类型不能为空");
        }

        logger.info("上报信用事件: address={}, eventType={}, impact={}", enterpriseAddress, eventType, impact);

        TransactionResponse response = sendTransactionWithAudit(
                creditCoreContract,
                "reportCreditEvent",
                new Object[]{enterpriseAddress, eventType, impact, eventDataHash},
                "CREDIT_EVENT_REPORT"
        );

        // 校验交易回执状态
        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("上报信用事件失败: {}", errorMsg);
            throw new RuntimeException("操作失败，请稍后重试");
        }
        return receipt;
    }

    // ==================== 信用查询 ====================

    /**
     * 获取信用额度信息
     *
     * @param enterpriseAddress 企业区块链地址
     * @return 信用信息 Tuple5 (enterpriseAddress, creditLimit, usedLimit, availableLimit, lastUpdateTime)
         */
    public Tuple5<String, BigInteger, BigInteger, BigInteger, BigInteger> getCreditInfo(String enterpriseAddress) {
        checkCoreContract();

        logger.debug("查询信用额度信息: {}", enterpriseAddress);

        try {
            return creditCoreContract.getCreditInfo(enterpriseAddress);
        } catch (ContractException e) {
            logger.error("查询信用额度信息失败: {}", enterpriseAddress, e);
            throw new RuntimeException("查询失败，请稍后重试");
        }
    }

    /**
     * 检查可用额度
     *
     * @param enterpriseAddress 企业区块链地址
     * @param amount 检查金额
     * @return 是否有足够额度
     */
    public boolean checkCreditLimit(String enterpriseAddress, BigInteger amount) {
        checkCoreContract();

        try {
            return creditCoreContract.checkCreditLimit(enterpriseAddress, amount);
        } catch (ContractException e) {
            logger.error("检查可用额度失败: {}", enterpriseAddress, e);
            throw new RuntimeException("查询失败，请稍后重试");
        }
    }

    /**
     * 检查企业是否有信用记录
     *
     * @param enterpriseAddress 企业区块链地址
     * @return 是否有记录
     */
    public boolean hasCreditRecord(String enterpriseAddress) {
        checkCoreContract();

        try {
            return creditCoreContract.hasCreditRecord(enterpriseAddress);
        } catch (ContractException e) {
            logger.error("检查信用记录失败: {}", enterpriseAddress, e);
            throw new RuntimeException("查询失败，请稍后重试");
        }
    }

    /**
     * 获取信用事件数量
     *
     * @param enterpriseAddress 企业区块链地址
     * @return 事件数量
     */
    public BigInteger getCreditEventCount(String enterpriseAddress) {
        checkCoreContract();

        try {
            return creditCoreContract.getCreditEventCount(enterpriseAddress);
        } catch (ContractException e) {
            logger.error("获取信用事件数量失败: {}", enterpriseAddress, e);
            throw new RuntimeException("查询失败，请稍后重试");
        }
    }

    /**
     * 获取信用事件列表
     *
     * @param enterpriseAddress 企业区块链地址
     * @param offset 起始索引
     * @param limit 数量限制
     * @return 事件列表
     */
    public List<CreditEvent> getCreditEvents(String enterpriseAddress, BigInteger offset, BigInteger limit) {
        checkCoreContract();

        try {
            return creditCoreContract.getCreditEvents(enterpriseAddress, offset, limit);
        } catch (ContractException e) {
            logger.error("获取信用事件列表失败: {}", enterpriseAddress, e);
            throw new RuntimeException("查询失败，请稍后重试");
        }
    }

    // ==================== 信用评分 ====================

    /**
     * 计算信用评分
     *
     * @param enterpriseAddress 企业区块链地址
     * @param factors 评分因素
     * @return 交易收据
     */
    public TransactionReceipt calculateScore(String enterpriseAddress, CreditLimitScore.ScoreFactors factors) {
        checkScoreContract();

        // 参数校验
        if (enterpriseAddress == null || enterpriseAddress.isBlank()) {
            throw new IllegalArgumentException("企业地址不能为空");
        }

        logger.info("计算信用评分: address={}", enterpriseAddress);

        TransactionResponse response = sendTransactionWithAudit(
                creditScoreContract,
                "calculateScore",
                new Object[]{enterpriseAddress, factors},
                "CREDIT_CALCULATE_SCORE"
        );

        // 校验交易回执状态
        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("计算信用评分失败: {}", errorMsg);
            throw new RuntimeException("操作失败，请稍后重试");
        }
        return receipt;
    }

    /**
     * 调整信用评分（手动）
     *
     * @param enterpriseAddress 企业区块链地址
     * @param newScore 新评分
     * @param reason 调整原因
     * @return 交易收据
     */
    public TransactionReceipt adjustScore(String enterpriseAddress, BigInteger newScore, String reason) {
        checkScoreContract();

        // 参数校验
        if (enterpriseAddress == null || enterpriseAddress.isBlank()) {
            throw new IllegalArgumentException("企业地址不能为空");
        }
        if (newScore == null || newScore.compareTo(BigInteger.ZERO) < 0) {
            throw new IllegalArgumentException("评分不能为负数");
        }

        logger.info("调整信用评分: address={}, newScore={}, reason={}", enterpriseAddress, newScore, reason);

        TransactionResponse response = sendTransactionWithAudit(
                creditScoreContract,
                "adjustScore",
                new Object[]{enterpriseAddress, newScore, reason},
                "CREDIT_ADJUST_SCORE"
        );

        // 校验交易回执状态
        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("调整信用评分失败: {}", errorMsg);
            throw new RuntimeException("操作失败，请稍后重试");
        }
        return receipt;
    }

    /**
     * 获取当前评分
     *
     * @param enterpriseAddress 企业区块链地址
     * @return 当前评分
     */
    public BigInteger getCurrentScore(String enterpriseAddress) {
        checkScoreContract();

        try {
            return creditScoreContract.getCurrentScore(enterpriseAddress);
        } catch (ContractException e) {
            logger.error("获取当前评分失败: {}", enterpriseAddress, e);
            throw new RuntimeException("查询失败，请稍后重试");
        }
    }

    /**
     * 获取最新评分记录
     *
     * @param enterpriseAddress 企业区块链地址
     * @return 评分记录 Tuple3 (score, previousScore, calculationTime)
     */
    public Tuple3<BigInteger, BigInteger, BigInteger> getLatestScore(String enterpriseAddress) {
        checkScoreContract();

        try {
            return creditScoreContract.getLatestScore(enterpriseAddress);
        } catch (ContractException e) {
            logger.error("获取最新评分记录失败: {}", enterpriseAddress, e);
            throw new RuntimeException("查询失败，请稍后重试");
        }
    }

    /**
     * 获取评分历史数量
     *
     * @param enterpriseAddress 企业区块链地址
     * @return 历史记录数量
     */
    public BigInteger getScoreHistoryCount(String enterpriseAddress) {
        checkScoreContract();

        try {
            return creditScoreContract.getScoreHistoryCount(enterpriseAddress);
        } catch (ContractException e) {
            logger.error("获取评分历史数量失败: {}", enterpriseAddress, e);
            throw new RuntimeException("查询失败，请稍后重试");
        }
    }

    /**
     * 获取评分历史
     *
     * @param enterpriseAddress 企业区块链地址
     * @param offset 起始索引
     * @param limit 数量限制
     * @return 评分历史列表
     */
    public List<ScoreRecord> getScoreHistory(String enterpriseAddress, BigInteger offset, BigInteger limit) {
        checkScoreContract();

        try {
            return creditScoreContract.getScoreHistory(enterpriseAddress, offset, limit);
        } catch (ContractException e) {
            logger.error("获取评分历史失败: {}", enterpriseAddress, e);
            throw new RuntimeException("查询失败，请稍后重试");
        }
    }

    /**
     * 检查评分等级
     *
     * @param enterpriseAddress 企业区块链地址
     * @param minScore 最低评分
     * @return 是否符合
     */
    public boolean checkScoreLevel(String enterpriseAddress, BigInteger minScore) {
        checkScoreContract();

        try {
            return creditScoreContract.checkScoreLevel(enterpriseAddress, minScore);
        } catch (ContractException e) {
            logger.error("检查评分等级失败: {}", enterpriseAddress, e);
            throw new RuntimeException("查询失败，请稍后重试");
        }
    }

    /**
     * 获取评分等级
     *
     * @param score 评分
     * @return 等级字符串
     */
    public String getScoreLevel(BigInteger score) {
        checkScoreContract();

        try {
            return creditScoreContract.getScoreLevel(score);
        } catch (ContractException e) {
            logger.error("获取评分等级失败: score={}", score, e);
            throw new RuntimeException("查询失败，请稍后重试");
        }
    }

    // ==================== 批量操作 ====================

    /**
     * 批量设置授信额度
     *
     * @param enterprises 企业地址列表
     * @param limits 额度列表
     * @return 交易收据
     */
    public TransactionReceipt batchSetCreditLimit(List<String> enterprises, List<BigInteger> limits) {
        checkCoreContract();

        // 参数校验
        if (enterprises == null || enterprises.isEmpty()) {
            throw new IllegalArgumentException("企业地址列表不能为空");
        }
        if (limits == null || limits.isEmpty()) {
            throw new IllegalArgumentException("额度列表不能为空");
        }
        if (enterprises.size() != limits.size()) {
            throw new IllegalArgumentException("企业地址数量与额度数量不匹配");
        }

        String firstAddress = enterprises.get(0);
        logger.info("批量设置授信额度: count={}, firstAddress={}", enterprises.size(), firstAddress);

        TransactionResponse response = sendTransactionWithAudit(
                creditCoreContract,
                "batchSetCreditLimit",
                new Object[]{enterprises, limits},
                "CREDIT_BATCH_SET_LIMIT"
        );

        // 校验交易回执状态
        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("批量设置授信额度失败: {}", errorMsg);
            throw new RuntimeException("操作失败，请稍后重试");
        }
        return receipt;
    }

    /**
     * 批量计算评分
     *
     * @param enterprises 企业地址列表
     * @param factorsArray 评分因素列表
     * @return 交易收据
     */
    public TransactionReceipt batchCalculateScore(List<String> enterprises,
            List<CreditLimitScore.ScoreFactors> factorsArray) {
        checkScoreContract();

        // 参数校验
        if (enterprises == null || enterprises.isEmpty()) {
            throw new IllegalArgumentException("企业地址列表不能为空");
        }
        if (factorsArray == null || factorsArray.isEmpty()) {
            throw new IllegalArgumentException("评分因素列表不能为空");
        }
        if (enterprises.size() != factorsArray.size()) {
            throw new IllegalArgumentException("企业地址数量与评分因素数量不匹配");
        }

        String firstAddress = enterprises.get(0);
        logger.info("批量计算评分: count={}, firstAddress={}", enterprises.size(), firstAddress);

        TransactionResponse response = sendTransactionWithAudit(
                creditScoreContract,
                "batchCalculateScore",
                new Object[]{enterprises, factorsArray},
                "CREDIT_BATCH_CALCULATE_SCORE"
        );

        // 校验交易回执状态
        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("批量计算评分失败: {}", errorMsg);
            throw new RuntimeException("操作失败，请稍后重试");
        }
        return receipt;
    }
}
