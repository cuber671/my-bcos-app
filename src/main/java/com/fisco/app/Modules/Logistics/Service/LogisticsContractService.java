package com.fisco.app.Modules.Logistics.Service;

import java.math.BigInteger;
import java.util.List;

import org.fisco.bcos.sdk.v3.contract.Contract;
import org.fisco.bcos.sdk.v3.model.TransactionReceipt;
import org.fisco.bcos.sdk.v3.transaction.model.dto.TransactionResponse;
import org.fisco.bcos.sdk.v3.transaction.model.exception.ContractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fisco.app.Common.Service.BaseContractService;
import com.fisco.app.contract.logistics.LogisticsCore;
import com.fisco.app.contract.logistics.LogisticsOps;

import javax.annotation.PostConstruct;

/**
 * 物流合约服务
 *
 * 提供物流模块的区块链上链操作
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Service
public class LogisticsContractService extends BaseContractService {

    private static final Logger logger = LoggerFactory.getLogger(LogisticsContractService.class);

    @Value("${contract.logistics-core:0x69ef4c5eca7bc099c2e8a8336c97af765d60dbf1}")
    private String logisticsCoreAddress;

    @Value("${contract.logistics-ops:0x41a1281dba209614f2ada8ecc75fd957ad179d7b}")
    private String logisticsOpsAddress;

    private LogisticsCore logisticsCore;
    private LogisticsOps logisticsOps;

    @PostConstruct
    public void init() {
        if (!fiscoEnabled) {
            logger.warn("FISCO BCOS 功能已禁用，物流合约服务不可用");
            return;
        }
        if (client == null || cryptoKeyPair == null) {
            logger.error("区块链客户端未初始化，无法加载物流合约");
            return;
        }

        logger.info("使用 SDK 密钥对，地址: {}", getCurrentAccountAddress());

        // 加载物流核心合约
        this.logisticsCore = LogisticsCore.load(
                logisticsCoreAddress,
                client,
                cryptoKeyPair
        );
        logger.info("物流核心合约加载成功，地址: {}", logisticsCoreAddress);

        // 加载物流操作合约
        this.logisticsOps = LogisticsOps.load(
                logisticsOpsAddress,
                client,
                cryptoKeyPair
        );
        logger.info("物流操作合约加载成功，地址: {}", logisticsOpsAddress);
    }

    /**
     * 实现抽象方法 - 加载物流核心合约
     */
    @Override
    @SuppressWarnings("unchecked")
    protected Contract loadContract(String contractAddress) {
        return LogisticsCore.load(contractAddress, client, cryptoKeyPair);
    }

    /**
     * 检查核心合约是否已加载
     */
    private void checkCoreContract() {
        if (logisticsCore == null) {
            throw new RuntimeException("物流核心合约未初始化，请检查区块链连接");
        }
    }

    /**
     * 检查操作合约是否已加载
     */
    private void checkOpsContract() {
        if (logisticsOps == null) {
            throw new RuntimeException("物流操作合约未初始化，请检查区块链连接");
        }
    }

    // ==================== LogisticsCore 合约方法 ====================

    /**
     * 创建物流委派单（核心合约）
     */
    public TransactionReceipt createLogisticsDelegateCore(String voucherNo) {
        checkCoreContract();

        logger.info("链上创建物流委派单: voucherNo={}", voucherNo);

        TransactionResponse response = sendTransactionWithAudit(
                logisticsCore,
                "createLogisticsDelegate",
                new Object[]{voucherNo},
                "LOGISTICS_CREATE"
        );

        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("链上创建物流委派单失败: {}", errorMsg);
            throw new RuntimeException("链上创建物流委派单失败: " + errorMsg);
        }

        logger.info("链上创建物流委派单成功: voucherNo={}, txHash={}",
                voucherNo, receipt != null ? receipt.getTransactionHash() : "N/A");
        return receipt;
    }

    /**
     * 获取物流状态
     */
    public BigInteger getStatus(String voucherNo) {
        checkCoreContract();

        try {
            return logisticsCore.getStatus(voucherNo);
        } catch (ContractException e) {
            logger.error("获取物流状态失败: voucherNo={}", voucherNo, e);
            throw new RuntimeException("获取物流状态失败: " + e.getMessage(), e);
        }
    }

    /**
     * 检查委派单是否存在
     */
    public Boolean exists(String voucherNo) {
        checkCoreContract();

        try {
            return logisticsCore.exists(voucherNo);
        } catch (ContractException e) {
            logger.error("检查委派单是否存在失败: voucherNo={}", voucherNo, e);
            throw new RuntimeException("检查委派单是否存在失败: " + e.getMessage(), e);
        }
    }

    // ==================== LogisticsOps 合约方法 ====================

    /**
     * 创建物流委派单（操作合约）
     */
    public TransactionReceipt createLogisticsDelegate(
            String voucherNo,
            int businessScene,
            String receiptId,
            BigInteger transportQuantity,
            String unit,
            byte[] ownerHash,
            byte[] carrierHash,
            byte[] sourceWhHash,
            byte[] targetWhHash,
            BigInteger validUntil) {
        checkOpsContract();

        logger.info("链上创建物流委派单: voucherNo={}, businessScene={}", voucherNo, businessScene);

        TransactionResponse response = sendTransactionWithAudit(
                logisticsOps,
                "createLogisticsDelegate",
                new Object[]{
                        voucherNo,
                        BigInteger.valueOf(businessScene),
                        receiptId,
                        transportQuantity,
                        unit,
                        ownerHash,
                        carrierHash,
                        sourceWhHash,
                        targetWhHash,
                        validUntil
                },
                "LOGISTICS_CREATE"
        );

        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("链上创建物流委派单失败: {}", errorMsg);
            throw new RuntimeException("链上创建物流委派单失败: " + errorMsg);
        }

        logger.info("链上创建物流委派单成功: voucherNo={}, txHash={}",
                voucherNo, receipt != null ? receipt.getTransactionHash() : "N/A");
        return receipt;
    }

    /**
     * 提货确认
     */
    public TransactionReceipt pickup(String voucherNo, BigInteger quantity) {
        checkOpsContract();

        logger.info("链上提货确认: voucherNo={}, quantity={}", voucherNo, quantity);

        TransactionResponse response = sendTransactionWithAudit(
                logisticsOps,
                "pickup",
                new Object[]{voucherNo, quantity},
                "LOGISTICS_PICKUP"
        );

        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("链上提货确认失败: {}", errorMsg);
            throw new RuntimeException("链上提货确认失败: " + errorMsg);
        }

        logger.info("链上提货确认成功: voucherNo={}, txHash={}",
                voucherNo, receipt != null ? receipt.getTransactionHash() : "N/A");
        return receipt;
    }

    /**
     * 到货并增加数量
     */
    public TransactionReceipt arriveAndAddQuantity(String voucherNo, String targetReceiptId, BigInteger quantity) {
        checkOpsContract();

        logger.info("链上到货增加数量: voucherNo={}, targetReceiptId={}, quantity={}",
                voucherNo, targetReceiptId, quantity);

        TransactionResponse response = sendTransactionWithAudit(
                logisticsOps,
                "arriveAndAddQuantity",
                new Object[]{voucherNo, targetReceiptId, quantity},
                "LOGISTICS_ARRIVE_ADD"
        );

        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("链上到货增加数量失败: {}", errorMsg);
            throw new RuntimeException("链上到货增加数量失败: " + errorMsg);
        }

        logger.info("链上到货增加数量成功: voucherNo={}, txHash={}",
                voucherNo, receipt != null ? receipt.getTransactionHash() : "N/A");
        return receipt;
    }

    /**
     * 到货并创建仓单
     */
    public TransactionReceipt arriveAndCreateReceipt(
            String voucherNo,
            String newReceiptId,
            BigInteger weight,
            String unit,
            byte[] ownerHash,
            byte[] warehouseHash) {
        checkOpsContract();

        logger.info("链上到货创建仓单: voucherNo={}, newReceiptId={}, weight={}",
                voucherNo, newReceiptId, weight);

        TransactionResponse response = sendTransactionWithAudit(
                logisticsOps,
                "arriveAndCreateReceipt",
                new Object[]{voucherNo, newReceiptId, weight, unit, ownerHash, warehouseHash},
                "LOGISTICS_ARRIVE_CREATE"
        );

        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("链上到货创建仓单失败: {}", errorMsg);
            throw new RuntimeException("链上到货创建仓单失败: " + errorMsg);
        }

        logger.info("链上到货创建仓单成功: voucherNo={}, txHash={}",
                voucherNo, receipt != null ? receipt.getTransactionHash() : "N/A");
        return receipt;
    }

    /**
     * 分配承运人
     */
    public TransactionReceipt assignCarrier(String voucherNo, byte[] carrierHash) {
        checkOpsContract();

        logger.info("链上分配承运人: voucherNo={}", voucherNo);

        TransactionResponse response = sendTransactionWithAudit(
                logisticsOps,
                "assignCarrier",
                new Object[]{voucherNo, carrierHash},
                "LOGISTICS_ASSIGN"
        );

        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("链上分配承运人失败: {}", errorMsg);
            throw new RuntimeException("链上分配承运人失败: " + errorMsg);
        }

        logger.info("链上分配承运人成功: voucherNo={}, txHash={}",
                voucherNo, receipt != null ? receipt.getTransactionHash() : "N/A");
        return receipt;
    }

    /**
     * 确认交付
     */
    public TransactionReceipt confirmDelivery(String voucherNo, int action, String targetReceiptId) {
        checkOpsContract();

        logger.info("链上确认交付: voucherNo={}, action={}", voucherNo, action);

        TransactionResponse response = sendTransactionWithAudit(
                logisticsOps,
                "confirmDelivery",
                new Object[]{voucherNo, BigInteger.valueOf(action), targetReceiptId},
                "LOGISTICS_CONFIRM"
        );

        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("链上确认交付失败: {}", errorMsg);
            throw new RuntimeException("链上确认交付失败: " + errorMsg);
        }

        logger.info("链上确认交付成功: voucherNo={}, txHash={}",
                voucherNo, receipt != null ? receipt.getTransactionHash() : "N/A");
        return receipt;
    }

    /**
     * 更新状态
     */
    public TransactionReceipt updateStatus(String voucherNo, int newStatus) {
        checkOpsContract();

        logger.info("链上更新状态: voucherNo={}, status={}", voucherNo, newStatus);

        TransactionResponse response = sendTransactionWithAudit(
                logisticsOps,
                "updateStatus",
                new Object[]{voucherNo, BigInteger.valueOf(newStatus)},
                "LOGISTICS_UPDATE_STATUS"
        );

        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("链上更新状态失败: {}", errorMsg);
            throw new RuntimeException("链上更新状态失败: " + errorMsg);
        }

        logger.info("链上更新状态成功: voucherNo={}, status={}, txHash={}",
                voucherNo, newStatus, receipt != null ? receipt.getTransactionHash() : "N/A");
        return receipt;
    }

    /**
     * 获取物流轨迹
     */
    @SuppressWarnings("unchecked")
    public List<BigInteger> getLogisticsTrack(String voucherNo) {
        checkOpsContract();

        try {
            return (List<BigInteger>) logisticsOps.getLogisticsTrack(voucherNo);
        } catch (ContractException e) {
            logger.error("获取物流轨迹失败: voucherNo={}", voucherNo, e);
            throw new RuntimeException("获取物流轨迹失败: " + e.getMessage(), e);
        }
    }

    /**
     * 验证物流委托
     */
    public Boolean validateLogisticsDelegate(String voucherNo) {
        checkOpsContract();

        try {
            return logisticsOps.validateLogisticsDelegate(voucherNo);
        } catch (ContractException e) {
            logger.error("验证物流委托失败: voucherNo={}", voucherNo, e);
            throw new RuntimeException("验证物流委托失败: " + e.getMessage(), e);
        }
    }

    /**
     * 检查承运人是否已授权
     */
    public Boolean isCarrierAuthorized(String voucherNo, byte[] carrierHash) {
        checkOpsContract();

        try {
            return logisticsOps.isCarrierAuthorized(voucherNo, carrierHash);
        } catch (ContractException e) {
            logger.error("检查承运人授权失败: voucherNo={}", voucherNo, e);
            throw new RuntimeException("检查承运人授权失败: " + e.getMessage(), e);
        }
    }

    /**
     * 使委派单失效
     */
    public TransactionReceipt invalidate(String voucherNo) {
        checkOpsContract();

        logger.info("链上使委派单失效: voucherNo={}", voucherNo);

        TransactionResponse response = sendTransactionWithAudit(
                logisticsOps,
                "invalidate",
                new Object[]{voucherNo},
                "LOGISTICS_INVALIDATE"
        );

        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("链上使委派单失效失败: {}", errorMsg);
            throw new RuntimeException("链上使委派单失效失败: " + errorMsg);
        }

        logger.info("链上使委派单失效成功: voucherNo={}, txHash={}",
                voucherNo, receipt != null ? receipt.getTransactionHash() : "N/A");
        return receipt;
    }
}
