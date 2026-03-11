package com.fisco.app.Modules.Warehouse.Service;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.fisco.bcos.sdk.v3.model.TransactionReceipt;
import org.fisco.bcos.sdk.v3.transaction.model.dto.TransactionResponse;
import org.fisco.bcos.sdk.v3.transaction.model.exception.ContractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fisco.app.Common.Service.BaseContractService;
import com.fisco.app.contract.warehouse.WarehouseReceiptCore;
import com.fisco.app.contract.warehouse.WarehouseReceiptCore.ReceiptInput;
import com.fisco.app.contract.warehouse.WarehouseReceiptCore.MergeInput;
import com.fisco.app.contract.warehouse.WarehouseReceiptCore.SplitInput;
import com.fisco.app.contract.warehouse.WarehouseReceiptOps;
import com.fisco.app.contract.warehouse.WarehouseReceiptOps.EndorsementInput;

import io.swagger.annotations.ApiOperation;

/**
 * 仓单上链服务
 *
 * 提供仓单注册、查询、转让、拆分、合并、质押、背书等区块链操作
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@ApiOperation("仓单上链服务")
@Service
public class WarehouseReceiptContractService extends BaseContractService {

    private static final Logger logger = LoggerFactory.getLogger(WarehouseReceiptContractService.class);

    /**
     * 仓单核心合约地址
     */
    @Value("${contract.addresses.warehouse-core:}")
    private String warehouseCoreAddress;

    /**
     * 仓单运营合约地址
     */
    @Value("${contract.addresses.warehouse-ops:}")
    private String warehouseOpsAddress;

    /**
     * 仓单核心合约实例
     */
    private WarehouseReceiptCore warehouseCoreContract;

    /**
     * 仓单运营合约实例
     */
    private WarehouseReceiptOps warehouseOpsContract;

    /**
     * 初始化仓单合约
     */
    @javax.annotation.PostConstruct
    public void init() {
        if (!fiscoEnabled) {
            logger.warn("FISCO BCOS 功能已禁用，仓单合约服务不可用");
            return;
        }
        if (client == null || cryptoKeyPair == null) {
            logger.error("区块链客户端未初始化，无法加载仓单合约");
            return;
        }
        // 加载仓单核心合约
        if (warehouseCoreAddress != null && !warehouseCoreAddress.isEmpty()) {
            this.warehouseCoreContract = WarehouseReceiptCore.load(
                    warehouseCoreAddress,
                    client,
                    cryptoKeyPair
            );
            logger.info("仓单核心合约加载成功，地址: {}", warehouseCoreAddress);
        } else {
            logger.warn("仓单核心合约地址未配置");
        }
        // 加载仓单运营合约
        if (warehouseOpsAddress != null && !warehouseOpsAddress.isEmpty()) {
            this.warehouseOpsContract = WarehouseReceiptOps.load(
                    warehouseOpsAddress,
                    client,
                    cryptoKeyPair
            );
            logger.info("仓单运营合约加载成功，地址: {}", warehouseOpsAddress);
        } else {
            logger.warn("仓单运营合约地址未配置");
        }
    }

    /**
     * 检查核心合约是否已加载
     */
    private void checkCoreContract() {
        if (warehouseCoreContract == null) {
            throw new RuntimeException("仓单核心合约未初始化，请检查区块链连接");
        }
    }

    /**
     * 检查运营合约是否已加载
     */
    private void checkOpsContract() {
        if (warehouseOpsContract == null) {
            throw new RuntimeException("仓单运营合约未初始化，请检查区块链连接");
        }
    }

    /**
     * 实现抽象方法 - 加载仓单核心合约
     */
    @Override
    @SuppressWarnings("unchecked")
    protected org.fisco.bcos.sdk.v3.contract.Contract loadContract(String contractAddress) {
        return (org.fisco.bcos.sdk.v3.contract.Contract)
                WarehouseReceiptCore.load(contractAddress, client, cryptoKeyPair);
    }

    // ==================== 仓单查询 BC_012 ====================

    /**
     * 根据仓单ID查询仓单信息
     *
     * @param receiptId 仓单ID
     * @return 仓单信息
     * @throws ContractException 仓单不存在
     */
    public ReceiptInfo getReceipt(String receiptId) throws ContractException {
        checkCoreContract();
        logger.debug("查询仓单信息: {}", receiptId);

        var result = warehouseCoreContract.getReceipt(receiptId);
        return new ReceiptInfo(result);
    }

    /**
     * 根据所有者查询仓单ID列表
     *
     * @param owner 所有者地址哈希
     * @param offset 起始偏移
     * @param limit 查询数量限制
     * @return 仓单ID列表
     */
    @SuppressWarnings("unchecked")
    public List<String> getReceiptIdsByOwner(String owner, BigInteger offset, BigInteger limit) {
        checkCoreContract();
        logger.debug("查询所有者仓单列表: owner={}, offset={}, limit={}", owner, offset, limit);

        try {
            return (List<String>) (List<?>) warehouseCoreContract.getReceiptIdsByOwner(owner, offset, limit);
        } catch (ContractException e) {
            logger.error("查询所有者仓单列表失败", e);
            return List.of();
        }
    }

    /**
     * 获取仓单权重
     *
     * @param receiptId 仓单ID
     * @return 仓单权重
     */
    public BigInteger getReceiptWeight(String receiptId) {
        checkCoreContract();
        logger.debug("获取仓单权重: {}", receiptId);

        try {
            return warehouseCoreContract.getReceiptWeight(receiptId);
        } catch (ContractException e) {
            logger.error("获取仓单权重失败", e);
            return BigInteger.ZERO;
        }
    }

    // ==================== 仓单签发 BC_013 ====================

    /**
     * 签发新仓单
     *
     * @param receiptId 仓单ID
     * @param ownerHash 所有者哈希
     * @param warehouseHash 仓库哈希
     * @param goodsDetailHash 货物详情哈希
     * @param locationPhotoHash 货位照片哈希
     * @param contractHash 合同哈希
     * @param weight 重量
     * @param unit 单位
     * @param quantity 数量
     * @param storageDate 存储日期
     * @param expiryDate 到期日期
     * @return 交易收据
     */
    @Transactional
    public TransactionReceipt issueReceipt(
            String receiptId,
            byte[] ownerHash,
            byte[] warehouseHash,
            byte[] goodsDetailHash,
            byte[] locationPhotoHash,
            byte[] contractHash,
            BigInteger weight,
            String unit,
            BigInteger quantity,
            BigInteger storageDate,
            BigInteger expiryDate) {

        checkCoreContract();

        ReceiptInput input = new ReceiptInput(
                receiptId,
                ownerHash != null ? ownerHash : new byte[32],
                warehouseHash != null ? warehouseHash : new byte[32],
                goodsDetailHash != null ? goodsDetailHash : new byte[32],
                locationPhotoHash != null ? locationPhotoHash : new byte[32],
                contractHash != null ? contractHash : new byte[32],
                weight,
                unit != null ? unit : "吨",
                quantity != null ? quantity : BigInteger.ONE,
                storageDate != null ? storageDate : BigInteger.ZERO,
                expiryDate != null ? expiryDate : BigInteger.ZERO
        );

        logger.info("签发仓单: receiptId={}, warehouseHash={}, weight={}", receiptId, warehouseHash, weight);

        TransactionResponse response = sendTransactionWithAudit(
                warehouseCoreContract,
                "issueReceipt",
                new Object[]{input},
                "WAREHOUSE_ISSUE"
        );

        // 校验交易回执状态
        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("签发仓单失败: {}", errorMsg);
            throw new RuntimeException("链上交易失败: " + errorMsg);
        }
        return receipt;
    }

    // ==================== 仓单背书转让 BC_014 ====================

    /**
     * 发起仓单背书转让
     *
     * @param receiptId 仓单ID
     * @param fromHash 转出方哈希
     * @param toHash 转入方哈希
     * @return 交易收据
     */
    @Transactional
    public TransactionReceipt launchEndorsement(
            String receiptId,
            byte[] fromHash,
            byte[] toHash) {

        checkOpsContract();

        EndorsementInput input = new EndorsementInput(
                receiptId,
                fromHash != null ? fromHash : new byte[32],
                toHash != null ? toHash : new byte[32],
                "STANDARD"
        );

        logger.info("发起仓单背书: receiptId={}", receiptId);

        TransactionResponse response = sendTransactionWithAudit(
                warehouseOpsContract,
                "launchEndorsement",
                new Object[]{input},
                "WAREHOUSE_LAUNCH_ENDORSEMENT"
        );

        // 校验交易回执状态
        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("发起背书失败: {}", errorMsg);
            throw new RuntimeException("链上交易失败: " + errorMsg);
        }
        return receipt;
    }

    /**
     * 确认仓单背书转让
     *
     * @param receiptId 仓单ID
     * @param fromHash 转出方哈希
     * @param toHash 转入方哈希
     * @return 交易收据
     */
    @Transactional
    public TransactionReceipt confirmEndorsement(
            String receiptId,
            byte[] fromHash,
            byte[] toHash) {

        checkOpsContract();

        logger.info("确认仓单背书: receiptId={}", receiptId);

        TransactionResponse response = sendTransactionWithAudit(
                warehouseOpsContract,
                "confirmEndorsement",
                new Object[]{receiptId, fromHash, toHash},
                "WAREHOUSE_CONFIRM_ENDORSEMENT"
        );

        // 校验交易回执状态
        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("确认背书失败: {}", errorMsg);
            throw new RuntimeException("链上交易失败: " + errorMsg);
        }
        return receipt;
    }

    /**
     * 拒绝仓单背书转让
     *
     * @param receiptId 仓单ID
     * @param fromHash 转出方哈希
     * @param reason 拒绝原因
     * @return 交易收据
     */
    @Transactional
    public TransactionReceipt rejectEndorsement(
            String receiptId,
            byte[] fromHash,
            String reason) {

        checkOpsContract();

        logger.info("拒绝仓单背书: receiptId={}, reason={}", receiptId, reason);

        TransactionResponse response = sendTransactionWithAudit(
                warehouseOpsContract,
                "rejectEndorsement",
                new Object[]{receiptId, fromHash, reason},
                "WAREHOUSE_REJECT_ENDORSEMENT"
        );

        // 校验交易回执状态
        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("拒绝背书失败: {}", errorMsg);
            throw new RuntimeException("链上交易失败: " + errorMsg);
        }
        return receipt;
    }

    // ==================== 仓单转让 BC_015 ====================

    /**
     * 转让仓单
     *
     * @param receiptId 仓单ID
     * @param newOwnerHash 新所有者哈希
     * @return 交易收据
     */
    @Transactional
    public TransactionReceipt transferReceipt(String receiptId, byte[] newOwnerHash) {
        checkCoreContract();

        logger.info("转让仓单: receiptId={}", receiptId);

        TransactionResponse response = sendTransactionWithAudit(
                warehouseCoreContract,
                "transferReceipt",
                new Object[]{receiptId, newOwnerHash != null ? newOwnerHash : new byte[32]},
                "WAREHOUSE_TRANSFER"
        );

        // 校验交易回执状态
        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("转让仓单失败: {}", errorMsg);
            throw new RuntimeException("链上交易失败: " + errorMsg);
        }
        return receipt;
    }

    // ==================== 仓单拆分 BC_016 ====================

    /**
     * 拆分仓单
     *
     * @param originalReceiptId 源仓单ID
     * @param newReceiptIds 新仓单ID列表
     * @param weights 对应重量列表
     * @param ownerHashes 对应所有者哈希列表
     * @param unit 单位
     * @return 交易收据
     */
    @Transactional
    public TransactionReceipt splitReceipt(
            String originalReceiptId,
            List<String> newReceiptIds,
            List<BigInteger> weights,
            List<byte[]> ownerHashes,
            String unit) {

        checkCoreContract();

        // 参数校验
        if (newReceiptIds == null || newReceiptIds.isEmpty()) {
            throw new IllegalArgumentException("新仓单ID列表不能为空");
        }
        if (weights == null || weights.size() != newReceiptIds.size()) {
            throw new IllegalArgumentException("重量列表长度必须与新仓单ID列表一致");
        }
        if (ownerHashes == null) {
            ownerHashes = new ArrayList<>();
        }

        // 填充默认所有者哈希
        while (ownerHashes.size() < newReceiptIds.size()) {
            ownerHashes.add(new byte[32]);
        }

        SplitInput input = new SplitInput(
                originalReceiptId,
                newReceiptIds,
                weights,
                ownerHashes,
                unit != null ? unit : "吨"
        );

        logger.info("拆分仓单: original={}, newCount={}", originalReceiptId, newReceiptIds.size());

        TransactionResponse response = sendTransactionWithAudit(
                warehouseCoreContract,
                "splitReceipt",
                new Object[]{input},
                "WAREHOUSE_SPLIT"
        );

        // 校验交易回执状态
        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("拆分仓单失败: {}", errorMsg);
            throw new RuntimeException("链上交易失败: " + errorMsg);
        }
        return receipt;
    }

    // ==================== 仓单合并 BC_017 ====================

    /**
     * 合并仓单
     *
     * @param sourceReceiptIds 源仓单ID列表
     * @param targetReceiptId 目标仓单ID
     * @param targetOwnerHash 目标所有者哈希
     * @param unit 单位
     * @param totalWeight 总重量
     * @return 交易收据
     */
    @Transactional
    public TransactionReceipt mergeReceipts(List<String> sourceReceiptIds, String targetReceiptId,
            byte[] targetOwnerHash, String unit, BigInteger totalWeight) {
        checkCoreContract();

        if (sourceReceiptIds == null || sourceReceiptIds.isEmpty()) {
            throw new IllegalArgumentException("源仓单ID列表不能为空");
        }

        MergeInput input = new MergeInput(
                sourceReceiptIds,
                targetReceiptId,
                targetOwnerHash != null ? targetOwnerHash : new byte[32],
                unit != null ? unit : "吨",
                totalWeight != null ? totalWeight : BigInteger.ZERO
        );

        logger.info("合并仓单: sources={}, target={}", sourceReceiptIds, targetReceiptId);

        TransactionResponse response = sendTransactionWithAudit(
                warehouseCoreContract,
                "mergeReceipts",
                new Object[]{input},
                "WAREHOUSE_MERGE"
        );

        // 校验交易回执状态
        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("合并仓单失败: {}", errorMsg);
            throw new RuntimeException("链上交易失败: " + errorMsg);
        }
        return receipt;
    }

    // ==================== 仓单质押 BC_018 ====================

    /**
     * 质押仓单（锁定）
     *
     * @param receiptId 仓单ID
     * @return 交易收据
     */
    @Transactional
    public TransactionReceipt lockReceipt(String receiptId) {
        checkCoreContract();

        logger.info("质押仓单: receiptId={}", receiptId);

        TransactionResponse response = sendTransactionWithAudit(
                warehouseCoreContract,
                "lockReceipt",
                new Object[]{receiptId},
                "WAREHOUSE_LOCK"
        );

        // 校验交易回执状态
        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("质押仓单失败: {}", errorMsg);
            throw new RuntimeException("链上交易失败: " + errorMsg);
        }
        return receipt;
    }

    // ==================== 仓单解除质押 BC_019 ====================

    /**
     * 解除质押（解锁）
     *
     * @param receiptId 仓单ID
     * @return 交易收据
     */
    @Transactional
    public TransactionReceipt unlockReceipt(String receiptId) {
        checkCoreContract();

        logger.info("解除质押仓单: receiptId={}", receiptId);

        TransactionResponse response = sendTransactionWithAudit(
                warehouseCoreContract,
                "unlockReceipt",
                new Object[]{receiptId},
                "WAREHOUSE_UNLOCK"
        );

        // 校验交易回执状态
        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("解除质押失败: {}", errorMsg);
            throw new RuntimeException("链上交易失败: " + errorMsg);
        }
        return receipt;
    }

    // ==================== 仓单核销 BC_020 ====================

    /**
     * 核销仓单
     *
     * @param receiptId 仓单ID
     * @param signatureHash 核销签名哈希
     * @return 交易收据
     */
    @Transactional
    public TransactionReceipt burnReceipt(String receiptId, byte[] signatureHash) {
        checkCoreContract();

        logger.info("核销仓单: receiptId={}", receiptId);

        TransactionResponse response = sendTransactionWithAudit(
                warehouseCoreContract,
                "burnReceipt",
                new Object[]{receiptId, signatureHash != null ? signatureHash : new byte[32]},
                "WAREHOUSE_BURN"
        );

        // 校验交易回执状态
        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("核销仓单失败: {}", errorMsg);
            throw new RuntimeException("链上交易失败: " + errorMsg);
        }
        return receipt;
    }

    // ==================== 仓单取消 BC_021 ====================

    /**
     * 取消仓单
     *
     * @param receiptId 仓单ID
     * @param reason 取消原因
     * @return 交易收据
     */
    @Transactional
    public TransactionReceipt cancelReceipt(String receiptId, String reason) {
        checkCoreContract();

        logger.info("取消仓单: receiptId={}, reason={}", receiptId, reason);

        TransactionResponse response = sendTransactionWithAudit(
                warehouseCoreContract,
                "cancelReceipt",
                new Object[]{receiptId, reason},
                "WAREHOUSE_CANCEL"
        );

        // 校验交易回执状态
        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("取消仓单失败: {}", errorMsg);
            throw new RuntimeException("链上交易失败: " + errorMsg);
        }
        return receipt;
    }

    // ==================== 内部类 ====================

    /**
     * 仓单信息封装
     */
    public static class ReceiptInfo {
        private String receiptId;
        private byte[] ownerHash;
        private byte[] warehouseHash;
        private BigInteger status;
        private String warehouse;
        private BigInteger weight;
        private BigInteger createTime;

        public ReceiptInfo(
                org.fisco.bcos.sdk.v3.codec.datatypes.generated.tuples.generated.Tuple7<
                        String, byte[], byte[], BigInteger, String, BigInteger, BigInteger> tuple) {
            this.receiptId = tuple.getValue1();
            this.ownerHash = tuple.getValue2();
            this.warehouseHash = tuple.getValue3();
            this.status = tuple.getValue4();
            this.warehouse = tuple.getValue5();
            this.weight = tuple.getValue6();
            this.createTime = tuple.getValue7();
        }

        // Getters
        public String getReceiptId() { return receiptId; }
        public byte[] getOwnerHash() { return ownerHash; }
        public byte[] getWarehouseHash() { return warehouseHash; }
        public BigInteger getStatus() { return status; }
        public String getWarehouse() { return warehouse; }
        public BigInteger getWeight() { return weight; }
        public BigInteger getCreateTime() { return createTime; }

        // Status helper
        public String getStatusName() {
            if (status == null) return "未知";
            switch (status.intValue()) {
                case 0: return "已签发";
                case 1: return "已转让";
                case 2: return "已质押";
                case 3: return "已核销";
                case 4: return "已取消";
                case 5: return "背书中";
                default: return "未知";
            }
        }

        public boolean isAvailable() {
            // 状态为0(已签发)或1(已转让)时可操作
            return status != null && (status.compareTo(BigInteger.ZERO) == 0 || status.compareTo(BigInteger.ONE) == 0);
        }

        public boolean isLocked() {
            // 状态为2(已质押)时锁定
            return status != null && status.compareTo(BigInteger.valueOf(2)) == 0;
        }
    }
}
