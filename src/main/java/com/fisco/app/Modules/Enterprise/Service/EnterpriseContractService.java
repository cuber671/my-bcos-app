package com.fisco.app.Modules.Enterprise.Service;

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
import com.fisco.app.contract.enterprise.EnterpriseRegistryV2;
import com.fisco.app.contract.enterprise.EnterpriseRegistryV2.EnterpriseRegistrationInput;

import io.swagger.annotations.ApiOperation;

/**
 * 企业上链服务
 *
 * 提供企业注册，信息查询、状态更新等区块链操作
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@ApiOperation("企业上链服务")
@Service
public class EnterpriseContractService extends BaseContractService {

    private static final Logger logger = LoggerFactory.getLogger(EnterpriseContractService.class);

    /**
     * 企业合约地址
     */
    @Value("${contract.enterprise}")
    private String enterpriseContractAddress;

    /**
     * 企业合约实例
     */
    private EnterpriseRegistryV2 enterpriseContract;

    /**
     * 初始化企业合约
     */
    @javax.annotation.PostConstruct
    public void init() {
        if (!fiscoEnabled) {
            logger.warn("FISCO BCOS 功能已禁用，企业合约服务不可用");
            return;
        }
        if (client == null || cryptoKeyPair == null) {
            logger.error("区块链客户端未初始化，无法加载企业合约");
            return;
        }
        // 使用从配置文件加载的密钥对进行交易签名
        logger.info("使用 SDK 密钥对，地址: {}", cryptoKeyPair.getAddress());
        this.enterpriseContract = EnterpriseRegistryV2.load(
                enterpriseContractAddress,
                client,
                cryptoKeyPair
        );
        logger.info("企业合约加载成功，地址: {}", enterpriseContractAddress);
    }

    /**
     * 检查合约是否已加载
     */
    private void checkContract() {
        if (enterpriseContract == null) {
            throw new RuntimeException("服务暂不可用，请稍后重试");
        }
    }

    /**
     * 实现抽象方法 - 加载企业合约
     */
    @Override
    @SuppressWarnings("unchecked")
    protected org.fisco.bcos.sdk.v3.contract.Contract loadContract(String contractAddress) {
        return (org.fisco.bcos.sdk.v3.contract.Contract)
                EnterpriseRegistryV2.load(contractAddress, client, cryptoKeyPair);
    }

    // ==================== 企业注册 ====================

    /**
     * 注册企业上链
     *
     * @param enterpriseAddress 企业区块链地址
     * @param creditCode 统一社会信用代码
     * @param role 企业角色 (0:核心企业, 1:供应商, 2:金融机构)
     * @param metadataHash 元数据哈希
     * @return 交易收据
     */

    public TransactionReceipt registerEnterprise(
            String enterpriseAddress,
            String creditCode,
            BigInteger role,
            byte[] metadataHash) {

        checkContract();

        // 参数校验
        if (enterpriseAddress == null || enterpriseAddress.isBlank()) {
            throw new IllegalArgumentException("企业地址不能为空");
        }
        if (creditCode == null || creditCode.isBlank()) {
            throw new IllegalArgumentException("统一社会信用代码不能为空");
        }

        // 构建输入参数
        EnterpriseRegistrationInput input = new EnterpriseRegistrationInput(
                enterpriseAddress,
                creditCode,
                role,
                metadataHash != null ? metadataHash : new byte[32]
        );

        logger.info("注册企业上链: address={}, creditCode={}, role={}",
                enterpriseAddress, creditCode, role);

        TransactionResponse response = sendTransactionWithAudit(
                enterpriseContract,
                "registerEnterprise",
                new Object[]{input},
                "ENTERPRISE_REGISTER"
        );

        // 校验交易回执状态
        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("注册企业上链失败: {}", errorMsg);
            throw new RuntimeException("操作失败，请稍后重试");
        }
        return receipt;
    }

    /**
     * 批量注册企业
     *
     * @param enterprises 企业信息列表
     * @return 交易收据列表
     */

    public List<TransactionReceipt> batchRegisterEnterprise(
            List<EnterpriseRegisterRequest> enterprises) {

        checkContract();

        logger.info("批量注册企业，数量: {}", enterprises.size());

        // TODO: 实现批量注册
        throw new UnsupportedOperationException("批量注册功能开发中");
    }

    // ==================== 企业查询 ====================

    /**
     * 根据地址获取企业信息
     *
     * @param enterpriseAddress 企业区块链地址
     * @return 企业信息元组 (address, creditCode, role, status, creditLimit, creditRating, createdAt, metadataHash)
     * @throws ContractException 企业不存在
     */
    public EnterpriseInfo getEnterprise(String enterpriseAddress) throws ContractException {
        checkContract();

        logger.debug("查询企业信息: {}", enterpriseAddress);

        var result = enterpriseContract.getEnterprise(enterpriseAddress);
        return new EnterpriseInfo(result);
    }

    /**
     * 根据信用代码获取企业地址
     *
     * @param creditCode 统一社会信用代码
     * @return 企业区块链地址
     */
    public String getEnterpriseByCreditCode(String creditCode) {
        checkContract();

        logger.debug("根据信用代码查询企业: {}", creditCode);

        try {
            return enterpriseContract.getEnterpriseByCreditCode(creditCode);
        } catch (ContractException e) {
            logger.warn("企业不存在: creditCode={}", creditCode);
            return null;
        }
    }

    /**
     * 获取企业列表
     *
     * @return 企业地址列表
     */
    @SuppressWarnings("unchecked")
    public List<String> getEnterpriseList() {
        checkContract();

        logger.debug("获取企业列表");

        try {
            return (List<String>) (List<?>) enterpriseContract.getEnterpriseList();
        } catch (ContractException e) {
            logger.error("获取企业列表失败", e);
            return List.of();
        }
    }

    /**
     * 验证企业是否有效
     *
     * @param enterpriseAddress 企业地址
     * @return true 表示企业有效（已注册且状态正常）
     */
    public boolean isEnterpriseValid(String enterpriseAddress) {
        checkContract();

        logger.debug("验证企业有效性: {}", enterpriseAddress);

        try {
            return enterpriseContract.isEnterpriseValid(enterpriseAddress);
        } catch (ContractException e) {
            logger.warn("企业有效性验证失败: address={}", enterpriseAddress, e);
            return false;
        }
    }

    /**
     * 获取企业数量
     *
     * @return 企业总数
     */
    public BigInteger getEnterpriseCount() {
        checkContract();

        logger.debug("获取企业数量");

        try {
            return enterpriseContract.enterpriseCount();
        } catch (ContractException e) {
            logger.error("获取企业数量失败", e);
            return BigInteger.ZERO;
        }
    }

    // ==================== 企业状态管理 ====================

    /**
     * 更新企业状态
     *
     * @param enterpriseAddress 企业地址
     * @param newStatus 新状态 (0:禁用, 1:正常)
     * @return 交易收据
     */

    public TransactionReceipt updateEnterpriseStatus(String enterpriseAddress, BigInteger newStatus) {
        checkContract();

        // 参数校验
        if (enterpriseAddress == null || enterpriseAddress.isBlank()) {
            throw new IllegalArgumentException("企业地址不能为空");
        }
        if (newStatus == null) {
            throw new IllegalArgumentException("状态不能为空");
        }

        logger.info("更新企业状态: address={}, status={}", enterpriseAddress, newStatus);

        TransactionResponse response = sendTransactionWithAudit(
                enterpriseContract,
                "updateEnterpriseStatus",
                new Object[]{enterpriseAddress, newStatus, "审核通过"},
                "ENTERPRISE_UPDATE_STATUS"
        );

        // 校验交易回执状态
        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("更新企业状态失败: {}", errorMsg);
            throw new RuntimeException("操作失败，请稍后重试");
        }
        return receipt;
    }

    /**
     * 更新企业信用评级
     *
     * @param enterpriseAddress 企业地址
     * @param newRating 新评级
     * @return 交易收据
     */

    public TransactionReceipt updateCreditRating(String enterpriseAddress, BigInteger newRating) {
        checkContract();

        // 参数校验
        if (enterpriseAddress == null || enterpriseAddress.isBlank()) {
            throw new IllegalArgumentException("企业地址不能为空");
        }
        if (newRating == null) {
            throw new IllegalArgumentException("信用评级不能为空");
        }

        logger.info("更新企业信用评级: address={}, rating={}", enterpriseAddress, newRating);

        TransactionResponse response = sendTransactionWithAudit(
                enterpriseContract,
                "updateCreditRating",
                new Object[]{enterpriseAddress, newRating, "信用评级更新"},
                "ENTERPRISE_UPDATE_RATING"
        );

        // 校验交易回执状态
        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("更新企业信用评级失败: {}", errorMsg);
            throw new RuntimeException("操作失败，请稍后重试");
        }
        return receipt;
    }

    /**
     * 设置企业授信额度
     *
     * @param enterpriseAddress 企业地址
     * @param newLimit 新额度
     * @return 交易收据
     */

    public TransactionReceipt setCreditLimit(String enterpriseAddress, BigInteger newLimit) {
        checkContract();

        // 参数校验
        if (enterpriseAddress == null || enterpriseAddress.isBlank()) {
            throw new IllegalArgumentException("企业地址不能为空");
        }
        if (newLimit == null || newLimit.compareTo(BigInteger.ZERO) < 0) {
            throw new IllegalArgumentException("授信额度不能为负数");
        }

        logger.info("设置企业授信额度: address={}, limit={}", enterpriseAddress, newLimit);

        TransactionResponse response = sendTransactionWithAudit(
                enterpriseContract,
                "setCreditLimit",
                new Object[]{enterpriseAddress, newLimit},
                "ENTERPRISE_SET_CREDIT_LIMIT"
        );

        // 校验交易回执状态
        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("设置企业授信额度失败: {}", errorMsg);
            throw new RuntimeException("操作失败，请稍后重试");
        }
        return receipt;
    }

    // ==================== 企业注销 ====================

    /**
     * 注销企业
     *
     * @param enterpriseAddress 企业地址
     * @param reason 注销原因
     * @return 交易收据
     */

    public TransactionReceipt removeEnterprise(String enterpriseAddress, String reason) {
        checkContract();

        // 参数校验
        if (enterpriseAddress == null || enterpriseAddress.isBlank()) {
            throw new IllegalArgumentException("企业地址不能为空");
        }

        logger.info("注销企业: address={}, reason={}", enterpriseAddress, reason);

        TransactionResponse response = sendTransactionWithAudit(
                enterpriseContract,
                "removeEnterprise",
                new Object[]{enterpriseAddress, reason},
                "ENTERPRISE_REMOVE"
        );

        // 校验交易回执状态
        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("注销企业失败: {}", errorMsg);
            throw new RuntimeException("操作失败，请稍后重试");
        }
        return receipt;
    }

    // ==================== 内部类 ====================

    /**
     * 企业注册请求
     */
    public static class EnterpriseRegisterRequest {
        private String enterpriseAddress;
        private String creditCode;
        private BigInteger role;
        private byte[] metadataHash;

        public String getEnterpriseAddress() {
            return enterpriseAddress;
        }

        public void setEnterpriseAddress(String enterpriseAddress) {
            this.enterpriseAddress = enterpriseAddress;
        }

        public String getCreditCode() {
            return creditCode;
        }

        public void setCreditCode(String creditCode) {
            this.creditCode = creditCode;
        }

        public BigInteger getRole() {
            return role;
        }

        public void setRole(BigInteger role) {
            this.role = role;
        }

        public byte[] getMetadataHash() {
            return metadataHash;
        }

        public void setMetadataHash(byte[] metadataHash) {
            this.metadataHash = metadataHash;
        }
    }

    /**
     * 企业信息封装
     */
    public static class EnterpriseInfo {
        private String address;
        private String creditCode;
        private BigInteger role;
        private BigInteger status;
        private BigInteger creditLimit;
        private BigInteger creditRating;
        private BigInteger createdAt;
        private byte[] metadataHash;

        public EnterpriseInfo(
                org.fisco.bcos.sdk.v3.codec.datatypes.generated.tuples.generated.Tuple8<
                        String, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger, byte[]> tuple) {
            this.address = tuple.getValue1();
            // 合约返回的 creditCode 是 BigInteger，需转换为 String
            this.creditCode = tuple.getValue2() != null ? tuple.getValue2().toString() : null;
            this.role = tuple.getValue3();
            this.status = tuple.getValue4();
            this.creditLimit = tuple.getValue5();
            this.creditRating = tuple.getValue6();
            this.createdAt = tuple.getValue7();
            this.metadataHash = tuple.getValue8();
        }

        // Getters
        public String getAddress() { return address; }
        public String getCreditCode() { return creditCode; }
        public BigInteger getRole() { return role; }
        public BigInteger getStatus() { return status; }
        public BigInteger getCreditLimit() { return creditLimit; }
        public BigInteger getCreditRating() { return creditRating; }
        public BigInteger getCreatedAt() { return createdAt; }
        public byte[] getMetadataHash() { return metadataHash; }

        // Status helper
        public boolean isActive() {
            return status != null && status.compareTo(BigInteger.ZERO) > 0;
        }

        // Role helper
        public String getRoleName() {
            if (role == null) return "未知";
            switch (role.intValue()) {
                case 0: return "核心企业";
                case 1: return "供应商";
                case 2: return "金融机构";
                default: return "未知";
            }
        }
    }
}
