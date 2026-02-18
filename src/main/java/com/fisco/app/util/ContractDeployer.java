package com.fisco.app.util;

import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.crypto.keypair.CryptoKeyPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.fisco.app.contract.bill.BillV2;
import com.fisco.app.contract.receivable.ReceivableV2;
import com.fisco.app.contract.warehouse.WarehouseReceiptV2;
import com.fisco.app.contract.enterprise.EnterpriseRegistryV2;


/**
 * 智能合约部署工具类
 */
@Component
@ConditionalOnProperty(name = "fisco.enabled", havingValue = "true", matchIfMissing = true)
public class ContractDeployer {

    private static final Logger log = LoggerFactory.getLogger(ContractDeployer.class);

    private final Client client;
    private final CryptoKeyPair cryptoKeyPair;

    public ContractDeployer(Client client, CryptoKeyPair cryptoKeyPair) {
        this.client = client;
        this.cryptoKeyPair = cryptoKeyPair;
    }

    /**
     * 部署BillV2合约
     */
    public DeployResult deployBill() {
        try {
            log.info("========== 开始部署 Bill V2 合约 ==========");
            log.info("正在部署 Bill V2 合约到区块链...");

            // 使用生成的BillV2合约类部署
            BillV2 bill = BillV2.deploy(client, cryptoKeyPair);

            // 获取合约地址
            String contractAddress = bill.getContractAddress();

            log.info("✅ Bill V2 合约部署成功！");
            log.info("   合约地址: {}", contractAddress);
            log.info("=====================================");

            return new DeployResult(contractAddress, "", "");

        } catch (Exception e) {
            log.error("❌ Bill V2 合约部署失败", e);
            throw new RuntimeException("Bill V2合约部署失败: " + e.getMessage(), e);
        }
    }

    /**
     * 部署ReceivableV2合约
     */
    public DeployResult deployReceivable() {
        try {
            log.info("========== 开始部署 Receivable V2 合约 ==========");
            log.info("正在部署 Receivable V2 合约到区块链...");

            // V2合约需要admin参数，使用部署者的地址
            String adminAddress = cryptoKeyPair.getAddress();

            // 使用生成的ReceivableV2合约类部署
            ReceivableV2 receivable = ReceivableV2.deploy(client, cryptoKeyPair, adminAddress);

            // 获取合约地址
            String contractAddress = receivable.getContractAddress();

            log.info("✅ Receivable V2 合约部署成功！");
            log.info("   合约地址: {}", contractAddress);
            log.info("   管理员地址: {}", adminAddress);
            log.info("======================================");

            return new DeployResult(contractAddress, "", "");

        } catch (Exception e) {
            log.error("❌ Receivable V2 合约部署失败", e);
            throw new RuntimeException("Receivable V2合约部署失败: " + e.getMessage(), e);
        }
    }

    /**
     * 部署WarehouseReceiptV2合约
     */
    public DeployResult deployWarehouseReceipt() {
        try {
            log.info("========== 开始部署 WarehouseReceipt V2 合约 ==========");
            log.info("正在部署 WarehouseReceipt V2 合约到区块链...");

            // 使用生成的WarehouseReceiptV2合约类部署
            WarehouseReceiptV2 warehouseReceipt = WarehouseReceiptV2.deploy(client, cryptoKeyPair);

            // 获取合约地址
            String contractAddress = warehouseReceipt.getContractAddress();

            log.info("✅ WarehouseReceipt V2 合约部署成功！");
            log.info("   合约地址: {}", contractAddress);
            log.info("=========================================");

            return new DeployResult(contractAddress, "", "");

        } catch (Exception e) {
            log.error("❌ WarehouseReceipt V2 合约部署失败", e);
            throw new RuntimeException("WarehouseReceipt V2合约部署失败: " + e.getMessage(), e);
        }
    }

    /**
     * 部署EnterpriseRegistryV2合约
     */
    public DeployResult deployEnterpriseRegistry() {
        try {
            log.info("========== 开始部署 EnterpriseRegistry V2 合约 ==========");
            log.info("正在部署 EnterpriseRegistry V2 合约到区块链...");

            // V2合约需要admin参数，使用部署者的地址
            String adminAddress = cryptoKeyPair.getAddress();

            // 使用生成的EnterpriseRegistryV2合约类部署
            EnterpriseRegistryV2 enterpriseRegistry = EnterpriseRegistryV2.deploy(client, cryptoKeyPair, adminAddress);

            // 获取合约地址
            String contractAddress = enterpriseRegistry.getContractAddress();

            log.info("✅ EnterpriseRegistry V2 合约部署成功！");
            log.info("   合约地址: {}", contractAddress);
            log.info("   管理员地址: {}", adminAddress);
            log.info("==========================================");

            return new DeployResult(contractAddress, "", "");

        } catch (Exception e) {
            log.error("❌ EnterpriseRegistry V2 合约部署失败", e);
            throw new RuntimeException("EnterpriseRegistry V2合约部署失败: " + e.getMessage(), e);
        }
    }

    /**
     * 部署所有合约
     */
    public void deployAll() {
        log.info("\n");
        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║       FISCO BCOS 智能合约批量部署工具                        ║");
        log.info("╚════════════════════════════════════════════════════════════╝");
        log.info("");

        try {
            DeployResult billResult = deployBill();
            Thread.sleep(2000); // 等待2秒，避免交易冲突

            DeployResult receivableResult = deployReceivable();
            Thread.sleep(2000);

            DeployResult warehouseReceiptResult = deployWarehouseReceipt();
            Thread.sleep(2000);

            DeployResult enterpriseRegistryResult = deployEnterpriseRegistry();

            // 输出配置信息
            log.info("\n");
            log.info("╔════════════════════════════════════════════════════════════╗");
            log.info("║              部署完成！请将以下配置添加到配置文件中            ║");
            log.info("╚════════════════════════════════════════════════════════════╝");
            log.info("");
            log.info("# Smart Contract Addresses");
            log.info("contracts.bill.address={}", billResult.contractAddress);
            log.info("contracts.receivable.address={}", receivableResult.contractAddress);
            log.info("contracts.warehouse-receipt.address={}", warehouseReceiptResult.contractAddress);
            log.info("contracts.enterprise.address={}", enterpriseRegistryResult.contractAddress);
            log.info("");

        } catch (Exception e) {
            log.error("批量部署过程中出现错误", e);
            throw new RuntimeException("批量部署失败", e);
        }
    }

    /**
     * 部署结果
     */
    public static class DeployResult {
        public final String contractAddress;
        public final String transactionHash;
        public final String gasUsed;

        public DeployResult(String contractAddress, String transactionHash, String gasUsed) {
            this.contractAddress = contractAddress;
            this.transactionHash = transactionHash;
            this.gasUsed = gasUsed;
        }
    }
}
