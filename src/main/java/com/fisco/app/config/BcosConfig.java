package com.fisco.app.config;

import javax.annotation.PreDestroy;

import org.fisco.bcos.sdk.v3.BcosSDK;
import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.crypto.keypair.CryptoKeyPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * FISCO BCOS SDK 配置类
 * 负责初始化 SDK 客户端和管理连接
 *
 * 当 fisco.enabled=false 时，区块链相关功能将被禁用，但应用仍可正常启动
 */
@Configuration
public class BcosConfig {

    private static final Logger logger = LoggerFactory.getLogger(BcosConfig.class);

    @Value("${fisco.config-file:config.toml}")
    private String configFile;

    @Value("${fisco.group:group0}")
    private String group;

    @Value("${fisco.enabled:true}")
    private boolean fiscoEnabled;

    private BcosSDK sdk;

    @Bean
    @ConditionalOnProperty(name = "fisco.enabled", havingValue = "true", matchIfMissing = true)
    public BcosSDK bcosSDK() {
        try {
            String configPath;
            // Support both classpath resources and absolute file paths
            if (configFile.startsWith("/")) {
                // Absolute path - use directly
                configPath = configFile;
            } else {
                // Classpath resource - load from classpath
                configPath = getClass().getClassLoader().getResource(configFile).getPath();
            }
            logger.info("Initializing FISCO BCOS SDK with config: {}", configPath);
            sdk = BcosSDK.build(configPath);
            logger.info("FISCO BCOS SDK initialized successfully");
            return sdk;
        } catch (Exception e) {
            logger.error("Failed to initialize FISCO BCOS SDK", e);
            logger.warn("Blockchain functionality will be unavailable. To disable blockchain features, set fisco.enabled=false in application.properties");
            throw new RuntimeException("Failed to initialize FISCO BCOS SDK. Set fisco.enabled=false to start without blockchain support.", e);
        }
    }

    @Bean
    @ConditionalOnProperty(name = "fisco.enabled", havingValue = "true", matchIfMissing = true)
    public Client bcosClient(BcosSDK bcosSDK) {
        try {
            Client client = bcosSDK.getClient(group);
            logger.info("Connected to group: {}", group);
            return client;
        } catch (Exception e) {
            logger.error("Failed to get client for group: {}", group, e);
            throw new RuntimeException("Failed to get client for group: " + group, e);
        }
    }

    @Bean
    @ConditionalOnProperty(name = "fisco.enabled", havingValue = "true", matchIfMissing = true)
    public CryptoKeyPair cryptoKeyPair(Client client) {
        return client.getCryptoSuite().getCryptoKeyPair();
    }

    @PreDestroy
    public void destroy() {
        if (sdk != null) {
            logger.info("Destroying FISCO BCOS SDK resources...");
            // SDK cleanup is handled automatically
            logger.info("FISCO BCOS SDK resources cleaned up");
        }
    }
}
