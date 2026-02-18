package com.fisco.app.deployment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.crypto.keypair.CryptoKeyPair;
import org.fisco.bcos.sdk.v3.transaction.model.exception.ContractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fisco.app.contract.bill.BillV2;
import com.fisco.app.contract.enterprise.EnterpriseRegistryV2;
import com.fisco.app.contract.warehouse.WarehouseReceiptV2;

/**
 * 合约管理器：负责合约的生命周期管理（部署、持久化、加载）
 */
@Component
public class ContractManager {
    private static final Logger logger = LoggerFactory.getLogger(ContractManager.class);
    private static final String ADDRESS_CONF = "conf/contract-addresses.properties";
    
    private final Client client;
    private final CryptoKeyPair cryptoKeyPair;
    private final Properties props = new Properties();

    public ContractManager(Client client, CryptoKeyPair cryptoKeyPair) {
        this.client = client;
        this.cryptoKeyPair = cryptoKeyPair;
        loadProperties();
    }

    /**
     * 一键部署所有合约（带幂等性保护）
     */
    public void deployAll() {
        // 1. 幂等性检查：如果核心合约地址已存在，则跳过部署
        if (props.containsKey("BillV2") && !props.getProperty("BillV2").isEmpty()) {
            logger.info(">>> [跳过部署] 检测到本地已存在合约地址记录。");
            logger.info(">>> 若需重新部署，请手动删除文件: {}", ADDRESS_CONF);
            return;
        }

        try {
            logger.info(">>> [开始部署] 正在初始化供应链金融平台合约...");

            // 1. 部署企业登记合约 (需传入初始信息)
            String registryInfo = "FISCO-BCOS-SupplyChain-V2"; 
            EnterpriseRegistryV2 registry = EnterpriseRegistryV2.deploy(client, cryptoKeyPair, registryInfo);
            saveAddress("EnterpriseRegistryV2", registry.getContractAddress());

            // 2. 部署仓单合约
            WarehouseReceiptV2 receipt = WarehouseReceiptV2.deploy(client, cryptoKeyPair);
            saveAddress("WarehouseReceiptV2", receipt.getContractAddress());

            // 3. 部署票据核心合约
            BillV2 billV2 = BillV2.deploy(client, cryptoKeyPair);
            saveAddress("BillV2", billV2.getContractAddress());

            logger.info(">>> [部署成功] 所有合约已上链并保存地址。");

        } catch (ContractException e) {
            logger.error(">>> [部署失败] 合约回执异常: {}", e.getMessage());
        } catch (Exception e) {
            logger.error(">>> [部署失败] 系统异常: ", e);
        }
    }

    /**
     * 加载票据服务
     */
    public BillV2 getBillService() {
        String address = props.getProperty("BillV2");
        if (address == null) return null;
        return BillV2.load(address, client, cryptoKeyPair);
    }

    // --- 持久化工具方法 ---

    private void saveAddress(String name, String address) {
        props.setProperty(name, address);
        File file = new File(ADDRESS_CONF);
        if (file.getParentFile() != null && !file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        try (FileOutputStream fos = new FileOutputStream(file)) {
            props.store(fos, "Contract Addresses Storage");
            logger.info("已记录地址: {} -> {}", name, address);
        } catch (IOException e) {
            logger.error("保存地址文件失败: {}", e.getMessage());
        }
    }

    private void loadProperties() {
        File file = new File(ADDRESS_CONF);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                props.load(fis);
                logger.info("已加载本地 {} 个合约地址记录", props.size());
            } catch (IOException e) {
                logger.warn("配置文件读取异常");
            }
        }
    }
}